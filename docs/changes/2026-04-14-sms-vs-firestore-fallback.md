# 2026-04-14 – SMS primárně; bez SIM náhrada přes Firestore + popis technologie

## Cíl

- **Primárně** klasická **SMS přes mobilní síť** ([`SmsManager`](https://developer.android.com/reference/android/telephony/SmsManager)), pokud má zařízení telefonii a připravenou SIM (`TelephonyManager.SIM_STATE_READY`).
- **Tablet bez SIM / bez mobilní SMS**: stejná obrazovka s textem, ale odeslání jako záznam do **`devices/{id}/messages`** s `delivery: tablet_firestore`, `outbound_phone`, `outbound_name` — rodina to vidí ve **webové administraci** (Firebase Firestore), nikoli jako SMS na číslo příjemce.
- V UI tabletu je u každého režimu uvedeno, **jakou technologií** se zpráva posílá.
- **Cloud Function** `notifyTabletOnNewMessage`: u zprávy vytvořené **tabletem** (`senderUid == deviceAuthUid`) **neposílat** FCM na stejný tablet (zbytečný ping).

## Firestore rules

- Vytváření vzkazu: **správce** jako dosud (bez pole `delivery`), nebo **tablet** (`isDeviceSelf`) s `delivery == 'tablet_firestore'` a povinným `outbound_phone`.

## Dotčené soubory

- `seniorhub-android/.../CellularSmsCapability.kt`, `HomeScreen.kt`, `HomeViewModel.kt`, `MvpRepository.kt`
- `firebase/firestore.rules`, `firebase/functions/src/index.ts`
- `seniorhub-web/src/constants.ts`, `main.ts`, `style.css`

## Nasazení

- `firebase deploy --only firestore:rules,functions` (Functions kvůli úpravě FCM).
