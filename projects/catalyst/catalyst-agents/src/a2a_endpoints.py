import os
from dataclasses import dataclass


@dataclass(frozen=True)
class A2AEndpoints:
    router_url: str
    catalyst_url: str


def load_a2a_endpoints() -> A2AEndpoints:
    return A2AEndpoints(
        router_url=os.getenv("CATALYST_ROUTER_URL", "http://localhost:9100"),
        catalyst_url=os.getenv("CATALYST_AGENT_URL", "http://localhost:9101"),
    )
