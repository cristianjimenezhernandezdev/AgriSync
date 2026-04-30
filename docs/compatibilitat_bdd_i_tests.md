# Compatibilitat BDD i tests de regressio

## Context

La base de dades real no sempre coincideix amb l'esquema documentat al repo. Durant la revisio han aparegut tres casos reals:

- `aplicacions_fertilitzants.entrega_id` no existeix
- `public.granja_campanya_balance` no existeix o no es veu al `schema cache`
- `entrega_dejeccions.tipus_fertilitzant` no existeix

Quan la UI feia consultes o escriptures assumint aquests elements, PostgREST retornava errors com:

- `42703` per columna inexistent
- `PGRST200` per relacio no resolta
- `PGRST205` per taula no trobada al `schema cache`

## Solucio aplicada

S'ha creat [SchemaCompatibility.kt](/C:/Cristian/DAM2/AgriSynct/composeApp/src/commonMain/kotlin/cat/agrisync/data/SchemaCompatibility.kt) per centralitzar:

- selectors PostgREST compatibles
- deteccio de taula absent al `schema cache`
- deteccio de columna absent
- deteccio de relacio absent

### Canvis de runtime

- Modul agricola:
  - no llegeix `entrega_id` a les aplicacions
  - mostra missatge controlat si reapareix error de relacio o columna antiga

- Modul ramader:
  - no llegeix ni envia `tipus_fertilitzant` a `entrega_dejeccions`
  - si falta `granja_campanya_balance`, la pantalla continua obrint amb el bloc de balanc desactivat

- Preparar DAN:
  - mateix mode compatible per `entrega_id`, `tipus_fertilitzant` i `granja_campanya_balance`

## Tests afegits

Fitxer: [SchemaCompatibilityTest.kt](/C:/Cristian/DAM2/AgriSynct/composeApp/src/jvmTest/kotlin/cat/agrisync/data/SchemaCompatibilityTest.kt)

Cobertura:

- detecta `PGRST205` per `granja_campanya_balance`
- detecta `42703` per `tipus_fertilitzant`
- detecta `PGRST200` per relacions absents
- garanteix que els `select` compatibles no tornen a demanar:
  - `aplicacions_fertilitzants.entrega_id`
  - `entrega_dejeccions.tipus_fertilitzant`
- garanteix que la serialitzacio JSON de `EntregaCreateRequest` i `EntregaUpdateRequest` no envia `tipus_fertilitzant`
- garanteix que `AplicacioCreateRequest` no envia `entrega_id` quan es `null`

## Com executar

Compilacio:

```powershell
.\gradlew.bat :composeApp:compileKotlinJvm
```

Tests JVM:

```powershell
.\gradlew.bat :composeApp:jvmTest
```

Només tests de compatibilitat:

```powershell
.\gradlew.bat :composeApp:jvmTest --tests "cat.agrisync.data.SchemaCompatibilityTest"
```

## Proces recomanat quan surt un nou error de BDD

1. Copiar el missatge complet de PostgREST.
2. Classificar-lo:
   - `42703`: columna absent
   - `PGRST200`: relacio absent
   - `PGRST205`: taula absent al `schema cache`
3. Buscar el `select` o `request` afectat al repositori.
4. Moure la regla a `SchemaCompatibility.kt` si encara no hi es.
5. Afegir o ampliar un test a `SchemaCompatibilityTest.kt`.
6. Compilar i executar `:composeApp:jvmTest`.
7. Si la funcionalitat es opcional, aplicar fallback i deixar la resta de pantalla operativa.

## Criteri de compatibilitat

Preferencia actual:

- no bloquejar pantalles senceres per una columna o taula opcional
- degradar funcionalitat concreta quan la base real no suporta un camp
- deixar tests de contracte perquè cap camp antic torni a entrar per error
