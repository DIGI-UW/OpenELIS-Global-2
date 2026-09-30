# Vector Surveillance — Implementation Tasks

> Module: Vector and BPP (v0.4) Branch: feature/vector-surveillance Depends on:
> address hierarchy (done), decoupled ordering workflow (done)

---

## Phase 1 — Database Schema & Reference Data

- [x] **1.1** Create `020-vector-surveillance-schema.xml`

  - Tables: `vector_organism_group`, `vector_trap_type`, `vector_species`
  - Seed groups: MOSQUITO (system), FLY, FLEA, RODENT
  - Seed trap types: BG-Sentinel Trap, CDC Light Trap, Ovitrap (all → Mosquito)
  - Seed species: _Aedes aegypti_ (Mosquito, Dengue/Zika/Chikungunya,
    ADULT/LARVA/PUPA/EGG), _Anopheles sp._ (Mosquito, Malaria)
  - All changeSets use `preConditions onFail="MARK_RAN"`

- [x] **1.2** Create `021-vector-sample-domain.xml`

  - Insert `SAMPLE_DOMAIN` row: code `V`, description `Vector Surveillance`

- [x] **1.3** Create `022-vector-observation-types.xml`

  - Insert `observation_history_type` rows:

  | Enum                           | DB type_name             |
  | ------------------------------ | ------------------------ |
  | `VECTOR_WORKFLOW_TYPE`         | `vecWorkflowType`        |
  | `VECTOR_ORGANISM_GROUP_ID`     | `vecOrganismGroupId`     |
  | `VECTOR_SPECIES_ID`            | `vecSpeciesId`           |
  | `VECTOR_LIFECYCLE_STAGE`       | `vecLifecycleStage`      |
  | `VECTOR_TRAP_TYPE_ID`          | `vecTrapTypeId`          |
  | `VECTOR_POOLING_METHOD`        | `vecPoolingMethod`       |
  | `VECTOR_POOL_COUNT`            | `vecPoolCount`           |
  | `VECTOR_SAMPLES_PER_POOL`      | `vecSamplesPerPool`      |
  | `VECTOR_PATHOGENS_OF_INTEREST` | `vecPathogensOfInterest` |
  | `VECTOR_COLLECTION_SITE_ID`    | `vecCollectionSiteId`    |
  | `VECTOR_COLLECTION_SITE_NAME`  | `vecCollectionSiteName`  |
  | `VECTOR_GPS_LATITUDE`          | `vecGpsLatitude`         |
  | `VECTOR_GPS_LONGITUDE`         | `vecGpsLongitude`        |

- [x] **1.4** Register all three migrations in
      `src/main/resources/liquibase/3.4.x.x/base.xml` after
      `018-uom-type-mapping-table.xml`

---

## Phase 2 — Backend Reference Data Layer

- [x] **2.1** Create entities using JPA annotations (`jakarta.persistence.*` +
      Hibernate annotations)

  - `org.openelisglobal.vector.valueholder.VectorOrganismGroup`
  - `org.openelisglobal.vector.valueholder.VectorTrapType` — `@ManyToOne` to
    `VectorOrganismGroup`
  - `org.openelisglobal.vector.valueholder.VectorSpecies` — `@ManyToOne` to
    `VectorOrganismGroup`
  - Uses `@Entity`, `@Table`, `@DynamicUpdate`, `@GenericGenerator`
    (StringSequenceGenerator), `@Type` (LIMSStringNumberUserType)
  - No HBM XML files — registered in `persistence.xml` and
    `test-persistence.xml`

- [x] **2.2** Register entities in `persistence.xml` and `test-persistence.xml`
      under `<!-- Vector Surveillance entities -->`

  - No changes needed to `hibernate.cfg.xml` or `test-hibernate.cfg.xml`

- [x] **2.3** Create DAO interfaces + impls (`BaseDAO` / `BaseDAOImpl` pattern)

  - `VectorOrganismGroupDAO` / `VectorOrganismGroupDAOImpl`
    - Extra methods: `getActiveGroups()`, `getByCode(String code)`
  - `VectorTrapTypeDAO` / `VectorTrapTypeDAOImpl`
    - Extra method: `getByGroupId(String groupId)`
  - `VectorSpeciesDAO` / `VectorSpeciesDAOImpl`
    - Extra method: `getByGroupId(String groupId)`

- [x] **2.4** Create service interfaces + impls (`@Transactional` on service
      only)

  - `VectorOrganismGroupService` / `VectorOrganismGroupServiceImpl`
  - `VectorTrapTypeService` / `VectorTrapTypeServiceImpl`
  - `VectorSpeciesService` / `VectorSpeciesServiceImpl`

- [x] **2.5** Create admin REST controllers
  - `VectorOrganismGroupRestController` —
    `@RequestMapping("/rest/admin/vector/groups")`
    - `GET /` — all groups (admin list)
    - `GET /active` — active only (order form dropdown)
    - `POST /` — create
    - `PUT /{id}` — update
  - `VectorTrapTypeRestController` —
    `@RequestMapping("/rest/admin/vector/trap-types")`
    - `GET /?groupId=X` — filtered by group
    - `POST /`, `PUT /{id}`
  - `VectorSpeciesRestController` —
    `@RequestMapping("/rest/admin/vector/species")`
    - `GET /?groupId=X` — filtered by group; response includes
      `lifecycle_stages` and `pathogens_of_interest`
    - `POST /`, `PUT /{id}`

---

## Phase 3 — Backend Order Entry Integration

- [x] **3.1** Add 13 new `ObservationType` enum entries to
      `ObservationHistoryServiceImpl.java`

  - `VS_WORKFLOW_TYPE` … `VS_GPS_LONGITUDE` using `vec*` DB names

- [x] **3.2** Extend domain-setting logic in `SamplePatientUpdateData.java`

  - `"vector"` branch sets `sample.setDomain("V")`

- [x] **3.3** Add `addVectorObservations()` to `SamplePatientUpdateData.java`

  - Guards on `workflowType = "vector"`; maps all 13 `vec*` keys
  - Called from `addObservations()` alongside `addEnvironmentalObservations()`

- [x] **3.4** Extend patient-skip logic in
      `SamplePatientEntryRestController.java`

  - All three `workflowType` guard conditions now include `"vector"`

- [ ] **3.5** Extend `SamplePatientUpdateData.persistSampleItems()` for pooling
  - When `workflowType = "vector"` and `vecPoolCount > 0`, create N `SampleItem`
    records
  - Set `SampleItem.sortOrder` = pool number (1…N)
  - Set `SampleItem.collectionQuantity` = `vecSamplesPerPool`
  - These items flow into the existing storage module unchanged

---

## Phase 4 — Frontend Admin UI

- [x] **4.1** Create
      `frontend/src/components/admin/vectorSurveillance/VectorSurveillanceSetup.jsx`

  - Two-column layout: groups list + inline form (left 5 cols) + detail Tabs
    (right 11 cols)
  - Left: inline add/edit form + clickable group list with system badge
  - Right Tabs: Trap Types (inline form + DataTable) / Species (inline form +
    DataTable)
  - Carbon: `DataTable`, `Tabs`, `TabList`, `TabPanels`, `TextInput`, `Toggle`,
    `Button`, `Tag`, `InlineLoading`, `Grid`, `Column`

- [x] **4.2** Register route and nav in `frontend/src/components/admin/Admin.js`
  - `<SideNavLink renderIcon={Sprout}>` → `vectorSurveillanceSetup`
  - `<Route path="${path}/vectorSurveillanceSetup" component={VectorSurveillanceSetup} />`

---

## Phase 5 — Frontend Order Entry

- [x] **5.1** Create
      `frontend/src/components/order/steps/sections/VectorSection.jsx`

  - Group → cascade species + trap types; species → auto-fill pathogens +
    lifecycle options
  - Pathogens rendered as read-only `Tag` chips
  - GPS latitude/longitude text inputs
  - Pooling: method select, pool count, specimens/pool, calculated total

- [x] **5.2** Extend `frontend/src/components/order/steps/OrderEnter.jsx`

  - `workflowType` comment updated to include `"vector"`
  - `handleWorkflowTypeChange`: index 2 → `"vector"`; `patientUpdateStatus` set
    to `NO_ACTION` for vector
  - `hasPatientOrSite`: vector branch checks `vecOrganismGroupId`
  - `ContentSwitcher`: third `<Switch name="vector">` added
  - `selectedIndex`: ternary extended for vector (index 2)
  - Render branch: `{workflowType === "vector" && <VectorSection ... />}`

- [ ] **5.3** Extend auto-save condition in
      `frontend/src/components/order/OrderContext.js`

  - Add vector branch to `hasPatientOrSite` check:
    `workflowType === "vector" ? !!(envFields.vecOrganismGroupId) : ...`

- [x] **5.4** Add i18n keys to `frontend/src/languages/en.json`
  ```json
  "workflow.vector": "Vector Surveillance",
  "vector.organismGroup": "Organism Group",
  "vector.species": "Species",
  "vector.lifecycleStage": "Lifecycle Stage",
  "vector.trapType": "Trap Type",
  "vector.poolingMethod": "Pooling Method",
  "vector.poolCount": "Number of Pools",
  "vector.samplesPerPool": "Specimens per Pool",
  "vector.totalSpecimens": "Total Specimens",
  "vector.pathogensOfInterest": "Pathogens of Interest",
  "vector.homogeneous": "Homogeneous",
  "vector.random": "Random"
  ```

---

## Files Changed Summary

| File                                                   | Action                                                                                   |
| ------------------------------------------------------ | ---------------------------------------------------------------------------------------- |
| `liquibase/3.4.x.x/020-vector-surveillance-schema.xml` | New                                                                                      |
| `liquibase/3.4.x.x/021-vector-sample-domain.xml`       | New                                                                                      |
| `liquibase/3.4.x.x/022-vector-observation-types.xml`   | New                                                                                      |
| `liquibase/3.4.x.x/base.xml`                           | Modified — 3 new includes                                                                |
| `vector/valueholder/VectorOrganismGroup.java`          | New                                                                                      |
| `vector/valueholder/VectorTrapType.java`               | New                                                                                      |
| `vector/valueholder/VectorSpecies.java`                | New                                                                                      |
| `vector/dao/` + `daoimpl/` (6 files)                   | New                                                                                      |
| `vector/service/` + `serviceimpl/` (6 files)           | New                                                                                      |
| `vector/controller/rest/` (3 controllers)              | New                                                                                      |
| `persistence/persistence.xml`                          | Modified — 3 new `<class>` entries                                                       |
| `persistence/test-persistence.xml`                     | Modified — 3 new `<class>` entries                                                       |
| `ObservationHistoryServiceImpl.java`                   | Modified — 13 new enum entries                                                           |
| `SamplePatientUpdateData.java`                         | Modified — domain branch + `addVectorObservations()` + pooling in `persistSampleItems()` |
| `SamplePatientEntryRestController.java`                | Modified — 2 condition extensions                                                        |
| `VectorSurveillanceSetup.jsx`                          | New                                                                                      |
| `Admin.js`                                             | Modified — nav + route                                                                   |
| `VectorSection.jsx`                                    | New                                                                                      |
| `ObservationHistoryServiceImpl.java`                   | Modified — 13 new `VS_*` enum entries                                                    |
| `SamplePatientUpdateData.java`                         | Modified — vector domain branch + `addVectorObservations()`                              |
| `SamplePatientEntryRestController.java`                | Modified — 3 workflowType guards extended for "vector"                                   |
| `VectorSurveillanceSetup.jsx`                          | New                                                                                      |
| `Admin.js`                                             | Modified — `Sprout` nav link + route                                                     |
| `VectorSection.jsx`                                    | New                                                                                      |
| `OrderEnter.jsx`                                       | Modified — 3rd switch, vector render branch, hasPatientOrSite, patientUpdateStatus       |
| `OrderContext.js`                                      | Pending — auto-save condition (5.3)                                                      |
| `en.json`                                              | Modified — 34 new keys                                                                   |
