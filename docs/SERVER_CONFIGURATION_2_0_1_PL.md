# Konfiguracja serwera TAK w Builderze 2.1.0 RC1

Builder 2.1.0 RC4 pozwala zmieniać serwer bez edycji kodu i bez ręcznej edycji `.fthproj`.

## GUI

W sekcji **Konfiguracja serwera TAK** dostępne są pola:

- Typ
- Nazwa serwera
- Host / domena
- CoT
- API / Enrollment
- Web

Domyślny przykład:

```text
SERVER_TYPE=OpenTAK
SERVER_NAME=GGZS OpenTAK
HOST=ggzstak.duckdns.org
COT_PORT=8089
API_PORT=8446
WEB_PORT=8443
```

Przed buildem można wpisać dowolny inny host i porty. Ustawienia są zapisywane w `.fthproj`.

## server.txt

Builder potrafi:

- wczytać `server.txt` do pól GUI,
- zapisać aktualne pola GUI do `server.txt`,
- przy każdym buildzie wygenerować `*-server.txt` obok `.ftak`,
- umieścić podpisaną kopię jako `META-INF/server.txt` wewnątrz `.ftak`.

`server.txt` nie powinien zawierać haseł, prywatnych kluczy ani sekretów certyfikatów.

## server.pref

Jeżeli `source/atak/` nie zawiera własnego pliku `.pref`, Builder generuje `server.pref` z aktywnych danych GUI/projektu i umieszcza go w ATAK Mission Package.

Jeżeli operator dostarczy własny `.pref`, ma on pierwszeństwo. Dzięki temu zaawansowana konfiguracja nadal jest możliwa.
