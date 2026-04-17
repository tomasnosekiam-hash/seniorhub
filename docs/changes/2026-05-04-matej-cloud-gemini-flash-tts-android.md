# 2026-05-04 — Matěj: cloud Gemini Flash, Cloud Gemini TTS, bez Nano, zjednodušená relace

Shrnuje **aktuální architekturu** hlasového asistenta Matěj v `seniorhub-android` a **Firebase Functions** po sérii úprav (mozek, TTS, prompty, tok relace).

## Mozek (rozhodování)

- **Pořadí:** **Gemini Flash (cloud)** přes `GenerativeModel` + `gemini.api.key` v `local.properties` → při výpadku sítě nebo prázdném klíči **heuristiky** (`MatejHeuristicBrain`).
- **Gemini Nano on-device bylo odstraněno** — třída `MatejNanoBrain` a závislost `com.google.mlkit:genai-prompt` už nejsou v projektu (rychlejší cesta k odpovědi, žádné čekání na nedostupný AICore).
- **Soubory:** `MatejBrainFactory.kt`, `MatejCompositeBrain.kt`, `MatejGeminiFlashBrain.kt`, `MatejHeuristicBrain.kt`, `MatejBrainPrompt.kt` (krátký systémový prompt JSON, `MATEJ_HISTORY_MAX_TURNS = 4`), `MatejBrain.kt`.

## Řeč (TTS)

- **Primárně (volitelné):** **Cloud Text-to-Speech — Gemini TTS** přes HTTPS callable **`matejSynthesizeSpeech`** (`firebase/functions/src/matejTts.ts`, region `europe-west1`), model např. `gemini-2.5-flash-tts`, EU endpoint, výstup MP3; klíče **nejsou v APK**.
- **Zapnutí v buildu:** `seniorhub-android/local.properties` → `matej.cloud.tts.enabled=true` (Gradle → `BuildConfig.MATEJ_GEMINI_TTS_ENABLED`).
- **Android:** `MatejCloudTts.kt` (callable → base64 → `MediaPlayer`), voláno z `MatejVoicePipeline` před systémovým TTS; Firebase Auth (případně anonymní přihlášení) kvůli callable.
- **Fallback:** systémový `TextToSpeech` (výchozí engine zařízení, `cs-CZ`).
- **GCP:** musí být zapnuté **Cloud Text-to-Speech API** a často i **Vertex AI API** (`aiplatform.googleapis.com`); billing; runtime service account funkce s rolí vhodnou pro TTS (např. `roles/cloudtexttospeech.user`). Funkce obsahuje opakování při přechodných 403 od Vertex propagace.

## Konverzace (produkťák + kód)

- **Pozdrav:** pevný text z `matej_tts_greeting` — „Ahoj, jak ti můžu pomoci?“
- **Po každé odpovědi asistenta:** přehrání **„Můžu ještě s něčím pomoct?“** (`matej_session_anything_else`) → poslech → při odmítnutí / rozloučení **„Nashle.“** a konec relace (`HomeViewModel.runMatejAssistantSession`).
- **Prompty** zkrácené — bez dlouhých návodů „jak nadiktovat SMS“; STT hint zkrácen (`matej_stt_prompt`).
- **Cloud TTS styl:** krátký `TTS_PROMPT_CS` ve funkci (stručná čeština); `speakingRate` v `audioConfig` pro méně „pomalý“ projev.

## Konfigurace vývojáře

| `local.properties` (seniorhub-android) | Význam |
|----------------------------------------|--------|
| `gemini.api.key` | Klíč Google AI Studio / Gemini API pro Flash v aplikaci |
| `gemini.cloud.model` | Volitelně jiný model (výchozí dle `build.gradle.kts`) |
| `matej.cloud.tts.enabled=true` | Zapne Cloud Gemini TTS místo jen systémového TTS |

## Nasazení backendu

```bash
firebase deploy --only functions:matejSynthesizeSpeech
```

## Související dokumentace

- Produktový plán konverzace a TTS: [`2026-05-03-matej-conversation-neural-tts-implementation-plan.md`](2026-05-03-matej-conversation-neural-tts-implementation-plan.md)
- Směr cloud companion / hlas / účtování: [`2026-05-02-matej-cloud-companion-voice-billing-direction.md`](2026-05-02-matej-cloud-companion-voice-billing-direction.md)
- Historický popis mozku s Nano v řetězci (zastaralé pro aktuální kód): [`2026-04-29-android-matej-gemini-brain-confirmation.md`](2026-04-29-android-matej-gemini-brain-confirmation.md)
