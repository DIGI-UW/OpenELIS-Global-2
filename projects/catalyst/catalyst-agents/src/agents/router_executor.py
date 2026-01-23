import logging
import os
from typing import List

import httpx
from a2a.client import ClientConfig, ClientFactory
from a2a.client.card_resolver import A2ACardResolver
from a2a.server.agent_execution import AgentExecutor, RequestContext
from a2a.server.events import EventQueue
from a2a.server.tasks import TaskUpdater
from a2a.types import Message, Part, Role, TextPart, TaskState, TransportProtocol
from a2a.utils import new_agent_text_message, new_task

logger = logging.getLogger(__name__)


class RouterAgentExecutor(AgentExecutor):
    def __init__(self) -> None:
        self.catalyst_url = os.getenv("CATALYST_AGENT_URL", "http://localhost:9101")
        self.http_client = httpx.AsyncClient(timeout=30.0)

    async def _create_client(self):
        resolver = A2ACardResolver(self.http_client, self.catalyst_url)
        agent_card = await resolver.get_agent_card()
        client_config = ClientConfig(
            httpx_client=self.http_client,
            supported_transports=[TransportProtocol.jsonrpc],
            use_client_preference=False,
        )
        return ClientFactory(client_config).create(agent_card)

    async def delegate_query(self, query: str) -> List[Part]:
        client = await self._create_client()
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

        return [Part(root=TextPart(text="No response from CatalystAgent."))]

    async def execute(
        self,
        context: RequestContext,
        event_queue: EventQueue,
    ) -> None:
        query = context.get_user_input()
        task = context.current_task or new_task(context.message)
        task_updater = TaskUpdater(event_queue, task.id, task.context_id)

        await task_updater.update_status(
            TaskState.working,
            new_agent_text_message(
                "Routing query to CatalystAgent.",
                task.context_id,
                task.id,
            ),
        )

        parts = await self.delegate_query(query)
        await task_updater.add_artifact(parts, name="catalyst_response")
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
