# Research: Catalyst - LLM-Powered Lab Data Assistant

**Feature**: OGC-070-catalyst-assistant  
**Date**: 2026-01-20  
**Status**: Complete

## Executive Summary

This document consolidates research findings for implementing Catalyst, an LLM-powered data assistant for OpenELIS Global. The research covers text-to-SQL approaches, LLM integration options, frontend components, and standards-based AI architecture for future phases.

---

## 1. Text-to-SQL Approach

### Decision: RAG-Based Schema Retrieval via MCP (Updated 2026-01-21)

**Rationale**: Modern text-to-SQL requires providing the LLM with relevant schema context. The OpenELIS schema is too large for a single prompt context window. A RAG approach with MCP standards validation was chosen for MVP.

**Alternatives Considered**:

| Approach | Accuracy | Complexity | Chosen? |
|----------|----------|------------|--------|
| Zero-shot (full schema in prompt) | 60-65% | Low | ❌ Schema too large |
| Static curated subset | 65-70% | Low | ❌ Limits query scope |
| Schema RAG (vector search for relevant tables) | 70-75% | Medium | ✅ MVP |
| Fine-tuned model on OpenELIS schema | 80%+ | High | Future |

**Implementation for MVP**: 
- Python MCP server with RAG-based schema retrieval
- ChromaDB for embedding storage and similarity search
- MCP tools: `get_relevant_tables`, `get_table_ddl`, `get_relationships`
- Java backend calls MCP server via Streamable HTTP transport (SSE optional for streaming)

**References**:
- [SQLCoder GitHub](https://github.com/defog-ai/sqlcoder) - State-of-the-art text-to-SQL model
- [Vanna.ai Training Approach](https://vanna.ai/docs/postgres-openai-standard-vannadb.html) - RAG-based SQL generation
- [Text-to-SQL Comparison 2026](https://research.aimultiple.com/text-to-sql/) - Model benchmarks

---

## 2. LLM Provider Selection

### Decision: LangChain4j Core Modules with Provider Switching

**Rationale**: LangChain4j provides a unified Java API across providers. Using core modules (not Spring Boot starters) aligns with OpenELIS's Traditional Spring MVC architecture.

**Provider Comparison**:

| Provider | Latency | Cost | Privacy | SQL Accuracy | Best For |
|----------|---------|------|---------|--------------|----------|
| GPT-4o (OpenAI) | 500-1000ms | $0.01-0.03/query | Data leaves network | 72% | Fast development iteration |
| Claude Sonnet (Anthropic) | 500-1000ms | $0.01-0.03/query | Data leaves network | 70% | Complex reasoning |
| SQLCoder-7B (Ollama) | 100-300ms | Hardware only | Fully air-gapped | 70%+ | Privacy-sensitive production |
| Llama 3.2 3B (Ollama) | 100-200ms | Hardware only | Fully air-gapped | 65% | Fallback/explanation |

**Recommended Strategy**:
- **Development**: Cloud APIs (OpenAI/Anthropic) for rapid iteration
- **Production**: SQLCoder-7B via Ollama for privacy compliance

**Dependencies** (pom.xml):
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>1.10.0</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>1.10.0</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>1.10.0</version>
</dependency>
```

**References**:
- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [Ollama Documentation](https://ollama.com/)

---

## 3. Local LLM Infrastructure

### Decision: Ollama with Docker Compose for Development

**Rationale**: Ollama provides easy model management and Docker integration. GPU support via NVIDIA Container Toolkit.

**Setup**:
```yaml
# catalyst-dev.docker-compose.yml
services:
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

**Model Selection**:
- **SQLCoder-7B** (defog/sqlcoder-7b-2): Best for SQL generation, ~6GB VRAM with Q4 quantization
- **Llama 3.2 3B**: Lightweight fallback for reasoning/explanation

**Alternatives Considered**:
- LM Studio: Better UI for model management, but less Docker-friendly
- vLLM: Higher performance but more complex setup
- llama.cpp: Lowest level, most flexible, but requires more configuration

**References**:
- [SQLCoder on Ollama](https://ollama.com/library/sqlcoder:7b)
- [LM Studio](https://lmstudio.ai/)

---

## 4. Frontend Chat Component

### Decision: @carbon/ai-chat v1.0

**Rationale**: IBM's official Carbon AI Chat library provides Constitution Principle II compliance out of the box.

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

**Caveat**: SSR not supported - client-side rendering only (acceptable for OpenELIS SPA)

**References**:
- [Carbon AI Chat Documentation](https://chat.carbondesignsystem.com/)
- [Carbon for AI Guidelines](https://carbondesignsystem.com/guidelines/carbon-for-ai/)

---

## 5. Privacy Architecture

### Decision: Schema-Only LLM Context

**Rationale**: Non-negotiable requirement from spec. LLM receives only metadata, never patient data.

**Implementation**:
1. **Schema Context Generation**: Extract DDL from `information_schema.columns`
2. **Prompt Construction**: Include only schema + user question
3. **SQL Execution**: Separate step, LLM never sees results
4. **Read-Only Connection**: Dedicated PostgreSQL user with SELECT-only permissions

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

**What**: Anthropic's standard for LLM-tool integration, adopted by OpenAI (March 2025).

**Why for Catalyst MVP** (Updated 2026-01-21):
- Validate standards-based architecture early
- Standardize schema access as MCP tools
- Enable RAG-based schema retrieval at scale
- Prepare for future A2A integration

**MVP Implementation**: Python MCP Server (Official SDK)
```python
# pyproject.toml
dependencies = [
    "mcp>=1.0.0",
    "chromadb>=0.4.0",
    "langchain>=0.1.0",
    "psycopg2-binary>=2.9.0",
]
```

**Java Client**: MCP Java SDK for backend integration
```xml
<dependency>
    <groupId>io.modelcontextprotocol</groupId>
    <artifactId>mcp-java-sdk</artifactId>
    <version>0.8.0</version>
</dependency>
```

**MCP Tools for MVP**:
- `get_relevant_tables(query: str) -> list[str]` - RAG-based table retrieval
- `get_table_ddl(table_name: str) -> str` - DDL extraction
- `get_relationships(table_names: list[str]) -> list[dict]` - FK relationships

**References**:
- [MCP Documentation](https://modelcontextprotocol.io/)
- [MCP Python SDK](https://github.com/modelcontextprotocol/python-sdk)
- [MCP Java SDK](https://github.com/modelcontextprotocol/java-sdk)

### A2A Protocol (Agent2Agent) - Phase 2

**What**: Google's open protocol for AI agent interoperability, donated to Linux Foundation (April 2025).

**Why for Catalyst Phase 2**:
- Enable multi-agent orchestration (Router → SQL Generator → Validator)
- Support external AI system collaboration
- Standardized authentication and task delegation

**Java Implementation**: [a2ajava](https://github.com/vishalmysore/a2ajava)
```xml
<dependency>
    <groupId>io.github.vishalmysore</groupId>
    <artifactId>a2ajava</artifactId>
    <version>0.0.9</version>
</dependency>
```

**References**:
- [A2A Official Documentation](https://google.github.io/A2A/)
- [A2A Specification](https://google.github.io/A2A/specification/)

### A2A + MCP Relationship

| Protocol | Layer | Purpose | Catalyst Phase |
|----------|-------|---------|----------------|
| **MCP** | Vertical | Agent-to-Tool | MVP (schema retrieval) |
| **A2A** | Horizontal | Agent-to-Agent | Phase 2 (multi-agent) |

---

## 8. Google HAI-DEF Patterns

### Relevance to Catalyst

Google's Health AI Developer Foundations (HAI-DEF) demonstrate agentic patterns applicable to healthcare AI:

**TxGemma Agentic-Tx Pattern**:
- Cognitive Orchestrator (Gemini) → Catalyst Router Agent
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

**Repository**: [github.com/pmanko/med-agent-hub](https://github.com/pmanko/med-agent-hub)

### pmanko/omrs-ai-playground

Healthcare AI research platform with OpenMRS integration.

**Relevant Patterns**:
- LLM provider abstraction
- Healthcare-specific prompt engineering
- FHIR-aware context construction

**Repository**: [github.com/pmanko/omrs-ai-playground](https://github.com/pmanko/omrs-ai-playground)

---

## Open Questions Resolved

| Question | Decision | Rationale |
|----------|----------|----------|
| Which LLM framework? | LangChain4j core modules | Java-native, provider-agnostic, no Spring Boot dependency |
| Cloud vs Local? | Both (configurable) | Cloud for dev speed, local for production privacy |
| Which chat component? | @carbon/ai-chat | Carbon compliance, official IBM support |
| MCP in MVP? | Yes (Python server) | Validate standards early, support full schema via RAG |
| Which LLM providers? | OpenAI, Gemini, Ollama, LM Studio | Cloud + local coverage, OpenAI-compatible API for LM Studio |
| Schema handling? | RAG via ChromaDB | Full clinical schema too large for context window |
| SQL validation? | Multi-layer guardrails | Defense in depth for security |

---

## Phase Roadmap

| Phase | Scope | Standards | Timeline |
|-------|-------|-----------|----------|
| **MVP** | MCP server + chat + SQL execution | MCP (schema retrieval) | 2-3 sprints |
| **Phase 2** | Multi-agent refactor | A2A + MCP (full) | 2-3 sprints |
| **Phase 3** | Reports, dashboards | Full standards | 4+ sprints |
