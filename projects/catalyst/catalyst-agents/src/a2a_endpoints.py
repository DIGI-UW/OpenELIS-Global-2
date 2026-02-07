import os
from dataclasses import dataclass


@dataclass(frozen=True)
class A2AEndpoints:
    router_url: str
    catalyst_url: str
    schema_url: str
    sqlgen_url: str


def load_a2a_endpoints() -> A2AEndpoints:
    return A2AEndpoints(
        router_url=os.getenv("CATALYST_ROUTER_URL", "http://localhost:9100"),
        catalyst_url=os.getenv("CATALYST_AGENT_URL", "http://localhost:9101"),
        schema_url=os.getenv("CATALYST_SCHEMA_AGENT_URL", "http://localhost:9102"),
        sqlgen_url=os.getenv("CATALYST_SQLGEN_AGENT_URL", "http://localhost:9103"),
    )
