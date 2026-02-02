"""
SchemaAgent executor - specialized agent for schema retrieval via MCP.

This agent calls MCP tools to retrieve relevant database schema metadata
based on the user's natural language query, enabling the SQLGenAgent to
generate accurate SQL without needing the entire schema.
"""

import json
import logging
from typing import Any

from a2a.server.agent_execution import AgentExecutor, RequestContext
from a2a.server.events import EventQueue
from a2a.server.tasks import TaskUpdater
from a2a.types import Part, TaskState, TextPart
from a2a.utils import new_agent_text_message, new_task

from .. import mcp_client

logger = logging.getLogger(__name__)


def get_schema_context(user_query: str) -> dict[str, Any]:
    """
    Retrieve relevant schema context for a user query via MCP.

    Args:
        user_query: Natural language query from the user

    Returns:
        Dictionary containing:
            - tables: List of relevant table names
            - schema: Schema metadata (DDL, relationships, etc.)
    """
    return mcp_client.get_query_context(user_query)


class SchemaAgentExecutor(AgentExecutor):
    """
    SchemaAgent executor for A2A protocol.

    This agent retrieves relevant database schema metadata based on the
    user's query, which is then passed to SQLGenAgent for SQL generation.
    """

    async def execute(
        self,
        context: RequestContext,
        event_queue: EventQueue,
    ) -> None:
        """Execute schema retrieval task."""
        query = context.get_user_input()
        task = context.current_task or new_task(context.message)
        task_updater = TaskUpdater(event_queue, task.id, task.context_id)

        await task_updater.update_status(
            TaskState.working,
            new_agent_text_message(
                "Retrieving relevant schema context via MCP.",
                task.context_id,
                task.id,
            ),
        )

        # Get schema context via MCP
        schema_context = get_schema_context(query)

        # Return schema context as artifact (JSON for A2A/interop consumers)
        await task_updater.add_artifact(
            [Part(root=TextPart(text=json.dumps(schema_context)))],
            name="schema_context",
        )
        await task_updater.complete()

    async def cancel(self, context: RequestContext, event_queue: EventQueue) -> None:
        """Handle cancellation of schema retrieval task."""
        task = context.current_task
        if not task:
            return
        task_updater = TaskUpdater(event_queue, task.id, task.context_id)
        await task_updater.update_status(
            TaskState.cancelled,
            new_agent_text_message(
                "Schema retrieval cancelled.",
                task.context_id,
                task.id,
            ),
        )
