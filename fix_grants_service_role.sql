-- =========================================================
-- fix_grants_all.sql
-- Executa al SQL Editor de Supabase per arreglar permisos.
-- =========================================================

-- 1) GRANT per a tots els rols necessaris
-- service_role (bypass RLS per operacions admin)
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO service_role;
GRANT USAGE ON SCHEMA public TO service_role;

-- authenticated (token JWT d'usuari loguejat - RLS controla accés)
GRANT SELECT, INSERT, UPDATE, DELETE ON public.oficina TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.tecnic TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.titular TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.tecnic_titular TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.dan_declaracio TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.terra TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.cessio_terra TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.aplicacions_fertilitzants TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.granja TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.bestiar TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.fase_productiva TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.granja_bestiar TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.emmagatzematge TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.entrega_dejeccions TO authenticated;
GRANT SELECT ON public.v_titular_access TO authenticated;
GRANT USAGE ON SCHEMA public TO authenticated;

-- anon (lectura mínima)
GRANT SELECT ON public.oficina TO anon;
GRANT SELECT ON public.tecnic TO anon;
GRANT SELECT ON public.titular TO anon;
GRANT SELECT ON public.v_titular_access TO anon;
GRANT USAGE ON SCHEMA public TO anon;

-- 2) Fix RLS policy: oficina visible per tots els autenticats (tècnics necessiten veure la seva)
DROP POLICY IF EXISTS oficina_select ON public.oficina;
CREATE POLICY oficina_select ON public.oficina
FOR SELECT TO authenticated
USING (true);  -- Tots els autenticats poden veure oficines (dada no sensible)

-- 3) Assegurar que les funcions helper existeixin i funcionin
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO service_role;

