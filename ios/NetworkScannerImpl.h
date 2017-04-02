#import "NetworkScanCallbacks.h"

@interface NetworkScannerImpl : NSObject

+ (void)scanSubnet:(id<NetworkScanCallbacks>)_callbacks;

@end
