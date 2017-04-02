#import "Pinger.h"
#import "SimplePing.h"

// inspired by https://github.com/chrishulbert/SimplePingHelper

@implementation Pinger {
    void(^callback)(bool);
    SimplePing *simplePing;
    NSTimer *timer;
    NSString *address;
}

- (void)ping:(NSString*)_address callback:(void(^)(bool success))_callback {
    self->callback = _callback;
    self->address = _address;
    self->simplePing = [[SimplePing alloc] initWithHostName:address];
    self->simplePing.delegate = self;
    [self performSelector:@selector(timeout) withObject:nil afterDelay:1.0]; // This timeout is what retains the ping helper
    [simplePing start];
}

- (void)fail {
    @synchronized(self) {
        [self safeInvokeCallbackAndClean:false];
    }
}

- (void)success {
    @synchronized(self) {
        [self safeInvokeCallbackAndClean:true];
    }
}

- (void)safeInvokeCallbackAndClean:(bool)result {
    if (self->simplePing != nil) {
        if (self->callback != nil) {
            self->callback(result);
            self->callback = nil;
        }
        
        [self cleanup];
    }
}

- (void)cleanup {
    [self->simplePing stop];
    [self->timer invalidate];
    self->simplePing = nil;
}

- (void)timeout {
    if (self->simplePing != nil) {
        [self fail];
    }
}

- (void)simplePing:(SimplePing *)pinger didFailWithError:(NSError *)error {
    NSLog(@"didFailWithError for %@", address);
    [self fail];
}

- (void)simplePing:(SimplePing *)pinger didStartWithAddress:(NSData *)address {
    [pinger sendPingWithData:nil];
}

- (void)simplePing:(SimplePing *)pinger didFailToSendPacket:(NSData *)packet sequenceNumber:(uint16_t)sequenceNumber error:(NSError *)error {
    
    NSLog(@"didFailToSendPacket %@", address);
    [self fail];
}

- (void)simplePing:(SimplePing *)pinger didReceivePingResponsePacket:(NSData *)packet sequenceNumber:(uint16_t)sequenceNumber {
    [self->timer invalidate];
    [self success];
}

- (void)simplePing:(SimplePing *)pinger didReceiveUnexpectedPacket:(NSData *)packet {
    NSLog(@"didReceiveUnexpectedPacket %@", address);
    // keep waiting - maybe the right packet will come!
}

@end
