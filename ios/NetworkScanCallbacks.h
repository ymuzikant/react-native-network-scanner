#import "Device.h"

@protocol NetworkScanCallbacks

-(void)onProgress:(int)progress total:(int)total;

-(void)onDeviceFound:(Device*)device;

-(void)onScanCompleted:(NSArray*)devices;

-(void)onError:(NSError*)error;

@end
