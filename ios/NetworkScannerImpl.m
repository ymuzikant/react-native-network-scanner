#import <Foundation/Foundation.h>
#import "NetworkUtils.h"
#import "NetworkScannerImpl.h"
#import "NetworkScanImpl.h"
#import "NetworkScanCallbacks.h"

@implementation NetworkScannerImpl

+ (void)scanSubnet:(id<NetworkScanCallbacks>)_callbacks {
    
    @try {
        AddressAndMask *addressAndMask = [NetworkUtils getIPAddressAndMask];
        
        unsigned int ip = [NetworkUtils convertIpAddress:[addressAndMask address]];
        unsigned int mask = [NetworkUtils convertIpAddress:[addressAndMask mask]];
        
        NSLog(@"IP = %08X, mask = %08X", ip, mask);

        unsigned int firstIpInSegment = ip & mask;
        unsigned int firstIp = firstIpInSegment + 1;
        unsigned int lastIp = firstIpInSegment + (~mask) - 1;
        
        NSLog(@"First IP = %08X, last IP = %08X", firstIp, lastIp);
        
        NetworkScanImpl *scan = [NetworkScanImpl alloc];
        [scan scan:firstIp lastIp:lastIp callbacks:_callbacks];
    } @catch (NSException *exception) {
        [_callbacks onError:
            [[NSError alloc] initWithDomain:@"Network Scanner" code:1
                                   userInfo:@{@"Name" : exception.name, @"Reason" : exception.reason}]];
    }
}

@end
