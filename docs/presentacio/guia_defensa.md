# Guia de defensa

## Missatge central

AgriSync es un MVP desktop per centralitzar la DAN amb autenticacio real, permisos reals a la base de dades i separacio clara entre UI, logica i dades.

## Ordre recomanat

1. problema real que resol
2. arquitectura client + Supabase
3. model de permisos amb RLS
4. recorregut d'usuari des del login fins a `Preparar DAN`
5. decisions de disseny i limits del MVP

## Punts forts a destacar

- no es un mock: hi ha Auth real i BDD real
- el control d'acces no depen nomes de la UI
- el projecte treballa per campanya
- el domini esta unificat al voltant del titular
- hi ha gestio administrativa i operativa en el mateix producte

## Fitxers que convé tenir controlats

- `composeApp/src/commonMain/kotlin/cat/agrisync/App.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/RestClient.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/TecnicRepository.kt`
- `docs/sql/schema/agrisync_schema.sql`
- `docs/sql/seeds/agrisync_demo_seed.sql`

## Resposta curta per preguntes dificils

Si et pregunten perque no has intentat cobrir tota la DAN oficial:

> Vaig prioritzar un MVP robust amb dades coherents, autenticacio real i permisos reals abans que un producte molt gran pero fragil.
