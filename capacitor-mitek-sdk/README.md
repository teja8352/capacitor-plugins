# capacitor-mitek-sdk

Capacitor plugin for the **Mitek MiSnap SDK** (v5.11.1).  
Supports identity document capture, biometric face capture, barcode scanning, NFC chip reading, and voice verification in **Ionic + Angular + Capacitor** applications.

| Platform | Support |
|----------|---------|
| Android  | ✅ Full implementation |
| iOS      | 🚧 Stub — implementation coming |
| Web      | ❌ Native SDK only |

---

## Table of Contents

1. [Requirements](#requirements)
2. [Project Integration](#project-integration)
3. [Android Setup](#android-setup)
4. [iOS Setup](#ios-setup)
5. [Angular Service Setup](#angular-service-setup)
6. [Session Usage](#session-usage)
   - [Document Capture](#document-capture)
   - [Face Capture](#face-capture)
   - [Barcode Scan](#barcode-scan)
   - [Voice Capture](#voice-capture)
   - [NFC Reading](#nfc-reading)
7. [Permission Handling](#permission-handling)
8. [Full KYC Flow Example](#full-kyc-flow-example)
9. [API Reference](#api-reference)
10. [Error Codes](#error-codes)
11. [Troubleshooting](#troubleshooting)

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Node.js | LTS (18+) |
| Capacitor | 6.x |
| Android minSdk | 24 (Android 7.0) |
| Android compileSdk | 34 |
| Kotlin | 1.8.10 |
| Gradle | 8.0 |
| JDK | 17 |
| iOS | 14.0+ |
| Mitek MiSnap SDK | 5.11.1 |

---

## Project Integration

This plugin lives inside your project under `plugins/capacitor-mitek-sdk`.  
It is referenced as a local file dependency — no npm registry required.

### Step 1 — Reference the plugin in your app's `package.json`

Open the `package.json` at the root of your Ionic project and add the plugin under `dependencies`:

```json
{
  "dependencies": {
    "capacitor-mitek-sdk": "file:./plugins/capacitor-mitek-sdk"
  }
}
```

### Step 2 — Build the plugin

```bash
cd plugins/capacitor-mitek-sdk
npm install
npm run build
cd ../..
```

### Step 3 — Install and sync

```bash
npm install
npx cap sync
```

`npx cap sync` copies the Android and iOS native code into `android/` and `ios/` and updates the Capacitor bridge.

---

## Android Setup

### 1. Add Mitek GitHub Package credentials

The MiSnap SDK is distributed through **GitHub Packages**.  
You need a GitHub **Personal Access Token (PAT)** with the `read:packages` scope.

Create or open `android/local.properties` and add:

```properties
mitek.github.username=YOUR_GITHUB_USERNAME
mitek.github.token=YOUR_GITHUB_PAT_WITH_READ_PACKAGES_SCOPE
```

> **Do not commit `local.properties` to version control.** Add it to `.gitignore` if it is not already there.

For CI/CD environments, set environment variables instead:

```bash
export MITEK_GITHUB_USERNAME="your-username"
export MITEK_GITHUB_TOKEN="your-pat"
```

### 2. Register the plugin in MainActivity

Open `android/app/src/main/java/<your-package>/MainActivity.kt`:

```kotlin
package com.yourcompany.yourapp

import android.os.Bundle
import com.getcapacitor.BridgeActivity
import com.mitek.capacitor.MitekSdkPlugin

class MainActivity : BridgeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        registerPlugin(MitekSdkPlugin::class.java)
        super.onCreate(savedInstanceState)
    }
}
```

### 3. Verify permissions in your app manifest

The plugin's manifest is merged automatically by Gradle. Run a build and confirm these appear in `android/app/build/outputs/merged_manifests/`:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.NFC" />
```

Remove the audio and NFC permissions from your app-level manifest if you are not using Voice or NFC sessions respectively.

### 4. Obtain a Mitek license key

Contact [Mitek Systems](https://www.miteksystems.com) to obtain a license key.  
Each key is scoped to your **application ID** (e.g. `com.yourcompany.yourapp`) and the **features** you have licensed (document, face, NFC, barcode, voice).

---

## iOS Setup

The iOS plugin bridge files are in place. All methods currently return an `UNIMPLEMENTED` result until the Mitek iOS SDK is wired up.

When ready to implement iOS:

1. Add the Mitek iOS SDK dependency to `plugins/capacitor-mitek-sdk/capacitor-mitek-sdk.podspec`.
2. Implement session launchers inside `ios/Plugin/MitekSdk.swift`.
3. Run `pod install` and `npx cap sync ios`.

Add the following keys to your app's `ios/App/App/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>Required to capture identity documents and facial images.</string>
<key>NSMicrophoneUsageDescription</key>
<string>Required to record voice samples for biometric verification.</string>
<key>NFCReaderUsageDescription</key>
<string>Required to read the NFC chip in your identity document.</string>
```

---

## Angular Service Setup

Generate a dedicated service to wrap all plugin calls:

```bash
ionic generate service services/mitek
```

### `src/app/services/mitek.service.ts`

```typescript
import { Injectable } from '@angular/core';
import {
  MitekSdk,
  DocumentSessionOptions,
  FaceSessionOptions,
  BarcodeSessionOptions,
  VoiceSessionOptions,
  NfcSessionOptions,
  SessionResult,
  PermissionStatus,
  MitekPermissionType,
} from 'capacitor-mitek-sdk';

@Injectable({ providedIn: 'root' })
export class MitekService {

  private readonly LICENSE = 'YOUR_MITEK_LICENSE_KEY';

  async checkPermissions(): Promise<PermissionStatus> {
    return MitekSdk.checkPermissions();
  }

  async requestPermissions(permissions?: MitekPermissionType[]): Promise<PermissionStatus> {
    return MitekSdk.requestPermissions(permissions ? { permissions } : undefined);
  }

  async capturePassport(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'PASSPORT' });
  }

  async captureIdFront(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'ID_FRONT' });
  }

  async captureIdBack(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'ID_BACK' });
  }

  async captureCheckFront(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'CHECK_FRONT' });
  }

  async captureCheckBack(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'CHECK_BACK' });
  }

  async captureGenericDocument(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'GENERIC_DOCUMENT' });
  }

  async captureFace(): Promise<SessionResult> {
    return MitekSdk.startFaceSession({ license: this.LICENSE });
  }

  async scanBarcode(): Promise<SessionResult> {
    return MitekSdk.startBarcodeSession({ license: this.LICENSE });
  }

  async captureVoice(phrase?: string): Promise<SessionResult> {
    return MitekSdk.startVoiceSession({ license: this.LICENSE, phrase });
  }

  async readNfc(options: Omit<NfcSessionOptions, 'license'>): Promise<SessionResult> {
    return MitekSdk.startNfcSession({ license: this.LICENSE, ...options });
  }
}
```

---

## Session Usage

### Document Capture

Captures a government-issued identity document.

**Supported document types:**

| `documentType` | Description |
|----------------|-------------|
| `PASSPORT` | International passport (TD3 MRZ) |
| `ID_FRONT` | National ID card — front side |
| `ID_BACK` | National ID card — back side (may include barcode) |
| `CHECK_FRONT` | Bank cheque — front |
| `CHECK_BACK` | Bank cheque — back |
| `GENERIC_DOCUMENT` | Any document |

```typescript
import { MitekSdk } from 'capacitor-mitek-sdk';

async captureDocument() {
  const result = await MitekSdk.startDocumentSession({
    license: 'YOUR_LICENSE_KEY',
    documentType: 'PASSPORT',
  });

  if (result.success) {
    console.log('Image (base64):', result.imageBase64);
    console.log('Classification:', result.classification);
    console.log('First name:', result.extractedData?.firstName);
    console.log('Surname:', result.extractedData?.surname);
    console.log('Doc number:', result.extractedData?.documentNumber);
    console.log('DOB:', result.extractedData?.dateOfBirth);
    console.log('Expiry:', result.extractedData?.dateOfExpiry);
    console.log('Nationality:', result.extractedData?.nationality);
    console.log('Raw MRZ:', result.extractedData?.rawMrz);
  } else {
    console.error(result.errorCode, result.errorMessage);
  }
}
```

**Display the captured image in your template:**

```html
<img *ngIf="result?.imageBase64"
     [src]="'data:image/jpeg;base64,' + result.imageBase64"
     alt="Captured document" />
```

---

### Face Capture

Captures a biometric selfie image.

```typescript
async captureFace() {
  const result = await MitekSdk.startFaceSession({
    license: 'YOUR_LICENSE_KEY',
  });

  if (result.success) {
    console.log('Face image (base64):', result.imageBase64);
    console.log('RTS payload:', result.rts);
    console.log('AI-based RTS:', result.aiBasedRtsBase64);
  } else {
    console.error(result.errorCode, result.errorMessage);
  }
}
```

---

### Barcode Scan

Scans barcodes including PDF417, QR Code, Aztec, and Visible Digital Seals (VDS).

```typescript
async scanBarcode() {
  const result = await MitekSdk.startBarcodeSession({
    license: 'YOUR_LICENSE_KEY',
  });

  if (result.success) {
    console.log('Decoded barcode:', result.barcode?.encodedBarcode);
    console.log('Barcode type:', result.barcode?.type);
    console.log('Is VDS:', result.barcode?.isVds);
    console.log('Frame image (base64):', result.imageBase64);
  } else {
    console.error(result.errorCode, result.errorMessage);
  }
}
```

---

### Voice Capture

Records the user speaking a phrase for voice-biometric verification.  
Raw audio is not returned to JavaScript. Use the `rts` payload to submit to the Mitek API for server-side verification.

```typescript
async captureVoice() {
  const result = await MitekSdk.startVoiceSession({
    license: 'YOUR_LICENSE_KEY',
    phrase: 'My voice is my password',
  });

  if (result.success) {
    console.log('Phrase spoken:', result.phrase);
    console.log('Samples recorded:', result.voiceSampleCount);
  } else {
    console.error(result.errorCode, result.errorMessage);
  }
}
```

---

### NFC Reading

Reads the NFC chip from a passport or EU driving licence.  
Supply document credentials to skip the SDK's manual-entry screen.

**Option A — Individual credential fields (recommended):**

Use the values extracted from a prior document session:

```typescript
async readNfcWithDocumentResult(docResult: SessionResult) {
  const extracted = docResult.extractedData;

  // Dates must be in YYMMdd format — strip the century digits
  const toYYMMdd = (isoDate?: string) =>
    isoDate ? isoDate.replace(/-/g, '').slice(2) : undefined;

  const result = await MitekSdk.startNfcSession({
    license: 'YOUR_LICENSE_KEY',
    documentNumber: extracted?.documentNumber,
    dateOfBirth:    toYYMMdd(extracted?.dateOfBirth),
    dateOfExpiry:   toYYMMdd(extracted?.dateOfExpiry),
    country:        extracted?.country,
    documentCode:   extracted?.documentType,
  });

  if (result.success) {
    const chip = result.nfcData;
    console.log('Chip type:', chip?.chipType);        // "ICAO" or "EU_DL"
    console.log('Name:', chip?.firstName, chip?.lastName);
    console.log('Gender:', chip?.gender);
    console.log('Issuing country:', chip?.issuingCountry);
    console.log('Date of birth:', chip?.dateOfBirth);
    console.log('Date of expiry:', chip?.dateOfExpiry);
    console.log('Date of issue:', chip?.dateOfIssue);
    console.log('Personal number:', chip?.personalNumber);
    console.log('Place of birth:', chip?.placeOfBirth);
    console.log('Chip auth:', chip?.chipAuthInfo);
    console.log('Active auth:', chip?.activeAuthInfo);
    console.log('Face image:', chip?.faceImageBase64?.substring(0, 20) + '…');

    // EU Driving Licence only
    if (chip?.chipType === 'EU_DL') {
      console.log('Vehicle categories:', chip.vehicleCategories);
      console.log('Residence:', chip.permanentPlaceOfResidence);
    }
  } else {
    console.error(result.errorCode, result.errorMessage);
  }
}
```

**Option B — Raw MRZ lines:**

```typescript
const result = await MitekSdk.startNfcSession({
  license: 'YOUR_LICENSE_KEY',
  mrzLine1: 'P<USASMITH<<JOHN<HENRY<<<<<<<<<<<<<<<<<<<<<<',
  mrzLine2: '1234567890USA9001151M2601311<<<<<<<<<<<<<<<4',
});
```

**Option C — Let the SDK prompt the user:**

```typescript
const result = await MitekSdk.startNfcSession({
  license: 'YOUR_LICENSE_KEY',
});
```

---

## Permission Handling

The plugin requests permissions automatically before launching each session.  
You can also check and request permissions manually before showing any capture UI.

### Check current permission state

```typescript
import { MitekSdk } from 'capacitor-mitek-sdk';

const status = await MitekSdk.checkPermissions();
console.log(status.camera);  // "granted" | "denied" | "prompt"
console.log(status.audio);
console.log(status.nfc);
```

### Request specific permissions

```typescript
// Request only camera
const status = await MitekSdk.requestPermissions({ permissions: ['camera'] });

// Request camera and audio
const status = await MitekSdk.requestPermissions({ permissions: ['camera', 'audio'] });

// Request all permissions at once
const status = await MitekSdk.requestPermissions();
```

### Guard a session with a permission pre-check

```typescript
async startDocumentCapture() {
  const perms = await MitekSdk.checkPermissions();

  if (perms.camera !== 'granted') {
    const requested = await MitekSdk.requestPermissions({ permissions: ['camera'] });
    if (requested.camera !== 'granted') {
      this.showToast('Camera permission is required to capture documents.', 'warning');
      return;
    }
  }

  const result = await MitekSdk.startDocumentSession({
    license: 'YOUR_LICENSE_KEY',
    documentType: 'PASSPORT',
  });
  // handle result
}
```

---

## Full KYC Flow Example

A complete end-to-end KYC flow: passport capture → face capture → NFC reading.

### Angular Component

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule, LoadingController, ToastController } from '@ionic/angular';
import { MitekService } from '../../services/mitek.service';
import { SessionResult } from 'capacitor-mitek-sdk';

@Component({
  selector: 'app-kyc',
  standalone: true,
  imports: [CommonModule, IonicModule],
  templateUrl: './kyc.page.html',
})
export class KycPage {

  documentResult: SessionResult | null = null;
  faceResult: SessionResult | null = null;
  nfcResult: SessionResult | null = null;
  isLoading = false;

  constructor(
    private mitek: MitekService,
    private loadingCtrl: LoadingController,
    private toastCtrl: ToastController,
  ) {}

  async startKyc() {
    const loader = await this.loadingCtrl.create({ message: 'Preparing…' });
    await loader.present();
    this.isLoading = true;

    try {
      const perms = await this.mitek.checkPermissions();
      if (perms.camera !== 'granted') {
        const req = await this.mitek.requestPermissions(['camera']);
        if (req.camera !== 'granted') {
          await this.toast('Camera permission is required.', 'warning');
          return;
        }
      }

      loader.message = 'Place your passport flat in view…';
      const doc = await this.mitek.capturePassport();
      if (!doc.success) {
        if (doc.errorCode !== 'SESSION_CANCELLED') {
          await this.toast('Document capture failed: ' + doc.errorMessage, 'danger');
        }
        return;
      }
      this.documentResult = doc;
      await this.toast('Passport captured.');

      loader.message = 'Look directly at the camera…';
      const face = await this.mitek.captureFace();
      if (!face.success) {
        if (face.errorCode !== 'SESSION_CANCELLED') {
          await this.toast('Face capture failed: ' + face.errorMessage, 'danger');
        }
        return;
      }
      this.faceResult = face;
      await this.toast('Face captured.');

      loader.message = 'Hold your passport near the top of the phone…';
      const toYYMMdd = (iso?: string) => iso ? iso.replace(/-/g, '').slice(2) : undefined;
      const nfc = await this.mitek.readNfc({
        documentNumber: doc.extractedData?.documentNumber,
        dateOfBirth:    toYYMMdd(doc.extractedData?.dateOfBirth),
        dateOfExpiry:   toYYMMdd(doc.extractedData?.dateOfExpiry),
        country:        doc.extractedData?.country,
        documentCode:   doc.extractedData?.documentType,
      });
      if (!nfc.success && nfc.errorCode !== 'SESSION_CANCELLED') {
        await this.toast('NFC reading failed: ' + nfc.errorMessage, 'warning');
      }
      this.nfcResult = nfc.success ? nfc : null;

      await this.toast('KYC capture complete. Submitting…', 'success');
      await this.submitToServer(doc, face, nfc);

    } catch (err) {
      console.error('[KycPage] unexpected error', err);
      await this.toast('An unexpected error occurred.', 'danger');
    } finally {
      this.isLoading = false;
      await loader.dismiss();
    }
  }

  private async submitToServer(
    doc: SessionResult,
    face: SessionResult,
    nfc: SessionResult,
  ) {
    // Send imageBase64, extractedData, rts, and aiBasedRtsBase64 to your backend.
    // Your backend forwards these to the Mitek API for identity verification.
    console.log('[KycPage] submitting to server:', {
      documentImage: doc.imageBase64?.substring(0, 20) + '…',
      faceImage: face.imageBase64?.substring(0, 20) + '…',
      nfcChipType: nfc.nfcData?.chipType,
    });
  }

  private async toast(message: string, color = 'primary') {
    const t = await this.toastCtrl.create({ message, duration: 3000, color });
    await t.present();
  }
}
```

### Angular Template

```html
<ion-header>
  <ion-toolbar>
    <ion-title>Identity Verification</ion-title>
  </ion-toolbar>
</ion-header>

<ion-content class="ion-padding">

  <ion-button expand="block" (click)="startKyc()" [disabled]="isLoading">
    <ion-icon name="shield-checkmark-outline" slot="start"></ion-icon>
    Start KYC
  </ion-button>

  <ion-card *ngIf="documentResult?.success">
    <ion-card-header>
      <ion-card-title>Passport</ion-card-title>
      <ion-card-subtitle>{{ documentResult?.classification }}</ion-card-subtitle>
    </ion-card-header>
    <ion-card-content>
      <img [src]="'data:image/jpeg;base64,' + documentResult?.imageBase64"
           style="max-width:100%; border-radius:8px;" />
      <p><strong>Name:</strong> {{ documentResult?.extractedData?.firstName }} {{ documentResult?.extractedData?.surname }}</p>
      <p><strong>Document #:</strong> {{ documentResult?.extractedData?.documentNumber }}</p>
      <p><strong>DOB:</strong> {{ documentResult?.extractedData?.dateOfBirth }}</p>
      <p><strong>Expiry:</strong> {{ documentResult?.extractedData?.dateOfExpiry }}</p>
      <p><strong>Nationality:</strong> {{ documentResult?.extractedData?.nationality }}</p>
    </ion-card-content>
  </ion-card>

  <ion-card *ngIf="faceResult?.success">
    <ion-card-header>
      <ion-card-title>Selfie</ion-card-title>
    </ion-card-header>
    <ion-card-content>
      <img [src]="'data:image/jpeg;base64,' + faceResult?.imageBase64"
           style="max-width:180px; border-radius:50%; display:block; margin:auto;" />
    </ion-card-content>
  </ion-card>

  <ion-card *ngIf="nfcResult?.success">
    <ion-card-header>
      <ion-card-title>NFC Chip — {{ nfcResult?.nfcData?.chipType }}</ion-card-title>
    </ion-card-header>
    <ion-card-content>
      <img *ngIf="nfcResult?.nfcData?.faceImageBase64"
           [src]="'data:image/jpeg;base64,' + nfcResult?.nfcData?.faceImageBase64"
           style="max-width:120px; border-radius:50%; display:block; margin:auto 0 12px;" />
      <p><strong>Name:</strong> {{ nfcResult?.nfcData?.firstName }} {{ nfcResult?.nfcData?.lastName }}</p>
      <p><strong>Gender:</strong> {{ nfcResult?.nfcData?.gender }}</p>
      <p><strong>Issuing country:</strong> {{ nfcResult?.nfcData?.issuingCountry }}</p>
      <p><strong>Chip auth:</strong> {{ nfcResult?.nfcData?.chipAuthInfo }}</p>
    </ion-card-content>
  </ion-card>

</ion-content>
```

---

## API Reference

### `startDocumentSession(options)`

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `license` | `string` | ✅ | — | Mitek license key |
| `documentType` | `DocumentType` | — | `'PASSPORT'` | Document type to capture |

**Returns:** `Promise<SessionResult>`  
**Requires permission:** `camera`

---

### `startFaceSession(options)`

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `license` | `string` | ✅ | Mitek license key |

**Returns:** `Promise<SessionResult>`  
**Requires permission:** `camera`

---

### `startBarcodeSession(options)`

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `license` | `string` | ✅ | Mitek license key |

**Returns:** `Promise<SessionResult>`  
**Requires permission:** `camera`

---

### `startVoiceSession(options)`

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `license` | `string` | ✅ | Mitek license key |
| `phrase` | `string` | — | Phrase for the user to speak |

**Returns:** `Promise<SessionResult>`  
**Requires permission:** `audio`

---

### `startNfcSession(options)`

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `license` | `string` | ✅ | Mitek license key |
| `mrzLine1` | `string` | — | First raw MRZ line (44 chars, TD3) |
| `mrzLine2` | `string` | — | Second raw MRZ line |
| `mrzLine3` | `string` | — | Third MRZ line (optional) |
| `documentNumber` | `string` | — | Document number (9 chars) |
| `dateOfBirth` | `string` | — | DOB in `YYMMdd` format |
| `dateOfExpiry` | `string` | — | Expiry in `YYMMdd` format |
| `country` | `string` | — | 3-letter issuing country code |
| `documentCode` | `string` | — | 2-letter document type code |

**Returns:** `Promise<SessionResult>`  
**Requires permission:** `nfc`

---

### `checkPermissions()`

**Returns:** `Promise<PermissionStatus>`

```typescript
interface PermissionStatus {
  camera: 'granted' | 'denied' | 'prompt';
  audio:  'granted' | 'denied' | 'prompt';
  nfc:    'granted' | 'denied' | 'prompt';
}
```

---

### `requestPermissions(options?)`

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `options.permissions` | `MitekPermissionType[]` | — | Subset to request. Omit for all. |

`MitekPermissionType` values: `'camera'` · `'audio'` · `'nfc'`

**Returns:** `Promise<PermissionStatus>`

---

### `SessionResult` shape

```typescript
interface SessionResult {
  success: boolean;
  sessionType: string;          // "document" | "face" | "barcode" | "nfc" | "voice"

  imageBase64?: string;         // Base64 JPEG — document, face, barcode sessions

  extractedData?: {             // Document sessions
    firstName?: string;
    surname?: string;
    sex?: string;
    dateOfBirth?: string;       // YYYY-MM-DD
    dateOfExpiry?: string;      // YYYY-MM-DD
    nationality?: string;
    country?: string;
    documentNumber?: string;
    documentType?: string;
    optionalData1?: string;
    optionalData2?: string;
    rawMrz?: string;
  };

  nfcData?: {                   // NFC sessions
    chipType?: string;          // "ICAO" | "EU_DL"
    documentNumber?: string;
    documentCode?: string;
    firstName?: string;
    lastName?: string;          // note: lastName, not surname
    gender?: string;            // note: gender, not sex
    nationality?: string;
    issuingCountry?: string;    // note: issuingCountry, not country
    dateOfBirth?: string;
    dateOfExpiry?: string;
    dateOfIssue?: string;
    personalNumber?: string;
    placeOfBirth?: string;
    permanentPlaceOfResidence?: string;  // EU_DL only
    faceImageBase64?: string;
    chipAuthInfo?: string;
    activeAuthInfo?: string;
    vehicleCategories?: string[];        // EU_DL only
  };

  barcode?: {                   // Barcode and document sessions
    encodedBarcode?: string;
    type?: string;              // e.g. "PDF417", "QR_CODE"
    isVds?: boolean;
  };

  classification?: string;      // e.g. "PASSPORT", "US_DRIVERS_LICENSE_FRONT"
  licenseExpired?: boolean;

  rts?: string;                 // Encrypted server-auth payload (document, face, barcode)
  aiBasedRtsBase64?: string;    // AI-based payload — face sessions only (base64)

  voiceSampleCount?: number;    // Voice sessions
  phrase?: string;              // Voice sessions

  errorCode?: string;
  errorMessage?: string;
}
```

---

## Error Codes

| `errorCode` | When it occurs |
|-------------|----------------|
| `LICENSE_MISSING` | `license` field not provided in options |
| `LICENSE_INVALID` | SDK rejected the license key (wrong key, expired, wrong app ID, wrong feature) |
| `PERMISSION_DENIED` | Required OS permission was not granted |
| `SESSION_CANCELLED` | User dismissed the capture UI |
| `CAMERA_ERROR` | Camera hardware unavailable or access blocked |
| `ANALYSIS_ERROR` | SDK internal frame analysis failure |
| `NFC_ERROR` | NFC chip could not be read |
| `VOICE_ERROR` | Voice recording failure |
| `SETTINGS_ERROR` | Invalid session configuration (e.g. unrecognised `documentType`) |
| `UNIMPLEMENTED` | Feature not yet available on this platform (iOS) |
| `UNKNOWN_ERROR` | Unexpected SDK or bridge error |

### Error handling pattern

```typescript
const result = await MitekSdk.startDocumentSession({ license: '...', documentType: 'PASSPORT' });

if (!result.success) {
  switch (result.errorCode) {
    case 'SESSION_CANCELLED':
      break;
    case 'LICENSE_MISSING':
    case 'LICENSE_INVALID':
      console.error('Mitek license problem:', result.errorMessage);
      break;
    case 'PERMISSION_DENIED':
      this.showPermissionRationale();
      break;
    case 'CAMERA_ERROR':
      this.showRetryPrompt('Camera unavailable. Please try again.');
      break;
    default:
      console.error('MiSnap error:', result.errorCode, result.errorMessage);
  }
}
```

---

## Troubleshooting

### `Could not resolve com.miteksystems.misnap:workflow:5.11.1`

Gradle cannot reach the Mitek GitHub Packages repository.

**Fix:**
1. Confirm `mitek.github.username` and `mitek.github.token` are set in `android/local.properties`.
2. Confirm the PAT has `read:packages` scope.
3. Confirm your GitHub account has been granted access to `mitek-systems/misnap-android`.

---

### `LICENSE_INVALID` error at runtime

The license key does not match the app's `applicationId` or the feature is not enabled.

**Fix:**
1. Open `android/app/build.gradle` and note the `applicationId`.
2. Confirm with Mitek that your license is provisioned for that exact application ID and the feature you are using.

---

### Camera freezes or session does not start after granting permission

On some Android 11+ devices the camera service restarts after a runtime permission grant.

**Fix:** This is handled automatically. The plugin relaunches the session inside `onCameraPermission` after the grant is confirmed. If the issue persists, ensure no other app holds the camera (Android only allows one camera user at a time).

---

### NFC session fails immediately with `NFC_ERROR`

**Possible causes:**
- NFC is disabled in device Settings.
- Device does not have NFC hardware.
- The document is not NFC-enabled.

**Fix:** Check NFC availability before starting a session:

```typescript
import { NFC } from '@awesome-cordova-plugins/nfc/ngx';

// Or check via native intent check
const perms = await MitekSdk.checkPermissions();
if (perms.nfc !== 'granted') {
  // Guide user to enable NFC in Settings
}
```

---

### Session result has no `extractedData`

The SDK captured the image but OCR did not succeed (poor lighting, glare, partial capture).

**Fix:** Prompt the user to retry with better lighting. The `success: true` flag means the image was captured — extraction is best-effort.

---

### `startVoiceSession` returns success but `voiceSampleCount` is 0

The session completed but no audio samples were recorded (user did not speak).

**Fix:** Ensure `RECORD_AUDIO` permission is granted and the device microphone is not blocked by another app.

---

## Plugin Structure

```
plugins/capacitor-mitek-sdk/
├── src/
│   ├── definitions.ts          TypeScript interfaces (SessionResult, options, etc.)
│   ├── index.ts                Plugin registration + public exports
│   └── web.ts                  Browser stub (throws unimplemented)
├── android/
│   ├── build.gradle            SDK Gradle dependencies + Maven repo config
│   ├── proguard-rules.pro      R8 / ProGuard keep rules
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/mitek/capacitor/
│           ├── MitekSdkPlugin.kt   @CapacitorPlugin bridge class
│           └── MitekSdk.kt         Step builders + result parser
├── ios/
│   └── Plugin/
│       ├── MitekSdkPlugin.m        ObjC CAP_PLUGIN registration
│       ├── MitekSdkPlugin.swift    iOS bridge stub
│       └── MitekSdk.swift          iOS helper stub
├── capacitor-mitek-sdk.podspec
├── package.json
├── tsconfig.json
└── rollup.config.js
```
