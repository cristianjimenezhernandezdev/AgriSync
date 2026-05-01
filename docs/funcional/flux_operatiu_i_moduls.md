# Flux operatiu i moduls

## Flux d'us principal

1. l'usuari inicia sessio amb email i password
2. l'app resol el tecnic funcional
3. la home mostra nomes titulars accessibles
4. des de cada titular es treballa per campanya
5. l'usuari entra al modul agricola, ramader o `Preparar DAN`
6. la BDD guarda dades i valida permisos

## Home de titulars

La pantalla inicial operativa es `Titulars`. Mostra els titulars accessibles segons:

- rol global
- assignacions de `tecnic_titular`
- comparticions per oficina
- scopes disponibles

## Modul agricola

Permet:

- editar dades del titular
  Inclou `nif`, `telefon`, `email`, `adreca` i `codi_postal`
- veure quines oficines i tecnics tenen acces al titular, amb scopes visibles
- crear, editar i eliminar terres
- crear, editar i eliminar aplicacions fertilitzants manuals
- veure aplicacions generades automàticament des de les entregues ramaderes
- treballar amb `Kg N`, `Volum m3` i `Kg N/m3` relacionats entre si
- treballar per campanya
- visualitzar avisos basics sobre nitrogen aplicat

## Modul ramader

Permet:

- editar dades del titular
  Inclou `nif`, `telefon`, `email`, `adreca` i `codi_postal`
- veure quines oficines i tecnics tenen acces al titular, amb scopes visibles
- crear, editar i eliminar granges
- gestionar bestiar i fases productives
- informar balanc de nitrogen per granja i campanya
- registrar entregues de dejeccions sempre cap a una terra concreta
- justificar cada entrega com a aplicacio fertilitzant equivalent sobre la terra de desti
- treballar amb `Kg N`, `Volum m3` i `Kg N/m3` relacionats entre si
- treballar per campanya

## Preparar DAN

La pantalla `Preparar DAN` agrega informacio per titular i campanya:

- dades identificatives i de contacte del titular
- terres
- aplicacions fertilitzants
- granges i censos
- balanc de nitrogen per granja i campanya
- entregues de dejeccions
- justificacio de cada entrega cap a terres
- totals derivats
- checklist automatica

També permet copiar un resum estructurat i la checklist al porta-retalls.

## Gestio administrativa

Segons rol, l'app ofereix:

- `Tecnics`: alta, baixa, detall, assignacions i canvi de password
- `Titulars`: alta, edicio, baixa, comparticio entre oficines i cerca ampliada per `nif`, `telefon`, `email`, `adreca` i `codi_postal`
- `Terres`: manteniment transversal de terres
- `Oficines`: alta, edicio i baixa
- `Perfil`: dades del tecnic actual amb `nom`, `oficina`, `email`, `telefon` i canvi del propi password

### Comparticio de titulars amb oficines no visibles

Per mantenir la RLS estricta, un manager nomes veu directament les oficines que ja entren dins del seu abast. Aixo pot fer que una oficina nova no aparegui encara a la llista de comparticio.

Com a solucio MVP, el dialeg de comparticio de titular permet introduir l'email del manager receptor. L'app busca l'oficina associada a aquell manager actiu, l'afegeix temporalment a la llista d'oficines receptores i permet crear la comparticio normal per `scope`.

Aquest flux evita haver d'obrir la lectura global d'oficines als managers i permet compartir amb oficines acabades de crear sense canviar l'esquema de base de dades.

## Casos de prova que cobreix la seed

- titulars compartits entre oficines
- usuaris de lectura
- campanyes 2024 i 2025
- treball combinat agricola i ramader
- entregues cap a terres accessibles amb justificacio agricola
- gestio des d'`admin` i des d'`oficina_manager`
