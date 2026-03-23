from fastapi import FastAPI

from .a2a_client import A2AClient
from .config import load_config


def create_app() -> FastAPI:
    app = FastAPI(title="Catalyst Gateway", version="0.0.1")
    config = load_config()
    client = A2AClient(config.router_url)

    @app.get("/health")
    async def health() -> dict:
        return {"status": "ok"}

    @app.post("/v1/chat/completions")
    async def chat_completions(payload: dict) -> dict:
        return await client.send_chat_completion(payload)

    return app


app = create_app()
