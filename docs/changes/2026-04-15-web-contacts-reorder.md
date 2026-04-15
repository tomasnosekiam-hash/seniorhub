# Web admin: řazení kontaktů (↑ / ↓)

## Shrnutí

- U seznamu kontaktů na webu jsou tlačítka **Výš** / **Níž** (stejný princip jako Android admin).
- Pořadí ve Firestore: prohození hodnot **`sortOrder`** mezi dvěma sousedními dokumenty v **`runTransaction`** (konzistence s Androidem).

## Dotčené soubory

- `seniorhub-web/src/main.ts` — `swapContactSortOrders`, úprava snapshotu kontaktů (`snap.docs` + listenery)
- `seniorhub-web/src/style.css` — `.contact-sort`, `.sort-arrow`

## Ověření

- `npm run build` ve `seniorhub-web/`
