import Foundation
import Capacitor

@objc(MitekSdkPlugin)
public class MitekSdkPlugin: CAPPlugin, CAPBridgedPlugin {

    public let identifier = "MitekSdkPlugin"
    public let jsName = "MitekSdk"

    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startDocumentSession", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startFaceSession",     returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startBarcodeSession",  returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startVoiceSession",    returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startNfcSession",      returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkPermissions",     returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions",   returnType: CAPPluginReturnPromise),
    ]

    @objc func startDocumentSession(_ call: CAPPluginCall) {
        call.resolve(MitekSdk.unimplementedResult(sessionType: "document"))
    }

    @objc func startFaceSession(_ call: CAPPluginCall) {
        call.resolve(MitekSdk.unimplementedResult(sessionType: "face"))
    }

    @objc func startBarcodeSession(_ call: CAPPluginCall) {
        call.resolve(MitekSdk.unimplementedResult(sessionType: "barcode"))
    }

    @objc func startVoiceSession(_ call: CAPPluginCall) {
        call.resolve(MitekSdk.unimplementedResult(sessionType: "voice"))
    }

    @objc func startNfcSession(_ call: CAPPluginCall) {
        call.resolve(MitekSdk.unimplementedResult(sessionType: "nfc"))
    }

    @objc func checkPermissions(_ call: CAPPluginCall) {
        call.resolve(["camera": "prompt", "audio": "prompt", "nfc": "prompt"])
    }

    @objc func requestPermissions(_ call: CAPPluginCall) {
        call.resolve(["camera": "prompt", "audio": "prompt", "nfc": "prompt"])
    }
}
