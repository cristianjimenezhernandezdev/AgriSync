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

## Consequencies practiques

- `admin` te visio global
- `oficina_manager` queda limitat a la seva oficina i a comparticions admeses
- un tecnic pot treballar nomes els titulars que te assignats o compartits
- `lectura` pot veure pero no editar
- les entregues cap a terres o titulars d'altres ambit passen per comprovacions de referencia i lectura

## Scripts de suport

- `docs/sql/maintenance/reaplica_permisos.sql`
  Reaplica grants i execucio de funcions.
- `docs/sql/maintenance/resincronitza_tecnic_user_ids.sql`
  Requadra `public.tecnic.user_id` amb `auth.users.id`.
- `docs/sql/maintenance/reset_auth_seed_users.sql`
  Neteja usuaris demo d'Auth abans de recrear-los.

## Criteris actuals

- no es conserva cap script SQL antic d'insercio directa a `auth.users`
- la documentacio queda alineada amb el flux actual: dashboard/Auth API per usuaris, no hacks interns sobre GoTrue
