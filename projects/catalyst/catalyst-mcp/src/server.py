from fastapi import FastAPI


def create_app() -> FastAPI:
    app = FastAPI(title="Catalyst MCP", version="0.0.1")

    @app.get("/health")
    async def health() -> dict:
        return {"status": "ok"}

    return app


app = create_app()
