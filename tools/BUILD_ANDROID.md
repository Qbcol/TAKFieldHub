# Android build — Field TAK Hub 2.3.0 RC1

1. Open `apps/android` in Android Studio.
2. Use JDK 17+.
3. Allow Gradle sync to download Gradle 9.6 and Android dependencies.
4. Build `app` → `assembleDebug`.
5. For release signing, copy `keystore.properties.example` to `keystore.properties` and fill in your private keystore values. Do not commit that file.

The source package intentionally does not contain private signing material.
