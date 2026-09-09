# Field TAK Hub Builder 2.1.0 RC5 — dystrybucja chmurowa

RC5 zachowuje czytelny layout Buildera RC4 i dodaje drugi sposób dystrybucji obok LAN.

## Dystrybucja zewnętrzna / chmura

1. Zbuduj `.ftak` albo wskaż istniejący lokalny `.ftak`.
2. Wgraj **dokładnie ten sam plik** do własnego HTTPS / GitHub Releases / S3 / R2 / innego hostingu z bezpośrednim linkiem do pliku.
3. Wklej bezpośredni link HTTPS w Builderze.
4. Kliknij **TESTUJ LINK CHMUROWY**.
5. Kliknij **GENERUJ QR CHMUROWY**.

Builder:

- liczy SHA-256 lokalnego `.ftak` strumieniowo;
- dodaje do QR rozmiar pakietu i termin ważności;
- generuje `fieldtak://provision?packageUrl=...`;
- zapisuje PNG i tekst deep-linku do katalogu `out`;
- nie wyłącza walidacji TLS i odrzuca publiczne HTTP;
- wykrywa typowy przypadek, gdy link zamiast pliku zwraca stronę HTML.

Maksymalna liczba pobrań pozostaje funkcją serwera LAN Buildera. Przy hostingu zewnętrznym limit pobrań musi być egzekwowany przez wybrany hosting/CDN, jeśli jest potrzebny.
