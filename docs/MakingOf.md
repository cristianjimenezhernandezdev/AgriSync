# MakingOf AgriSync

## 1. Objectiu del projecte

AgriSync neix per resoldre un problema molt concret: la preparacio de la DAN acostuma a repartir-se entre fulls de calcul, dades disperses i comprovacions manuals. El projecte planteja un MVP d'escriptori que centralitza la informacio, permet treball multiusuari i deixa el control real d'acces a la base de dades.

Els objectius que s'han perseguit han estat aquests:

- centralitzar dades en una sola base de dades
- tenir autenticacio real
- separar permisos per rol, oficina i titular
- diferenciar modul agricola i modul ramader
- donar una base suficient per preparar una DAN

## 2. Decisio de producte

El centre funcional del sistema es el `titular`.

Al voltant del titular hi ha tres espais de treball:

- modul agricola
- modul ramader
- pantalla `Preparar DAN`

La decisio clau del MVP ha estat no intentar automatitzar tot el document oficial, sino:

- recollir dades base coherents
- calcular alguns valors derivats
- ajudar el tecnic a treballar per campanya
- presentar la informacio d'una forma util per al trasllat final
- permetre copiar un resum estructurat i una checklist final sense dependre encara d'un PDF

## 3. Tecnologies escollides

### Client

- Kotlin Multiplatform
- Compose Multiplatform Desktop
- Ktor Client

### Backend i dades

- Supabase
- PostgreSQL
- Supabase Auth
- REST API de Supabase
- Row Level Security

La combinacio ha permes concentrar l'esforc en model de dades, permisos i casos d'us reals, en lloc de muntar backend i autenticacio des de zero.

## 4. Arquitectura general

### 4.1. Client desktop

La part client viu a `composeApp` i s'encarrega de:

- pantalles i navegacio
- formularis i validacions basiques
- consultes REST a Supabase
- missatges d'error i confirmacions
- resum funcional de dades per titular i campanya

### 4.2. Capa de dades

Entre la UI i Supabase hi ha una capa de repositoris i serveis:

- `AuthService`
- `SupabaseAuthApi`
- `RestClient`
- `AgricolaRepository`
- `RamaderRepository`
- `DanPreparationRepository`
- `TecnicRepository`
- `OficinaRepository`
- `TitularManagementRepository`
- `AccessRepository`

La separacio ha ajudat a mantenir la logica de negoci fora de les pantalles.

### 4.3. Base de dades

La base de dades queda concentrada a `SQLAgriSync.sql`.

Aquest script:

- elimina objectes previs del projecte
- recrea taules, indexes i triggers
- defineix funcions helper
- activa RLS
- recrea totes les policies del MVP

La decisio de tenir un fitxer principal reconstructiu fa que el projecte sigui repetible des de zero.

## 5. Model funcional actual

El model del MVP es basa en aquestes entitats:

- `oficina`
- `tecnic`
- `titular`
- `tecnic_titular`
- `oficina_titular_compartit`
- `dan_declaracio`
- `terra`
- `aplicacions_fertilitzants`
- `granja`
- `bestiar`
- `fase_productiva`
- `granja_bestiar`
- `entrega_dejeccions`

L'esquema ha anat retallant elements antics o no usats per quedar-se amb la part que realment fa servir l'app.

## 6. Dos ajustos funcionals importants del projecte

### 6.1. Permisos per oficina mes coherents

Una de les revisions importants del projecte ha estat evitar que `oficina_manager` actuï com si fos gairebe global.

Ara el model fa servir `can_manage_office_titular(...)` i limita el gestor a:

- titulars creats per ell
- o titulars que ja tenen tecnics actius de la seva oficina assignats

Aixo acosta molt mes l'app al comportament esperat per oficines.

En una evolucio posterior del mateix MVP s'ha afegit tambe `oficina_titular_compartit`, que permet:

- compartir nomes la part agricola d'un titular amb una altra oficina
- compartir nomes la part ramadera
- o compartir `comu` si es vol una col.laboracio mes ampla

### 6.2. Treball real per campanya

La segona millora funcional forta ha estat convertir la campanya en un element real del flux.

Actualment:

- `TitularAgricolaViewModel` carrega aplicacions per campanya
- `TitularRamaderViewModel` carrega entregues per campanya
- `DanPreparationViewModel` resumeix per campanya
- les altes noves creen o reutilitzen la `dan_declaracio` correcta

La UI no entra des d'una pantalla exclusiva de campanyes, pero el comportament funcional ja esta centrat en la campanya activa.

## 7. Flux d'arrencada

Quan l'app arrenca:

1. intenta llegir `SUPABASE_URL`, `SUPABASE_ANON_KEY` i `SUPABASE_SERVICE_ROLE_KEY`
2. si no hi son, la versio JVM prova `agrisync.properties`
3. construeix els serveis d'app
4. recupera sessio local si existeix
5. refresca token
6. carrega el tecnic autenticat
7. obre la navegacio interna

Aquest suport dual a variables d'entorn i fitxer de propietats es va mantenir per fer l'entrega academica mes practicable.

## 8. Flux funcional dels moduls

### 8.1. Titulars

La pantalla principal mostra titulars accessibles segons RLS i assignacions.

Des d'aqui es pot:

- cercar
- entrar al modul agricola
- entrar al modul ramader
- obrir `Preparar DAN`

### 8.2. Modul agricola

Permet:

- editar dades basiques del titular
- gestionar terres
- gestionar aplicacions fertilitzants
- treballar sobre la campanya activa

Tambe mostra zona nitrogen, limit `kg N/ha` i calculs per campanya.

### 8.3. Modul ramader

Permet:

- gestionar granges
- gestionar cens per bestiar i fase
- gestionar entregues de dejeccions
- treballar sobre la campanya activa
- enviar una entrega a titulars o terres accessibles d'altres titulars

### 8.4. Preparar DAN

`Preparar DAN` no substitueix el document oficial. El que fa es:

- unificar la lectura de dades
- resumir terres, aplicacions, granges, censos i entregues
- recalcular totals per campanya
- deixar visibles camps que encara requereixen revisio manual
- generar una checklist automatica de completitud
- permetre copiar al porta-retalls un resum estructurat o nomes la checklist

## 9. Estrategia de dades de prova

Per fer demostracions i proves s'han mantingut dos seeds:

- `seed_complet.sql`
- `seed_final_demo.sql`

La seed important per a defensa i proves completes es `seed_final_demo.sql`, perque incorpora:

- diversos rols
- diverses oficines
- titulars compartits
- historics 2024 i 2025
- dades suficients per als tres grans espais de treball

Per reconstruccio des de zero s'ha afegit tambe `reset_auth_seed_users.sql`, que permet netejar els usuaris Auth dels seeds sense tocar la resta del projecte de Supabase.

## 10. Seguretat i permisos

La seguretat del sistema no depen de la UI.

Es basa en:

- `auth.uid()` de Supabase Auth
- la taula funcional `public.tecnic`
- scopes a `tecnic_titular`
- comparticions interoficina a `oficina_titular_compartit`
- funcions helper de permisos
- policies RLS per taula

Els `grant` permeten intentar l'operacio, pero la decisio final la pren l'RLS.

Un ajust important recent ha estat reforcar la policy de `tecnic` per evitar que l'autoedicio del perfil pogues modificar camps sensibles com rol, oficina o estat.

## 11. Paquet SQL i reconstruccio

El paquet SQL actual queda aixi:

- `SQLAgriSync.sql`
- `fix_permisos.sql`
- `reset_auth_seed_users.sql`
- `seed_complet.sql`
- `seed_final_demo.sql`
- `fix_user_ids.sql`

La combinacio d'aquests fitxers permet:

- netejar usuaris Auth de prova
- recrear l'esquema public
- reaplicar grants si cal
- carregar dades de demo
- resincronitzar `user_id` si els usuaris s'han recreat

## 12. Limitacions actuals del MVP

Tot i la millora del flux per campanya, el projecte encara no arriba a una DAN completa de nivell productiu.

Falta principalment:

- model normatiu agricola mes ric
- calcul de nitrogen generat a partir del cens
- estat inicial i final de fossa
- traçabilitat directa entre entrega concreta i aplicacio concreta
- exportacio a fitxer o PDF i automatitzacio del document oficial

## 13. Resultat final del projecte

El valor d'AgriSync no es nomes visual.

El projecte aporta:

- autenticacio real
- persistencia real
- permisos reals a nivell de BDD
- treball multiusuari
- modul agricola i ramader coherents
- una reconstruccio repetible des de zero
- una seed rica per demostracio
- una sortida final rapida des de `Preparar DAN` per enganxar el resum fora de l'app

Despres de tancar les millores d'ambit per oficina i de treball per campanya, la base del MVP ja aguanta be. El que queda pendent es sobretot profunditat funcional, no pas corregir un nucli trencat.
