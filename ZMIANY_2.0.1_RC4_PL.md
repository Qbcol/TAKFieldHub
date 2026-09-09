# Field TAK Hub 2.0.1 RC4 — trzy osobne tryby QR

RC4 zachowuje layout Androida znany z RC2/RC3, ale rozdziela trzy działania, aby kodów OpenTAKServer nie mieszać z provisioningiem Field TAK Hub.

1. **PRZYGOTUJ TELEFON** — skanuje wyłącznie QR Field TAK Hub / `.ftak` i prowadzi instalację ATAK, pluginów, map i konfiguracji.
2. **ENROLLMENT UŻYTKOWNIKA Z QR** — skanuje QR wygenerowany w UI OpenTAKServer dla konkretnego użytkownika (`tak://.../enroll`). Token nie jest zapisywany ani logowany; URI jest przekazywane do ATAK.
3. **DATA PACKAGES Z QR** — skanuje ATAK import QR albo bezpośredni bezpieczny URL do Data Package, w tym typowe linki pobierania OpenTAKServer, a następnie przekazuje import do ATAK.

Jeśli zeskanujesz kod niewłaściwego typu, aplikacja pokaże błąd zamiast uruchamiać inny workflow.
