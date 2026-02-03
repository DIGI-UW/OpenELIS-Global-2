"""
Tests for single-agent fallback mode.

These tests verify that RouterAgent can fall back to using CatalystAgent
directly when CATALYST_AGENT_MODE=single.
"""

import pytest
from a2a.types import Part, TextPart

from src.agents.router_executor import RouterAgentExecutor


@pytest.mark.asyncio
async def test_router_single_agent_mode_calls_catalyst_agent(monkeypatch):
    """
    Test that RouterAgent calls CatalystAgent directly in single-agent mode.

    Verifies:
    - Router delegates to CatalystAgent URL (port 9101)
    - Router does NOT call SchemaAgent or SQLGenAgent
    - Router returns CatalystAgent's response
    """
    catalyst_called = False
    schema_called = False
    sqlgen_called = False

    async def mock_call_agent(self, agent_url: str, query: str):
        nonlocal catalyst_called, schema_called, sqlgen_called

        if "9101" in agent_url:  # CatalystAgent port
            catalyst_called = True
            return [Part(root=TextPart(text="SELECT 1"))]
        if "9102" in agent_url:  # SchemaAgent port
            schema_called = True
            return []
        if "9103" in agent_url:  # SQLGenAgent port
            sqlgen_called = True
            return []

        return []

    monkeypatch.setattr(RouterAgentExecutor, "_call_agent", mock_call_agent)

    executor = RouterAgentExecutor()
    executor.mode = "single"  # Force single-agent mode

    result = await executor.delegate_query_single_agent("count samples")

    # Verify only CatalystAgent was called
    assert catalyst_called, "CatalystAgent should have been called"
    assert not schema_called, "SchemaAgent should NOT have been called in single mode"
    assert not sqlgen_called, "SQLGenAgent should NOT have been called in single mode"

    # Verify result is from CatalystAgent
    assert len(result) > 0
    assert result[0].root.text == "SELECT 1"


@pytest.mark.asyncio
async def test_router_mode_switch_via_env_var(monkeypatch):
    """
    Test that RouterAgent mode can be controlled via CATALYST_AGENT_MODE env var.

    Default is single so dev environments without specialist agents work out of the box.
    """
    # Default (no env) is single
    executor_default = RouterAgentExecutor()
    assert executor_default.mode == "single", "Default mode should be 'single'"

    # Multi mode via env var
    monkeypatch.setenv("CATALYST_AGENT_MODE", "multi")
    executor_multi = RouterAgentExecutor()
    assert executor_multi.mode == "multi", "Mode should be 'multi' when env var set"
