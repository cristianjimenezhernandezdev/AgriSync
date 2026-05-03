# Documentacio d'AgriSync

Aquest directori es la font de veritat documental del projecte. L'objectiu no es nomes descriure que fa l'app, sino deixar clar com s'engega, com esta construida, com parla amb Supabase i com funciona la base de dades a nivell real.

## Com llegir aquesta documentacio

Si una persona no coneix gens el projecte, l'ordre recomanat es aquest:

1. `guies/instalacio_i_demo.md`
   Per entendre que cal tenir preparat, com aixecar el projecte i quins problemes son normals en la primera execucio.
2. `arquitectura/arquitectura_i_codi.md`
   Per entendre l'estructura del programa, les capes, els fitxers principals, el flux d'execucio i les pantalles.
3. `api/api_i_integracio.md`
   Per entendre quina API fa servir l'app, que hi ha de Supabase pur, que hi ha de personalitzat i com es podria evolucionar cap a una API propia o externa.
4. `sql/model_de_dades_i_bdd.md`
   Per entendre el model relacional, les funcions helper, els triggers, les policies RLS i el flux de dades a la base de dades.
5. `arquitectura/permisos_i_seguretat.md`
   Per llegir el resum de seguretat i control d'acces un cop ja es coneix el model general.
6. `presentacio/memoria_capitols_4_5_i_manual_usuari.md`
   Per tenir els capitols 4 i 5 de la memoria desenvolupats, amb requeriments, casos d'us, diagrames, model de dades i manual d'usuari.

## Estructura de `docs/`

- `api/`
  Documentacio de la capa d'integracio HTTP i del paper de Supabase com a API real del projecte.
- `arquitectura/`
  Explicacio de l'estructura del codi, flux de l'app, permisos i decisions tecniques.
- `exemples/`
  PDFs de referencia de DAN.
- `funcional/`
  Resum funcional dels moduls i del flux operatiu d'usuari.
- `guies/`
  Posada en marxa, demo, resolucio de problemes habituals i procediment de testing.
- `presentacio/`
  Material de suport per defensa o explicacio oral del projecte.
- `projecte/`
  Estat actual del MVP, roadmap curt i millores futures previstes.
- `sql/`
  Esquema, seed, manteniment i documentacio detallada de la BDD.

## Documents principals

- [Index complet de documentacio i estructura del projecte](index_complet_documentacio_i_projecte.md)
- [Guia detallada de posada en marxa, demo i troubleshooting](guies/instalacio_i_demo.md)
- [Manual d'usuari complet](guies/Manual_Usuari.md)
- [Arquitectura, estructura del programa i explicacio del codi](arquitectura/arquitectura_i_codi.md)
- [API i integracio amb Supabase](api/api_i_integracio.md)
- [Model de dades i funcionament de la BDD](sql/model_de_dades_i_bdd.md)
- [Permisos i seguretat](arquitectura/permisos_i_seguretat.md)
- [Flux operatiu i moduls](funcional/flux_operatiu_i_moduls.md)
- [Capitols 4 i 5 de la memoria i manual d'usuari](presentacio/memoria_capitols_4_5_i_manual_usuari.md)
- [Millores previstes per versions posteriors al MVP](projecte/millores_post_mvp.md)
- [Index del paquet SQL](sql/README.md)
- [Procediment de testing](guies/testing.md)

## Quina pregunta respon cada document

- `guies/instalacio_i_demo.md`
  "Com poso en marxa el projecte des de zero, quines claus necessito, quines credencials demo faig servir i que faig si alguna cosa falla?"
- `guies/Manual_Usuari.md`
  "Com utilitza l'aplicacio cada perfil d'usuari, quines pantalles hi ha, quines captures cal inserir i com es pot preparar un manual final?"
- `arquitectura/arquitectura_i_codi.md`
  "Com esta organitzat el programa, quins fitxers hi ha, quines pantalles i viewmodels existeixen, i com flueixen les dades?"
- `api/api_i_integracio.md`
  "Quina API consumeix el client, on es troba el codi d'integracio i fins a quin punt l'app esta lligada a Supabase?"
- `sql/model_de_dades_i_bdd.md`
  "Com esta modelada la BDD, quines taules intervenen, com s'aplica la seguretat i com es relaciona tot amb la UI?"
- `arquitectura/permisos_i_seguretat.md`
  "Com es combina Auth, `public.tecnic`, scopes i RLS per decidir qui pot veure o modificar cada dada?"
- `presentacio/memoria_capitols_4_5_i_manual_usuari.md`
  "Com puc explicar formalment l'ambit, requeriments, casos d'us, activitats, classes, moduls, model ER, model relacional i manual d'usuari a la memoria?"

## Criteri editorial

- La documentacio descriu l'estat real del repositori actual.
- Quan un comportament depen directament d'un fitxer concret del codi o SQL, es referencia explicitament.
- Les decisions importants queden explicades amb el motiu tecnic i amb les limitacions conegudes.
- La documentacio no assumeix coneixement previ del projecte.
