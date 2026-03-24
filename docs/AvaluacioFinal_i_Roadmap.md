# Avaluacio Final i Roadmap d'Iteracions

## 1. Objectiu d'aquest document

Aquest document serveix per valorar si el projecte AgriSync, en l'estat actual, compleix els objectius del MVP i si es pot considerar prou tancat per a un projecte final de curs. També recull millores recomanades, tant funcionals com d'experiència d'usuari, i les ordena per iteracions.

## 2. Conclusio general

La conclusio general és aquesta:

- sí, el projecte compleix bastant bé els objectius principals del MVP
- sí, es pot defensar com a projecte funcional i coherent
- però no és recomanable donar-lo per completament tancat sense fer algunes millores petites però importants

En altres paraules:

- com a MVP acadèmic, és vàlid i defensable
- com a producte més polit i robust, encara hi ha feina clara a fer

## 3. Què compleix bé ara mateix

Actualment el projecte ja resol bé aquests punts:

- autenticació real amb Supabase Auth
- sessió persistent amb refresh de token
- model de dades coherent amb el domini del projecte
- control d'accés real a base de dades amb RLS
- llista de titulars accessible segons permisos
- separació funcional entre mòdul agrícola i mòdul ramader
- gestió d'oficines
- gestió de tècnics
- gestió de titulars
- gestió de terres
- canvi de password propi
- reset de password des de gestió de tècnics
- alta de tècnics nous des de l'aplicació

Per tant, el nucli del projecte sí que està assolit.

## 4. Valoració de si es pot donar per finalitzat

### 4.1. Si el criteri és MVP acadèmic

Sí, el projecte es pot considerar pràcticament finalitzable.

Per què:

- el cas d'ús principal es pot demostrar
- hi ha autenticació i autorització reals
- hi ha persistència real
- hi ha dades de prova
- hi ha documentació funcional i tècnica
- la base de dades no és superficial: està pensada i protegida

### 4.2. Si el criteri és producte més rodó o entrega excel·lent

No del tot. Encara falten algunes millores importants de qualitat:

- seguretat de configuració
- més control administratiu des de la UI
- validacions de dades més estrictes
- alguns fluxos de creació/eliminació dins dels mòduls agrícola i ramader

## 5. Findings principals

### 5.1. Crític: hi ha claus per defecte dins del codi, inclosa la `service_role`

Referència:

- [JvmEnvConfig.kt](C:/Cristian/DAM2/AgriSynct/composeApp/src/jvmMain/kotlin/cat/agrisync/data/JvmEnvConfig.kt#L6)

Problema:

- la `anon key` i la `service_role key` estan hardcodejades com a valors per defecte
- això és acceptable per prototip o desenvolupament ràpid, però és un risc greu si el projecte es distribueix o s'ensenya fora d'un entorn controlat

Impacte:

- exposició d'accés administratiu
- dependència d'un entorn concret
- mala pràctica de seguretat

Conclusió:

- és la millora més important abans de donar el projecte per completament tancat

### 5.2. Alta: hi ha un fallback que pot bypassejar RLS amb `service_role`

Referència:

- [SupabaseAuthApi.kt](C:/Cristian/DAM2/AgriSynct/composeApp/src/commonMain/kotlin/cat/agrisync/data/SupabaseAuthApi.kt#L98)

Problema:

- si falla l'RPC `get_my_tecnic()`, el codi prova una consulta directa a `tecnic` amb `service_role`

Impacte:

- és útil com a mecanisme de recuperació
- però conceptualment debilita la separació neta entre accés normal i accés administrador

Conclusió:

- per a MVP pot passar, però per acabar bé el projecte convé reduir o encapsular millor aquesta dependència

### 5.3. Alta: els mòduls agrícola i ramader permeten editar però no gestionar completament el cicle de dades

Referències:

- [TitularAgricolaScreen.kt](C:/Cristian/DAM2/AgriSynct/composeApp/src/commonMain/kotlin/cat/agrisync/ui/TitularAgricolaScreen.kt#L58)
- [TitularRamaderScreen.kt](C:/Cristian/DAM2/AgriSynct/composeApp/src/commonMain/kotlin/cat/agrisync/ui/TitularRamaderScreen.kt#L58)

Problema:

- es poden editar titulars, terres, aplicacions, granges, cens i entregues
- però no es poden crear ni eliminar directament des dels mòduls agrícola i ramader

Impacte:

- el flux de treball queda a mig camí
- obliga a dependre d'altres pantalles o de seeds per tenir dades
- dona sensació de mòdul incomplet

Conclusió:

- si vols una experiència real de producte, aquesta és una de les millores amb més valor

### 5.4. Mitjana: hi ha camps numèrics que, si s'escriuen malament, poden acabar guardant `0`

Referències:

- [TitularAgricolaScreen.kt](C:/Cristian/DAM2/AgriSynct/composeApp/src/commonMain/kotlin/cat/agrisync/ui/TitularAgricolaScreen.kt#L153)
- [TitularRamaderScreen.kt](C:/Cristian/DAM2/AgriSynct/composeApp/src/commonMain/kotlin/cat/agrisync/ui/TitularRamaderScreen.kt#L189)

Problema:

- en alguns formularis inline, si la conversió a número falla, el codi fa servir `0.0`

Impacte:

- risc de guardar dades incorrectes sense avís clar
- mala experiència d'usuari

Conclusió:

- s'hauria de bloquejar el guardat i mostrar error de validació

### 5.5. Mitjana: l'administració d'usuaris està bastant bé, però no hi ha esborrat complet d'usuari des de l'app

Referències:

- [TecnicRepository.kt](C:/Cristian/DAM2/AgriSynct/composeApp/src/commonMain/kotlin/cat/agrisync/data/TecnicRepository.kt#L16)
- [TecnicManagementScreen.kt](C:/Cristian/DAM2/AgriSynct/composeApp/src/commonMain/kotlin/cat/agrisync/ui/TecnicManagementScreen.kt#L44)

Estat actual:

- sí que es poden crear tècnics
- sí que es poden activar i desactivar
- sí que es poden editar
- sí que es poden canviar passwords

Què falta:

- eliminar completament un usuari Auth + el seu tècnic des de la UI

Conclusió:

- no és un bloquejador per al MVP
- però com a funcionalitat d'administració és una millora molt coherent

### 5.6. Mitjana: hi ha millores de UX clares a nivell visual i de formularis

Problemes observables:

- el login és molt funcional però molt bàsic
- falta feedback més fi en formularis inline
- falta confirmació o resum després d'algunes accions importants
- falta més consistència entre pantalles de gestió i pantalles de detall

Impacte:

- no impedeix usar l'app
- però fa que l'experiència sigui més “prototip” que “producte”

## 6. Veredicte recomanat

### Si busques tancar el projecte aviat

Pots donar-lo per finalitzat com a MVP acadèmic, sempre que:

- mantinguis clar que és un MVP
- expliquis les limitacions com a evolució natural
- no el presentis com un producte complet

### Si vols una entrega més forta

Et recomano fer almenys una iteració curta més, centrada en:

- hardening de seguretat
- validacions
- un parell de millores d'administració i UX

## 7. Ruta d'iteracions recomanada

## Iteracio 1: Tancament tècnic mínim recomanat

Objectiu:

- deixar el projecte prou sòlid per considerar-lo “finalitzat amb criteri”

Tasques:

- treure `DEFAULT_SERVICE_KEY` i `DEFAULT_KEY` del codi
- obligar a configurar les claus per entorn
- reduir o eliminar el fallback amb `service_role` a `getMyTecnic`
- impedir guardats amb valors numèrics invàlids
- millorar missatges d'error de validació

Resultat esperat:

- projecte molt més defensable tècnicament
- menys risc de dades incorrectes

## Iteracio 2: Administració i operació

Objectiu:

- completar les funcions bàsiques d'administrador dins de l'app

Tasques:

- permetre esborrar tècnics des de la UI
- definir si l'esborrat és lògic o físic
- si es fa esborrat físic, contemplar també l'usuari d'Auth
- afegir confirmacions clares abans de destruccions importants
- millorar la gestió de casos com “usuari sense login” o “tecnic sense assignacions”

Resultat esperat:

- administració més completa
- menys dependència del Dashboard de Supabase

## Iteracio 3: Completar mòduls agrícola i ramader

Objectiu:

- passar de mòduls d'edició a mòduls realment operatius

Tasques:

- crear i eliminar aplicacions de fertilitzants des del mòdul agrícola
- crear i eliminar granges
- crear i eliminar línies de granja-bestiar
- crear i eliminar entregues de dejeccions
- valorar si cal crear DAN des de la UI

Resultat esperat:

- els mòduls es poden fer servir sense dependre tant de seeds o càrregues prèvies

## Iteracio 4: UX i poliment visual

Objectiu:

- millorar percepció de qualitat del producte

Tasques:

- millorar la pantalla de login
- afegir placeholders, ajudes i validacions més visibles
- fer més consistents els formularis
- mostrar errors inline en lloc de convertir silenciosament valors
- millorar navegació i estats buits

Resultat esperat:

- app més agradable
- millor experiència en presentació i ús real

## Iteracio 5: Valor afegit funcional

Objectiu:

- acostar el projecte a una versió més útil professionalment

Tasques:

- càlculs derivats com `kg N/ha` o `kg N/UF`
- resums per titular o campanya
- informes exportables
- importació des de fulls de càlcul

Resultat esperat:

- més valor de negoci
- millor connexió amb el procés real del sector

## 8. Quines millores faria jo abans d'entregar

Si només fes una última passada curta, prioritzaria exactament això:

1. treure claus sensibles del codi
2. arreglar validacions numèriques perquè mai guardin `0` per error
3. afegir esborrat o baixa més clara de tècnics
4. afegir alta/eliminació d'elements dins dels mòduls agrícola i ramader

## 9. Resum final

El projecte, en l'estat actual, compleix bé els objectius del MVP i es pot defensar com a projecte final de curs funcional. No obstant això, si vols una entrega més madura i amb millor experiència d'usuari, encara hi ha algunes millores molt raonables i ben acotades.

La bona notícia és que no cal reinventar el projecte. El nucli ja està fet. El que queda és sobretot:

- polir seguretat
- completar alguns fluxos
- fer més robusta la UX

Per tant, la decisió més honesta seria aquesta:

- sí, es pot donar per gairebé finalitzat com a MVP
- però amb una iteració més curta quedaria molt millor tancat
