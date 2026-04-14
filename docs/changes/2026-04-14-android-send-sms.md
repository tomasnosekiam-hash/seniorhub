# 2026-04-14 – Android: odesílání SMS z kontaktů (Senior)

## Cíl

- Na tabletu u kontaktu s platným číslem umožnit **napsat a odeslat SMS** (systémový `SmsManager`, multipart pro delší text).
- **Oprávnění** `SEND_SMS` (manifest + runtime u prvního odeslání).

## Chování

- U každého kontaktu s číslem jsou tlačítka **Zavolat** a **SMS** (místo jednoho klikacího celého řádku).
- **SMS** otevře překryv s textovým polem, **Odeslat** / **Zrušit**; při chybě zobrazení hlášky v překryvu.
- **Čtení SMS** a sjednocené vlákno s Firestore zprávami zůstávají další iterace (viz `docs/PROJECT_CONTEXT.md`).

## Dotčené soubory

- `seniorhub-android/app/src/main/AndroidManifest.xml` – `SEND_SMS`.
- `seniorhub-android/.../util/SmsSender.kt` – `SmsManager`, `divideMessage` / multipart.
- `seniorhub-android/.../ui/HomeScreen.kt` – `SmsComposeOverlay`, `HomeRoute` (oprávnění), `ContactRow`.

## Poznámky

- Tablet **bez telefonního modulu** nebo bez SIM SMS neodešle — chyba se zobrazí z výjimky / systému.
- Na některých zařízeních může být potřeba **výchozí SMS aplikace** nebo oprávnění v nastavení — záleží na OEM.
