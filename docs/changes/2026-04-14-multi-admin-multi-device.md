# 2026-04-14 – Multi-admin + Multi-device (MVP2)

## Co bylo cílem
- Umožnit, aby **jedno zařízení** spravovalo **více admin účtů** a admin spravoval **více zařízení**.
- Uzamknout Firestore (žádné otevřené `allow read, write: if true`).

## Co bylo realizováno
- **Firestore schéma** přesunuto z `mvp_devices` na:
  - `devices/{deviceId}` + `contacts` subkolekce
  - join `deviceAdmins/{deviceId}_{uid}`
  - pairing `pairingClaims/{code}`
  - profil `users/{uid}`
- **Firestore rules** přepsána na model `devices + deviceAdmins + pairingClaims`.
- **Android**:
  - stabilní `deviceId` přes DataStore
  - anonymní Firebase Auth pro tablet
  - pairing overlay s kódem a expirací (možnost obnovit kód)
- **Web**:
  - Google Sign-In
  - „Moje tablety“ přes join kolekci
  - párování zadáním kódu z tabletu

## Poznámky / provoz
- Ve Firebase Console je potřeba zapnout **Authentication providers**:
  - `Google`
  - `Anonymous`
- Publikovat nové rules z `firebase/firestore.rules`.

## Dotčené soubory (high-level)
- Android: `seniorhub-android/app/src/main/java/com/seniorhub/os/**`
- Web: `seniorhub-web/src/main.ts`, `seniorhub-web/src/firebase.ts`, `seniorhub-web/src/constants.ts`
- Firebase: `firebase/firestore.rules`

