-- =============================================================================
-- MIGRACIÓ: Afegir columnes de volum/nitrogen a entrega_dejeccions
-- =============================================================================
-- Error detectat: column entrega_dejeccions.volum_m3 does not exist (codi 42703)
-- Causa: la BDD fou creada amb una versió anterior de l'esquema que no incloïa
--        les columnes volum_m3, kg_n_m3, kg_n a la taula entrega_dejeccions.
-- Solució:
--   - instal·lacio nova o reconstruccio completa: executar directament
--     docs/sql/schema/agrisync_schema.sql, que ja integra aquests camps.
--   - base de dades antiga en produccio: executar aquest script puntualment.
-- =============================================================================

alter table public.entrega_dejeccions
    add column if not exists volum_m3 numeric check (volum_m3 is null or volum_m3 >= 0),
    add column if not exists kg_n_m3  numeric check (kg_n_m3  is null or kg_n_m3  >= 0),
    add column if not exists kg_n     numeric check (kg_n     is null or kg_n     >= 0);

-- Opcional: afegir també a aplicacions_fertilitzants si tampoc existeixen
alter table public.aplicacions_fertilitzants
    add column if not exists volum_m3 numeric check (volum_m3 is null or volum_m3 >= 0),
    add column if not exists kg_n_m3  numeric check (kg_n_m3  is null or kg_n_m3  >= 0),
    add column if not exists kg_n     numeric check (kg_n     is null or kg_n     >= 0);

-- Verificació: comprova que les columnes existeixen
select column_name, data_type
from information_schema.columns
where table_schema = 'public'
  and table_name in ('entrega_dejeccions', 'aplicacions_fertilitzants')
  and column_name in ('volum_m3', 'kg_n_m3', 'kg_n')
order by table_name, column_name;

