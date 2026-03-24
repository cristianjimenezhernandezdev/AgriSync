-- =========================================================
-- fix_auth_test_users.sql
-- Neteja usuaris de prova a Supabase Auth
-- Executa al SQL Editor abans de recrear els usuaris de prova
-- =========================================================
--
-- Nota:
--   Segons la documentacio oficial de Supabase, un usuari Auth no es pot
--   eliminar si es propietari d'objectes a Storage.
--   Per aixo aquest script:
--   1) desvincula public.tecnic
--   2) elimina objectes de storage si n'hi ha
--   3) elimina auth.users
--
-- Fonts:
--   Supabase Docs > User Management
-- =========================================================

-- 1) Veure quins usuaris de prova existeixen
select id, email, created_at
from auth.users
where email in (
  'admin@agrisync.com',
  'manager@agrisync.com',
  'agricola@agrisync.com',
  'ramader@agrisync.com',
  'lectura@agrisync.com'
)
order by email;

-- 2) Desvincular la part funcional
update public.tecnic
set user_id = null
where email in (
  'admin@agrisync.com',
  'manager@agrisync.com',
  'agricola@agrisync.com',
  'ramader@agrisync.com',
  'lectura@agrisync.com'
);

-- 3) Comprovar si tenen objectes a Storage
select
  o.id,
  o.name,
  o.bucket_id,
  o.owner
from storage.objects o
where o.owner in (
  select u.id
  from auth.users u
  where u.email in (
    'admin@agrisync.com',
    'manager@agrisync.com',
    'agricola@agrisync.com',
    'ramader@agrisync.com',
    'lectura@agrisync.com'
  )
)
order by o.bucket_id, o.name;

-- 4) Eliminar objectes de Storage propietat d'aquests usuaris
delete from storage.objects
where owner in (
  select u.id
  from auth.users u
  where u.email in (
    'admin@agrisync.com',
    'manager@agrisync.com',
    'agricola@agrisync.com',
    'ramader@agrisync.com',
    'lectura@agrisync.com'
  )
);

-- 5) Eliminar usuaris Auth
delete from auth.users
where email in (
  'admin@agrisync.com',
  'manager@agrisync.com',
  'agricola@agrisync.com',
  'ramader@agrisync.com',
  'lectura@agrisync.com'
);

-- 6) Comprovacio final
select id, email
from auth.users
where email in (
  'admin@agrisync.com',
  'manager@agrisync.com',
  'agricola@agrisync.com',
  'ramader@agrisync.com',
  'lectura@agrisync.com'
)
order by email;
