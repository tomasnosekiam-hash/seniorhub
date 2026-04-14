# 2026-04-14 – Profil seniora + nouzové kontakty (Fáze A bod 5)

## Cíl

- Rozšířit **`devices/{deviceId}/config/main`** o **jméno, příjmení, adresu** (jeden řádek).
- U kontaktů přidat **`is_emergency`** — priorita pro nouzi / budoucího Matěje; správa z webu (checkbox), zobrazení na tabletu (vizuální zvýraznění).

## Chování

- **Web**: v kartě nastavení tabletu sekce **Profil seniora** + tlačítko **Uložit profil seniora** (nezávislé na PINu). U každého kontaktu checkbox **Nouze** (okamžitý zápis přes `updateDoc`).
- **Tablet**: ve sloupci stavu zobrazení **Senior: …** a **adresa**; u kontaktů s `is_emergency` pozadí a štítek **NOUZE**.

## Dotčené soubory

- `seniorhub-web/src/constants.ts`, `main.ts`, `style.css`
- `seniorhub-android/.../Models.kt`, `MvpRepository.kt`, `HomeScreen.kt`

## Pravidla Firestore

- Beze změny — `config` a `contacts` už podléhají `canAccessDevice`.

## Nasazení

- Publikovat **web** (build / hosting); **Android** nová verze APK pro zobrazení na tabletu.
