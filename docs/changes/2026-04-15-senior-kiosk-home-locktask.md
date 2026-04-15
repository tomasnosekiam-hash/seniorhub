# 2026-04-15 — Kiosk (Senior): lock task, domovský launcher, singleTask

## Shrnutí

- **`Activity.startLockTask()`** po **spárování** při `onResume`; **`stopLockTask()`** před otevřením systémových nastavení po správném PINu; po návratu z nastavení se pin znovu zapne.
- **Manifest**: druhý `intent-filter` `MAIN` + `HOME` + `DEFAULT` — SeniorHub lze nastavit jako **výchozí domovskou aplikaci** (tlačítko Domů vrací do aplikace).
- **`launchMode="singleTask"`** na `MainActivity` (typické pro domovskou aktivitu).
- **Zpět** v režimu Senior **neukončuje** aplikaci (`OnBackPressedCallback`).
- **Hint v UI** (`kiosk_home_hint`): po spárování, pokud aplikace **není** výchozí domovská — odkaz na nastavení „Domovská aplikace“.
- **PIN overlay**: doplněný text, že po ověření se kiosk krátce uvolní a otevřou systémová nastavení.

## Omezení (záměr)

- **Plný COSU / Device Owner** (nepřetržité zamčení bez systémového dialogu, skrytí z nedávných apod.) zde **není** — vyžaduje provisioning / OEM. Na běžném tabletu platí systémové chování při prvním `startLockTask()` (může chtít potvrzení uživatele).

## Nasazení

- Jen nový **APK** (bez změn Firebase).

## Soubory (orientačně)

- `AndroidManifest.xml`, `MainActivity.kt`, `HomeScreen.kt`, `util/KioskMode.kt`, `values/strings.xml`
