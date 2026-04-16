# 2026-05-02 — Matěj: směr bez spolehlivého Nano, konverzace, hlas, účtování

Produktové a technické poznámky z ladění na zařízení (referenční tablet často **nemá Gemini Nano / AICore** ve stavu `AVAILABLE` — OEM variabilita).

## Realita hardwaru

- **Nano nelze brát jako základ pro všechny tablety.** Když ML Kit hlásí `UNAVAILABLE`, jediná spolehlivá cesta je **cloud LLM** (aktuálně výchozí např. **Gemini 3.1 Flash-lite** přes `gemini.cloud.model`, srov. `app/build.gradle.kts`).
- **Probuzení hlasem (Porcupine)** je volitelné; spolehlivější je **explicitní start** (tlačítko na dashboardu, případně PIN/gesto dle produktu) → rovnou cloud inference bez závislosti na lokálním modelu.

## Produktový cíl (dvě role jedné „postavy“)

1. **Asistent prostředí** — přirozená řeč → záměry → bezpečné akce v aplikaci (kontakty, SMS, hovor s potvrzením, navigace UI). Dnes blíž **strukturované odpovědi + potvrzení** než plné dialogy.
2. **Společník** — seniori často osamělí; dlouhodobě smysl dává **konverzační** vrstva (paměť relace, empatický tón, limity délky), odděleně od „nebezpečných“ akcí (které zůstanou přes potvrzení / tool calling).

Současný **systémový Android TTS** je pro tento produkt často **nepřijatelně mechanický**; jde o limit platformy, ne jen nastavení `speechRate`. Směr řešení: **neurální TTS** (výběr hlasu podle jazyka, případně prémiový poskytovatel), konfigurovatelné v produktu / balíčku.

## Placený LLM a ekonomika

- Klíč k rozšířené konverzaci nesmí zůstat jen v APK (`local.properties` = vývoj).
- **Produkční model**: volání Gemini (nebo ekvivalentu) přes **backend** (např. Cloud Functions / vlastní API), který:
  - drží **API klíč** a **kvóty**,
  - **účtuje spotřebu** vázanou na zařízení / předplatné (Firestore nebo billing systém),
  - vynucuje **měsíční / denní strop** tokenů nebo nákladů → po překročení srozumitelná hláška uživateli, ne tichý dluh.
- **Paušál** = předem definovaný objem (tokeny / požadavky) v ceně; **marže** = rozdíl mezi velkoobchodní cenou modelu a retailovou cenou balíčku — nutné měřit v produkci (telemetrie agregovaná, ne ukládat obsah konverzací bez souhlasu).

## Technický návrh (iterace)

| Oblast | Krátký směr |
|--------|-------------|
| Aktivace | Primárně UI + volitelně wake; první inference cloud |
| Mozek | Postupně od JSON slotů k **dialogu** + **nástroje** pro akce (volání/SMS) s potvrzením |
| Hlas | Neural TTS / výběr hlasu; nesystémový výchozí pro Senior režim |
| Bezpečnost API | Žádný sdílený klíč v klientské app v produkci; kvóty na backendu |

## Související

- Laťka AI a modely: `docs/changes/2026-05-01-matej-mvp-ai-quality-bar-nano-flash.md`
- Spec Matěje 2.0: `docs/changes/2026-04-25-matej-2-product-spec.md`
