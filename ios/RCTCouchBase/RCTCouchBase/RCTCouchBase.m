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
NSString* const NOT_FOUND = @"couchBaseNotFound";
NSString* const OFFLINE_KEY = @"couchBaseOffline";
NSString* const ONLINE_KEY = @"couchBaseOnline";

- (id)init
{
    self = [super init];
    
    if (self) {
        CBLRegisterJSViewCompiler();
        manager = [[CBLManager alloc] init];
        databases = [[NSMutableDictionary alloc] init];
        pulls = [[NSMutableDictionary alloc] init];
        pushes = [[NSMutableDictionary alloc] init];
        timeout = 0;
        if (!manager) {
            NSLog(@"Cannot create Manager instance");
            return self;
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
             @"AuthError": AUTH_ERROR,
             @"NotFound": NOT_FOUND,
             @"Offline": OFFLINE_KEY,
             @"Online": ONLINE_KEY
             };
}

- (void) startServer: (int) port
            withUser: (NSString *) user
        withPassword: (NSString * ) pass
        withCallback: (RCTResponseSenderBlock) onEnd
{
    
    
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
            [self startServer:port+1 withUser:user withPassword:pass withCallback:onEnd];
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
    CBLDatabase* db = [manager existingDatabaseNamed:databaseLocal error:nil];
    if (!db) {
        NSLog(@"Database %@: could not be found", databaseLocal);
    } else {
        // Establish the connection.
        NSURL *url = [NSURL URLWithString:remoteUrl];
        id<CBLAuthenticator> auth = [CBLAuthenticator
                                     basicAuthenticatorWithName:remoteUser
                                     password:remotePassword];
        CBLReplication* push = [db createPushReplication: url];
        CBLReplication* pull = [db createPullReplication: url];
        
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
        [pushes setObject:push forKey:databaseLocal];
        [pulls  setObject:pull forKey:databaseLocal];
        // Add the events handler.
        if (events) {
            [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleReplicationEvent:) name:kCBLReplicationChangeNotification object:push];
            [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleReplicationEvent:) name:kCBLReplicationChangeNotification object:pull];
            [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleDatabaseEvent:) name:kCBLDatabaseChangeNotification object:nil];
        }
        
        [push start];
        [pull start];
    }
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
    if (repl.status == kCBLReplicationOffline) {
        NSDictionary* mapError = @{
                                   @"databaseName": repl.localDatabase.name,
                                   };
        [self.bridge.eventDispatcher sendAppEventWithName:OFFLINE_KEY body:mapError];
    } else {
        NSDictionary* mapSuccess = @{
                                   @"databaseName": repl.localDatabase.name,
                                   };
        [self.bridge.eventDispatcher sendAppEventWithName:ONLINE_KEY body:mapSuccess];
    }
    if (repl.status == kCBLReplicationActive ||
        (repl.completedChangesCount > 0 && repl.completedChangesCount == repl.changesCount))
    {
        NSDictionary* map = @{
                              @"databaseName": repl.localDatabase.name,
                              @"changesCount": [NSString stringWithFormat:@"%u", repl.completedChangesCount],
                              @"totalChanges": [NSString stringWithFormat:@"%u", repl.changesCount]
                              };
        [self.bridge.eventDispatcher sendAppEventWithName:nameEvent body:map];
    }
    NSError *error = repl.lastError;
    if (error != nil && error.code == 401) {
        NSDictionary* mapError = @{
                                   @"databaseName": repl.localDatabase.name,
                                   };
        [self.bridge.eventDispatcher sendAppEventWithName:AUTH_ERROR body:mapError];
    } else if (error != nil && error.code == 404) {
        NSDictionary* mapError = @{
                                   @"databaseName": repl.localDatabase.name,
                                   };
        [self.bridge.eventDispatcher sendAppEventWithName:NOT_FOUND body:mapError];
    }
}

RCT_EXPORT_METHOD(serverManager: (RCTResponseSenderBlock) onEnd)
{
    if (!manager) {
        NSLog(@"Couchbase manager does not exist.");
    }
    // Callback handler
    if (onEnd != nil) {
        onEnd(@{});
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
    
    [manager doAsync:^(void) {
        NSError* err;
        CBLDatabase* database = [manager databaseNamed:databaseLocal error:&err];
        if (!database) {
            NSLog(@"Database %@: could not be created. %@", databaseLocal, err);
        } else {
            [databases setObject:database forKey:databaseLocal];
        }
        // Init sync.
        [self startSync:databaseLocal
              withRemoteUrl:remoteUrl
             withRemoteUser:remoteUser
         withRemotePassword:remotePassword
                 withEvents:events
               withCallback:nil];
    }];
}




RCT_EXPORT_METHOD(serverRemote: (NSString*) databaseLocal
                  withRemoteUrl: (NSString*) remoteUrl
                  withRemoteUser: (NSString*) remoteUser
                  withRemotePassword: (NSString*) remotePassword
                  withEvents: (BOOL) events
                  withCallback: (RCTResponseSenderBlock) onEnd)
{
    [manager doAsync:^(void) {
        // Init sync.
        [self startSync:databaseLocal
              withRemoteUrl:remoteUrl
             withRemoteUser:remoteUser
         withRemotePassword:remotePassword
                 withEvents:events
               withCallback:onEnd];
    }];
}

RCT_EXPORT_METHOD(compact: (NSString*) databaseLocal)
{
    NSError* err;
    if (![manager databaseExistsNamed: databaseLocal]) {
        NSLog(@"Database %@: could not be found", databaseLocal);
    } else {
        [manager doAsync:^(void) {
            NSError* err;
            CBLDatabase* database = [manager existingDatabaseNamed:databaseLocal error:&err];
            bool compact = [database compact:&err];
            if (!compact) {
                NSLog(@"Database %@: could not compact. %@", databaseLocal, err);
            }
        }];
    }
}

RCT_EXPORT_METHOD(setTimeout: (NSInteger) newtimeout)
{
    timeout = newtimeout;
}

RCT_EXPORT_METHOD(createDatabase: (NSString*) databaseName
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [manager doAsync:^(void) {
        NSError* err;
        CBLDatabase* database = [manager databaseNamed:databaseName error:&err];
        if (!database) {
            reject(@"not_opened", [NSString stringWithFormat:@"Database %@: could not be created", databaseName], err);
            return;
        } else {
            [databases setObject:database forKey:databaseName];
            resolve(@{});
        }
    }];
}

RCT_EXPORT_METHOD(destroyDatabase: (NSString*) databaseName
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    __block NSError* err;
    if (![manager databaseExistsNamed:databaseName]) {
        reject(@"not_opened", [NSString stringWithFormat:@"Database %@: could not be opened", databaseName], nil);
        return;
    } else {
        [databases removeObjectForKey:databaseName];
        [pushes removeObjectForKey:databaseName];
        [pulls removeObjectForKey:databaseName];
        
        [manager doAsync:^(void) {
            CBLDatabase* database = [manager existingDatabaseNamed:databaseName error:nil];
            bool deleted = [database deleteDatabase:&err];
            if (!deleted) {
                reject(@"not_opened", [NSString stringWithFormat:@"Database %@: could not be destroyed", databaseName], err);
                return;
            } else {
                resolve(@{});
            }
        }];
    }
}

RCT_EXPORT_METHOD(closeDatabase: (NSString*) databaseName withCallback: (RCTResponseSenderBlock) onEnd)
{
    __block NSError* err;
    if (![manager databaseExistsNamed:databaseName]) {
        NSLog(@"Database %@: could not be found", databaseName);
        // Callback handler
        if (onEnd != nil) {
            NSArray *cb = @[];
            onEnd(@[[NSNull null], cb]);
        }
    } else {
        [manager doAsync:^(void) {
            CBLDatabase* database = [manager existingDatabaseNamed:databaseName error:nil];
            bool closed = [database close:&err];
            if (!closed) {
                NSLog(@"Database %@: could not be destroyed. %@", databaseName, err);
            }
            // Callback handler
            if (onEnd != nil) {
                NSArray *cb = @[];
                onEnd(@[[NSNull null], cb]);
            }
        }];
        
    }
}

RCT_EXPORT_METHOD(putDocument: (NSString*) db
                  withId:(NSString*) docId
                  withObject:(NSDictionary*) dict
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (![manager databaseExistsNamed: db]) {
        reject(@"not_opened", [NSString stringWithFormat:@"Database %@: could not be opened", db], nil);
        return;
    }
    [manager doAsync:^(void) {
        NSError* err;
        
        CBLDatabase* database = [manager existingDatabaseNamed:db error:&err];
        // We need to check if it is a _local document or a normal document.
        NSRegularExpression* regex = [NSRegularExpression regularExpressionWithPattern:@"^_local\/(.+)"
                                                                               options:0
                                                                                 error:&err];
        NSTextCheckingResult* match = [regex firstMatchInString:docId
                                                        options:0
                                                          range:NSMakeRange(0, [docId length])];
        if (match) {
            NSString* localDocId = [docId substringWithRange:[match rangeAtIndex:1]];
            bool success = [database putLocalDocument:dict withID:localDocId error:&err];
            if (success) {
                resolve(@{});
            } else {
                reject(@"missing_document", [NSString stringWithFormat:@"could not create/update document: %@", docId], nil);
            }
        } else {
            CBLDocument* doc = [database documentWithID:docId];
            if (doc != nil) {
                CBLSavedRevision* revision = [doc putProperties:dict error:&err];
                if (revision) {
                    resolve(@{});
                } else {
                    reject(@"missing_document", [NSString stringWithFormat:@"could not create/update document: %@", docId], nil);
                }
            } else {
                reject(@"missing_document", [NSString stringWithFormat:@"could not create/update document: %@", docId], nil);
            }
        }
    }];
}

RCT_EXPORT_METHOD(getDocument: (NSString*) db
                  withId:(NSString*) docId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (![manager databaseExistsNamed: db]) {
        reject(@"not_opened", [NSString stringWithFormat:@"Database %@: could not be opened", db], nil);
        return;
    }
    [manager doAsync:^(void) {
        NSError *err;
        
        CBLDatabase* database = [manager existingDatabaseNamed:db error:&err];
        // We need to check if it is a _local document or a normal document.
        NSRegularExpression* regex = [NSRegularExpression regularExpressionWithPattern:@"^_local\/(.+)"
                                                                               options:0
                                                                                 error:&err];
        NSTextCheckingResult* match = [regex firstMatchInString:docId
                                                        options:0
                                                          range:NSMakeRange(0, [docId length])];
        
        if (match) {
            NSString* localDocId = [docId substringWithRange:[match rangeAtIndex:1]];
            CBLJSONDict* doc = [database existingLocalDocumentWithID:localDocId];
            if (doc != nil) {
                resolve(doc);
            } else {
                reject(@"not_opened", [NSString stringWithFormat:@"document not found %@", docId], nil);
            }
        } else {
            CBLDocument* doc = [database existingDocumentWithID:docId];
            if (doc != nil && doc.properties != nil) {
                resolve(doc.properties);
            } else {
                reject(@"not_opened", [NSString stringWithFormat:@"document not found %@", docId], nil);
            }
        }
    }];
}


RCT_EXPORT_METHOD(getAllDocuments: (NSString*) db
                  withIds:(nullable NSArray*) ids
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (![manager databaseExistsNamed: db]) {
        reject(@"not_opened", [NSString stringWithFormat:@"Database %@: could not be opened", db], nil);
        return;
    }
    [manager doAsync:^(void) {
        NSError* err;
        
        CBLDatabase* database = [manager existingDatabaseNamed:db error:&err];
        NSMutableArray* results = [[NSMutableArray alloc] init];
        if (ids == NULL || [ids count] == 0) {
            CBLQuery* query = [database createAllDocumentsQuery];
            query.allDocsMode = kCBLAllDocs;
            
            CBLQueryEnumerator* qResults = [query run:&err];
            
            if (qResults == nil) {
                reject(@"query_failed", @"The query could not be completed", err);
                return;
            }
            
            for (CBLQueryRow* row in qResults) {
                if (row.document.properties != nil) {
                    [results addObject:@{@"doc": row.document.properties,
                                         @"_id": row.document.documentID,
                                         @"key": row.document.documentID,
                                         @"value": @{@"rev": row.document.currentRevisionID}
                                         }];
                }
            }
        } else {
            
            for(NSString* docId in ids) {
                // We need to check if it is a _local document or a normal document.
                NSRegularExpression* regex = [NSRegularExpression regularExpressionWithPattern:@"^_local\/(.+)"
                                                                                       options:0
                                                                                         error:&err];
                NSTextCheckingResult* match = [regex firstMatchInString:docId
                                                                options:0
                                                                  range:NSMakeRange(0, [docId length])];
                
                if (match) {
                    NSString* localDocId = [docId substringWithRange:[match rangeAtIndex:1]];
                    CBLJSONDict* doc = [database existingLocalDocumentWithID:localDocId];
                    if (doc != nil) {
                        [results addObject: @{@"doc": doc,
                                              @"_id": localDocId,
                                              @"key": localDocId
                                              }];
                    }
                } else {
                    CBLDocument* doc = [database existingDocumentWithID:docId];
                    if (doc != nil && doc.properties != nil) {
                        NSMutableDictionary* values = [NSMutableDictionary dictionaryWithObjects: [doc.properties allValues] forKeys:[doc.properties allKeys]];
                        [values setValue: doc.documentID forKey:@"_id"];
                        [results addObject: @{@"doc": values,
                                              @"_id": doc.documentID,
                                              @"key": doc.documentID,
                                              @"value": @{@"rev": doc.currentRevisionID}
                                              }];
                    }
                }
            }
        }
        resolve(@{
                  @"rows": results,
                  @"total_rows": [NSNumber numberWithUnsignedInteger: results.count]
                  });
    }];
}


RCT_EXPORT_METHOD(getView: (NSString*) db
                  withDesign: (NSString*) design
                  withView: (NSString*) viewName
                  withParams: (NSDictionary*) params
                  withKeys: (NSArray*) keys
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if (![manager databaseExistsNamed: db]) {
        reject(@"not_opened", [NSString stringWithFormat:@"Database %@: could not be opened", db], nil);
        return;
    }
    [manager doAsync:^(void) {
        NSError* err;
        
        CBLDatabase* database = [manager existingDatabaseNamed:db error:&err];
        CBLView* view = [database existingViewNamed:viewName];
        if (view == nil || (view && [view mapBlock] == nil)) {
            view = [database viewNamed:viewName];
            
            CBLDocument* viewsDoc = [database existingDocumentWithID:[NSString stringWithFormat:@"_design/%@", design]];
            if (viewsDoc == nil || viewsDoc.properties == nil || [viewsDoc.properties objectForKey:@"views"] == nil) {
                reject(@"not_found", [NSString stringWithFormat:@"Database %@: design file could not be opened", db], nil);
                return;
            }
            
            NSDictionary* views = [viewsDoc.properties objectForKey:@"views"];
            if ([views objectForKey:viewName] == nil || [[views objectForKey:viewName] objectForKey:@"map"] == nil) {
                reject(@"not_found", [NSString stringWithFormat:@"Database %@: view not found", db], nil);
                return;
            }
            
            NSDictionary* viewDefinition = [views objectForKey:viewName];
            CBLMapBlock mapBlock = [[CBLView compiler]compileMapFunction:[viewDefinition objectForKey:@"map"] language:@"javascript"];
            NSString* version = [viewDefinition objectForKey:@"version"];
            
            if (mapBlock == nil) {
                reject(@"invalid_map", @"Invalid map function", [NSNull null]);
                return;
            }
            
            if([viewDefinition objectForKey:@"reduce"] != nil) {
                CBLReduceBlock reduceBlock = [[CBLView compiler]compileReduceFunction:[viewDefinition objectForKey:@"reduce"] language:@"javascript"];
                if (reduceBlock == nil) {
                    reject(@"invalid_reduce", @"Invalid reduce function", [NSNull null]);
                    return;
                }
                [view setMapBlock:mapBlock reduceBlock:reduceBlock version: version != nil ? [NSString stringWithString: version] : @"1.1"];
            } else {
                [view setMapBlock:mapBlock version: version != nil ? [NSString stringWithString: version] : @"1.1"];
            }
        } else {
            [view updateIndex];
        }
        
        CBLQuery* query = [view createQuery];
        
        NSArray* paramKeys = [params allKeys];
        if ([paramKeys containsObject:@"startkey"]) query.startKeyDocID = [params objectForKey:@"startkey"];
        if ([paramKeys containsObject:@"endkey"]) query.endKeyDocID = [params objectForKey:@"endkey"];
        if ([paramKeys containsObject:@"descending"]) query.descending = [params objectForKey:@"descending"];
        if ([paramKeys containsObject:@"limit"]) query.limit = [params objectForKey:@"limit"];
        if ([paramKeys containsObject:@"skip"]) query.skip = [params objectForKey:@"skip"];
        if ([paramKeys containsObject:@"group"]) query.groupLevel = [params objectForKey:@"group"];
        if (keys != nil && [keys count] > 0) query.keys = keys;
        
        CBLQueryEnumerator* qResults = [query run: &err];
        if (err != nil) {
            reject(@"query_error", @"The query failed", err);
            return;
        }
        
        NSMutableArray* results = [[NSMutableArray alloc] init];
        for (CBLQueryRow* row in qResults) {
            if (row.document.properties != nil) {
                NSMutableDictionary* values = [NSMutableDictionary dictionaryWithObjects: [row.document.properties allValues] forKeys:[row.document.properties allKeys]];
                [values setValue: row.document.documentID forKey:@"_id"];
                [results addObject: @{@"value": values,
                                      @"_id": row.document.documentID,
                                      @"key": row.document.documentID,
                                      }];
            // The reduced views have a different format.
            } else if([view reduceBlock] != nil) {
                NSDictionary* resultEntry = @{@"key": row.key? row.key : nil, @"value": row.value};
                [results addObject:resultEntry];
            }
        }

        if ([paramKeys containsObject:@"update_seq"] && [params objectForKey:@"update_seq"] == @YES) {
            resolve(@{@"rows": results,
                      @"offset": [NSNumber numberWithUnsignedInteger: query.skip ? query.skip : 0],
                      @"total_rows": [NSNumber numberWithLongLong: view.totalRows],
                      @"update_seq": [NSNumber numberWithLongLong:qResults.sequenceNumber]
                      });
        } else {
            resolve(@{@"rows": results,
                      @"offset": [NSNumber numberWithUnsignedInteger: query.skip ? query.skip : 0],
                      @"total_rows": [NSNumber numberWithLongLong: view.totalRows]
                      });
        }
    }];
}


@end
