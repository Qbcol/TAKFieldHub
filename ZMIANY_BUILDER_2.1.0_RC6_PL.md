# Field TAK Hub Builder 2.1.0 RC6 — Google Drive

RC6 rozszerza dystrybucję chmurową z RC5 o obsługę zwykłych linków udostępniania Google Drive.

## Co można wkleić

Builder nadal przyjmuje zwykły bezpośredni adres HTTPS do pliku `.ftak`, a dodatkowo rozpoznaje m.in.:

```text
https://drive.google.com/file/d/FILE_ID/view?usp=sharing
https://drive.google.com/open?id=FILE_ID
```

Link do pojedynczego pliku jest automatycznie konwertowany na bezpośredni adres pobierania `drive.usercontent.google.com`, testowany, a następnie używany w QR.

## Ważne

- plik w Google Drive musi być udostępniony co najmniej jako **Każdy, kto ma link / Wyświetlający**;
- link do folderu Google Drive jest celowo odrzucany;
- QR nadal wiąże link z SHA-256 lokalnego `.ftak`;
- jeśli Google Drive zwróci stronę HTML/login zamiast pliku, Builder pokaże czytelny błąd;
- Builder nie zapisuje danych logowania do Google.
