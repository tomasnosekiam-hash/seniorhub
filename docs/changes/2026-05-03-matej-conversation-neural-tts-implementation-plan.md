# 2026-05-03 — Matěj: konverzace bez skriptů, neurální čeština (TTS), účtování řeči

Dokumentuje **dohodu o směru** a **implementační plán** pro hlasového asistenta (Matěj) na senior tabletu. Navazuje na `docs/changes/2026-05-02-matej-cloud-companion-voice-billing-direction.md` a praxi v kódu (`matej/*`, `MatejVoicePipeline`, `MatejBrainPrompt`).

## Produktová zásada: ne skript, ale pravidla

- **Konverzace** má být **přirozená** — tón, navazování, společník; **ne** pevný scénář po větách v aplikaci.
- **Natvrdo** patří jen **tenká bezpečnostní pravidla** k akcím s dopadem do reálného světa: zejména **SMS a hovor** přes **jedno jasné potvrzení** (ano/ne / strukturovaný výstup), ne předčítání předem napsaných větví dialogu.
- **LLM** (cloud Gemini Flash v aplikaci; dříve zvažované Nano v zařízení) drží **dialog a záměr**; Kotlin drží **mapování na nástroje** (kontakty, potvrzení), ne „scénář hovoru“.

## STT vs TTS (pro Lenova a obecně)

- **Robotický výstup** je primárně problém **TTS** (Text-to-Speech), ne STT. Špatné **rozpoznávání řeči** (STT) je **samostatný** problém (hluk, OEM, čeština).
- **Nejrychlejší zlepšení hlasu bez kódu:** na tabletu **Nastavení → Systém → Jazyk a vstup → Výstup textu na řeč** — **Preferovaný modul: Hlasové služby Google**, doinstalovat **česká hlasová data**, zvolit **neurální variantu** (např. Hlas II / III), pokud je OEM nabízí.
- Tím se **neúčtuje nic** za syntézu — běží na zařízení v rámci systému Google.

## Účtování: nerad „speech“ v ceně

**Preferovaný produktový model:**

- **Účtovat cloud LLM** (Gemini přes backend / klíč ve vývoji) lze strukturovaně; **syntéza řeči zákazníkovi neúčtovat jako samostatný „speech meter“**, pokud stačí **systémový neurální TTS** (Google na zařízení).
- **Placený cloud TTS** (ElevenLabs, Azure Neural, OpenAI TTS atd.) řadit jako **volitelný prémiový doplněk** nebo **B2B**, ne jako výchozí nutnost — kvůli **latenci, nákladům, soukromí a offline**.

**STT:** vestavěný Android `SpeechRecognizer` je zdarma z pohledu API klíče, ale **kvalita je proměnlivá**. Lepší přepis (např. cloud Whisper, Google Cloud Speech) znovu zvyšuje **náklady a komplexitu** — řešit až po vyčerpání úprav intentů a uživatelského nastavení.

## Reference (externí možnosti — nejsou MVP povinné)

| Oblast | Poznámka |
|--------|----------|
| ElevenLabs / Azure Neural / OpenAI TTS | Špičková kvalita, typicky **API + síť + klíč**; vhodné jako pozdější volba. |
| Whisper (+ velký model) | Silné STT; často **server** nebo těžší on-device řešení. |

## Implementační plán (fáze)

### Fáze 0 — Okamžité (bez změny APK)

- Ověřit na referenčním tabletu Lenovo **Google neurální češtinu** podle nastavení výše.
- V dokumentaci pro rodinu / onboarding krátká nápověda: „pro přirozenější hlas nastavte Google hlasová data“.

### Fáze 1 — Konverzace v kódu (tenké pravidlo, ne skript)

- **Prompt / JSON:** posílit systémovou instrukci: přirozený dialog; **SMS/hovor** jen přes `confirm_*` nebo explicitní potvrzení; žádné předčítání „jsem asistent“.
- **Ořezat** redundantní heuristické větve, kde duplikují LLM — nechat heuristiky jako **fallback** při výpadku sítě/klíče.
- **Paměť relace:** držet stávající okno historie; případně zkrátit / sumarizovat u dlouhých relací (později).

### Fáze 2 — TTS v aplikaci (volitelné, stále bez placeného API)

- Zvážit **`TextToSpeech` s výběrem engine / hlasu** podle dostupných **Google** hlasů (ne nový placený provider).
- Jednoduchá **nastavení v `config`** nebo interní předvolba: jazyk `cs-CZ`, preferovaný **voice name** pokud existuje (po detekci `voices`).
- Zachovat **krátké** systémové hlášky u chyb STT, aby TTS neotravoval.

### Fáze 3 — Spolehlivější STT (stále preferovat bez „speech bill“)

- Ladění **intent extras** / retry (část už v `MatejVoicePipeline`).
- Zvážit **přepsání audio bufferu** přes **cloud STT** jen při **explicitním souhlasu / prémiu** nebo jen na Wi‑Fi — **samostatná produktová volba**, ne výchozí nutnost.

### Fáze 4 — Prémiový hlas (volitelně)

- Jedna integrace (např. Azure nebo ElevenLabs) za **backend proxy** (klíč ne v APK), stream nebo předgenerování krátkých vět.
- **Účtování:** vázat na předplatné / kredity produktu, ne na metr „sekund TTS“ odděleně od LLM, pokud to produkt nechce.

## Související soubory v repu

- `seniorhub-android/app/src/main/java/com/seniorhub/os/matej/MatejBrainPrompt.kt` — systémový prompt a JSON schéma.
- `seniorhub-android/app/src/main/java/com/seniorhub/os/matej/MatejCompositeBrain.kt` — Flash → heuristiky (bez Nano; viz 2026-05-04).
- `seniorhub-android/app/src/main/java/com/seniorhub/os/matej/MatejVoicePipeline.kt` — STT/TTS.
- `docs/changes/2026-05-02-matej-cloud-companion-voice-billing-direction.md` — cloud companion, paušál, neurální TTS jako směr.

## Otevřené rozhodnutí

- Kdy přesunout Gemini volání **jen na backend** (kvóty, účtování LLM) — viz 2026-05-02; **TTS zůstává preferenčně lokální** dokud neurální Google čeština stačí.

---

## Stav implementace (k 2026-05-04)

Konkrétní popis toho, co je dnes v repu (cloud Flash bez Nano, volitelný Cloud Gemini TTS, tok relace, GCP): viz **[`2026-05-04-matej-cloud-gemini-flash-tts-android.md`](2026-05-04-matej-cloud-gemini-flash-tts-android.md)**.
