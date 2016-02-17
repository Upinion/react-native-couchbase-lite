//
//  RCTCouchBase.h
//  RCTCouchBase
//
//  Created by Alberto Martinez de Murga on 17/02/2016.
//  Copyright © 2016 Upinion. All rights reserved.
//

#import "RCTBridgeModule.h"
#import <CouchbaseLite/CBLManager.h>
#import <CouchbaseLite/CBLReplication.h>
#import <CouchbaseLiteListener/CBLListener.h>

@interface RCTCouchBase : NSObject <RCTBridgeModule>
{
    CBLManager *manager;
    CBLListener *listener;
    CBLReplication *push;
    CBLReplication *pull;
}

@end