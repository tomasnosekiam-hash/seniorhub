# 2026-04-27 — Android: klíčové slovo (Porcupine) → probuzení → TTS → naslouchání (5 s)

## Shrnutí

- **Porcupine** (`ai.picovoice:porcupine-android`) — foreground služba [MatejForegroundService.kt](../../seniorhub-android/app/src/main/java/com/seniorhub/os/matej/MatejForegroundService.kt), typ `microphone`, notifikační kanál `matej_wake_listen`.
- **Access key** z `local.properties`: `picovoice.access.key=…` → `BuildConfig.PICOVOICE_ACCESS_KEY`. Bez klíče se služba nespustí.
- **Vlastní slovo (česky):** volitelně `assets/porcupine/keyword.ppn` z Picovoice Console; jinak vestavěné anglické „Porcupine“ (ověření toku). Návod: `assets/porcupine/README.txt`.
- Po detekci: pozastavení Porcupine → [SeniorHubApp.emitMatejWake](../../seniorhub-android/app/src/main/java/com/seniorhub/os/SeniorHubApp.kt) → [HomeViewModel](../../seniorhub-android/app/src/main/java/com/seniorhub/os/ui/HomeViewModel.kt): UI **Greeting** → **TTS** (`MatejVoicePipeline.speakGreeting`) → **Listening** → **jedno** STT (`listenOnceCsOrTimeout`, timeout 5 s) → skrytí UI → **resume** Porcupine.
- **Stav Matěje** v `HomeViewModel` (`matejSession`); [HomeRoute](../../seniorhub-android/app/src/main/java/com/seniorhub/os/ui/HomeScreen.kt) skládá kompaktní režim při vlákně/SMS.
- Oprávnění: `RECORD_AUDIO` (Senior v MainActivity), `FOREGROUND_SERVICE_MICROPHONE` v manifestu.

Produktová spec: [2026-04-25-matej-2-product-spec.md](2026-04-25-matej-2-product-spec.md).

## Otestovat

1. V `local.properties` doplnit `picovoice.access.key`.
2. Spárovaný tablet Senior, povolit mikrofon.
3. Bez vlastního `keyword.ppn` říct nahlas **„Porcupine“** (anglicky, vestavěný model).
4. Ověřit: notifikace naslouchání → po slově UI Matěje → české TTS → poslech max. 5 s → návrat do režimu naslouchání klíči.
