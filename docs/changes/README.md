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
| [`2026-04-16-android-cloud-remove-matej-voice-ai.md`](2026-04-16-android-cloud-remove-matej-voice-ai.md) | Kompletní odstranění hlasové vrstvy z Androidu + `matej*` Functions; `notifyAdminsOnDeviceIncident` + `device_incident` |

## 2026-04-17

| Soubor | Téma |
|--------|------|
| [`2026-04-17-fcm-emergency-channel-functions-maint.md`](2026-04-17-fcm-emergency-channel-functions-maint.md) | Nouzový notif. kanál `emergency_incidents` (Android + FCM payload), refaktoring functions, Node 22 |

## Plán (refaktoring bez změny chování)

| Soubor | Téma |
|--------|------|
| [`2026-04-18-home-screen-refactor-plan.md`](2026-04-18-home-screen-refactor-plan.md) | Rozdělení velkého `HomeScreen.kt` — navrhované soubory, pořadí kroků, testy |
| [`2026-04-18-android-home-overlays-split.md`](2026-04-18-android-home-overlays-split.md) | Hotovo: overlaye přesunuty do `HomeOverlays.kt` |
| [`2026-04-19-android-home-dashboard-matej-debug-test.md`](2026-04-19-android-home-dashboard-matej-debug-test.md) | `HomeDashboard.kt`; debug „Test nouze · DBG“ pro Matěje |

## 2026-04-20

| Soubor | Téma |
|--------|------|
| [`2026-04-20-matej-postwake-skills-natural-speech-plan.md`](2026-04-20-matej-postwake-skills-natural-speech-plan.md) | Matěj: dovednosti po probuzení (počasí, vzkaz, SMS, hovor); STT prompt; plán vrstvy přirozené řeči (Nano / cloud / heuristiky) |

## 2026-04-21

| Soubor | Téma |
|--------|------|
| [`2026-04-21-tablet-reference-on-hand-verification.md`](2026-04-21-tablet-reference-on-hand-verification.md) | Referenční tablet Lenovo Idea Tab 11 5G u vývojáře — milník pro ladění na zařízení (kiosk, Matěj, oprávnění, AICore) |

## 2026-04-22

| Soubor | Téma |
|--------|------|
| [`2026-04-22-pairing-ux-firestore-matej-stt-firebase-cli.md`](2026-04-22-pairing-ux-firestore-matej-stt-firebase-cli.md) | Web: pořadí sekcí párování, nápověda, kontrola existující vazby, batch včetně `paired`; Firestore: viewer dokončení párování, `pairingClaims` diff; Matěj: STT interval + tlumení pípání; Firebase CLI `activeProjects` / `firebase use` |

## 2026-04-23

| Soubor | Téma |
|--------|------|
| [`2026-04-23-android-matej-postwake-dashboard-m3-responsive.md`](2026-04-23-android-matej-postwake-dashboard-m3-responsive.md) | Matěj: prázdný STT → `Unclear` (ne nouze), okno poslechu po probuzení, `CALL_PHONE` + TTS; `PhoneDialer` bez crash; Senior dashboard M3 (typografie, barvy, tvary), breakpoint 840 dp, stagger fade kontaktů, stringy |

## 2026-04-24

| Soubor | Téma |
|--------|------|
| [`2026-04-24-android-remove-matej-voice-module.md`](2026-04-24-android-remove-matej-voice-module.md) | Android: odstranění hlasového asistenta (STT heuristiky, Porcupine, foreground služba, incidenty z tabletu); PIN/SIM admin bez úpravy jména asistenta |

## 2026-04-25

| Soubor | Téma |
|--------|------|
| [`2026-04-25-matej-2-product-spec.md`](2026-04-25-matej-2-product-spec.md) | Matěj 2.0: produktová a UX spec (wake, Nano/Flash, potvrzení akcí, 5 s ticho, viditelné UI + kompaktní roh); bez STT smyčky a pípání — před implementací |

## 2026-04-26

| Soubor | Téma |
|--------|------|
| [`2026-04-26-android-matej-assistant-ui-milestone.md`](2026-04-26-android-matej-assistant-ui-milestone.md) | Android: Matěj 2.0 první milník — overlay UI (rozšířený + kompaktní roh), náhled přes debug FAB „M“ |

## 2026-04-27

| Soubor | Téma |
|--------|------|
| [`2026-04-27-android-matej-wake-listen-flow.md`](2026-04-27-android-matej-wake-listen-flow.md) | Android: Porcupine wake → TTS → STT 5 s → resume; `local.properties` Picovoice; volitelný `assets/porcupine/keyword.ppn` |

## 2026-04-28

| Soubor | Téma |
|--------|------|
| [`2026-04-28-android-matej-2-heuristics-dashboard.md`](2026-04-28-android-matej-2-heuristics-dashboard.md) | Android: Matěj 2.0 — relace TTS → STT 5 s → heuristiky → TTS; karta „Spustit Matěje“ na dashboardu; sdílený tok s Porcupine wake |

## 2026-04-29

| Soubor | Téma |
|--------|------|
| [`2026-04-29-android-matej-gemini-brain-confirmation.md`](2026-04-29-android-matej-gemini-brain-confirmation.md) | Matěj 2.0: MatejBrain, Gemini Flash + heuristiky; potvrzení SMS/hovoru (ano/ne); `gemini.api.key` v `local.properties` |

## 2026-04-30

| Soubor | Téma |
|--------|------|
| [`2026-04-30-matej-device-verification-and-ai-clarification.md`](2026-04-30-matej-device-verification-and-ai-clarification.md) | Matěj 2.0: ověření na zařízení; vysvětlení TTS vs Gemini Flash vs Nano (Nano zatím ne) |

## 2026-05-01

| Soubor | Téma |
|--------|------|
| [`2026-05-01-matej-mvp-ai-quality-bar-nano-flash.md`](2026-05-01-matej-mvp-ai-quality-bar-nano-flash.md) | Laťka AI pro MVP Matěje: Nano jako základ; cloud min. 2.5, volitelně 3.0 Flash / 3.1 Flash-lite (cena); bez Nano není MVP-ready; výchozí cloud v buildu 2.5 Flash |

## 2026-05-02

| Soubor | Téma |
|--------|------|
| [`2026-05-02-matej-cloud-companion-voice-billing-direction.md`](2026-05-02-matej-cloud-companion-voice-billing-direction.md) | Matěj: Nano často nedostupné na OEM → cloud jako spolehlivá cesta; tlačítko/PIN vs wake; konverzace + společník; neurální TTS; paušál, strop, backend billing |

## 2026-05-03

| Soubor | Téma |
|--------|------|
| [`2026-05-03-matej-conversation-neural-tts-implementation-plan.md`](2026-05-03-matej-conversation-neural-tts-implementation-plan.md) | Matěj: konverzace bez skriptů (jen tenká pravidla pro akce); Google neurální čeština jako první krok TTS; neúčtovat speech zbytečně; fázovaný implementační plán |

## 2026-05-04

| Soubor | Téma |
|--------|------|
| [`2026-05-04-matej-cloud-gemini-flash-tts-android.md`](2026-05-04-matej-cloud-gemini-flash-tts-android.md) | **Aktuální stav Matěje v kódu:** Flash → heuristiky (bez Nano), Cloud Gemini TTS přes Callable, relace (pozdrav / ještě něco / nashle), `local.properties`, GCP, odkazy na starší dokumenty |

Pro **nejnovější etapu** použij soubor s **nejvyšším datem** v názvu; při stejném datu rozhoduje čas poslední úpravy souboru (viz `PROJECT_CONTEXT.md`).
