# Google Play Data Safety Declaration

**Application Name**: Nói Chuẩn (Voice Filter: Speak Challenge)  
**Package Name**: `com.antigravity.noichuan`  
**Target Audience**: Age 13+ (English language learners, casual challenges)  
**Last Updated**: 2026-09-30

---

## 1. Overview of Data Practices

| Category | Collected? | Shared with Third Parties? | Processed Ephemerally? | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Audio / Microphone** | Yes (Locally) | **No** (When using default Vosk engine) | Yes | Real-time speech recognition and pronunciation scoring on-device. Audio is recorded locally into user's challenge video clip. |
| **Camera / Photos & Videos** | Yes (Locally) | **No** | Yes | Displays front-camera selfie filter and renders 3D interactive runway gates. Saved strictly to device's public media storage upon user tap. |
| **Personal Info (Name, Email)** | **No** | **No** | N/A | App does not require user accounts or login. |
| **Location** | **No** | **No** | N/A | No geolocation collected or requested. |
| **Device Identifiers** | **No** | **No** | N/A | No advertising ID (AAID) or hardware identifiers collected. |
| **App Performance & Diagnostics** | **No** | **No** | N/A | Local logcat only; no crash-reporting telemetry sent off-device. |

---

## 2. Detailed Permissions Breakdown

### 2.1. Microphone (`android.permission.RECORD_AUDIO`)
- **Usage**: Used during game rounds to capture user speech.
- **Engine Handling**:
  - **Vosk Engine (Default)**: Audio PCM data is processed 100% on-device using embedded native Kaldi/Vosk libraries. Zero voice data is uploaded to remote servers.
  - **Google Engine (Optional Fallback)**: If explicitly enabled by the user in Settings, uses the system `RecognitionService` provided by Google.

### 2.2. Camera (`android.permission.CAMERA`)
- **Usage**: Displays real-time camera preview for the AR video filter challenge.
- **Optional Graceful Fallback**: If camera permission is denied, the game continues without interruption using an animated geometric gradient background.

### 2.3. Foreground Service (`FOREGROUND_SERVICE_MEDIA_PROJECTION`)
- **Usage**: Enables high-quality screen recording combining the camera feed, 3D animated canvas, and game HUD into a final MP4 video saved in `Movies/NoiChuan`.

### 2.4. Storage (`WRITE_EXTERNAL_STORAGE` maxSdkVersion 28)
- **Usage**: Used only on legacy Android versions (Android 9 and below) to save exported gameplay videos to the Movies folder. Modern Android versions use scoped storage via `MediaStore.Video`.

---

## 3. Security & Privacy Practices
- **Data Encryption**: All optional network communication (such as content catalog sync) strictly enforces TLS 1.3 HTTPS.
- **Data Deletion**: Users can delete any recorded video directly within the app's Profile screen, which removes both the Room database index and the underlying MP4 file from disk.
- **No Third-Party Ad Networks**: The application contains no third-party tracking SDKs or behavioral ad network libraries.
