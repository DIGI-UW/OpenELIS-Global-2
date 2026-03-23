"""
Integration tests for multi-agent flow: Router → SchemaAgent → SQLGenAgent.

These tests verify that the RouterAgent correctly orchestrates the
multi-agent workflow when in multi-agent mode.
"""

import pytest
from a2a.types import Part, TextPart

from src.agents.router_executor import RouterAgentExecutor


@pytest.mark.asyncio
async def test_router_calls_schema_agent_then_sqlgen_agent(monkeypatch):
    """
    Test that RouterAgent orchestrates SchemaAgent → SQLGenAgent flow.

    Verifies:
    - Router calls SchemaAgent first
    - Router calls SQLGenAgent second
    - Router returns SQLGenAgent's response
    """
    schema_called = False
    sqlgen_called = False
    call_order = []

    async def mock_call_agent(self, agent_url: str, query: str):
        nonlocal schema_called, sqlgen_called, call_order

        if "9102" in agent_url:  # SchemaAgent port
            schema_called = True
            call_order.append("schema")
            # Return mock schema context
            return [Part(root=TextPart(text="{'tables': ['sample'], 'schema': 'sample(id)'}"))]
        if "9103" in agent_url:  # SQLGenAgent port
            sqlgen_called = True
            call_order.append("sqlgen")
            # Return mock SQL
            return [Part(root=TextPart(text="SELECT COUNT(*) FROM sample"))]

        return [Part(root=TextPart(text="Unknown agent"))]

    # Patch _call_agent method
    monkeypatch.setattr(RouterAgentExecutor, "_call_agent", mock_call_agent)

    executor = RouterAgentExecutor()
    executor.mode = "multi"  # Force multi-agent mode

    result = await executor.delegate_query_multi_agent("count samples")

    # Verify both agents were called
    assert schema_called, "SchemaAgent should have been called"
    assert sqlgen_called, "SQLGenAgent should have been called"

    # Verify call order: SchemaAgent first, then SQLGenAgent
    assert call_order == ["schema", "sqlgen"], f"Expected ['schema', 'sqlgen'], got {call_order}"

    # Verify result is from SQLGenAgent
    assert len(result) > 0
    assert "SELECT" in result[0].root.text


@pytest.mark.asyncio
async def test_router_multi_agent_mode_returns_sql(monkeypatch):
    """
    Test that multi-agent mode returns SQL from SQLGenAgent.
    """

    async def mock_call_agent(self, agent_url: str, query: str):
        if "9102" in agent_url:  # SchemaAgent
            return [Part(root=TextPart(text="{'tables': ['test'], 'schema': 'test(id)'}"))]
        if "9103" in agent_url:  # SQLGenAgent
            return [Part(root=TextPart(text="SELECT * FROM test"))]
        return []

    monkeypatch.setattr(RouterAgentExecutor, "_call_agent", mock_call_agent)

    executor = RouterAgentExecutor()
    executor.mode = "multi"

    result = await executor.delegate_query_multi_agent("get all tests")

    assert len(result) > 0
    assert "SELECT * FROM test" in result[0].root.text
