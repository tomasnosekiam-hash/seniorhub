# 2026-04-20 — Matěj: dovednosti po probuzení, drobné UX STT, plán vrstvy přirozené řeči

## Shrnutí implementovaného (toto vlákno vývoje)

Po probuzení (Porcupine nebo STT na jméno asistenta) následuje **druhé krátké naslouchání** (české STT) a **klasifikace příkazu** v `MatejPostWake.kt` (heuristiky nad normalizovaným textem — ne plné NLU).

### Co je v kódu

| Oblast | Chování |
|--------|---------|
| **Počasí** | Open-Meteo (`OpenMeteoWeather`), stejný zdroj jako dashboard; TTS odpovědi. |
| **Čtení vzkazu od rodiny** | `MvpRepository.fetchFirstUnreadFamilyMessage()` — nepřečtený záznam bez `delivery`; TTS; po přečtení `markMessageRead`. Sdílené mapování dokumentů: `deviceMessageFromDoc` + refaktor `observeMessages`. |
| **SMS (diktát)** | Klíčová slova typu SMS + rozpoznané jméno kontaktu → **druhé STT** s textem zprávy; odeslání jako na dashboardu: cellular + `recordOutboundCellularSms` při oprávnění a SIM, jinak `sendTabletFirestoreMessage`. |
| **Hovor** | Klíčová slova (*zavolej*, *vytoč*, …) + kontakt; nebo **jen jméno kontaktu** (vytočení jako dřív). Nouze zůstává výchozí větev při nejasném přepisu. |
| **Nouze** | `pickEmergencyDialTarget`, `incidents`, FCM — beze změny logiky oproti dřívější nouzové větvi. |
| **Debug** | `ACTION_TEST_EMERGENCY` / „Test nouze · DBG“ — přímo nouzová sekvence, bez druhého příkazu. |
| **STT** | Společná factory `csRecognizeSpeechIntent` (cs-CZ); u diktátu SMS volitelný **`EXTRA_PROMPT`** („Řekněte text zprávy“). |

### Omezení současného přístupu

- Klasifikace je **seznam frází a podřetězců** — uživatel nemusí znát manuál, ale **nejde o volnou konverzaci**; jde o rychlý MVP bez modelu záměru.
- **Nano / AICore** zatím **není** napojen na intent; závislost na tabletu a dostupnosti AI Core se ověří až na referenčním zařízení.

---

## Plán: vrstva „přirozené řeči“ (intent / NLU)

**Cíl produktu:** co nejpřirozenější řeč seniora; **technické řešení se přizpůsobí UX**, ne naopak.

### Princip

1. **Vstup** zůstává stejný: po wake jeden (nebo více) **přepisů** z STT, případně později audio do modelu.
2. **Nová vrstva** mapuje přepis → **strukturovaný záměr** (enum / JSON): např. `weather`, `read_family_message`, `sms`, `dial`, `emergency`, entity `contactId` nebo jméno, text zprávy atd.
3. **Deterministické větve** (zejména **nouze**) zůstávají **predikovatelné** a testovatelné; NLU je může navrhnout, ale kritické chování musí mít **fallback** (pravidla / timeout → nouze).

### Varianty technické realizace (kombinovatelné)

| Vrstva | Kdy | Poznámka |
|--------|-----|----------|
| **Heuristiky (současný stav)** | Offline, vždy | Záloha při výpadku sítě/modelu; jednoduchá údržba seznamů. |
| **Gemini Nano přes AICore** | Po ověření na tabletu | Lokální soukromí, nízká latence u základní komunikace — **ověřit na Lenovo Idea Tab 11 5G** (systém, aktualizace, dostupnost AI). |
| **Cloud (např. Gemini Flash)** | Když lokální model chybí nebo nestačí | Lepší čeština a parafraze; řeší síť, náklady, zásady ochrany dat — sladit s produktem. |

### Doporučený postup implementace (bez závislosti na pořadí)

1. Definovat **jednotný výstup intentu** (data class / JSON schema) sdílený mezi pravidly a modely.
2. **Krátký spike** na jedné funkci (např. jen rozlišení počasí vs. ostatní) přes zvolený model vs. pravidla — měření latence a přesnosti.
3. Po příchodu tabletu: **kontrola AICore**; rozhodnutí, co poběží lokálně vs. v cloudu.
4. Ponechat **heuristiky jako fallback** pro offline a bezpečnou nouzi.

### Související soubory (Android)

- `matej/MatejForegroundService.kt` — orchestrace wake → STT → akce.
- `matej/MatejPostWake.kt` — klasifikace (nahraditelná voláním NLU).
- `data/MvpRepository.kt` — zprávy, SMS zrcadlo.
- `data/OpenMeteoWeather.kt` — počasí.

### Testy (bez zařízení)

- JVM unit testy: `app/src/test/java/com/seniorhub/os/matej/MatejPostWakeTest.kt` — `classifyPostWakeCommand` a `findContactByTranscript` (prázdný vstup, počasí, vzkaz, SMS, hovor, nouze, delší jméno kontaktu).
- Spuštění: `./gradlew :app:testDebugUnitTest --tests "com.seniorhub.os.matej.MatejPostWakeTest"`.

---

## Reference

- Předchozí etapa Matěj (základ, Porcupine, incidenty): `2026-04-16-android-matej-incidents-porcupine-fcm.md`.
- Home refaktor + debug nouze: `2026-04-19-android-home-dashboard-matej-debug-test.md`.
- AI strategie v repu: `docs/PROJECT_CONTEXT.md` — sekce *Referenční zařízení a AI strategie*.
