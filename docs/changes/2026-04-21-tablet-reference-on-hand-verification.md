# 2026-04-21 — Referenční tablet k dispozici (ladění na zařízení)

## Stav

**Referenční zařízení Lenovo Idea Tab 11 5G** je u vývojáře — lze přejít z čistě repové práce na **ověřování na reálném hardware** (kiosk, Matěj, mikrofon, SIM/SMS podle konfigurace).

Tento soubor **neznamená dokončení** žádné nové implementace; fixuje **milník v provozu** a sladí dokumentaci s `docs/PROJECT_CONTEXT.md`.

## Doporučené ověření (pořadí orientační)

1. **Systém**: aktualizace OS a služeb Google; v nastavení kontrola **AI / AI Core** (pokud je relevantní pro budoucí Nano / AICore).
2. **Senior režim**: párování, PIN z cloudu, kontakty a nouzový kontakt.
3. **Kiosk**: výchozí domovská aplikace, lock task, návrat Domů do SeniorHubu (viz dřívější změny kiosku).
4. **Oprávnění**: mikrofon, telefon, SMS podle scénáře (tablet s/bez SIM).
5. **Matěj**: debug **„Test nouze · DBG“** (debug build) — řetězec nouze bez wake word; poté wake (Porcupine + `picovoice.access.key` / assets nebo STT).
6. **Dovednosti po probuzení**: počasí, vzkaz, SMS, hovor — chování dle `docs/changes/2026-04-20-matej-postwake-skills-natural-speech-plan.md`.
7. **Porcupine (volitelně)**: vlastní české `.ppn` / `.pv` v `assets/porcupine/` po vygenerování v Picovoice Console.

## Reference

- Kontext zařízení a AI strategie: `docs/PROJECT_CONTEXT.md` — sekce *Referenční zařízení a AI strategie*.
- Matěj dovednosti + plán NLU: `docs/changes/2026-04-20-matej-postwake-skills-natural-speech-plan.md`.
