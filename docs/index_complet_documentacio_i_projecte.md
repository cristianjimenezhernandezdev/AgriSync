# Index complet de documentacio i estructura del projecte

## Objectiu

Aquest document defineix en un sol lloc:

- l'estructura completa de la documentacio
- la funcio de cada document
- l'estructura principal de carpetes i fitxers del projecte
- quins directoris son codi font, configuracio, documentacio o generats

## 1. Estructura de la documentacio

```text
docs/
|-- README.md
|-- index_complet_documentacio_i_projecte.md
|-- arquitectura/
|   |-- arquitectura_i_codi.md
|   `-- permisos_i_seguretat.md
|-- exemples/
|   |-- dan_agricola_exemple.pdf
|   `-- dan_ramadera_exemple.pdf
|-- funcional/
|   `-- flux_operatiu_i_moduls.md
|-- guies/
|   `-- instalacio_i_demo.md
|-- presentacio/
|   `-- guia_defensa.md
|-- projecte/
|   `-- estat_actual_i_roadmap.md
`-- sql/
    |-- README.md
    |-- maintenance/
    |   |-- reaplica_permisos.sql
    |   |-- reset_auth_seed_users.sql
    |   `-- resincronitza_tecnic_user_ids.sql
    |-- schema/
    |   `-- agrisync_schema.sql
    `-- seeds/
        `-- agrisync_demo_seed.sql
```

## 2. Funcio de cada document

### `docs/README.md`

Index curt de la documentacio. Serveix com a porta d'entrada rapida.

### `docs/index_complet_documentacio_i_projecte.md`

Index mestre. Explica com esta organitzat `docs/` i com esta organitzat el projecte sencer.

### `docs/arquitectura/arquitectura_i_codi.md`

Mapa tecnic de l'aplicacio:

- capes
- navegacio
- peces principals del codi
- decisions d'arquitectura

### `docs/arquitectura/permisos_i_seguretat.md`

Model de seguretat:

- rols
- scopes
- RLS
- funcions helper SQL
- scripts de suport de permisos i Auth

### `docs/funcional/flux_operatiu_i_moduls.md`

Explica el funcionament del producte:

- flux d'us principal
- home de titulars
- modul agricola
- modul ramader
- `Preparar DAN`
- gestio administrativa

### `docs/guies/instalacio_i_demo.md`

Guia practica per:

- reconstruir el projecte en Supabase
- crear usuaris demo
- executar esquema i seed
- configurar l'app
- provar la demo

### `docs/presentacio/guia_defensa.md`

Guia resumida per a defensa del projecte:

- missatge central
- ordre de defensa
- punts forts
- fitxers clau que cal dominar

### `docs/projecte/estat_actual_i_roadmap.md`

Document de seguiment del projecte:

- estat actual del MVP
- punts tancats
- limits actuals
- roadmap curt

### `docs/sql/README.md`

Index del paquet SQL:

- esquema
- seed
- manteniment
- ordre d'execucio recomanat

### `docs/sql/schema/agrisync_schema.sql`

Esquema principal de la base de dades:

- enums
- taules
- `titular` amb `nif`, `telefon`, `email`, `adreca` i `codi_postal`
- triggers d'auditoria
- funcions helper
- grants
- policies RLS

### `docs/sql/seeds/agrisync_demo_seed.sql`

Seed principal de demo. Carrega dades representatives del MVP, incloses dades de contacte del titular.

### `docs/sql/maintenance/reaplica_permisos.sql`

Script auxiliar per reaplicar grants i execucio de funcions.

### `docs/sql/maintenance/resincronitza_tecnic_user_ids.sql`

Script auxiliar per tornar a quadrar `public.tecnic.user_id` amb `auth.users.id`.

### `docs/sql/maintenance/reset_auth_seed_users.sql`

Script auxiliar per eliminar els usuaris demo coneguts abans de recrear-los.

### `docs/exemples/dan_agricola_exemple.pdf`

Exemple de referencia documental de DAN agricola.

### `docs/exemples/dan_ramadera_exemple.pdf`

Exemple de referencia documental de DAN ramadera.

## 3. Estructura principal del projecte

```text
AgriSynct/
|-- README.md
|-- build.gradle.kts
|-- settings.gradle.kts
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
|-- agrisync.properties
|-- agrisync.properties.example
|-- .env.local
|-- composeApp/
|   |-- build.gradle.kts
|   |-- agrisync.properties
|   `-- src/
|       |-- commonMain/
|       |   `-- kotlin/cat/agrisync/
|       |       |-- App.kt
|       |       |-- data/
|       |       |-- ui/
|       |       `-- viewmodel/
|       |-- jvmMain/
|       |   |-- composeResources/
|       |   `-- kotlin/cat/agrisync/
|       |       |-- main.kt
|       |       |-- Platform.kt
|       |       |-- Greeting.kt
|       |       `-- data/
|       `-- jvmTest/
|           `-- kotlin/cat/agrisync/
|               `-- ComposeAppDesktopTest.kt
|-- docs/
|   `-- ...
|-- gradle/
|   `-- wrapper/
|-- build/
|-- .gradle/
|-- .gradle-user-home/
|-- .kotlin/
|-- .idea/
|-- .run/
|-- SupabaseProbe.java
`-- SupabaseProbe.class
```

## 4. Funcio de les carpetes i fitxers principals del projecte

### Arrel del projecte

#### `README.md`

Punt d'entrada rapid del repositori. Redirigeix a `docs/`.

#### `build.gradle.kts`

Configuracio Gradle arrel del projecte.

#### `settings.gradle.kts`

Defineix els moduls Gradle del projecte.

#### `gradle.properties`

Propietats generals de Gradle.

#### `gradlew` i `gradlew.bat`

Wrappers per executar Gradle sense instal.lacio manual global.

#### `agrisync.properties`

Configuracio local real de l'aplicacio. Pot contenir claus i URL d'entorn.

#### `agrisync.properties.example`

Plantilla segura per recrear `agrisync.properties`.

#### `.env.local`

Fitxer local d'entorn. Utilitat de configuracio/desenvolupament.

## 5. Modul `composeApp`

### Objectiu

Conté l'aplicacio desktop Compose Multiplatform.

### `composeApp/build.gradle.kts`

Configuracio Gradle del modul d'aplicacio.

### `composeApp/src/commonMain/kotlin/cat/agrisync/App.kt`

Entrada funcional de l'app:

- carrega configuracio
- inicialitza Auth
- decideix pantalles
- munta serveis i navegacio

### `composeApp/src/commonMain/kotlin/cat/agrisync/data/`

Capa de dades i serveis:

- `AuthService.kt`
- `RestClient.kt`
- repositoris d'acces a Supabase
- DTOs i models del MVP
- utilitats de configuracio i serialitzacio

### `composeApp/src/commonMain/kotlin/cat/agrisync/ui/`

Pantalles Compose del producte:

- login
- titulars
- modul agricola
- modul ramader
- preparar DAN
- oficines
- tecnics
- titulars
- terres
- perfil

### `composeApp/src/commonMain/kotlin/cat/agrisync/viewmodel/`

Logica d'estat de pantalla:

- carga de dades
- validacions basiques
- missatges d'error
- coordinacio d'accions entre UI i repositoris

### `composeApp/src/commonMain/kotlin/cat/agrisync/ui/navigation/Screen.kt`

Definicio de pantalles i rutes internes del client.

### `composeApp/src/jvmMain/`

Implementacio especifica de desktop/JVM:

- `main.kt`
- lectura de configuracio JVM
- persistencia local de sessio
- client HTTP JVM
- recursos Compose

### `composeApp/src/jvmTest/`

Tests JVM del modul desktop.

## 6. Directori `docs`

Documentacio funcional, tecnica, de projecte i SQL. Es la font de veritat documental del repositori.

## 7. Directori `gradle/wrapper`

Fitxers del wrapper de Gradle necessaris per executar el build.

## 8. Directoris generats o de suport local

### `build/`

Sortida general de compilacio i tasques Gradle a nivell arrel.

### `composeApp/build/`

Sortida generada del modul:

- classes compilades
- jars
- informes de tests
- recursos processats

### `.gradle/`

Caches i estat local de Gradle.

### `.gradle-user-home/`

Home local de Gradle usada en aquest entorn.

### `.kotlin/`

Caches i metadades de Kotlin.

### `.idea/`

Configuracio local d'IntelliJ IDEA.

### `.run/`

Configuracions locals d'execucio.

## 9. Fitxers auxiliars detectats a l'arrel

### `SupabaseProbe.java`

Utility local de prova o diagnosi de connexio amb Supabase.

### `SupabaseProbe.class`

Bytecode compilat de la utility anterior. No forma part del nucli documental ni del codi font principal del producte.

## 10. Resum de criteri estructural

- `docs/` concentra tota la documentacio i tots els SQL
- `composeApp/` concentra el codi de l'aplicacio
- l'arrel queda per build, configuracio i punts d'entrada
- `build/`, `.gradle/`, `.idea/` i similars son directoris generats o locals
- els exemples documentals externs queden a `docs/exemples/`
