# 2026-05-01 — Android Senior UI: historie hovorů a klidnější plochy

## Co se změnilo

- Přidáno čtení systémové historie hovorů přes `CallLog.Calls`.
- Senior režim žádá `READ_CALL_LOG`; UI oprávnění ukazuje chybějící historii hovorů vedle SMS/volání.
- `HomeUiState` drží `callHistory`, obnovované periodicky.
- Sekce **Volání** teď zobrazuje skutečné hovory:
  - všechny hovory při filtru **Všechny kontakty**,
  - jen hovory daného čísla po výběru kontaktu.
- Vizuál senior dashboardu je zklidněný:
  - menu používá nejtmavší plochu,
  - sloupec kontaktů je oddělený jednou vertikální linkou,
  - běžné položky nemají rámečky,
  - rámečky zůstávají hlavně u inputů a explicitních formulářových prvků.

## Test na referenčním tabletu

- Build prošel přes `./gradlew :app:assembleDebug`.
- APK nainstalováno přes ADB.
- `READ_CALL_LOG` bylo pro test povoleno přes:

```bash
adb shell pm grant com.seniorhub.os android.permission.READ_CALL_LOG
```
