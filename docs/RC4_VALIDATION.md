# Builder 2.1.0 RC4 Validation Report

Target: **Field TAK Hub 2.0.1 RC2 / Builder 2.1.0 RC4**

## Source-level validation in this package

- Source Gate
- PL/EN resource parity
- JSON/XML/XAML parse
- legacy importer contract: RSA PKCS#1 v1.5 + SHA-256 verification and per-file SHA-256 verification
- direct ATAK APK contract: one APK from `source/atak` -> `payload/atak/atak.apk`
- ATAK APK excluded from generated Mission Package
- `.ftak` exact signed-file contract unchanged
- Android 1.9-derived launcher visual assets retained
- Builder 1.9 application icon retained while the WPF layout is restored to the clearer pre-dark-theme layout

## Still required before promotion

This container does not provide the Windows .NET/WPF SDK or full Android SDK. Run GitHub Actions for **Source Gate**, **Android** and **Windows Builder**, then perform one Windows Builder smoke test and one physical Android provisioning test.
