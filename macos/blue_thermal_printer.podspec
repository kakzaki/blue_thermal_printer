#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint flutter_blue.podspec' to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'blue_thermal_printer'
  s.version          = '0.0.1'
  s.summary          = 'A new Flutter plugin for connecting to thermal printer via bluetooth'
  s.description      = <<-DESC
A new Flutter plugin for connecting to thermal printer vie bluetooth
                       DESC
  s.homepage         = 'http://kakzaki.my.id'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Zaki Mubarok' => 'kakzaki@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'FlutterMacOS'
  s.platform = :osx, '10.13'
  s.framework = 'CoreBluetooth'

end
