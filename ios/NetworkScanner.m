#import <Foundation/Foundation.h>
#import "NetworkScanner.h"
#import "NetworkScannerImpl.h"

@implementation NetworkScanner {
    RCTResponseSenderBlock onScanFinished;
    RCTResponseSenderBlock onError;
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(scan:(RCTResponseSenderBlock)_onScanFinished onError:(RCTResponseSenderBlock)_onError) {
    self->onScanFinished = _onScanFinished;
    self->onError = _onError;
    
    [NetworkScannerImpl scanSubnet:self];

}

-(void)onError:(NSError *)error {
    onError(@[error]);
}

-(void)onScanCompleted:(NSArray *)devices {
    onScanFinished(@[devices]);
}

-(void)onProgress:(int)progress total:(int)total {
    [self sendEventWithName:@"progress"
                       body:@{@"progress":[NSNumber numberWithInt:progress],
                              @"total": [NSNumber numberWithInt:total]}];
}

-(void)onDeviceFound:(Device *)device {
    [self sendEventWithName:@"deviceFound"
                       body:[device asDictionary]];
}

-(NSArray<NSString *>*)supportedEvents {
    return @[@"deviceFound", @"progress"];
}

@end

