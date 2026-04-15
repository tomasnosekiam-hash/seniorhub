# Plán — refaktoring `HomeScreen.kt` (brzy)

## Kontext

- Soubor `seniorhub-android/.../ui/HomeScreen.kt` má **~1,3k řádků** — obsahuje vstupní route, hlavní dashboard i většinu overlayů a vedlejších `@Composable` funkcí.
- Cíl: **čitelnost**, snadnější změny po sekcích, menší konflikty v gitu — **bez změny chování** (stejné API pro `HomeRoute` / navigaci).

## Navrhované soubory (iterativně)

| Krok | Soubor (návrh) | Obsah |
|------|----------------|--------|
| 1 | `HomeRoute.kt` nebo ponechat v `HomeScreen.kt` nahoře | Jen `HomeRoute` — permission launchery, `MatejForegroundService.sync`, overlay stack (`ContactThreadOverlay`, `SmsComposeOverlay`). *Nejjednodušší první řez.* |
| 2 | `HomeDashboard.kt` | **Hotovo** — `HomeScreen`, `StatusColumn`, `ContactsColumn`, `ContactRow`, `PairingSummaryCard` + preview (`docs/changes/2026-04-19-android-home-dashboard-matej-debug-test.md`). |
| 3 | `HomeOverlays.kt` | **Hotovo** (`docs/changes/2026-04-18-android-home-overlays-split.md`) — `ContactThreadOverlay`, `SmsComposeOverlay`, `MessageOverlay`, `AlertOverlay`, `KioskUnlockOverlay`, `PairingOverlay`. |
| 4 | `KioskHomeEffects.kt` (volitelně) | `KioskPinningEffect` — životní cyklus + `KioskMode`, aby nepletl overlay logiku. |

Pořadí lze upravit: někdo začne **overlayi** (izolované, málo stavu z route), jiný **sloupce** (vizuálně jasné).

## Pravidla při přesunu

- Zachovat **`internal` / `private`** podle viditelnosti; veřejné zůstávají **`HomeRoute`** a **`HomeScreen`** (pokud je volá navigace / preview).
- Importy sladit s existujícím stylem (`com.seniorhub.os.ui.theme`, data modely z `data/`).
- Po každém kroku: **`./gradlew :app:compileDebugKotlin`** a **smoke test na tabletu** (kiosk, kontakt, vlákno, SMS, vzkaz).

## Související údržba (hotovo)

- Cloud Functions závislosti a deploy — viz `docs/changes/2026-04-17-fcm-emergency-channel-functions-maint.md` (`firebase-functions` v7, Node 22).
