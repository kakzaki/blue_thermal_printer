#import "BlueThermalPrinterPlugin.h"

static NSString *const CHANNEL_NAME = @"plugins.kzaki.com/blue_thermal_printer";


@implementation BlueThermalPrinterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    FlutterMethodChannel* channel = [FlutterMethodChannel
                                     methodChannelWithName:CHANNEL_NAME
                                     binaryMessenger:[registrar messenger]];
    BlueThermalPrinterPlugin* instance = [[BlueThermalPrinterPlugin alloc] init];
    [registrar addMethodCallDelegate:instance channel:channel];
}

@end

