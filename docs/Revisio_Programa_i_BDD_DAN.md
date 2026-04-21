# Revisio del Programa i la BDD respecte la idea funcional

## 1. Objectiu de la revisio

Aquest document contrasta l'estat actual d'AgriSync amb la idea funcional desitjada:

- una sola base de dades central
- tecnics de diferents empreses o oficines connectats al mateix sistema
- accessos limitats segons els titulars amb qui treballa cada tecnic
- suport a la DAN agricola i ramadera
- tractament principal de UF o volum, kg de nitrogen, hectares i kg N/ha
- control dels limits de nitrogen admissibles a terres
- seguiment del nitrogen generat per les granges i del seu desti
- resum final suficient per preparar la DAN

Aquest document queda actualitzat despres de tancar la iteracio funcional centrada en campanyes.

## 2. Veredicte curt

Conclusio:

- el projecte continua sent un MVP parcial respecte la DAN real completa
- pero ara esta millor alineat amb el flux funcional que al començament de la revisio
- la base de dades unica, els permisos per oficina i el treball per campanya ja queden molt mes ben resolts

El que avui queda ben cobert:

- BDD compartida i centralitzada
- login real i model multiusuari
- assignacions per titular amb scopes agricola, ramader, comu i lectura
- limitacio realista de l'ambit de `oficina_manager`
- treball per campanya a modul agricola, modul ramader i `Preparar DAN`
- resum DAN orientat a trasllat
- copia estructurada del resum i checklist automatica per al tancament final
- login principal sense dependència operativa directa de `service_role`, amb fallback tecnic nomes per recuperacio de desalineaments
- comparticio interoficina per `scope` del titular

El que encara no queda tancat del tot:

- camps normatius mes complets de la DAN agricola
- calcul del nitrogen generat a partir del cens ramader
- model d'estat inicial i final de fossa
- traçabilitat forta entre entrega concreta i aplicacio concreta
- suport real a receptors externs de tercers des de la UI

## 3. Valoracio per blocs

### 3.1. Base de dades unica i acces multiusuari

Estat: compleix.

Hi ha una sola BDD amb model centralitzat de:

- oficina
- tecnic
- titular
- tecnic_titular
- dan_declaracio
- terra
- aplicacions_fertilitzants
- granja
- bestiar
- fase_productiva
- granja_bestiar
- entrega_dejeccions

La base tecnica del model compartit existeix i es defensable.

### 3.2. Tecnics de diferents oficines amb accessos limitats per titular

Estat: compleix de manera molt mes coherent que abans.

Per a tecnics normals, el model encaixa be:

- el tecnic pertany a una oficina
- el tecnic rep assignacions per titular a `tecnic_titular`
- l'acces agricola o ramader depen del `scope`

Per a `oficina_manager`, el comportament ja no es global. La BDD utilitza `can_manage_office_titular(...)` i restringeix l'ambit del gestor a:

- titulars creats per ell mateix
- o titulars que ja tenen tecnics actius de la seva oficina assignats

Aixo vol dir que la desviacio principal detectada a la primera revisio queda molt mes acotada.

Matis:

- un `oficina_manager` pot donar d'alta titulars nous
- pero la gestio forta per oficina no es basa en "veure tot", sino en titulars creats o ja vinculats al seu ambit
- ara, a mes, un titular es pot compartir amb una altra oficina nomes per la part `agricola`, `ramader`, `lectura` o `comu`

### 3.3. Part agricola

Estat: compleix parcialment.

Actualment es pot guardar:

- terres
- superficie
- zona nitrogen (`ZV` o `ZNV`)
- limit derivat de `kg N/ha`
- municipi literal
- us SIGPAC
- cultiu
- aplicacions
- tipus de fertilitzant
- procedencia
- `volum m3`
- `kg N/m3`
- `kg N`
- `UF`

I es pot calcular:

- total ha
- total `kg N`
- total `UF`
- `kg N/ha`
- `kg N/UF`
- marge de nitrogen per terra segons zona

Per tant, la part agricola esta millor que en la revisio inicial, perque ja hi ha una aproximacio operativa al limit de nitrogen admissible.

Encara falten camps mes propis de DAN real:

- classificacio normativa mes detallada
- taules mestres normatives
- origen o procedencia amb traçabilitat forta i no nomes textual
- integracio directa amb criteris oficials de declaracio

### 3.4. Part ramadera

Estat: compleix parcialment.

Actualment es pot guardar:

- granges
- cens per bestiar i fase
- entregues de dejeccions

Pero encara no hi ha model suficient per calcular automaticament:

- quants kg de nitrogen genera una granja
- estat inicial de fossa
- estoc final despres de les entregues
- balanc final pendent o gestionat

La part ramadera continua sent valida com a registre operatiu, no com a balanc complet.

### 3.5. DAN per campanya

Estat: compleix funcionalment al MVP actual.

Aquest era el finding principal de la iteracio 2 i ara queda resolt de manera clara:

- el modul agricola deixa triar campanya
- el modul ramader deixa triar campanya
- `Preparar DAN` deixa triar campanya
- els totals visibles es calculen per la campanya seleccionada
- les noves aplicacions i entregues es creen sobre la campanya activa
- si la campanya encara no existeix, es crea la `dan_declaracio` corresponent en el moment del primer insert
- el resum es pot copiar de forma estructurada per reutilitzar-lo fora de l'app
- la pantalla mostra una checklist automatica amb buits basics detectats

En consequencia, el sistema ja respon molt millor a preguntes com:

- quants `kg N` s'han aplicat el 2025
- quines entregues de dejeccions corresponen a la campanya 2024
- quina campanya s'esta preparant a la pantalla de resum

Matis encara existent:

- la UI no obre primer una pantalla exclusiva de campanya i despres el titular
- el flux actual continua entrant pel titular i, un cop a dins, es selecciona la campanya activa

Per a l'MVP, aquest compromis es defensable i funcional.

### 3.6. Procedencia i desti del nitrogen

Estat: no compleix del tot.

Actualment hi ha:

- `entrega_dejeccions` amb granja origen i receptor
- `aplicacions_fertilitzants` amb terra, `UF` i `kg N`

Pero no hi ha una relacio directa entre una entrega concreta i una aplicacio concreta.

Consequencies:

- no es pot afirmar des del sistema que una aplicacio provingui d'una granja concreta
- no es pot fer traçabilitat forta entre nitrogen generat, entregat i aplicat
- la procedencia queda parcialment manual

### 3.7. Entregues a altres titulars o terres externes

Estat: millorada respecte revisions anteriors, encara no total.

La BDD admet:

- receptor per titular
- receptor per terra

La UI actual permet:

- entregar a titulars accessibles
- entregar a terres accessibles, incloses terres d'altres titulars si hi ha permisos

Per tant:

- el model de dades i la pantalla ja suporten receptors externs accessibles
- la limitacio actual no es la pantalla, sino la necessitat de tenir permisos reals sobre el titular o terra receptor

## 4. Diagnosi resumida

Si la pregunta es "serveix per defensar un MVP funcional?"

- si

Si la pregunta es "ja modela la idea funcional completa de la DAN real?"

- no encara

Ara be, respecte la revisio inicial, el projecte ha millorat en dos punts molt importants:

- l'ambit de permisos per oficina ja no es tan obert
- el treball per campanya ja es un eix real de lectura i escriptura

El sistema actual esta mes a prop de:

- una eina operativa de treball per titular i campanya amb permisos reals

I encara lluny de:

- un sistema complet de control tecnic, normatiu i de balanc ramader

## 5. Estat de les iteracions funcionals

### Iteracio 1. Tancar l'ambit d'acces per oficina

Estat: completada de manera satisfactoria.

Ja hi ha:

- limitacio de `oficina_manager` al seu ambit funcional
- restriccions sobre `tecnic_titular` lligades a mateixa oficina i titular gestionable
- eliminacio del comportament efectivament global que es detectava al principi

### Iteracio 2. Fer que la campanya sigui el centre del flux

Estat: completada funcionalment.

Ja hi ha:

- seleccio de campanya als tres punts clau del flux
- filtratge de dades per campanya
- creacio de dades noves sobre la campanya activa
- resum `Preparar DAN` treballant sobre la campanya seleccionada

Resultat:

- els totals i resums ja responen a "aquella campanya"

## 6. Seguent prioritat recomanada

Un cop tancades les iteracions 1, 2 i 3, la seguent iteracio amb mes valor es la 4.

### Iteracio 3. Modelar millor la part agricola real de la DAN

Estat: completada en versio MVP ampliada.

Ja hi ha:

- ampliacio de `terra` amb municipi literal, us SIGPAC i cultiu
- ampliacio d'`aplicacions_fertilitzants` amb tipus, procedencia, `volum m3` i `kg N/m3`
- visualitzacio del limit admissible per terra i del marge disponible
- resum mes tecnic a `Preparar DAN`

### Iteracio 4. Modelar la part ramadera real

Objectiu:

- calcular nitrogen generat i balanc de granja

Tasques:

- coeficients productius per bestiar i fase
- model de fossa o emmagatzematge
- estat inicial, moviments i estat final
- resum de nitrogen generat, entregat i pendent

### Iteracio 5. Traçabilitat completa entre ramaderia i agricultura

Objectiu:

- unir origen i desti del nitrogen dins del mateix model

Tasques:

- relacionar una entrega amb una o mes aplicacions
- permetre receptors externs complets
- identificar millor nitrogen propi, extern o mineral

### Iteracio 6. Sortida final orientada a presentacio DAN

Estat: completada en versio MVP.

Objectiu:

- convertir el sistema en una eina de preparacio final encara mes directa

Tasques:

- exportacio o copia estructurada
- checklists finals
- comprovacions de dades incompletes
- proves funcionals amb casos reals

Ja hi ha:

- copia estructurada del resum DAN
- copia separada de la checklist
- checklist automatica integrada dins de la pantalla

## 7. Conclusio final

AgriSync no s'ha hagut de refer de zero.

El que s'ha confirmat en aquesta revisio actualitzada es que:

- la base centralitzada funciona
- els permisos estan millor acotats
- el treball per campanya ja esta incorporat de debò
- el projecte te una seed prou rica per demostrar diversos rols, oficines i campanyes

El que queda pendent no es el nucli del MVP, sino la capa de profunditat tecnica:

- normativa agricola mes completa
- balanc ramader
- traçabilitat forta del nitrogen

Per tant, el projecte ja no esta en el punt "cal refer el model".

Esta en el punt:

- "cal fer la seguent capa de detall funcional sobre una base que ja aguanta"
