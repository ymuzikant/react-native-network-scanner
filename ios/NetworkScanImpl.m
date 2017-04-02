#import <Foundation/Foundation.h>
#import "NetworkUtils.h"
#import "NetworkScanImpl.h"
#import "PingOperation.h"
#import "MacFinder.h"
#import "NetworkScanCallbacks.h"

@implementation NetworkScanImpl {
    unsigned int totalIps;
    unsigned int devicesScanned;
    NSMutableDictionary *devicesByIp;
    NSMutableArray *devices;
    NSOperationQueue *queue;
    id<NetworkScanCallbacks> callbacks;
    id _self; // to prevent cleanup
    bool allQueued;
}

-(void)scan:(unsigned int)firstIp lastIp:(unsigned int)lastIp callbacks:(id<NetworkScanCallbacks>)_callbacks {
    
    _self = self;
    devicesScanned = 0;
    totalIps = (lastIp - firstIp) + 1;
    callbacks = _callbacks;
    
    devicesByIp = [[NSMutableDictionary alloc]initWithCapacity:15];
    devices = [[NSMutableArray alloc]initWithCapacity:15];
    
    queue = [[NSOperationQueue alloc] init];
    [queue setMaxConcurrentOperationCount:10];
    [queue addObserver:self forKeyPath:@"operationCount" options:0 context:NULL];
    
    allQueued = false;
    for (unsigned int ip = firstIp; ip <= lastIp; ip ++) {
        NSLog(@"Pinging %08X", ip);
        PingOperation *pingOperation = [PingOperation alloc];
        [queue addOperation:[pingOperation initWithIp:ip completion:^void(bool success) {
            @synchronized(self) {
                devicesScanned ++;
                [callbacks onProgress:devicesScanned total:totalIps];
            }
            if (success)
            {
                NSString *address = [pingOperation address];
                NSString *resolvedName = [NetworkUtils findHostnameForIP:address];
                resolvedName = (resolvedName != nil) ? resolvedName : address;
                
                Device *device = [[Device alloc] init];
                [device setIp:address];
                [device setHostname:resolvedName];
                [self->devicesByIp setObject:device forKey:address];
                [callbacks onDeviceFound:device];
            }
        }]];
    }
    allQueued = true;
    
    NSLog(@"scan - exiting");
}

- (void) observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object
                         change:(NSDictionary *)change context:(void *)context
{
    if (object == self->queue && [keyPath isEqualToString:@"operationCount"]) {
        if (self->allQueued && self->queue.operationCount == 0) {
            NSLog(@"queue has completed");
            [queue removeObserver:self forKeyPath:@"operationCount"];

            [devicesByIp enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
                NSLog(@"Found address %@", key);
                NSString *macAddress = [MacFinder ip2mac:key];
                if (macAddress != nil) {
                    Device *device = obj;
                    [device setMac:macAddress];
                    [self->devices addObject:[device asDictionary]];
                }
            }];
            
            [callbacks onScanCompleted:devices];
            
            _self = nil;
        }
        
    }
    else {
        [super observeValueForKeyPath:keyPath ofObject:object
                               change:change context:context];
    }
}

@end
