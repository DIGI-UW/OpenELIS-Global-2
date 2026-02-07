# Restart Analyzer Harness

When the user invokes `/restart-analyzer-harness` (optionally with arguments),
perform an analyzer harness environment restart workflow with:

- **Container restart** with force-recreate for harness services
- **Optional database reset** (drop volumes with `--full-reset`)
- **Analyzer fixture loading** (from canonical `analyzer-e2e.generated.sql`)
- **Analyzer infrastructure verification** (ASTM bridge, simulator, virtual
  serial)

This command is for **analyzer manual testing and E2E validation**. It uses the
harness stack (`projects/analyzer-harness/`) with full analyzer test
infrastructure, NOT the root dev stack.

## User Input

```text
$ARGUMENTS
```

Interpret arguments best-effort. Support these patterns:

- `/restart-analyzer-harness` → Full restart (restart containers, load fixtures)
- `/restart-analyzer-harness --full-reset` → Drop database volumes before
  restart (clean slate)
- `/restart-analyzer-harness --skip-fixtures` → Skip loading test fixtures
- `/restart-analyzer-harness --build` → Build WAR before restarting (for code
  changes)
- Combine flags as needed: `/restart-analyzer-harness --full-reset --build`

## Safety Rules (non-negotiable)

- **Warn** if root dev stack is running (suggest stopping it first to avoid port
  conflicts).
- **Never** drop database volumes unless `--full-reset` is explicitly passed.
- **Always** wait for webapp readiness before loading fixtures.
- **Report** container status after restart (even if some containers fail).
- If Let's Encrypt certs are missing, **warn but continue** (use self-signed
  certs).

## Workflow

### 0) Preflight (gather facts, no changes yet)

Run these and summarize the results:

- `git rev-parse --show-toplevel` (verify project root)
- **Detect harness directory**: `projects/analyzer-harness/` (must exist)
- **Check if root stack running**:
  `docker ps --filter name=openelisglobal- --format {{.Names}}`
  - If root stack is running, **warn** that port conflicts may occur (root uses
    80/443/15432, harness uses same ports)
- **Load .env if present** (harness uses repo root .env for LETSENCRYPT_DOMAIN):
  ```bash
  set -a; [ -f .env ] && . ./.env; set +a
  ```
- `git status --porcelain` (warn if uncommitted changes)
- Check `LETSENCRYPT_DOMAIN` env var (harness default:
  `analyzers.openelis-global.org`)

Determine:

- **DOMAIN**: From `LETSENCRYPT_DOMAIN` (after loading .env) or default
  `analyzers.openelis-global.org`
- **FULL_RESET**: true if `--full-reset` flag present
- **SKIP_FIXTURES**: true if `--skip-fixtures` flag present
- **DO_BUILD**: true if `--build` flag present

Report the detected configuration before proceeding.

### 1) Build WAR file (checkpoint #1) - OPTIONAL

**Run only if `--build` was passed.**

This allows testing code changes without rebuilding images. The harness mounts
`../../target/OpenELIS-Global.war` into the oe service.

Run:

```bash
cd /home/ubuntu/OpenELIS-Global-2
mvn clean install -DskipTests -Dmaven.test.skip=true
```

After build completes:

- Verify `target/OpenELIS-Global.war` exists
- Report build success or failure

**If build fails**: Stop and report the error. Do not proceed.

### 2) Stop containers (checkpoint #2)

Choose command based on `--full-reset` flag:

- **With `--full-reset`**:

  ```bash
  cd /home/ubuntu/OpenELIS-Global-2/projects/analyzer-harness
  docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml down -v
  ```

  This removes database and other volumes (clean slate).

- **Without `--full-reset`**:
  ```bash
  cd /home/ubuntu/OpenELIS-Global-2/projects/analyzer-harness
  docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml down
  ```
  This preserves database and volumes.

Report: "Stopped harness stack (volumes: [preserved|removed])"

### 3) Ensure volume directories exist (checkpoint #3)

```bash
mkdir -p /home/ubuntu/OpenELIS-Global-2/volume/letsencrypt
mkdir -p /home/ubuntu/OpenELIS-Global-2/volume/nginx/certbot
```

This ensures bind mounts work when starting the stack.

### 4) Start containers (checkpoint #4)

```bash
cd /home/ubuntu/OpenELIS-Global-2/projects/analyzer-harness
docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml up -d
```

This starts:

- db (PostgreSQL on 15432)
- oe (OpenELIS webapp with mounted WAR)
- fhir (HAPI FHIR server)
- frontend (React dev server with hot reload)
- proxy (nginx with Let's Encrypt support)
- astm-http-bridge (ASTM→HTTP bridge on 12001)
- astm-simulator (Mock analyzer on 5000)
- virtual-serial (Virtual serial ports /dev/serial/ttyVUSB0-4)

Report: "Started harness stack (8 services)"

### 5) Wait for webapp (checkpoint #5)

Poll `https://localhost/` with curl until it responds (max 120 seconds):

```bash
MAX_WAIT=120
ELAPSED=0
WAIT_INTERVAL=5

while [ $ELAPSED -lt $MAX_WAIT ]; do
    if curl -sk https://localhost/ 2>/dev/null | grep -q "OpenELIS\|Login"; then
        echo "Webapp ready (${ELAPSED}s)"
        break
    fi
    sleep $WAIT_INTERVAL
    ELAPSED=$((ELAPSED + WAIT_INTERVAL))
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    echo "ERROR: Webapp not ready after ${MAX_WAIT}s"
    exit 1
fi
```

Report: "Webapp ready at https://localhost/"

### 6) Load fixtures (checkpoint #6)

**Skip if `--skip-fixtures` was passed.**

Load test fixtures via `load-test-fixtures.sh` with harness DB port:

```bash
cd /home/ubuntu/OpenELIS-Global-2
export DB_PORT=15432
export DB_HOST=localhost

if [ "$FULL_RESET" = true ]; then
    ./src/test/resources/load-test-fixtures.sh --no-verify
else
    ./src/test/resources/load-test-fixtures.sh --reset --no-verify
fi
```

This loads:

- Foundational data (e2e-foundational-data.sql)
- Storage fixtures (storage-e2e.generated.sql from DBUnit)
- **Analyzer fixtures** (analyzer-e2e.generated.sql - 12 analyzers 2000-2012)

Report: "Loaded fixtures (foundational + storage + analyzers)"

### 7) Verify analyzer infrastructure (checkpoint #7)

```bash
docker ps --format "table {{.Names}}\t{{.Status}}" | grep -E "astm-http-bridge|astm-simulator|virtual-serial"
```

Expected containers:

- `analyzer-harness-astm-http-bridge-1` → Up
- `analyzer-harness-astm-simulator-1` → Up (healthy)
- `analyzer-harness-virtual-serial-1` → Up

Report each container's status. If any are not running, warn but continue.

### 8) Final report (checkpoint #8)

Print summary:

```
======================================
  Analyzer Harness Ready
======================================

  Domain: https://[DOMAIN]/
  Login: admin / adminADMIN!

  Database: localhost:15432
  Analyzers: 12 loaded (IDs 2000-2012)
  Defaults: 11 templates at /data/analyzer-defaults

  Analyzer Infrastructure:
    - ASTM Bridge: astm-http-bridge:12001
    - ASTM Simulator: 172.20.1.100:5000 (healthy)
    - Serial Ports: /dev/serial/ttyVUSB0-4

  Container Status:
    [list all harness containers with status]

  Let's Encrypt: [CERT_STATUS]
    [If domain is not localhost and certs missing:]
    Generate certs: LETSENCRYPT_DOMAIN=[DOMAIN] ./scripts/generate-letsencrypt-certs.sh
```

Where:

- `[DOMAIN]` is `analyzers.openelis-global.org` or value from .env
- `[CERT_STATUS]` is "Valid cert found" or "Using self-signed (generate cert if
  needed)"

## Important Notes

- **Harness uses port 15432** (not 15432 like root dev - actually both use
  15432, so stop root first to avoid conflict)
- **Frontend hot-reloads**: Changes to `frontend/src/` are picked up
  automatically (mounted into container)
- **Backend requires rebuild**: Changes to Java code require `--build` flag
- **Root stack conflict**: If root dev stack is running on 80/443/15432, harness
  will fail. Stop root first.
- **Let's Encrypt certs**: Shared with root stack via `volume/letsencrypt/`
  (generate once, use everywhere)

## Example Executions

```bash
# Quick restart (preserve DB, skip build)
/restart-analyzer-harness

# Full reset (drop DB, rebuild)
/restart-analyzer-harness --full-reset --build

# Code iteration (rebuild WAR, preserve DB)
/restart-analyzer-harness --build

# Fast iteration (no build, no fixtures)
/restart-analyzer-harness --skip-build --skip-fixtures
```

## Reference

- Harness compose files:
  `projects/analyzer-harness/docker-compose.{dev,analyzer-test,letsencrypt}.yml`
- Fixture loader: `src/test/resources/load-test-fixtures.sh`
- Analyzer fixtures: `src/test/resources/testdata/analyzer-e2e.generated.sql`
  (canonical)
- Build script: `projects/analyzer-harness/build.sh` (WAR + harness images)
- Reset script: `projects/analyzer-harness/reset-env.sh` (implements this
  workflow)
