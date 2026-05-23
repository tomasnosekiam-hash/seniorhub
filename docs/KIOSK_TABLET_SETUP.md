# Kiosk tablet — provozní nastavení (Senior)

SeniorHub dnes používá **měkký kiosk** (`startLockTask` + výchozí domovská aplikace). Plné vypnutí systémového zámku a cizích notifikací vyžaduje **Device Owner** (viz níže).

## Android 16 (Lenovo TB336ZU a podobné)

Plné skrytí horní lišty vyžaduje Device Owner (viz níže). Pro běžné použití tabletu i mimo SeniorHub **Device Owner neprovádějte** — stačí měkký režim (domovská aplikace + volitelné připnutí).

Po **restartu tabletu** je potřeba znovu zadat **PIN SIM karty** — SeniorHub na to upozorní a otevře nastavení SIM. Bez odemknutí nefungují volání, SMS ani mobilní data.

## Dva různé PINy

| PIN | Kde | Účel |
|-----|-----|------|
| **PIN tabletu / zámek obrazovky** | Nastavení Androidu → Zabezpečení | Odemykání zařízení po zhasnutí displeje |
| **Admin PIN (4 číslice)** | Firestore `config/main`, 5× klepnutí v rohu v SeniorHub | Dočasné uvolnění kiosku → systémová nastavení |

Pro seniora **nesmí** být nutné zadávat systémový PIN při každém probuzení. Doporučení pro provozní tablet:

1. V Androidu **vypnout zámek obrazovky** (Žádný / Swipe), nebo použít jen biometrii bez PINu u správce při servisu.
2. Ochrana nastavení pouze přes **admin PIN v aplikaci** (skryté gesto).

## Checklist před předáním tabletu

- [ ] SeniorHub = **výchozí domovská aplikace** (Domovská aplikace).
- [ ] Tablet **spárovaný** (lock task se zapíná po párování).
- [ ] Při prvním `startLockTask` potvrdit systémový dialog „Připnout“ (jednorázově).
- [ ] **Zámek obrazovky vypnutý** nebo nastavený tak, aby senior po probuzení šel rovnou do SeniorHubu.
- [ ] V **Nastavení → Aplikace → Zprávy / Google** zvážit vypnutí oznámení na lock screen, nebo nasadit Device Owner (viz dole).
- [ ] SeniorHub má oprávnění k hovorům/SMS dle scénáře.

## Notifikace

- Klepnutí na **notifikaci SeniorHub** (vzkaz z rodiny) má otevřít aplikaci uvnitř kiosku.
- Notifikace **Google Zprávy**, e-mail, systém atd. mohou uživatele vyvést z kiosku — bez Device Owner je nelze spolehlivě zablokovat jen z APK.

## Device Owner (plný kiosk — doporučeno pro Android 16)

Provisioning (vývoj / tovární nastavení, **bez Google účtu na zařízení**):

1. **Tovární nastavení** tabletu (Nastavení → Systém → Obnovení).
2. Průvodce nastavením **nedokončovat** přidáním Google účtu (nebo účet hned odstranit).
3. Zapnout **Vývojářské možnosti** a **USB debugging**.
4. Nainstalovat APK a spustit SeniorHub jednou (výběr role Senior).
5. Na PC:

```bash
adb shell dpm set-device-owner com.seniorhub.os/.kiosk.KioskDeviceAdminReceiver
```

6. Restart aplikace — SeniorHub automaticky aktivuje Device Owner kiosk (skrytá lišta, lock task).

**Proč to na běžném tabletu nejde:** `set-device-owner` selže s „already some accounts on the device“. Sekundární uživatel na Androidu 16 také nepomůže („already several users“). Musí být **jediný uživatel, bez účtů**.

Po úspěchu aplikace automaticky:

- skryje status lištu (`setStatusBarDisabled`),
- spustí lock task bez nutnosti screen pin dialogu,
- omezí lock task features (žádné systémové gesta v pin režimu).

Bez Device Owner platí limity běžného tabletu z Google Play — lištu a notifikace nelze spolehlivě vypnout jen z APK.
