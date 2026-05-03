# Index complet de documentacio i estructura del projecte

## Objectiu d'aquest index

Aquest document fa de mapa mestre. Resumeix:

- quins documents existeixen
- en quin ordre es recomana llegir-los
- com esta organitzat el repositori
- on viu cada part del sistema: aplicacio, integracio, SQL, documentacio i fitxers auxiliars

## Ruta recomanada per entendre el projecte de zero

1. [Guia de posada en marxa, demo i incidencies](guies/instalacio_i_demo.md)
2. [Manual d'usuari complet](guies/Manual_Usuari.md)
3. [Arquitectura, estructura del programa i explicacio del codi](arquitectura/arquitectura_i_codi.md)
4. [API i integracio amb Supabase](api/api_i_integracio.md)
5. [Model de dades i funcionament de la BDD](sql/model_de_dades_i_bdd.md)
6. [Permisos i seguretat](arquitectura/permisos_i_seguretat.md)
7. [Flux operatiu i moduls](funcional/flux_operatiu_i_moduls.md)

## Estructura actual de `docs/`

```text
docs/
|-- README.md
|-- index_complet_documentacio_i_projecte.md
|-- api/
|   `-- api_i_integracio.md
|-- arquitectura/
|   |-- arquitectura_i_codi.md
|   `-- permisos_i_seguretat.md
|-- exemples/
|   |-- dan_agricola_exemple.pdf
|   `-- dan_ramadera_exemple.pdf
|-- funcional/
|   `-- flux_operatiu_i_moduls.md
|-- guies/
|   |-- Manual_Usuari.md
|   `-- instalacio_i_demo.md
|-- presentacio/
|   |-- guia_defensa.md
|   `-- mem.md
|-- projecte/
|   |-- estat_actual_i_roadmap.md
|   `-- millores_post_mvp.md
`-- sql/
    |-- README.md
    |-- model_de_dades_i_bdd.md
    |-- maintenance/
    |   `-- reset_auth_seed_users.sql
    |-- schema/
    |   `-- agrisync_schema.sql
    `-- seeds/
        `-- agrisync_demo_seed.sql
```

## Funcio de cada bloc documental

### `docs/README.md`

Porta d'entrada curta. Explica com llegir la documentacio i quins documents son essencials.

### `docs/guies/instalacio_i_demo.md`

Document operatiu. Serveix per:

- preparar un projecte Supabase net
- entendre quines claus i fitxers de configuracio fan falta
- consultar les credencials de demo generades pel seed
- aplicar esquema i seed en l'ordre correcte
- executar el client desktop
- validar la demo
- resoldre problemes habituals de config, Auth, RLS i xarxa

### `docs/guies/Manual_Usuari.md`

Manual complet per usuari final i defensa. Explica pantalles, rols, fluxos d'us, demo, gestio administrativa, resolucio de problemes i inclou marcadors `[CAPTURA: ...]` per inserir imatges del programa i de Supabase.

### `docs/arquitectura/arquitectura_i_codi.md`

Document principal per entendre el programa. Explica:

- stack tecnologic
- flux d'arrencada
- estructura per carpetes
- fitxers clau
- repositoris, viewmodels i pantalles
- navegacio
- flux de dades UI -> ViewModel -> Repository -> Supabase -> UI

### `docs/api/api_i_integracio.md`

Document principal de la capa d'integracio. Detalla:

- API real consumida pel client
- endpoints de Supabase Auth, PostgREST, RPC i Admin API
- peces personalitzades del projecte
- punts de fort acoblament actual
- com evolucionar cap a una API propia o admetre APIs externes

### `docs/sql/model_de_dades_i_bdd.md`

Document principal de la base de dades. Explica:

- model conceptual i model fisic
- taules, relacions i enums
- triggers d'auditoria
- funcions helper de permisos
- grants i RLS
- com es relaciona la BDD amb cada modul del client

### `docs/arquitectura/permisos_i_seguretat.md`

Resum especialitzat del model de permisos. Complementa el document detallat de BDD.

### `docs/funcional/flux_operatiu_i_moduls.md`

Resum funcional centrat en us d'usuari i no tant en implementacio.

### `docs/sql/README.md`

Index del paquet SQL i de l'ordre d'execucio.

### `docs/projecte/millores_post_mvp.md`

Document pensat per defensa i tancament del MVP. Recull limits coneguts i linies d'evolucio futures sense necessitat d'implementar-les en aquesta fase.

## Estructura principal del repositori

```text
AgriSynct/
|-- README.md
|-- build.gradle.kts
|-- settings.gradle.kts
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
|-- agrisync.properties.example
|-- agrisync.properties
|-- composeApp/
|   |-- build.gradle.kts
|   `-- src/
|       |-- commonMain/kotlin/cat/agrisync/
|       |   |-- App.kt
|       |   |-- data/
|       |   |-- ui/
|       |   `-- viewmodel/
|       |-- jvmMain/kotlin/cat/agrisync/
|       |   |-- main.kt
|       |   `-- data/
|       `-- jvmTest/
|-- docs/
|-- gradle/
`-- SupabaseProbe.java
```

## Significat de cada zona del repositori

### Arrel del projecte

- `README.md`
  Resum curt del projecte i enllaços als documents principals.
- `build.gradle.kts`
  Configuracio Gradle arrel. Centralitza plugins per no carregar-los repetidament.
- `settings.gradle.kts`
  Declara el modul `:composeApp` i repositoris de plugins/dependencies.
- `agrisync.properties.example`
  Plantilla de configuracio local.
- `agrisync.properties`
  Configuracio local real, no apta per compartir si conté claus de Supabase.

### `composeApp/`

Conté l'aplicacio desktop Kotlin Multiplatform basada en Compose per a JVM.

- `composeApp/build.gradle.kts`
  Dependències, plugins, target `jvm()` i configuracio de distribucions natives.
- `src/commonMain/kotlin/cat/agrisync/`
  Codi compartit del client: arrencada funcional, capa de dades, UI i viewmodels.
- `src/jvmMain/kotlin/cat/agrisync/`
  Entrada real de desktop i implementacions específiques de JVM.
- `src/jvmTest/`
  Tests del modul desktop.

### `docs/`

Documentacio funcional, tecnica, API i SQL.

### `gradle/`

Wrapper de Gradle per compilar sense instal·lacio global addicional.

### `SupabaseProbe.java`

Utility local de prova de connexio. No forma part del flux principal de l'app.

## Fitxers clau que cal dominar

Si algú ha d'explicar o mantenir el projecte, els fitxers minimament imprescindibles son:

- `composeApp/src/jvmMain/kotlin/cat/agrisync/main.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/App.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/AuthService.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/RestClient.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/SupabaseAuthApi.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/TecnicRepository.kt`
- `docs/sql/schema/agrisync_schema.sql`
- `docs/sql/seeds/agrisync_demo_seed.sql`

## Criteri estructural actual

- El client no te backend propi separat; consumeix Supabase directament.
- La seguretat real viu a PostgreSQL mitjançant RLS i funcions helper.
- Els viewmodels coordinen estat i validacions de pantalla.
- Els repositoris concentren les crides HTTP i les queries PostgREST.
- La documentacio esta separada per perspectiva: posada en marxa, codi, API, BDD, funcional i seguretat.
