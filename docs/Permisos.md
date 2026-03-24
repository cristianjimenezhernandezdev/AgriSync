diff --git a/c:\Cristian\DAM2\AgriSynct\docs\Permisos.md b/c:\Cristian\DAM2\AgriSynct\docs\Permisos.md
new file mode 100644
--- /dev/null
+++ b/c:\Cristian\DAM2\AgriSynct\docs\Permisos.md
@@ -0,0 +1,468 @@
---
# Permisos i Seguretat a AgriSync

## 1. Objectiu del sistema de permisos

El sistema de permisos d'AgriSync està pensat perquè el control d'accés real no depengui només de la interfície gràfica, sinó de la base de dades. Això vol dir que encara que un usuari intenti accedir directament a l'API REST de Supabase, la base de dades continua decidint què pot veure i què pot modificar.

L'objectiu és garantir tres coses:

* Cada usuari només veu la informació que li correspon
* Només poden modificar dades els usuaris amb permís real
* Els permisos es poden justificar de manera clara a nivell funcional i tècnic

Aquest sistema es basa en tres capes:

* Autenticació amb Supabase Auth
* Model de rols i scopes a la base de dades
* Polítiques RLS sobre cada taula

---

## 2. Idea general de funcionament

Quan un usuari inicia sessió:

1. Supabase Auth valida email i contrasenya.
2. L'aplicació obté el token JWT de l'usuari.
3. Amb aquest token, es busca quin tècnic de la taula `public.tecnic` està associat a `auth.uid()`.
4. A partir d'aquí, la base de dades sap quin usuari és, a quina oficina pertany, quin rol global té i a quins titulars està assignat.
5. Quan l'app fa consultes o modificacions, la base de dades aplica automàticament les policies RLS.

---

## 3. Peces que formen el sistema de permisos

### 3.1. Supabase Auth

És la capa d'autenticació.

**Responsabilitat:**

* Validar credencials
* Generar el token JWT
* Identificar l'usuari autenticat amb `auth.uid()`

**Important:**

Supabase Auth només diu qui és l'usuari. No diu encara què pot fer dins del negoci. Això ho resol la taula `tecnic` i les policies SQL.

### 3.2. Taula `tecnic`

La taula `public.tecnic` és el pont entre l'usuari autenticat i el model funcional del sistema.

**Camps que afecten permisos:**

* `user_id`
* `oficina_id`
* `rol`
* `actiu`
+

Sense un registre vàlid en aquesta taula, l'usuari no pot treballar amb normalitat dins del sistema.

### 3.3. Taula `tecnic_titular`

La taula `public.tecnic_titular` defineix a quins titulars pot accedir un tècnic i amb quin nivell funcional.

**Camps clau:**

* `tecnic_id`
* `titular_id`
* `scope`
* `actiu`

---
+

## 4. Rols globals

Els rols globals estan definits a l'enum `rol_global`:

* `admin`
* `oficina_manager`
* `tecnic`
* `lectura`

#### 4.1. Admin

És el rol amb més capacitat. Pot gestionar l'estructura del sistema i veure totes les dades del MVP.

#### 4.2. Oficina manager

És un rol intermedi. Pot gestionar tècnics del seu àmbit i operar sobre les dades principals del projecte.

#### 4.3. Tècnic

És el perfil de treball habitual. Només veu titulars assignats i només pot modificar allò que li permet l'scope sobre cada titular.

#### 4.4. Lectura

Existeix com a rol previst al model. En el SQL actual es conserva a l'enum però no rep permisos especials d'escriptura.

---
+

## 5. Scopes per titular

Els scopes estan definits a l'enum `scope_titular`:

* `comu`
* `agricola`
* `ramader`
* `lectura`

#### 5.1. Comu

Permet treballar tant la part agrícola com la ramadera d'un titular.

#### 5.2. Agricola

Permet treballar la part agrícola del titular.

#### 5.3. Ramader

Permet treballar la part ramadera del titular.

#### 5.4. Lectura

Representa un accés sense escriptura. En el model actual no rep permisos d'escriptura perquè `can_write_agricola` i `can_write_ramader` no el validen.

---
+

## 6. Per què hi ha rol global i scope alhora

S'ha separat en dues capes perquè resolen problemes diferents:

* El rol global defineix la posició de l'usuari dins l'organització
* L'scope defineix què pot fer sobre un titular concret

**Exemple:**

* Un `admin` no necessita assignacions per veure-ho tot
* Un `tecnic` normal necessita assignacions a `tecnic_titular`
* Un tècnic pot tenir `agricola` en un titular i `ramader` en un altre

---
+

## 7. Funcions helper de permisos

El SQL defineix funcions helper per reutilitzar lògica a les policies.

* **7.1.** `get_my_tecnic()` — Busca el tècnic associat a `auth.uid()`.
* **7.2.** `current_oficina_id()` — Retorna l'oficina del tècnic autenticat.
* **7.3.** `is_admin()` — Comprova si l'usuari és admin.
* **7.4.** `is_oficina_manager()` — Comprova si l'usuari és gestor d'oficina.
* **7.5.** `same_oficina(p_tecnic_id uuid)` — Comprova si un tècnic és de la mateixa oficina que l'usuari actual.
* **7.6.** `can_read_titular(p_titular_id uuid)` — Permet llegir un titular si:
	* ets `admin`
	* ets `oficina_manager`
	* o tens una assignació activa a aquell titular
* **7.7.** `can_write_scope(p_titular_id uuid, p_scope scope_titular)` — Permet escriure si:
	* ets `admin`
	* ets `oficina_manager`
	* tens `scope = comu`
	* o tens exactament l'scope requerit
* **7.8.** `can_write_agricola(...)` — Especialització per la part agrícola.
* **7.9.** `can_write_ramader(...)` — Especialització per la part ramadera.

---
+

## 8. Què és RLS

RLS vol dir **Row Level Security**.

És un mecanisme de PostgreSQL que permet definir permisos per fila. Això fa possible que dos usuaris consultin la mateixa taula però no vegin els mateixos registres.

---
+

## 9. Diferència entre GRANT i RLS

* **GRANT:** Permet intentar operar sobre la taula.
* **RLS:** La policy decideix si aquella operació concreta està realment permesa.

Per tant:

* El `GRANT` obre la porta general
* La `policy RLS` decideix si pots passar

---
+

## 10. Taules amb RLS al MVP actual

El SQL actual activa RLS a:

* `oficina`
* `tecnic`
* `titular`
* `tecnic_titular`
* `dan_declaracio`
* `terra`
* `aplicacions_fertilitzants`
* `granja`
* `bestiar`
* `fase_productiva`
* `granja_bestiar`
* `entrega_dejeccions`

---
+

## 11. Policies per taula

* **11.1.** `oficina`
	* `select`: `admin`, `oficina_manager`
	* `insert`, `update`, `delete`: només `admin`
* **11.2.** `tecnic`
	* `select`: `admin`, `oficina_manager` si és de la mateixa oficina, el mateix usuari sobre el seu propi registre
	* `insert`: `admin`, `oficina_manager` dins la seva oficina
	* `update`: `admin`, `oficina_manager` dins la seva oficina, el mateix usuari sobre el seu propi registre
	* `delete`: `admin`, `oficina_manager` dins la seva oficina
* **11.3.** `titular`
	* `select`: qui compleixi `can_read_titular(...)`
	* `insert`: `admin`, `oficina_manager`
	* `update`: `admin`, `oficina_manager`, un tècnic amb `scope = comu`
	* `delete`: `admin`, `oficina_manager`
* **11.4.** `tecnic_titular`
	* `select`: `admin`, `oficina_manager`, el tècnic sobre les seves pròpies assignacions
	* `insert`, `update`, `delete`: `admin`, `oficina_manager`
* **11.5.** `dan_declaracio`
	* `select`: si pots llegir el titular relacionat
	* `insert`, `update`: `admin`, `oficina_manager`, qui tingui permís agrícola o ramader sobre el titular
	* `delete`: `admin`, `oficina_manager`
* **11.6.** `terra`
	* `select`: `admin`, `oficina_manager`, qui pugui llegir el titular de la terra
	* `insert`, `update`, `delete`: `admin`, `oficina_manager`, qui tingui `can_write_agricola(...)`
* **11.7.** `aplicacions_fertilitzants`
	* `select`: `admin`, `oficina_manager`, qui pugui llegir el titular de la DAN relacionada
	* `insert`, `update`, `delete`: `admin`, `oficina_manager`, qui tingui permís agrícola sobre el titular de la DAN
* **11.8.** `granja`
	* `select`: qui pugui llegir el titular propietari
	* `insert`, `update`, `delete`: `admin`, `oficina_manager`, qui tingui permís ramader sobre el titular
* **11.9.** `bestiar`
	* `select`: qualsevol usuari autenticat
	* `insert`, `update`, `delete`: només `admin`
* **11.10.** `fase_productiva`
	* `select`: qualsevol usuari autenticat
	* `insert`, `update`, `delete`: només `admin`
* **11.11.** `granja_bestiar`
	* `select`: `admin`, `oficina_manager`, qui pugui llegir el titular de la granja relacionada
	* `insert`, `update`, `delete`: `admin`, `oficina_manager`, qui tingui permís ramader sobre el titular de la granja
* **11.12.** `entrega_dejeccions`
	* `select`: `admin`, `oficina_manager`, qui pugui llegir el titular de la DAN associada
	* `insert`, `update`, `delete`: `admin`, `oficina_manager`, qui tingui permís ramader sobre el titular de la DAN

---
+

## 12. Herència funcional dels permisos

Moltes taules no tenen permisos aïllats, sinó que hereten la lògica d'una entitat superior.

**Exemples:**

* `terra` hereta del `titular`
* `aplicacions_fertilitzants` hereta del `titular` a través de `dan_declaracio`
* `granja` hereta del `titular`
* `granja_bestiar` hereta del `titular` a través de `granja`
* `entrega_dejeccions` hereta del `titular` a través de `dan_declaracio`

---
+

## 13. Exemples pràctics

* **Cas 1. Admin**
	* veu tot
	* pot gestionar estructura i dades
* **Cas 2. Manager d'oficina**
	* gestiona tècnics del seu àmbit
	* pot operar sobre dades del MVP
* **Cas 3. Tècnic amb scope agrícola**
	* pot veure el titular
	* pot editar terres i aplicacions
	* no pot editar la part ramadera
* **Cas 4. Tècnic amb scope ramader**
	* pot veure el titular
	* pot editar granges, cens i entregues
	* no pot editar la part agrícola
* **Cas 5. Tècnic amb scope comú**
	* pot operar tant a agrícola com a ramader
	* pot editar dades bàsiques del titular

---
+

## 14. Valor d'aquest model

Aquest model és defensable perquè:

* No deixa la seguretat a la UI
* Separa autenticació i autorització
* Combina rol global i permís específic
* Encaixa amb el domini real del projecte

---
+

## 15. Millores futures

Possibles millores:

* Donar més comportament específic al rol `lectura`
* Restringir encara més l'abast de `oficina_manager` si cal
* Afegir proves específiques de permisos per rol i taula

---
+

## 16. Resum final

El sistema de permisos d'AgriSync es basa en Supabase Auth per identificar l'usuari i en PostgreSQL amb RLS per decidir què pot veure i modificar.

La combinació de:

* `user_id`
* `rol`
* `oficina`
* `tecnic_titular`
* `scope`
* `policies`

fa possible un control d'accés robust i molt adequat per a un MVP acadèmic amb dades sensibles.
+
