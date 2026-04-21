-- =========================================================
-- agrisync_demo_seed.sql
-- Seed ampliat de demo per simular una base de dades de treball
-- Executa'l DESPRES de schema/agrisync_schema.sql al SQL Editor de Supabase
-- =========================================================
--
-- Aquest seed esta pensat per:
--   - tenir diverses oficines i rols
--   - provar titulars compartits entre tecnics de diferents oficines
--   - tenir diverses campanyes DAN amb dades agricoles i ramaderes
--   - donar una sensacio de base de dades molt mes propera a produccio
--
-- IMPORTANT:
--   Aquest script no crea usuaris a auth.users.
--   Crea primer aquests usuaris a Supabase Authentication > Users:
--
--   admin.demo@agrisync.com           / admin1234
--   manager.lleida.demo@agrisync.com  / lleida1234
--   manager.girona.demo@agrisync.com  / girona1234
--   sergi.agri.demo@agrisync.com      / sergi1234
--   marta.ram.demo@agrisync.com       / marta1234
--   laia.comu.demo@agrisync.com       / laia1234
--   nil.shared.demo@agrisync.com      / nil1234
--   joan.agri.demo@agrisync.com       / joan1234
--   anna.ram.demo@agrisync.com        / anna1234
--   lectura.demo@agrisync.com         / lectura1234
-- =========================================================

-- =========================================================
-- 0) VERIFICAR USUARIS AUTH
-- =========================================================

do $$
declare
  missing_emails text[];
begin
  select array_agg(req.email)
  into missing_emails
  from (
    values
      ('admin.demo@agrisync.com'),
      ('manager.lleida.demo@agrisync.com'),
      ('manager.girona.demo@agrisync.com'),
      ('sergi.agri.demo@agrisync.com'),
      ('marta.ram.demo@agrisync.com'),
      ('laia.comu.demo@agrisync.com'),
      ('nil.shared.demo@agrisync.com'),
      ('joan.agri.demo@agrisync.com'),
      ('anna.ram.demo@agrisync.com'),
      ('lectura.demo@agrisync.com')
  ) as req(email)
  where not exists (
    select 1
    from auth.users u
    where u.email = req.email
  );

  if missing_emails is not null then
    raise exception
      'Falten usuaris a Authentication: %. Crea''ls primer al Dashboard de Supabase.',
      array_to_string(missing_emails, ', ');
  end if;
end
$$;

-- =========================================================
-- 1) OFICINES
-- =========================================================

insert into public.oficina (id, nom)
values
  ('a0000000-0000-0000-0000-000000000001', 'Oficina Lleida'),
  ('a0000000-0000-0000-0000-000000000002', 'Oficina Girona'),
  ('a0000000-0000-0000-0000-000000000003', 'Oficina Vic')
on conflict (id) do update set
  nom = excluded.nom;

-- =========================================================
-- 2) TECNICS
-- =========================================================

insert into public.tecnic (id, oficina_id, user_id, nom, email, rol, actiu)
values
  (
    'c1000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    (select id from auth.users where email = 'admin.demo@agrisync.com'),
    'Administrador Demo',
    'admin.demo@agrisync.com',
    'admin',
    true
  ),
  (
    'c1000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    (select id from auth.users where email = 'manager.lleida.demo@agrisync.com'),
    'Marta Puig Manager',
    'manager.lleida.demo@agrisync.com',
    'oficina_manager',
    true
  ),
  (
    'c1000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000002',
    (select id from auth.users where email = 'manager.girona.demo@agrisync.com'),
    'Arnau Serra Manager',
    'manager.girona.demo@agrisync.com',
    'oficina_manager',
    true
  ),
  (
    'c1000000-0000-0000-0000-000000000004',
    'a0000000-0000-0000-0000-000000000001',
    (select id from auth.users where email = 'sergi.agri.demo@agrisync.com'),
    'Sergi Camps',
    'sergi.agri.demo@agrisync.com',
    'tecnic',
    true
  ),
  (
    'c1000000-0000-0000-0000-000000000005',
    'a0000000-0000-0000-0000-000000000001',
    (select id from auth.users where email = 'marta.ram.demo@agrisync.com'),
    'Marta Soler',
    'marta.ram.demo@agrisync.com',
    'tecnic',
    true
  ),
  (
    'c1000000-0000-0000-0000-000000000006',
    'a0000000-0000-0000-0000-000000000002',
    (select id from auth.users where email = 'laia.comu.demo@agrisync.com'),
    'Laia Roca',
    'laia.comu.demo@agrisync.com',
    'tecnic',
    true
  ),
  (
    'c1000000-0000-0000-0000-000000000007',
    'a0000000-0000-0000-0000-000000000002',
    (select id from auth.users where email = 'nil.shared.demo@agrisync.com'),
    'Nil Pujol',
    'nil.shared.demo@agrisync.com',
    'tecnic',
    true
  ),
  (
    'c1000000-0000-0000-0000-000000000008',
    'a0000000-0000-0000-0000-000000000003',
    (select id from auth.users where email = 'joan.agri.demo@agrisync.com'),
    'Joan Vives',
    'joan.agri.demo@agrisync.com',
    'tecnic',
    true
  ),
  (
    'c1000000-0000-0000-0000-000000000009',
    'a0000000-0000-0000-0000-000000000003',
    (select id from auth.users where email = 'anna.ram.demo@agrisync.com'),
    'Anna Bosch',
    'anna.ram.demo@agrisync.com',
    'tecnic',
    true
  ),
  (
    'c1000000-0000-0000-0000-000000000010',
    'a0000000-0000-0000-0000-000000000002',
    (select id from auth.users where email = 'lectura.demo@agrisync.com'),
    'Usuari Lectura Demo',
    'lectura.demo@agrisync.com',
    'lectura',
    true
  )
on conflict (id) do update set
  oficina_id = excluded.oficina_id,
  user_id = excluded.user_id,
  nom = excluded.nom,
  email = excluded.email,
  rol = excluded.rol,
  actiu = excluded.actiu;

-- =========================================================
-- 3) TITULARS
-- =========================================================

insert into public.titular (id, nif, nom_rao)
values
  ('b1000000-0000-0000-0000-000000000001', '40325245N', 'Agro Boix Jorda'),
  ('b1000000-0000-0000-0000-000000000002', '40334852M', 'Ramadera Mas Padrosa'),
  ('b1000000-0000-0000-0000-000000000003', '40303198E', 'SAT Plans de la Vall'),
  ('b1000000-0000-0000-0000-000000000004', 'B17598616', 'Cooperativa Segria i Ter'),
  ('b1000000-0000-0000-0000-000000000005', 'B17888991', 'Granges del Ter SL'),
  ('b1000000-0000-0000-0000-000000000006', '40876543K', 'Agroforestal Ponent'),
  ('b1000000-0000-0000-0000-000000000007', '40999111H', 'Explotacio Can Roca'),
  ('b1000000-0000-0000-0000-000000000008', '41000222J', 'Serveis Agraris del Pla'),
  ('b1000000-0000-0000-0000-000000000009', 'B17123456', 'Granja Les Comes'),
  ('b1000000-0000-0000-0000-000000000010', '41111333L', 'Masia Puigventos')
on conflict (id) do update set
  nif = excluded.nif,
  nom_rao = excluded.nom_rao;

-- =========================================================
-- 4) ASSIGNACIONS I TITULARS COMPARTITS
-- =========================================================

insert into public.tecnic_titular (id, tecnic_id, titular_id, scope, actiu)
values
  ('f2000000-0000-0000-0000-000000000001', 'c1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000001', 'agricola', true),
  ('f2000000-0000-0000-0000-000000000002', 'c1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000001', 'agricola', true),
  ('f2000000-0000-0000-0000-000000000003', 'c1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000001', 'lectura', true),

  ('f2000000-0000-0000-0000-000000000004', 'c1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000002', 'ramader', true),
  ('f2000000-0000-0000-0000-000000000005', 'c1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000002', 'ramader', true),
  ('f2000000-0000-0000-0000-000000000006', 'c1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000002', 'comu', true),

  ('f2000000-0000-0000-0000-000000000007', 'c1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000003', 'agricola', true),
  ('f2000000-0000-0000-0000-000000000008', 'c1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000003', 'ramader', true),
  ('f2000000-0000-0000-0000-000000000009', 'c1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003', 'comu', true),

  ('f2000000-0000-0000-0000-000000000010', 'c1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000004', 'agricola', true),
  ('f2000000-0000-0000-0000-000000000011', 'c1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000004', 'ramader', true),
  ('f2000000-0000-0000-0000-000000000012', 'c1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000004', 'comu', true),

  ('f2000000-0000-0000-0000-000000000013', 'c1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000005', 'ramader', true),
  ('f2000000-0000-0000-0000-000000000014', 'c1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000005', 'ramader', true),
  ('f2000000-0000-0000-0000-000000000015', 'c1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000005', 'lectura', true),

  ('f2000000-0000-0000-0000-000000000016', 'c1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000006', 'agricola', true),
  ('f2000000-0000-0000-0000-000000000017', 'c1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000007', 'ramader', true),

  ('f2000000-0000-0000-0000-000000000018', 'c1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000008', 'agricola', true),
  ('f2000000-0000-0000-0000-000000000019', 'c1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000008', 'agricola', true),
  ('f2000000-0000-0000-0000-000000000020', 'c1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000008', 'lectura', true),

  ('f2000000-0000-0000-0000-000000000021', 'c1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000009', 'ramader', true),
  ('f2000000-0000-0000-0000-000000000022', 'c1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000009', 'ramader', true),

  ('f2000000-0000-0000-0000-000000000023', 'c1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000010', 'agricola', true),
  ('f2000000-0000-0000-0000-000000000024', 'c1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000010', 'comu', true)
on conflict (id) do update set
  tecnic_id = excluded.tecnic_id,
  titular_id = excluded.titular_id,
  scope = excluded.scope,
  actiu = excluded.actiu;

-- =========================================================
-- 4.1) COMPARTICIO ENTRE OFICINES
-- =========================================================

insert into public.oficina_titular_compartit (id, oficina_id, titular_id, scope)
values
  ('f3000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000003', 'agricola'),
  ('f3000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000004', 'ramader'),
  ('f3000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000002', 'ramader')
on conflict (id) do update set
  oficina_id = excluded.oficina_id,
  titular_id = excluded.titular_id,
  scope = excluded.scope;

-- =========================================================
-- 5) DAN DECLARACIONS
-- =========================================================

insert into public.dan_declaracio (id, titular_id, campanya, estat)
values
  ('e1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', 2024, 'lliurada'),
  ('e1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000001', 2025, 'en_curs'),
  ('e1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000002', 2024, 'lliurada'),
  ('e1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000002', 2025, 'lliurada'),
  ('e1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000003', 2024, 'lliurada'),
  ('e1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000003', 2025, 'en_curs'),
  ('e1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000004', 2025, 'lliurada'),
  ('e1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000005', 2025, 'lliurada'),
  ('e1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000006', 2025, 'en_curs'),
  ('e1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000007', 2025, 'lliurada'),
  ('e1000000-0000-0000-0000-000000000011', 'b1000000-0000-0000-0000-000000000008', 2024, 'lliurada'),
  ('e1000000-0000-0000-0000-000000000012', 'b1000000-0000-0000-0000-000000000008', 2025, 'en_curs'),
  ('e1000000-0000-0000-0000-000000000013', 'b1000000-0000-0000-0000-000000000009', 2025, 'lliurada'),
  ('e1000000-0000-0000-0000-000000000014', 'b1000000-0000-0000-0000-000000000010', 2025, 'en_curs')
on conflict (id) do update set
  titular_id = excluded.titular_id,
  campanya = excluded.campanya,
  estat = excluded.estat;

-- =========================================================
-- 6) TERRES
-- =========================================================

insert into public.terra (id, titular_id, mun_codi, poligon, parcela, recinte, municipi_literal, us_sigpac, cultiu, superficie)
values
  ('d1000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000001', '25120', 3, 45, 1, 'Lleida', 'PA', 'Blat tou', 11.28),
  ('d1000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000001', '25120', 3, 46, 1, 'Lleida', 'FY', 'Userda', 9.40),
  ('d1000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000003', '17195', 4, 22, 1, 'Salt', 'PA', 'Blat de moro', 18.60),
  ('d1000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000003', '17195', 4, 22, 2, 'Salt', 'CV', 'Civada', 7.90),
  ('d1000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000004', '25110', 7, 13, 1, 'Alcarras', 'PA', 'Blat de moro', 22.75),
  ('d1000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000004', '25110', 7, 14, 1, 'Alcarras', 'FY', 'Festuca', 14.20),
  ('d1000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000006', '25210', 2, 88, 1, 'Agramunt', 'PA', 'Ordi', 33.50),
  ('d1000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000006', '25210', 2, 89, 1, 'Agramunt', 'FY', 'Alfals', 12.40),
  ('d1000000-0000-0000-0000-000000000009', 'b1000000-0000-0000-0000-000000000008', '17079', 5, 9, 1, 'Figueres', 'PA', 'Blat tou', 16.80),
  ('d1000000-0000-0000-0000-000000000010', 'b1000000-0000-0000-0000-000000000008', '17079', 5, 9, 2, 'Figueres', 'FY', 'Raigras', 13.10),
  ('d1000000-0000-0000-0000-000000000011', 'b1000000-0000-0000-0000-000000000010', '08215', 1, 31, 1, 'Moia', 'PA', 'Ordi', 10.25),
  ('d1000000-0000-0000-0000-000000000012', 'b1000000-0000-0000-0000-000000000010', '08215', 1, 32, 1, 'Moia', 'FY', 'Prat permanent', 8.70)
on conflict (id) do update set
  titular_id = excluded.titular_id,
  mun_codi = excluded.mun_codi,
  poligon = excluded.poligon,
  parcela = excluded.parcela,
  recinte = excluded.recinte,
  municipi_literal = excluded.municipi_literal,
  us_sigpac = excluded.us_sigpac,
  cultiu = excluded.cultiu,
  superficie = excluded.superficie;

-- =========================================================
-- 7) CATALEGS
-- =========================================================

insert into public.bestiar (id, codi, descripcio)
values
  ('30000000-0000-0000-0000-000000000001', 'BOVI', 'Bovi'),
  ('30000000-0000-0000-0000-000000000002', 'PORCI', 'Porci'),
  ('30000000-0000-0000-0000-000000000003', 'OVI', 'Ovi'),
  ('30000000-0000-0000-0000-000000000004', 'AVICOLA', 'Avicola'),
  ('30000000-0000-0000-0000-000000000005', 'CABRUM', 'Cabrum')
on conflict (id) do update set
  codi = excluded.codi,
  descripcio = excluded.descripcio;

insert into public.fase_productiva (id, codi, descripcio)
values
  ('40000000-0000-0000-0000-000000000001', 'CRIA', 'Cria'),
  ('40000000-0000-0000-0000-000000000002', 'ENGREIX', 'Engreix'),
  ('40000000-0000-0000-0000-000000000003', 'LLET', 'Produccio de llet'),
  ('40000000-0000-0000-0000-000000000004', 'REPRO', 'Reproduccio'),
  ('40000000-0000-0000-0000-000000000005', 'POSTA', 'Posta')
on conflict (id) do update set
  codi = excluded.codi,
  descripcio = excluded.descripcio;

-- =========================================================
-- 8) APLICACIONS FERTILITZANTS
-- =========================================================

insert into public.aplicacions_fertilitzants (id, dan_id, terra_id, data, tipus_fertilitzant, procedencia, volum_m3, kg_n_m3, kg_n, tecnic_id)
values
  ('11000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000001', '2024-03-12', 'Purí porcí', 'Granja propia de referencia', 26.00, 10.77, 280.00, 'c1000000-0000-0000-0000-000000000004'),
  ('11000000-0000-0000-0000-000000000002', 'e1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000002', '2024-05-10', 'Fems bovins', 'Aportacio organica interna', 18.00, 8.06, 145.00, 'c1000000-0000-0000-0000-000000000004'),
  ('11000000-0000-0000-0000-000000000003', 'e1000000-0000-0000-0000-000000000002', 'd1000000-0000-0000-0000-000000000001', '2025-02-18', 'Purí porcí', 'Granja origen declarada pel titular', 28.50, 10.88, 310.00, 'c1000000-0000-0000-0000-000000000004'),
  ('11000000-0000-0000-0000-000000000004', 'e1000000-0000-0000-0000-000000000002', 'd1000000-0000-0000-0000-000000000002', '2025-04-09', 'Adob mineral', 'Compra externa', null, null, 175.00, 'c1000000-0000-0000-0000-000000000006'),

  ('11000000-0000-0000-0000-000000000005', 'e1000000-0000-0000-0000-000000000005', 'd1000000-0000-0000-0000-000000000003', '2024-03-22', 'Purí boví', 'Recepcio de granja vinculada', 35.00, 12.00, 420.00, 'c1000000-0000-0000-0000-000000000004'),
  ('11000000-0000-0000-0000-000000000006', 'e1000000-0000-0000-0000-000000000005', 'd1000000-0000-0000-0000-000000000004', '2024-06-14', 'Adob mineral', 'Compra externa', null, null, 120.00, 'c1000000-0000-0000-0000-000000000004'),
  ('11000000-0000-0000-0000-000000000007', 'e1000000-0000-0000-0000-000000000006', 'd1000000-0000-0000-0000-000000000003', '2025-03-05', 'Purí boví', 'Recepcio declarada campanya 2025', 37.00, 12.30, 455.00, 'c1000000-0000-0000-0000-000000000006'),

  ('11000000-0000-0000-0000-000000000008', 'e1000000-0000-0000-0000-000000000007', 'd1000000-0000-0000-0000-000000000005', '2025-02-11', 'Gallinassa', 'Cooperativa propia', 22.00, 23.18, 510.00, 'c1000000-0000-0000-0000-000000000008'),
  ('11000000-0000-0000-0000-000000000009', 'e1000000-0000-0000-0000-000000000007', 'd1000000-0000-0000-0000-000000000006', '2025-04-16', 'Purí porcí', 'Recepcio interna', 24.00, 11.67, 280.00, 'c1000000-0000-0000-0000-000000000007'),

  ('11000000-0000-0000-0000-000000000010', 'e1000000-0000-0000-0000-000000000009', 'd1000000-0000-0000-0000-000000000007', '2025-03-01', 'Purí porcí', 'Granja de suport', 30.00, 11.67, 350.00, 'c1000000-0000-0000-0000-000000000008'),
  ('11000000-0000-0000-0000-000000000011', 'e1000000-0000-0000-0000-000000000009', 'd1000000-0000-0000-0000-000000000008', '2025-05-20', 'Adob mineral', 'Compra externa', null, null, 140.00, 'c1000000-0000-0000-0000-000000000008'),

  ('11000000-0000-0000-0000-000000000012', 'e1000000-0000-0000-0000-000000000011', 'd1000000-0000-0000-0000-000000000009', '2024-03-19', 'Purí porcí', 'Titular receptor habitual', 20.00, 11.50, 230.00, 'c1000000-0000-0000-0000-000000000004'),
  ('11000000-0000-0000-0000-000000000013', 'e1000000-0000-0000-0000-000000000011', 'd1000000-0000-0000-0000-000000000010', '2024-06-02', 'Adob mineral', 'Compra externa', null, null, 180.00, 'c1000000-0000-0000-0000-000000000006'),
  ('11000000-0000-0000-0000-000000000014', 'e1000000-0000-0000-0000-000000000012', 'd1000000-0000-0000-0000-000000000009', '2025-02-27', 'Purí porcí', 'Recepcio des de granja propera', 21.00, 11.67, 245.00, 'c1000000-0000-0000-0000-000000000006'),
  ('11000000-0000-0000-0000-000000000015', 'e1000000-0000-0000-0000-000000000012', 'd1000000-0000-0000-0000-000000000010', '2025-04-24', 'Adob mineral', 'Compra externa', null, null, 195.00, 'c1000000-0000-0000-0000-000000000004'),

  ('11000000-0000-0000-0000-000000000016', 'e1000000-0000-0000-0000-000000000014', 'd1000000-0000-0000-0000-000000000011', '2025-03-03', 'Fems bovins', 'Ramaderia propia', 16.00, 10.00, 160.00, 'c1000000-0000-0000-0000-000000000008'),
  ('11000000-0000-0000-0000-000000000017', 'e1000000-0000-0000-0000-000000000014', 'd1000000-0000-0000-0000-000000000012', '2025-05-06', 'Adob mineral', 'Compra externa', null, null, 145.00, 'c1000000-0000-0000-0000-000000000007')
on conflict (id) do update set
  dan_id = excluded.dan_id,
  terra_id = excluded.terra_id,
  data = excluded.data,
  tipus_fertilitzant = excluded.tipus_fertilitzant,
  procedencia = excluded.procedencia,
  volum_m3 = excluded.volum_m3,
  kg_n_m3 = excluded.kg_n_m3,
  kg_n = excluded.kg_n,
  tecnic_id = excluded.tecnic_id;

-- =========================================================
-- 9) GRANJES
-- =========================================================

insert into public.granja (id, titular_id, marca_oficial, nom)
values
  ('21000000-0000-0000-0000-000000000001', 'b1000000-0000-0000-0000-000000000002', '2500AX', 'Mas Padrosa 1'),
  ('21000000-0000-0000-0000-000000000002', 'b1000000-0000-0000-0000-000000000003', '1700BV', 'Plans de la Vall'),
  ('21000000-0000-0000-0000-000000000003', 'b1000000-0000-0000-0000-000000000004', '2500CX', 'Cooperativa Ter Nord'),
  ('21000000-0000-0000-0000-000000000004', 'b1000000-0000-0000-0000-000000000005', '1700DX', 'Granges del Ter A'),
  ('21000000-0000-0000-0000-000000000005', 'b1000000-0000-0000-0000-000000000005', '1700DY', 'Granges del Ter B'),
  ('21000000-0000-0000-0000-000000000006', 'b1000000-0000-0000-0000-000000000007', '0800EX', 'Can Roca'),
  ('21000000-0000-0000-0000-000000000007', 'b1000000-0000-0000-0000-000000000009', '1700FX', 'Les Comes'),
  ('21000000-0000-0000-0000-000000000008', 'b1000000-0000-0000-0000-000000000004', '2500CZ', 'Cooperativa Ter Sud')
on conflict (id) do update set
  titular_id = excluded.titular_id,
  marca_oficial = excluded.marca_oficial,
  nom = excluded.nom;

-- =========================================================
-- 10) GRANJA_BESTIAR
-- =========================================================

insert into public.granja_bestiar (id, granja_id, bestiar_id, fase_productiva_id, cens)
values
  ('51000000-0000-0000-0000-000000000001', '21000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 820),
  ('51000000-0000-0000-0000-000000000002', '21000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000004', 140),
  ('51000000-0000-0000-0000-000000000003', '21000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000003', 110),
  ('51000000-0000-0000-0000-000000000004', '21000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000005', 12000),
  ('51000000-0000-0000-0000-000000000005', '21000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000003', 95),
  ('51000000-0000-0000-0000-000000000006', '21000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', 240),
  ('51000000-0000-0000-0000-000000000007', '21000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000004', 180),
  ('51000000-0000-0000-0000-000000000008', '21000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 430),
  ('51000000-0000-0000-0000-000000000009', '21000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000005', 9800)
on conflict (id) do update set
  granja_id = excluded.granja_id,
  bestiar_id = excluded.bestiar_id,
  fase_productiva_id = excluded.fase_productiva_id,
  cens = excluded.cens;

-- =========================================================
-- 11) ENTREGUES DEJECCIONS
-- =========================================================

insert into public.entrega_dejeccions (id, dan_id, granja_origen_id, data, quantitat, terra_desti_id, receptor_titular_id)
values
  ('71000000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000003', '21000000-0000-0000-0000-000000000001', '2024-04-08', 250.00, 'd1000000-0000-0000-0000-000000000001', null),
  ('71000000-0000-0000-0000-000000000002', 'e1000000-0000-0000-0000-000000000004', '21000000-0000-0000-0000-000000000001', '2025-03-21', 310.00, 'd1000000-0000-0000-0000-000000000003', null),
  ('71000000-0000-0000-0000-000000000003', 'e1000000-0000-0000-0000-000000000006', '21000000-0000-0000-0000-000000000002', '2025-05-18', 180.00, null, 'b1000000-0000-0000-0000-000000000008'),
  ('71000000-0000-0000-0000-000000000004', 'e1000000-0000-0000-0000-000000000007', '21000000-0000-0000-0000-000000000003', '2025-02-26', 420.00, 'd1000000-0000-0000-0000-000000000005', null),
  ('71000000-0000-0000-0000-000000000005', 'e1000000-0000-0000-0000-000000000008', '21000000-0000-0000-0000-000000000004', '2025-04-14', 275.00, null, 'b1000000-0000-0000-0000-000000000010'),
  ('71000000-0000-0000-0000-000000000006', 'e1000000-0000-0000-0000-000000000008', '21000000-0000-0000-0000-000000000005', '2025-06-03', 165.00, null, 'b1000000-0000-0000-0000-000000000006'),
  ('71000000-0000-0000-0000-000000000007', 'e1000000-0000-0000-0000-000000000010', '21000000-0000-0000-0000-000000000006', '2025-03-30', 145.00, null, 'b1000000-0000-0000-0000-000000000004'),
  ('71000000-0000-0000-0000-000000000008', 'e1000000-0000-0000-0000-000000000013', '21000000-0000-0000-0000-000000000007', '2025-04-27', 290.00, 'd1000000-0000-0000-0000-000000000009', null),
  ('71000000-0000-0000-0000-000000000009', 'e1000000-0000-0000-0000-000000000007', '21000000-0000-0000-0000-000000000008', '2025-05-09', 360.00, 'd1000000-0000-0000-0000-000000000006', null)
on conflict (id) do update set
  dan_id = excluded.dan_id,
  granja_origen_id = excluded.granja_origen_id,
  data = excluded.data,
  quantitat = excluded.quantitat,
  terra_desti_id = excluded.terra_desti_id,
  receptor_titular_id = excluded.receptor_titular_id;

-- =========================================================
-- 12) VERIFICACIONS DE DEMO
-- =========================================================

select 'oficina' as taula, count(*) as registres from public.oficina
union all select 'tecnic', count(*) from public.tecnic
union all select 'titular', count(*) from public.titular
union all select 'tecnic_titular', count(*) from public.tecnic_titular
union all select 'dan_declaracio', count(*) from public.dan_declaracio
union all select 'terra', count(*) from public.terra
union all select 'aplicacions_fertilitzants', count(*) from public.aplicacions_fertilitzants
union all select 'granja', count(*) from public.granja
union all select 'bestiar', count(*) from public.bestiar
union all select 'fase_productiva', count(*) from public.fase_productiva
union all select 'granja_bestiar', count(*) from public.granja_bestiar
union all select 'entrega_dejeccions', count(*) from public.entrega_dejeccions
order by taula;

select
  ti.nom_rao as titular,
  count(distinct tt.tecnic_id) as tecnics_assignats,
  string_agg(distinct o.nom, ', ' order by o.nom) as oficines_implicades
from public.tecnic_titular tt
join public.tecnic t on t.id = tt.tecnic_id
join public.oficina o on o.id = t.oficina_id
join public.titular ti on ti.id = tt.titular_id
where tt.actiu = true
group by ti.nom_rao
having count(distinct t.oficina_id) > 1
order by ti.nom_rao;

select
  t.nom,
  t.email,
  t.rol,
  o.nom as oficina,
  count(tt.id) as assignacions_actives
from public.tecnic t
join public.oficina o on o.id = t.oficina_id
left join public.tecnic_titular tt on tt.tecnic_id = t.id and tt.actiu = true
group by t.nom, t.email, t.rol, o.nom
order by o.nom, t.nom;
