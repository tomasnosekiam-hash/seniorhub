# 2026-04-26 — Android: první milník Matěje 2.0 (viditelné UI)

## Shrnutí

- Nový modul UI: `MatejAssistantUi.kt` — stavy `MatejPhase` (Greeting, Listening, Processing), `MatejUiSession` (včetně `compact`).
- **Rozšířená karta** vlevo nahoře (`TopStart`): jméno, podtitul pod fází, při naslouchání pulzující tečka + ikona mikrofonu.
- **Kompaktní box** vpravo nahoře (`TopEnd`): ikona asistenta, při Listening pulz, zavření.
- **HomeRoute** (`HomeScreen.kt`): stav `matejSession`; při otevřeném vlákně zpráv nebo SMS overlay se zobrazení **vynutí kompaktní** (`copy(compact = true)`), přičemž Matěj je vykreslen **nad** těmito overlayi (pořadí v `Box`).
- **Debug build**: FAB „M“ cykluje náhled stavů (bez wake); v release se nezobrazuje.

Produktová spec: `docs/changes/2026-04-25-matej-2-product-spec.md`.

## Soubory

- `seniorhub-android/.../MatejAssistantUi.kt` (nový)
- `seniorhub-android/.../HomeScreen.kt` (`HomeRoute`)
- `seniorhub-android/app/src/main/res/values/strings.xml` (řetězce Matěje)
