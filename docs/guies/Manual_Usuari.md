# Manual d'usuari d'AgriSync

## Objectiu del manual

Aquest manual explica com utilitzar AgriSync des del punt de vista d'una persona usuaria. Esta preparat per convertir-se en un document final amb captures de pantalla, imatges del programa i captures de Supabase.

El document tambe esta preparat per passar-lo a una altra IA. Les marques `[CAPTURA: ...]` indiquen exactament on inserir imatges i quin contingut hauria de mostrar cada captura.

## Com utilitzar aquest document per generar la versio final

Per maquetar aquest manual:

1. Mantingues l'ordre dels apartats.
2. Substitueix cada marcador `[CAPTURA: ...]` per una imatge real.
3. Si una captura mostra dades sensibles, tapa claus, tokens, URLs privades o contrasenyes.
4. Conserva els noms de pantalles i botons tal com apareixen a l'aplicacio.
5. Si es genera una versio PDF, posa les captures centrades i amb peu d'imatge.

Exemple de peu d'imatge:

```text
Figura 1. Pantalla de login d'AgriSync.
```

## Descripcio general de l'aplicacio

AgriSync es una aplicacio desktop per gestionar dades relacionades amb la DAN. Permet treballar amb titulars, terres, granges, aplicacions fertilitzants, entregues de dejeccions, campanyes i preparacio de la declaracio.

L'aplicacio funciona amb:

- autenticacio real amb Supabase Auth
- base de dades PostgreSQL a Supabase
- permisos reals amb Row Level Security
- rols d'usuari
- assignacions de tecnics a titulars
- comparticions entre oficines

El flux general d'us es:

1. L'usuari inicia sessio amb email i contrasenya.
2. AgriSync comprova quin tecnic funcional correspon a aquell usuari.
3. La pantalla inicial mostra nomes els titulars accessibles.
4. L'usuari treballa el titular des del modul agricola, ramader o Preparar DAN.
5. La base de dades valida permisos i guarda canvis.

[CAPTURA: vista general de l'aplicacio oberta a la pantalla de titulars, amb la barra superior visible]

## Perfils d'usuari

AgriSync treballa amb quatre rols principals.

### Admin

L'administrador te visio global del sistema.

Pot:

- iniciar sessio
- veure tots els titulars
- accedir al modul agricola i ramader
- preparar DAN
- gestionar titulars
- gestionar terres
- gestionar tecnics
- gestionar oficines
- canviar passwords de tecnics
- gestionar assignacions i comparticions

### Oficina manager

El manager d'oficina gestiona dades dins del seu ambit.

Pot:

- iniciar sessio
- veure titulars de la seva oficina o titulars compartits
- treballar titulars segons scope
- gestionar tecnics de la seva oficina amb rols `tecnic` o `lectura`
- crear o editar titulars dins del seu abast
- compartir titulars amb altres oficines
- gestionar terres dins del seu abast
- editar dades de la seva oficina

No pot:

- gestionar lliurement totes les oficines
- editar tecnics d'altres oficines
- convertir usuaris en administradors globals

### Tecnic

El tecnic es l'usuari operatiu habitual.

Pot:

- iniciar sessio amb email i contrasenya si te usuari Auth vinculat
- veure titulars assignats o compartits
- entrar al modul agricola si te scope `agricola` o `comu`
- entrar al modul ramader si te scope `ramader` o `comu`
- consultar Preparar DAN si te lectura del titular
- editar el seu perfil
- canviar la seva contrasenya

Punt important:

Un tecnic pot tenir email i login propi. El registre funcional es troba a `public.tecnic`, i el login real es valida amb Supabase Auth.

### Lectura

L'usuari de lectura pot consultar dades, pero no hauria de modificar dades operatives.

Pot:

- iniciar sessio
- consultar titulars accessibles
- veure dades agricoles o ramaderes segons permisos
- consultar Preparar DAN
- consultar i editar dades propies permeses

No pot:

- crear terres
- crear aplicacions
- crear entregues
- gestionar tecnics
- gestionar oficines
- modificar dades administratives

## Resum de permisos

| Rol | Consulta | Edicio operativa | Gestio administrativa |
|---|---|---|---|
| `admin` | Global | Global segons policies | Global |
| `oficina_manager` | Oficina i comparticions | Segons abast i scope | Limitada a la seva oficina |
| `tecnic` | Assignacions i comparticions | Segons scope | No |
| `lectura` | Assignacions i comparticions | No | No |

Scopes sobre titular:

| Scope | Significat |
|---|---|
| `comu` | Permet treball agricola i ramader |
| `agricola` | Permet treball agricola |
| `ramader` | Permet treball ramader |
| `lectura` | Permet consultar |

[CAPTURA: exemple de tecnic o titular amb scopes visibles a la UI, si la pantalla mostra col·laboradors o assignacions]

## Requisits previs per usar l'aplicacio

Per utilitzar AgriSync cal:

- tenir l'aplicacio instalada o poder executar-la des del projecte
- tenir connexio a internet
- tenir un projecte Supabase configurat
- tenir `SUPABASE_URL`, `SUPABASE_ANON_KEY` i `SUPABASE_SERVICE_ROLE_KEY`
- tenir usuaris creats a Supabase Auth
- tenir registres funcionals a `public.tecnic`
- tenir esquema i seed aplicats si s'usa la demo

## Posada en marxa de l'entorn demo

Aquest apartat esta pensat per professorat, tribunal o persona que hagi de provar el projecte.

### 1. Obrir Supabase

Entra al projecte de Supabase.

[CAPTURA: dashboard principal del projecte Supabase, ocultant identificadors sensibles si cal]

### 2. Executar reset d'usuaris demo

Obre `SQL Editor` i executa:

```text
docs/sql/maintenance/reset_auth_seed_users.sql
```

Aquest script neteja usuaris Auth de demo. S'ha d'usar amb compte perquè elimina usuaris de l'entorn actual.

[CAPTURA: SQL Editor de Supabase amb el fitxer reset_auth_seed_users.sql preparat o executat]

### 3. Executar esquema

Executa:

```text
docs/sql/schema/agrisync_schema.sql
```

Aquest script crea:

- enums
- taules
- relacions
- indexos
- funcions helper
- RPCs
- triggers
- grants
- policies RLS

[CAPTURA: SQL Editor executant agrisync_schema.sql sense errors]

### 4. Executar seed de demo

Executa:

```text
docs/sql/seeds/agrisync_demo_seed.sql
```

Aquest script crea dades de prova:

- oficines
- tecnics
- titulars
- assignacions
- comparticions
- campanyes 2024 i 2025
- terres
- aplicacions
- granges
- censos
- entregues

[CAPTURA: SQL Editor executant agrisync_demo_seed.sql i mostrant missatge final correcte]

### 5. Comprovar Authentication

A Supabase, ves a `Authentication > Users` i comprova que existeixen els usuaris demo.

[CAPTURA: Supabase Authentication > Users amb alguns emails demo visibles, ocultant dades sensibles si cal]

### 6. Comprovar taules principals

A Supabase, ves a `Table Editor` i revisa:

- `tecnic`
- `titular`
- `tecnic_titular`
- `oficina_titular_compartit`
- `terra`
- `granja`
- `entrega_dejeccions`
- `aplicacions_fertilitzants`

[CAPTURA: Table Editor amb la taula `tecnic` visible]

[CAPTURA: Table Editor amb la taula `titular` visible]

## Credencials demo

Despres d'executar el seed, es poden utilitzar aquests usuaris:

| Rol | Email | Password | Us recomanat |
|---|---|---|---|
| Admin | `admin.demo@agrisync.com` | `admin1234` | Validar gestio global |
| Manager Lleida | `manager.lleida.demo@agrisync.com` | `lleida1234` | Provar oficina i gestio |
| Manager Girona | `manager.girona.demo@agrisync.com` | `girona1234` | Provar comparticions |
| Tecnic agricola | `sergi.agri.demo@agrisync.com` | `sergi1234` | Provar modul agricola |
| Tecnic ramader | `marta.ram.demo@agrisync.com` | `marta1234` | Provar modul ramader |
| Tecnic comu | `laia.comu.demo@agrisync.com` | `laia1234` | Provar agricola i ramader |
| Tecnic compartit | `nil.shared.demo@agrisync.com` | `nil1234` | Provar dades compartides |
| Tecnic agricola Girona | `joan.agri.demo@agrisync.com` | `joan1234` | Provar altra oficina |
| Tecnic ramader Girona | `anna.ram.demo@agrisync.com` | `anna1234` | Provar altra oficina |
| Lectura | `lectura.demo@agrisync.com` | `lectura1234` | Validar consulta sense edicio |

[CAPTURA: document o pantalla amb credencials demo preparades, sense mostrar claus de Supabase]

## Execucio de l'aplicacio

Per executar AgriSync en entorn de desenvolupament:

```powershell
./gradlew :composeApp:run
```

Per comprovar compilacio sense obrir la UI:

```powershell
./gradlew :composeApp:compileKotlinJvm
```

[CAPTURA: terminal executant `./gradlew :composeApp:run` i obrint l'aplicacio]

## Pantalla de login

La pantalla de login es el primer punt d'entrada quan no hi ha sessio guardada.

### Camps

- Email
- Password

### Accio principal

- Entrar / iniciar sessio

### Funcionament

1. L'usuari escriu email i password.
2. L'app envia les credencials a Supabase Auth.
3. Supabase valida l'usuari.
4. AgriSync resol el registre `public.tecnic`.
5. Si el tecnic existeix i esta actiu, s'obre la pantalla de titulars.

[CAPTURA: pantalla de login buida]

[CAPTURA: pantalla de login amb un usuari demo escrit, abans d'entrar]

### Errors habituals al login

| Error | Causa probable | Solucio |
|---|---|---|
| Credencials incorrectes | Email o password mal escrits | Revisar credencials demo |
| Usuari no trobat | No existeix a Auth | Revisar Supabase Authentication |
| Perfil tecnic no trobat | Falta fila a `public.tecnic` o `user_id` no coincideix | Reexecutar seed o revisar taula `tecnic` |
| Configuracio incompleta | Falten claus Supabase | Revisar `agrisync.properties` |
| Error 401 | Token invalid o sessio caducada | Tancar i tornar a entrar |
| Error 403 | RLS denega acces | Revisar rol, scope i assignacions |

[CAPTURA: exemple de missatge d'error de login, si es pot provocar amb credencial incorrecta]

## Barra superior i navegacio

Quan l'usuari entra, la part superior de l'app mostra opcions de navegacio.

Opcions comunes:

- `Titulars`
- `Perfil`
- `Logout`

Opcions per `admin` i `oficina_manager`:

- `Gestio Titulars`
- `Terres`
- `Tecnics`
- `Oficines`

[CAPTURA: barra superior com a admin, amb totes les opcions visibles]

[CAPTURA: barra superior com a tecnic, amb menys opcions visibles]

## Pantalla Titulars

La pantalla `Titulars` es la pantalla principal de treball.

### Que mostra

- nombre de titulars visibles
- nombre de pagines
- camp de cerca
- targetes de titulars
- dades basiques del titular
- accessos a moduls disponibles

### Dades visibles d'un titular

Segons dades disponibles, la targeta pot mostrar:

- nom o rao social
- NIF
- telefon
- email
- adreca
- codi postal
- ultima modificacio
- accions disponibles

[CAPTURA: pantalla Titulars completa amb diverses targetes]

### Cerca de titulars

La cerca permet filtrar per:

- nom
- NIF
- telefon
- email
- adreca
- codi postal

Passos:

1. Escriure text al camp de cerca.
2. La llista es filtra automaticament.
3. Si hi ha moltes coincidencies, usar paginacio.

[CAPTURA: cerca activa filtrant per NIF o codi postal]

### Paginacio

Si hi ha molts titulars, la pantalla permet passar de pagina.

Accions:

- pagina anterior
- pagina seguent

[CAPTURA: controls de paginacio a la pantalla Titulars]

### Obrir moduls des d'un titular

Segons permisos, cada targeta pot oferir:

- `Agricola`
- `Ramader`
- `Preparar DAN`

Interpretacio:

- `Agricola`: obre gestio agricola del titular.
- `Ramader`: obre gestio ramadera del titular.
- `Preparar DAN`: obre resum agregat per campanya.

[CAPTURA: targeta de titular amb botons Agricola, Ramader i Preparar DAN]

## Modul Agricola

El modul agricola serveix per gestionar terres i aplicacions fertilitzants d'un titular.

### Acces

Poden accedir-hi:

- admin
- oficina manager amb ambit suficient
- tecnic amb scope `agricola` o `comu`

La lectura pot estar disponible si el titular es visible, pero l'edicio queda bloquejada per RLS si no hi ha permisos.

[CAPTURA: pantalla inicial del modul Agricola]

### Capcalera del modul

La pantalla mostra:

- nom del titular
- boto de tornar
- selector de campanya
- dades generals del titular
- blocs de terres i aplicacions

[CAPTURA: part superior del modul Agricola amb selector de campanya]

### Seleccionar campanya

La campanya determina quines aplicacions es mostren.

Passos:

1. Localitzar el selector de campanya.
2. Seleccionar campanya disponible.
3. La pantalla recarrega aplicacions i totals.

[CAPTURA: selector de campanya desplegat o amb diverses campanyes visibles]

### Editar dades generals del titular

Dades editables:

- NIF
- nom o rao social
- telefon
- email
- adreca
- codi postal

Passos:

1. Premre `Editar`.
2. Modificar els camps.
3. Premre `Guardar`.
4. Esperar missatge de confirmacio.

[CAPTURA: targeta de dades del titular en mode visualitzacio]

[CAPTURA: targeta de dades del titular en mode edicio]

### Consultar colaboradors

El modul pot mostrar tecnics i oficines relacionats amb el titular.

Aixo ajuda a entendre:

- quina oficina pot veure el titular
- quins tecnics hi estan assignats
- quins scopes tenen

[CAPTURA: bloc de colaboradors del titular dins modul Agricola]

### Consultar terres

Cada terra pot mostrar:

- codi SIGPAC complet
- municipi
- us SIGPAC
- cultiu
- superficie
- zona `ZV` o `ZNV`
- limit de nitrogen per hectarea
- auditoria d'ultima modificacio

[CAPTURA: llista de terres del modul Agricola]

### Crear terra

Passos:

1. Premre boto per crear terra.
2. Omplir municipi/codi, poligon, parcela i recinte.
3. Omplir superficie.
4. Seleccionar zona `ZV` o `ZNV`.
5. Informar cultiu o us SIGPAC si cal.
6. Premre `Crear`.

Validacions importants:

- el codi municipal ha de tenir format correcte
- poligon, parcela i recinte han de ser positius
- superficie no pot ser negativa
- el codi SIGPAC complet es genera a la base de dades

[CAPTURA: dialeg de creacio de terra buit]

[CAPTURA: dialeg de creacio de terra emplenat]

### Editar terra

Passos:

1. Localitzar la terra.
2. Premre `Editar`.
3. Modificar superficie, zona, municipi, us o cultiu.
4. Premre `Guardar`.

[CAPTURA: targeta de terra en mode edicio]

### Eliminar terra

Passos:

1. Localitzar la terra.
2. Premre `Eliminar`.
3. Confirmar eliminacio.

Pot fallar si la terra esta referenciada per aplicacions o entregues.

[CAPTURA: dialeg de confirmacio d'eliminacio de terra]

### Consultar aplicacions fertilitzants

Cada aplicacio pot mostrar:

- data
- terra relacionada
- tipus de fertilitzant
- procedencia
- volum `m3`
- `kg N/m3`
- `kg N`
- auditoria

[CAPTURA: bloc d'aplicacions fertilitzants]

### Crear aplicacio fertilitzant manual

Passos:

1. Premre boto per crear aplicacio.
2. Seleccionar terra.
3. Escriure data.
4. Escriure tipus de fertilitzant.
5. Escriure procedencia.
6. Informar valors de nitrogen:
   - `volum_m3`
   - `kg_n_m3`
   - `kg_n`
7. Premre `Crear`.

La base de dades i la logica validen la coherencia entre:

- volum
- concentracio
- nitrogen total

[CAPTURA: dialeg de creacio d'aplicacio fertilitzant]

### Editar aplicacio manual

Passos:

1. Localitzar l'aplicacio.
2. Confirmar que no es una aplicacio sincronitzada des de ramader.
3. Premre `Editar`.
4. Modificar dades.
5. Premre `Guardar`.

[CAPTURA: aplicacio fertilitzant manual en mode edicio]

### Aplicacions sincronitzades des del modul Ramader

Quan una entrega de dejeccions es registra al modul ramader, pot aparèixer com una aplicacio fertilitzant equivalent al modul agricola.

Aquestes aplicacions indiquen traçabilitat entre:

- granja origen
- terra desti
- entrega ramadera
- justificacio agricola

No s'han de tractar com aplicacions manuals normals.

[CAPTURA: aplicacio sincronitzada amb text o indicador d'origen ramader]

### Avisos de nitrogen

El modul calcula totals de nitrogen aplicat per terra i campanya. Pot mostrar avisos si se superen limits.

Zones:

- `ZV`: zona vulnerable, limit habitual 170 kg N/ha
- `ZNV`: zona no vulnerable, limit habitual 190 kg N/ha

[CAPTURA: avis o resum de nitrogen per terra]

## Modul Ramader

El modul ramader serveix per gestionar granges, bestiar, balances i entregues de dejeccions.

### Acces

Poden accedir-hi:

- admin
- oficina manager amb ambit suficient
- tecnic amb scope `ramader` o `comu`

[CAPTURA: pantalla inicial del modul Ramader]

### Seleccionar campanya

Igual que al modul agricola, la campanya defineix les dades que es mostren.

Passos:

1. Localitzar selector de campanya.
2. Seleccionar campanya.
3. Revisar balances i entregues associades.

[CAPTURA: selector de campanya al modul Ramader]

### Editar dades generals del titular

El modul ramader tambe permet editar dades generals del titular si hi ha permisos.

Dades:

- NIF
- nom
- telefon
- email
- adreca
- codi postal

[CAPTURA: targeta de titular dins modul Ramader]

### Consultar granges

Cada granja pot mostrar:

- marca oficial
- nom
- titular associat
- ultima modificacio

[CAPTURA: llista de granges del titular]

### Crear granja

Passos:

1. Premre boto per crear granja.
2. Informar nom.
3. Informar marca oficial.
4. Premre `Crear`.

La marca oficial ha de ser unica.

[CAPTURA: dialeg de creacio de granja]

### Editar granja

Passos:

1. Localitzar la granja.
2. Premre `Editar`.
3. Modificar nom o marca.
4. Premre `Guardar`.

[CAPTURA: targeta de granja en mode edicio]

### Eliminar granja

Passos:

1. Premre `Eliminar`.
2. Confirmar.

Pot fallar si la granja te censos, balances o entregues relacionades.

[CAPTURA: dialeg de confirmacio d'eliminacio de granja]

### Gestionar bestiar i fases productives

El sistema utilitza catalegs de:

- bestiar
- fases productives

El registre de cens relaciona:

- granja
- tipus de bestiar
- fase productiva
- cens

[CAPTURA: bloc de bestiar i fases productives dins modul Ramader]

### Crear cens de bestiar

Passos:

1. Premre boto per crear registre de bestiar.
2. Seleccionar granja.
3. Seleccionar tipus de bestiar.
4. Seleccionar fase productiva.
5. Informar cens.
6. Premre `Crear`.

[CAPTURA: dialeg de creacio de cens de bestiar]

### Editar cens

Passos:

1. Localitzar el registre.
2. Premre `Editar`.
3. Modificar cens.
4. Premre `Guardar`.

[CAPTURA: registre de cens en mode edicio]

### Balanc de nitrogen per granja i campanya

El balanc permet informar:

- estoc inicial de nitrogen
- kg N generat
- estoc final declarat

El sistema pot calcular i mostrar resum de balanc per campanya.

[CAPTURA: bloc de balanc de nitrogen per granja]

### Crear o editar balanc

Passos:

1. Localitzar la granja.
2. Premre `Configurar` o `Editar`.
3. Informar estoc inicial.
4. Informar kg N generat.
5. Informar estoc final declarat.
6. Premre `Guardar`.

[CAPTURA: formulari de balanc ramader en mode edicio]

### Entregues de dejeccions

Una entrega representa una sortida de dejeccions des d'una granja origen cap a una terra desti.

Dades principals:

- granja origen
- terra desti
- data
- tipus de fertilitzant
- volum
- kg N/m3
- kg N

[CAPTURA: llista d'entregues de dejeccions]

### Crear entrega

Passos:

1. Premre boto per crear entrega.
2. Seleccionar granja origen.
3. Seleccionar terra desti.
4. Escriure data.
5. Escriure tipus de fertilitzant.
6. Informar volum, kg N/m3 i/o kg N.
7. Premre `Crear`.

La base de dades comprova:

- permis ramader sobre titular origen
- possibilitat de referenciar la terra desti
- coherencia dels valors de nitrogen

[CAPTURA: dialeg de creacio d'entrega de dejeccions]

### Sincronitzacio amb aplicacio agricola

Quan es crea o edita una entrega, la base de dades pot crear o actualitzar una aplicacio fertilitzant equivalent sobre la terra desti.

Aixo permet:

- justificar la sortida ramadera
- veure l'impacte agricola
- mantenir traçabilitat entre granges i terres

[CAPTURA: mateixa entrega vista al modul Ramader]

[CAPTURA: aplicacio equivalent visible al modul Agricola]

### Editar entrega

Passos:

1. Localitzar entrega.
2. Premre `Editar`.
3. Modificar data, terra, tipus o nitrogen.
4. Premre `Guardar`.

[CAPTURA: entrega de dejeccions en mode edicio]

### Eliminar entrega

Passos:

1. Premre `Eliminar`.
2. Confirmar.

Si l'entrega tenia aplicacio sincronitzada, la relacio es gestiona a base de dades.

[CAPTURA: dialeg de confirmacio d'eliminacio d'entrega]

## Pantalla Preparar DAN

La pantalla `Preparar DAN` agrega informacio per titular i campanya. Esta pensada per revisar i preparar dades, no per editar-les.

### Que mostra

- dades identificatives del titular
- campanya seleccionada
- terres
- aplicacions fertilitzants
- granges
- censos
- balances de nitrogen
- entregues
- totals
- checklist automatica

[CAPTURA: pantalla Preparar DAN completa]

### Seleccionar campanya

Passos:

1. Obrir Preparar DAN des d'un titular.
2. Localitzar selector de campanya.
3. Seleccionar campanya.
4. Revisar dades agregades.

[CAPTURA: selector de campanya dins Preparar DAN]

### Consultar resum

El resum mostra una visio conjunta de la campanya.

Pot incloure:

- totals de superficie
- totals de nitrogen
- nombre de terres
- nombre d'aplicacions
- nombre de granges
- nombre d'entregues
- avisos o punts pendents

[CAPTURA: bloc de metrics o resum de Preparar DAN]

### Checklist automatica

La checklist ajuda a revisar si falten dades o si hi ha punts que cal comprovar manualment.

[CAPTURA: checklist automatica de Preparar DAN]

### Copiar resum

Passos:

1. Premre `Copiar resum`.
2. El text queda copiat al porta-retalls.
3. Enganxar-lo en document extern si cal.

[CAPTURA: boto Copiar resum]

### Copiar checklist

Passos:

1. Premre `Copiar checklist`.
2. El text queda copiat al porta-retalls.
3. Enganxar-lo en document extern si cal.

[CAPTURA: boto Copiar checklist]

## Pantalla Perfil

La pantalla `Perfil` mostra dades del tecnic autenticat.

### Dades visibles

- nom
- email
- telefon
- rol
- oficina
- informacio d'auditoria si esta disponible

[CAPTURA: pantalla Perfil en mode consulta]

### Editar perfil

Passos:

1. Entrar a `Perfil`.
2. Premre `Editar`.
3. Modificar dades permeses.
4. Premre `Guardar`.

[CAPTURA: Perfil en mode edicio]

### Canviar password propia

Passos:

1. Entrar a `Perfil`.
2. Premre `Canviar password`.
3. Escriure nova contrasenya.
4. Confirmar.

[CAPTURA: dialeg de canvi de password propia]

## Gestio de titulars

La gestio de titulars esta disponible per `admin` i `oficina_manager`.

### Que permet

- consultar titulars
- cercar titulars
- crear titular
- editar titular
- eliminar titular
- compartir titular amb una oficina
- eliminar comparticions

[CAPTURA: pantalla Gestio Titulars]

### Crear titular

Passos:

1. Entrar a `Gestio Titulars`.
2. Premre boto de crear.
3. Omplir:
   - NIF
   - nom o rao social
   - telefon
   - email
   - adreca
   - codi postal
4. Premre `Crear`.

[CAPTURA: dialeg de creacio de titular]

### Editar titular

Passos:

1. Localitzar titular.
2. Premre `Editar`.
3. Modificar camps.
4. Premre `Guardar`.

[CAPTURA: targeta de titular en gestio en mode edicio]

### Eliminar titular

Passos:

1. Localitzar titular.
2. Premre `Eliminar`.
3. Confirmar.

Pot fallar si hi ha dades relacionades que impedeixen l'eliminacio.

[CAPTURA: confirmacio d'eliminacio de titular]

### Compartir titular amb una oficina

Passos:

1. Localitzar titular.
2. Premre `Compartir`.
3. Seleccionar oficina receptora.
4. Seleccionar scope:
   - `lectura`
   - `agricola`
   - `ramader`
   - `comu`
5. Confirmar.

[CAPTURA: dialeg de comparticio de titular amb selector d'oficina i scope]

### Compartir amb oficina no visible

Si l'oficina receptora no apareix a la llista, es pot buscar pel email del manager receptor.

Passos:

1. Obrir dialeg de comparticio.
2. Escriure email del manager receptor.
3. Buscar oficina.
4. Seleccionar oficina trobada.
5. Assignar scope.
6. Confirmar comparticio.

[CAPTURA: dialeg de comparticio amb camp email del manager receptor]

### Eliminar comparticio

Passos:

1. Obrir comparticions d'un titular.
2. Localitzar oficina compartida.
3. Premre eliminar.
4. Confirmar.

[CAPTURA: llista de comparticions d'un titular]

## Gestio transversal de terres

La pantalla `Terres` permet gestionar terres fora del modul agricola d'un titular concret.

### Que permet

- consultar terres
- filtrar per titular
- crear terra
- editar terra
- eliminar terra

[CAPTURA: pantalla Terres]

### Filtrar per titular

Passos:

1. Obrir `Terres`.
2. Seleccionar titular o escriure filtre.
3. Revisar terres mostrades.

[CAPTURA: filtre de terres per titular]

### Crear terra des de gestio

Passos:

1. Premre crear terra.
2. Seleccionar titular si cal.
3. Omplir dades SIGPAC.
4. Omplir superficie i zona.
5. Premre `Crear`.

[CAPTURA: dialeg de crear terra des de pantalla Terres]

## Gestio de tecnics

La pantalla `Tecnics` permet administrar usuaris funcionals.

### Acces

Disponible per:

- admin
- oficina manager amb limitacions

[CAPTURA: pantalla Tecnics amb targetes de tecnics]

### Informacio visible

Cada tecnic pot mostrar:

- nom
- email
- telefon
- rol
- oficina
- si te login Auth vinculat

[CAPTURA: targeta d'un tecnic amb indicador Amb login o Sense login]

### Crear tecnic

Passos:

1. Entrar a `Tecnics`.
2. Premre boto de crear.
3. Omplir:
   - nom
   - email
   - telefon
   - oficina
   - rol
   - password si es vol crear login
4. Confirmar.

[CAPTURA: dialeg de creacio de tecnic]

### Crear login per tecnic

Quan es crea un tecnic amb email i password, l'app pot crear tambe l'usuari a Supabase Auth.

Resultat:

- es crea usuari a `auth.users`
- es crea o actualitza registre a `public.tecnic`
- `public.tecnic.user_id` queda vinculat a l'usuari Auth

[CAPTURA: Supabase Authentication > Users mostrant el tecnic nou]

[CAPTURA: Table Editor > tecnic mostrant user_id vinculat]

### Obrir detall de tecnic

Passos:

1. Localitzar tecnic.
2. Premre `Detalls`.
3. Revisar dades i assignacions.

[CAPTURA: pantalla Detall de tecnic]

### Editar tecnic

En el detall es poden modificar:

- nom
- email
- telefon
- oficina
- rol

Limitacions:

- un manager nomes pot gestionar tecnics de la seva oficina
- un manager no hauria de poder assignar rols administratius globals

[CAPTURA: detall de tecnic amb camps editables]

### Canviar password d'un tecnic

Passos:

1. Obrir `Tecnics`.
2. Localitzar tecnic.
3. Premre `Password` o entrar al detall.
4. Escriure nova password.
5. Confirmar.

[CAPTURA: dialeg de canvi de password administratiu]

### Assignar tecnic a titular

Passos:

1. Obrir detall de tecnic.
2. Localitzar bloc d'assignacions.
3. Seleccionar titular.
4. Seleccionar scope:
   - `lectura`
   - `agricola`
   - `ramader`
   - `comu`
5. Premre afegir assignacio.

[CAPTURA: bloc d'assignacions dins detall de tecnic]

### Eliminar assignacio

Passos:

1. Obrir detall del tecnic.
2. Localitzar assignacio.
3. Premre eliminar.
4. Confirmar.

[CAPTURA: confirmacio d'eliminacio d'assignacio]

### Eliminar tecnic

Passos:

1. Obrir `Tecnics`.
2. Localitzar tecnic.
3. Premre `Eliminar`.
4. Confirmar.

Pot eliminar o desactivar dades segons el flux implementat i permisos disponibles.

[CAPTURA: confirmacio d'eliminacio de tecnic]

## Gestio d'oficines

La pantalla `Oficines` permet gestionar oficines.

### Acces

- admin: gestio global
- oficina manager: edicio limitada a la seva oficina

[CAPTURA: pantalla Oficines]

### Crear oficina

Passos:

1. Entrar a `Oficines`.
2. Premre crear.
3. Escriure nom.
4. Confirmar.

[CAPTURA: dialeg de creacio d'oficina]

### Editar oficina

Passos:

1. Localitzar oficina.
2. Premre `Editar`.
3. Modificar nom.
4. Premre `Guardar`.

[CAPTURA: targeta d'oficina en mode edicio]

### Eliminar oficina

Passos:

1. Localitzar oficina.
2. Premre `Eliminar`.
3. Confirmar.

Pot fallar si hi ha tecnics o comparticions vinculades.

[CAPTURA: confirmacio d'eliminacio d'oficina]

## Auditoria

AgriSync guarda informacio d'auditoria en moltes taules.

Pot registrar:

- data de creacio
- usuari creador
- data d'ultima modificacio
- usuari modificador

Aquesta informacio ajuda a saber qui ha canviat dades i quan.

[CAPTURA: bloc d'auditoria visible en una targeta de titular, terra, granja o aplicacio]

## Supabase per a revisio tecnica

Aquest apartat no es necessari per a un usuari final normal, pero si per a defensa del projecte o comprovacio tecnica.

### Project Settings > API

Mostra on es troben:

- Project URL
- anon key
- service role key

No s'han de publicar claus reals en la memoria.

[CAPTURA: Supabase Project Settings > API amb claus tapades]

### Authentication > Users

Serveix per comprovar usuaris que poden fer login.

[CAPTURA: Authentication > Users amb usuaris demo]

### Table Editor

Serveix per revisar dades persistides.

Captures recomanades:

[CAPTURA: taula `tecnic` mostrant rols, email, actiu i user_id]

[CAPTURA: taula `titular` mostrant titulars demo]

[CAPTURA: taula `tecnic_titular` mostrant assignacions i scopes]

[CAPTURA: taula `oficina_titular_compartit` mostrant comparticions]

[CAPTURA: taula `terra` mostrant codi SIGPAC i zona]

[CAPTURA: taula `entrega_dejeccions` mostrant entregues]

[CAPTURA: taula `aplicacions_fertilitzants` mostrant aplicacions manuals i sincronitzades]

### SQL Editor

Serveix per executar scripts.

Captures recomanades:

[CAPTURA: SQL Editor amb `docs/sql/schema/agrisync_schema.sql`]

[CAPTURA: SQL Editor amb `docs/sql/seeds/agrisync_demo_seed.sql`]

[CAPTURA: resultat final del seed sense errors]

## Recorreguts recomanats per demostrar l'aplicacio

### Demo 1. Flux complet com admin

1. Entrar com `admin.demo@agrisync.com`.
2. Mostrar pantalla Titulars.
3. Obrir Gestio Titulars.
4. Crear o editar un titular.
5. Obrir Tecnics.
6. Mostrar un tecnic amb login.
7. Obrir Detall de tecnic.
8. Mostrar assignacions i scopes.
9. Obrir Oficines.
10. Mostrar gestio global.

[CAPTURA: admin amb totes les opcions de menu visibles]

### Demo 2. Flux agricola

1. Entrar com tecnic agricola.
2. Obrir un titular accessible.
3. Entrar a `Agricola`.
4. Seleccionar campanya.
5. Crear o editar terra.
6. Crear aplicacio fertilitzant.
7. Mostrar avisos de nitrogen.
8. Obrir Preparar DAN.

[CAPTURA: modul Agricola amb terra i aplicacio visibles]

### Demo 3. Flux ramader

1. Entrar com tecnic ramader.
2. Obrir titular accessible.
3. Entrar a `Ramader`.
4. Crear o revisar granja.
5. Revisar cens.
6. Crear entrega cap a una terra.
7. Obrir modul Agricola i veure aplicacio sincronitzada.

[CAPTURA: entrega creada al modul Ramader]

[CAPTURA: aplicacio sincronitzada al modul Agricola]

### Demo 4. Usuari de lectura

1. Entrar com `lectura.demo@agrisync.com`.
2. Mostrar titulars accessibles.
3. Obrir Preparar DAN.
4. Intentar identificar que no hi ha accions administratives.
5. Explicar que la base de dades tambe bloqueja escriptures.

[CAPTURA: menu d'usuari lectura sense opcions administratives]

## Bones practiques d'us

- Treballar sempre amb la campanya correcta.
- Revisar titular abans de crear dades agricoles o ramaderes.
- Evitar duplicar terres amb el mateix SIGPAC.
- Comprovar que una entrega ramadera te terra desti correcta.
- Revisar Preparar DAN abans de donar la informacio per bona.
- Usar scopes ajustats al que necessita cada tecnic.
- No compartir claus de Supabase.
- No utilitzar `service_role` en captures publiques.

## Problemes habituals i solucions

### L'aplicacio no arrenca

Possibles causes:

- falta Java/JDK
- Gradle no pot descarregar dependències
- configuracio local absent

Solucio:

- comprovar JDK
- comprovar connexio
- revisar `agrisync.properties`

[CAPTURA: error de configuracio incompleta dins l'app, si apareix]

### Login no funciona

Possibles causes:

- credencials incorrectes
- usuari no existeix a Auth
- tecnic no existeix a `public.tecnic`
- `user_id` no coincideix
- tecnic inactiu

Solucio:

- revisar Authentication > Users
- revisar taula `tecnic`
- reexecutar `reset -> schema -> seed` en entorn demo

### L'usuari veu pocs titulars

Possibles causes:

- no te assignacions
- scope nomes lectura
- titular no esta compartit amb la seva oficina
- usuari es de lectura

Solucio:

- revisar `tecnic_titular`
- revisar `oficina_titular_compartit`
- revisar rol i oficina del tecnic

### Error 403 o denegacio

Significat:

- la base de dades ha bloquejat l'operacio per RLS

Solucio:

- revisar rol
- revisar scope
- revisar oficina
- revisar si l'usuari pot gestionar aquell titular

### No es pot crear tecnic o canviar password

Possible causa:

- falta `SUPABASE_SERVICE_ROLE_KEY`

Solucio:

- revisar configuracio local
- comprovar clau a Supabase Project Settings > API

### No es pot eliminar un registre

Possible causa:

- hi ha dades relacionades
- restriccio de clau forana
- RLS ho bloqueja

Solucio:

- revisar dependències
- eliminar primer dades relacionades si te sentit
- comprovar permisos

## Glossari

| Terme | Significat |
|---|---|
| DAN | Declaracio anual relacionada amb gestio agraria/ramadera del projecte |
| Titular | Persona o entitat central sobre la qual es gestionen terres i granges |
| Tecnic | Usuari funcional que treballa titulars |
| Oficina | Grup organitzatiu de tecnics |
| Scope | Ambit de permis sobre un titular |
| RLS | Row Level Security, seguretat per fila a PostgreSQL |
| Auth | Sistema d'autenticacio de Supabase |
| PostgREST | API REST automatica sobre PostgreSQL |
| RPC | Funcio SQL exposada com endpoint |
| Terra | Parcel.la o recinte associat a titular |
| Granja | Explotacio ramadera del titular |
| Aplicacio fertilitzant | Aplicacio de fertilitzant sobre una terra |
| Entrega de dejeccions | Sortida ramadera cap a una terra desti |
| Campanya | Any o periode de treball DAN |
| ZV | Zona vulnerable |
| ZNV | Zona no vulnerable |

## Llista completa de captures recomanades

### Aplicacio

- Pantalla login buida.
- Login amb usuari demo escrit.
- Error de login.
- Home Titulars com admin.
- Home Titulars com tecnic.
- Home Titulars com lectura.
- Cerca de titulars activa.
- Targeta de titular amb accions.
- Modul Agricola complet.
- Selector de campanya agricola.
- Edicio de titular en agricola.
- Llista de terres.
- Dialeg crear terra.
- Llista d'aplicacions.
- Dialeg crear aplicacio.
- Aplicacio sincronitzada des d'entrega ramadera.
- Modul Ramader complet.
- Llista de granges.
- Dialeg crear granja.
- Cens de bestiar.
- Balanc de nitrogen.
- Dialeg crear entrega.
- Preparar DAN complet.
- Checklist de Preparar DAN.
- Botons copiar resum/checklist.
- Perfil en consulta.
- Perfil en edicio.
- Canvi de password propia.
- Gestio Titulars.
- Crear titular.
- Compartir titular amb oficina.
- Buscar oficina per email de manager.
- Gestio Terres.
- Gestio Tecnics.
- Targeta de tecnic amb login.
- Detall de tecnic.
- Assignacions de tecnic.
- Canvi de password administratiu.
- Gestio Oficines.

### Supabase

- Dashboard del projecte.
- Project Settings > API amb claus tapades.
- Authentication > Users.
- SQL Editor executant reset.
- SQL Editor executant schema.
- SQL Editor executant seed.
- Table Editor > `tecnic`.
- Table Editor > `titular`.
- Table Editor > `tecnic_titular`.
- Table Editor > `oficina_titular_compartit`.
- Table Editor > `terra`.
- Table Editor > `granja`.
- Table Editor > `entrega_dejeccions`.
- Table Editor > `aplicacions_fertilitzants`.



## Tancament

Aquest manual cobreix l'us complet d'AgriSync des de la perspectiva d'usuari i de demo tecnica. Per entendre el funcionament intern del codi i la base de dades, cal complementar-lo amb:

- `docs/arquitectura/arquitectura_i_codi.md`
- `docs/sql/model_de_dades_i_bdd.md`
- `docs/arquitectura/permisos_i_seguretat.md`
- `docs/funcional/flux_operatiu_i_moduls.md`
- `docs/presentacio/guia_defensa.md`
