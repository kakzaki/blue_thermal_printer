# blue_thermal_printer

[![pub package](https://img.shields.io/pub/v/blue_thermal_printer.svg)](https://pub.dev/packages/blue_thermal_printer)

A maintained Flutter plugin for connecting to thermal printers via Bluetooth (Android). Updated to support modern Flutter SDKs, AGP 8+/9+, Kotlin 2.2+, and Android SDK 35.

> **Note:** This repository is an active, maintained fork of the original `blue_thermal_printer` package.

## Key Updates (v1.3.4)
* **Gradle & Kotlin:** Upgraded AGP to `8.11.1`, Gradle Wrapper to `9.1.0`, and Kotlin to `2.2.20` (Java 17 target).
* **Android SDK:** Updated `compileSdk` and `targetSdk` to `35`, with NDK `28.2.13676358`.
* **Architecture:** Migrated to Flutter built-in Kotlin support & removed legacy multidex.

## Platform Support

| Android | iOS | MacOS | Web | Linux | Windows |
| :-----: | :-: | :---: | :-: | :---: | :-----: |
|   ✔️   |  ❌  |  ❌   |  ❌  |  ❌   |   ❌    |

## Getting Started

### 1. Depend on it

Add this to your package's `pubspec.yaml` file:

```yaml
dependencies:
  blue_thermal_printer:
    git:
      url: https://github.com/hudy-79/blue_thermal_printer.git
      ref: master
