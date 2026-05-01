-- =========================================================
-- reset_auth_seed_users.sql
-- Neteja COMPLETAMENT Supabase Auth del projecte actual
-- Executa'l al SQL Editor de Supabase ABANS de recrear usuaris
-- =========================================================
--
-- ATENCIO:
--   Aquest script es destructiu.
--   Elimina TOTS els usuaris d'auth.users del projecte actual,
--   no nomes els del seed demo.
--
--   També elimina, si existeixen:
--   - sessions
--   - refresh tokens
--   - factors MFA
--   - one time tokens
--   - identities
--
--   I deixa a null tots els user_id de public.tecnic
--   per evitar referencies trencades cap a Auth.
-- =========================================================

do $$
begin
  if to_regclass('auth.sessions') is not null then
    execute 'delete from auth.sessions';
  end if;

  if to_regclass('auth.refresh_tokens') is not null then
    execute 'delete from auth.refresh_tokens';
  end if;

  if to_regclass('auth.mfa_factors') is not null then
    execute 'delete from auth.mfa_factors';
  end if;

  if to_regclass('auth.one_time_tokens') is not null then
    execute 'delete from auth.one_time_tokens';
  end if;

  if to_regclass('auth.identities') is not null then
    execute 'delete from auth.identities';
  end if;
end
$$;

do $$
begin
  if to_regclass('public.tecnic') is not null then
    execute 'update public.tecnic set user_id = null where user_id is not null';
  end if;
end
$$;

delete from auth.users;

select
  (select count(*) from auth.users) as auth_users_restants,
  (select count(*) from public.tecnic where user_id is not null) as tecnics_encara_enllacats;
