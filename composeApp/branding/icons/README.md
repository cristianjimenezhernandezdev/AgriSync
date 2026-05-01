Branding desktop AgriSync

Posa aqui les icones finals de paquet:

- `app-icon.ico`
  Icona de l'instal·lador i app a Windows (`MSI`)
- `app-icon.icns`
  Icona per macOS (`DMG`)
- `app-icon.png`
  Icona per Linux (`DEB`)

Notes:

- Pots deixar nomes els fitxers del sistema que vagis a construir.
- La configuracio Gradle nomes aplica cada icona si el fitxer existeix.
- Per a la icona visible dins la finestra de l'app, fes servir el recurs:
  `composeApp/src/jvmMain/composeResources/drawable/app-icon.svg`
- Si prefereixes `PNG` per a la finestra, elimina `app-icon.svg` i posa un unic fitxer amb el mateix nom base:
  `composeApp/src/jvmMain/composeResources/drawable/app-icon.png`
