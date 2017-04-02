#import "Modules/RCTEventEmitter.h"
#import "NetworkScanCallbacks.h"

@interface NetworkScanner : RCTEventEmitter <NetworkScanCallbacks, RCTBridgeModule>

- (void)scan:(RCTResponseSenderBlock)onScanFinished onError:(RCTResponseSenderBlock)onError;
- (NSArray<NSString *>*)supportedEvents;

@end
