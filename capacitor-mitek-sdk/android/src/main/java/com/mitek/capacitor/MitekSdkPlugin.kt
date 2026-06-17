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
        const val TAG            = "MitekSdkPlugin"
        const val KEY_LICENSE    = "license"
        const val KEY_DOC_TYPE   = "documentType"
        const val KEY_MRZ_LINE1  = "mrzLine1"
        const val KEY_MRZ_LINE2  = "mrzLine2"
        const val KEY_MRZ_LINE3  = "mrzLine3"
        const val KEY_DOC_NUMBER = "documentNumber"
        const val KEY_DOB        = "dateOfBirth"
        const val KEY_DOE        = "dateOfExpiry"
        const val KEY_COUNTRY    = "country"
        const val KEY_DOC_CODE   = "documentCode"
        const val KEY_PERMISSIONS = "permissions"
    }

    private lateinit var sdk: MitekSdk

    override fun load() {
        sdk = MitekSdk(context)
    }

    @PluginMethod
    fun startDocumentSession(call: PluginCall) {
        val license = call.getString(KEY_LICENSE)
        if (license.isNullOrBlank()) {
            call.reject("'license' is required", MitekSdk.ERR_LICENSE_MISSING)
            return
        }
        val useCase = resolveDocumentUseCase(call.getString(KEY_DOC_TYPE))
        if (useCase == null) {
            call.reject("Unknown documentType. Valid: PASSPORT, ID_FRONT, ID_BACK, CHECK_FRONT, CHECK_BACK, GENERIC_DOCUMENT", MitekSdk.ERR_SETTINGS_ERROR)
            return
        }
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            requestPermissionForAlias("camera", call, "onCameraPermission")
            return
        }
        launchDocument(call, license, useCase)
    }

    @PluginMethod
    fun startFaceSession(call: PluginCall) {
        val license = call.getString(KEY_LICENSE)
        if (license.isNullOrBlank()) {
            call.reject("'license' is required", MitekSdk.ERR_LICENSE_MISSING)
            return
        }
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            requestPermissionForAlias("camera", call, "onCameraPermission")
            return
        }
        launchFace(call, license)
    }

    @PluginMethod
    fun startBarcodeSession(call: PluginCall) {
        val license = call.getString(KEY_LICENSE)
        if (license.isNullOrBlank()) {
            call.reject("'license' is required", MitekSdk.ERR_LICENSE_MISSING)
            return
        }
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            requestPermissionForAlias("camera", call, "onCameraPermission")
            return
        }
        launchBarcode(call, license)
    }

    @PluginMethod
    fun startVoiceSession(call: PluginCall) {
        val license = call.getString(KEY_LICENSE)
        if (license.isNullOrBlank()) {
            call.reject("'license' is required", MitekSdk.ERR_LICENSE_MISSING)
            return
        }
        if (getPermissionState("audio") != PermissionState.GRANTED) {
            requestPermissionForAlias("audio", call, "onAudioPermission")
            return
        }
        launchVoice(call, license)
    }

    @PluginMethod
    fun startNfcSession(call: PluginCall) {
        val license = call.getString(KEY_LICENSE)
        if (license.isNullOrBlank()) {
            call.reject("'license' is required", MitekSdk.ERR_LICENSE_MISSING)
            return
        }
        if (getPermissionState("nfc") != PermissionState.GRANTED) {
            requestPermissionForAlias("nfc", call, "onNfcPermission")
            return
        }
        launchNfc(call, license)
    }

    @PluginMethod
    fun checkPermissions(call: PluginCall) {
        call.resolve(permissionStatus())
    }

    @PluginMethod
    fun requestPermissions(call: PluginCall) {
        val requested = call.getArray(KEY_PERMISSIONS)?.toList<String>()
        val aliases = if (requested.isNullOrEmpty()) {
            arrayOf("camera", "audio", "nfc")
        } else {
            requested.toTypedArray()
        }
        requestPermissionForAliases(aliases, call, "onAllPermissions")
    }

    @PermissionCallback
    private fun onCameraPermission(call: PluginCall) {
        if (getPermissionState("camera") != PermissionState.GRANTED) {
            call.reject("Camera permission denied", MitekSdk.ERR_PERMISSION_DENIED)
            return
        }
        reLaunch(call)
    }

    @PermissionCallback
    private fun onAudioPermission(call: PluginCall) {
        if (getPermissionState("audio") != PermissionState.GRANTED) {
            call.reject("Microphone permission denied", MitekSdk.ERR_PERMISSION_DENIED)
            return
        }
        reLaunch(call)
    }

    @PermissionCallback
    private fun onNfcPermission(call: PluginCall) {
        if (getPermissionState("nfc") != PermissionState.GRANTED) {
            call.reject("NFC permission denied", MitekSdk.ERR_PERMISSION_DENIED)
            return
        }
        reLaunch(call)
    }

    @PermissionCallback
    private fun onAllPermissions(call: PluginCall) {
        call.resolve(permissionStatus())
    }

    @ActivityCallback
    private fun onDocumentResult(call: PluginCall, result: ActivityResult) {
        handleResult(call, "document")
    }

    @ActivityCallback
    private fun onFaceResult(call: PluginCall, result: ActivityResult) {
        handleResult(call, "face")
    }

    @ActivityCallback
    private fun onBarcodeResult(call: PluginCall, result: ActivityResult) {
        handleResult(call, "barcode")
    }

    @ActivityCallback
    private fun onVoiceResult(call: PluginCall, result: ActivityResult) {
        handleResult(call, "voice")
    }

    @ActivityCallback
    private fun onNfcResult(call: PluginCall, result: ActivityResult) {
        handleResult(call, "nfc")
    }

    private fun reLaunch(call: PluginCall) {
        val method = call.methodName ?: run {
            call.reject("Internal error: missing method name", MitekSdk.ERR_UNKNOWN)
            return
        }
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
            else -> call.reject("Unknown session method", MitekSdk.ERR_UNKNOWN)
        }
    }

    private fun launchDocument(call: PluginCall, license: String, useCase: MiSnapSettings.UseCase) {
        try {
            val step = sdk.buildDocumentStep(license, useCase)
            startActivityForResult(call, MiSnapWorkflowActivity.buildIntent(context, step), "onDocumentResult")
        } catch (e: Exception) {
            Log.e(TAG, "launchDocument", e)
            call.reject("Failed to start document session: ${e.message}", MitekSdk.ERR_UNKNOWN, e)
        }
    }

    private fun launchFace(call: PluginCall, license: String) {
        try {
            val step = sdk.buildFaceStep(license)
            startActivityForResult(call, MiSnapWorkflowActivity.buildIntent(context, step), "onFaceResult")
        } catch (e: Exception) {
            Log.e(TAG, "launchFace", e)
            call.reject("Failed to start face session: ${e.message}", MitekSdk.ERR_UNKNOWN, e)
        }
    }

    private fun launchBarcode(call: PluginCall, license: String) {
        try {
            val step = sdk.buildBarcodeStep(license)
            startActivityForResult(call, MiSnapWorkflowActivity.buildIntent(context, step), "onBarcodeResult")
        } catch (e: Exception) {
            Log.e(TAG, "launchBarcode", e)
            call.reject("Failed to start barcode session: ${e.message}", MitekSdk.ERR_UNKNOWN, e)
        }
    }

    private fun launchVoice(call: PluginCall, license: String) {
        try {
            val step = sdk.buildVoiceStep(license)
            startActivityForResult(call, MiSnapWorkflowActivity.buildIntent(context, step), "onVoiceResult")
        } catch (e: Exception) {
            Log.e(TAG, "launchVoice", e)
            call.reject("Failed to start voice session: ${e.message}", MitekSdk.ERR_UNKNOWN, e)
        }
    }

    private fun launchNfc(call: PluginCall, license: String) {
        try {
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
            startActivityForResult(call, MiSnapWorkflowActivity.buildIntent(context, step), "onNfcResult")
        } catch (e: Exception) {
            Log.e(TAG, "launchNfc", e)
            call.reject("Failed to start NFC session: ${e.message}", MitekSdk.ERR_UNKNOWN, e)
        }
    }

    private fun handleResult(call: PluginCall, sessionType: String) {
        try {
            val result = sdk.parseResults(sessionType)
            call.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "handleResult sessionType=$sessionType", e)
            val err = JSObject()
            err.put("success", false)
            err.put("sessionType", sessionType)
            err.put("errorCode", MitekSdk.ERR_UNKNOWN)
            err.put("errorMessage", "Unexpected error: ${e.message}")
            call.resolve(err)
        } finally {
            MiSnapWorkflowActivity.Result.clearResults()
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
