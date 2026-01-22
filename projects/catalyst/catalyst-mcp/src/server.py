import os

from mcp.server.fastmcp import FastMCP

from .tools import schema_tools

host = os.getenv("MCP_HOST", "0.0.0.0")
port = int(os.getenv("MCP_PORT", "9102"))

mcp = FastMCP(
    "Catalyst Schema Server",
    host=host,
    port=port,
    json_response=True,
)


@mcp.tool()
def get_query_context(user_query: str) -> dict[str, object]:
    """
    Get query context (schema bundle) for allowed tables only.
    
    Provides the LLM with schema information (columns, primary keys, foreign keys)
    for tables in the allowlist, enabling accurate SQL generation within safe boundaries.
    """
    return schema_tools.get_query_context(user_query)


@mcp.tool()
def validate_sql(sql: str) -> dict[str, object]:
    """
    Validate SQL against guardrails and allowlist.
    
    Ensures generated SQL:
    - Uses only SELECT/WITH (no DDL/DML)
    - References only allowlisted tables
    - Provides warnings for potential issues (missing LIMIT, SELECT *)
    """
    return schema_tools.validate_sql(sql)


if __name__ == "__main__":
    mcp.run(transport="streamable-http")
