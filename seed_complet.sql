-- =========================================================
-- seed_complet.sql
-- Seed complet de proves per a l'esquema MVP actual d'AgriSync
-- Executa'l DESPRES de SQLAgriSync.sql al SQL Editor de Supabase
-- =========================================================
--
-- Credencials de prova creades per aquest script:
--
--   admin@agrisync.com    / admin1234
--   manager@agrisync.com  / manager1234
--   agricola@agrisync.com / agricola1234
--   ramader@agrisync.com  / ramader1234
--   lectura@agrisync.com  / lectura1234
--
-- Recomanacio:
--   1) Executa primer SQLAgriSync.sql
--   2) Executa aquest seed
--   3) Fes login amb admin@agrisync.com / admin1234
--
-- Aquest seed omple totes les taules actives del MVP:
--   oficina
--   tecnic
--   titular
--   tecnic_titular
--   dan_declaracio
--   terra
--   aplicacions_fertilitzants
--   granja
--   bestiar
--   fase_productiva
--   granja_bestiar
--   entrega_dejeccions
-- =========================================================

create extension if not exists pgcrypto;

-- =========================================================
-- 0) NETEJA DELS USUARIS AUTH DE PROVA SI JA EXISTEIXEN
-- =========================================================

delete from auth.identities
where user_id in (
  '90000000-0000-0000-0000-000000000001',
  '90000000-0000-0000-0000-000000000002',
  '90000000-0000-0000-0000-000000000003',
  '90000000-0000-0000-0000-000000000004',
  '90000000-0000-0000-0000-000000000005'
);

delete from auth.users
where id in (
  '90000000-0000-0000-0000-000000000001',
  '90000000-0000-0000-0000-000000000002',
  '90000000-0000-0000-0000-000000000003',
  '90000000-0000-0000-0000-000000000004',
  '90000000-0000-0000-0000-000000000005'
)
or email in (
  'admin@agrisync.com',
  'manager@agrisync.com',
  'agricola@agrisync.com',
  'ramader@agrisync.com',
  'lectura@agrisync.com'
);

-- =========================================================
-- 1) CREACIO D'USUARIS AUTH
-- =========================================================

insert into auth.users (
  instance_id,
  id,
  aud,
  role,
  email,
  encrypted_password,
  email_confirmed_at,
  raw_app_meta_data,
  raw_user_meta_data,
  created_at,
  updated_at,
  confirmation_token,
  recovery_token
)
values
(
  '00000000-0000-0000-0000-000000000000',
  '90000000-0000-0000-0000-000000000001',
  'authenticated',
  'authenticated',
  'admin@agrisync.com',
  crypt('admin1234', gen_salt('bf')),
  now(),
  '{"provider":"email","providers":["email"]}',
  '{"nom":"Admin AgriSync"}',
  now(),
  now(),
  '',
  ''
),
(
  '00000000-0000-0000-0000-000000000000',
  '90000000-0000-0000-0000-000000000002',
  'authenticated',
  'authenticated',
  'manager@agrisync.com',
  crypt('manager1234', gen_salt('bf')),
  now(),
  '{"provider":"email","providers":["email"]}',
  '{"nom":"Manager AgriSync"}',
  now(),
  now(),
  '',
  ''
),
(
  '00000000-0000-0000-0000-000000000000',
  '90000000-0000-0000-0000-000000000003',
  'authenticated',
  'authenticated',
  'agricola@agrisync.com',
  crypt('agricola1234', gen_salt('bf')),
  now(),
  '{"provider":"email","providers":["email"]}',
  '{"nom":"Tecnic Agricola"}',
  now(),
  now(),
  '',
  ''
),
(
  '00000000-0000-0000-0000-000000000000',
  '90000000-0000-0000-0000-000000000004',
  'authenticated',
  'authenticated',
  'ramader@agrisync.com',
  crypt('ramader1234', gen_salt('bf')),
  now(),
  '{"provider":"email","providers":["email"]}',
  '{"nom":"Tecnic Ramader"}',
  now(),
  now(),
  '',
  ''
),
(
  '00000000-0000-0000-0000-000000000000',
  '90000000-0000-0000-0000-000000000005',
  'authenticated',
  'authenticated',
  'lectura@agrisync.com',
  crypt('lectura1234', gen_salt('bf')),
  now(),
  '{"provider":"email","providers":["email"]}',
  '{"nom":"Usuari Lectura"}',
  now(),
  now(),
  '',
  ''
);

insert into auth.identities (
  id,
  user_id,
  provider_id,
  identity_data,
  provider,
  last_sign_in_at,
  created_at,
  updated_at
)
select
  u.id,
  u.id,
  u.email,
  jsonb_build_object('sub', u.id::text, 'email', u.email),
  'email',
  now(),
  now(),
  now()
from auth.users u
where u.id in (
  '90000000-0000-0000-0000-000000000001',
  '90000000-0000-0000-0000-000000000002',
  '90000000-0000-0000-0000-000000000003',
  '90000000-0000-0000-0000-000000000004',
  '90000000-0000-0000-0000-000000000005'
);

-- =========================================================
-- 2) OFICINES
-- =========================================================

insert into public.oficina (id, nom)
values
  ('a0000000-0000-0000-0000-000000000001', 'Oficina Lleida'),
  ('a0000000-0000-0000-0000-000000000002', 'Oficina Girona')
on conflict (id) do update set
  nom = excluded.nom;

-- =========================================================
-- 3) TECNICS
-- =========================================================

insert into public.tecnic (id, oficina_id, user_id, nom, email, rol, actiu)
values
  (
    'c0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    '90000000-0000-0000-0000-000000000001',
    'Administrador',
    'admin@agrisync.com',
    'admin',
    true
  ),
  (
    'c0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    '90000000-0000-0000-0000-000000000002',
    'Gestora Oficina',
    'manager@agrisync.com',
    'oficina_manager',
    true
  ),
  (
    'c0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    '90000000-0000-0000-0000-000000000003',
    'Tecnic Agricola',
    'agricola@agrisync.com',
    'tecnic',
    true
  ),
  (
    'c0000000-0000-0000-0000-000000000004',
    'a0000000-0000-0000-0000-000000000002',
    '90000000-0000-0000-0000-000000000004',
    'Tecnic Ramader',
    'ramader@agrisync.com',
    'tecnic',
    true
  ),
  (
    'c0000000-0000-0000-0000-000000000005',
    'a0000000-0000-0000-0000-000000000002',
    '90000000-0000-0000-0000-000000000005',
    'Usuari Lectura',
    'lectura@agrisync.com',
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
-- 4) TITULARS
-- =========================================================

insert into public.titular (id, nif, nom_rao)
values
  ('b0000000-0000-0000-0000-000000000001', '40325245N', 'Jordi Boix Jorda'),
  ('b0000000-0000-0000-0000-000000000002', '40334852M', 'Miquel Padrosa Trias'),
  ('b0000000-0000-0000-0000-000000000003', '40303198E', 'Joan Gifra'),
  ('b0000000-0000-0000-0000-000000000004', '11223344C', 'Cooperativa La Vall'),
  ('b0000000-0000-0000-0000-000000000005', '55667788D', 'Granja Mas Roig SL'),
  ('b0000000-0000-0000-0000-000000000006', '99887766E', 'SAT La Plana')
on conflict (id) do update set
  nif = excluded.nif,
  nom_rao = excluded.nom_rao;

-- =========================================================
-- 5) ASSIGNACIONS TECNIC_TITULAR
-- =========================================================

insert into public.tecnic_titular (id, tecnic_id, titular_id, scope, actiu)
values
  ('f1000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000001', 'agricola', true),
  ('f1000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002', 'agricola', true),
  ('f1000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'comu', true),
  ('f1000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', 'ramader', true),
  ('f1000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000005', 'ramader', true),
  ('f1000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000006', 'comu', true),
  ('f1000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000001', 'lectura', true)
on conflict (id) do update set
  tecnic_id = excluded.tecnic_id,
  titular_id = excluded.titular_id,
  scope = excluded.scope,
  actiu = excluded.actiu;

-- =========================================================
-- 6) DAN
-- =========================================================

insert into public.dan_declaracio (id, titular_id, campanya, estat)
values
  ('e0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 2025, 'en_curs'),
  ('e0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 2025, 'en_curs'),
  ('e0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 2025, 'en_curs'),
  ('e0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', 2025, 'en_curs'),
  ('e0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000005', 2025, 'en_curs'),
  ('e0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006', 2025, 'en_curs'),
  ('e0000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000001', 2024, 'tancat')
on conflict (id) do update set
  titular_id = excluded.titular_id,
  campanya = excluded.campanya,
  estat = excluded.estat;

-- =========================================================
-- 7) TERRES
-- =========================================================

insert into public.terra (id, titular_id, mun_codi, poligon, parcela, recinte, superficie)
values
  ('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', '25120', 3, 45, 1, 11.28),
  ('d0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', '25120', 3, 46, 1, 36.53),
  ('d0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', '25120', 3, 47, 1, 12.40),
  ('d0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000003', '25120', 3, 47, 2, 9.80),
  ('d0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000006', '17071', 2, 8, 1, 30.00),
  ('d0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000006', '17071', 2, 8, 2, 15.20)
on conflict (id) do update set
  titular_id = excluded.titular_id,
  mun_codi = excluded.mun_codi,
  poligon = excluded.poligon,
  parcela = excluded.parcela,
  recinte = excluded.recinte,
  superficie = excluded.superficie;

-- =========================================================
-- 8) APLICACIONS FERTILITZANTS
-- =========================================================

insert into public.aplicacions_fertilitzants (id, dan_id, terra_id, data, kg_n, uf, tecnic_id)
values
  ('10000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', '2025-03-15', 385.00, 70.00, 'c0000000-0000-0000-0000-000000000003'),
  ('10000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000002', '2025-03-20', 453.75, 82.50, 'c0000000-0000-0000-0000-000000000003'),
  ('10000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000003', '2025-04-01', 440.00, 80.00, 'c0000000-0000-0000-0000-000000000003'),
  ('10000000-0000-0000-0000-000000000004', 'e0000000-0000-0000-0000-000000000006', 'd0000000-0000-0000-0000-000000000005', '2025-02-28', 150.00, 55.00, 'c0000000-0000-0000-0000-000000000004'),
  ('10000000-0000-0000-0000-000000000005', 'e0000000-0000-0000-0000-000000000007', 'd0000000-0000-0000-0000-000000000001', '2024-06-01', 95.00, 40.00, 'c0000000-0000-0000-0000-000000000001')
on conflict (id) do update set
  dan_id = excluded.dan_id,
  terra_id = excluded.terra_id,
  data = excluded.data,
  kg_n = excluded.kg_n,
  uf = excluded.uf,
  tecnic_id = excluded.tecnic_id;

-- =========================================================
-- 9) GRANJES
-- =========================================================

insert into public.granja (id, titular_id, marca_oficial, nom)
values
  ('20000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000004', '2500AX', 'Granja La Vall'),
  ('20000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000005', '2500BX', 'Granja Mas Roig'),
  ('20000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000006', '1700CX', 'Granja La Plana')
on conflict (id) do update set
  titular_id = excluded.titular_id,
  marca_oficial = excluded.marca_oficial,
  nom = excluded.nom;

-- =========================================================
-- 10) CATALEG BESTIAR
-- =========================================================

insert into public.bestiar (id, codi, descripcio)
values
  ('30000000-0000-0000-0000-000000000001', 'BOVI', 'Bovi'),
  ('30000000-0000-0000-0000-000000000002', 'PORCI', 'Porci'),
  ('30000000-0000-0000-0000-000000000003', 'OVI', 'Ovi'),
  ('30000000-0000-0000-0000-000000000004', 'AVICOLA', 'Avicola')
on conflict (id) do update set
  codi = excluded.codi,
  descripcio = excluded.descripcio;

-- =========================================================
-- 11) CATALEG FASE PRODUCTIVA
-- =========================================================

insert into public.fase_productiva (id, codi, descripcio)
values
  ('40000000-0000-0000-0000-000000000001', 'CRIA', 'Cria'),
  ('40000000-0000-0000-0000-000000000002', 'ENGREIX', 'Engreix'),
  ('40000000-0000-0000-0000-000000000003', 'LLET', 'Produccio de llet'),
  ('40000000-0000-0000-0000-000000000004', 'REPRO', 'Reproduccio')
on conflict (id) do update set
  codi = excluded.codi,
  descripcio = excluded.descripcio;

-- =========================================================
-- 12) GRANJA_BESTIAR
-- =========================================================

insert into public.granja_bestiar (id, granja_id, bestiar_id, fase_productiva_id, cens)
values
  ('50000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 800),
  ('50000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000004', 120),
  ('50000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000003', 95),
  ('50000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000001', 210)
on conflict (id) do update set
  granja_id = excluded.granja_id,
  bestiar_id = excluded.bestiar_id,
  fase_productiva_id = excluded.fase_productiva_id,
  cens = excluded.cens;

-- =========================================================
-- 13) ENTREGA_DEJECCIONS
-- =========================================================

insert into public.entrega_dejeccions (id, dan_id, granja_origen_id, data, quantitat, terra_desti_id, receptor_titular_id)
values
  ('70000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001', '2025-04-10', 250.00, 'd0000000-0000-0000-0000-000000000001', null),
  ('70000000-0000-0000-0000-000000000002', 'e0000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000002', '2025-05-14', 180.00, null, 'b0000000-0000-0000-0000-000000000006'),
  ('70000000-0000-0000-0000-000000000003', 'e0000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000003', '2025-06-02', 310.00, 'd0000000-0000-0000-0000-000000000005', null)
on conflict (id) do update set
  dan_id = excluded.dan_id,
  granja_origen_id = excluded.granja_origen_id,
  data = excluded.data,
  quantitat = excluded.quantitat,
  terra_desti_id = excluded.terra_desti_id,
  receptor_titular_id = excluded.receptor_titular_id;

-- =========================================================
-- 14) VERIFICACIO RAPIDA
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
  t.nom,
  t.email,
  t.rol,
  o.nom as oficina
from public.tecnic t
join public.oficina o on o.id = t.oficina_id
order by t.nom;

select
  u.id,
  u.email,
  (u.email_confirmed_at is not null) as email_confirmat
from auth.users u
where u.email in (
  'admin@agrisync.com',
  'manager@agrisync.com',
  'agricola@agrisync.com',
  'ramader@agrisync.com',
  'lectura@agrisync.com'
)
order by u.email;
