import pytest

from a2a.types import Part, TextPart

from src import llm_clients
from src import mcp_client
from src.agents.catalyst_executor import generate_sql
from src.agents.router_executor import RouterAgentExecutor


class _FakeArtifact:
    def __init__(self, text: str) -> None:
        self.parts = [Part(root=TextPart(text=text))]
        self.name = "generated_sql"


class _FakeTask:
    def __init__(self, text: str) -> None:
        self.artifacts = [_FakeArtifact(text)]


class _FakeClient:
    def __init__(self, sql: str) -> None:
        self._sql = sql

    async def send_message(self, message):
        yield _FakeTask(self._sql)


@pytest.mark.asyncio
async def test_router_to_catalyst_to_mcp_flow(monkeypatch):
    monkeypatch.setattr(mcp_client, "get_schema", lambda: "sample\nanalysis")

    # Mock LMStudioClient.generate_sql method to return expected SQL
    def mock_generate_sql(self, prompt: str) -> str:
        return "SELECT COUNT(*) FROM sample"
    
    monkeypatch.setattr(llm_clients.LMStudioClient, "generate_sql", mock_generate_sql)
    
    sql_result = generate_sql("count samples")["sql"]
    # Verify sql_result is what we expect
    assert sql_result == "SELECT COUNT(*) FROM sample", f"Expected 'SELECT COUNT(*) FROM sample', got '{sql_result}'"
    
    executor = RouterAgentExecutor()

    async def _fake_create_client():
        return _FakeClient(sql_result)

    monkeypatch.setattr(executor, "_create_client", _fake_create_client)
    parts = await executor.delegate_query("count samples")
    # Verify the parts contain the expected SQL
    assert len(parts) > 0, "Should have at least one part"
    assert parts[0].root.text == "SELECT COUNT(*) FROM sample", f"Expected 'SELECT COUNT(*) FROM sample', got '{parts[0].root.text}'"
