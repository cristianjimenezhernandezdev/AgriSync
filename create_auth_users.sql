-- =========================================================
-- create_auth_users.sql
-- Script legacy per crear usuaris Auth de demo.
-- IMPORTANT:
--   En versions noves de Supabase es recomana crear usuaris des de
--   Authentication > Users o via Admin API amb service_role.
--   Inserir directament a auth.users/auth.identities pot deixar Auth
--   en un estat inconsistent i provocar errors com:
--   "Database error querying schema" o "Database error finding users".
-- Si et passa, elimina i recrea els usuaris demo des del Dashboard i
-- després torna a executar seed_final_demo.sql o fix_user_ids.sql.
-- =========================================================
-- Executa'l al SQL Editor de Supabase ABANS del seed_final_demo.sql
-- =========================================================

-- 1) Neteja: elimina identitats i usuaris anteriors si existeixen
DELETE FROM auth.identities
WHERE user_id IN (
  SELECT id FROM auth.users WHERE email IN (
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
);

DELETE FROM auth.users
WHERE email IN (
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
);

-- 2) Inserir usuaris amb tots els camps obligatoris de GoTrue
DO $$
DECLARE
  rec RECORD;
BEGIN
  FOR rec IN
    SELECT * FROM (VALUES
      ('admin.demo@agrisync.com',              'admin1234'),
      ('manager.lleida.demo@agrisync.com',     'lleida1234'),
      ('manager.girona.demo@agrisync.com',     'girona1234'),
      ('sergi.agri.demo@agrisync.com',         'sergi1234'),
      ('marta.ram.demo@agrisync.com',          'marta1234'),
      ('laia.comu.demo@agrisync.com',          'laia1234'),
      ('nil.shared.demo@agrisync.com',         'nil1234'),
      ('joan.agri.demo@agrisync.com',          'joan1234'),
      ('anna.ram.demo@agrisync.com',           'anna1234'),
      ('lectura.demo@agrisync.com',            'lectura1234')
    ) AS t(email, password)
  LOOP
    -- Inserir a auth.users
    INSERT INTO auth.users (
      instance_id,
      id,
      aud,
      role,
      email,
      encrypted_password,
      email_confirmed_at,
      invited_at,
      confirmation_token,
      confirmation_sent_at,
      recovery_token,
      recovery_sent_at,
      email_change_token_new,
      email_change,
      email_change_sent_at,
      last_sign_in_at,
      raw_app_meta_data,
      raw_user_meta_data,
      is_super_admin,
      created_at,
      updated_at,
      phone,
      phone_confirmed_at,
      phone_change,
      phone_change_token,
      phone_change_sent_at,
      email_change_token_current,
      email_change_confirm_status,
      banned_until,
      reauthentication_token,
      reauthentication_sent_at,
      is_sso_user,
      deleted_at
    ) VALUES (
      '00000000-0000-0000-0000-000000000000',
      gen_random_uuid(),
      'authenticated',
      'authenticated',
      rec.email,
      crypt(rec.password, gen_salt('bf')),
      now(),   -- email_confirmed_at
      NULL,    -- invited_at
      '',      -- confirmation_token
      NULL,    -- confirmation_sent_at
      '',      -- recovery_token
      NULL,    -- recovery_sent_at
      '',      -- email_change_token_new
      '',      -- email_change
      NULL,    -- email_change_sent_at
      NULL,    -- last_sign_in_at
      jsonb_build_object('provider', 'email', 'providers', array['email']),
      '{}'::jsonb,
      false,
      now(),
      now(),
      NULL,    -- phone
      NULL,    -- phone_confirmed_at
      '',      -- phone_change
      '',      -- phone_change_token
      NULL,    -- phone_change_sent_at
      '',      -- email_change_token_current
      0,       -- email_change_confirm_status
      NULL,    -- banned_until
      '',      -- reauthentication_token
      NULL,    -- reauthentication_sent_at
      false,   -- is_sso_user
      NULL     -- deleted_at
    );

    -- Inserir identitat (OBLIGATORI per login)
    INSERT INTO auth.identities (
      id,
      user_id,
      provider_id,
      identity_data,
      provider,
      last_sign_in_at,
      created_at,
      updated_at
    )
    SELECT
      u.id,
      u.id,
      u.email,
      jsonb_build_object(
        'sub', u.id::text,
        'email', u.email,
        'email_verified', true
      ),
      'email',
      now(),
      now(),
      now()
    FROM auth.users u
    WHERE u.email = rec.email;

  END LOOP;
END
$$;

-- 3) Verificacio
SELECT u.email, u.id, u.email_confirmed_at IS NOT NULL AS confirmed,
       EXISTS(SELECT 1 FROM auth.identities i WHERE i.user_id = u.id) AS has_identity
FROM auth.users u
WHERE u.email LIKE '%.demo@agrisync.com' OR u.email LIKE '%demo@agrisync.com'
ORDER BY u.email;

