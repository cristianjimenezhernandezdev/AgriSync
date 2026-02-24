-- =========================================================
-- 00_init_agrisync_supabase_ok.sql
-- (enganxa sencer al SQL Editor de Supabase)
-- =========================================================

-- 1) Extensions
create extension if not exists pgcrypto;

-- 2) Types (enums)
do $$ begin
  create type rol_global as enum ('admin', 'oficina_manager', 'tecnic', 'lectura');
exception when duplicate_object then null; end $$;

do $$ begin
  create type scope_titular as enum ('comu', 'agricola', 'ramader', 'lectura');
exception when duplicate_object then null; end $$;

-- 3) Trigger helper: fill created_by/updated_by using auth.uid()
create or replace function public.audit_fill_actor()
returns trigger
language plpgsql
as $$
begin
  if tg_op = 'INSERT' then
    if new.created_at is null then new.created_at = now(); end if;
    if new.created_by is null then new.created_by = auth.uid(); end if;
    new.updated_at = now();
    new.updated_by = auth.uid();
    return new;
  elsif tg_op = 'UPDATE' then
    new.updated_at = now();
    new.updated_by = auth.uid();
    return new;
  end if;

  return new;
end $$;

-- =========================================================
-- TABLES
-- =========================================================

create table if not exists public.oficina (
  id uuid primary key default gen_random_uuid(),
  nom text not null unique,
  created_at timestamptz not null default now()
);

create table if not exists public.tecnic (
  id uuid primary key default gen_random_uuid(),
  oficina_id uuid not null references public.oficina(id),
  user_id uuid unique, -- auth.users.id
  nom text not null,
  email text,
  rol rol_global not null default 'tecnic',
  actiu boolean not null default true,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid
);

create table if not exists public.titular (
  id uuid primary key default gen_random_uuid(),
  nif text,
  nom_rao text not null,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid
);

create table if not exists public.tecnic_titular (
  id uuid primary key default gen_random_uuid(),
  tecnic_id uuid not null references public.tecnic(id) on delete cascade,
  titular_id uuid not null references public.titular(id) on delete cascade,
  scope scope_titular not null default 'comu',
  actiu boolean not null default true,
  created_at timestamptz not null default now(),
  created_by uuid,
  unique (tecnic_id, titular_id, scope)
);

create table if not exists public.dan_declaracio (
  id uuid primary key default gen_random_uuid(),
  titular_id uuid not null references public.titular(id) on delete cascade,
  campanya int not null,
  estat text not null default 'en_curs',
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  unique (titular_id, campanya)
);

create table if not exists public.terra (
  id uuid primary key default gen_random_uuid(),

  titular_id uuid references public.titular(id),

  -- Parts SIGPAC
  mun_codi text not null check (mun_codi ~ '^[0-9]{5}$'),  -- ex: 17071
  poligon integer not null check (poligon > 0),
  parcela integer not null check (parcela > 0),
  recinte integer not null check (recinte > 0),

  -- Codi complet derivat de les parts (NO s'escriu manualment)
  codi_sigpac_complet text generated always as (
    mun_codi || ':0:0:' || poligon::text || ':' || parcela::text || ':' || recinte::text
  ) stored,

  superficie numeric not null check (superficie >= 0),

  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,

  -- Unicitat real per SIGPAC
  unique (mun_codi, poligon, parcela, recinte)
);

create index if not exists idx_terra_sigpac_parts
on public.terra (mun_codi, poligon, parcela, recinte);

create index if not exists idx_terra_titular
on public.terra (titular_id);


create table if not exists public.cessio_terra (
  id uuid primary key default gen_random_uuid(),
  dan_id uuid not null references public.dan_declaracio(id) on delete cascade,
  terra_id uuid not null references public.terra(id),
  data_inici date not null,
  data_fi date not null,
  titular_explotador_id uuid not null references public.titular(id),
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  check (data_inici <= data_fi)
);

create table if not exists public.aplicacions_fertilitzants (
  id uuid primary key default gen_random_uuid(),
  dan_id uuid not null references public.dan_declaracio(id) on delete cascade,
  terra_id uuid not null references public.terra(id),
  data date not null,
  kg_n numeric not null default 0,
  uf numeric not null default 0,
  tecnic_id uuid references public.tecnic(id),
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid
);

create table if not exists public.granja (
  id uuid primary key default gen_random_uuid(),
  titular_id uuid not null references public.titular(id) on delete cascade,
  marca_oficial text not null,
  nom text,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  unique (marca_oficial)
);

create table if not exists public.bestiar (
  id uuid primary key default gen_random_uuid(),
  codi text not null unique,
  descripcio text
);

create table if not exists public.fase_productiva (
  id uuid primary key default gen_random_uuid(),
  codi text not null unique,
  descripcio text
);

create table if not exists public.granja_bestiar (
  id uuid primary key default gen_random_uuid(),
  granja_id uuid not null references public.granja(id) on delete cascade,
  bestiar_id uuid not null references public.bestiar(id),
  fase_productiva_id uuid not null references public.fase_productiva(id),
  cens numeric not null default 0,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  unique (granja_id, bestiar_id, fase_productiva_id)
);

create table if not exists public.emmagatzematge (
  id uuid primary key default gen_random_uuid(),
  granja_id uuid not null references public.granja(id) on delete cascade,
  tipus text not null,
  capacitat numeric not null default 0,
  info_extra text,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid
);

create table if not exists public.entrega_dejeccions (
  id uuid primary key default gen_random_uuid(),
  dan_id uuid not null references public.dan_declaracio(id) on delete cascade,
  granja_origen_id uuid not null references public.granja(id),
  data date not null,
  quantitat numeric not null default 0,
  terra_desti_id uuid references public.terra(id),
  receptor_titular_id uuid references public.titular(id),
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



-- =========================================================
-- TRIGGERS
-- =========================================================
do $$ begin
  create trigger trg_tecnic_actor
  before insert or update on public.tecnic
  for each row execute function public.audit_fill_actor();
exception when duplicate_object then null; end $$;

do $$ begin
  create trigger trg_titular_actor
  before insert or update on public.titular
  for each row execute function public.audit_fill_actor();
exception when duplicate_object then null; end $$;

do $$ begin
  create trigger trg_dan_actor
  before insert or update on public.dan_declaracio
  for each row execute function public.audit_fill_actor();
exception when duplicate_object then null; end $$;

do $$ begin
  create trigger trg_terra_actor
  before insert or update on public.terra
  for each row execute function public.audit_fill_actor();
exception when duplicate_object then null; end $$;

do $$ begin
  create trigger trg_cessio_actor
  before insert or update on public.cessio_terra
  for each row execute function public.audit_fill_actor();
exception when duplicate_object then null; end $$;

do $$ begin
  create trigger trg_aplicacions_actor
  before insert or update on public.aplicacions_fertilitzants
  for each row execute function public.audit_fill_actor();
exception when duplicate_object then null; end $$;

do $$ begin
  create trigger trg_granja_actor
  before insert or update on public.granja
  for each row execute function public.audit_fill_actor();
exception when duplicate_object then null; end $$;

do $$ begin
  create trigger trg_granja_bestiar_actor
  before insert or update on public.granja_bestiar
  for each row execute function public.audit_fill_actor();
exception when duplicate_object then null; end $$;

do $$ begin
  create trigger trg_emmagatzematge_actor
  before insert or update on public.emmagatzematge
  for each row execute function public.audit_fill_actor();
exception when duplicate_object then null; end $$;

do $$ begin
  create trigger trg_entrega_actor
  before insert or update on public.entrega_dejeccions
  for each row execute function public.audit_fill_actor();
exception when duplicate_object then null; end $$;

-- =========================================================
-- RLS HELPERS
-- =========================================================

create or replace function public.current_oficina_id()
returns uuid
language sql
stable
as $$
  select t.oficina_id
  from public.tecnic t
  where t.user_id = auth.uid()
    and t.actiu = true
$$;

create or replace function public.current_tecnic_id()
returns uuid
language sql
stable
as $$
  select t.id
  from public.tecnic t
  where t.user_id = auth.uid()
    and t.actiu = true
$$;

create or replace function public.is_admin()
returns boolean
language sql
stable
as $$
  select exists(
    select 1
    from public.tecnic t
    where t.user_id = auth.uid()
      and t.actiu = true
      and t.rol = 'admin'
  )
$$;

create or replace function public.is_oficina_manager()
returns boolean
language sql
stable
as $$
  select exists(
    select 1
    from public.tecnic t
    where t.user_id = auth.uid()
      and t.actiu = true
      and t.rol = 'oficina_manager'
  )
$$;

create or replace function public.same_oficina(p_tecnic_id uuid)
returns boolean
language sql
stable
as $$
  select exists(
    select 1
    from public.tecnic t
    where t.id = p_tecnic_id
      and t.oficina_id = public.current_oficina_id()
  )
$$;

create or replace function public.can_read_titular(p_titular_id uuid)
returns boolean
language sql
stable
as $$
  select
    public.is_admin()
    or public.is_oficina_manager()
    or exists (
      select 1
      from public.tecnic_titular tt
      join public.tecnic t on t.id = tt.tecnic_id
      where t.user_id = auth.uid()
        and t.actiu = true
        and tt.titular_id = p_titular_id
        and tt.actiu = true
    )
$$;

create or replace function public.can_write_scope(p_titular_id uuid, p_scope scope_titular)
returns boolean
language sql
stable
as $$
  select
    public.is_admin()
    or exists (
      select 1
      from public.tecnic_titular tt
      join public.tecnic t on t.id = tt.tecnic_id
      where t.user_id = auth.uid()
        and t.actiu = true
        and tt.titular_id = p_titular_id
        and tt.actiu = true
        and (tt.scope = 'comu' or tt.scope = p_scope)
    )
$$;

create or replace function public.can_write_agricola(p_titular_id uuid)
returns boolean
language sql
stable
as $$
  select public.can_write_scope(p_titular_id, 'agricola'::scope_titular)
$$;

create or replace function public.can_write_ramader(p_titular_id uuid)
returns boolean
language sql
stable
as $$
  select public.can_write_scope(p_titular_id, 'ramader'::scope_titular)
$$;

-- =========================================================
-- ENABLE RLS
-- =========================================================
alter table public.oficina enable row level security;
alter table public.tecnic enable row level security;
alter table public.titular enable row level security;
alter table public.tecnic_titular enable row level security;
alter table public.dan_declaracio enable row level security;
alter table public.terra enable row level security;
alter table public.cessio_terra enable row level security;
alter table public.aplicacions_fertilitzants enable row level security;
alter table public.granja enable row level security;
alter table public.bestiar enable row level security;
alter table public.fase_productiva enable row level security;
alter table public.granja_bestiar enable row level security;
alter table public.emmagatzematge enable row level security;
alter table public.entrega_dejeccions enable row level security;


-- =========================================================
-- POLICIES (totes separades per operació)
-- =========================================================

-- ---------- OFICINA ----------
drop policy if exists oficina_select on public.oficina;
create policy oficina_select on public.oficina
for select to authenticated
using (public.is_admin() or public.is_oficina_manager());

drop policy if exists oficina_insert on public.oficina;
create policy oficina_insert on public.oficina
for insert to authenticated
with check (public.is_admin());

drop policy if exists oficina_update on public.oficina;
create policy oficina_update on public.oficina
for update to authenticated
using (public.is_admin())
with check (public.is_admin());

drop policy if exists oficina_delete on public.oficina;
create policy oficina_delete on public.oficina
for delete to authenticated
using (public.is_admin());

-- ---------- TECNIC ----------
drop policy if exists tecnic_select on public.tecnic;
create policy tecnic_select on public.tecnic
for select to authenticated
using (
  public.is_admin()
  or (public.is_oficina_manager() and public.same_oficina(tecnic.id))
  or (tecnic.user_id = auth.uid())
);

drop policy if exists tecnic_insert on public.tecnic;
create policy tecnic_insert on public.tecnic
for insert to authenticated
with check (
  public.is_admin()
  or (public.is_oficina_manager() and oficina_id = public.current_oficina_id())
);

drop policy if exists tecnic_update on public.tecnic;
create policy tecnic_update on public.tecnic
for update to authenticated
using (
  public.is_admin()
  or (public.is_oficina_manager() and public.same_oficina(tecnic.id))
)
with check (
  public.is_admin()
  or (public.is_oficina_manager() and oficina_id = public.current_oficina_id())
);




drop policy if exists tecnic_delete on public.tecnic;
create policy tecnic_delete on public.tecnic
for delete to authenticated
using (
  public.is_admin()
  or (public.is_oficina_manager() and public.same_oficina(tecnic.id))
);

-- ---------- TITULAR ----------
drop policy if exists titular_select on public.titular;
create policy titular_select on public.titular
for select to authenticated
using (public.can_read_titular(titular.id));

drop policy if exists titular_insert on public.titular;
create policy titular_insert on public.titular
for insert to authenticated
with check (public.is_admin() or public.is_oficina_manager());

drop policy if exists titular_update on public.titular;
create policy titular_update on public.titular
for update to authenticated
using (
  public.is_admin()
  or exists (
    select 1
    from public.tecnic_titular tt
    join public.tecnic t on t.id = tt.tecnic_id
    where t.user_id = auth.uid()
      and t.actiu = true
      and tt.titular_id = titular.id
      and tt.actiu = true
      and tt.scope = 'comu'
  )
)
with check (true);

drop policy if exists titular_delete on public.titular;
create policy titular_delete on public.titular
for delete to authenticated
using (public.is_admin() or public.is_oficina_manager());

-- ---------- TECNIC_TITULAR ----------
drop policy if exists tecnic_titular_select on public.tecnic_titular;
create policy tecnic_titular_select on public.tecnic_titular
for select to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.tecnic t
    where t.id = tecnic_titular.tecnic_id
      and t.user_id = auth.uid()
      and t.actiu = true
  )
);

drop policy if exists tecnic_titular_insert on public.tecnic_titular;
create policy tecnic_titular_insert on public.tecnic_titular
for insert to authenticated
with check (public.is_admin() or public.is_oficina_manager());

drop policy if exists tecnic_titular_update on public.tecnic_titular;
create policy tecnic_titular_update on public.tecnic_titular
for update to authenticated
using (public.is_admin() or public.is_oficina_manager())
with check (public.is_admin() or public.is_oficina_manager());

drop policy if exists tecnic_titular_delete on public.tecnic_titular;
create policy tecnic_titular_delete on public.tecnic_titular
for delete to authenticated
using (public.is_admin() or public.is_oficina_manager());

-- ---------- DAN_DECLARACIO ----------
drop policy if exists dan_select on public.dan_declaracio;
create policy dan_select on public.dan_declaracio
for select to authenticated
using (public.can_read_titular(dan_declaracio.titular_id));

drop policy if exists dan_insert on public.dan_declaracio;
create policy dan_insert on public.dan_declaracio
for insert to authenticated
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or public.can_write_agricola(titular_id)
  or public.can_write_ramader(titular_id)
);

drop policy if exists dan_update on public.dan_declaracio;
create policy dan_update on public.dan_declaracio
for update to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or public.can_write_agricola(dan_declaracio.titular_id)
  or public.can_write_ramader(dan_declaracio.titular_id)
)
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or public.can_write_agricola(dan_declaracio.titular_id)
  or public.can_write_ramader(dan_declaracio.titular_id)
);

drop policy if exists dan_delete on public.dan_declaracio;
create policy dan_delete on public.dan_declaracio
for delete to authenticated
using (public.is_admin() or public.is_oficina_manager());

-- ---------- TERRA ----------
drop policy if exists terra_select on public.terra;
create policy terra_select on public.terra
for select to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or terra.titular_id is null                      -- catàleg visible per tothom
  or public.can_read_titular(terra.titular_id)     -- terres assignades, només si tens accés al titular
);


drop policy if exists terra_insert on public.terra;
create policy terra_insert on public.terra
for insert to authenticated
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or (titular_id is not null and public.can_write_agricola(titular_id))
);

drop policy if exists terra_update on public.terra;
create policy terra_update on public.terra
for update to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or (terra.titular_id is not null and public.can_write_agricola(terra.titular_id))

)
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or (titular_id is not null and public.can_write_agricola(titular_id))
  
);


drop policy if exists terra_delete on public.terra;
create policy terra_delete on public.terra
for delete to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or (terra.titular_id is not null and public.can_write_agricola(terra.titular_id))
);

-- ---------- CESSIO_TERRA ----------
drop policy if exists cessio_select on public.cessio_terra;
create policy cessio_select on public.cessio_terra
for select to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = cessio_terra.dan_id
      and public.can_read_titular(d.titular_id)
  )
);

drop policy if exists cessio_insert on public.cessio_terra;
create policy cessio_insert on public.cessio_terra
for insert to authenticated
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = dan_id
      and public.can_write_agricola(d.titular_id)
  )
);

drop policy if exists cessio_update on public.cessio_terra;
create policy cessio_update on public.cessio_terra
for update to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = cessio_terra.dan_id
      and public.can_write_agricola(d.titular_id)
  )
)
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = cessio_terra.dan_id
      and public.can_write_agricola(d.titular_id)
  )
);

drop policy if exists cessio_delete on public.cessio_terra;
create policy cessio_delete on public.cessio_terra
for delete to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = cessio_terra.dan_id
      and public.can_write_agricola(d.titular_id)
  )
);

-- ---------- APLICACIONS_FERTILITZANTS ----------
drop policy if exists aplicacions_select on public.aplicacions_fertilitzants;
create policy aplicacions_select on public.aplicacions_fertilitzants
for select to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = aplicacions_fertilitzants.dan_id
      and public.can_read_titular(d.titular_id)
  )
);

drop policy if exists aplicacions_insert on public.aplicacions_fertilitzants;
create policy aplicacions_insert on public.aplicacions_fertilitzants
for insert to authenticated
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = dan_id
      and public.can_write_agricola(d.titular_id)
  )
);

drop policy if exists aplicacions_update on public.aplicacions_fertilitzants;
create policy aplicacions_update on public.aplicacions_fertilitzants
for update to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = aplicacions_fertilitzants.dan_id
      and public.can_write_agricola(d.titular_id)
  )
)
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = aplicacions_fertilitzants.dan_id
      and public.can_write_agricola(d.titular_id)
  )
);

drop policy if exists aplicacions_delete on public.aplicacions_fertilitzants;
create policy aplicacions_delete on public.aplicacions_fertilitzants
for delete to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = aplicacions_fertilitzants.dan_id
      and public.can_write_agricola(d.titular_id)
  )
);

-- ---------- GRANJA ----------
drop policy if exists granja_select on public.granja;
create policy granja_select on public.granja
for select to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or public.can_read_titular(granja.titular_id)
);

drop policy if exists granja_insert on public.granja;
create policy granja_insert on public.granja
for insert to authenticated
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or public.can_write_ramader(titular_id)
);

drop policy if exists granja_update on public.granja;
create policy granja_update on public.granja
for update to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or public.can_write_ramader(granja.titular_id)
)
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or public.can_write_ramader(granja.titular_id)
);

drop policy if exists granja_delete on public.granja;
create policy granja_delete on public.granja
for delete to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or public.can_write_ramader(granja.titular_id)
);

-- ---------- BESTIAR (catàleg) ----------
drop policy if exists bestiar_select on public.bestiar;
create policy bestiar_select on public.bestiar
for select to authenticated
using (true);

drop policy if exists bestiar_insert on public.bestiar;
create policy bestiar_insert on public.bestiar
for insert to authenticated
with check (public.is_admin());

drop policy if exists bestiar_update on public.bestiar;
create policy bestiar_update on public.bestiar
for update to authenticated
using (public.is_admin())
with check (public.is_admin());

drop policy if exists bestiar_delete on public.bestiar;
create policy bestiar_delete on public.bestiar
for delete to authenticated
using (public.is_admin());

-- ---------- FASE_PRODUCTIVA (catàleg) ----------
drop policy if exists fase_select on public.fase_productiva;
create policy fase_select on public.fase_productiva
for select to authenticated
using (true);

drop policy if exists fase_insert on public.fase_productiva;
create policy fase_insert on public.fase_productiva
for insert to authenticated
with check (public.is_admin());

drop policy if exists fase_update on public.fase_productiva;
create policy fase_update on public.fase_productiva
for update to authenticated
using (public.is_admin())
with check (public.is_admin());

drop policy if exists fase_delete on public.fase_productiva;
create policy fase_delete on public.fase_productiva
for delete to authenticated
using (public.is_admin());

-- ---------- GRANJA_BESTIAR ----------
drop policy if exists gb_select on public.granja_bestiar;
create policy gb_select on public.granja_bestiar
for select to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.granja g
    where g.id = granja_bestiar.granja_id
      and public.can_read_titular(g.titular_id)
  )
);

drop policy if exists gb_insert on public.granja_bestiar;
create policy gb_insert on public.granja_bestiar
for insert to authenticated
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.granja g
    where g.id = granja_id
      and public.can_write_ramader(g.titular_id)
  )
);

drop policy if exists gb_update on public.granja_bestiar;
create policy gb_update on public.granja_bestiar
for update to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.granja g
    where g.id = granja_bestiar.granja_id
      and public.can_write_ramader(g.titular_id)
  )
)
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.granja g
    where g.id = granja_bestiar.granja_id
      and public.can_write_ramader(g.titular_id)
  )
);

drop policy if exists gb_delete on public.granja_bestiar;
create policy gb_delete on public.granja_bestiar
for delete to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.granja g
    where g.id = granja_bestiar.granja_id
      and public.can_write_ramader(g.titular_id)
  )
);

-- ---------- EMMAGATZEMATGE ----------
drop policy if exists emmag_select on public.emmagatzematge;
create policy emmag_select on public.emmagatzematge
for select to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.granja g
    where g.id = emmagatzematge.granja_id
      and public.can_read_titular(g.titular_id)
  )
);

drop policy if exists emmag_insert on public.emmagatzematge;
create policy emmag_insert on public.emmagatzematge
for insert to authenticated
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.granja g
    where g.id = granja_id
      and public.can_write_ramader(g.titular_id)
  )
);

drop policy if exists emmag_update on public.emmagatzematge;
create policy emmag_update on public.emmagatzematge
for update to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.granja g
    where g.id = emmagatzematge.granja_id
      and public.can_write_ramader(g.titular_id)
  )
)
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.granja g
    where g.id = emmagatzematge.granja_id
      and public.can_write_ramader(g.titular_id)
  )
);

drop policy if exists emmag_delete on public.emmagatzematge;
create policy emmag_delete on public.emmagatzematge
for delete to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.granja g
    where g.id = emmagatzematge.granja_id
      and public.can_write_ramader(g.titular_id)
  )
);

-- ---------- ENTREGA_DEJECCIONS ----------
drop policy if exists entrega_select on public.entrega_dejeccions;
create policy entrega_select on public.entrega_dejeccions
for select to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = entrega_dejeccions.dan_id
      and public.can_read_titular(d.titular_id)
  )
);

drop policy if exists entrega_insert on public.entrega_dejeccions;
create policy entrega_insert on public.entrega_dejeccions
for insert to authenticated
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = dan_id
      and public.can_write_ramader(d.titular_id)
  )
);

drop policy if exists entrega_update on public.entrega_dejeccions;
create policy entrega_update on public.entrega_dejeccions
for update to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = entrega_dejeccions.dan_id
      and public.can_write_ramader(d.titular_id)
  )
)
with check (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = entrega_dejeccions.dan_id
      and public.can_write_ramader(d.titular_id)
  )
);

drop policy if exists entrega_delete on public.entrega_dejeccions;
create policy entrega_delete on public.entrega_dejeccions
for delete to authenticated
using (
  public.is_admin()
  or public.is_oficina_manager()
  or exists (
    select 1
    from public.dan_declaracio d
    where d.id  = entrega_dejeccions.dan_id
      and public.can_write_ramader(d.titular_id)
  )
);

