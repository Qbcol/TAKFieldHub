# Changelog

## Field TAK Hub 2.0.1 RC1 / Builder 2.1.0 RC1 — 2026-09-09

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
