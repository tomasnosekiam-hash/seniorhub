# 2026-04-22 — Web párování (UX + idempotence), Firestore pravidla, Matěj STT, Firebase CLI

## Shrnutí

### Web (`seniorhub-web`)

- Sekce **„Moje tablety“** je **nad** blokem **„Přidat nový tablet“** — nejdřív viditelný seznam již napojených zařízení.
- **Žlutý box** vysvětluje, že párovací kód je jen při **prvním** napojení účtu k tabletu; opakované párování stejného zařízení neřešit.
- Před zápisem do Firestore se ověří **`deviceAdmins/{deviceId}_{uid}`**: pokud vazba už existuje, zobrazí se srozumitelná zpráva (bez planého `PERMISSION_DENIED` při druhém pokusu o párování).
- **Atomický `writeBatch`**: `pairingClaims` (usedAt/usedByUid) + `deviceAdmins` + **`devices` (`paired`, `pairedAt`)** v jednom commit — pravidla vyhodnotí spárování konzistentně (včetně role viewer u zápisu `paired`).
- Vylepšené texty chyb (použitý kód, permission) a **styly stavu** (`pair-status-info` / `ok` / `err`) v `style.css`.

### Firestore (`firebase/firestore.rules`)

- **`viewerCompletingPairing`**: viewer smí na `devices/{deviceId}` měnit **jen** `paired` a `pairedAt` při dokončení párování (dříve selhával zápis `paired` po batchi, protože `canMutateAdminFields` pro viewer je false).
- **`pairingClaims` update**: místo striktní kontroly `usedAt != null` u server timestamp — **`request.resource.data.diff(resource.data).affectedKeys().hasOnly(['usedAt','usedByUid'])`** + stávající invarianty (`code`, `deviceId`, `expiresAt`, …).

### Android — Matěj (`MatejForegroundService`)

- Při wake word přes **Google STT** (fallback bez Porcupine): **delší interval** mezi pokusy (základ ~8 s, backoff až ~28 s) kvůli systémovému „pípání“ při opakovaném `startListening`.
- **`suppressSttStartBeep`**: krátké ztlumení **`AudioManager.STREAM_SYSTEM`** okolo `startListening` (odmutování zpožděné), použito i u `listenForCommandOnce`.
- Po úspěšném probuzení se interval STT resetuje.

### Provoz — Firebase CLI

- Pokud `firebase deploy` hlásí projekt doslova jako řetězec příkazu nebo 403 u Rules API: v **`~/.config/configstore/firebase-tools.json`** může být u cesty k repu špatný **`activeProjects`** — oprava: `firebase use seniorhub-716f0` (nebo smazat/opravit záznam). Soubor `.firebaserc` v repu má správný default; CLI lokální cache může přepsat kontext složky.

## Nasazení

- `firebase deploy --only firestore:rules` (projekt `seniorhub-716f0` po `firebase use …`).
- Web: přegenerovat / nasadit frontend z aktuálního `main.ts` + CSS.

## Soubory (orientačně)

- `seniorhub-web/src/main.ts`, `seniorhub-web/src/style.css`
- `firebase/firestore.rules`
- `seniorhub-android/.../matej/MatejForegroundService.kt`
