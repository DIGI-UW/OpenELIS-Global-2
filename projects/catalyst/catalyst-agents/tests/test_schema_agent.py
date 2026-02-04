"""
Tests for SchemaAgent - specialized agent for schema retrieval via MCP.

SchemaAgent calls MCP get_query_context to retrieve relevant schema metadata
based on the user's natural language query.
"""

from src import mcp_client
from src.agents import schema_executor


def test_schema_executor_calls_mcp_get_query_context(monkeypatch):
    """
    Test that SchemaAgent calls MCP get_query_context and returns schema context.

    This test verifies:
    - SchemaAgent accepts a user query
    - SchemaAgent calls MCP get_query_context with the query
    - SchemaAgent returns relevant schema metadata
    """

    # Mock MCP get_query_context to return sample schema
    def mock_get_query_context(query: str) -> dict:
        return {
            "tables": ["sample", "analysis"],
            "schema": "sample(id, entered_date)\nanalysis(id, sample_id, test_name)",
        }

    monkeypatch.setattr(mcp_client, "get_query_context", mock_get_query_context)

    # Call schema executor
    result = schema_executor.get_schema_context("count samples today")

    # Verify result contains schema context
    assert "tables" in result
    assert "sample" in result["tables"]
    assert "analysis" in result["tables"]
    assert "schema" in result
    assert "sample(id, entered_date)" in result["schema"]


def test_schema_executor_passes_query_to_mcp(monkeypatch):
    """
    Test that SchemaAgent passes the user query to MCP for context retrieval.

    This ensures the MCP server can use the query semantics to retrieve
    relevant tables via RAG (in M1+).
    """
    captured_query = None

    def mock_get_query_context(query: str) -> dict:
        nonlocal captured_query
        captured_query = query
        return {"tables": ["test"], "schema": "test(id, name)"}

    monkeypatch.setattr(mcp_client, "get_query_context", mock_get_query_context)

    # Call with specific query
    user_query = "What tests are available?"
    schema_executor.get_schema_context(user_query)

    # Verify query was passed to MCP
    assert captured_query == user_query


def test_schema_executor_returns_empty_on_no_tables(monkeypatch):
    """
    Test SchemaAgent handles case where MCP returns no relevant tables.
    """

    def mock_get_query_context(query: str) -> dict:
        return {"tables": [], "schema": ""}

    monkeypatch.setattr(mcp_client, "get_query_context", mock_get_query_context)

    result = schema_executor.get_schema_context("unknown query")

    assert "tables" in result
    assert len(result["tables"]) == 0
    assert "schema" in result
    assert result["schema"] == ""
