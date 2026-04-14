# 2026-04-14 – Stav tabletu: baterie a heartbeat (Firestore + web)

## Cíl

- Základ **provozního přehledu** pro správce: tablet pravidelně zapisuje do `devices/{deviceId}` **baterii**, **nabíjení** a **`lastHeartbeatAt`** (vedle existujícího `lastSeenAt`).
- **Web**: v přehledu tabletů a v podnadpisu vybraného zařízení zobrazit baterii / čas posledního kontaktu.
- **Android (Senior)**: stejné údaje v levém sloupci (baterie), perioda zápisu cca **3 min**.

## Pole na `devices/{deviceId}`

| Pole | Typ | Poznámka |
|------|-----|----------|
| `batteryPercent` | number (0–100) | volitelné, pokud systém vrátí úroveň |
| `charging` | boolean | |
| `lastHeartbeatAt` | timestamp | server time při každém pulzu |

**Firestore rules** beze změny — tablet už může mergovat vlastní dokument zařízení.

## Dotčené soubory

- `seniorhub-android/.../util/BatteryStatus.kt` — čtení úrovně a nabíjení (sticky `ACTION_BATTERY_CHANGED`).
- `seniorhub-android/.../data/MvpRepository.kt` — `postDeviceHeartbeat`, mapování v `observeDevice`.
- `seniorhub-android/.../data/Models.kt` — rozšíření `DeviceSettings`.
- `seniorhub-android/.../ui/HomeViewModel.kt` — `AndroidViewModel`, smyčka heartbeatu.
- `seniorhub-android/.../ui/HomeScreen.kt` — zobrazení baterie.
- `seniorhub-android/.../MainActivity.kt` — factory pro `HomeViewModel`.
- `seniorhub-web/src/constants.ts`, `main.ts` — klíče a UI.

## Poznámky

- Samostatná subkolekce `devices/.../status` zatím není — data jsou na kořenovém dokumentu zařízení kvůli jednoduchosti a stávajícím pravidlům.
- Build Androidu lokálně: JDK **17 nebo 21** (viz jiné záznamy) — JDK 25 může Gradle rozbít.
