# Field TAK Hub 2.0.1 RC1 / Builder 2.1.0 RC1 — Project Status

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

The repository is intentionally marked **RC1** until those builds and the real-device checklist pass.


## GitHub Actions RC1 hotfix

The first browser-driven GitHub Actions run exposed CI-only build blockers that the source gate could not detect. The RC1 source now includes the corresponding fixes for Android SDK 37 preview provisioning and Windows Builder compilation. A new GitHub Actions run is still required to confirm the next full compile stage.

## Validation completed in this environment

- source gate: PASS
- Android PL/EN resource parity: 162/162
- Builder PL/EN resource parity: 78/78
- all JSON, XML, XAML and GitHub Actions YAML parsed successfully
- pure Kotlin URL-policy/version utilities compiled with local `kotlinc`
- release-manifest generator contract test passed with SHA-256 verification
- `git diff --check` passed
- `.ftak` exact signed-file contract statically checked, including signed `META-INF/server.txt`
- provisioning descriptor package-size/free-space hints checked end-to-end in source

Two defects were found and fixed during RC validation: prerelease update discovery and the signed `META-INF/server.txt` verifier mismatch.
