//
//  RCTCouchBase.m
//  RCTCouchBase
//
//  Created by Alberto Martinez de Murga on 16/02/2016.
//  Copyright © 2016 Upinion. All rights reserved.
//

#import "RCTCouchBase.h"
#import "CBLRegisterJSViewCompiler.h"

@implementation RCTCouchBase

+ (NSString*)getName
{
    return @"CouchBase";
}

- (id)init
{
    self = [super init];
    
    if (self) {
        CBLRegisterJSViewCompiler();
        manager = [CBLManager sharedInstance];
        
        if (!manager) {
            NSLog(@"Cannot create Manager instance");
            exit(-1);
        }
    }
    return self;
}



RCT_EXPORT_MODULE("CouchBase")


RCT_EXPORT_METHOD(serverLocal: (int) listenPort
                  withUserLocal: (NSString*) userLocal
                  withPasswordLocal: (NSString*) passwordLocal
                  withCallback: (RCTResponseSenderBlock) onEnd)
{
    
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
    NSLog(@"Called server local remote");
}




RCT_EXPORT_METHOD(serverRemote: (NSString*) databaseLocal
                  withRemoteUrl: (NSString*) remoteUrl
                  withRemoteUser: (NSString*) remoteUser
                  withRemotePassword: (NSString*) remotePassword
                  withEvents: (BOOL) events
                  withCallback: (RCTResponseSenderBlock) onEnd)
{
    NSLog(@"Called server remote");
}

RCT_EXPORT_METHOD(compact: (NSString*) databaseLocal)
{
    NSLog(@"Called compact");
}


@end
