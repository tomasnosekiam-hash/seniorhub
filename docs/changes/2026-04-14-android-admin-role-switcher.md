# Android: volba režimu (Senior / Admin), Google správce, přepínač tabletů

## Shrnutí

- **První spuštění**: obrazovka výběru režimu — **tablet u seniora** (stávající anonymní tok + FCM) nebo **správce** (Google Sign-In, kolekce `deviceAdmins`, stejný model jako web).
- **Trvalá volba** v DataStore (`AppRoleStore`); změna režimu vyžaduje vymazání dat aplikace nebo přeinstalaci (záměr — stejná APK, oddělené životní cykly).
- **Admin**: seznam spárovaných tabletů (`FilterChip`), párování kódem (batch jako web), úprava názvu/hlášky/hlasitosti, PIN/SIM/asistent, profil seniora, odeslání vzkazu, náhled posledních vzkazů.
- **Orientace**: Senior zůstává `sensorLandscape`; Admin a výběr režimu `unspecified` (telefon i tablet).
- **Google OAuth**: vyžaduje **Web client ID** v `res/values/strings.xml` (`default_web_client_id`) a SHA-1/256 v Firebase Console — prázdný řetězec zobrazí návod v UI.

## Dotčené soubory (Android)

- `data/AppRole.kt`, `data/AppRoleStore.kt`
- `data/AdminRepository.kt`, `data/MvpRepository.kt` (konstanta `COLLECTION_DEVICE_ADMINS`, veřejné `DEFAULT_ASSISTANT_NAME`)
- `ui/AdminViewModel.kt`, `ui/AdminRoute.kt`, `ui/RolePickerScreen.kt`
- `MainActivity.kt` — větvení podle role; FCM / `messagingDeps` jen pro Senior
- `app/build.gradle.kts` — `play-services-auth`
- `res/values/strings.xml` — `default_web_client_id`

## Ověření

- Sestavení: `./gradlew :app:compileDebugKotlin` (vyžaduje JDK kompatibilní s Gradle/Kotlin pluginem projektu).
- Po doplnění Web client ID: přihlášení Google → dotaz na `deviceAdmins` → úprava vybraného `devices/{id}` podle pravidel správce.
