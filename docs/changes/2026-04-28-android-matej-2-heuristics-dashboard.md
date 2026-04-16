# 2026-04-28 — Android: Matěj 2.0 — relace (TTS → STT → heuristiky → TTS), vstup z dashboardu

## Shrnutí

- **Produktová spec** (záměr včetně Gemini): [`2026-04-25-matej-2-product-spec.md`](2026-04-25-matej-2-product-spec.md). Tato etapa **neimplementuje** Gemini / AICore — rozumění je dočasně přes **české heuristiky** v [`MatejHeuristics.kt`](../../seniorhub-android/app/src/main/java/com/seniorhub/os/matej/MatejHeuristics.kt) (počasí z `HomeUiState.weatherLine`, čas, nápověda, odkaz na volání/SMS přes UI, fallback).
- **Hlasový tok**: sdílená suspend funkce [`HomeViewModel.runMatejAssistantSession`](../../seniorhub-android/app/src/main/java/com/seniorhub/os/ui/HomeViewModel.kt) — fáze **Greeting** → **Listening** (5 s) → **Processing** → odpověď přes [`MatejVoicePipeline.speakText`](../../seniorhub-android/app/src/main/java/com/seniorhub/os/matej/MatejVoicePipeline.kt). Stejný tok používá probuzení z **Porcupine** (když je v APK `PICOVOICE_ACCESS_KEY`) a **ruční start**.
- **UI**: stavy `MatejPhase` / [`MatejAssistantChrome`](../../seniorhub-android/app/src/main/java/com/seniorhub/os/ui/MatejAssistantUi.kt); na přehledu (jen **spárovaný** tablet) karta **Hlasový asistent** + tlačítko **Spustit Matěje** v [`HomeDashboard.kt`](../../seniorhub-android/app/src/main/java/com/seniorhub/os/ui/HomeDashboard.kt) → [`HomeViewModel.startMatejAssistant`](../../seniorhub-android/app/src/main/java/com/seniorhub/os/ui/HomeViewModel.kt). Debug FAB „M“ zůstává pro náhled stavů (debug build).
- **Řetězce**: `dashboard_matej_*`, `matej_reply_*` v [`strings.xml`](../../seniorhub-android/app/src/main/res/values/strings.xml).

## Další kroky (mimo tuto etapu)

- Napojení **Gemini Nano / Flash** místo nebo za heuristiky; strukturované nástroje (SMS, hovor) s **hlasovým potvrzením** dle spec.
- Wake word **Porcupine** zůstává v kódu; vyžaduje Picovoice klíč v `local.properties` — viz [`2026-04-27-android-matej-wake-listen-flow.md`](2026-04-27-android-matej-wake-listen-flow.md).
