# 2026-04-16 — Příchozí SMS do vlákna, deploy FCM, role viewer, web párování

## Shrnutí

- **Příchozí klasická SMS** (režim Senior): `BroadcastReceiver` + `RECEIVE_SMS`; pokud číslo odpovídá kontaktu (normalizace jako u hovorů), zápis do `devices/.../messages` s `delivery: sms_inbound`, `inbound_from_phone` / `inbound_from_name`. Vlákno u kontaktu zahrnuje odchozí i příchozí; náhled vzkazů „od rodiny“ jen u zpráv bez `delivery` (čistě web).
- **Firestore rules**: rozšíření create pro `sms_inbound`; funkce `isDeviceViewer` / `canMutateAdminFields` — **viewer** nesmí zapisovat `contacts` ani `config`; zapisovat `devices/{id}` může tablet nebo ne-viewer správce (rebind tabletu beze změny).
- **Cloud Functions** (`notifyTabletOnNewMessage`): u zpráv vytvořených tabletem (`senderUid` = `deviceAuthUid`) se neposílá FCM na tablet (jako dřív) a **navíc se neposílají kopie správcům** (méně šumu u zrcadel SMS / odchozích z tabletu).
- **Deploy**: `firestore:rules` + `functions` na projekt `seniorhub-716f0` (CLI s explicitním `--project …` kvůli spolehlivému kontextu).
- **Web**: při párování výběr role **správce** (`admin`) vs **člen rodiny** (`viewer`); u zařízení s `viewer` skryté nastavení a kontakty, viditelné vzkazy + nápověda.

## Nasazení

- `npx firebase-tools@latest deploy --only firestore:rules,functions --project <PROJECT_ID>`

## Soubory (orientačně)

- `firebase/firestore.rules`, `firebase/functions/src/index.ts`
- `seniorhub-android/.../IncomingSmsReceiver.kt`, `AndroidManifest.xml`, `MainActivity.kt`, `MvpRepository.kt`, `MessageThreads.kt`, `HomeScreen.kt`, `HomeViewModel.kt`, `Models.kt`, `AdminRepository.kt`, `AdminRoute.kt`
- `seniorhub-web/src/main.ts`, `seniorhub-web/src/constants.ts`, `seniorhub-web/src/style.css`
