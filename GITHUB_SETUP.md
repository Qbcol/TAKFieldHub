# GitHub Setup

1. Create an empty repository (recommended name: `field-tak-hub`).
2. Push this repository to GitHub.
3. In **Settings → Secrets and variables → Actions**, configure the Android signing secrets listed in `docs/BUILD_AND_SIGN.md`.
4. For Stable, also configure the Windows Authenticode certificate secrets.
5. Confirm all three CI workflows are green on `main`/`release/*`.
6. Push the annotated RC tag `v2.0.1-rc2-builder-2.1.0-rc4`.
7. Verify the generated GitHub Release contains the APK, Builder ZIP, update manifest and checksums.
8. Install the RC on test devices and follow `docs/RELEASE_CHECKLIST.md`.

If your repository is not `Qbcol/field-tak-hub`, update the updater repository setting before making a release.
