# Tasks: Catalyst - LLM-Powered Lab Data Assistant

**Branch**: `spec/OGC-070-catalyst-assistant` | **Date**: 2026-01-21  
**Input**: Design documents from `/specs/OGC-070-catalyst-assistant/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Organization**: Tasks are organized by **Milestone** per Constitution
Principle IX. Tests are **MANDATORY** per Constitution Principle V (TDD).

## Format: `[ID] [P?] [M#] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[M#]**: Which milestone this task belongs to (e.g., M0, M1, M2, M3, M4)
- Include exact file paths in descriptions

## Total Task Count

- **M0.0 (Skeleton POC)**: 8 tasks
- **M0.1 (Provider Switching)**: 6 tasks
- **M0.2 (Multi-Agent)**: 16 tasks
- **M1 (MCP Server)**: 23 tasks (unchanged)
- **M2 (Backend Core)**: 28 tasks (reduced - security deferred)
- **M3 (Frontend Chat)**: 22 tasks (reduced - token handling deferred)
- **M4 (Integration + Security)**: 27 tasks (increased - security added)
- **Total**: 130 tasks (unchanged, just redistributed)

---

## Milestone 0.0: Skeleton POC (Estimate: 1-2 days)

**Branch**: `feat/OGC-070-catalyst-assistant-m0-skeleton-poc`  
**Goal**: Prove A2A + LLM works with ZERO complexity  
**Verification**: Agent returns SQL from single provider (LM Studio)

### M0.0.1: Branch Setup & Project Structure

- [ ] T001 [M0.0] Create milestone branch
      `feat/OGC-070-catalyst-assistant-m0-skeleton-poc` from `develop`
- [ ] T002 [M0.0] Create project directory structure
      `projects/catalyst/catalyst-agents/` with pyproject.toml
- [ ] T002a [M0.0] Add a2a-sdk dependency to
      `projects/catalyst/catalyst-agents/pyproject.toml` (version 0.3.22+)
- [ ] T002b [M0.0] Add FastAPI, uvicorn, and httpx dependencies to
      `projects/catalyst/catalyst-agents/pyproject.toml` for agent runtime
- [ ] T003 [P] [M0.0] Create `projects/catalyst/catalyst-agents/src/__init__.py`
- [ ] T004 [P] [M0.0] Create
      `projects/catalyst/catalyst-agents/src/agents/__init__.py`
- [ ] T005 [P] [M0.0] Create
      `projects/catalyst/catalyst-agents/tests/__init__.py`

### M0.0.2: SQLGenAgent Test (TDD - MANDATORY)

> **NOTE: Write this test FIRST, ensure it FAILS before implementation**

- [ ] T010 [P] [M0.0] Write pytest test for SQLGenAgent SQL generation with
      hardcoded schema in
      `projects/catalyst/catalyst-agents/tests/test_sqlgen_agent.py`

### M0.0.3: SQLGenAgent Implementation

- [ ] T014 [M0.0] Implement SQLGenAgent in
      `projects/catalyst/catalyst-agents/src/agents/sqlgen_agent.py` with LM Studio
      provider only (OpenAI-compatible API) and hardcoded schema context (3-5 sample tables as string)

### M0.0.4: Agent Server & Discovery

- [ ] T019 [M0.0] Implement agent server entry point in
      `projects/catalyst/catalyst-agents/src/main.py` with FastAPI + uvicorn
- [ ] T020 [M0.0] Create minimal Agent Card at
      `projects/catalyst/catalyst-agents/.well-known/agent.json` for SQLGenAgent
      discovery

### M0.0.5: Verification & PR

- [ ] T023 [M0.0] Run pytest to verify M0.0 test passes, verify curl returns
      SQL, create PR `feat/OGC-070-catalyst-assistant-m0-skeleton-poc` →
      `develop`

---

## Milestone 0.1: Provider Switching (Estimate: 0.5 days)

**Branch**: `feat/OGC-070-catalyst-assistant-m0-provider-switching`  
**Goal**: Prove same agent works with local AND cloud providers  
**Verification**: Both providers (LM Studio, Gemini) generate SQL

### M0.1.1: Provider Switching Tests (TDD - MANDATORY)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T010a [P] [M0.1] Write pytest test for SQLGenAgent provider switching
      (Gemini/LM Studio) in
      `projects/catalyst/catalyst-agents/tests/test_sqlgen_agent.py` (FR-007)

### M0.1.2: Provider Implementation

- [ ] T014a [M0.1] Implement provider switching in SQLGenAgent supporting
      Gemini, LM Studio in
      `projects/catalyst/catalyst-agents/src/agents/sqlgen_agent.py`
- [ ] T014b [M0.1] Add provider configuration loading from
      `projects/catalyst/catalyst-agents/src/config/agents_config.yaml` in
      SQLGenAgent
- [ ] T018 [M0.1] Create agent configuration in
      `projects/catalyst/catalyst-agents/src/config/agents_config.yaml` with both
      providers (Gemini, LM Studio)

### M0.1.3: Verification & PR

- [ ] T023a [M0.1] Run pytest to verify all provider tests pass, test with each
      provider, create PR
      `feat/OGC-070-catalyst-assistant-m0-provider-switching` → `develop`

---

## Milestone 0.2: Multi-Agent Team (Estimate: 2-3 days)

**Branch**: `feat/OGC-070-catalyst-assistant-m0-multi-agent`  
**Goal**: Prove Router → SchemaAgent → SQLGenAgent orchestration  
**Verification**: RouterAgent delegates correctly, single-agent fallback works

### M0.2.1: Branch Setup & Additional Structure

- [ ] T005a [P] [M0.2] Create
      `projects/catalyst/catalyst-agents/src/agent_cards/` directory
- [ ] T006 [P] [M0.2] Create `projects/catalyst/catalyst-agents/src/config/`
      directory (if not already created)

### M0.2.2: Multi-Agent Tests (TDD - MANDATORY)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T008 [P] [M0.2] Write pytest test for RouterAgent orchestration logic in
      `projects/catalyst/catalyst-agents/tests/test_router_agent.py`
- [ ] T009 [P] [M0.2] Write pytest test for SchemaAgent with hardcoded schema
      (no MCP yet) in
      `projects/catalyst/catalyst-agents/tests/test_schema_agent.py`
- [ ] T011 [P] [M0.2] Write pytest test for Agent Card validation in
      `projects/catalyst/catalyst-agents/tests/test_agent_cards.py`

### M0.2.3: Multi-Agent Implementation

- [ ] T012 [M0.2] Implement RouterAgent in
      `projects/catalyst/catalyst-agents/src/agents/router_agent.py` with A2A
      task delegation (NO PHI detection - deferred to M4)
- [ ] T013 [M0.2] Implement SchemaAgent in
      `projects/catalyst/catalyst-agents/src/agents/schema_agent.py` with
      hardcoded schema (NO MCP yet - MCP in M1)
- [ ] T015 [M0.2] Create RouterAgent Card in
      `projects/catalyst/catalyst-agents/src/agent_cards/router.json` per A2A
      spec
- [ ] T016 [P] [M0.2] Create SchemaAgent Card in
      `projects/catalyst/catalyst-agents/src/agent_cards/schema.json` per A2A
      spec
- [ ] T017 [P] [M0.2] Create SQLGenAgent Card in
      `projects/catalyst/catalyst-agents/src/agent_cards/sqlgen.json` per A2A
      spec
- [ ] T021 [M0.2] Create Dockerfile for agent runtime in
      `projects/catalyst/catalyst-agents/Dockerfile`
- [ ] T022 [M0.2] Add agent service to
      `projects/catalyst/catalyst-dev.docker-compose.yml`

### M0.2.4: Verification & PR

- [ ] T023b [M0.2] Run pytest to verify all M0.2 tests pass, verify RouterAgent
      delegates correctly, verify single-agent fallback mode, create PR
      `feat/OGC-070-catalyst-assistant-m0-multi-agent` → `develop`

---

## Milestone 1: MCP Schema Server (Estimate: 3-4 days) [PARALLEL]

**Branch**: `feat/OGC-070-catalyst-assistant-m1-mcp-server`  
**Goal**: Implement Python MCP server for schema RAG retrieval with ChromaDB
embeddings  
**Verification**: MCP tools callable via Streamable HTTP, pytest tests pass  
**Depends On**: M0.2 (SchemaAgent needs to call MCP)

### M1.1: Branch Setup & Project Structure

- [ ] T024 [M1] Create milestone branch
      `feat/OGC-070-catalyst-assistant-m1-mcp-server` from `develop`
- [ ] T025 [M1] Create project directory structure
      `projects/catalyst/catalyst-mcp/` with pyproject.toml
- [ ] T025a [M1] Add mcp SDK dependency to
      `projects/catalyst/catalyst-mcp/pyproject.toml`
- [ ] T025b [M1] Add chromadb and langchain dependencies to
      `projects/catalyst/catalyst-mcp/pyproject.toml` for RAG
- [ ] T026 [P] [M1] Create `projects/catalyst/catalyst-mcp/src/__init__.py`
- [ ] T027 [P] [M1] Create
      `projects/catalyst/catalyst-mcp/src/tools/__init__.py`
- [ ] T028 [P] [M1] Create `projects/catalyst/catalyst-mcp/src/rag/__init__.py`
- [ ] T029 [P] [M1] Create `projects/catalyst/catalyst-mcp/src/db/__init__.py`
- [ ] T030 [P] [M1] Create `projects/catalyst/catalyst-mcp/tests/__init__.py`

### M1.2: MCP Server Tests (TDD - MANDATORY)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T031 [P] [M1] Write pytest test for `get_relevant_tables` MCP tool in
      `projects/catalyst/catalyst-mcp/tests/test_schema_tools.py`
- [ ] T032 [P] [M1] Write pytest test for `get_table_ddl` MCP tool in
      `projects/catalyst/catalyst-mcp/tests/test_schema_tools.py`
- [ ] T033 [P] [M1] Write pytest test for `get_relationships` MCP tool in
      `projects/catalyst/catalyst-mcp/tests/test_relationship_tools.py`
- [ ] T034 [P] [M1] Write pytest test for RAG retriever in
      `projects/catalyst/catalyst-mcp/tests/test_retriever.py`

### M1.3: MCP Tools Implementation

- [ ] T035 [M1] Implement MCP schema tools in
      `projects/catalyst/catalyst-mcp/src/tools/schema_tools.py`
      (`get_relevant_tables`, `get_table_ddl`)
- [ ] T036 [M1] Implement MCP relationship tools in
      `projects/catalyst/catalyst-mcp/src/tools/relationship_tools.py`
      (`get_relationships`)
- [ ] T037 [M1] Implement PostgreSQL schema extractor in
      `projects/catalyst/catalyst-mcp/src/db/schema_extractor.py`

### M1.4: RAG Implementation

- [ ] T038 [M1] Implement schema embedding generation in
      `projects/catalyst/catalyst-mcp/src/rag/embeddings.py` with ChromaDB
- [ ] T039 [M1] Implement vector retriever in
      `projects/catalyst/catalyst-mcp/src/rag/retriever.py` for RAG-based table
      filtering

### M1.5: MCP Server & Deployment

- [ ] T040 [M1] Implement MCP server entry point in
      `projects/catalyst/catalyst-mcp/src/server.py` with Streamable HTTP
      transport
- [ ] T041 [M1] Create MCP server configuration in
      `projects/catalyst/catalyst-mcp/config/mcp_config.yaml`
- [ ] T042 [M1] Create Dockerfile for MCP server in
      `projects/catalyst/catalyst-mcp/Dockerfile`
- [ ] T043 [M1] Add MCP service to
      `projects/catalyst/catalyst-dev.docker-compose.yml`

### M1.6: Verification & PR

- [ ] T044 [M1] Run pytest to verify all M1 tests pass, create PR
      `feat/OGC-070-catalyst-assistant-m1-mcp-server` → `develop`

---

## Milestone 2: Backend Core (Estimate: 4-5 days) [PARALLEL]

**Branch**: `feat/OGC-070-catalyst-assistant-m2-backend-core`  
**Goal**: Implement Java OpenELIS integration with A2A client, SQL guardrails,
audit logging (no security features)  
**Verification**: Unit tests pass, ORM test passes, A2A client calls
RouterAgent  
**Depends On**: M0.2 (needs RouterAgent to call)  
**Note**: Security features (PHI detection, confirmation tokens) deferred to M4

### M2.1: Branch Setup & Package Structure

- [ ] T045 [M2] Create milestone branch
      `feat/OGC-070-catalyst-assistant-m2-backend-core` from `develop`
- [ ] T045a [M2] Add HTTP client dependency to `pom.xml` for A2A agent
      communication (e.g., Apache HttpClient or OkHttp)
- [ ] T045b [M2] Add JSON processing dependency to `pom.xml` if not already
      present (Jackson)
- [ ] T046 [M2] Create package structure
      `src/main/java/org/openelisglobal/catalyst/` with subpackages (config,
      agent, service, dao, valueholder, guardrails, form)

### M2.2: Backend Tests (TDD - MANDATORY)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T047 [P] [M2] Write ORM validation test for CatalystQuery entity in
      `src/test/java/org/openelisglobal/catalyst/HibernateMappingValidationTest.java`
      (Constitution V.4)
- [ ] T048 [P] [M2] Write JUnit test for CatalystQueryService in
      `src/test/java/org/openelisglobal/catalyst/service/CatalystQueryServiceTest.java`
      with mocked A2A client
- [ ] T048a [P] [M2] Write JUnit test for audit metadata capture (providerType,
      providerId, tablesUsed) in
      `src/test/java/org/openelisglobal/catalyst/service/CatalystQueryServiceTest.java`
      (FR-019 - without phiGated, deferred to M4)
- [ ] T049 [P] [M2] Write JUnit test for A2AAgentClient in
      `src/test/java/org/openelisglobal/catalyst/agent/A2AAgentClientTest.java`
- [ ] T050 [P] [M2] Write JUnit test for SQLGuardrails in
      `src/test/java/org/openelisglobal/catalyst/guardrails/SQLGuardrailsTest.java`
      (blocked tables only, NO PHI detection - deferred to M4)
- [ ] T050a [P] [M2] Write JUnit test for row estimation (EXPLAIN-based) in
      `src/test/java/org/openelisglobal/catalyst/guardrails/SQLGuardrailsTest.java`
      (FR-009)
- [ ] T050b [P] [M2] Write JUnit test for >10k row warning/truncation logic in
      `src/test/java/org/openelisglobal/catalyst/guardrails/SQLGuardrailsTest.java`
      (FR-009)

### M2.3: Entity Layer (Valueholder)

- [ ] T051 [M2] Create CatalystQuery valueholder in
      `src/main/java/org/openelisglobal/catalyst/valueholder/CatalystQuery.java`
      extending BaseObject with JPA annotations (user_id, query_text,
      generated_sql, provider_type, provider_id, tables_used, timestamp)
      (FR-019 - without phi_gated and confirmation_token, deferred to M4)

### M2.4: DAO Layer

- [ ] T052 [M2] Create CatalystQueryDAO interface in
      `src/main/java/org/openelisglobal/catalyst/dao/CatalystQueryDAO.java`
- [ ] T053 [M2] Implement CatalystQueryDAOImpl in
      `src/main/java/org/openelisglobal/catalyst/dao/CatalystQueryDAOImpl.java`
      extending BaseDAOImpl with @Component

### M2.5: Service Layer

- [ ] T054 [M2] Create CatalystQueryService interface in
      `src/main/java/org/openelisglobal/catalyst/service/CatalystQueryService.java`
- [ ] T055 [M2] Implement CatalystQueryServiceImpl in
      `src/main/java/org/openelisglobal/catalyst/service/CatalystQueryServiceImpl.java`
      with @Service and @Transactional
- [ ] T055a [M2] Implement row estimation using EXPLAIN in
      `src/main/java/org/openelisglobal/catalyst/service/CatalystQueryServiceImpl.java`
      (FR-009)
- [ ] T056 [M2] Implement SQL guardrails in
      `src/main/java/org/openelisglobal/catalyst/guardrails/SQLGuardrails.java`
      (blocked tables, SQL validation - NO PHI detection, deferred to M4)

### M2.6: A2A Agent Client

- [ ] T057 [M2] Create A2AAgentClient interface in
      `src/main/java/org/openelisglobal/catalyst/agent/A2AAgentClient.java`
- [ ] T058 [M2] Implement A2AAgentClientImpl in
      `src/main/java/org/openelisglobal/catalyst/agent/A2AAgentClientImpl.java`
      with HTTP client to call RouterAgent

### M2.7: Configuration

- [ ] T059 [M2] Create CatalystAgentConfig in
      `src/main/java/org/openelisglobal/catalyst/config/CatalystAgentConfig.java`
      with @Configuration for A2A agent URL and mode (multi/single)
- [ ] T060 [M2] Create CatalystDatabaseConfig in
      `src/main/java/org/openelisglobal/catalyst/config/CatalystDatabaseConfig.java`
      for read-only connection
- [ ] T061 [M2] Create Catalyst properties file in
      `volume/properties/catalyst.properties` with agent URL, mode, guardrails
      config

### M2.8: Database Schema

- [ ] T062 [M2] Create Liquibase changeset in
      `src/main/resources/liquibase/catalyst/catalyst-001-create-audit-table.xml`
      for CatalystQuery table without security fields (phi_gated,
      confirmation_token added in M4) (Constitution VI)

### M2.9: Forms (DTOs)

- [ ] T063 [P] [M2] Create CatalystQueryForm in
      `src/main/java/org/openelisglobal/catalyst/form/CatalystQueryForm.java`
      for request mapping
- [ ] T064 [P] [M2] Create CatalystQueryResponse in
      `src/main/java/org/openelisglobal/catalyst/form/CatalystQueryResponse.java`
      for response mapping

### M2.10: Verification & PR

- [ ] T065 [M2] Run ORM validation test (MUST pass in <5s, no database)
- [ ] T066 [M2] Run unit tests with Maven (MUST pass, >80% coverage target)
- [ ] T067 [M2] Format code with `mvn spotless:apply` (MANDATORY before commit)
- [ ] T068 [M2] Build backend with
      `mvn clean install -DskipTests -Dmaven.test.skip=true`
- [ ] T069 [M2] Verify A2A client can call RouterAgent (integration check)
- [ ] T070 [M2] Create PR `feat/OGC-070-catalyst-assistant-m2-backend-core` →
      `develop`

---

## Milestone 3: Frontend Chat (Estimate: 3-4 days) [PARALLEL]

**Branch**: `feat/OGC-070-catalyst-assistant-m3-frontend-chat`  
**Goal**: Implement Carbon chat sidebar with i18n, query input, results
display  
**Verification**: Jest tests pass, component renders correctly with en/fr
translations

### M3.1: Branch Setup & Component Structure

- [ ] T071 [M3] Create milestone branch
      `feat/OGC-070-catalyst-assistant-m3-frontend-chat` from `develop`
- [ ] T071a [M3] Add @carbon/ai-chat dependency to `frontend/package.json`
      (version 1.0+)
- [ ] T072 [M3] Create component directory `frontend/src/components/catalyst/`
      with index.js

### M3.2: Frontend Tests (TDD - MANDATORY)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T073 [P] [M3] Write Jest test for CatalystSidebar in
      `frontend/src/components/catalyst/__tests__/CatalystSidebar.test.jsx`
      (render, i18n)
- [ ] T074 [P] [M3] Write Jest test for ChatInterface in
      `frontend/src/components/catalyst/__tests__/ChatInterface.test.jsx`
      (message display)
- [ ] T075 [P] [M3] Write Jest test for QueryInput in
      `frontend/src/components/catalyst/__tests__/QueryInput.test.jsx` (user
      input)
- [ ] T076 [P] [M3] Write Jest test for ResultsDisplay in
      `frontend/src/components/catalyst/__tests__/ResultsDisplay.test.jsx`
      (table rendering)

### M3.3: Component Implementation

- [ ] T077 [M3] Implement CatalystSidebar in
      `frontend/src/components/catalyst/CatalystSidebar.jsx` using
      @carbon/ai-chat
- [ ] T078 [M3] Implement ChatInterface in
      `frontend/src/components/catalyst/ChatInterface.jsx` with message list and
      Carbon components
- [ ] T079 [M3] Implement QueryInput in
      `frontend/src/components/catalyst/QueryInput.jsx` with Carbon TextInput
      and Button
- [ ] T080 [M3] Implement ResultsDisplay in
      `frontend/src/components/catalyst/ResultsDisplay.jsx` with Carbon
      DataTable
- [ ] T080a [M3] Implement >10k row warning UI in
      `frontend/src/components/catalyst/ResultsDisplay.jsx` (FR-009)
- [ ] T081 [M3] Implement SQLPreview in
      `frontend/src/components/catalyst/SQLPreview.jsx` with Carbon CodeSnippet
- [ ] T081a [M3] Implement example prompts display in
      `frontend/src/components/catalyst/CatalystSidebar.jsx` with Carbon
      components (FR-014)
- [ ] T081b [M3] Add example prompts i18n keys to
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`
      (FR-014)

### M3.4: Internationalization (Constitution VII - MANDATORY)

- [ ] T082 [M3] Add Catalyst keys to `frontend/src/languages/en.json`
      (catalyst.sidebar.title, catalyst.query.placeholder,
      catalyst.button.submit, catalyst.results.title, catalyst.sql.preview,
      etc.)
- [ ] T083 [M3] Add Catalyst keys to `frontend/src/languages/fr.json` with
      French translations (Constitution VII - minimum en + fr)

### M3.5: Component Exports

- [ ] T084 [M3] Export components from
      `frontend/src/components/catalyst/index.js`

### M3.6: Verification & PR

- [ ] T085 [M3] Run Jest tests with `npm test` (MUST pass, >70% coverage target)
- [ ] T086 [M3] Format code with `npm run format` (MANDATORY before commit)
- [ ] T087 [M3] Verify components render correctly with `npm start` (manual
      check)
- [ ] T088 [M3] Verify i18n works for en/fr by switching language in browser
- [ ] T089 [M3] Create PR `feat/OGC-070-catalyst-assistant-m3-frontend-chat` →
      `develop`

---

## Milestone 4: Integration + Security (Estimate: 3-4 days) [SEQUENTIAL - depends on M0.2, M1, M2, M3]

**Branch**: `feat/OGC-070-catalyst-assistant-m4-integration-security`  
**Goal**: Wire all components, implement REST controller, add security features
(PHI detection, confirmation tokens), create E2E test  
**Verification**: Controller integration tests pass, E2E test passes
(chat→agents→SQL→results), security features work

### M4.1: Branch Setup & REST Controller

- [ ] T090 [M4] Create milestone branch
      `feat/OGC-070-catalyst-assistant-m4-integration-security` from `develop`
      (merge M0.2, M1, M2, M3 first)
- [ ] T091 [M4] Implement CatalystRestController in
      `src/main/java/org/openelisglobal/catalyst/controller/CatalystRestController.java`
      with @RestController and /rest/catalyst/query endpoint

### M4.2: Integration Tests (TDD - MANDATORY)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T092 [P] [M4] Write controller integration test for /rest/catalyst/query
      in
      `src/test/java/org/openelisglobal/catalyst/controller/CatalystRestControllerTest.java`
      extending BaseWebContextSensitiveTest
- [ ] T093 [M4] Write Cypress E2E test in `frontend/cypress/e2e/catalyst.cy.js`
      proving full chat→agents→SQL→confirmation→results flow (Constitution V.5)
- [ ] T093a [M4] Write Cypress E2E test for JOIN queries in
      `frontend/cypress/e2e/catalyst.cy.js` (FR-015)
- [ ] T093b [M4] Write Cypress E2E test for aggregation queries in
      `frontend/cypress/e2e/catalyst.cy.js` (FR-015)
- [ ] T093c [M4] Write Cypress E2E test for date filtering queries in
      `frontend/cypress/e2e/catalyst.cy.js` (FR-015)

### M4.3: Full Stack Integration

- [ ] T094 [M4] Wire frontend CatalystSidebar to backend REST endpoint with
      fetch/axios in `frontend/src/components/catalyst/CatalystSidebar.jsx`
- [ ] T094a [M4] Implement basic query handling in frontend (confirmation token
      handling deferred to M4.6)
- [ ] T095 [M4] Configure full stack Docker Compose in
      `projects/catalyst/catalyst-dev.docker-compose.yml` (agents + MCP +
      OpenELIS + frontend)
- [ ] T096 [M4] Add Agent Card discovery endpoint proxy at
      `/.well-known/agent.json` in
      `src/main/java/org/openelisglobal/catalyst/controller/CatalystRestController.java`

### M4.4: Response Formatting & Export

- [ ] T097 [M4] Implement table response formatting in
      `src/main/java/org/openelisglobal/catalyst/service/CatalystQueryServiceImpl.java`
- [ ] T098 [M4] Implement CSV export endpoint in
      `src/main/java/org/openelisglobal/catalyst/controller/CatalystRestController.java`
      (GET /export/{queryId}?format=csv per contract)
- [ ] T099 [M4] Implement JSON export endpoint in
      `src/main/java/org/openelisglobal/catalyst/controller/CatalystRestController.java`
      (GET /export/{queryId}?format=json per contract)

### M4.6: Security Features (Deferred from M0/M2)

- [ ] T090a [M4] Add PHI detection to RouterAgent in
      `projects/catalyst/catalyst-agents/src/agents/router_agent.py` (FR-018)
- [ ] T090b [M4] Add provider routing for PHI-flagged queries in RouterAgent: if
      PHI detected and provider is external, route to local provider if healthy,
      else block
- [ ] T090c [M4] Add confirmation_token column to CatalystQuery entity in
      `src/main/java/org/openelisglobal/catalyst/valueholder/CatalystQuery.java`
      (FR-016)
- [ ] T090d [M4] Add phi_gated column to CatalystQuery entity in
      `src/main/java/org/openelisglobal/catalyst/valueholder/CatalystQuery.java`
      (FR-019)
- [ ] T090e [M4] Create Liquibase changeset to add confirmation_token and
      phi_gated columns to catalyst_query table in
      `src/main/resources/liquibase/catalyst/catalyst-002-add-security-fields.xml`
- [ ] T090f [M4] Implement confirmation token generation in
      `src/main/java/org/openelisglobal/catalyst/service/CatalystQueryServiceImpl.java`
      (compute hash from generated SQL) (FR-016)
- [ ] T090g [M4] Implement confirmation token validation in
      `src/main/java/org/openelisglobal/catalyst/service/CatalystQueryServiceImpl.java`
      (validate token matches SQL before execution) (FR-016)
- [ ] T090h [M4] Update audit metadata capture to include phiGated in
      `src/main/java/org/openelisglobal/catalyst/service/CatalystQueryServiceImpl.java`
      (FR-019)
- [ ] T090i [M4] Write pytest test for RouterAgent PHI detection and provider
      routing in `projects/catalyst/catalyst-agents/tests/test_router_agent.py`
      (FR-018)
- [ ] T090j [M4] Write unit test for confirmation token validation in
      `src/test/java/org/openelisglobal/catalyst/service/CatalystQueryServiceTest.java`
      (test token mismatch rejection)
- [ ] T090k [M4] Write unit test for PHI gating in
      `src/test/java/org/openelisglobal/catalyst/service/CatalystQueryServiceTest.java`
- [ ] T090l [M4] Implement confirmation token handling in frontend: store token
      from generation response, include in execution request in
      `frontend/src/components/catalyst/CatalystSidebar.jsx`
- [ ] T090m [M4] Write E2E test for PHI blocking in
      `frontend/cypress/e2e/catalyst.cy.js` (FR-018)
- [ ] T090n [M4] Write E2E test for confirmation flow in
      `frontend/cypress/e2e/catalyst.cy.js` (FR-016)

### M4.7: Verification & PR

- [ ] T100 [M4] Run controller integration tests with Maven (MUST pass)
- [ ] T101 [M4] Run Cypress E2E test individually with
      `npm run cy:run -- --spec "cypress/e2e/catalyst.cy.js"` (Constitution V.5)
- [ ] T102 [M4] Verify multi-agent flow works (Router delegates to Schema +
      SQLGen)
- [ ] T103 [M4] Verify single-agent fallback mode works when configured
- [ ] T104 [M4] Verify security features work (PHI detection, confirmation
      tokens)
- [ ] T105 [M4] Create PR
      `feat/OGC-070-catalyst-assistant-m4-integration-security` → `develop`

---

## Dependencies & Execution Order

### Milestone Dependencies

```mermaid
graph TD
    M00["M0.0: Skeleton POC"] --> M01["M0.1: Provider Switching"]
    M01 --> M02["M0.2: Multi-Agent Team"]
    M02 --> M1["M1: MCP Server"]
    M02 --> M2["M2: Backend Core"]
    M02 --> M3["M3: Frontend Chat"]
    M1 --> M4["M4: Integration + Security"]
    M2 --> M4
    M3 --> M4

    subgraph parallel ["Parallel Development"]
        M1
        M2
        M3
    end
```

- **M0.0 → M0.1 → M0.2**: Sequential (foundational validation)
- **M1, M2, M3**: Can be developed in parallel after M0.2 (marked [P] in
  milestone table)
- **M4**: MUST wait for M0.2, M1, M2, M3 to complete (sequential)

### Within Each Milestone

1. **Branch creation task** runs first
2. **Tests** MUST be written before implementation (TDD - Constitution V)
3. **Implementation** follows Red-Green-Refactor cycle
4. **Verification** confirms all tests pass
5. **PR creation** marks milestone complete

### Parallel Opportunities

- **M0-M3** can be worked on by different developers simultaneously
- Within each milestone, tasks marked [P] can run in parallel
- Tests within a milestone marked [P] can run in parallel

---

## Parallel Example: M2 (Backend Core)

```bash
# Launch all tests together (TDD):
Task T047: "Write ORM validation test for CatalystQuery entity"
Task T048: "Write JUnit test for CatalystQueryService"
Task T049: "Write JUnit test for A2AAgentClient"
Task T050: "Write JUnit test for SQLGuardrails"

# Launch all form DTOs together:
Task T063: "Create CatalystQueryForm"
Task T064: "Create CatalystQueryResponse"
```

---

## Implementation Strategy

### MVP Delivery (All Milestones)

1. **Week 1**: M0.0 (Skeleton POC) → M0.1 (Provider Switching) → M0.2
   (Multi-Agent)
2. **Week 2**: M1 (MCP Server) + M2 (Backend Core) + M3 (Frontend Chat) in
   parallel
3. **Week 3**: M4 (Integration + Security) + Testing + Bug fixes
4. **Deploy MVP**: Full chat→agents→SQL→results flow validated with security
   features

### Constitution Checkpoints (MANDATORY)

- **After M0.0**: SQLGenAgent test MUST pass, agent returns SQL
- **After M0.1**: Provider switching tests MUST pass, both providers (Gemini + LM Studio) work
- **After M0.2**: Multi-agent tests MUST pass, RouterAgent delegates correctly
- **After M1**: MCP tests MUST pass, MCP tools callable
- **After M2**: ORM test + unit tests MUST pass (>80% coverage)
- **After M3**: Jest tests MUST pass (>70% coverage)
- **After M4**: E2E test MUST pass, security features validated (Constitution
  V.5)

### Pre-Commit Checklist (MANDATORY)

```bash
# Backend (M2, M4)
mvn spotless:apply                                    # Format code
mvn clean install -DskipTests -Dmaven.test.skip=true  # Build
mvn test                                              # Run tests

# Frontend (M3, M4)
npm run format                                        # Format code
npm test                                              # Run tests
npm run cy:run -- --spec "cypress/e2e/catalyst.cy.js" # E2E test (M4 only)

# Python (M0, M1)
pytest                                                # Run tests
```

---

## Notes

- **[P]** tasks = different files, no dependencies, can run in parallel
- **[M#]** label maps task to specific milestone for traceability
- Each milestone is independently completable and testable
- **Tests FIRST** (TDD - Constitution V): Verify tests fail before implementing
- **Format code** before EVERY commit (Constitution: `mvn spotless:apply`,
  `npm run format`)
- **Individual E2E tests** during development (Constitution V.5:
  `npm run cy:run -- --spec "cypress/e2e/catalyst.cy.js"`)
- Commit after each task or logical group
- Stop at any checkpoint to validate milestone independently
