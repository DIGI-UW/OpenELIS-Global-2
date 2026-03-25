from typing import Any, Dict, Optional

import httpx
from a2a.client import ClientConfig, ClientFactory
from a2a.client.card_resolver import A2ACardResolver
from a2a.types import Message, Part, Role, TextPart, TransportProtocol


class A2AClient:
    def __init__(self, router_url: str) -> None:
        self._router_url = router_url
        self._http_client = httpx.AsyncClient(timeout=30.0)

    async def _create_client(self):
        resolver = A2ACardResolver(self._http_client, self._router_url)
        agent_card = await resolver.get_agent_card()
        client_config = ClientConfig(
            httpx_client=self._http_client,
            supported_transports=[TransportProtocol.jsonrpc],
            use_client_preference=False,
        )
        return ClientFactory(client_config).create(agent_card)

    @staticmethod
    def _extract_user_message(payload: Dict[str, Any]) -> Optional[str]:
        messages = payload.get("messages") or []
        for message in reversed(messages):
            if message.get("role") == "user":
                return message.get("content")
        return None

    async def send_chat_completion(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        query = self._extract_user_message(payload) or ""
        client = await self._create_client()
        message = Message(
            messageId=payload.get("id") or "catalyst-m0",
            role=Role.user,
            parts=[Part(root=TextPart(text=query))],
        )

        final_task = None
        async for event in client.send_message(message):
            final_task = event[0] if isinstance(event, tuple) else event

        sql_text = ""
        if final_task and getattr(final_task, "artifacts", None):
            parts = final_task.artifacts[-1].parts
            if parts and hasattr(parts[0].root, "text"):
                sql_text = parts[0].root.text

        return {
            "id": payload.get("id", "catalyst-m0"),
            "object": "chat.completion",
            "choices": [
                {
                    "index": 0,
                    "message": {"role": "assistant", "content": sql_text},
                    "finish_reason": "stop",
                }
            ],
        }
