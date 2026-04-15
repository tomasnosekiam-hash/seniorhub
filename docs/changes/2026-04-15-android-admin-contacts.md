# Android Admin: správa kontaktů (parita s webem)

## Shrnutí

- Režim **Správce** zobrazuje kontakty vybraného zařízení (`devices/{id}/contacts`, řazení `sortOrder`).
- **Přidat kontakt** — jméno a/nebo telefon; stejná pole jako web (`name`, `phone`, `is_emergency`, `sortOrder`, `createdAt`).
- **Nouze** — přepínač `is_emergency` (priorita pro hovor / Matěje).
- **Smazat** — odstranění dokumentu kontaktu.
- **Pořadí** — tlačítka ↑ / ↓ prohazují `sortOrder` dvou sousedních kontaktů v **Firestore transakci** (stejný model jako web; web nové kontakty řadí přes `Date.now()`).

## Dotčené soubory

- `seniorhub-android/.../data/Models.kt` — `Contact.sortOrder`
- `seniorhub-android/.../data/MvpRepository.kt` — mapování `sortOrder` ze snapshotu
- `seniorhub-android/.../data/AdminRepository.kt` — `observeContacts`, `addContact`, `setContactEmergency`, `deleteContact`, `swapContactSortOrders`
- `seniorhub-android/.../ui/AdminViewModel.kt` — stav `contacts`, `moveContactUp` / `moveContactDown`
- `seniorhub-android/.../ui/AdminRoute.kt` — sekce UI mezi profilem seniora a vzkazem

## Ověření

- `./gradlew :app:compileDebugKotlin`
