-- =========================================================
-- seed_complet.sql
-- Seed complet per proves MVP — totes les taules
-- Executa al SQL Editor de Supabase DESPRES de SQLAgriSync.sql
-- =========================================================
-- PREREQUISIT:
--   1) L'usuari admin@agrisync.com ha d'existir a Authentication > Users
--      amb UUID: 325e0bc1-a081-49b7-b6fd-42ad656f744b
--   2) SQLAgriSync.sql ja executat (taules + RLS + vista)
-- =========================================================

-- =====================
-- OFICINES
-- =====================
INSERT INTO public.oficina (id, nom) VALUES
  ('a0000000-0000-0000-0000-000000000001', 'Oficina Lleida'),
  ('a0000000-0000-0000-0000-000000000002', 'Oficina Girona')
ON CONFLICT (id) DO UPDATE SET nom = EXCLUDED.nom;

-- =====================
-- TECNICS
-- =====================
-- Admin (login principal) — NO forcem id, respectem el que ja existeixi
INSERT INTO public.tecnic (oficina_id, user_id, nom, email, rol, actiu) VALUES
  ('a0000000-0000-0000-0000-000000000001',
   '325e0bc1-a081-49b7-b6fd-42ad656f744b',
   'Admin', 'admin@agrisync.com', 'admin', true)
ON CONFLICT (user_id) DO UPDATE SET
  rol = 'admin', actiu = true, nom = 'Admin', email = 'admin@agrisync.com';

-- Tecnics adicionals (sense user_id — no fan login, nomes per assignar)
INSERT INTO public.tecnic (id, oficina_id, nom, email, rol, actiu) VALUES
  ('c0000000-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001',
   'Maria Lopez', 'maria@agrisync.com', 'tecnic', true),
  ('c0000000-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000001',
   'Pere Soler', 'pere@agrisync.com', 'tecnic', true),
  ('c0000000-0000-0000-0000-000000000004',
   'a0000000-0000-0000-0000-000000000002',
   'Anna Puig', 'anna@agrisync.com', 'oficina_manager', true)
ON CONFLICT (id) DO NOTHING;

-- =====================
-- TITULARS
-- =====================
INSERT INTO public.titular (id, nif, nom_rao) VALUES
  ('b0000000-0000-0000-0000-000000000001', '12345678A', 'Agropecuaria El Pla S.L.'),
  ('b0000000-0000-0000-0000-000000000002', '87654321B', 'Joan Vila Camps'),
  ('b0000000-0000-0000-0000-000000000003', '11223344C', 'Cooperativa La Vall'),
  ('b0000000-0000-0000-0000-000000000004', '55667788D', 'Granja Mas Roig S.L.'),
  ('b0000000-0000-0000-0000-000000000005', '99887766E', 'SAT La Plana')
ON CONFLICT (id) DO NOTHING;

-- =====================
-- TECNIC_TITULAR (assignacions amb scopes)
-- Usem subquery per l'admin per agafar l'id real que te a la BD
-- =====================
INSERT INTO public.tecnic_titular (tecnic_id, titular_id, scope, actiu) VALUES
  -- Admin te acces comu/agricola/ramader
  ((SELECT id FROM public.tecnic WHERE user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b'), 'b0000000-0000-0000-0000-000000000001', 'comu', true),
  ((SELECT id FROM public.tecnic WHERE user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b'), 'b0000000-0000-0000-0000-000000000002', 'agricola', true),
  ((SELECT id FROM public.tecnic WHERE user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b'), 'b0000000-0000-0000-0000-000000000003', 'ramader', true),
  ((SELECT id FROM public.tecnic WHERE user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b'), 'b0000000-0000-0000-0000-000000000004', 'comu', true),
  ((SELECT id FROM public.tecnic WHERE user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b'), 'b0000000-0000-0000-0000-000000000005', 'agricola', true),
  -- Maria: agricola
  ('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'agricola', true),
  ('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 'agricola', true),
  -- Pere: ramader
  ('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'ramader', true),
  ('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000004', 'ramader', true)
ON CONFLICT (tecnic_id, titular_id, scope) DO NOTHING;

-- =====================
-- TERRES (camps SIGPAC)
-- =====================
INSERT INTO public.terra (id, titular_id, mun_codi, poligon, parcela, recinte, superficie) VALUES
  ('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', '25120', 3, 45, 1, 12.50),
  ('d0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', '25120', 3, 45, 2, 8.30),
  ('d0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002', '25120', 5, 12, 1, 22.00),
  ('d0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000002', '17071', 2, 8, 1, 5.75),
  ('d0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000005', '25120', 7, 3, 1, 30.00),
  ('d0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000005', '25120', 7, 3, 2, 15.20)
ON CONFLICT (mun_codi, poligon, parcela, recinte) DO NOTHING;

-- =====================
-- DAN_DECLARACIO (campanyes)
-- =====================
INSERT INTO public.dan_declaracio (id, titular_id, campanya, estat) VALUES
  ('e0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 2025, 'en_curs'),
  ('e0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', 2025, 'en_curs'),
  ('e0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 2025, 'en_curs'),
  ('e0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', 2025, 'en_curs'),
  ('e0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000005', 2025, 'en_curs'),
  ('e0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000001', 2024, 'tancat')
ON CONFLICT (titular_id, campanya) DO NOTHING;

-- =====================
-- CESSIO_TERRA
-- =====================
INSERT INTO public.cessio_terra (id, dan_id, terra_id, data_inici, data_fi, titular_explotador_id) VALUES
  ('f0000000-0000-0000-0000-000000000001',
   'e0000000-0000-0000-0000-000000000001',
   'd0000000-0000-0000-0000-000000000003',
   '2025-01-01', '2025-12-31',
   'b0000000-0000-0000-0000-000000000001'),
  ('f0000000-0000-0000-0000-000000000002',
   'e0000000-0000-0000-0000-000000000002',
   'd0000000-0000-0000-0000-000000000001',
   '2025-03-01', '2025-09-30',
   'b0000000-0000-0000-0000-000000000002')
ON CONFLICT (id) DO NOTHING;

-- =====================
-- APLICACIONS_FERTILITZANTS
-- Usem subquery per tecnic_id de l'admin
-- =====================
INSERT INTO public.aplicacions_fertilitzants (id, dan_id, terra_id, data, kg_n, uf, tecnic_id) VALUES
  ('10000000-0000-0000-0000-000000000001',
   'e0000000-0000-0000-0000-000000000001',
   'd0000000-0000-0000-0000-000000000001',
   '2025-03-15', 120.5, 45.0,
   (SELECT id FROM public.tecnic WHERE user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b')),
  ('10000000-0000-0000-0000-000000000002',
   'e0000000-0000-0000-0000-000000000001',
   'd0000000-0000-0000-0000-000000000002',
   '2025-04-10', 85.0, 32.0,
   'c0000000-0000-0000-0000-000000000002'),
  ('10000000-0000-0000-0000-000000000003',
   'e0000000-0000-0000-0000-000000000002',
   'd0000000-0000-0000-0000-000000000003',
   '2025-05-20', 200.0, 78.5,
   (SELECT id FROM public.tecnic WHERE user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b')),
  ('10000000-0000-0000-0000-000000000004',
   'e0000000-0000-0000-0000-000000000005',
   'd0000000-0000-0000-0000-000000000005',
   '2025-02-28', 150.0, 55.0,
   'c0000000-0000-0000-0000-000000000002'),
  ('10000000-0000-0000-0000-000000000005',
   'e0000000-0000-0000-0000-000000000006',
   'd0000000-0000-0000-0000-000000000001',
   '2024-06-01', 95.0, 40.0,
   (SELECT id FROM public.tecnic WHERE user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b'))
ON CONFLICT (id) DO NOTHING;

-- =====================
-- GRANJES
-- =====================
INSERT INTO public.granja (id, titular_id, marca_oficial, nom) VALUES
  ('20000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', 'ES25-0001', 'Granja La Vall'),
  ('20000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000003', 'ES25-0002', 'Granja El Torrent'),
  ('20000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000004', 'ES17-0010', 'Granja Mas Roig'),
  ('20000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000004', 'ES17-0011', 'Granja Can Prat')
ON CONFLICT (marca_oficial) DO NOTHING;

-- =====================
-- BESTIAR (cataleg)
-- =====================
INSERT INTO public.bestiar (id, codi, descripcio) VALUES
  ('30000000-0000-0000-0000-000000000001', 'BOVIN', 'Boví'),
  ('30000000-0000-0000-0000-000000000002', 'PORCI', 'Porcí'),
  ('30000000-0000-0000-0000-000000000003', 'OVI', 'Oví'),
  ('30000000-0000-0000-0000-000000000004', 'AVICOLA', 'Avícola'),
  ('30000000-0000-0000-0000-000000000005', 'EQUI', 'Equí')
ON CONFLICT (codi) DO NOTHING;

-- =====================
-- FASE_PRODUCTIVA (cataleg)
-- =====================
INSERT INTO public.fase_productiva (id, codi, descripcio) VALUES
  ('40000000-0000-0000-0000-000000000001', 'CRIA', 'Cria'),
  ('40000000-0000-0000-0000-000000000002', 'ENGREIX', 'Engreix'),
  ('40000000-0000-0000-0000-000000000003', 'LLET', 'Producció de llet'),
  ('40000000-0000-0000-0000-000000000004', 'REPRODUCCIO', 'Reproducció'),
  ('40000000-0000-0000-0000-000000000005', 'MIXT', 'Cicle mixt')
ON CONFLICT (codi) DO NOTHING;

-- =====================
-- GRANJA_BESTIAR
-- =====================
INSERT INTO public.granja_bestiar (id, granja_id, bestiar_id, fase_productiva_id, cens) VALUES
  ('50000000-0000-0000-0000-000000000001',
   '20000000-0000-0000-0000-000000000001',
   '30000000-0000-0000-0000-000000000001',
   '40000000-0000-0000-0000-000000000003', 120),
  ('50000000-0000-0000-0000-000000000002',
   '20000000-0000-0000-0000-000000000001',
   '30000000-0000-0000-0000-000000000001',
   '40000000-0000-0000-0000-000000000001', 45),
  ('50000000-0000-0000-0000-000000000003',
   '20000000-0000-0000-0000-000000000002',
   '30000000-0000-0000-0000-000000000002',
   '40000000-0000-0000-0000-000000000002', 800),
  ('50000000-0000-0000-0000-000000000004',
   '20000000-0000-0000-0000-000000000003',
   '30000000-0000-0000-0000-000000000003',
   '40000000-0000-0000-0000-000000000004', 350),
  ('50000000-0000-0000-0000-000000000005',
   '20000000-0000-0000-0000-000000000003',
   '30000000-0000-0000-0000-000000000003',
   '40000000-0000-0000-0000-000000000001', 200),
  ('50000000-0000-0000-0000-000000000006',
   '20000000-0000-0000-0000-000000000004',
   '30000000-0000-0000-0000-000000000004',
   '40000000-0000-0000-0000-000000000002', 15000)
ON CONFLICT (granja_id, bestiar_id, fase_productiva_id) DO NOTHING;

-- =====================
-- EMMAGATZEMATGE
-- =====================
INSERT INTO public.emmagatzematge (id, granja_id, tipus, capacitat, info_extra) VALUES
  ('60000000-0000-0000-0000-000000000001',
   '20000000-0000-0000-0000-000000000001',
   'Bassa', 500.0, 'Bassa coberta amb mescla de purins'),
  ('60000000-0000-0000-0000-000000000002',
   '20000000-0000-0000-0000-000000000002',
   'Fossa', 1200.0, 'Fossa soterrada'),
  ('60000000-0000-0000-0000-000000000003',
   '20000000-0000-0000-0000-000000000003',
   'Estercolera', 300.0, NULL),
  ('60000000-0000-0000-0000-000000000004',
   '20000000-0000-0000-0000-000000000004',
   'Bassa', 800.0, 'Bassa a cel obert')
ON CONFLICT (id) DO NOTHING;

-- =====================
-- ENTREGA_DEJECCIONS
-- =====================
INSERT INTO public.entrega_dejeccions (id, dan_id, granja_origen_id, data, quantitat, terra_desti_id, receptor_titular_id) VALUES
  ('70000000-0000-0000-0000-000000000001',
   'e0000000-0000-0000-0000-000000000003',
   '20000000-0000-0000-0000-000000000001',
   '2025-04-01', 250.0,
   'd0000000-0000-0000-0000-000000000001', NULL),
  ('70000000-0000-0000-0000-000000000002',
   'e0000000-0000-0000-0000-000000000003',
   '20000000-0000-0000-0000-000000000002',
   '2025-05-15', 600.0,
   'd0000000-0000-0000-0000-000000000003', NULL),
  ('70000000-0000-0000-0000-000000000003',
   'e0000000-0000-0000-0000-000000000004',
   '20000000-0000-0000-0000-000000000003',
   '2025-06-10', 180.0,
   NULL, 'b0000000-0000-0000-0000-000000000005'),
  ('70000000-0000-0000-0000-000000000004',
   'e0000000-0000-0000-0000-000000000004',
   '20000000-0000-0000-0000-000000000004',
   '2025-07-20', 400.0,
   'd0000000-0000-0000-0000-000000000005', NULL)
ON CONFLICT (id) DO NOTHING;

-- =====================
-- VERIFICACIO FINAL
-- =====================
SELECT 'oficina' as taula, count(*) as registres FROM public.oficina
UNION ALL SELECT 'tecnic', count(*) FROM public.tecnic
UNION ALL SELECT 'titular', count(*) FROM public.titular
UNION ALL SELECT 'tecnic_titular', count(*) FROM public.tecnic_titular
UNION ALL SELECT 'dan_declaracio', count(*) FROM public.dan_declaracio
UNION ALL SELECT 'terra', count(*) FROM public.terra
UNION ALL SELECT 'cessio_terra', count(*) FROM public.cessio_terra
UNION ALL SELECT 'aplicacions_fertilitzants', count(*) FROM public.aplicacions_fertilitzants
UNION ALL SELECT 'granja', count(*) FROM public.granja
UNION ALL SELECT 'bestiar', count(*) FROM public.bestiar
UNION ALL SELECT 'fase_productiva', count(*) FROM public.fase_productiva
UNION ALL SELECT 'granja_bestiar', count(*) FROM public.granja_bestiar
UNION ALL SELECT 'emmagatzematge', count(*) FROM public.emmagatzematge
UNION ALL SELECT 'entrega_dejeccions', count(*) FROM public.entrega_dejeccions
ORDER BY taula;

