# Field TAK Hub Builder 2.1.0 RC3 — paczka od zera

## Najkrótszy workflow

1. Uruchom Builder. Przy pierwszym starcie automatycznie powstaje:
   `Dokumenty\\FieldTAKHub\\Projects\\Field-TAK-Package\\source` oraz `out`.
2. Kliknij **NOWY PROJEKT**.
3. Wpisz nazwę, ID pakietu i wersję. Builder sam utworzy:
   `source\\atak`, `plugins`, `maps`, `overlays`, `config`, `data` oraz `out`.
4. Dodaj pliki przez przeciągnięcie ich na odpowiedni wiersz w sekcji **Foldery projektu** albo kliknij nazwę folderu, aby otworzyć go w Eksploratorze.
5. Wpisz host/porty TAK, kliknij **TESTUJ SERWER**.
6. Kliknij **ANALIZUJ**, sprawdź zawartość, następnie **BUDUJ PAKIET**.
7. Przetestuj `.ftak` na jednym telefonie.
8. Kliknij **URUCHOM DYSTRYBUCJĘ LAN** i zeskanuj QR.

## Automatyczna struktura

Builder tworzy i naprawia bez kasowania plików:

```text
Projekt\\
├── Projekt.fthproj
├── source\\
│   ├── atak\\
│   ├── plugins\\
│   ├── maps\\
│   ├── overlays\\
│   ├── config\\
│   ├── data\\
│   └── README-WRZUC-PLIKI-TUTAJ.txt
└── out\\
```

Jeśli usuniesz np. `source\\maps`, przy następnym otwarciu projektu/analizie/buildzie Builder odtworzy pusty katalog. Istniejące pliki nie są usuwane ani nadpisywane.

## Drag & drop

Każdy wiersz ATAK / Pluginy / Mapy / Nakładki / Config / Dane jest celem przeciągania. Jeżeli plik o tej samej nazwie już istnieje, Builder tworzy nazwę `plik (2).ext` zamiast nadpisywać poprzedni plik.

## Domyślny workspace

`%USERPROFILE%\\Documents\\FieldTAKHub\\Projects`

Przycisk **Otwórz folder projektu** otwiera bieżący katalog projektu. **Napraw foldery** ręcznie wykonuje tę samą bezpieczną kontrolę struktury, która działa automatycznie.
