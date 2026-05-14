# Restart Dev Environment

When the user invokes `/restart-dev-env` (optionally with arguments), perform a
development environment restart workflow with:

- A **WAR file rebuild** (unless skipped)
- **Container restart** with force-recreate for the webapp
- **Optional database reset** (drop volumes with `--full-reset`)
- **E2E test fixture loading** (by default)
- **Let's Encrypt setup** (only for non-localhost domains)

This command is **action-oriented**: it builds and restarts by default. Use
flags to skip steps when iterating quickly. The command reports progress at each
checkpoint so you can monitor the restart process.

## User Input

```text
$ARGUMENTS
```

Interpret arguments best-effort. Support these patterns:

- `/restart-dev-env` → Full restart (build WAR, restart containers, load
  fixtures)
- `/restart-dev-env --full-reset` → Drop database volumes before restart (clean
  slate)
- `/restart-dev-env --skip-build` → Skip WAR rebuild (use existing
  target/OpenELIS-Global.war)
- `/restart-dev-env --skip-fixtures` → Skip loading E2E test fixtures
- Combine flags as needed: `/restart-dev-env --full-reset --skip-build`

If any inputs are unclear, ask a single concise question and continue with safe
defaults.

## Safety Rules (non-negotiable)

- **Warn** if there are uncommitted changes (but proceed unless user stops).
- **Never** drop database volumes unless `--full-reset` is explicitly passed.
- **Always** wait for webapp readiness before loading fixtures.
- **Report** container status after restart (even if some containers fail).
- If Let's Encrypt generation fails, **warn but continue** (use self-signed
  certs).

## Workflow

### 0) Preflight (gather facts, no changes yet)

Run these and summarize the results:

- `git rev-parse --show-toplevel` (verify project root)
- **Load .env if present** (so dev servers with a domain use it):
  ```bash
  set -a; [ -f .env ] && . ./.env; set +a
  ```
  Then `LETSENCRYPT_DOMAIN` and `LETSENCRYPT_EMAIL` from .env are available for
  the rest of the workflow. **If .env is missing**, both vars are unset; DOMAIN
  then uses the default below (same as `scripts/reset-dev-env.sh`).
- `git status --porcelain` (warn if uncommitted changes)
- Check `LETSENCRYPT_DOMAIN` env var (default: `analyzers.openelis-global.org`
  when unset, so hosted dev envs get the right domain even without .env)
- Check `LETSENCRYPT_EMAIL` env var (required for Let's Encrypt cert generation)

Determine:

- **DOMAIN**: From `LETSENCRYPT_DOMAIN` (after loading .env) or default
  `analyzers.openelis-global.org` when unset (use `localhost` only if .env
  explicitly sets `LETSENCRYPT_DOMAIN=localhost`)
- **NEEDS_LETSENCRYPT**: true if domain is not `localhost` AND
  `LETSENCRYPT_EMAIL` is set
- **FULL_RESET**: true if `--full-reset` flag present
- **SKIP_BUILD**: true if `--skip-build` flag present
- **SKIP_FIXTURES**: true if `--skip-fixtures` flag present

Report the detected configuration before proceeding.

### 1) Build WAR file (checkpoint #1)

**Skip if `--skip-build` was passed.**

Run:

```bash
mvn clean install -DskipTests -Dmaven.test.skip=true
```

This follows AGENTS.md guidance: always use BOTH flags to properly skip tests.

After build completes:

- Verify `target/OpenELIS-Global.war` exists
- Report build success or failure

**If build fails**: Stop and report the error. Do not proceed.

### 2) Stop containers (checkpoint #2)

Choose command based on `--full-reset` flag:

- **With `--full-reset`**:

  ```bash
  docker compose -f dev.docker-compose.yml down -v
  ```

  This removes ALL volumes including database data.

- **Without `--full-reset`** (default):
  ```bash
  docker compose -f dev.docker-compose.yml down
  ```
  This preserves database data.

Report which mode was used.

### 3) Setup Let's Encrypt (checkpoint #3)

**Skip if DOMAIN is `localhost`.**

Only if `LETSENCRYPT_DOMAIN` is set and not `localhost`:

1. Check if certificates exist:

   ```bash
   CERT_PATH="./volume/letsencrypt/live/${DOMAIN}/fullchain.pem"
   ```

2. If certificates exist: Report "Using existing Let's Encrypt certificates"

3. If certificates don't exist AND `LETSENCRYPT_EMAIL` is set:

   - Start proxy first (required for ACME challenge):
     ```bash
     docker compose -f dev.docker-compose.yml up -d proxy
     ```
   - Wait 5 seconds for proxy to be ready
   - Run certificate generation:
     ```bash
     ./scripts/generate-letsencrypt-certs.sh
     ```
   - If generation fails: Warn but continue (will use self-signed)

4. If certificates don't exist AND `LETSENCRYPT_EMAIL` is not set:
   - Warn: "Let's Encrypt requires LETSENCRYPT_EMAIL to be set"
   - Continue with self-signed certificates

### 4) Start containers (checkpoint #4)

Choose compose command based on Let's Encrypt status:

- **With Let's Encrypt certificates**:

  ```bash
  docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml up -d
  ```

- **Without Let's Encrypt** (localhost or no certs):
  ```bash
  docker compose -f dev.docker-compose.yml up -d
  ```

Then force-recreate the webapp container to pick up the new WAR:

```bash
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

Report container startup status.

### 5) Wait for webapp readiness (checkpoint #5)

Poll the webapp health endpoint with incremental progress:

```bash
curl -sk https://localhost/api/OpenELIS-Global/LoginPage
```

- Wait up to 120 seconds
- Check every 5 seconds
- Report progress: "Waiting for webapp... (10s)", "Waiting for webapp... (20s)",
  etc.

**If webapp ready**: Report success with elapsed time.

**If timeout (120s)**: Report failure and show:

```bash
docker logs openelisglobal-webapp --tail 50
```

### 6) Load E2E test fixtures (checkpoint #6)

**Skip if `--skip-fixtures` was passed.**

Run:

```bash
./src/test/resources/load-test-fixtures.sh --no-verify
```

This script:

- Automatically detects Docker container
- Loads foundational data (providers, organizations)
- Loads storage fixtures + E2E test data

**If fixture loading fails**: Warn but don't exit (non-fatal).

### 7) Post-restart report

Produce a concise report including:

**Environment Status:**

- Domain: {DOMAIN}
- Full reset performed: yes/no
- WAR build: skipped/success
- Let's Encrypt: active/self-signed/skipped
- Fixtures loaded: yes/skipped/failed

**Container Status:**

```bash
docker compose -f dev.docker-compose.yml ps --format "table {{.Name}}\t{{.Status}}"
```

**Access Points:**

| Instance  | URL                                    | Credentials         |
| --------- | -------------------------------------- | ------------------- |
| React UI  | https://localhost/                     | admin / adminADMIN! |
| Legacy UI | https://localhost/api/OpenELIS-Global/ | admin / adminADMIN! |

**Next Steps:**

- If any containers failed: Show relevant logs
- If fixtures failed: Suggest manual reload command
- If Let's Encrypt failed: Suggest checking DNS and email config

---

## Troubleshooting: Why wasn't Let's Encrypt started?

Let's Encrypt runs only when **DOMAIN** is not `localhost` and
**LETSENCRYPT_EMAIL** is set. Common causes it was skipped:

1. **No `.env` file**  
   `.env` is not committed (see `.gitignore`). If it's missing, the workflow
   loads no vars; DOMAIN defaults to `analyzers.openelis-global.org` (so the
   domain is correct for hosted dev), but **LETSENCRYPT_EMAIL** stays unset, so
   cert generation is skipped.  
   **Fix:** Create `.env` from `.env.example`, set
   `LETSENCRYPT_DOMAIN=analyzers.openelis-global.org` and
   `LETSENCRYPT_EMAIL=your-email@example.com`, then run `/restart-dev-env`
   again.

2. **DOMAIN is `localhost`**  
   If `.env` explicitly sets `LETSENCRYPT_DOMAIN=localhost`, the workflow treats
   this as local dev and skips Let's Encrypt.  
   **Fix:** Set `LETSENCRYPT_DOMAIN=analyzers.openelis-global.org` (or your real
   domain) in `.env` for hosted/analyzer dev.

3. **LETSENCRYPT_EMAIL unset**  
   Cert generation requires an email for Let's Encrypt notifications.  
   **Fix:** Set `LETSENCRYPT_EMAIL` in `.env` and re-run the restart.
