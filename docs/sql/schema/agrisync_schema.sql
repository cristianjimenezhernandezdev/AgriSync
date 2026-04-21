-- =========================================================
-- AgriSync MVP - SQL base per Supabase
-- Reinicia els objectes del projecte i recrea un esquema net
-- =========================================================

-- =========================================================
-- 0) NETEJA INICIAL
-- =========================================================

drop view if exists public.v_titular_access cascade;

drop function if exists public.get_my_tecnic() cascade;
drop function if exists public.audit_fill_actor() cascade;
drop function if exists public.current_oficina_id() cascade;
drop function if exists public.is_admin() cascade;
drop function if exists public.is_oficina_manager() cascade;
drop function if exists public.same_oficina(uuid) cascade;
drop function if exists public.can_manage_office_titular(uuid) cascade;
drop function if exists public.office_has_any_share(uuid, uuid) cascade;
drop function if exists public.office_has_shared_scope(uuid, public.scope_titular, uuid) cascade;
drop function if exists public.can_read_titular(uuid) cascade;
drop function if exists public.can_write_agricola(uuid) cascade;
drop function if exists public.can_write_ramader(uuid) cascade;
drop function if exists public.can_reference_terra(uuid) cascade;

do $$
begin
  if exists (
    select 1
    from pg_proc
    where pronamespace = 'public'::regnamespace
      and proname = 'can_self_update_tecnic'
  ) then
    execute 'drop function if exists public.can_self_update_tecnic(uuid, uuid, uuid, public.rol_global, boolean) cascade';
  end if;
end
$$;

do $$
begin
  if exists (
    select 1
    from pg_type
    where typnamespace = 'public'::regnamespace
      and typname = 'scope_titular'
  ) then
    execute 'drop function if exists public.can_write_scope(uuid, public.scope_titular) cascade';
  end if;
end
$$;

drop table if exists public.entrega_dejeccions cascade;
drop table if exists public.granja_bestiar cascade;
drop table if exists public.fase_productiva cascade;
drop table if exists public.bestiar cascade;
drop table if exists public.granja cascade;
drop table if exists public.aplicacions_fertilitzants cascade;
drop table if exists public.terra cascade;
drop table if exists public.dan_declaracio cascade;
drop table if exists public.oficina_titular_compartit cascade;
drop table if exists public.tecnic_titular cascade;
drop table if exists public.titular cascade;
drop table if exists public.tecnic cascade;
drop table if exists public.oficina cascade;

drop type if exists public.scope_titular cascade;
drop type if exists public.rol_global cascade;
drop type if exists public.zona_nitrogen cascade;

create extension if not exists pgcrypto;

-- =========================================================
-- 1) TYPES
-- =========================================================

create type public.rol_global as enum ('admin', 'oficina_manager', 'tecnic', 'lectura');
create type public.scope_titular as enum ('comu', 'agricola', 'ramader', 'lectura');
create type public.zona_nitrogen as enum ('ZV', 'ZNV');

-- =========================================================
-- 2) HELPERS D'AUDITORIA
-- =========================================================

create or replace function public.audit_fill_actor()
returns trigger
language plpgsql
as $$
begin
  if tg_op = 'INSERT' then
    if new.created_at is null then
      new.created_at = now();
    end if;
    if new.created_by is null then
      new.created_by = auth.uid();
    end if;
    new.updated_at = now();
    new.updated_by = auth.uid();
    return new;
  end if;

  if tg_op = 'UPDATE' then
    new.updated_at = now();
    new.updated_by = auth.uid();
    return new;
  end if;

  return new;
end;
$$;

-- =========================================================
-- 3) TAULES MVP
-- =========================================================

create table public.oficina (
  id uuid primary key default gen_random_uuid(),
  nom text not null unique,
  created_at timestamptz not null default now()
);

create table public.tecnic (
  id uuid primary key default gen_random_uuid(),
  oficina_id uuid not null references public.oficina(id) on delete restrict,
  user_id uuid unique,
  nom text not null,
  email text,
  telefon text,
  rol public.rol_global not null default 'tecnic',
  actiu boolean not null default true,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid
);

create table public.titular (
  id uuid primary key default gen_random_uuid(),
  nif text,
  nom_rao text not null,
  telefon text,
  email text,
  adreca text,
  codi_postal text check (codi_postal is null or codi_postal ~ '^[0-9]{5}$'),
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid
);

create table public.tecnic_titular (
  id uuid primary key default gen_random_uuid(),
  tecnic_id uuid not null references public.tecnic(id) on delete cascade,
  titular_id uuid not null references public.titular(id) on delete cascade,
  scope public.scope_titular not null default 'comu',
  actiu boolean not null default true,
  created_at timestamptz not null default now(),
  created_by uuid,
  unique (tecnic_id, titular_id, scope)
);

create table public.oficina_titular_compartit (
  id uuid primary key default gen_random_uuid(),
  oficina_id uuid not null references public.oficina(id) on delete cascade,
  titular_id uuid not null references public.titular(id) on delete cascade,
  scope public.scope_titular not null default 'lectura',
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  unique (oficina_id, titular_id, scope)
);

create table public.dan_declaracio (
  id uuid primary key default gen_random_uuid(),
  titular_id uuid not null references public.titular(id) on delete cascade,
  campanya integer not null,
  estat text not null default 'en_curs',
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  unique (titular_id, campanya)
);

create table public.terra (
  id uuid primary key default gen_random_uuid(),
  titular_id uuid references public.titular(id) on delete set null,
  mun_codi text not null check (mun_codi ~ '^[0-9]{5}$'),
  poligon integer not null check (poligon > 0),
  parcela integer not null check (parcela > 0),
  recinte integer not null check (recinte > 0),
  codi_sigpac_complet text generated always as (
    mun_codi || ':0:0:' || poligon::text || ':' || parcela::text || ':' || recinte::text
  ) stored,
  municipi_literal text,
  us_sigpac text,
  cultiu text,
  superficie numeric not null check (superficie >= 0),
  zona public.zona_nitrogen not null default 'ZNV',
  limit_kg_n_ha numeric generated always as (
    case
      when zona = 'ZV'::public.zona_nitrogen then 170
      else 190
    end
  ) stored,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  unique (mun_codi, poligon, parcela, recinte)
);

create table public.aplicacions_fertilitzants (
  id uuid primary key default gen_random_uuid(),
  dan_id uuid not null references public.dan_declaracio(id) on delete cascade,
  terra_id uuid not null references public.terra(id) on delete restrict,
  data date not null,
  tipus_fertilitzant text,
  procedencia text,
  volum_m3 numeric check (volum_m3 is null or volum_m3 >= 0),
  kg_n_m3 numeric check (kg_n_m3 is null or kg_n_m3 >= 0),
  kg_n numeric not null default 0 check (kg_n >= 0),
  tecnic_id uuid references public.tecnic(id) on delete set null,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid
);

create table public.granja (
  id uuid primary key default gen_random_uuid(),
  titular_id uuid not null references public.titular(id) on delete cascade,
  marca_oficial text not null unique,
  nom text,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid
);

create table public.bestiar (
  id uuid primary key default gen_random_uuid(),
  codi text not null unique,
  descripcio text
);

create table public.fase_productiva (
  id uuid primary key default gen_random_uuid(),
  codi text not null unique,
  descripcio text
);

create table public.granja_bestiar (
  id uuid primary key default gen_random_uuid(),
  granja_id uuid not null references public.granja(id) on delete cascade,
  bestiar_id uuid not null references public.bestiar(id) on delete restrict,
  fase_productiva_id uuid not null references public.fase_productiva(id) on delete restrict,
  cens numeric not null default 0 check (cens >= 0),
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  unique (granja_id, bestiar_id, fase_productiva_id)
);

create table public.entrega_dejeccions (
  id uuid primary key default gen_random_uuid(),
  dan_id uuid not null references public.dan_declaracio(id) on delete cascade,
  granja_origen_id uuid not null references public.granja(id) on delete restrict,
  data date not null,
  quantitat numeric not null default 0 check (quantitat >= 0),
  terra_desti_id uuid references public.terra(id) on delete set null,
  receptor_titular_id uuid references public.titular(id) on delete set null,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  check (
    (terra_desti_id is not null and receptor_titular_id is null)
    or
    (terra_desti_id is null and receptor_titular_id is not null)
  )
);

create index idx_tecnic_oficina_id on public.tecnic(oficina_id);
create index idx_tecnic_user_id on public.tecnic(user_id);
create index idx_tecnic_email on public.tecnic(email);
create index idx_tecnic_telefon on public.tecnic(telefon);
create index idx_tecnic_titular_tecnic on public.tecnic_titular(tecnic_id);
create index idx_tecnic_titular_titular on public.tecnic_titular(titular_id);
create index idx_titular_nif on public.titular(nif);
create index idx_titular_telefon on public.titular(telefon);
create index idx_titular_email on public.titular(email);
create index idx_titular_codi_postal on public.titular(codi_postal);
create index idx_oficina_titular_compartit_oficina on public.oficina_titular_compartit(oficina_id);
create index idx_oficina_titular_compartit_titular on public.oficina_titular_compartit(titular_id);
create index idx_dan_titular on public.dan_declaracio(titular_id);
create index idx_terra_titular on public.terra(titular_id);
create index idx_aplicacions_dan on public.aplicacions_fertilitzants(dan_id);
create index idx_aplicacions_terra on public.aplicacions_fertilitzants(terra_id);
create index idx_granja_titular on public.granja(titular_id);
create index idx_gb_granja on public.granja_bestiar(granja_id);
create index idx_entrega_dan on public.entrega_dejeccions(dan_id);

-- =========================================================
-- 4) TRIGGERS
-- =========================================================

create trigger trg_tecnic_actor
before insert or update on public.tecnic
for each row execute function public.audit_fill_actor();

create trigger trg_titular_actor
before insert or update on public.titular
for each row execute function public.audit_fill_actor();

create trigger trg_oficina_titular_compartit_actor
before insert or update on public.oficina_titular_compartit
for each row execute function public.audit_fill_actor();

create trigger trg_dan_actor
before insert or update on public.dan_declaracio
for each row execute function public.audit_fill_actor();

create trigger trg_terra_actor
before insert or update on public.terra
for each row execute function public.audit_fill_actor();

create trigger trg_aplicacions_actor
before insert or update on public.aplicacions_fertilitzants
for each row execute function public.audit_fill_actor();

create trigger trg_granja_actor
before insert or update on public.granja
for each row execute function public.audit_fill_actor();

create trigger trg_granja_bestiar_actor
before insert or update on public.granja_bestiar
for each row execute function public.audit_fill_actor();

create trigger trg_entrega_actor
before insert or update on public.entrega_dejeccions
for each row execute function public.audit_fill_actor();

-- =========================================================
-- 5) HELPERS DE SEGURETAT
-- =========================================================

create or replace function public.get_my_tecnic()
returns setof public.tecnic
language sql
stable
security definer
set search_path = public
as $$
  select *
  from public.tecnic t
  where t.user_id = auth.uid()
  limit 1;
$$;

create or replace function public.current_oficina_id()
returns uuid
language sql
stable
security definer
set search_path = public
as $$
  select t.oficina_id
  from public.tecnic t
  where t.user_id = auth.uid()
    and t.actiu = true
  limit 1;
$$;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.tecnic t
    where t.user_id = auth.uid()
      and t.actiu = true
      and t.rol = 'admin'
  );
$$;

create or replace function public.is_oficina_manager()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.tecnic t
    where t.user_id = auth.uid()
      and t.actiu = true
      and t.rol = 'oficina_manager'
  );
$$;

create or replace function public.same_oficina(p_tecnic_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.tecnic t
    where t.id = p_tecnic_id
      and t.oficina_id = public.current_oficina_id()
  );
$$;

create or replace function public.can_view_tecnic(p_tecnic_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select
    public.is_admin()
    or (public.is_oficina_manager() and public.same_oficina(p_tecnic_id))
    or exists (
      select 1
      from public.tecnic t
      where t.id = p_tecnic_id
        and t.user_id = auth.uid()
    )
    or exists (
      select 1
      from public.tecnic_titular tt
      where tt.tecnic_id = p_tecnic_id
        and tt.actiu = true
        and public.can_read_titular(tt.titular_id)
    );
$$;

create or replace function public.can_view_oficina(p_oficina_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select
    public.is_admin()
    or p_oficina_id = public.current_oficina_id()
    or exists (
      select 1
      from public.tecnic t
      join public.tecnic_titular tt on tt.tecnic_id = t.id
      where t.oficina_id = p_oficina_id
        and t.actiu = true
        and tt.actiu = true
        and public.can_read_titular(tt.titular_id)
    )
    or exists (
      select 1
      from public.oficina_titular_compartit otc
      where otc.oficina_id = p_oficina_id
        and public.can_read_titular(otc.titular_id)
    );
$$;

create or replace function public.can_self_update_tecnic(
  p_tecnic_id uuid,
  p_user_id uuid,
  p_oficina_id uuid,
  p_rol public.rol_global,
  p_actiu boolean
)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.tecnic t
    where t.id = p_tecnic_id
      and t.user_id = auth.uid()
      and t.user_id is not distinct from p_user_id
      and t.oficina_id = p_oficina_id
      and t.rol = p_rol
      and t.actiu = p_actiu
  );
$$;

create or replace function public.can_manage_office_titular(p_titular_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.titular ti
    where ti.id = p_titular_id
      and (
        ti.created_by = auth.uid()
        or exists (
          select 1
          from public.tecnic_titular tt
          join public.tecnic t on t.id = tt.tecnic_id
          where tt.titular_id = p_titular_id
            and tt.actiu = true
            and t.actiu = true
            and t.oficina_id = public.current_oficina_id()
            and not exists (
              select 1
              from public.oficina_titular_compartit otc
              where otc.titular_id = p_titular_id
                and otc.oficina_id = public.current_oficina_id()
            )
        )
      )
  );
$$;

create or replace function public.office_has_any_share(
  p_titular_id uuid,
  p_oficina_id uuid default public.current_oficina_id()
)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.oficina_titular_compartit otc
    where otc.titular_id = p_titular_id
      and otc.oficina_id = p_oficina_id
  );
$$;

create or replace function public.office_has_shared_scope(
  p_titular_id uuid,
  p_scope public.scope_titular,
  p_oficina_id uuid default public.current_oficina_id()
)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.oficina_titular_compartit otc
    where otc.titular_id = p_titular_id
      and otc.oficina_id = p_oficina_id
      and (
        otc.scope = 'comu'
        or otc.scope = p_scope
      )
  );
$$;

create or replace function public.can_read_titular(p_titular_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select
    public.is_admin()
    or (public.is_oficina_manager() and public.can_manage_office_titular(p_titular_id))
    or (public.is_oficina_manager() and public.office_has_any_share(p_titular_id))
    or exists (
      select 1
      from public.tecnic_titular tt
      join public.tecnic t on t.id = tt.tecnic_id
      where t.user_id = auth.uid()
        and t.actiu = true
        and tt.titular_id = p_titular_id
        and tt.actiu = true
    );
$$;

create or replace function public.can_write_scope(p_titular_id uuid, p_scope public.scope_titular)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select
    public.is_admin()
    or (public.is_oficina_manager() and public.can_manage_office_titular(p_titular_id))
    or (public.is_oficina_manager() and public.office_has_shared_scope(p_titular_id, p_scope))
    or exists (
      select 1
      from public.tecnic_titular tt
      join public.tecnic t on t.id = tt.tecnic_id
      where t.user_id = auth.uid()
        and t.actiu = true
        and tt.titular_id = p_titular_id
        and tt.actiu = true
        and (tt.scope = 'comu' or tt.scope = p_scope)
    );
$$;

create or replace function public.can_write_agricola(p_titular_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.can_write_scope(p_titular_id, 'agricola'::public.scope_titular);
$$;

create or replace function public.can_write_ramader(p_titular_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.can_write_scope(p_titular_id, 'ramader'::public.scope_titular);
$$;

create or replace function public.can_reference_terra(p_terra_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.terra te
    where te.id = p_terra_id
      and te.titular_id is not null
      and public.can_read_titular(te.titular_id)
  );
$$;

grant execute on function public.get_my_tecnic() to authenticated;
grant execute on function public.can_self_update_tecnic(uuid, uuid, uuid, public.rol_global, boolean) to authenticated;
grant execute on function public.can_view_tecnic(uuid) to authenticated;
grant execute on function public.can_view_oficina(uuid) to authenticated;

-- =========================================================
-- 6) GRANTS
-- =========================================================

grant select, insert, update, delete on public.oficina to authenticated;
grant select, insert, update, delete on public.tecnic to authenticated;
grant select, insert, update, delete on public.titular to authenticated;
grant select, insert, update, delete on public.tecnic_titular to authenticated;
grant select, insert, update, delete on public.oficina_titular_compartit to authenticated;
grant select, insert, update, delete on public.dan_declaracio to authenticated;
grant select, insert, update, delete on public.terra to authenticated;
grant select, insert, update, delete on public.aplicacions_fertilitzants to authenticated;
grant select, insert, update, delete on public.granja to authenticated;
grant select, insert, update, delete on public.bestiar to authenticated;
grant select, insert, update, delete on public.fase_productiva to authenticated;
grant select, insert, update, delete on public.granja_bestiar to authenticated;
grant select, insert, update, delete on public.entrega_dejeccions to authenticated;

-- =========================================================
-- 7) RLS
-- =========================================================

alter table public.oficina enable row level security;
alter table public.tecnic enable row level security;
alter table public.titular enable row level security;
alter table public.tecnic_titular enable row level security;
alter table public.oficina_titular_compartit enable row level security;
alter table public.dan_declaracio enable row level security;
alter table public.terra enable row level security;
alter table public.aplicacions_fertilitzants enable row level security;
alter table public.granja enable row level security;
alter table public.bestiar enable row level security;
alter table public.fase_productiva enable row level security;
alter table public.granja_bestiar enable row level security;
alter table public.entrega_dejeccions enable row level security;

-- OFICINA
create policy oficina_select on public.oficina
for select to authenticated
using (public.can_view_oficina(oficina.id));

create policy oficina_insert on public.oficina
for insert to authenticated
with check (public.is_admin());

create policy oficina_update on public.oficina
for update to authenticated
using (public.is_admin())
with check (public.is_admin());

create policy oficina_delete on public.oficina
for delete to authenticated
using (public.is_admin());

-- TECNIC
create policy tecnic_select on public.tecnic
for select to authenticated
using (public.can_view_tecnic(tecnic.id));

create policy tecnic_insert on public.tecnic
for insert to authenticated
with check (
  public.is_admin()
  or (public.is_oficina_manager() and oficina_id = public.current_oficina_id())
);

create policy tecnic_update on public.tecnic
for update to authenticated
using (
  public.is_admin()
  or (public.is_oficina_manager() and public.same_oficina(tecnic.id))
  or public.can_self_update_tecnic(tecnic.id, tecnic.user_id, tecnic.oficina_id, tecnic.rol, tecnic.actiu)
)
with check (
  public.is_admin()
  or (public.is_oficina_manager() and oficina_id = public.current_oficina_id())
  or public.can_self_update_tecnic(id, user_id, oficina_id, rol, actiu)
);

create policy tecnic_delete on public.tecnic
for delete to authenticated
using (
  public.is_admin()
  or (public.is_oficina_manager() and public.same_oficina(tecnic.id))
);

-- TITULAR
create policy titular_select on public.titular
for select to authenticated
using (public.can_read_titular(titular.id));

create policy titular_insert on public.titular
for insert to authenticated
with check (public.is_admin() or public.is_oficina_manager());

create policy titular_update on public.titular
for update to authenticated
using (public.can_write_scope(titular.id, 'comu'::public.scope_titular))
with check (public.can_write_scope(titular.id, 'comu'::public.scope_titular));

create policy titular_delete on public.titular
for delete to authenticated
using (
  public.is_admin()
  or (public.is_oficina_manager() and public.can_manage_office_titular(titular.id))
);

-- TECNIC_TITULAR
create policy tecnic_titular_select on public.tecnic_titular
for select to authenticated
using (
  public.is_admin()
  or (public.is_oficina_manager() and public.same_oficina(tecnic_titular.tecnic_id))
  or public.can_read_titular(tecnic_titular.titular_id)
  or exists (
    select 1
    from public.tecnic t
    where t.id = tecnic_titular.tecnic_id
      and t.user_id = auth.uid()
      and t.actiu = true
  )
);

create policy tecnic_titular_insert on public.tecnic_titular
for insert to authenticated
with check (
  public.is_admin()
  or (
    public.is_oficina_manager()
    and public.same_oficina(tecnic_id)
    and (
      public.can_manage_office_titular(titular_id)
      or public.office_has_shared_scope(titular_id, scope)
    )
  )
);

create policy tecnic_titular_update on public.tecnic_titular
for update to authenticated
using (
  public.is_admin()
  or (
    public.is_oficina_manager()
    and public.same_oficina(tecnic_titular.tecnic_id)
    and (
      public.can_manage_office_titular(tecnic_titular.titular_id)
      or public.office_has_shared_scope(tecnic_titular.titular_id, tecnic_titular.scope)
    )
  )
)
with check (
  public.is_admin()
  or (
    public.is_oficina_manager()
    and public.same_oficina(tecnic_id)
    and (
      public.can_manage_office_titular(titular_id)
      or public.office_has_shared_scope(titular_id, scope)
    )
  )
);

create policy tecnic_titular_delete on public.tecnic_titular
for delete to authenticated
using (
  public.is_admin()
  or (
    public.is_oficina_manager()
    and public.same_oficina(tecnic_titular.tecnic_id)
    and (
      public.can_manage_office_titular(tecnic_titular.titular_id)
      or public.office_has_shared_scope(tecnic_titular.titular_id, tecnic_titular.scope)
    )
  )
);

-- OFICINA_TITULAR_COMPARTIT
create policy oficina_titular_compartit_select on public.oficina_titular_compartit
for select to authenticated
using (
  public.is_admin()
  or public.can_read_titular(oficina_titular_compartit.titular_id)
  or (
    public.is_oficina_manager()
    and (
      oficina_titular_compartit.oficina_id = public.current_oficina_id()
      or public.can_manage_office_titular(oficina_titular_compartit.titular_id)
    )
  )
);

create policy oficina_titular_compartit_insert on public.oficina_titular_compartit
for insert to authenticated
with check (
  public.is_admin()
  or (
    public.is_oficina_manager()
    and public.can_manage_office_titular(titular_id)
  )
);

create policy oficina_titular_compartit_update on public.oficina_titular_compartit
for update to authenticated
using (
  public.is_admin()
  or (
    public.is_oficina_manager()
    and public.can_manage_office_titular(oficina_titular_compartit.titular_id)
  )
)
with check (
  public.is_admin()
  or (
    public.is_oficina_manager()
    and public.can_manage_office_titular(titular_id)
  )
);

create policy oficina_titular_compartit_delete on public.oficina_titular_compartit
for delete to authenticated
using (
  public.is_admin()
  or (
    public.is_oficina_manager()
    and public.can_manage_office_titular(oficina_titular_compartit.titular_id)
  )
);

-- DAN
create policy dan_select on public.dan_declaracio
for select to authenticated
using (public.can_read_titular(dan_declaracio.titular_id));

create policy dan_insert on public.dan_declaracio
for insert to authenticated
with check (
  public.can_write_agricola(titular_id)
  or public.can_write_ramader(titular_id)
);

create policy dan_update on public.dan_declaracio
for update to authenticated
using (
  public.can_write_agricola(dan_declaracio.titular_id)
  or public.can_write_ramader(dan_declaracio.titular_id)
)
with check (
  public.can_write_agricola(titular_id)
  or public.can_write_ramader(titular_id)
);

create policy dan_delete on public.dan_declaracio
for delete to authenticated
using (
  public.is_admin()
  or (public.is_oficina_manager() and public.can_manage_office_titular(dan_declaracio.titular_id))
);

-- TERRA
create policy terra_select on public.terra
for select to authenticated
using (
  public.is_admin()
  or (public.is_oficina_manager() and terra.titular_id is null and terra.created_by = auth.uid())
  or (terra.titular_id is not null and public.can_read_titular(terra.titular_id))
);

create policy terra_insert on public.terra
for insert to authenticated
with check (
  public.is_admin()
  or (public.is_oficina_manager() and (titular_id is null or public.can_manage_office_titular(titular_id)))
  or (titular_id is not null and public.can_write_agricola(titular_id))
);

create policy terra_update on public.terra
for update to authenticated
using (
  public.is_admin()
  or (public.is_oficina_manager() and terra.titular_id is null and terra.created_by = auth.uid())
  or (terra.titular_id is not null and public.can_write_agricola(terra.titular_id))
)
with check (
  public.is_admin()
  or (public.is_oficina_manager() and (titular_id is null or public.can_manage_office_titular(titular_id)))
  or (titular_id is not null and public.can_write_agricola(titular_id))
);

create policy terra_delete on public.terra
for delete to authenticated
using (
  public.is_admin()
  or (public.is_oficina_manager() and terra.titular_id is null and terra.created_by = auth.uid())
  or (terra.titular_id is not null and public.can_write_agricola(terra.titular_id))
);

-- APLICACIONS_FERTILITZANTS
create policy aplicacions_select on public.aplicacions_fertilitzants
for select to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id = aplicacions_fertilitzants.dan_id
      and public.can_read_titular(d.titular_id)
  )
);

create policy aplicacions_insert on public.aplicacions_fertilitzants
for insert to authenticated
with check (
  public.is_admin()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id = dan_id
      and public.can_write_agricola(d.titular_id)
  )
);

create policy aplicacions_update on public.aplicacions_fertilitzants
for update to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id = aplicacions_fertilitzants.dan_id
      and public.can_write_agricola(d.titular_id)
  )
)
with check (
  public.is_admin()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id = dan_id
      and public.can_write_agricola(d.titular_id)
  )
);

create policy aplicacions_delete on public.aplicacions_fertilitzants
for delete to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id = aplicacions_fertilitzants.dan_id
      and public.can_write_agricola(d.titular_id)
  )
);

-- GRANJA
create policy granja_select on public.granja
for select to authenticated
using (
  public.is_admin()
  or public.can_read_titular(granja.titular_id)
);

create policy granja_insert on public.granja
for insert to authenticated
with check (
  public.is_admin()
  or public.can_write_ramader(titular_id)
);

create policy granja_update on public.granja
for update to authenticated
using (
  public.is_admin()
  or public.can_write_ramader(granja.titular_id)
)
with check (
  public.is_admin()
  or public.can_write_ramader(titular_id)
);

create policy granja_delete on public.granja
for delete to authenticated
using (
  public.is_admin()
  or public.can_write_ramader(granja.titular_id)
);

-- BESTIAR
create policy bestiar_select on public.bestiar
for select to authenticated
using (true);

create policy bestiar_insert on public.bestiar
for insert to authenticated
with check (public.is_admin());

create policy bestiar_update on public.bestiar
for update to authenticated
using (public.is_admin())
with check (public.is_admin());

create policy bestiar_delete on public.bestiar
for delete to authenticated
using (public.is_admin());

-- FASE_PRODUCTIVA
create policy fase_select on public.fase_productiva
for select to authenticated
using (true);

create policy fase_insert on public.fase_productiva
for insert to authenticated
with check (public.is_admin());

create policy fase_update on public.fase_productiva
for update to authenticated
using (public.is_admin())
with check (public.is_admin());

create policy fase_delete on public.fase_productiva
for delete to authenticated
using (public.is_admin());

-- GRANJA_BESTIAR
create policy gb_select on public.granja_bestiar
for select to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.granja g
    where g.id = granja_bestiar.granja_id
      and public.can_read_titular(g.titular_id)
  )
);

create policy gb_insert on public.granja_bestiar
for insert to authenticated
with check (
  public.is_admin()
  or exists (
    select 1
    from public.granja g
    where g.id = granja_id
      and public.can_write_ramader(g.titular_id)
  )
);

create policy gb_update on public.granja_bestiar
for update to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.granja g
    where g.id = granja_bestiar.granja_id
      and public.can_write_ramader(g.titular_id)
  )
)
with check (
  public.is_admin()
  or exists (
    select 1
    from public.granja g
    where g.id = granja_id
      and public.can_write_ramader(g.titular_id)
  )
);

create policy gb_delete on public.granja_bestiar
for delete to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.granja g
    where g.id = granja_bestiar.granja_id
      and public.can_write_ramader(g.titular_id)
  )
);

-- ENTREGA_DEJECCIONS
create policy entrega_select on public.entrega_dejeccions
for select to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id = entrega_dejeccions.dan_id
      and public.can_read_titular(d.titular_id)
  )
  or (
    entrega_dejeccions.receptor_titular_id is not null
    and public.can_read_titular(entrega_dejeccions.receptor_titular_id)
  )
  or exists (
    select 1
    from public.terra te
    where te.id = entrega_dejeccions.terra_desti_id
      and te.titular_id is not null
      and public.can_read_titular(te.titular_id)
  )
);

create policy entrega_insert on public.entrega_dejeccions
for insert to authenticated
with check (
  public.is_admin()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id = dan_id
      and public.can_write_ramader(d.titular_id)
  )
  and (terra_desti_id is null or public.can_reference_terra(terra_desti_id))
  and (receptor_titular_id is null or public.can_read_titular(receptor_titular_id))
);

create policy entrega_update on public.entrega_dejeccions
for update to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id = entrega_dejeccions.dan_id
      and public.can_write_ramader(d.titular_id)
  )
)
with check (
  public.is_admin()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id = dan_id
      and public.can_write_ramader(d.titular_id)
  )
  and (terra_desti_id is null or public.can_reference_terra(terra_desti_id))
  and (receptor_titular_id is null or public.can_read_titular(receptor_titular_id))
);

create policy entrega_delete on public.entrega_dejeccions
for delete to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id = entrega_dejeccions.dan_id
      and public.can_write_ramader(d.titular_id)
  )
);

-- =========================================================
-- NOTES MVP
-- ---------------------------------------------------------
-- Es mantenen només les entitats que la app actual fa servir.
-- S'han tret elements que ara mateix no aporten valor directe:
--   - public.v_titular_access
--   - helper public.current_tecnic_id()
--
-- Les columnes tipus "kg N/ha" es poden calcular,
-- no cal guardar-les duplicades a base de dades.
-- =========================================================
