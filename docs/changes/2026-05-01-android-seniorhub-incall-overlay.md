# 2026-05-01 — Android: SeniorHub jako in-call UI pro kiosk hovory

## Co se změnilo

- Přidán minimální vlastní `InCallService` pro GSM hovory:
  - `calls/SeniorHubInCallService.kt`
  - `calls/SeniorHubCallManager.kt`
  - `calls/SeniorHubCallOverlayActivity.kt`
- SeniorHub se v Senior režimu umí požádat o roli výchozí telefonní aplikace (`RoleManager.ROLE_DIALER`).
- Manifest doplněn o povinné `ACTION_DIAL` intent filtry a `android.telecom.InCallService` metadata.
- Při hovoru se otevírá SeniorHub dialogové okno nad aplikací s:
  - stavem hovoru,
  - číslem,
  - přepínačem reproduktoru,
  - tlačítkem ukončit.

## Důležitý limit

Android použije vlastní SeniorHub in-call dialog jen tehdy, když je aplikace držitelem role výchozí telefonní aplikace. Jinak systém dál otevře výchozí Google/Android Telefon UI.

Na referenčním tabletu bylo po instalaci ověřeno:

```bash
adb shell cmd role get-role-holders android.app.role.DIALER
# com.seniorhub.os
```
