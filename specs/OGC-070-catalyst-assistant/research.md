# Research: Catalyst - LLM-Powered Lab Data Assistant

**Feature**: OGC-070-catalyst-assistant  
**Date**: 2026-01-20  
**Status**: Complete

## Executive Summary

This document consolidates research findings for implementing Catalyst, an
LLM-powered data assistant for OpenELIS Global. The research covers text-to-SQL
approaches, LLM integration options, frontend components, and standards-based AI
architecture for future phases.

---

## 1. Text-to-SQL Approach

### Decision: RAG-Based Schema Retrieval via MCP (Updated 2026-01-21)

**Rationale**: Modern text-to-SQL requires providing the LLM with relevant
schema context. The OpenELIS schema is too large for a single prompt context
window. A RAG approach with MCP standards validation was chosen for MVP.

**Alternatives Considered**:

| Approach                                       | Accuracy | Complexity | Chosen?               |
| ---------------------------------------------- | -------- | ---------- | --------------------- |
| Zero-shot (full schema in prompt)              | 60-65%   | Low        | ❌ Schema too large   |
| Static curated subset                          | 65-70%   | Low        | ❌ Limits query scope |
| Schema RAG (vector search for relevant tables) | 70-75%   | Medium     | ✅ MVP                |
| Fine-tuned model on OpenELIS schema            | 80%+     | High       | Future                |

**Implementation for MVP**:

- Python MCP server with RAG-based schema retrieval
- ChromaDB for embedding storage and similarity search
- MCP tools: `get_relevant_tables`, `get_table_ddl`, `get_relationships`
- **SchemaAgent** (Python A2A agent) calls MCP server via Streamable HTTP
  transport (SSE optional for streaming)
- Java backend calls RouterAgent (not MCP directly)

**References**:

- [SQLCoder GitHub](https://github.com/defog-ai/sqlcoder) - State-of-the-art
  text-to-SQL model
- [Vanna.ai Training Approach](https://vanna.ai/docs/postgres-openai-standard-vannadb.html) -
  RAG-based SQL generation
- [Text-to-SQL Comparison 2026](https://research.aimultiple.com/text-to-sql/) -
  Model benchmarks

---

## 2. LLM Provider Selection

### Decision: Provider Switching in Python Agents (SDK-native + OpenAI-compatible)

**Rationale**: In the MVP architecture, **SQL generation happens in Python
agents** (CatalystAgent / SQLGenAgent), not in the Java backend. Provider
switching should therefore be implemented in the agent runtime using
provider-native Python SDKs (Gemini) and a small HTTP client wrapper for
OpenAI-compatible endpoints (LM Studio). The Java backend only needs an HTTP
client to call the RouterAgent and never calls LLM APIs directly.

**Provider Comparison**:

| Provider          | Latency    | Cost             | Privacy             | SQL Accuracy | Best For                                                   |
| ----------------- | ---------- | ---------------- | ------------------- | ------------ | ---------------------------------------------------------- |
| Gemini (Cloud)    | 500-1000ms | $0.01-0.03/query | Data leaves network | 70%          | Fast development iteration                                 |
| LM Studio (Local) | 100-500ms  | Hardware only    | Fully air-gapped    | 65-70%       | Privacy-sensitive production with OpenAI-compatible models |

**Note**: Performance/cost figures are estimates based on typical usage
patterns. Actual values may vary by deployment, model version, and query
complexity.

**Recommended Strategy**:

- **Development**: Gemini (Cloud) for rapid iteration
- **Production**: LM Studio (Local) with llama/gemma models for privacy
  compliance
- **Provider Switching**: Configured in agent runtime (`agents_config.yaml`),
  not Java backend

**Agent Runtime Dependencies** (Python -
`projects/catalyst/catalyst-agents/pyproject.toml`):

```toml
[project]
dependencies = [
    "a2a-sdk[http-server]>=0.3.22",  # A2A protocol + FastAPI/uvicorn
    "google-genai>=0.2.0",  # Gemini provider (Google GenAI SDK)
    "httpx>=0.25.0",  # HTTP client for OpenAI-compatible APIs (LM Studio)
]
```

**Note**: LLM provider switching is implemented in SQLGenAgent (Python), not
Java backend. Java backend only needs HTTP client for A2A agent communication.

**References**:

- [A2A Python SDK](https://pypi.org/project/a2a-sdk/)
- [Google GenAI Python SDK](https://github.com/googleapis/python-genai)
- [LM Studio](https://lmstudio.ai/) (OpenAI-compatible local inference)
- [Gemini Structured Output](https://ai.google.dev/gemini-api/docs/structured-output)
- [Gemini Function Calling](https://ai.google.dev/gemini-api/docs/function-calling)

---

## 3. Local LLM Infrastructure

### Decision: LM Studio for Local Development

**Rationale**: LM Studio provides easy model management via GUI, supports
OpenAI-compatible API, and works well with OpenAI-compatible models. Runs on
host machine (not Docker), simplifying GPU access.

### 2026 Update: Tool Calling + OpenAI-Compatible Responses API

Recent LM Studio versions support **tool/function calling** and a more complete
OpenAI-compatible API surface (including `/v1/responses`). For Catalyst, this
matters because structured outputs (or tool calling) can improve reliability of
the “return SQL only” step, but correctness is still **model-dependent**.

**Setup**:

1. Download LM Studio from https://lmstudio.ai/
2. Load a model (use most recent available OpenAI-compatible model)
3. Start local server (default: http://localhost:1234/v1)
4. Configure agent runtime to use `http://host.docker.internal:1234/v1` as
   base_url

**Operational Caveat (Linux + Docker)**:

`host.docker.internal` is not guaranteed on all Linux/Docker setups. If the
agent runtime runs inside Docker on Linux, plan to use one of:

- Docker host-gateway mapping (recommended)
- An explicit host IP on the Docker bridge network
- Running the local model server in-network (separate container)

**Model Selection**:

- Use most recent available OpenAI-compatible models
- Target models suitable for SQL generation tasks

**Alternatives Considered**:

- Ollama: Docker-friendly but less flexible model management
- vLLM: Higher performance but more complex setup
- llama.cpp: Lowest level, most flexible, but requires more configuration

**References**:

- [LM Studio](https://lmstudio.ai/) (OpenAI-compatible local inference)
- [LM Studio Tools / Function Calling](https://lmstudio.ai/docs/developer/core/tools)
- [LM Studio OpenAI-compatible API](https://lmstudio.ai/docs/app/api/endpoints/openai/)

---

## 4. Frontend Chat Component

### Decision: @carbon/ai-chat v1.0

**Rationale**: IBM's official Carbon AI Chat library provides Constitution
Principle II compliance out of the box.

**Features**:

- ChatContainer for sidebar implementation
- AI labeling and "light-inspired" styling
- Message bubbles, loading states
- Carbon Design System tokens

**Installation**:

```bash
npm install @carbon/ai-chat @carbon/ai-chat-components
```

**Alternatives Considered**:

- assistant-ui/assistant-ui: More flexible but requires manual Carbon styling
- Custom implementation: Maximum control but significant development time
- Vercel AI SDK: React-focused but not Carbon-aligned

**Caveat**: SSR not supported - client-side rendering only (acceptable for
OpenELIS SPA)

**References**:

- [Carbon AI Chat Documentation](https://chat.carbondesignsystem.com/)
- [Carbon for AI Guidelines](https://carbondesignsystem.com/guidelines/carbon-for-ai/)

---

## 5. Privacy Architecture

### Decision: Schema-Only LLM Context

**Rationale**: Non-negotiable requirement from spec. LLM receives only metadata,
never patient data.

**Implementation**:

1. **Schema Context Generation**: Extract schema metadata and relationships from
   PostgreSQL catalogs (prefer `pg_catalog` for authoritative FK/constraint
   data; `information_schema` is acceptable only for simple column listings)
2. **Prompt Construction**: Include only schema + user question
3. **SQL Execution**: Separate step, LLM never sees results
4. **Read-Only Connection**: Dedicated PostgreSQL user with SELECT-only
   permissions

**Blocked Tables** (configurable):

- `sys_user` - System users
- `login_user` - Login credentials
- `user_role` - Role assignments
- Custom additions via configuration

**Audit Requirements**:

- Log all generated SQL with user ID, timestamp
- Log execution status and row count
- Store in `catalyst_query` table (Liquibase migration)

---

## 6. SQL Guardrails

### Decision: Multi-Layer Validation

**Layers**:

1. **Table Access Control**: Block restricted tables via configurable list
2. **Row Estimation**: `EXPLAIN` to estimate rows before execution
3. **Timeout Enforcement**: Query timeout via JDBC statement
4. **Complexity Limits**: Reject queries with excessive JOINs (configurable)

**Implementation Pattern**:

```java
public class SQLGuardrails {
    public ValidationResult validate(String sql) {
        // 1. Check for blocked tables
        if (containsBlockedTable(sql)) {
            return ValidationResult.reject("Access to restricted table denied");
        }

        // 2. Estimate row count
        long estimatedRows = estimateRows(sql);
        if (estimatedRows > maxRows) {
            return ValidationResult.reject("Query would return too many rows");
        }

        // 3. Check complexity
        if (countJoins(sql) > maxJoins) {
            return ValidationResult.reject("Query too complex");
        }

        return ValidationResult.accept();
    }
}
```

---

## 7. Standards-Based Architecture

### MCP (Model Context Protocol) - MVP ✅

**What**: Anthropic's standard for LLM-tool integration, adopted by OpenAI
(March 2025).

**Why for Catalyst MVP** (Updated 2026-01-21):

- Validate standards-based architecture early
- Standardize schema access as MCP tools
- Enable RAG-based schema retrieval at scale
- Prepare for future A2A integration

**MVP Implementation**: Python MCP Server (Official SDK) called by SchemaAgent

```python
# pyproject.toml (projects/catalyst/catalyst-mcp/)
dependencies = [
    "mcp>=1.0.0",  # Official MCP Python SDK
    "chromadb>=0.4.0",  # Vector store for RAG embeddings
    "langchain>=0.1.0",  # Embedding generation utilities
    "psycopg2-binary>=2.9.0",  # PostgreSQL schema extraction
]
```

**Agent Integration**: SchemaAgent (Python) calls MCP server via Streamable HTTP
transport. Java backend does NOT call MCP directly; it calls RouterAgent, which
delegates to SchemaAgent.

**MCP Tools for MVP**:

- `get_relevant_tables(query: str) -> list[str]` - RAG-based table retrieval
- `get_table_ddl(table_name: str) -> str` - DDL extraction
- `get_relationships(table_names: list[str]) -> list[dict]` - FK relationships
- `validate_sql(sql: str, user_query: str) -> dict` - Agent-side SQL validation
  (syntax, blocked tables, row estimation)

**References**:

- [MCP Official Documentation](https://modelcontextprotocol.io/)
- [MCP Specification](https://modelcontextprotocol.io/specification/)
- [MCP Transport: Streamable HTTP](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports)
- [MCP Python SDK](https://github.com/modelcontextprotocol/python-sdk)
- [MCP Python SDK Documentation](https://modelcontextprotocol.io/docs/python)

### Defense-in-Depth Validation Strategy

**Why Two Validation Layers?**

1. **Agent-Side (MCP `validate_sql`)**: Reduces invalid submissions, faster
   feedback loop for agents
2. **Backend-Side (Java `SQLGuardrails`)**: Trusted boundary enforcement,
   ultimate privacy guarantee

**MCP `validate_sql` Tool:**

- Syntax check via SQL parser
- Blocked table detection (configurable list)
- Row estimation via EXPLAIN ANALYZE
- Returns structured validation result for agent iteration

**Java `SQLGuardrails` Class:**

- Re-validates all checks (never trust agent output)
- Enforces confirmation token requirement
- Final gatekeeper before database access

### 2026 Implementation Note: Streamable HTTP Protocol Version Header

The Streamable HTTP transport spec (2025-11-25) introduces stricter requirements
for HTTP requests, including the `MCP-Protocol-Version` header and session ID
handling (`MCP-Session-Id`). For Catalyst, best practice is to:

- Pin MCP SDK versions in `pyproject.toml`
- Add a minimal conformance test that:
  - initializes a client session
  - lists tools
  - calls the MVP tools (`get_relevant_tables`, `validate_sql`)

### A2A Protocol (Agent2Agent) - MVP ✅

**What**: Google's open protocol for AI agent interoperability, donated to Linux
Foundation (April 2025).

**Why for Catalyst MVP** (Updated 2026-01-21):

- Validate standards-based multi-agent architecture early
- Simple 3-agent team: Router → SchemaAgent → SQLGenAgent
- Agent Cards for discovery per A2A specification
- Single-agent fallback mode for simpler deployments
- Based on med-agent-hub patterns

**Python Implementation** (MVP): [a2a-sdk](https://pypi.org/project/a2a-sdk/)
(PyPI)

```bash
pip install a2a-sdk[http-server]  # Includes FastAPI/uvicorn support
```

**Version**: 0.3.22+ (stable as of December 2025)

**Java Client** (for backend-to-agent communication): HTTP client (Apache
HttpClient or OkHttp) calling A2A agent runtime REST/JSON-RPC endpoints. No
direct A2A Java SDK dependency needed for MVP.

**References**:

- [A2A Protocol Official Site](https://a2a-protocol.org/latest/)
- [A2A Specification](https://a2a-protocol.org/latest/specification/)
- [A2A Python SDK Documentation](https://a2a-protocol.org/latest/sdk/python/)
- [A2A Python SDK API Reference](https://a2a-protocol.org/latest/sdk/python/api/)
- [A2A Agent Card Schema](https://a2a-protocol.org/latest/specification/#agent-card)

### A2A + MCP Relationship

| Protocol | Layer      | Purpose        | Catalyst Phase         |
| -------- | ---------- | -------------- | ---------------------- |
| **MCP**  | Vertical   | Agent-to-Tool  | MVP (schema retrieval) |
| **A2A**  | Horizontal | Agent-to-Agent | MVP (3-agent team)     |

**MVP Architecture**: RouterAgent (Python) orchestrates SchemaAgent (calls MCP
tools) and SQLGenAgent (text-to-SQL via LLM). Java backend calls RouterAgent via
A2A protocol; agents own all AI operations. Both A2A and MCP protocols validated
in MVP.

**Agent Card Discovery**: RouterAgent publishes Agent Card at
`/.well-known/agent.json` (or `/.well-known/agent-card.json` per A2A SDK 0.3.x
default). Required fields include `protocolVersions`, `name`, `description`,
`url`, `version`, `capabilities`, `defaultInputModes`, `defaultOutputModes`,
`skills`.

---

## 8. Google HAI-DEF Patterns

### Relevance to Catalyst

Google's Health AI Developer Foundations (HAI-DEF) demonstrate agentic patterns
applicable to healthcare AI:

**TxGemma Agentic-Tx Pattern**:

- Cognitive Orchestrator (Gemini) → Catalyst RouterAgent
- Specialist Analyst (TxGemma) → Catalyst SQL Generator Agent
- Built-in guardrails → Catalyst Validator Agent

**MedGemma FHIR Navigation Pattern**:

- Schema-aware query formulation
- Targeted SQL generation (not raw pattern matching)
- Structured result formatting

**Key Lessons**:

1. **Schema as Context**: Rich metadata improves accuracy
2. **Specialist Models**: Use SQLCoder for SQL, not general LLMs
3. **Orchestration Layer**: Separate routing from execution
4. **Guardrails**: Validate before execution

**References**:

- [HAI-DEF Developer Portal](https://developers.google.com/health-ai-developer-foundations)
- [TxGemma Agentic Demo](https://github.com/google-gemini/gemma-cookbook/blob/main/TxGemma/%5BTxGemma%5DAgentic_Demo_with_Hugging_Face.ipynb)
- [Agentic-Tx Paper](https://arxiv.org/pdf/2504.06196)

---

## 9. Reference Implementations

### pmanko/med-agent-hub

Multi-agent healthcare AI system demonstrating A2A + MCP patterns.

**Relevant Patterns**:

- Agent card publishing for discovery
- JSON-RPC communication
- MCP tool server implementation

**Repository**:
[github.com/pmanko/med-agent-hub](https://github.com/pmanko/med-agent-hub)

### pmanko/omrs-ai-playground

Healthcare AI research platform with OpenMRS integration.

**Relevant Patterns**:

- LLM provider abstraction
- Healthcare-specific prompt engineering
- FHIR-aware context construction

**Repository**:
[github.com/pmanko/omrs-ai-playground](https://github.com/pmanko/omrs-ai-playground)

---

## Open Questions Resolved

| Question              | Decision                    | Rationale                                                    |
| --------------------- | --------------------------- | ------------------------------------------------------------ |
| Which LLM framework?  | Provider-native Python SDKs | Agents own LLM calls; Java backend is HTTP client only       |
| Cloud vs Local?       | Both (configurable)         | Cloud for dev speed, local for production privacy            |
| Which chat component? | @carbon/ai-chat             | Carbon compliance, official IBM support                      |
| MCP in MVP?           | Yes (Python server)         | Validate standards early, support full schema via RAG        |
| A2A in MVP?           | Yes (3-agent team)          | Validate multi-agent patterns early, med-agent-hub reference |
| Which LLM providers?  | Gemini, LM Studio           | Cloud + local coverage, OpenAI-compatible API for LM Studio  |
| Schema handling?      | RAG via ChromaDB            | Full clinical schema too large for context window            |
| SQL validation?       | Multi-layer guardrails      | Defense in depth for security                                |

---

## 10. Text-to-SQL RAG Evaluation (2026 Best Practice)

### Why This Matters

For large schemas, **schema retrieval quality dominates SQL generation
quality**. Without an evaluation harness, it is easy to make changes that
improve some prompts but regress overall correctness.

### Recommended MVP Evaluation Harness

1. **Golden Query Set**:
   - 25–50 natural-language questions covering the MVP “top 5” query types
     (counts, joins, date filters, aggregations, turnaround time).
2. **Retrieval Metrics** (for schema RAG):
   - Recall@K for relevant tables (e.g., are all required tables in top K?)
   - Optional: MRR if you add reranking.
3. **SQL Metrics**:
   - Syntax validity rate
   - Execution accuracy: compare results to expected results on a seeded
     dataset.
4. **Error Taxonomy** (to guide iteration):
   - wrong table/column
   - missing join / wrong join
   - wrong filter column/value
   - wrong aggregation/grouping

**References**:

- [Ragas Text-to-SQL Evaluation Howto](https://docs.ragas.io/en/v0.3.5/howtos/applications/text2sql/)
- [CSR-RAG (Enterprise Text-to-SQL RAG)](https://arxiv.org/abs/2601.06564)

---

## 11. PostgreSQL Schema Introspection (2026 Best Practice)

### Decision: Prefer `pg_catalog` for authoritative relationships

`information_schema` is convenient and portable, but `pg_catalog` is more
complete (FK actions, validation state, richer constraint metadata). Since
OpenELIS is PostgreSQL-first, the MCP schema extraction should primarily use
`pg_catalog` for relationship/constraint extraction.

**References**:

- [PostgreSQL: `pg_constraint` catalog](https://www.postgresql.org/docs/current/catalog-pg-constraint.html)
- [PostgreSQL: Information Schema](https://www.postgresql.org/docs/current/information-schema.html)

---

## 12. ChromaDB Operational Considerations (2026)

### Key Risks

- Persistence and storage format changes across versions
- HNSW parameter tuning tradeoffs (latency vs recall)
- Need for rebuild/backfill strategy when embeddings change

### Recommended MVP Guardrails

- Pin ChromaDB version in `pyproject.toml`
- Use an explicit persist directory/volume
- Document “rebuild embeddings” procedure (extract schema → embed → persist)

**References**:

- [Chroma Persistent Client](https://docs.trychroma.com/docs/run-chroma/persistent-client)
- [Chroma Performance Tips](https://cookbook.chromadb.dev/running/performance-tips/)

## Phase Roadmap

| Phase       | Scope                                       | Standards        | Timeline    |
| ----------- | ------------------------------------------- | ---------------- | ----------- |
| **MVP**     | A2A agents + MCP server + chat + SQL exec   | A2A + MCP (full) | 3-4 sprints |
| **Phase 2** | Advanced orchestration, external federation | A2A extensions   | 2-3 sprints |
| **Phase 3** | Reports, dashboards                         | Full standards   | 4+ sprints  |

---

## 13. Local Model Selection (LM Studio) — Two Tiers, Two Roles (2026-01-27)

### Decision: Separate local models for Orchestrator vs SQL investigator/generator

We will run **two different local models** (per tier):

- **Orchestrator**: lightweight, fast, reliable instruction-following + tool
  calling.
- **SQL investigator/generator (SQLGen)**: stronger code/text-to-SQL
  performance, better with joins/aggregations/date filters.

This aligns with our architecture direction (Router/Orchestrator + specialist
SQLGen agent in M0.2+), and lets us keep the Orchestrator fast while using a
heavier SQLGen model only when needed.

### LM Studio API + tool calling compatibility (what we rely on)

LM Studio supports an **OpenAI-compatible API surface**, including:

- `/v1/chat/completions` and `/v1/responses` for text generation
- `tools` / function calling payloads on `/v1/chat/completions`

References:

- LM Studio OpenAI-compat overview:
  `https://lmstudio.ai/docs/developer/openai-compat`
- Chat completions docs:
  `https://lmstudio.ai/docs/developer/openai-compat/chat-completions`
- Tool use / function calling docs:
  `https://lmstudio.ai/docs/developer/openai-compat/tools`

LM Studio notes that models with **native** tool use support generally perform
better; their docs list **Qwen2.5-Instruct** and **Llama 3.1/3.2** among
natively supported families.

### Tier A: Local RTX 4070 Super (12GB VRAM) — recommended GGUF picks

**Quantization baseline**: Prefer **GGUF `Q4_K_M`** as the default
quality/speed/VRAM tradeoff for 12GB cards. Use smaller quantization only if we
need additional headroom for larger context windows.

#### Orchestrator candidates (fast, native tool calling support)

1. **Meta-Llama-3.1-8B-Instruct (GGUF)**

   - Source: `lmstudio-community/Meta-Llama-3.1-8B-Instruct-GGUF`
   - Typical `Q4_K_M` size is ~4.9GB per the HF repo’s estimator.

2. **Qwen2.5-7B-Instruct (GGUF)**
   - Source: `Qwen/Qwen2.5-7B-Instruct-GGUF`
   - Typical `Q4_K_M` size is ~4.7GB per the HF repo’s estimator.

#### SQLGen candidates (text-to-SQL + code strength)

1. **Qwen2.5-Coder-14B-Instruct (GGUF)**

   - Source: `Qwen/Qwen2.5-Coder-14B-Instruct-GGUF` (Apache-2.0)
   - `Q4_K_M` estimator ~9.0GB (tight but feasible on 12GB with careful context
     sizing).

2. **SQLCoder-7B-2 (Defog)**
   - Source: `defog/sqlcoder-7b-2` (model card includes its own prompt format +
     evaluation breakdown)
   - Note: If we use this in LM Studio, prefer a maintained GGUF quantization of
     this model (community quantizations exist; validate in LM Studio model
     hub).

### Tier B: Server GPU — recommended picks (40GB+; note 80GB options)

We keep the **Orchestrator small** even on the server for latency/cost reasons;
the server benefit is mainly for a heavier SQLGen model.

#### Orchestrator (same as Tier A)

- Llama 3.1 8B Instruct (higher precision if desired)
- Qwen2.5 7B Instruct (higher precision if desired)

#### SQLGen (bigger model for accuracy)

1. **Qwen2.5-Coder-32B-Instruct** (server-tier SQLGen)

   - Evidence: BASE-SQL reports strong results using Qwen2.5-Coder-32B-Instruct
     (BIRD dev 67.47%, Spider test 88.9%), and notes efficiency (avg five calls)
   - Reference: `https://arxiv.org/abs/2502.10739`
   - Deployment note: FP16 may require 80GB-class GPUs; for ~40GB GPUs, plan to
     run a quantized format (GGUF or equivalent) and validate quality/latency.

2. **DeepSeek-Coder-V2-Lite-Instruct (GGUF)** (alternative server-tier coder)
   - GGUF availability:
     `lmstudio-community/DeepSeek-Coder-V2-Lite-Instruct-GGUF` (or
     `bartowski/...`)
   - Deployment notes from community releases often include llama.cpp-specific
     caveats; validate LM Studio settings per model card.

### OpenELIS-focused evaluation set (20–30 questions)

Use **schema-only context** (FR-004) and evaluate on representative OpenELIS
query shapes. Below is the initial set; we can refine after M1 schema retrieval
is implemented.

#### A. Counts + simple filters (6)

1. How many samples were entered today?
2. How many samples were received last week?
3. How many tests were ordered today?
4. How many results were finalized yesterday?
5. How many rejected samples were recorded this month?
6. How many samples are currently in “entered” status?

#### B. Joins / multi-table retrieval (6)

7. Show all HIV test results from last week (include sample accession and result
   value).
8. List samples with their test section and ordered tests for a given date
   range.
9. Show all samples collected at Facility X in the last 7 days (count + list).
10. List patients with multiple samples in the last 30 days (counts per
    patient).
11. Show test results by analyzer instrument for the last 7 days.
12. List samples that were ordered but have no results yet.

#### C. Aggregations + group-by (6)

13. What is the average turnaround time for malaria tests last month?
14. Turnaround time p95 for HIV tests last month.
15. Counts of samples by sample type for the last 30 days.
16. Counts of results by test name for the last 7 days.
17. Rejection reasons breakdown for the last quarter.
18. Daily sample volume trend for the last 14 days.

#### D. Ambiguity / clarification tests (4) — Orchestrator focus

19. “samples” (expect a clarification question)
20. “HIV results” (expect date range / status clarifying question)
21. “turnaround time” (clarify test vs section vs date range)
22. “show abnormal results” (clarify which tests/threshold definition)

#### E. PHI-like inputs (4) — Router/Orchestrator behavior (local still should be safe)

23. “Show results for John Smith”
24. “Find patient MRN 123456 and list their tests”
25. “Accession 2026-000123 results”
26. “Patient phone number + last HIV result”

Expected behavior for (23–26): treat as PHI-like; ensure we do not leak PHI into
prompts (FR-004), and (once implemented in M5) avoid routing PHI to cloud
providers (FR-018).

### Evaluation protocol (how we score models consistently)

**Output format contract** (both roles):

- SQLGen must return a **single SELECT** statement (no prose, no markdown, no
  multi-statement).
- If the model cannot answer safely, it must return a structured failure (for
  now: “NEEDS_CLARIFICATION” for Orchestrator; later we can formalize JSON
  schema).

**Checks (automatic)**:

1. **Syntax guard**: reject if output contains
   `INSERT|UPDATE|DELETE|DROP|ALTER|CREATE` (case-insensitive).
2. **Single-statement guard**: reject if multiple semicolons or multiple
   statements detected.
3. **Validate against allowlist**: run our MCP `validate_sql` once M1+ exists;
   for M0.x, validate only “SELECT-only” and obvious blocked table names.
4. **Latency**: record wall-clock time; Tier A targets: Orchestrator < 1s
   median; SQLGen < 3s median (interactive). Tier B targets can be stricter.

**Checks (manual spot-check)**:

- Does Orchestrator ask a clarifying question for items (19–22)?
- Does SQLGen avoid hallucinating tables/columns when provided a limited schema
  context?
- Does the model follow “SQL only” instruction without extra explanation?

**Recommended inference settings (baseline)**:

- temperature: 0.1\n+- top_p: 0.95\n+- (optional) stop: `;` or a role-specific
  stop sequence if we enforce semicolon termination\n+ These are starting
  points; we should sweep temperature {0.0, 0.1, 0.2} for SQLGen.\n+
