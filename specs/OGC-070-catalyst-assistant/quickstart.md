# Quickstart: Catalyst - LLM-Powered Lab Data Assistant

**Feature**: OGC-070-catalyst-assistant  
**Date**: 2026-01-21  
**Estimated Time**: 45-90 minutes for MVP setup

## Overview

This guide walks you through setting up and running the Catalyst MVP - a
chat-to-SQL assistant for OpenELIS Global with A2A multi-agent architecture and
MCP-based schema retrieval. By the end, you'll be able to ask natural language
questions and get SQL results.

**Architecture**: Python A2A Agent Runtime (RouterAgent, SchemaAgent,
SQLGenAgent) → Python MCP Server (schema RAG) → Java Backend (OpenELIS
integration + SQL execution) → React Frontend (Carbon chat UI)

## M0.0 Foundation POC (current milestone)

M0.0 validates the **core A2A loop** only: Gateway → RouterAgent → CatalystAgent
→ MCP `get_schema` → LLM stub. There is **no OpenELIS backend** or UI
integration yet.

```bash
# 1. Copy env template
cp projects/catalyst/env.recommended projects/catalyst/.env

# 2. Install Python deps (per component)
cd projects/catalyst/catalyst-gateway && poetry install && cd ../..
cd projects/catalyst/catalyst-agents && poetry install && cd ../..
cd projects/catalyst/catalyst-mcp && poetry install && cd ../..

# 3. Start services (local dev)
cd projects/catalyst
cd catalyst-gateway && poetry run honcho -f ../Procfile.dev start

# 4. Run smoke tests
./tests/run_tests.sh all
```

## Prerequisites

- [ ] OpenELIS development environment running
      (`docker compose -f dev.docker-compose.yml up -d`)
- [ ] Java 21 installed (`java -version` shows 21.x.x)
- [ ] Python 3.11+ installed (`python3 --version`)
- [ ] Node.js 16+ installed
- [ ] Docker with compose v2
- [ ] Either:
  - **Cloud API Key**: Google Gemini API key for fast iteration, OR
  - **Local Setup**: LM Studio for privacy-preserving deployment

## Quick Start Options

### Option A: Cloud API with Gemini (Fastest - No GPU Required)

```bash
# 1. Start A2A agents + MCP server + OpenELIS
docker compose -f projects/catalyst/catalyst-dev.docker-compose.yml up -d

# 2. Configure LLM provider
# Create projects/catalyst/catalyst.env file:
cat > projects/catalyst/catalyst.env <<EOF
GOOGLE_API_KEY=your-google-api-key
CATALYST_LLM_PROVIDER=gemini  # or lmstudio
EOF

# Edit projects/catalyst/catalyst-agents/src/config/agents_config.yaml:
#   llm:
#     provider: ${CATALYST_LLM_PROVIDER}
#     gemini:
#       api_key: ${GOOGLE_API_KEY}

# Docker Compose will load catalyst.env automatically

# 3. Build backend with Catalyst
mvn clean install -DskipTests -Dmaven.test.skip=true

# 4. Restart OpenELIS container
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org

# 5. Test the endpoint (Stage A: generate SQL, review before execution)
curl -k -X POST https://localhost/rest/catalyst/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_SESSION_TOKEN" \
  -d '{"query": "How many samples are in the database?", "execute": false}'
# Response includes queryId, confirmationToken, and generated SQL for review

# 6. Execute with confirmation (Stage B)
# Note: execute defaults to false (safer default). Setting execute: true with
# queryId + confirmationToken validates token and executes the query.
curl -k -X POST https://localhost/rest/catalyst/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_SESSION_TOKEN" \
  -d '{"queryId": "...", "confirmationToken": "...", "execute": true}'
```

### Option B: Local LLM with LM Studio (Privacy-First)

```bash
# 1. Start A2A agents + MCP server
docker compose -f projects/catalyst/catalyst-dev.docker-compose.yml up -d

# 2. Start LM Studio on host machine (download from https://lmstudio.ai/)
# Load a model (use most recent available OpenAI-compatible model)

# 3. Verify LM Studio API is accessible
curl http://localhost:1234/v1/models

# 4. Configure agent runtime for local LLM
# Edit projects/catalyst/catalyst-agents/src/config/agents_config.yaml:
#   llm:
#     provider: lmstudio
#     lmstudio:
#       base_url: http://host.docker.internal:1234/v1
#       model: local-model  # Match the model name in LM Studio

# 5. Restart agent runtime to load config
docker compose -f projects/catalyst/catalyst-dev.docker-compose.yml restart catalyst-agents

# 6. Build and start OpenELIS
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org

# 7. Test (generate SQL first, then execute with confirmation)
curl -k -X POST https://localhost/rest/catalyst/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_SESSION_TOKEN" \
  -d '{"query": "How many samples are in the database?", "execute": false}'
```

### Option C: LM Studio (Local with UI)

```bash
# 1. Download and start LM Studio from https://lmstudio.ai/
# 2. Load a model (use most recent available OpenAI-compatible model)
# 3. Start the local server (default: http://localhost:1234)

# 4. Start A2A agents + MCP server
docker compose -f projects/catalyst/catalyst-dev.docker-compose.yml up -d catalyst-agents catalyst-mcp

# 5. Configure agent runtime for LM Studio
# Edit projects/catalyst/catalyst-agents/src/config/agents_config.yaml:
#   llm:
#     provider: lmstudio
#     lmstudio:
#       base_url: http://host.docker.internal:1234/v1
#       model: local-model

# 6. Restart agent runtime to load config
docker compose -f projects/catalyst/catalyst-dev.docker-compose.yml restart catalyst-agents

# 7. Build and start OpenELIS
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

---

## Step-by-Step Setup

### Step 1: Start the A2A Agent Runtime and MCP Schema Server

The agent runtime orchestrates query flow; the MCP server provides RAG-based
schema retrieval:

```bash
# Start agents + MCP server via Docker (recommended)
docker compose -f projects/catalyst/catalyst-dev.docker-compose.yml up -d catalyst-agents catalyst-mcp

# OR for local development:
# Agent runtime:
cd projects/catalyst/catalyst-agents
python -m pip install -e .
python -m src.main

# MCP server (separate terminal):
cd projects/catalyst/catalyst-mcp
python -m pip install -e .
python -m src.server

# Verify services are running
curl http://localhost:8000/.well-known/agent.json  # RouterAgent discovery
curl http://localhost:8001/health  # MCP server health (if implemented)
```

### Step 2: Initialize Schema Embeddings

Generate embeddings for RAG-based schema retrieval:

```bash
# Connect to MCP server and initialize embeddings
docker exec catalyst-mcp python -m src.rag.init_embeddings

# This will:
# 1. Extract schema from OpenELIS PostgreSQL
# 2. Generate embeddings for each table
# 3. Store in ChromaDB for similarity search
```

### Step 3: Configure LLM Provider (Agent Runtime)

Edit `projects/catalyst/catalyst-agents/src/config/agents_config.yaml`:

```yaml
# LLM Provider Selection (gemini, lmstudio)
llm:
  provider: lmstudio # Options: gemini, lmstudio

  # Cloud: Google Gemini
  gemini:
    model: latest # Use most recent available Gemini model
    api_key: ${GOOGLE_API_KEY} # Set environment variable

  # Local: LM Studio (OpenAI-compatible)
  lmstudio:
    base_url: http://host.docker.internal:1234/v1
    model: local-model # Use most recent available OpenAI-compatible model

# MCP Server (SchemaAgent uses this)
mcp:
  server_url: http://catalyst-mcp:8000/mcp
```

### Step 3b: Configure Java Backend

Edit `volume/properties/catalyst.properties`:

```properties
# Catalyst Gateway (OpenAI-compatible entrypoint)
catalyst.gateway.url=http://catalyst-gateway:8000
catalyst.gateway.api-key=not-required-for-mvp  # Gateway may require auth in future

# Guardrails (enforced in Java backend)
catalyst.guardrails.max-rows=10000
catalyst.guardrails.query-timeout=30s
catalyst.guardrails.blocked-tables=sys_user,login_user,user_role
```

### Step 4: Build and Deploy

```bash
# Format code (required before commit)
mvn spotless:apply

# Build backend
mvn clean install -DskipTests -Dmaven.test.skip=true

# Restart OpenELIS container
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org

# Check logs for startup
docker logs -f oe.openelis.org 2>&1 | grep -i catalyst
```

### Step 5: Verify Installation

```bash
# Test query generation (Stage A: no execution)
curl -k -X POST https://localhost/rest/catalyst/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_SESSION_TOKEN" \
  -d '{
    "query": "How many samples were entered today?",
    "execute": false
  }'

# Expected response (Stage A: SQL generated and validated):
# {
#   "queryId": "550e8400-e29b-41d4-a716-446655440000",
#   "status": "VALIDATED",  # Changed from GENERATED
#   "userQuery": "How many samples were entered today?",
#   "generatedSql": "SELECT COUNT(*) FROM sample WHERE entered_date = CURRENT_DATE",
#   "estimatedRows": 150,
#   "confirmationToken": "abc123def456...",
#   "results": null
# }

# Stage B: Execute with confirmation
curl -k -X POST https://localhost/rest/catalyst/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_SESSION_TOKEN" \
  -d '{
    "queryId": "550e8400-e29b-41d4-a716-446655440000",
    "confirmationToken": "abc123def456...",
    "execute": true
  }'

# Expected response (Stage B: accepted and executed):
# {
#   "queryId": "550e8400-e29b-41d4-a716-446655440000",
#   "status": "EXECUTED",  # VALIDATED → ACCEPTED → EXECUTED
#   "userQuery": "How many samples were entered today?",
#   "generatedSql": "SELECT COUNT(*) FROM sample WHERE entered_date = CURRENT_DATE",
#   "estimatedRows": 150,
#   "actualRows": 147,
#   "executionTimeMs": 45,
#   "results": { "columns": ["count"], "rows": [[147]] }
# }
```

### Step 6: Run Tests

```bash
# A2A Agent tests (pytest)
cd projects/catalyst/catalyst-agents && pytest

# MCP Server tests (pytest)
cd projects/catalyst/catalyst-mcp && pytest

# Backend tests (JUnit)
mvn test -Dtest=*Catalyst*

# Frontend tests (Jest)
cd frontend && npm test -- --testPathPattern=catalyst

# E2E test (Cypress - individual file per Constitution V.5)
cd frontend && npm run cy:run -- --spec "cypress/e2e/catalyst.cy.js"
```

---

## Development Workflow

### MCP Server Changes (Python)

```bash
cd projects/catalyst/catalyst-mcp

# 1. Make changes in src/

# 2. Run tests
pytest

# 3. Restart server
docker compose -f ../catalyst-dev.docker-compose.yml restart catalyst-mcp
```

### Backend Changes (Java)

```bash
# 1. Make code changes in src/main/java/org/openelisglobal/catalyst/

# 2. Format code
mvn spotless:apply

# 3. Run unit tests
mvn test -Dtest=CatalystQueryServiceTest

# 4. Build and redeploy
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

### Frontend Changes (React)

```bash
cd frontend

# 1. Make changes in src/components/catalyst/

# 2. Format code
npm run format

# 3. Run Jest tests
npm test -- --testPathPattern=catalyst

# 4. Changes auto-reload via Webpack HMR
```

---

## Troubleshooting

### Agent Runtime Issues

```bash
# Check agent runtime is running
curl http://localhost:8000/.well-known/agent.json  # RouterAgent discovery

# Check agent runtime logs
docker logs catalyst-agents

# Verify agent runtime can call MCP server
docker logs catalyst-agents | grep -i "mcp\|schema"
```

### MCP Server Issues

```bash
# Check MCP server is running (if health endpoint exists)
curl http://localhost:8001/health

# Check MCP server logs
docker logs catalyst-mcp

# Test MCP tool directly (via MCP protocol, not HTTP)
# Use MCP client tools or SchemaAgent to test
```

### LLM Connection Failed

```bash
# Check agent runtime logs for LLM errors
docker logs catalyst-agents | grep -i "llm\|provider\|error"

# For Gemini (check API key and agent config)
curl "https://generativelanguage.googleapis.com/v1/models?key=$GOOGLE_API_KEY"
# Verify agents_config.yaml has correct provider and api_key

# For LM Studio
curl http://localhost:1234/v1/models
# Verify agents_config.yaml has correct base_url
```

### SQL Generation Errors

```bash
# Check audit log for errors
docker exec oe-postgres psql -U clinlims -d clinlims \
  -c "SELECT user_query, execution_status, error_message FROM catalyst_query ORDER BY lastupdated DESC LIMIT 5"
```

### Blocked Table Access

```bash
# Check blocked tables configuration
grep blocked-tables volume/properties/catalyst.properties

# View blocked attempt in logs
docker logs oe.openelis.org 2>&1 | grep -i "blocked table"
```

---

## Docker Compose Services

The `projects/catalyst/catalyst-dev.docker-compose.yml` includes:

```yaml
services:
  catalyst-agents:
    build: ./projects/catalyst/catalyst-agents
    ports:
      - "8000:8000" # A2A agent runtime
    environment:
      - DATABASE_URL=postgresql://clinlims:clinlims@oe-postgres:5432/clinlims
    depends_on:
      - catalyst-mcp
      # Note: LM Studio runs on host machine, not in Docker
    volumes:
      - ./projects/catalyst/catalyst-agents/src/config:/app/config

  catalyst-mcp:
    build: ./projects/catalyst/catalyst-mcp
    ports:
      - "8001:8000" # MCP server (different port to avoid conflict)
    environment:
      - DATABASE_URL=postgresql://clinlims:clinlims@oe-postgres:5432/clinlims
    depends_on:
      - oe-postgres

  # Note: LM Studio runs on host machine, not in Docker
  # No Docker service needed for LM Studio
```

---

## Example Queries

| Natural Language                                        | Generated SQL                                                                                                                               |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| "How many samples were entered today?"                  | `SELECT COUNT(*) FROM sample WHERE entered_date = CURRENT_DATE`                                                                             |
| "Show HIV test results from last week"                  | `SELECT * FROM analysis a JOIN test t ON a.test_id = t.id WHERE t.name LIKE '%HIV%' AND a.started_date >= CURRENT_DATE - INTERVAL '7 days'` |
| "What's the average turnaround time for malaria tests?" | `SELECT AVG(completed_date - started_date) FROM analysis a JOIN test t ON a.test_id = t.id WHERE t.name LIKE '%malaria%'`                   |
| "Count samples by type this month"                      | `SELECT sample_type, COUNT(*) FROM sample WHERE entered_date >= DATE_TRUNC('month', CURRENT_DATE) GROUP BY sample_type`                     |

---

## Next Steps

After MVP validation:

1. **Phase 2**: Advanced multi-agent orchestration, external agent federation
2. **Phase 3**: Report storage, scheduling, dashboard widgets

## Resources

- **Spec**: [specs/OGC-070-catalyst-assistant/spec.md](./spec.md)
- **Plan**: [specs/OGC-070-catalyst-assistant/plan.md](./plan.md)
- **API Contract**:
  [specs/OGC-070-catalyst-assistant/contracts/catalyst-api.yaml](./contracts/catalyst-api.yaml)
- **Jira**: [OGC-70](https://uwdigi.atlassian.net/browse/OGC-70)
- **MCP Documentation**:
  [modelcontextprotocol.io](https://modelcontextprotocol.io/)
- **LangChain4j Docs**: [docs.langchain4j.dev](https://docs.langchain4j.dev/)
- **Carbon AI Chat**:
  [chat.carbondesignsystem.com](https://chat.carbondesignsystem.com/)
