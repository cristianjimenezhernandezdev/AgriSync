# Documentacio d'AgriSync

Aquest directori concentra tota la documentacio funcional, tecnica i SQL del projecte.

## Estructura

- `guies/`: instalacio, reconstruccio i demo
- `arquitectura/`: estructura de l'app, model de dades i seguretat
- `funcional/`: flux de treball i moduls de l'aplicacio
- `projecte/`: estat actual i roadmap
- `presentacio/`: material per a defensa o explicacio del projecte
- `sql/`: esquema, seed i scripts de manteniment
- `exemples/`: PDFs de referencia de la DAN

## Documents principals

- [Index complet de documentacio i estructura del projecte](index_complet_documentacio_i_projecte.md)
- [Guia d'instalacio i demo](guies/instalacio_i_demo.md)
- [Arquitectura i codi](arquitectura/arquitectura_i_codi.md)
- [Permisos i seguretat](arquitectura/permisos_i_seguretat.md)
- [Flux operatiu i moduls](funcional/flux_operatiu_i_moduls.md)
- [Estat actual i roadmap](projecte/estat_actual_i_roadmap.md)
- [Guia de defensa](presentacio/guia_defensa.md)
- [Scripts SQL](sql/README.md)

## Criteri de reorganitzacio

- s'han eliminat documents antics o molt solapats
- s'han corregit noms de fitxer que ja no coincidien amb l'estat real del projecte
- els SQL han quedat agrupats per tipus: esquema, seed i manteniment
- el seed principal revisat del projecte es `sql/seeds/agrisync_demo_seed.sql`
