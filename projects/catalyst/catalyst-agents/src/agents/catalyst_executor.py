import logging
from typing import Any

from a2a.server.agent_execution import AgentExecutor, RequestContext
from a2a.server.events import EventQueue
from a2a.server.tasks import TaskUpdater
from a2a.types import Part, TextPart, TaskState
from a2a.utils import new_agent_text_message, new_task

from .. import mcp_client
from ..config import load_llm_config
from ..llm_clients import LMStudioClient

logger = logging.getLogger(__name__)


def generate_sql(user_query: str) -> dict[str, Any]:
    schema = mcp_client.get_schema()
    config = load_llm_config()
    client = LMStudioClient(config.lmstudio_base_url, config.lmstudio_model)
    prompt = f"Schema:\n{schema}\n\nQuestion:\n{user_query}\n\nSQL:"
    sql = client.generate_sql(prompt)
    return {"sql": sql, "schema": schema}


class CatalystAgentExecutor(AgentExecutor):
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
                "Generating SQL from schema context.",
                task.context_id,
                task.id,
            ),
        )

        result = generate_sql(query)
        sql_text = result["sql"]

        await task_updater.add_artifact(
            [Part(root=TextPart(text=sql_text))],
            name="generated_sql",
        )
        await task_updater.complete()

    async def cancel(self, context: RequestContext, event_queue: EventQueue) -> None:
        task = context.current_task
        if not task:
            return
        task_updater = TaskUpdater(event_queue, task.id, task.context_id)
        await task_updater.update_status(
            TaskState.cancelled,
            new_agent_text_message(
                "Catalyst execution cancelled.",
                task.context_id,
                task.id,
            ),
        )
