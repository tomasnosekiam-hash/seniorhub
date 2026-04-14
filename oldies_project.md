Aktualizovaná Technická Specifikace: SeniorHub OS (v.2.0)
1. Onboarding & Provisioning (Instalační proces)
Systém využívá „Zero-Config“ model pro seniora a „Master-Control“ pro administrátora.

První spuštění (Tablet/Klient):

Uživatel zvolí roli „Senior“.

Aplikace vygeneruje náhodný 4místný Admin PIN (např. 8254) a unikátní DeviceID.

Zobrazí se QR kód pro spárování.

Aplikace čeká na potvrzení z Firebase, že byla spárována.

První spuštění (Mobil/Admin):

Uživatel zvolí roli „Admin“.

Naskenuje QR kód z tabletu.

Zadá telefonní číslo SIM karty vložené v tabletu (pro potřeby nouzového volání a SMS).

Výsledek: Admin v aplikaci okamžitě vidí vygenerovaný PIN tabletu a může ho vzdáleně spravovat.

2. Multi-Client Architecture
Admin aplikace není vázána na jedno zařízení.

Device Switcher: V admin rozhraní je seznam spárovaných klientů (např. „Babička Lužice“, „Děda Brno“).

Pairing New: Možnost kdykoliv naskenovat další QR kód a přidat nové zařízení pod jeden účet.

Web Bridge: Firestore slouží jako centrální uzel. Webová aplikace (pro vnoučata) přistupuje ke stejným dokumentům jako mobilní admin app, umožňující posílání vzkazů bez instalace.

3. Bezpečnost & Admin PIN
Vzdálená správa PINu: Admin vidí aktuální PIN ve svém dashboardu a může jej přepsat. Změna se okamžitě synchronizuje do tabletu.

Kiosk Break: Vstup do nastavení na tabletu je chráněn skrytým gestem (5x tap v rohu) a následným zadáním tohoto PINu.

Hardcoded Fallback: Pro případ totálního výpadku sítě obsahuje kód tajné univerzální heslo pro technika (offline bypass).

4. Modul: Emergency Voice Proxy („Matěj“)
Aktivace: Lokální Wake Word engine (Porcupine) naslouchá na jméno „Matěj“ (nastavitelné adminem).

Inteligentní Eskalace:

Analýza: Gemini Nano vyhodnotí závažnost (např. pád, bolest na hrudi).

Hlasitý hovor: Systém vytočí číslo admina (Jirky) s automatickým reprodutorem.

AI Proxy: Pokud admin hovor zvedne, ale v místnosti je ticho, TTS Matěj nahlásí: „Tady Matěj, asistent paní Novákové. Detekoval jsem krizovou situaci a spojuji vás.“

Offline priorita: Volání Jirky musí fungovat i při výpadku dat (přes GSM/LTE hovor).

5. Technický Stack & Data Sync
Konektivita: Primární LTE (SIM) s detekcí kvality signálu.

Firebase Structure:

/devices/{deviceId}/config: Obsahuje admin_pin, sim_number, assistant_name, paired_admin_id.

/devices/{deviceId}/status: Online/Offline stav, baterie, síla signálu.

/devices/{deviceId}/messages: Kolekce pro vzkazy (přístupná i z webu).

6. Realizační instrukce pro Cursor (Composer 2)
Krok 1: Inicializace Role-Based Setupu
"Vytvoř startovací aktivitu, která se zeptá na roli (Senior/Admin). Pokud Senior, vygeneruj náhodný 4místný PIN a zobraz QR kód s ID zařízení. Pokud Admin, otevři skener a po úspěšném skenu ulož propojení do Firestore pod currentUserId."

Krok 2: Dashboard s PINem (Admin App)
"Vytvoř v admin části obrazovku 'Správa zařízení'. Zobraz zde náhodně vygenerovaný PIN z připojeného tabletu. Přidej možnost tento PIN upravit a odeslat změnu zpět do Firebase."

Krok 3: Implementace skrytého vstupu (Tablet)
"V Zero UI (černá plocha) vytvoř neviditelnou zónu 100x100dp. Po 5 rychlých klepnutích vyvolej PIN keypad. Porovnej zadaný kód s PINem uloženým ve Firebase. Při úspěchu ukonči Kiosk mód a otevři systémové nastavení."

Krok 4: Webová synchronizace
"Zajisti, aby vzkazy poslané z webové aplikace (přes Firestore kolekci 'messages') vyvolaly na tabletu okamžitou notifikaci přes celou obrazovku s potvrzením o přečtení zpět do databáze."










Technický dodatek: Hybridní AI Edge-Hub & Matter Integration
1. Hardware a systémové parametry
Cílové zařízení: Lenovo Tab M11 (LTE, 8GB RAM, 128GB Storage).

Konektivita: Primární LTE (SIM) pro nezávislost na Wi-Fi; Bluetooth/Thread pro Matter.

Optimalizace RAM: Využití 8GB RAM pro rezidentní AI služby a minimalizaci swapování při běhu Kiosk módu.

2. Architektura AI: Hybrid Intelligence Model
Systém využívá dvouúrovňové zpracování přirozeného jazyka (NLP) pro zajištění maximální odezvy a hloubky konverzace.

A. Lokální vrstva (On-Device AI - Gemini Nano)
Funkce: Zpracování systémových příkazů (Intent Recognition), ovládání UI a dotazy na lokální data.

Využití: "Chci volat", "Jaká je teplota v pokoji?", "Zhasni světlo".

Technologie: Android AICore.

Výhoda: Nulová latence, funkčnost v offline režimu, ochrana soukromí u rutinních povelů.

B. Cloudová vrstva (Cloud API - Gemini Flash/Pro)
Funkce: Komplexní empatická konverzace, analýza aktuálního dění (RSS, zprávy), hluboká paměť.

Využití: Diskuze o tématech, vysvětlování složitých pojmů, sociální interakce.

Technologie: Google AI SDK (Vertex AI / Gemini API).

Mechanismus: Pokud lokální model vyhodnotí Confidence Score < 0.7 pro systémový příkaz, nebo detekuje konverzační záměr, deleguje požadavek do cloudu.

3. Integrace Matter & Smart Home
Tablet funguje jako Matter Controller a vizualizační terminál.

Protokol: Implementace Matter přes Google Home Mobile SDK.

Senzorika: Primární integrace lokální meteostanice (teplota, vlhkost, tlak) a senzorů prostředí.

Data Processing:

Surová data ze senzorů jsou transformována do přirozeného jazyka (např. 21.5°C -> "V obýváku je příjemně").

Contextual Alerting: Pokud senzor detekuje anomálii (např. pokles teploty pod 15°C), AI vygeneruje upozornění na dashboard a informuje administrátora (vzdálené dítě) přes Firebase.

4. UI/UX: Zero UI & High Contrast Dashboard
Designový systém eliminující kognitivní zátěž.

Vizuální styl: Pure Black (#000000) pozadí pro maximální kontrast a šetření zraku (OLED/LCD optimalizace).

Typografie: Sans-serif, tučné písmo, dynamická velikost textu podle délky zprávy.

Prvky:

Permanentní stav: Čas, datum, lokální počasí (z Matter senzoru).

Konverzační vrstva: Textový přepis řeči AI v reálném čase (přístupnost pro nedoslýchavé).

Persistence: Konverzace se po ukončení mažou, ale extrahovaná fakta (sémantická paměť) se ukládají do šifrovaného JSON profilu.

5. Správa napájení a odolnost
Boot-on-Charge: Konfigurace bootloaderu pro automatický start po připojení k napájení (řešení totálního vybití).

Magnetická konektivita: Implementace fyzické USB-C redukce s magnetickým konektorem pro snadnou manipulaci.

Admin Watchdog: Služba na pozadí monitorující zdraví aplikace. V případě pádu nebo zamrznutí provede restart UI a odešle diagnostiku administrátorovi.

Poznámka pro Cursor (Composer 2):
"Při generování kódu dbej na to, aby veškeré operace s Gemini Nano byly zapouzdřeny tak, aby při nedostupnosti modelu systém transparentně přešel na Cloud API. UI musí zůstat plně reaktivní i při slabém LTE signálu."








************************************************************************************************************
Technický Brief: Emergency Voice Proxy (Modul „Matěj“)
************************************************************************************************************

1. Účel funkce
Poskytnout seniorovi okamžitou hlasovou pomoc v situaci fyzické tísně, kdy není schopen ovládat dotykové rozhraní. Systém kombinuje detekci klíčového slova (Wake Word), analýzu kontextu pomocí AI a inteligentní eskalaci hovoru.

2. Scénář fungování (User Flow)
Aktivace: Uživatel vysloví jméno asistenta (např. „Matěji!“).

Sběr dat: Systém signalizuje naslouchání (vizuální pulzování okraje displeje) a nahrává hlasový vstup.

Analýza (Gemini Nano): AI vyhodnotí sémantiku. Pokud detekuje krizová slova (zle, srdce, sanitka, pomoc, bolest), aktivuje Emergency protokol.

Eskalace:

Systém vyhledá v lokální DB prioritní kontakt (např. „Jirka“).

Vytočí hovor přes systémové API s automatickým zapnutím hlasitého odposlechu (Speakerphone).

AI Proxy: Pokud druhá strana hovor přijme, ale uživatel nemluví, AI Matěj převezme slovo a nahlásí stav nouze syntetickým hlasem.

3. Technické zadání pro vývoj
A. Wake Word Engine (Always-on)
Technologie: Integrace Picovoice Porcupine nebo PocketSphinx (běží lokálně, nízká spotřeba).

Parametr: Dynamické klíčové slovo definované v nastavení (String variable: assistant_name).

Priorita: Služba musí běžet jako Foreground Service s vysokou prioritou, aby ji Android neukončil.

B. Emergency Logic Controller (Kotlin)
Intent Recognition: Prompt pro Gemini Nano:

"Analyzuj následující text a urči: 1. Úroveň nouze (0-10), 2. Jméno osoby k zavolání, 3. Stručný popis potíží. Vstup: [User Audio Transcript]"

Telecom Integration:

Využití TelecomManager pro iniciaci hovoru.

Programové vynucení audio výstupu do AudioManager.STREAM_VOICE_CALL s aktivním setSpeakerphoneOn(true).

C. AI Voice Interaction (TTS)
Engine: Android TextToSpeech (TTS).

Obsah zprávy: Dynamicky generovaná věta:

"Dobrý den, tady [Jméno asistenta], asistent uživatele [Jméno uživatele]. Právě mi nahlásil zdravotní potíže: [Popis potíží]. Prosím, mluvte, babička vás slyší na hlasitý odposlech."

D. Firebase Cloud Sync (Admin Reporting)
Okamžitý zápis: Jakmile dojde k detekci nouze, aplikace zapíše do kolekce incidents dokument s časovým razítkem a přepisem volání.

Push Notifikace: Firebase Cloud Messaging (FCM) odešle adminovi (synovi) notifikaci s vysokou prioritou, která obejde režim "Nerušit".

4. Bezpečnostní a technická omezení
Oprávnění: Aplikace musí mít status Default Dialer nebo Device Owner, aby mohla ovládat hovory bez interakce uživatele.

Offline schopnost: Rozpoznání jména ("Matěji") a vytočení kontaktu Jirka musí fungovat i bez internetu (pouze přes GSM). Cloud AI se využívá jen pro vylepšení konverzace, nikoliv pro samotné vytočení pomoci.

Vzdálená konfigurace: Jméno asistenta a seznam nouzových kontaktů se synchronizují z Firebase, aby je senior nemohl omylem smazat.

Prompt pro Cursor k zahájení vývoje této funkce:
"Vytvoř Foreground Service v Kotlinu, která na pozadí naslouchá na klíčové slovo 'Matěj'. Jakmile je detekováno, spusť nahrávání mikrofonu na 5 sekund a text pošli do funkce pro analýzu záměru (mockuj Gemini Nano volání). Pokud analýza vrátí 'emergency', iniciuj hovor na definované číslo a po 3 sekundách od přijetí hovoru přehraj přes TTS informaci o nouzovém stavu."