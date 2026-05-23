# 2026-05-23 12:00 — Android: kanál SMS/RCS u zpráv a chytrá odpověď

**Datum zápisu:** 2026-05-23 12:00  
**Rozsah:** `seniorhub-android`, `firebase/firestore.rules`

## Cíl

Šetřit náklady na placené SMS: odpovídat stejným kanálem jako příchozí zpráva; pokud jde o chat (RCS) a je Wi‑Fi nebo mobilní data, preferovat RCS místo SMS.

## Změny

| Oblast | Popis |
|--------|--------|
| Firestore | Volitelné pole `cellular_channel`: `sms` \| `rcs` u `sms_inbound` a `sms_cellular` |
| Příchozí | `recordInboundCellularSms(..., viaRcs)` ukládá kanál (ne jen text v `senderDisplayName`) |
| Odchozí | `CellularChannelResolver` + `CellularOutbound` — SMS přes `SmsManager`, RCS přes `RESPOND_VIA_MESSAGE` výchozí aplikace Zpráv; při selhání RCS fallback na SMS |
| Síť | `hasNetworkForRcs()` — Wi‑Fi / mobilní data / ethernet; bez sítě u RCS vlákna → SMS |
| UI | Dialog odpovědi: „Odeslat přes RCS/SMS“; vlákno zobrazuje kanál |

## Soubory

- `util/CellularChannel.kt`, `util/CellularOutbound.kt`
- `data/MvpRepository.kt`, `data/Models.kt`
- `ui/HomeScreen.kt`, `ui/dashboard/DashboardDialogOverlays.kt`

## Omezení

Plné RCS API pro třetí aplikace Android neposkytuje. Odeslání RCS spoléhá na **výchozí aplikaci SMS** (typicky Google Zprávy) a intent `RESPOND_VIA_MESSAGE`. Tablet musí mít Zprávy jako výchozí SMS; jinak se použije SMS. Kiosk (SeniorHub jako domovská app) s tím obvykle koliduje jen v roli výchozí SMS — domovská aplikace může zůstat SeniorHub.

## Ověření

1. Příchozí RCS → odpověď s popiskem „Odeslat přes RCS“ (Wi‑Fi zapnuté).
2. Příchozí SMS → „Odeslat přes SMS“.
3. Ve Firestore u nových zpráv `cellular_channel`.
