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

🟡 **En desenvolupament (fase inicial)**

Actualment el projecte es troba en la fase de:
- Preparació de l’entorn de desenvolupament
- Definició de l’arquitectura
- Documentació inicial i planificació

---

## Estructura del repositori

AgriSync/
├── docs/ # Documentació del projecte
├── shared/ # Codi compartit (Kotlin Multiplatform)
├── desktop/ # Aplicació desktop (Compose)
├── build.gradle.kts
└── README.md

---

## Estat actual (MVP Desktop)

Aplicacio KMP + Compose Desktop connectada a Supabase amb:

- Login / logout amb `email + password` (Supabase Auth)
- Sessio persistent local (Desktop) + refresh automàtic de token
- Home de titulars assignats via vista `public.v_titular_access`
- Navegacio simple: `Login` -> `Titulars` -> `Modul Agricola` / `Modul Ramader`
- Pantalla `El meu perfil` (nom, rol, oficina, email)
- Errors visibles per casos de permisos RLS (`401/403`)

## SQL de base de dades

Aplica `SQLAgriSync.sql` al SQL Editor de Supabase.

Inclou:

- Taules i triggers d'auditoria
- Funcions helpers de permisos (`can_read_titular`, `can_write_*`)
- RLS policies
- Vista `v_titular_access` per la Home del client Desktop

## Entorn (ENV)

Defineix aquestes variables abans d'arrencar l'app:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`

## Execucio (Desktop)

Exemple amb PowerShell:

```powershell
$env:SUPABASE_URL="https://<PROJECT>.supabase.co"
$env:SUPABASE_ANON_KEY="<ANON_KEY>"
./gradlew :composeApp:run
```

## Estructura rellevant

- `composeApp/src/commonMain/kotlin/cat/agrisync/data`: auth, client REST i repositoris
- `composeApp/src/commonMain/kotlin/cat/agrisync/viewmodel`: presenters + `UiState`
- `composeApp/src/commonMain/kotlin/cat/agrisync/ui`: pantalles Compose Desktop
- `composeApp/src/jvmMain/kotlin/cat/agrisync/data/JvmEnvConfig.kt`: lectura d'ENV
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
