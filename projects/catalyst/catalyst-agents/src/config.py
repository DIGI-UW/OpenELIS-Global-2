import os
from dataclasses import dataclass


@dataclass(frozen=True)
class LlmConfig:
    provider: str
    lmstudio_base_url: str
    lmstudio_model: str


def load_llm_config() -> LlmConfig:
    return LlmConfig(
        provider=os.getenv("CATALYST_LLM_PROVIDER", "lmstudio"),
        lmstudio_base_url=os.getenv(
            "LMSTUDIO_BASE_URL", "http://host.docker.internal:1234/v1"
        ),
        lmstudio_model=os.getenv("LMSTUDIO_MODEL", "local-model"),
    )
