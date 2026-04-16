# 2026-04-29 — Matěj 2.0: MatejBrain, Gemini Flash, potvrzení SMS/hovoru

## Shrnutí

- Rozhraní **[MatejBrain](../../seniorhub-android/app/src/main/java/com/seniorhub/os/matej/MatejBrain.kt)** a výsledek tahu **MatejTurnOutcome** (`Speak` / `ConfirmSendSms` / `ConfirmCall`).
- **Gemini 2.5 Flash** (cloud, výchozí `BuildConfig.GEMINI_CLOUD_MODEL`) přes `com.google.ai.client.generativeai:generativeai` — JSON odpověď; při chybě sítě, timeoutu nebo parsování záloha **[MatejHeuristicBrain](../../seniorhub-android/app/src/main/java/com/seniorhub/os/matej/MatejHeuristicBrain.kt)** (**[MatejCompositeBrain](../../seniorhub-android/app/src/main/java/com/seniorhub/os/matej/MatejCompositeBrain.kt)**). Produktová laťka (Nano jako základ): `docs/changes/2026-05-01-matej-mvp-ai-quality-bar-nano-flash.md`.
- **Potvrzení**: po TTS výzvě druhý poslech (5 s), **[MatejConfirmationPhrases](../../seniorhub-android/app/src/main/java/com/seniorhub/os/matej/MatejConfirmationPhrases.kt)** (ano/ne); SMS přes **[SmsSender](../../seniorhub-android/app/src/main/java/com/seniorhub/os/util/SmsSender.kt)** + Firestore zrcadlo nebo Firestore-only bez SIM; hovor přes **[PhoneDialer](../../seniorhub-android/app/src/main/java/com/seniorhub/os/util/PhoneDialer.kt)** / číselník bez `CALL_PHONE`.
- UI: fáze **Confirming** v **[MatejAssistantUi](../../seniorhub-android/app/src/main/java/com/seniorhub/os/ui/MatejAssistantUi.kt)** (řádek „Řekněte ano nebo ne“).

## Konfigurace vývojáře

V `seniorhub-android/local.properties`:

```properties
gemini.api.key=…
```

Klíč z [Google AI Studio](https://aistudio.google.com/) (Gemini API). Bez klíče zůstávají jen heuristiky.

## Nano (AICore)

Zatím neintegrováno — stejné rozhraní [MatejBrain] umožní později přidat on-device model jako další implementaci / výběr v továrně.

## Související

- Produktová spec: `docs/changes/2026-04-25-matej-2-product-spec.md`
- Předchozí milník heuristik: `docs/changes/2026-04-28-android-matej-2-heuristics-dashboard.md`
