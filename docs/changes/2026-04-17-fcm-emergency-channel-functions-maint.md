# 2026-04-17 — Nouzový FCM kanál, údržba Cloud Functions

## Shrnutí

- **Android (Admin i příjem FCM)**: nový `NotificationChannel` **`emergency_incidents`** (`SeniorHubApp.CHANNEL_ID_EMERGENCY_INCIDENT`) — vysoká priorita, vibrace, **výchozí systémový zvuk** (bez vlastního `raw`). Vzkazy zůstávají na **`family_messages`**; foreground služba Matěje dál na **`matej_assistant`** (nízká priorita).
- **`SeniorHubMessagingService`**: při `data.type == matej_incident` a aplikaci v popředí lokální notifikace přes stejný kanál jako u doručení na pozadí (oddělené `notificationId` od vzkazů).
- **Cloud Functions**: `notifyAdminsOnMatejIncident` posílá `android.notification.channelId: emergency_incidents`; vzkazy beze změny chování (`family_messages`). Kód rozdělen do modulů (`src/adminFcmTokens.ts`, `src/fcmConstants.ts`) — **`index.ts`** jen exportuje triggery.
- **Runtime**: `package.json` → **Node.js 22** (náhrada za brzy zastaralý Node 20 na GCF).
- **Závislosti**: upgrade na **`firebase-functions` ^7** a aktuální **`firebase-admin`** (viz `firebase/functions/package.json`); build `tsc` bez úprav handlerů.
- **Deploy**: funkce nasazené na `seniorhub-716f0` (Node.js **22**, viz výstup `firebase deploy --only functions`).

## Nasazení

```bash
cd /path/to/oldies && npx firebase-tools@latest deploy --only functions --project seniorhub-716f0
```

## Soubory (orientačně)

- `seniorhub-android/.../SeniorHubApp.kt`, `SeniorHubMessagingService.kt`, `res/values/strings.xml`
- `firebase/functions/src/index.ts`, `adminFcmTokens.ts`, `fcmConstants.ts`, `package.json`

## Poznámka k větším souborům (Android)

- `ui/HomeScreen.kt` (~1.3k řádků) je hlavní kandidát na budoucí rozdělení podle sekcí dashboardu (kontakty, vlákno, počasí, oprávnění). V této etapě jen zdokumentováno; refaktor až s testy na zařízení.
