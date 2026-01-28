import os
from dataclasses import dataclass


@dataclass(frozen=True)
class LlmConfig:
    provider: str
    lmstudio_base_url: str
    lmstudio_model: str
    gemini_api_key: str
    gemini_model: str


def load_llm_config() -> LlmConfig:
    """Load LLM configuration from environment variables."""
    provider = os.getenv("CATALYST_LLM_PROVIDER", "lmstudio")
    return LlmConfig(
        provider=provider,
        lmstudio_base_url=os.getenv("LMSTUDIO_BASE_URL", "http://host.docker.internal:1234/v1"),
        lmstudio_model=os.getenv("LMSTUDIO_MODEL", "local-model"),
        gemini_api_key=os.getenv("GOOGLE_API_KEY", ""),
        gemini_model=os.getenv("GEMINI_MODEL", "gemini-pro"),
    )
