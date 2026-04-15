# 2026-04-15 — FCM správci, stav zařízení (síť), počasí, náhled zpráv

## Shrnutí

- **FCM tokeny správců**: `users/{uid}/fcmTokens/{installationId}` (`token`, `platform`, `updatedAt`). Android Admin při Google přihlášení + v `SeniorHubMessagingService.onNewToken` podle `AppRole.Admin`. Cloud Function po novém vzkazu pošle FCM i ostatním správcům zařízení (`new_message_admin_copy`), odesílatele vyloučí.
- **Stav zařízení**: `devices/{deviceId}/status/main` — `lastUpdatedAt`, `networkType`, `networkLabel`, zrcadlo baterie/nabíjení/heartbeat. Zapisuje senior tablet v `postDeviceHeartbeat` (síť z `ConnectivityManager`). Web a Android admin UI zobrazují síť v přehledu.
- **Počasí (senior)**: Open-Meteo bez klíče, výchozí Praha; obnovení cca 30 min; řádek na levém sloupci dashboardu.
- **Jednotný chat — první řez**: náhled posledních z Firestore zpráv s označením **Rodina** vs **Tablet** (`tablet_firestore`); poznámka, že klasické SMS v tomto náhledu nejsou.

## Soubory

- `firebase/firestore.rules` — `devices/.../status`, `users/.../fcmTokens`
- `firebase/functions/src/index.ts` — notifikace správcům
- `seniorhub-android/...` — repository, `HomeViewModel`/`HomeScreen`, `AdminRepository`/`AdminViewModel`, `SeniorHubMessagingService`, síť, počasí
- `seniorhub-web/src/constants.ts`, `main.ts` — status + seznam zařízení

## Nasazení

- Po merge: `firebase deploy --only firestore:rules,functions`
