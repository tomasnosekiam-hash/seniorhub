# 2026-04-30 — Matěj 2.0: ověření na zařízení, co je „hlas“ a co je „mozek“

## Ověření

- Tok **Spustit Matěje** / debug FAB (**DEBUG**) s žádostí o mikrofon a toasty při blokaci — ověřeno vývojářem jako funkční (`HomeRoute` → `requestMatejStart`, `installDebug` z Gradle).

## Co na tebe mluví — není to Gemini Nano

| Vrstva | Co to je v aktuálním kódu |
|--------|---------------------------|
| **Hlas (TTS)** | Systémový Android **TextToSpeech** v češtině (`MatejVoicePipeline`) — obecný syntetický hlas tabletu, **ne** Gemini Nano a ne „hlas modelu“ z cloudu. |
| **Rozumění příkazu (NLU)** | Při `gemini.api.key` + internetu: **Gemini 2.5 Flash** (výchozí cloud model v buildu) v cloudu (`MatejGeminiFlashBrain`). Jinak nebo při chybě: **heuristiky** (`MatejHeuristicBrain`). |
| **Gemini Nano (AICore)** | **Nepoužívá se** — zůstává plánovaná varianta on-device; rozhraní `MatejBrain` k ní může později připojit další implementaci. |

Krátké odpovědi a občas „hloupý“ dojem často souvisí s **jednoduchým promptem JSON**, **heuristikami jako zálohou** a **omezeným kontextem** (jeden tah STT), ne s tím, že by mluvil Nano.

## Související

- Implementace mozku a potvrzení: `docs/changes/2026-04-29-android-matej-gemini-brain-confirmation.md`
- Produktová spec (Nano vs Flash): `docs/changes/2026-04-25-matej-2-product-spec.md`
- **Laťka MVP a Nano vs cloud (2026-05-01):** `docs/changes/2026-05-01-matej-mvp-ai-quality-bar-nano-flash.md`
