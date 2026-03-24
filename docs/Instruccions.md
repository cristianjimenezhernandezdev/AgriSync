# Instruccions d'ús d'AgriSync

## 1. Què és AgriSync

AgriSync és una aplicació d'escriptori per gestionar dades bàsiques de la Declaració Anual de Nitrogen (DAN) en un entorn agrícola i ramader. L'aplicació treballa sobre titulars, terres, granges, bestiar, aplicacions de fertilitzants i entregues de dejeccions.

L'objectiu d'aquest manual és explicar com fer servir l'aplicació de forma pràctica, com si fos una guia de treball diària.

## 2. Abans de començar

Per poder entrar a l'aplicació necessites:

- tenir l'aplicació en execució
- disposar d'un usuari vàlid a Supabase Auth
- tenir un registre de tècnic creat a la base de dades i vinculat al teu usuari
- estar actiu dins del sistema

Si no tens compte o no pots entrar, ho ha de revisar un administrador o un gestor d'oficina.

## 3. Inici de sessió

Quan s'obre el programa apareix la pantalla de login.

Has d'introduir:

- email
- password

Després prem `Login`.

Què pot passar:

- si les credencials són correctes, entraràs a l'aplicació
- si l'usuari no existeix com a tècnic, no es podrà carregar el perfil
- si el tècnic està inactiu, no podràs continuar
- si no tens permís sobre certes dades, ho veuràs després com a error `401` o `403`

## 4. Pantalla principal

Quan entres, arribes a la pantalla de `Titulars`.

Aquesta pantalla mostra els titulars als quals tens accés segons els teus permisos.

Hi trobaràs:

- llista de titulars
- cerca per NIF o nom
- accés al mòdul agrícola si tens permís agrícola
- accés al mòdul ramader si tens permís ramader
- accés al teu perfil
- segons el teu rol, accessos de gestió

## 5. Barra superior i navegació

A la barra superior tens les opcions principals.

Opcions habituals:

- `Titulars`: torna a la llista principal
- `Perfil`: obre la pantalla del teu perfil
- `Logout`: tanca la sessió

Opcions visibles només si tens permisos suficients:

- `Gestio Titulars`
- `Terres`
- `Tecnics`
- `Oficines`

Si una opció no et surt, normalment és perquè el teu rol no la pot fer servir.

## 6. Pantalla de titulars

Aquesta és la pantalla principal de treball.

Què hi pots fer:

- buscar un titular pel nom o NIF
- navegar per pàgines si hi ha molts resultats
- entrar al mòdul agrícola del titular
- entrar al mòdul ramader del titular

Què has de tenir en compte:

- no tots els titulars són visibles per a tothom
- un tècnic normal només veu els titulars assignats
- un admin o gestor d'oficina en veu més segons la configuració de permisos

## 7. Mòdul agrícola

El mòdul agrícola mostra la informació agrícola del titular seleccionat.

Normalment hi trobaràs:

- dades bàsiques del titular
- terres vinculades al titular
- aplicacions de fertilitzants vinculades a les DAN del titular

### 7.1. Què pots editar

Si tens permisos suficients, pots modificar:

- NIF i nom del titular
- superfície d'una terra
- data d'una aplicació
- kg N
- UF

### 7.2. Què has de revisar quan edites

- que el NIF sigui correcte
- que la superfície sigui un número vàlid
- que la data estigui ben escrita
- que `kg N` i `UF` siguin valors numèrics

### 7.3. Què passa quan guardes

Quan guardes, l'aplicació envia els canvis a la base de dades. Si tens permís, els canvis es desaran. Si no tens permís, veuràs un error.

## 8. Mòdul ramader

El mòdul ramader mostra la informació ramadera del titular seleccionat.

Normalment hi trobaràs:

- dades bàsiques del titular
- granges del titular
- cens de bestiar per granja
- entregues de dejeccions relacionades amb les DAN del titular

### 8.1. Què pots editar

Si tens permisos suficients, pots modificar:

- NIF i nom del titular
- nom o marca oficial d'una granja
- cens d'una línia de bestiar
- data i quantitat d'una entrega

### 8.2. Recomanacions pràctiques

- revisa bé la `marca oficial` abans de guardar
- introdueix el `cens` com a número
- introdueix la `quantitat` de l'entrega com a valor numèric
- si no tens permís ramader sobre el titular, no podràs guardar

## 9. Perfil

A la pantalla `Perfil` pots consultar la teva informació com a usuari del sistema.

Hi veuràs:

- nom
- email
- rol
- oficina

Segons el teu accés, també podràs:

- editar el teu nom
- editar el teu email
- canviar la teva contrasenya

Si el canvi de password falla, revisa que:

- el nou password tingui almenys 6 caràcters
- la confirmació coincideixi
- el teu tècnic tingui `user_id` associat

## 10. Gestió de titulars

Aquesta pantalla està pensada per crear, editar i eliminar titulars.

### 10.1. Crear un titular

Passos:

1. Entra a `Gestio Titulars`.
2. Obre el diàleg de creació.
3. Escriu el nom.
4. Escriu el NIF si el tens.
5. Desa.

El nom és obligatori.

### 10.2. Editar un titular

Pots modificar:

- nom
- NIF

Després de desar, la llista es recarrega.

### 10.3. Eliminar un titular

Pots eliminar-lo des de la mateixa pantalla. Si aquell titular té dades relacionades, la base de dades pot impedir l'eliminació o eliminar també dades dependents segons la relació definida.

## 11. Gestió de terres

La pantalla `Terres` serveix per mantenir les terres del sistema.

### 11.1. Crear una terra

Cal introduir:

- titular associat, si n'hi ha
- codi municipal de 5 dígits
- polígon
- parcel·la
- recinte
- superfície

### 11.2. Validacions importants

- el codi municipal ha de tenir exactament 5 dígits
- polígon, parcel·la i recinte han de ser nombres enters
- la superfície ha de ser numèrica

### 11.3. Editar una terra

Pots canviar:

- titular associat
- superfície

El codi SIGPAC complet es calcula automàticament a partir dels camps base i no s'escriu manualment.

## 12. Gestió de tècnics

La pantalla `Tecnics` serveix per administrar usuaris operatius del sistema.

### 12.1. Crear un tècnic

Cal introduir:

- nom
- email
- password
- oficina
- rol

Quan el crees, el sistema fa dues coses:

1. crea l'usuari a Supabase Auth
2. crea el registre funcional a la taula `tecnic`

### 12.2. Activar o desactivar

Pots activar o desactivar un tècnic. Si està inactiu, no podrà treballar normalment amb l'aplicació.

### 12.3. Reset de password

Des d'aquesta pantalla també pots canviar el password d'un tècnic existent.

Recomanacions:

- el nou password ha de tenir almenys 6 caràcters
- comprova que el tècnic tingui `user_id`
- revisa bé l'email abans de comunicar les noves credencials

## 13. Detall de tècnic i assignacions

Quan obres el detall d'un tècnic pots veure o gestionar les assignacions amb titulars.

Cada assignació té:

- el titular
- l'scope
- si està activa o no

Scopes possibles:

- `comu`
- `agricola`
- `ramader`
- `lectura`

Interpretació pràctica:

- `comu`: accés ampli sobre el titular
- `agricola`: només part agrícola
- `ramader`: només part ramadera
- `lectura`: pensat per lectura sense escriptura

## 14. Gestió d'oficines

La pantalla `Oficines` permet crear, editar i eliminar oficines.

### 14.1. Crear

Només cal indicar:

- nom de l'oficina

### 14.2. Editar

Pots canviar el nom de l'oficina.

### 14.3. Eliminar

Si hi ha tècnics associats a una oficina, la base de dades pot impedir l'eliminació.

## 15. Missatges d'error més habituals

### 15.1. Credencials incorrectes

Vol dir que l'email o la contrasenya no són correctes.

### 15.2. Sense permís

Sol aparèixer com a `401` o `403`.

Vol dir que:

- no tens accés al titular
- no tens l'scope necessari
- el teu rol no permet aquella operació

### 15.3. Error de validació

Pot passar si:

- falta un camp obligatori
- un número no està ben escrit
- una contrasenya és massa curta
- el codi municipal no té 5 dígits

### 15.4. No s'ha trobat perfil tècnic

Vol dir que tens usuari d'Auth però no hi ha un registre vàlid a `public.tecnic` per a tu.

## 16. Bones pràctiques d'ús

Per treballar bé amb l'aplicació:

- entra sempre amb el teu usuari real
- revisa el titular abans d'editar dades
- comprova si estàs al mòdul agrícola o al ramader
- no intentis repetir una operació si t'ha sortit un error de permisos
- avisa a l'administrador si no veus titulars que hauries de veure
- mantén actualitzat el teu perfil i password

## 17. Què fa el programa i què no fa encara

L'aplicació actual permet:

- iniciar sessió
- carregar el perfil tècnic
- veure titulars segons permisos
- treballar dades agrícoles i ramaderes bàsiques
- gestionar oficines, tècnics, titulars i terres

Actualment encara no fa:

- importació automàtica des d'Excel
- càlculs avançats mostrats com a informes finals
- exportacions PDF o informes oficials complets

## 18. Resum ràpid per començar a treballar

Si és el primer cop que la fas servir, l'ordre recomanat és aquest:

1. inicia sessió
2. comprova el teu perfil
3. entra a `Titulars`
4. busca el titular amb el qual has de treballar
5. entra al mòdul agrícola o ramader
6. revisa les dades
7. edita només els camps permesos
8. desa i comprova el missatge de confirmació

Aquest flux és el recorregut principal de l'aplicació.
