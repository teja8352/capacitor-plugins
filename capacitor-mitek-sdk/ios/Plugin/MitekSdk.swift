import Foundation
import AVFoundation
import CoreNFC
import MiSnapCore
import MiSnap
import MiSnapUX
import MiSnapFacialCapture
import MiSnapFacialCaptureUX
import MiSnapVoiceCapture
import MiSnapVoiceCaptureUX
import MiSnapNFC
import MiSnapNFCUX

// MARK: - Error codes (mirrors Android constants)

enum MitekError: String {
    case licenseMissing   = "LICENSE_MISSING"
    case licenseInvalid   = "LICENSE_INVALID"
    case permissionDenied = "PERMISSION_DENIED"
    case cancelled        = "SESSION_CANCELLED"
    case cameraError      = "CAMERA_ERROR"
    case analysisError    = "ANALYSIS_ERROR"
    case nfcError         = "NFC_ERROR"
    case voiceError       = "VOICE_ERROR"
    case settingsError    = "SETTINGS_ERROR"
    case unknown          = "UNKNOWN_ERROR"
}

// MARK: - License validation

final class MitekLicenseValidator {

    /// Sets the license globally and returns nil if valid, or a human-readable error string.
    static func validate(_ license: String) -> String? {
        MiSnapLicenseManager.shared.setLicenseKey(license)
        let status = MiSnapLicenseManager.shared.status

        switch status {
        case .valid:
            return nil
        case .expired:
            return nil
        case .notValid:
            return "License failed validation"
        case .notValidAppId:
            return "License is not valid for this application bundle ID"
        case .platformNotSupported:
            return "License does not support this platform"
        case .disabled:
            return "License is disabled — contact Mitek to renew"
        @unknown default:
            return "License check returned an unrecognised status"
        }
    }

    static var isExpired: Bool {
        return MiSnapLicenseManager.shared.status == .expired
    }
}

// MARK: - Permission helpers

enum MitekPermission: String {
    case camera, audio, nfc

    var status: String {
        switch self {
        case .camera:
            return avStatus(for: .video)
        case .audio:
            return avStatus(for: .audio)
        case .nfc:
            if #available(iOS 13.0, *) {
                return NFCTagReaderSession.readingAvailable ? "granted" : "unavailable"
            }
            return "unavailable"
        }
    }

    private func avStatus(for mediaType: AVMediaType) -> String {
        switch AVCaptureDevice.authorizationStatus(for: mediaType) {
        case .authorized:          return "granted"
        case .denied, .restricted: return "denied"
        case .notDetermined:       return "prompt"
        @unknown default:          return "prompt"
        }
    }
}

// MARK: - Result builders

struct MitekResultBuilder {

    static func errorResult(sessionType: String, code: MitekError, message: String) -> [String: Any] {
        MiSnapLogger.log("[\(sessionType)] \(code.rawValue): \(message)", level: .warning)
        return [
            "success":      false,
            "sessionType":  sessionType,
            "errorCode":    code.rawValue,
            "errorMessage": message,
        ]
    }

    static func licenseValidResult() -> [String: Any] {
        return ["isValid": true]
    }

    static func licenseInvalidResult(message: String) -> [String: Any] {
        return [
            "isValid":      false,
            "errorCode":    MitekError.licenseInvalid.rawValue,
            "errorMessage": message,
        ]
    }

    static func documentResult(_ result: MiSnapResult) -> [String: Any] {
        var dict: [String: Any] = [
            "success":        true,
            "sessionType":    "document",
            "licenseExpired": MitekLicenseValidator.isExpired,
        ]

        if let encoded = result.encodedImage {
            dict["imageBase64"] = encoded
        }
        if let rts = result.rts {
            dict["rts"] = rts
        }
        dict["classification"] = result.classification.documentTypeString

        if let extraction = result.extraction {
            var extracted: [String: Any] = [:]
            extracted["firstName"]      = extraction.givenName
            extracted["surname"]        = extraction.surname
            extracted["sex"]            = extraction.sex
            extracted["dateOfBirth"]    = extraction.dateOfBirth
            extracted["dateOfExpiry"]   = extraction.expirationDate
            extracted["nationality"]    = extraction.nationality
            extracted["country"]        = extraction.country
            extracted["documentNumber"] = extraction.documentNumber
            extracted["rawMrz"]         = extraction.mrzString
            dict["extractedData"]       = extracted.compactMapValues { $0 }

            if let barcodeStr = extraction.barcodeString, !barcodeStr.isEmpty {
                dict["barcode"] = barcodeDict(from: extraction)
            }
        }

        return dict
    }

    static func faceResult(_ result: MiSnapFacialCaptureResult) -> [String: Any] {
        var dict: [String: Any] = [
            "success":        true,
            "sessionType":    "face",
            "licenseExpired": MitekLicenseValidator.isExpired,
        ]
        if let encoded = result.encodedImage {
            dict["imageBase64"] = encoded
        }
        if let rts = result.rts {
            dict["rts"] = rts
        }
        if let aiRts = result.aiBasedRts {
            dict["aiBasedRtsBase64"] = aiRts
        }
        return dict
    }

    static func barcodeResult(_ result: MiSnapResult) -> [String: Any] {
        var dict: [String: Any] = [
            "success":        true,
            "sessionType":    "barcode",
            "licenseExpired": MitekLicenseValidator.isExpired,
        ]
        if let encoded = result.encodedImage {
            dict["imageBase64"] = encoded
        }
        if let rts = result.rts {
            dict["rts"] = rts
        }
        if let extraction = result.extraction, let barcodeStr = extraction.barcodeString, !barcodeStr.isEmpty {
            dict["barcode"] = barcodeDict(from: extraction)
        }
        return dict
    }

    static func voiceResult(_ results: [MiSnapVoiceCaptureResult], phrase: String?) -> [String: Any] {
        var dict: [String: Any] = [
            "success":          true,
            "sessionType":      "voice",
            "voiceSampleCount": results.count,
            "licenseExpired":   MitekLicenseValidator.isExpired,
        ]
        if let phrase = phrase {
            dict["phrase"] = phrase
        }
        if let rts = results.first?.rts {
            dict["rts"] = rts
        }
        return dict
    }

    static func nfcResult(_ nfcData: [String: Any]) -> [String: Any] {
        var dict: [String: Any] = [
            "success":     true,
            "sessionType": "nfc",
        ]
        var chip: [String: Any] = [:]
        chip["documentNumber"] = nfcData["documentNumber"]
        chip["firstName"]      = nfcData["firstName"]
        chip["lastName"]       = nfcData["lastName"]
        chip["gender"]         = nfcData["gender"]
        chip["nationality"]    = nfcData["nationality"]
        chip["issuingCountry"] = nfcData["issuingCountry"]
        chip["dateOfBirth"]    = nfcData["dateOfBirth"]
        chip["dateOfExpiry"]   = nfcData["dateOfExpiry"]
        chip["dateOfIssue"]    = nfcData["dateOfIssue"]
        chip["personalNumber"] = nfcData["personalNumber"]
        chip["placeOfBirth"]   = nfcData["placeOfBirth"]

        if let photo = nfcData["faceImage"] as? Data {
            chip["faceImageBase64"] = photo.base64EncodedString()
        }
        dict["nfcData"] = chip.compactMapValues { $0 }
        return dict
    }

    private static func barcodeDict(from extraction: MiSnapScienceExtractionResult) -> [String: Any] {
        var dict: [String: Any] = [:]
        dict["encodedBarcode"] = extraction.barcodeString
        dict["type"]           = String(describing: extraction.barcodeType)
        dict["isVds"]          = extraction.vds != nil
        return dict.compactMapValues { $0 }
    }
}

// MARK: - Logging shim

enum MiSnapLogLevel { case debug, info, warning, error }

struct MiSnapLogger {
    static func log(_ message: String, level: MiSnapLogLevel = .info) {
        let tag = "MitekSdk"
        switch level {
        case .debug:   NSLog("[%@][DEBUG] %@", tag, message)
        case .info:    NSLog("[%@][INFO]  %@", tag, message)
        case .warning: NSLog("[%@][WARN]  %@", tag, message)
        case .error:   NSLog("[%@][ERROR] %@", tag, message)
        }
    }
}
