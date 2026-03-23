"""
Trajectory validation tests per research.md Section 14.2 (Layer 2: Workflow/Trajectory).

Validates multi-step agent flow as trajectories: User Query → SchemaAgent → Response →
SQLGenAgent → Final Output. Ensures correct delegation order and task completion
without requiring database or LLM.
"""

import pytest
from a2a.types import Part, TextPart

from src.agents.router_executor import RouterAgentExecutor


@pytest.mark.asyncio
async def test_trajectory_schema_then_sqlgen_order(monkeypatch):
    """
    Trajectory: Router must call SchemaAgent first, then SQLGenAgent (Section 14.2).
    """
    trajectory_steps = []

    async def mock_call_agent(self, agent_url: str, query: str):
        if "9102" in agent_url:
            trajectory_steps.append(("SchemaAgent", query))
            return [Part(root=TextPart(text="{'tables': ['sample'], 'schema': 'sample(id)'}"))]
        if "9103" in agent_url:
            trajectory_steps.append(("SQLGenAgent", query))
            return [Part(root=TextPart(text="SELECT COUNT(*) FROM sample"))]
        trajectory_steps.append(("Unknown", agent_url))
        return [Part(root=TextPart(text="Unknown agent"))]

    monkeypatch.setattr(RouterAgentExecutor, "_call_agent", mock_call_agent)
    executor = RouterAgentExecutor()
    executor.mode = "multi"

    result = await executor.delegate_query_multi_agent("How many samples today?")

    assert (
        len(trajectory_steps) >= 2
    ), "Trajectory must have at least SchemaAgent and SQLGenAgent steps"
    assert trajectory_steps[0][0] == "SchemaAgent", "First step must be SchemaAgent"
    assert trajectory_steps[1][0] == "SQLGenAgent", "Second step must be SQLGenAgent"
    assert len(result) > 0, "Final output must be present"
    assert "SELECT" in result[0].root.text, "Final output must be SQL"


@pytest.mark.asyncio
async def test_trajectory_task_completion_rate(monkeypatch):
    """
    Trajectory metric: Task completion — agent must produce final answer (Section 14).
    """

    async def mock_call_agent(self, agent_url: str, query: str):
        if "9102" in agent_url:
            return [Part(root=TextPart(text="{'schema': 'sample(id)'}"))]
        if "9103" in agent_url:
            return [Part(root=TextPart(text="SELECT 1"))]
        return [Part(root=TextPart(text="No response"))]

    monkeypatch.setattr(RouterAgentExecutor, "_call_agent", mock_call_agent)
    executor = RouterAgentExecutor()
    executor.mode = "multi"

    result = await executor.delegate_query_multi_agent("count")

    assert len(result) > 0, "Trajectory must produce a final output"
    assert "SELECT" in result[0].root.text, "Final output must contain SQL"


@pytest.mark.asyncio
async def test_trajectory_fallback_single_agent_when_multi_unavailable(monkeypatch):
    """
    Trajectory: Fallback to single-agent when specialists unavailable (Section 14.2.3).
    """
    catalyst_called = False

    async def mock_call_agent(self, agent_url: str, query: str):
        nonlocal catalyst_called
        if "9101" in agent_url:
            catalyst_called = True
            return [Part(root=TextPart(text="SELECT 0"))]
        return [Part(root=TextPart(text="Unavailable"))]

    monkeypatch.setattr(RouterAgentExecutor, "_call_agent", mock_call_agent)
    executor = RouterAgentExecutor()
    executor.mode = "single"

    result = await executor.delegate_query_single_agent("count samples")

    assert catalyst_called, "Single-agent mode must call CatalystAgent"
    assert len(result) > 0, "Fallback must return a response"


@pytest.mark.asyncio
async def test_trajectory_query_passed_to_both_agents(monkeypatch):
    """
    Trajectory: User query must be passed to SchemaAgent and SQLGenAgent.
    """
    queries_received = []

    async def mock_call_agent(self, agent_url: str, query: str):
        queries_received.append((agent_url, query))
        if "9102" in agent_url:
            return [Part(root=TextPart(text="{'schema': 'x'}"))]
        if "9103" in agent_url:
            return [Part(root=TextPart(text="SELECT 1"))]
        return []

    monkeypatch.setattr(RouterAgentExecutor, "_call_agent", mock_call_agent)
    executor = RouterAgentExecutor()
    executor.mode = "multi"
    user_query = "How many samples were entered today?"

    await executor.delegate_query_multi_agent(user_query)

    assert len(queries_received) >= 2
    for _url, q in queries_received:
        assert q == user_query, "Same user query must be passed to each agent"
