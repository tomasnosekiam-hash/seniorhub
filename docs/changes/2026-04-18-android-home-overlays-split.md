# 2026-04-18 — Android: overlaye z `HomeScreen` do `HomeOverlays`

## Shrnutí

- Nový soubor **`ui/HomeOverlays.kt`**: `ContactThreadOverlay`, `SmsComposeOverlay`, `MessageOverlay`, `AlertOverlay`, `KioskUnlockOverlay`, `PairingOverlay` (viditelnost **`internal`**, stejný balíček).
- **`HomeScreen.kt`**: zůstávají `HomeRoute`, `HomeScreen`, `StatusColumn`, `ContactsColumn`, `ContactRow`, `KioskPinningEffect`, `PairingSummaryCard` + preview; odstraněné importy jen pro přesunuté composables.
- Chování UI beze změny; ověřit na tabletu: vlákno kontaktu, SMS overlay, vzkaz přes overlay, PIN kiosku, párovací overlay.

## Soubory

- `seniorhub-android/.../ui/HomeScreen.kt` (~805 řádků po úpravě)
- `seniorhub-android/.../ui/HomeOverlays.kt` (nový)

## Další krok (plán)

- Viz `docs/changes/2026-04-18-home-screen-refactor-plan.md` — dashboard (`HomeScreen` + sloupce) nebo `HomeRoute` do vlastního souboru.
