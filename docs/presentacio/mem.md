# 1 Introduccio i objectius

## 1.1 Descripcio general

AgriSync es una aplicacio desktop desenvolupada amb Kotlin Multiplatform i JetBrains Compose orientada a la gestio de la DAN. El projecte centralitza en una sola eina la part administrativa i la part operativa del treball amb titulars, terres, granges, aplicacions fertilitzants i entregues de dejeccions.

L'aplicacio no es basa en dades simulades, sino en una integracio real amb Supabase. Aixo permet tenir autenticacio real, persistencia real sobre PostgreSQL i control d'acces aplicat directament a la base de dades mitjancant Row Level Security. D'aquesta manera, el projecte es planteja com un MVP funcional i coherent, no com una simple maqueta visual.

## 1.2 Motivacio a fer el projecte

La motivacio principal del projecte neix de la necessitat de disposar d'una eina unificada per gestionar la DAN de manera mes ordenada, segura i eficient. En molts casos, aquesta informacio es troba dispersa en diferents fulls, aplicacions o processos manuals, cosa que dificulta el seguiment de les dades i augmenta el risc d'errors.

Amb AgriSync s'ha volgut construir una base solida que permeta treballar amb usuaris reals, permisos reals i dades estructurades des del primer moment. La intencio no era cobrir tota la complexitat del problema en una primera fase, sino demostrar que es possible crear un producte usable, robust i preparat per creixer a partir d'una arquitectura clara.

## 1.3 Finalitats del projecte

Les finalitats principals del projecte son:

- centralitzar la gestio de titulars, terres, granges i entregues en una sola aplicacio
- facilitar la preparacio de la DAN per campanya
- garantir que cada usuari nomes puga veure o modificar la informacio que li correspon
- oferir una estructura clara de dades per reduir errors i duplicacions
- establir una base tecnica escalable per a futures ampliacions

En conjunt, el projecte busca millorar l'organitzacio del treball, augmentar la tracabilitat de les dades i simplificar les tasques habituals dels tecnics i responsables.

## 1.4 Alternatives a la construccio

Abans de definir la solucio actual, es podien considerar diverses alternatives:

- utilitzar fulls de calcul o documents manuals, una opcio simple pero poc escalable i amb molt risc d'inconsistencies
- desenvolupar una aplicacio web completa amb backend propi, una opcio potent pero amb mes cost de desenvolupament i desplegament
- fer una aplicacio exclusivament local amb base de dades interna, mes facil d'executar pero limitada en autenticacio, sincronitzacio i control d'accessos
- construir un prototip sense seguretat real, valid per a demostracio visual pero insuficient per reflectir un entorn de treball real

Finalment es va escollir una aplicacio desktop connectada a Supabase perque oferia un bon equilibri entre simplicitat de desenvolupament, funcionalitat real, persistencia remota i seguretat basada en rols i policies.

## 1.5 Tria dels llenguatges de programacio i SGBD

Per al desenvolupament del client s'ha triat Kotlin, concretament Kotlin Multiplatform, perque permet organitzar el projecte amb un codi modern, clar i preparat per reutilitzar logica en diferents plataformes. A mes, l'ús de JetBrains Compose facilita la construccio d'una interfície moderna i coherent dins l'entorn desktop.

També s'han utilitzat tecnologies com Ktor per a la comunicacio HTTP, coroutines i `StateFlow` per a la gestio de l'estat, i `kotlinx.serialization` per al tractament de dades.

Pel que fa al sistema gestor de base de dades, s'ha escollit PostgreSQL a traves de Supabase. Aquesta decisio respon a diversos motius:

- es un SGBD robust, molt estes i adequat per a dades relacionals
- permet treballar amb restriccions, triggers, funcions i consultes potents
- facilita la implantacio de Row Level Security per aplicar permisos reals
- Supabase afegeix autenticacio, API REST i eines de gestio sense necessitat de crear un backend complet des de zero

En resum, la combinacio de Kotlin i PostgreSQL amb Supabase s'ajusta bé a l'objectiu del projecte: construir un MVP real, mantenible i amb capacitat de creixement.
