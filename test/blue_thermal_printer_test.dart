import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:blue_thermal_printer/blue_thermal_printer.dart';

void main() {
  const MethodChannel channel = MethodChannel('blue_thermal_printer');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    //expect(await BlueThermalPrinter.platformVersion, '42');
  });
}
