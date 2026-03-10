-- =========================================================
-- fix_grants.sql
-- Executa al SQL Editor de Supabase per donar permisos
-- al rol 'authenticated' sobre totes les taules.
-- RLS segueix controlant qui veu/edita què.
-- =========================================================

-- Taules principals
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

-- Vista
GRANT SELECT ON public.v_titular_access TO authenticated;

-- Permisos per al rol anon (lectura limitada, RLS controla)
GRANT SELECT ON public.oficina TO anon;
GRANT SELECT ON public.tecnic TO anon;
GRANT SELECT ON public.titular TO anon;
GRANT SELECT ON public.v_titular_access TO anon;

