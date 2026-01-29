import logging
from typing import Any

from a2a.server.agent_execution import AgentExecutor, RequestContext
from a2a.server.events import EventQueue
from a2a.server.tasks import TaskUpdater
from a2a.types import Part, TaskState, TextPart
from a2a.utils import new_agent_text_message, new_task

from .. import mcp_client
from ..config import load_llm_config
from ..llm_clients import GeminiClient, LMStudioClient

logger = logging.getLogger(__name__)


def _create_llm_client(config):
    """Create LLM client based on configured provider (provider-agnostic)."""
    if config.provider == "gemini":
        if not config.gemini_api_key:
            raise ValueError("GOOGLE_API_KEY environment variable required for Gemini provider")
        return GeminiClient(config.gemini_api_key, config.gemini_model)
    if config.provider == "lmstudio":
        return LMStudioClient(config.lmstudio_base_url, config.lmstudio_model)
    raise ValueError(f"Unsupported LLM provider: {config.provider}. Supported: gemini, lmstudio")


def generate_sql(user_query: str) -> dict[str, Any]:
    """Generate SQL from natural language query using configured LLM provider."""
    schema = mcp_client.get_schema()
    config = load_llm_config()
    client = _create_llm_client(config)
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
