# 2026-04-14 – FCM: notifikace při aplikaci v popředí (tablet)

## Cíl

- Když tablet **běží na popředí**, Firebase **nezobrazí** systémovou notifikaci z FCM payloadu s `notification` — uživatel tak mohl přehlédnout doručení, i když překryv ze Firestore funguje.
- Při příjmu `data.type == new_message` v `onMessageReceived` zobrazit **lokální** notifikaci ve stejném kanálu `family_messages` (náhled textu, klepnutí → `MainActivity`).
- Aktivita `MainActivity`: `launchMode="singleTop"` + `onNewIntent`, aby klepnutí na notifikaci nepřidávalo zásobník aktivit.

## Dotčené soubory

- `seniorhub-android/.../SeniorHubMessagingService.kt` — `showForegroundAwareMessageNotification`, parsování `RemoteMessage`.
- `seniorhub-android/.../MainActivity.kt` — `EXTRA_FROM_MESSAGE_NOTIFICATION`, `onNewIntent`.
- `seniorhub-android/.../AndroidManifest.xml` — `android:launchMode="singleTop"` u `MainActivity`.
- `seniorhub-android/.../values/strings.xml` — záložní řetězce pro titulek/text.

## Poznámky

- Na **pozadí** systém stále zobrazí tray notifikaci z FCM (handler se typicky nevolá) — duplicita nevzniká.
- Plný „fullscreen incoming call“ styl pro vzkazy záměrně neřešíme (omezení Android 14+ u `USE_FULL_SCREEN_INTENT`); překryv v aplikaci zůstává u `MessageOverlay`.
