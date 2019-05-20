
import 'package:blue_thermal_printer/blue_thermal_printer.dart';

class TestPrint {
  BlueThermalPrinter bluetooth = BlueThermalPrinter.instance;

   sample() async {
    //SIZE
    // 0- normal size text
    // 1- only bold text
    // 2- bold with medium text
    // 3- bold with large text
    //ALIGN
    // 0- ESC_ALIGN_LEFT
    // 1- ESC_ALIGN_CENTER
    // 2- ESC_ALIGN_RIGHT
    bluetooth.isConnected.then((isConnected) {
      if (isConnected) {
//        bluetooth.printCustom("HEADER",3,1);
//        bluetooth.printNewLine();
//        bluetooth.printLeftRight("Kiri123456", "Kanan1234567");
//        bluetooth.printImage(pathImage);
//        bluetooth.printNewLine();
//        bluetooth.printCustom("Body left",1,0);
//        bluetooth.printCustom("Body right",0,2);
//        bluetooth.printNewLine();
//        bluetooth.printCustom("Terimakasih",2,1);
//        bluetooth.printNewLine();
        bluetooth.printQRcode("Insert Your Own Text to Generate");
//        bluetooth.printNewLine();
//        bluetooth.printNewLine();
//        bluetooth.paperCut();
      }
    });
  }
}