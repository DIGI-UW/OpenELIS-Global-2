"""
SQLGenAgent executor - specialized agent for SQL generation from schema context.

This agent receives schema context (from SchemaAgent) and generates SQL
using the configured LLM provider, without needing to retrieve schema itself.
"""

import logging
from typing import Any

from a2a.server.agent_execution import AgentExecutor, RequestContext
from a2a.server.events import EventQueue
from a2a.server.tasks import TaskUpdater
from a2a.types import Part, TaskState, TextPart
from a2a.utils import new_agent_text_message, new_task

from ..config import load_llm_config
from ..llm_clients import create_llm_client

logger = logging.getLogger(__name__)


def generate_sql_from_context(user_query: str, schema_context: dict[str, Any]) -> dict[str, Any]:
    """
    Generate SQL from user query and schema context.

    Args:
        user_query: Natural language query from the user
        schema_context: Schema metadata from SchemaAgent (tables + DDL)

    Returns:
        Dictionary containing generated SQL
    """
    schema = schema_context.get("schema", "")
    config = load_llm_config()
    client = create_llm_client(config)

    # Build prompt with schema context + user query
    prompt = f"Schema:\n{schema}\n\nQuestion:\n{user_query}\n\nSQL:"
    sql = client.generate_sql(prompt)

    return {"sql": sql}


class SQLGenAgentExecutor(AgentExecutor):
    """
    SQLGenAgent executor for A2A protocol.

    This agent generates SQL from user queries using schema context
    provided by SchemaAgent (or directly from RouterAgent).
    """

    async def execute(
        self,
        context: RequestContext,
        event_queue: EventQueue,
    ) -> None:
        """Execute SQL generation task."""
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

        # In multi-agent flow, schema context would come from SchemaAgent via task artifacts
        # For M0.2, we use a placeholder schema context (will be wired in router update)
        schema_context = {
            "tables": ["sample", "analysis"],
            "schema": "sample(id, entered_date)\nanalysis(id, sample_id, test_name)",
        }

        result = generate_sql_from_context(query, schema_context)
        sql_text = result["sql"]

        await task_updater.add_artifact(
            [Part(root=TextPart(text=sql_text))],
            name="generated_sql",
        )
        await task_updater.complete()

    async def cancel(self, context: RequestContext, event_queue: EventQueue) -> None:
        """Handle cancellation of SQL generation task."""
        task = context.current_task
        if not task:
            return
        task_updater = TaskUpdater(event_queue, task.id, task.context_id)
        await task_updater.update_status(
            TaskState.cancelled,
            new_agent_text_message(
                "SQL generation cancelled.",
                task.context_id,
                task.id,
            ),
        )
