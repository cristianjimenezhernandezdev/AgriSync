# Permisos i seguretat

## Objectiu

AgriSync delega la seguretat real a Supabase/PostgreSQL. La UI ajuda, pero la decisio final sobre lectura i escriptura la pren la base de dades.

Aquest document es un resum especialitzat. Per al detall complet del model i de la BDD, consulta:

- `docs/sql/model_de_dades_i_bdd.md`
- `docs/arquitectura/arquitectura_i_codi.md`

## Peces del model

- `auth.users`
  Identitat autenticada.
- `public.tecnic`
  Perfil funcional actiu dins de l'aplicacio.
- `public.tecnic_titular`
  Assignacio directa entre tecnic i titular amb `scope`.
- `public.oficina_titular_compartit`
  Comparticio interoficina d'un titular per ambit.
- policies RLS de `docs/sql/schema/agrisync_schema.sql`

## Rols globals

- `admin`
- `oficina_manager`
- `tecnic`
- `lectura`

## Scopes per titular

- `agricola`
- `ramader`
- `comu`
- `lectura`

El rol global no basta per operar. Cal que l'usuari tingui accions compatibles amb el `scope` del titular.

## Funcions helper clau

L'esquema defineix, entre d'altres:

- `get_my_tecnic()`
- `current_oficina_id()`
- `is_admin()`
- `is_oficina_manager()`
- `can_manage_office_titular(...)`
- `can_read_titular(...)`
- `can_write_scope(...)`
- `can_write_agricola(...)`
- `can_write_ramader(...)`
- `can_reference_terra(...)`
- `create_titular(...)`
- `create_terra(...)`

## Consequencies practiques

- `admin` te visio global
- `oficina_manager` queda limitat a la seva oficina i a comparticions admeses
- un tecnic pot treballar nomes els titulars que te assignats o compartits
- `lectura` pot veure pero no editar
- les entregues cap a terres o titulars d'altres ambit passen per comprovacions de referencia i lectura
- les altes de titulars i terres passen per RPCs de domini per conservar permisos i auditoria
- la comparticio amb una oficina no visible es resol introduint l'email del manager receptor; l'app nomes usa aquesta cerca per trobar l'oficina i despres crea la comparticio amb les policies normals

## Scripts de suport

- `docs/sql/maintenance/reset_auth_seed_users.sql`
  Buida completament Auth abans de reconstruir la demo.

## Criteris actuals

- el flux SQL actual si que crea usuaris demo a `auth.users` per deixar una reconstruccio completa en tres passos
- la documentacio queda alineada amb el flux actual: `reset -> schema -> seed`
