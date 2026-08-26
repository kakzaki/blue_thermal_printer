## 1.3.4
* Upgrade Android Gradle Plugin (AGP) version to `8.11.1` and Gradle Wrapper to `9.1.0`.
* Upgrade Kotlin version to `2.2.20` and set Java/JVM compatibility target to `17`.
* Update `compileSdk` and `targetSdk` to `35`, and set `ndkVersion` to `28.2.13676358` for modern Android SDK & NDK compatibility.
* Migrate project to Flutter's Built-in Kotlin support by removing manual Kotlin Gradle Plugin (`org.jetbrains.kotlin.android`) applications.
* Remove legacy `multidex` dependency (`androidx.multidex:multidex`) as `minSdk` is set to 21.
* Fix build queue locking issues and Kotlin compiler DSL configuration for Gradle 8+/9+ compatibility.


## 1.2.3
* demonstrate using enum for readability
* drawer pin by erica
* fix bug on ios

## 1.2.2
* upgrading gradle

## 1.2.1
* fix android 12 permission

## 1.2.0
* fix web build issue

## 1.1.9
* fix bluetooth listener not working, add additional bluetooth state info, add get connected device function
* Issue #123 fix - app will now automatically disconnect if device is turned off
  thanks to knight-dev

## 1.1.8
* Fix bug on ios build

## 1.1.7

* dartdoc comments and Dart formatter

## 1.1.6

* Add print3Column method
* Add print4Column method
* Support format string

## 1.1.5

* Migrate to Android embedding v2

## 1.1.4

* Fix Error Null-Safety

## 1.1.3

* Fix Cast Error on getBondedDevices

## 1.1.2

* Migrate to null-safety

## 1.1.1

* Support different charset in print methods, thanks to danilof

## 1.1.0

* Add print image bytes method, thanks to mvanvu

## 1.0.9

* Avoiding activity null pointer, thanks to wmattei

## 1.0.8

* fix crash with "Methods marked with @UiThread must be executed on the main thread, thanks to ricardochen

## 1.0.7

* change documentation

## 1.0.6

* add size to printleftright

## 1.0.5

* fix bug

## 1.0.4

* fix bug

## 1.0.3

* Add print left + right

## 1.0.2

* Add Endline in QRCODE

## 1.0.1

* Add Doc.

## 1.0.0

* initial release.
