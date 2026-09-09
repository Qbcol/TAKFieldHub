# Deployment Engine 2.0

Field TAK Hub 2.0 nie uzależnia logiki wdrożenia od aktualnie wyświetlanego ekranu. UI jest projekcją trwałego `DeploymentSession`.

```text
NEW
 ↓
QR_SCANNED
 ↓
DESCRIPTOR_VERIFIED
 ↓
DOWNLOADING
 ↓
BUNDLE_DOWNLOADED
 ↓
BUNDLE_VERIFIED
 ↓
PREFLIGHT
 ↓
ATAK_READY
 ↓
PLUGINS_INSTALLING
 ↓
PLUGINS_READY
 ↓
MAPS_IMPORTING
 ↓
MAPS_READY
 ↓
OTS_VERIFYING
 ↓
OTS_CONNECTED
 ↓
FINAL_VERIFY
 ↓
COMPLETE
```

Stany przerwań: `PAUSED`, `WAITING_FOR_USER`, `FAILED`, `CANCELLED`.

Aktywna sesja jest zapisywana lokalnie. Powrót z instalatora APK lub ATAK powoduje ponowne odczytanie rzeczywistego stanu pakietów i przejście do właściwego kroku. Nie zakładamy sukcesu instalacji tylko dlatego, że użytkownik wrócił do Hub.
