# 2026-04-14 – Firestore: PERMISSION_DENIED na tabletu (pravidla + rebind)

## Příčina

- Tablet používá **Anonymous Auth**; v `devices/{deviceId}` je pole **`deviceAuthUid`**.
- Pravidla povolovala čtení/zápis jen při **`deviceAuthUid == request.auth.uid`** (`canAccessDevice`).
- Po **přeinstalaci aplikace / vymazání dat** dostane tablet **nové anonymní uid**, ale ve Firestore zůstane **staré** `deviceAuthUid` → žádný `get`/`listen` na `devices/...` ani podkolekce neprojde → **`PERMISSION_DENIED`**.
- Stejně tak **první čtení neexistujícího** dokumentu zařízení vyžaduje v pravidlech explicitně povolit `resource == null`, jinak `get()` před prvním `set` může selhat.

## Úprava pravidel

- **`devices/{deviceId}` read**: `canAccessDevice` **nebo** neexistující dokument **nebo** přihlášený uživatel a shoda / doplnění pole `deviceId` s cestou (včetně starých dokumentů bez pole).
- **`devices/{deviceId}` update**: `canAccessDevice` **nebo** rebind — v těle musí být `deviceId` shodné s cestou a `deviceAuthUid` s aktuálním uid (tablet si znovu „nárokuje“ dokument).
- **`create`**: doplněna kontrola `request.resource.data.deviceId == deviceId`.

## Nasazení

- `firebase deploy --only firestore:rules` (projekt `seniorhub-716f0`) — hotové v tomto kroku.

## Co udělat na tabletu

- Po nasazení pravidel **restart aplikace**; `bootstrapDevice()` provede `set(merge)` a přepíše `deviceAuthUid` na aktuální anonymní účet.
