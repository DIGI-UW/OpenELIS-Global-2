import os
from dataclasses import dataclass


@dataclass(frozen=True)
class GatewayConfig:
    router_url: str


def load_config() -> GatewayConfig:
    return GatewayConfig(
        router_url=os.getenv("CATALYST_ROUTER_URL", "http://localhost:9100"),
    )
