# Field TAK Hub 2.0.1 RC4 / Builder 2.1.0 RC7 — Project Status

## Implemented in RC1

- complete `.ftak` v2 signing/verification architecture retained from 2.0
- persistent deployment/resume engine
- pre-flight, ATAK/plugin checks, history and service diagnostics
- resumable LAN downloads
- editable TAK server GUI + `server.txt` + generated `server.pref`
- Build Preview and stricter server/project validation
- Builder server DNS/TCP/TLS test
- storage sizing metadata + Android free-space check
- Android stale-storage cleanup
- publisher-key encrypted backup/import
- English/Polish UI in Hub and Builder
- Android TalkBack/readiness/progress semantics and touch-target improvements
- Builder keyboard/Narrator/DPI-oriented accessibility metadata
- GitHub Releases auto-update check for Hub and Builder
- separate RC/stable release-channel selection
- HTTPS-only update transport + SHA-256 artifact verification
- public HTTP provisioning rejected; private LAN HTTP retained
- release workflow builds signed Android Release APK
- optional Windows Authenticode signing in RC; required by policy before Stable
- source release gate and Builder/Android unit-test hooks

## Local validation limitation

This generation environment has Java/Kotlin and Python, but does not contain a complete Android SDK/Gradle toolchain or Windows .NET/WPF SDK. Therefore Android `assembleRelease` and WPF `dotnet publish` must be validated by GitHub Actions or local supported machines.

The repository is intentionally kept in the **RC channel** until the refreshed Android/Builder builds and the real-device checklist pass.


## GitHub Actions RC1 hotfix

The first browser-driven GitHub Actions run exposed CI-only build blockers that the source gate could not detect. The RC1 source now includes the corresponding fixes for Android SDK 37 preview provisioning and Windows Builder compilation. A new GitHub Actions run is still required to confirm the next full compile stage.

## Validation completed in this environment

- source gate: PASS
- Android PL/EN resource parity: 183/183
- Builder PL/EN resource parity: 122/122
- all JSON, XML, XAML and GitHub Actions YAML parsed successfully
- pure Kotlin URL-policy/version utilities compiled with local `kotlinc`
- release-manifest generator contract test passed with SHA-256 verification
- `git diff --check` passed
- `.ftak` exact signed-file contract statically checked, including signed `META-INF/server.txt`
- provisioning descriptor package-size/free-space hints checked end-to-end in source

Two defects were found and fixed during RC validation: prerelease update discovery and the signed `META-INF/server.txt` verifier mismatch.

## GitHub Actions RC1 CI Hotfix 2

The second browser-driven CI run progressed further and exposed compile-time issues in Android UI wiring and two Builder source files. RC1 now includes fixes for the Compose `setContent` import, the SCAN back callback, the `Long` download progress pair, explicit Builder `System.IO` imports, and Builder test imports. The obsolete LAN URL escaping warning was also removed. A new GitHub Actions run is required to validate the next compile stage.

## RC1 CI Hotfix 3

Real GitHub Actions exposed additional Windows Builder compile errors after Hotfix 2. All remaining Builder files using `Path`, `File`, `Directory`, IO exceptions or stream types now import `System.IO` explicitly. The source gate has been generalized to prevent the same class of error from recurring.


## Builder RC2 workspace UX

Builder now creates and repairs the complete project folder tree automatically, provides a New Project wizard, per-folder open/count controls and drag-and-drop. Existing project files are preserved.


## Builder RC4 readability + legacy migration

Field TAK Hub Android 2.0.1 RC2 keeps the 1.9-inspired launcher/visual identity. Builder 2.1.0 RC4 deliberately restores the clearer pre-dark-theme WPF layout after RC3 field feedback, while retaining the 1.9 Builder application icon.

RC4 also adds a controlled **Import legacy 1.x ZIP** path. Legacy `manifest.json`, RSA signature and per-file SHA-256 values are verified before files are copied into a new 2.x workspace. Rebuilding signs the resulting `.ftak` v2 with the current Ed25519 Publisher key. A single ATAK APK in `source/atak/` is now packaged as `payload/atak/atak.apk`; other ATAK import data still becomes a Mission Package.

The earlier RC1 code path reached green GitHub Actions for Android, Windows Builder and Source Gate. This exact RC4 commit must still pass the three workflows before release promotion.


## RC4 / RC7 dedicated OpenTAK QR workflows

- Android RC4 keeps the RC2/RC3 layout and adds three dedicated QR buttons without changing the overall visual structure.
- `DOKOŃCZ KONFIGURACJĘ` now opens the per-app unknown-sources setting when required and resumes after permission is granted.
- QR scanner now has separate provisioning, enrollment and Data Package entry points; the universal deep-link router remains for backwards compatibility.
- Builder RC7 retains RC6 direct HTTPS/Google Drive cloud distribution with URL probe, local SHA-256 binding, expiry, QR PNG/TXT output and no TLS bypass.
- `.ftak` schema stays at v2.
- This exact RC4/RC7 source still requires a fresh Android + Windows Builder GitHub Actions run before release promotion.

### Dedicated QR actions in Android RC4
- Prepare phone / Field TAK provisioning QR.
- OpenTAKServer user enrollment QR (`tak://com.atakmap.app/enroll?...`).
- OpenTAKServer/ATAK Data Package import QR or safe Data Package URL.
- Enrollment tokens are not persisted/logged; enrollment is handed to ATAK.
- The Android visual layout remains the RC2/RC3 layout requested for field use.
