-- =========================================================
-- seed_mvp.sql
-- Executa al SQL Editor de Supabase
-- =========================================================
-- PREREQUISIT: Crea l'usuari manualment al Dashboard de Supabase:
--   Authentication > Users > Add user
--   Email: admin@agrisync.com
--   Password: 12345678
--
-- Després copia el UUID que Supabase li assigna i posa'l aquí:

-- >>> UUID de l'usuari auth: 325e0bc1-a081-49b7-b6fd-42ad656f744b <<<

-- 1) Oficina
INSERT INTO public.oficina (id, nom)
VALUES ('a0000000-0000-0000-0000-000000000001', 'Oficina Test')
ON CONFLICT (nom) DO NOTHING;

-- 2) Tècnic vinculat a l'usuari Auth
INSERT INTO public.tecnic (oficina_id, user_id, nom, email, rol, actiu)
VALUES (
  'a0000000-0000-0000-0000-000000000001',
  '325e0bc1-a081-49b7-b6fd-42ad656f744b',
  'Admin',
  'admin@agrisync.com',
  'admin',
  true
)
ON CONFLICT (user_id) DO UPDATE SET
  rol = 'admin',
  actiu = true,
  nom = 'Admin',
  email = 'admin@agrisync.com';

-- 3) Titulars de prova
INSERT INTO public.titular (id, nif, nom_rao)
VALUES
  ('b0000000-0000-0000-0000-000000000001', '12345678A', 'Agropecuaria El Pla S.L.'),
  ('b0000000-0000-0000-0000-000000000002', '87654321B', 'Joan Vila Camps'),
  ('b0000000-0000-0000-0000-000000000003', '11223344C', 'Cooperativa La Vall')
ON CONFLICT (id) DO NOTHING;

-- 4) Assignar titulars al tècnic amb scopes
INSERT INTO public.tecnic_titular (tecnic_id, titular_id, scope, actiu)
VALUES
  (
    (SELECT id FROM public.tecnic WHERE user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b'),
    'b0000000-0000-0000-0000-000000000001',
    'comu', true
  ),
  (
    (SELECT id FROM public.tecnic WHERE user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b'),
    'b0000000-0000-0000-0000-000000000002',
    'agricola', true
  ),
  (
    (SELECT id FROM public.tecnic WHERE user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b'),
    'b0000000-0000-0000-0000-000000000003',
    'ramader', true
  )
ON CONFLICT (tecnic_id, titular_id, scope) DO NOTHING;

-- 5) Verificació
SELECT t.id as tecnic_id, t.nom, t.email, t.rol, t.user_id,
       tt.titular_id, tt.scope
FROM public.tecnic t
LEFT JOIN public.tecnic_titular tt ON tt.tecnic_id = t.id
WHERE t.user_id = '325e0bc1-a081-49b7-b6fd-42ad656f744b';

