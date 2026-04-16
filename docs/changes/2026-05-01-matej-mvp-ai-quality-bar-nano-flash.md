# 2026-05-01 — Matěj / AI: laťka pro MVP (Nano jako základ, cloud ≥ 2.5 Flash)

## Produktové rozhodnutí

**MVP může mít nedokonalosti — nesmí postrádat jádrovou schopnost.** Současný stav (cloudový model bez silného on-device mozku, případně slabší generace ve cloudu) je pro uživatelsky přijatelný **hlasový Matěj v MVP neakceptovatelný** — analogie: MVP auta bez kol.

## Technická laťka (pořadí priorit)

1. **Gemini Nano (AICore / on-device)** — **absolutní základ** rozumění a dialogu tam, kde OEM a zařízení dovolí. Bez Nano jako primární cesty není hlasový asistent v MVP „hotový produkt“, jen experiment.
2. **Cloud** — dotazy mimo schopnosti Nano nebo při nedostupnosti modelu na zařízení: **minimálně Gemini 2.5** (v API typicky `gemini-2.5-flash` nebo novější ekvivalent). Pokud 2.5 nestačí nebo chceme novější generaci: **Gemini 3.0 Flash**. Pro **nejlepší cenu** u cloudové odpovědi: **Gemini 3.1 Flash-lite** (až bude v API stabilní identifikátor — přepínat v `gemini.cloud.model`). Pořadí rozhodování: laťka kvality → 2.5 → 3.0 Flash → 3.1 Flash-lite podle měření a rozpočtu.
3. **Heuristiky** — jen bezpečný **fallback** (offline / chyba), ne náhrada za rozumné NLU.

## Stav v kódu (k datu tohoto zápisu)

- Cloudová cesta používá **Gemini API** přes `GenerativeModel`; výchozí ID modelu je **`gemini-2.5-flash`**, přepínatelné v `local.properties` (`gemini.cloud.model`). Dřívější **2.0 Flash** jako výchozí bylo pro vývoj nedostatečné vůči laťce výše.
- **Nano v kódu zatím není napojené** — další krok: implementace `MatejBrain` pro on-device vrstvu a skládání před cloud (viz `docs/changes/2026-04-21-tablet-reference-on-hand-verification.md`).

## Související

- Produktová spec Matěje 2.0: `docs/changes/2026-04-25-matej-2-product-spec.md`
- Ověření zařízení / vysvětlení TTS vs Flash: `docs/changes/2026-04-30-matej-device-verification-and-ai-clarification.md`
