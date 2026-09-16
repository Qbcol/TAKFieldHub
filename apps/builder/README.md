# Field TAK Hub Builder 2.3.0

Windows/WPF builder for preparing, validating, signing and distributing Field TAK Hub `.ftak` deployment packages.

**Author:** Jakub /Qbcol/ Baron  
**Version:** 2.3.0 FINAL  
**Target:** Windows / .NET 10

## What the Builder does

- creates and repairs a standard project workspace;
- imports legacy packages;
- analyzes ATAK/Mission Package files, plugins, maps and deployment data;
- generates Field TAK Hub `.ftak` packages using schema v2;
- signs packages with the Publisher key;
- validates OpenTAKServer configuration;
- supports optional ATAK enrollment URI;
- supports Meshtastic HYBRID configuration;
- generates QR/deep links for HTTPS/Cloud distribution;
- can expose a local distribution server;
- supports Polish and English UI.

## Current project structure

A new project is created below:

```text
Documents/
└── FieldTAKHub/
    └── Projects/
        └── <ProjectName>/
            ├── <ProjectName>.fthproj
            ├── source/
            │   ├── atak/
            │   ├── plugins/
            │   ├── maps/
            │   ├── overlays/
            │   ├── config/
            │   ├── data/
            │   ├── meshtastic/
            │   └── enrollment/
            └── out/
```

The Builder automatically recreates missing source folders.

## Quick Start

1. Start **Field TAK Hub Builder**.
2. Click **New Project** or open an existing `.fthproj`.
3. Put deployment files into the appropriate `source/` folder.
4. Configure package metadata.
5. Configure OpenTAKServer host/ports if required.
6. Optionally configure **Enrollment** using a short-lived/one-time `tak://.../enroll` URI.
7. Optionally configure **Meshtastic**.
8. Click **Analyze**.
9. Click **Build Package** and confirm the preview.
10. Find the resulting signed `.ftak` file in `out/`.

## Source folders

| Folder | Purpose |
|---|---|
| `atak/` | ATAK Mission Package files and ATAK-imported data |
| `plugins/` | ATAK plugin APK files |
| `maps/` | MBTiles, GeoPackage, SQLite and other map data |
| `overlays/` | KML/KMZ overlays |
| `config/` | Additional configuration files |
| `data/` | Other deployment data |
| `meshtastic/` | Meshtastic channel/profile data |
| `enrollment/` | Optional ATAK enrollment URI |

A current project also receives `source/README-WRZUC-PLIKI-TUTAJ.txt` with the same beginner-oriented instructions.

## Meshtastic HYBRID

The Builder supports the project-level Meshtastic profile:

- mode: `hybrid`, `compatible` or `direct`;
- channel name;
- region;
- modem preset;
- hop limit;
- PLI;
- GeoChat;
- OTS relay;
- file transfer.

For channel provisioning, paste the canonical URL beginning with:

```text
https://meshtastic.org/e/
```

The Builder stores the channel URL in:

```text
source/meshtastic/channel.url
```

and packages it under:

```text
payload/meshtastic/channel.url
```

The Builder does **not** implement direct radio/BLE I/O and is not tied to a specific Meshtastic hardware vendor. Radio communication is handled by the Meshtastic Android application/plugin layer. Gateway type/model is descriptive and may be changed to any supported Meshtastic device identifier.

## ATAK Enrollment

Enrollment is optional. When configured, Builder stores:

```text
source/enrollment/enroll.url
```

Use a short-lived or one-time enrollment token. Do not commit reusable enrollment secrets to GitHub or include them in public distribution packages.

## Building from source

Requirements:

- Windows
- .NET 10 SDK
- network access for NuGet restore

From `apps/builder/`:

```powershell
dotnet restore FieldTakHub.Builder.sln
dotnet build FieldTakHub.Builder.sln -c Release
dotnet test FieldTakHub.Builder.sln -c Release
```

The repository also contains the Windows build scripts under `scripts/` and CI workflow under `.github/workflows/windows-builder.yml`.

## Security

Never place these in the project source or Git repository:

- Publisher private key;
- passwords;
- MQTT credentials;
- reusable enrollment tokens;
- other long-lived secrets.

Meshtastic channel URLs may contain information that allows access to a protected channel and should be treated as sensitive.

## Versioning

Builder version must stay aligned with the repository `version.json` for stable releases. Current release:

```text
Field TAK Hub: 2.3.0
Builder:       2.3.0
Channel:       stable
Project schema: 2
.ftak schema:   2
```

## Related documentation

- `README.md` — complete project overview
- `docs/BUILD.md` — build information
- `docs/BUILD_AND_SIGN.md` — Android/build/signing information
- `docs/MESHTASTIC_HYBRID.md` — Meshtastic HYBRID architecture
- `docs/FTAK_FORMAT.md` — `.ftak` format
- `docs/SECURITY_MODEL.md` — security model
- `docs/RELEASE_2_3_0.md` — 2.3.0 release notes

## Independence

Field TAK Hub is an independent community project. It is not an official ATAK, Meshtastic or OpenTAKServer product.
