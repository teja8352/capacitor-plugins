package com.mitek.capacitor

import android.Manifest
import android.util.Log
import androidx.activity.result.ActivityResult
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import com.miteksystems.misnap.core.MiSnapSettings
import com.miteksystems.misnap.workflow.MiSnapWorkflowActivity

@CapacitorPlugin(
    name = "MitekSdk",
    permissions = [
        Permission(alias = "camera", strings = [Manifest.permission.CAMERA]),
        Permission(alias = "audio",  strings = [Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS]),
        Permission(alias = "nfc",    strings = [Manifest.permission.NFC]),
    ],
)
class MitekSdkPlugin : Plugin() {

    private companion object {
        const val TAG = "MitekSdkPlugin"
        const val KEY_LICENSE      = "license"
        const val KEY_DOC_TYPE     = "documentType"
        const val KEY_MRZ_LINE1    = "mrzLine1"
        const val KEY_MRZ_LINE2    = "mrzLine2"
        const val KEY_MRZ_LINE3    = "mrzLine3"
        const val KEY_DOC_NUMBER   = "documentNumber"
        const val KEY_DOB          = "dateOfBirth"
        const val KEY_DOE          = "dateOfExpiry"
        const val KEY_COUNTRY      = "country"
        const val KEY_DOC_CODE     = "documentCode"
        const val KEY_PERMISSIONS  = "permissions"
        const val CB_DOCUMENT      = "onDocumentResult"
        const val CB_FACE          = "onFaceResult"
        const val CB_BARCODE       = "onBarcodeResult"
        const val CB_VOICE         = "onVoiceResult"
        const val CB_NFC           = "onNfcResult"
        const val PCB_CAMERA       = "onCameraPermission"
        const val PCB_AUDIO        = "onAudioPermission"
        const val PCB_NFC          = "onNfcPermission"
        const val PCB_ALL          = "onAllPermissions"
    }

    private lateinit var sdk: MitekSdk

    override fun load() {
        Log.i(TAG, "load")
        sdk = MitekSdk(context)
    }

    @PluginMethod
    fun startDocumentSession(call: PluginCall) {
        Log.i(TAG, "startDocumentSession")
        val license = call.getString(KEY_LICENSE)
        if (license.isNullOrBlank()) {
            Log.e(TAG, "startDocumentSession: missing license")
            call.reject("'license' is required", MitekSdk.ERR_LICENSE_MISSING)
            return
        }
        val useCase = resolveDocumentUseCase(call.getString(KEY_DOC_TYPE))
        if (useCase == null) {
            Log.e(TAG, "startDocumentSession: unknown documentType=${call.getString(KEY_DOC_TYPE)}")
            call.reject("Unknown documentType. Valid: PASSPORT, ID_FRONT, ID_BACK, CHECK_FRONT, CHECK_BACK, GENERIC_DOCUMENT", MitekSdk.ERR_SETTINGS_ERROR)
            return
        }
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            Log.d(TAG, "startDocumentSession: requesting camera permission")
            requestPermissionForAlias("camera", call, PCB_CAMERA)
            return
        }
        launchDocument(call, license, useCase)
    }

    @PluginMethod
    fun startFaceSession(call: PluginCall) {
        Log.i(TAG, "startFaceSession")
        val license = call.getString(KEY_LICENSE)
        if (license.isNullOrBlank()) {
            Log.e(TAG, "startFaceSession: missing license")
            call.reject("'license' is required", MitekSdk.ERR_LICENSE_MISSING)
            return
        }
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            Log.d(TAG, "startFaceSession: requesting camera permission")
            requestPermissionForAlias("camera", call, PCB_CAMERA)
            return
        }
        launchFace(call, license)
    }

    @PluginMethod
    fun startBarcodeSession(call: PluginCall) {
        Log.i(TAG, "startBarcodeSession")
        val license = call.getString(KEY_LICENSE)
        if (license.isNullOrBlank()) {
            Log.e(TAG, "startBarcodeSession: missing license")
            call.reject("'license' is required", MitekSdk.ERR_LICENSE_MISSING)
            return
        }
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            Log.d(TAG, "startBarcodeSession: requesting camera permission")
            requestPermissionForAlias("camera", call, PCB_CAMERA)
            return
        }
        launchBarcode(call, license)
    }

    @PluginMethod
    fun startVoiceSession(call: PluginCall) {
        Log.i(TAG, "startVoiceSession")
        val license = call.getString(KEY_LICENSE)
        if (license.isNullOrBlank()) {
            Log.e(TAG, "startVoiceSession: missing license")
            call.reject("'license' is required", MitekSdk.ERR_LICENSE_MISSING)
            return
        }
        if (getPermissionState("audio") != PermissionState.GRANTED) {
            Log.d(TAG, "startVoiceSession: requesting audio permission")
            requestPermissionForAlias("audio", call, PCB_AUDIO)
            return
        }
        launchVoice(call, license)
    }

    @PluginMethod
    fun startNfcSession(call: PluginCall) {
        Log.i(TAG, "startNfcSession")
        val license = call.getString(KEY_LICENSE)
        if (license.isNullOrBlank()) {
            Log.e(TAG, "startNfcSession: missing license")
            call.reject("'license' is required", MitekSdk.ERR_LICENSE_MISSING)
            return
        }
        if (getPermissionState("nfc") != PermissionState.GRANTED) {
            Log.d(TAG, "startNfcSession: requesting NFC permission")
            requestPermissionForAlias("nfc", call, PCB_NFC)
            return
        }
        launchNfc(call, license)
    }

    @PluginMethod
    fun checkPermissions(call: PluginCall) {
        Log.d(TAG, "checkPermissions")
        call.resolve(permissionStatus())
    }

    @PluginMethod
    fun requestPermissions(call: PluginCall) {
        Log.d(TAG, "requestPermissions")
        val requested = call.getArray(KEY_PERMISSIONS)?.toList<String>()
        val aliases = if (requested.isNullOrEmpty()) {
            arrayOf("camera", "audio", "nfc")
        } else {
            requested.toTypedArray()
        }
        requestPermissionForAliases(aliases, call, PCB_ALL)
    }

    @PermissionCallback
    private fun onCameraPermission(call: PluginCall) {
        Log.d(TAG, "onCameraPermission state=${getPermissionState("camera")}")
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            Log.w(TAG, "onCameraPermission: denied")
            call.reject("Camera permission denied", MitekSdk.ERR_PERMISSION_DENIED)
            return
        }
        reLaunch(call)
    }

    @PermissionCallback
    private fun onAudioPermission(call: PluginCall) {
        Log.d(TAG, "onAudioPermission state=${getPermissionState("audio")}")
        if (getPermissionState("audio") != PermissionState.GRANTED) {
            Log.w(TAG, "onAudioPermission: denied")
            call.reject("Microphone permission denied", MitekSdk.ERR_PERMISSION_DENIED)
            return
        }
        reLaunch(call)
    }

    @PermissionCallback
    private fun onNfcPermission(call: PluginCall) {
        Log.d(TAG, "onNfcPermission state=${getPermissionState("nfc")}")
        if (getPermissionState("nfc") != PermissionState.GRANTED) {
            Log.w(TAG, "onNfcPermission: denied")
            call.reject("NFC permission denied", MitekSdk.ERR_PERMISSION_DENIED)
            return
        }
        reLaunch(call)
    }

    @PermissionCallback
    private fun onAllPermissions(call: PluginCall) {
        Log.d(TAG, "onAllPermissions")
        call.resolve(permissionStatus())
    }

    @ActivityCallback
    private fun onDocumentResult(call: PluginCall, result: ActivityResult) {
        Log.d(TAG, "onDocumentResult resultCode=${result.resultCode}")
        handleResult(call, "document")
    }

    @ActivityCallback
    private fun onFaceResult(call: PluginCall, result: ActivityResult) {
        Log.d(TAG, "onFaceResult resultCode=${result.resultCode}")
        handleResult(call, "face")
    }

    @ActivityCallback
    private fun onBarcodeResult(call: PluginCall, result: ActivityResult) {
        Log.d(TAG, "onBarcodeResult resultCode=${result.resultCode}")
        handleResult(call, "barcode")
    }

    @ActivityCallback
    private fun onVoiceResult(call: PluginCall, result: ActivityResult) {
        Log.d(TAG, "onVoiceResult resultCode=${result.resultCode}")
        handleResult(call, "voice")
    }

    @ActivityCallback
    private fun onNfcResult(call: PluginCall, result: ActivityResult) {
        Log.d(TAG, "onNfcResult resultCode=${result.resultCode}")
        handleResult(call, "nfc")
    }

    private fun reLaunch(call: PluginCall) {
        val method = call.methodName ?: run {
            Log.e(TAG, "reLaunch: no methodName on call")
            call.reject("Internal error: missing method name", MitekSdk.ERR_UNKNOWN)
            return
        }
        Log.d(TAG, "reLaunch method=$method")
        val license = call.getString(KEY_LICENSE) ?: run {
            call.reject("'license' is required", MitekSdk.ERR_LICENSE_MISSING)
            return
        }
        when (method) {
            "startDocumentSession" -> {
                val useCase = resolveDocumentUseCase(call.getString(KEY_DOC_TYPE)) ?: MiSnapSettings.UseCase.PASSPORT
                launchDocument(call, license, useCase)
            }
            "startFaceSession"    -> launchFace(call, license)
            "startBarcodeSession" -> launchBarcode(call, license)
            "startVoiceSession"   -> launchVoice(call, license)
            "startNfcSession"     -> launchNfc(call, license)
            else -> {
                Log.e(TAG, "reLaunch: unknown method $method")
                call.reject("Unknown session method", MitekSdk.ERR_UNKNOWN)
            }
        }
    }

    private fun launchDocument(call: PluginCall, license: String, useCase: MiSnapSettings.UseCase) {
        try {
            Log.d(TAG, "launchDocument useCase=$useCase")
            val step = sdk.buildDocumentStep(license, useCase)
            startActivityForResult(call, MiSnapWorkflowActivity.buildIntent(context, step), CB_DOCUMENT)
        } catch (e: Exception) {
            Log.e(TAG, "launchDocument failed", e)
            call.reject("Failed to start document session: ${e.message}", MitekSdk.ERR_UNKNOWN, e)
        }
    }

    private fun launchFace(call: PluginCall, license: String) {
        try {
            Log.d(TAG, "launchFace")
            val step = sdk.buildFaceStep(license)
            startActivityForResult(call, MiSnapWorkflowActivity.buildIntent(context, step), CB_FACE)
        } catch (e: Exception) {
            Log.e(TAG, "launchFace failed", e)
            call.reject("Failed to start face session: ${e.message}", MitekSdk.ERR_UNKNOWN, e)
        }
    }

    private fun launchBarcode(call: PluginCall, license: String) {
        try {
            Log.d(TAG, "launchBarcode")
            val step = sdk.buildBarcodeStep(license)
            startActivityForResult(call, MiSnapWorkflowActivity.buildIntent(context, step), CB_BARCODE)
        } catch (e: Exception) {
            Log.e(TAG, "launchBarcode failed", e)
            call.reject("Failed to start barcode session: ${e.message}", MitekSdk.ERR_UNKNOWN, e)
        }
    }

    private fun launchVoice(call: PluginCall, license: String) {
        try {
            Log.d(TAG, "launchVoice")
            val step = sdk.buildVoiceStep(license)
            startActivityForResult(call, MiSnapWorkflowActivity.buildIntent(context, step), CB_VOICE)
        } catch (e: Exception) {
            Log.e(TAG, "launchVoice failed", e)
            call.reject("Failed to start voice session: ${e.message}", MitekSdk.ERR_UNKNOWN, e)
        }
    }

    private fun launchNfc(call: PluginCall, license: String) {
        try {
            Log.d(TAG, "launchNfc")
            val step = sdk.buildNfcStep(
                license        = license,
                mrzLine1       = call.getString(KEY_MRZ_LINE1),
                mrzLine2       = call.getString(KEY_MRZ_LINE2),
                mrzLine3       = call.getString(KEY_MRZ_LINE3),
                documentNumber = call.getString(KEY_DOC_NUMBER),
                dateOfBirth    = call.getString(KEY_DOB),
                dateOfExpiry   = call.getString(KEY_DOE),
                country        = call.getString(KEY_COUNTRY),
                documentCode   = call.getString(KEY_DOC_CODE),
            )
            startActivityForResult(call, MiSnapWorkflowActivity.buildIntent(context, step), CB_NFC)
        } catch (e: Exception) {
            Log.e(TAG, "launchNfc failed", e)
            call.reject("Failed to start NFC session: ${e.message}", MitekSdk.ERR_UNKNOWN, e)
        }
    }

    private fun handleResult(call: PluginCall, sessionType: String) {
        try {
            val result = sdk.parseResults(sessionType)
            Log.d(TAG, "handleResult sessionType=$sessionType success=${result.optBoolean("success")}")
            call.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "handleResult error sessionType=$sessionType", e)
            val err = JSObject()
            err.put("success", false)
            err.put("sessionType", sessionType)
            err.put("errorCode", MitekSdk.ERR_UNKNOWN)
            err.put("errorMessage", "Unexpected error: ${e.message}")
            call.resolve(err)
        } finally {
            MiSnapWorkflowActivity.Result.clearResults()
            Log.d(TAG, "handleResult: SDK results cleared")
        }
    }

    private fun resolveDocumentUseCase(type: String?): MiSnapSettings.UseCase? {
        return when (type?.uppercase()) {
            "PASSPORT"         -> MiSnapSettings.UseCase.PASSPORT
            "ID_FRONT"         -> MiSnapSettings.UseCase.ID_FRONT
            "ID_BACK"          -> MiSnapSettings.UseCase.ID_BACK
            "CHECK_FRONT"      -> MiSnapSettings.UseCase.CHECK_FRONT
            "CHECK_BACK"       -> MiSnapSettings.UseCase.CHECK_BACK
            "GENERIC_DOCUMENT" -> MiSnapSettings.UseCase.GENERIC_DOCUMENT
            null, ""           -> MiSnapSettings.UseCase.PASSPORT
            else               -> null
        }
    }

    private fun permissionStatus(): JSObject {
        val status = JSObject()
        status.put("camera", getPermissionState("camera").toString().lowercase())
        status.put("audio",  getPermissionState("audio").toString().lowercase())
        status.put("nfc",    getPermissionState("nfc").toString().lowercase())
        return status
    }
}
