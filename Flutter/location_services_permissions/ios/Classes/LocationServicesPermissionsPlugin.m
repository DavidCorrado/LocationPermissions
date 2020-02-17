#import "LocationServicesPermissionsPlugin.h"
#if __has_include(<location_services_permissions/location_services_permissions-Swift.h>)
#import <location_services_permissions/location_services_permissions-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "location_services_permissions-Swift.h"
#endif

@implementation LocationServicesPermissionsPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftLocationServicesPermissionsPlugin registerWithRegistrar:registrar];
}
@end
