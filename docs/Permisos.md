# Permisos i Seguretat a AgriSync

## 1. Objectiu del sistema de permisos

El sistema de permisos d'AgriSync està pensat perquè el control d'accés real no depengui només de la interfície, sinó de la base de dades. Això vol dir que encara que un usuari conegui l'endpoint REST, la base de dades continua decidint què pot veure i què pot modificar.

Objectius del model:

- cada usuari només veu la informació que li correspon
- només poden modificar dades els usuaris amb permís real
- els permisos es poden justificar de manera clara a nivell funcional i tècnic

## 2. Estructura general dels permisos

El sistema es basa en tres capes:

- autenticació amb Supabase Auth
- model funcional de tècnics, rols i assignacions a la BDD
- polítiques RLS sobre les taules

Flux resumit:

1. l'usuari fa login amb Auth
2. el sistema identifica `auth.uid()`
3. es recupera el registre de `public.tecnic`
4. a partir d'aquí es determinen rol, oficina i assignacions
5. les policies RLS decideixen l'accés final

## 3. Peces principals

### 3.1. Supabase Auth

Serveix per autenticar l'usuari.

Responsabilitats:

- validar credencials
- generar el token JWT
- identificar l'usuari autenticat

### 3.2. Taula `tecnic`

És el pont entre l'usuari d'Auth i el model funcional.

Camps que afecten permisos:

- `user_id`
- `oficina_id`
- `rol`
- `actiu`

Sense un registre vàlid en aquesta taula, l'usuari no pot treballar dins del sistema.

### 3.3. Taula `tecnic_titular`

Defineix a quins titulars pot accedir cada tècnic i amb quin àmbit funcional.

Camps clau:

- `tecnic_id`
- `titular_id`
- `scope`
- `actiu`

## 4. Rols globals

Els rols globals són:

- `admin`
- `oficina_manager`
- `tecnic`
- `lectura`

### 4.1. `admin`

Té accés global al sistema.

En general pot:

- veure totes les oficines
- gestionar tècnics
- gestionar titulars
- accedir a dades agrícoles i ramaderes
- administrar catàlegs

### 4.2. `oficina_manager`

És un rol intermedi de gestió.

En general pot:

- veure oficines
- gestionar tècnics del seu àmbit
- gestionar titulars
- operar sobre dades del MVP

### 4.3. `tecnic`

És el rol operatiu habitual.

En general:

- només veu titulars assignats
- només pot modificar allò que li permet el seu `scope`

### 4.4. `lectura`

És un rol previst al model.

En el SQL actual:

- existeix com a rol vàlid
- no rep permisos d'escriptura específics

## 5. Scopes per titular

Els scopes són:

- `comu`
- `agricola`
- `ramader`
- `lectura`

### 5.1. `comu`

Permet treballar tant la part agrícola com la ramadera del titular.

### 5.2. `agricola`

Permet treballar la part agrícola.

### 5.3. `ramader`

Permet treballar la part ramadera.

### 5.4. `lectura`

Representa accés sense escriptura. Les funcions `can_write_agricola` i `can_write_ramader` no el consideren suficient per editar.

## 6. Per què hi ha rol i scope alhora

Són dos nivells diferents:

- el rol defineix la posició global dins del sistema
- l'scope defineix què pot fer aquell tècnic sobre un titular concret

Exemple:

- un `admin` no necessita assignacions per veure-ho tot
- un `tecnic` normal sí que necessita assignacions a `tecnic_titular`
- un mateix tècnic pot tenir `agricola` en un titular i `ramader` en un altre

## 7. Funcions helper de permisos

El SQL defineix funcions helper per simplificar les policies.

### 7.1. `get_my_tecnic()`

Recupera el tècnic associat a `auth.uid()`.

### 7.2. `current_oficina_id()`

Retorna l'oficina del tècnic autenticat.

### 7.3. `is_admin()`

Comprova si l'usuari és admin.

### 7.4. `is_oficina_manager()`

Comprova si l'usuari és gestor d'oficina.

### 7.5. `same_oficina(p_tecnic_id uuid)`

Comprova si un tècnic és de la mateixa oficina que l'usuari actual.

### 7.6. `can_read_titular(p_titular_id uuid)`

Permet llegir un titular si:

- l'usuari és `admin`
- l'usuari és `oficina_manager`
- o té una assignació activa sobre aquell titular

### 7.7. `can_write_scope(p_titular_id uuid, p_scope scope_titular)`

Permet escriure si:

- l'usuari és `admin`
- l'usuari és `oficina_manager`
- té `scope = comu`
- o té exactament l'scope requerit

### 7.8. `can_write_agricola(...)`

Especialització per a la part agrícola.

### 7.9. `can_write_ramader(...)`

Especialització per a la part ramadera.

## 8. Què és RLS

RLS vol dir `Row Level Security`.

És el mecanisme de PostgreSQL que permet definir permisos per fila, no només per taula. Això fa possible que dos usuaris consultin la mateixa taula però no vegin els mateixos registres.

## 9. Diferència entre GRANT i RLS

### 9.1. GRANT

El `GRANT` permet intentar una operació sobre una taula.

### 9.2. RLS

La `policy` decideix si aquella operació concreta està realment permesa.

Per tant:

- el `GRANT` obre la porta general
- la `policy RLS` decideix si pots passar

## 10. Taules amb RLS al MVP actual

El SQL actual activa RLS a:

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

## 11. Policies per taula

### 11.1. `oficina`

- `select`: `admin`, `oficina_manager`
- `insert`, `update`, `delete`: només `admin`

### 11.2. `tecnic`

- `select`: `admin`, `oficina_manager` de la mateixa oficina, o el mateix usuari sobre el seu registre
- `insert`: `admin` o `oficina_manager` dins la seva oficina
- `update`: `admin`, `oficina_manager` dins la seva oficina, o el mateix usuari sobre el seu registre
- `delete`: `admin` o `oficina_manager` dins la seva oficina

### 11.3. `titular`

- `select`: qui compleixi `can_read_titular(...)`
- `insert`: `admin` o `oficina_manager`
- `update`: `admin`, `oficina_manager` o tècnic amb `scope = comu`
- `delete`: `admin` o `oficina_manager`

### 11.4. `tecnic_titular`

- `select`: `admin`, `oficina_manager` o el mateix tècnic sobre les seves assignacions
- `insert`, `update`, `delete`: `admin` o `oficina_manager`

### 11.5. `dan_declaracio`

- `select`: si l'usuari pot llegir el titular relacionat
- `insert`, `update`: `admin`, `oficina_manager` o qui tingui permís agrícola o ramader sobre el titular
- `delete`: `admin` o `oficina_manager`

### 11.6. `terra`

- `select`: `admin`, `oficina_manager` o qui pugui llegir el titular de la terra
- `insert`, `update`, `delete`: `admin`, `oficina_manager` o qui tingui `can_write_agricola(...)`

### 11.7. `aplicacions_fertilitzants`

- `select`: `admin`, `oficina_manager` o qui pugui llegir el titular de la DAN
- `insert`, `update`, `delete`: `admin`, `oficina_manager` o qui tingui permís agrícola sobre el titular de la DAN

### 11.8. `granja`

- `select`: qui pugui llegir el titular propietari
- `insert`, `update`, `delete`: `admin`, `oficina_manager` o qui tingui permís ramader sobre el titular

### 11.9. `bestiar`

- `select`: qualsevol usuari autenticat
- `insert`, `update`, `delete`: només `admin`

### 11.10. `fase_productiva`

- `select`: qualsevol usuari autenticat
- `insert`, `update`, `delete`: només `admin`

### 11.11. `granja_bestiar`

- `select`: `admin`, `oficina_manager` o qui pugui llegir el titular de la granja
- `insert`, `update`, `delete`: `admin`, `oficina_manager` o qui tingui permís ramader sobre el titular de la granja

### 11.12. `entrega_dejeccions`

- `select`: `admin`, `oficina_manager` o qui pugui llegir el titular de la DAN
- `insert`, `update`, `delete`: `admin`, `oficina_manager` o qui tingui permís ramader sobre el titular de la DAN

## 12. Herència funcional dels permisos

Moltes taules no defineixen permisos completament independents, sinó que hereten lògica d'una entitat superior.

Exemples:

- `terra` hereta del `titular`
- `aplicacions_fertilitzants` hereta del `titular` a través de `dan_declaracio`
- `granja` hereta del `titular`
- `granja_bestiar` hereta del `titular` a través de `granja`
- `entrega_dejeccions` hereta del `titular` a través de `dan_declaracio`

## 13. Exemples pràctics

### Cas 1. `admin`

- veu tot
- pot gestionar estructura i dades

### Cas 2. `oficina_manager`

- gestiona tècnics del seu àmbit
- pot operar sobre dades del MVP

### Cas 3. Tècnic amb `scope = agricola`

- pot veure el titular
- pot editar terres i aplicacions
- no pot editar la part ramadera

### Cas 4. Tècnic amb `scope = ramader`

- pot veure el titular
- pot editar granges, cens i entregues
- no pot editar la part agrícola

### Cas 5. Tècnic amb `scope = comu`

- pot operar tant a agrícola com a ramader
- pot editar dades bàsiques del titular

### Cas 6. Tècnic amb `scope = lectura`

- pot quedar limitat a lectura
- no obté permisos d'escriptura a través de `can_write_*`

## 14. Relació amb els usuaris de prova

El `seed_complet.sql` està pensat perquè puguis provar diferents perfils.

Usuaris de prova previstos:

- `admin.test@agrisync.com` / `admin1234`
- `manager.test@agrisync.com` / `manager1234`
- `agricola.test@agrisync.com` / `agricola1234`
- `ramader.test@agrisync.com` / `ramader1234`
- `lectura.test@agrisync.com` / `lectura1234`

Això et permet validar el comportament del sistema de permisos amb casos reals.

## 15. Valor del model

Aquest model és defensable perquè:

- la seguretat no depèn de la UI
- separa autenticació i autorització
- combina rol global i permís específic per titular
- encaixa amb el domini real del projecte

## 16. Millores futures

Possibles millores:

- donar més comportament específic al rol `lectura`
- restringir encara més l'abast d'`oficina_manager` si cal
- afegir proves automàtiques de permisos per rol i taula

## 17. Resum final

El sistema de permisos d'AgriSync es basa en Supabase Auth per identificar l'usuari i en PostgreSQL amb RLS per decidir què pot veure i modificar.

La combinació de:

- `user_id`
- `rol`
- `oficina`
- `tecnic_titular`
- `scope`
- `policies`

permet un control d'accés robust i adequat per a un MVP acadèmic amb dades sensibles.
