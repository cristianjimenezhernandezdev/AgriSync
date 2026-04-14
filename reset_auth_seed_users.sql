-- =========================================================
-- reset_auth_seed_users.sql
-- Neteja usuaris Auth dels seeds basic i demo d'AgriSync
-- Executa'l al SQL Editor de Supabase ABANS de recrear usuaris
-- =========================================================
--
-- Aquest script elimina, si existeixen:
--   - sessions
--   - refresh tokens
--   - factors MFA
--   - one time tokens
--   - identities
--   - usuaris d'auth.users
--
-- Nomes actua sobre els emails coneguts dels seeds del projecte.
-- No esborra altres usuaris del projecte de Supabase.
-- =========================================================

create temporary table if not exists tmp_agrisync_seed_emails (
  email text primary key
) on commit drop;

truncate table tmp_agrisync_seed_emails;

insert into tmp_agrisync_seed_emails (email)
values
  ('admin.test@agrisync.com'),
  ('manager.test@agrisync.com'),
  ('agricola.test@agrisync.com'),
  ('ramader.test@agrisync.com'),
  ('lectura.test@agrisync.com'),
  ('admin.demo@agrisync.com'),
  ('manager.lleida.demo@agrisync.com'),
  ('manager.girona.demo@agrisync.com'),
  ('sergi.agri.demo@agrisync.com'),
  ('marta.ram.demo@agrisync.com'),
  ('laia.comu.demo@agrisync.com'),
  ('nil.shared.demo@agrisync.com'),
  ('joan.agri.demo@agrisync.com'),
  ('anna.ram.demo@agrisync.com'),
  ('lectura.demo@agrisync.com')
on conflict do nothing;

do $$
begin
  if to_regclass('auth.sessions') is not null then
    execute $sql$
      delete from auth.sessions
      where user_id::text in (
        select u.id::text
        from auth.users u
        join tmp_agrisync_seed_emails e on e.email = u.email
      )
    $sql$;
  end if;

  if to_regclass('auth.refresh_tokens') is not null then
    execute $sql$
      delete from auth.refresh_tokens
      where user_id::text in (
        select u.id::text
        from auth.users u
        join tmp_agrisync_seed_emails e on e.email = u.email
      )
    $sql$;
  end if;

  if to_regclass('auth.mfa_factors') is not null then
    execute $sql$
      delete from auth.mfa_factors
      where user_id::text in (
        select u.id::text
        from auth.users u
        join tmp_agrisync_seed_emails e on e.email = u.email
      )
    $sql$;
  end if;

  if to_regclass('auth.one_time_tokens') is not null then
    execute $sql$
      delete from auth.one_time_tokens
      where user_id::text in (
        select u.id::text
        from auth.users u
        join tmp_agrisync_seed_emails e on e.email = u.email
      )
    $sql$;
  end if;

  if to_regclass('auth.identities') is not null then
    execute $sql$
      delete from auth.identities
      where user_id::text in (
        select u.id::text
        from auth.users u
        join tmp_agrisync_seed_emails e on e.email = u.email
      )
    $sql$;
  end if;
end
$$;

do $$
begin
  if to_regclass('public.tecnic') is not null then
    execute $sql$
      update public.tecnic
      set user_id = null
      where email in (select email from tmp_agrisync_seed_emails)
    $sql$;
  end if;
end
$$;

delete from auth.users
where email in (select email from tmp_agrisync_seed_emails);

select
  e.email,
  exists(select 1 from auth.users u where u.email = e.email) as continua_a_auth
from tmp_agrisync_seed_emails e
order by e.email;
