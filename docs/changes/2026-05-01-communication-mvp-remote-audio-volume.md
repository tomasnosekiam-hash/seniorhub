# 2026-05-01 — Komunikační MVP: vzdálená hlasitost oznámení a hovorů

Navazuje na aktuální MVP komunikace v Android aplikaci: SMS, cloud vzkazy, systémové hlášky a volání jsou řízené per zařízením přes Firestore a admin UI.

## Co se změnilo

- Přidán `RemoteAudioVolume`, který promítá `devices/{deviceId}.volumePercent` do Android audio streamů:
  - `STREAM_NOTIFICATION` pro FCM / systémová oznámení,
  - `STREAM_RING` pro vyzvánění,
  - `STREAM_MUSIC` pro TTS / přehrávání,
  - `STREAM_VOICE_CALL` pro hovory.
- Senior tablet synchronizuje hlasitost automaticky při změně `volumePercent` z Firestore.
- Lokální FCM notifikace v popředí před zobrazením použijí poslední známou vzdálenou hlasitost.
- Manifest doplněn o `MODIFY_AUDIO_SETTINGS`.

## Stav MVP po úpravě

- **SMS**: tablet umí odeslat klasickou SMS přes `SmsManager`; když zařízení nemá SMS kapacitu, zapíše cloudovou náhradu do stejného vlákna (`tablet_firestore`). Příchozí SMS od známého kontaktu se zrcadlí do `messages` jako `sms_inbound`.
- **Interní / systémové vzkazy**: admin nebo web zapisují do `devices/{deviceId}/messages`, Cloud Functions posílají FCM na tablet; tablet ukazuje fullscreen overlay a stav přečtení.
- **Volání**: kontakt lze vytočit z tabletu přes `CALL_PHONE`, fallback je dialer.
- **Admin per klient**: Android admin i web spravují `volumePercent`, kontakty, profil seniora, PIN/SIM a systémovou hlášku pro vybrané zařízení.

## Poznámky / limity

- Android notifikační kanály po vytvoření drží některé volby na straně systému. Proto MVP synchronizuje systémové audio streamy, ne per-channel zvuk v samotném `NotificationChannel`.
- Pokud uživatel nebo systém vynutí režim Nerušit / ticho, Android může změnu hlasitosti omezit.
