# Guia per defensar el codi d'AgriSync

## 1. Quin és el millor enfocament per defensar-lo?

La millor forma **no** és explicar-lo només:

- per blocs tècnics
- ni només per ordre d'execució
- ni només per camps semàntics

La millor forma és una **estratègia híbrida**:

1. **començar pel problema que resol**
2. **explicar l'arquitectura general**
3. **fer un recorregut curt per l'ordre d'execució real**
4. **baixar després als blocs funcionals principals**
5. **tancar amb seguretat, decisions tècniques i límits**

Dit d'una altra manera:

- primer defenses **per què existeix**
- després defenses **com està organitzat**
- després demostres **que saps què passa quan s'executa**
- i finalment defenses **les decisions importants**

Si ho fas així, el tribunal veu tres coses:

- que entens el producte
- que entens l'arquitectura
- que entens el codi i no només l'has assemblat

## 2. Idea força que has de transmetre

La idea central que has de repetir amb diferents paraules és aquesta:

> AgriSync és un MVP desktop per centralitzar la gestió de la DAN, amb autenticació real, permisos reals a nivell de base de dades i una separació clara entre interfície, lògica i dades.

Si el tribunal et talla, et posa nerviós o et fa saltar de tema, aquesta frase et serveix per tornar al fil conductor.

## 3. Ordre recomanat de la defensa

### Bloc 1. Problema real i objectiu del projecte

Comença així:

- el problema és que la DAN sovint es prepara amb fulls de càlcul, dades disperses i verificacions manuals
- això provoca duplicació, incoherències i poca traçabilitat
- el projecte no intenta fer un ERP complet, sinó un **MVP funcional** que centralitza les dades i ajuda a preparar la DAN

Què defenses ací:

- que el projecte parteix d'una necessitat real
- que l'abast està controlat
- que no has intentat fer "massa coses"

Frase útil:

> Vaig preferir fer un MVP coherent amb autenticació, permisos i fluxos reals abans que una aplicació enorme però fràgil.

### Bloc 2. Arquitectura general

Ací no entres encara al detall de fitxers. Explica el mapa gran:

- client desktop fet amb **Kotlin Multiplatform + Compose**
- backend de dades en **Supabase/PostgreSQL**
- autenticació amb **Supabase Auth**
- accés a dades via **REST API de Supabase**
- control d'accés real amb **RLS**

Què defenses ací:

- has separat responsabilitats
- la UI no decideix els permisos
- la base de dades forma part de l'arquitectura, no és només magatzem

Frase útil:

> La UI mostra i demana accions, però la decisió final de si es pot llegir o escriure la pren la base de dades amb RLS.

### Bloc 3. Ordre d'execució real

Ací és on guanyes molta credibilitat. Explica el recorregut:

1. entra per `main.kt`
2. s'obre la finestra Compose
3. `App.kt` carrega configuració
4. crea `AppServices`
5. `AuthService.initialize()` intenta recuperar sessió
6. si no hi ha sessió, es mostra `LoginScreen`
7. si hi ha sessió vàlida, entra a `AuthenticatedContent`
8. des d'allí es navega a mòduls i pantalles

Què defenses ací:

- que saps què passa de veritat en executar
- que no només coneixes pantalles, sinó també flux intern

Frase útil:

> El flux real d'entrada és configuració, autenticació, estat d'usuari i després navegació funcional.

### Bloc 4. Blocs funcionals

Ací sí expliques per responsabilitats.

#### 4.1. Autenticació

Explica:

- `LoginViewModel` recull email i password
- `AuthService` centralitza la sessió
- `SupabaseAuthApi` parla amb Supabase Auth
- la sessió es guarda localment
- hi ha refresh automàtic del token

El que has de defensar:

- no és un login "fake"
- hi ha persistència de sessió
- hi ha control de tècnic actiu/inactiu

#### 4.2. Home i accés a titulars

Explica:

- `HomeViewModel` carrega els titulars accessibles
- `AccessRepository` calcula què veu cada usuari
- el resultat depèn de rol, oficina, assignacions i comparticions

El que defenses:

- no tots els usuaris veuen el mateix
- hi ha model d'accés real

#### 4.3. Mòdul agrícola

Explica:

- treballa sobre un titular
- mostra i edita terres
- registra aplicacions fertilitzants
- opera per campanya
- calcula totals útils per DAN

El que defenses:

- hi ha validacions
- la campanya és una dimensió funcional real
- no és només CRUD, també hi ha càlculs

#### 4.4. Mòdul ramader

Explica:

- gestiona granges
- gestiona cens per bestiar i fase
- registra entregues de dejeccions
- permet enviar-les a titulars o terres accessibles
- també funciona per campanya

El que defenses:

- modeles el flux ramader de manera separada però connectada
- el sistema contempla relacions entre titulars i terres

#### 4.5. Preparar DAN

Aquest bloc és molt potent per a la defensa.

Explica:

- no genera encara el document oficial final
- però unifica informació dispersa
- calcula totals
- detecta buits bàsics
- genera una checklist
- permet copiar un resum estructurat

El que defenses:

- has pensat en el cas d'ús real
- el producte ajuda al tècnic de manera pràctica
- el MVP és útil encara que no tanqui tot el procés

Frase útil:

> Preparar DAN és la peça que transforma dades disperses en informació accionable.

### Bloc 5. Seguretat i permisos

Aquest és un dels blocs que més valor tècnic et dona.

Has d'explicar:

- autenticació amb `auth.uid()`
- taula `tecnic` com a perfil funcional
- assignacions `tecnic_titular`
- comparticions `oficina_titular_compartit`
- funcions helper com `can_read_titular`, `can_manage_office_titular`, `can_write_agricola`, `can_write_ramader`
- polítiques RLS per taula

Què defenses ací:

- els permisos no depenen de si algú "amaga" o "ensenya" botons
- la seguretat és de dades, no només de presentació

Frase útil:

> Una de les decisions més importants del projecte és que els permisos no estan a la UI, sinó a la base de dades.

### Bloc 6. Decisions tècniques

Ací defenses el perquè de la tecnologia.

#### Per què Kotlin Multiplatform?

Resposta:

- perquè la lògica compartida queda preparada per créixer
- encara que ara el producte sigui desktop, la idea era no tancar la porta a Android

#### Per què Compose Desktop?

Resposta:

- perquè dona una UI moderna en Kotlin
- permet mantenir UI i lògica dins del mateix ecosistema

#### Per què Supabase?

Resposta:

- perquè resol autenticació, PostgreSQL i API REST
- així el temps del projecte es va poder invertir en model de dades, permisos i fluxos

#### Per què RLS?

Resposta:

- perquè la restricció d'accés queda garantida a la base de dades
- això evita confiar-ho tot al client

## 4. Com hauries d'explicar el codi si et deixen obrir fitxers

Si et fan obrir codi, no saltes directament a qualsevol pantalla. Obri'l en aquest ordre:

1. `composeApp/src/jvmMain/kotlin/cat/agrisync/main.kt`
2. `composeApp/src/commonMain/kotlin/cat/agrisync/App.kt`
3. `composeApp/src/commonMain/kotlin/cat/agrisync/data/AppServices.kt`
4. `composeApp/src/commonMain/kotlin/cat/agrisync/data/AuthService.kt`
5. `composeApp/src/commonMain/kotlin/cat/agrisync/data/RestClient.kt`
6. un repositori, per exemple `AgricolaRepository.kt`
7. un viewmodel, per exemple `TitularAgricolaViewModel.kt`
8. una pantalla Compose, per exemple `TitularAgricolaScreen.kt`
9. `SQLAgriSync.sql` per parlar de permisos

Per què aquest ordre és bo:

- comences per l'entrada
- continues amb la composició dels serveis
- després ensenyes el flux de dades
- acabes amb el control d'accés

## 5. Guió curt de defensa oral

Si tens uns 8-12 minuts, aquest és un bon guió:

### Minut 1. Context

- què és la DAN
- quin problema resol AgriSync
- quin és l'objectiu del MVP

### Minuts 2-3. Arquitectura

- client desktop
- Supabase com a backend de dades i auth
- separació UI / viewmodel / repositori / base de dades

### Minuts 3-4. Flux d'execució

- `main.kt`
- `App.kt`
- autenticació
- càrrega de sessió
- navegació

### Minuts 4-7. Mòduls

- titulars i accés
- agrícola
- ramader
- preparar DAN

### Minuts 7-8. Seguretat

- RLS
- taules d'assignació
- compartició per oficina

### Minuts 8-9. Decisions i límits

- per què KMP
- per què Supabase
- què està fet
- què queda com a evolució futura

## 6. Preguntes que et poden fer i com defensar-les

### "Per què no has fet un backend propi?"

Resposta bona:

> En un PFC amb temps limitat vaig prioritzar el valor funcional. Supabase em resolia autenticació, base de dades i API, i així vaig poder centrar l'esforç en model de dades, permisos i fluxos reals.

### "Per què és desktop i no web?"

Resposta bona:

> El projecte està pensat com a eina de treball de tècnic. Per a un MVP, desktop em permetia una entrega directa i controlada, i al mateix temps mantenir oberta l'opció de reutilitzar lògica més endavant.

### "Per què uses Kotlin Multiplatform si només tens desktop?"

Resposta bona:

> Perquè la decisió no és només l'estat actual sinó l'evolució. La lògica compartida de dades, viewmodels i models queda preparada per aprofitar-se si el projecte creix.

### "On estan realment els permisos?"

Resposta bona:

> A la base de dades. La UI orienta l'usuari, però la restricció real està en les polítiques RLS i en funcions helper com `can_read_titular` o `can_write_scope`.

### "Aleshores qualsevol que manipuli el client podria saltar-se la UI?"

Resposta bona:

> Podria intentar cridar l'API, però no podria llegir o escriure si no compleix les polítiques RLS. Aquesta és precisament la gràcia de no deixar la seguretat a la interfície.

### "Per què hi ha `service_role_key` al projecte?"

Resposta honesta i defensable:

> En un producte real això hauria d'anar darrere d'un backend segur. Ací s'ha acceptat com a decisió de MVP acadèmic perquè hi ha funcionalitats administratives i de demo que depenen de l'Admin API. És una limitació coneguda i una de les primeres coses a externalitzar en una versió productiva.

### "Això és només CRUD?"

Resposta bona:

> No. Hi ha CRUD, però també hi ha estat d'autenticació, resolució de permisos, treball per campanya, càlculs de totals, checklist automàtica i una capa de seguretat a base de dades.

## 7. Coses que has de dir amb honestedat

No intentes vendre com a "producció final" el que és un MVP.

El que convé dir:

- és un MVP funcional i demostrable
- el nucli és estable
- hi ha funcionalitats completes de login, permisos i treball per mòduls
- encara queden parts normatives i automatitzacions per fer

Dir això et fa quedar millor que exagerar.

## 8. Punts forts que t'interessa remarcar

- autenticació real amb Supabase Auth
- sessió persistent local
- refresh de token
- permisos reals a base de dades amb RLS
- model d'accés per rol, oficina i titular
- separació clara de capes
- treball per campanya
- resum i checklist de `Preparar DAN`
- esquema SQL reconstructiu des de zero
- seeds de demo per validar l'aplicació

## 9. Punts febles que has de saber reconèixer

- no genera encara el document oficial final
- el `service_role_key` no és una solució de producció
- falta més profunditat normativa en càlculs de DAN
- hi ha part del valor encara pensada com a assistència al tècnic i no com a automatització completa

Si et pregunten per aquests punts, no et justifiques massa. Contesta curt:

> Sí, és una limitació coneguda del MVP i està identificada com a següent pas de producte.

## 10. La millor frase final per tancar

> El valor principal d'AgriSync no és només la interfície, sinó haver construït un MVP amb flux real de treball, autenticació real i control real d'accés sobre dades sensibles.

## 11. Resum molt curt per memoritzar

Si et quedes en blanc, recorda aquest ordre:

1. problema real
2. arquitectura client + Supabase
3. flux `main -> App -> auth -> pantalles`
4. mòdul agrícola
5. mòdul ramader
6. preparar DAN
7. RLS i permisos
8. límits i evolució

Si controles això, ja tens una defensa sòlida.
