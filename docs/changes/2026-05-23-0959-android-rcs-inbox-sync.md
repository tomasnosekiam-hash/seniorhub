# 2026-05-23 09:59 — Android: příjem RCS přes sync systémové schránky

**Datum zápisu:** 2026-05-23 09:59  
**Rozsah:** `seniorhub-android` — příchozí mobilní zprávy (SMS + RCS, kde je systém zpřístupní schránku).

## Problém

Testovací zpráva na tablet dorazila jako **RCS** (Google Zprávy / Chat). Aplikace ji nezobrazila ve vlákně ani na dashboardu, protože zachytávala jen broadcast **`SMS_RECEIVED`** — u RCS často **nepřijde**.

## Řešení

Doplňková cesta vedle `IncomingSmsReceiver`:

| Část | Soubor / místo |
|------|----------------|
| Čtení schránky | `util/InboundSmsInboxReader.kt` — `Telephony.Sms.Inbox`, `READ_SMS` |
| Sync do Firestore | `util/InboundCellularMessageSync.kt` — `sms_inbound` u známého kontaktu |
| Deduplikace | `data/InboundSmsDedupStore.kt` — telefonní `_id` zprávy |
| Periodický běh | `HomeViewModel` — cca každých **15 s** |
| Po klasické SMS | `IncomingSmsReceiver` — po PDU zavolá sync schránky |

### Oprávnění

- Manifest: **`READ_SMS`**
- `CommunicationPermissions.readSmsGranted`, banner „čtení schránky (RCS)“
- `MainActivity` + `communicationPermissionArray` — žádost při startu Senior režimu

### Datový model

Beze změny schématu Firestore: `delivery: sms_inbound`, `inbound_from_phone`, `inbound_from_name`. Web a dashboard používají stejné vlákno jako u klasické SMS.

## Omezení (produkt)

- RCS musí být **zapsaná do systémového Telephony provideru**. Pokud ji drží jen Google Zprávy interně, **třetí app ji neuvidí**.
- Spolehlivá náhrada: **vzkaz z webu / Firestore** (FCM).
- Plná parita s RCS jako u výchozí SMS aplikace = role **výchozí SMS handler** (zatím mimo scope).

## Ověření

- `./gradlew :app:installDebug` na referenční tablet (Lenovo TB336ZU).
- Odesílatel v **kontaktech** tabletu; povolená **SMS** včetně čtení schránky.
- Po restartu SeniorHub — nová zpráva do cca 15 s ve **Domů** / vlákně.

## Dokumentace

- `docs/PROJECT_CONTEXT.md` — sekce **Příchozí SMS a RCS (tablet Senior)**.
