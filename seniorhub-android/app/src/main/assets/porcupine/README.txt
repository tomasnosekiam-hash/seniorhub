Vlastní české klíčové slovo (např. „Matěj“):
1) Získejte Picovoice Access Key (https://console.picovoice.ai/) a vložte ho do kořenového souboru local.properties jako řádek:
   picovoice.access.key=VÁŠ_KLÍČ
2) V Picovoice Console vytvořte vlastní .ppn pro Android a soubor uložte sem jako:
   keyword.ppn
   (přesná cesta v APK: assets/porcupine/keyword.ppn)

Pokud keyword.ppn chybí, aplikace použije vestavěné anglické slovo „Porcupine“ — vhodné jen pro ověření toku.
