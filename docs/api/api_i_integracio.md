# API i integracio amb Supabase

## Objectiu d'aquest document

Aquest document explica quina API fa servir AgriSync, on es troba el codi d'integracio, què hi ha de personalitzat i fins a quin punt el projecte esta preparat per admetre una API propia o APIs externes.

La resposta curta es:

- avui el projecte fa servir Supabase com a API real
- no hi ha backend propi separat
- sí que hi ha lògica personalitzada, pero viu al client i a SQL

## Quina API consumeix actualment l'aplicacio

AgriSync consumeix quatre superfícies d'API de Supabase:

1. Auth API
2. PostgREST API
3. RPC sobre PostgREST
4. Admin API

## 1. Supabase Auth API

Endpoints principals:

- `POST /auth/v1/token?grant_type=password`
- `POST /auth/v1/token?grant_type=refresh_token`

On es fa servir:

- `composeApp/src/commonMain/kotlin/cat/agrisync/data/SupabaseAuthApi.kt`

Per a què serveix:

- login amb email i password
- refresc de sessio

Quin fitxer l'orquestra:

- `composeApp/src/commonMain/kotlin/cat/agrisync/data/AuthService.kt`

## 2. Supabase PostgREST API

Base URL:

- `${SUPABASE_URL}/rest/v1/...`

On es centralitza:

- `composeApp/src/commonMain/kotlin/cat/agrisync/data/RestClient.kt`

Què fa:

- `GET`
- `POST`
- `PATCH`
- `DELETE`

Aquest client construeix les crides per a:

- `titular`
- `tecnic`
- `oficina`
- `tecnic_titular`
- `oficina_titular_compartit`
- `dan_declaracio`
- `terra`
- `aplicacions_fertilitzants`
- `granja`
- `bestiar`
- `fase_productiva`
- `granja_bestiar`
- `entrega_dejeccions`

No hi ha una API REST pròpia per sobre d'aquestes entitats. El client consumeix directament les taules publicades via PostgREST.

## 3. RPC sobre PostgREST

Endpoints clau:

- `POST /rest/v1/rpc/get_my_tecnic`
- `POST /rest/v1/rpc/create_titular`
- `POST /rest/v1/rpc/create_terra`

On es fa servir:

- `composeApp/src/commonMain/kotlin/cat/agrisync/data/SupabaseAuthApi.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/TitularManagementRepository.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/AgricolaRepository.kt`

Per què existeix:

- per resoldre quin `public.tecnic` correspon a `auth.uid()`
- per encapsular aquesta lògica a la base de dades
- per fer altes sensibles sense dependre d'inserts directes que poden xocar amb RLS

Aquestes RPCs son especialment importants perquè el model funcional de l'app no gira directament al voltant d'`auth.users`, sino de `public.tecnic` i dels seus permisos sobre titulars.

## 4. Supabase Admin API

Endpoints principals:

- `POST /auth/v1/admin/users`
- `PUT /auth/v1/admin/users/{id}`
- `DELETE /auth/v1/admin/users/{id}`

On es fa servir:

- `composeApp/src/commonMain/kotlin/cat/agrisync/data/TecnicRepository.kt`

Per a què serveix:

- crear compte Auth quan es crea un tecnic
- canviar passwords
- eliminar usuaris Auth

Important:

- aquestes crides necessiten `SUPABASE_SERVICE_ROLE_KEY`
- son molt potents
- la UI limita les accions de manager a tecnics de la seva oficina, i les policies RLS reforcen els canvis sobre `public.tecnic` i `tecnic_titular`
- en un entorn de produccio més exigent haurien de passar per una API backend propia en comptes de viatjar directament des del client

## Peces personalitzades del projecte

Encara que no hi hagi backend propi, el projecte sí que te integracio personalitzada.

## Personalitzacio al client

### `RestClient.kt`

Fa de wrapper únic per a PostgREST:

- injecta `apikey`
- injecta `Authorization`
- selecciona token de sessió o `anonKey`
- gestiona errors HTTP i els converteix en `ApiException`

### `SupabaseAuthApi.kt`

Conté comportaments específics del projecte:

- fallback de resolucio de `tecnic` per `user_id`
- fallback per `email`
- intent d'auto-fix de `public.tecnic.user_id`

Això no és Supabase estàndard "tal qual"; es lògica del projecte construïda sobre Supabase.

### `AuthService.kt`

Implementa:

- gestio de sessio persistent
- refresc automatic de token
- validacio que el tecnic estigui actiu

### Repositoris

Els repositoris encapsulen queries PostgREST concretes del domini:

- `AccessRepository`
- `AgricolaRepository`
- `RamaderRepository`
- `DanPreparationRepository`
- `OficinaRepository`
- `TecnicRepository`
- `TitularManagementRepository`

Cada repositori coneix:

- la taula o conjunt de taules a consultar
- les projeccions `select=...`
- les relacions anidades
- les operacions de creacio o actualitzacio

## Personalitzacio a la BDD

Una part important de l'"API" real del sistema viu al SQL:

- funcions helper com `get_my_tecnic()`
- funcions de permisos com `can_read_titular(...)`
- funcions de domini com `can_write_agricola(...)`
- RLS policies

Des del punt de vista del client, aquestes funcions formen part del contracte d'integracio tant com qualsevol endpoint HTTP.

## On es troba cada part de la capa API

| Tipus d'integracio | Fitxer principal |
|---|---|
| càrrega de config | `JvmEnvConfig.kt` |
| client HTTP | `SupabaseHttpClient.kt` |
| wrapper REST | `RestClient.kt` |
| Auth | `SupabaseAuthApi.kt` |
| sessió | `AuthService.kt` |
| composició de serveis | `AppServices.kt` |
| Admin API tècnics | `TecnicRepository.kt` |
| auditoria amb `service_role` | `AuditRepository.kt` |

## Flux d'autenticacio i API

1. `LoginViewModel` crida `AuthService.login()`
2. `AuthService` usa `SupabaseAuthApi.signInWithPassword()`
3. Supabase retorna `access_token` i `refresh_token`
4. `AuthService` crida `getMyTecnic()`
5. `SupabaseAuthApi` intenta RPC `get_my_tecnic`
6. si cal, fa fallback a consultes REST
7. el `Session` queda persistit
8. a partir d'aqui `RestClient` ja treballa amb el token de l'usuari

## Flux de dades operatives

Per a la majoria de pantalles, el flux es:

1. ViewModel
2. Repository
3. `RestClient`
4. `/rest/v1/...`
5. PostgreSQL aplica RLS
6. resposta JSON
7. DTO
8. `UiState`
9. Compose

Excepcio controlada:

- les altes de `titular` i `terra` passen per RPCs (`create_titular`, `create_terra`) perquè la BDD pugui comprovar permisos i escriure auditoria de forma consistent.

## Té API pròpia el projecte

### Resposta curta

No. Avui no hi ha una API backend propia separada de Supabase.

### Què sí que hi ha

Hi ha un contracte d'integracio propi, pero repartit entre:

- el codi client
- els DTOs
- les queries PostgREST
- les funcions SQL
- les policies RLS

Per tant, la lògica d'integracio es personalitzada, pero l'API exposada es la de Supabase.

## Està preparat per admetre una API externa

### Resposta curta

Parcialment.

### El que ja ajuda

- hi ha una capa de repositoris
- la UI no coneix directament HTTP
- `RestClient` concentra la major part de les crides REST
- `AuthService` concentra la sessio

Aquestes quatre coses faciliten refactoritzar.

### El que encara lliga fort a Supabase

- els repositoris fan queries PostgREST literals
- molts DTOs estan modelats segons la resposta exacta de Supabase
- l'app assumeix que la seguretat real viu a RLS
- hi ha dependència directa de RPC i Admin API
- no existeix una capa de "ports/adapters" o una capa de domini independent de transport

Conclusio:

- canviar completament a una API externa no seria trivial
- pero tampoc caldria reescriure tota la UI
- la capa que sofriria mes seria `data/`

## Com seria una migracio cap a una API propia

El camí raonable seria:

1. definir contractes de repositori més abstractes
2. separar DTO HTTP de models de domini
3. crear un backend intermedi
4. moure operacions amb `service_role` a aquest backend
5. exposar endpoints propis per login tècnic, gestio tècnica i operacions de domini
6. deixar Supabase com a BDD/Auth interna o reemplaçar-lo parcialment

## Possibles millores futures a la capa API

### Millores de seguretat

- treure `service_role` del client final
- moure Admin API a backend propi
- signar i auditar millor operacions sensibles

### Millores d'arquitectura

- afegir una capa `ApiGateway` o `RemoteDataSource`
- desacoblar models de domini de models PostgREST
- definir contractes clars per cada cas d'us

### Millores de robustesa

- retries selectius
- millor observabilitat i logs estructurats
- contract tests sobre endpoints i RPC
- tractament unificat d'errors de xarxa i d'RLS

### Millores funcionals

- integrar APIs externes de sistemes agraris
- exposar OpenAPI d'un backend propi
- suportar sincronitzacions amb ERPs o registres oficials

## Possibles usos futurs d'una API externa

Una API externa o backend intermedi permetria:

- reutilitzar la mateixa lògica des de web, mòbil i desktop
- amagar completament l'estructura SQL al client
- centralitzar validacions més complexes
- gestionar integracions oficials de DAN o tercers
- controlar millor rols administratius sensibles

## Risc actual a tenir en compte

Per a un MVP o entorn de demo, l'arquitectura actual es viable i molt pràctica. Per a un producte produccio amb usuaris finals distribuïts, el punt més delicat es aquest:

- el client coneix massa detalls interns de Supabase
- algunes operacions sensibles depenen de `service_role`

Per tant, la documentacio actual descriu una arquitectura funcional i coherent, pero no necessàriament la definitiva per a una explotacio gran.

## Resum executiu

- AgriSync no té API backend pròpia avui
- la seva API real és Supabase Auth + PostgREST + RPC + Admin API
- la personalització existeix, pero està al client i al SQL
- l'app podria evolucionar cap a una API pròpia
- per fer-ho bé, caldria desacoblar la capa `data/` de Supabase
