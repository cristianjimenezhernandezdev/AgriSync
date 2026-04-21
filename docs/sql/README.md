# Paquet SQL d'AgriSync

## Objectiu

Aquest directori agrupa tot el que afecta la base de dades:

- esquema
- seed
- manteniment
- documentacio detallada del model i de la seguretat

## Documents i scripts principals

- [Model de dades i funcionament detallat de la BDD](model_de_dades_i_bdd.md)
- `schema/agrisync_schema.sql`
- `seeds/agrisync_demo_seed.sql`
- `maintenance/reaplica_permisos.sql`
- `maintenance/resincronitza_tecnic_user_ids.sql`
- `maintenance/reset_auth_seed_users.sql`

## Què fa cada fitxer

### `schema/agrisync_schema.sql`

Reconstrueix la base de dades del projecte:

- enums
- taules
- indexos
- triggers d'auditoria
- funcions helper
- grants
- RLS
- policies

### `seeds/agrisync_demo_seed.sql`

Carrega una demo funcional amb:

- oficines
- tecnics
- titulars
- assignacions
- comparticions
- campanyes DAN
- terres
- aplicacions
- granges
- bestiar
- fases
- entregues

### `maintenance/reaplica_permisos.sql`

Reaplica permisos i execucio de funcions. Es útil quan s'ha tocat el `schema` o algun entorn ha quedat a mig ajustar.

### `maintenance/resincronitza_tecnic_user_ids.sql`

Torna a quadrar `public.tecnic.user_id` amb `auth.users.id`.

### `maintenance/reset_auth_seed_users.sql`

Neteja usuaris demo coneguts d'Auth abans de recrear-los.

## Ordre recomanat d'execucio

1. `schema/agrisync_schema.sql`
2. crear usuaris demo a Supabase Auth
3. `seeds/agrisync_demo_seed.sql`
4. manteniment només si cal

## Recomanacio operativa

Si el `schema` falla a mig cami, no segueixis amb trossos solts. El millor es:

1. corregir la causa
2. tornar a executar el fitxer sencer

El motiu es que hi ha dependències entre funcions, triggers, grants i policies.
