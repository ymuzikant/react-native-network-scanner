#import "SimplePing.h"

@interface Pinger : NSObject<SimplePingDelegate>

- (void)ping:(NSString*)address callback:(void(^)(bool success))callback;

@end
