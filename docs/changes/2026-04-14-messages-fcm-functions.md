# 2026-04-14 – `messages` + FCM token + Cloud Function (push)

## Cíl (Fáze A – navazuje na `config`/kiosk)

- Datový model **`devices/{deviceId}/messages/{messageId}`** s poli `body`, `createdAt`, `senderUid`, `senderDisplayName`, `readAt`.
- **Firestore rules**: čtení pro tablet i správce; vytváření jen správce (`deviceAdmins`); úprava dokumentu vzkazu pro tablet (zápis `readAt`).
- **Web**: sekce „Vzkazy na tablet“, odeslání přes `addDoc`, realtime historie s přečtením.
- **Android**: snapshot vzkazů, překryv pro nejnovější nepřečtený vzkaz, tlačítko „Přečetl jsem“ → `readAt`; pole **`fcmRegistrationToken`** na dokumentu zařízení; `SeniorHubMessagingService` + kanál `family_messages`; `POST_NOTIFICATIONS` (Android 13+).
- **Cloud Functions** (`firebase/functions`): trigger `onDocumentCreated` na `devices/{deviceId}/messages/{messageId}` → FCM na token tabletu (region `europe-west1`).

## Dotčené soubory

- `firebase/firestore.rules` – `match /messages/{messageId}`.
- `firebase.json` – kořen repa, cesty k rules a functions.
- `firebase/functions/` – `notifyTabletOnNewMessage`.
- `seniorhub-web/src/constants.ts`, `main.ts`.
- `seniorhub-android/.../MvpRepository.kt`, `Models.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `MainActivity.kt`, `SeniorHubApp.kt`, `SeniorHubMessagingService.kt`, `AndroidManifest.xml`, `build.gradle.kts`, `strings.xml`.

## Nasazení

1. Publikovat **Firestore rules** (`firebase deploy --only firestore:rules` nebo konzole).
2. Nasadit **Functions** (`firebase deploy --only functions`) – vyžaduje Blaze a fakturační účet u Google Cloud.
3. Na tabletu povolit **oznámení** po aktualizaci aplikace (FCM + kanál).

## Poznámky

- Lokální `./gradlew` může selhat na **JDK 25** (Kotlin/Gradle DSL); použij LTS JDK 17 nebo 21 pro build.
- Webové **FCM** (prohlížeč) zůstává volitelné; tablet je priorita.
