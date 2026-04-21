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
- crear, editar i eliminar terres
- crear, editar i eliminar aplicacions fertilitzants
- treballar per campanya
- visualitzar avisos basics sobre nitrogen aplicat

## Modul ramader

Permet:

- editar dades del titular
- crear, editar i eliminar granges
- gestionar bestiar i fases productives
- registrar entregues de dejeccions
- seleccionar terra o titular receptor quan es permet
- treballar per campanya

## Preparar DAN

La pantalla `Preparar DAN` agrega informacio per titular i campanya:

- terres
- aplicacions fertilitzants
- granges i censos
- entregues de dejeccions
- totals derivats
- checklist automatica

També permet copiar un resum estructurat i la checklist al porta-retalls.

## Gestio administrativa

Segons rol, l'app ofereix:

- `Tecnics`: alta, baixa, detall, assignacions i canvi de password
- `Titulars`: alta, edicio, baixa i comparticio entre oficines
- `Terres`: manteniment transversal de terres
- `Oficines`: alta, edicio i baixa
- `Perfil`: dades del tecnic actual i canvi del propi password

## Casos de prova que cobreix la seed

- titulars compartits entre oficines
- usuaris de lectura
- campanyes 2024 i 2025
- treball combinat agricola i ramader
- entregues cap a altres titulars o terres accessibles
- gestio des d'`admin` i des d'`oficina_manager`
