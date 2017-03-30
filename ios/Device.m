#import "Device.h"

@implementation Device

@synthesize ip;
@synthesize mac;
@synthesize hostname;

-(Device*) init {
    ip = @"";
    mac = @"";
    hostname = @"";
    
    return self;
}

-(NSDictionary*)asDictionary {
    return @{@"ip": self.ip, @"mac": self.mac, @"hostname": self.hostname};
}

@end
