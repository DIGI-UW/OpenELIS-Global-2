"""
Tests for SQLGenAgent - specialized agent for SQL generation from schema context.

SQLGenAgent receives schema context (from SchemaAgent or RouterAgent) and
generates SQL using the configured LLM provider.
"""

from src.agents import sqlgen_executor
from src.config import LlmConfig


def test_sqlgen_executor_generates_sql_from_schema_context(monkeypatch):
    """
    Test that SQLGenAgent generates SQL when given schema context.

    This verifies:
    - SQLGenAgent accepts user query + schema context
    - SQLGenAgent calls LLM to generate SQL
    - SQLGenAgent returns valid SQL
    """
    # Mock LLM config
    mock_config = LlmConfig(
        provider="lmstudio",
        lmstudio_base_url="http://localhost:1234",
        lmstudio_model="local-model",
        gemini_api_key="",
        gemini_model="gemini-pro",
    )
    monkeypatch.setattr(sqlgen_executor, "load_llm_config", lambda: mock_config)

    class DummyClient:
        def generate_sql(self, prompt: str) -> str:
            return "SELECT COUNT(*) FROM sample"

    monkeypatch.setattr(sqlgen_executor, "create_llm_client", lambda _: DummyClient())

    # Call SQLGen with schema context
    schema_context = {
        "tables": ["sample", "analysis"],
        "schema": "sample(id, entered_date)\nanalysis(id, sample_id)",
    }
    user_query = "count samples"

    result = sqlgen_executor.generate_sql_from_context(user_query, schema_context)

    # Verify SQL was generated
    assert "sql" in result
    assert result["sql"] == "SELECT COUNT(*) FROM sample"


def test_sqlgen_executor_uses_schema_context_in_prompt(monkeypatch):
    """
    Test that SQLGenAgent includes schema context in the LLM prompt.

    This ensures the generated SQL uses only the provided schema tables
    (avoiding hallucination of non-existent tables).
    """
    captured_prompt = None

    mock_config = LlmConfig(
        provider="lmstudio",
        lmstudio_base_url="http://localhost:1234",
        lmstudio_model="local-model",
        gemini_api_key="",
        gemini_model="gemini-pro",
    )
    monkeypatch.setattr(sqlgen_executor, "load_llm_config", lambda: mock_config)

    class PromptCaptureClient:
        def generate_sql(self, prompt: str) -> str:
            nonlocal captured_prompt
            captured_prompt = prompt
            return "SELECT * FROM sample"

    monkeypatch.setattr(sqlgen_executor, "create_llm_client", lambda _: PromptCaptureClient())

    schema_context = {"tables": ["sample"], "schema": "sample(id, entered_date)"}
    user_query = "get all samples"

    sqlgen_executor.generate_sql_from_context(user_query, schema_context)

    # Verify prompt includes schema context
    assert captured_prompt is not None
    assert "sample(id, entered_date)" in captured_prompt
    assert "get all samples" in captured_prompt


def test_sqlgen_executor_handles_empty_schema_context(monkeypatch):
    """
    Test SQLGenAgent behavior when schema context is empty.

    Should still attempt to generate SQL (LLM may return error message
    or ask for clarification).
    """
    mock_config = LlmConfig(
        provider="lmstudio",
        lmstudio_base_url="http://localhost:1234",
        lmstudio_model="local-model",
        gemini_api_key="",
        gemini_model="gemini-pro",
    )
    monkeypatch.setattr(sqlgen_executor, "load_llm_config", lambda: mock_config)

    class DummyClient:
        def generate_sql(self, prompt: str) -> str:
            return "-- No relevant tables found"

    monkeypatch.setattr(sqlgen_executor, "create_llm_client", lambda _: DummyClient())

    schema_context = {"tables": [], "schema": ""}
    user_query = "count samples"

    result = sqlgen_executor.generate_sql_from_context(user_query, schema_context)

    # Should still return a result (even if it's an error message)
    assert "sql" in result
