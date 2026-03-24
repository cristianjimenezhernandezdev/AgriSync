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
- tenir configurades les variables `SUPABASE_URL`, `SUPABASE_ANON_KEY` i `SUPABASE_SERVICE_ROLE_KEY` en l'entorn on s'executa l'app

## 3. Usuaris de prova

Si has carregat el `seed_complet.sql`, els usuaris de prova previstos són aquests:

- `admin.test@agrisync.com` / `admin1234`
- `manager.test@agrisync.com` / `manager1234`
- `agricola.test@agrisync.com` / `agricola1234`
- `ramader.test@agrisync.com` / `ramader1234`
- `lectura.test@agrisync.com` / `lectura1234`

Aquests usuaris s'han de crear abans a `Supabase > Authentication > Users` i després executar el seed.

## 4. Inici de sessió

Quan s'obre el programa apareix la pantalla de login.

Has d'introduir:

- email
- password

Després prem `Entrar`.

Si tot va bé:

- l'app valida les credencials a Supabase Auth
- recupera el teu perfil tècnic
- carrega la pantalla principal

## 5. Errors habituals al login

### 5.1. Credencials incorrectes

Vol dir que l'email o la contrasenya no coincideixen.

### 5.2. No s'ha trobat perfil tècnic

Vol dir que l'usuari existeix a Auth però no està vinculat correctament a `public.tecnic`.

### 5.3. Tècnic inactiu

Vol dir que el teu registre funcional existeix però està desactivat.

### 5.4. Configuració incompleta

Si l'app mostra un missatge de configuració incompleta, falten variables d'entorn obligatòries de Supabase i el programa no continuarà fins que estiguin definides.

## 6. Pantalla principal

En entrar, s'obre la pantalla de `Titulars`.

Allà veuràs:

- la llista de titulars accessibles segons els teus permisos
- una cerca per nom o NIF
- accés al mòdul agrícola si tens permís agrícola
- accés al mòdul ramader si tens permís ramader
- accés al teu perfil
- opcions de gestió si el teu rol ho permet

## 7. Barra superior

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

## 8. Pantalla de titulars

La pantalla principal serveix per localitzar el titular amb qui vols treballar.

Què hi pots fer:

- buscar per nom o NIF
- canviar de pàgina si hi ha molts resultats
- obrir el mòdul agrícola
- obrir el mòdul ramader

Recorda:

- no tots els usuaris veuen els mateixos titulars
- la visibilitat depèn del rol i de les assignacions a `tecnic_titular`

## 9. Mòdul agrícola

Aquest mòdul mostra la informació agrícola del titular seleccionat.

Hi trobaràs normalment:

- dades bàsiques del titular
- terres vinculades
- aplicacions de fertilitzants

### 9.1. Què s'hi pot fer ara

Segons els permisos:

- editar NIF i nom del titular
- crear terres noves
- editar superfície d'una terra
- eliminar terres
- crear aplicacions de fertilitzants
- editar data, `kg N` i `UF`
- eliminar aplicacions

### 9.2. Crear una terra

Prem `+ Nova Terra` i informa:

- codi municipal de 5 dígits
- polígon
- parcel·la
- recinte
- superfície

### 9.3. Crear una aplicació

Prem `+ Nova Aplicacio` i informa:

- terra sobre la qual es registra
- data amb format `YYYY-MM-DD`
- `kg N`
- `UF`

Si el titular encara no tenia cap DAN, el sistema en crea una automàticament per poder guardar la nova aplicació.

### 9.4. Validacions importants

- el nom del titular no pot quedar buit
- superfície, `kg N` i `UF` han de ser nombres vàlids
- els valors numèrics no poden ser negatius
- la data ha de tenir format `YYYY-MM-DD`
- abans d'eliminar una terra o una aplicació, l'app demana confirmació

## 10. Mòdul ramader

Aquest mòdul mostra la informació ramadera del titular seleccionat.

Hi trobaràs:

- dades del titular
- granges
- cens de bestiar per granja
- entregues de dejeccions

### 10.1. Què s'hi pot fer ara

Segons permisos:

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

### 10.2. Crear una granja

Prem `+ Nova Granja` i informa:

- nom, si el vols guardar
- marca oficial, que és obligatòria

### 10.3. Crear un registre de granja-bestiar

Prem `+ Nou Registre` i informa:

- granja
- tipus de bestiar
- fase productiva
- cens

### 10.4. Crear una entrega

Prem `+ Nova Entrega` i informa:

- granja d'origen
- data amb format `YYYY-MM-DD`
- quantitat
- tipus de receptor

El receptor pot ser:

- el mateix titular
- una terra del titular

Si el titular encara no tenia cap DAN, el sistema en crea una automàticament per poder guardar la nova entrega.

### 10.5. Validacions importants

- la marca oficial és obligatòria
- cens i quantitat han de ser valors numèrics vàlids
- la data ha de tenir format `YYYY-MM-DD`
- els valors numèrics no poden ser negatius
- abans d'eliminar una granja, un registre de bestiar o una entrega, l'app demana confirmació

## 11. Perfil

La pantalla `Perfil` permet veure les teves dades:

- nom
- email
- rol
- oficina

També pots, si tens permís:

- editar nom i email
- canviar el password

## 12. Gestió de titulars

Aquesta pantalla permet administrar titulars.

### 12.1. Crear titular

Cal informar:

- nom
- NIF, si es coneix

El nom és obligatori.

### 12.2. Editar titular

Es poden modificar:

- nom
- NIF

### 12.3. Eliminar titular

Es pot eliminar des de la mateixa pantalla, però si té dades relacionades la base de dades pot impedir l'operació o provocar eliminacions en cascada segons la relació.

## 13. Gestió de terres

La pantalla `Terres` permet administrar el catàleg de terres.

### 13.1. Crear una terra

Cal introduir:

- titular associat, si n'hi ha
- codi municipal de 5 dígits
- polígon
- parcel·la
- recinte
- superfície

### 13.2. Validacions

- `mun_codi` ha de tenir exactament 5 dígits
- polígon, parcel·la i recinte han de ser enters
- superfície ha de ser numèrica

### 13.3. Editar una terra

Es pot modificar:

- titular associat
- superfície

El codi SIGPAC complet es genera automàticament.

## 14. Gestió de tècnics

La pantalla `Tecnics` serveix per administrar usuaris operatius.

### 14.1. Crear tècnic

Cal indicar:

- nom
- email
- password
- oficina
- rol

El sistema farà dues operacions:

1. crear l'usuari a Supabase Auth
2. crear el registre funcional a `public.tecnic`

### 14.2. Activar o desactivar

Un tècnic inactiu no pot operar normalment dins l'app.

### 14.3. Canviar password

Des d'aquesta pantalla també es pot fer reset de password d'un tècnic.

### 14.4. Eliminar tècnic

Ara es pot eliminar directament des de la pantalla de gestió.

Funcionament:

1. prems `Eliminar`
2. l'app mostra un diàleg de confirmació
3. si el tècnic té login, l'app intenta eliminar tant el registre funcional com l'usuari d'Auth
4. si no té login, només s'elimina el registre funcional

Important:

- l'acció és destructiva
- les assignacions del tècnic a titulars també desapareixen

## 15. Detall de tècnic i assignacions

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

### 15.1. Casos especials que veuràs a la pantalla

- `Sense login`: vol dir que el tècnic no té `user_id` i no pot entrar a l'aplicació
- `Cap titular assignat`: si el tècnic té rol normal, probablement no veurà titulars a la home fins que tingui alguna assignació activa

### 15.2. Eliminar assignacions

Quan elimines una assignació, l'app demana confirmació abans de fer l'acció.

## 16. Gestió d'oficines

La pantalla `Oficines` permet:

- crear oficines
- editar el nom d'una oficina
- eliminar oficines si no hi ha dependències que ho impedeixin

## 17. Errors habituals dins l'app

### 17.1. `401` o `403`

Vol dir que no tens permís per veure o modificar aquella dada.

### 17.2. Error de validació

Pot passar si:

- falta un camp obligatori
- un camp numèric no és vàlid
- una contrasenya és massa curta
- el codi municipal no compleix el format
- una data no té format correcte
- no has seleccionat una dada obligatòria en un formulari nou

### 17.3. Error en esborrar un tècnic

Pot passar si:

- no tens permisos suficients
- hi ha un problema amb Supabase Auth
- s'ha pogut eliminar el tècnic funcional però no l'usuari d'Auth

En aquest últim cas, l'app t'ho indicarà al missatge final.

### 17.4. Error en eliminar una granja

Pot passar si aquella granja està sent referenciada per entregues o per altres dades que la BDD no permet esborrar en aquell moment.

## 18. Bones pràctiques d'ús

- entra sempre amb el teu usuari real o de prova vàlid
- comprova si estàs al mòdul agrícola o ramader abans d'editar
- no repeteixis operacions si t'ha aparegut un error de permisos
- revisa les dades abans de desar
- si no veus un titular que esperes veure, revisa assignacions i permisos
- llegeix els diàlegs de confirmació abans d'eliminar dades

## 19. Resum ràpid de treball

Flux recomanat d'ús:

1. inicia sessió
2. comprova el perfil
3. entra a `Titulars`
4. busca el titular
5. obre el mòdul agrícola o ramader
6. crea o edita les dades necessàries directament al mòdul
7. desa i comprova el missatge de confirmació
8. si ets administrador, usa `Tecnics` per donar altes, canviar passwords, assignar titulars o fer baixes
