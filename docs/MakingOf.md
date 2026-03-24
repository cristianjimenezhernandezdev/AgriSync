# MakingOf AgriSync

## 1. Objectiu del projecte

AgriSync és una aplicació d'escriptori orientada a centralitzar la informació necessària per gestionar la Declaració Anual de Nitrogen (DAN) en explotacions agrícoles i ramaderes. El punt de partida del projecte és un problema real: moltes d'aquestes dades es treballen amb fulls de càlcul, documents separats i procediments manuals, cosa que provoca duplicitats, incoherències i poca traçabilitat.

L'objectiu del projecte és construir un MVP funcional que demostri:

- centralització de dades en una única base de dades
- autenticació real d'usuaris
- control d'accés per rols i permisos reals
- separació entre mòdul agrícola i mòdul ramader
- arquitectura prou neta per poder créixer en el futur

## 2. Idea funcional del programa

El concepte central del sistema és el `titular`. Un titular és la persona o entitat sobre la qual es gestionen dades agrícoles, ramaderes o de tots dos àmbits.

A partir del titular, l'aplicació es divideix en dos grans blocs:

- bloc agrícola
- bloc ramader

Quan un usuari entra al sistema:

1. s'autentica amb email i password
2. el sistema recupera el seu perfil tècnic
3. es determina el seu rol i els titulars assignats
4. es mostren només les dades que pot consultar o editar

## 3. Tecnologies escollides

### Client

- Kotlin Multiplatform
- Compose Multiplatform Desktop
- Ktor Client

### Backend i dades

- Supabase
- PostgreSQL
- Supabase Auth
- REST API automàtica de Supabase
- Row Level Security (RLS)

La tria de Supabase permet resoldre amb una sola plataforma tres necessitats molt importants del projecte:

- autenticació
- base de dades relacional
- API de dades

Això redueix complexitat tècnica i fa possible enfocar l'esforç a model de dades, permisos i casos d'ús reals.

## 4. Arquitectura general

L'aplicació segueix una arquitectura client-servidor.

### 4.1. Client desktop

La part client està dins de `composeApp` i s'encarrega de:

- mostrar pantalles
- gestionar formularis
- validar entrada bàsica
- llançar consultes i actualitzacions
- mostrar errors, càrregues i resultats

### 4.2. Capa de dades

Entre la UI i Supabase hi ha una capa pròpia formada per:

- `AuthService`
- `SupabaseAuthApi`
- `RestClient`
- `AccessRepository`
- `AgricolaRepository`
- `RamaderRepository`
- `TecnicRepository`
- `OficinaRepository`
- `TitularManagementRepository`

Això permet separar responsabilitats:

- la UI mostra dades
- el ViewModel coordina l'estat
- el Repository parla amb la base de dades

### 4.3. Base de dades

La base de dades es defineix a `SQLAgriSync.sql` i inclou:

- taules principals del MVP
- relacions entre entitats
- enums de rols i scopes
- triggers d'auditoria
- funcions helper de permisos
- RLS i policies per operació

## 5. Estructura real del codi

La part principal del projecte està en:

- `composeApp/src/commonMain/kotlin/cat/agrisync`
- `composeApp/src/jvmMain/kotlin/cat/agrisync`
- `SQLAgriSync.sql`
- `seed_complet.sql`
- `docs/`

Distribució principal:

- `App.kt`: entrada principal i navegació
- `data/`: models, auth, REST i repositoris
- `viewmodel/`: lògica d'estat de pantalles
- `ui/`: pantalles Compose
- `ui/navigation/Screen.kt`: mapa de pantalles

## 6. Ajust del codi a l'esquema actual

L'aplicació actual està alineada amb l'esquema SQL simplificat del MVP.

Això significa que:

- la home de titulars consulta directament `titular` i `tecnic_titular`
- ja no depèn de la vista `v_titular_access`
- el codi actiu no depèn de `cessio_terra` ni `emmagatzematge`
- el model funcional actiu es concentra en oficines, tècnics, titulars, terres, DAN, aplicacions, granges, bestiar, fases i entregues

## 7. Flux principal del programa

### 7.1. Arrencada

Quan l'app arrenca:

1. carrega la configuració de Supabase
2. construeix els serveis a `AppServices`
3. intenta recuperar una sessió guardada localment
4. si la troba, refresca el token
5. carrega el perfil tècnic de l'usuari autenticat
6. entra a l'aplicació si tot és correcte

### 7.2. Login

El login es fa amb Supabase Auth.

Flux:

1. l'usuari introdueix email i password
2. `LoginViewModel` valida que els camps no siguin buits
3. `AuthService` crida `SupabaseAuthApi.signInWithPassword`
4. Supabase retorna els tokens
5. l'app recupera el tècnic associat amb `get_my_tecnic()`
6. si el tècnic existeix i està actiu, es guarda la sessió localment

### 7.3. Perfil tècnic

Un usuari autenticat no n'hi ha prou. També ha d'existir a `public.tecnic`.

Això és clau perquè:

- el login real es fa contra Auth
- els permisos reals es calculen des de la BDD funcional

## 8. Model funcional de dades

El model del MVP queda reduït a les entitats realment necessàries.

### 8.1. `oficina`

Agrupa tècnics i permet limitar accés d'un `oficina_manager`.

Camps principals:

- `id`
- `nom`
- `created_at`

### 8.2. `tecnic`

Representa l'usuari operatiu del sistema.

Camps principals:

- `id`
- `oficina_id`
- `user_id`
- `nom`
- `email`
- `rol`
- `actiu`
- timestamps i camps d'auditoria

### 8.3. `titular`

Entitat central del domini.

Camps principals:

- `id`
- `nif`
- `nom_rao`
- timestamps i camps d'auditoria

### 8.4. `tecnic_titular`

Defineix l'assignació entre tècnic i titular.

Camps principals:

- `tecnic_id`
- `titular_id`
- `scope`
- `actiu`

Scopes previstos:

- `comu`
- `agricola`
- `ramader`
- `lectura`

### 8.5. `dan_declaracio`

Capçalera de campanya per titular.

### 8.6. `terra`

Representa una parcel·la o recinte agrícola.

Camps clau:

- `mun_codi`
- `poligon`
- `parcela`
- `recinte`
- `codi_sigpac_complet`
- `superficie`

### 8.7. `aplicacions_fertilitzants`

Registra aplicacions de nitrogen vinculades a una DAN i una terra.

### 8.8. `granja`

Representa una explotació ramadera del titular.

### 8.9. `bestiar`

Catàleg de tipus de bestiar.

### 8.10. `fase_productiva`

Catàleg de fases productives.

### 8.11. `granja_bestiar`

Relaciona granja, tipus de bestiar, fase productiva i cens.

### 8.12. `entrega_dejeccions`

Registra entregues de dejeccions amb destí a terra o a un altre titular.

## 9. Dades d'entrada del sistema

L'aplicació treballa amb tres grans tipus d'entrada.

### 9.1. Configuració

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`

### 9.2. Entrada d'usuari

- email i password per fer login
- dades de titular
- dades de tècnic
- dades d'oficina
- dades de terres
- dades agrícoles
- dades ramaderes

### 9.3. Catàlegs i estructura

- rols globals
- scopes
- catàleg de bestiar
- catàleg de fase productiva

## 10. Tractament de dades

### 10.1. Validació

La capa de ViewModel valida abans d'enviar a la base de dades.

Exemples:

- email i password no poden ser buits
- password mínim de 6 caràcters
- nom obligatori a titular i oficina
- `mun_codi` de 5 dígits
- camps numèrics per superfície, cens i quantitats

### 10.2. Transformació

El sistema també transforma dades:

- normalitza NIF per a cerques
- converteix text a enters o decimals
- genera `codi_sigpac_complet` a la BDD
- converteix JSON de Supabase a DTOs Kotlin
- guarda la sessió en format serialitzable

### 10.3. Filtratge i càrrega

Cada pantalla:

- posa estat de càrrega
- demana dades al repositori
- rep models Kotlin
- actualitza UI

També hi ha filtratge local:

- cerca de titulars per NIF o nom
- cerca de terres per titular o codi SIGPAC
- paginació simple

### 10.4. Edició

Quan l'usuari edita:

1. la UI recull el canvi
2. el ViewModel valida
3. el Repository envia la petició REST
4. la base de dades aplica RLS
5. Supabase retorna la representació actualitzada
6. la pantalla es refresca

### 10.5. Sessió persistent

La sessió es guarda localment amb `Preferences`.

Això permet:

- reobrir l'app sense fer login immediatament
- refrescar el token automàticament
- recuperar el perfil tècnic després d'arrencar

## 11. Tractament per mòduls

### 11.1. Home de titulars

Mostra els titulars accessibles segons rol i assignacions.

### 11.2. Mòdul agrícola

Carrega:

- titular
- terres
- aplicacions de fertilitzants

### 11.3. Mòdul ramader

Carrega:

- titular
- granges
- cens de bestiar
- entregues de dejeccions

### 11.4. Gestió de titulars

Permet crear, editar, cercar i eliminar titulars.

### 11.5. Gestió de terres

Permet alta, edició i eliminació de terres.

### 11.6. Gestió de tècnics

Permet:

- crear usuaris d'Auth
- crear el registre funcional a `public.tecnic`
- activar o desactivar tècnics
- canviar dades bàsiques
- reset de password

### 11.7. Gestió d'oficines

CRUD simple d'oficines.

## 12. Seguretat i permisos

La seguretat real es resol a la BDD.

Punts clau:

- login real amb Supabase Auth
- perfil funcional a `public.tecnic`
- rols globals
- scopes per titular
- funcions helper de permisos
- policies RLS per operació

La base de dades decideix:

- què pot veure cada usuari
- què pot modificar
- sobre quin titular
- en quin àmbit

## 13. Sortides del sistema

### 13.1. Sortida visual

- formularis
- llistes paginades
- dades de titulars
- dades agrícoles
- dades ramaderes
- missatges d'èxit i error

### 13.2. Sortida persistent

- registres nous i actualitzacions a Supabase

### 13.3. Sortida de control

- timestamps
- `created_by` i `updated_by`
- activació o desactivació de tècnics
- sessió local guardada

## 14. Relació amb el full de càlcul original

El full Excel de partida i el model de dades no són una còpia 1:1. El projecte guarda les dades base i deixa com a derivats alguns valors.

Correspondències importants:

- `Marca Oficial` -> `granja.marca_oficial`
- `Agricultor` -> `titular.nom_rao`
- `NIF Agr.` -> `titular.nif`
- `ha` -> `terra.superficie` o suma de terres
- `UF` -> `aplicacions_fertilitzants.uf`
- `kg N` -> `aplicacions_fertilitzants.kg_n`

Valors com `kg N/ha` o `kg N/UF` són derivables, per això no cal guardar-los com a camps físics en aquest MVP.

## 15. Seed i proves

Per poder provar l'aplicació amb dades reals del MVP, el projecte inclou `seed_complet.sql`.

Funcionament del seed actual:

- no escriu directament a `auth.users`
- espera que els usuaris existeixin abans a `Authentication > Users`
- després vincula aquests usuaris a `public.tecnic`
- carrega dades a totes les taules actives del MVP

Usuaris de prova previstos al seed:

- `admin.test@agrisync.com` / `admin1234`
- `manager.test@agrisync.com` / `manager1234`
- `agricola.test@agrisync.com` / `agricola1234`
- `ramader.test@agrisync.com` / `ramader1234`
- `lectura.test@agrisync.com` / `lectura1234`

## 16. Limitacions actuals del MVP

- no hi ha importador automàtic des d'Excel
- no hi ha informes oficials finals ni PDF
- no hi ha dashboard de càlculs agregats
- alguns camps del full original encara no estan modelats
- hi ha configuració de desenvolupament dins del codi que en producció caldria externalitzar millor

## 17. Estat actual verificat

Amb l'esquema SQL simplificat i els ajustos realitzats:

- l'aplicació compila correctament
- el login amb usuaris de prova ja s'ha pogut validar
- el codi i la documentació han quedat alineats amb la BDD real

## 18. Resum final

AgriSync és un MVP funcional de gestió agrària centrat en la DAN. Combina autenticació, model relacional, permisos reals, client desktop i una estructura neta per capes.

Des del punt de vista de defensa del projecte, el valor principal és que no és només una maqueta visual: és una aplicació amb autenticació real, persistència real i control d'accés real sobre dades de negoci.
