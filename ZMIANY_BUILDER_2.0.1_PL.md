# Field TAK Hub Builder 2.0.1 — zmiany

Ta poprawka dotyczy wygodnej konfiguracji serwera TAK bez edycji kodu.

## Nowy formularz serwera

W Builderze można ręcznie wpisać:

- typ serwera,
- nazwę,
- host / domenę,
- port CoT,
- port API / Enrollment,
- port Web.

Domyślnie pola pokazują przykładowy profil:

```text
OpenTAK
GGZS OpenTAK
ggzstak.duckdns.org
8089
8446
8443
```

Wszystkie wartości można zmienić przed buildem.

## Jeden zestaw danych, trzy wyniki

Po kliknięciu `Build package` ten sam profil jest używany do:

1. zapisania konfiguracji w `.fthproj`,
2. wygenerowania czytelnego `*-server.txt`,
3. wygenerowania `server.pref` dla ATAK, jeżeli operator nie dostarczył własnego `.pref`.

Dodatkowo podpisana kopia profilu trafia do `META-INF/server.txt` wewnątrz `.ftak`.

## Import / eksport TXT

Przyciski:

- `Wczytaj server.txt`
- `Zapisz server.txt`

pozwalają przenosić profil między projektami i komputerami bez ręcznej edycji JSON.

`server.txt` celowo nie przechowuje haseł, kluczy prywatnych ani sekretów certyfikatów.
