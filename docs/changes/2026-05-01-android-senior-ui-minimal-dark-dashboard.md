# 2026-05-01 — Android Senior UI: minimalistický dark dashboard

## Co se změnilo

- Senior dashboard je přepsaný na jednodušší tmavé UI se třemi hlavními režimy:
  - **Domů**
  - **Volání**
  - **Zprávy**
- Levé menu má větší zaoblené ikonové položky a výrazný aktivní stav.
- **Domů**:
  - levý obsahový sloupec ukazuje poslední zprávy a kontaktní položky volání,
  - zprávy jsou velkým písmem a zkrácené na 3 řádky,
  - klepnutí na zprávu otevře detailový overlay,
  - pravý sloupec ukazuje počasí, nejčastější kontakty a nejnovější fotky.
- **Volání**:
  - vedle menu je svislý seznam kontaktů s první položkou **Všechny kontakty**,
  - výběr kontaktu připraví rychlé volání,
  - místo čtení systémového call logu je zatím jasný placeholder pro další krok.
- **Zprávy**:
  - stejný kontaktní seznam jako ve Volání,
  - výběr kontaktu filtruje zprávy podle vlákna,
  - pod zprávami je velké pole pro napsání nové zprávy a tlačítko odeslat.
- Tlačítko **Nový** ve Volání i Zprávách otevírá formulář kontaktu.
- Formulář kontaktu má pole **Jméno**, dropdown národního předčíslí a telefonní číslo.

## Poznámky

- Odesílání zpráv ve stránce **Zprávy** používá stejnou cestu jako dosavadní SMS overlay: klasická SMS, nebo cloudová náhrada podle schopností zařízení.
- Detail zprávy umí odpovědět přímo u zpráv, které lze přiřadit ke kontaktu. Rodinné/admin vzkazy bez kontaktu zatím zůstávají jen k přečtení.
