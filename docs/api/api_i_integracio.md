# API i integracio amb Supabase

## Objectiu

Aquest document explica quina API utilitza AgriSync, on es troba el codi d'integracio i quin paper tenen Supabase, els repositoris Kotlin i les funcions SQL.

La situacio actual del projecte es aquesta:

- no hi ha un backend propi separat
- el client desktop consumeix directament Supabase
- la API real esta formada per Supabase Auth, PostgREST, RPC SQL i Admin API
- la logica especifica del projecte esta repartida entre la capa `data/` del client i l'esquema SQL

## Tipus d'API utilitzats

AgriSync utilitza quatre superficies d'API:

| Superficie | Us dins del projecte |
|---|---|
| Supabase Auth API | Login, refresh de sessio i gestio d'usuaris Auth |
| Supabase PostgREST | Consultes i operacions CRUD sobre taules publicades |
| RPC SQL via PostgREST | Funcions controlades com `get_my_tecnic`, `create_titular` i `create_terra` |
| Supabase Admin API | Creacio, eliminacio i canvi de password d'usuaris Auth |

## Fitxers principals

| Fitxer | Responsabilitat |
|---|---|
| `composeApp/src/commonMain/kotlin/cat/agrisync/data/SupabaseAuthApi.kt` | Crides a Auth, refresh de token i resolucio del tecnic autenticat |
| `composeApp/src/commonMain/kotlin/cat/agrisync/data/AuthService.kt` | Orquestra sessio, persistencia, login, logout i refresc |
| `composeApp/src/commonMain/kotlin/cat/agrisync/data/RestClient.kt` | Wrapper centralitzat de `GET`, `POST`, `PATCH` i `DELETE` contra `/rest/v1` |
| `composeApp/src/commonMain/kotlin/cat/agrisync/data/AppServices.kt` | Crea el client HTTP, AuthService, RestClient i repositoris |
| `composeApp/src/commonMain/kotlin/cat/agrisync/data/TecnicRepository.kt` | Gestio de tecnics, assignacions i operacions Admin API |
| `composeApp/src/commonMain/kotlin/cat/agrisync/data/AgricolaRepository.kt` | Consultes i escriptures del modul agricola |
| `composeApp/src/commonMain/kotlin/cat/agrisync/data/RamaderRepository.kt` | Consultes i escriptures del modul ramader |
| `composeApp/src/commonMain/kotlin/cat/agrisync/data/DanPreparationRepository.kt` | Lectura agregada per a la pantalla `Preparar DAN` |
| `docs/sql/schema/agrisync_schema.sql` | Funcions SQL, triggers, RLS i RPC exposades a PostgREST |

## Supabase Auth API

La autenticacio es fa contra Supabase Auth.

Endpoints utilitzats:

- `POST /auth/v1/token?grant_type=password`
- `POST /auth/v1/token?grant_type=refresh_token`

Flux principal:

1. `LoginViewModel` envia email i contrasenya a `AuthService.login()`.
2. `AuthService` crida `SupabaseAuthApi.signInWithPassword(...)`.
3. Supabase retorna `access_token` i `refresh_token`.
4. `AuthService` recupera el perfil funcional amb `getMyTecnic(...)`.
5. La sessio queda guardada i l'aplicacio entra a l'estat autenticat.

El login real no depen nomes d'`auth.users`. L'aplicacio necessita trobar tambe un registre actiu a `public.tecnic`.

## Resolucio del tecnic autenticat

La funcio principal es:

- `SupabaseAuthApi.getMyTecnic(accessToken, loginEmail)`

Ordre de resolucio:

1. Crida la RPC SQL `get_my_tecnic()`.
2. Si cal, consulta `public.tecnic` per `user_id`.
3. Si encara no es pot resoldre i hi ha `service_role`, consulta per email.
4. Si detecta un desajust entre l'usuari Auth i `public.tecnic.user_id`, pot actualitzar la relacio.

La RPC SQL esta definida a:

- `docs/sql/schema/agrisync_schema.sql`

## PostgREST i RestClient

La majoria de consultes del projecte passen per:

- `composeApp/src/commonMain/kotlin/cat/agrisync/data/RestClient.kt`

Aquest client construeix URLs del tipus:

```text
${SUPABASE_URL}/rest/v1/<taula>?select=...
```

Responsabilitats de `RestClient`:

- afegir `apikey`
- afegir `Authorization`
- utilitzar el token de sessio quan existeix
- usar `Prefer: return=representation` en `POST` i `PATCH`
- convertir errors HTTP en `ApiException`

Taules consultades mitjancant PostgREST:

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
- `granja_campanya_balance`
- `entrega_dejeccions`

## RPC SQL exposades com API

Algunes operacions passen per funcions SQL en comptes d'inserts directes.

| RPC | Us |
|---|---|
| `get_my_tecnic()` | Retorna el perfil funcional del tecnic autenticat |
| `create_titular(...)` | Alta controlada de titular amb permisos i auditoria |
| `create_terra(...)` | Alta controlada de terra amb validacions i codi SIGPAC generat |

Aquestes funcions formen part del contracte real d'API del projecte, encara que estiguin implementades a PostgreSQL.

## Supabase Admin API

La Admin API s'utilitza des de:

- `composeApp/src/commonMain/kotlin/cat/agrisync/data/TecnicRepository.kt`

Operacions principals:

- crear usuaris Auth
- canviar passwords
- eliminar usuaris Auth
- resoldre dades administratives quan cal `service_role`

Endpoints utilitzats:

- `POST /auth/v1/admin/users`
- `PUT /auth/v1/admin/users/{id}`
- `DELETE /auth/v1/admin/users/{id}`

Aquestes operacions requereixen `SUPABASE_SERVICE_ROLE_KEY`. En l'estat actual del projecte, aquesta clau forma part de la configuracio local necessaria per a les funcions administratives.

## Repositoris com a capa d'integracio

Els repositoris son la capa que coneix les consultes concretes de cada modul.

| Repositori | Taules o funcions principals |
|---|---|
| `AccessRepository` | `titular`, `tecnic_titular`, `oficina_titular_compartit` |
| `AgricolaRepository` | `titular`, `terra`, `dan_declaracio`, `aplicacions_fertilitzants`, `create_terra` |
| `RamaderRepository` | `granja`, `granja_bestiar`, `granja_campanya_balance`, `entrega_dejeccions`, `terra` |
| `DanPreparationRepository` | lectura agregada de titular, terres, aplicacions, granges, cens, balanços i entregues |
| `TitularManagementRepository` | `titular`, `oficina_titular_compartit`, `terra`, `create_titular`, `create_terra` |
| `TecnicRepository` | `tecnic`, `tecnic_titular`, `oficina`, Admin API |
| `OficinaRepository` | `oficina` |
| `AuditRepository` | resolucio de noms d'actor d'auditoria |

## Flux general d'una consulta

El flux habitual es:

1. la pantalla Compose recull una accio o necessita dades
2. el ViewModel actualitza estat i crida el repositori
3. el repositori construeix la consulta PostgREST o RPC
4. `RestClient` envia la peticio HTTP
5. PostgreSQL aplica RLS, triggers i restriccions
6. Supabase retorna JSON
7. Kotlin deserialitza la resposta en DTOs
8. el ViewModel actualitza el `UiState`
9. la UI es recomposa

## API interna o externa

AgriSync no te una API backend propia ni una API externa de tercers per al domini DAN.

La API utilitzada es externa en el sentit que el servei HTTP el proporciona Supabase, pero el contracte funcional es propi del projecte:

- taules dissenyades per AgriSync
- DTOs Kotlin adaptats al model SQL
- RPCs SQL especifiques
- triggers propis
- policies RLS propies
- repositoris amb consultes PostgREST del domini

Per tant, la integracio actual es una API Supabase personalitzada pel projecte.

## Seguretat de les consultes

La UI pot mostrar o amagar botons, pero la seguretat efectiva no depen de la UI.

La validacio real es fa a:

- Supabase Auth
- funcions SQL de permisos
- policies RLS
- triggers i restriccions de PostgreSQL

Funcions destacades:

- `can_read_titular(...)`
- `can_write_scope(...)`
- `can_write_agricola(...)`
- `can_write_ramader(...)`
- `can_reference_terra(...)`
- `office_has_shared_scope(...)`

## Cas especial: entregues i aplicacions fertilitzants

Quan es crea o modifica una entrega de dejeccions, la base de dades pot sincronitzar una aplicacio fertilitzant equivalent.

Elements implicats:

- taula `entrega_dejeccions`
- taula `aplicacions_fertilitzants`
- trigger `trg_entrega_sync_aplicacio`
- funcio `sync_entrega_to_aplicacio()`
- funcio `find_or_create_dan(...)`

Aixo connecta el modul ramader amb el modul agricola sense que el client hagi de crear manualment dues operacions separades.

## Resum

- L'aplicacio no te backend propi separat.
- El client desktop consumeix Supabase directament.
- `RestClient.kt` centralitza les crides PostgREST.
- `SupabaseAuthApi.kt` centralitza Auth i resolucio del tecnic.
- Els repositoris defineixen les consultes de cada modul.
- Les RPC, triggers i RLS de PostgreSQL formen part de la API real del sistema.
