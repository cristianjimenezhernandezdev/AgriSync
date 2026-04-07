-- =========================================================
-- fix_user_ids.sql
-- Sincronitza el user_id de public.tecnic amb auth.users
-- basant-se en l'email que comparteixen.
-- Executa al SQL Editor de Supabase.
-- =========================================================

-- 1) Mostra l'estat actual (abans de corregir)
SELECT
  t.nom,
  t.email,
  t.user_id AS user_id_actual_tecnic,
  u.id AS user_id_auth,
  CASE WHEN t.user_id = u.id THEN 'OK' ELSE 'DESINCRONITZAT' END AS estat
FROM public.tecnic t
LEFT JOIN auth.users u ON u.email = t.email
ORDER BY t.nom;

-- 2) Actualitza tots els user_id per coincidir amb auth.users
UPDATE public.tecnic t
SET user_id = u.id
FROM auth.users u
WHERE u.email = t.email
  AND (t.user_id IS NULL OR t.user_id != u.id);

-- 3) Verifica el resultat
SELECT
  t.nom,
  t.email,
  t.user_id,
  t.rol,
  t.actiu
FROM public.tecnic t
ORDER BY t.nom;

