#import <Foundation/Foundation.h>

@interface Device : NSObject

@property NSString *ip;
@property NSString *mac;
@property NSString *hostname;

-(NSDictionary*)asDictionary;

@end
