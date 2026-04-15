# 2026-04-15 — Jednotné vlákno u kontaktu (odchozí): Firestore + zrcadlo SMS

## Shrnutí

- **`delivery: sms_cellular`**: po úspěšném odeslání klasické SMS se stejný text zapíše do `devices/.../messages` (vedle `tablet_firestore` bez SIM).
- **Firestore rules**: tablet smí vytvářet zprávy s `delivery` buď `tablet_firestore`, nebo `sms_cellular` (stejná pole `outbound_phone` / `outbound_name`).
- **Odchozí z tabletu**: u záznamů z tabletu je **`readAt` nastaveno při vytvoření** — nepřečtený overlay se vztahuje na vzkazy **od rodiny**, ne na vlastní odchozí.
- **UI Senior**: levý sloupec = **vzkazy od rodiny** (bez odchozích z tabletu); u kontaktu **„Vlákno“** = chronologie odchozích (cloud vs SMS); nápověda u SMS o zrcadlení do cloudu.
- **Web** (`main.ts`): popisek technologie pro `sms_cellular`.
- **Android admin**: u seznamu vzkazů krátká značka kanálu (web / cloud / SMS zrcadlo).

## Nasazení

- `firebase deploy --only firestore:rules` (nová hodnota `delivery` v pravidlech create).

## Soubory (orientačně)

- `firebase/firestore.rules`, `seniorhub-android/.../MvpRepository.kt`, `MessageThreads.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `AdminRoute.kt`, `seniorhub-web/src/main.ts`, `Models.kt`
