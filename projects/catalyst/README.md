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
cd projects/catalyst/catalyst-gateway && uv sync && cd ../..
cd projects/catalyst/catalyst-agents && uv sync && cd ../..
cd projects/catalyst/catalyst-mcp && uv sync && cd ../..

# 3. Start services
cd projects/catalyst
cd catalyst-gateway && uv run honcho -f ../Procfile.dev start
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

### MCP Database Access Architecture

**Decision**: MCP server provides **allowlisted schema context** to support LLM
query generation and validation. MCP does **not** execute user queries.

**Architecture**:

- **MCP Layer (Python)**:

  - Direct read-only database access for schema introspection (M1+)
  - Provides `get_query_context()` and `validate_sql()` tools only
  - Enforces table allowlist boundary (what LLM can reference)
  - M0.0: Mock responses (no DB connection)
  - M1+: Direct PostgreSQL connection via `psycopg2-binary` for real schema
    introspection

- **OE Backend (Java)**:
  - Executes user-accepted queries (after user reviews generated SQL in chat)
  - Enforces RBAC, audit trail, transaction management
  - Returns query results to frontend

**Privacy Boundary**:

- **Table allowlist**: `MCP_ALLOWED_TABLES` environment variable
  (comma-separated)
- **Default profile**: Minimal non-PHI tables (terminology, test catalog,
  statuses)
- **Enforcement**: Both `get_query_context()` and `validate_sql()` respect
  allowlist
- LLM cannot generate SQL referencing non-allowed tables (blocked at validation)

**Security**:

- Read-only database user for MCP (no INSERT/UPDATE/DELETE permissions)
- Network isolation (MCP server in same Docker network as DB)
- Connection string via environment variables (not hardcoded)
- Defense in depth: MCP validates SQL structure; OE backend enforces RBAC at
  execution time
