# RC1 Validation Report

Target: **Field TAK Hub 2.0.1 RC1 / Builder 2.1.0 RC1**

## Passed source-level checks

- source release gate
- Android English/Polish resource parity (162 keys per locale)
- Builder English/Polish resource parity (78 keys per locale)
- JSON, XML, XAML and GitHub Actions YAML parsing
- Python release scripts compile and execute
- pure Kotlin URL-policy and version utilities compile with the available Kotlin compiler
- release-manifest generator emits the expected Hub/Builder versions, channel, asset names and SHA-256 values
- Git whitespace/error check
- no production P12/PFX/JKS/keystore/private-key files committed
- updater transport contract: HTTPS-only for application updates
- provisioning cleartext contract: HTTP allowed only for private/loopback LAN destinations by application policy
- provisioning descriptor includes pre-download size hints

## Defects found and fixed during validation

1. **RC update discovery** — GitHub `/releases/latest` does not provide the required prerelease behavior. Both updaters now enumerate releases and choose the newest release matching the configured `rc` or `stable` channel.
2. **`.ftak` signed-file contract** — Builder signs `META-INF/server.txt`; the Android verifier now includes every package file in the exact signed set except the three self-referential cryptographic metadata files (`checksums.sha256`, `signature.ed25519`, `publisher.pub`).
3. **Pre-download storage guard** — the LAN descriptor now carries `packageBytes` and `recommendedFreeBytes`; Android checks available storage before downloading when those hints are present.

## Not validated in this container

This environment does not provide the complete Android SDK/Gradle toolchain or Windows .NET/WPF SDK. Therefore the following remain release gates for GitHub Actions or supported local machines:

- Android `testDebugUnitTest`
- Android signed `assembleRelease`
- Builder xUnit tests
- Builder `dotnet publish`
- Windows Authenticode signature for Stable
- physical-device and field matrix tests

See `RELEASE_CHECKLIST.md` before promoting RC1 to Stable.
