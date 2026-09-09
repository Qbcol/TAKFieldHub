# Field TAK Hub 2.0.1 RC3 / Builder 2.1.0 RC6 — szybki test

## 1. Najpierw GitHub Actions
Po wgraniu źródeł uruchom ręcznie: **Source Gate**, **Android** i **Windows Builder**. Ten snapshot przeszedł lokalny Source Gate, ale środowisko generujące paczkę nie ma kompletnego Android SDK ani .NET/WPF SDK, więc świeży compile gate musi wykonać GitHub Actions.

## 2. Test poprawki „DOKOŃCZ KONFIGURACJĘ”
1. Na telefonie odinstaluj ATAK albo użyj telefonu bez ATAK.
2. Zaimportuj `.ftak` zawierający `payload/atak/atak.apk`.
3. Zaufaj wydawcy.
4. Kliknij **DOKOŃCZ KONFIGURACJĘ**.
5. Jeśli Field TAK Hub nie ma prawa instalowania APK, Android powinien otworzyć ekran **Instaluj nieznane aplikacje** dla Field TAK Hub.
6. Włącz zgodę i wróć do aplikacji — Hub powinien automatycznie kontynuować i otworzyć instalator ATAK.
7. Po instalacji ATAK Hub powinien przejść do pluginów, a następnie Mission Package.

## 3. Test dystrybucji z chmury
1. W Builderze zbuduj `.ftak`.
2. Wgraj dokładnie ten plik na hosting HTTPS / storage / GitHub Release.
3. W sekcji **Dystrybucja zewnętrzna / chmura** wybierz lokalny `.ftak`.
4. Wklej bezpośredni HTTPS URL do tego samego pliku.
5. Kliknij **TESTUJ LINK**. Link do strony HTML zamiast pliku powinien zostać odrzucony.
6. Kliknij **GENERUJ QR**.
7. Zeskanuj QR w Hub. Hub pobiera `.ftak`, porównuje SHA-256 zapisany w QR i wykonuje normalną weryfikację Ed25519/SHA-256 pakietu.

## 4. Test enrollment QR z OpenTAKServer
1. W UI OpenTAKServer wygeneruj QR enrollment dla testowego użytkownika.
2. W Hub kliknij **SKANUJ NOWY QR**.
3. Kod w formacie `tak://com.atakmap.app/enroll?...` powinien zostać przekazany do ATAK.
4. Token nie jest zapisywany ani logowany przez Hub.
5. Zakończ enrollment w ATAK i potwierdź rzeczywiste połączenie po stronie ATAK/OTS.

## 5. Test Data Package QR
Hub obsługuje:
- `tak://com.atakmap.app/import?url=...`,
- bezpośrednie HTTPS/prywatne-LAN URL kończące się `.zip`, `.dpk` lub `.tak`,
- bezpośrednie linki OTS zawierające `/api/data_packages`.

Po zeskanowaniu Hub przekazuje import do ATAK. Jeżeli konkretna wersja OpenTAKServer UI generuje inny format QR, zapisz/wyślij tekst zakodowany w tym QR — parser można wtedy rozszerzyć bez zmiany layoutu.

## 6. Layout
Layout Androida z RC2 został zachowany. Zmiany RC3 dotyczą zachowania przycisku i routingu QR, nie przebudowy ekranu.


## Google Drive

1. Wgraj plik `.ftak` na Google Drive.
2. Ustaw **Dostęp ogólny → Każdy, kto ma link → Wyświetlający**.
3. Skopiuj zwykły link udostępniania do pojedynczego pliku.
4. W Builderze wybierz lokalny `.ftak`, wklej link Google Drive i kliknij **Testuj / rozpoznaj link**.
5. Builder powinien zamienić link na `drive.usercontent.google.com/...` i pokazać `OK`.
6. Kliknij **Generuj QR chmurowy** i zeskanuj go w Field TAK Hub.

Link do folderu ma zostać odrzucony celowo.
