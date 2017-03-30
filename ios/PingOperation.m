#import <Foundation/Foundation.h>
#import "PingOperation.h"
#import "Pinger.h"

// Adjusted from http://codereview.stackexchange.com/q/36632

@implementation PingOperation {
    BOOL _isExecuting;
    BOOL _isFinished;
    
    unsigned int _ip;
    Pinger *_pinger;
    bool _result;
    completion_block_t _completionHandler;
    id _self;  // immortality
    
    NSString *pingedAddress;
}

- (id)initWithIp:(unsigned int)ip completion:(completion_block_t)completionHandler
{
    self = [super init];
    if (self) {
        _ip = ip;
        _completionHandler = [completionHandler copy];
    }
    return self;
}

- (NSString*) address {
    return pingedAddress;
}

- (bool) result {
    return _result;
}

- (void) start
{
    if (!self.isCancelled && !_isFinished && !_isExecuting) {
        self.isExecuting = YES;
        _self = self; // make self immortal for the duration of the task
 
        [self doPing];
    }
}

- (void) main
{
    [self doPing];
}

- (void) doPing
{
    pingedAddress = [NSString stringWithFormat:@"%d.%d.%d.%d",
                    (_ip >> 24) & 0xFF, (_ip >> 16) & 0xFF, (_ip >> 8) & 0xFF, _ip & 0xFF];
    
    _pinger = [Pinger alloc];
    [_pinger ping:pingedAddress callback:^void(bool success) {
        //NSLog(@"Callback - %d", success);
        // Set result and terminate
        _result = success;
        [self terminate];
    }];
    
    do {
        [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate distantFuture]];
    } while (_self != nil);
}

- (void) terminate {
    completion_block_t completionHandler = _completionHandler;
    _completionHandler = nil;
    bool result = _result;
    //NSLog(@"terminating");
    _self = nil;
    if (completionHandler) {
        //dispatch_async(dispatch_get_global_queue(0, 0), ^{
            completionHandler(result);
        //});
    }
    self.isExecuting = NO;
    self.isFinished = YES;
}

- (BOOL) isConcurrent {
    return YES;
}

- (BOOL) isAsynchronous {
    return NO;
}

- (BOOL) isExecuting {
    return _isExecuting;
}
- (void) setIsExecuting:(BOOL)isExecuting {
    if (_isExecuting != isExecuting) {
        [self willChangeValueForKey:@"isExecuting"];
        _isExecuting = isExecuting;
        [self didChangeValueForKey:@"isExecuting"];
    }
}

- (BOOL) isFinished {
    return _isFinished;
}
- (void) setIsFinished:(BOOL)isFinished {
    if (_isFinished != isFinished) {
        [self willChangeValueForKey:@"isFinished"];
        _isFinished = isFinished;
        [self didChangeValueForKey:@"isFinished"];
    }
}

- (void) cancel {
    [super cancel];
    _result = false;
        /*[[NSError alloc] initWithDomain:@"MyOperation"
                                                 code:-1000
                                             userInfo:@{NSLocalizedDescriptionKey: @"cancelled"}];*/
}

@end
