# Guia de posada en marxa, demo i resolucio de problemes

## Objectiu

Aquesta guia explica, pas a pas, com reconstruir AgriSync des de zero i com validar que queda funcionant. No nomes cobreix la instal·lacio ideal, sino tambe les dificultats normals que poden aparèixer quan es treballa amb Compose Desktop, Supabase Auth, PostgREST i RLS.

## Què necessites abans de començar

### Infraestructura

- un projecte de Supabase amb acces a:
  `SQL Editor`, `Table Editor` i `Project Settings > API`
- connexio a internet des de la maquina on s'executa l'app

### Entorn local

- Git
- JDK 21 o superior
- el `Gradle Wrapper` del repositori
- permisos locals per crear un fitxer `agrisync.properties`

### Claus necessaries

Necessites aquestes tres dades:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`

La primera i la segona permeten que el client es connecti a Auth i a PostgREST. La tercera es necessaria per a operacions administratives que el client actual encara fa directament, com ara:

- crear usuaris Auth des de gestio de tecnics
- canviar passwords des de gestio
- resoldre etiquetes d'auditoria
- alguns fallbacks de recuperacio de perfil tecnic

## Com localitza la configuracio l'aplicacio

L'ordre real de prioritat, implementat a `composeApp/src/jvmMain/kotlin/cat/agrisync/data/JvmEnvConfig.kt`, es:

1. propietats JVM `-D...`
2. variables d'entorn del sistema
3. fitxer `agrisync.properties`
4. fitxer `config/agrisync.properties`

El codi busca aquests fitxers en diversos directoris candidats relacionats amb:

- `user.dir`
- directori del binari / codi
- `APP_HOME`
- directori de recursos de Compose

Aixo vol dir que el lloc mes simple i fiable per desenvolupament es posar `agrisync.properties` a l'arrel del repositori.

## Fitxer de configuracio recomanat

Crea `agrisync.properties` a l'arrel:

```properties
SUPABASE_URL=https://<el-teu-projecte>.supabase.co
SUPABASE_ANON_KEY=<anon_key>
SUPABASE_SERVICE_ROLE_KEY=<service_role_key>
```

També pots fer-ho amb variables d'entorn:

```powershell
$env:SUPABASE_URL="https://<el-teu-projecte>.supabase.co"
$env:SUPABASE_ANON_KEY="<anon_key>"
$env:SUPABASE_SERVICE_ROLE_KEY="<service_role_key>"
```

## Ordre correcte de reconstruccio

L'ordre important es aquest:

1. executar `docs/sql/maintenance/reset_auth_seed_users.sql`
2. executar `docs/sql/schema/agrisync_schema.sql`
3. executar `docs/sql/seeds/agrisync_demo_seed.sql`

Aquest ordre no es arbitrari:

- el reset d'Auth evita col·lisions quan es reconstrueix una demo ja existent
- el reset deixa buit tant `auth.users` com la resta de dades relacionades d'Auth
- l'esquema crea taules, funcions helper, grants i policies
- el seed crea els usuaris Auth demo i la resta de dades funcionals
- el schema i el seed ja intenten mantenir sincronitzats `auth.users.id` i `public.tecnic.user_id`

## Pas 1. Aplicar l'esquema SQL

Obre el `SQL Editor` de Supabase i executa el fitxer:

- `docs/sql/schema/agrisync_schema.sql`

Aquest fitxer fa una reconstruccio completa:

- esborra objectes previs
- recrea enums
- recrea totes les taules
- integra directament els camps `volum_m3`, `kg_n_m3` i `kg_n`
- recrea triggers d'auditoria
- afegeix sincronitzacio automatica entre `public.tecnic.email` i `auth.users`
- recrea funcions helper de seguretat
- aplica grants
- activa RLS
- crea totes les policies

### Recomanacio important

Si el `schema` falla a mig cami, no intentis continuar enganxant nomes el tros on ha fallat. El millor es:

1. corregir el problema
2. tornar a executar el fitxer sencer des del principi

El motiu es que l'script te dependències entre funcions, triggers i policies. Deixar-lo a mitges pot provocar estats inconsistents.

## Pas 2. Executar el seed de demo

Executa:

- `docs/sql/seeds/agrisync_demo_seed.sql`

Aquest seed no es limita a posar quatre registres. Carrega una demo realista amb:

- usuaris Auth
- oficines
- tecnics
- titulars
- assignacions `tecnic_titular`
- comparticions `oficina_titular_compartit`
- DAN de 2024 i 2025
- terres
- aplicacions fertilitzants
- granges
- bestiar i fases productives
- entregues de dejeccions

I a mes:

- neteja abans les dades demo conegudes del projecte
- recrea els logins demo amb password conegut
- resincronitza `public.tecnic.user_id` amb `auth.users.id` segons email
- falla de forma explicita si algun tecnic demo no queda ben vinculat a Auth
- deixa aplicacions manuals i aplicacions sincronitzades des d'entregues
- inclou terres en `ZV` i `ZNV`
- deixa casos per comprovar limits anuals de nitrogen per campanya

Al final deixa consultes de verificacio per confirmar:

- volum de dades carregades
- titulars compartits entre oficines
- nombre d'assignacions actives per tecnic
- volum d'aplicacions manuals versus sincronitzades
- terres que superen el limit anual de nitrogen

## Pas 4. Executar l'aplicacio

Des de l'arrel del repositori:

```powershell
./gradlew :composeApp:run
```

Per una verificacio de compilacio sense llançar la UI:

```powershell
./gradlew :composeApp:compileKotlinJvm
```

## Què hauria de passar en una arrencada sana

1. l'app llegeix configuracio amb `JvmEnvConfig`
2. `App.kt` crea `AppServices`
3. `AuthService.initialize()` comprova si hi ha sessio guardada
4. si no hi ha sessio, es mostra `LoginScreen`
5. si hi ha sessio valida, el client resol `public.tecnic` i entra a l'app

Si falla algun punt d'aquests, normalment l'error pertany a una d'aquestes families:

- configuracio
- Auth
- desquadrament entre `auth.users` i `public.tecnic`
- permisos/RLS
- xarxa o SSL

## Validacio funcional minima

### Com a `admin`

Has de poder:

- veure la home de titulars
- entrar a `Gestio Titulars`
- entrar a `Terres`
- entrar a `Tecnics`
- entrar a `Oficines`
- canviar a `Perfil`

### Com a `oficina_manager`

Has de poder:

- veure dades limitades a la seva oficina i comparticions
- editar titulars dins del seu abast
- assignar tecnics de la seva oficina
- veure o crear comparticions d'oficina quan pertoqui

### Com a tecnic agricola

Has de poder:

- veure nomes titulars accessibles
- entrar al modul agricola dels titulars amb `scope` compatible
- editar dades del titular si el `scope` ho permet
- crear i editar terres i aplicacions de la seva campanya

### Com a tecnic ramader

Has de poder:

- entrar al modul ramader dels titulars compatibles
- gestionar granges, cens, balanç nitrogen i entregues
- seleccionar terres accessibles com a destí de cada entrega

### Com a lectura

Has de poder:

- veure dades accessibles
- no poder fer escriptures operatives

## Dificultats habituals i com resoldre-les

### 1. L'app diu que falta configuracio

Símptoma:

- surt una pantalla de configuracio incompleta

Comprova:

- que existeixen `SUPABASE_URL`, `SUPABASE_ANON_KEY` i `SUPABASE_SERVICE_ROLE_KEY`
- que no hi ha espais en blanc o claus truncades
- que `agrisync.properties` esta realment a l'arrel o a `config/agrisync.properties`

### 2. El login falla amb credencials correctes

Causes habituals:

- el seed no s'ha executat completament
- existeix a Auth pero no existeix o no coincideix a `public.tecnic`
- `public.tecnic.user_id` no esta sincronitzat amb `auth.users.id`

Solucio:

1. comprovar que l'email existeix a `auth.users`
2. comprovar que l'email existeix a `public.tecnic`
3. si has refet el projecte, repetir `reset_auth_seed_users.sql -> agrisync_schema.sql -> agrisync_demo_seed.sql`

### 3. La UI carrega pero algunes pantalles donen `401` o `403`

Interpretacio:

- `401` acostuma a ser sessio caducada o token invalid
- `403` acostuma a ser denegacio per RLS

Comprova:

- que el `schema` s'ha aplicat complet
- que les policies existeixen
- que el tecnic te rol, oficina i assignacions coherents

### 4. No es creen tecnics o no es poden canviar passwords

Causa habitual:

- falta `SUPABASE_SERVICE_ROLE_KEY`

Aquestes operacions fan servir Admin API i no es poden fer nomes amb `anon key`.

### 5. L'app veu dades incorrectes o massa poques

Causes habituals:

- el login s'ha fet amb un usuari que no te `public.tecnic` ben configurat
- hi ha assignacions `tecnic_titular` absents o `actiu=false`
- hi ha comparticions d'oficina que no existeixen
- l'usuari te rol `lectura`

### 6. Error de xarxa, timeout, SSL o host desconegut

Normalment el problema no es del codi, sino de:

- URL mal escrita
- tallafocs
- proxy
- antivirus amb inspeccio HTTPS
- DNS

La `LoginViewModel` ja tradueix molts d'aquests casos a missatges mes clars.

### 7. El schema SQL falla a mig crear funcions

Si treballes amb una copia antiga del fitxer, pot passar per ordre de dependències entre funcions. Solucio:

- assegurar-se d'executar la versio actual de `docs/sql/schema/agrisync_schema.sql`
- tornar a llançar l'script sencer

## Els 3 scripts SQL a executar

### `docs/sql/maintenance/reset_auth_seed_users.sql`

Buida completament Supabase Auth del projecte actual.

Fa això:

- elimina tots els usuaris de `auth.users`
- elimina sessions, refresh tokens, identities i altres dades relacionades si existeixen
- posa `public.tecnic.user_id = null` per evitar enllacos trencats

Fes-lo servir si:

- vols reconstruir la demo realment des de zero
- vols reutilitzar emails antics sense conflictes
- has creat usuaris manuals i vols deixar Auth completament net

### `docs/sql/schema/agrisync_schema.sql`

Reconstrueix tota la part `public` del projecte: taules, funcions, triggers, grants i RLS.

### `docs/sql/seeds/agrisync_demo_seed.sql`

Crea els usuaris Auth demo i carrega totes les dades funcionals de la demo.

## Recomanacions per una demo estable

- no modifiquis manualment registres a `public.tecnic` mentre tens l'app oberta
- si canvies l'esquema, torna a aplicar el fitxer sencer
- si canvies el seed, torna a executar la sequencia completa sobre un entorn net
- usa usuaris diferents per provar rols diferents
- valida sempre almenys una campanya 2024 i una 2025

## Com saber que la reconstruccio ha quedat be

Pots considerar la posada en marxa correcta quan es compleixen tots aquests punts:

- l'app compila
- el login entra com a `admin`
- la home mostra titulars
- `Perfil` mostra dades del tecnic
- `Tecnics` permet veure o crear tecnics
- `Titulars` deixa compartir per oficina
- els moduls agricola i ramader carreguen dades sense `403`
- `Preparar DAN` mostra resum i checklist

## Comandes utiles

Execucio:

```powershell
./gradlew :composeApp:run
```

Compilacio:

```powershell
./gradlew :composeApp:compileKotlinJvm
```

Tests JVM:

```powershell
./gradlew :composeApp:jvmTest
```

## Resum

La posta en marxa d'AgriSync te tres punts sensibles:

- configuracio local correcta
- projecte Supabase ben preparat
- ordre correcte `reset -> schema -> seed`

Quan aquests tres punts estan bé, la resta del projecte acostuma a funcionar de manera bastant directa.
