# Field TAK Hub Builder 2.1.0 RC4

- przywrócono czytelny, jasny układ Buildera sprzed eksperymentalnego ciemnego motywu RC3;
- pozostawiono ikonę aplikacji z linii 1.9;
- zachowano automatyczne foldery, Nowy projekt, naprawę katalogów, liczniki i drag & drop;
- dodano **Importuj ZIP 1.x** z weryfikacją starego podpisu RSA i sum SHA-256;
- import tworzy nowy projekt 2.x, który po kliknięciu **Buduj pakiet** jest podpisywany aktualnym kluczem Publisher Ed25519;
- dodano bezpośrednią obsługę APK ATAK: jeden APK w `source/atak/` trafia do `payload/atak/atak.apk`;
- APK ATAK nie jest już wkładany do Mission Package;
- format `.ftak v2` i deep-link `fieldtak://provision` pozostają bez zmian.
