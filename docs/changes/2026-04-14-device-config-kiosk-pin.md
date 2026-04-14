# 2026-04-14 – `config` dokument (PIN, SIM) + kiosk break na tabletu

## Cíl (Fáze A – body 1–2 z PROJECT_CONTEXT)

- Zavést **`devices/{deviceId}/config/main`** ve Firestore s poli sladěnými se spec (`admin_pin`, `sim_number`, `assistant_name`).
- Umožnit **správci na webu** číst a měnit PIN / SIM / jméno asistenta.
- Na **tabletě**: při bootstrapu vygenerovat **4místný PIN**, pokud ještě neexistuje platný PIN v cloudu; **5 rychlých klepnutí** do levého horního rohu (100×100 dp) otevře zadání PINu; po shodě s cloudem se otevře **systémová nastavení** Androidu (základ kiosk break dle `oldies_project.md` krok 3).

## Co je v produktu (chování)

- **Firestore:** dokument `devices/{deviceId}/config/main` (ID `main` v subkolekci `config`).
- **Web (`seniorhub-web`):** v kartě nastavení vybraného tabletu sekce **Provoz tabletu (PIN, SIM)** — pole Admin PIN (4 číslice), číslo SIM, jméno asistenta (výchozí „Matěj“); realtime načítání z Firestore; tlačítko **Uložit PIN a SIM** (validace 4 číslic).
- **Android (tablet):** po startu aplikace doplnění `config`, pokud chybí platný 4místný PIN; poslech `config` pro ověření PINu; skrytá zóna vlevo nahoře → overlay s číselníkem → při shodě spuštění `Settings.ACTION_SETTINGS` (východ do systémových nastavení).

## Dotčené soubory

- `firebase/firestore.rules` – pravidla pro subkolekci `config/{configDocId}` (`canAccessDevice`).
- `seniorhub-android/.../data/Models.kt` – `DeviceConfig`.
- `seniorhub-android/.../data/MvpRepository.kt` – `ensureDeviceConfig`, `observeDeviceConfig`, konstanty cesty dokumentu (`SUB_CONFIG`, `CONFIG_DOC_ID = main`).
- `seniorhub-android/.../ui/HomeViewModel.kt` – kombinace `observeDevice` + kontakty + `observeDeviceConfig`; stav odemčení; `onKioskSecretTap` / `tryUnlockWithPin` / `dismissKioskUnlock`.
- `seniorhub-android/.../ui/HomeScreen.kt` – `HomeRoute` s `LocalContext` pro intent nastavení; skrytá zóna; `KioskUnlockOverlay`.
- `seniorhub-web/src/constants.ts` – `SUB_CONFIG`, `CONFIG_DOC_ID`, klíče `admin_pin`, `sim_number`, `assistant_name`.
- `seniorhub-web/src/main.ts` – sekce v UI, `onSnapshot` na `config/main`, handler `saveConfig`.

## Repo (mimo aplikaci, stejný den)

- Kořen projektu inicializován jako **git** monorepo, první commit; remote **GitHub** `tomasnosekiam-hash/seniorhub` (viz historie gitu — není součástí runtime aplikace).

## Poznámky

- PIN je v MVP uložen v **plaintextu** ve Firestore (rodinný správce); produkční tvrdší model je mimo tuto vlnu.
- Rules pro `config` musí být **publikované** v Firebase konzoli (už oznámeno jako hotové).

## Navazuje (nebylo v této vlně)

Podle `docs/PROJECT_CONTEXT.md` Fáze A dál: subkolekce **`messages`**, pravidla, **FCM** na tabletu, zápis tokenu, **Cloud Functions** pro push při vzkazu, případně profil seniora nad rámec `config`.
