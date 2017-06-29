//
//  RCTCouchBase.h
//  RCTCouchBase
//
//  Created by Alberto Martinez de Murga on 17/02/2016.
//  Copyright © 2016 Upinion. All rights reserved.
//
#import <React/RCTEventEmitter.h>
#import <CouchbaseLite/CouchbaseLite.h>
#import <CouchbaseLiteListener/CBLListener.h>
#import "CBLRegisterJSViewCompiler.h"

@interface RCTCouchBase : RCTEventEmitter
{
    CBLManager *manager;
    CBLListener *listener;
    NSMutableDictionary* databases;
    NSMutableDictionary* pulls;
    NSMutableDictionary* pushes;
    NSInteger timeout;
}

@end
