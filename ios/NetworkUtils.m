#import "NetworkUtils.h"
#import <netdb.h>
#import <arpa/inet.h>
#import <netinet/in.h>
#import <ifaddrs.h>

@implementation NetworkUtils

+(NSString *)findHostnameForIP:(NSString*)ip {
    struct addrinfo hints;
    memset(&hints, 0, sizeof(hints));
    hints.ai_flags    = AI_NUMERICHOST;
    hints.ai_family   = PF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = 0;
    
    struct addrinfo *addr = NULL;
    int errorStatus = getaddrinfo([ip cStringUsingEncoding:NSASCIIStringEncoding], NULL, &hints, &addr);
    if (errorStatus != 0) return nil;
    
    CFDataRef addressRef = CFDataCreate(NULL, (UInt8 *)addr->ai_addr, addr->ai_addrlen);
    freeaddrinfo(addr);
    if (addressRef == nil) return nil;
    
    CFHostRef hostRef = CFHostCreateWithAddress(kCFAllocatorDefault, addressRef);
    NSString *hostname = nil;
    if (hostRef) {
        if (CFHostStartInfoResolution(hostRef, kCFHostNames, NULL) == TRUE) {
            Boolean result;
            NSArray *addresses = CFBridgingRelease(CFHostGetNames(hostRef, &result)); // transfers ownership of hostRef to ARC
            if (result == TRUE) {
                hostname = addresses[0];
            }
        }
    }
    
    CFRelease(addressRef);
    
    NSLog(@"Resolved %@ to %@", ip, hostname);
    
    return hostname;
}

+ (unsigned int)convertIpAddress:(NSString *)ipAddress {
    struct sockaddr_in sin;
    inet_aton([ipAddress UTF8String], &sin.sin_addr);
    return ntohl(sin.sin_addr.s_addr);
}

+ (AddressAndMask *)getIPAddressAndMask {

    AddressAndMask *addressAndMask = [AddressAndMask alloc];
    [addressAndMask setAddress:@"error"];
    [addressAndMask setMask:@"error"];
    
    struct ifaddrs *interfaces = NULL;
    int success = getifaddrs(&interfaces);
    if (success == 0) {
        for (struct ifaddrs *temp_addr = interfaces; temp_addr != NULL; temp_addr = temp_addr->ifa_next) {
            if(temp_addr->ifa_addr->sa_family == AF_INET) {
                // Look for "en0" which is the wifi NIC
                if([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:@"en0"]) {
                    
                    [addressAndMask
                     setAddress:[NSString stringWithUTF8String:
                                 inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)]];
                    
                    [addressAndMask
                     setMask:[NSString stringWithUTF8String:
                              inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_netmask)->sin_addr)]];

                }
                
            }
        }
    }

    freeifaddrs(interfaces);
    
    return addressAndMask;
}

@end
