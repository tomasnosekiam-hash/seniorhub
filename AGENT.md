# AGENT.md – SeniorHub Engineering Rules

Tento dokument je **závazný** pro veškeré změny v repozitáři. Cíl: bezpečný, přístupný a úsporný systém pro seniorské tablety + web administraci rodiny.

## 1) Principy (nejvyšší priorita)
- **Security-first**: žádné „dočasné“ otevřené přístupy bez jasného vypnutí a závazného plánu odstranění.
- **Accessibility-first**: vysoký kontrast, velké fonty, deterministické UI, žádné „tiny tap targets“.
- **Resource-first**: cílová zařízení mohou mít slabší HW (RAM/CPU/baterie). Preferuj jednoduchost.
- **Offline/poor network tolerance**: UI musí zůstat použitelné i při výpadcích sítě.
- **Least privilege**: každá role/klient má jen minimum oprávnění.

## 2) Bezpečnost a autentizace (Firebase)
### 2.1 Základní pravidla
- **Nikdy** nepoužívat Firestore rules typu `allow read, write: if true` mimo lokální demo. Pokud je to někdy potřeba, musí to být:
  - **časově omezené** (uveď datum/čas expirace),
  - **zdokumentované** v `docs/changes/` (proč, jak poznáme že je hotovo, kdo to vypíná),
  - **odstraněné hned po ověření**.
- **Web admin** vždy autentizovaný (Firebase Auth). Minimálně Google Sign‑In.
- **Tablet** musí mít jednoznačnou identitu. Preferuj anonymní Auth + uložené stabilní `deviceId`.
- **Žádné API klíče pro placené AI** v klientovi (APK/JS) pro produkci. Volat přes backend (Cloud Functions / vlastní server) se server-side autentizací.

### 2.2 Firestore rules
- Rules musí být:
  - **čitelná**, s helper funkcemi, bez duplikace
  - **testovatelná** (aspoň ruční scénáře + emulator, když bude přidán)
  - navázaná na model oprávnění (např. join kolekce `deviceAdmins`)
- Každá změna schématu musí zahrnout i změnu rules (stejný PR/commit).
- Každá změna rules musí mít v `docs/changes/` stručně sepsané:
  - **allowed scénáře** (co má projít)
  - **denied scénáře** (co se musí zamítnout)

### 2.3 Secrets & konfigurace
- **Nikdy necommitovat**:
  - `seniorhub-web/.env`
  - service account JSON
  - privátní klíče, certifikáty, tokeny
- Veškeré secrets patří do:
  - lokálních `.env` / OS keychain
  - Vercel/Firebase env vars (pokud bude nasazení)

### 2.4 PII a soukromí (kontakty, telefony)
- Kontakty/telefony jsou **osobní údaje**.
- **Nikdy neloguj** celé telefonní číslo ani obsah zpráv do konzole/logů; pro debug používej maskování (např. `+420 *** ** 22`).
- Screenshoty/exporty UI (pokud se zavedou) musí být **privátní**, časově omezené a auditované.

## 3) Datový model a migrace
- Schéma musí být **versionované konceptem**, ne nutně číslem:
  - nové kolekce/pole přidávej kompatibilně
  - staré pole nemaž hned (nejdřív „write both“, pak „read new“, pak cleanup)
- Změny schématu musí být popsány v `docs/changes/YYYY-MM-DD-*.md`.
- Vztahy typu many‑to‑many řeš pomocí **join kolekcí**, ne map v dokumentu, pokud na tom stojí rules a query.

## 4) Web (TypeScript + Vite) – best practices
- **Type safety**: žádné `any` bez důvodu. Validuj data z Firestore (nepředpokládej shape).
- **Auth gating**: UI nesmí zobrazovat data před přihlášením.
- **XSS**:
  - Preferuj DOM API (vytváření elementů) pro dynamický obsah.
  - `innerHTML` používej jen pro **statické šablony** nebo pokud jsou **všechny proměnné** povinně escapované (`escapeHtml`).
- **Perf**: omez `getDoc` v cyklech. Preferuj query, cache, nebo denormalizaci labelu.
- **UX**: jasné stavy loading/error/empty, žádné tiché selhání.

## 5) Android (Kotlin + Compose) – best practices
- **Stavové UI**: jednoznačné stavy (loading/ready/error/pairing). Žádné implicitní side-effecty v Composables.
- **MVVM**: IO v repository/use-case, UI pouze render.
- **Flows**: každá listener registrace musí být správně ukončená (`awaitClose`).
- **Battery/CPU**: vyvaruj se častému polling. Preferuj realtime listenery a throttling.
- **Crash safety**: žádné system dialogy pro seniora; chyby zobrazit srozumitelně a logovat pro admina (později).

## 6) Kiosk/Launcher (budoucí práce)
- Žádné „hard block“ bez device-owner strategie.
- Vše, co může zablokovat zařízení (gesta, notifikace), musí mít **recovery plan**.

## 7) Přístupnost (A11y) a UX pravidla
- **Kontrast**: default black canvas (`#000`) + bílá/žlutá pro klíčové prvky.
- **Font**: velké (typicky 18–72sp podle kontextu), čitelné, bez jemných váh.
- **Tlačítka**: velké hitboxy, minimálně ~48dp.
- **Determinismus**: žádné překvapivé animace, žádné přeplácané layouty.
- **Jazyk**: krátké věty, bez technických termínů pro seniora.

## 8) Observability a provoz
- Logování:
  - klient: lokální log + (později) odeslání agregovaných chyb adminovi
  - web: zobrazení chyb uživateli + konzole pro debug
- Změny konfigurace přes Firestore musí být auditovatelné:
  - **Web zápisy** do `devices/{deviceId}` musí nastavovat `updatedAt` a `updatedByUid`.
  - (Volitelně) ukládat i `updatedByEmail` pro podporu (nepovinné, může být PII).

## 9) Pairing a abuse-prevence
- Pairing kód musí být:
  - **krátký** (pro opsání), ale bez zaměnitelných znaků
  - **časově omezený** (expirace)
  - **jednorázový** (po použití je neplatný)
- Ve webu přidej **throttling** na pokusy o spárování (např. krátký cooldown po chybě), aby se snížilo brute-force riziko.
- Pro produkci zvaž přesun „claim pairing“ do **Cloud Function** (atomicky ověřit + zapsat), pokud se objeví hraniční případy nebo útoky.

## 10) Testovací minimum (bez ohledu na rozsah)
Každá změna musí splnit:
- Android: `./gradlew :app:assembleDebug` projde
- Web: `npm run build` projde
- Ruční sanity check:
  - přihlášení (web)
  - načtení zařízení
  - změna nastavení → projeví se na tabletu

## 11) PR / commit disciplína (i bez GitHubu)
- Každý větší zásah doplň do `docs/changes/` (datum + co + proč).
- Nepřidávej velké refaktory do stejné změny jako bugfix bezpečnosti.
- Preferuj malé, izolované změny.

## 12) „Stop the line“ pravidla (blokující)
Změna se **nesmí** mergnout/použít, pokud:
- porušuje Firestore rules (otevřené přístupy) nebo obchází Auth
- ukládá secrets do repa
- rozbíjí build (Android/Web)
- výrazně zhoršuje přístupnost (malé fonty, nízký kontrast, složitá navigace)
- přidává dlouhé background smyčky/polling bez throttlingu

---

## Kontextové dokumenty
- Rychlý přehled: `docs/PROJECT_CONTEXT.md`
- Changelog: `docs/changes/`

