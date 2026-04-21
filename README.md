# AgriSync

AgriSync es una aplicacio desktop en Kotlin Multiplatform per centralitzar la gestio de la DAN amb Supabase, autenticacio real i permisos RLS.

La documentacio del projecte viu dins de `docs/`. El `README` de l'arrel queda com a punt d'entrada rapid del repositori.

## Documentacio

- [Index de documentacio](docs/README.md)
- [Guia d'instalacio i demo](docs/guies/instalacio_i_demo.md)
- [Arquitectura i codi](docs/arquitectura/arquitectura_i_codi.md)
- [Flux operatiu i moduls](docs/funcional/flux_operatiu_i_moduls.md)
- [Permisos i seguretat](docs/arquitectura/permisos_i_seguretat.md)
- [Estat actual i roadmap](docs/projecte/estat_actual_i_roadmap.md)
- [Guia de defensa](docs/presentacio/guia_defensa.md)
- [Scripts SQL](docs/sql/README.md)

## Posada en marxa rapida

1. Aplica l'esquema a Supabase amb `docs/sql/schema/agrisync_schema.sql`.
2. Crea els usuaris demo des de `Authentication > Users`.
3. Executa `docs/sql/seeds/agrisync_demo_seed.sql`.
4. Configura `SUPABASE_URL`, `SUPABASE_ANON_KEY` i `SUPABASE_SERVICE_ROLE_KEY` o crea `agrisync.properties`.
5. Executa `./gradlew :composeApp:run`.

## Estructura principal

- `composeApp/`: aplicacio Compose Desktop
- `docs/`: documentacio i scripts SQL reorganitzats
- `agrisync.properties.example`: plantilla de configuracio

## Estat

MVP funcional i demostrable amb:

- login real amb Supabase Auth
- permisos RLS a nivell de base de dades
- gestio de titulars, terres, tecnics i oficines
- modul agricola i modul ramader per campanya
- pantalla `Preparar DAN` amb resum i checklist
