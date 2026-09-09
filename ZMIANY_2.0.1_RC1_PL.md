# Field TAK Hub 2.0.1 RC1 / Builder 2.1.0 RC1

Najważniejsze zmiany przed wydaniem Stable:

- pełny interfejs PL/EN w aplikacji i Builderze,
- poprawki accessibility (TalkBack, Narrator, klawiatura, duży tekst/DPI),
- sprawdzanie aktualizacji Hub/Builder przez GitHub Releases,
- osobny kanał RC i Stable,
- weryfikacja SHA-256 pobranych aktualizacji i brak cichej instalacji,
- Testuj serwer (DNS/TCP/TLS),
- Build Preview,
- szyfrowany backup/import klucza Publishera `.fthkey`,
- lepsza kontrola wolnego miejsca i czyszczenie starych plików, również przed pobraniem paczki z QR,
- publiczne HTTP jest blokowane; HTTP pozostaje tylko dla prywatnej sieci LAN Buildera,
- release CI buduje podpisany APK Release,
- Builder liczy SHA-256 strumieniowo, dzięki czemu wielogigabajtowe mapy/pakiety nie są wczytywane w całości do RAM,
- source gate kontroluje wersje, tłumaczenia, XML/JSON/XAML, wymagane elementy release i przypadkowe sekrety.

Dodatkowo podczas walidacji naprawiono dwa istotne błędy: zgodność podpisanej listy plików `.ftak` dla `META-INF/server.txt` oraz wykrywanie kolejnych prerelease’ów GitHub przez updater.

RC1 pozostaje kandydatem do wydania. `Stable` powinien zostać oznaczony dopiero po przejściu checklisty terenowej i podpisaniu Buildera Authenticode.

## CI Hotfix 2

Drugi rzeczywisty build GitHub Actions ujawnił kolejne błędy kompilacji. Poprawiono:

- brak importu `androidx.activity.compose.setContent` w Androidzie,
- callback przycisku Wstecz na ekranie skanera QR,
- typ licznika postępu pobierania (`Long`),
- jawne `System.IO` w `FtakPackageBuilder.cs` i `UpdateService.cs`,
- importy wymagane przez testy Buildera,
- przestarzałe `Uri.EscapeUriString` w serwerze LAN.

Source Gate został rozszerzony o kontrole tych regresji.

## CI Hotfix 3

- Naprawiono pozostałe błędy kompilacji Windows Buildera związane z brakującym `System.IO` w `ProjectService`, `MissionPackageBuilder`, `SourceAnalyzer`, `SigningKeyService`, `ServerTextConfigService` i `ServerValidator`.
- Source Gate kontroluje teraz cały kod Buildera pod kątem użycia `Path/File/Directory/Stream*` bez wymaganej przestrzeni nazw.
