typedef void (^completion_block_t)(bool result);

@interface PingOperation : NSOperation

- (id)initWithIp:(unsigned int)ip completion:(completion_block_t)completionHandler;
- (void)start;
- (NSString*)address;

@end
