import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class BlueThermalPrinter {
  static const int STATE_OFF = 10;
  static const int STATE_TURNING_ON = 11;
  static const int STATE_ON = 12;
  static const int STATE_TURNING_OFF = 13;
  static const int STATE_BLE_TURNING_ON = 14;
  static const int STATE_BLE_ON = 15;
  static const int STATE_BLE_TURNING_OFF = 16;
  static const int ERROR = -1;
  static const int CONNECTED = 1;
  static const int DISCONNECTED = 0;
  static const int DISCONNECT_REQUESTED = 2;

  static const String namespace = 'blue_thermal_printer';

  static const MethodChannel _channel =
      const MethodChannel('$namespace/methods');

  static const EventChannel _readChannel =
      const EventChannel('$namespace/read');

  static const EventChannel _stateChannel =
      const EventChannel('$namespace/state');

  final StreamController<MethodCall> _methodStreamController =
      new StreamController.broadcast();

  //Stream<MethodCall> get _methodStream => _methodStreamController.stream;

  BlueThermalPrinter._() {
    _channel.setMethodCallHandler((MethodCall call) async {
      _methodStreamController.add(call);
    });
  }

  static BlueThermalPrinter _instance = new BlueThermalPrinter._();

  static BlueThermalPrinter get instance => _instance;

  ///onStateChanged()
  Stream<int?> onStateChanged() async* {
    yield await _channel.invokeMethod('state').then((buffer) => buffer);

    yield* _stateChannel.receiveBroadcastStream().map((buffer) => buffer);
  }

  ///onRead()
  Stream<String> onRead() =>
      _readChannel.receiveBroadcastStream().map((buffer) => buffer.toString());

  Future<bool?> get isAvailable async =>
      await _channel.invokeMethod('isAvailable');

  Future<bool?> get isOn async => await _channel.invokeMethod('isOn');

  Future<bool?> get isConnected async =>
      await _channel.invokeMethod('isConnected');

  Future<bool?> get openSettings async =>
      await _channel.invokeMethod('openSettings');

  ///getBondedDevices()
  Future<List<BluetoothDevice>> getBondedDevices() async {
    final List list = await (_channel.invokeMethod('getBondedDevices'));
    return list.map((map) => BluetoothDevice.fromMap(map)).toList();
  }

  ///isDeviceConnected(BluetoothDevice device)
  Future<bool?> isDeviceConnected(BluetoothDevice device) =>
      _channel.invokeMethod('isDeviceConnected', device.toMap());

  ///connect(BluetoothDevice device)
  Future<dynamic> connect(BluetoothDevice device) =>
      _channel.invokeMethod('connect', device.toMap());

  ///disconnect()
  Future<dynamic> disconnect() => _channel.invokeMethod('disconnect');

  ///write(String message)
  Future<dynamic> write(String message) =>
      _channel.invokeMethod('write', {'message': message});

  ///writeBytes(Uint8List message)
  Future<dynamic> writeBytes(Uint8List message) =>
      _channel.invokeMethod('writeBytes', {'message': message});

  ///printCustom(String message, int size, int align,{String? charset})
  Future<dynamic> printCustom(String message, int size, int align,
          {String? charset}) =>
      _channel.invokeMethod('printCustom', {
        'message': message,
        'size': size,
        'align': align,
        'charset': charset
      });

  ///printNewLine()
  Future<dynamic> printNewLine() => _channel.invokeMethod('printNewLine');

  ///paperCut()
  Future<dynamic> paperCut() => _channel.invokeMethod('paperCut');

  ///drawerPin5()
  Future<dynamic> drawerPin2() => _channel.invokeMethod('drawerPin2');

  ///drawerPin5()
  Future<dynamic> drawerPin5() => _channel.invokeMethod('drawerPin5');

  ///printImage(String pathImage)
  Future<dynamic> printImage(String pathImage) =>
      _channel.invokeMethod('printImage', {'pathImage': pathImage});

  ///printImageBytes(Uint8List bytes)
  Future<dynamic> printImageBytes(Uint8List bytes) =>
      _channel.invokeMethod('printImageBytes', {'bytes': bytes});

  ///printQRcode(String textToQR, int width, int height, int align)
  Future<dynamic> printQRcode(
          String textToQR, int width, int height, int align) =>
      _channel.invokeMethod('printQRcode', {
        'textToQR': textToQR,
        'width': width,
        'height': height,
        'align': align
      });

  ///printLeftRight(String string1, String string2, int size,{String? charset, String? format})
  Future<dynamic> printLeftRight(String string1, String string2, int size,
          {String? charset, String? format}) =>
      _channel.invokeMethod('printLeftRight', {
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
      _channel.invokeMethod('print3Column', {
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
      _channel.invokeMethod('print4Column', {
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

  BluetoothDevice.fromMap(Map map)
      : name = map['name'],
        address = map['address'];

  Map<String, dynamic> toMap() => {
        'name': this.name,
        'address': this.address,
        'type': this.type,
        'connected': this.connected,
      };

  operator ==(Object other) {
    return other is BluetoothDevice && other.address == this.address;
  }

  @override
  int get hashCode => address.hashCode;
}
