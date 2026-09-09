# Field TAK Hub 2.0.0 FINAL — zakres zmian

Ta wersja łączy architekturę `.ftak v2` z najważniejszymi usprawnieniami UX z linii 1.9.2/1.9.3.

## Deployment

- trwała maszyna stanów: `QR_SCANNED → BUNDLE_VERIFIED → PREFLIGHT → ATAK_READY → PLUGINS_READY → MAPS_READY → OTS_VERIFYING → COMPLETE`
- stany pomocnicze: `WAITING_FOR_USER`, `PAUSED`, `FAILED`, `CANCELLED`
- zapis aktywnego deploymentu i historia ostatnich wdrożeń
- wznowienie aktywnego deploymentu po ponownym uruchomieniu aplikacji
- retry/kontynuacja od bieżącego kroku

## Pobieranie

- pliki `.part`
- HTTP `Range`
- obsługa `206 Partial Content`
- Builder LAN obsługuje zakresy bajtów
- descriptor zawiera SHA-256 całego `.ftak`
- po wznowieniu cały pakiet jest ponownie walidowany

## UX

- ekran główny pokazuje procent gotowości telefonu
- duży przycisk `DOKOŃCZ KONFIGURACJĘ`
- statusy `GOTOWE / DO WYKONANIA / PROBLEM / NIEZWERYFIKOWANE`
- własny stos nawigacji i prawidłowe zachowanie Wstecz
- automatyczne odświeżanie po powrocie z instalatora/ATAK
- reakcja na `PACKAGE_ADDED`, `PACKAGE_REPLACED`, `PACKAGE_REMOVED`
- historia deploymentów z opcją ponownego użycia lokalnej paczki
- informacja o nowszej wersji tego samego bundle

## ATAK i pluginy

- detekcja ATAK CIV
- sprawdzanie wersji ATAK względem `minVersion` / `maxVersion`
- odczyt packageName/versionCode/versionName z plugin APK
- porównanie z wersją zainstalowaną
- rozróżnienie: aktualny / wymaga aktualizacji / brak / nierozpoznany

## OpenTAK / tryb serwisowy

- DNS
- port API/enrollment
- port web
- port CoT
- raport serwisowy TXT
- kopiowanie i udostępnianie raportu
- jawne oznaczenie prywatnego stanu certyfikatu ATAK jako `NIEZWERYFIKOWANE`, jeśli Hub nie ma technicznej możliwości jego wiarygodnego odczytu

## Security

- Ed25519
- SHA-256
- lista wszystkich podpisanych plików payload
- odrzucanie dodatkowych niepodpisanych plików payload
- kontrola fingerprintu wydawcy
- ochrona przed Zip Slip
- limit bezpieczeństwa dla rozpakowanego bundle
- wyłączony Android Auto Backup dla lokalnego store deploymentów
