from src import llm_clients
from src import mcp_client
from src.agents.catalyst_executor import generate_sql


def test_catalyst_executor_generates_sql_with_schema(monkeypatch):
    monkeypatch.setattr(mcp_client, "get_schema", lambda: "sample\nanalysis")

    class DummyClient:
        def generate_sql(self, prompt: str) -> str:
            return "SELECT 1"

    monkeypatch.setattr(llm_clients, "LMStudioClient", DummyClient)

    result = generate_sql("count samples")
    assert result["sql"] == "SELECT 1"
    assert "sample" in result["schema"]
