# MCP Implementation Research & Best Practices

**Date**: 2026-01-21  
**Feature**: OGC-070 Catalyst Assistant  
**Purpose**: Deep dive into MCP (Model Context Protocol) best practices to
ensure OpenELIS MCP layer follows industry standards

---

## Executive Summary

**Current State**: Our M0.0 implementation uses a basic FastAPI server with
hardcoded tools. This is a valid POC but doesn't leverage the official MCP SDK.

**Recommendation**: Migrate to **FastMCP** (official MCP Python SDK high-level
framework) with **Streamable HTTP transport** for production-ready
implementation.

**Key Findings**:

- ✅ Official SDK: `mcp` package (v1.x stable, v2 pre-alpha Q1 2026)
- ✅ FastMCP: High-level decorator-based API (recommended for most use cases)
- ✅ Streamable HTTP: Current standard (SSE deprecated as of 2025-03-26)
- ⚠️ Current implementation: Custom FastAPI server (works but not
  standards-compliant)

---

## Current Implementation Analysis

### What We Have (M0.0)

```python
# projects/catalyst/catalyst-mcp/src/server.py
from fastapi import FastAPI

def create_app() -> FastAPI:
    app = FastAPI(title="Catalyst MCP", version="0.0.1")
    @app.get("/health")
    async def health() -> dict:
        return {"status": "ok"}
    return app
```

**Issues**:

- ❌ Not using official MCP SDK
- ❌ No MCP protocol compliance (no tool discovery, no proper transport)
- ❌ Hardcoded tool function (`get_schema()`) not exposed as MCP tool
- ❌ No type annotations for structured output
- ❌ No context injection for logging/progress
- ❌ Client-side stub (`mcp_client.py`) doesn't use MCP protocol

---

## Official MCP Python SDK

### Package: `mcp`

**Installation**:

```bash
pip install "mcp[cli]"  # Includes CLI tools and examples
# OR
pip install mcp  # Core package only
```

**Version Strategy**:

- **v1.x**: Stable, production-ready (current recommendation)
- **v2**: Pre-alpha, target stable Q1 2026 (experimental only)

**Source**:
[modelcontextprotocol/python-sdk](https://github.com/modelcontextprotocol/python-sdk)

---

## Recommended Implementation: FastMCP

### Why FastMCP?

1. **High-level decorator API**: Clean, Pythonic tool/resource/prompt
   definitions
2. **Built-in transports**: Streamable HTTP, stdio, SSE (deprecated)
3. **Automatic structured output**: Type annotations → JSON schema validation
4. **Context injection**: Built-in logging, progress reporting, request metadata
5. **Lifespan management**: Async context managers for DB connections, etc.
6. **Production-ready**: Used by Anthropic, OpenAI, and MCP community

### FastMCP Example (Our Use Case)

```python
# projects/catalyst/catalyst-mcp/src/server.py
from mcp.server.fastmcp import FastMCP, Context
from mcp.server.session import ServerSession
from typing import Annotated

# Create MCP server instance
mcp = FastMCP(
    name="Catalyst Schema Server",
    version="0.0.1",
    json_response=True  # Structured JSON responses
)

@mcp.tool()
def get_schema() -> str:
    """
    Get database schema (table names).

    Returns a newline-separated list of table names in the OpenELIS database.
    For M0.0, returns hardcoded list. Future milestones will use RAG-based retrieval.
    """
    return "\n".join([
        "sample",
        "test",
        "analysis",
        "patient",
        "organization",
    ])

@mcp.tool()
async def get_relevant_tables(
    query: Annotated[str, "Natural language query to find relevant tables"],
    ctx: Context[ServerSession, None]
) -> list[str]:
    """
    Get relevant database tables for a query using RAG-based retrieval.

    This tool will be implemented in M1 (RAG-based schema milestone).
    For now, returns all tables.
    """
    await ctx.info(f"Finding relevant tables for query: {query}")
    # M0.0: Return all tables (hardcoded)
    # M1: Implement ChromaDB RAG retrieval
    return ["sample", "test", "analysis", "patient", "organization"]

@mcp.tool()
async def get_table_ddl(
    table_name: Annotated[str, "Name of the table"],
    ctx: Context[ServerSession, None]
) -> str:
    """
    Get DDL (Data Definition Language) for a specific table.

    Returns CREATE TABLE statement with columns, types, constraints.
    """
    await ctx.info(f"Retrieving DDL for table: {table_name}")
    # M0.0: Return placeholder
    # M1: Extract from PostgreSQL pg_catalog
    return f"-- DDL for {table_name} (placeholder for M0.0)"

@mcp.tool()
async def validate_sql(
    sql: Annotated[str, "SQL query to validate"],
    user_query: Annotated[str, "Original user query for context"],
    ctx: Context[ServerSession, None]
) -> dict[str, str | bool | int]:
    """
    Validate SQL query before execution.

    Performs:
    - Syntax validation
    - Blocked table detection
    - Row estimation (EXPLAIN-based)

    Returns structured validation result.
    """
    await ctx.info(f"Validating SQL: {sql[:50]}...")
    # M0.0: Basic validation (placeholder)
    # M2: Full validation with SQL parser
    return {
        "valid": True,
        "syntax_ok": True,
        "blocked_tables": [],
        "estimated_rows": 0,
        "warnings": []
    }

if __name__ == "__main__":
    # Run with Streamable HTTP transport (recommended)
    mcp.run(
        transport="streamable-http",
        host="0.0.0.0",
        port=9102
    )
```

### Key Features Used

1. **Type Annotated Parameters**: `Annotated[str, "description"]` provides tool
   parameter descriptions
2. **Return Type Annotations**: `-> dict[str, str | bool | int]` enables
   automatic structured output
3. **Context Injection**: `ctx: Context[ServerSession, None]` for logging and
   progress
4. **Docstrings**: Tool descriptions extracted from function docstrings
5. **Transport**: `streamable-http` (current standard, replaces deprecated SSE)

---

## MCP Client Implementation

### Current Stub (Needs Replacement)

```python
# projects/catalyst/catalyst-agents/src/mcp_client.py (CURRENT - STUB)
def get_schema() -> str:
    # TODO: Call MCP server over HTTP in later milestones.
    return "sample\nanalysis\npatient\norganization\ntest"
```

### Recommended: Official MCP Client

```python
# projects/catalyst/catalyst-agents/src/mcp_client.py
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client
import httpx
from typing import Optional

class MCPClient:
    """Client for calling Catalyst MCP server tools."""

    def __init__(self, mcp_url: str):
        self.mcp_url = mcp_url
        self._session: Optional[ClientSession] = None

    async def __aenter__(self):
        # For HTTP transport, use httpx client
        # For stdio, use stdio_client
        # Streamable HTTP requires proper session management
        return self

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self._session:
            await self._session.__aexit__(exc_type, exc_val, exc_tb)

    async def get_schema(self) -> str:
        """Call MCP get_schema tool."""
        # M0.0: Use HTTP client for Streamable HTTP transport
        # Future: Use official MCP client SDK
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.mcp_url}/mcp",
                json={
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "tools/call",
                    "params": {
                        "name": "get_schema",
                        "arguments": {}
                    }
                },
                headers={
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                    "MCP-Protocol-Version": "2025-11-25"
                }
            )
            result = response.json()
            return result["result"]["content"][0]["text"]

    async def get_relevant_tables(self, query: str) -> list[str]:
        """Call MCP get_relevant_tables tool."""
        # Similar implementation
        pass

# Singleton instance
_mcp_client: Optional[MCPClient] = None

async def get_mcp_client(mcp_url: str) -> MCPClient:
    """Get or create MCP client instance."""
    global _mcp_client
    if _mcp_client is None:
        _mcp_client = MCPClient(mcp_url)
    return _mcp_client

# Convenience function for M0.0 (synchronous stub)
def get_schema() -> str:
    """Synchronous wrapper (M0.0 compatibility)."""
    # M0.0: Return hardcoded (current behavior)
    # M1+: Use async MCP client
    return "sample\nanalysis\npatient\norganization\ntest"
```

**Note**: For M0.0, we can keep the synchronous stub. For M1+, migrate to async
MCP client.

---

## Transport: Streamable HTTP (Recommended)

### Why Streamable HTTP?

- ✅ **Current Standard**: Replaces deprecated SSE transport (as of 2025-03-26)
- ✅ **Unified Endpoint**: Single POST endpoint for all requests
- ✅ **Batch + Streaming**: Supports both JSON (batch) and SSE (streaming)
  responses
- ✅ **Session Support**: Optional session IDs for resumable connections
- ✅ **Production-Ready**: Better for remote deployments, load balancing

### Streamable HTTP Requirements

1. **Protocol Version Header**: `MCP-Protocol-Version: 2025-11-25`
2. **Session ID Header**: `MCP-Session-Id: <uuid>` (optional, for stateful
   sessions)
3. **Accept Header**: `Accept: application/json` (batch) or `text/event-stream`
   (streaming)
4. **Unified Endpoint**: POST to `/mcp` for all JSON-RPC requests
5. **GET Endpoint**: GET with `Accept: text/event-stream` to open SSE stream

### FastMCP Streamable HTTP Example

```python
# FastMCP automatically handles Streamable HTTP when you use:
mcp.run(transport="streamable-http", host="0.0.0.0", port=9102)
```

**FastMCP handles**:

- Protocol version headers
- Session management
- JSON-RPC request/response
- SSE streaming (if requested)
- Error handling

---

## Best Practices Checklist

### ✅ Security

- [ ] **Authentication**: Use Bearer tokens for MCP server access
- [ ] **HTTPS Only**: Always use HTTPS in production (never HTTP)
- [ ] **Input Validation**: Validate all tool parameters (Pydantic models
      recommended)
- [ ] **Rate Limiting**: Protect upstream dependencies (database, APIs)
- [ ] **Guardrails**: Blocked table detection, SQL injection prevention

### ✅ Performance

- [ ] **Async I/O**: Use `async def` for network-bound tools
- [ ] **Caching**: Cache tool lists, resource content (if safe)
- [ ] **Batching**: Batch compatible requests when possible
- [ ] **Timeouts**: Set timeouts for all external calls
- [ ] **Connection Pooling**: Reuse database connections (lifespan context)

### ✅ Observability

- [ ] **Health Endpoints**: `/health` and `/readiness` (FastMCP provides these)
- [ ] **Structured Logging**: Use `ctx.info()`, `ctx.debug()`, `ctx.error()`
- [ ] **Request IDs**: Correlation IDs for tracing (available via `ctx`)
- [ ] **Metrics**: Track latency, errors, tool usage
- [ ] **Progress Reporting**: `ctx.report_progress()` for long-running tools

### ✅ Testing

- [ ] **Contract Tests**: Validate tool schemas match implementations
- [ ] **Integration Tests**: Test MCP client → server flow
- [ ] **Security Tests**: Test unauthorized access, malformed inputs
- [ ] **Performance Tests**: Load testing for remote deployments

### ✅ Code Quality

- [ ] **Type Annotations**: All tool parameters and return types annotated
- [ ] **Docstrings**: Clear descriptions for tools, parameters, return values
- [ ] **Error Handling**: Proper exception handling with structured errors
- [ ] **Versioning**: Version MCP server and tool schemas

---

## Migration Plan

### Phase 1: M0.0 → FastMCP Migration (Immediate)

**Goal**: Replace custom FastAPI server with FastMCP while keeping M0.0
functionality.

**Tasks**:

1. Update `pyproject.toml` to use `mcp` package (already have `mcp = "^1.1.2"`)
2. Replace `src/server.py` with FastMCP implementation
3. Convert `get_schema()` to `@mcp.tool()` decorator
4. Update tests to use MCP protocol
5. Update `mcp_client.py` to call MCP server properly (or keep stub for M0.0)

**Estimated Effort**: 2-3 hours

### Phase 2: M1 → Full MCP Tools (RAG Schema)

**Goal**: Implement all planned MCP tools with RAG-based schema retrieval.

**Tasks**:

1. Implement `get_relevant_tables()` with ChromaDB RAG
2. Implement `get_table_ddl()` with PostgreSQL introspection
3. Implement `get_relationships()` for FK relationships
4. Implement `validate_sql()` with SQL parser
5. Add proper async MCP client in agents

**Estimated Effort**: 3-4 days (M1 milestone)

### Phase 3: Production Hardening

**Goal**: Add security, observability, performance optimizations.

**Tasks**:

1. Add authentication (Bearer tokens)
2. Add rate limiting
3. Add structured logging
4. Add metrics collection
5. Add connection pooling
6. Add comprehensive error handling

**Estimated Effort**: 2-3 days

---

## Comparison: Current vs. Recommended

| Aspect                  | Current (M0.0)       | Recommended (FastMCP)        |
| ----------------------- | -------------------- | ---------------------------- |
| **Framework**           | Custom FastAPI       | FastMCP (official SDK)       |
| **Tool Definition**     | Plain function       | `@mcp.tool()` decorator      |
| **Transport**           | Custom HTTP          | Streamable HTTP (standard)   |
| **Type Safety**         | No annotations       | Full type annotations        |
| **Structured Output**   | Manual JSON          | Automatic (from types)       |
| **Logging**             | Print statements     | `ctx.info()`, `ctx.debug()`  |
| **Progress**            | Not supported        | `ctx.report_progress()`      |
| **Protocol Compliance** | ❌ Not MCP-compliant | ✅ Full MCP spec compliance  |
| **Client SDK**          | Stub function        | Official MCP client (future) |

---

## References

### Official Documentation

- **MCP Specification**: https://modelcontextprotocol.io/specification/
- **MCP Python SDK**: https://github.com/modelcontextprotocol/python-sdk
- **MCP Python Docs**: https://modelcontextprotocol.io/docs/python
- **Streamable HTTP Transport**:
  https://modelcontextprotocol.io/specification/2025-11-25/basic/transports

### Best Practices

- **MCP Best Practices Guide**:
  https://mcp-best-practice.github.io/mcp-best-practice/best-practice/
- **Security Research**:
  - Prompt Injection: https://arxiv.org/abs/2506.13538
  - Cryptography Misuse: https://arxiv.org/abs/2512.03775

### Example Servers

- **Official Examples**: https://github.com/modelcontextprotocol/servers
- **Community Servers**: Various implementations in Python, TypeScript

---

## Recommendations

### Immediate (M0.0)

1. ✅ **Keep current stub for M0.0** - It works for POC validation
2. ⚠️ **Document limitation** - Note that M0.0 uses custom FastAPI, not MCP SDK
3. 📋 **Plan migration** - Add FastMCP migration to M1 tasks

### Short-term (M1)

1. **Migrate to FastMCP** - Replace custom server with FastMCP
2. **Implement all tools** - `get_relevant_tables`, `get_table_ddl`,
   `get_relationships`, `validate_sql`
3. **Add async MCP client** - Replace stub with proper MCP client
4. **Add conformance tests** - Validate MCP protocol compliance

### Long-term (Production)

1. **Add authentication** - Bearer token validation
2. **Add observability** - Structured logging, metrics
3. **Add performance optimizations** - Caching, connection pooling
4. **Add security hardening** - Rate limiting, input validation, guardrails

---

## Conclusion

Our current M0.0 implementation is a **valid POC** but not MCP-compliant. For
production readiness, we should migrate to **FastMCP** with **Streamable HTTP
transport** in M1. This aligns with industry best practices and ensures
compatibility with MCP ecosystem tools and clients.

**Next Steps**:

1. Review this research document
2. Decide on migration timeline (M1 vs. later)
3. Update M1 tasks to include FastMCP migration
4. Consider adding FastMCP migration as separate task in M0.0 if time permits
