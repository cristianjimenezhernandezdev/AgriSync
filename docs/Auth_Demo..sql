-- =========================================================
-- Crear usuaris Auth per al seed demo
-- Executa ABANS del seed_final_demo.sql
-- =========================================================

INSERT INTO auth.users (
  instance_id, id, aud, role, email, encrypted_password,
  email_confirmed_at, created_at, updated_at,
  confirmation_token, recovery_token,
  raw_app_meta_data, raw_user_meta_data, is_super_admin
)
VALUES
  (
    '00000000-0000-0000-0000-000000000000', gen_random_uuid(), 'authenticated', 'authenticated',
    'admin.demo@agrisync.com', crypt('admin1234', gen_salt('bf')),
    now(), now(), now(), '', '',
    '{"provider":"email","providers":["email"]}', '{}', false
  ),
  (
    '00000000-0000-0000-0000-000000000000', gen_random_uuid(), 'authenticated', 'authenticated',
    'manager.lleida.demo@agrisync.com', crypt('lleida1234', gen_salt('bf')),
    now(), now(), now(), '', '',
    '{"provider":"email","providers":["email"]}', '{}', false
  ),
  (
    '00000000-0000-0000-0000-000000000000', gen_random_uuid(), 'authenticated', 'authenticated',
    'manager.girona.demo@agrisync.com', crypt('girona1234', gen_salt('bf')),
    now(), now(), now(), '', '',
    '{"provider":"email","providers":["email"]}', '{}', false
  ),
  (
    '00000000-0000-0000-0000-000000000000', gen_random_uuid(), 'authenticated', 'authenticated',
    'sergi.agri.demo@agrisync.com', crypt('sergi1234', gen_salt('bf')),
    now(), now(), now(), '', '',
    '{"provider":"email","providers":["email"]}', '{}', false
  ),
  (
    '00000000-0000-0000-0000-000000000000', gen_random_uuid(), 'authenticated', 'authenticated',
    'marta.ram.demo@agrisync.com', crypt('marta1234', gen_salt('bf')),
    now(), now(), now(), '', '',
    '{"provider":"email","providers":["email"]}', '{}', false
  ),
  (
    '00000000-0000-0000-0000-000000000000', gen_random_uuid(), 'authenticated', 'authenticated',
    'laia.comu.demo@agrisync.com', crypt('laia1234', gen_salt('bf')),
    now(), now(), now(), '', '',
    '{"provider":"email","providers":["email"]}', '{}', false
  ),
  (
    '00000000-0000-0000-0000-000000000000', gen_random_uuid(), 'authenticated', 'authenticated',
    'nil.shared.demo@agrisync.com', crypt('nil1234', gen_salt('bf')),
    now(), now(), now(), '', '',
    '{"provider":"email","providers":["email"]}', '{}', false
  ),
  (
    '00000000-0000-0000-0000-000000000000', gen_random_uuid(), 'authenticated', 'authenticated',
    'joan.agri.demo@agrisync.com', crypt('joan1234', gen_salt('bf')),
    now(), now(), now(), '', '',
    '{"provider":"email","providers":["email"]}', '{}', false
  ),
  (
    '00000000-0000-0000-0000-000000000000', gen_random_uuid(), 'authenticated', 'authenticated',
    'anna.ram.demo@agrisync.com', crypt('anna1234', gen_salt('bf')),
    now(), now(), now(), '', '',
    '{"provider":"email","providers":["email"]}', '{}', false
  ),
  (
    '00000000-0000-0000-0000-000000000000', gen_random_uuid(), 'authenticated', 'authenticated',
    'lectura.demo@agrisync.com', crypt('lectura1234', gen_salt('bf')),
    now(), now(), now(), '', '',
    '{"provider":"email","providers":["email"]}', '{}', false
  );

-- Crear identitats (necessari perquè Supabase Auth pugui fer login)
INSERT INTO auth.identities (id, user_id, provider_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
SELECT
  u.id, u.id, u.email,
  jsonb_build_object('sub', u.id::text, 'email', u.email),
  'email', now(), now(), now()
FROM auth.users u
WHERE u.email IN (
  'admin.demo@agrisync.com',
  'manager.lleida.demo@agrisync.com',
  'manager.girona.demo@agrisync.com',
  'sergi.agri.demo@agrisync.com',
  'marta.ram.demo@agrisync.com',
  'laia.comu.demo@agrisync.com',
  'nil.shared.demo@agrisync.com',
  'joan.agri.demo@agrisync.com',
  'anna.ram.demo@agrisync.com',
  'lectura.demo@agrisync.com'
)
AND NOT EXISTS (
  SELECT 1 FROM auth.identities i WHERE i.user_id = u.id
);

-- Verificació
SELECT email, id FROM auth.users
WHERE email LIKE '%agrisync.com'
ORDER BY email;
