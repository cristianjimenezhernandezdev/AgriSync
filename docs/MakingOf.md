# MakingOf AgriSync

## 1. Objectiu del projecte

AgriSync és una aplicació d'escriptori pensada per centralitzar la gestió de dades relacionades amb la Declaració Anual de Nitrogen (DAN) en explotacions agrícoles i ramaderes. El problema de partida és que aquesta informació sovint es gestiona amb fulls de càlcul, documents separats i processos manuals. Això provoca duplicació, poca traçabilitat i dificultat per controlar qui pot veure o modificar cada dada.

L'objectiu del projecte no és construir encara un producte complet per a tota la gestió agrària, sinó un MVP funcional que demostri:

- que és possible unificar la informació en una sola base de dades
- que es pot treballar amb control d'accés real segons usuari i rol
- que la informació agrícola i ramadera es pot consultar i editar des d'una sola aplicació
- que l'arquitectura ja queda preparada per créixer en el futur

En resum, el projecte resol una necessitat real amb un abast acotat i defensable per a un projecte final de cicle.

## 2. Idea funcional del programa

El programa treballa al voltant del concepte de `titular`. Un titular és la persona o entitat que té associada activitat agrícola, ramadera o totes dues.

A partir d'aquí, l'aplicació divideix la informació en dos grans blocs:

- Bloc agrícola
- Bloc ramader

Cada tècnic inicia sessió i el sistema només li mostra els titulars i les dades que li corresponen segons els permisos que té assignats.

L'ús típic del programa és aquest:

1. El tècnic inicia sessió.
2. El sistema comprova qui és i quin rol té.
3. Es carrega la llista de titulars als quals pot accedir.
4. L'usuari entra al mòdul agrícola o ramader d'un titular.
5. El sistema mostra les dades relacionades amb aquell titular.
6. Si l'usuari té permisos d'escriptura, pot editar la informació.
7. Les dades es guarden a Supabase i queden subjectes a RLS.

## 3. Tecnologies i motius de la tria

### Client

- Kotlin Multiplatform
- Compose Multiplatform Desktop
- Ktor Client

S'ha triat Kotlin perquè permet tenir una base moderna, fortament tipada i preparada per a compartir lògica si en el futur es vol una versió Android. Compose Desktop encaixa bé amb un MVP acadèmic perquè permet construir interfície d'escriptori ràpidament sense haver de fer una aplicació web.

### Backend i dades

- Supabase
- PostgreSQL
- Supabase Auth
- REST API automàtica de Supabase
- Row Level Security (RLS)

Supabase s'ha triat perquè resol tres necessitats del projecte amb una sola plataforma:

- autenticació
- base de dades relacional
- API de dades sense haver de construir un backend tradicional des de zero

Per un projecte final de curs, això redueix complexitat però permet demostrar arquitectura real.

## 4. Arquitectura general

L'aplicació està pensada amb arquitectura client-servidor.

### 4.1. Client

El client és la part desktop i està dins de `composeApp`. Aquesta capa s'encarrega de:

- mostrar pantalles
- gestionar formularis
- validar dades bàsiques d'entrada
- llançar crides a la capa de dades
- representar errors, llistes i estats de càrrega

### 4.2. Capa de dades

Entre la UI i Supabase hi ha una capa pròpia formada per:

- `AuthService`
- `SupabaseAuthApi`
- `RestClient`
- repositoris (`AccessRepository`, `AgricolaRepository`, `RamaderRepository`, `TecnicRepository`, `OficinaRepository`, `TitularManagementRepository`)

La idea és separar responsabilitats:

- la UI no parla directament amb HTTP
- els repositoris encapsulen com es consulta o modifica cada conjunt de dades
- l'autenticació té la seva pròpia lògica separada

### 4.3. Base de dades

La base de dades és PostgreSQL dins Supabase. Aquí és on hi ha:

- les taules principals
- les relacions entre entitats
- els enums de rols i scopes
- els triggers d'auditoria
- les funcions de permisos
- les polítiques RLS

És una decisió important del projecte: part de la lògica de seguretat no es deixa a la UI, sinó a la base de dades.

## 5. Estructura real del codi

La part principal del projecte està en:

- `composeApp/src/commonMain/kotlin/cat/agrisync`
- `composeApp/src/jvmMain/kotlin/cat/agrisync`

La distribució funcional és aquesta:

- `App.kt`: punt d'entrada de l'app Compose, controla autenticació i navegació
- `data/`: models, autenticació, REST i repositoris
- `viewmodel/`: estat i lògica de pantalla
- `ui/`: composables de cada pantalla
- `ui/navigation/Screen.kt`: definició de les pantalles
- `SQLAgriSync.sql`: base de dades del projecte

### 5.1. Ajust a l'esquema actual

L'aplicació actual està alineada amb l'esquema SQL simplificat del MVP:

- la home de titulars consulta directament `titular` i `tecnic_titular`
- ja no depèn de la vista `v_titular_access`
- el codi actiu no fa servir `cessio_terra` ni `emmagatzematge`
- el nucli funcional queda centrat en oficines, tècnics, titulars, terres, DAN, aplicacions, granges, bestiar, fases i entregues

## 6. Flux principal del programa

### 6.1. Arrencada

Quan l'aplicació arrenca:

1. Carrega configuració de Supabase.
2. Construeix els serveis principals a `AppServices`.
3. Intenta recuperar una sessió guardada localment.
4. Si existeix, refresca el token.
5. Recupera el perfil tècnic associat a l'usuari.
6. Si tot és correcte, entra a l'aplicació.

### 6.2. Login

El login es fa amb email i password contra Supabase Auth.

Flux:

1. L'usuari escriu email i contrasenya.
2. `LoginViewModel` valida que no estiguin buits.
3. `AuthService` crida `SupabaseAuthApi.signInWithPassword`.
4. Supabase retorna `access_token` i `refresh_token`.
5. Amb aquest token, l'app intenta obtenir el tècnic associat.
6. Si existeix i està actiu, la sessió es guarda localment.
7. L'usuari passa a estat autenticat.

### 6.3. Recuperació del perfil tècnic

El perfil del tècnic és clau perquè defineix els permisos. Es resol principalment a través de la funció SQL `get_my_tecnic()`, que busca a la taula `public.tecnic` el registre que tingui `user_id = auth.uid()`.

Això vol dir que:

- una identitat d'Auth sola no n'hi ha prou
- l'usuari també ha d'existir com a tècnic dins la base de dades funcional

## 7. Model funcional de dades

El model de dades s'ha simplificat per quedar-se amb el mínim necessari per al MVP actual.

### 7.1. Oficina

Representa una oficina o unitat organitzativa.

Camps principals:

- `id`
- `nom`
- `created_at`

Funció dins del sistema:

- agrupar tècnics
- limitar la visibilitat dels `oficina_manager`

### 7.2. Tècnic

Representa l'usuari operatiu del sistema.

Camps principals:

- `id`
- `oficina_id`
- `user_id`
- `nom`
- `email`
- `rol`
- `actiu`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`

Funció:

- identificar qui ha iniciat sessió
- saber si és `admin`, `oficina_manager`, `tecnic` o `lectura`
- controlar permisos

### 7.3. Titular

És l'entitat central del domini.

Camps principals:

- `id`
- `nif`
- `nom_rao`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`

Funció:

- representar la persona o entitat de referència
- servir de punt d'unió entre mòdul agrícola i mòdul ramader

### 7.4. Assignació tècnic-titular

Relació entre un tècnic i un titular.

Camps principals:

- `id`
- `tecnic_id`
- `titular_id`
- `scope`
- `actiu`
- `created_at`
- `created_by`

`scope` pot ser:

- `comu`
- `agricola`
- `ramader`
- `lectura`

Funció:

- definir sobre quin titular pot treballar un tècnic
- definir si ho fa a nivell agrícola, ramader o general

### 7.5. DAN declaració

Representa la declaració d'un titular per a una campanya.

Camps principals:

- `id`
- `titular_id`
- `campanya`
- `estat`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`

Funció:

- actuar com a capçalera de campanya
- lligar-hi aplicacions de fertilitzants i entregues de dejeccions

### 7.6. Terra

Representa una parcel·la o recinte agrícola.

Camps principals:

- `id`
- `titular_id`
- `mun_codi`
- `poligon`
- `parcela`
- `recinte`
- `codi_sigpac_complet`
- `superficie`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`

Funció:

- emmagatzemar la base territorial de la part agrícola
- identificar una terra mitjançant dades SIGPAC
- relacionar aplicacions i destins

Observació:

`codi_sigpac_complet` no s'introdueix manualment. Es genera a partir de `mun_codi`, `poligon`, `parcela` i `recinte`.

### 7.7. Aplicacions de fertilitzants

Representa una aplicació de nitrogen o fertilització vinculada a una DAN i una terra.

Camps principals:

- `id`
- `dan_id`
- `terra_id`
- `data`
- `kg_n`
- `uf`
- `tecnic_id`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`

Funció:

- registrar què s'ha aplicat, quan i sobre quina terra

### 7.8. Granja

Representa una explotació ramadera.

Camps principals:

- `id`
- `titular_id`
- `marca_oficial`
- `nom`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`

Funció:

- identificar l'explotació ramadera associada al titular

### 7.9. Bestiar

Catàleg de tipus de bestiar.

Camps principals:

- `id`
- `codi`
- `descripcio`

### 7.10. Fase productiva

Catàleg de fases productives.

Camps principals:

- `id`
- `codi`
- `descripcio`

### 7.11. Granja bestiar

Relaciona una granja amb un tipus de bestiar i una fase productiva.

Camps principals:

- `id`
- `granja_id`
- `bestiar_id`
- `fase_productiva_id`
- `cens`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`

Funció:

- emmagatzemar el cens ramader per explotació, espècie i fase

### 7.12. Entrega de dejeccions

Representa una sortida de dejeccions d'una granja d'origen.

Camps principals:

- `id`
- `dan_id`
- `granja_origen_id`
- `data`
- `quantitat`
- `terra_desti_id`
- `receptor_titular_id`
- `created_at`
- `created_by`
- `updated_at`
- `updated_by`

Funció:

- registrar una entrega ramadera
- controlar si el destí és una terra o un altre titular

Restricció important:

La base de dades obliga que una entrega tingui exactament un dels dos destins:

- o bé `terra_desti_id`
- o bé `receptor_titular_id`

No poden existir tots dos alhora ni tots dos buits.

## 8. Dades d'entrada del sistema

El programa treballa amb tres grans tipus d'entrada.

### 8.1. Entrada de configuració

Són valors necessaris perquè l'app es connecti a Supabase.

Variables principals:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`

Actualment existeixen valors per defecte a `JvmEnvConfig.kt`. Això permet arrencar el projecte fàcilment en entorn de desenvolupament, però en un producte real seria millor no deixar claus sensibles dins del codi.

### 8.2. Entrada d'usuari

Són les dades que l'usuari escriu a l'app:

- email i password per fer login
- dades de titular
- dades de tècnic
- dades d'oficina
- dades de terres
- dades agrícoles
- dades ramaderes

### 8.3. Entrada de catàleg i estructura

Hi ha informació que no entra cada cop per formulari, sinó que forma part de l'estructura del sistema:

- rols globals
- scopes d'accés
- catàleg de bestiar
- catàleg de fase productiva

## 9. Camps que entren i camps que es calculen

Un aspecte important del disseny és que no es guarda tot. Només s'emmagatzema el que té valor com a dada base, i la resta es pot calcular.

### 9.1. Camps que entren directament

Exemples:

- nom i NIF del titular
- nom i email del tècnic
- marca oficial d'una granja
- codi municipal, polígon, parcel·la i recinte
- superfície
- data d'aplicació
- kg de nitrogen
- UF
- cens
- quantitat d'entrega

### 9.2. Camps derivats o reconstruïbles

Exemples:

- `codi_sigpac_complet`, generat a partir de parts SIGPAC
- llista de titulars accessibles per un tècnic, derivada dels permisos
- permisos de lectura o escriptura, derivats de rol i assignacions
- valors com `kg N/ha` o `kg N/UF`, que es poden calcular a partir de `kg_n`, `uf` i `superficie`

Per això, aquests valors no es guarden com a camp físic al model actual del MVP.

## 10. Tractament de dades que fa el programa

Aquest és el bloc més important per explicar el funcionament intern.

### 10.1. Validació d'entrada

Abans de guardar, el programa fa validacions bàsiques a la capa de ViewModel.

Exemples:

- login: email i password no poden ser buits
- tècnic: cal nom, email, password i oficina
- password: mínim 6 caràcters
- titular: el nom és obligatori
- oficina: el nom és obligatori
- terra: `mun_codi` ha de tenir 5 dígits
- terra: polígon, parcel·la, recinte i superfície han de ser numèrics
- edicions de cens, superfície o quantitat: han de ser valors numèrics

Aquestes validacions no substitueixen la base de dades, però milloren l'experiència d'usuari i eviten errors evidents.

### 10.2. Transformació de dades

El programa transforma dades a diferents punts:

- normalitza cerques de NIF eliminant espais, punts i guions
- converteix textos del formulari a enters o decimals abans de guardar
- genera el `codi_sigpac_complet` a base de dades
- converteix respostes JSON de Supabase a DTOs Kotlin
- converteix sessió autenticada a format serialitzable per poder-la guardar localment

### 10.3. Càrrega i filtratge

Quan l'usuari entra a una pantalla:

- el ViewModel posa `isLoading = true`
- el repositori demana dades a Supabase
- la resposta es transforma a models Kotlin
- el ViewModel actualitza l'estat de pantalla

També hi ha filtratge local en memòria:

- cerca de titulars per nom o NIF
- cerca de terres per titular, NIF o codi SIGPAC
- paginació simple en llistes

### 10.4. Edició de dades

Quan l'usuari modifica una dada:

1. La UI recull el canvi.
2. El ViewModel valida.
3. El repositori envia un `PATCH` a Supabase.
4. La base de dades aplica RLS.
5. Si l'operació és vàlida, Supabase retorna la representació actualitzada.
6. El ViewModel actualitza l'estat local de la pantalla.

### 10.5. Persistència de sessió

La sessió autenticada es guarda localment a Desktop usant `Preferences`.

Què es guarda:

- access token
- refresh token
- id d'usuari
- email
- moment d'expiració

Quan l'app es torna a obrir:

- intenta recuperar la sessió
- refresca el token
- torna a carregar el perfil tècnic

Això evita obligar l'usuari a iniciar sessió cada cop.

## 11. Tractament específic per mòduls

### 11.1. Mòdul de titulars

Objectiu:

- mostrar els titulars accessibles a l'usuari

D'on surt la informació:

- `titular`
- `tecnic_titular`

Tractament:

- si l'usuari és `admin` o `oficina_manager`, veu tots els titulars
- si és tècnic normal, només veu els assignats
- es calcula si pot entrar al mòdul agrícola, ramader o tots dos

Sortida:

- llistat de titulars amb accions d'accés

### 11.2. Mòdul agrícola

Objectiu:

- consultar i editar dades agrícoles del titular

Entrades principals:

- dades del titular
- terres vinculades
- aplicacions de fertilitzants

Tractament:

- carrega el titular
- carrega terres del titular
- busca les DAN del titular
- a partir d'aquestes DAN, carrega aplicacions de fertilitzants

Sortida:

- pantalla amb dades del titular
- llista de terres
- llista d'aplicacions
- missatges de guardat o errors de permisos

### 11.3. Mòdul ramader

Objectiu:

- consultar i editar dades ramaderes del titular

Entrades principals:

- dades del titular
- granges del titular
- relacions de bestiar
- entregues de dejeccions

Tractament:

- carrega el titular
- carrega granges del titular
- obté els ids de granja
- recupera el cens ramader amb joins a bestiar i fase productiva
- busca les DAN del titular
- a partir d'aquestes DAN, carrega entregues

Sortida:

- pantalla amb granges
- cens per granja/tipus/fase
- entregues ramaderes

### 11.4. Gestió de titulars

Objectiu:

- crear, editar, cercar i eliminar titulars

Entrades:

- nom
- NIF

Tractament:

- validació del nom
- operacions CRUD sobre `titular`

Sortida:

- taula paginada de titulars
- missatges de confirmació o error

### 11.5. Gestió de terres

Objectiu:

- administrar terres del sistema

Entrades:

- titular associat
- codi municipal
- polígon
- parcel·la
- recinte
- superfície

Tractament:

- validació del patró del municipi
- validació numèrica
- alta, edició o eliminació
- consulta amb nom i NIF del titular relacionat

Sortida:

- llistat de terres filtrable
- codi SIGPAC complet
- titular relacionat

### 11.6. Gestió de tècnics

Objectiu:

- crear tècnics i gestionar-los

Entrades:

- nom
- email
- password
- oficina
- rol

Tractament:

1. crea primer l'usuari a Supabase Auth
2. després crea el registre funcional a `public.tecnic`
3. permet activar o desactivar el tècnic
4. permet canviar dades bàsiques
5. permet reset de password

És una de les parts més interessants del projecte perquè combina:

- identitat a Auth
- perfil funcional a base de dades
- permisos per rol

### 11.7. Gestió d'oficines

Objectiu:

- crear i mantenir oficines

Entrades:

- nom d'oficina

Tractament:

- CRUD simple
- si hi ha tècnics assignats, l'eliminació pot fallar per integritat referencial

## 12. Seguretat i control d'accés

Aquest és un dels punts més forts del projecte.

El sistema no depèn només del que decideix la UI. El control real és a la base de dades amb RLS.

### 12.1. Rols globals

Els rols principals són:

- `admin`
- `oficina_manager`
- `tecnic`
- `lectura`

### 12.2. Scope per titular

Un tècnic pot tenir accés:

- `comu`
- `agricola`
- `ramader`
- `lectura`

### 12.3. Funcions helper

La base de dades incorpora funcions com:

- `get_my_tecnic()`
- `current_oficina_id()`
- `is_admin()`
- `is_oficina_manager()`
- `same_oficina(...)`
- `can_read_titular(...)`
- `can_write_agricola(...)`
- `can_write_ramader(...)`

Aquestes funcions permeten escriure polítiques RLS més clares i reutilitzables.

### 12.4. RLS

Cada taula important té polítiques de:

- `select`
- `insert`
- `update`
- `delete`

Exemples de comportament:

- un tècnic només veu els titulars assignats
- un manager veu el que toca dins el seu àmbit
- un admin té control global
- les terres, aplicacions, granges i entregues hereten el control d'accés del titular o de la DAN relacionada

### 12.5. Resultat pràctic

Això evita que un usuari amb token vàlid pugui consultar o modificar qualsevol registre lliurement només perquè coneix l'endpoint REST.

## 13. Què surt del programa

El programa produeix diferents tipus de sortida.

### 13.1. Sortida visual

L'usuari veu:

- formularis
- llistes paginades
- dades de titulars
- dades agrícoles
- dades ramaderes
- missatges d'èxit
- missatges d'error
- estats de càrrega

### 13.2. Sortida estructurada a base de dades

Les operacions creen o actualitzen registres persistents a Supabase.

Exemples:

- alta d'un titular
- edició d'una terra
- canvi de cens
- actualització d'una granja
- registre o edició d'una entrega

### 13.3. Sortida de control

També es generen dades indirectes de control:

- timestamps d'alta i modificació
- identificador de qui ha fet un canvi
- estat d'activació d'un tècnic
- sessió persistida localment

## 14. Relació amb les dades del full de càlcul

Les dades del full mostrat encaixen amb el model del projecte, però no es copien 1:1.

Exemples de correspondència:

- `Marca Oficial` -> `granja.marca_oficial`
- `Ramader` -> titular o explotació vinculada a la part ramadera
- `Agricultor` -> `titular.nom_rao`
- `NIF Agr.` -> `titular.nif`
- `ha` -> `terra.superficie` o suma de terres
- `UF` -> `aplicacions_fertilitzants.uf`
- `kg N` -> `aplicacions_fertilitzants.kg_n`

Hi ha columnes del full que en el model MVP es consideren:

- derivades
- de validació manual
- o futures ampliacions

Per exemple:

- `kg N/ha`
- `kg N/UF`
- indicadors tipus `Llesta`, `Enviada`, `Conf`, `Dates`, `GPS`

Aquests camps es poden afegir més endavant si es decideix que formen part del flux real del producte, però per al MVP no és obligatori guardar-los si no intervenen directament en els casos d'ús actuals.

## 15. Decisions importants de disseny

### 15.1. Fer un MVP i no un ERP complet

Això ha permès prioritzar el que realment es pot demostrar:

- login
- rols
- accés per tècnic
- gestió de titulars
- part agrícola
- part ramadera

### 15.2. Posar la seguretat a la base de dades

És una decisió madura tècnicament perquè evita confiar massa en el client.

### 15.3. Reutilitzar l'API REST de Supabase

Ha evitat desenvolupar un backend complet i ha permès concentrar l'esforç en:

- model de dades
- permisos
- experiència d'usuari

### 15.4. Separar UI, ViewModel i Repository

Això dona una estructura clara i fàcil d'explicar:

- la UI mostra
- el ViewModel coordina
- el Repository accedeix a dades

## 16. Limitacions actuals del MVP

Per defensar bé el projecte, és important explicar també què no fa encara.

- no hi ha importador automàtic des d'Excel
- no hi ha informes finals ni exportació PDF
- no hi ha dashboard amb càlculs agregats
- alguns camps del full original encara no estan modelats
- hi ha claus per defecte al codi de desenvolupament, cosa acceptable per prototip però no per producció
- hi ha càlculs que encara no es presenten com a resultats derivats a la UI

Aquestes limitacions són coherents amb l'abast d'un MVP acadèmic.

## 17. Verificació de l'estat actual

Després d'ajustar el projecte a l'esquema SQL simplificat del MVP, s'ha verificat que l'aplicació continua compilant correctament a nivell JVM. Això reforça que la relació actual entre codi, model de dades i permisos és coherent.

També s'ha revisat que:

- la home ja no depèn d'una vista SQL eliminada
- el codi actiu no consumeix taules descartades del model antic
- la documentació principal queda alineada amb l'estat real del programa

## 18. Valor tècnic que es pot defensar a la presentació

Aquest projecte es pot defensar bé perquè combina diverses capes reals de desenvolupament:

- modelatge relacional
- autenticació
- control d'accés amb RLS
- arquitectura per capes
- persistència local de sessió
- interfície funcional d'escriptori
- validació i transformació de dades

No és només una maqueta visual. És una aplicació amb flux complet:

1. un usuari real s'autentica
2. el sistema identifica el seu perfil tècnic
3. el sistema decideix què pot veure o editar
4. l'usuari treballa amb dades reals de negoci
5. la informació queda persistent i traçable

## 19. Resum final

AgriSync s'ha construït com un MVP de gestió agrària centrat en la DAN, amb una arquitectura simple però professional. El programa rep dades de configuració, dades d'autenticació i dades de negoci; les valida, les transforma, les filtra segons permisos i les persisteix en una base de dades relacional segura.

El nucli conceptual del sistema és:

- un tècnic autenticat
- amb un rol concret
- que treballa sobre titulars
- i accedeix a dades agrícoles o ramaderes segons el seu scope

Des del punt de vista de defensa del projecte, el més important és que cada part té una justificació clara:

- la base de dades està pensada per la realitat del domini
- la capa de permisos protegeix la informació
- la UI resol els casos d'ús mínims del producte
- l'abast està ajustat a un projecte final de cicle
