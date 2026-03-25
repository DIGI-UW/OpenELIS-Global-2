import json
from pathlib import Path

from a2a.server.apps import A2AStarletteApplication
from a2a.server.request_handlers import DefaultRequestHandler
from a2a.server.tasks import InMemoryTaskStore
from a2a.types import AgentCapabilities, AgentCard, AgentSkill, TransportProtocol

from ..a2a_endpoints import load_a2a_endpoints
from .router_executor import RouterAgentExecutor


def _load_agent_card() -> AgentCard:
    card_path = Path(__file__).resolve().parent.parent / "agent_cards" / "router.json"
    data = json.loads(card_path.read_text(encoding="utf-8"))
    skills = data.get("skills", [])
    data["skills"] = [AgentSkill(**skill) for skill in skills]
    return AgentCard(**data)


def create_app():
    executor = RouterAgentExecutor()
    request_handler = DefaultRequestHandler(
        agent_executor=executor,
        task_store=InMemoryTaskStore(),
    )
    agent_card = _load_agent_card()
    endpoints = load_a2a_endpoints()
    agent_card.url = endpoints.router_url
    agent_card.preferred_transport = TransportProtocol.jsonrpc
    agent_card.capabilities = AgentCapabilities(streaming=False)

    server = A2AStarletteApplication(
        agent_card=agent_card,
        http_handler=request_handler,
    )
    return server.build()


app = create_app()
