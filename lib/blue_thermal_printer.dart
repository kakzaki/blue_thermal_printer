import 'dart:async';

import 'package:flutter/services.dart';

class BlueThermalPrinter {
  static const int stateOff = 10;
  static const int stateTurningOn = 11;
  static const int stateOn = 12;
  static const int stateTurningOff = 13;
  static const int stateBleTurningOn = 14;
  static const int stateBleOn = 15;
  static const int stateBleTurningOff = 16;
  static const int error = -1;
  static const int connected = 1;
  static const int disconnected = 0;
  static const int disconnectRequested = 2;

  static const String namespace = 'blue_thermal_printer';

  static const MethodChannel _channel = MethodChannel('$namespace/methods');

  static const EventChannel _readChannel = EventChannel('$namespace/read');

  static const EventChannel _stateChannel = EventChannel('$namespace/state');

  BlueThermalPrinter._() {
    _channel.setMethodCallHandler((MethodCall call) async {});
  }

  static final BlueThermalPrinter _instance = BlueThermalPrinter._();

  static BlueThermalPrinter get instance => _instance;

  Future<T?> _invokeMethod<T>(String method, [dynamic arguments]) {
    return _channel.invokeMethod<T>(method, arguments);
  }

  ///onStateChanged()
  Stream<int?> onStateChanged() async* {
    yield await _invokeMethod<int>('state');

    yield* _stateChannel
        .receiveBroadcastStream()
        .map((buffer) => buffer is int ? buffer : null);
  }

  ///onRead()
  Stream<String> onRead() =>
      _readChannel.receiveBroadcastStream().map((buffer) => buffer.toString());

  Future<bool?> get isAvailable => _invokeMethod<bool>('isAvailable');

  Future<bool?> get isOn => _invokeMethod<bool>('isOn');

  Future<bool?> get isConnected => _invokeMethod<bool>('isConnected');

  Future<bool?> get openSettings => _invokeMethod<bool>('openSettings');

  ///getBondedDevices()
  Future<List<BluetoothDevice>> getBondedDevices() async {
    final rawDevices =
        await _channel.invokeListMethod<dynamic>('getBondedDevices') ??
            <dynamic>[];

    return rawDevices
        .whereType<Map>()
        .map((item) => BluetoothDevice.fromMap(Map<String, dynamic>.from(item)))
        .toList();
  }

  ///isDeviceConnected(BluetoothDevice device)
  Future<bool?> isDeviceConnected(BluetoothDevice device) =>
      _invokeMethod<bool>('isDeviceConnected', device.toMap());

  ///connect(BluetoothDevice device)
  Future<dynamic> connect(BluetoothDevice device) =>
      _invokeMethod('connect', device.toMap());

  ///disconnect()
  Future<dynamic> disconnect() => _invokeMethod('disconnect');

  ///write(String message)
  Future<dynamic> write(String message) =>
      _invokeMethod('write', {'message': message});

  ///writeBytes(Uint8List message)
  Future<dynamic> writeBytes(Uint8List message) =>
      _invokeMethod('writeBytes', {'message': message});

  ///printCustom(String message, int size, int align,{String? charset})
  Future<dynamic> printCustom(String message, int size, int align,
          {String? charset}) =>
      _invokeMethod('printCustom', {
        'message': message,
        'size': size,
        'align': align,
        'charset': charset
      });

  ///printNewLine()
  Future<dynamic> printNewLine() => _invokeMethod('printNewLine');

  ///paperCut()
  Future<dynamic> paperCut() => _invokeMethod('paperCut');

  ///drawerPin5()
  Future<dynamic> drawerPin2() => _invokeMethod('drawerPin2');

  ///drawerPin5()
  Future<dynamic> drawerPin5() => _invokeMethod('drawerPin5');

  ///printImage(String pathImage)
  Future<dynamic> printImage(String pathImage) =>
      _invokeMethod('printImage', {'pathImage': pathImage});

  ///printImageBytes(Uint8List bytes)
  Future<dynamic> printImageBytes(Uint8List bytes) =>
      _invokeMethod('printImageBytes', {'bytes': bytes});

  ///printQRcode(String textToQR, int width, int height, int align)
  Future<dynamic> printQRcode(
          String textToQR, int width, int height, int align) =>
      _invokeMethod('printQRcode', {
        'textToQR': textToQR,
        'width': width,
        'height': height,
        'align': align
      });

  ///printLeftRight(String string1, String string2, int size,{String? charset, String? format})
  Future<dynamic> printLeftRight(String string1, String string2, int size,
          {String? charset, String? format}) =>
      _invokeMethod('printLeftRight', {
        'string1': string1,
        'string2': string2,
        'size': size,
        'charset': charset,
        'format': format
      });

  ///print3Column(String string1, String string2, String string3, int size,{String? charset, String? format})
  Future<dynamic> print3Column(
          String string1, String string2, String string3, int size,
          {String? charset, String? format}) =>
      _invokeMethod('print3Column', {
        'string1': string1,
        'string2': string2,
        'string3': string3,
        'size': size,
        'charset': charset,
        'format': format
      });

  ///print4Column(String string1, String string2, String string3,String string4, int size,{String? charset, String? format})
  Future<dynamic> print4Column(String string1, String string2, String string3,
          String string4, int size,
          {String? charset, String? format}) =>
      _invokeMethod('print4Column', {
        'string1': string1,
        'string2': string2,
        'string3': string3,
        'string4': string4,
        'size': size,
        'charset': charset,
        'format': format
      });
}

class BluetoothDevice {
  final String? name;
  final String? address;
  final int type = 0;
  bool connected = false;

  BluetoothDevice(this.name, this.address);

  BluetoothDevice.fromMap(Map<String, dynamic> map)
      : name = map['name'],
        address = map['address'];

  Map<String, dynamic> toMap() => {
        'name': name,
        'address': address,
        'type': type,
        'connected': connected,
      };

  @override
  bool operator ==(Object other) {
    return other is BluetoothDevice && other.address == address;
  }

  @override
  int get hashCode => address?.hashCode ?? 0;

  @override
  String toString() => 'BluetoothDevice(name: $name, address: $address)';
}
