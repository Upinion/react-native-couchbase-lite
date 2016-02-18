//
//  RCTCouchBase.m
//  RCTCouchBase
//
//  Created by Alberto Martinez de Murga on 16/02/2016.
//  Copyright © 2016 Upinion. All rights reserved.
//

#import "RCTCouchBase.h"

@implementation RCTCouchBase

RCT_EXPORT_MODULE("CouchBase")

NSString* const PUSH = @"couchBasePushEvent";
NSString* const PULL = @"couchBasePullEvent";
NSString* const DB_CHANGED = @"couchBaseDBEvent";

- (id)init
{
    self = [super init];
    
    if (self) {
        CBLRegisterJSViewCompiler();
        manager = [CBLManager sharedInstance];
        databases = [[NSMutableDictionary alloc] init];
        pulls = [[NSMutableDictionary alloc] init];
        pushes = [[NSMutableDictionary alloc] init];
        
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
             @"DBChanged": DB_CHANGED
             };
}


- (void) startServer: (int) port
            withUser: (NSString *) user
        withPassword: (NSString * ) pass
        withCallback: (RCTResponseSenderBlock) onEnd
{
    
    // Set up the listener.
    NSDictionary *auth = @{user:pass};
    listener = [[CBLListener alloc] initWithManager:manager port:port];
    [listener setPasswords: auth];
    
    // Init the listener.
    @try {
        NSError *err = nil;
        BOOL success = [listener start: &err];
        
        // Error handler
        if (success) {
            NSLog(@"CouchBase running on %@", listener.URL);
            
            // Callback handler
            if (onEnd != nil) {
                NSArray *cb = @[listener.URL];
                onEnd(@[[NSNull null], cb]);
            }
            
        } else {
            NSLog(@"%@", err);
            exit(-1);
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
    [databases setObject:database forKey:databaseLocal];
    
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
    
    // Add the events handler.
    if (events) {
        
        [[NSNotificationCenter defaultCenter]
         addObserverForName:kCBLDatabaseChangeNotification
         object:pull
         queue:nil
         usingBlock:^(NSNotification* n) {
             if (pull.status == kCBLReplicationActive &&
                 pull.completedChangesCount == pull.changesCount) {
                 [self.bridge.eventDispatcher sendAppEventWithName:PULL body:@{}];
             }
         }];
        
        [[NSNotificationCenter defaultCenter]
         addObserverForName:kCBLDatabaseChangeNotification
         object:push
         queue:nil
         usingBlock:^(NSNotification* n) {
             if (push.status == kCBLReplicationActive &&
                 push.completedChangesCount == push.changesCount) {
                 [self.bridge.eventDispatcher sendAppEventWithName:PUSH body:@{}];
             }
         }];
        
        
        [[NSNotificationCenter defaultCenter]
         addObserverForName:kCBLDatabaseChangeNotification
         object:database
         queue:nil
         usingBlock:^(NSNotification* n) {
             NSArray* changes = n.userInfo[@"changes"];
             
             for (CBLDatabaseChange* change in changes) {
                 NSDictionary* map = @{
                                       @"databaseName": database.name,
                                       @"id": change.documentID
                                       };
                 [self.bridge.eventDispatcher sendAppEventWithName:DB_CHANGED body:map];
             }
         }];
    }
    
    [push start];
    [pull start];
    
    [pushes setObject:push forKey:databaseLocal];
    [pulls  setObject:pull forKey:databaseLocal];
    
    // Callback handler
    if (onEnd != nil) {
        NSArray *cb = @[];
        onEnd(@[[NSNull null], cb]);
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

@end
