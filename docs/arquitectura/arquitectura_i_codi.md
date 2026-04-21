# Arquitectura, estructura del programa i explicacio del codi

## Objectiu d'aquest document

Aquest document explica el programa des del punt de vista intern. La idea es que una persona que no conegui el projecte pugui entendre:

- com arrenca l'aplicacio
- quines llibreries fa servir
- com esta estructurat el codi
- quines pantalles hi ha
- quins viewmodels i repositoris existeixen
- quin es el flux de dades entre UI, estat, API i BDD
- per quins fitxers cal començar si s'ha de mantenir o ampliar l'app

No es un resum d'alt nivell. Es un mapa tecnic detallat.

## Stack tecnologic real

### Llenguatge i plataforma

- Kotlin Multiplatform
- target actual actiu: JVM desktop

### UI

- JetBrains Compose Multiplatform
- Material 3

### Concurrencia i estat

- Kotlin Coroutines
- `StateFlow` i `MutableStateFlow`

### Serialitzacio i HTTP

- `kotlinx.serialization`
- Ktor Client
- engine OkHttp a `jvmMain`

### Backend

- Supabase Auth
- Supabase PostgREST
- Supabase RPC sobre PostgreSQL
- Supabase Admin API per operacions administratives

### Persistencia i seguretat

- PostgreSQL gestionat per Supabase
- Row Level Security
- funcions helper SQL
- triggers d'auditoria

## Dependències declarades al build

El fitxer `composeApp/build.gradle.kts` declara:

- `org.jetbrains.compose.runtime`
- `org.jetbrains.compose.foundation`
- `org.jetbrains.compose.material3`
- `org.jetbrains.compose.ui`
- `org.jetbrains.compose.components:components-resources`
- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`
- `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core`
- `org.jetbrains.kotlinx:kotlinx-coroutines-swing`
- `org.jetbrains.kotlinx:kotlinx-serialization-json`
- `io.ktor:ktor-client-core`
- `io.ktor:ktor-client-okhttp`
- `io.ktor:ktor-client-content-negotiation`
- `io.ktor:ktor-serialization-kotlinx-json`
- `org.slf4j:slf4j-simple`

### Que implica aquest stack

- no hi ha Spring, Koin, Dagger ni framework backend propi
- no hi ha ORM ni capa DAO tradicional
- la capa de dades esta feta a ma amb `RestClient` i queries PostgREST
- la UI depen de Compose i de patrons simples de `ViewModel + StateFlow`

## Estructura global del repositori

```text
composeApp/
|-- build.gradle.kts
`-- src/
    |-- commonMain/kotlin/cat/agrisync/
    |   |-- App.kt
    |   |-- data/
    |   |-- ui/
    |   `-- viewmodel/
    |-- jvmMain/kotlin/cat/agrisync/
    |   |-- main.kt
    |   `-- data/
    `-- jvmTest/
```

## Com arrenca l'aplicacio

### 1. Entrada JVM

Fitxer:

- `composeApp/src/jvmMain/kotlin/cat/agrisync/main.kt`

Flux:

1. `main()` obre una finestra Compose Desktop
2. el contingut de la finestra crida `App(JvmEnvConfig())`
3. des d'aquell moment tota la logica queda a `commonMain`

### 2. Carrega de configuracio

Fitxers implicats:

- `composeApp/src/commonMain/kotlin/cat/agrisync/data/EnvConfig.kt`
- `composeApp/src/jvmMain/kotlin/cat/agrisync/data/JvmEnvConfig.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/SupabaseConfig.kt`

Responsabilitat:

- `EnvConfig` defineix la interfície de carregat de config
- `JvmEnvConfig` implementa la cerca real de claus a JVM
- `SupabaseConfig` encapsula `url`, `anonKey` i `serviceRoleKey`

Ordre de resolucio de config:

1. propietats JVM
2. variables d'entorn
3. fitxers `.properties`

Si la config falta, `App.kt` no continua i mostra una pantalla d'error de configuracio.

### 3. Creacio de serveis

Fitxer:

- `composeApp/src/commonMain/kotlin/cat/agrisync/data/AppServices.kt`

Aquest fitxer crea el graf manual de dependències:

- `HttpClient`
- `SupabaseAuthApi`
- `AuthService`
- `RestClient`
- repositoris

No hi ha contenidor d'injeccio. Tot es munta manualment i de manera explícita.

### 4. Inicialitzacio d'autenticacio

Fitxer:

- `composeApp/src/commonMain/kotlin/cat/agrisync/data/AuthService.kt`

Quan `App.kt` arrenca:

1. crea `AuthService`
2. crida `initialize()`
3. intenta recuperar sessio guardada
4. si hi ha `refresh_token`, prova de refrescar sessio
5. intenta resoldre `public.tecnic`
6. emet un `AuthState`

Els estats possibles son:

- `Initializing`
- `Unauthenticated`
- `Loading`
- `Authenticated`
- `Error`

### 5. Navegacio principal

Fitxers:

- `composeApp/src/commonMain/kotlin/cat/agrisync/App.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/ui/navigation/Screen.kt`

`App.kt` decideix si mostrar:

- pantalla de login
- error de configuracio
- o contingut autenticat

Quan l'usuari esta autenticat, `AuthenticatedContent` governa una variable `currentScreen` amb aquestes pantalles:

- `Login`
- `TitularsHome`
- `Profile`
- `DanPreparation`
- `TitularAgricola`
- `TitularRamader`
- `TecnicManagement`
- `TecnicDetail`
- `TitularManagement`
- `TerraManagement`
- `OficinaManagement`

## Patró arquitectònic real

El patró que fa servir el projecte es:

```text
Composable UI
   -> ViewModel
      -> Repository
         -> RestClient / SupabaseAuthApi
            -> Supabase Auth / PostgREST / RPC / Admin API
               -> PostgreSQL + RLS
```

### Què fa cada capa

#### UI

- dibuixa pantalles
- captura interaccio d'usuari
- mostra errors, diàlegs, snackbars i formularis
- no decideix permisos reals

#### ViewModel

- carrega dades
- manté `UiState`
- valida formularis basics
- coordina crides asíncrones
- transforma dades per a la UI

#### Repository

- coneix endpoints PostgREST o Auth
- construeix queries `select=...`
- envia `POST`, `PATCH`, `DELETE`
- parseja DTOs

#### SQL / Supabase

- és la font de veritat de permisos
- filtra amb RLS
- valida integritat
- resol `get_my_tecnic()` i altres helpers

## Fitxers principals de la capa de dades

### Configuracio i infra base

| Fitxer | Responsabilitat |
|---|---|
| `EnvConfig.kt` | Contracte de càrrega de configuració |
| `JvmEnvConfig.kt` | Implementació real de lectura de variables i `.properties` |
| `SupabaseConfig.kt` | Model immutable amb URL i claus |
| `SupabaseJson.kt` | Configuració comuna de serialització JSON |
| `SupabaseHttpClient.kt` | Factoria del client Ktor |
| `ApiException.kt` | Excepció pròpia per errors HTTP |
| `PlatformDateTime.kt` | Abstracció de data/hora per plataforma |
| `SessionPersistence.kt` | Persistència de sessió per plataforma |

### Integracio HTTP i Auth

| Fitxer | Responsabilitat |
|---|---|
| `RestClient.kt` | Wrapper genèric per `GET`, `POST`, `PATCH`, `DELETE` sobre `/rest/v1` |
| `SupabaseAuthApi.kt` | Login, refresh i resolució del tècnic autenticat |
| `AuthModels.kt` | Models de sessió, auth i `TecnicDto` |
| `AuthService.kt` | Orquestració de sessió i estat d'autenticació |
| `AppServices.kt` | Composició manual de serveis i repositoris |

### Models de domini i DTOs

| Fitxer | Responsabilitat |
|---|---|
| `MvpModels.kt` | DTOs operatius de DAN, terra, granja, entrega i resum |
| `TitularDto.kt` | DTOs i requests de `titular`, `tecnic_titular` i `oficina` |

### Repositoris funcionals

| Fitxer | Responsabilitat |
|---|---|
| `AccessRepository.kt` | Resol titulars visibles a la home segons rol i assignacions |
| `AgricolaRepository.kt` | CRUD de titular agrícola, terres i aplicacions |
| `RamaderRepository.kt` | CRUD de titular ramader, granges, bestiar i entregues |
| `DanPreparationRepository.kt` | Agregació de dades per a `Preparar DAN` |
| `OficinaRepository.kt` | CRUD d'oficines |
| `TecnicRepository.kt` | CRUD de tècnics, assignacions i operacions Admin API |
| `TitularManagementRepository.kt` | CRUD de titulars, comparticions d'oficina i terres transversals |
| `AuditRepository.kt` | Resol noms d'actor d'auditoria a partir de `user_id` |

## Mètodes importants de la capa de dades

### `RestClient.kt`

Mètodes nuclears:

- `get(...)`
- `get(..., accessToken)`
- `post(...)`
- `patch(...)`
- `delete(...)`

Característiques:

- apunta a `${config.url}/rest/v1/...`
- posa `apikey`
- posa `Authorization` amb JWT de sessió o `anonKey`
- usa `Prefer: return=representation` en `POST` i `PATCH`

### `SupabaseAuthApi.kt`

Mètodes nuclears:

- `signInWithPassword(email, password)`
- `refresh(refreshToken)`
- `getMyTecnic(accessToken, loginEmail)`

`getMyTecnic` te un flux important:

1. intenta cridar RPC `get_my_tecnic`
2. si falla, busca `tecnic` per `user_id` amb el token de l'usuari
3. si encara falla, prova un fallback per `email` amb `service_role`
4. si detecta desquadrament entre `user_id` real i guardat, intenta auto-fix

Aquesta peça es clau per entendre per què el projecte pot recuperar-se d'alguns desajustos d'Auth sense backend addicional.

### `AuthService.kt`

Mètodes nuclears:

- `initialize()`
- `login(email, password)`
- `refreshNow()`
- `reloadTecnic()`
- `signOut()`

Responsabilitats:

- persistir sessio
- refrescar tokens
- mantenir `AuthState`
- garantir que el tecnic associat estigui actiu

### `AccessRepository.kt`

Mètode clau:

- `listTitularAccessForTecnic(tecnic)`

Com funciona:

- si el rol es `admin`, recupera tots els titulars
- si el rol es `oficina_manager`, combina titulars visibles i comparticions de la seva oficina
- si el rol es `tecnic`, llegeix `tecnic_titular` i deriva `can_agricola` i `can_ramader`

### `AgricolaRepository.kt`

Mètodes principals:

- `getTitular`
- `updateTitular`
- `listTerres`
- `createTerra`
- `updateTerra`
- `deleteTerra`
- `listAplicacionsByTitular`
- `listCampanyesByTitular`
- `createAplicacio`
- `updateAplicacio`
- `deleteAplicacio`

Detall important:

- quan es crea una aplicacio, el repositori pot crear automaticament la `dan_declaracio` de la campanya si encara no existeix

### `RamaderRepository.kt`

Mètodes principals:

- `getTitular`
- `updateTitular`
- `listGranges`
- `createGranja`
- `updateGranja`
- `deleteGranja`
- `listGranjaBestiar`
- `createGranjaBestiar`
- `updateGranjaBestiar`
- `deleteGranjaBestiar`
- `listEntreguesByTitular`
- `createEntrega`
- `updateEntrega`
- `deleteEntrega`
- `listAccessibleTitulars`
- `listAccessibleTerres`

Detall important:

- els receptors d'una entrega es resolen contra dades accessibles via RLS

### `DanPreparationRepository.kt`

Mètodes principals:

- `getTitular`
- `listTerres`
- `listAplicacionsByTitular`
- `listCampanyesByTitular`
- `listGranges`
- `listGranjaBestiar`
- `listEntreguesByTitular`

Aquest repositori no fa escriptures. La seva feina es agregadora.

### `TecnicRepository.kt`

Mètodes principals:

- `listAll`
- `listOficines`
- `createTecnic`
- `updateTecnic`
- `deleteTecnic`
- `createAuthUser`
- `updateAuthUserPassword`
- `deleteAuthUser`
- `listAssignacions`
- `createAssignacio`
- `deleteAssignacio`
- `listCollaboratingTecnicsByTitular`
- `listCollaboratingOficinesByTitular`
- `resolveActorLabel`

Detall important:

- combina PostgREST i Admin API
- es una de les peces mes sensibles del projecte perque toca dades de seguretat i gestio d'usuaris

### `TitularManagementRepository.kt`

Mètodes principals:

- `listAll`
- `create`
- `update`
- `delete`
- `listOfficeShares`
- `createOfficeShare`
- `deleteOfficeShare`
- `listTerres`
- `createTerra`
- `updateTerra`
- `deleteTerra`

## Fitxers principals de `viewmodel/`

Cada viewmodel segueix el mateix patró:

- `CoroutineScope(SupervisorJob() + Dispatchers.Default)`
- un `MutableStateFlow`
- un `UiState`
- mètodes `load(...)`, accions de formulari i `clear()`

### Relacio pantalla -> viewmodel

| Pantalla | ViewModel | Responsabilitat principal |
|---|---|---|
| `LoginScreen` | `LoginViewModel` | login i traduccio d'errors de xarxa/Auth |
| `TitularsScreen` | `HomeViewModel` | home, cerca i paginacio |
| `ProfileScreen` | `ProfileViewModel` | perfil propi i canvi de password |
| `TitularAgricolaScreen` | `TitularAgricolaViewModel` | titular, terres i aplicacions |
| `TitularRamaderScreen` | `TitularRamaderViewModel` | titular, granges, cens i entregues |
| `DanPreparationScreen` | `DanPreparationViewModel` | resum per campanya i text per clipboard |
| `TecnicManagementScreen` | `TecnicManagementViewModel` | alta/baixa de tecnics i reset de password |
| `TecnicDetailScreen` | `TecnicDetailViewModel` | detall de tecnic i assignacions |
| `TitularManagementScreen` | `TitularManagementViewModel` | CRUD de titulars i comparticions |
| `TerraManagementScreen` | `TerraManagementViewModel` | manteniment transversal de terres |
| `OficinaManagementScreen` | `OficinaManagementViewModel` | CRUD d'oficines |

### `LoginViewModel.kt`

Mètodes principals:

- `onEmailChange`
- `onPasswordChange`
- `login`
- `clear`

Detall important:

- converteix errors tècnics en missatges mes útils per l'usuari
- distingeix credencials incorrectes, problemes de xarxa i errors d'Auth

### `HomeViewModel.kt`

Mètodes principals:

- `load`
- `onSearchChange`
- `nextPage`
- `prevPage`
- `clear`

Detall important:

- la cerca no es limita al nom; busca per `nif`, `telefon`, `codi_postal`, `email` i `adreca`

### `ProfileViewModel.kt`

Mètodes principals:

- `startEditing`
- `cancelEditing`
- `saveProfile`
- `showPasswordDialog`
- `changePassword`
- `clearMessage`
- `clear`

Detall important:

- guarda `nom`, `email` i `telefon`
- crida `authService.reloadTecnic()` despres de guardar per refrescar tota l'app

### `TitularAgricolaViewModel.kt`

Mètodes principals:

- `load`
- `onSelectCampanya`
- `updateTitular`
- `updateTerra`
- `createTerra`
- `deleteTerra`
- `updateAplicacio`
- `createAplicacio`
- `deleteAplicacio`

Detall important:

- calcula nitrogen aplicat per terra
- calcula límits per zona `ZV` o `ZNV`
- mostra avisos quan una terra supera el límit anual
- carrega també col·laboradors del titular

### `TitularRamaderViewModel.kt`

Mètodes principals:

- `load`
- `updateTitular`
- `updateGranja`
- `createGranja`
- `deleteGranja`
- `updateGranjaBestiar`
- `createGranjaBestiar`
- `deleteGranjaBestiar`
- `updateEntrega`
- `createEntrega`
- `deleteEntrega`

Detall important:

- treballa per campanya
- carrega titulars i terres accessibles com a possibles receptors
- mostra col·laboradors del titular

### `DanPreparationViewModel.kt`

Mètodes principals:

- `load`
- `onSelectCampanya`
- `buildClipboardSummary`
- `buildClipboardChecklist`
- `automaticChecklistItems`

Detall important:

- agrega dades de multiples entitats
- no escriu res
- calcula totals i construeix textos llestos per copiar

### Viewmodels de gestio

`TecnicManagementViewModel`, `TecnicDetailViewModel`, `TitularManagementViewModel`, `TerraManagementViewModel` i `OficinaManagementViewModel` comparteixen aquestes responsabilitats:

- carregar llistes
- obrir i tancar dialogs
- validar formularis
- executar CRUD
- mantenir missatges d'operacio

## Fitxers principals de `ui/`

La carpeta `ui/` concentra les pantalles Compose i components d'ajuda.

### Utilitats comunes

| Fitxer | Contingut |
|---|---|
| `UiHelpers.kt` | `AuditInfoBlock`, `TitularCollaborationCard`, `SearchableSelectionField` i helpers de format |

### Pantalles operatives

| Fitxer | Pantalla principal | Què fa |
|---|---|---|
| `LoginScreen.kt` | `LoginScreen` | formulari de login |
| `TitularsScreen.kt` | `TitularsScreen` | home de titulars amb cerca i accessos a moduls |
| `ProfileScreen.kt` | `ProfileScreen` | perfil propi i canvi de password |
| `TitularAgricolaScreen.kt` | `TitularAgricolaScreen` | edicio de titular, terres i aplicacions |
| `TitularRamaderScreen.kt` | `TitularRamaderScreen` | edicio de titular, granges, cens i entregues |
| `DanPreparationScreen.kt` | `DanPreparationScreen` | resum DAN i checklist |

### Pantalles de gestio

| Fitxer | Pantalla principal | Què fa |
|---|---|---|
| `TitularManagementScreen.kt` | `TitularManagementScreen` | alta, edicio, baixa i comparticio de titulars |
| `TerraManagementScreen.kt` | `TerraManagementScreen` | manteniment transversal de terres |
| `TecnicManagementScreen.kt` | `TecnicManagementScreen` | alta i baixa de tecnics, reset de password |
| `TecnicDetailScreen.kt` | `TecnicDetailScreen` | detall d'un tecnic, oficina, rol i assignacions |
| `OficinaManagementScreen.kt` | `OficinaManagementScreen` | alta, edicio i baixa d'oficines |

## Composables i subcomponents destacats

Aquest apartat no enumera cada línia de UI, pero sí les peces rellevants per entendre cada pantalla.

### `LoginScreen.kt`

Peces importants:

- `LoginScreen`

Fa:

- formulari de credencials
- indicador de càrrega
- visualització d'errors de login

### `TitularsScreen.kt`

Peces importants:

- `TitularsScreen`
- `MessageCard`
- `TitularCard`

Fa:

- cerca
- paginació
- targetes de titular
- botons cap a agrícola, ramader i `Preparar DAN`

### `ProfileScreen.kt`

Peces importants:

- `ProfileScreen`
- `AuditSection`
- `ProfileRow`
- `ChangePasswordDialog`

Fa:

- visualització del perfil propi
- edició de nom, email i telèfon
- canvi de password
- dades d'oficina i auditoria

### `TitularAgricolaScreen.kt`

Peces importants:

- `TitularAgricolaScreen`
- `CampaignSelectorCard`
- `SectionHeader`
- `EditableTitularCard`
- `EditableTerraCard`
- `EditableAplicacioCard`
- `CreateTerraDialog`
- `CreateAplicacioDialog`
- `TerraDropdown`
- `ConfirmDeleteDialog`

Fa:

- editar el titular
- mostrar col·laboració d'oficines i tècnics
- canviar de campanya
- gestionar terres
- gestionar aplicacions fertilitzants

### `TitularRamaderScreen.kt`

Peces importants:

- `TitularRamaderScreen`
- `CampaignSelectorCard`
- `EditableRamaderTitularCard`
- `EditableGranjaCard`
- `EditableGranjaBestiarCard`
- `EditableEntregaCard`
- `CreateGranjaDialog`
- `CreateGranjaBestiarDialog`
- `CreateEntregaDialog`
- `GranjaDropdown`
- `BestiarDropdown`
- `FaseDropdown`
- `TitularDropdown`
- `TerraDropdown`

Fa:

- editar el titular
- mostrar col·laboració d'oficines i tècnics
- gestionar granges
- gestionar cens per bestiar i fase
- gestionar entregues i receptors

### `DanPreparationScreen.kt`

Peces importants:

- `DanPreparationScreen`
- `TitularSummaryCard`
- `PreparationActionsCard`
- `CampaignSelectorCard`
- `SummaryMetrics`
- `AutomaticChecklistCard`
- `TerraPreparationCard`
- `AplicacioPreparationCard`
- `GranjaPreparationCard`
- `GranjaBestiarPreparationCard`
- `EntregaPreparationCard`
- `ManualReviewCard`

Fa:

- resum agregat per campanya
- càlculs totals
- checklist automàtica
- generació de text per copiar

### `TitularManagementScreen.kt`

Peces importants:

- `TitularManagementScreen`
- `TitularManagementCard`
- `ShareTitularDialog`
- `CreateTitularDialog`

Fa:

- CRUD de titulars
- cerca avançada
- compartició per oficina

### `TerraManagementScreen.kt`

Peces importants:

- `TerraManagementScreen`
- `TerraManagementCard`
- `CreateTerraDialog`
- `ZonaSelector`

Fa:

- manteniment transversal de terres
- filtre per titular
- alta i edició fora del mòdul agrícola

### `TecnicManagementScreen.kt`

Peces importants:

- `TecnicManagementScreen`
- `TecnicCard`
- `CreateTecnicDialog`
- `ResetPasswordDialog`
- `DeleteTecnicDialog`

Fa:

- llistat de tècnics
- alta de tècnic i usuari Auth
- activació/desactivació
- eliminació
- canvi de password administratiu

### `TecnicDetailScreen.kt`

Peces importants:

- `TecnicDetailScreen`
- formulari de dades del tècnic
- bloc d'assignacions
- bloc d'alta d'assignació

Fa:

- detall d'un tècnic concret
- canvi de rol
- canvi d'oficina
- gestió d'assignacions a titulars

### `OficinaManagementScreen.kt`

Peces importants:

- `OficinaManagementScreen`
- `OficinaCard`

Fa:

- alta, edició i baixa d'oficines

## Quines pantalles veu cada rol

### Barra superior comuna

Sempre hi ha:

- `Titulars`
- `Perfil`
- `Logout`

### Rols `admin` i `oficina_manager`

A mes de l'anterior, veuen:

- `Gestio Titulars`
- `Terres`
- `Tecnics`
- `Oficines`

### Rol `tecnic` o `lectura`

No veuen pantalles de gestio administrativa. Treballen des de:

- `Titulars`
- `Perfil`
- `Modul Agricola`
- `Modul Ramader`
- `Preparar DAN`

sempre que el titular i l'ambit ho permetin.

## Flux funcional complet dins del codi

### Flux 1. Login

1. `LoginScreen` envia email i password a `LoginViewModel.login()`
2. `LoginViewModel` crida `AuthService.login()`
3. `AuthService` usa `SupabaseAuthApi.signInWithPassword()`
4. `AuthService` recupera `public.tecnic` amb `getMyTecnic()`
5. si tot es correcte, `AuthState` passa a `Authenticated`
6. `App.kt` entra a `AuthenticatedContent`

### Flux 2. Home de titulars

1. `App.kt` crea `HomeViewModel`
2. `HomeViewModel.load()` crida `AccessRepository.listTitularAccessForTecnic()`
3. el repositori llegeix titulars visibles segons rol i RLS
4. la UI mostra targetes de titular
5. cada targeta ofereix accions cap a:
   `Modul Agricola`, `Modul Ramader` o `Preparar DAN`

### Flux 3. Modul Agricola

1. `TitularAgricolaViewModel.load()` carrega titular, terres, campanyes i aplicacions
2. calcula totals de nitrogen i avisos
3. la pantalla permet crear o editar terres
4. la pantalla permet crear o editar aplicacions
5. cada operacio escriu via `AgricolaRepository`
6. la BDD decideix si l'usuari pot fer l'operacio segons `can_write_agricola(...)`

### Flux 4. Modul Ramader

1. `TitularRamaderViewModel.load()` carrega titular, granges, cens, entregues, receptors i campanyes
2. la pantalla permet mantenir granges i registres de bestiar
3. la pantalla permet registrar entregues amb receptor titular o terra
4. la BDD valida accessos amb `can_write_ramader(...)`, `can_read_titular(...)` i `can_reference_terra(...)`

### Flux 5. Preparar DAN

1. `DanPreparationViewModel.load()` agrega totes les dades per titular i campanya
2. calcula totals, checklist i textos de resum
3. `DanPreparationScreen` mostra targetes informatives i accions de copiat
4. no hi ha edicio des d'aquesta pantalla

### Flux 6. Gestio de tecnics

1. `TecnicManagementViewModel.load()` llegeix tecnics i oficines
2. la creacio d'un tecnic pot implicar:
   - crear usuari Auth
   - crear registre a `public.tecnic`
3. el detall del tecnic usa `TecnicDetailViewModel`
4. el detall permet editar oficina, rol, dades i assignacions

### Flux 7. Gestio de titulars i comparticions

1. `TitularManagementViewModel.load()` carrega titulars i oficines
2. la UI permet alta, baixa i edicio
3. quan s'obre comparticio, es llegeixen `oficina_titular_compartit`
4. es poden crear comparticions per `scope`

## Gestio d'estat a Compose

El projecte no utilitza `androidx.lifecycle.ViewModel` clàssic de forma directa. El patró real es:

- crear l'objecte viewmodel amb `remember(...)`
- alliberar recursos amb `DisposableEffect { onDispose { vm.clear() } }`
- carregar dades amb `LaunchedEffect(...)`
- observar `StateFlow` amb `collectAsState()`

Aixo fa el cicle de vida molt explicit i fàcil de seguir llegint `App.kt`.

## Decisions tecniques importants

### 1. No hi ha backend personalitzat

El client parla directament amb Supabase. Avantatge:

- menys infraestructura
- menys codi servidor
- permisos centrats a BDD

Cost:

- el client coneix massa detalls de l'esquema SQL
- hi ha dependència forta de PostgREST i de les columnes exactes

### 2. La seguretat no viu a la UI

La UI mostra o amaga accions, pero la decisio final la pren PostgreSQL via RLS. Això evita que manipular el client doni permisos reals.

### 3. Els repositoris estan acoblats a queries PostgREST

Les `select=` llargues i anidades donen molta simplicitat de desplegament, pero també fan que els canvis d'esquema obliguin a revisar diversos repositoris.

### 4. El `service_role` existeix al client actual

Per un MVP/demo es útil, pero per produccio seriosa seria millor moure aquestes operacions a una API backend controlada.

## On començar si s'ha de modificar alguna cosa

### Si vols tocar login o sessio

Llegeix:

- `main.kt`
- `App.kt`
- `AuthService.kt`
- `SupabaseAuthApi.kt`

### Si vols tocar permisos o visibilitat de dades

Llegeix:

- `AccessRepository.kt`
- `TecnicRepository.kt`
- `docs/sql/schema/agrisync_schema.sql`
- `docs/arquitectura/permisos_i_seguretat.md`

### Si vols tocar una pantalla operativa

Llegeix en ordre:

1. pantalla `ui/...`
2. `viewmodel/...`
3. repositori corresponent a `data/...`
4. taules i policies SQL implicades

### Si vols ampliar el model funcional

Normalment el flux correcte de canvis es:

1. ampliar SQL i RLS
2. ampliar repositori
3. ampliar viewmodel
4. ampliar UI
5. actualitzar documentacio

## Limitacions actuals

- no hi ha una capa de domini desacoblada de PostgREST
- no hi ha backend intermedi
- el testing automatic encara es limitat
- no hi ha export formal a PDF oficial
- algunes operacions administratives depenen de `service_role`

## Resum

AgriSync es una aplicacio desktop Compose que funciona com a client ric sobre Supabase. El cor del sistema no es la UI, sino la combinacio de:

- `AuthService` per la sessio
- `RestClient` i `SupabaseAuthApi` per la integracio
- repositoris per casos d'us
- viewmodels per estat de pantalla
- SQL amb RLS per fer complir permisos

Si s'entenen aquests cinc eixos, s'entén el projecte.
