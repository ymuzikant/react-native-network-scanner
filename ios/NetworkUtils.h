#import <Foundation/Foundation.h>
#import "AddressAndMask.h"

@interface NetworkUtils : NSObject

+(NSString *)findHostnameForIP:(NSString*)ip;
+(AddressAndMask *)getIPAddressAndMask;
+(unsigned int)convertIpAddress:(NSString *)ipAddress;

@end
