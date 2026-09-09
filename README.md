# Field TAK Hub

**Field TAK Hub** is a local-first provisioning toolkit for ATAK deployments. It is designed for controlled teams, training events and field operations where an administrator needs to prepare many Android devices consistently without manually repeating the same ATAK, plugin, map and OpenTAK setup on every phone.

This repository contains:

- **Field TAK Hub 2.0.1 RC3 for Android** — scans a provisioning QR, downloads or imports a signed `.ftak` bundle, verifies it, checks device readiness and guides the user through ATAK/plugin/data provisioning.
- **Field TAK Hub Builder 2.1.0 RC5 for Windows** — creates signed `.ftak` packages, builds ATAK Mission Packages, edits TAK server profiles, serves packages over LAN and generates provisioning QR codes.

> Status: **Release Candidate 3 (Android) / Release Candidate 5 (Builder)**, not yet Stable. The feature scope is frozen. RC testing should focus on build/signing, device compatibility, interrupted deployments, update paths and field reliability.

## Key features

### Android Hub

- persistent deployment state machine with resume after restart
- resumable HTTP Range downloads
- `.ftak` v2 verification with SHA-256 + Ed25519
- trusted publisher model
- ATAK CIV detection and target-version checks
- ATAK plugin APK version comparison
- system-guided APK installation and Mission Package hand-off
- pre-flight storage/network/ATAK/plugin checks
- OpenTAK DNS/TCP diagnostics
- readiness percentage, checklist, history and retry
- service report export
- storage cleanup and stale partial-download cleanup
- **Polish and English UI**
- **TalkBack/accessibility semantics, large-text friendly layout, minimum touch targets and status text that does not rely on color alone**
- **GitHub Releases update checker** with release-channel filtering, HTTPS-only update transport and SHA-256 verification before Android installer hand-off

### Windows Builder

Builder 2.1.0 RC5 automatically creates and repairs the project workspace (`source/atak`, `plugins`, `maps`, `overlays`, `config`, `data`, and `out`). **New Project** creates a ready-to-use portable project, project-folder rows support drag-and-drop, and existing files are never silently overwritten. See `docs/BUILDER_2_1_RC4_FROM_SCRATCH_EN.md` (workspace flow remains valid for RC5).

RC5 keeps the clearer pre-dark-theme Builder layout while retaining the 1.9 application icon. It also adds **Import legacy 1.x ZIP**: a signed 1.x deployment can be verified (RSA + SHA-256), migrated into a fresh 2.x project and rebuilt as an Ed25519-signed `.ftak` v2. A single ATAK APK placed in `source/atak/` is now emitted as `payload/atak/atak.apk` instead of being nested inside the Mission Package.


- portable `.fthproj` projects with relative source/output paths
- editable TAK server fields in the GUI
- human-editable `server.txt` import/export
- automatic ATAK `server.pref` generation when no custom `.pref` is supplied
- source analysis and Build Preview
- server validation and **Test server** (DNS/TCP/TLS)
- `.ftak` v2 + Mission Package generation
- Ed25519 publisher signing with DPAPI-protected local key
- encrypted `.fthkey` publisher-key backup/restore using PBKDF2-SHA256 + AES-256-GCM
- LAN HTTP distribution with expiring token, max-download limit and HTTP Range/206 support
- external/cloud HTTPS distribution with direct `.ftak` link test, SHA-256-bound QR and PNG export
- QR deep links (`fieldtak://provision?...`)
- **Polish and English UI**
- keyboard/Narrator/DPI/high-contrast friendly WPF controls
- **GitHub Releases update checker** with channel filtering and SHA-256 verification

## Quick start

### 1. Build the Windows Builder

Requirements:

- Windows 10/11 x64
- .NET 10 SDK

```powershell
.\scripts\build-windows.ps1
```

Or:

```powershell
cd apps\builder
dotnet restore .\FieldTakHub.Builder\FieldTakHub.Builder.csproj
dotnet build .\FieldTakHub.Builder\FieldTakHub.Builder.csproj -c Release
```

### 2. Build Android

Requirements:

- JDK 17
- Android SDK Platform 37
- Gradle 9.6

First-time wrapper/bootstrap helper:

```powershell
.\scripts\bootstrap-android.ps1
```

Then:

```powershell
.\scripts\build-android.ps1
```

For a production release APK, configure `apps/android/keystore.properties` from `keystore.properties.example` and run:

```powershell
cd apps\android
gradle assembleRelease
```

### 3. Run the source release gate

```powershell
.\scripts\run-source-gate.ps1
```

or:

```bash
python tests/source_gate.py
```

The gate verifies version consistency, PL/EN resource parity, JSON/XML/XAML syntax, expected release files, updater/release workflow requirements and accidental secret/certificate files.

## Example OpenTAK profile

`examples/ggzs-basic/GGZS-Milsim.fthproj` contains non-secret sample values:

- host: `ggzstak.duckdns.org`
- CoT: `8089`
- API / enrollment: `8446`
- web: `8443`
- ATAK target: `5.6` through `5.8`

These are **example/default values only**. In Builder 2.1.0 RC5 you can edit the server host and ports directly in the GUI, import a `server.txt`, or edit the project file. Building the package regenerates the signed server metadata and, unless you supplied your own `.pref`, the ATAK `server.pref`.

## Typical field workflow

```text
Administrator
    ↓
Open/create .fthproj
    ↓
Select source folder
    ↓
Edit/test TAK server
    ↓
Analyze → Build Preview → Build .ftak
    ↓
Start LAN distribution OR paste/test direct cloud HTTPS URL
    ↓
Generate/show QR

Player
    ↓
Open Field TAK Hub
    ↓
Scan QR
    ↓
Verify publisher/package
    ↓
Pre-flight
    ↓
Finish configuration
    ↓
Confirm final authenticated ATAK connection inside ATAK
```

## Update system

The Hub and Builder can check GitHub Releases. The default repository is `Qbcol/TAKFieldHub`; change it before publishing if your actual repository path differs.

- Android default is compiled through `BuildConfig.UPDATE_REPOSITORY`.
- Builder reads `update-config.json` next to the executable.
- RC builds only consider GitHub **prereleases**.
- Stable builds only consider non-prerelease releases.
- Update metadata is read from the release asset `fieldtak-release.json`.
- Update assets are verified against SHA-256 before they are handed to the OS/user.
- Neither component silently installs an update.

See [`docs/UPDATES.md`](docs/UPDATES.md).

## Security model

The project intentionally does **not** write into ATAK's private `Android/data` tree. Normal Android package installation remains subject to Android's user confirmation/install-source permission. Mission Package import is handed to ATAK through supported Android intents.

Provisioning downloads may use HTTP **only for private/loopback LAN addresses** so the Windows Builder can work without Internet access. Public provisioning and all application updates require HTTPS. Every `.ftak` package is still verified with SHA-256 and Ed25519 before use.

The QR scanner can hand ATAK/OpenTAK `tak://.../enroll` and `tak://.../import` URIs to ATAK without persisting enrollment tokens. Field TAK Hub cannot directly inspect ATAK's private certificate store or prove an authenticated CoT session on stock Android, so those states are reported as **UNVERIFIED** rather than falsely marked successful.

Read [`SECURITY.md`](SECURITY.md) and [`docs/SECURITY_MODEL.md`](docs/SECURITY_MODEL.md) before using real certificates.

## Documentation

- [`docs/ADMIN_GUIDE.md`](docs/ADMIN_GUIDE.md) — operator workflow
- [`docs/BUILD_AND_SIGN.md`](docs/BUILD_AND_SIGN.md) — local and CI builds/signing
- [`docs/UPDATES.md`](docs/UPDATES.md) — Hub/Builder update system
- [`docs/ACCESSIBILITY.md`](docs/ACCESSIBILITY.md) — PL/EN and accessibility behavior
- [`docs/FTAK_FORMAT.md`](docs/FTAK_FORMAT.md) — package format
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — architecture overview
- [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md) — RC → Stable checklist
- [`docs/RC2_VALIDATION.md`](docs/RC2_VALIDATION.md) — checks performed and remaining release gates
- [`TEST_DZISIAJ_RC3_RC5_PL.md`](TEST_DZISIAJ_RC3_RC5_PL.md) — szybki scenariusz testu RC3/RC5 na telefonie i w Builderze

## Release policy

The intended distribution model is **GitHub Releases / controlled sideloading**. `QUERY_ALL_PACKAGES` is used so a controlled deployment can inspect arbitrary plugin package/version metadata. A future Google Play release would require a separate Play policy review and likely a narrower package-discovery strategy.

## License

See [`LICENSE`](LICENSE).
