---
name: Analyzer Fixtures E2E Manual
overview:
  Use analyzer-harness to bring OpenELIS up for manual analyzer testing with a
  cohesive, integrity-safe analyzer fixture set (including newly added
  analyzers) and default config templates, and add a restart-analyzer-harness
  command to manage that harness lifecycle repeatably.
todos:
  - id: stop-root-stack
    content: Stop root dev.docker-compose stack
    status: completed
  - id: mount-defaults
    content:
      Add analyzer-defaults mount to
      projects/analyzer-harness/docker-compose.dev.yml
    status: pending
  - id: symlink-env
    content: Symlink .env to projects/analyzer-harness/.env
    status: completed
  - id: unify-analyzer-fixtures
    content:
      Consolidate analyzer fixtures to a single canonical source + single SQL
      artifact used by harness + E2E/CI (includes newly added analyzers)
    status: pending
  - id: whitelist-filtering
    content:
      Add YAML whitelist support to generate a filtered analyzer fixture SQL
      without maintaining multiple competing fixture sources
    status: pending
  - id: wire-loaders
    content:
      Update harness reset + unified loader + CI/E2E to load the same analyzer
      fixture SQL
    status: pending
  - id: run-harness-reset
    content: Run projects/analyzer-harness/reset-env.sh --full-reset
    status: completed
  - id: verify-fixtures
    content:
      Verify analyzer fixtures loaded in DB (localhost:15432) and no
      collisions/duplicates
    status: pending
  - id: test-defaults-api
    content: Test defaults API returns 11 templates
    status: pending
  - id: verify-infra
    content: Verify analyzer test infrastructure containers
    status: pending
  - id: create-command
    content: Create .specify/oe/commands/restart-analyzer-harness.md
    status: pending
isProject: false
---

# Analyzer Harness Setup for Manual Testing

## Problem

Goal: get OpenELIS up for **manual testing** with the testset of **default
analyzer configurations** (GenericASTM/GenericHL7 defaults), and do it in a
cohesive way that also supports E2E runs without fixture drift or integrity
surprises.

**Wrong stack was used previously**: `/restart-dev-env --full-reset` ran the
**root stack** (`[dev.docker-compose.yml](dev.docker-compose.yml)`) which:

- Has no analyzer test infrastructure (ASTM bridge, simulator, virtual serial
  ports)
- Historically relied on a storage-focused fixture loader (no analyzer testset)
- Mounts local WAR (for backend dev iteration) but isn't set up for analyzer
  manual testing

For **analyzer manual testing**, use the **analyzer-harness** stack
(`[projects/analyzer-harness/](projects/analyzer-harness/)`):

- Full analyzer test infrastructure via `docker-compose.analyzer-test.yml`
- `[reset-env.sh](projects/analyzer-harness/reset-env.sh)` loads ALL fixtures
  including Madagascar analyzers
- Pre-built images (faster for testing vs dev iteration)

Two gaps that block the goal:

- **Defaults not wired**: `analyzer-defaults/` (11 JSON templates) must be
  mounted into the webapp at `/data/analyzer-defaults` for the Defaults API and
  dashboard “Load Default Config” flow.
- **Analyzer fixtures not unified**: analyzer fixtures currently exist in
  multiple forms (SQL + DBUnit), and not all workflows load the same set. We
  need one canonical fixture set and one SQL artifact used by both manual +
  E2E/CI.

## Solution

### Phase 1: Reset analyzer-harness with full fixtures

#### 1. Stop root stack (currently running)

```bash
docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml down
```

#### 2. Mount analyzer-defaults in harness compose

Edit
`[projects/analyzer-harness/docker-compose.dev.yml](projects/analyzer-harness/docker-compose.dev.yml)`
line ~55 (oe service, after `analyzer-imports`):

```yaml
- ./volume/analyzer-imports:/data/analyzer-imports
- ../../analyzer-defaults:/data/analyzer-defaults:ro # Repo root analyzer-defaults/
```

Path is relative from `projects/analyzer-harness/` so `../../` reaches repo
root.

#### 3. Link .env to harness

Harness needs `.env` for `LETSENCRYPT_DOMAIN` in nginx template substitution:

```bash
ln -s ../../.env projects/analyzer-harness/.env
```

(Symlink preserves single source of truth.)

#### 4. Run harness full reset

```bash
cd projects/analyzer-harness
./reset-env.sh --full-reset
```

This already:

- Stops stack, removes volumes (`--full-reset`)
- Starts `docker-compose.dev.yml` + `docker-compose.analyzer-test.yml`:
  - ASTM-HTTP bridge (`openelis-analyzer-bridge:12001`)
  - ASTM simulator (`172.20.1.100:5000`)
  - Virtual serial ports (`/dev/serial/ttyVUSB0-4`)
- Waits for webapp
- Loads fixtures via `DB_PORT=15432`:
  - `[load-test-fixtures.sh](src/test/resources/load-test-fixtures.sh)`
    (foundational + storage)
  - `[load-analyzer-test-data.sh --dataset-011](src/test/resources/load-analyzer-test-data.sh)`
    (Madagascar analyzers 2000-2012)

Important: today this harness flow **does not** guarantee that the same analyzer
testset is used by E2E/CI (CI currently loads
`src/test/resources/analyzer-test-data.sql`). Phase 2 fixes that by
consolidating to a single artifact.

#### 5. Verify fixtures and defaults API

```bash
# Check Madagascar analyzers (2000-2012)
psql -U clinlims -d clinlims -h localhost -p 15432 -c \
  "SELECT id, name, analyzer_type FROM analyzer WHERE id BETWEEN 2000 AND 2012 ORDER BY id;"

# Verify generic configs
psql -U clinlims -d clinlims -h localhost -p 15432 -c \
  "SELECT id, analyzer_id, is_generic_plugin, identifier_pattern FROM analyzer_configuration WHERE is_generic_plugin = true ORDER BY id;"

# Test defaults API
curl -sk https://localhost/rest/analyzer/defaults | jq -r '.[]'
curl -sk https://localhost/rest/analyzer/defaults/hl7/mindray-bc2000 | jq .
```

Expected: 12 analyzers (2000-2012 including BC2000 at 2012), 11 default
templates.

#### 6. Verify analyzer infrastructure

```bash
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "openelis-analyzer-bridge|astm-simulator|virtual-serial"
```

### Phase 2: Unify analyzer fixtures into one SQL artifact (manual + E2E/CI)

You asked whether we should use DBUnit for E2E and how to have one SQL file used
in both workflows. The most cohesive approach is:

- **Canonical source (editable)**: one analyzer DBUnit dataset that reflects the
  contract set and required supporting rows.  
  Suggested path: `src/test/resources/testdata/analyzer-e2e.xml`
- **Canonical runtime artifact (executed everywhere)**: one committed SQL file
  generated from that dataset.  
  Suggested path: `src/test/resources/testdata/analyzer-e2e.generated.sql`

This yields a **single SQL file** that can be loaded by:

- Harness reset (direct `psql` to `localhost:15432`)
- Root loader (if used) and ad-hoc resets
- CI workflows (instead of `src/test/resources/analyzer-test-data.sql`)

Concrete wiring changes (to implement after plan confirmation):

1. **Create/merge canonical dataset**:

- Start from `src/test/resources/testdata/madagascar-analyzer-test-data.xml`
  (2000–2012 default analyzer configurations).
- Add a small set of **newly added analyzers** to exercise core workflows
  end-to-end (create analyzer, status=SETUP, query analyzer, build mappings).
  Put these in a clearly reserved ID range (e.g. 9000–9099) to avoid collisions.
- Avoid overlapping/competing analyzer definitions (no duplicate “same analyzer”
  under different IDs unless explicitly justified for a workflow).

2. **Generate SQL deterministically**: Reuse
   `src/test/resources/testdata/xml-to-sql.py` to produce
   `analyzer-e2e.generated.sql` and commit it.  
    (This keeps runtime loading SQL-only while retaining structured source.)
3. **Update loader entrypoints**:

- Update `projects/analyzer-harness/reset-env.sh` to load
  `analyzer-e2e.generated.sql` via psql in the same style it loads
  foundational/storage.
- Update `.github/workflows/frontend-qa.yml` to load
  `analyzer-e2e.generated.sql` instead of `analyzer-test-data.sql`.
- Optionally update `src/test/resources/load-test-fixtures.sh` to load
  `analyzer-e2e.generated.sql` (or expose a `--with-analyzers` flag).

Data-integrity policy:

- **Preferred**: harness resets use `--full-reset` so we start from a clean DB;
  fixture SQL can use stable IDs without worrying about collisions.
- **Also support repeatability**: include idempotent patterns where practical
  (or a `--reset` mode that truncates test ranges before insert).

Future: whitelist filtering (requested)

- Add YAML whitelist file:
  `src/test/resources/testdata/analyzer-e2e.whitelist.yml`
- Extend the generator step so we can optionally produce
  `analyzer-e2e.generated.filtered.sql` from the canonical dataset using that
  YAML whitelist.
  - Default remains “full” (comprehensive) for manual testing.
  - Filtered SQL enables targeted E2E suites or country-specific subsets without
    maintaining multiple fixture sources.

### Phase 3: Create restart-analyzer-harness command

Create
`[.specify/oe/commands/restart-analyzer-harness.md](.specify/oe/commands/restart-analyzer-harness.md)`
based on `[restart-dev-env.md](.specify/oe/commands/restart-dev-env.md)`.

Before creating the command, fix fixture consistency (Phase 2 above) so the
command can load one authoritative analyzer fixture SQL artifact.

**Key differences from restart-dev-env** (no tables; single source of truth):

- **Compose files**: use `projects/analyzer-harness/docker-compose.dev.yml` plus
  `projects/analyzer-harness/docker-compose.analyzer-test.yml`
- **WAR build**: skip (harness uses pre-built images; root stack is for local
  WAR iteration)
- **DB connectivity**: treat `localhost:15432` as the canonical port for fixture
  loading and verification
- **Fixture loading**: load one analyzer fixture SQL artifact (see Phase 2) plus
  storage/foundational fixtures
- **Infrastructure check**: verify analyzer infra containers (bridge, simulator,
  virtual serial)
- **Domain**: harness is `https://localhost/` (do not assume
  `analyzers.openelis-global.org` here)

**Command signature**:
`/restart-analyzer-harness [--full-reset] [--skip-fixtures]`

**Workflow checkpoints**:

1. Preflight (warn if root stack running; detect harness dir)
2. Stop harness stack (with `-v` if `--full-reset`)
3. Start harness stack (dev + analyzer-test)
4. Wait for webapp readiness (`https://localhost/`)
5. Load fixtures (unless `--skip-fixtures`):

- export `DB_PORT=15432` and load storage/foundational fixtures
- load analyzer fixtures from the unified analyzer SQL artifact (or
  `analyzer-e2e.generated.filtered.sql` when a whitelist is explicitly
  requested)

6. Verify analyzer infrastructure services
7. Report: container status, DB port 15432, access URLs, analyzer services

## Manual Testing Flow

**Dashboard**: [https://localhost/](https://localhost/)  
**Credentials**: admin / adminADMIN!

**Test scenarios** (representative, contract-aligned):

- **Generic ASTM (config-driven)**: Mindray BA-88A (fixture analyzer ID 2006)
  - Verify default loads; `identifier_pattern` is present; you can set
    `prefer_generic_plugin=true` to force generic over legacy when both could
    match.
- **Generic HL7 (config-driven)**: Mindray BC2000 (fixture analyzer ID 2012)
  - Verify default loads; identifier matching is configured.
- **Legacy HL7**: Abbott Architect (fixture analyzer ID 2000)
  - Verify it remains manageable as legacy alongside generic analyzers.
- **Legacy ASTM**: Horiba Pentra 60 C+ (fixture analyzer ID 2005)
  - Verify it remains manageable as legacy alongside generic analyzers.

**Analyzer infrastructure** (docker-compose.analyzer-test.yml):

- ASTM bridge at `openelis-analyzer-bridge:12001` (for TCP ASTM analyzers)
- ASTM simulator at `172.20.1.100:5000` (for fixture CONFIG-001)
- Virtual serial `/dev/serial/ttyVUSB0-4` (for RS232 analyzers like BA-88A,
  Pentra 60)

## Files Modified

- `[projects/analyzer-harness/docker-compose.dev.yml](projects/analyzer-harness/docker-compose.dev.yml)`
  — Add `analyzer-defaults` volume mount
- `[projects/analyzer-harness/.env](projects/analyzer-harness/.env)` — Symlink
  to root `.env`
- `[.specify/oe/commands/restart-analyzer-harness.md](.specify/oe/commands/restart-analyzer-harness.md)`
  — NEW command

## Success Criteria

- Harness stack running with full analyzer infrastructure (bridge, simulator,
  virtual serial)
- Madagascar contract analyzers (2000-2012) loaded in DB and coherent
- E2E/CI and manual testing load the same analyzer testset from a single SQL
  artifact
- Defaults API returns 11 templates from `/data/analyzer-defaults`
- Dashboard can load defaults and manage both generic and legacy analyzers
- `/restart-analyzer-harness` command available for repeatable resets
