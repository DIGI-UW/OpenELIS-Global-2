import logging
import os

import httpx
from a2a.client import ClientConfig, ClientFactory
from a2a.client.card_resolver import A2ACardResolver
from a2a.server.agent_execution import AgentExecutor, RequestContext
from a2a.server.events import EventQueue
from a2a.server.tasks import TaskUpdater
from a2a.types import Message, Part, Role, TaskState, TextPart, TransportProtocol
from a2a.utils import new_agent_text_message, new_task

logger = logging.getLogger(__name__)


class RouterAgentExecutor(AgentExecutor):
    def __init__(self) -> None:
        # Agent URLs
        self.catalyst_url = os.getenv("CATALYST_AGENT_URL", "http://localhost:9101")
        self.schema_url = os.getenv("CATALYST_SCHEMA_AGENT_URL", "http://localhost:9102")
        self.sqlgen_url = os.getenv("CATALYST_SQLGEN_AGENT_URL", "http://localhost:9103")

        # Agent mode: "multi" for SchemaAgent → SQLGenAgent, "single" for CatalystAgent fallback.
        # Default "single" so dev (Procfile/docker-compose without specialist agents) works.
        self.mode = os.getenv("CATALYST_AGENT_MODE", "single")

        self.http_client = httpx.AsyncClient(timeout=30.0)

    async def _create_client(self, agent_url: str):
        """Create A2A client for a specific agent."""
        resolver = A2ACardResolver(self.http_client, agent_url)
        agent_card = await resolver.get_agent_card()
        client_config = ClientConfig(
            httpx_client=self.http_client,
            supported_transports=[TransportProtocol.jsonrpc],
            use_client_preference=False,
        )
        return ClientFactory(client_config).create(agent_card)

    async def _call_agent(self, agent_url: str, query: str) -> list[Part]:
        """Call an agent with a query and return response parts."""
        client = await self._create_client(agent_url)
        message = Message(
            messageId=os.urandom(16).hex(),
            role=Role.user,
            parts=[Part(root=TextPart(text=query))],
        )

        final_task = None
        async for event in client.send_message(message):
            final_task = event[0] if isinstance(event, tuple) else event

        if final_task and getattr(final_task, "artifacts", None):
            return final_task.artifacts[-1].parts

        return [Part(root=TextPart(text=f"No response from agent at {agent_url}."))]

    async def delegate_query_single_agent(self, query: str) -> list[Part]:
        """Single-agent mode: delegate directly to CatalystAgent."""
        return await self._call_agent(self.catalyst_url, query)

    async def delegate_query_multi_agent(self, query: str) -> list[Part]:
        """
        Multi-agent mode: orchestrate SchemaAgent → SQLGenAgent flow.

        Flow:
        1. Call SchemaAgent to get relevant schema context
        2. Call SQLGenAgent with query (M0.2: SQLGenAgent uses its own placeholder schema;
           M1+ will pass schema context explicitly via task artifacts)
        """
        # Step 1: Get schema context from SchemaAgent
        schema_parts = await self._call_agent(self.schema_url, query)

        # Extract schema context from response (for future M1+ task artifact use)
        for part in schema_parts:
            if hasattr(part.root, "text"):
                _ = part.root.text
                break

        # Step 2: Call SQLGenAgent with query (SQLGenAgent will use its own schema context for now)
        # In M1+, we'll pass the schema context explicitly via task artifacts
        return await self._call_agent(self.sqlgen_url, query)

    async def execute(
        self,
        context: RequestContext,
        event_queue: EventQueue,
    ) -> None:
        query = context.get_user_input()
        task = context.current_task or new_task(context.message)
        task_updater = TaskUpdater(event_queue, task.id, task.context_id)

        if self.mode == "single":
            await task_updater.update_status(
                TaskState.working,
                new_agent_text_message(
                    "Routing query to CatalystAgent (single-agent mode).",
                    task.context_id,
                    task.id,
                ),
            )
            parts = await self.delegate_query_single_agent(query)
        else:
            await task_updater.update_status(
                TaskState.working,
                new_agent_text_message(
                    "Routing query through SchemaAgent → SQLGenAgent (multi-agent mode).",
                    task.context_id,
                    task.id,
                ),
            )
            parts = await self.delegate_query_multi_agent(query)

        await task_updater.add_artifact(parts, name="router_response")
        await task_updater.complete()

    async def cancel(self, context: RequestContext, event_queue: EventQueue) -> None:
        task = context.current_task
        if not task:
            return
        task_updater = TaskUpdater(event_queue, task.id, task.context_id)
        await task_updater.update_status(
            TaskState.cancelled,
            new_agent_text_message(
                "Routing cancelled.",
                task.context_id,
                task.id,
            ),
        )
