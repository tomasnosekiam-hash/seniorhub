# 2026-05-23 10:15 — Android: RCS přes MMS provider (Telephony.Mms)

**Datum zápisu:** 2026-05-23 10:15  
**Rozsah:** `seniorhub-android` — dokončení příjmu RCS ze systémové schránky.

## Problém

Po první iteraci (sync jen `Telephony.Sms`) RCS z kontaktu v adresáři stále chyběly. Na tabletu Lenovo TB336ZU (Google Zprávy, ne výchozí SMS app SeniorHub) testovací RCS končily v **`content://mms`**, zatímco klasické SMS v `content://sms`.

## Řešení

| Část | Soubor |
|------|--------|
| SMS + MMS schránka | `util/InboundCellularInboxReader.kt` — `readRecentCellularInbox`, MMS text z `content://mms/part`, odesílatel z `content://mms/{id}/addr` |
| Dedup | `data/InboundCellularDedupStore.kt` — klíče `sms:{id}` / `mms:{id}` |
| Sync | `util/InboundCellularMessageSync.kt` — import obou kanálů |
| Observer | `HomeViewModel` — `ContentObserver` na `Telephony.Sms` i `Telephony.Mms` |
| Označení | `MvpRepository.recordInboundCellularSms(..., viaRcs)` — štítek „(příchozí RCS)“ vs SMS |

Odstraněno: `InboundSmsInboxReader.kt`, `InboundSmsDedupStore.kt` (nahrazeno sloučeným readerem).

## Ověření

1. `READ_SMS` povoleno v SeniorHub.
2. Odeslat RCS z čísla v kontaktech.
3. Po max. ~15 s nebo po otevření dashboardu: logcat `InboundCellularSync` — `imported mms:… rcs=true`.
4. Vlákno / „Co je nového“ — text s „(příchozí RCS)“.

## Omezení

Beze změny oproti [`2026-05-23-0959-android-rcs-inbox-sync.md`](2026-05-23-0959-android-rcs-inbox-sync.md): pokud Google Zprávy RCS nezapíše do Telephony provideru, sync nic nenajde.
