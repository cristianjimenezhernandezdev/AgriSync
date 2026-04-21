# Arquitectura i codi

## Visio general

AgriSync es un client desktop que consumeix Supabase com a backend de dades i autenticacio. La logica de permisos reals viu a la base de dades; la UI coordina navegacio, formularis i peticions REST.

## Capa d'aplicacio

- `composeApp/src/commonMain/kotlin/cat/agrisync/App.kt`
  Punt d'entrada funcional. Decideix login, sessio, navegacio i pantalles.
- `composeApp/src/commonMain/kotlin/cat/agrisync/ui/`
  Pantalles Compose del MVP.
- `composeApp/src/commonMain/kotlin/cat/agrisync/viewmodel/`
  Estat de pantalla i coordinacio d'accions.
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/`
  Repositoris, client REST, auth i DTOs.
- `composeApp/src/jvmMain/kotlin/cat/agrisync/data/`
  Configuracio JVM i persistencia local de sessio.

## Flux tecnic principal

1. `App.kt` carrega configuracio i inicialitza `AuthService`.
2. El login autentica contra Supabase Auth.
3. `get_my_tecnic()` resol el tecnic funcional associat a `auth.uid()`.
4. Les pantalles consumeixen repositoris REST.
5. La BDD valida visibilitat i escriptura amb RLS i funcions helper.

## Navegacio actual

Pantalles principals:

- `Titulars`
- `Preparar DAN`
- `Modul Agricola`
- `Modul Ramader`
- `Perfil`
- `Gestio Titulars`
- `Terres`
- `Tecnics`
- `Oficines`

Les pantalles de gestio queden reservades a `admin` i `oficina_manager`.

## Domini principal

El centre del model es el `titular`. Al seu voltant hi ha:

- dades identificatives i de contacte del titular: `nif`, `telefon`, `email`, `adreca` i `codi_postal`
- `terra` i `aplicacions_fertilitzants` per la part agricola
- `granja`, `granja_bestiar` i `entrega_dejeccions` per la part ramadera
- `dan_declaracio` per separar treball per campanya
- `tecnic_titular` i `oficina_titular_compartit` per permisos i comparticions

Els moduls de titular consumeixen aquestes relacions per mostrar al tecnic quines oficines i quins altres tecnics tenen abast sobre aquell titular, agregant scopes i comparticions visibles via RLS.

## Decisions importants

- el client no implementa permisos de negoci com a font de veritat
- la BDD te auditoria basica amb `created_*` i `updated_*`
- el `codi_postal` del titular es desa separat de l'adreca per facilitar cerques i filtratges futurs
- el `service_role` es necessari per crear usuaris Auth, canviar passwords des de gestio i resoldre etiquetes d'auditoria
- la sessio es persisteix en local per a desktop

## Fitxers especialment rellevants

- `composeApp/src/commonMain/kotlin/cat/agrisync/App.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/AuthService.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/RestClient.kt`
- `composeApp/src/commonMain/kotlin/cat/agrisync/data/TecnicRepository.kt`
- `docs/sql/schema/agrisync_schema.sql`

## Que no fa l'app encara

- no genera el PDF oficial de la DAN
- no calcula tot el nitrogen ramader normatiu a partir del cens
- no cobreix tota la casuistica administrativa de receptors externs des de la UI
