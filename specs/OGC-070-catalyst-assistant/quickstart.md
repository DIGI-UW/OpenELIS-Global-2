# Quickstart: Catalyst - LLM-Powered Lab Data Assistant

**Feature**: OGC-070-catalyst-assistant  
**Date**: 2026-01-21  
**Estimated Time**: 45-90 minutes for MVP setup

## Overview

This guide walks you through setting up and running the Catalyst MVP - a chat-to-SQL assistant for OpenELIS Global with MCP-based schema retrieval. By the end, you'll be able to ask natural language questions and get SQL results.

**Architecture**: Python MCP Server (schema RAG) → Java Backend (LLM orchestration) → React Frontend (Carbon chat UI)

## Prerequisites

- [ ] OpenELIS development environment running (`docker compose -f dev.docker-compose.yml up -d`)
- [ ] Java 21 installed (`java -version` shows 21.x.x)
- [ ] Python 3.11+ installed (`python3 --version`)
- [ ] Node.js 16+ installed
- [ ] Docker with compose v2
- [ ] Either:
  - **Cloud API Key**: OpenAI or Google Gemini API key for fast iteration, OR
  - **Local Setup**: Ollama or LM Studio for privacy-preserving deployment

## Quick Start Options

### Option A: Cloud API (Fastest - No GPU Required)

```bash
# 1. Start MCP server + OpenELIS
docker compose -f catalyst-dev.docker-compose.yml up -d

# 2. Set your API key (choose one)
export CATALYST_LLM_PROVIDER=openai
export OPENAI_API_KEY=sk-your-key-here
# OR for Gemini:
# export CATALYST_LLM_PROVIDER=gemini
# export GOOGLE_API_KEY=your-google-api-key

# 3. Build backend with Catalyst
mvn clean install -DskipTests -Dmaven.test.skip=true

# 4. Restart OpenELIS container
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org

# 5. Test the endpoint
curl -k -X POST https://localhost/rest/catalyst/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_SESSION_TOKEN" \
  -d '{"query": "How many samples are in the database?"}'
```

### Option B: Local LLM with Ollama (Privacy-First)

```bash
# 1. Start MCP server + Ollama
docker compose -f catalyst-dev.docker-compose.yml up -d

# 2. Pull SQLCoder model (~4GB download, first time only)
docker exec -it catalyst-ollama ollama pull sqlcoder:7b

# 3. Verify model is ready
curl http://localhost:11434/api/tags

# 4. Configure for local LLM
export CATALYST_LLM_PROVIDER=ollama

# 5. Build and start OpenELIS
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org

# 6. Test
curl -k -X POST https://localhost/rest/catalyst/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_SESSION_TOKEN" \
  -d '{"query": "How many samples are in the database?"}'
```

### Option C: LM Studio (Local with UI)

```bash
# 1. Download and start LM Studio from https://lmstudio.ai/
# 2. Load a model (e.g., SQLCoder or Mistral)
# 3. Start the local server (default: http://localhost:1234)

# 4. Start MCP server only
docker compose -f catalyst-dev.docker-compose.yml up -d catalyst-mcp

# 5. Configure for LM Studio
export CATALYST_LLM_PROVIDER=lmstudio
export LM_STUDIO_BASE_URL=http://host.docker.internal:1234/v1

# 6. Build and start OpenELIS
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

---

## Step-by-Step Setup

### Step 1: Start the MCP Schema Server

The MCP server provides RAG-based schema retrieval:

```bash
# Start just the MCP server for development
cd catalyst-mcp
python -m pip install -e .
python -m src.server

# OR via Docker
docker compose -f catalyst-dev.docker-compose.yml up -d catalyst-mcp

# Verify MCP server is running
curl http://localhost:8000/health
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

### Step 3: Configure LLM Provider

Edit `volume/properties/catalyst.properties`:

```properties
# LLM Provider Selection (openai, gemini, ollama, lmstudio)
catalyst.llm.provider=ollama

# Cloud: OpenAI
catalyst.llm.openai.model=gpt-4o

# Cloud: Google Gemini  
catalyst.llm.gemini.model=gemini-1.5-pro

# Local: Ollama
catalyst.llm.ollama.base-url=http://ollama:11434
catalyst.llm.ollama.model=sqlcoder:7b
catalyst.llm.ollama.timeout=60s

# Local: LM Studio (OpenAI-compatible)
catalyst.llm.lmstudio.base-url=http://host.docker.internal:1234/v1
catalyst.llm.lmstudio.model=local-model

# MCP Server
catalyst.mcp.server-url=http://catalyst-mcp:8000/sse

# Guardrails
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
# Health check - should return provider + MCP status
curl -k https://localhost/rest/catalyst/health

# Test query generation (no execution)
curl -k -X POST https://localhost/rest/catalyst/query \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_SESSION_TOKEN" \
  -d '{
    "query": "How many samples were entered today?",
    "execute": false
  }'

# Expected response:
# {
#   "queryId": "...",
#   "status": "GENERATED",
#   "generatedSql": "SELECT COUNT(*) FROM sample WHERE entered_date = CURRENT_DATE",
#   "estimatedRows": 150
# }
```

### Step 6: Run Tests

```bash
# MCP Server tests (pytest)
cd catalyst-mcp && pytest

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
cd catalyst-mcp

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

### MCP Server Issues

```bash
# Check MCP server is running
curl http://localhost:8000/health

# Check MCP server logs
docker logs catalyst-mcp

# Test MCP tool directly
curl -X POST http://localhost:8000/tools/get_relevant_tables \
  -H "Content-Type: application/json" \
  -d '{"query": "samples entered today"}'
```

### LLM Connection Failed

```bash
# For Ollama
curl http://localhost:11434/api/tags

# For OpenAI (check API key)
curl https://api.openai.com/v1/models -H "Authorization: Bearer $OPENAI_API_KEY"

# For Gemini (check API key)
curl "https://generativelanguage.googleapis.com/v1/models?key=$GOOGLE_API_KEY"
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

The `catalyst-dev.docker-compose.yml` includes:

```yaml
services:
  catalyst-mcp:
    build: ./catalyst-mcp
    ports:
      - "8000:8000"
    environment:
      - DATABASE_URL=postgresql://clinlims:clinlims@oe-postgres:5432/clinlims
    depends_on:
      - oe-postgres
      
  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [gpu]
```

---

## Example Queries

| Natural Language | Generated SQL |
|-----------------|---------------|
| "How many samples were entered today?" | `SELECT COUNT(*) FROM sample WHERE entered_date = CURRENT_DATE` |
| "Show HIV test results from last week" | `SELECT * FROM analysis a JOIN test t ON a.test_id = t.id WHERE t.name LIKE '%HIV%' AND a.started_date >= CURRENT_DATE - INTERVAL '7 days'` |
| "What's the average turnaround time for malaria tests?" | `SELECT AVG(completed_date - started_date) FROM analysis a JOIN test t ON a.test_id = t.id WHERE t.name LIKE '%malaria%'` |
| "Count samples by type this month" | `SELECT sample_type, COUNT(*) FROM sample WHERE entered_date >= DATE_TRUNC('month', CURRENT_DATE) GROUP BY sample_type` |

---

## Next Steps

After MVP validation:

1. **Phase 2**: Add A2A Agent orchestration for multi-agent workflows
2. **Phase 3**: Add report storage, scheduling, dashboard widgets

## Resources

- **Spec**: [specs/OGC-070-catalyst-assistant/spec.md](./spec.md)
- **Plan**: [specs/OGC-070-catalyst-assistant/plan.md](./plan.md)
- **API Contract**: [specs/OGC-070-catalyst-assistant/contracts/catalyst-api.yaml](./contracts/catalyst-api.yaml)
- **Jira**: [OGC-70](https://uwdigi.atlassian.net/browse/OGC-70)
- **MCP Documentation**: [modelcontextprotocol.io](https://modelcontextprotocol.io/)
- **LangChain4j Docs**: [docs.langchain4j.dev](https://docs.langchain4j.dev/)
- **Carbon AI Chat**: [chat.carbondesignsystem.com](https://chat.carbondesignsystem.com/)
