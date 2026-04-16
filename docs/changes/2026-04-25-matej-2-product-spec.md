# 2026-04-25 — Matěj 2.0: produktová a UX specifikace (před implementací)

## Kontext

Předchozí implementace (předchystané fráze, nulové porozumění kontextu, heuristiky po STT) je **mimo produkt** — viz odstranění modulu `docs/changes/2026-04-24-android-remove-matej-voice-module.md`. Tento dokument je **jednotný zdroj** pro návrh **Matěje 2.0**: co má dělat, jak se má chovat vůči uživateli a jakým směrem řešit technickou vrstvu.

Související podklady: přirozená řeč a dovednosti po probuzení (`2026-04-20-matej-postwake-skills-natural-speech-plan.md`), referenční tablet a AICore (`2026-04-21-tablet-reference-on-hand-verification.md`). Historické chování STT v cyklu a pípání: `2026-04-22-pairing-ux-firestore-matej-stt-firebase-cli.md` — **nová architektura se tomu má vyhnout** (viz níže).

---

## Produktové cíle

### 1. Probouzení klíčovým slovem (jako „Ok, Google“)

- Aplikace **naslouchá** definovanému **klíčovému slovu** a tím **probouzí** asistenta Matěje.
- Slovo (a případné tvary) nastavuje **administrátor** — např. „Matěj“, „Matěji“, … (přesný datový model a forma zápisu ve Firestore se doladí při implementaci; technicky jde o sadu variant pro wake engine / ověření).
- **Nesmí** se opakovat starý vzorec: **nekonečné restartování Google STT** jen kvůli detekci slova — to na zařízeních spouští **systémové pípnutí** při každém `startListening` a je nepřijatelné z hlediska UX.

### 2. Po rozpoznání klíčového slova: poslech příkazu

- Po detekci slova aplikace **začne naslouchat** uživatelské větě.
- Indikace: **systémový** stav mikrofonu (např. zelená tečka / indikátor ochrany soukromí dle OEM) + **viditelná vrstva v aplikaci** (viz sekce UI).
- Pokud uživatel **nic neřekne do 5 s** po aktivaci, **poslech se ukončí** (žádné nekonečné čekání).

### 3. Konverzační inteligence (Gemini)

- Záměr a dialog mají zpracovat **Gemini Nano (AICore)**, pokud je na zařízení **dostupný a použitelný** pro daný typ úlohy.
- Pokud ne: použít **co nejefektivnější** cloudový model vyšší kvality než Nano pro daný krok — např. **Gemini Flash** (aktuální generace dle produktu, např. Flash 3), s ohledem na náklady a latenci.
- Hybrid (lokální vs cloud) musí být **transparentní pro UX** — uživatel dostává stejný typ odpovědi a potvrzení.

### 4. Složité požadavky s potvrzením (příklad SMS)

- Asistent musí zvládnout zadání typu: *„Napiš Tomovi zprávu (SMS), že přijedu zítra kolem osmé, a pošli ji.“*
- Postup má být **bezpečný**: před odesláním **nahlas přečte** navrhovaný obsah a zeptá se např. *„Můžu takto odeslat?“*
- Po **kladné odpovědi** („Ano“, „Jo“, „Jasně“ nebo jiný jednoznačně souhlasný pokyn) **odešle**.
- Negace / nejasnost → nepřijat odeslání nebo upřesnění (stavový dialog, ne jednorázová heuristika).

### 5. Ohlášení po probuzení

- Po aktivaci se Matěj **ohlásí** vlídným **TTS**, např. *„Ahoj, jak ti můžu pomoci?“* (přesný text lze ladit; lokalizace: minimálně čeština).

### 6. Zvuková otrava: žádné pípání v smyčce

- **Cíl:** systém **nepípá** při opakovaném „hledání“ klíčového slova; ideálně **žádné** zbytečné pípnutí — pokud půjde technicky a napříč OEM omezit.
- **Princip:** probuzení řešit **dedikovaným wake** (ne polling STT); příkazový poslech spouštět **omezeně** (jedna relace / stream na turn), ne restartovat `SpeechRecognizer` v krátkém intervalu kvůli wake.
- Historické **tlumení `STREAM_SYSTEM`** okolo STT bylo jen obvaz — u Matěje 2.0 preferovat **architekturu bez zdroje problému**.

---

## UX a rozhraní na displeji (povinné)

**Probudit Matěje = slyšet + vidět.** Čistě audio bez viditelného stavu nestačí (senioři, důvěra, orientace).

### Viditelná interakce

- Po probuzení musí být na displeji **jasná viditelná nápověda**, že Matěj naslouchá nebo pracuje:
  - **minimálně** prvkek v **rohu** (např. indikátor stavu), nebo
  - **overlay** přes obsah, pokud to produkt vyžaduje pro čitelnost.
- Vhodně sladit se **systémovým** indikátorem mikrofonu (OEM).

### Kompaktní režim při zásahu do systému

- Když asistent **spouští jiné funkce tabletu** (přechody obrazovek, psaní, odesílání, …), může se UI **shrout do pravého horního rohu** jako **malý box** s **ikonou Matěje** (později doplnit animaci stavu: naslouchám / zpracovávám / čekám na potvrzení).
- Cíl: **nepřekrývat** nutně celou plochu, ale **zůstat viditelný**, aby uživatel věděl, že asistent stále „žije“.

### Implementační poznámka (Android)

- V rámci **jedné aktivity / kiosku** typicky **Compose overlay** ve stromu UI (`HomeScreen` / overlay vrstva) — bez nutnosti systémového overlay oprávnění, pokud zůstáváme v aplikaci.
- Nad jinými aplikacemi by vyžadovalo jiné API / oprávnění — u cílového nasazení **Senior kiosk** často stačí **in-app** vrstva.

---

## Technické poznámky (neblokující detail)

- **Wake word + tvary:** engine často pracuje s natrénovaným vzorkem; adminem zadané tvary lze mapovat na více modelů nebo sekundární ověření — doladí se v implementační etapě.
- **Nástroje (tools):** SMS, volání a podobné akce přes **strukturované kroky** a **stav** (návrh → TTS → ano/ne) spíš než čisté regexy.
- **Bezpečnost a oprávnění:** stejná rodina jako u ruční komunikace (`RECORD_AUDIO`, SMS, kontakty, …) podle scénáře; foreground služba pro dlouhý poslech dle potřeby.

---

## Stav dokumentu

- **Produktová specifikace** pro implementaci Matěje 2.0; **kód zatím neukotvuje** — první implementační PR by měl odkazovat na tento soubor a případně ho upřesnit (datový model `config`, konkrétní knihovny wake/STT).
