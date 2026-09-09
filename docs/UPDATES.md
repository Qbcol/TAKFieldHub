# Update System

Field TAK Hub updates are deliberately **user-approved**. There is no silent updater.

## Release channels

`version.json` contains the canonical project channel:

```json
"releaseChannel": "rc"
```

- `rc` clients consider GitHub prereleases.
- `stable` clients consider normal non-prerelease releases.

Both updaters query the GitHub Releases list rather than `/releases/latest`, because GitHub's latest-release endpoint does not represent prerelease channels correctly.

## Release manifest

Each GitHub Release includes `fieldtak-release.json`:

```json
{
  "schema": "fieldtak.release",
  "version": 1,
  "channel": "rc",
  "hub": {
    "version": "2.0.1-rc1",
    "versionCode": 20101,
    "asset": "FieldTAKHub-2.0.1-RC1.apk",
    "sha256": "..."
  },
  "builder": {
    "version": "2.1.0-rc1",
    "asset": "FieldTAKHub-Builder-2.1.0-RC1-win-x64.zip",
    "sha256": "..."
  }
}
```

The workflow generates SHA-256 from the actual build artifacts.

## Android flow

1. Hub checks the configured GitHub repository.
2. It chooses the newest release for its channel.
3. It downloads `fieldtak-release.json` over HTTPS.
4. If a newer Hub exists, the UI offers **Download update**.
5. APK is downloaded over HTTPS and checked against SHA-256.
6. Android's normal installer is opened and the user confirms installation.

Every redirect is revalidated as HTTPS.

## Builder flow

1. Builder checks releases at startup without interrupting the operator.
2. Manual **Check for updates** can download an update ZIP.
3. The ZIP is SHA-256 verified.
4. Builder opens Explorer with the verified file selected.
5. Builder never self-replaces while running.

## Repository setting

The default example repository is `Qbcol/field-tak-hub`.

Before the first public release, set the actual repository path consistently in:

- `version.json` (`defaultUpdateRepository`)
- Android `BuildConfig.UPDATE_REPOSITORY`
- Builder `update-config.json`

The source gate validates versions/channel but intentionally does not assume that a particular GitHub repository already exists.
