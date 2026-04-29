# Guia de posada en marxa, demo i resolucio de problemes

## Objectiu

Aquesta guia explica, pas a pas, com reconstruir AgriSync des de zero i com validar que queda funcionant. No nomes cobreix la instal·lacio ideal, sino tambe les dificultats normals que poden aparèixer quan es treballa amb Compose Desktop, Supabase Auth, PostgREST i RLS.

## Què necessites abans de començar

### Infraestructura

- un projecte de Supabase amb acces a:
  `SQL Editor`, `Table Editor`, `Authentication > Users` i `Project Settings > API`
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

1. executar `docs/sql/schema/agrisync_schema.sql`
2. crear manualment els usuaris demo a `Authentication > Users`
3. executar `docs/sql/seeds/agrisync_demo_seed.sql`
4. si cal, executar `docs/sql/maintenance/resincronitza_tecnic_user_ids.sql`
5. si cal, executar `docs/sql/maintenance/reaplica_permisos.sql`

Aquest ordre no es arbitrari:

- l'esquema crea taules, funcions helper, grants i policies
- el seed assumeix que els usuaris Auth ja existeixen
- les operacions de manteniment nomes tenen sentit quan la base ja esta desplegada

## Pas 1. Aplicar l'esquema SQL

Obre el `SQL Editor` de Supabase i executa el fitxer:

- `docs/sql/schema/agrisync_schema.sql`

Aquest fitxer fa una reconstruccio completa:

- esborra objectes previs
- recrea enums
- recrea totes les taules
- recrea triggers d'auditoria
- recrea funcions helper de seguretat
- aplica grants
- activa RLS
- crea totes les policies

### Recomanacio important

Si el `schema` falla a mig cami, no intentis continuar enganxant nomes el tros on ha fallat. El millor es:

1. corregir el problema
2. tornar a executar el fitxer sencer des del principi

El motiu es que l'script te dependències entre funcions, triggers i policies. Deixar-lo a mitges pot provocar estats inconsistents.

## Pas 2. Crear els usuaris demo a Supabase Auth

Abans d'executar el seed, crea aquests usuaris a `Authentication > Users`:

- `admin.demo@agrisync.com` / `admin1234`
- `manager.lleida.demo@agrisync.com` / `lleida1234`
- `manager.girona.demo@agrisync.com` / `girona1234`
- `sergi.agri.demo@agrisync.com` / `sergi1234`
- `marta.ram.demo@agrisync.com` / `marta1234`
- `laia.comu.demo@agrisync.com` / `laia1234`
- `nil.shared.demo@agrisync.com` / `nil1234`
- `joan.agri.demo@agrisync.com` / `joan1234`
- `anna.ram.demo@agrisync.com` / `anna1234`
- `lectura.demo@agrisync.com` / `lectura1234`

### Per què es fan a ma i no per SQL

El projecte evita inserir directament a `auth.users` via SQL intern. Es prefereix:

- dashboard de Supabase
- o Admin API

perque es un flux mes robust i menys dependent dels detalls interns de GoTrue.

## Pas 3. Executar el seed de demo

Executa:

- `docs/sql/seeds/agrisync_demo_seed.sql`

Aquest seed no es limita a posar quatre registres. Carrega una demo realista amb:

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

Al final deixa consultes de verificacio per confirmar:

- volum de dades carregades
- titulars compartits entre oficines
- nombre d'assignacions actives per tecnic

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

- l'usuari no existeix a `Authentication > Users`
- existeix a Auth pero no existeix o no coincideix a `public.tecnic`
- `public.tecnic.user_id` no esta sincronitzat amb `auth.users.id`

Solucio:

1. comprovar que l'email existeix a Auth
2. comprovar que l'email existeix a `public.tecnic`
3. executar `docs/sql/maintenance/resincronitza_tecnic_user_ids.sql` si cal

### 3. El seed falla dient que falten usuaris

Símptoma:

- excepcio del bloc inicial del seed

Causa:

- encara no has creat algun usuari demo a `Authentication > Users`

Solucio:

- crear els usuaris que falten i tornar a executar el seed

### 4. La UI carrega pero algunes pantalles donen `401` o `403`

Interpretacio:

- `401` acostuma a ser sessio caducada o token invalid
- `403` acostuma a ser denegacio per RLS

Comprova:

- que el `schema` s'ha aplicat complet
- que els `grant execute` i les policies existeixen
- que el tecnic te rol, oficina i assignacions coherents

Si sospites d'un desajust de permisos:

- executa `docs/sql/maintenance/reaplica_permisos.sql`

### 5. No es creen tecnics o no es poden canviar passwords

Causa habitual:

- falta `SUPABASE_SERVICE_ROLE_KEY`

Aquestes operacions fan servir Admin API i no es poden fer nomes amb `anon key`.

### 6. L'app veu dades incorrectes o massa poques

Causes habituals:

- el login s'ha fet amb un usuari que no te `public.tecnic` ben configurat
- hi ha assignacions `tecnic_titular` absents o `actiu=false`
- hi ha comparticions d'oficina que no existeixen
- l'usuari te rol `lectura`

### 7. Error de xarxa, timeout, SSL o host desconegut

Normalment el problema no es del codi, sino de:

- URL mal escrita
- tallafocs
- proxy
- antivirus amb inspeccio HTTPS
- DNS

La `LoginViewModel` ja tradueix molts d'aquests casos a missatges mes clars.

### 8. El schema SQL falla a mig crear funcions

Si treballes amb una copia antiga del fitxer, pot passar per ordre de dependències entre funcions. Solucio:

- assegurar-se d'executar la versio actual de `docs/sql/schema/agrisync_schema.sql`
- tornar a llançar l'script sencer

## Scripts de manteniment i quan fer-los servir

### `docs/sql/maintenance/resincronitza_tecnic_user_ids.sql`

Serveix per reparar la correspondencia entre:

- `auth.users.id`
- `public.tecnic.user_id`

Fes-lo servir si:

- l'usuari existeix a Auth pero l'app no li troba perfil tecnic

### `docs/sql/maintenance/reaplica_permisos.sql`

Reaplica:

- grants sobre taules
- execucio de funcions helper

Fes-lo servir si:

- algun entorn ha quedat amb permisos desalineats
- has tocat manualment objectes SQL

### `docs/sql/maintenance/reset_auth_seed_users.sql`

Neteja els usuaris demo coneguts d'Auth. Es util si vols reconstruir la demo des de zero sense anar esborrant un per un.

## Recomanacions per una demo estable

- no modifiquis manualment registres a `public.tecnic` mentre tens l'app oberta
- si canvies l'esquema, torna a aplicar el fitxer sencer
- si canvies el seed, torna a validar usuaris Auth i relacio `user_id`
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

La posta en marxa d'AgriSync te quatre punts sensibles:

- configuracio local correcta
- projecte Supabase ben preparat
- ordre correcte `schema -> Auth users -> seed`
- coherencia entre `auth.users` i `public.tecnic`

Quan aquests quatre punts estan bé, la resta del projecte acostuma a funcionar de manera bastant directa.
