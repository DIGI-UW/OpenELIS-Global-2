## Catalyst (OGC-70)

This folder contains **Catalyst-specific tooling and supporting services** that
live alongside OpenELIS Global.

### Intended contents

- `projects/catalyst/catalyst-mcp/`: Python MCP server (schema RAG / retrieval)
- `projects/catalyst/catalyst-dev.docker-compose.yml`: Docker Compose for
  Catalyst dev services
- `projects/catalyst/scripts/`: helper scripts (optional)

OpenELIS integration points remain in:

- Backend: `src/main/java/org/openelisglobal/catalyst/`
- Frontend: `frontend/src/components/catalyst/`
- Config: `volume/properties/catalyst.properties`

### Version managers (project setup)

This project follows the same setup patterns used in similar multi-service agent
repos (e.g., med-agent-hub and omrs-ai-playground): keep tool versions explicit
and local to the repo.

- Java tooling: use the repo root `.sdkmanrc` and run `sdk env` (Java 21).
- Node tooling: use `frontend/.nvmrc` and run `nvm use` if you work on the
  frontend milestone.
- Python tooling: use `projects/catalyst/.python-version` with pyenv/asdf
  (Python 3.11+).

**Note**: This folder is created to keep Catalyst work scoped to a small surface
area.

### Local dev (M0.0)

```bash
# 1. Copy env template
cp projects/catalyst/env.recommended projects/catalyst/.env

# 2. Install Python deps (per component)
cd projects/catalyst/catalyst-gateway && poetry install && cd ../..
cd projects/catalyst/catalyst-agents && poetry install && cd ../..
cd projects/catalyst/catalyst-mcp && poetry install && cd ../..

# 3. Start services
cd projects/catalyst
cd catalyst-gateway && poetry run honcho -f ../Procfile.dev start
```

### Smoke tests (M0.0)

```bash
cd projects/catalyst
./tests/run_tests.sh all
```

### Docker compose (M0.0)

```bash
cd projects/catalyst
docker compose -f catalyst-dev.docker-compose.yml up -d
```
