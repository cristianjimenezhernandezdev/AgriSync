# Entendre el codi d'AgriSync pas a pas

## 1. Objectiu d'aquest document

Aquest document està fet per entendre el projecte **des de zero**, encara que ara mateix no entenguis gairebé res del codi.

La idea no és només dir "què hi ha", sinó explicar:

- què fa cada peça
- per què existeix
- com es connecta amb la resta
- quin és el recorregut real de les dades

Has de llegir aquest document com si fos un mapa del projecte.

## 2. La idea més important per entendre-ho tot

Abans de mirar fitxers, has d'entendre aquesta idea:

**AgriSync és una aplicació d'escriptori que actua com a client.**

No guarda tota la lògica al propi programa. El que fa és:

1. mostrar pantalles
2. recollir dades de l'usuari
3. fer peticions a Supabase
4. mostrar les dades rebudes
5. deixar que la base de dades decideixi si l'usuari té permís o no

Això és clau.

El programa **no és**:

- un backend complet
- un servidor
- una app que decideix sola tots els permisos

El programa és sobretot:

- UI
- estat
- coordinació
- crides REST

## 3. Com pensar el projecte mentalment

La millor forma de pensar el projecte és en 5 capes:

### Capa 1. Arrencada

Obrir l'app, carregar configuració i decidir si hi ha sessió.

### Capa 2. Autenticació

Fer login, guardar la sessió, refrescar-la i obtenir el tècnic real que ha entrat.

### Capa 3. Navegació i estat principal

Decidir quina pantalla es mostra segons l'estat d'autenticació i la pantalla triada.

### Capa 4. Lògica funcional

Titulars, agrícola, ramader, DAN, gestió de tècnics, oficines i terres.

### Capa 5. Dades i permisos

Supabase, PostgreSQL, REST API i polítiques RLS.

Si mantens sempre aquestes 5 capes al cap, és molt més difícil perdre't.

## 4. Estructura general del repositori

### `composeApp/`

Ací hi ha l'aplicació desktop.

És la carpeta més important si vols entendre el codi Kotlin.

### `docs/`

Documentació del projecte.

### `SQLAgriSync.sql`

És el cor de la base de dades.

Conté:

- taules
- funcions SQL
- permisos
- RLS
- polítiques

### `seed_*.sql`

Dades de prova per poder demostrar el projecte.

## 5. Entrada real del programa

### Fitxer: `composeApp/src/jvmMain/kotlin/cat/agrisync/main.kt`

Aquest és el punt d'entrada.

Fa una cosa molt simple:

- obre la finestra desktop
- posa títol `AgriSync`
- crida `App(JvmEnvConfig())`

Traduït a llenguatge normal:

> "Arranca la finestra i passa a la capa principal de l'aplicació una manera de llegir la configuració."

Ací encara no hi ha negoci. Només arrencada de la finestra.

## 6. El fitxer més important: `App.kt`

### Què és `App.kt`?

És el **director d'orquestra** de l'aplicació.

No és on viu tota la lògica de negoci, però sí on es decideix:

- si hi ha configuració o no
- si hi ha sessió o no
- quina pantalla toca veure
- quins serveis es creen

### Què fa `App(envConfig: EnvConfig)`?

El recorregut és aquest:

1. aplica `MaterialTheme`
2. carrega la configuració amb `envConfig.load()`
3. si falta configuració, mostra `MissingConfigScreen`
4. si la configuració existeix, crea `AppServices`
5. observa l'estat d'autenticació de `AuthService`
6. crea un `LoginViewModel` persistent
7. crida `authService.initialize()`
8. segons `authState`, mostra:
   - spinner
   - loading
   - login
   - error
   - contingut autenticat

### Per què és important?

Perquè ací veus la primera gran idea del projecte:

**la UI reacciona a l'estat**

No és:

- "si l'usuari ha premut tal botó, ja veurem"

És:

- "l'estat és autenticat / no autenticat / error / carregant, i en funció d'això la UI canvia"

Aquest és un patró típic de UI reactiva.

## 7. `AuthenticatedContent`: què passa quan el login ja és correcte

Quan l'usuari ja està autenticat, `App.kt` entra a `AuthenticatedContent`.

Ací passen diverses coses:

### 7.1. Es guarda la pantalla actual

La variable:

- `currentScreen`

guarda quina pantalla s'està mostrant.

Al principi és:

- `Screen.TitularsHome`

### 7.2. Es calcula el rol

Es mira si el tècnic és:

- `admin`
- `oficina_manager`
- `tecnic`

Això serveix per mostrar o no certs botons del menú.

Important:

**això només controla la navegació de la UI, no la seguretat real.**

La seguretat real continua estant a RLS.

### 7.3. Es carrega l'oficina del tècnic

Amb `OficinaRepository.getById(...)`.

### 7.4. Es dibuixa el menú superior

Des d'ací es pot anar a:

- Titulars
- Gestió Titulars
- Terres
- Tècnics
- Oficines
- Perfil
- Logout

### 7.5. Segons `currentScreen`, es crea un ViewModel concret

Aquest punt és clau.

Per exemple:

- si la pantalla és `TitularsHome`, es crea `HomeViewModel`
- si la pantalla és `DanPreparation`, es crea `DanPreparationViewModel`
- si la pantalla és `TitularAgricola`, es crea `TitularAgricolaViewModel`
- si la pantalla és `TitularRamader`, es crea `TitularRamaderViewModel`

Després:

- es crida `load(...)`
- la pantalla mostra el `uiState`

Això vol dir que cada pantalla important té:

1. un `ViewModel`
2. un `UiState`
3. un `Repository`

Aquest patró es repeteix per quasi tot el projecte.

## 8. Navegació: `Screen.kt`

El fitxer `Screen.kt` defineix les pantalles possibles.

És una `sealed interface`.

Això, en paraules senzilles, significa:

> El programa té una llista tancada de pantalles possibles, i cada una pot portar o no portar dades.

Exemples:

- `Screen.Login`
- `Screen.TitularsHome`
- `Screen.Profile`
- `Screen.TitularAgricola(val titularId: String)`
- `Screen.TitularRamader(val titularId: String)`
- `Screen.DanPreparation(val titularId: String)`

Per què algunes porten `titularId`?

Perquè per obrir el mòdul agrícola, ramader o DAN no n'hi ha prou amb dir "vull eixa pantalla". També cal dir:

- de quin titular estem parlant

## 9. Configuració: `JvmEnvConfig.kt`

Aquest fitxer resol una necessitat pràctica:

> D'on llig l'app la URL de Supabase i les claus?

Busca la configuració en aquest ordre:

1. propietats JVM
2. variables d'entorn
3. fitxer `agrisync.properties`

També busca el fitxer en diversos directoris candidats.

Això està fet així perquè l'app es pugui executar:

- en desenvolupament
- des del directori del projecte
- o en una entrega amb `.exe` i fitxer de propietats al costat

És una decisió molt pràctica.

## 10. `AppServices.kt`: la factoria de serveis

Aquest fitxer crea els objectes principals del sistema.

Quan tens la configuració de Supabase, construeix:

- `HttpClient`
- `SupabaseAuthApi`
- `AuthService`
- `RestClient`
- `AuditRepository`
- `AccessRepository`
- `AgricolaRepository`
- `RamaderRepository`
- `DanPreparationRepository`
- `OficinaRepository`
- `TecnicRepository`
- `TitularManagementRepository`

### Quina és la idea?

En lloc de crear aquests objectes escampats per tot arreu, es creen en un únic lloc.

Això té avantatges:

- és més fàcil veure les dependències
- és més fàcil mantenir l'arquitectura
- evita duplicacions

### Idea molt important

`RestClient` rep una funció:

- `authService.sessionOrNull()`

Això vol dir que el `RestClient` pot consultar el token actual en cada petició.

## 11. Autenticació: la capa completa

Per entendre el login, has de veure 4 peces:

1. `LoginScreen`
2. `LoginViewModel`
3. `AuthService`
4. `SupabaseAuthApi`

### 11.1. `LoginScreen.kt`

És la pantalla Compose.

Fa coses de UI:

- mostra camps de text
- mostra errors
- mostra botó `Entrar`
- quan l'usuari escriu, crida mètodes del ViewModel
- quan prem entrar, crida `viewModel.login()`

Important:

La pantalla **no** parla directament amb Supabase.

La pantalla només:

- mostra estat
- envia esdeveniments

### 11.2. `LoginViewModel.kt`

Té un `LoginUiState` amb:

- email
- password
- `isLoading`
- error

Quan l'usuari prem login:

1. valida que email i password no estiguin buits
2. posa `isLoading = true`
3. crida `authService.login(email, password)`
4. si va bé, neteja password
5. si falla, prepara el missatge d'error

### 11.3. `AuthService.kt`

Aquest és el cervell de la sessió.

És una de les classes més importants del projecte.

S'encarrega de:

- inicialitzar la sessió guardada
- fer login
- refrescar tokens
- tancar sessió
- recarregar el perfil tècnic
- guardar l'estat d'autenticació en un `StateFlow`

### Quins estats d'autenticació hi ha?

L'app fa servir `AuthState`:

- `Initializing`
- `Loading`
- `Authenticated`
- `Error`
- `Unauthenticated`

### `initialize()`

Quan arrenca l'app:

1. intenta carregar una sessió guardada amb `SessionPersistence.load()`
2. si no hi ha sessió, passa a `Unauthenticated`
3. si n'hi ha, intenta refrescar-la
4. després demana el tècnic real amb `getMyTecnic(...)`
5. si el tècnic no existeix o està inactiu, tanca la sessió
6. si tot va bé, deixa l'estat en `Authenticated`
7. programa el refresh automàtic

### `login(email, password)`

Quan fas login:

1. neteja l'email
2. posa l'estat en `Loading`
3. crida `SupabaseAuthApi.signInWithPassword`
4. converteix la resposta en `Session`
5. crida `getMyTecnic`
6. comprova que el tècnic existeix i està actiu
7. guarda la sessió
8. posa estat `Authenticated`
9. programa el refresh

### `scheduleRefresh(session)`

Això està molt bé tècnicament.

Què fa?

- mira quan caduca el token
- calcula un moment una mica abans de la caducitat
- espera
- fa `refreshNow()`

Si falla:

- neteja sessió
- torna a `Unauthenticated`

És a dir:

> la sessió no depèn només del login inicial; també es manté viva automàticament

### 11.4. `SessionPersistence.kt`

A la versió JVM, la sessió es guarda amb `Preferences`.

No es guarda en una taula ni en fitxer manual.

Es serialitza un `StoredSession` i es desa en:

- `Preferences.userRoot().node("cat.agrisync.session")`

Quan tornes a obrir l'app:

- es pot recuperar la sessió anterior

### 11.5. `SupabaseAuthApi.kt`

És la classe que parla directament amb:

- `/auth/v1/token`
- `/rest/v1/rpc/get_my_tecnic`
- altres endpoints necessaris

#### `signInWithPassword`

Fa la petició real a Supabase Auth.

#### `refresh`

Refresca el token amb el `refresh_token`.

#### `getMyTecnic`

Aquesta funció és especialment important perquè connecta:

- l'usuari d'Auth
- amb el tècnic funcional de la teva aplicació

Fa diversos intents:

1. intenta `rpc/get_my_tecnic`
2. si falla, intenta buscar per `user_id`
3. si encara falla, intenta buscar per email amb `service_role_key`
4. fins i tot pot auto-corregir `user_id` si detecta desquadrament

Això està pensat per suportar situacions reals de demo o usuaris recreats.

## 12. `RestClient.kt`: el pont genèric cap a Supabase REST

Aquest fitxer és molt important per entendre el codi.

És una classe genèrica que sap fer:

- `get`
- `post`
- `patch`
- `delete`

### Què li afegeix a cada petició?

- `apikey`
- `Authorization`
- `ContentType`

### D'on surt el token?

Si hi ha sessió d'usuari:

- fa servir el `accessToken`

Si no hi ha sessió:

- usa `anonKey`

### Per què és útil?

Perquè els repositoris no han de repetir tota l'estona:

- construir URL
- posar headers
- comprovar errors HTTP

`RestClient` centralitza tot això.

### `handle(response)`

Si la resposta no és `2xx`:

- llig el cos
- fa log
- llança `ApiException`

Això fa que els repositoris puguin treballar més nets.

## 13. Model mental de la capa de dades

La capa de dades està organitzada en **repositoris**.

Cada repositori s'encarrega d'un tema funcional.

Pensa-ho així:

- `AuthService` gestiona sessió
- `AccessRepository` decideix quins titulars veus
- `AgricolaRepository` treballa la part agrícola
- `RamaderRepository` treballa la part ramadera
- `DanPreparationRepository` unifica la informació per DAN
- `TitularManagementRepository` gestiona titulars i comparticions
- `TecnicRepository` gestiona tècnics i usuaris Auth
- `OficinaRepository` gestiona oficines
- `AuditRepository` resol qui ha modificat què

## 14. `AccessRepository.kt`: qui pot veure quins titulars

Aquest repositori és clau per a la pantalla principal.

La funció important és:

- `listTitularAccessForTecnic(tecnic)`

### Què fa segons el rol?

#### Si és `admin`

Llegeix tots els titulars i dona:

- `can_agricola = true`
- `can_ramader = true`

#### Si és `oficina_manager`

Llegeix:

- tots els titulars accessibles
- les comparticions d'oficina

I segons els `scope`, decideix si pot:

- agrícola
- ramader
- o tots dos

#### Si és tècnic normal

Llig `tecnic_titular`

Cada assignació té un `scope`:

- `comu`
- `agricola`
- `ramader`

Agrupa per titular i calcula:

- `canAgricola`
- `canRamader`

### Resultat

Retorna una llista de `TitularAccessRow`.

Això és exactament el que consumeix `HomeViewModel`.

## 15. `HomeViewModel.kt`: la home de titulars

Aquest ViewModel:

- carrega titulars accessibles
- filtra per NIF o nom
- fa paginació
- carrega etiquetes d'auditoria

### Què és `actorLabels`?

Moltes files tenen:

- `updated_by`

Però això és un `user_id`, que és poc humà.

`AuditRepository` converteix aquest `user_id` en:

- nom del tècnic
- i, si pot, també email

Per això a la UI pots mostrar millor qui ha modificat un registre.

## 16. `AuditRepository.kt`

És un repositori petit però útil.

Fa una consulta a `tecnic` amb `service_role_key` per resoldre:

- `user_id -> nom/email`

Serveix per posar informació d'auditoria a les pantalles.

No és la part central del negoci, però millora molt la traçabilitat.

## 17. Mòdul agrícola: com està pensat

### Fitxers clau

- `AgricolaRepository.kt`
- `TitularAgricolaViewModel.kt`
- `TitularAgricolaScreen.kt`

### 17.1. Què guarda el mòdul agrícola?

Treballa principalment amb:

- `titular`
- `terra`
- `dan_declaracio`
- `aplicacions_fertilitzants`

### 17.2. Per què hi ha `dan_declaracio`?

Perquè el projecte treballa per **campanya**.

Les aplicacions no pengen directament només del titular.

Pengen de:

- una declaració DAN

I la declaració DAN té:

- `titular_id`
- `campanya`

Això permet separar dades de 2024, 2025, etc.

### 17.3. Què fa `AgricolaRepository`?

Fa operacions com:

- obtenir titular
- actualitzar titular
- llistar terres
- crear terra
- actualitzar terra
- eliminar terra
- llistar aplicacions d'una campanya
- crear aplicació
- actualitzar aplicació
- eliminar aplicació
- llistar campanyes
- buscar o crear `dan_declaracio`

### 17.4. Idea clau: `getOrCreateDan`

Quan crees una aplicació:

- primer es comprova si ja hi ha `dan_declaracio` per a aquell titular i campanya
- si no hi és, es crea

Això és una decisió elegant.

Per què?

Perquè no obligues l'usuari a crear manualment la campanya abans de treballar.

La campanya existeix quan realment fa falta.

### 17.5. `TitularAgricolaViewModel`

Té un `TitularAgricolaUiState` amb:

- titular
- terres
- aplicacions
- campanyes disponibles
- campanya seleccionada
- labels d'auditoria
- loading/error/saveMessage

### `load(titularId, preferredCampanya)`

Carrega:

1. titular
2. terres
3. campanyes existents
4. campanya seleccionada
5. aplicacions de la campanya
6. etiquetes d'auditoria

### Validacions

El ViewModel valida abans de cridar el repositori.

Per exemple:

- superfície és número
- zona és `ZV` o `ZNV`
- data és `YYYY-MM-DD`
- kg N i UF no són negatius

Això és important perquè la UI no queda "tonta":

- la pantalla envia
- el ViewModel valida
- el repositori persisteix

### 17.6. `TitularAgricolaScreen`

És la pantalla Compose que:

- observa `uiState`
- mostra titular
- mostra selector de campanya
- mostra terres
- mostra aplicacions
- obri diàlegs de crear
- mostra snackbars amb missatges

Ací pots veure molt bé el patró del projecte:

1. la pantalla llig estat
2. la pantalla no coneix Supabase
3. la pantalla només crida mètodes del ViewModel

### 17.7. Què aporta funcionalment aquest mòdul?

No és només un CRUD de terres.

També:

- mostra límit de kg N/ha
- calcula kg N aplicat per terra
- calcula marge disponible
- separa dades per campanya

## 18. Mòdul ramader: com està pensat

### Fitxers clau

- `RamaderRepository.kt`
- `TitularRamaderViewModel.kt`
- pantalla `TitularRamaderScreen.kt`

### 18.1. Quines entitats toca?

- `granja`
- `granja_bestiar`
- `bestiar`
- `fase_productiva`
- `entrega_dejeccions`
- `terra`
- `titular`
- `dan_declaracio`

### 18.2. Què fa `RamaderRepository`?

Permet:

- llistar granges
- crear/editar/esborrar granges
- llistar terres accessibles
- llistar titulars accessibles
- llistar catàleg de bestiar
- llistar catàleg de fases productives
- llistar cens per granja
- crear/editar/esborrar cens
- llistar entregues per campanya
- crear/editar/esborrar entregues
- buscar o crear `dan_declaracio`

### 18.3. Per què és important `listAccessibleTitulars` i `listAccessibleTerres`?

Perquè una entrega de dejeccions pot anar:

- a un titular receptor
- o a una terra concreta

I eixos receptors no tenen per què ser del mateix titular.

Això fa el sistema més realista.

### 18.4. `TitularRamaderViewModel`

El seu `UiState` és més ric que l'agrícola.

Té:

- titular
- granges
- cens
- entregues
- terres del titular
- titulars receptors
- terres receptores
- campanyes
- catàleg de bestiars
- catàleg de fases
- auditoria

### Validacions importants

Valida que:

- la granja tingui marca oficial
- el cens sigui numèric i no negatiu
- la data sigui correcta
- la quantitat sigui numèrica i no negativa
- en una entrega hi hagi exactament un tipus de receptor

Aquest últim punt és molt important:

- o tries terra
- o tries titular
- però no tots dos i tampoc cap dels dos

### 18.5. Per què aquest mòdul és interessant?

Perquè no és un simple "guardar granja".

Modela una part del món real:

- una granja
- té cens
- el cens es desglossa per bestiar i fase
- hi ha entregues de dejeccions
- les entregues tenen un destí
- i tot això es projecta sobre una campanya

## 19. `DanPreparationViewModel`: la peça de síntesi

Aquest ViewModel és probablement el més interessant de cara a negoci.

Per què?

Perquè és on el projecte deixa de ser només un conjunt de CRUDs i passa a ser una eina d'ajuda real.

### Què carrega?

- titular
- terres
- aplicacions
- granges
- cens
- entregues
- campanyes

### Què calcula?

- total hectàrees
- total kg N
- total UF
- total cens
- total quantitat entregada
- kg N/ha
- kg N/UF
- kg N per terra

### `automaticChecklistItems()`

Aquesta funció és molt bona per entendre el valor del projecte.

Fa comprovacions automàtiques com:

- terres sense municipi/us/cultiu
- terres sense superfície
- aplicacions sense origen
- aplicacions sense volum o kg N/m3
- granges sense cens
- granges sense entregues
- entregues sense receptor resolt

I si no detecta buits:

- ho diu igualment

I a més:

- recorda els camps que encara no modela el MVP

### `buildClipboardSummary()` i `buildClipboardChecklist()`

Aquestes funcions generen textos llargs preparats per copiar.

Per tant, `Preparar DAN` no és només una pantalla de lectura.

És:

- lectura
- càlcul
- verificació
- exportació textual ràpida

## 20. Gestió administrativa: titulars, terres, tècnics i oficines

Hi ha una part del projecte orientada a administració.

### 20.1. `TitularManagementRepository`

Gestiona:

- titulars
- oficines
- comparticions entre oficines
- terres a nivell global

### Comparticions `oficina_titular_compartit`

Aquest concepte és molt important.

No tot titular compartit ha de compartir-se igual.

El `scope` permet compartir:

- agrícola
- ramader
- comú

Això fa el model més flexible.

### 20.2. `TitularManagementViewModel`

Gestiona:

- alta de titular
- edició
- eliminació
- diàleg de compartició
- creació i eliminació de comparticions
- cerca i paginació

### 20.3. `TerraManagementViewModel`

És una pantalla més "d'oficina":

- llista totes les terres
- filtra per titular
- cerca per SIGPAC o titular
- permet crear/editar/esborrar

### 20.4. `TecnicRepository`

És especial perquè no només parla amb REST normal.

També parla amb la **Admin API de Supabase** per:

- crear usuaris Auth
- canviar passwords
- eliminar usuaris Auth

I alhora treballa amb la taula `public.tecnic`.

Ací hi ha una distinció molt important:

### Usuari Auth vs tècnic funcional

No són exactament el mateix.

Un usuari Auth és:

- identitat de login

Un tècnic funcional és:

- perfil de l'aplicació
- nom
- oficina
- rol
- estat actiu

Normalment van relacionats per `user_id`, però conceptualmente no són la mateixa peça.

## 21. Les dades principals del domini

Ara toca entendre les entitats més importants.

### `oficina`

Representa una oficina de treball.

### `tecnic`

Representa un usuari funcional del sistema.

Té:

- oficina
- nom
- email
- rol
- actiu/inactiu
- user_id cap a Auth

### `titular`

És el centre funcional del projecte.

Un titular pot tenir:

- terres
- granges
- DANs
- assignacions a tècnics
- comparticions amb oficines

### `tecnic_titular`

Relaciona tècnics amb titulars.

I afegeix `scope`.

### `oficina_titular_compartit`

Permet compartir titulars entre oficines amb un àmbit concret.

### `dan_declaracio`

Representa la DAN d'un titular per a una campanya concreta.

### `terra`

Representa la part agrícola.

### `aplicacions_fertilitzants`

Representa aplicacions agrícoles lligades a una DAN.

### `granja`

Representa instal·lacions ramaderes del titular.

### `bestiar`

Catàleg de tipus de bestiar.

### `fase_productiva`

Catàleg de fases productives.

### `granja_bestiar`

Relaciona granja + bestiar + fase + cens.

### `entrega_dejeccions`

Representa moviments/entregues de dejeccions lligats a la DAN.

## 22. La part més important de seguretat: SQL i RLS

### On està?

Principalment en `SQLAgriSync.sql`.

### Què conté?

- creació de taules
- índexs
- triggers
- funcions helper
- `grant`
- activació de row level security
- polítiques per taula

### Quines funcions helper són clau?

Algunes de les més importants:

- `get_my_tecnic()`
- `current_oficina_id()`
- `is_admin()`
- `is_oficina_manager()`
- `same_oficina(...)`
- `can_self_update_tecnic(...)`
- `can_manage_office_titular(...)`
- `can_read_titular(...)`
- `can_write_scope(...)`
- `can_write_agricola(...)`
- `can_write_ramader(...)`
- `can_reference_terra(...)`

### Què fan aquestes funcions?

No guarden dades. Decideixen si una acció és legal o no.

Per exemple:

- pot aquest usuari llegir aquest titular?
- pot aquest manager gestionar aquest titular?
- pot aquest tècnic escriure part agrícola?
- pot referenciar aquesta terra?

### Per què això és tan potent?

Perquè després les polítiques RLS les fan servir.

Per exemple, una política pot dir:

- pots seleccionar `titular` si `can_read_titular(titular.id)` és cert

### Què guanyes amb això?

Que el criteri de seguretat:

- no està duplicat a cada consulta manual
- no està depenent només de la UI
- queda centralitzat en funcions SQL

## 23. RLS en llenguatge molt pla

RLS significa:

> Encara que la taula existeixi, cada fila decideix si aquest usuari la pot llegir o tocar.

Per tant:

- dos usuaris poden fer la mateixa consulta REST
- però rebre resultats diferents

No perquè el client filt ri, sinó perquè la base de dades ja els entrega filtrats.

Això és una idea fonamental del projecte.

## 24. Triggers d'auditoria

El SQL també defineix auditoria.

Això serveix per omplir camps com:

- `created_at`
- `updated_at`
- `created_by`
- `updated_by`

Després la UI pot mostrar:

- qui ha tocat un registre
- quan s'ha modificat

## 25. El patró UI del projecte

Ara que ja hem vist dades i viewmodels, toca entendre la UI.

### Patró general

Cada pantalla important fa això:

1. rep un `ViewModel`
2. fa `collectAsState()`
3. llig `uiState`
4. mostra el que toca segons eixe estat
5. quan l'usuari fa alguna cosa, crida mètodes del ViewModel

És a dir:

- la pantalla no implementa la regla de negoci
- la pantalla no fa directament la consulta REST
- la pantalla és una capa de presentació

### Exemple: `TitularAgricolaScreen`

Fa:

- `val ui by viewModel.uiState.collectAsState()`
- si `isLoading`, spinner
- si `error`, bloc d'error
- si no, mostra dades
- crea diàlegs
- envia accions cap al ViewModel

Això és un patró molt net.

## 26. Com viatgen les dades de punta a punta

Aquesta és una de les preguntes més importants.

Exemple: crear una aplicació fertilitzant.

### Pas 1. Usuari

L'usuari obri el diàleg i escriu dades.

### Pas 2. Pantalla

`TitularAgricolaScreen` recull els camps i crida:

- `viewModel.createAplicacio(...)`

### Pas 3. ViewModel

`TitularAgricolaViewModel`:

- valida dades
- comprova terra
- comprova format de data
- comprova números

Si és correcte:

- crida `repository.createAplicacio(...)`

### Pas 4. Repository

`AgricolaRepository`:

- busca o crea la `dan_declaracio`
- fa `POST` a `aplicacions_fertilitzants`

### Pas 5. RestClient

Construeix URL, headers i token.

### Pas 6. Supabase/PostgreSQL

La base de dades comprova:

- si el token és vàlid
- si l'RLS permet escriure

### Pas 7. Resposta

Torna el registre creat.

### Pas 8. ViewModel

Actualitza el `uiState`.

### Pas 9. UI

Compose detecta el canvi i es redibuixa.

Aquest patró és quasi sempre el mateix.

## 27. Flux complet d'usuari 1: login

1. l'usuari entra email i password
2. `LoginScreen` crida `LoginViewModel.login()`
3. el ViewModel crida `AuthService.login()`
4. `AuthService` crida `SupabaseAuthApi.signInWithPassword`
5. Supabase retorna `access_token` i `refresh_token`
6. `AuthService` busca el tècnic funcional
7. guarda la sessió
8. posa `AuthState.Authenticated`
9. `App.kt` reacciona i mostra `AuthenticatedContent`

## 28. Flux complet d'usuari 2: obrir mòdul agrícola

1. des de `TitularsHome` l'usuari prem "Agrícola"
2. `currentScreen = Screen.TitularAgricola(titularId)`
3. `App.kt` crea `TitularAgricolaViewModel`
4. `LaunchedEffect` crida `vm.load(titularId)`
5. el ViewModel carrega titular, terres, campanyes i aplicacions
6. la pantalla mostra les dades

## 29. Flux complet d'usuari 3: preparar DAN

1. l'usuari prem `Preparar DAN`
2. s'obre `DanPreparationScreen`
3. el ViewModel carrega dades de diverses fonts
4. calcula totals
5. calcula checklist
6. mostra resum
7. si l'usuari vol, copia text al porta-retalls

## 30. On està la lògica de negoci i on no està

Per no perdre't, recorda això:

### La UI fa:

- mostrar
- recollir inputs
- obrir diàlegs
- mostrar errors i snackbars

### El ViewModel fa:

- mantenir estat
- validar
- decidir quan carregar
- cridar el repositori
- transformar resultats en `UiState`

### El Repository fa:

- parlar amb l'API REST
- encapsular consultes
- crear o actualitzar dades

### SQL/RLS fa:

- decidir si realment tens permís
- definir relacions i estructura

## 31. Coses que estan ben resoltes al projecte

### 31.1. Separació de capes

No hi ha una barreja caòtica de UI i dades.

### 31.2. Sessió persistent i refresh

És millor que un login bàsic.

### 31.3. Permisos a base de dades

És un punt tècnic fort.

### 31.4. Campanya com a eix real

No és un camp decoratiu.

### 31.5. `Preparar DAN` com a peça de valor

Ajuda a treballar, no només a guardar registres.

### 31.6. Esquema SQL reconstructiu

Pots recrear el sistema des de zero.

## 32. Limitacions que has d'entendre

Per entendre bé el codi, també cal entendre el que **encara no fa**:

- no genera el document oficial final complet
- no tota la normativa de DAN està modelada
- hi ha ús de `service_role_key` que en producció s'hauria de treure del client
- hi ha automatitzacions que encara són assistència al tècnic, no tancament complet del procés

Això no invalida el projecte.

Només el situa correctament com a MVP.

## 33. Per què hi ha coses "estranyes" al codi d'autenticació

Potser, llegint `SupabaseAuthApi`, et sorprèn:

- extracció de `user_id` del JWT
- fallback per email
- auto-fix de `user_id`

Això no és "bonic" en sentit acadèmic pur, però respon a un problema real:

- en demos i recreacions d'usuaris de Supabase, `auth.users` i `public.tecnic` poden desalinear-se

Aquesta capa s'ha fet més robusta per aguantar aquests casos.

És important que ho entenguis:

> no tot el que és bo en un sistema és "minimalista"; a vegades és millor fer-lo resistent a problemes reals

## 34. La millor manera d'estudiar el projecte

Si el vols entendre de debò, no comences per fitxers enormes aleatoris.

L'ordre recomanat és:

1. `main.kt`
2. `App.kt`
3. `Screen.kt`
4. `JvmEnvConfig.kt`
5. `AppServices.kt`
6. `AuthService.kt`
7. `SupabaseAuthApi.kt`
8. `RestClient.kt`
9. `HomeViewModel.kt`
10. `AccessRepository.kt`
11. `TitularAgricolaViewModel.kt`
12. `AgricolaRepository.kt`
13. `TitularRamaderViewModel.kt`
14. `RamaderRepository.kt`
15. `DanPreparationViewModel.kt`
16. `TitularManagementViewModel.kt`
17. `TecnicRepository.kt`
18. `SQLAgriSync.sql`

Aquest ordre té sentit perquè va de:

- arrencada
- sessió
- navegació
- dades
- permisos

## 35. Com explicar-te el projecte amb una metàfora

Si et costa molt veure-ho, pensa en AgriSync com una oficina.

### `main.kt`

Obri la porta de l'oficina.

### `App.kt`

És recepció. Mira si tens credencials i et diu on has d'anar.

### `AuthService`

És control d'accés i acreditacions.

### `RestClient`

És el missatger que porta formularis entre l'oficina i l'arxiu.

### Repositoris

Són els departaments especialitzats:

- agrícola
- ramader
- titulars
- tècnics

### ViewModels

Són els responsables de cada taulell. Organitzen el treball i expliquen a la pantalla què ha d'ensenyar.

### UI

És el taulell visible al públic.

### SQL + RLS

És l'arxiu central i la normativa interna que decideix qui pot veure cada expedient.

## 36. Què has d'entendre sí o sí per dir que "ja ho entens"

Si entens bé aquests 10 punts, ja tens el projecte bastant controlat:

1. `main.kt` només arrenca la finestra
2. `App.kt` decideix què mostrar segons estat i sessió
3. `AppServices` crea tots els serveis i repositoris
4. `AuthService` és el cervell de la sessió
5. `RestClient` centralitza les crides REST
6. cada pantalla important té `ViewModel + UiState + Repository`
7. agrícola i ramader treballen per campanya
8. `dan_declaracio` és la peça que relaciona dades amb campanya
9. `Preparar DAN` unifica i calcula
10. la seguretat real està a `SQLAgriSync.sql` amb RLS

## 37. Resum final molt clar

AgriSync està fet així:

- **entrada** a `main.kt`
- **coordinació principal** a `App.kt`
- **configuració** a `JvmEnvConfig.kt`
- **composició de serveis** a `AppServices.kt`
- **autenticació** a `AuthService.kt` i `SupabaseAuthApi.kt`
- **peticions REST** a `RestClient.kt`
- **lògica funcional** en repositoris
- **estat de pantalla** en viewmodels
- **presentació** en pantalles Compose
- **seguretat real** en `SQLAgriSync.sql`

La idea forta és aquesta:

> la UI no governa les dades; la UI coordina l'usuari, el ViewModel coordina l'estat, el repositori parla amb Supabase i la base de dades decideix si l'acció és legal.

Si memoritzes i entens això, ja no veuràs el projecte com un munt de fitxers inconnexos, sinó com un sistema amb una estructura clara.
