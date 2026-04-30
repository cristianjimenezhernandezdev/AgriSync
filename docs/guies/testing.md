# Procediment de Testing — AgriSync

## 1. Context i objectiu

AgriSync és una aplicació Kotlin Multiplatform Desktop per a la gestió agrícola i ramadera de tècnics. El framework de testing triat és **JUnit 5** (JUnit Jupiter), integrat amb el sistema de tests de Kotlin (`kotlin.test`), i executat via Gradle.

L'objectiu del sistema de tests és verificar que la **lògica de negoci pura** de l'aplicació funciona correctament de forma aïllada: sense necessitat d'engegar la UI, ni connectar-se a Supabase, ni tenir cap dependència externa. Això permet detectar errors de càlcul i de format de manera ràpida i repetible, i garantir que els canvis futurs no trenquen comportaments existents (tests de regressió).

---

## 2. Mòduls sota test

S'han triat els dos fitxers de lògica pura del paquet `util/`:

| Fitxer font | Paquet | Funció al projecte |
|---|---|---|
| `util/NitrogenMath.kt` | `cat.agrisync.util` | Càlculs del DAN: valida i resol el triplet kgN / volum m³ / concentració kgN/m³ que emplen els tècnics als formularis d'aplicació de dejeccions ramaderes |
| `util/DateFormats.kt` | `cat.agrisync.util` | Normalització i conversió de dates entre format visual (dd/MM/yyyy), format d'entrada de l'usuari i format ISO 8601 (yyyy-MM-dd) que emmagatzema Supabase |

Aquests mòduls no depenen de UI, Compose, ni Ktor: són funcions pures ideals per a tests unitaris.

---

## 3. Stack de testing

| Component | Versió | Paper |
|---|---|---|
| Kotlin Multiplatform | 2.3.0 | Plataforma base del projecte |
| JUnit Jupiter (`junit-jupiter`) | 5.10.2 | Motor d'execució de tests JUnit 5 |
| `kotlin("test")` | inclòs amb Kotlin | Assertions neutres: `assertEquals`, `assertNull`, `assertNotNull`, `assertTrue` |
| Gradle | 8.14+ | Compila i executa els tests |

---

## 4. Configuració: arxius modificats

### 4.1 `composeApp/build.gradle.kts`

**a) Dependències de test per al target JVM** (dins del bloc `kotlin > sourceSets`):

```kotlin
jvmTest.dependencies {
    implementation(kotlin("test"))
    implementation("org.junit.jupiter:junit-jupiter:5.10.2")
}
```

- `kotlin("test")` proporciona les funcions d'asserció compatibles amb múltiples motors.
- `junit-jupiter` és el motor JUnit 5 que permet executar els tests via `useJUnitPlatform()`.

En KMP ja existia `commonTest.dependencies` amb `kotlin.test`, però calia afegir JUnit 5 explícitament al `jvmTest` perquè el target JVM requereix el motor de plataforma.

**b) Activació de JUnit Platform** (fora del bloc `kotlin`):

```kotlin
tasks.withType<Test> {
    useJUnitPlatform()
}
```

Sense aquesta línia, Gradle ignora JUnit 5 o usa el motor antic de JUnit 4.

### 4.2 `build.gradle.kts` (arrel del projecte)

Alias per al task `test` des de l'arrel:

```kotlin
tasks.register("test") {
    dependsOn(":composeApp:jvmTest")
}
```

En KMP el task es diu `jvmTest`. Sense l'alias, `.\gradlew test` des de l'arrel dona error. Amb ell, ambdues formes funcionen:

```powershell
.\gradlew test                   # des de l'arrel (via alias)
.\gradlew :composeApp:jvmTest    # directe al subprojecte
```

---

## 5. Estructura de carpetes de test

```
composeApp/
└── src/
    └── jvmTest/
        └── kotlin/
            └── cat/
                └── agrisync/
                    ├── ComposeAppDesktopTest.kt      ← test de plantilla (conservat)
                    ├── data/
                    │   └── SchemaCompatibilityTest.kt ← 17 tests de compatibilitat BDD
                    └── util/
                        ├── NitrogenMathTest.kt        ← 24 tests de lògica DAN
                        └── DateFormatsTest.kt         ← 24 tests de dates
```

---

## 6. Tests implementats

### 6.0 `SchemaCompatibilityTest.kt` — 17 tests

**Ruta:** `composeApp/src/jvmTest/kotlin/cat/agrisync/data/SchemaCompatibilityTest.kt`

Cobreix la capa de **compatibilitat d'esquema** entre el codi Kotlin i la base de dades Supabase real. Quan la BDD no coincideix exactament amb l'esquema esperat (columnes que falten, taules opcionals, relacions inexistents), el codi ha de detectar-ho i retornar missatges d'error intel·ligibles en lloc de mostrar el JSON brut de PostgreSQL.

**Origen real d'aquest test:** error en producció `column entrega_dejeccions.volum_m3 does not exist` (codi PostgreSQL 42703), causat perquè la BDD de Supabase fou creada amb una versió anterior de l'esquema que no incloïa les columnes `volum_m3`, `kg_n_m3`, `kg_n` a la taula `entrega_dejeccions`.

| Grup | Tests | Cobertura |
|---|---|---|
| Taula absent a la caché | 2 | `isMissingSchemaCacheTable` — detecció i no-fals-positiu |
| Columna absent `tipus_fertilitzant` | 1 | Error legacy conegut |
| **Columna absent `volum_m3`** | **4** | **Error real reportat: detecció, sense filtre de taula, fals negatiu, null** |
| Relació absent | 1 | `isMissingRelationship` |
| Selects segurs (camp legacy absent) | 2 | No contenen camps legacy als SELECTs |
| **Selects de fallback (sense volum)** | **3** | **Fallbacks ometen `volum_m3/kg_n_m3/kg_n`, conserven FK** |
| Serialització JSON | 4 | Requests no inclouen camps legacy al body |

#### Correcció aplicada a la BDD

L'error `volum_m3 does not exist` s'arregla executant la migració SQL:

```
docs/sql/maintenance/add_volum_nitrogen_columns.sql
```

```sql
alter table public.entrega_dejeccions
    add column if not exists volum_m3 numeric check (volum_m3 is null or volum_m3 >= 0),
    add column if not exists kg_n_m3  numeric check (kg_n_m3  is null or kg_n_m3  >= 0),
    add column if not exists kg_n     numeric check (kg_n     is null or kg_n     >= 0);
```

#### Codi afegit a `SchemaCompatibility.kt`

```kotlin
const val legacyEntregaVolumField = "volum_m3"

const val ramaderEntregaSelectNoVolum =
    "?select=id,data,granja_origen_id,terra_desti_id,updated_at,updated_by,dan:dan_id(...)..."

const val danPreparationEntregaSelectNoVolum =
    "?select=id,data,granja_origen:granja_origen_id(...)..."
```

#### Codi afegit als ViewModels

A `TitularRamaderViewModel` i `DanPreparationViewModel`, dins de `mapHttpError`:

```kotlin
SchemaCompatibility.isMissingColumn(msg, SchemaCompatibility.legacyEntregaVolumField, "entrega_dejeccions") ->
    "La base de dades actual no te les columnes de volum/nitrogen a entrega_dejeccions. Cal executar la migracio SQL."
```

---
### 6.1 `NitrogenMathTest.kt` — 24 tests

**Ruta:** `composeApp/src/jvmTest/kotlin/cat/agrisync/util/NitrogenMathTest.kt`

Cobreix la funció central del DAN (Document d'Aplicació de Nitrogen): donats tres valors `kgN`, `volum m³` i `kgN/m³`, en qualsevol combinació de dos o tres camps, el sistema ha de poder resoldre el que falta o validar la consistència.

#### Grup `parseDecimalInput` (6 tests)

| Test | Entrada | Resultat esperat |
|---|---|---|
| accepta nombre enter | `"100"` | `100.0` |
| accepta decimal amb punt | `"3.5"` | `3.5` |
| accepta coma i converteix a punt | `"3,5"` | `3.5` |
| ignora espais laterals | `"  10  "` | `10.0` |
| retorna null si buit | `""` / `"   "` | `null` |
| retorna null si text no numèric | `"abc"` / `"12.3.4"` | `null` |

#### Grup `validateAndResolveNitrogenTriplet` (14 tests)

| Test | Situació |
|---|---|
| `resol kgN donat volum i concentracio` | 50 m³ × 4 kgN/m³ = **200 kgN** |
| `resol volum donat kgN i concentracio` | 200 kgN / 4 kgN/m³ = **50 m³** |
| `resol concentracio donat kgN i volum` | 200 kgN / 50 m³ = **4 kgN/m³** |
| `accepta tres camps consistents` | 200 / 50 / 4 → OK |
| `rebutja tres camps inconsistents` | 200 / 50 / 4.1 → error "igual" |
| `error si nomes un camp informat` | Missatge "almenys 2" |
| `error si cap camp informat` | Error retornat |
| `error si volum es zero i cal calcular concentracio` | Missatge "superior a 0" |
| `error si concentracio es zero i cal calcular volum` | Missatge "superior a 0" |
| `error si kgN es negatiu` | Missatge "negatius" |
| `error si volum es negatiu` | Missatge "negatius" |
| `error si kgN no es numeric` | Missatge "Kg N" |
| `error si volum no es numeric` | Missatge "volum" |
| `error si concentracio no es numerica` | Missatge "kg N/m3" |

#### Grup `autofillNitrogenTexts` (4 tests)

| Test | Situació |
|---|---|
| `autofill omple kgN quan es dona volum i concentracio` | Resultat: `"200"` |
| `autofill omple volum quan es dona kgN i concentracio` | Resultat: `"50"` |
| `autofill omple concentracio quan es dona kgN i volum` | Resultat: `"4"` |
| `autofill no modifica res amb un sol camp informat` | Camps buits sense modificar |

---

### 6.2 `DateFormatsTest.kt` — 24 tests

**Ruta:** `composeApp/src/jvmTest/kotlin/cat/agrisync/util/DateFormatsTest.kt`

Cobreix tota la capa de dates que usa l'app: des de l'entrada de l'usuari fins a l'emmagatzematge a Supabase i la visualització als formularis.

#### Grup `normalizeDateInput` (6 tests)

| Test | Entrada | Resultat esperat |
|---|---|---|
| formata 8 dígits seguits | `"15062024"` | `"15/06/2024"` |
| ignora caràcters no numèrics | `"15-06-2024"` | `"15/06/2024"` |
| trunca a 8 dígits si en sobren | `"150620249999"` | `"15/06/2024"` |
| amb 4 dígits mostra dia i mes | `"1506"` | `"15/06"` |
| amb 2 dígits mostra només dia | `"15"` | `"15"` |
| buit retorna buit | `""` | `""` |

#### Grup `parseEnteredDateToIso` (6 tests)

| Test | Entrada | Resultat esperat |
|---|---|---|
| converteix dd/MM/yyyy a ISO | `"15/06/2024"` | `"2024-06-15"` |
| accepta ISO directament | `"2024-06-15"` | `"2024-06-15"` |
| retorna null si buit | `""` / `"   "` | `null` |
| retorna null amb dia 00 | `"00/06/2024"` | `null` |
| retorna null amb mes 13 | `"01/13/2024"` | `null` |
| retorna null amb text aleatori | `"no es una data"` | `null` |

#### Grup anys de traspàs (4 tests)

| Test | Entrada | Resultat esperat |
|---|---|---|
| accepta 29/02 en any de traspàs | `"29/02/2024"` | `"2024-02-29"` |
| rebutja 29/02 en any no de traspàs | `"29/02/2023"` | `null` |
| accepta any divisible per 400 (2000) | `"29/02/2000"` | `"2000-02-29"` |
| rebutja any divisible per 100 però no 400 (1900) | `"29/02/1900"` | `null` |

#### Grup `formatStoredDateForDisplay` i `formatStoredDateForInput` (6 tests)

| Test | Entrada | Resultat esperat |
|---|---|---|
| display: converteix ISO a visual | `"2024-06-15"` | `"15/06/2024"` |
| display: retorna guió si null | `null` | `"-"` |
| display: retorna guió si buit | `""` | `"-"` |
| input: converteix ISO per al camp | `"2024-06-15"` | `"15/06/2024"` |
| input: retorna buit si null | `null` | `""` |
| input: retorna buit si buit | `""` | `""` |

#### Grup round-trip (2 tests)

| Test | Flux |
|---|---|
| `round-trip display → ISO → display` | `"31/12/2025"` → `"2025-12-31"` → `"31/12/2025"` ✓ |
| `round-trip ISO → display → ISO` | `"2024-03-01"` → `"01/03/2024"` → `"2024-03-01"` ✓ |

---

## 7. Resultat obtingut

```
> Task :composeApp:compileTestKotlinJvm
> Task :composeApp:jvmTestClasses
> Task :composeApp:jvmTest

BUILD SUCCESSFUL in 14s
15 actionable tasks: 15 executed
```

| Suite | Tests | Passats | Fallats | Temps |
|---|---|---|---|---|
| `SchemaCompatibilityTest` | 17 | ✅ 17 | 0 | 0.052 s |
| `NitrogenMathTest` | 24 | ✅ 24 | 0 | 0.031 s |
| `DateFormatsTest` | 24 | ✅ 24 | 0 | 0.028 s |
| `ComposeAppDesktopTest` | 1 | ✅ 1 | 0 | 0.028 s |
| **Total** | **66** | **✅ 66** | **0** | — |

L'informe HTML complet es genera automàticament a:

```
composeApp/build/reports/tests/jvmTest/index.html
```

---

## 8. Com executar els tests

```powershell
# Des de l'arrel del projecte
cd C:\Cristian\DAM2\AgriSynct

# Execució estàndard
.\gradlew test

# Forçar re-execució (sense caché)
.\gradlew :composeApp:jvmTest --rerun-tasks

# Amb sortida verbose
.\gradlew :composeApp:jvmTest --info
```

Des de l'IDE, clic dret sobre qualsevol fitxer de test → **Run**.

---

## 9. Com afegir nous tests

1. Crear un fitxer `.kt` a `composeApp/src/jvmTest/kotlin/cat/agrisync/` (o subpaquet).
2. Anotar els mètodes amb `@Test`.
3. Usar `assertEquals`, `assertNull`, `assertNotNull`, `assertTrue` de `kotlin.test`.
4. Executar `.\gradlew test`.

La convenció de noms dels tests segueix el patró de frase descriptiva amb backticks:

```kotlin
@Test
fun `descripció clara del comportament esperat`() { ... }
```

---

## 10. Decisions tècniques

| Decisió | Motiu |
|---|---|
| Tests sobre `util/` (no sobre ViewModels) | Els ViewModels depenen de `StateFlow` i corutines; els utils són funcions pures sense dependències externes |
| JUnit 5 en lloc de JUnit 4 | Estàndard actual, millor suport a IntelliJ, permet extensions futures (parametritzats, mocks) |
| `kotlin.test` com a capa d'assertions | Neutral respecte al motor; funciona igual si en el futur es canvia de JUnit |
| Tests a `jvmTest` (no `commonTest`) | Totes les dependències del projecte són JVM-specific; `commonTest` no pot resoldre les mateixes llibreries |
| Alias `test` a l'arrel | Ergonomia: `.\gradlew test` és la convenció habitual en projectes Kotlin/JVM estàndard |
| Smart-cast après `assertNotNull` | Kotlin fa smart-cast automàticament després d'una asserció de no-nul·litat; no cal `!!` en el codi de test |
