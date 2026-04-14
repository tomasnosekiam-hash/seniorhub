# 2026-04-14 – Kontakty → hovor dotykem (Fáze A bod 6, první krok)

## Cíl

- Na **tabletě (Senior / home)** umožnit **klepnutím na řádek kontaktu** zahájit **odchozí hovor** na uložené číslo (MVP: přímé volání po udělení `CALL_PHONE`; při odmítnutí oprávnění fallback na **číselník** `ACTION_DIAL`).

## Chování

- Kontakt s **vyplněným číslem** (alespoň jedna číslice po normalizaci) je **klikatelný**; u řádku je nápis „Klepnutím zavoláš“.
- Bez platného čísla řádek není klikací (zobrazení „—“ beze změny).
- Při prvním volání systém vyžádá oprávnění **Telefon / Hovory**; po zamítnutí se otevře předvyplněný číselník (uživatel dokončí zeleným tlačítkem).

## Dotčené soubory

- `seniorhub-android/app/src/main/AndroidManifest.xml` – `CALL_PHONE`.
- `seniorhub-android/.../util/PhoneDialer.kt` – normalizace čísla, `ACTION_CALL` / `ACTION_DIAL`.
- `seniorhub-android/.../ui/HomeScreen.kt` – `HomeRoute` (runtime permission), `ContactsColumn` / `ContactRow`, callback `onContactCall`.

## Navazuje

- Hlasové vytáčení (Matěj), diktát, sjednocený chat SMS + in-app dle `docs/PROJECT_CONTEXT.md` Fáze A bod 6.
