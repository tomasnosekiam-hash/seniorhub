# 2026-04-16 — Matěj (Senior): služba, Porcupine/STT, `incidents`, FCM správcům

## Shrnutí

- **Android (`matej/`)**: `MatejForegroundService` — foreground + mikrofon; wake buď **Porcupine** (klíč `picovoice.access.key` v `seniorhub-android/local.properties` → `BuildConfig`), vlastní `*.ppn` + `*.pv` v `assets/porcupine/`, jinak vestavěné anglické „porcupine“; při selhání Porcupine **fallback STT** (české oslovení podle `assistant_name` z cloudu). Nouzové **vytočení** prioritního kontaktu (`is_emergency` / SIM), **TTS** po spuštění hovoru.
- **Firestore**: pod `devices/{deviceId}/incidents` zápis tabletem (`source: matej_emergency`, `dialedPhone`, volitelně `dialedContactLabel`, `createdAt`) — `MvpRepository.recordMatejEmergencyIncident`.
- **Firestore rules**: `match /incidents/{incidentId}` — create jen tablet, validace `source` a `dialedPhone`.
- **Cloud Functions**: `notifyAdminsOnMatejIncident` — při novém incidentu FCM na tokeny správců (`users/.../fcmTokens`); existující `notifyTabletOnNewMessage` beze změny chování.
- **Deploy**: `firestore:rules` + `functions` na projekt `seniorhub-716f0`.

## Nasazení

- `npx -y firebase-tools@latest deploy --only=firestore:rules,functions --project seniorhub-716f0`

## Soubory (orientačně)

- `seniorhub-android/.../matej/MatejForegroundService.kt`, `MatejPorcupineWake.kt`, `MatejEmergencyPhone.kt`, `MatejWakeText.kt`
- `seniorhub-android/app/build.gradle.kts` (Porcupine, `PICOVOICE_ACCESS_KEY`), `AndroidManifest.xml`, `SeniorHubApp.kt`, `MainActivity.kt`, `HomeScreen.kt`
- `seniorhub-android/.../data/MvpRepository.kt`
- `firebase/firestore.rules`, `firebase/functions/src/index.ts`
