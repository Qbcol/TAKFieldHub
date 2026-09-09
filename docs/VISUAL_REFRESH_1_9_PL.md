# Odświeżenie wyglądu inspirowane TAK Field Hub 1.9

Field TAK Hub 2.0.1 RC2 i Builder 2.1.0 RC3 przywracają rozpoznawalną identyfikację wizualną linii 1.9 bez zmiany architektury provisioningu 2.x.

## Paleta

- tło: `#0B1114`
- panel: `#102127`
- akcent cyan: `#28E0D7`
- status OK: `#59D17D`
- błąd: `#FF3B4E`

## Android

- przywrócono ikonę launchera/adaptive icon używaną w linii 1.9,
- zastosowano ciemny motyw cyan w Material 3,
- dodano nagłówek `PROVISION • DEPLOY • READY`,
- główne akcje provisioningu są mocniej wyróżnione,
- zachowano TalkBack, duże pola dotykowe i tekstowe opisy stanów.

## Windows Builder

- ta sama ciemna kolorystyka cyan i ikona z linii 1.9,
- nowy nagłówek oraz wyróżnione przyciski Buduj / Testuj serwer / Uruchom LAN,
- zachowano natywne zachowanie WPF dla klawiatury i fokusu.

To zmiana wizualna/UX. Format `.ftak` schema v2, QR i kontrakty bezpieczeństwa pozostają bez zmian.
