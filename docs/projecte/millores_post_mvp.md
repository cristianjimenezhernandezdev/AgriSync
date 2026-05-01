# Millores previstes per versions posteriors al MVP

## Objectiu del document

Aquest document no descriu funcionalitats ja implementades, sino una proposta d'evolucio posterior al MVP actual.

L'objectiu es doble:

- deixar constancia de limits coneguts detectats durant el desenvolupament i les proves
- justificar davant el tribunal que el projecte te recorregut real, criteri tecnic i una linia clara de millora

El criteri seguit en el MVP ha estat prioritzar una base funcional, coherent i demostrable abans d'afegir mes complexitat. Per tant, les millores descrites aqui no son omissions arbitraries, sino decisions ajornades a una fase posterior per mantenir el projecte acotat i presentable.

## Prioritats de millora

Les millores futures es poden agrupar en cinc blocs:

1. seguretat i desplegament real
2. correccions funcionals detectades a la practica
3. usabilitat i millores visuals
4. ampliacio funcional del domini DAN
5. qualitat, mantenibilitat i operacio

## 1. Seguretat i desplegament real

### 1.1 Eliminacio de la `service_role_key` del client instal·lat

Per poder distribuir el MVP com a aplicacio instal·lable, s'ha habilitat una configuracio que permet que el client arrenqui amb les claus necessaries.

Aixo es practic per una demo o una entrega de MVP, pero no es la solucio adequada per una distribucio real a usuaris finals, especialment perque la `SUPABASE_SERVICE_ROLE_KEY` no hauria d'estar disponible dins una aplicacio desktop instal·lada.

Millora prevista:

- moure les operacions administratives sensibles a una capa backend controlada o a Supabase Edge Functions
- deixar al client nomes `SUPABASE_URL` i `SUPABASE_ANON_KEY`
- fer que la creacio d'usuaris, el reset de password i l'eliminacio de comptes es resolguin fora del client

Benefici:

- evita exposar credencials privilegiades
- fa el producte molt mes robust per un entorn real
- separa millor les responsabilitats entre client i serveis d'administracio

### 1.2 Millora del model de configuracio d'instal·lacio

Actualment el sistema ja pot funcionar en build, pero una versio posterior hauria de permetre:

- pantalla inicial de configuracio guiada
- comprovacio de connexio amb Supabase abans d'entrar al login
- fitxer de configuracio extern gestionat per l'instal·lador o per una eina de suport
- missatges d'error orientats a usuari no tecnic

Benefici:

- simplifica el desplegament a oficines o equips nous
- redueix dependencias manuals en la posada en marxa

### 1.3 Rotacio i separacio d'entorns

En una evolucio natural del projecte seria convenient separar:

- entorn local
- entorn demo
- entorn preproduccio
- entorn produccio

Amb aquesta separacio es podria:

- provar canvis sense afectar dades compartides
- controlar millor claus i permisos
- fer migracions amb menys risc

## 2. Correccions funcionals detectades

### 2.1 Conversio correcta de dates i hores d'auditoria

S'ha detectat que les dates i hores d'actualitzacio es poden mostrar amb aproximadament dues hores menys que l'hora real del sistema.

Causa probable dins l'estat actual del MVP:

- els valors `updated_at` de base de dades es guarden com a `timestamptz`
- a la UI es mostren en format textual gairebe directe, sense conversio explicita de zona horaria local

Conseqüencia funcional:

- l'usuari veu una hora valida tecnicament, pero no alineada amb la seva hora local real
- pot generar confusio en revisions, auditoria i defensa del flux de treball

Millora prevista:

- convertir sempre els `timestamp` des de UTC a la zona local del dispositiu
- unificar el format de visualitzacio de dates i hores a tota l'aplicacio
- cobrir-ho amb tests especifics de timezone

Resultat esperat:

- coherencia entre hora del sistema, hora mostrada i hora percebuda per l'usuari

### 2.2 Recalcul immediat del triplet `Kg N`, `Volum m3`, `Kg N/m3`

El model funcional del projecte ja contempla que, si l'usuari informa dos dels tres camps:

- `Kg N`
- `Volum m3`
- `Kg N/m3`

el tercer camp es pugui deduir automaticamente.

Tot i aixi, en l'estat actual del MVP s'ha observat un comportament millorable: quan el tercer camp ja te valor, el recalcul no sempre es refresca de manera natural mentre s'editen els altres dos, i sovint l'usuari ha d'esborrar el camp calculat per forcar-ne el recalcul.

Impacte:

- trenca la sensacio d'automatisme
- pot generar dubtes sobre quin valor es considera manual i quin calculat
- afegeix una friccio innecessaria a l'edicio

Millora prevista:

- definir clarament quin camp s'esta editant i quin queda bloquejat com a calculat
- recalcular en viu el tercer camp quan canvien els altres dos
- afegir una logica de prioritat o "camp font" per evitar ambiguitats
- reforcar-ho amb tests d'interaccio i regressio

### 2.3 Millora del tractament d'errors de domini

El MVP ja mostra errors funcionals basics, pero en versions posteriors seria convenient:

- distingir millor errors de validacio, errors de xarxa i errors de permisos
- oferir missatges mes contextuals segons el modul
- suggerir una accio concreta a l'usuari quan el problema sigui recuperable

Exemples:

- "no tens permis per modificar aquest titular"
- "la connexio amb Supabase ha fallat"
- "cal informar almenys dos dels camps de nitrogen"

## 3. Usabilitat i millores visuals

### 3.1 Refinament visual general

La UI actual es suficient per un MVP funcional, pero encara te marge de millora en aspectes de presentacio i consistencia.

Linies de millora:

- jerarquia visual mes clara entre bloc principal, subbloc i accions
- espaiat i alineacions mes uniformes
- millor tractament dels colors d'avisos, errors i confirmacions
- revisio de densitat visual en pantalles amb moltes targetes
- refinament dels dialogs perque siguin mes nets i llegibles

Benefici:

- millor percepcio de producte acabat
- menys fatiga visual en us intensiu

### 3.2 Reforc de feedback d'usuari

En versions posteriors convindria millorar:

- confirmacions de guardat mes visibles
- indicadors de carrega mes consistents
- prevencio de dobles clics o dobles enviaments
- avisos de canvis no guardats en formularis llargs

### 3.3 Millora de l'experiencia en formularis

Punts recomanables:

- validacio en viu de camps obligatoris
- filtres i mascara d'entrada mes estrictes en camps numerics i de dates
- millor navegacio per teclat
- seleccio i cerca mes rapida en llistes llargues
- estandarditzar placeholders, etiquetes i textos d'ajuda

### 3.4 Iconografia i branding

Ja s'ha introduit suport d'icones i packaging, pero una versio posterior podria afegir:

- recursos de marca definitius per tots els sistemes operatius
- revisio del nom de paquet i metadades del producte
- millor poliment de l'instal·lador i de la carpeta distribuible

## 4. Ampliacio funcional del domini DAN

### 4.1 Exportacio formal de resum o document DAN

Actualment la pantalla `Preparar DAN` funciona com a resum operatiu i checklist. Una evolucio natural seria:

- exportar un resum formal en PDF
- generar una plantilla d'impressio
- oferir una vista preparada per trasllat administratiu

### 4.2 Calcul normatiu mes complet

El MVP cobreix una base funcional bona, pero encara no implementa tot el calcul normatiu possible.

Millores futures:

- refinament del calcul de nitrogen generat per bestiar
- validacions normatives mes detallades segons context
- comprovacions de coherencia mes properes al procediment final de DAN

### 4.3 Tracabilitat completa entre origen i desti

Un dels limits reconeguts del MVP es que la relacio entre:

- entrega concreta
- aplicacio concreta
- balanc ramader

encara es tracta de forma simplificada.

Una versio posterior podria:

- vincular millor cada entrega amb el seu desti funcional
- explicar millor l'origen del balanc calculat
- reforcar la justificacio de moviments de dejeccions

### 4.4 Completitud dels camps finals de DAN

La pantalla `Preparar DAN` ja ajuda a revisar dades, pero es pot estendre amb:

- mes comprovacions automatiques
- mes avisos contextuals
- ajuda especifica per camps finals no resolts
- suport a mes casuistiques documentals

## 5. Qualitat, mantenibilitat i operacio

### 5.1 Cobertura de tests superior

El projecte ja incorpora tests utiles, especialment en dates, compatibilitat d'esquema i calculs de nitrogen. En una fase posterior seria desitjable ampliar:

- tests de repositoris
- tests de viewmodels
- tests de regressio sobre fluxos de formulari
- proves orientades a errors reals reportats pels usuaris

### 5.2 Observabilitat i diagnosi

Una millora clara seria professionalitzar el seguiment d'incidencies:

- logs mes estructurats
- menys `println` de debug dispersos
- millor separacio entre missatge intern tecnic i missatge final d'usuari
- rastre de fallades de xarxa, permisos i integracio

### 5.3 Revisio de deprecacions i deute tecnic de framework

Durant la compilacio ja apareixen alguns avisos de deprecacio o APIs experimentals.

Millores futures:

- adaptar l'uso del portaretalls a les APIs noves de Compose
- revisar APIs `expect/actual` marcades com a beta
- reduir punts frikils de compatibilitat abans de creixer en funcionalitats

### 5.4 Instal·lacio i actualitzacio de versions

La generacio de l'instal·lador ja es pot fer, pero una versio posterior podria afegir:

- procediment d'actualitzacio entre versions
- conservacio controlada de configuracio local
- millor estrategia de numeracio de versions
- paquet portable i paquet instal·lable ben diferenciats

## Proposta de prioritzacio posterior al tribunal

Si calguessin prioritzar les millores en fases petites, l'ordre recomanat seria aquest:

1. seguretat del client i eliminacio de `service_role` del build distribuible
2. correccio de timezone a dates i hores mostrades
3. recalcul mes natural dels camps `Kg N`, `Volum m3` i `Kg N/m3`
4. exportacio formal del resum DAN
5. millores visuals i de formulari
6. ampliacio de tests i observabilitat

## Conclusio

El MVP actual compleix la seva funcio principal: demostrar una aplicacio desktop real, amb autenticacio, permisos, persistencia i fluxos operatius coherents.

Les millores descrites aqui mostren que el projecte no esta tancat en un estat de maqueta, sino que te un cami clar de maduracio.

En defensa, aquest document permet argumentar que:

- s'han identificat correctament els limits del MVP
- hi ha criteri per diferenciar entre imprescindible i ajornable
- el projecte es escalable tant funcionalment com tecnicament
