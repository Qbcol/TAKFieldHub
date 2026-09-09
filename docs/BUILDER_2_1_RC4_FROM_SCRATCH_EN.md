# Field TAK Hub Builder 2.1.0 RC4 — build a package from scratch

1. Start Builder. On first launch it creates a default workspace under `Documents\\FieldTAKHub\\Projects`.
2. Click **NEW PROJECT**. Enter the name, package ID and package version.
3. Builder automatically creates `source/atak`, `plugins`, `maps`, `overlays`, `config`, `data` and `out`.
4. Drop files on the matching project-folder row or click the folder name to open it in Explorer.
5. Enter the TAK server host/ports and click **TEST SERVER**.
6. Click **ANALYZE**, review content and then **BUILD PACKAGE**.
7. Test the generated `.ftak` on one phone.
8. Start LAN distribution and scan the QR code.

Missing project folders are safely recreated when Builder starts, opens a project, analyzes or builds. Existing files are never deleted. Name collisions during drag-and-drop are preserved as `file (2).ext` rather than overwritten.

## ATAK APK in RC4

Place at most one ATAK CIV APK in `source/atak/`. Builder writes it as `payload/atak/atak.apk`, which Field TAK Hub can hand to the Android package installer. Other files in `source/atak/` are used to build the ATAK Mission Package.

## Migrating a 1.x ZIP

Use **Import legacy 1.x ZIP**. Builder verifies the legacy RSA signature when present and verifies SHA-256 values from `manifest.json`, creates a fresh 2.x project, imports ATAK/plugins/data and loads the legacy server profile. Then click **Build package** to create a new Ed25519-signed `.ftak` v2.
