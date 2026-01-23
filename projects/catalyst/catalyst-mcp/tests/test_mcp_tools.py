import os
import socket
import subprocess
import sys
import time

import pytest
from mcp.client.session import ClientSession
from mcp.client.streamable_http import streamable_http_client
from mcp.types import Implementation

from src.tools import schema_tools


def _find_free_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def _wait_for_port(port: int, timeout_seconds: float = 5.0) -> bool:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        try:
            with socket.create_connection(("127.0.0.1", port), timeout=0.2):
                return True
        except OSError:
            time.sleep(0.1)
    return False


def test_get_query_context_returns_allowed_tables_only():
    """Test that get_query_context returns only allowlisted tables."""
    context = schema_tools.get_query_context("find tests")
    
    assert "allowed_tables" in context
    assert "tables" in context
    assert isinstance(context["allowed_tables"], list)
    assert isinstance(context["tables"], dict)
    
    # Verify all returned tables are in allowed list
    for table_name in context["tables"].keys():
        assert table_name in context["allowed_tables"]
    
    # Verify default minimal non-PHI profile
    default_allowed = [
        "test", "test_section", "analyte", "method", "panel", "panel_item",
        "type_of_sample", "type_of_test_result", "unit_of_measure",
        "dictionary", "dictionary_category", "status_of_sample",
    ]
    for table in default_allowed:
        assert table in context["allowed_tables"]


def test_get_query_context_includes_schema_info():
    """Test that get_query_context includes columns, PKs, FKs for tables."""
    context = schema_tools.get_query_context("show test schema")
    
    assert "test" in context["tables"]
    test_table = context["tables"]["test"]
    
    assert "columns" in test_table
    assert "primary_key" in test_table
    assert "foreign_keys" in test_table
    
    # Verify test table has expected structure
    assert len(test_table["columns"]) > 0
    assert "id" in test_table["primary_key"]
    assert any(col["name"] == "id" for col in test_table["columns"])


def test_validate_sql_allows_select():
    """Test that validate_sql allows SELECT queries on allowed tables."""
    result = schema_tools.validate_sql("SELECT * FROM test LIMIT 10")
    
    assert result["valid"] is True
    assert len(result["errors"]) == 0
    assert "test" in result["referenced_tables"]


def test_validate_sql_rejects_non_allowed_tables():
    """Test that validate_sql rejects queries referencing non-allowed tables."""
    result = schema_tools.validate_sql("SELECT * FROM patient LIMIT 10")
    
    assert result["valid"] is False
    assert len(result["errors"]) > 0
    assert any("non-allowed" in err.lower() for err in result["errors"])


def test_validate_sql_rejects_ddl_dml():
    """Test that validate_sql rejects DDL/DML operations."""
    result = schema_tools.validate_sql("DELETE FROM test")
    
    assert result["valid"] is False
    assert any("blocked" in err.lower() for err in result["errors"])
    
    result = schema_tools.validate_sql("DROP TABLE test")
    assert result["valid"] is False
    
    result = schema_tools.validate_sql("UPDATE test SET name = 'x'")
    assert result["valid"] is False


def test_validate_sql_warns_missing_limit():
    """Test that validate_sql warns about missing LIMIT clause."""
    result = schema_tools.validate_sql("SELECT * FROM test")
    
    assert any("limit" in w.lower() for w in result["warnings"])


def test_validate_sql_allows_with_cte():
    """Test that validate_sql allows WITH (CTE) queries."""
    result = schema_tools.validate_sql(
        "WITH test_cte AS (SELECT * FROM test LIMIT 10) SELECT * FROM test_cte"
    )
    
    assert result["valid"] is True


@pytest.mark.asyncio
async def test_mcp_tools_list_and_call():
    """Test MCP protocol compliance: list tools and call get_query_context."""
    port = _find_free_port()
    env = os.environ.copy()
    env["MCP_HOST"] = "127.0.0.1"
    env["MCP_PORT"] = str(port)

    process = subprocess.Popen(
        [sys.executable, "-m", "src.server"],
        cwd=os.path.dirname(os.path.dirname(__file__)),
        env=env,
    )
    try:
        assert _wait_for_port(port), "MCP server did not start in time."

        url = f"http://127.0.0.1:{port}/mcp"
        async with streamable_http_client(url) as (read_stream, write_stream, _):
            client_info = Implementation(name="CatalystTestClient", version="0.0.1")
            async with ClientSession(read_stream, write_stream, client_info=client_info) as session:
                await session.initialize()
                tools_result = await session.list_tools()
                tool_names = {tool.name for tool in tools_result.tools}
                
                # Verify only the two expected tools are present
                required_tools = {"get_query_context", "validate_sql"}
                assert required_tools.issubset(tool_names), f"Expected {required_tools}, got {tool_names}"
                
                # Verify old tools are not present
                old_tools = {"get_schema", "get_relevant_tables", "get_table_ddl", "get_relationships"}
                assert not old_tools.intersection(tool_names), f"Old tools should not be present: {old_tools.intersection(tool_names)}"
                
                # Test get_query_context
                result = await session.call_tool("get_query_context", {"user_query": "find tests"})
                assert result.content, "Expected content in tool response."
                assert len(result.content) > 0
                
                # Test validate_sql
                result = await session.call_tool("validate_sql", {"sql": "SELECT * FROM test LIMIT 10"})
                assert result.content, "Expected content in tool response."
    finally:
        process.terminate()
        process.wait(timeout=5)
