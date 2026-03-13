# Playwright E2E Tests

## Running tests

Use the `package.json` scripts from the `frontend` directory:

```bash
cd frontend

# Full suite (requires app running)
npm run pw:test

# Barcode tests only
npm run pw:test:barcode

# Interactive UI
npm run pw:test:ui
```

## Prerequisites

1. **App must be running** at `https://localhost` (or set `BASE_URL`).
2. **Auth env vars**: `TEST_USER` and `TEST_PASS` (e.g. `admin` / `adminADMIN!`).

## Local validation

### 1. Resolve Docker network conflicts

If you use **analyzer-harness**, stop it first. It uses subnet `172.20.1.0/24`, which conflicts with `dev.docker-compose.yml`:

```bash
# Stop analyzer-harness containers (if running)
docker stop analyzer-harness-oe-1 analyzer-harness-proxy-1 analyzer-harness-frontend-1 \
  analyzer-harness-db-1 analyzer-harness-fhir-1 analyzer-harness-certs-1 \
  analyzer-harness-astm-simulator-1 analyzer-harness-openelis-analyzer-bridge-1 \
  analyzer-harness-virtual-serial-1 2>/dev/null || true
```

### 2. Start the dev stack

```bash
docker compose -f dev.docker-compose.yml up -d
```

Wait 2–3 minutes for the frontend (React dev server) and backend to be ready.

### 3. Run Playwright barcode tests

```bash
cd frontend && TEST_USER=admin TEST_PASS='adminADMIN!' npm run pw:test:barcode
```

### 4. Verify app is reachable (optional)

```bash
# From another terminal – should return HTML
curl -sk https://localhost/login | head -5
```

CI runs the full suite after starting the app via `build.docker-compose.yml`.
