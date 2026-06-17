#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

CAP_PLUGIN(MitekSdkPlugin, "MitekSdk",
    CAP_PLUGIN_METHOD(startDocumentSession, CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(startFaceSession,     CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(startBarcodeSession,  CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(startVoiceSession,    CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(startNfcSession,      CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(checkPermissions,     CAPPluginReturnPromise);
    CAP_PLUGIN_METHOD(requestPermissions,   CAPPluginReturnPromise);
)
