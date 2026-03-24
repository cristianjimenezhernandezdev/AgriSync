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

### 9.1. Camps que es poden editar

Segons els permisos:

- NIF i nom del titular
- superfície d'una terra
- data d'una aplicació
- `kg N`
- `UF`

### 9.2. Bones pràctiques

- comprova el NIF abans de guardar
- introdueix superfície, `kg N` i `UF` com a nombres
- revisa bé la data

## 10. Mòdul ramader

Aquest mòdul mostra la informació ramadera del titular seleccionat.

Hi trobaràs:

- dades del titular
- granges
- cens de bestiar per granja
- entregues de dejeccions

### 10.1. Camps que es poden editar

Segons permisos:

- NIF i nom del titular
- nom o marca oficial de la granja
- cens
- data i quantitat d'una entrega

### 10.2. Recomanacions

- revisa bé la marca oficial
- introdueix cens i quantitat com a valors numèrics
- si no tens permís ramader, no podràs guardar canvis

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

## 18. Bones pràctiques d'ús

- entra sempre amb el teu usuari real o de prova vàlid
- comprova si estàs al mòdul agrícola o ramader abans d'editar
- no repeteixis operacions si t'ha aparegut un error de permisos
- revisa les dades abans de desar
- si no veus un titular que esperes veure, revisa assignacions i permisos

## 19. Resum ràpid de treball

Flux recomanat d'ús:

1. inicia sessió
2. comprova el perfil
3. entra a `Titulars`
4. busca el titular
5. obre el mòdul agrícola o ramader
6. revisa i edita les dades permeses
7. desa i comprova el missatge de confirmació
