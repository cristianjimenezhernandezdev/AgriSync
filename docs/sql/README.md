# Scripts SQL

## Estructura

- `schema/agrisync_schema.sql`
  Esquema principal: enums, taules, auditoria, funcions helper, grants i RLS.
  La taula `titular` inclou `nif`, `telefon`, `email`, `adreca` i `codi_postal`, amb indexos per facilitar cerques futures.
- `seeds/agrisync_demo_seed.sql`
  Seed principal de demo i proves, amb dades de contacte completes als titulars.
- `maintenance/reaplica_permisos.sql`
  Reaplica grants i execucions.
- `maintenance/resincronitza_tecnic_user_ids.sql`
  Requadra `public.tecnic.user_id` amb `auth.users`.
- `maintenance/reset_auth_seed_users.sql`
  Elimina usuaris demo coneguts d'Auth abans de recrear-los.

## Ordre d'execucio habitual

1. `schema/agrisync_schema.sql`
2. crear usuaris demo a Supabase Auth
3. `seeds/agrisync_demo_seed.sql`

## Resultat de la revisio

Despres de revisar el seed principal:

- no esta a mitges
- cobreix totes les taules operatives del MVP
- porta verificacions finals utiles
- es suficient com a base de demo

## Scripts eliminats

S'han retirat els scripts antics de creacio manual d'usuaris Auth via SQL per dos motius:

- duplicaven funcionalitat
- depenen massa de l'estructura interna de Supabase Auth i es poden trencar
