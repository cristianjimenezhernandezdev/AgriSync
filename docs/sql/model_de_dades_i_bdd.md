# Model de dades i funcionament detallat de la BDD

## Objectiu d'aquest document

Aquest document explica la base de dades d'AgriSync amb detall suficient per entendre:

- quin és el model conceptual
- quines taules existeixen i per què
- quines relacions hi ha
- com funciona la seguretat real
- com es connecta la BDD amb la UI
- quins scripts cal executar
- quines incidències son normals i com diagnosticar-les

La base de dades no és un simple magatzem passiu. En aquest projecte també és el motor real de permisos.

## Idea central del model

El nucli del sistema és el `titular`. Al seu voltant es construeixen:

- dades administratives
- dades agricoles
- dades ramaderes
- comparticions
- permisos
- campanyes DAN

La relació de nivell alt és aquesta:

```text
auth.users
   -> public.tecnic
      -> public.tecnic_titular
         -> public.titular
            -> public.dan_declaracio
            -> public.terra
            -> public.granja
               -> public.granja_bestiar
               -> public.granja_campanya_balance
               -> public.entrega_dejeccions
            -> public.aplicacions_fertilitzants

public.oficina
   -> public.tecnic
   -> public.oficina_titular_compartit
```

## Fitxers SQL del projecte

### Esquema principal

- `docs/sql/schema/agrisync_schema.sql`

Conté:

- `DROP` inicial d'objectes
- tipus enumerats
- taules
- indexos
- trigger d'auditoria
- funcions helper
- grants
- activació d'RLS
- policies

### Seed principal

- `docs/sql/seeds/agrisync_demo_seed.sql`

Conté:

- creació dels usuaris Auth demo
- càrrega de demo
- consultes finals de comprovació

### Scripts de manteniment

- `docs/sql/maintenance/reset_auth_seed_users.sql`

## Ordre correcte d'execució

1. `maintenance/reset_auth_seed_users.sql`
2. `schema/agrisync_schema.sql`
3. `seeds/agrisync_demo_seed.sql`

## Tipus enumerats

El `schema` defineix tres enums:

### `public.rol_global`

Valors:

- `admin`
- `oficina_manager`
- `tecnic`
- `lectura`

Serveix per definir el rol global d'un tecnic.

### `public.scope_titular`

Valors:

- `comu`
- `agricola`
- `ramader`
- `lectura`

Serveix per definir l'àmbit d'accés sobre un titular.

### `public.zona_nitrogen`

Valors:

- `ZV`
- `ZNV`

Serveix per calcular el límit de nitrogen per hectàrea de la terra.

## Taules principals

## 1. `public.oficina`

Propòsit:

- representa una oficina funcional de treball

Columnes clau:

- `id`
- `nom`

Relacions:

- una oficina pot tenir molts `tecnic`
- una oficina pot aparèixer a `oficina_titular_compartit`

Pantalles que la fan servir:

- `OficinaManagement`
- `Tecnics`
- `Perfil`
- gestió de comparticions

## 2. `public.tecnic`

Propòsit:

- perfil funcional d'un usuari autenticat

Columnes clau:

- `id`
- `oficina_id`
- `user_id`
- `nom`
- `email`
- `telefon`
- `rol`
- `actiu`
- `created_*`
- `updated_*`

Comentari important:

- `auth.users` conté la identitat d'Auth
- `public.tecnic` conté el perfil de negoci
- el projecte només considera operatiu un usuari si existeix a `public.tecnic` i està `actiu = true`

Pantalles que la fan servir:

- login
- perfil
- gestió de tècnics
- detall de tècnic

## 3. `public.titular`

Propòsit:

- entitat central del domini

Columnes clau:

- `id`
- `nif`
- `nom_rao`
- `telefon`
- `email`
- `adreca`
- `codi_postal`
- `created_*`
- `updated_*`

Pantalles que la fan servir:

- home de titulars
- mòdul agrícola
- mòdul ramader
- `Preparar DAN`
- gestió de titulars

## 4. `public.tecnic_titular`

Propòsit:

- assignació directa entre un tècnic i un titular

Columnes clau:

- `tecnic_id`
- `titular_id`
- `scope`
- `actiu`

Comentari:

- és la peça bàsica per donar accés específic a un titular
- hi pot haver diverses files per al mateix tècnic i titular amb scopes diferents

## 5. `public.oficina_titular_compartit`

Propòsit:

- compartir un titular amb una oficina sencera

Columnes clau:

- `oficina_id`
- `titular_id`
- `scope`

Comentari:

- permet que una oficina tingui accés a titulars no creats o no gestionats directament per ella

## 6. `public.dan_declaracio`

Propòsit:

- representar el context de treball d'un titular en una campanya

Columnes clau:

- `titular_id`
- `campanya`
- `estat`

Comentari:

- hi ha unicitat per `(titular_id, campanya)`
- moltes operacions agrícoles i ramaderes pengen de la DAN de la campanya

## 7. `public.terra`

Propòsit:

- terres, parcel·les i recintes associats a un titular

Columnes clau:

- `titular_id`
- `mun_codi`
- `poligon`
- `parcela`
- `recinte`
- `codi_sigpac_complet` generat
- `municipi_literal`
- `us_sigpac`
- `cultiu`
- `superficie`
- `zona`
- `limit_kg_n_ha` generat

Comentaris importants:

- `codi_sigpac_complet` es calcula automàticament
- `limit_kg_n_ha` també es calcula segons `zona`
- això evita duplicació i inconsistències

## 8. `public.aplicacions_fertilitzants`

Propòsit:

- registrar aplicacions de fertilitzant a una terra dins d'una DAN concreta

Columnes clau:

- `dan_id`
- `terra_id`
- `data`
- `tipus_fertilitzant`
- `procedencia`
- `volum_m3`
- `kg_n_m3`
- `kg_n`
- `tecnic_id`
- `entrega_id`

Pantalla principal:

- `Modul Agricola`

Comentaris importants:

- una aplicacio pot ser manual o venir sincronitzada des d'una `entrega_dejeccions`
- si ve d'una entrega, la seva traçabilitat fins a la granja origen queda preservada
- `kg_n`, `volum_m3` i `kg_n_m3` s'han de poder resoldre entre si

## 9. `public.granja`

Propòsit:

- explotacions ramaderes del titular

Columnes clau:

- `titular_id`
- `marca_oficial`
- `nom`

Pantalla principal:

- `Modul Ramader`

## 10. `public.bestiar`

Propòsit:

- catàleg de tipus de bestiar

Exemples:

- boví
- porcí
- oví

## 11. `public.fase_productiva`

Propòsit:

- catàleg de fases productives

Exemples:

- cria
- engreix
- llet

## 12. `public.granja_bestiar`

Propòsit:

- enllaç entre granja, tipus de bestiar, fase productiva i cens

Columnes clau:

- `granja_id`
- `bestiar_id`
- `fase_productiva_id`
- `cens`

Pantalla principal:

- `Modul Ramader`

## 13. `public.granja_campanya_balance`

Propòsit:

- guardar el balanç de nitrogen per granja i campanya

Columnes clau:

- `dan_id`
- `granja_id`
- `estoc_inicial_kg_n`
- `kg_n_generat`
- `estoc_final_declarat_kg_n`

Comentari:

- permet comparar el nitrogen justificat per entregues amb l'estoc final declarat

## 14. `public.entrega_dejeccions`

Propòsit:

- registrar sortides de dejeccions sempre cap a una terra concreta

Columnes clau:

- `dan_id`
- `granja_origen_id`
- `terra_desti_id`
- `data`
- `tipus_fertilitzant`
- `volum_m3`
- `kg_n_m3`
- `kg_n`

Restricció important:

- tota entrega ha d'anar a una `terra`
- l'entrega representa una justificació agrícola real
- el sistema pot sincronitzar aquesta entrega cap a `aplicacions_fertilitzants`

## Auditoria

El `schema` defineix una funció:

- `public.audit_fill_actor()`

I la connecta amb triggers sobre:

- `tecnic`
- `titular`
- `oficina_titular_compartit`
- `dan_declaracio`
- `terra`
- `aplicacions_fertilitzants`
- `granja`
- `granja_bestiar`
- `entrega_dejeccions`

Com funciona:

- en `INSERT`, omple `created_at`, `created_by`, `updated_at`, `updated_by`
- en `UPDATE`, refresca `updated_at` i `updated_by`

Això permet que la UI mostri:

- última actualització
- últim editor

## Integritat, restriccions i columnes calculades

La BDD no es limita a guardar dades. També imposa coherència.

### Restriccions destacades

- `tecnic.user_id` es únic
- `oficina.nom` es únic
- `granja.marca_oficial` es única
- `bestiar.codi` es únic
- `fase_productiva.codi` es únic
- `dan_declaracio` es única per `(titular_id, campanya)`
- `tecnic_titular` es única per `(tecnic_id, titular_id, scope)`
- `oficina_titular_compartit` es única per `(oficina_id, titular_id, scope)`
- `granja_bestiar` es única per `(granja_id, bestiar_id, fase_productiva_id)`
- `terra` es única per `(mun_codi, poligon, parcela, recinte)`

### `CHECK` destacats

- `codi_postal` ha de tenir 5 digits si s'informa
- `mun_codi` ha de tenir 5 digits
- `poligon`, `parcela` i `recinte` han de ser positius
- `superficie`, `cens`, `kg_n`, `volum_m3` i altres magnituds no poden ser negatives
- `entrega_dejeccions` obliga a informar una terra de destí
- `kg_n`, `volum_m3` i `kg_n_m3` han de quadrar entre si

### Columnes calculades importants

- `terra.codi_sigpac_complet`
- `terra.limit_kg_n_ha`

Aquestes columnes són especialment útils perquè:

- eviten recalcular constantment al client
- centralitzen la regla de negoci en una sola capa

## Indexos principals

L'esquema crea indexos per accelerar consultes habituals. Alguns dels més importants són:

- `idx_tecnic_oficina_id`
- `idx_tecnic_user_id`
- `idx_tecnic_email`
- `idx_tecnic_telefon`
- `idx_tecnic_titular_tecnic`
- `idx_tecnic_titular_titular`
- `idx_titular_nif`
- `idx_titular_telefon`
- `idx_titular_email`
- `idx_titular_codi_postal`
- `idx_oficina_titular_compartit_oficina`
- `idx_oficina_titular_compartit_titular`
- `idx_dan_titular`
- `idx_terra_titular`
- `idx_aplicacions_dan`
- `idx_aplicacions_terra`
- `idx_granja_titular`
- `idx_gb_granja`
- `idx_entrega_dan`
- `idx_balance_dan`
- `idx_balance_granja`
- `idx_aplicacions_entrega`

El sentit d'aquests indexos no és teòric. Responen directament a com consulta el client:

- cerca de titulars per NIF, telèfon, email i CP
- resolució de tècnic per `user_id`
- càrrega de terres, DAN i aplicacions per titular
- resolució de comparticions i assignacions

## Funcions helper de seguretat

Aquestes funcions són clau perquè la lògica de permisos viu aquí.

### Identitat i context

- `get_my_tecnic()`
- `current_oficina_id()`
- `is_admin()`
- `is_oficina_manager()`
- `same_oficina(uuid)`

### Permisos sobre tècnics i oficines

- `can_self_update_tecnic(...)`
- `can_view_tecnic(uuid)`
- `can_view_oficina(uuid)`

### Permisos sobre titulars

- `can_manage_office_titular(uuid)`
- `office_has_any_share(uuid, uuid)`
- `office_has_shared_scope(uuid, scope_titular, uuid)`
- `can_read_titular(uuid)`
- `can_write_scope(uuid, scope_titular)`
- `can_write_agricola(uuid)`
- `can_write_ramader(uuid)`

### Referències creuades

- `can_reference_terra(uuid)`

## Flux real de permisos

La pregunta important no és "quin rol té l'usuari?", sinó:

- quin `public.tecnic` és
- a quina oficina pertany
- quins titulars té assignats
- quins titulars rep la seva oficina com a compartits
- quin `scope` té sobre cada titular

### Exemple 1. Un tècnic normal entra a la home

1. el login resol `public.tecnic`
2. la UI consulta titulars visibles
3. `tecnic_titular` i RLS filtren el que pot veure
4. la UI rep només el que la BDD deixa passar

### Exemple 2. Un manager comparteix un titular

1. la UI intenta inserir a `oficina_titular_compartit`
2. la policy comprova `can_manage_office_titular(titular_id)`
3. si no el pot gestionar, la inserció falla

### Exemple 3. Crear una aplicació agrícola

1. la UI envia un `POST` a `aplicacions_fertilitzants`
2. la policy comprova si la DAN és d'un titular sobre el qual hi ha `can_write_agricola`
3. si no, torna error

### Exemple 4. Crear una entrega ramadera

1. la UI envia un `POST` a `entrega_dejeccions`
2. la policy comprova:
   - escriptura ramadera sobre el titular origen
   - referència a terra de destí
3. la BDD pot sincronitzar automàticament una aplicació fertilitzant equivalent sobre la terra de destí

## Grants i RLS

Després de crear les taules, l'script fa tres coses:

1. dona `grant select, insert, update, delete` a `authenticated`
2. activa `row level security`
3. crea una policy per cada tipus d'operació i taula

Important:

- el `grant` sol no dona accés real
- la decisió final la fan les policies RLS

## Resum de polítiques per taula

| Taula | Lectura | Escriptura |
|---|---|---|
| `oficina` | `can_view_oficina` | només `admin` |
| `tecnic` | `can_view_tecnic` | `admin`, manager de la mateixa oficina o autoactualització restringida |
| `titular` | `can_read_titular` | segons `can_write_scope` o rol |
| `tecnic_titular` | admin, manager o lectura vinculada al titular | admin o manager dins del seu abast |
| `oficina_titular_compartit` | admin, managers i usuaris amb lectura del titular | admin o manager que pot gestionar el titular |
| `dan_declaracio` | lectura del titular | escriptura agrícola o ramadera sobre el titular |
| `terra` | lectura del titular | escriptura agrícola o gestió específica de manager/admin |
| `aplicacions_fertilitzants` | lectura del titular de la DAN | escriptura agrícola |
| `granja` | lectura del titular | escriptura ramadera |
| `granja_bestiar` | lectura del titular de la granja | escriptura ramadera |
| `granja_campanya_balance` | lectura del titular de la DAN | escriptura ramadera |
| `entrega_dejeccions` | lectura del titular origen o de la terra destí | escriptura ramadera amb validacions addicionals |
| `bestiar`, `fase_productiva` | lectura oberta a autenticats | administració restringida a `admin` |

## Seed de demo

El seed principal està pensat per simular un entorn funcional coherent. Inclou:

- 3 oficines
- 10 tècnics
- 10 titulars
- assignacions multirol
- comparticions interoficina
- dades de campanyes 2024 i 2025
- terres
- aplicacions
- granges
- cens
- entregues

També està preparat per provar casos com:

- titulars compartits
- usuaris de lectura
- treball agrícola i ramader sobre el mateix titular
- col·laboració entre oficines
- traçabilitat de sortides cap a terres amb justificació nitrogenada

## Scripts SQL actuals

### `reset_auth_seed_users.sql`

Buida completament `auth.users` i deixa `public.tecnic.user_id` a `null`.

### `agrisync_schema.sql`

Reconstrueix l'esquema funcional complet del projecte.

### `agrisync_demo_seed.sql`

Crea els usuaris Auth demo i carrega totes les dades de demostracio.

## Errors habituals de BDD

### 1. El `schema` falla creant una funció

Causes habituals:

- ordre incorrecte si s'usa una versió antiga del fitxer
- execució parcial d'un script antic

Solució:

- executar la versió actual de `schema/agrisync_schema.sql`
- tornar a llançar el fitxer sencer

### 3. El login existeix a Auth però no a l'app

Causa habitual:

- desquadrament entre `auth.users.id` i `public.tecnic.user_id`

### 4. L'usuari veu menys dades de les esperades

Causes habituals:

- no existeix fila activa a `tecnic_titular`
- no existeix compartició d'oficina
- el `scope` és `lectura`
- el tècnic està inactiu

### 5. Un manager no pot editar un titular

No és necessàriament un error. Pot passar si:

- el titular està compartit però no gestionable per la seva oficina
- el `scope` no dona escriptura

## Relació entre BDD i pantalles de l'app

| Pantalla | Taules principals |
|---|---|
| Login | `auth.users`, `public.tecnic` |
| Home de titulars | `titular`, `tecnic_titular`, `oficina_titular_compartit` |
| Perfil | `tecnic`, `oficina` |
| Modul Agricola | `titular`, `terra`, `dan_declaracio`, `aplicacions_fertilitzants` |
| Modul Ramader | `titular`, `granja`, `granja_bestiar`, `entrega_dejeccions`, `terra` |
| Preparar DAN | agregació de `titular`, `terra`, `aplicacions`, `granja`, `granja_bestiar`, `entrega` |
| Gestio Titulars | `titular`, `oficina_titular_compartit`, `oficina` |
| Gestio Terres | `terra`, `titular` |
| Gestio Tecnics | `tecnic`, `oficina`, `tecnic_titular` |
| Gestio Oficines | `oficina` |

## Si haguessis de reconstruir la BDD des de la documentacio

L'ordre conceptual correcte seria:

1. definir enums
2. definir oficines, tècnics i titulars
3. definir assignacions i comparticions
4. definir DAN per campanya
5. definir terres
6. definir aplicacions agrícoles
7. definir granges, bestiar i fases
8. definir cens per granja
9. definir balanç de nitrogen per granja i campanya
10. definir entregues cap a terres
11. sincronitzar entregues amb aplicacions quan pertoqui
12. afegir auditoria
13. afegir funcions helper
14. afegir grants i RLS

Precisament això és el que fa `agrisync_schema.sql`.

## Resum final

La BDD d'AgriSync té tres papers alhora:

- persistir dades
- modelar el domini
- fer complir la seguretat real

Si s'entenen:

- `public.tecnic`
- `public.titular`
- `tecnic_titular`
- `oficina_titular_compartit`
- `dan_declaracio`
- RLS

llavors s'entén gairebé tot el comportament del sistema.
