# capacitor-mitek-sdk

Capacitor plugin wrapping the **[Mitek MiSnap Android SDK](https://github.com/Mitek-Systems/MiSnap-Android)** (v5.11.1).

The plugin does not reimplement any capture logic. It configures and launches `MiSnapWorkflowActivity` from the MiSnap SDK, reads the results it returns, and bridges them to JavaScript via Capacitor.

| Platform | Support |
|----------|---------|
| Android  | ✅ Full — delegates to MiSnapWorkflowActivity |
| iOS      | 🚧 Stub — implementation coming |
| Web      | ❌ Native SDK only |

---

## Table of Contents

1. [Requirements](#requirements)
2. [How it works](#how-it-works)
3. [Project Integration](#project-integration)
4. [Android Setup](#android-setup)
5. [iOS Setup](#ios-setup)
6. [Angular Service](#angular-service)
7. [Session Usage](#session-usage)
   - [Document Capture](#document-capture)
   - [Face Capture](#face-capture)
   - [Barcode Scan](#barcode-scan)
   - [Voice Capture](#voice-capture)
   - [NFC Reading](#nfc-reading)
8. [Permission Handling](#permission-handling)
9. [Full KYC Flow Example](#full-kyc-flow-example)
10. [API Reference](#api-reference)
11. [Error Codes](#error-codes)
12. [Troubleshooting](#troubleshooting)

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Node.js | 20+ |
| Angular | 17+ |
| Ionic Framework | 8+ |
| Capacitor | 8.x |
| TypeScript | 5.6+ |
| Android minSdk | 26 (Android 8.0) |
| Android compileSdk | 35 |
| Kotlin | 2.0+ |
| Gradle | 8.7+ |
| JDK | 21 |
| iOS | 16.0+ |
| Mitek MiSnap SDK | 5.11.1 |

---

## How it works

```
JavaScript (Ionic/Angular)
        │
        │  call.startDocumentSession({ license, documentType })
        ▼
MitekSdkPlugin.kt  (Capacitor bridge)
        │
        │  builds MiSnapSettings + MiSnapWorkflowStep
        │  calls startActivityForResult → MiSnapWorkflowActivity
        ▼
MiSnapWorkflowActivity  (Mitek SDK — owns all UI, camera, NFC, voice)
        │
        │  user completes capture
        ▼
MitekSdk.kt  (result parser)
        │
        │  reads MiSnapWorkflowActivity.Result
        │  maps sealed MiSnapFinalResult → JSObject
        ▼
JavaScript  ← SessionResult
```

The MiSnap SDK handles everything inside `MiSnapWorkflowActivity`: camera preview, auto-capture, image quality analysis, NFC chip reading, voice recording, and real-time feedback UI. The plugin only passes configuration in and reads results out.

---

## Project Integration

This plugin lives inside your project at `plugins/capacitor-mitek-sdk`. It is referenced as a local file dependency — no npm registry needed.

### Step 1 — Add to your app's `package.json`

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

---

## Android Setup

### 1. GitHub credentials for MiSnap SDK

The MiSnap SDK is distributed via **GitHub Packages** (`maven.pkg.github.com`). GitHub requires a token even for public packages — it is not a Mitek access restriction.

Add to `android/local.properties`:

```properties
mitek.github.username=YOUR_GITHUB_USERNAME
mitek.github.token=YOUR_GITHUB_PAT
```

Generate the PAT at **github.com → Settings → Developer settings → Personal access tokens (classic)** with `read:packages` scope. Since the repository is public, any GitHub account works — no special approval from Mitek required.

> Do not commit `local.properties`. It should already be in `.gitignore`.

For CI/CD, use environment variables instead:

```bash
export MITEK_GITHUB_USERNAME="your-username"
export MITEK_GITHUB_TOKEN="your-pat"
```

### 2. Register the plugin in MainActivity

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

### 3. Obtain a Mitek license key

Contact [Mitek Systems](https://www.miteksystems.com) for a license key scoped to your app's **applicationId** and the features you need.

---

## iOS Setup

Bridge files are in place. All methods return `UNIMPLEMENTED` until the Mitek iOS SDK is integrated.

Add to `ios/App/App/Info.plist`:

```xml
<key>NSCameraUsageDescription</key>
<string>Required to capture identity documents and facial images.</string>
<key>NSMicrophoneUsageDescription</key>
<string>Required to record voice samples for biometric verification.</string>
<key>NFCReaderUsageDescription</key>
<string>Required to read the NFC chip in your identity document.</string>
```

---

## Angular Service

```bash
ionic generate service services/mitek
```

### `src/app/services/mitek.service.ts`

```typescript
import { Injectable } from '@angular/core';
import type {
  NfcSessionOptions,
  SessionResult,
  PermissionStatus,
  MitekPermissionType,
} from 'capacitor-mitek-sdk';
import { MitekSdk } from 'capacitor-mitek-sdk';

@Injectable({ providedIn: 'root' })
export class MitekService {

  private readonly LICENSE = 'YOUR_MITEK_LICENSE_KEY';

  checkPermissions(): Promise<PermissionStatus> {
    return MitekSdk.checkPermissions();
  }

  requestPermissions(permissions?: MitekPermissionType[]): Promise<PermissionStatus> {
    return MitekSdk.requestPermissions(permissions ? { permissions } : undefined);
  }

  capturePassport(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'PASSPORT' });
  }

  captureIdFront(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'ID_FRONT' });
  }

  captureIdBack(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'ID_BACK' });
  }

  captureCheckFront(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'CHECK_FRONT' });
  }

  captureCheckBack(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'CHECK_BACK' });
  }

  captureGenericDocument(): Promise<SessionResult> {
    return MitekSdk.startDocumentSession({ license: this.LICENSE, documentType: 'GENERIC_DOCUMENT' });
  }

  captureFace(): Promise<SessionResult> {
    return MitekSdk.startFaceSession({ license: this.LICENSE });
  }

  scanBarcode(): Promise<SessionResult> {
    return MitekSdk.startBarcodeSession({ license: this.LICENSE });
  }

  captureVoice(phrase?: string): Promise<SessionResult> {
    return MitekSdk.startVoiceSession({ license: this.LICENSE, phrase });
  }

  readNfc(options: Omit<NfcSessionOptions, 'license'>): Promise<SessionResult> {
    return MitekSdk.startNfcSession({ license: this.LICENSE, ...options });
  }
}
```

---

## Session Usage

### Document Capture

| `documentType` | Description |
|----------------|-------------|
| `PASSPORT` | International passport (TD3 MRZ) |
| `ID_FRONT` | National ID card — front |
| `ID_BACK` | National ID card — back |
| `CHECK_FRONT` | Bank cheque — front |
| `CHECK_BACK` | Bank cheque — back |
| `GENERIC_DOCUMENT` | Any document |

```typescript
import { MitekSdk } from 'capacitor-mitek-sdk';

const result = await MitekSdk.startDocumentSession({
  license: 'YOUR_LICENSE_KEY',
  documentType: 'PASSPORT',
});

if (result.success) {
  console.log(result.imageBase64);
  console.log(result.extractedData?.surname);
  console.log(result.extractedData?.documentNumber);
  console.log(result.extractedData?.dateOfBirth);
  console.log(result.extractedData?.dateOfExpiry);
  console.log(result.extractedData?.nationality);
  console.log(result.extractedData?.rawMrz);
}
```

---

### Face Capture

```typescript
const result = await MitekSdk.startFaceSession({ license: 'YOUR_LICENSE_KEY' });

if (result.success) {
  console.log(result.imageBase64);
  console.log(result.rts);
  console.log(result.aiBasedRtsBase64);
}
```

---

### Barcode Scan

```typescript
const result = await MitekSdk.startBarcodeSession({ license: 'YOUR_LICENSE_KEY' });

if (result.success) {
  console.log(result.barcode?.encodedBarcode);
  console.log(result.barcode?.type);
  console.log(result.barcode?.isVds);
}
```

---

### Voice Capture

```typescript
const result = await MitekSdk.startVoiceSession({
  license: 'YOUR_LICENSE_KEY',
  phrase: 'My voice is my password',
});

if (result.success) {
  console.log(result.voiceSampleCount);
  console.log(result.phrase);
}
```

---

### NFC Reading

Supply document credentials so the SDK skips its manual-entry screen.

**Option A — From a prior document session (recommended):**

```typescript
const toYYMMdd = (iso?: string) => iso ? iso.replace(/-/g, '').slice(2) : undefined;

const result = await MitekSdk.startNfcSession({
  license: 'YOUR_LICENSE_KEY',
  documentNumber: docResult.extractedData?.documentNumber,
  dateOfBirth:    toYYMMdd(docResult.extractedData?.dateOfBirth),
  dateOfExpiry:   toYYMMdd(docResult.extractedData?.dateOfExpiry),
  country:        docResult.extractedData?.country,
  documentCode:   docResult.extractedData?.documentType,
});

if (result.success) {
  const chip = result.nfcData;
  console.log(chip?.chipType);       // "ICAO" or "EU_DL"
  console.log(chip?.firstName, chip?.lastName);
  console.log(chip?.gender);
  console.log(chip?.issuingCountry);
  console.log(chip?.chipAuthInfo);
  console.log(chip?.faceImageBase64);
  if (chip?.chipType === 'EU_DL') {
    console.log(chip.vehicleCategories);
    console.log(chip.permanentPlaceOfResidence);
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
const result = await MitekSdk.startNfcSession({ license: 'YOUR_LICENSE_KEY' });
```

---

## Permission Handling

The plugin requests permissions automatically before each session. You can also check and request them manually.

```typescript
const status = await MitekSdk.checkPermissions();
// status.camera / status.audio / status.nfc → "granted" | "denied" | "prompt"

const updated = await MitekSdk.requestPermissions({ permissions: ['camera'] });
const all     = await MitekSdk.requestPermissions(); // requests camera + audio + nfc
```

---

## Full KYC Flow Example

Passport capture → face capture → NFC reading using Angular 17+ signals and Ionic 8 standalone components.

### `kyc.page.ts`

```typescript
import { Component, signal, inject } from '@angular/core';
import {
  IonHeader, IonToolbar, IonTitle, IonContent,
  IonButton, IonIcon, IonCard, IonCardHeader,
  IonCardTitle, IonCardSubtitle, IonCardContent,
  LoadingController, ToastController,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { shieldCheckmarkOutline } from 'ionicons/icons';
import { MitekService } from '../../services/mitek.service';
import type { SessionResult } from 'capacitor-mitek-sdk';

@Component({
  selector: 'app-kyc',
  standalone: true,
  imports: [
    IonHeader, IonToolbar, IonTitle, IonContent,
    IonButton, IonIcon, IonCard, IonCardHeader,
    IonCardTitle, IonCardSubtitle, IonCardContent,
  ],
  templateUrl: './kyc.page.html',
})
export class KycPage {

  private readonly mitek        = inject(MitekService);
  private readonly loadingCtrl  = inject(LoadingController);
  private readonly toastCtrl    = inject(ToastController);

  documentResult = signal<SessionResult | null>(null);
  faceResult     = signal<SessionResult | null>(null);
  nfcResult      = signal<SessionResult | null>(null);
  isLoading      = signal(false);

  constructor() {
    addIcons({ shieldCheckmarkOutline });
  }

  async startKyc() {
    const loader = await this.loadingCtrl.create({ message: 'Preparing…' });
    await loader.present();
    this.isLoading.set(true);

    try {
      const perms = await this.mitek.checkPermissions();
      if (perms.camera !== 'granted') {
        const req = await this.mitek.requestPermissions(['camera']);
        if (req.camera !== 'granted') {
          await this.showToast('Camera permission is required.', 'warning');
          return;
        }
      }

      loader.message = 'Place your passport flat in view…';
      const doc = await this.mitek.capturePassport();
      if (!doc.success) {
        if (doc.errorCode !== 'SESSION_CANCELLED') {
          await this.showToast('Document capture failed: ' + doc.errorMessage, 'danger');
        }
        return;
      }
      this.documentResult.set(doc);
      await this.showToast('Passport captured.');

      loader.message = 'Look directly at the camera…';
      const face = await this.mitek.captureFace();
      if (!face.success) {
        if (face.errorCode !== 'SESSION_CANCELLED') {
          await this.showToast('Face capture failed: ' + face.errorMessage, 'danger');
        }
        return;
      }
      this.faceResult.set(face);
      await this.showToast('Face captured.');

      loader.message = 'Hold your passport near the top of the phone…';
      const toYYMMdd = (iso?: string) => iso ? iso.replace(/-/g, '').slice(2) : undefined;
      const nfc = await this.mitek.readNfc({
        documentNumber: doc.extractedData?.documentNumber,
        dateOfBirth:    toYYMMdd(doc.extractedData?.dateOfBirth),
        dateOfExpiry:   toYYMMdd(doc.extractedData?.dateOfExpiry),
        country:        doc.extractedData?.country,
        documentCode:   doc.extractedData?.documentType,
      });
      this.nfcResult.set(nfc.success ? nfc : null);
      if (!nfc.success && nfc.errorCode !== 'SESSION_CANCELLED') {
        await this.showToast('NFC reading failed: ' + nfc.errorMessage, 'warning');
      }

      await this.showToast('KYC complete.', 'success');

    } catch (err) {
      console.error('[KycPage]', err);
      await this.showToast('An unexpected error occurred.', 'danger');
    } finally {
      this.isLoading.set(false);
      await loader.dismiss();
    }
  }

  private async showToast(message: string, color = 'primary') {
    const t = await this.toastCtrl.create({ message, duration: 3000, color });
    await t.present();
  }
}
```

### `kyc.page.html`

```html
<ion-header>
  <ion-toolbar>
    <ion-title>Identity Verification</ion-title>
  </ion-toolbar>
</ion-header>

<ion-content class="ion-padding">

  <ion-button expand="block" (click)="startKyc()" [disabled]="isLoading()">
    <ion-icon name="shield-checkmark-outline" slot="start"></ion-icon>
    Start KYC
  </ion-button>

  @if (documentResult()?.success) {
    <ion-card>
      <ion-card-header>
        <ion-card-title>Passport</ion-card-title>
        <ion-card-subtitle>{{ documentResult()?.classification }}</ion-card-subtitle>
      </ion-card-header>
      <ion-card-content>
        <img [src]="'data:image/jpeg;base64,' + documentResult()?.imageBase64"
             style="max-width:100%; border-radius:8px;" />
        <p><strong>Name:</strong> {{ documentResult()?.extractedData?.firstName }} {{ documentResult()?.extractedData?.surname }}</p>
        <p><strong>Doc #:</strong> {{ documentResult()?.extractedData?.documentNumber }}</p>
        <p><strong>DOB:</strong>   {{ documentResult()?.extractedData?.dateOfBirth }}</p>
        <p><strong>Expiry:</strong>{{ documentResult()?.extractedData?.dateOfExpiry }}</p>
      </ion-card-content>
    </ion-card>
  }

  @if (faceResult()?.success) {
    <ion-card>
      <ion-card-header>
        <ion-card-title>Selfie</ion-card-title>
      </ion-card-header>
      <ion-card-content>
        <img [src]="'data:image/jpeg;base64,' + faceResult()?.imageBase64"
             style="max-width:180px; border-radius:50%; display:block; margin:auto;" />
      </ion-card-content>
    </ion-card>
  }

  @if (nfcResult()?.success) {
    <ion-card>
      <ion-card-header>
        <ion-card-title>NFC — {{ nfcResult()?.nfcData?.chipType }}</ion-card-title>
      </ion-card-header>
      <ion-card-content>
        @if (nfcResult()?.nfcData?.faceImageBase64) {
          <img [src]="'data:image/jpeg;base64,' + nfcResult()!.nfcData!.faceImageBase64"
               style="max-width:120px; border-radius:50%; display:block; margin:auto 0 12px;" />
        }
        <p><strong>Name:</strong>    {{ nfcResult()?.nfcData?.firstName }} {{ nfcResult()?.nfcData?.lastName }}</p>
        <p><strong>Gender:</strong>  {{ nfcResult()?.nfcData?.gender }}</p>
        <p><strong>Country:</strong> {{ nfcResult()?.nfcData?.issuingCountry }}</p>
      </ion-card-content>
    </ion-card>
  }

</ion-content>
```

---

## API Reference

### `startDocumentSession(options)`

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `license` | `string` | ✅ | — | Mitek license key |
| `documentType` | `DocumentType` | — | `'PASSPORT'` | Document type |

### `startFaceSession(options)`

| Parameter | Type | Required |
|-----------|------|----------|
| `license` | `string` | ✅ |

### `startBarcodeSession(options)`

| Parameter | Type | Required |
|-----------|------|----------|
| `license` | `string` | ✅ |

### `startVoiceSession(options)`

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `license` | `string` | ✅ | |
| `phrase` | `string` | — | Phrase for the user to speak |

### `startNfcSession(options)`

| Parameter | Type | Description |
|-----------|------|-------------|
| `license` | `string` | ✅ |
| `documentNumber` | `string` | Doc number (9 chars) |
| `dateOfBirth` | `string` | `YYMMdd` format |
| `dateOfExpiry` | `string` | `YYMMdd` format |
| `country` | `string` | 3-letter country code |
| `documentCode` | `string` | 2-letter document type |
| `mrzLine1` | `string` | Raw MRZ line 1 |
| `mrzLine2` | `string` | Raw MRZ line 2 |
| `mrzLine3` | `string` | Raw MRZ line 3 (optional) |

### `SessionResult`

```typescript
interface SessionResult {
  success: boolean;
  sessionType: string;
  imageBase64?: string;
  extractedData?: {
    firstName?: string; surname?: string; sex?: string;
    dateOfBirth?: string; dateOfExpiry?: string;
    nationality?: string; country?: string;
    documentNumber?: string; documentType?: string;
    optionalData1?: string; optionalData2?: string;
    rawMrz?: string;
  };
  nfcData?: {
    chipType?: string;               // "ICAO" | "EU_DL"
    documentNumber?: string;
    documentCode?: string;
    firstName?: string; lastName?: string;
    gender?: string;
    nationality?: string; issuingCountry?: string;
    dateOfBirth?: string; dateOfExpiry?: string;
    dateOfIssue?: string; personalNumber?: string;
    placeOfBirth?: string;
    permanentPlaceOfResidence?: string;  // EU_DL only
    faceImageBase64?: string;
    chipAuthInfo?: string; activeAuthInfo?: string;
    vehicleCategories?: string[];        // EU_DL only
  };
  barcode?: {
    encodedBarcode?: string;
    type?: string;
    isVds?: boolean;
  };
  classification?: string;
  rts?: string;
  aiBasedRtsBase64?: string;
  voiceSampleCount?: number;
  phrase?: string;
  errorCode?: string;
  errorMessage?: string;
}
```

---

## Error Codes

| `errorCode` | When |
|-------------|------|
| `LICENSE_MISSING` | `license` field not provided |
| `LICENSE_INVALID` | SDK rejected the license key |
| `PERMISSION_DENIED` | OS permission not granted |
| `SESSION_CANCELLED` | User dismissed the capture UI |
| `CAMERA_ERROR` | Camera hardware unavailable |
| `ANALYSIS_ERROR` | SDK frame analysis failure |
| `NFC_ERROR` | NFC chip could not be read |
| `VOICE_ERROR` | Voice recording failure |
| `SETTINGS_ERROR` | Invalid session configuration |
| `UNIMPLEMENTED` | Platform not yet supported (iOS) |
| `UNKNOWN_ERROR` | Unexpected error |

```typescript
const result = await MitekSdk.startDocumentSession({ license: '...', documentType: 'PASSPORT' });

if (!result.success) {
  switch (result.errorCode) {
    case 'SESSION_CANCELLED': break;
    case 'LICENSE_INVALID':
      console.error('Check your Mitek license key and applicationId.');
      break;
    case 'PERMISSION_DENIED':
      // guide user to Settings
      break;
    default:
      console.error(result.errorCode, result.errorMessage);
  }
}
```

---

## Troubleshooting

**`Could not resolve com.miteksystems.misnap:workflow:5.11.1`**  
Gradle cannot reach GitHub Packages. Confirm `mitek.github.username` and `mitek.github.token` are set in `android/local.properties` and the PAT has `read:packages` scope.

**`LICENSE_INVALID` at runtime**  
The license key does not match the app's `applicationId` or the feature is not enabled for that key. Confirm with Mitek.

**Session does not start after granting camera permission**  
Handled automatically — the plugin relaunches the session inside `onCameraPermission`. If it still fails, confirm no other app holds the camera.

**NFC session fails immediately**  
Check that NFC is enabled in device Settings and the document is NFC-enabled (chip icon on the document).

**`extractedData` is empty**  
The image was captured but OCR did not succeed. Prompt the user to retry in better lighting.

---

## Plugin Structure

```
plugins/capacitor-mitek-sdk/
├── src/
│   ├── definitions.ts          TypeScript interfaces
│   ├── index.ts                Plugin registration + exports
│   └── web.ts                  Browser stub
├── android/
│   ├── build.gradle            MiSnap SDK Gradle dependencies
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/mitek/capacitor/
│           ├── MitekSdkPlugin.kt   Capacitor bridge — launches MiSnapWorkflowActivity
│           └── MitekSdk.kt         Builds steps + parses MiSnapFinalResult
├── ios/Plugin/
│   ├── MitekSdkPlugin.m
│   ├── MitekSdkPlugin.swift    (stub)
│   └── MitekSdk.swift          (stub)
├── capacitor-mitek-sdk.podspec
├── package.json
├── rollup.config.js
└── tsconfig.json
```
