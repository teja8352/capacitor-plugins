package com.mitek.capacitor

import android.content.Context
import android.util.Base64
import android.util.Log
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.miteksystems.misnap.core.Barcode
import com.miteksystems.misnap.core.MiSnapSettings
import com.miteksystems.misnap.core.MrzData
import com.miteksystems.misnap.core.Mrz1Line
import com.miteksystems.misnap.nfc.MiSnapNfcReader
import com.miteksystems.misnap.workflow.MiSnapFinalResult
import com.miteksystems.misnap.workflow.MiSnapWorkflowActivity
import com.miteksystems.misnap.workflow.MiSnapWorkflowError
import com.miteksystems.misnap.workflow.MiSnapWorkflowStep

class MitekSdk(private val context: Context) {

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
        val settings = MiSnapSettings(useCase, license)
        return MiSnapWorkflowStep(settings)
    }

    fun buildFaceStep(license: String): MiSnapWorkflowStep {
        val settings = MiSnapSettings(MiSnapSettings.UseCase.FACE, license)
        return MiSnapWorkflowStep(settings)
    }

    fun buildBarcodeStep(license: String): MiSnapWorkflowStep {
        val settings = MiSnapSettings(MiSnapSettings.UseCase.BARCODE, license)
        return MiSnapWorkflowStep(settings)
    }

    fun buildVoiceStep(license: String): MiSnapWorkflowStep {
        val settings = MiSnapSettings(MiSnapSettings.UseCase.VOICE, license)
        return MiSnapWorkflowStep(settings)
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
        val settings = MiSnapSettings(MiSnapSettings.UseCase.NFC, license)

        when {
            !documentNumber.isNullOrBlank() && !dateOfBirth.isNullOrBlank() && !dateOfExpiry.isNullOrBlank() -> {
                settings.nfc.mrz = MrzData(
                    documentNumber = documentNumber,
                    dateOfBirth    = dateOfBirth,
                    dateOfExpiry   = dateOfExpiry,
                    country        = country ?: "",
                    documentCode   = documentCode ?: "P",
                )
            }
            !mrzLine1.isNullOrBlank() && !mrzLine2.isNullOrBlank() -> {
                settings.nfc.mrz = mrzFromLines(mrzLine1, mrzLine2, mrzLine3)
            }
        }

        return MiSnapWorkflowStep(settings)
    }

    fun parseResults(sessionType: String): JSObject {
        val steps = MiSnapWorkflowActivity.Result.results
        if (steps.isNullOrEmpty()) {
            return errorResult(sessionType, ERR_UNKNOWN, "No results from MiSnapWorkflowActivity")
        }
        val step = steps.first()
        return when (step) {
            is MiSnapWorkflowStep.Result.Success -> parseFinalResult(sessionType, step)
            is MiSnapWorkflowStep.Result.Error   -> parseError(sessionType, step)
        }
    }

    private fun mrzFromLines(line1: String, line2: String, line3: String?): com.miteksystems.misnap.core.Mrz {
        return if (line2.length >= 26) {
            val docNum      = line2.substring(0, 9)
            val nationality = if (line2.length >= 13) line2.substring(10, 13) else ""
            val dob         = line2.substring(13, 19)
            val doe         = line2.substring(20, 26)
            val ctry        = if (line1.length >= 5) line1.substring(2, 5) else ""
            val dc          = if (line1.isNotEmpty()) line1.substring(0, 1) else "P"
            val raw         = if (line3 != null) "$line1$line2$line3" else "$line1$line2"
            MrzData(
                documentNumber = docNum,
                dateOfBirth    = dob,
                dateOfExpiry   = doe,
                country        = ctry,
                documentCode   = dc,
                nationality    = nationality,
                rawData        = raw,
            )
        } else {
            Mrz1Line(line1)
        }
    }

    private fun parseFinalResult(sessionType: String, step: MiSnapWorkflowStep.Result.Success): JSObject {
        return when (val r = step.result) {
            is MiSnapFinalResult.DocumentSession -> parseDocument(sessionType, r)
            is MiSnapFinalResult.FaceSession     -> parseFace(sessionType, r)
            is MiSnapFinalResult.BarcodeSession  -> parseBarcode(sessionType, r)
            is MiSnapFinalResult.NfcSession      -> parseNfc(sessionType, r)
            is MiSnapFinalResult.VoiceSession    -> parseVoice(sessionType, r)
        }
    }

    private fun parseDocument(sessionType: String, r: MiSnapFinalResult.DocumentSession): JSObject {
        val obj = JSObject()
        obj.put("success",        true)
        obj.put("sessionType",    sessionType)
        obj.put("imageBase64",    Base64.encodeToString(r.jpegImage, Base64.NO_WRAP))
        obj.put("classification", r.classification?.documentType?.name)
        obj.put("rts",            r.rts)
        obj.put("licenseExpired", r.licenseExpired)

        val extracted = r.extraction?.extractedData
        if (extracted != null) {
            val data = JSObject()
            data.put("firstName",      extracted.firstName)
            data.put("surname",        extracted.surname)
            data.put("sex",            extracted.sex)
            data.put("dateOfBirth",    extracted.dateOfBirth)
            data.put("dateOfExpiry",   extracted.dateOfExpiration)
            data.put("nationality",    extracted.nationality)
            data.put("country",        extracted.country)
            data.put("documentNumber", extracted.docNumber)
            data.put("documentType",   extracted.docType)
            data.put("optionalData1",  extracted.optionalData1)
            data.put("optionalData2",  extracted.optionalData2)
            data.put("rawMrz",         extracted.rawData)
            obj.put("extractedData", data)
        }

        val barcode = r.barcode
        if (barcode != null) {
            obj.put("barcode", barcodeObject(barcode))
        }

        return obj
    }

    private fun parseFace(sessionType: String, r: MiSnapFinalResult.FaceSession): JSObject {
        val obj = JSObject()
        obj.put("success",        true)
        obj.put("sessionType",    sessionType)
        obj.put("imageBase64",    Base64.encodeToString(r.jpegImage, Base64.NO_WRAP))
        obj.put("rts",            r.rts)
        obj.put("licenseExpired", r.licenseExpired)
        val aiRts = r.aIBasedRts
        if (aiRts != null) {
            obj.put("aiBasedRtsBase64", Base64.encodeToString(aiRts, Base64.NO_WRAP))
        }
        return obj
    }

    private fun parseBarcode(sessionType: String, r: MiSnapFinalResult.BarcodeSession): JSObject {
        val obj = JSObject()
        obj.put("success",        true)
        obj.put("sessionType",    sessionType)
        obj.put("imageBase64",    Base64.encodeToString(r.jpegImage, Base64.NO_WRAP))
        obj.put("rts",            r.rts)
        obj.put("licenseExpired", r.licenseExpired)
        val barcode = r.barcode
        if (barcode != null) {
            obj.put("barcode", barcodeObject(barcode))
        }
        return obj
    }

    private fun parseNfc(sessionType: String, r: MiSnapFinalResult.NfcSession): JSObject {
        val obj = JSObject()
        obj.put("success",        true)
        obj.put("sessionType",    sessionType)
        obj.put("licenseExpired", r.licenseExpired)

        val nfc = JSObject()
        when (val chip = r.nfcData) {
            is MiSnapNfcReader.ChipData.Icao -> {
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
                nfc.put("chipAuthInfo",   chip.authenticationData.chipAuthInfo)
                nfc.put("activeAuthInfo", chip.authenticationData.activeAuthInfo)
                val photo = chip.photo
                if (photo != null) {
                    nfc.put("faceImageBase64", Base64.encodeToString(photo, Base64.NO_WRAP))
                }
            }
            is MiSnapNfcReader.ChipData.EuDl -> {
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
                nfc.put("chipAuthInfo",              chip.authenticationData.chipAuthInfo)
                nfc.put("activeAuthInfo",            chip.authenticationData.activeAuthInfo)
                val photo = chip.photo
                if (photo != null) {
                    nfc.put("faceImageBase64", Base64.encodeToString(photo, Base64.NO_WRAP))
                }
                val cats = chip.vehicleCategories
                if (!cats.isNullOrEmpty()) {
                    val arr = JSArray()
                    cats.forEach { arr.put(it) }
                    nfc.put("vehicleCategories", arr)
                }
            }
        }
        obj.put("nfcData", nfc)
        return obj
    }

    private fun parseVoice(sessionType: String, r: MiSnapFinalResult.VoiceSession): JSObject {
        val obj = JSObject()
        obj.put("success",          true)
        obj.put("sessionType",      sessionType)
        obj.put("voiceSampleCount", r.voiceSamples.size)
        obj.put("phrase",           r.phrase)
        obj.put("licenseExpired",   r.licenseExpired)
        val firstRts = r.rts.firstOrNull { it != null }
        if (firstRts != null) {
            obj.put("rts", firstRts)
        }
        return obj
    }

    private fun parseError(sessionType: String, step: MiSnapWorkflowStep.Result.Error): JSObject {
        val code = when (step.errorResult.error) {
            is MiSnapWorkflowError.Cancelled    -> ERR_SESSION_CANCELLED
            is MiSnapWorkflowError.Camera       -> ERR_CAMERA_ERROR
            is MiSnapWorkflowError.Analysis     -> ERR_ANALYSIS_ERROR
            is MiSnapWorkflowError.Nfc          -> ERR_NFC_ERROR
            is MiSnapWorkflowError.Voice        -> ERR_VOICE_ERROR
            is MiSnapWorkflowError.SettingState -> ERR_SETTINGS_ERROR
            is MiSnapWorkflowError.License      -> ERR_LICENSE_INVALID
            is MiSnapWorkflowError.Permission   -> ERR_PERMISSION_DENIED
            else                                -> ERR_UNKNOWN
        }
        return errorResult(sessionType, code, step.errorResult.error.toString())
    }

    private fun barcodeObject(barcode: Barcode): JSObject {
        val obj = JSObject()
        obj.put("encodedBarcode", barcode.encodedBarcode)
        obj.put("type",           barcode.type?.name)
        obj.put("isVds",          barcode.isVds)
        return obj
    }

    private fun errorResult(sessionType: String, code: String, message: String): JSObject {
        Log.w(TAG, "session=$sessionType code=$code msg=$message")
        val obj = JSObject()
        obj.put("success",      false)
        obj.put("sessionType",  sessionType)
        obj.put("errorCode",    code)
        obj.put("errorMessage", message)
        return obj
    }
}
