# OpenELIS Ethiopia — Configuration Guide

The `volume` directory contains the **reference configuration** for the
OpenELIS Ethiopia deployment. The
[openelis-ethiopia-distro](https://github.com/DIGI-UW/openelis-ethiopia-distro)
repository pulls this structure into its own `configs/` directory, mounts it
into Docker containers, and that is how the Ethiopian system is configured —
no source-code changes to OpenELIS itself are needed.

---

## How a Distro Works

```
openelis-ethiopia-distro/
├── .env                   ← passwords and hostnames
├── docker-compose.yml     ← pulls pre-built images, mounts configs/
└── configs/               ← mirrors this volume/ directory
    ├── configuration/
    ├── properties/
    ├── database/
    ├── nginx/
    ├── menu/
    ├── analyzer/
    └── ...
```

On `docker-compose up`:

1. Docker pulls the pre-built `itechuw/openelis-global-2` images.
2. Each `configs/` subdirectory is bind-mounted into the relevant container
   (see [Docker Volume Mappings](#docker-volume-mappings)).
3. On startup the application reads every CSV/JSON under
   `configuration/backend/` and seeds the database — test sections, tests,
   sample types, roles, etc.
4. Changes to config files take effect on the next container restart.

---

## Directory Overview

```
volume/                            ← configs/ in the distro repo
├── analyzer/                      # Instrument-to-test mappings (CSV)
├── configuration/
│   └── backend/
│       ├── dictionaries/          # Custom dictionary/result entries
│       ├── notebook-departments/  # Notebook ↔ lab department linkages
│       ├── ocl/                   # Observation Concept Library bundles
│       ├── questionnaires/        # FHIR Questionnaire resources
│       ├── roles/                 # RBAC role definitions
│       ├── sample-types/          # Biological sample types
│       ├── storage-boxes/         # Cold-storage box definitions
│       ├── storage-devices/       # Cold-storage device definitions
│       ├── storage-racks/         # Cold-storage rack definitions
│       ├── storage-rooms/         # Cold-storage room definitions
│       ├── storage-shelves/       # Cold-storage shelf definitions
│       ├── test-sample-types/     # Test ↔ sample-type mappings
│       ├── test-sections/         # Laboratory departments
│       └── tests/                 # Lab test definitions
├── database/                      # DB credentials and init SQL
├── letsencrypt/                   # TLS certificates (runtime)
├── logs/                          # Application logs (runtime)
├── lucene/                        # Search indexes (auto-generated)
├── menu/                          # Navigation menu (JSON)
├── nginx/                         # Nginx reverse-proxy config
├── odoo/                          # Odoo billing integration
├── plugins/                       # Drop-in plugin JARs
├── programs/                      # FHIR programme resources
├── properties/                    # Core application properties
└── tomcat/                        # Tomcat server descriptors
```

---

## Docker Volume Mappings

| Host path (`configs/…`) | Container path | Container |
|--------------------------|----------------|-----------|
| `./configuration` | `/var/lib/openelis-global/configuration` | openelis |
| `./properties` | `/var/lib/openelis-global/` | openelis |
| `./database` | `/var/lib/postgresql/` | db |
| `./logs/oeLogs` | `/var/log/openelis-global/` | openelis |
| `./logs/tomcatLogs` | `/usr/local/tomcat/logs/` | openelis |
| `./nginx` | `/etc/nginx/` | nginx |
| `./letsencrypt` | `/etc/letsencrypt/` | certbot |
| `./plugins` | `/var/lib/openelis-global/plugins/` | openelis |
| `./programs` | `/var/lib/openelis-global/programs/` | openelis |
| `./menu` | `/var/lib/openelis-global/menu/` | openelis |
| `./lucene` | `/var/lib/openelis-global/lucene/` | openelis |
| `./analyzer` | `/var/lib/openelis-global/analyzer/` | openelis |
| `./odoo` | `/var/lib/openelis-global/odoo/` | openelis |
| `./tomcat` | `/usr/local/tomcat/conf/Catalina/localhost/` | openelis |

---

## How Configuration Loading Works

Files under `configuration/backend/` are picked up at startup by Spring-managed
`DomainConfigurationHandler` classes:

1. **Startup scan** — each handler reads all files in its subdirectory.
2. **Checksum tracking** — a `*-checksums.properties` file per domain is kept
   alongside each subdirectory. Files whose checksums have not changed are
   skipped, so restarts are fast.
3. **Dual source merge** — built-in defaults ship inside the application JAR
   (classpath). Files you place on the filesystem (in `configs/`) take
   precedence and override those defaults.
4. **Force reload** — if you need to reprocess every file regardless of
   checksums, see [Force-Reloading Configuration](#force-reloading-configuration).

---

## Ethiopia Distro Setup — Step by Step

### Step 1 — Environment variables (`.env`)

```env
ADMIN_PASSWORD=change-me
OE_DB_PASSWORD=change-me
SSL_KEYSTORE_PASSWORD=kspass
SSL_TRUSTSTORE_PASSWORD=tspass
```

Keep this file out of version control (it is in `.gitignore`).

---

### Step 2 — Application properties

**`configs/properties/common.properties`** — integration endpoints and feature
flags. Key settings for Ethiopia:

```properties
# FHIR store
org.openelisglobal.fhirstore.uri=https://fhir.openelis.org:8443/fhir/

# Client registry (OpenCR) — fill in when available
org.openelisglobal.crserver.uri=
org.openelisglobal.crserver.username=
org.openelisglobal.crserver.password=

# Odoo billing — set true and fill credentials to enable
org.openelisglobal.odoo.enabled=false
org.openelisglobal.odoo.baseUrl=
org.openelisglobal.odoo.database=
org.openelisglobal.odoo.username=
org.openelisglobal.odoo.password=

# Freezer monitoring (Modbus)
org.openelisglobal.freezermonitoring.enabled=true
org.openelisglobal.freezermonitoring.modbus.poll-interval=PT5M
```

**`configs/properties/SystemConfiguration.properties`** — lab identity and UI
behaviour. All entries are commented out by default; uncomment only the ones
you need:

```properties
SiteName=Ethiopian Public Health Institute
SiteCode=EPHI-01
DEFAULT_DATE_LOCALE=en-US
AccessionFormat=SITEYEARNUM
REQUIRE_LAB_UNIT_AT_LOGIN=true
PATIENT_NATIONAL_ID_REQUIRED=false
```

---

### Step 3 — Laboratory departments (`test-sections/`)

File: `configs/configuration/backend/test-sections/ethiopia.csv`

The Ethiopia distro defines the following laboratory sections:

```csv
testSectionName,description,isActive,sortOrder,isExternal,englishName,frenchName
Hematology,Hematology Department,Y,1,N,Hematology,Hematology
Biochemistry,Biochemistry Department,Y,2,N,Biochemistry,Biochemistry
Serology,Serology Department,Y,3,N,Serology,Serology
Parasitology,Parasitology Department,Y,4,N,Parasitology,Parasitology
Urinalysis,Urinalysis Department,Y,5,N,Urinalysis,Urinalysis
Microbiology,Microbiology Department,Y,6,N,Microbiology,Microbiology
Immunology,Immunology Department,Y,7,N,Immunology,Immunology
Molecular Biology,Molecular Biology Department,Y,8,N,Molecular Biology,Molecular Biology
Cytology,Cytology Department,Y,9,N,Cytology,Cytology
Histopathology,Histopathology Department,Y,10,N,Histopathology,Histopathology
Genomics & Bioinformatics Laboratory,Genomics & Bioinformatics Lab,Y,11,N,Genomics & Bioinformatics Laboratory,Genomics & Bioinformatics Laboratory
Traditional & Modern Medicine Research Lab,Traditional & Modern Medicine Research Lab,Y,12,N,Traditional & Modern Medicine Research Lab,Traditional & Modern Medicine Research Lab
Bioanalytical Laboratory,Bioanalytical Testing Laboratory Department,Y,13,N,Bioanalytical Laboratory,Bioanalytical Laboratory
Bioequivalence Laboratory,Bioequivalence Testing Laboratory Department,Y,14,N,Bioequivalence Laboratory,Bioequivalence Laboratory
```

Rules:
- `testSectionName` is the unique key referenced everywhere else (tests, roles,
  notebook linkages).
- `isActive=N` hides a section without deleting historical data.
- `sortOrder` controls the order in department dropdowns at login.
- `englishName` / `frenchName` can be identical — Ethiopia uses English only.

---

### Step 4 — Sample types (`sample-types/`)

File: `configs/configuration/backend/sample-types/ethiopia.csv`

One file covers all labs. `domain=H` denotes human specimens.

```csv
description,localAbbreviation,domain,isActive,sortOrder,englishName,frenchName
Whole Blood,WB,H,Y,1,Whole Blood,Whole Blood
Serum,SER,H,Y,2,Serum,Serum
Plasma,PL,H,Y,3,Plasma,Plasma
Urine,UR,H,Y,4,Urine,Urine
Stool,ST,H,Y,5,Stool,Stool
CSF,CSF,H,Y,6,Cerebrospinal Fluid,Cerebrospinal Fluid
Sputum,SPT,H,Y,7,Sputum,Sputum
Swab,SWB,H,Y,8,Swab,Swab
Tissue,TIS,H,Y,9,Tissue,Tissue
Saliva,SAL,H,Y,10,Saliva,Saliva
```

---

### Step 5 — Lab tests (`tests/`)

The Ethiopia distro uses **one CSV file per lab section** — e.g.
`immunology.csv`, `tuberculosis.csv`, `bacteriology.csv`. This keeps each lab's
test list self-contained and easy to maintain.

```csv
testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure,englishName,frenchName
```

Examples from the Ethiopia distro:

```csv
# immunology.csv
HIV 1/2 Antibody,Immunology,Serum,75622-1,Y,Y,1,,HIV 1/2 Antibody,HIV 1/2 Antibody
HBsAg,Immunology,Serum,5196-1,Y,Y,2,,Hepatitis B Surface Antigen,Hepatitis B Surface Antigen

# tuberculosis.csv
MTB Detection,Molecular Biology,Sputum,89646-4,Y,Y,1,,MTB Detection,MTB Detection
RIF Resistance,Molecular Biology,Sputum,89646-4,Y,Y,2,,Rifampicin Resistance,Rifampicin Resistance

# bacteriology.csv
Culture & Sensitivity,Microbiology,Swab,,Y,Y,1,,Culture and Sensitivity,Culture and Sensitivity
```

Rules:
- `testSection` must exactly match a `testSectionName` from Step 3.
- `sampleType` must exactly match a `description` from Step 4.
- `loinc` is optional but recommended for FHIR interoperability.

---

### Step 6 — Test ↔ sample-type mappings (`test-sample-types/`)

Also one file per lab section. This defines which sample types each test
accepts (many-to-many).

```csv
testName,sampleType
HIV 1/2 Antibody,Serum
HIV 1/2 Antibody,Whole Blood
MTB Detection,Sputum
Culture & Sensitivity,Swab
Culture & Sensitivity,Tissue
```

A test can have multiple valid sample types — add one row per allowed
combination.

---

### Step 7 — Roles (`roles/`)

File: `configs/configuration/backend/roles/ethiopia-roles.csv`

See `example-lab-roles.csv` for the column structure. The Ethiopia distro
defines roles aligned to the lab sections above — e.g. a Hematology
Technician role scoped to the Hematology section.

---

### Step 8 — Notebook ↔ department linkages (`notebook-departments/`)

Notebooks are created through the Admin UI (Settings → Notebooks). Once they
exist in the system, link each notebook to its lab department here so that
users see the right notebook when they log in.

File: `configs/configuration/backend/notebook-departments/research-lab-linkages.csv`

Current Ethiopia linkages:

```csv
notebookTitle,departmentName
Traditional & Modern Medicine Research Lab,Traditional & Modern Medicine Research Lab
Genomics & Bioinformatics Laboratory,Genomics & Bioinformatics Laboratory
Bioanalytical Laboratory,Bioanalytical Laboratory
Bioequivalence Laboratory,Bioequivalence Laboratory
Immunology Laboratory,Immunology
Pathology Laboratory,Pathology Laboratory
Bacteriology Laboratory,Bacteriology
Malaria and Neglected Tropical Disease (MNTD) Laboratory,Malaria and Neglected Tropical Disease (MNTD) Laboratory
Pharmaceuticals Laboratory,Pharmaceuticals Laboratory
Virology and Vaccine Unit,Virology and Vaccine Unit
Tuberculosis Laboratory,Tuberculosis Laboratory
Medical Laboratory,Medical Laboratory
Biorepository Laboratory,Biorepository Laboratory
```

Rules:
- Both values are **case-sensitive** and must be exact matches.
- `notebookTitle` must match the notebook's title in the database.
- `departmentName` must match a `testSectionName` from Step 3.
- A notebook can appear in multiple departments — add one row per department.

> See [`configuration/backend/notebook-departments/README.md`](configuration/backend/notebook-departments/README.md)
> for troubleshooting SQL queries.

---

### Step 9 — Analyzer mappings (`analyzer/`)

The Ethiopia distro maps two analyzers:

File: `configs/analyzer/analyzer-test-map.csv`

```csv
ANALYZER,ANALYZER_TEST_NAME,LOINC_CODE,ACTUAL_TEST_NAME
GeneXpertAnalyzer,MTB,89646-4,MTB Detection
GeneXpertAnalyzer,MTB Trace,89646-4,MTB Trace
GeneXpertAnalyzer,RIF Resistance,89646-4,RIF Resistance
PocH100iAnalyzer,WBC,6690-2,White Blood Cell Count
PocH100iAnalyzer,RBC,789-8,Red Blood Cell Count
PocH100iAnalyzer,HGB,718-7,Hemoglobin
```

`ANALYZER` must match the analyzer identifier registered in the system.
`ACTUAL_TEST_NAME` must match a `testName` from Step 5.

---

### Step 10 — Storage hierarchy (optional)

For labs managing physical sample storage (freezers, racks, boxes), seed the
hierarchy in order from largest to smallest container:

```
storage-rooms/ → storage-devices/ → storage-racks/ → storage-shelves/ → storage-boxes/
```

Each level is a CSV with `name`, `description`, `status`, `sortOrder`, and a
parent reference column. The Biorepository lab uses this for cryogenic storage.

---

### Step 11 — Database credentials

`configs/database/database.env`:

```env
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your-secure-password
POSTGRES_DB=clinlims
POSTGRES_INITDB_ARGS="--auth-host=md5"
```

`POSTGRES_PASSWORD` **must match** the value in
`configs/properties/datasource.password`. Change both together.

---

### Step 12 — Nginx

Edit `configs/nginx/nginx.conf` and set `server_name` to the Ethiopia
deployment hostname. For production TLS, use `nginx-prod.conf` and place the
Let's Encrypt certificates in `configs/letsencrypt/`.

Standard proxy routes (do not change):

```
/        →  frontend  (port 3000)
/api/    →  openelis  (port 8443)
/rest/   →  openelis  (port 8443)
```

---

### Step 13 — Menu (`menu/menu_config.json`)

The menu configuration is generated through the Admin UI — you do not edit the
JSON by hand.

#### How to generate the menu config

1. Start the OpenELIS instance (`docker-compose up -d`).
2. Log in as an administrator.
3. Navigate to **Admin → Global Menu Configuration** in the sidebar.
4. Check the menu items you want to include; uncheck those you want to hide.
5. Click **Save**.
6. Click **Export Config** — this downloads `menu_config.json`.
7. Copy the downloaded file into `configs/menu/menu_config.json` in your distro
   repository.
8. Commit the file so the menu is locked in for all future deployments.

On the next `docker-compose up`, the application loads the file automatically
(controlled by `org.openelisglobal.menu.configuration.autocreate=true` in
`common.properties`, which is on by default).

#### Updating the menu later

Repeat the same steps — adjust selections in the UI, export again, and replace
`configs/menu/menu_config.json` with the new file.

> The `menu_config.json` already committed in this repository reflects the
> current Ethiopia menu selection. Use it as a starting point.

---

### Step 14 — Deploy

```bash
cd openelis-ethiopia-distro
docker-compose up -d

# Watch startup — look for "Configuration loaded" per domain
docker-compose logs -f openelis
```

---

## Configuration Dependency Order

Seed data depends on earlier domains being loaded. The application handles this
automatically on startup, but when authoring files keep this order in mind:

```
1. test-sections          ← everything else references department names
2. sample-types           ← needed by tests and test-sample-types
3. tests                  ← depends on test-sections + sample-types
4. test-sample-types      ← depends on tests + sample-types
5. roles                  ← can reference test-sections
6. notebook-departments   ← depends on notebooks existing in DB + test-sections
7. storage-rooms → devices → racks → shelves → boxes  (parent before child)
```

---

## Force-Reloading Configuration

After `docker-compose down -v`, checksums are wiped and all files reload
automatically on the next start. If checksums exist but you still need to
force a reload:

```properties
# configs/properties/common.properties
org.openelisglobal.configuration.forcereload=true
```

Restart the container, then remove or set back to `false`.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Config change ignored after restart | Checksum cached | Set `forcereload=true`, restart, then remove it |
| Lab section not visible at login | `isActive=N` or name mismatch | Check CSV; `SELECT * FROM test_section;` |
| Test missing from a sample type | No row in `test-sample-types/` | Add `testName,sampleType` row |
| Notebook not visible for a user | Missing linkage or user not assigned to section | Check `research-lab-linkages.csv` and user's test section assignment |
| Analyzer result not mapped | Missing row in `analyzer-test-map.csv` | Add the instrument + test row and restart |
| Database won't start | Password mismatch | Align `database.env` and `datasource.password` |
| SSL errors | Wrong cert paths | Verify `ssl_certificate` paths in `nginx.conf` |
| Menu items missing | File not loaded | Ensure `menu.configuration.autocreate=true` |

---

## Related Documentation

- [`configuration/README.md`](configuration/README.md) — `DomainConfigurationHandler` mechanism
- [`configuration/backend/test-sections/README.md`](configuration/backend/test-sections/README.md) — Test sections field reference
- [`configuration/backend/notebook-departments/README.md`](configuration/backend/notebook-departments/README.md) — Notebook linkages and troubleshooting
- [`../AGENTS.md`](../AGENTS.md) — Full project onboarding guide
- [openelis-ethiopia-distro](https://github.com/DIGI-UW/openelis-ethiopia-distro) — The Ethiopia deployment repository
