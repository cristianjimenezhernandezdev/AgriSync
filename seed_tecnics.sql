-- =========================================================
-- seed_tecnics.sql
-- Crea tècnics amb credencials PROPIES (email + password diferent)
-- Executa al SQL Editor de Supabase
-- =========================================================
-- NO cal crear usuaris al Dashboard manualment!
-- Aquest script ho fa tot: crea auth.users + public.tecnic + assignacions
-- =========================================================

-- =====================
-- 1) CREAR USUARIS AUTH AMB PASSWORDS DIFERENTS
-- Canvia els emails i passwords com vulguis
-- =====================

-- Maria: password "maria2024"
INSERT INTO auth.users (
  instance_id, id, aud, role, email, encrypted_password,
  email_confirmed_at, raw_app_meta_data, raw_user_meta_data,
  created_at, updated_at, confirmation_token, recovery_token
) VALUES (
  '00000000-0000-0000-0000-000000000000',
  gen_random_uuid(),
  'authenticated', 'authenticated',
  'maria@agrisync.com',
  crypt('maria2024', gen_salt('bf')),
  NOW(),
  '{"provider":"email","providers":["email"]}',
  '{}',
  NOW(), NOW(), '', ''
)
ON CONFLICT (email) DO NOTHING;

-- Crear identitat per Maria (necessari perquè Supabase Auth funcioni)
INSERT INTO auth.identities (
  id, user_id, provider_id, identity_data, provider, last_sign_in_at, created_at, updated_at
)
SELECT
  u.id, u.id, u.email,
  jsonb_build_object('sub', u.id::text, 'email', u.email),
  'email', NOW(), NOW(), NOW()
FROM auth.users u
WHERE u.email = 'maria@agrisync.com'
ON CONFLICT (provider, provider_id) DO NOTHING;

-- Pere: password "pere2024"
INSERT INTO auth.users (
  instance_id, id, aud, role, email, encrypted_password,
  email_confirmed_at, raw_app_meta_data, raw_user_meta_data,
  created_at, updated_at, confirmation_token, recovery_token
) VALUES (
  '00000000-0000-0000-0000-000000000000',
  gen_random_uuid(),
  'authenticated', 'authenticated',
  'pere@agrisync.com',
  crypt('pere2024', gen_salt('bf')),
  NOW(),
  '{"provider":"email","providers":["email"]}',
  '{}',
  NOW(), NOW(), '', ''
)
ON CONFLICT (email) DO NOTHING;

-- Crear identitat per Pere
INSERT INTO auth.identities (
  id, user_id, provider_id, identity_data, provider, last_sign_in_at, created_at, updated_at
)
SELECT
  u.id, u.id, u.email,
  jsonb_build_object('sub', u.id::text, 'email', u.email),
  'email', NOW(), NOW(), NOW()
FROM auth.users u
WHERE u.email = 'pere@agrisync.com'
ON CONFLICT (provider, provider_id) DO NOTHING;

-- =====================
-- 2) CREAR TÈCNICS A public.tecnic
-- =====================

-- Maria: tècnica agrícola a Oficina Lleida
INSERT INTO public.tecnic (oficina_id, user_id, nom, email, rol, actiu)
VALUES (
  'a0000000-0000-0000-0000-000000000001',
  (SELECT id FROM auth.users WHERE email = 'maria@agrisync.com'),
  'Maria Lopez',
  'maria@agrisync.com',
  'tecnic',
  true
)
ON CONFLICT (user_id) DO UPDATE SET
  nom = 'Maria Lopez', email = 'maria@agrisync.com', rol = 'tecnic', actiu = true;

-- Pere: tècnic ramader a Oficina Lleida
INSERT INTO public.tecnic (oficina_id, user_id, nom, email, rol, actiu)
VALUES (
  'a0000000-0000-0000-0000-000000000001',
  (SELECT id FROM auth.users WHERE email = 'pere@agrisync.com'),
  'Pere Soler',
  'pere@agrisync.com',
  'tecnic',
  true
)
ON CONFLICT (user_id) DO UPDATE SET
  nom = 'Pere Soler', email = 'pere@agrisync.com', rol = 'tecnic', actiu = true;

-- =====================
-- 3) ASSIGNAR TITULARS AMB SCOPES
-- Maria: AGRICOLA → Agropecuaria El Pla + Joan Vila
-- Pere:  RAMADER  → Cooperativa La Vall + Granja Mas Roig
-- =====================
INSERT INTO public.tecnic_titular (tecnic_id, titular_id, scope, actiu) VALUES
  ((SELECT id FROM public.tecnic WHERE email = 'maria@agrisync.com'),
   'b0000000-0000-0000-0000-000000000001', 'agricola', true),
  ((SELECT id FROM public.tecnic WHERE email = 'maria@agrisync.com'),
   'b0000000-0000-0000-0000-000000000002', 'agricola', true),
  ((SELECT id FROM public.tecnic WHERE email = 'pere@agrisync.com'),
   'b0000000-0000-0000-0000-000000000003', 'ramader', true),
  ((SELECT id FROM public.tecnic WHERE email = 'pere@agrisync.com'),
   'b0000000-0000-0000-0000-000000000004', 'ramader', true)
ON CONFLICT (tecnic_id, titular_id, scope) DO NOTHING;

-- =====================
-- 4) VERIFICACIO
-- =====================

-- Usuaris Auth creats:
SELECT id, email, email_confirmed_at IS NOT NULL AS confirmat
FROM auth.users
WHERE email IN ('maria@agrisync.com', 'pere@agrisync.com');

-- Tècnics amb assignacions:
SELECT
  t.nom AS tecnic,
  t.email,
  t.rol,
  tt.scope,
  ti.nom_rao AS titular
FROM public.tecnic t
JOIN public.tecnic_titular tt ON tt.tecnic_id = t.id
JOIN public.titular ti ON ti.id = tt.titular_id
WHERE t.email IN ('maria@agrisync.com', 'pere@agrisync.com')
ORDER BY t.email, ti.nom_rao;
