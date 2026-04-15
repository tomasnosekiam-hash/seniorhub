# Android: toolchain, build, smoke test (dokončení vlákna)

## Shrnutí postupu (2026-04, vývojové prostředí)

- **JDK:** výchozí systémová Jaba 25 rozbíjela Gradle (`What went wrong: 25.0.1`). Řešení: **JDK 21** pro Gradle; v repu **`seniorhub-android/gradle.properties`** je `org.gradle.java.home=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` (upravit podle `/usr/libexec/java_home -v 21` na jiném stroji).
- **Kompilace:** oprava **`AdminRoute.kt`** — `FilterChip(selected = row.deviceId == selectedId)` (dříve rozbitý odkaz na `selected`).
- **Build:** `./gradlew :app:assembleDebug` — **BUILD SUCCESSFUL** s výše uvedeným `gradle.properties`.
- **Firebase (mimo kód):** doplněné **SHA-1/256** (debug) u Android app; **Web OAuth client ID** v `res/values/strings.xml` → `default_web_client_id` pro Google Sign-In v režimu Správce.
- **Android Studio:** občas **IDE chyba EDT** po Gradle sync (`SpawnMultipleDaemonsWarningListener` / `GradleInstallationManager`) — bug IDE; build z terminálu / po srovnání **Gradle JDK** na 21 bývá OK.
- **Logcat:** `GoogleApiManager` / `SecurityException: Unknown calling package name 'com.google.android.gms'` — často **šum z GMS**; pokud přihlášení a Firestore jedou, ignorovat nebo aktualizovat Google Play Services.

## Ověření v aplikaci (uživatel)

- Start nabízí **Tablet u seniora** vs **Správce**; obě větve vyzkoušeny — v pořádku.
- Režim je **trvalý** v DataStore; změna volby vyžaduje vymazání dat aplikace / přeinstalaci.

## Související dřívější záznam

- Hlavní implementace Admin/Senior: [`2026-04-14-android-admin-role-switcher.md`](2026-04-14-android-admin-role-switcher.md).
