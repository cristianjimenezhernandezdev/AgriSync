# Paquet SQL d'AgriSync

## Objectiu

Aquest directori agrupa tot el que afecta la base de dades:

- esquema
- seed
- manteniment
- documentacio detallada del model i de la seguretat

## Documents i scripts principals

- [Model de dades i funcionament detallat de la BDD](model_de_dades_i_bdd.md)
- `maintenance/reset_auth_seed_users.sql`
- `schema/agrisync_schema.sql`
- `seeds/agrisync_demo_seed.sql`

## Què fa cada fitxer

### `schema/agrisync_schema.sql`

Reconstrueix la base de dades del projecte:

- enums
- taules
- indexos
- triggers d'auditoria
- funcions helper
- RPCs de domini per a creacions sensibles
- grants
- RLS
- policies

També deixa integrats dins l'esquema base els camps:

- `entrega_dejeccions.volum_m3`
- `entrega_dejeccions.kg_n_m3`
- `entrega_dejeccions.kg_n`
- `aplicacions_fertilitzants.volum_m3`
- `aplicacions_fertilitzants.kg_n_m3`
- `aplicacions_fertilitzants.kg_n`

I exposa operacions controlades per evitar errors de RLS en altes habituals:

- `create_titular(...)`
- `create_terra(...)`

### `seeds/agrisync_demo_seed.sql`

Carrega una demo funcional completa amb:

- usuaris Auth
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

El seed actual:

- es pot reexecutar sobre la mateixa BDD de demo
- recrea els usuaris demo a `auth.users`
- neteja les dades demo conegudes abans de recarregar-les
- inclou zones `ZV` i `ZNV`
- deixa dades manuals i dades sincronitzades des de `entrega_dejeccions`

### `maintenance/reset_auth_seed_users.sql`

Neteja completament Supabase Auth del projecte actual abans de recrear usuaris.

## Ordre recomanat d'execucio

1. `maintenance/reset_auth_seed_users.sql`
2. `schema/agrisync_schema.sql`
3. `seeds/agrisync_demo_seed.sql`

## Recomanacio operativa

Si el `schema` falla a mig cami, no segueixis amb trossos solts. El millor es:

1. corregir la causa
2. tornar a executar el fitxer sencer

El motiu es que hi ha dependències entre funcions, triggers, grants i policies.
