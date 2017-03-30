#import "NetworkScanCallbacks.h"

@interface NetworkScanImpl : NSObject

-(void)scan:(unsigned int)firstIp lastIp:(unsigned int)lastIp callbacks:(id<NetworkScanCallbacks>)_callbacks;

@end
