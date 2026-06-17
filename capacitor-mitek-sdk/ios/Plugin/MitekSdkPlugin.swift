import Foundation
import AVFoundation
import Capacitor
import MiSnapCore
import MiSnap
import MiSnapUX
import MiSnapFacialCapture
import MiSnapFacialCaptureUX
import MiSnapVoiceCapture
import MiSnapVoiceCaptureUX
import MiSnapNFC
import MiSnapNFCUX

// MARK: - Plugin

@objc(MitekSdkPlugin)
public class MitekSdkPlugin: CAPPlugin, CAPBridgedPlugin {

    public let identifier = "MitekSdkPlugin"
    public let jsName = "MitekSdk"

    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "validateLicense",      returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startDocumentSession", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startFaceSession",     returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startBarcodeSession",  returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startVoiceSession",    returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startNfcSession",      returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkPermissions",     returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions",   returnType: CAPPluginReturnPromise),
    ]

    // Retain presented VCs so ARC doesn't release them mid-session
    private var documentVC: MiSnapViewController?
    private var faceVC: MiSnapFacialCaptureViewController?
    private var barcodeVC: MiSnapViewController?
    private var voiceVC: MiSnapVoiceCaptureViewController?
    private var nfcVC: MiSnapNFCViewController?

    // Phrase captured from voice delegate before success callback fires
    private var capturedVoicePhrase: String?

    // MARK: - validateLicense

    @objc func validateLicense(_ call: CAPPluginCall) {
        guard let license = call.getString("license"), !license.isEmpty else {
            call.reject("'license' is required", MitekError.licenseMissing.rawValue)
            return
        }
        MiSnapLogger.log("validateLicense called")
        if let error = MitekLicenseValidator.validate(license) {
            call.resolve(MitekResultBuilder.licenseInvalidResult(message: error))
        } else {
            call.resolve(MitekResultBuilder.licenseValidResult())
        }
    }

    // MARK: - startDocumentSession

    @objc func startDocumentSession(_ call: CAPPluginCall) {
        guard validatedLicense(call) != nil else { return }
        guard cameraAuthorized(call) else { return }

        let docType = call.getString("documentType") ?? "PASSPORT"
        guard let useCase = resolveDocumentUseCase(docType) else {
            call.reject(
                "Unknown documentType '\(docType)'. Valid: PASSPORT, ID_FRONT, ID_BACK, CHECK_FRONT, CHECK_BACK, GENERIC_DOCUMENT",
                MitekError.settingsError.rawValue
            )
            return
        }

        MiSnapLogger.log("startDocumentSession useCase=\(useCase)")

        let configuration = MiSnapConfiguration(for: useCase)

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            let vc = MiSnapViewController(with: configuration, delegate: self)
            self.documentVC = vc
            self.bridge?.viewController?.present(vc, animated: true)
        }

        call.keepAlive = true
        objc_setAssociatedObject(self, &AssociatedKeys.documentCall, call, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    }

    // MARK: - startFaceSession

    @objc func startFaceSession(_ call: CAPPluginCall) {
        guard validatedLicense(call) != nil else { return }
        guard cameraAuthorized(call) else { return }

        MiSnapLogger.log("startFaceSession")

        let configuration = MiSnapFacialCaptureConfiguration()

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            let vc = MiSnapFacialCaptureViewController(with: configuration, delegate: self)
            self.faceVC = vc
            self.bridge?.viewController?.present(vc, animated: true)
        }

        call.keepAlive = true
        objc_setAssociatedObject(self, &AssociatedKeys.faceCall, call, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    }

    // MARK: - startBarcodeSession

    @objc func startBarcodeSession(_ call: CAPPluginCall) {
        guard validatedLicense(call) != nil else { return }
        guard cameraAuthorized(call) else { return }

        MiSnapLogger.log("startBarcodeSession")

        let configuration = MiSnapConfiguration(for: .anyId)
            .withCustomParameters { params in
                params.science.barcodeRequired = true
            }

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            let vc = MiSnapViewController(with: configuration, delegate: self)
            self.barcodeVC = vc
            self.bridge?.viewController?.present(vc, animated: true)
        }

        call.keepAlive = true
        objc_setAssociatedObject(self, &AssociatedKeys.barcodeCall, call, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    }

    // MARK: - startVoiceSession

    @objc func startVoiceSession(_ call: CAPPluginCall) {
        guard validatedLicense(call) != nil else { return }
        guard microphoneAuthorized(call) else { return }

        MiSnapLogger.log("startVoiceSession")

        let configuration = MiSnapVoiceCaptureConfiguration(for: .enrollment)

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            let vc = MiSnapVoiceCaptureViewController(with: configuration, delegate: self)
            self.voiceVC = vc
            self.bridge?.viewController?.present(vc, animated: true)
        }

        call.keepAlive = true
        objc_setAssociatedObject(self, &AssociatedKeys.voiceCall, call, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    }

    // MARK: - startNfcSession

    @objc func startNfcSession(_ call: CAPPluginCall) {
        guard validatedLicense(call) != nil else { return }

        MiSnapLogger.log("startNfcSession")

        let configuration = MiSnapNFCConfiguration()
            .withInputs { inputs in
                if let docNumber = call.getString("documentNumber"),
                   let dob      = call.getString("dateOfBirth"),
                   let doe      = call.getString("dateOfExpiry") {
                    inputs.documentNumber = docNumber
                    inputs.dateOfBirth    = dob
                    inputs.dateOfExpiry   = doe
                    inputs.chipLocation   = MiSnapNFCChipLocator.chipLocation(
                        mrzString:      "",
                        documentNumber: docNumber,
                        dateOfBirth:    dob,
                        dateOfExpiry:   doe
                    )
                } else if let mrzLine1 = call.getString("mrzLine1"),
                          let mrzLine2 = call.getString("mrzLine2") {
                    let mrz = mrzLine1 + mrzLine2 + (call.getString("mrzLine3") ?? "")
                    // Extract BAC fields from TD3 MRZ line 2 (positions are 0-indexed)
                    let docNum = mrzLine2.count >= 9
                        ? String(mrzLine2.prefix(9)).replacingOccurrences(of: "<", with: "")
                        : ""
                    let dobStr = mrzLine2.count >= 19
                        ? String(mrzLine2[mrzLine2.index(mrzLine2.startIndex, offsetBy: 13)..<mrzLine2.index(mrzLine2.startIndex, offsetBy: 19)])
                        : ""
                    let doeStr = mrzLine2.count >= 26
                        ? String(mrzLine2[mrzLine2.index(mrzLine2.startIndex, offsetBy: 20)..<mrzLine2.index(mrzLine2.startIndex, offsetBy: 26)])
                        : ""
                    inputs.mrzString    = mrz
                    inputs.chipLocation = MiSnapNFCChipLocator.chipLocation(
                        mrzString:      mrz,
                        documentNumber: docNum,
                        dateOfBirth:    dobStr,
                        dateOfExpiry:   doeStr
                    )
                }
            }

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            let vc = MiSnapNFCViewController(with: configuration, delegate: self)
            self.nfcVC = vc
            self.bridge?.viewController?.present(vc, animated: true)
        }

        call.keepAlive = true
        objc_setAssociatedObject(self, &AssociatedKeys.nfcCall, call, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    }

    // MARK: - checkPermissions / requestPermissions

    @objc public override func checkPermissions(_ call: CAPPluginCall) {
        call.resolve([
            "camera": MitekPermission.camera.status,
            "audio":  MitekPermission.audio.status,
            "nfc":    MitekPermission.nfc.status,
        ])
    }

    @objc public override func requestPermissions(_ call: CAPPluginCall) {
        let requested = call.getArray("permissions", String.self) ?? ["camera", "audio", "nfc"]
        let group = DispatchGroup()

        if requested.contains("camera") {
            group.enter()
            AVCaptureDevice.requestAccess(for: .video) { _ in group.leave() }
        }
        if requested.contains("audio") {
            group.enter()
            AVCaptureDevice.requestAccess(for: .audio) { _ in group.leave() }
        }
        // NFC has no runtime prompt on iOS

        group.notify(queue: .main) {
            call.resolve([
                "camera": MitekPermission.camera.status,
                "audio":  MitekPermission.audio.status,
                "nfc":    MitekPermission.nfc.status,
            ])
        }
    }

    // MARK: - Private helpers

    /// Validates license presence and integrity. Rejects the call and returns nil on failure.
    private func validatedLicense(_ call: CAPPluginCall) -> String? {
        guard let license = call.getString("license"), !license.isEmpty else {
            call.reject("'license' is required", MitekError.licenseMissing.rawValue)
            return nil
        }
        if let error = MitekLicenseValidator.validate(license) {
            call.reject(error, MitekError.licenseInvalid.rawValue)
            return nil
        }
        return license
    }

    private func cameraAuthorized(_ call: CAPPluginCall) -> Bool {
        let status = AVCaptureDevice.authorizationStatus(for: .video)
        guard status == .authorized else {
            if status == .notDetermined {
                AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                    if granted {
                        // Re-entry not supported; tell caller to retry
                        call.reject("Camera permission just granted — please retry", MitekError.permissionDenied.rawValue)
                    } else {
                        call.reject("Camera permission denied", MitekError.permissionDenied.rawValue)
                    }
                }
            } else {
                call.reject("Camera permission denied", MitekError.permissionDenied.rawValue)
            }
            return false
        }
        return true
    }

    private func microphoneAuthorized(_ call: CAPPluginCall) -> Bool {
        let status = AVCaptureDevice.authorizationStatus(for: .audio)
        guard status == .authorized else {
            if status == .notDetermined {
                AVCaptureDevice.requestAccess(for: .audio) { [weak self] granted in
                    if granted {
                        call.reject("Microphone permission just granted — please retry", MitekError.permissionDenied.rawValue)
                    } else {
                        call.reject("Microphone permission denied", MitekError.permissionDenied.rawValue)
                    }
                }
            } else {
                call.reject("Microphone permission denied", MitekError.permissionDenied.rawValue)
            }
            return false
        }
        return true
    }

    private func resolveDocumentUseCase(_ type: String) -> MiSnapScienceDocumentType? {
        switch type.uppercased() {
        case "PASSPORT":          return .passport
        case "ID_FRONT":          return .idFront
        case "ID_BACK":           return .idBack
        case "CHECK_FRONT":       return .checkFront
        case "CHECK_BACK":        return .checkBack
        case "GENERIC_DOCUMENT":  return .generic
        default:                  return nil
        }
    }

    private func dismissAndResolve(vc: UIViewController?, resolve: () -> Void) {
        DispatchQueue.main.async {
            vc?.dismiss(animated: true)
        }
        resolve()
    }

    // Stored call accessor helpers
    private func storedCall(key: UnsafeRawPointer) -> CAPPluginCall? {
        return objc_getAssociatedObject(self, key) as? CAPPluginCall
    }

    private func clearCall(key: UnsafeRawPointer) {
        objc_setAssociatedObject(self, key, nil, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
    }
}

// MARK: - Associated object keys

private enum AssociatedKeys {
    static var documentCall: UInt8 = 0
    static var faceCall:     UInt8 = 0
    static var barcodeCall:  UInt8 = 0
    static var voiceCall:    UInt8 = 0
    static var nfcCall:      UInt8 = 0
}

// MARK: - MiSnapViewControllerDelegate (document + barcode)

extension MitekSdkPlugin: MiSnapViewControllerDelegate {

    public func miSnapLicenseStatus(_ status: MiSnapLicenseStatus) {
        MiSnapLogger.log("licenseStatus=\(status.rawValue)", level: .info)
    }

    public func miSnapSuccess(_ result: MiSnapResult) {
        let isBarcode = (barcodeVC != nil &&
                         objc_getAssociatedObject(self, &AssociatedKeys.barcodeCall) != nil)

        if isBarcode, let call = storedCall(key: &AssociatedKeys.barcodeCall) {
            MiSnapLogger.log("barcodeSession success")
            dismissAndResolve(vc: barcodeVC) {
                call.resolve(MitekResultBuilder.barcodeResult(result))
            }
            barcodeVC = nil
            clearCall(key: &AssociatedKeys.barcodeCall)
        } else if let call = storedCall(key: &AssociatedKeys.documentCall) {
            MiSnapLogger.log("documentSession success")
            dismissAndResolve(vc: documentVC) {
                call.resolve(MitekResultBuilder.documentResult(result))
            }
            documentVC = nil
            clearCall(key: &AssociatedKeys.documentCall)
        }
    }

    public func miSnapCancelled(_ result: MiSnapResult) {
        let isBarcode = (barcodeVC != nil &&
                         objc_getAssociatedObject(self, &AssociatedKeys.barcodeCall) != nil)

        if isBarcode, let call = storedCall(key: &AssociatedKeys.barcodeCall) {
            MiSnapLogger.log("barcodeSession cancelled", level: .warning)
            dismissAndResolve(vc: barcodeVC) {
                call.resolve(MitekResultBuilder.errorResult(sessionType: "barcode", code: .cancelled, message: "User cancelled"))
            }
            barcodeVC = nil
            clearCall(key: &AssociatedKeys.barcodeCall)
        } else if let call = storedCall(key: &AssociatedKeys.documentCall) {
            MiSnapLogger.log("documentSession cancelled", level: .warning)
            dismissAndResolve(vc: documentVC) {
                call.resolve(MitekResultBuilder.errorResult(sessionType: "document", code: .cancelled, message: "User cancelled"))
            }
            documentVC = nil
            clearCall(key: &AssociatedKeys.documentCall)
        }
    }

    public func miSnapException(_ exception: NSException) {
        MiSnapLogger.log("miSnapException: \(exception.reason ?? exception.name.rawValue)", level: .error)
        let isBarcode = (barcodeVC != nil &&
                         objc_getAssociatedObject(self, &AssociatedKeys.barcodeCall) != nil)

        if isBarcode, let call = storedCall(key: &AssociatedKeys.barcodeCall) {
            dismissAndResolve(vc: barcodeVC) {
                call.resolve(MitekResultBuilder.errorResult(sessionType: "barcode", code: .unknown, message: exception.reason ?? "Unknown exception"))
            }
            barcodeVC = nil
            clearCall(key: &AssociatedKeys.barcodeCall)
        } else if let call = storedCall(key: &AssociatedKeys.documentCall) {
            dismissAndResolve(vc: documentVC) {
                call.resolve(MitekResultBuilder.errorResult(sessionType: "document", code: .unknown, message: exception.reason ?? "Unknown exception"))
            }
            documentVC = nil
            clearCall(key: &AssociatedKeys.documentCall)
        }
    }
}

// MARK: - MiSnapFacialCaptureViewControllerDelegate

extension MitekSdkPlugin: MiSnapFacialCaptureViewControllerDelegate {

    public func miSnapFacialCaptureLicenseStatus(_ status: MiSnapLicenseStatus) {
        MiSnapLogger.log("facialCapture licenseStatus=\(status.rawValue)", level: .info)
    }

    public func miSnapFacialCaptureSuccess(_ result: MiSnapFacialCaptureResult) {
        guard let call = storedCall(key: &AssociatedKeys.faceCall) else { return }
        MiSnapLogger.log("faceSession success")
        dismissAndResolve(vc: faceVC) {
            call.resolve(MitekResultBuilder.faceResult(result))
        }
        faceVC = nil
        clearCall(key: &AssociatedKeys.faceCall)
    }

    public func miSnapFacialCaptureCancelled(_ result: MiSnapFacialCaptureResult) {
        guard let call = storedCall(key: &AssociatedKeys.faceCall) else { return }
        MiSnapLogger.log("faceSession cancelled", level: .warning)
        dismissAndResolve(vc: faceVC) {
            call.resolve(MitekResultBuilder.errorResult(sessionType: "face", code: .cancelled, message: "User cancelled"))
        }
        faceVC = nil
        clearCall(key: &AssociatedKeys.faceCall)
    }
}

// MARK: - MiSnapVoiceCaptureViewControllerDelegate

extension MitekSdkPlugin: MiSnapVoiceCaptureViewControllerDelegate {

    public func miSnapVoiceCaptureLicenseStatus(_ status: MiSnapLicenseStatus) {
        MiSnapLogger.log("voiceCapture licenseStatus=\(status.rawValue)", level: .info)
    }

    public func miSnapVoiceCaptureDidSelectPhrase(_ phrase: String) {
        MiSnapLogger.log("voiceCapture phrase selected: \(phrase)")
        capturedVoicePhrase = phrase
    }

    public func miSnapVoiceCaptureSuccess(_ results: [MiSnapVoiceCaptureResult], for type: MiSnapVoiceCaptureFlow) {
        guard let call = storedCall(key: &AssociatedKeys.voiceCall) else { return }
        MiSnapLogger.log("voiceSession success sampleCount=\(results.count)")
        let phrase = capturedVoicePhrase
        capturedVoicePhrase = nil
        dismissAndResolve(vc: voiceVC) {
            call.resolve(MitekResultBuilder.voiceResult(results, phrase: phrase))
        }
        voiceVC = nil
        clearCall(key: &AssociatedKeys.voiceCall)
    }

    public func miSnapVoiceCaptureCancelled(_ result: MiSnapVoiceCaptureResult) {
        guard let call = storedCall(key: &AssociatedKeys.voiceCall) else { return }
        MiSnapLogger.log("voiceSession cancelled", level: .warning)
        capturedVoicePhrase = nil
        dismissAndResolve(vc: voiceVC) {
            call.resolve(MitekResultBuilder.errorResult(sessionType: "voice", code: .cancelled, message: "User cancelled"))
        }
        voiceVC = nil
        clearCall(key: &AssociatedKeys.voiceCall)
    }

    public func miSnapVoiceCaptureError(_ result: MiSnapVoiceCaptureResult) {
        guard let call = storedCall(key: &AssociatedKeys.voiceCall) else { return }
        let msg = result.rts ?? "Voice capture error"
        MiSnapLogger.log("voiceSession error: \(msg)", level: .error)
        capturedVoicePhrase = nil
        dismissAndResolve(vc: voiceVC) {
            call.resolve(MitekResultBuilder.errorResult(sessionType: "voice", code: .voiceError, message: msg))
        }
        voiceVC = nil
        clearCall(key: &AssociatedKeys.voiceCall)
    }
}

// MARK: - MiSnapNFCViewControllerDelegate

extension MitekSdkPlugin: MiSnapNFCViewControllerDelegate {

    public func miSnapNfcLicenseStatus(_ status: MiSnapLicenseStatus) {
        MiSnapLogger.log("nfc licenseStatus=\(status.rawValue)", level: .info)
    }

    public func miSnapNfcSuccess(_ result: [String: Any]) {
        guard let call = storedCall(key: &AssociatedKeys.nfcCall) else { return }
        MiSnapLogger.log("nfcSession success")
        dismissAndResolve(vc: nfcVC) {
            call.resolve(MitekResultBuilder.nfcResult(result))
        }
        nfcVC = nil
        clearCall(key: &AssociatedKeys.nfcCall)
    }

    public func miSnapNfcCancelled(_ result: [String: Any]) {
        guard let call = storedCall(key: &AssociatedKeys.nfcCall) else { return }
        MiSnapLogger.log("nfcSession cancelled", level: .warning)
        dismissAndResolve(vc: nfcVC) {
            call.resolve(MitekResultBuilder.errorResult(sessionType: "nfc", code: .cancelled, message: "User cancelled"))
        }
        nfcVC = nil
        clearCall(key: &AssociatedKeys.nfcCall)
    }

    public func miSnapNfcSkipped(_ result: [String: Any]) {
        guard let call = storedCall(key: &AssociatedKeys.nfcCall) else { return }
        MiSnapLogger.log("nfcSession skipped", level: .warning)
        dismissAndResolve(vc: nfcVC) {
            call.resolve(MitekResultBuilder.errorResult(sessionType: "nfc", code: .nfcError, message: "NFC step skipped by user"))
        }
        nfcVC = nil
        clearCall(key: &AssociatedKeys.nfcCall)
    }
}
