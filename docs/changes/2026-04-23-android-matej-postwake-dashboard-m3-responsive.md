# 2026-04-23 — Matěj: post-wake logika a pady; Senior dashboard (M3, responzivita)

## Kontext

Úpravy na **klientském tabletu (Senior)**: spolehlivější chování asistenta **Matěj** po probuzení a bezpečnější volání; přehlednější **domovský dashboard** s Material Design 3 a adaptivním rozložením.

## Matěj (`seniorhub-android`)

### Klasifikace po probuzení (`MatejPostWake.kt`)

- Prázdný přepis z druhého STT už **není mapován na nouzi** (`Emergency`).
- Nový výsledek **`Unclear`** — uživatelsky TTS „Nerozuměl jsem…“ (`matej_tts_unclear_command`), **bez vytočení** nouzové linky.
- Důvod: dříve prázdný přepis spouštěl nouzovou sekvenci → `ACTION_CALL`; bez runtime oprávnění **`CALL_PHONE`** mohlo dojít k **`SecurityException`** a pádu aplikace.

### Služba (`MatejForegroundService.kt`)

- Po probuzení **okno opakovaného poslechu** příkazu (`POST_WAKE_LISTEN_WINDOW_MS` ≈ 28 s, mezery mezi pokusy `POST_WAKE_RETRY_GAP_MS`), dokud nepřijde neprázdný text nebo nevyprší čas.
- Před voláním: kontrola **`CALL_PHONE`**; při zamítnutí nebo selhání **`startOutgoingCall`** TTS z `matej_tts_no_call_permission`.
- Unit test: `MatejPostWakeTest` — prázdný přepis očekává **`Unclear`**.

### Telefon (`PhoneDialer.kt`)

- `startOutgoingCall`: **`SecurityException`** zachycena, logována, návrat `false` (bez crash).

## Dashboard Senior (`HomeDashboard.kt`, téma)

### Téma (`ui/theme/`)

- **`Type.kt`**: `SeniorHubTypography` — větší řezy pro kiosk (hodiny `displayLarge`, nadpisy, tělo).
- **`Shape.kt`**: `SeniorHubShapes` (zaoblení M3).
- **`Theme.kt`**: rozšířený **dark** `ColorScheme` (surface kontejnery, tertiary pro hinty, chyby).

### UI a rozložení

- Kořen **`Surface`**; přechod načtení → obsah (`AnimatedContent`); chybová lišta (`AnimatedVisibility`).
- **Breakpoint `840.dp`** (`BoxWithConstraints`): pod touto šířkou **jeden sloupec** (`LazyColumn`: přehled, oddělovač, kontakty); od ní **dva sloupce** (38 % / 62 %) jako dříve.
- Sdílené bloky: **`LazyListScope.dashboardOverviewItems`**, **`contactListItems`** (kontakty se **postupným fade-in** přes `itemsIndexed` + `AnimatedVisibility` + krátké zpoždění podle indexu).
- Texty dashboardu přes **`strings.xml`** (sekce, kontakty, párování, baterie, chyba spojení).
- Preview **úzkého** layoutu: `HomeScreenNarrowPreview` (400×780).

### Poznámka k animaci položek

- API `animateItem` / `animateItemPlacement` z aktuálního Compose BOM v tomto prostředí nešlo stabilně naimportovat; místo toho zvolený **staggered fade** u kontaktů.

## Související soubory (výběr)

| Oblast | Soubory |
|--------|---------|
| Matěj | `matej/MatejPostWake.kt`, `matej/MatejForegroundService.kt`, `util/PhoneDialer.kt`, `res/values/strings.xml` |
| Test | `app/src/test/.../MatejPostWakeTest.kt` |
| Dashboard | `ui/HomeDashboard.kt`, `ui/theme/Theme.kt`, `Type.kt`, `Shape.kt`, `res/values/strings.xml` |

## Ověření

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --tests com.seniorhub.os.matej.MatejPostWakeTest`
