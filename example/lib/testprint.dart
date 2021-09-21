import 'package:blue_thermal_printer/blue_thermal_printer.dart';


class TestPrint {
  BlueThermalPrinter bluetooth = BlueThermalPrinter.instance;

   sample(String pathImage) async {
    //SIZE
    // 0- normal size text
    // 1- only bold text
    // 2- bold with medium text
    // 3- bold with large text
    //ALIGN
    // 0- ESC_ALIGN_LEFT
    // 1- ESC_ALIGN_CENTER
    // 2- ESC_ALIGN_RIGHT

//     var response = await http.get("IMAGE_URL");
//     Uint8List bytes = response.bodyBytes;
    bluetooth.isConnected.then((isConnected) {
      if (isConnected) {
        bluetooth.printNewLine();
        bluetooth.printCustom("HEADER",3,1);
        bluetooth.printNewLine();
        bluetooth.printImage(pathImage);   //path of your image/logo
        bluetooth.printNewLine();
//      bluetooth.printImageBytes(bytes.buffer.asUint8List(bytes.offsetInBytes, bytes.lengthInBytes));
        bluetooth.printLeftRight("LEFT", "RIGHT",0);
        bluetooth.printLeftRight("LEFT", "RIGHT",1);
        bluetooth.printLeftRight("LEFT", "RIGHT",1,format: "%-15s %15s %n");
        bluetooth.printNewLine();
        bluetooth.printLeftRight("LEFT", "RIGHT",2);
        bluetooth.printLeftRight("LEFT", "RIGHT",3);
        bluetooth.printLeftRight("LEFT", "RIGHT",4);
        bluetooth.printNewLine();
        bluetooth.print3Column("Col1", "Col2", "Col3",1);
        bluetooth.print3Column("Col1", "Col2", "Col3",1,format: "%-10s %10s %10s %n");
        bluetooth.printNewLine();
        bluetooth.print4Column("Col1","Col2","Col3","Col4",1);
        bluetooth.print4Column("Col1","Col2","Col3","Col4",1,format: "%-8s %7s %7s %7s %n" );
        bluetooth.printNewLine();
        String testString = " čĆžŽšŠ-H-ščđ";
        bluetooth.printCustom(testString, 1, 1, charset: "windows-1250");
        bluetooth.printLeftRight("Številka:", "18000001", 1, charset: "windows-1250");
        bluetooth.printCustom("Body left",1,0);
        bluetooth.printCustom("Body right",0,2);
        bluetooth.printNewLine();
        bluetooth.printCustom("Thank You",2,1);
        bluetooth.printNewLine();
        bluetooth.printQRcode("Insert Your Own Text to Generate", 200, 200, 1);
        bluetooth.printNewLine();
        bluetooth.printNewLine();
        bluetooth.paperCut();
      }
    });
  }
}