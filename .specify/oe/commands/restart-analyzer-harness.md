# Restart Analyzer Harness

When the user invokes `/restart-analyzer-harness` (optionally with arguments),
restart the analyzer harness test environment using the project's own scripts.

**This skill delegates to `reset-env.sh`** — the authoritative harness lifecycle
script. It handles compose layering, bootstrap, plugin staging, fixture loading,
analyzer seeding, and Let's Encrypt setup. Do NOT reimplement these steps inline
— the scripts already handle edge cases (compose-stack.sh layering, credential
defaults, submodule init, checksum clearing, etc.).

## User Input

```text
$ARGUMENTS
```

Interpret arguments best-effort. Support these patterns:

- `/restart-analyzer-harness` → Default: `reset-env.sh --full-reset`
- `/restart-analyzer-harness --full-reset` → Drop database volumes (clean slate)
- `/restart-analyzer-harness --skip-fixtures` → Skip fixture and analyzer
  seeding
- `/restart-analyzer-harness --build` → Build WAR before restarting
- `/restart-analyzer-harness --letsencrypt` → Add LE compose overlay
- `/restart-analyzer-harness --skip-letsencrypt` → Skip LE even when env is set
- `/restart-analyzer-harness --ci-parity` → **Bring up the CI-parity stack**
  (`build.docker-compose.yml` + `.github/ci/ci.analyzer-harness.yml`) instead of
  the local dev stack. Selects frontend Dockerfile `target: runtime` (nginx
  serving minified `dist/`) — the exact image CI runs. Use this to locally
  reproduce CI E2E failures (e.g. React #130-class regressions that only fire
  under the production bundle) before pushing. Implicitly forces
  `--skip-letsencrypt` (CI compose files don't include the LE overlay).
- Combine flags as needed

## Safety Rules (non-negotiable)

- **Warn** if other Docker containers using ports 80/443/8443/15432 are running.
- **Never** drop database volumes unless `--full-reset` is explicitly passed.
- **Always** wait for webapp readiness before declaring success.
- **NEVER** modify, regenerate, or replace password hashes anywhere.

## Architecture (read this — do NOT hardcode compose files)

### Compose layering — `compose-stack.sh` is the single source of truth

The harness compose stack is defined in
`projects/analyzer-harness/compose-stack.sh`, which exports two functions:

- `compose_args_local()` — returns the local-dev compose file list:
  ```
  -f docker-compose.dev.yml
  -f docker-compose.base.yml          ← DEFINES ALL NAMED VOLUMES + SERVICES
  -f docker-compose.analyzer-test.yml
  [-f docker-compose.letsencrypt.yml]  ← only when --letsencrypt
  ```
- `compose_args_ci()` — returns the CI compose file list (not used here)

**CRITICAL: `docker-compose.base.yml` must ALWAYS be included.** It defines:

- All named volumes (`db-data`, `key_trust-store-volume`, `certs-vol`,
  `keys-vol`, `lucene_index-vol`)
- Core service definitions (`db.openelis.org`, `oe.openelis.org`,
  `fhir.openelis.org`, `frontend.openelis.org`, `proxy`, `certs`)

Without `base.yml`, compose fails with "undefined volume" errors.

### Service names vs container names

| Compose service name       | Container name             | Use service name for     |
| -------------------------- | -------------------------- | ------------------------ |
| `oe.openelis.org`          | `openelisglobal-webapp`    | `docker compose restart` |
| `db.openelis.org`          | `openelisglobal-database`  | compose commands         |
| `frontend.openelis.org`    | `openelisglobal-front-end` | compose commands         |
| `fhir.openelis.org`        | `external-fhir-api`        | compose commands         |
| `proxy`                    | `openelisglobal-proxy`     | compose commands         |
| `openelis-analyzer-bridge` | `openelis-analyzer-bridge` | compose commands         |
| `astm-simulator`           | `openelis-astm-simulator`  | compose commands         |

Use **service names** with `docker compose` commands. Use **container names**
with `docker inspect`, `docker logs`, `docker ps --filter`.

### Credential handling

- `verify-login.sh` defaults to `admin` / `adminADMIN!` when `TEST_USER` /
  `TEST_PASS` are not set. No `.env` file is required for local dev.
- `seed-analyzers.sh` requires `TEST_PASS` to be exported. If `.env` is not
  present, export manually: `export TEST_USER=admin TEST_PASS='adminADMIN!'`
- `reset-env.sh` sources `.env` from harness dir or repo root if present;
  proceeds without it if absent.

## Workflow

### 0) Preflight

```bash
REPO_ROOT=$(git rev-parse --show-toplevel)
HARNESS_DIR="$REPO_ROOT/projects/analyzer-harness"
```

Verify:

- `$HARNESS_DIR` exists
- Docker daemon is running (`docker info`)
- No port conflicts:
  `docker ps --format '{{.Names}} {{.Ports}}' | grep -E '15432|:443|:8443|:80[^0-9]'`
  - If found, warn and show the conflicting containers. Ask user whether to stop
    them.
- Submodules initialized:
  `git submodule status tools/analyzer-mock-server tools/openelis-analyzer-bridge plugins`
  - If any show leading `-`, run:
    `git submodule update --init tools/analyzer-mock-server tools/openelis-analyzer-bridge plugins`

### 1) Run reset-env.sh (the authoritative script)

Translate the user's flags to `reset-env.sh` arguments and run it:

```bash
cd "$REPO_ROOT/projects/analyzer-harness"

# Build the argument list
ARGS=()
# --full-reset is the default for /restart-analyzer-harness (unlike reset-env.sh)
[[ "$FULL_RESET" == true || "$USER_PASSED_NO_FLAGS" == true ]] && ARGS+=(--full-reset)
[[ "$DO_BUILD" == true ]] && ARGS+=(--build)
[[ "$SKIP_FIXTURES" == true ]] && ARGS+=(--skip-fixtures)
[[ "$USE_LETSENCRYPT" == true ]] && ARGS+=(--letsencrypt)
[[ "$SKIP_LETSENCRYPT" == true ]] && ARGS+=(--skip-letsencrypt)

bash reset-env.sh "${ARGS[@]}"
```

**If `reset-env.sh` fails at any step**, read the output, diagnose, and report.
Common failures:

- Docker build fails → check submodule init, WAR existence
- Liquibase migration fails → likely stale DB; suggest `--full-reset`
- Login timeout → check OE container logs (`docker logs openelisglobal-webapp`)
- Fixture load fails → check DB connectivity on port 15432
- Seed-analyzers fails → `TEST_PASS` not set; export manually and re-run

**If `.env` is missing and seed-analyzers needs credentials:**

```bash
export TEST_USER=admin TEST_PASS='adminADMIN!'
BASE_URL=https://localhost bash "$HARNESS_DIR/seed-analyzers.sh"
```

### 2) Post-startup verification

After `reset-env.sh` completes (or after manual recovery):

```bash
# Container status
docker ps --format "table {{.Names}}\t{{.Status}}" \
  --filter name=openelis --filter name=analyzer --filter name=fhir --filter name=serial

# Login check
"$HARNESS_DIR/scripts/verify-login.sh" --base-url https://localhost:8443

# Analyzer count (authenticated curl)
export TEST_USER="${TEST_USER:-admin}" TEST_PASS="${TEST_PASS:-adminADMIN!}"
curl -sk --data-urlencode "loginName=$TEST_USER" \
  --data-urlencode "password=$TEST_PASS" \
  "https://localhost:8443/api/OpenELIS-Global/ValidateLogin?apiCall=true" \
  -c /tmp/oe-cookies > /dev/null
curl -sk -b /tmp/oe-cookies \
  "https://localhost:8443/api/OpenELIS-Global/rest/analyzer/analyzers" | \
  python3 -c "import sys,json; d=json.load(sys.stdin); print(f'{len(d.get(\"analyzers\",d))} analyzers')"
```

### 3) Final report

```
======================================
  Analyzer Harness Ready
======================================

  URL: https://localhost:8443/ (direct) or https://localhost/ (proxy)
  Login: admin / adminADMIN! (or from .env)

  Database: localhost:15432
  Analyzers: 10 seeded via seed-analyzers.sh
  Plugins: [count] jars in volume/plugins/

  Container Status:
    [list all harness containers with status]
```

## Troubleshooting

If `reset-env.sh` fails, the most common issues are:

| Symptom                                   | Cause                                        | Fix                                                                                         |
| ----------------------------------------- | -------------------------------------------- | ------------------------------------------------------------------------------------------- |
| "undefined volume key_trust-store-volume" | Compose file list missing `base.yml`         | This means `compose-stack.sh` wasn't sourced. Check `$HARNESS_DIR/compose-stack.sh` exists. |
| "no such service: openelisglobal-webapp"  | Using container name instead of service name | Service name is `oe.openelis.org`, not the container name.                                  |
| Liquibase checksum mismatch               | Stale DB from a previous branch              | Use `--full-reset` to drop volumes.                                                         |
| Liquibase "constraint already exists"     | Same as above                                | Use `--full-reset`.                                                                         |
| Login 401                                 | Fixtures not loaded or account locked        | Re-run `seed-analyzers.sh` with credentials exported.                                       |
| "TEST_PASS not set"                       | No `.env` file                               | Export: `export TEST_USER=admin TEST_PASS='adminADMIN!'`                                    |
| Bridge can't reach OE                     | Docker network mismatch                      | Check `docker network ls` for `analyzer-harness_default`.                                   |
| Proxy 502                                 | nginx upstream misconfigured                 | Re-run `bootstrap.sh` to regenerate nginx.conf.                                             |

## Reference

- **Compose layering**: `projects/analyzer-harness/compose-stack.sh`
  (authoritative file list)
- **Reset script**: `projects/analyzer-harness/reset-env.sh` (full lifecycle)
- **Bootstrap**: `projects/analyzer-harness/bootstrap.sh` (idempotent volume
  setup)
- **Analyzer seeding**: `projects/analyzer-harness/seed-analyzers.sh` (10
  analyzers via REST API)
- **Login verifier**: `projects/analyzer-harness/scripts/verify-login.sh`
  (defaults to admin/adminADMIN!)
- **Build script**: `projects/analyzer-harness/build.sh` (WAR + harness images)
- **Plugin runtime dir**: `projects/analyzer-harness/volume/plugins/`
