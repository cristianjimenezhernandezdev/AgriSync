-- =========================================================
-- seed_login_test.sql
-- Executa al SQL Editor de Supabase per crear dades de prova
-- =========================================================

-- 1) Crear una oficina de prova
INSERT INTO public.oficina (id, nom)
VALUES ('a0000000-0000-0000-0000-000000000001', 'Oficina Test')
ON CONFLICT (nom) DO NOTHING;

-- 2) Inserir el tècnic vinculat a l'usuari Auth
--    UUID de admin@agrisync.com a auth.users
INSERT INTO public.tecnic (oficina_id, user_id, nom, email, rol, actiu)
VALUES (
  'a0000000-0000-0000-0000-000000000001',
  '44dc0687-5bf2-4079-ba3d-6f23f5e9b1aa',
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

-- 3) Crear titulars de prova
INSERT INTO public.titular (id, nif, nom_rao)
VALUES
  ('b0000000-0000-0000-0000-000000000001', '12345678A', 'Agropecuaria El Pla S.L.'),
  ('b0000000-0000-0000-0000-000000000002', '87654321B', 'Joan Vila Camps'),
  ('b0000000-0000-0000-0000-000000000003', '11223344C', 'Cooperativa La Vall')
ON CONFLICT (id) DO NOTHING;

-- 4) Assignar titulars al tècnic
INSERT INTO public.tecnic_titular (tecnic_id, titular_id, scope, actiu)
VALUES
  ((SELECT id FROM public.tecnic WHERE user_id = '44dc0687-5bf2-4079-ba3d-6f23f5e9b1aa'), 'b0000000-0000-0000-0000-000000000001', 'comu', true),
  ((SELECT id FROM public.tecnic WHERE user_id = '44dc0687-5bf2-4079-ba3d-6f23f5e9b1aa'), 'b0000000-0000-0000-0000-000000000002', 'agricola', true),
  ((SELECT id FROM public.tecnic WHERE user_id = '44dc0687-5bf2-4079-ba3d-6f23f5e9b1aa'), 'b0000000-0000-0000-0000-000000000003', 'ramader', true)
ON CONFLICT (tecnic_id, titular_id, scope) DO NOTHING;

-- 5) Funcio RPC get_my_tecnic (SECURITY DEFINER)
CREATE OR REPLACE FUNCTION public.get_my_tecnic()
RETURNS SETOF public.tecnic
LANGUAGE sql STABLE SECURITY DEFINER
AS $$
  SELECT * FROM public.tecnic t WHERE t.user_id = auth.uid() LIMIT 1;
$$;

-- 6) Funcions RLS amb SECURITY DEFINER
CREATE OR REPLACE FUNCTION public.current_oficina_id()
RETURNS uuid LANGUAGE sql STABLE SECURITY DEFINER AS $$
  SELECT t.oficina_id FROM public.tecnic t WHERE t.user_id = auth.uid() AND t.actiu = true
$$;
CREATE OR REPLACE FUNCTION public.current_tecnic_id()
RETURNS uuid LANGUAGE sql STABLE SECURITY DEFINER AS $$
  SELECT t.id FROM public.tecnic t WHERE t.user_id = auth.uid() AND t.actiu = true
$$;
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER AS $$
  SELECT EXISTS(SELECT 1 FROM public.tecnic t WHERE t.user_id = auth.uid() AND t.actiu = true AND t.rol = 'admin')
$$;
CREATE OR REPLACE FUNCTION public.is_oficina_manager()
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER AS $$
  SELECT EXISTS(SELECT 1 FROM public.tecnic t WHERE t.user_id = auth.uid() AND t.actiu = true AND t.rol = 'oficina_manager')
$$;
CREATE OR REPLACE FUNCTION public.same_oficina(p_tecnic_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER AS $$
  SELECT EXISTS(SELECT 1 FROM public.tecnic t WHERE t.id = p_tecnic_id AND t.oficina_id = public.current_oficina_id())
$$;
CREATE OR REPLACE FUNCTION public.can_read_titular(p_titular_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER AS $$
  SELECT public.is_admin() OR public.is_oficina_manager()
    OR EXISTS (SELECT 1 FROM public.tecnic_titular tt JOIN public.tecnic t ON t.id = tt.tecnic_id
      WHERE t.user_id = auth.uid() AND t.actiu = true AND tt.titular_id = p_titular_id AND tt.actiu = true)
$$;
CREATE OR REPLACE FUNCTION public.can_write_scope(p_titular_id uuid, p_scope scope_titular)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER AS $$
  SELECT public.is_admin() OR EXISTS (SELECT 1 FROM public.tecnic_titular tt JOIN public.tecnic t ON t.id = tt.tecnic_id
    WHERE t.user_id = auth.uid() AND t.actiu = true AND tt.titular_id = p_titular_id AND tt.actiu = true
    AND (tt.scope = 'comu' OR tt.scope = p_scope))
$$;
CREATE OR REPLACE FUNCTION public.can_write_agricola(p_titular_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER AS $$
  SELECT public.can_write_scope(p_titular_id, 'agricola'::scope_titular)
$$;
CREATE OR REPLACE FUNCTION public.can_write_ramader(p_titular_id uuid)
RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER AS $$
  SELECT public.can_write_scope(p_titular_id, 'ramader'::scope_titular)
$$;

