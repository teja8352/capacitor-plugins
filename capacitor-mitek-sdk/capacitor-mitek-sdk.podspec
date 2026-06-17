require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name             = 'capacitor-mitek-sdk'
  s.version          = package['version']
  s.summary          = package['description']
  s.license          = package['license'] || 'MIT'
  s.homepage         = 'https://github.com'
  s.author           = package['author'] || {}
  s.source           = { :git => '', :tag => s.version.to_s }
  s.source_files     = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
  s.ios.deployment_target = '14.0'
  s.dependency 'Capacitor'
  s.swift_version    = '5.1'
end
