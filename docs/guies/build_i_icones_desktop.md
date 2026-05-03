# Build i icones desktop

Guia curta per preparar AgriSync abans de generar l'instal·lador.

## 1. Fitxers que has de tenir

### Instal·lador i app per sistema operatiu

- `composeApp/branding/icons/app-icon.ico`
  Per Windows `MSI`
- `composeApp/branding/icons/app-icon.png`
  Per Linux `DEB`

### Icona visible dins la finestra de l'app

- `composeApp/src/jvmMain/composeResources/drawable/app-icon.png`

Important:

- Mantingues nomes un fitxer `app-icon` dins `drawable/` per evitar conflictes de recursos.
- Si afegeixes una altra variant d'icona, comprova que no entri en conflicte amb `composeApp/src/jvmMain/composeResources/drawable/app-icon.png`.

## 2. Que ja queda preparat al projecte

- La finestra desktop carrega `Res.drawable.app_icon`
- El paquet Windows mira `composeApp/branding/icons/app-icon.ico`
- El paquet Linux mira `composeApp/branding/icons/app-icon.png`
- Si algun fitxer no existeix, Gradle no falla per aquesta part; simplement no aplica aquella icona

## 3. Pas recomanat per al teu cas a Windows

Substitueix aquests fitxers:

1. `composeApp/src/jvmMain/composeResources/drawable/app-icon.png`
2. `composeApp/branding/icons/app-icon.ico`

Opcional si algun dia vols empaquetar per altres sistemes:

1. `composeApp/branding/icons/app-icon.png`

## 4. Com provar-ho abans del build

Executa:

```powershell
.\gradlew.bat :composeApp:run
```

Comprova:

- icona de la finestra
- icona de la barra de tasques
- nom de l'app

## 5. Build a Windows

Per generar nomes l'instal·lador Windows:

```powershell
.\gradlew.bat :composeApp:packageMsi
```

Sortida habitual:

- `composeApp/build/compose/binaries/main/msi/`

Si vols la imatge distribuible sense instal·lador:

```powershell
.\gradlew.bat :composeApp:createDistributable
```

Sortida habitual:

- `composeApp/build/compose/binaries/main/app/`

## 6. Notes importants

- `DMG` nomes es pot generar des de macOS
- `DEB` normalment s'ha de generar des de Linux
- Des de Windows, centra't en `packageMsi`
- Si canvies icones i no les veus, tanca del tot l'app i torna a generar el paquet
