import type { PermissionState } from '@capacitor/core';

export type MitekPermissionType = 'camera' | 'audio' | 'nfc';

export interface PermissionStatus {
  camera: PermissionState;
  audio: PermissionState;
  nfc: PermissionState;
}

export type DocumentType =
  | 'PASSPORT'
  | 'ID_FRONT'
  | 'ID_BACK'
  | 'CHECK_FRONT'
  | 'CHECK_BACK'
  | 'GENERIC_DOCUMENT';

export interface DocumentSessionOptions {
  license: string;
  documentType?: DocumentType;
}

export interface FaceSessionOptions {
  license: string;
}

export interface BarcodeSessionOptions {
  license: string;
}

export interface VoiceSessionOptions {
  license: string;
  phrase?: string;
}

export interface NfcSessionOptions {
  license: string;
  mrzLine1?: string;
  mrzLine2?: string;
  mrzLine3?: string;
  documentNumber?: string;
  dateOfBirth?: string;
  dateOfExpiry?: string;
  country?: string;
  documentCode?: string;
}

export interface ExtractedDocumentData {
  firstName?: string;
  surname?: string;
  sex?: string;
  dateOfBirth?: string;
  dateOfExpiry?: string;
  nationality?: string;
  country?: string;
  documentNumber?: string;
  documentType?: string;
  optionalData1?: string;
  optionalData2?: string;
  rawMrz?: string;
}

export interface NfcChipData {
  chipType?: string;
  documentNumber?: string;
  documentCode?: string;
  firstName?: string;
  lastName?: string;
  gender?: string;
  nationality?: string;
  issuingCountry?: string;
  dateOfBirth?: string;
  dateOfExpiry?: string;
  dateOfIssue?: string;
  personalNumber?: string;
  placeOfBirth?: string;
  permanentPlaceOfResidence?: string;
  faceImageBase64?: string;
  chipAuthInfo?: string;
  activeAuthInfo?: string;
  vehicleCategories?: string[];
}

export interface BarcodeData {
  encodedBarcode?: string;
  type?: string;
  isVds?: boolean;
}

export interface SessionResult {
  success: boolean;
  sessionType: string;
  imageBase64?: string;
  extractedData?: ExtractedDocumentData;
  nfcData?: NfcChipData;
  barcode?: BarcodeData;
  classification?: string;
  licenseExpired?: boolean;
  rts?: string;
  aiBasedRtsBase64?: string;
  voiceSampleCount?: number;
  phrase?: string;
  errorCode?: string;
  errorMessage?: string;
}

export interface LicenseValidationOptions {
  license: string;
}

export interface LicenseValidationResult {
  isValid: boolean;
  errorCode?: string;
  errorMessage?: string;
}

export interface MitekSdkPlugin {
  validateLicense(options: LicenseValidationOptions): Promise<LicenseValidationResult>;
  startDocumentSession(options: DocumentSessionOptions): Promise<SessionResult>;
  startFaceSession(options: FaceSessionOptions): Promise<SessionResult>;
  startBarcodeSession(options: BarcodeSessionOptions): Promise<SessionResult>;
  startVoiceSession(options: VoiceSessionOptions): Promise<SessionResult>;
  startNfcSession(options: NfcSessionOptions): Promise<SessionResult>;
  checkPermissions(): Promise<PermissionStatus>;
  requestPermissions(options?: { permissions: MitekPermissionType[] }): Promise<PermissionStatus>;
}
