//
//  RCTCouchBase.h
//  RCTCouchBase
//
//  Created by Alberto Martinez de Murga on 17/02/2016.
//  Copyright © 2016 Upinion. All rights reserved.
//

#import "RCTBridgeModule.h"
#import "RCTEventDispatcher.h"
#import <CouchbaseLite/CouchbaseLite.h>
#import <CouchbaseLiteListener/CBLListener.h>
#import "CBLRegisterJSViewCompiler.h"

extern NSString* const PUSH;
extern NSString* const PULL;
extern NSString* const DB_CHANGED;

@interface RCTCouchBase : NSObject <RCTBridgeModule>
{
    CBLManager *manager;
    CBLListener *listener;
    NSMutableDictionary* databases;
    NSMutableDictionary* pulls;
    NSMutableDictionary* pushes;
}

@end