# Visual refresh inspired by TAK Field Hub 1.9

Field TAK Hub 2.0.1 RC2 and Builder 2.1.0 RC3 restore the recognizable visual identity used by the 1.9 line while keeping the 2.x provisioning architecture.

## Brand palette

- background: `#0B1114`
- panel: `#102127`
- cyan accent: `#28E0D7`
- success: `#59D17D`
- error: `#FF3B4E`

## Android

- restores the launcher/adaptive icon shipped with the 1.9 line,
- applies the 1.9 dark/cyan Material 3 palette,
- adds the `PROVISION • DEPLOY • READY` brand header,
- highlights primary provisioning actions in cyan,
- keeps TalkBack semantics, 48+ dp touch targets and text status labels.

## Windows Builder

- uses the same dark/cyan palette and the 1.9 icon,
- adds branded header and prominent Build / Test server / Start LAN actions,
- keeps native WPF focus/keyboard behavior for accessibility.

This is a visual/UX refresh only. `.ftak` schema v2, QR provisioning and security contracts are unchanged.
