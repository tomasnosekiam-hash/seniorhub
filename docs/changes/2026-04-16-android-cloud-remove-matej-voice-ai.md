# 2026-04-16 — Odstranění hlasové vrstvy (Matěj) z Androidu a Cloud Functions

## Rozsah

- **Android (`seniorhub-android`)**: celý balík `com.seniorhub.os.matej`, `MatejAssistantUi.kt`, karta a debug FAB na dashboardu, stav a logika v `HomeViewModel` / `HomeRoute`, kanál notifikací wake v `SeniorHubApp`, `MatejForegroundService` z manifestu, oprávnění `RECORD_AUDIO` a `FOREGROUND_SERVICE_MICROPHONE`, závislosti Porcupine / Generative AI / OkHttp / Firebase Functions SDK (už se nevolají callables).
- **Cloud Functions (`firebase/functions`)**: smazány `matejStt.ts`, `matejTts.ts`, `matejSttStreamHttp.ts`; z `package.json` odstraněny `@google-cloud/speech` a `google-auth-library`.
- **Incident FCM**: export **`notifyAdminsOnDeviceIncident`** místo `notifyAdminsOnMatejIncident`; data payload `type: device_incident` (Android `SeniorHubMessagingService`); notifikace bez textu „Matěj“; trigger na jakýkoli nový dokument v `devices/{deviceId}/incidents/{incidentId}` (bez filtru `source`).
- **Web**: neutrální popisky u `assistant_name` (volitelné pole ve Firestore; výchozí prázdný řetězec při uložení).
- **Dokumentace**: smazán `docs/matej-vad-cloud-stt.md`; aktualizace `docs/PROJECT_CONTEXT.md`.

## Nasazení

```bash
cd firebase/functions && npm install && npm run build
firebase deploy --only functions:notifyTabletOnNewMessage,functions:notifyAdminsOnDeviceIncident
```

V Firebase Console případně odstranit staré funkce `notifyAdminsOnMatejIncident`, `matejTranscribeSpeech`, `matejSynthesizeSpeech`, `matejStreamSttHttp`, pokud jsou ještě nasazené.

## Historie

Starší milníky a specifikace zůstávají v `docs/changes/` pod názvy `*matej*` jako archiv.
