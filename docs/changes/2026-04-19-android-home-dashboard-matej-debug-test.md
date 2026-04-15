# 2026-04-19 — Dashboard do `HomeDashboard.kt`, debug test Matěje

## Shrnutí

- **`HomeDashboard.kt`**: `HomeScreen`, sloupce (`StatusColumn`, `ContactsColumn`, `ContactRow`, `PairingSummaryCard`) + preview; hlavní UI senior dashboardu oddělené od routy.
- **`HomeScreen.kt`**: jen **`HomeRoute`** (overlay stack, SMS, sync Matěje) a **`KioskPinningEffect`** (~260 řádků).
- **Testování Matěje (debug APK)**: v pravém dolním rohu dashboardu tlačítko **„Test nouze · DBG“** — volá `MatejForegroundService.triggerTestEmergency` → stejná sekvence jako po probuzení (vytočení + zápis `incidents` + TTS), **bez wake word**. Viditelné jen při `BuildConfig.DEBUG`.
- **`MatejForegroundService`**: akce `ACTION_TEST_EMERGENCY` (ignorována v release; `triggerTestEmergency` no-op mimo debug).

## Jak testovat

1. **Debug build** na tabletu ve Senior režimu, spárované zařízení (aby běžel `MatejForegroundService.sync` a snapshot měl kontakty).
2. Alespoň jeden **nouzový kontakt** nebo platné číslo / SIM v konfiguraci (viz `pickEmergencyDialTarget`).
3. Klepnout **„Test nouze · DBG“** — měl by proběhnout odchozí hovor, zápis `incidents`, TTS; u správců FCM (po nasazení functions).
4. Plný řetězec s hlasem: `picovoice.access.key` v `local.properties` + případně `.ppn`/`.pv` v `assets/porcupine/`, oprávnění **mikrofon**.

## Soubory

- `seniorhub-android/.../ui/HomeDashboard.kt`, `HomeScreen.kt`
- `seniorhub-android/.../matej/MatejForegroundService.kt`
