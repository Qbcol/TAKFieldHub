# RC → Stable Release Checklist

## CI / build

- [ ] `python tests/source_gate.py` passes
- [ ] Android unit tests pass
- [ ] Android `assembleRelease` passes
- [ ] release APK is signed with the production Android key
- [ ] Builder unit tests pass on Windows
- [ ] Builder `dotnet publish` passes
- [ ] Builder EXE is Authenticode-signed for Stable
- [ ] GitHub Release contains `fieldtak-release.json` and `SHA256SUMS.txt`
- [ ] updater sees the release on the correct channel

## Package/security tests

- [ ] valid `.ftak` verifies
- [ ] changed payload file is rejected
- [ ] changed manifest is rejected
- [ ] extra unsigned payload file is rejected
- [ ] bad Ed25519 signature is rejected
- [ ] wrong descriptor whole-bundle SHA-256 is rejected
- [ ] Zip Slip paths are rejected
- [ ] expired descriptor/package is rejected
- [ ] public `http://` provisioning is rejected
- [ ] private-LAN `http://` provisioning works
- [ ] publisher-key export/import preserves fingerprint

## Interrupted operation tests

- [ ] interrupted LAN download resumes with HTTP 206
- [ ] server ignoring Range restarts safely
- [ ] app restart during download resumes
- [ ] app restart during plugin install resumes state
- [ ] package installed/replaced/removed refresh works
- [ ] Mission Package return path does not reset deployment

## Device matrix

Test at least 5–10 devices for RC1 and 20–50 devices before Stable, including multiple OEMs and Android versions used by the team.

Suggested matrix:

- Android 11
- Android 13
- Android 14
- Android 15
- Android 16

## Accessibility

- [ ] TalkBack smoke test
- [ ] 200% Android font-size test
- [ ] Windows Narrator smoke test
- [ ] keyboard-only Builder workflow
- [ ] Windows 200% DPI test
- [ ] high-contrast smoke test

## Field test

- [ ] one LAN-only deployment without Internet
- [ ] one HTTPS/GitHub deployment
- [ ] 1+ GB package resume test
- [ ] deliberate wrong server profile and diagnostics test
- [ ] key backup restored on a second Windows profile/machine

Only after these checks should `releaseChannel` move from `rc` to `stable` and the tag become `v2.0.1` / Builder `2.1.0`.
