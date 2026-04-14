# Permisos i seguretat a AgriSync

## 1. Objectiu

El model de permisos d'AgriSync vol garantir que el control real d'acces no depengui de la UI, sino de la base de dades.

La idea base es:

- Auth identifica l'usuari
- `public.tecnic` defineix el perfil funcional
- `tecnic_titular` concreta sobre quins titulars pot treballar
- l'RLS decideix finalment que es pot llegir o modificar

## 2. Peces del sistema

El model es basa en quatre peces:

- Supabase Auth
- taula `public.tecnic`
- taula `public.tecnic_titular`
- policies RLS del `SQLAgriSync.sql`

## 3. Que fa cada capa

### 3.1. Supabase Auth

Responsabilitats:

- validar credencials
- generar la sessio
- exposar `auth.uid()`

### 3.2. `public.tecnic`

Es el pont entre l'usuari autenticat i el model funcional.

Camps clau:

- `user_id`
- `oficina_id`
- `rol`
- `actiu`

Sense un tecnic valid i actiu, l'usuari no pot operar normalment a l'app.

### 3.3. `public.tecnic_titular`

Defineix la relacio d'un tecnic amb cada titular.

Camps clau:

- `tecnic_id`
- `titular_id`
- `scope`
- `actiu`

## 4. Rols globals

Els rols globals del sistema son:

- `admin`
- `oficina_manager`
- `tecnic`
- `lectura`

### 4.1. `admin`

Pot gestionar tota l'estructura i totes les dades del MVP.

### 4.2. `oficina_manager`

Es un gestor d'ambit intermedi.

No te visio global de tots els titulars. El seu ambit queda limitat pels helpers de BDD.

### 4.3. `tecnic`

Es el perfil operatiu habitual. Depen de les seves assignacions i scopes.

### 4.4. `lectura`

Es un rol global de consulta. No rep permisos d'escriptura especifics.

## 5. Scopes per titular

Els scopes disponibles son:

- `comu`
- `agricola`
- `ramader`
- `lectura`

Interpretacio funcional:

- `comu`
  Pot operar tant la part agricola com la ramadera del titular.
- `agricola`
  Pot operar la part agricola.
- `ramader`
  Pot operar la part ramadera.
- `lectura`
  Dona lectura, no escriptura.

## 6. Per que hi ha rol i scope alhora

Son dos nivells diferents:

- el rol defineix la posicio global a l'aplicacio
- l'scope defineix el tipus de feina per un titular concret

Exemple:

- un `admin` no necessita assignacions per veure dades
- un `tecnic` normal si
- un mateix tecnic pot tenir `agricola` en un titular i `ramader` en un altre

## 7. Helpers de seguretat definits al SQL

El `SQLAgriSync.sql` defineix aquestes funcions helper:

- `get_my_tecnic()`
- `current_oficina_id()`
- `is_admin()`
- `is_oficina_manager()`
- `same_oficina(uuid)`
- `can_self_update_tecnic(...)`
- `can_manage_office_titular(uuid)`
- `can_read_titular(uuid)`
- `can_write_scope(uuid, scope_titular)`
- `can_write_agricola(uuid)`
- `can_write_ramader(uuid)`

## 8. Significat real dels helpers importants

### 8.1. `current_oficina_id()`

Retorna l'oficina del tecnic autenticat i actiu.

### 8.2. `same_oficina(p_tecnic_id)`

Comprova si el tecnic indicat pertany a la mateixa oficina que l'usuari autenticat.

### 8.3. `can_manage_office_titular(p_titular_id)`

Es la funcio clau per acotar `oficina_manager`.

Permet gestionar un titular si es compleix una d'aquestes condicions:

- el titular va ser creat per l'usuari actual
- o hi ha almenys un tecnic actiu de la mateixa oficina assignat a aquell titular

Per tant, `oficina_manager` no veu tot el sistema.

### 8.4. `can_read_titular(p_titular_id)`

Permet lectura si:

- l'usuari es `admin`
- o es `oficina_manager` i el titular entra dins del seu ambit de gestio
- o te una assignacio activa a `tecnic_titular`

### 8.5. `can_write_scope(...)`

Permet escriptura si:

- l'usuari es `admin`
- o es `oficina_manager` i el titular entra dins del seu ambit
- o te `scope = comu`
- o te exactament el scope demanat

### 8.6. `can_self_update_tecnic(...)`

Aquesta funcio protegeix l'autoedicio del perfil.

Permet que un usuari actualitzi el seu propi registre funcional nomes si mante iguals els camps sensibles:

- `user_id`
- `oficina_id`
- `rol`
- `actiu`

Aixo evita escalades de privilegi per autoedicio directa sobre la taula `tecnic`.

## 9. Grants i RLS

La logica de seguretat combina dues coses:

- `GRANT`
- `RLS`

Interpretacio correcta:

- el `GRANT` obre la possibilitat tecnica d'intentar `select`, `insert`, `update` o `delete`
- la policy RLS decideix si aquella fila concreta es pot tocar

El projecte dona `grant` de DML a `authenticated` sobre les taules del MVP, pero el filtre real el fa l'RLS.

## 10. Que fa `fix_permisos.sql`

`fix_permisos.sql` no canvia el model funcional.

El que fa es:

- reaplicar `grant` sobre les taules
- reaplicar `execute` sobre les funcions helper
- assegurar que `service_role` mante acces administratiu

Es un script de manteniment, no un script de modelatge.

## 11. Taules protegides per RLS

L'RLS esta actiu a:

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

## 12. Resum de permisos per taula

### 12.1. `oficina`

- `select`
  `admin` i `oficina_manager`
- `insert`, `update`, `delete`
  nomes `admin`

### 12.2. `tecnic`

- `select`
  `admin`, `oficina_manager` de la mateixa oficina o el mateix usuari sobre el seu registre
- `insert`
  `admin` o `oficina_manager` dins la seva oficina
- `update`
  `admin`, `oficina_manager` de la mateixa oficina o el mateix usuari de forma limitada per `can_self_update_tecnic(...)`
- `delete`
  `admin` o `oficina_manager` de la mateixa oficina

### 12.3. `titular`

- `select`
  qui compleixi `can_read_titular(...)`
- `insert`
  `admin` o `oficina_manager`
- `update`
  qui compleixi `can_write_scope(..., 'comu')`
- `delete`
  `admin` o `oficina_manager` dins del seu ambit

### 12.4. `tecnic_titular`

- `select`
  `admin`, `oficina_manager` sobre tecnics de la seva oficina o el mateix tecnic
- `insert`, `update`, `delete`
  `admin` o `oficina_manager` si el tecnic es de la seva oficina i el titular entra en el seu ambit

### 12.5. `dan_declaracio`

- `select`
  qui pot llegir el titular relacionat
- `insert`, `update`
  qui pot escriure part agricola o ramadera del titular
- `delete`
  `admin` o `oficina_manager` dins del seu ambit

### 12.6. `terra`

- `select`
  `admin`, alguns casos puntuals de terres sense titular creades pel mateix gestor, o qui pugui llegir el titular
- `insert`, `update`, `delete`
  `admin`, `oficina_manager` dins del seu ambit o qui tingui permis agricola sobre el titular

### 12.7. `aplicacions_fertilitzants`

- `select`
  qui pot llegir el titular de la DAN relacionada
- `insert`, `update`, `delete`
  qui pot escriure part agricola del titular relacionat

### 12.8. `granja`

- `select`
  qui pot llegir el titular propietari
- `insert`, `update`, `delete`
  qui pot escriure part ramadera del titular

### 12.9. `bestiar`

- `select`
  qualsevol usuari autenticat
- `insert`, `update`, `delete`
  nomes `admin`

### 12.10. `fase_productiva`

- `select`
  qualsevol usuari autenticat
- `insert`, `update`, `delete`
  nomes `admin`

### 12.11. `granja_bestiar`

- `select`
  qui pot llegir el titular de la granja
- `insert`, `update`, `delete`
  qui pot escriure part ramadera del titular de la granja

### 12.12. `entrega_dejeccions`

- `select`
  qui pot llegir el titular de la DAN relacionada
- `insert`, `update`, `delete`
  qui pot escriure part ramadera del titular de la DAN

## 13. Herencia funcional dels permisos

Moltes taules hereten el control d'acces d'una entitat superior.

Exemples:

- `terra` hereta del `titular`
- `aplicacions_fertilitzants` hereta de `dan_declaracio` i, a traves d'aquesta, del `titular`
- `granja` hereta del `titular`
- `granja_bestiar` hereta de `granja`
- `entrega_dejeccions` hereta de `dan_declaracio`

## 14. Relacio amb `service_role`

L'aplicacio encara fa servir `service_role` per algunes operacions administratives i de suport a Auth, com:

- crear usuaris
- canviar passwords
- eliminar usuaris Auth
- resoldre noms d'actor a auditories

També hi ha un matís al login:

- el cami normal de login no depen de `service_role`
- primer es prova `get_my_tecnic()` i la consulta directa amb el token de l'usuari
- nomes si hi ha un desalineament de `user_id` es mante un fallback tecnic per email amb `service_role` per intentar autocorregir-lo

Pero el model funcional explicat en aquest document descriu el cami normal de treball dels usuaris autenticats via `authenticated` i RLS.

## 15. Usuaris recomanats per provar permisos

Amb `seed_final_demo.sql` pots validar aquests casos:

- `admin.demo@agrisync.com`
  Acces global.
- `manager.lleida.demo@agrisync.com`
  Gestio d'ambit Lleida.
- `manager.girona.demo@agrisync.com`
  Gestio d'ambit Girona.
- `sergi.agri.demo@agrisync.com`
  Tecnic agricola.
- `marta.ram.demo@agrisync.com`
  Tecnic ramader.
- `laia.comu.demo@agrisync.com`
  Tecnica amb visio comuna.
- `lectura.demo@agrisync.com`
  Lectura sense escriptura.

## 16. Resum final

El sistema de permisos d'AgriSync es basa en:

- autenticacio real amb Supabase Auth
- perfil funcional a `public.tecnic`
- assignacions a `tecnic_titular`
- funcions helper de permisos
- RLS per fila

La part mes important del model actual es que:

- `admin` veu tot
- `oficina_manager` ja no es global
- el tecnic normal depen de titular i `scope`
- l'autoedicio de perfil ha quedat acotada per no permetre canvis de privilegi
