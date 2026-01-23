import pytest

from a2a.types import Part, TextPart

from src.agents.router_executor import RouterAgentExecutor


class _FakeArtifact:
    def __init__(self, text: str) -> None:
        self.parts = [Part(root=TextPart(text=text))]
        self.name = "generated_sql"


class _FakeTask:
    def __init__(self, text: str) -> None:
        self.artifacts = [_FakeArtifact(text)]


class _FakeClient:
    async def send_message(self, message):
        yield _FakeTask("SELECT 1")


@pytest.mark.asyncio
async def test_router_delegates_to_catalyst_agent(monkeypatch):
    executor = RouterAgentExecutor()

    async def _fake_create_client():
        return _FakeClient()

    monkeypatch.setattr(executor, "_create_client", _fake_create_client)
    parts = await executor.delegate_query("count samples")
    assert parts[0].root.text == "SELECT 1"
