package id.kakzaki.blue_thermal_printer;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class BlueThermalPrinterPlugin implements FlutterPlugin, ActivityAware,MethodCallHandler, RequestPermissionsResultListener {

  private static final String TAG = "BThermalPrinterPlugin";
  private static final String NAMESPACE = "blue_thermal_printer";
  private static final int REQUEST_COARSE_LOCATION_PERMISSIONS = 1451;
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  private static ConnectedThread THREAD = null;
  private BluetoothAdapter mBluetoothAdapter;

  private Result pendingResult;

  private EventSink readSink;
  private EventSink statusSink;

  private FlutterPluginBinding pluginBinding;
  private ActivityPluginBinding activityBinding;
  private final Object initializationLock = new Object();
  private Context context;
  private MethodChannel channel;

  private EventChannel stateChannel;
  private BluetoothManager mBluetoothManager;

  private Activity activity;

  public BlueThermalPrinterPlugin() {
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    pluginBinding = binding;
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    pluginBinding = null;
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activityBinding = binding;
    setup(
            pluginBinding.getBinaryMessenger(),
            (Application) pluginBinding.getApplicationContext(),
            activityBinding.getActivity(),
            activityBinding);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    detach();
  }

  private void setup(
          final BinaryMessenger messenger,
          final Application application,
          final Activity activity,
          final ActivityPluginBinding activityBinding) {
    synchronized (initializationLock) {
      Log.i(TAG, "setup");
      this.activity = activity;
      this.context = application;
      channel = new MethodChannel(messenger, NAMESPACE + "/methods");
      channel.setMethodCallHandler(this);
      stateChannel = new EventChannel(messenger, NAMESPACE + "/state");
      stateChannel.setStreamHandler(stateStreamHandler);
      EventChannel readChannel = new EventChannel(messenger, NAMESPACE + "/read");
      readChannel.setStreamHandler(readResultsHandler);
      mBluetoothManager = (BluetoothManager) application.getSystemService(Context.BLUETOOTH_SERVICE);
      mBluetoothAdapter = mBluetoothManager.getAdapter();
      activityBinding.addRequestPermissionsResultListener(this);
    }
  }


  private void detach() {
    Log.i(TAG, "detach");
    context = null;
    activityBinding.removeRequestPermissionsResultListener(this);
    activityBinding = null;
    channel.setMethodCallHandler(null);
    channel = null;
    stateChannel.setStreamHandler(null);
    stateChannel = null;
    mBluetoothAdapter = null;
    mBluetoothManager = null;
  }

  // MethodChannel.Result wrapper that responds on the platform thread.
  private static class MethodResultWrapper implements Result {
    private final Result methodResult;
    private final Handler handler;

    MethodResultWrapper(Result result) {
      methodResult = result;
      handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void success(final Object result) {
      handler.post(() -> methodResult.success(result));
    }

    @Override
    public void error(@NonNull final String errorCode, final String errorMessage, final Object errorDetails) {
      handler.post(() -> methodResult.error(errorCode, errorMessage, errorDetails));
    }

    @Override
    public void notImplemented() {
      handler.post(methodResult::notImplemented);
    }
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result rawResult) {
    Result result = new MethodResultWrapper(rawResult);

    if (mBluetoothAdapter == null && !"isAvailable".equals(call.method)) {
      result.error("bluetooth_unavailable", "the device does not have bluetooth", null);
      return;
    }

    final Map<String, Object> arguments = call.arguments();
    switch (call.method) {

      case "state":
        state(result);
        break;

      case "isAvailable":
        result.success(mBluetoothAdapter != null);
        break;

      case "isOn":
        try {
          result.success(mBluetoothAdapter.isEnabled());
        } catch (Exception ex) {
          result.error("Error", ex.getMessage(), exceptionToString(ex));
        }
        break;

      case "isConnected":
        result.success(THREAD != null);
        break;

      case "isDeviceConnected":
        if (arguments.containsKey("address")) {
          String address = (String) arguments.get("address");
          isDeviceConnected(result, address);
        } else {
          result.error("invalid_argument", "argument 'address' not found", null);
        }
        break;

      case "openSettings":
        ContextCompat.startActivity(context, new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS),
                null);
        result.success(true);
        break;

      case "getBondedDevices":
        try {

          if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity,
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

              ActivityCompat.requestPermissions(activity,new String[]{
                      Manifest.permission.BLUETOOTH_SCAN,
                      Manifest.permission.BLUETOOTH_CONNECT,
                      Manifest.permission.ACCESS_FINE_LOCATION,
              }, 1);

              pendingResult = result;
              break;
            }
          } else {
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

              ActivityCompat.requestPermissions(activity,
                      new String[] { Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_COARSE_LOCATION_PERMISSIONS);

              pendingResult = result;
              break;
            }
          }
          getBondedDevices(result);

        } catch (Exception ex) {
          result.error("Error", ex.getMessage(), exceptionToString(ex));
        }

        break;

      case "connect":
        if (arguments.containsKey("address")) {
          String address = (String) arguments.get("address");
          connect(result, address);
        } else {
          result.error("invalid_argument", "argument 'address' not found", null);
        }
        break;

      case "disconnect":
        disconnect(result);
        break;

      case "write":
        if (arguments.containsKey("message")) {
          String message = (String) arguments.get("message");
          write(result, message);
        } else {
          result.error("invalid_argument", "argument 'message' not found", null);
        }
        break;

      case "writeBytes":
        if (arguments.containsKey("message")) {
          byte[] message = (byte[]) arguments.get("message");
          writeBytes(result, message);
        } else {
          result.error("invalid_argument", "argument 'message' not found", null);
        }
        break;

      case "printCustom":
        if (arguments.containsKey("message")) {
          String message = (String) arguments.get("message");
          int size = (int) arguments.get("size");
          int align = (int) arguments.get("align");
          String charset = (String) arguments.get("charset");
          printCustom(result, message, size, align, charset);
        } else {
          result.error("invalid_argument", "argument 'message' not found", null);
        }
        break;

      case "printNewLine":
        printNewLine(result);
        break;

      case "paperCut":
        paperCut(result);
        break;

      case "drawerPin2":
        drawerPin2(result);
        break;

      case "drawerPin5":
        drawerPin5(result);
        break;

      case "printImage":
        if (arguments.containsKey("pathImage")) {
          String pathImage = (String) arguments.get("pathImage");
          printImage(result, pathImage);
        } else {
          result.error("invalid_argument", "argument 'pathImage' not found", null);
        }
        break;

        case "printImageBytes":
        if (arguments.containsKey("bytes")) {
          byte[] bytes = (byte[]) arguments.get("bytes");
          printImageBytes(result, bytes);
        } else {
          result.error("invalid_argument", "argument 'bytes' not found", null);
        }
        break;

      case "printQRcode":
        if (arguments.containsKey("textToQR")) {
          String textToQR = (String) arguments.get("textToQR");
          int width = (int) arguments.get("width");
          int height = (int) arguments.get("height");
          int align = (int) arguments.get("align");
          printQRcode(result, textToQR, width, height, align);
        } else {
          result.error("invalid_argument", "argument 'textToQR' not found", null);
        }
        break;
      case "printLeftRight":
        if (arguments.containsKey("string1")) {
          String string1 = (String) arguments.get("string1");
          String string2 = (String) arguments.get("string2");
          int size = (int) arguments.get("size");
          String charset = (String) arguments.get("charset");
          String format = (String) arguments.get("format");
          printLeftRight(result, string1, string2, size, charset,format);
        } else {
          result.error("invalid_argument", "argument 'message' not found", null);
        }
        break;
      case "print3Column":
        if (arguments.containsKey("string1")) {
          String string1 = (String) arguments.get("string1");
          String string2 = (String) arguments.get("string2");
          String string3 = (String) arguments.get("string3");
          int size = (int) arguments.get("size");
          String charset = (String) arguments.get("charset");
          String format = (String) arguments.get("format");
          print3Column(result, string1, string2,string3, size, charset,format);
        } else {
          result.error("invalid_argument", "argument 'message' not found", null);
        }
        break;
      case "print4Column":
        if (arguments.containsKey("string1")) {
          String string1 = (String) arguments.get("string1");
          String string2 = (String) arguments.get("string2");
          String string3 = (String) arguments.get("string3");
          String string4 = (String) arguments.get("string4");
          int size = (int) arguments.get("size");
          String charset = (String) arguments.get("charset");
          String format = (String) arguments.get("format");
          print4Column(result, string1, string2,string3,string4, size, charset,format);
        } else {
          result.error("invalid_argument", "argument 'message' not found", null);
        }
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  /**
   * @param requestCode  requestCode
   * @param permissions  permissions
   * @param grantResults grantResults
   * @return boolean
   */
  @Override
  public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    if (requestCode == REQUEST_COARSE_LOCATION_PERMISSIONS) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        getBondedDevices(pendingResult);
      } else {
        pendingResult.error("no_permissions", "this plugin requires location permissions for scanning", null);
        pendingResult = null;
      }
      return true;
    }
    return false;
  }

  private void state(Result result) {
    try {
      switch (mBluetoothAdapter.getState()) {
        case BluetoothAdapter.STATE_OFF:
          result.success(BluetoothAdapter.STATE_OFF);
          break;
        case BluetoothAdapter.STATE_ON:
          result.success(BluetoothAdapter.STATE_ON);
          break;
        case BluetoothAdapter.STATE_TURNING_OFF:
          result.success(BluetoothAdapter.STATE_TURNING_OFF);
          break;
        case BluetoothAdapter.STATE_TURNING_ON:
          result.success(BluetoothAdapter.STATE_TURNING_ON);
          break;
        default:
          result.success(0);
          break;
      }
    } catch (SecurityException e) {
      result.error("invalid_argument", "Argument 'address' not found", null);
    }
  }

  /**
   * @param result result
   */
  private void getBondedDevices(Result result) {

    List<Map<String, Object>> list = new ArrayList<>();

    for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
      Map<String, Object> ret = new HashMap<>();
      ret.put("address", device.getAddress());
      ret.put("name", device.getName());
      ret.put("type", device.getType());
      list.add(ret);
    }

    result.success(list);
  }


  /**
   * @param result  result
   * @param address address
   */
  private void isDeviceConnected(Result result, String address) {

    AsyncTask.execute(() -> {
      try {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
          result.error("connect_error", "device not found", null);
          return;
        }

        if (THREAD != null && device.ACTION_ACL_CONNECTED.equals(new Intent(BluetoothDevice.ACTION_ACL_CONNECTED).getAction())) {
          result.success(true);
        }else{
          result.success(false);
        }

      } catch (Exception ex) {
        Log.e(TAG, ex.getMessage(), ex);
        result.error("connect_error", ex.getMessage(), exceptionToString(ex));
      }
    });
  }

  private String exceptionToString(Exception ex) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    ex.printStackTrace(pw);
    return sw.toString();
  }

  /**
   * @param result  result
   * @param address address
   */
  private void connect(Result result, String address) {

    if (THREAD != null) {
      result.error("connect_error", "already connected", null);
      return;
    }
    AsyncTask.execute(() -> {
      try {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
          result.error("connect_error", "device not found", null);
          return;
        }

        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);

        if (socket == null) {
          result.error("connect_error", "socket connection not established", null);
          return;
        }

        // Cancel bt discovery, even though we didn't start it
        mBluetoothAdapter.cancelDiscovery();

        try {
          socket.connect();
          THREAD = new ConnectedThread(socket);
          THREAD.start();
          result.success(true);
        } catch (Exception ex) {
          Log.e(TAG, ex.getMessage(), ex);
          result.error("connect_error", ex.getMessage(), exceptionToString(ex));
        }
      } catch (Exception ex) {
        Log.e(TAG, ex.getMessage(), ex);
        result.error("connect_error", ex.getMessage(), exceptionToString(ex));
      }
    });
  }

  /**
   * @param result result
   */
  private void disconnect(Result result) {

    if (THREAD == null) {
      result.error("disconnection_error", "not connected", null);
      return;
    }
    AsyncTask.execute(() -> {
      try {
        THREAD.cancel();
        THREAD = null;
        result.success(true);
      } catch (Exception ex) {
        Log.e(TAG, ex.getMessage(), ex);
        result.error("disconnection_error", ex.getMessage(), exceptionToString(ex));
      }
    });
  }

  /**
   * @param result  result
   * @param message message
   */
  private void write(Result result, String message) {
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }

    try {
      THREAD.write(message.getBytes());
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }
  }

  private void writeBytes(Result result, byte[] message) {
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }

    try {
      THREAD.write(message);
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }
  }

  private void printCustom(Result result, String message, int size, int align, String charset) {
    // Print config "mode"
    byte[] cc = new byte[] { 0x1B, 0x21, 0x03 }; // 0- normal size text
    // byte[] cc1 = new byte[]{0x1B,0x21,0x00}; // 0- normal size text
    byte[] bb = new byte[] { 0x1B, 0x21, 0x08 }; // 1- only bold text
    byte[] bb2 = new byte[] { 0x1B, 0x21, 0x20 }; // 2- bold with medium text
    byte[] bb3 = new byte[] { 0x1B, 0x21, 0x10 }; // 3- bold with large text
    byte[] bb4 = new byte[] { 0x1B, 0x21, 0x30 }; // 4- strong text
    byte[] bb5 = new byte[] { 0x1B, 0x21, 0x50 }; // 5- extra strong text
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }

    try {
      switch (size) {
        case 0:
          THREAD.write(cc);
          break;
        case 1:
          THREAD.write(bb);
          break;
        case 2:
          THREAD.write(bb2);
          break;
        case 3:
          THREAD.write(bb3);
          break;
        case 4:
          THREAD.write(bb4);
          break;
        case 5:
          THREAD.write(bb5);
      }

      switch (align) {
        case 0:
          // left align
          THREAD.write(PrinterCommands.ESC_ALIGN_LEFT);
          break;
        case 1:
          // center align
          THREAD.write(PrinterCommands.ESC_ALIGN_CENTER);
          break;
        case 2:
          // right align
          THREAD.write(PrinterCommands.ESC_ALIGN_RIGHT);
          break;
      }
      if(charset != null) {
        THREAD.write(message.getBytes(charset));
      } else {
        THREAD.write(message.getBytes());
      }
      THREAD.write(PrinterCommands.FEED_LINE);
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }
  }

  private void printLeftRight(Result result, String msg1, String msg2, int size ,String charset,String format) {
    byte[] cc = new byte[] { 0x1B, 0x21, 0x03 }; // 0- normal size text
    // byte[] cc1 = new byte[]{0x1B,0x21,0x00}; // 0- normal size text
    byte[] bb = new byte[] { 0x1B, 0x21, 0x08 }; // 1- only bold text
    byte[] bb2 = new byte[] { 0x1B, 0x21, 0x20 }; // 2- bold with medium text
    byte[] bb3 = new byte[] { 0x1B, 0x21, 0x10 }; // 3- bold with large text
    byte[] bb4 = new byte[] { 0x1B, 0x21, 0x30 }; // 4- strong text
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }
    try {
      switch (size) {
        case 0:
          THREAD.write(cc);
          break;
        case 1:
          THREAD.write(bb);
          break;
        case 2:
          THREAD.write(bb2);
          break;
        case 3:
          THREAD.write(bb3);
          break;
        case 4:
          THREAD.write(bb4);
          break;
      }
      THREAD.write(PrinterCommands.ESC_ALIGN_CENTER);
      String line = String.format("%-15s %15s %n", msg1, msg2);
      if(format != null) {
        line = String.format(format, msg1, msg2);
      }
      if(charset != null) {
        THREAD.write(line.getBytes(charset));
      } else {
        THREAD.write(line.getBytes());
      }
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }

  }

  private void print3Column(Result result, String msg1, String msg2, String msg3, int size ,String charset, String format) {
    byte[] cc = new byte[] { 0x1B, 0x21, 0x03 }; // 0- normal size text
    // byte[] cc1 = new byte[]{0x1B,0x21,0x00}; // 0- normal size text
    byte[] bb = new byte[] { 0x1B, 0x21, 0x08 }; // 1- only bold text
    byte[] bb2 = new byte[] { 0x1B, 0x21, 0x20 }; // 2- bold with medium text
    byte[] bb3 = new byte[] { 0x1B, 0x21, 0x10 }; // 3- bold with large text
    byte[] bb4 = new byte[] { 0x1B, 0x21, 0x30 }; // 4- strong text
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }
    try {
      switch (size) {
        case 0:
          THREAD.write(cc);
          break;
        case 1:
          THREAD.write(bb);
          break;
        case 2:
          THREAD.write(bb2);
          break;
        case 3:
          THREAD.write(bb3);
          break;
        case 4:
          THREAD.write(bb4);
          break;
      }
      THREAD.write(PrinterCommands.ESC_ALIGN_CENTER);
      String line = String.format("%-10s %10s %10s %n", msg1, msg2  , msg3);
      if(format != null) {
        line = String.format(format, msg1, msg2, msg3);
      }
      if(charset != null) {
        THREAD.write(line.getBytes(charset));
      } else {
        THREAD.write(line.getBytes());
      }
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }

  }

  private void print4Column(Result result, String msg1, String msg2,String msg3,String msg4, int size, String charset, String format) {
    byte[] cc = new byte[] { 0x1B, 0x21, 0x03 }; // 0- normal size text
    // byte[] cc1 = new byte[]{0x1B,0x21,0x00}; // 0- normal size text
    byte[] bb = new byte[] { 0x1B, 0x21, 0x08 }; // 1- only bold text
    byte[] bb2 = new byte[] { 0x1B, 0x21, 0x20 }; // 2- bold with medium text
    byte[] bb3 = new byte[] { 0x1B, 0x21, 0x10 }; // 3- bold with large text
    byte[] bb4 = new byte[] { 0x1B, 0x21, 0x30 }; // 4- strong text
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }
    try {
      switch (size) {
        case 0:
          THREAD.write(cc);
          break;
        case 1:
          THREAD.write(bb);
          break;
        case 2:
          THREAD.write(bb2);
          break;
        case 3:
          THREAD.write(bb3);
          break;
        case 4:
          THREAD.write(bb4);
          break;
      }
      THREAD.write(PrinterCommands.ESC_ALIGN_CENTER);
      String line = String.format("%-8s %7s %7s %7s %n", msg1, msg2,msg3,msg4);
      if(format != null) {
        line = String.format(format, msg1, msg2,msg3,msg4);
      }
      if(charset != null) {
        THREAD.write(line.getBytes(charset));
      } else {
        THREAD.write(line.getBytes());
      }
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }

  }

  private void printNewLine(Result result) {
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }
    try {
      THREAD.write(PrinterCommands.FEED_LINE);
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }
  }

  private void paperCut(Result result) {
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }
    try {
      THREAD.write(PrinterCommands.FEED_PAPER_AND_CUT);
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }
  }

  private void drawerPin2(Result result) {
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }
    try {
      THREAD.write(PrinterCommands.ESC_DRAWER_PIN2);
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }
  }

  private void drawerPin5(Result result) {
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }
    try {
      THREAD.write(PrinterCommands.ESC_DRAWER_PIN5);
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }
  }

  private void printImage(Result result, String pathImage) {
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }
    try {
      Bitmap bmp = BitmapFactory.decodeFile(pathImage);
      if (bmp != null) {
        byte[] command = Utils.decodeBitmap(bmp);
        THREAD.write(PrinterCommands.ESC_ALIGN_CENTER);
        THREAD.write(command);
      } else {
        Log.e("Print Photo error", "the file isn't exists");
      }
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }
  }

  private void printImageBytes(Result result, byte[] bytes) {
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }
    try {
      Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
      if (bmp != null) {
        byte[] command = Utils.decodeBitmap(bmp);
        THREAD.write(PrinterCommands.ESC_ALIGN_CENTER);
        THREAD.write(command);
      } else {
        Log.e("Print Photo error", "the file isn't exists");
      }
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }
  }

  private void printQRcode(Result result, String textToQR, int width, int height, int align) {
    MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
    if (THREAD == null) {
      result.error("write_error", "not connected", null);
      return;
    }
    try {
      switch (align) {
        case 0:
          // left align
          THREAD.write(PrinterCommands.ESC_ALIGN_LEFT);
          break;
        case 1:
          // center align
          THREAD.write(PrinterCommands.ESC_ALIGN_CENTER);
          break;
        case 2:
          // right align
          THREAD.write(PrinterCommands.ESC_ALIGN_RIGHT);
          break;
      }
      BitMatrix bitMatrix = multiFormatWriter.encode(textToQR, BarcodeFormat.QR_CODE, width, height);
      BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
      Bitmap bmp = barcodeEncoder.createBitmap(bitMatrix);
      if (bmp != null) {
        byte[] command = Utils.decodeBitmap(bmp);
        THREAD.write(command);
      } else {
        Log.e("Print Photo error", "the file isn't exists");
      }
      result.success(true);
    } catch (Exception ex) {
      Log.e(TAG, ex.getMessage(), ex);
      result.error("write_error", ex.getMessage(), exceptionToString(ex));
    }
  }

  private class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream inputStream;
    private final OutputStream outputStream;

    ConnectedThread(BluetoothSocket socket) {
      mmSocket = socket;
      InputStream tmpIn = null;
      OutputStream tmpOut = null;

      try {
        tmpIn = socket.getInputStream();
        tmpOut = socket.getOutputStream();
      } catch (IOException e) {
        e.printStackTrace();
      }
      inputStream = tmpIn;
      outputStream = tmpOut;
    }

    public void run() {
      byte[] buffer = new byte[1024];
      int bytes;
      while (true) {
        try {
          bytes = inputStream.read(buffer);
          readSink.success(new String(buffer, 0, bytes));
        } catch (NullPointerException e) {
          break;
        } catch (IOException e) {
          break;
        }
      }
    }

    public void write(byte[] bytes) {
      try {
        outputStream.write(bytes);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public void cancel() {
      try {
        outputStream.flush();
        outputStream.close();

        inputStream.close();

        mmSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private final StreamHandler stateStreamHandler = new StreamHandler() {

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        Log.d(TAG, action);

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
          THREAD = null;
          statusSink.success(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1));
        } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
          statusSink.success(1);
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
          THREAD = null;
          statusSink.success(2);
        } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
          THREAD = null;
          statusSink.success(0);
        }
      }
    };

    @Override
    public void onListen(Object o, EventSink eventSink) {
      statusSink = eventSink;
      context.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

      context.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

      context.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED));

      context.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));

    }

    @Override
    public void onCancel(Object o) {
      statusSink = null;
      context.unregisterReceiver(mReceiver);
    }
  };

  private final StreamHandler readResultsHandler = new StreamHandler() {
    @Override
    public void onListen(Object o, EventSink eventSink) {
      readSink = eventSink;
    }

    @Override
    public void onCancel(Object o) {
      readSink = null;
    }
  };
}