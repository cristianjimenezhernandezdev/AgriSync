# Instruccions d'installacio, reconstruccio i us d'AgriSync

## 1. Objectiu d'aquest document

Aquest document esta pensat per a dues situacions:

- muntar AgriSync des de zero en un projecte nou de Supabase
- provar l'aplicacio amb una seed solida que cobreixi totes les funcionalitats actuals del MVP

La configuracio recomanada per a una demo completa es:

- esquema: `SQLAgriSync.sql`
- grants de suport: `fix_permisos.sql` si cal
- seed principal: `seed_final_demo.sql`
- neteja d'usuaris Auth de proves: `reset_auth_seed_users.sql`
- resincronitzacio excepcional d'usuaris: `fix_user_ids.sql`

## 2. Que necessites abans de començar

Per reconstruir i provar el projecte necessites:

- un projecte de Supabase buit o que puguis resetejar
- acces a `Authentication > Users` i al `SQL Editor` de Supabase
- JDK 21
- el repositori d'AgriSync descarregat
- connexio a internet per executar Gradle i l'app

## 3. Paquet SQL del projecte

Els fitxers SQL del repositori tenen aquest paper:

- `SQLAgriSync.sql`
  Recrea l'esquema public complet d'AgriSync. Fa neteja de vistes, funcions, taules, triggers, enums, grants i policies abans de tornar-ho a crear.
- `fix_permisos.sql`
  Reaplica `grant` i `execute` de suport si algun permis ha quedat desalineat.
- `seed_complet.sql`
  Seed curta per smoke test rapid del MVP.
- `seed_final_demo.sql`
  Seed recomanada per defensa i proves completes. Inclou diverses oficines, rols, titulars compartits, campanyes 2024 i 2025, terres, aplicacions, granges, censos i entregues.
- `reset_auth_seed_users.sql`
  Esborra els usuaris Auth coneguts dels seeds, amb identitats, sessions i tokens relacionats.
- `fix_user_ids.sql`
  Torna a quadrar `public.tecnic.user_id` amb `auth.users.id` a partir de l'email.
- `create_auth_users.sql`
  Script legacy. No es recomana com a primera opcio.

## 4. Quina seed has de fer servir

Per provar tota l'aplicacio, fes servir `seed_final_demo.sql`.

Es la seed recomanada perque cobreix:

- `admin`
- `oficina_manager`
- tecnics amb `scope agricola`
- tecnics amb `scope ramader`
- tecnics amb `scope comu`
- usuaris de `lectura`
- titulars visibles des de mes d'una oficina
- campanyes 2024 i 2025
- modul agricola
- modul ramader
- pantalla `Preparar DAN`
- comparticio entre oficines per part agricola o ramadera
- entregues a terres accessibles d'altres titulars

`seed_complet.sql` nomes es recomana si vols una prova rapida amb menys dades.

## 5. Ordre recomanat per reconstruir-ho tot des de zero

### 5.1. Pas 1. Neteja d'usuaris Auth de prova

Si vols partir realment de zero, executa primer:

- `reset_auth_seed_users.sql`

Aquest script:

- elimina sessions i refresh tokens dels usuaris de prova si existeixen
- elimina identitats
- elimina usuaris de `auth.users` corresponents als seeds basic i demo
- fa les comparacions de `user_id` en format text per ser compatible amb variants internes de l'esquema Auth de Supabase

No toca la configuracio interna global de Supabase ni altres usuaris que no estiguin a la llista del projecte.

### 5.2. Pas 2. Crea els usuaris a Supabase Authentication

Des de `Authentication > Users > Add user`, crea aquests usuaris si faras servir la seed principal:

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

Si nomes vols la seed curta:

- `admin.test@agrisync.com` / `admin1234`
- `manager.test@agrisync.com` / `manager1234`
- `agricola.test@agrisync.com` / `agricola1234`
- `ramader.test@agrisync.com` / `ramader1234`
- `lectura.test@agrisync.com` / `lectura1234`

Important:

- la via recomanada es el Dashboard de Supabase
- `create_auth_users.sql` queda nomes com a recurs legacy en entorns controlats

### 5.3. Pas 3. Reconstrueix l'esquema public

Executa al `SQL Editor`:

- `SQLAgriSync.sql`

Aquest script ja fa la neteja de l'esquema public abans de recrear-lo. En concret:

- elimina la vista antiga `v_titular_access`
- elimina funcions helper i d'auditoria
- elimina taules del MVP
- elimina enums propis
- recrea taules, triggers, indexes, grants i policies RLS

No cal esborrar manualment:

- taules
- triggers
- funcions
- policies
- enums

perque aquest fitxer ja ho fa.

### 5.4. Pas 4. Reaplica permisos si cal

Normalment no fa falta immediatament despres del pas anterior, pero si detectes algun problema de grants, executa:

- `fix_permisos.sql`

### 5.5. Pas 5. Carrega la seed funcional

Per a una demo completa, executa:

- `seed_final_demo.sql`

La seed valida que els usuaris d'Auth existeixin abans de continuar.

### 5.6. Pas 6. Si els `user_id` no quadren, arregla'ls

Si has recreat usuaris i algun tecnic no pot entrar o surt desalineat, executa:

- `fix_user_ids.sql`

Aquest script:

- compara `public.tecnic.email` amb `auth.users.email`
- actualitza `public.tecnic.user_id` amb l'id real d'Auth

## 6. Configuracio local de l'aplicacio

L'aplicacio necessita tres valors:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`

La manera recomanada es crear un fitxer `agrisync.properties` a l'arrel del projecte o al costat de l'executable.

Plantilla:

```properties
SUPABASE_URL=https://el-teu-projecte.supabase.co
SUPABASE_ANON_KEY=enganxa_aqui_la_anon_key
SUPABASE_SERVICE_ROLE_KEY=enganxa_aqui_la_service_role_key
```

Pots partir de `agrisync.properties.example`.

## 7. Arrencada en desenvolupament

Amb el fitxer de propietats preparat, executa:

```powershell
./gradlew :composeApp:run
```

Si prefereixes variables d'entorn:

```powershell
$env:SUPABASE_URL="https://<PROJECT>.supabase.co"
$env:SUPABASE_ANON_KEY="<ANON_KEY>"
$env:SUPABASE_SERVICE_ROLE_KEY="<SERVICE_ROLE_KEY>"
./gradlew :composeApp:run
```

Si vols una comprovacio rapida abans d'obrir la UI, pots compilar primer:

```powershell
./gradlew :composeApp:compileKotlinJvm
```

## 8. Flux de prova funcional recomanat

### 8.1. Login i rols

Comprova aquests casos amb `seed_final_demo.sql`:

- `admin.demo@agrisync.com`
  Ha de veure i gestionar tota l'estructura.
- `manager.lleida.demo@agrisync.com`
  Ha de gestionar tecnics del seu ambit i titulars del seu entorn funcional.
- `manager.girona.demo@agrisync.com`
  Mateix criteri per Girona.
- `sergi.agri.demo@agrisync.com`
  Cas clar de tecnic agricola.
- `marta.ram.demo@agrisync.com`
  Cas clar de tecnic ramader.
- `laia.comu.demo@agrisync.com`
  Cas de tecnic amb visio comuna.
- `lectura.demo@agrisync.com`
  Cas de lectura sense escriptura.

### 8.2. Campanyes

La iteracio funcional de campanyes ja esta incorporada. Prova aquest comportament:

1. entra a un titular
2. obre `Modul Agricola`, `Modul Ramader` o `Preparar DAN`
3. canvia entre campanyes 2024 i 2025
4. comprova que les dades visibles canvien
5. crea una aplicacio o una entrega i verifica que queda associada a la campanya activa

Funcionament actual:

- cada pantalla te selector de campanya
- la campanya activa governa els totals visibles
- si no existeix una `dan_declaracio` per l'any seleccionat, es crea en el primer insert

### 8.3. Modul agricola

Proves recomanades:

- editar el nom o NIF del titular si el teu rol ho permet
- crear una terra
- editar superficie, zona (`ZV` o `ZNV`), municipi literal, us SIGPAC i cultiu
- crear una aplicacio a la campanya activa
- informar tipus fertilitzant, procedencia, `volum m3` i `kg N/m3`
- comprovar `kg N/ha` i marge de nitrogen
- eliminar una terra o aplicacio

### 8.4. Modul ramader

Proves recomanades:

- crear una granja
- afegir bestiar i fase productiva
- crear una entrega a titular
- crear una entrega a terra del mateix titular
- crear una entrega a terra d'un altre titular si tens acces a aquella terra
- canviar de campanya i verificar el filtratge
- eliminar granja, registre o entrega

Limit actual a tenir en compte:

- per enviar a terres externes cal tenir acces real a aquell titular o a la seva oficina compartida
- la relacio entre entrega concreta i aplicacio concreta continua sense quedar modelada

### 8.5. Preparar DAN

Proves recomanades:

- obrir `Preparar DAN` des de la llista de titulars
- canviar de campanya
- verificar totals de `ha`, `kg N`, `UF`, cens i entregues
- revisar terres, aplicacions, granges, censos i entregues en una sola pantalla
- provar `Copiar resum` i enganxar el text en un editor per validar la sortida estructurada
- provar `Copiar checklist` i verificar que hi apareixen els buits detectats automaticament

## 9. Sequencia curta si vols un hard reset net

Ordre minim recomanat:

1. `reset_auth_seed_users.sql`
2. crear usuaris Auth al Dashboard
3. `SQLAgriSync.sql`
4. `seed_final_demo.sql`
5. `fix_user_ids.sql` nomes si cal
6. preparar `agrisync.properties`
7. `./gradlew :composeApp:run`

## 9.1. Posta a punt abans d'una defensa o entrega

Checklist recomanada:

1. executar `SQLAgriSync.sql`
2. carregar `seed_final_demo.sql`
3. executar `fix_user_ids.sql` si algun usuari no entra
4. comprovar que `agrisync.properties` apunta al projecte correcte
5. executar `./gradlew :composeApp:compileKotlinJvm`
6. validar login amb `admin.demo@agrisync.com`
7. validar login amb un tecnic agricola i un tecnic ramader
8. comprovar canvi de campanya a `Modul Agricola`, `Modul Ramader` i `Preparar DAN`
9. provar una comparticio de titular a una altra oficina des de `Gestio de Titulars`
10. provar alta i baixa minima de terra, aplicacio, granja i entrega
11. provar una entrega a terra externa si el titular esta compartit amb l'oficina corresponent
12. provar `Copiar resum` i `Copiar checklist` a `Preparar DAN`

## 10. Errors habituals i com resoldre'ls

### 10.1. Falten usuaris a Authentication

Si la seed falla amb un missatge de tipus:

- `Falten usuaris a Authentication`

vol dir que no has creat tots els usuaris previs a `Authentication > Users`.

### 10.2. Login correcte pero sense perfil tecnic

Vol dir que l'usuari existeix a Auth pero `public.tecnic.user_id` no quadra o no existeix el registre funcional.

Solucio recomanada:

- executar `fix_user_ids.sql`

### 10.3. Error `401` o `403`

Normalment vol dir:

- sessio caducada
- o permisos insuficients segons rol, oficina o `scope`

### 10.4. El gestor d'oficina no veu un titular que esperava

Comportament actual correcte:

- `oficina_manager` no veu tot el sistema
- nomes veu titulars creats per ell o ja vinculats a tecnics actius de la seva oficina

### 10.5. La campanya no apareix encara

Si encara no hi ha `dan_declaracio` per un any concret:

- el selector mostra igualment l'any actual
- la DAN es crea quan es fa el primer insert agricola o ramader sobre aquella campanya

## 11. Que queda fora d'aquest muntatge de zero

Aquest paquet et deixa l'app completament operativa per al MVP actual, pero no fa aquestes coses:

- no genera el PDF oficial de la DAN
- no exporta encara directament a fitxer o PDF, tot i que si permet copiar el resum i la checklist
- no crea usuaris Auth arbitraris externs a les llistes de prova
- no importa dades des d'Excel
- no calcula encara nitrogen generat a partir del cens ramader
- no relaciona una entrega concreta amb una aplicacio concreta

## 12. Resum final

Si vols la configuracio mes fiable per provar tota l'aplicacio, la recepta correcta es aquesta:

- netejar Auth amb `reset_auth_seed_users.sql`
- crear els usuaris demo al Dashboard
- reconstruir la BDD amb `SQLAgriSync.sql`
- carregar `seed_final_demo.sql`
- arreglar `user_id` amb `fix_user_ids.sql` si cal
- configurar `agrisync.properties`
- executar `./gradlew :composeApp:run`

Amb aquest flux tindras una base prou rica per provar:

- permisos
- oficines
- titulars compartits
- campanyes
- modul agricola
- modul ramader
- `Preparar DAN`
- copia rapida del resum DAN i de la checklist final

Si el que busques es una posta a punt final abans de presentar el projecte, la seccio clau es la `9.1`.
