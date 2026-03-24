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
- confirmar accions destructives abans d'executar-les
- orientar l'usuari amb textos d'ajuda, placeholders i estats buits més clars

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
- `agrisync.properties.example`
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

1. intenta carregar la configuració de Supabase des de propietats JVM o variables d'entorn
2. si no hi són, en la versió JVM prova de llegir un fitxer `agrisync.properties`
3. busca aquest fitxer al directori d'execució o al costat del `.exe` o `.jar`
4. si continua faltant alguna dada obligatòria, mostra una pantalla de configuració incompleta i no continua
5. construeix els serveis a `AppServices`
6. intenta recuperar una sessió guardada localment
7. si la troba, refresca el token
8. carrega el perfil tècnic de l'usuari autenticat
9. entra a l'aplicació si tot és correcte

Les claus obligatòries actuals són:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`

Aquest canvi és important perquè resol un problema pràctic d'entrega: els professors o usuaris de demo poden executar l'aplicació amb un `.exe` i un fitxer de configuració al costat, sense haver de definir variables d'entorn manualment.

### 7.2. Login

El login es fa amb Supabase Auth.

Flux:

1. l'usuari introdueix email i password
2. `LoginViewModel` valida que els camps no siguin buits
3. `AuthService` crida `SupabaseAuthApi.signInWithPassword`
4. Supabase retorna els tokens
5. l'app recupera el tècnic associat amb `get_my_tecnic()`
6. si el tècnic existeix i està actiu, es guarda la sessió localment

La pantalla de login també té millor jerarquia visual i més textos d'ajuda des de la iteració 4.

### 7.3. Recuperació del perfil tècnic

Un usuari autenticat no n'hi ha prou. També ha d'existir a `public.tecnic`.

Això és clau perquè:

- el login real es fa contra Auth
- els permisos reals es calculen des de la BDD funcional

## 8. Model funcional de dades

El model del MVP queda reduït a les entitats realment necessàries:

- `oficina`
- `tecnic`
- `titular`
- `tecnic_titular`
- `dan_declaracio`
- `terra`
- `aplicacions_fertilitzants`
- `granja`
- `bestiar`
- `fase_productiva`
- `granja_bestiar`
- `entrega_dejeccions`

## 9. Dades d'entrada del sistema

L'aplicació treballa amb tres grans tipus d'entrada.

### 9.1. Configuració

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`

Aquestes dades es poden injectar de dues maneres:

- variables d'entorn o propietats JVM
- fitxer `agrisync.properties`

### 9.2. Entrada d'usuari

- email i password per fer login
- dades de titular
- dades de tècnic
- dades d'oficina
- dades de terres
- dades agrícoles
- dades ramaderes
- accions d'administració com alta, baixa, canvi de password i assignacions

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
- format de data `YYYY-MM-DD` als editors inline que ho requereixen
- comprovació que els camps de selecció obligatoris estiguin informats abans de crear registres nous

### 10.2. Transformació

El sistema també transforma dades:

- normalitza NIF per a cerques
- converteix text a enters o decimals quan la validació és correcta
- genera `codi_sigpac_complet` a la BDD
- converteix JSON de Supabase a DTOs Kotlin
- guarda la sessió en format serialitzable
- resol automàticament l'any actual de campanya mitjançant `PlatformDateTime`

### 10.3. Filtratge i càrrega

Cada pantalla:

- posa estat de càrrega
- demana dades al repositori
- rep models Kotlin
- actualitza UI

### 10.4. Edició i administració

Quan l'usuari edita o administra:

1. la UI recull el canvi o l'acció
2. el ViewModel valida
3. si la validació falla, el formulari queda obert i es mostra un missatge d'error
4. si l'acció és destructiva, la UI demana confirmació
5. el Repository envia la petició REST o la crida a Admin API
6. la base de dades aplica RLS quan toca
7. Supabase retorna el resultat
8. la pantalla es refresca

### 10.5. Gestió automàtica de DAN

Per crear una aplicació agrícola o una entrega ramadera, la BDD exigeix un `dan_id`.

El repositori segueix aquesta regla:

1. busca si el titular ja té alguna `dan_declaracio`
2. si en troba, fa servir la més recent
3. si no en troba cap, crea automàticament una DAN per a la campanya actual

### 10.6. Poliment d'UX

La iteració 4 ha afegit:

- formularis amb més context i més guia
- capçaleres de secció que expliquen què representa cada bloc
- estats buits que orienten sobre el següent pas
- missatges d'error i ajuda més coherents entre pantalles

### 10.7. Preparació per a l'entrega

La millora posterior a la iteració 4 resol específicament la forma d'entregar el projecte en `.exe`.

La decisió ha estat:

- mantenir suport a variables d'entorn per desenvolupament
- afegir suport a `agrisync.properties` per a execució empaquetada
- proporcionar una plantilla `agrisync.properties.example`

A nivell acadèmic això és molt útil perquè fa possible una entrega executable senzilla sense haver de tocar configuració del sistema operatiu del professor.

## 11. Tractament per mòduls

### 11.1. Home de titulars

Mostra els titulars accessibles segons rol i assignacions.

També incorpora:

- cerca més guiada
- resum visual dels resultats
- millor estat buit i millor estat d'error

### 11.2. Mòdul agrícola

Carrega i permet gestionar:

- titular
- terres
- aplicacions de fertilitzants

### 11.3. Mòdul ramader

Carrega i permet gestionar:

- titular
- granges
- cens de bestiar
- entregues de dejeccions
- catàlegs de bestiar i fase productiva
- terres del titular per oferir destí d'entrega

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
- eliminar tècnics des de la mateixa app
- intentar eliminar també el seu usuari d'Auth si tenia login associat
- gestionar assignacions de titulars

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

La base de dades continua sent la que decideix què pot veure o modificar cada usuari.

Matís important de defensa:

- fer servir `agrisync.properties` és una solució pràctica d'entrega per al context acadèmic
- no és la solució ideal de seguretat per a una distribució real a tercers, especialment mentre hi hagi operacions administratives que depenen de `service_role`

## 13. Sortides del sistema

### 13.1. Sortida visual

- formularis
- llistes paginades
- dades de titulars
- dades agrícoles
- dades ramaderes
- missatges d'èxit i error
- diàlegs de confirmació en accions destructives
- avisos contextuals i estats buits més útils

### 13.2. Sortida persistent

- registres nous i actualitzacions a Supabase
- eliminacions administratives de tècnics i assignacions
- altes i baixes de registres dins dels mòduls agrícola i ramader

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

## 15. Seed i proves

Per poder provar l'aplicació amb dades reals del MVP, el projecte inclou `seed_complet.sql`.

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
- la `service_role` continua sent necessària per a funcions administratives avançades
- `agrisync.properties` resol bé l'entrega acadèmica, però no seria la millor estratègia per a un producte final distribuït a gran escala
- els càlculs derivats i els resums encara no formen part de la UI

## 17. Estat actual verificat

Amb l'esquema SQL simplificat i els ajustos realitzats:

- l'aplicació compila correctament
- el login amb usuaris de prova ja s'ha pogut validar
- el codi i la documentació han quedat alineats amb la BDD real
- la configuració sensible ja no queda embeguda dins del codi
- les validacions del detall agrícola i ramader són més estrictes
- la gestió de tècnics és més completa
- els mòduls agrícola i ramader ja permeten altes i baixes dels principals registres de treball
- l'experiència d'usuari al login i a les pantalles principals és més clara i més guiada
- l'app ja està preparada per ser entregada amb `.exe` + `agrisync.properties`
- les iteracions 2, 3 i 4, i la millora d'entrega, s'han pogut resoldre sense refer la BDD

## 18. Resum final

AgriSync és un MVP funcional de gestió agrària centrat en la DAN. Combina autenticació, model relacional, permisos reals, client desktop i una estructura neta per capes.

Des del punt de vista de defensa del projecte, el valor principal és que no és només una maqueta visual: és una aplicació amb autenticació real, persistència real i control d'accés real sobre dades de negoci. A més, ara també està preparada per a una entrega acadèmica molt més pràctica gràcies al suport de configuració per fitxer.
