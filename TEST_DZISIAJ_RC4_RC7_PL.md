# Field TAK Hub 2.0.1 RC4 / Builder 2.1.0 RC7 — szybki test

## 1. GitHub Actions
Uruchom: Source Gate, Android, Windows Builder.

## 2. Przygotowanie telefonu
W Hub wybierz **1. PRZYGOTUJ TELEFON**, zeskanuj QR paczki `.ftak` i sprawdź instalację ATAK/pluginów oraz przekazanie Mission Package.

## 3. Enrollment
W Web UI OpenTAKServer wygeneruj QR enrollment konkretnego użytkownika. W Hub wybierz **2. ENROLLMENT UŻYTKOWNIKA Z QR** i zeskanuj kod. ATAK powinien przejąć URI enrollmentu. Sprawdź rejestrację/certyfikat w ATAK.

## 4. Data Packages
W Web UI OpenTAKServer wygeneruj QR Data Package. W Hub wybierz **3. DATA PACKAGES Z QR** i zeskanuj kod. ATAK powinien przejąć import.

## 5. Test pomyłki
Zeskanuj QR enrollmentu w trybie „Przygotuj telefon” — Hub powinien go odrzucić. Zeskanuj provisioning Field TAK Hub w trybie enrollmentu — również powinien zostać odrzucony.

## 6. Builder
Sprawdź, że RC7 nadal buduje `.ftak`, generuje LAN QR oraz Cloud/Google Drive QR tak samo jak RC6.
