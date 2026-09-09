# Build and Signing Guide

## Windows Builder

Requirements: Windows 10/11 x64 and .NET 10 SDK.

```powershell
.\scripts\build-windows.ps1
```

The publish script creates a self-contained win-x64 output under `artifacts/windows/`.

### Authenticode

RC builds may be distributed unsigned for testing, but **Stable should be Authenticode-signed**. GitHub Actions supports optional secrets:

- `WINDOWS_CERT_BASE64`
- `WINDOWS_CERT_PASSWORD`

The release workflow signs `FieldTakHub.Builder.exe` before creating the release ZIP when the certificate secret is present.

## Android

Requirements: JDK 17, Android SDK Platform 37 and Gradle 9.6.

Create `apps/android/keystore.properties` from the example:

```properties
storeFile=../release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then:

```powershell
cd apps\android
gradle testDebugUnitTest assembleRelease
```

### GitHub release signing secrets

The release workflow requires:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

A release tag fails if the Android keystore is missing; this prevents accidentally publishing a debug/unsigned APK.

## Source gate

Run before every release:

```bash
python tests/source_gate.py
```

## Release tag

RC1 uses:

```bash
git tag -a v2.0.1-rc3-builder-2.1.0-rc5 -m "Field TAK Hub 2.0.1 RC3 / Builder 2.1.0 RC5"
git push origin v2.0.1-rc3-builder-2.1.0-rc5
```

The workflow publishes:

- `FieldTAKHub-2.0.1-RC2.apk`
- `FieldTAKHub-Builder-2.1.0-RC4-win-x64.zip`
- `fieldtak-release.json`
- `SHA256SUMS.txt`
