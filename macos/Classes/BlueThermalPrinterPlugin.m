#import "BlueThermalPrinterPlugin.h"
#import <blue_thermal_printer/blue_thermal_printer-Swift.h>

@implementation BlueThermalPrinterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftBlueThermalPrinterPlugin registerWithRegistrar:registrar];
}
@end
