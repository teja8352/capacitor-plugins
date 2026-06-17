package com.mitek.capacitor

import android.content.Context
import android.util.Base64
import android.util.Log
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.miteksystems.misnap.core.MiSnapSettings
import com.miteksystems.misnap.core.MrzData
import com.miteksystems.misnap.nfc.MiSnapNfcReader
import com.miteksystems.misnap.workflow.MiSnapFinalResult
import com.miteksystems.misnap.workflow.MiSnapWorkflowActivity
import com.miteksystems.misnap.workflow.MiSnapWorkflowError
import com.miteksystems.misnap.workflow.MiSnapWorkflowStep

internal class MitekSdk(private val context: Context) {

    companion object {
        private const val TAG = "MitekSdk"
        const val ERR_LICENSE_MISSING   = "LICENSE_MISSING"
        const val ERR_LICENSE_INVALID   = "LICENSE_INVALID"
        const val ERR_PERMISSION_DENIED = "PERMISSION_DENIED"
        const val ERR_SESSION_CANCELLED = "SESSION_CANCELLED"
        const val ERR_CAMERA_ERROR      = "CAMERA_ERROR"
        const val ERR_ANALYSIS_ERROR    = "ANALYSIS_ERROR"
        const val ERR_NFC_ERROR         = "NFC_ERROR"
        const val ERR_VOICE_ERROR       = "VOICE_ERROR"
        const val ERR_SETTINGS_ERROR    = "SETTINGS_ERROR"
        const val ERR_UNKNOWN           = "UNKNOWN_ERROR"
    }

    fun buildDocumentStep(license: String, useCase: MiSnapSettings.UseCase): MiSnapWorkflowStep {
        Log.d(TAG, "buildDocumentStep useCase=$useCase")
        return MiSnapWorkflowStep(MiSnapSettings(useCase, license))
    }

    fun buildFaceStep(license: String): MiSnapWorkflowStep {
        Log.d(TAG, "buildFaceStep")
        return MiSnapWorkflowStep(MiSnapSettings(MiSnapSettings.UseCase.FACE, license))
    }

    fun buildBarcodeStep(license: String): MiSnapWorkflowStep {
        Log.d(TAG, "buildBarcodeStep")
        return MiSnapWorkflowStep(MiSnapSettings(MiSnapSettings.UseCase.BARCODE, license))
    }

    fun buildVoiceStep(license: String): MiSnapWorkflowStep {
        Log.d(TAG, "buildVoiceStep")
        return MiSnapWorkflowStep(MiSnapSettings(MiSnapSettings.UseCase.VOICE, license))
    }

    fun buildNfcStep(
        license: String,
        mrzLine1: String?,
        mrzLine2: String?,
        mrzLine3: String?,
        documentNumber: String?,
        dateOfBirth: String?,
        dateOfExpiry: String?,
        country: String?,
        documentCode: String?,
    ): MiSnapWorkflowStep {
        Log.d(TAG, "buildNfcStep")
        val settings = MiSnapSettings(MiSnapSettings.UseCase.NFC, license)
        when {
            !documentNumber.isNullOrBlank() &&
            !dateOfBirth.isNullOrBlank() &&
            !dateOfExpiry.isNullOrBlank() &&
            !country.isNullOrBlank() &&
            !documentCode.isNullOrBlank() -> {
                Log.d(TAG, "buildNfcStep: individual credentials")
                settings.nfc.mrz = MrzData(
                    documentNumber = documentNumber,
                    dateOfBirth    = dateOfBirth,
                    dateOfExpiry   = dateOfExpiry,
                    country        = country,
                    documentCode   = documentCode,
                )
            }
            !mrzLine1.isNullOrBlank() && !mrzLine2.isNullOrBlank() -> {
                Log.d(TAG, "buildNfcStep: raw MRZ lines")
                settings.nfc.mrz = MrzData(
                    documentNumber = "",
                    dateOfBirth    = "",
                    dateOfExpiry   = "",
                    country        = "",
                    documentCode   = "",
                    rawData        = listOfNotNull(mrzLine1, mrzLine2, mrzLine3).joinToString(""),
                )
            }
            else -> Log.d(TAG, "buildNfcStep: no credentials, SDK will prompt user")
        }
        return MiSnapWorkflowStep(settings)
    }

    fun parseResults(sessionType: String): JSObject {
        val results = MiSnapWorkflowActivity.Result.results
        Log.d(TAG, "parseResults sessionType=$sessionType count=${results.size}")
        if (results.isEmpty()) {
            return errorResult(sessionType, ERR_UNKNOWN, "No results returned by the SDK")
        }
        return when (val step = results.first()) {
            is MiSnapWorkflowStep.Result.Success -> {
                Log.d(TAG, "parseResults: success")
                parseFinalResult(sessionType, step.result)
            }
            is MiSnapWorkflowStep.Result.Error -> {
                Log.w(TAG, "parseResults: error ${step.errorResult}")
                parseError(sessionType, step.errorResult.error)
            }
        }
    }

    private fun parseFinalResult(sessionType: String, result: MiSnapFinalResult): JSObject {
        return when (result) {
            is MiSnapFinalResult.DocumentSession -> parseDocument(result)
            is MiSnapFinalResult.FaceSession     -> parseFace(result)
            is MiSnapFinalResult.BarcodeSession  -> parseBarcode(result)
            is MiSnapFinalResult.NfcSession      -> parseNfc(result)
            is MiSnapFinalResult.VoiceSession    -> parseVoice(result)
        }
    }

    private fun parseDocument(result: MiSnapFinalResult.DocumentSession): JSObject {
        Log.d(TAG, "parseDocument")
        val obj = JSObject()
        obj.put("success", true)
        obj.put("sessionType", "document")
        obj.put("licenseExpired", result.licenseExpired)
        obj.put("imageBase64", Base64.encodeToString(result.jpegImage, Base64.NO_WRAP))

        result.classification?.documentType?.let { type ->
            obj.put("classification", type.name)
        }

        result.extraction?.extractedData?.let { doc ->
            val extracted = JSObject()
            extracted.put("firstName",      doc.firstName)
            extracted.put("surname",        doc.surname)
            extracted.put("sex",            doc.sex)
            extracted.put("dateOfBirth",    doc.dateOfBirth)
            extracted.put("dateOfExpiry",   doc.dateOfExpiration)
            extracted.put("nationality",    doc.nationality)
            extracted.put("country",        doc.country)
            extracted.put("documentNumber", doc.docNumber)
            extracted.put("documentType",   doc.docType)
            extracted.put("optionalData1",  doc.optionalData1)
            extracted.put("optionalData2",  doc.optionalData2)
            extracted.put("rawMrz",         doc.rawData)
            obj.put("extractedData", extracted)
        }

        result.barcode?.let { obj.put("barcode", barcodeObject(it)) }
        result.rts?.let { obj.put("rts", it) }

        return obj
    }

    private fun parseFace(result: MiSnapFinalResult.FaceSession): JSObject {
        Log.d(TAG, "parseFace")
        val obj = JSObject()
        obj.put("success", true)
        obj.put("sessionType", "face")
        obj.put("licenseExpired", result.licenseExpired)
        obj.put("imageBase64", Base64.encodeToString(result.jpegImage, Base64.NO_WRAP))
        result.rts?.let { obj.put("rts", it) }
        result.aIBasedRts?.let { obj.put("aiBasedRtsBase64", Base64.encodeToString(it, Base64.NO_WRAP)) }
        return obj
    }

    private fun parseBarcode(result: MiSnapFinalResult.BarcodeSession): JSObject {
        Log.d(TAG, "parseBarcode")
        val obj = JSObject()
        obj.put("success", true)
        obj.put("sessionType", "barcode")
        obj.put("licenseExpired", result.licenseExpired)
        obj.put("imageBase64", Base64.encodeToString(result.jpegImage, Base64.NO_WRAP))
        result.barcode?.let { obj.put("barcode", barcodeObject(it)) }
        result.rts?.let { obj.put("rts", it) }
        return obj
    }

    private fun parseNfc(result: MiSnapFinalResult.NfcSession): JSObject {
        Log.d(TAG, "parseNfc chipType=${result.nfcData::class.simpleName}")
        val obj = JSObject()
        obj.put("success", true)
        obj.put("sessionType", "nfc")
        obj.put("licenseExpired", result.licenseExpired)

        val nfc = JSObject()
        when (val chip = result.nfcData) {
            is MiSnapNfcReader.ChipData.Icao -> {
                Log.d(TAG, "parseNfc: ICAO")
                nfc.put("chipType",       "ICAO")
                nfc.put("documentNumber", chip.documentNumber)
                nfc.put("documentCode",   chip.documentCode)
                nfc.put("firstName",      chip.firstName)
                nfc.put("lastName",       chip.lastName)
                nfc.put("gender",         chip.gender)
                nfc.put("nationality",    chip.nationality)
                nfc.put("issuingCountry", chip.issuingCountry)
                nfc.put("dateOfBirth",    chip.dateOfBirth)
                nfc.put("dateOfExpiry",   chip.dateOfExpiry)
                nfc.put("dateOfIssue",    chip.dateOfIssue)
                nfc.put("personalNumber", chip.personalNumber)
                nfc.put("placeOfBirth",   chip.placeOfBirth)
                chip.photo?.let { nfc.put("faceImageBase64", Base64.encodeToString(it, Base64.NO_WRAP)) }
                nfc.put("chipAuthInfo",  chip.authenticationData.chipAuthInfo)
                nfc.put("activeAuthInfo", chip.authenticationData.activeAuthInfo?.toString())
            }
            is MiSnapNfcReader.ChipData.EuDl -> {
                Log.d(TAG, "parseNfc: EU_DL")
                nfc.put("chipType",                  "EU_DL")
                nfc.put("documentNumber",            chip.documentNumber)
                nfc.put("documentCode",              chip.documentCode)
                nfc.put("firstName",                 chip.firstName)
                nfc.put("lastName",                  chip.lastName)
                nfc.put("gender",                    chip.gender)
                nfc.put("nationality",               chip.nationality)
                nfc.put("issuingCountry",            chip.issuingCountry)
                nfc.put("dateOfBirth",               chip.dateOfBirth)
                nfc.put("dateOfExpiry",              chip.dateOfExpiry)
                nfc.put("dateOfIssue",               chip.dateOfIssue)
                nfc.put("personalNumber",            chip.personalNumber)
                nfc.put("placeOfBirth",              chip.placeOfBirth)
                nfc.put("permanentPlaceOfResidence", chip.permanentPlaceOfResidence)
                chip.photo?.let { nfc.put("faceImageBase64", Base64.encodeToString(it, Base64.NO_WRAP)) }
                chip.vehicleCategories?.let { categories ->
                    val arr = JSArray()
                    categories.forEach { arr.put(it.toString()) }
                    nfc.put("vehicleCategories", arr)
                }
                nfc.put("chipAuthInfo",  chip.authenticationData.chipAuthInfo)
                nfc.put("activeAuthInfo", chip.authenticationData.activeAuthInfo?.toString())
            }
        }
        obj.put("nfcData", nfc)
        return obj
    }

    private fun parseVoice(result: MiSnapFinalResult.VoiceSession): JSObject {
        Log.d(TAG, "parseVoice samples=${result.voiceSamples.size}")
        val obj = JSObject()
        obj.put("success", true)
        obj.put("sessionType", "voice")
        obj.put("licenseExpired", result.licenseExpired)
        obj.put("phrase", result.phrase)
        obj.put("voiceSampleCount", result.voiceSamples.size)
        return obj
    }

    private fun parseError(sessionType: String, error: MiSnapWorkflowError): JSObject {
        val (code, message) = when (error) {
            is MiSnapWorkflowError.Cancelled                  -> ERR_SESSION_CANCELLED to "Session cancelled by user"
            is MiSnapWorkflowError.Camera                     -> ERR_CAMERA_ERROR      to "Camera error"
            is MiSnapWorkflowError.License                    -> ERR_LICENSE_INVALID   to "License error: ${error.reason}"
            is MiSnapWorkflowError.Analysis                   -> ERR_ANALYSIS_ERROR    to "Frame analysis failed"
            is MiSnapWorkflowError.Permission                 -> ERR_PERMISSION_DENIED to "Required permission not granted"
            is MiSnapWorkflowError.SettingState               -> ERR_SETTINGS_ERROR    to "Invalid SDK settings"
            is MiSnapWorkflowError.Nfc                        -> ERR_NFC_ERROR         to "NFC read failed"
            is MiSnapWorkflowError.Voice                      -> ERR_VOICE_ERROR       to "Voice recording failed"
            is MiSnapWorkflowError.CombinedWorkflow           -> ERR_SETTINGS_ERROR    to "Invalid combined workflow"
            is MiSnapWorkflowError.CombinedWorkflowSkippedStep -> ERR_UNKNOWN          to "Workflow step skipped"
            else                                              -> ERR_UNKNOWN           to "Unexpected error: ${error::class.simpleName}"
        }
        Log.w(TAG, "parseError [$code] $message")
        return errorResult(sessionType, code, message)
    }

    private fun barcodeObject(barcode: com.miteksystems.misnap.core.Barcode): JSObject {
        val js = JSObject()
        js.put("encodedBarcode", barcode.encodedBarcode)
        js.put("type", barcode.type?.name)
        js.put("isVds", barcode.isVds)
        return js
    }

    private fun errorResult(sessionType: String, code: String, message: String): JSObject {
        val obj = JSObject()
        obj.put("success", false)
        obj.put("sessionType", sessionType)
        obj.put("errorCode", code)
        obj.put("errorMessage", message)
        return obj
    }
}
