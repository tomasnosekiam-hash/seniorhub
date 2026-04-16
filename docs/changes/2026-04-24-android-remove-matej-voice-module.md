# Android: odstranění hlasového asistenta (heuristiky / Porcupine / STT)

**Datum:** 2026-04-24  
**Rozsah:** `seniorhub-android` — modul „Matěj“ v podobě pravidel + Google STT + volitelný Porcupine.

## Důvod

Současná úroveň (wake přes STT nebo anglický vestavěný Porcupine, klasifikace příkazů pravidly) neodpovídala očekávání „důstojné“ asistence. Dokud nebude k dispozici spolehlivé lokální probuzení na definované slovo a rozumné porozumění (např. Nano / cloud podle produktu), zůstává v aplikaci **žádný** hlasový asistent.

## Co bylo odstraněno

- Celý balík `com.seniorhub.os.matej` včetně:
  - `MatejForegroundService` (foreground služba, TTS, STT, skills)
  - `MatejPostWake.kt`, `MatejWakeText.kt`, `MatejPorcupineWake.kt`, `MatejEmergencyPhone.kt`, `MatejEmergencyConfig.kt`
- Unit testy `MatejPostWakeTest.kt`
- Spouštění / synchronizace služby z `HomeScreen.kt` (`LaunchedEffect` + `MatejForegroundService.sync` / `stop`)
- Debug tlačítko „Test nouze · DBG“ na `HomeDashboard.kt`
- `MainActivity`: volání `MatejForegroundService.stop`, žádost o `RECORD_AUDIO` v režimu Senior
- Manifest: služba `MatejForegroundService`; oprávnění `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE`
- Gradle: závislost `ai.picovoice:porcupine-android`, `buildConfigField` `PICOVOICE_ACCESS_KEY` a čtení `picovoice.access.key` z `local.properties`
- Notifikační kanál asistenta v `SeniorHubApp`; stringy `matej_*` a související popisy v `strings.xml`
- `MvpRepository.recordMatejEmergencyIncident` — tablet už **nezapisuje** incidenty z této cesty
- Konstanta `VAL_INCIDENT_SOURCE_MATEJ` odstraněna z companion objectu

## Co zůstává (kompatibilita / backend)

- **FCM** v `SeniorHubMessagingService`: typ dat `matej_incident` se stále zpracovává (Cloud Functions můžou posílat dál); uživatelsky je kanál popsán jako **incident / nouze**, ne jako „Matěj“.
- **`devices/.../config/main`**: pole `assistant_name` zůstává ve schématu Firestore; výchozí hodnota při čtení už není vynucená jako „Matěj“ (`DEFAULT_ASSISTANT_NAME = ""`).
- **Admin Android**: sekce **PIN a SIM** — pole pro úpravu jména asistenta odstraněno; při uložení se do Firestore posílá stávající `assistant_name` z načteného configu (beze změny), dokud ji web nebo jiný klient neupraví.

## Související dokumentace

- Historické popisy implementace Matěje zůstávají ve starších souborech `docs/changes/2026-04-16-*.md`, `2026-04-20-*.md`, `2026-04-22-*.md`, `2026-04-23-*.md` jako kontext; **aktuální stav produktu** je tento soubor + aktualizovaný snapshot v `docs/PROJECT_CONTEXT.md`.

## Cloud Functions / web

- Funkce `notifyAdminsOnMatejIncident` a pojmenování typu v payloadu v tomto commitu **neměněny** — případná přejmenování na obecné `device_incident` a úprava webové administrace (`assistant_name`) jsou samostatný krok.
