# AgriSync

## Descripció del projecte

**AgriSync** és una aplicació d’escriptori orientada a la gestió centralitzada de la **Declaració Anual de Nitrogen (DAN)** per a explotacions agrícoles i ramaderes.  
El projecte neix com a **Projecte Final de Cicle (DAM/DAW)**, amb l’objectiu de crear un **MVP funcional** que resolgui una necessitat real del sector agrari i que tingui projecció de futur com a producte professional.

Actualment, la gestió de la DAN es fa sovint mitjançant fulls de càlcul i documents dispersos, fet que provoca duplicació de dades, incoherències i manca de traçabilitat. AgriSync pretén solucionar aquest problema mitjançant una aplicació amb base de dades centralitzada i control d’accessos.

---

## Objectius del projecte

- Centralitzar les dades relacionades amb la DAN en una única base de dades.
- Facilitar la gestió de titulars, terres, granges, bestiar i aplicacions de nitrogen.
- Garantir la coherència i la integritat de les dades.
- Permetre una futura ampliació cap a aplicació mòbil (Android).
- Desenvolupar el projecte seguint metodologia **SCRUM**.

---

## Tipus de projecte

- **Aplicació d’escriptori**
- Arquitectura **client-servidor**
- Projecte **Kotlin Multiplatform** amb enfocament escalable

---

## Tecnologies utilitzades

### Frontend
- **Kotlin Multiplatform**
- **Compose Multiplatform (Desktop)**
- IntelliJ IDEA

### Backend i base de dades
- **Supabase**
  - PostgreSQL com a sistema gestor de base de dades
  - Autenticació i control d’accessos
  - API REST automàtica

### Gestió del projecte
- **Jira** (SCRUM, sprints i seguiment)
- **Confluence** (documentació)
- **GitHub** (control de versions)

---

## Arquitectura del sistema

L’aplicació segueix una arquitectura **client-servidor**:

- El client és una aplicació d’escriptori desenvolupada amb Kotlin i Compose.
- El servidor és Supabase, que proporciona la base de dades PostgreSQL i l’API d’accés a les dades.
- La comunicació entre l’aplicació i la base de dades es fa mitjançant serveis API.

L’arquitectura està pensada per permetre, en el futur, l’addició d’una aplicació Android reutilitzant la lògica compartida.

---

## Metodologia de treball

El projecte es desenvolupa seguint la metodologia **SCRUM**:

- Organització del treball en **sprints setmanals**
- Backlog gestionat amb Jira
- Seguiment continu del progrés
- Control de versions amb Git i commits regulars

---

## Estat del projecte

🟢 **MVP funcional i demostrable**

Actualment el projecte ja disposa de:
- Aplicació desktop funcional
- Login real amb Supabase Auth
- Control d’accés amb RLS
- Mòdul de titulars
- Mòdul agrícola
- Mòdul ramader
- Pantalla `Preparar DAN` amb resum unificat per titular i campanya
- Còpia ràpida al porta-retalls del resum DAN i de la checklist final
- Compartició de titulars entre oficines per `scope` agrícola o ramader
- Entregues de dejeccions cap a titulars o terres accessibles d'altres titulars
- Pantalles de gestió per oficines, tècnics, titulars i terres
- Alta i baixa de dades principals directament dins dels mòduls

---

## Estructura del repositori

AgriSync/
├── docs/ # Documentació del projecte
├── composeApp/ # Aplicació Compose Multiplatform Desktop
├── SQLAgriSync.sql # Esquema SQL i permisos
├── fix_permisos.sql # Reaplica grants i execució de funcions helper
├── seed_complet.sql # Seed bàsic de prova del MVP
├── seed_final_demo.sql # Seed ampliat de demo gairebé com a producció
├── agrisync.properties.example # Plantilla de configuració per a l'exe
├── build.gradle.kts
└── README.md

---

## Estat actual (MVP Desktop)

Aplicacio KMP + Compose Desktop connectada a Supabase amb:

- Login / logout amb `email + password` (Supabase Auth)
- Sessio persistent local (Desktop) + refresh automàtic de token
- Home de titulars carregada directament des de `titular` + `tecnic_titular`
- Navegacio simple: `Login` -> `Titulars` -> `Modul Agricola` / `Modul Ramader`
- Treball per campanya a `Modul Agricola`, `Modul Ramader` i `Preparar DAN`
- Resum DAN amb dades agricoles i ramaderes en una sola pantalla
- Còpia estructurada del resum i de la checklist des de `Preparar DAN`
- Pantalla `El meu perfil` (nom, rol, oficina, email)
- Gestio de `Oficines`, `Tecnics`, `Titulars` i `Terres`
- Errors visibles per casos de permisos RLS (`401/403`)

## SQL de base de dades

Aplica `SQLAgriSync.sql` al SQL Editor de Supabase.

Inclou:

- Taules i triggers d'auditoria
- Funcions helpers de permisos (`can_read_titular`, `can_write_*`)
- RLS policies
- Neteja inicial d'objectes previs per recrear l'esquema
- Esquema MVP simplificat sense elements fora d'ús

Scripts SQL útils que es mantenen al projecte:

- `SQLAgriSync.sql`: esquema principal
- `fix_permisos.sql`: utilitat per reaplicar grants i execució de funcions helper
- `seed_complet.sql`: seed ràpid amb pocs usuaris per validar el MVP
- `seed_final_demo.sql`: seed ampliat amb diverses oficines, tècnics i titulars compartits entre oficines

Per als usuaris de demo d'Auth, és més segur crear-los des de `Authentication > Users` de Supabase o via Admin API amb `service_role`.
La inserció manual directa a `auth.users` i `auth.identities` pot quedar incompatible amb canvis interns de Supabase Auth i acabar donant errors com `Database error querying schema` en fer login.
Si passa, recrea els usuaris demo i torna a executar `seed_final_demo.sql` o `fix_user_ids.sql` per resincronitzar `public.tecnic.user_id`.

## Configuracio de l'app

L'aplicació pot llegir la configuració de dues maneres:

### Opcio 1. Variables d'entorn o propietats JVM

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SUPABASE_SERVICE_ROLE_KEY`

### Opcio 2. Fitxer `agrisync.properties`

Si no hi ha variables d'entorn, la versió JVM també busca un fitxer anomenat `agrisync.properties`:

- al directori des d'on s'executa l'app
- o al costat del `.exe` / `.jar`

Pots partir de `agrisync.properties.example` i crear el fitxer real així:

```properties
SUPABASE_URL=https://<PROJECT>.supabase.co
SUPABASE_ANON_KEY=<ANON_KEY>
SUPABASE_SERVICE_ROLE_KEY=<SERVICE_ROLE_KEY>
```

## Execucio (Desktop)

### Desenvolupament amb PowerShell

```powershell
$env:SUPABASE_URL="https://<PROJECT>.supabase.co"
$env:SUPABASE_ANON_KEY="<ANON_KEY>"
$env:SUPABASE_SERVICE_ROLE_KEY="<SERVICE_ROLE_KEY>"
./gradlew :composeApp:run
```

### Entrega als professors

Per una entrega en `.exe`, la manera pràctica és distribuir:

- `AgriSync.exe`
- `agrisync.properties`

al mateix directori.

Així l'app arrenca sense haver de configurar variables d'entorn a l'ordinador del professor.

## Estructura rellevant

- `composeApp/src/commonMain/kotlin/cat/agrisync/data`: auth, client REST i repositoris
- `composeApp/src/commonMain/kotlin/cat/agrisync/viewmodel`: presenters + `UiState`
- `composeApp/src/commonMain/kotlin/cat/agrisync/ui`: pantalles Compose Desktop
- `composeApp/src/jvmMain/kotlin/cat/agrisync/data/JvmEnvConfig.kt`: lectura de configuració JVM i fitxer `agrisync.properties`
- `composeApp/src/jvmMain/kotlin/cat/agrisync/data/SessionPersistence.kt`: persistencia de sessio local

---

## Desenvolupador

- **Nom:** Cristian Jimenez Hernandez
- **Estudis:** Desenvolupament d’Aplicacions Multiplataforma (DAM)
- **Centre:** IES Campalans
- **Curs:** Projecte Final de Cicle

---

## Notes finals

Aquest projecte està concebut com un **producte real**, amb un MVP funcional per a l’àmbit acadèmic i una arquitectura preparada per a la seva evolució professional un cop finalitzada l’avaluació del curs.
