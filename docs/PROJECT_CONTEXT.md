# SeniorHub OS – Project Context

Tento dokument slouží jako **rychlá orientace** v projektu a jako „paměť“ pro AI asistenta.

## Zásady práce s tímto repem
- **Číst on-demand**: před změnami načíst jen relevantní soubory.
- **Kontext minimum**: pro pokračování obvykle stačí tento soubor + **nejnovější záznam** v `docs/changes/` (viz níže).
- **Zápisy změn**: každá nová etapa práce dostane nový soubor v `docs/changes/` ve formátu `YYYY-MM-DD-kratky-nazev.md`.
- **Co je hotové (changes)**: brát **poslední soubor podle data v názvu** — prefix `YYYY-MM-DD` určuje pořadí; **nejvyšší datum = poslední dokončená etapa** popsaná v repu. Při více souborech se **stejným datem** rozhoduje **čas poslední úpravy** souboru (mtime). Starší soubory jsou historie; pro aktuální stav implementation vždy nejdřív přečíst ten nejnovější záznam.
- **Changelog**: jednotlivé etapy jsou v **`docs/changes/`** (jeden soubor na etapu); samostatný kořenový `CHANGELOG.md` se nepoužívá — tento dokument + `docs/changes/` tvoří „živý“ přehled.

### Snapshot stavu (2026-04, Fáze A)

- **Firestore**: model `devices` / `config` / `messages` / kontakty; **rules** včetně rebindu anonymního tabletu po přeinstalaci (`docs/changes/2026-04-14-firestore-permission-rebind.md`).
- **Web**: PIN/SIM/asistent, profil seniora, nouzové kontakty, vzkazy, párování.
- **Android Senior (tablet)**: PIN z cloudu, kiosk break, kontakty → hovor dotykem, SMS odeslání z kontaktů; FCM / vzkazy; **baterie + heartbeat** na dokument zařízení pro web — detailní popisy v `docs/changes/2026-04-14-*.md`.
- **Další z Fáze A (bod 6+)**: hlasové vytáčení / diktát, jednotný chat (SMS + in-app), Matěj, počasí — viz tabulky níže.

## Co je projekt
- **Produkt**: **jedna Android aplikace** – při prvním spuštění (nebo provisioning) si uživatel vybere **režim Senior** (kiosk / klientský tablet) nebo **režim Admin** (klasická aplikace pro správu klientů). Stejný balíček, odlišné UI, oprávnění a životní cyklus.
- **Produktová priorita**: **klientská část u seniora** (tablet v **kiosku** – komunikace, přístupnost, Matěj, vzkazy, počasí) je **nejdůležitější vrstva produktu**. Bez ní nemá smysl „správcovská“ strana jako samostatná hodnota.
- **Admin režim (Android)**: správa zařízení, kontaktů, nastavení; **asistent Matěj zde není** – žádný wake word ani hlasový proxy v admin UI (správce ovládá aplikaci klasicky).
- **Komunikace**: Firebase (Firestore) – tablet i web čtou a zapisují společná data; **web** je určený i pro **širší rodinu** (např. bez instalace správcovské appky), s **omezenější** sadou akcí než plný admin v Androidu.
- **Kdo co smí**: rozdíly mezi **admin aplikací na Androidu**, **webem** a případnými rolemi v rodině se řídí **přihlášenou identitou** (`uid`) a **Firestore Security Rules** (a v budoucnu případně custom claims) – stejná databáze, jiná pravidla podle uživatele.
- **Správa zařízení**: jeden tablet může mít více správců a jeden správce více zařízení (multi-client / device switcher v admin režimu – viz spec).
- **Plánováno – Emergency Voice Proxy (modul „Matěj“)** (pouze **režim Senior / klientský tablet**): wake word, AI analýza nouze, eskalace hovoru a reporting (viz sekce níže; detailní brief v `oldies_project.md`).
- **Push – Firebase Cloud Messaging (FCM)**: jednotný kanál pro **rychlé upozornění** – nouzové incidenty (správci), **nové vzkazy** z webu na tablet (fullscreen / prioritní UI), případně synchronizační „data messages“. Doplňuje SMS/hovor; odesílání z cloudu vyžaduje **Firebase Admin SDK** nebo **HTTP v1 API** (typicky **Cloud Functions** v `firebase/functions` – zatím může chybět, doplnit při implementaci push).

Úplná produktová a onboarding specifikace (PIN, QR, SIM, zprávy, Matter atd.): `oldies_project.md`.

## Referenční zařízení a AI strategie
- **Tablet pro vývoj (aktuální cíl)**: **Lenovo Idea Tab 11 5G** – **MediaTek Dimensity 6300**, **Mali-G57**, **8 GB RAM**, **Android 15**, 5G. Referenční kus pro ladění; finální podpora dalších zařízení je samostatné rozhodnutí.
- **Gemini Nano (AICore)**: lokální běh přes systémové komponenty Google tam, kde je OEM dodá – **u ne-Pixel tabletů často nejde spolehlivě ověřit před doručením** (žádný úplný veřejný seznam). Po rozbalení: aktualizace systému a Google aplikací, kontrola dostupnosti **AI Core** / nastavení AI v systému.
- **Hybrid**: aplikace by měla umět stejný typ úkolu obsloužit přes **cloud Gemini** (např. Flash/Pro / API podle produktu), když Nano na zařízení **není** nebo **nestačí** – viz také hybridní model v `oldies_project.md` (Nano lokálně + cloud pro složitější konverzaci).

## Směr produktového MVP (shrnutí)
- **Klientský tablet (Senior, kiosk)**: základní „produk“ – **kiosk** jako běžný režim, komunikace a **Matěj** jen zde; admin aplikace tyto funkce nekopíruje.
- **Komunikace**: kontakty, **hlasové vytáčení** ze seznamu, čtení/psaní SMS, postupně **sjednocené vlákno** s kontaktem (in-app chat + SMS); **hlasové zprávy** v konverzaci; práce s telefonem vyžaduje **silná oprávnění** / někdy roli výchozího telefonu nebo SMS aplikace.
- **Informace**: počasí (widgetový přístup, API + cache).
- **Bezpečnost a rodina**: modul **Matěj** (tablet senior), zápisy / **FCM**, stav zařízení pro admina (viz spec v2).
- **Přístupnost**: velký text, kontrast, TTS u důležitých stavů.

## Plán MVP – funkce (co má být v první vydatelné verzi)

Tato sekce je **produktový checklist** pro MVP: sladěný s `oldies_project.md` (onboarding, PIN, QR, vzkazy, Matěj, Matter/AI v dodatku). **Detailní UX a pořadí kroků** zůstávají v `oldies_project.md`; zde jde o **přehled podle platformy** a **stav**.

### Explicitní požadavky – mapa pokrytí (produktová vize)

Níže **tvůj seznam** oproti tabulkám níže: *Ano* = explicitně v plánu (řádek v tabulce nebo tato sekce); *Částečně* = zmíněno obecně, chybí konkrétní řádek nebo datový model; *Doplnit* = doplněno v textu pod tabulkou.

| Požadavek | V plánu jako | Stav poznámka |
|-----------|----------------|---------------|
| **Správa kontaktů** (CRUD, pořadí) | `contacts` + úprava z webu; admin Android | Částečně – web umí; senior UI + admin parita a pravidla pro role. |
| **Vytáčet hlasem** (bez dotyku / hands-free) | Matěj + intenty; řádek „vytočení“ | Plánováno – rozšířit o explicitní **hlasové vytáčení konkrétního kontaktu** (ne jen nouze). |
| **Psát hlasem zprávy** (diktát do SMS / chatu) | Směr MVP + `ai` balíček | Plánováno – STT → kanál SMS nebo in-app. |
| **SMS + interní zprávy v jednom chatu** u kontaktu | „Sjednocené vlákno“, `messages` + SMS | Plánováno – **jedna obrazovka vlákna**; technicky sloučit SMS provider + Firestore zprávy (náročné na UX a synchronizaci). |
| **Základní info o uživateli** – jméno, příjmení, adresa, **kritické kontakty v nouzi** | Rozšířit `config` / profil zařízení | Částečně – `config/main` + `is_emergency` u kontaktů; viz změna 2026-04-14 senior profile. |
| **Vše administrovatelné** z admin části (Android + ideálně web) | Dashboard admin, web | Částečně – cíl; explicitně **jeden zdroj pravdy ve Firestore** upravovaný jen správcem. |
| **Matěj**: poslouchat, vytáčet, psát zprávy, číst zprávy, hlasitý odposlech, počasí | Sekce Matěj + tabulka níže | Částečně – nouze/hovor v textu; **doplněna tabulka schopností** + počasí jako skill. |
| **Počasí** – widget na ploše, **free API** | Směr MVP, widget | Plánováno – např. **Open-Meteo** (bez klíče) nebo jiné free tier API + cache. |

### Asistent Matěj – požadované schopnosti (MVP)

**Rozsah nasazení**: **jen režim Senior** (klientský tablet v kiosku). V **režimu Admin** aplikace **Matěj není** – správce používá standardní obrazovky bez wake wordu a bez hlasového asistenta v aplikaci.

| Schopnost | Poznámka / napojení |
|-----------|---------------------|
| Poslouchat (wake / povel) | Foreground service, wake word; rozšířit mimo čistou „nouzi“. |
| Vytáčet hovory | `TelecomManager`, výběr kontaktu (prioritní / podle jména). |
| Psát zprávy | STT → odeslání SMS nebo in-app zprávy přes existující kanály. |
| Číst zprávy nahlas | TTS nad příchozími SMS / in-app (vyžaduje oprávnění a čisté UI). |
| Zapnout hlasitý odposlech | `AudioManager` / speakerphone u aktivního hovoru. |
| Počasí | Intent → volání free weather API + krátká hlasová odpověď; widget může sdílet stejný zdroj/cache. |

### Profil seniora a nouzové kontakty (datově)

- **Implementováno v MVP**: v `devices/{deviceId}/config/main` pole `senior_first_name`, `senior_last_name`, `address_line`; u kontaktů `is_emergency` (boolean).  
- **Doplnění později**: strukturovaná adresa, `displayName`, případně `emergencyContactIds` v `config` místo / vedle příznaku u kontaktu.  
- **Admin** mění na **webu** (a v budoucnu parita v admin Android); tablet zobrazuje profil a zvýraznění nouzových kontaktů.

### Aktuální zaměření vývoje (priorita)

**Nejzásadnější**: **klientská verze pro seniora** – tablet v **kiosku** (zero UI launcher, odemčení PINem z cloudu, pairing), na něm **komunikace** a **Matěj** (hlas, hovory, diktát, čtení zpráv, repro, počasí jako skill + widget). Bez solidního kiosku a senior UX nemá smysl stavět zbytek jako uživatelsky hotový produkt.  
**Admin (Android)**: správa zařízení a dat; **bez Matěje** – paralela s weby správy, ne s hlasovým asistentem.  
**Komunikační vrstva** (FCM, vzkazy, profil, kontakty) musí být sladěná s tím, co běží **na klientském tabletu**.  
**Neřešíme v této vlně jako hlavní téma** čistě „zdravotní“ use-casy nad rámec komunikace a nouze (např. samostatný wellness modul).  
**Balík „bezpečí a zdraví“** (viz níže) až po posunu v kiosku/komunikaci/Matějovi – sladit pravidla času, tísňové kanály a provozní přehled (**kiosk jako takový do tohoto balíku nepatří** – je v Balíku 1).

### Implementační balík 1 – Komunikace a interakce s Matějem

Souvisící věci **dělat společně** v jedné nebo v těsně navazujících iteracích (stejné oprávnění, stejný datový model, stejné UI vlákno). **Vše podstatné pro seniora běží v kiosku**; admin jen spravuje data a zařízení.

| Podbalík | Co patří dohromady |
|----------|-------------------|
| **Kiosk a shell tabletu (Senior)** | Kiosk / launcher chování, základní „zero UI“, pairing, skryté gesto + **PIN z cloudu**, odemčení do systémových nastavení dle spec – **zásadní pro klientskou verzi**. |
| **Kanály zpráv** | Interní `messages`, SMS, jednotné vlákno u kontaktu; fronty při výpadku; potvrzení přečtení; FCM pro příchozí. |
| **Hlas a text** | STT diktát do zprávy, TTS čtení příchozích, hlasové vytáčení; sdílená úprava kontaktů z webu/adminu. |
| **Matěj (jádro)** | Wake / poslech, intenty na volání a zprávy, repro u hovoru; napojení na stejné kontakty a kanály jako ruční UI; počasí přes stejný API/cache jako widget. **Pouze senior flow; admin aplikace bez Matěje.** |
| **Provoz komunikace** | Průvodce oprávněními (telefon, SMS, mikrofon) v logickém pořadí; stav „chybí oprávnění“; přístupnost ovládání chatu a hovorů (velký text, kontrast, TTS stavů); **lokalizace** (min. čeština) pro UI a hlasové odpovědi. |

### Implementační balík 2 – Bezpečí a zdraví

Doporučené **dříve vyjmenované doplnky**, které spolu logicky souvisí – **seskupit a implementovat najednou** (jedna vlna návrhu: pravidla, UI seniora, admin přehled, případně Firestore):

| Podbalík | Funkce (implementovat společně) |
|----------|--------------------------------|
| **Tísň a signály** | Rychlý přístup k **112 / lokální tísňové lince** mimo běžné kontakty; signál **„jsem v pořádku“** rodině (zápis / notifikace); **souhra Nerušit vs priorita** nouze a kritických FCM (časová pravidla, kanály). |
| **Provoz a „zdraví“ zařízení** | **Stav zařízení** pro admina (`status`: online, baterie, signál); základ pro pozdější rozšíření o explicitnější „wellness“ funkce, pokud produkt přidá. |
| **Ochrana dat a účtů** | **Záloha / export** kontaktů a profilu pro správce; **odhlášení webu** na sdíleném PC; **audit** nebo alespoň poslední editor u kritických polí ve Firestore. (Kiosk jako produktová vrstva je v **Balíku 1**, ne zde.) |
| **Servis a nouze provozu** | **Technický / servisní přístup** nebo fallback heslo dle `oldies_project.md`, bezpečné uložení a procedura. |

**Legenda stavu**

| Stav | Význam |
|------|--------|
| **Hotovo** | Implementováno v repu; ověřit v nejnovějším `docs/changes/`. |
| **Částečně** | Část hotová nebo jen jedna platforma; doplnit podle poznámky. |
| **Plánováno** | Ještě není v kódu; cíl MVP (priorita P0/P1). |

**Priorita**: **P0** = bez toho není MVP použitelné v terénu; **P1** = silně žádoucí v první vlně, může přijít hned po P0.

### Společná platforma (Firebase)

| Funkce | Stav | Priorita | Poznámka |
|--------|------|----------|----------|
| Firestore model `devices`, `deviceAdmins`, `pairingClaims`, kontakty | Hotovo | P0 | Viz změna 2026-04-14. |
| Security Rules (tablet + admin podle vazeb, pairing s expirací) | Hotovo | P0 | Doplnění read/update u `devices/{id}` pro rebind anonymního uid (`docs/changes/2026-04-14-firestore-permission-rebind.md`). Role web vs plný admin → Plánováno. |
| Anonymní Auth (tablet) + Google (správci) | Hotovo | P0 | Konzole: Anonymous + Google. |
| `devices/.../config/main` (PIN, SIM, jméno asistenta, …) | Hotovo | P0 | Viz `docs/changes/2026-04-14-device-config-kiosk-pin.md`; další pole (profil seniora) viz níže. |
| `config`: **profil seniora** (jméno, příjmení, adresa) + reference na **kritické kontakty** | Částečně | P0 | Jméno/příjmení/adresa v `config/main`; kritické kontakty přes `is_emergency` u `contacts` — viz `docs/changes/2026-04-14-senior-profile-emergency.md`. |
| `devices/.../messages` + potvrzení přečtení | Hotovo | P0 | Viz `docs/changes/2026-04-14-messages-fcm-functions.md`; vylepšení kanálů/rolí dál. |
| Jednotné **vlákno s kontaktem** (in-app + SMS): model zpráv s rozlišením kanálu | Plánováno | P0 | UI jeden chat; technicky SMS Telephony + Firestore. |
| `devices/.../status` (online, baterie, signál) | Částečně | P1 | Baterie + `lastHeartbeatAt` na dokumentu `devices/{id}` (tablet + web); vlastní subdokument `status` / signál sítě zatím ne. |
| `incidents` + zápis při nouzi | Plánováno | P1 | Navázat na Matěj / FCM. |
| Cloud Functions: odeslání FCM při novém vzkazu / incidentu | Částečně | P0 | Funkce u vzkazu v repu; incidenty + deploy na projekt. |
| Ukládání FCM tokenů (zařízení / správci) | Částečně | P0 | `fcmRegistrationToken` na `devices/{id}` (tablet); správci / více tokenů dál. |

### Android – režim Senior (tablet u seniora)

| Funkce | Stav | Priorita | Poznámka |
|--------|------|----------|----------|
| Stabilní `deviceId`, bootstrap zařízení v Firestore | Hotovo | P0 | `DeviceIdentityStore`, `MvpRepository`. |
| Párování: zobrazení kódu, expirace, obnova kódu | Hotovo | P0 | QR podle spec zatím ne; kód + web. |
| **Kiosk** – klientský režim jako launcher / „zero UI“, základ dashboardu (hlasitost, alert), odemčení nastavení | Částečně | P0 | **Zásadní** pro produkt; **PIN z cloudu + skryté gesto → nastavení** hotovo (viz změna 2026-04-14); doplnit plný launcher / zero UI dle spec. |
| První spuštění: volba **Senior** (bez plného setup flow ze spec) | Plánováno | P0 | Spec: generovaný PIN, QR; sladit s aktuálním pairing tokem. |
| Skryté gesto + keypad + odemčení vs **admin PIN** ve Firebase | Hotovo | P0 | Základ dle změny 2026-04-14; rozšíření dle `oldies_project.md` krok 3 dál. |
| **Správa kontaktů** (zobrazení ze serveru; úpravy přes admin / web dle pravidel) | Částečně | P0 | Web umí CRUD; senior tablet typicky jen používá; sladit s `contacts`. |
| Seznam kontaktů → **vytočení hovoru** (dotyk) | Hotovo | P0 | `CALL_PHONE`; viz `docs/changes/2026-04-14-contacts-touch-dial.md`. |
| **Hlasem vytočit** hovor (asistent / rozpoznání kontaktu) | Plánováno | P0 | Matěj nebo voice UI; navázat na kontakty. |
| **Hlasem diktovat** zprávy (STT → SMS nebo in-app) | Plánováno | P0 | Minimálně jeden kanál v MVP. |
| **Jednotný chat** s kontaktem (SMS + interní zprávy v jednom vlákně) | Plánováno | P0 | Viz Firebase řádek o kanálech. |
| SMS **odeslání** z kontaktů (tablet, psaní + odeslat) | Hotovo | P0 | `SEND_SMS`; viz `docs/changes/2026-04-14-android-send-sms.md`. |
| SMS **čtení** / sjednocení s in-app chatem | Plánováno | P1 | Oprávnění + UI vlákna; navázat na řádek výše. |
| **Počasí** – widget na ploše (free API + cache) | Plánováno | P1 | Např. Open-Meteo; sdílet data s Matějem. |
| Příjem **FCM** → fullscreen vzkaz / prioritní UI | Částečně | P0 | Push + překryv z Firestore; plné fullscreen chování dle produktu dál. |
| Modul **Matěj** – plný rozsah: poslech, hovory, psaní/čtení zpráv, repro, počasí (viz tabulka schopností) | Plánováno | P1 | Jen **Senior**; lze štěpit: P0 = nouze + hovor; P1 = zbytek skills. |

### Android – režim Admin (správce z telefonu/tabletu)

| Funkce | Stav | Priorita | Poznámka |
|--------|------|----------|----------|
| **Bez asistenta Matěje** – žádný wake word, žádný hlasový proxy v aplikaci | Rozhodnutí | – | Admin = správa; Matěj pouze na tabletu u seniora. |
| Google Sign-In, přístup ke spárovaným zařízením | Částečně | P0 | Ověřit paritu s webem (`deviceAdmins`). |
| **Device switcher** (více klientů pod jedním účtem) | Plánováno | P0 | Spec sekce Multi-Client. |
| Párování dalšího zařízení (QR nebo zadání kódu) | Částečně | P0 | Web umí kód; Android admin skener dle spec. |
| Dashboard: zobrazení / úprava **PINu** tabletu, **SIM** | Částečně | P0 | **Web** hotovo (`config/main`); plná parita v Android admin UI → Plánováno. |
| **Správa kontaktů, profilu seniora** (jméno, příjmení, adresa, kritické kontakty) – plná parita s webem | Plánováno | P0 | Jeden model ve Firestore. |
| Odeslání vzkazu / synchronizace kontaktů se shodným modelem jako web | Plánováno | P1 | Sjednotit s `messages` a kontakty. |

### Web (rodina / správci bez Android appky)

| Funkce | Stav | Priorita | Poznámka |
|--------|------|----------|----------|
| Google Sign-In | Hotovo | P0 | |
| Seznam „Moje tablety“, párování kódem z tabletu | Hotovo | P0 | |
| Úprava nastavení a kontaktů pro vybrané zařízení | Hotovo | P0 | Sekce PIN/SIM/asistent ve `config/main` (viz změna 2026-04-14); role / další pole dál. |
| Vzkazy do `messages` + viditelnost stavu přečtení | Hotovo | P0 | Základ v repu; rozšíření rolí později. |
| FCM Web Push (volitelně) | Plánováno | P1 | Tablet + mobil správců jsou důležitější. |

### MVP – doporučené pořadí dodávek (hrubě)

**Fáze A – Balík 1 (Komunikace a Matěj)** – aktuální zaměření; **jádrem je klientský tablet (kiosk + senior UX)**:

1. **Kiosk na tabletu (Senior)** – chování launcheru, zero UI, **PIN/gesto z cloudu**, pairing; paralelně **dashboard PIN/SIM** u správce (web + Android admin), aby šlo zařízení bezpečně provozovat.  
2. **Datový model** `config` + `messages` (+ rules), případně úprava stávajících polí na zařízení.  
3. **FCM** na tabletu (token do Firestore) + **Cloud Functions** pro push při novém vzkazu.  
4. **Vzkazy** end-to-end (web → tablet, přečtení zpět).  
5. **Profil seniora** + **kritické kontakty** ve `config`; správa z adminu a webu.  
6. **Kontakty → hovor** (dotyk) a **SMS odeslání** — hotovo; dál **hlasové vytáčení a diktát**; **sjednocený chat** (SMS + in-app) podle kapacity.  
7. **Počasí** (widget + sdílená data pro Matěje).  
8. **Matěj** (pouze senior) – postupně: nouze + hovor → psaní/čtení zpráv → počasí jako skill (viz tabulka schopností).

**Fáze B – Balík 2 (Bezpečí a zdraví)** – až po rozumném pokrytí fáze A; jedna souvislá vlna:

- Tísňové linky, signál „jsem v pořádku“, pravidla Nerušit vs kritické události, **stav zařízení** v adminu, záloha/export, audit webu + servisní přístup (podle tabulky v balíku 2).

Po dokončení významného bloku z tohoto plánu přidat záznam do `docs/changes/` (nový soubor `YYYY-MM-DD-…`), aby byl „poslední podle data“ v souladu se zásadami práce s `docs/changes/` v úvodu tohoto dokumentu.

## Struktura repozitáře (monorepo)

### Kořen
- `seniorhub-android/` – jedna Android aplikace (Kotlin + Compose), **Senior + Admin** v jednom APK
- `seniorhub-web/` – web (Vite + TypeScript), rodina / omezený přístup bez správcovské appky
- `firebase/` – **`firestore.rules`**, **`functions/`** (FCM při novém vzkazu); kořenové **`firebase.json`**
- `docs/` – `PROJECT_CONTEXT.md`, záznamy etap v `docs/changes/` (index: `docs/changes/README.md`)
- `oldies_project.md` – produktová specifikace v2 (root)
- `AGENT.md` – pokyny pro AI asistenty (root)

### Cílové členění Android kódu (`seniorhub-android/app/.../com/seniorhub/os/`)
Jedno `app` modul; logika **rozvržená do balíčků** (současný kód je často v `data/` + `ui/` – při refaktoru směřovat sem):

| Oblast | Účel |
|--------|------|
| `role` nebo `setup` | první spuštění: volba **Senior / Admin**, persistovaný režim |
| `senior` | kiosk / zero UI, pairing (QR), odemčení nastavení (PIN) |
| `admin` | přepínač zařízení, správa PINu/SIM, párování dalších tabletů – **bez modulu Matěj** |
| `data` | Firestore repozitáře, identity, modely (stávající `MvpRepository`, `DeviceIdentityStore`, …) |
| `ai` nebo `assistant` | **Gemini** (Nano přes AICore + **cloud fallback**), intenty pro volání/SMS/diktát – **jen ve flow Senior / kiosk** |
| `messaging` | **FCM**: `FirebaseMessagingService`, registrace tokenu, zápis tokenu do Firestore, routing notifikací na UI (vzkazy, nouze) – primárně tablet senior |
| `emergency` nebo `voice` | modul **Matěj** (foreground service, wake word) – **jen režim Senior**, ne admin |
| `ui` | sdílené Compose obrazovky, téma (`theme/`) |

### Web (`seniorhub-web/src/`)
- Postupně dle domén: `auth`, `devices`, `messages`, sdílené `firebase.ts` – až poroste funkce, oddělit moduly; **FCM na webu** volitelně (Web Push přes FCM) pro členy rodiny, není nutné pro MVP tabletu.

## Firebase platforma (Firestore, Auth, FCM)

### Firestore
- Primární úložiště stavu zařízení, vzkazů, správcovských vazeb; pravidla v `firebase/firestore.rules`.

### Authentication
- Tablet: anonymní účet zařízení; správci: Google Sign-In (web i Android admin). Rozlišení oprávnění viz rules + `deviceAdmins`.

### Firebase Cloud Messaging (FCM)
- **Účel**: push na **Android** (tablet i telefony správců) bez čekání na polling; vhodné pro kritické události a vzkazy přes celou obrazovku na tabletu.
- **Klient Android**: po přihlášení k FCM získat **registrační token**, uložit do Firestore (např. pole na `devices/{deviceId}` a/nebo dokument uživatele u správců), token obnovovat při změně (`onNewToken`).
- **Klient web** (volitelně): FCM Web SDK pro notifikace v prohlížeči rodinným účtem.
- **Odeslání z backendu**: Cloud Functions spouštěné zápisem (např. nový dokument ve `messages`, nový `incidents`) nebo HTTPS callable; volání **Firebase Admin SDK** `send()` / HTTP v1. Čistý klient (jen web/Android) **neumí bezpečně poslat** push ostatním bez serveru nebo Functions – proto plánovat `firebase/functions` nebo jiný důvěryhodný backend.
- **Obsah**: `notification` + `data` payload (ID zařízení, typ události) pro správné otevření obrazovky na tabletu.

## Android aplikace (dva režimy v jedné app)
### Senior (kiosk / klient)
- **Produktová priorita**: tato vrstva je **hlavní uživatelská hodnota** – tablet jako kioskový klient s komunikací a Matějem.
- Cíl: „zero UI“ launcher na tabletu pro seniora; vstup do nastavení chráněný (např. skryté gesto + **Admin PIN** synchronizovaný z cloudu – viz `oldies_project.md`).
- Pairing se správcem přes **QR** + potvrzení ve Firebase (v aktuálním MVP jiný tok: zobrazovaný kód a zadání na webu – evoluce směrem k specifikaci).

### Admin (správce)
- Cíl: klasická Android aplikace – **přepínač zařízení** (více klientů pod jedním účtem), párování dalších tabletů (QR), dashboard včetně **viditelnosti / úpravy PINu** tabletu, zadání **čísla SIM** v tabletu pro nouzové volání/SMS.
- **Matěj a hlasový asistent nejsou součástí admin režimu** – správa zařízení a dat bez wake wordu.
- Není vázaný na jedno fyzické zařízení; stejná appka jako u Seniora, jiná role po instalaci.

### Entry points (současný kód)
- `seniorhub-android/app/src/main/java/com/seniorhub/os/MainActivity.kt`
  - start UI, inicializuje repository, používá stabilní `deviceId`.

### Data / připojení na Firebase
- `seniorhub-android/app/src/main/java/com/seniorhub/os/data/DeviceIdentityStore.kt`
  - ukládá stabilní `deviceId` do DataStore (typicky `tablet-${ANDROID_ID}`).
- `seniorhub-android/app/src/main/java/com/seniorhub/os/data/MvpRepository.kt`
  - anonymní Firebase Auth pro tablet
  - bootstrap dokumentu zařízení v `devices/{deviceId}`
  - generování pairing kódu a zápis do `pairingClaims/{code}`
  - realtime read nastavení + kontaktů

### UI
- `seniorhub-android/app/src/main/java/com/seniorhub/os/ui/HomeViewModel.kt`
  - state pro dashboard + pairing overlay
- `seniorhub-android/app/src/main/java/com/seniorhub/os/ui/HomeScreen.kt`
  - „black canvas“ UI, pairing overlay s kódem

## Emergency Voice Proxy (modul „Matěj“)
**Platí pouze pro klientský tablet v režimu Senior (kiosk).** V admin režimu se neprovozuje.

Účel: poskytnout seniorovi **okamžitou hlasovou pomoc** ve fyzické tísně bez ovládání dotykem – kombinace wake wordu, AI kontextu a eskalace hovoru.

### Scénář (user flow)
- **Aktivace**: vyslovení jména asistenta (např. „Matěji!“) – dynamicky z nastavení (`assistant_name`).
- **Naslouchání**: vizuální signál (pulzování okraje displeje), nahrávka hlasu.
- **Analýza (Gemini Nano)**: vyhodnocení sémantiky; při krizových výrazech spuštění emergency protokolu.
- **Eskalace**: výběr prioritního kontaktu z lokální DB; hovor přes systémové API s **hlasitým odposlechem** (speakerphone).
- **AI proxy**: pokud druhá strana přijme hovor a uživatel nemluví, TTS převezme slovo a nahlásí stav nouze syntetickým hlasem.

### Technické bloky (Android + cloud)
- **Wake Word Engine (always-on)**: Picovoice Porcupine nebo PocketSphinx (lokálně, nízká spotřeba); **Foreground Service** s vysokou prioritou.
- **Emergency Logic Controller (Kotlin)**: rozpoznání záměru (prompt pro Gemini Nano: stupeň nouze 0–10, osoba k volání, stručný popis); **TelecomManager** pro hovor; **AudioManager** – `STREAM_VOICE_CALL`, `setSpeakerphoneOn(true)`.
- **TTS**: Android `TextToSpeech` – dynamická věta (asistent, uživatel, popis potíží, výzva k dialogu přes hlasitý odposlech).
- **Firebase**: zápis do kolekce **`incidents`** (čas, přepis / metadata volání); **FCM** k okamžitému upozornění správců (priorita kanálu, viz sekce *Firebase platforma*; plné „probudit z Nerušit“ závisí na OEM/Android).

### Omezení a zásady
- **Oprávnění**: pro plnou kontrolu hovorů bez dotyku typicky **Default Dialer** nebo **Device Owner**.
- **Offline**: wake word + vytočení prioritního kontaktu musí fungovat **bez internetu** (GSM); cloud AI jen pro doplnění analýzy, ne pro samotné vytočení pomoci.
- **Konfigurace**: jméno asistenta a nouzové kontakty synchronizované z Firebase (senior je omylem nesmaže).

### Vývojový kickoff (shrnutí z briefu)
Foreground Service naslouchá klíčovému slovu → po detekci krátké nahrávání → analýza záměru (zatím mock Gemini Nano) → při `emergency` vytočení definovaného čísla → po přijetí hovoru TTS s informací o nouzi.

## Web (rodina / správci bez Android appky)
- Stejný Firestore jako Android; vhodné pro **širší rodinu** (např. vzkazy), kde nechceme instalovat správcovskou aplikaci. Oproti **plnému admin režimu na Androidu** bude web typicky **omezenější** – co přesně půjde dělat, určí **pravidla podle `uid`** (Security Rules) a produktová návrhová tabulka oprávnění.
- **FCM na webu** je volitelný (notifikace v prohlížeči); kritický je **FCM na tabletu** při nových vzkazech a u správců při incidentech (viz *Firebase platforma*).
- `seniorhub-web/src/main.ts`
  - Google Sign-In
  - seznam „Moje tablety“ přes join kolekci `deviceAdmins`
  - pairing přes zadání kódu (batch: update claim + create join; následně nastaví `paired=true`)
  - správa nastavení a kontaktů pro vybraný tablet
- `seniorhub-web/src/firebase.ts`
  - init Firebase + helpery pro Auth/Firestore
- `seniorhub-web/.env` (lokální, necommitovat)
  - Firebase web config (Vite `VITE_*`)

## Firestore datový model (aktuální)
### Kolekce
- `devices/{deviceId}`
  - `deviceId`, `deviceAuthUid`, `deviceLabel`, `volumePercent`, `alertMessage`
  - `fcmRegistrationToken` (string, poslední token FCM pro tablet)
  - `batteryPercent` (0–100, volitelně), `charging` (bool), `lastHeartbeatAt` (timestamp — pravidelný signál z tabletu pro správce)
  - `paired` (bool)
  - pairing metadata: `pairingCode`, `pairingExpiresAt`
- `devices/{deviceId}/config/main` (document id `main`)
  - `admin_pin`, `sim_number`, `assistant_name`, `senior_first_name`, `senior_last_name`, `address_line` (a generovaný PIN na tabletu při bootstrapu, pokud chybí)
- `devices/{deviceId}/contacts/{contactId}`
  - `name`, `phone`, `sortOrder`, `is_emergency` (boolean, priorita nouze)
- `devices/{deviceId}/messages/{messageId}`
  - `body`, `createdAt`, `senderUid`, `senderDisplayName`, `readAt` (tablet při potvrzení)
- `pairingClaims/{code}`
  - `code`, `deviceId`, `deviceAuthUid`, `expiresAt`, `usedAt`, `usedByUid`
- `deviceAdmins/{deviceId}_{uid}`
  - `deviceId`, `uid`, `role`, `claimCode`, `createdAt`
- `users/{uid}`
  - poslední aktivita + profil (volitelně)
- **Plánováno – `incidents`** (Emergency Voice Proxy): zápis při detekci nouze (časové razítko, přepis / metadata volání); viz sekce Matěj výše.

### Směr vývoje (spec v2 v `oldies_project.md`)
Struktura dokumentů pod zařízením může rozšířit např. o:
- `devices/{deviceId}/config` – **část hotová**: dokument `main` s `admin_pin`, `sim_number`, `assistant_name`; dál např. `paired_admin_id`, profil seniora
- `devices/{deviceId}/status` – online/offline, baterie, signál
- `devices/{deviceId}/messages` – vzkazy (web i admin; tablet notifikace / fullscreen + potvrzení přečtení)
- **FCM**: na dokumentu zařízení a/nebo u uživatele pole typu **`fcmTokens`** / **`fcmRegistrationTokens`** (jeden uživatel může mít více zařízení) – pro cílení push na tablet a na telefony správců

## Firestore rules
- `firebase/firestore.rules`
  - přístup k zařízení povolen pro:
    - daný tablet (anonymní auth, `deviceAuthUid`)
    - admin účet (existuje `deviceAdmins/{deviceId}_{uid}`)
  - pairing je vázaný na existující `pairingClaims/{code}` s expirací a jednorázovým použitím
  - **Do budoucna**: pravidla rozšířit tak, aby rozlišovala **plného správce** (Android admin app), **webového člena rodiny** a případné další role – podle `request.auth.uid` a vazeb na zařízení (viz cíle oprávnění v `oldies_project.md`).

## Firebase Console checklist
- **Authentication**: povolit `Google` a `Anonymous`
- **Firestore rules**: publikovat obsah `firebase/firestore.rules`
- **Cloud Messaging (FCM)**: v projektu zapnuté; Android – stáhnout **google-services.json** do `seniorhub-android/app/`; pro serverové odesílání nastavit **Service Account** (HTTP v1) pro Admin SDK / Functions
- **Cloud Functions** (až budou v repu): nasadit funkce pro odeslání FCM při novém vzkazu / incidentu (nebo ekvivalentní backend)

## Changelog
- Viz `docs/changes/`. **Aktuální hotová etapa** = soubor s **nejpozdějším `YYYY-MM-DD`** v názvu (při shodě datumů soubor naposledy upravený v systému souborů). Ten záznam popisuje, co je v kódu a pravidlech už zaneseno; starší soubory jsou jen archiv kroků.

