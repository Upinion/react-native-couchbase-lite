//
//  RCTCouchBase.m
//  RCTCouchBase
//
//  Created by Alberto Martinez de Murga on 16/02/2016.
//  Copyright © 2016 Upinion. All rights reserved.
//

#import "RCTCouchBase.h"

@implementation RCTCouchBase

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE(CouchBase)

NSString* const PUSH = @"couchBasePushEvent";
NSString* const PULL = @"couchBasePullEvent";
NSString* const DB_CHANGED = @"couchBaseDBEvent";
NSString* const AUTH_ERROR = @"couchbBaseAuthError";

- (id)init
{
    self = [super init];
    
    if (self) {
        CBLRegisterJSViewCompiler();
        manager = [CBLManager sharedInstance];
        databases = [[NSMutableDictionary alloc] init];
        pulls = [[NSMutableDictionary alloc] init];
        pushes = [[NSMutableDictionary alloc] init];
        timeout = 0;
        
        if (!manager) {
            NSLog(@"Cannot create Manager instance");
            exit(-1);
        }
    }
    return self;
}


- (NSString*)getName
{
    return @"CouchBase";
}


- (NSDictionary*)constantsToExport
{
    return @{
             @"PUSH" : PUSH,
             @"PULL" : PULL,
             @"DBChanged": DB_CHANGED,
             @"AuthError": AUTH_ERROR
             };
}


- (void) startServer: (int) port
            withUser: (NSString *) user
        withPassword: (NSString * ) pass
        withCallback: (RCTResponseSenderBlock) onEnd
{
    
initListener:
    // Set up the listener.
    listener = [[CBLListener alloc] initWithManager:manager port:port];
    if (user != nil && pass != nil){
        NSDictionary *auth = @{user:pass};
        [listener setPasswords: auth];
    }

    // Init the listener.
    @try {
        NSError *err = nil;
        BOOL success = [listener start: &err];
        
        // Error handler
        if (success) {
            NSLog(@"CouchBase running on %@", listener.URL);
            // Callback handler
            if (onEnd != nil) {
                onEnd(@[[NSNumber numberWithInt:listener.port]]);
            }
            
        } else {
            NSLog(@"%@", err);
            //Close old databases
            for (CBLDatabase *db in [databases allValues]) {
                [db close:nil];
            }
            port++;
            goto initListener;
        }
        
        // Exception handler
    } @catch (NSException *e) {
        NSLog(@"%@",e);
    }
    
}


- (void) startSync: (NSString*) databaseLocal
     withRemoteUrl: (NSString*) remoteUrl
    withRemoteUser: (NSString*) remoteUser
withRemotePassword: (NSString*) remotePassword
        withEvents: (BOOL) events
      withCallback: (RCTResponseSenderBlock) onEnd
{
    
    if (listener == nil) {
        NSLog(@"Listener has not been initialised.");
        exit(-1);
    }
    
    // Create the database.
    NSError *err;
    CBLDatabase* database = [manager databaseNamed:databaseLocal error:&err];
    
    if (!database) {
        NSLog(@"%@", err);
        exit(-1);
    }
    
    
    // Establish the connection.
    NSURL *url = [NSURL URLWithString:remoteUrl];
    id<CBLAuthenticator> auth = [CBLAuthenticator
                                 basicAuthenticatorWithName:remoteUser
                                 password:remotePassword];
    
    CBLReplication* push = [database createPushReplication: url];
    CBLReplication* pull = [database createPullReplication: url];
    
    push.continuous = YES;
    pull.continuous = YES;
    
    push.authenticator = auth;
    pull.authenticator = auth;

    if (timeout > 0) {
        push.customProperties = [CBLJSONDict dictionaryWithDictionary: @{
                                                                         @"poll": [NSNumber numberWithInteger:timeout],
                                                                         @"websocket": @false
                                                                         }];
        
        pull.customProperties = [CBLJSONDict dictionaryWithDictionary: @{
                                                                         @"poll": [NSNumber numberWithInteger:timeout],
                                                                         @"websocket": @false
                                                                         }];
    }
    // Add the events handler.
    if (events) {
        
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleReplicationEvent:) name:kCBLReplicationChangeNotification object:pull];
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleReplicationEvent:) name:kCBLReplicationChangeNotification object:push]; 
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleDatabaseEvent:) name:kCBLDatabaseChangeNotification object:nil];
    }
    
    [push start];
    [pull start];
    
    [databases setObject:database forKey:databaseLocal];
    
    [pushes setObject:push forKey:databaseLocal];
    [pulls  setObject:pull forKey:databaseLocal];
    
    // Callback handler
    if (onEnd != nil) {
        NSArray *cb = @[];
        onEnd(@[[NSNull null], cb]);
    }

}

- (void) handleDatabaseEvent: (NSNotification*) notification
{
    CBLDatabase* database = notification.object;
    NSArray* changes = notification.userInfo[@"changes"];
    
    for (CBLDatabaseChange* change in changes) {
        NSDictionary* map = @{
                              @"databaseName": database.name,
                              @"id": change.documentID
                              };
        [self.bridge.eventDispatcher sendAppEventWithName:DB_CHANGED body:map];
    }
}

- (void) handleReplicationEvent: (NSNotification*) notification
{
    CBLReplication* repl = notification.object;
    NSString* nameEvent = repl.pull? PULL : PUSH;
    if (repl.status == kCBLReplicationActive ||
        (repl.completedChangesCount > 0 && repl.completedChangesCount == repl.changesCount))
    {
        // NSLog(@"Replication is active");
        NSDictionary* map = @{
                              @"databaseName": repl.localDatabase.name,
                              @"changesCount": [NSString stringWithFormat:@"%u", repl.completedChangesCount],
                              @"totalChanges": [NSString stringWithFormat:@"%u", repl.changesCount]
                              };
        [self.bridge.eventDispatcher sendAppEventWithName:nameEvent body:map];
    } else {
        NSError *error = repl.lastError;
        if (error != nil && error.code == 401) {
            NSDictionary* mapError = @{};
            [self.bridge.eventDispatcher sendAppEventWithName:AUTH_ERROR body:mapError];
        }
    }
}

RCT_EXPORT_METHOD(serverLocal: (int) listenPort
                  withUserLocal: (NSString*) userLocal
                  withPasswordLocal: (NSString*) passwordLocal
                  withCallback: (RCTResponseSenderBlock) onEnd)
{
    // Init server.
    [self startServer: listenPort
             withUser: userLocal
         withPassword: passwordLocal
         withCallback: onEnd];
}


RCT_EXPORT_METHOD(serverLocalRemote: (int) listenPort
                  withUserLocal: (NSString*) userLocal
                  withPasswordLocal: (NSString*) passwordLocal
                  withDatabaseLocal: (NSString*) databaseLocal
                  withRemoteUrl: (NSString*) remoteUrl
                  withRemoteUser: (NSString*) remoteUser
                  withRemotePassword: (NSString*) remotePassword
                  withEvents: (BOOL) events
                  withCallback: (RCTResponseSenderBlock) onEnd)
{
    // Init the server.
    [self startServer:listenPort
             withUser:userLocal
         withPassword:passwordLocal
         withCallback:onEnd];
    
    // Init sync.
    [self startSync:databaseLocal
      withRemoteUrl:remoteUrl
     withRemoteUser:remoteUser
 withRemotePassword:remotePassword
         withEvents:events
       withCallback:nil];
}




RCT_EXPORT_METHOD(serverRemote: (NSString*) databaseLocal
                  withRemoteUrl: (NSString*) remoteUrl
                  withRemoteUser: (NSString*) remoteUser
                  withRemotePassword: (NSString*) remotePassword
                  withEvents: (BOOL) events
                  withCallback: (RCTResponseSenderBlock) onEnd)
{
    // Init sync.
    [self startSync:databaseLocal
      withRemoteUrl:remoteUrl
     withRemoteUser:remoteUser
 withRemotePassword:remotePassword
         withEvents:events
       withCallback:onEnd];
}

RCT_EXPORT_METHOD(compact: (NSString*) databaseLocal)
{
    
    NSError* error;
    CBLDatabase* database = [databases objectForKey:databaseLocal];
    if (database == nil) {
        NSLog(@"Database does not exist.");
        exit(-1);
    }
    
    BOOL success = [database compact:&error];
    if (!success) {
        NSLog(@"%@", error);
        exit(-1);
    }
}

RCT_EXPORT_METHOD(setTimeout: (NSInteger) newtimeout)
{
    timeout = newtimeout;
}

RCT_EXPORT_METHOD(closeDatabase: (NSString*) databaseName withCallback: (RCTResponseSenderBlock) onEnd)
{
    NSError *err;
    //Close object so we can destroy database through REST API
    CBLDatabase *database = [databases objectForKey: databaseName];
    if (database != nil) {
        [database close: &err];
    }
    // Callback handler
    if (onEnd != nil) {
        NSArray *cb = @[];
        onEnd(@[[NSNull null], cb]);
    }
}

@end
