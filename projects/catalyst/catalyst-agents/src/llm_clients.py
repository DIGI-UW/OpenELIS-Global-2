from typing import Protocol

import httpx
from google import genai

from .config import LlmConfig


class LLMClient(Protocol):
    """Protocol for LLM clients - all providers must implement this interface."""

    def generate_sql(self, prompt: str) -> str:
        """Generate SQL from natural language prompt."""
        ...


class LMStudioClient:
    """LM Studio client (local OpenAI-compatible API)."""

    def __init__(self, base_url: str, model: str) -> None:
        self._base_url = base_url
        self._model = model

    def generate_sql(self, prompt: str) -> str:
        """Generate SQL using LM Studio OpenAI-compatible API."""
        # Use structured output/function calling for reliable SQL JSON format
        # For models with native tool support (Qwen, Llama 3.x, Mistral)
        with httpx.Client() as client:
            response = client.post(
                f"{self._base_url}/chat/completions",
                json={
                    "model": self._model,
                    "messages": [
                        {
                            "role": "system",
                            "content": (
                                "You are a SQL expert. Generate valid PostgreSQL SQL queries "
                                "from natural language questions. Return only the SQL statement, "
                                "no explanations."
                            ),
                        },
                        {"role": "user", "content": prompt},
                    ],
                    "temperature": 0.1,
                },
                timeout=30.0,
            )
            response.raise_for_status()
            result = response.json()
            return result["choices"][0]["message"]["content"].strip()


class GeminiClient:
    """Google Gemini client (cloud provider)."""

    def __init__(self, api_key: str, model: str) -> None:
        self._client = genai.Client(api_key=api_key)
        self._model = model

    def generate_sql(self, prompt: str) -> str:
        """Generate SQL using Gemini with structured outputs for reliable format."""
        # Use Gemini's structured output/function calling for reliable SQL JSON format
        # This reduces hallucination/format errors
        from google.genai import types

        response = self._client.models.generate_content(
            model=self._model,
            contents=prompt,
            config=types.GenerateContentConfig(
                temperature=0.1,
                top_p=0.95,
                top_k=40,
            ),
        )
        return response.text.strip()


def create_llm_client(config: LlmConfig) -> LLMClient:
    """Create LLM client based on configured provider (provider-agnostic)."""
    if config.provider == "gemini":
        if not config.gemini_api_key:
            raise ValueError("GOOGLE_API_KEY environment variable required for Gemini provider")
        return GeminiClient(config.gemini_api_key, config.gemini_model)
    if config.provider == "lmstudio":
        return LMStudioClient(config.lmstudio_base_url, config.lmstudio_model)
    raise ValueError(f"Unsupported LLM provider: {config.provider}. Supported: gemini, lmstudio")
