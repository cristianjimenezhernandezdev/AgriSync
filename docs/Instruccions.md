# Instruccions d'ús d'AgriSync

## 1. Què és AgriSync

AgriSync és una aplicació d'escriptori per gestionar dades bàsiques de la Declaració Anual de Nitrogen (DAN) en entorns agrícoles i ramaders. El sistema treballa sobre titulars, terres, granges, bestiar, aplicacions de fertilitzants i entregues de dejeccions.

Aquest document és un manual pràctic per a l'usuari.

## 2. Requisits previs

Per poder fer servir l'aplicació necessites:

- tenir l'app en execució
- tenir un usuari vàlid a Supabase Auth
- tenir un registre de tècnic vinculat a aquest usuari
- estar actiu dins del sistema
- tenir configurada la connexió amb Supabase

La connexió es pot configurar de dues maneres:

- amb variables d'entorn
- o amb un fitxer `agrisync.properties`

## 3. Configuració per a l'entrega o l'executable

Si l'aplicació s'entrega en `.exe`, la manera recomanada és posar al mateix directori:

- `AgriSync.exe`
- `agrisync.properties`

El fitxer `agrisync.properties` ha de contenir:

```properties
SUPABASE_URL=https://el-teu-projecte.supabase.co
SUPABASE_ANON_KEY=enganxa_aqui_la_anon_key
SUPABASE_SERVICE_ROLE_KEY=enganxa_aqui_la_service_role_key
```

L'aplicació també accepta aquestes mateixes dades com a variables d'entorn si s'executa en desenvolupament.

Si falta la configuració, l'app mostrarà una pantalla indicant quines dades falten.

## 4. Usuaris de prova

Si has carregat el `seed_complet.sql`, els usuaris de prova previstos són aquests:

- `admin.test@agrisync.com` / `admin1234`
- `manager.test@agrisync.com` / `manager1234`
- `agricola.test@agrisync.com` / `agricola1234`
- `ramader.test@agrisync.com` / `ramader1234`
- `lectura.test@agrisync.com` / `lectura1234`

## 5. Inici de sessió

Quan s'obre el programa apareix la pantalla de login.

Has d'introduir:

- email
- password

La pantalla et guia amb textos d'ajuda perquè sàpigues què s'espera a cada camp.

Si tot va bé:

- l'app valida les credencials a Supabase Auth
- recupera el teu perfil tècnic
- carrega la pantalla principal

## 6. Errors habituals al login

### 6.1. Credencials incorrectes

Vol dir que l'email o la contrasenya no coincideixen.

### 6.2. No s'ha trobat perfil tècnic

Vol dir que l'usuari existeix a Auth però no està vinculat correctament a `public.tecnic`.

### 6.3. Tècnic inactiu

Vol dir que el teu registre funcional existeix però està desactivat.

### 6.4. Configuració incompleta

Si l'app mostra un missatge de configuració incompleta, falten dades de connexió de Supabase. Revisa el fitxer `agrisync.properties` o les variables d'entorn.

## 7. Pantalla principal

En entrar, s'obre la pantalla de `Titulars`.

Allà veuràs:

- la llista de titulars accessibles segons els teus permisos
- una cerca per nom o NIF
- un resum de resultats visibles i pàgines
- accés al mòdul agrícola si tens permís agrícola
- accés al mòdul ramader si tens permís ramader
- accés al teu perfil
- opcions de gestió si el teu rol ho permet

## 8. Barra superior

A la part superior tens la navegació principal.

Opcions habituals:

- `Titulars`
- `Perfil`
- `Logout`

Opcions visibles només si tens permisos:

- `Gestio Titulars`
- `Terres`
- `Tecnics`
- `Oficines`

## 9. Pantalla de titulars

La pantalla principal serveix per localitzar el titular amb qui vols treballar.

Què hi pots fer:

- buscar per nom o NIF
- canviar de pàgina si hi ha molts resultats
- obrir el mòdul agrícola
- obrir el mòdul ramader

Recorda:

- no tots els usuaris veuen els mateixos titulars
- la visibilitat depèn del rol i de les assignacions a `tecnic_titular`
- si no surt cap resultat, la pantalla t'indicarà si és per filtre o per manca de titulars accessibles

## 10. Mòdul agrícola

Aquest mòdul mostra la informació agrícola del titular seleccionat.

Hi trobaràs normalment:

- dades bàsiques del titular
- terres vinculades
- aplicacions de fertilitzants

Cada secció incorpora una explicació curta sobre què representa i què hi pots fer.

### 10.1. Què s'hi pot fer ara

Segons els permisos:

- editar NIF i nom del titular
- crear terres noves
- editar superfície d'una terra
- eliminar terres
- crear aplicacions de fertilitzants
- editar data, `kg N` i `UF`
- eliminar aplicacions

### 10.2. Crear una terra

Prem `+ Nova Terra` i informa:

- codi municipal de 5 dígits
- polígon
- parcel·la
- recinte
- superfície

### 10.3. Crear una aplicació

Prem `+ Nova Aplicacio` i informa:

- terra sobre la qual es registra
- data amb format `YYYY-MM-DD`
- `kg N`
- `UF`

Si el titular encara no tenia cap DAN, el sistema en crea una automàticament per poder guardar la nova aplicació.

### 10.4. Validacions importants

- el nom del titular no pot quedar buit
- superfície, `kg N` i `UF` han de ser nombres vàlids
- els valors numèrics no poden ser negatius
- la data ha de tenir format `YYYY-MM-DD`
- abans d'eliminar una terra o una aplicació, l'app demana confirmació
- si una secció és buida, la pantalla t'indicarà quin és el següent pas recomanat

## 11. Mòdul ramader

Aquest mòdul mostra la informació ramadera del titular seleccionat.

Hi trobaràs:

- dades del titular
- granges
- cens de bestiar per granja
- entregues de dejeccions

Cada secció incorpora una explicació curta sobre què representa i què hi pots fer.

### 11.1. Què s'hi pot fer ara

Segons els permisos:

- editar NIF i nom del titular
- crear granges
- editar granja
- eliminar granges
- crear registres de granja-bestiar
- editar cens
- eliminar registres de bestiar
- crear entregues de dejeccions
- editar data i quantitat d'una entrega
- eliminar entregues

### 11.2. Crear una granja

Prem `+ Nova Granja` i informa:

- nom, si el vols guardar
- marca oficial, que és obligatòria

### 11.3. Crear un registre de granja-bestiar

Prem `+ Nou Registre` i informa:

- granja
- tipus de bestiar
- fase productiva
- cens

### 11.4. Crear una entrega

Prem `+ Nova Entrega` i informa:

- granja d'origen
- data amb format `YYYY-MM-DD`
- quantitat
- tipus de receptor

El receptor pot ser:

- el mateix titular
- una terra del titular

Si el titular encara no tenia cap DAN, el sistema en crea una automàticament per poder guardar la nova entrega.

### 11.5. Validacions importants

- la marca oficial és obligatòria
- cens i quantitat han de ser valors numèrics vàlids
- la data ha de tenir format `YYYY-MM-DD`
- els valors numèrics no poden ser negatius
- abans d'eliminar una granja, un registre de bestiar o una entrega, l'app demana confirmació
- si una secció és buida, la pantalla t'indicarà quin és el següent pas recomanat

## 12. Perfil

La pantalla `Perfil` permet veure les teves dades:

- nom
- email
- rol
- oficina

També pots, si tens permís:

- editar nom i email
- canviar el password

## 13. Gestió de titulars

Aquesta pantalla permet administrar titulars.

### 13.1. Crear titular

Cal informar:

- nom
- NIF, si es coneix

El nom és obligatori.

### 13.2. Editar titular

Es poden modificar:

- nom
- NIF

### 13.3. Eliminar titular

Es pot eliminar des de la mateixa pantalla, però si té dades relacionades la base de dades pot impedir l'operació o provocar eliminacions en cascada segons la relació.

## 14. Gestió de terres

La pantalla `Terres` permet administrar el catàleg de terres.

### 14.1. Crear una terra

Cal introduir:

- titular associat, si n'hi ha
- codi municipal de 5 dígits
- polígon
- parcel·la
- recinte
- superfície

### 14.2. Validacions

- `mun_codi` ha de tenir exactament 5 dígits
- polígon, parcel·la i recinte han de ser enters
- superfície ha de ser numèrica

### 14.3. Editar una terra

Es pot modificar:

- titular associat
- superfície

El codi SIGPAC complet es genera automàticament.

## 15. Gestió de tècnics

La pantalla `Tecnics` serveix per administrar usuaris operatius.

### 15.1. Crear tècnic

Cal indicar:

- nom
- email
- password
- oficina
- rol

El sistema farà dues operacions:

1. crear l'usuari a Supabase Auth
2. crear el registre funcional a `public.tecnic`

### 15.2. Activar o desactivar

Un tècnic inactiu no pot operar normalment dins l'app.

### 15.3. Canviar password

Des d'aquesta pantalla també es pot fer reset de password d'un tècnic.

### 15.4. Eliminar tècnic

Ara es pot eliminar directament des de la pantalla de gestió.

Funcionament:

1. prems `Eliminar`
2. l'app mostra un diàleg de confirmació
3. si el tècnic té login, l'app intenta eliminar tant el registre funcional com l'usuari d'Auth
4. si no té login, només s'elimina el registre funcional

Important:

- l'acció és destructiva
- les assignacions del tècnic a titulars també desapareixen

## 16. Detall de tècnic i assignacions

En el detall d'un tècnic es poden veure o gestionar les assignacions de titulars.

Cada assignació té:

- el titular
- l'scope
- si està activa o no

Scopes possibles:

- `comu`
- `agricola`
- `ramader`
- `lectura`

### 16.1. Casos especials que veuràs a la pantalla

- `Sense login`: vol dir que el tècnic no té `user_id` i no pot entrar a l'aplicació
- `Cap titular assignat`: si el tècnic té rol normal, probablement no veurà titulars a la home fins que tingui alguna assignació activa

### 16.2. Eliminar assignacions

Quan elimines una assignació, l'app demana confirmació abans de fer l'acció.

## 17. Gestió d'oficines

La pantalla `Oficines` permet:

- crear oficines
- editar el nom d'una oficina
- eliminar oficines si no hi ha dependències que ho impedeixin

## 18. Errors habituals dins l'app

### 18.1. `401` o `403`

Vol dir que no tens permís per veure o modificar aquella dada.

### 18.2. Error de validació

Pot passar si:

- falta un camp obligatori
- un camp numèric no és vàlid
- una contrasenya és massa curta
- el codi municipal no compleix el format
- una data no té format correcte
- no has seleccionat una dada obligatòria en un formulari nou

### 18.3. Error en esborrar un tècnic

Pot passar si:

- no tens permisos suficients
- hi ha un problema amb Supabase Auth
- s'ha pogut eliminar el tècnic funcional però no l'usuari d'Auth

### 18.4. Error en eliminar una granja

Pot passar si aquella granja està sent referenciada per entregues o per altres dades que la BDD no permet esborrar en aquell moment.

## 19. Bones pràctiques d'ús

- entra sempre amb el teu usuari real o de prova vàlid
- comprova si estàs al mòdul agrícola o ramader abans d'editar
- no repeteixis operacions si t'ha aparegut un error de permisos
- revisa les dades abans de desar
- si no veus un titular que esperes veure, revisa assignacions i permisos
- llegeix els diàlegs de confirmació abans d'eliminar dades
- fixa't en els textos de suport de la pantalla, perquè acostumen a indicar quin és el següent pas útil

## 20. Resum ràpid de treball

Flux recomanat d'ús:

1. assegura't que `agrisync.properties` o les variables d'entorn estan configurades
2. inicia sessió
3. comprova el perfil
4. entra a `Titulars`
5. busca el titular
6. obre el mòdul agrícola o ramader
7. crea o edita les dades necessàries directament al mòdul
8. desa i comprova el missatge de confirmació
9. si ets administrador, usa `Tecnics` per donar altes, canviar passwords, assignar titulars o fer baixes
