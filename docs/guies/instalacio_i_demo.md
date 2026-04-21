# Guia d'installacio i demo

## Objectiu

Aquesta guia serveix per reconstruir AgriSync des de zero en un projecte Supabase i provar la demo completa del MVP.

## Requisits

- un projecte Supabase amb acces a `SQL Editor` i `Authentication > Users`
- JDK 21
- Gradle Wrapper del repositori
- credencials `SUPABASE_URL`, `SUPABASE_ANON_KEY` i `SUPABASE_SERVICE_ROLE_KEY`

## Ordre recomanat de SQL

1. `docs/sql/schema/agrisync_schema.sql`
2. crear els usuaris demo a `Authentication > Users`
3. `docs/sql/seeds/agrisync_demo_seed.sql`
4. si cal, `docs/sql/maintenance/resincronitza_tecnic_user_ids.sql`
5. si cal, `docs/sql/maintenance/reaplica_permisos.sql`

## Usuaris demo recomanats

Crea aquests usuaris manualment a Supabase Auth:

- `admin.demo@agrisync.com` / `admin1234`
- `manager.lleida.demo@agrisync.com` / `lleida1234`
- `manager.girona.demo@agrisync.com` / `girona1234`
- `sergi.agri.demo@agrisync.com` / `sergi1234`
- `marta.ram.demo@agrisync.com` / `marta1234`
- `laia.comu.demo@agrisync.com` / `laia1234`
- `nil.shared.demo@agrisync.com` / `nil1234`
- `joan.agri.demo@agrisync.com` / `joan1234`
- `anna.ram.demo@agrisync.com` / `anna1234`
- `lectura.demo@agrisync.com` / `lectura1234`

## Estat del seed

El seed principal no esta a mitges. Despres de revisar-lo, cobreix totes les taules operatives del MVP:

- oficines
- tecnics
- titulars i assignacions
- comparticions entre oficines
- declaracions DAN 2024 i 2025
- terres
- aplicacions fertilitzants
- granges, bestiar i fases productives
- entregues de dejeccions

També incorpora consultes finals de verificacio per comprovar volum de dades i titulars compartits.

## Configuracio de l'app

Opcio A. Variables d'entorn:

```powershell
$env:SUPABASE_URL="https://<project>.supabase.co"
$env:SUPABASE_ANON_KEY="<anon_key>"
$env:SUPABASE_SERVICE_ROLE_KEY="<service_role_key>"
```

Opcio B. Fitxer `agrisync.properties` al directori d'execucio:

```properties
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=<anon_key>
SUPABASE_SERVICE_ROLE_KEY=<service_role_key>
```

## Execucio en desenvolupament

```powershell
./gradlew :composeApp:run
```

## Com provar la demo

1. Entra com a `admin` per validar pantalles de gestio.
2. Entra com a `oficina_manager` per validar abast per oficina.
3. Entra com a tecnics agricola, ramader i lectura per validar scopes.
4. Revisa `Preparar DAN` en campanyes 2024 i 2025.
5. Comprova titulars compartits i entregues entre titulars/oficines.

## Incidencies habituals

- Si un usuari existeix a Auth pero no entra a l'app, executa `docs/sql/maintenance/resincronitza_tecnic_user_ids.sql`.
- Si algun permís queda desalineat, executa `docs/sql/maintenance/reaplica_permisos.sql`.
- Si vols recrear els usuaris demo, primer executa `docs/sql/maintenance/reset_auth_seed_users.sql`.

## Criteri actual

Ja no es recomana inserir manualment usuaris a `auth.users` des d'SQL. El projecte actual treballa millor creant usuaris des del dashboard o via Admin API.
