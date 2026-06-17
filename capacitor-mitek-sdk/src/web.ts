import { WebPlugin } from '@capacitor/core';

import type {
  BarcodeSessionOptions,
  DocumentSessionOptions,
  FaceSessionOptions,
  LicenseValidationOptions,
  LicenseValidationResult,
  MitekPermissionType,
  MitekSdkPlugin,
  NfcSessionOptions,
  PermissionStatus,
  SessionResult,
  VoiceSessionOptions,
} from './definitions';

export class MitekSdkWeb extends WebPlugin implements MitekSdkPlugin {
  async validateLicense(_options: LicenseValidationOptions): Promise<LicenseValidationResult> {
    throw this.unimplemented('validateLicense is not available on web.');
  }

  async startDocumentSession(_options: DocumentSessionOptions): Promise<SessionResult> {
    throw this.unimplemented('startDocumentSession is not available on web.');
  }

  async startFaceSession(_options: FaceSessionOptions): Promise<SessionResult> {
    throw this.unimplemented('startFaceSession is not available on web.');
  }

  async startBarcodeSession(_options: BarcodeSessionOptions): Promise<SessionResult> {
    throw this.unimplemented('startBarcodeSession is not available on web.');
  }

  async startVoiceSession(_options: VoiceSessionOptions): Promise<SessionResult> {
    throw this.unimplemented('startVoiceSession is not available on web.');
  }

  async startNfcSession(_options: NfcSessionOptions): Promise<SessionResult> {
    throw this.unimplemented('startNfcSession is not available on web.');
  }

  async checkPermissions(): Promise<PermissionStatus> {
    throw this.unimplemented('checkPermissions is not available on web.');
  }

  async requestPermissions(_options?: { permissions: MitekPermissionType[] }): Promise<PermissionStatus> {
    throw this.unimplemented('requestPermissions is not available on web.');
  }
}
