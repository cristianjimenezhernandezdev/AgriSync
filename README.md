# AgriSync

AgriSync es una aplicacio desktop en Kotlin Multiplatform per gestionar la DAN amb autenticacio real, permisos reals a PostgreSQL/Supabase i separacio clara entre UI, capa de dades i model SQL.

Aquest `README` es nomes el punt d'entrada ràpid. La documentacio extensa viu a `docs/`.

## Documents principals

- [Index general de documentacio](docs/README.md)
- [Guia detallada de posada en marxa i troubleshooting](docs/guies/instalacio_i_demo.md)
- [Arquitectura, codi i estructura del programa](docs/arquitectura/arquitectura_i_codi.md)
- [API i integracio amb Supabase](docs/api/api_i_integracio.md)
- [Model de dades i funcionament de la BDD](docs/sql/model_de_dades_i_bdd.md)
- [Permisos i seguretat](docs/arquitectura/permisos_i_seguretat.md)

## Posada en marxa ràpida

1. Aplica `docs/sql/schema/agrisync_schema.sql` a Supabase.
2. Crea els usuaris demo a `Authentication > Users`.
3. Executa `docs/sql/seeds/agrisync_demo_seed.sql`.
4. Configura `SUPABASE_URL`, `SUPABASE_ANON_KEY` i `SUPABASE_SERVICE_ROLE_KEY`.
5. Executa `./gradlew :composeApp:run`.

## Estructura principal

- `composeApp/`
  Codi de l'aplicacio Compose Desktop.
- `docs/`
  Documentacio funcional, tecnica, API i SQL.
- `agrisync.properties.example`
  Plantilla de configuracio local.

## Estat del projecte

MVP funcional amb:

- login real amb Supabase Auth
- sessio persistent en desktop
- permisos reals amb RLS
- home de titulars
- modul agricola
- modul ramader
- pantalla `Preparar DAN`
- gestio de titulars, terres, tecnics i oficines
