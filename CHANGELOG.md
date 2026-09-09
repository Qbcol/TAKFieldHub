# Changelog

## 2.0.1 RC3 / Builder 2.1.0 RC5 — 2026-09-09

### Android
- fixed `FINISH CONFIGURATION` / `DOKOŃCZ KONFIGURACJĘ` when ATAK is missing and install-source permission is not yet granted;
- automatically opens the per-app unknown-sources setting and resumes after permission is granted;
- added universal QR routing for Field TAK cloud provisioning, OpenTAK/ATAK enrollment and ATAK data-package import;
- supports `tak://com.atakmap.app/enroll?...` and `tak://com.atakmap.app/import?url=...` without persisting enrollment tokens;
- supports direct `.ftak` HTTPS URLs and OTS-style `/api/data_packages...` links;
- kept the RC2 Android layout unchanged.

### Builder
- added **External / cloud distribution** next to LAN;
- added local `.ftak` selector, direct HTTPS URL test, SHA-256-bound cloud QR, copy deep-link and QR PNG export;
- cloud QR includes package URL, SHA-256, byte size and expiry;
- cloud link test keeps normal TLS certificate validation and rejects HTML landing pages/public HTTP.


## 2.0.1 RC2 / Builder 2.1.0 RC4 — 2026-09-09

### Builder
- restored the clearer pre-dark-theme Builder layout while keeping the 1.9 application icon;
- added **Import legacy 1.x ZIP** with legacy RSA signature and SHA-256 verification;
- imported legacy deployments become normal 2.x workspaces and are rebuilt with the current Ed25519 Publisher key;
- added direct ATAK APK packaging: one APK in `source/atak/` becomes `payload/atak/atak.apk`;
- ATAK APK files are excluded from the generated ATAK Mission Package;
- retained automatic project folders, drag-and-drop, server profile GUI, Build Preview and LAN QR distribution;
- corrected the Builder update repository default to `Qbcol/TAKFieldHub` (the previous default could return GitHub 404).

### Android
- no provisioning/security contract change from 2.0.1 RC2; `.ftak` schema v2 remains unchanged.


## 2.0.1 RC2 / Builder 2.1.0 RC3 — 2026-09-09

- restored the TAK Field Hub 1.9 launcher/application icon,
- restored the 1.9 dark/cyan visual palette on Android and Windows Builder,
- added branded `PROVISION • DEPLOY • READY` headers,
- emphasized primary provisioning/build actions while preserving accessibility,
- kept `.ftak` schema v2 and provisioning QR contracts unchanged.

## Builder 2.1.0 RC3 — 2026-09-09

- automatic workspace creation on startup,
- New Project wizard,
- automatic creation/repair of source/atak, plugins, maps, overlays, config, data and out,
- per-folder Open controls and file counts,
- drag-and-drop into project folders with collision-safe filenames,
- bilingual source-folder README,
- tests and source-gate coverage for workspace automation.

# Changelog

## Field TAK Hub 2.0.1 RC2 / Builder 2.1.0 RC1 — 2026-09-09

### Added

- full English/Polish UI resources for Android and Builder
- Android accessibility semantics, explicit status text and large-text/touch-target improvements
- Builder keyboard/Narrator/DPI/high-contrast-oriented accessibility metadata
- Hub and Builder GitHub Releases update checks with RC/stable channel selection
- HTTPS-only update downloads with SHA-256 verification
- Android private-LAN-only policy for cleartext provisioning URLs
- Builder Test server (DNS/TCP/TLS)
- Build Preview before package creation
- encrypted publisher-key backup/import (`.fthkey`)
- signed manifest size/recommended-free-space metadata
- Android storage cleanup and storage-schema migration
- source release gate and expanded CI
- provisioning descriptors include package-size and recommended-free-space hints for pre-download storage checks
- signed Android release workflow and optional Authenticode step

### Changed

- release pipeline now publishes `fieldtak-release.json`
- RC updater uses the GitHub releases list instead of `/releases/latest` so prereleases are discovered correctly
- project status moved from FINAL naming to explicit RC1 until device/build gates pass

### Fixed

- fixed second real GitHub Actions compile blockers: imported `androidx.activity.compose.setContent`, corrected the SCAN back-button callback, corrected the download progress pair to use `Long`, and added explicit `System.IO` imports required by the Builder compiler
- hardened Builder test compilation with explicit `System`, `System.IO`, and `System.Linq` imports
- replaced obsolete `Uri.EscapeUriString` usage in the LAN distribution URL with segment-safe `Uri.EscapeDataString` encoding
- fixed GitHub Actions Android SDK provisioning for the Android 17 preview package (`platforms;android-37.0`) and added a compatibility alias for tools resolving `compileSdk = 37`
- fixed Windows Builder CI compilation blockers reported by the first real GitHub Actions run: removed WinForms namespace collisions, switched folder selection to WPF `OpenFolderDialog`, added explicit IO/HTTP imports, and renamed the Mission Package XML helper that collided with the internal `Entry` record
- removed the redundant `System.Security.Cryptography.ProtectedData` package reference on .NET 10 Windows
- added manual `workflow_dispatch` triggers to Source Gate, Android, and Windows Builder workflows for browser-only reruns
- changed Builder package/update SHA-256 calculation to streaming I/O so multi-gigabyte maps and bundles do not require equivalent RAM
- fixed `.ftak` integrity-contract mismatch where Builder signed `META-INF/server.txt` but the Android verifier did not include it in the exact signed-file set
- fixed RC update discovery by using the GitHub releases list rather than `/releases/latest`, which does not surface prereleases as required

### Security

- update redirects are revalidated as HTTPS
- public cleartext provisioning is rejected while private LAN Builder HTTP remains supported

### RC1 CI Hotfix 3

- Fixed remaining Windows Builder compilation blockers caused by missing explicit `System.IO` imports across project, mission package, source analysis, publisher-key, server-text and validation services.
- Source Gate now scans all Builder C# files for System.IO symbols and rejects missing imports before GitHub Actions reaches `dotnet test`.
