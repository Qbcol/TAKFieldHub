# Field TAK Hub 2.0.1 RC3 — QR / instalacja ATAK / OpenTAK

RC3 zachowuje układ Androida z RC2 i skupia się na przepływie wdrożenia.

## Zmiany

- naprawiono przycisk **DOKOŃCZ KONFIGURACJĘ** przy braku ATAK:
  - jeśli Android nie zezwala Field TAK Hub na instalację APK, aplikacja otwiera właściwy ekran „Instaluj nieznane aplikacje”;
  - po nadaniu uprawnienia i powrocie do aplikacji deployment jest automatycznie kontynuowany;
  - następnie uruchamiany jest systemowy instalator ATAK z `payload/atak/atak.apk`;
- uniwersalny skaner QR obsługuje:
  - `fieldtak://provision?url=...` — klasyczny descriptor LAN/HTTPS;
  - `fieldtak://provision?packageUrl=...` — bezpośrednią paczkę `.ftak` w chmurze;
  - `tak://com.atakmap.app/enroll?...` — enrollment ATAK/OpenTAKServer;
  - `tak://com.atakmap.app/import?url=...` — import Data/Mission Package przez ATAK;
  - bezpośrednie HTTPS do `.ftak`;
  - bezpośrednie URL do ZIP/DPK/TAK oraz endpointów `/api/data_packages...` jako import pakietu danych do ATAK;
- token enrollmentu z QR nie jest zapisywany ani logowany przez Field TAK Hub;
- usunięto podwójne URL-dekodowanie starego QR descriptorowego.

## Bezpieczeństwo

Field TAK Hub nie przejmuje prywatnego magazynu certyfikatów ATAK. Enrollment i Data Package są przekazywane do ATAK przez natywne `tak://` URI. Publiczne pakiety zewnętrzne wymagają HTTPS, a `.ftak` nadal przechodzi SHA-256 + Ed25519.
