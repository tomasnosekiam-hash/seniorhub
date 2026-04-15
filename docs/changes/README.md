# Záznamy změn (`docs/changes/`)

Každá významná etapa má vlastní soubor `YYYY-MM-DD-kratky-nazev.md`. **Aktuální stav** vždy sladit s [`docs/PROJECT_CONTEXT.md`](../PROJECT_CONTEXT.md).

## 2026-04-14 (Fáze A – platforma a Senior tablet)

| Soubor | Téma |
|--------|------|
| [`2026-04-14-multi-admin-multi-device.md`](2026-04-14-multi-admin-multi-device.md) | Multi-admin / multi-device |
| [`2026-04-14-device-config-kiosk-pin.md`](2026-04-14-device-config-kiosk-pin.md) | `config/main`, PIN, kiosk break, web |
| [`2026-04-14-messages-fcm-functions.md`](2026-04-14-messages-fcm-functions.md) | Vzkazy, FCM, Cloud Functions |
| [`2026-04-14-senior-profile-emergency.md`](2026-04-14-senior-profile-emergency.md) | Profil seniora, nouzové kontakty |
| [`2026-04-14-firestore-permission-rebind.md`](2026-04-14-firestore-permission-rebind.md) | Firestore rules – rebind po přeinstalaci |
| [`2026-04-14-contacts-touch-dial.md`](2026-04-14-contacts-touch-dial.md) | Kontakty → hovor dotykem |
| [`2026-04-14-android-send-sms.md`](2026-04-14-android-send-sms.md) | SMS odeslání z kontaktů (Android) |
| [`2026-04-14-device-status-heartbeat.md`](2026-04-14-device-status-heartbeat.md) | Baterie + heartbeat na `devices`, web |
| [`2026-04-14-fcm-foreground-notification.md`](2026-04-14-fcm-foreground-notification.md) | FCM — lokální notifikace při popředí, `singleTop` |
| [`2026-04-14-sms-vs-firestore-fallback.md`](2026-04-14-sms-vs-firestore-fallback.md) | SMS vs Firestore náhrada, pravidla, web |
| [`2026-04-14-android-admin-role-switcher.md`](2026-04-14-android-admin-role-switcher.md) | Android: Senior vs Admin, Google, přepínač zařízení |

## 2026-04-15

| Soubor | Téma |
|--------|------|
| [`2026-04-15-android-toolchain-smoke-test.md`](2026-04-15-android-toolchain-smoke-test.md) | JDK 21 v Gradle, oprava buildu, Firebase/OAuth, smoke test |
| [`2026-04-15-android-admin-contacts.md`](2026-04-15-android-admin-contacts.md) | Android admin: kontakty (seznam, Nouze, smazání, přidání) jako na webu |
| [`2026-04-15-web-contacts-reorder.md`](2026-04-15-web-contacts-reorder.md) | Web admin: řazení kontaktů (↑↓, transakce `sortOrder`) |
| [`2026-04-15-fcm-admin-status-weather-unified-messages.md`](2026-04-15-fcm-admin-status-weather-unified-messages.md) | FCM tokeny správců, `status/main` (síť), počasí Open-Meteo, náhled zpráv |
| [`2026-04-15-unified-contact-thread-sms-mirror.md`](2026-04-15-unified-contact-thread-sms-mirror.md) | Jednotné vlákno u kontaktu: zrcadlo SMS do Firestore, vlákno v UI, rules `sms_cellular` |
| [`2026-04-15-senior-kiosk-home-locktask.md`](2026-04-15-senior-kiosk-home-locktask.md) | Kiosk Senior: lock task, HOME launcher, `singleTask`, Zpět neukončí, hint domovské aplikace |

## 2026-04-16

| Soubor | Téma |
|--------|------|
| [`2026-04-16-inbound-sms-fcm-roles-web-viewer.md`](2026-04-16-inbound-sms-fcm-roles-web-viewer.md) | Příchozí SMS do vlákna, deploy rules/functions, viewer v rules, FCM bez spamu správcům, web párování viewer |
| [`2026-04-16-android-matej-incidents-porcupine-fcm.md`](2026-04-16-android-matej-incidents-porcupine-fcm.md) | Matěj: foreground služba, Porcupine/STT, `devices/.../incidents`, rules, `notifyAdminsOnMatejIncident`, deploy |

## 2026-04-17

| Soubor | Téma |
|--------|------|
| [`2026-04-17-fcm-emergency-channel-functions-maint.md`](2026-04-17-fcm-emergency-channel-functions-maint.md) | Nouzový notif. kanál `emergency_incidents` (Android + FCM payload), refaktoring functions, Node 22 |

## Plán (refaktoring bez změny chování)

| Soubor | Téma |
|--------|------|
| [`2026-04-18-home-screen-refactor-plan.md`](2026-04-18-home-screen-refactor-plan.md) | Rozdělení velkého `HomeScreen.kt` — navrhované soubory, pořadí kroků, testy |

Pro **nejnovější etapu** použij soubor s **nejvyšším datem** v názvu; při stejném datu rozhoduje čas poslední úpravy souboru (viz `PROJECT_CONTEXT.md`).
