# Field TAK Hub 2.3 — Simple Mode UX

## Cel

Tryb prosty ma prowadzić użytkownika przez wdrożenie bez znajomości ATAK, OpenTAKServer, MQTT czy szczegółów Meshtastic.

## Główne działania

1. **SPRAWDŹ WSZYSTKO** — jedna kontrola całego telefonu i wdrożenia.
2. **SKONFIGURUJ RADIO** — uruchamia przygotowaną ścieżkę Meshtastic, jeżeli paczka ją zawiera.
3. **PRZYGOTUJ TELEFON** — skan QR organizatora.
4. **IMPORT .FTAK** — ręczne załadowanie paczki.
5. **STAN URZĄDZENIA** — szybki podgląd.

Enrollment i Data Package pozostają dostępne niżej jako działania pomocnicze.

## Statusy

- zielony — gotowe;
- niebieski/żółty — wymaga działania;
- czerwony — problem;
- szary — jeszcze nie sprawdzono.

## Meshtastic

„Skonfiguruj radio” nie udaje, że Field TAK Hub posiada własny sterownik radiowy. Hub przekazuje przygotowany kanał do oficjalnej aplikacji Meshtastic albo otwiera ATAK dla bezpośredniego pluginu Meshtastic. Połączenie i zapis konfiguracji wykonuje właściwa warstwa Meshtastic.

## Korekta UX po teście na telefonie

Simple Mode został dodatkowo uproszczony po teście na urządzeniu:
- usunięto z widoku prostego bezpośrednie przyciski ENROLLMENT i DATA PACKAGES — pozostają w trybie zaawansowanym,
- usunięto numerowanie kroków z głównego ekranu,
- karta statusu została zmniejszona do zwartego komunikatu z ikoną stanu,
- główną akcją jest teraz „PRZYGOTUJ TELEFON”,
- „SPRAWDŹ WSZYSTKO” pozostaje akcją diagnostyczną drugiego poziomu,
- „SKONFIGURUJ RADIO” pojawia się tylko wtedy, gdy paczka zawiera przygotowany kanał Meshtastic i radio nie jest połączone,
- „IMPORTUJ .FTAK”, „NAPRAW MÓJ TELEFON” i „STAN URZĄDZENIA” są podporządkowane głównemu przepływowi,
- przejście do trybu zaawansowanego jest oddzielone wizualnie na dole ekranu.
