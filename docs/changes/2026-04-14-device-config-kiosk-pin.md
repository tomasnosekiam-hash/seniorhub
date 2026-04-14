# 2026-04-14 – `config` dokument (PIN, SIM) + kiosk break na tabletu

## Cíl (Fáze A – body 1–2 z PROJECT_CONTEXT)

- Zavést **`devices/{deviceId}/config/main`** ve Firestore s poli sladěnými se spec (`admin_pin`, `sim_number`, `assistant_name`).
- Umožnit **správci na webu** číst a měnit PIN / SIM / jméno asistenta.
- Na **tabletě**: při bootstrapu vygenerovat **4místný PIN**, pokud ještě neexistuje platný PIN v cloudu; **5 rychlých klepnutí** do levého horního rohu (100×100 dp) otevře zadání PINu; po shodě s cloudem se otevře **systémová nastavení** Androidu (základ kiosk break dle `oldies_project.md` krok 3).

## Dotčené soubory

- `firebase/firestore.rules` – pravidla pro subkolekci `config/{configDocId}` (`canAccessDevice`).
- `seniorhub-android/.../data/Models.kt` – `DeviceConfig`.
- `seniorhub-android/.../data/MvpRepository.kt` – `ensureDeviceConfig`, `observeDeviceConfig`, konstanty cesty dokumentu.
- `seniorhub-android/.../ui/HomeViewModel.kt` – kombinace toků + stav odemčení + ověření PINu.
- `seniorhub-android/.../ui/HomeScreen.kt` – skrytá zóna, overlay PINu, otevření `Settings.ACTION_SETTINGS`.
- `seniorhub-web/src/constants.ts`, `seniorhub-web/src/main.ts` – sekce „Provoz tabletu“, realtime sync `config/main`.

## Poznámky

- PIN je v MVP uložen v **plaintextu** ve Firestore (rodinný správce); produkční tvrdší model je mimo tuto vlnu.
- Po nasazení rules je potřeba **publikovat** aktualizované `firebase/firestore.rules` v konzoli.
