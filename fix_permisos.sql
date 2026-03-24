-- =========================================================
-- fix_permisos.sql
-- Reaplica grants i execucio de funcions a l'esquema MVP actual
-- Executa'l nomes si algun permís ha quedat desalineat
-- =========================================================

grant usage on schema public to anon;
grant usage on schema public to authenticated;
grant usage on schema public to service_role;

grant all on all tables in schema public to service_role;
grant all on all sequences in schema public to service_role;

grant select, insert, update, delete on public.oficina to authenticated;
grant select, insert, update, delete on public.tecnic to authenticated;
grant select, insert, update, delete on public.titular to authenticated;
grant select, insert, update, delete on public.tecnic_titular to authenticated;
grant select, insert, update, delete on public.dan_declaracio to authenticated;
grant select, insert, update, delete on public.terra to authenticated;
grant select, insert, update, delete on public.aplicacions_fertilitzants to authenticated;
grant select, insert, update, delete on public.granja to authenticated;
grant select, insert, update, delete on public.bestiar to authenticated;
grant select, insert, update, delete on public.fase_productiva to authenticated;
grant select, insert, update, delete on public.granja_bestiar to authenticated;
grant select, insert, update, delete on public.entrega_dejeccions to authenticated;

grant execute on function public.get_my_tecnic() to authenticated;
grant execute on function public.current_oficina_id() to authenticated;
grant execute on function public.is_admin() to authenticated;
grant execute on function public.is_oficina_manager() to authenticated;
grant execute on function public.same_oficina(uuid) to authenticated;
grant execute on function public.can_read_titular(uuid) to authenticated;
grant execute on function public.can_write_scope(uuid, public.scope_titular) to authenticated;
grant execute on function public.can_write_agricola(uuid) to authenticated;
grant execute on function public.can_write_ramader(uuid) to authenticated;

grant execute on all functions in schema public to service_role;

select 'grants_reaplicats' as resultat;
