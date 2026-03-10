-- =========================================================
-- seed_fix_permisos.sql
-- Executa al SQL Editor de Supabase
-- Arregla permisos i grants per que tot funcioni correctament
-- =========================================================

-- 1) Assegurar que la funció get_my_tecnic existeix amb SECURITY DEFINER
CREATE OR REPLACE FUNCTION public.get_my_tecnic()
RETURNS SETOF public.tecnic
LANGUAGE sql STABLE SECURITY DEFINER
AS $$
  SELECT * FROM public.tecnic t WHERE t.user_id = auth.uid() LIMIT 1;
$$;

-- 2) Assegurar grants per authenticated
GRANT USAGE ON SCHEMA public TO authenticated;
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

-- 3) Assegurar grants per anon (necessari per login)
GRANT USAGE ON SCHEMA public TO anon;

-- 4) Assegurar que l'execució de la funció RPC està permesa
GRANT EXECUTE ON FUNCTION public.get_my_tecnic() TO authenticated;

-- 5) Verificació: mostra els tècnics amb user_id
SELECT t.id, t.nom, t.email, t.rol, t.actiu, t.user_id,
       u.email AS auth_email
FROM public.tecnic t
LEFT JOIN auth.users u ON u.id = t.user_id
ORDER BY t.nom;

