# Estat actual i roadmap

## Estat actual

AgriSync esta en un punt de MVP funcional i demostrable. El nucli del projecte es coherent tant a nivell d'app com de model SQL.

## El que ja queda tancat

- login real amb Supabase Auth
- sessio persistent en desktop
- model de dades centrat en titular, terres, granges i campanyes
- titular amb dades de contacte estructurades: `telefon`, `email`, `adreca` i `codi_postal`
- permisos reals amb RLS
- gestio d'oficines, tecnics, titulars i terres
- modul agricola i modul ramader operatius
- pantalla `Preparar DAN` amb resum i checklist
- comparticio de titulars entre oficines per `scope`
- seed de demo completa revisada

## Revisio del seed

El fitxer actual `docs/sql/seeds/agrisync_demo_seed.sql` no sembla una seed incompleta. Fa la feina esperada per a demo i proves funcionals:

- recrea els usuaris Auth demo
- carrega dades representatives
- cobreix entitats principals del model
- deixa consultes de comprovacio al final

La principal incidència detectada no era el seed, sino la documentacio: hi havia noms i referencies de fitxers antics que ja no coincidien amb el repositori real.

## Limits actuals del MVP

- no hi ha exportacio a PDF oficial
- no hi ha calcul normatiu complet del nitrogen generat per bestiar
- la tracabilitat entre entrega concreta i aplicacio concreta continua simplificada
- hi ha funcionalitats administratives que depenen de `service_role`

## Roadmap curt

1. Automatitzar proves de repositoris i viewmodels clau.
2. Afegir exportacio o plantilla formal de resum DAN.
3. Refinar calculs normatius i validacions de domini.
4. Revisar si algunes operacions administratives es poden desacoblar del `service_role`.

Per a una llista mes detallada de limits detectats i possibles evolucions, consulta `docs/projecte/millores_post_mvp.md`.
