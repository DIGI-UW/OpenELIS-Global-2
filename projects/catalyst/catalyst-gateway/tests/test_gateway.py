from fastapi import FastAPI

from src import gateway


def test_gateway_exposes_chat_completions_endpoint():
    app = gateway.create_app()
    assert isinstance(app, FastAPI)
    paths = {route.path for route in app.router.routes}
    assert "/v1/chat/completions" in paths
    assert "/health" in paths
