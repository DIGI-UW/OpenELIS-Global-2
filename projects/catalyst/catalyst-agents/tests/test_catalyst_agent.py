from src import mcp_client
from src.agents import catalyst_executor


def test_catalyst_executor_generates_sql_with_schema(monkeypatch):
    monkeypatch.setattr(mcp_client, "get_schema", lambda: "sample\nanalysis")

    class DummyClient:
        def __init__(self, base_url: str, model: str) -> None:
            # Accept init params but ignore them for test
            pass

        def generate_sql(self, prompt: str) -> str:
            return "SELECT 1"

    monkeypatch.setattr(catalyst_executor, "LMStudioClient", DummyClient)

    result = catalyst_executor.generate_sql("count samples")
    assert result["sql"] == "SELECT 1"
    assert "sample" in result["schema"]


def test_fr004_llm_prompt_contains_only_schema_and_query_no_phi(monkeypatch):
    """
    FR-004 Validation: Verify LLM prompts contain ONLY schema metadata and user query,
    with NO patient data, test results, or other PHI.
    """
    # Mock schema (non-PHI metadata only)
    mock_schema = "test\ntest_section\ndictionary"
    monkeypatch.setattr(mcp_client, "get_schema", lambda: mock_schema)

    # Capture the prompt sent to LLM
    captured_prompt = None

    class PromptCaptureClient:
        def __init__(self, base_url: str, model: str) -> None:
            # Accept init params but ignore them for test
            pass

        def generate_sql(self, prompt: str) -> str:
            nonlocal captured_prompt
            captured_prompt = prompt
            return "SELECT COUNT(*) FROM test"

    # Patch the class in the module where it's used (catalyst_executor imports it)
    monkeypatch.setattr(catalyst_executor, "LMStudioClient", PromptCaptureClient)

    # Test with user query that does NOT contain PHI
    user_query = "How many tests are in the catalog?"
    result = catalyst_executor.generate_sql(user_query)

    # Verify prompt was captured
    assert captured_prompt is not None, "LLM prompt should have been captured"

    # FR-004: Verify prompt contains schema metadata
    assert "Schema:" in captured_prompt, "Prompt should contain schema section"
    assert mock_schema in captured_prompt, "Prompt should contain schema content"
    assert "test" in captured_prompt.lower()  # Schema table name

    # FR-004: Verify prompt contains user query
    assert "Question:" in captured_prompt, "Prompt should contain question section"
    assert user_query in captured_prompt, "Prompt should contain user query"

    # FR-004: Verify prompt structure (should only have Schema, Question, SQL sections)
    prompt_sections = captured_prompt.split("\n\n")
    assert len(prompt_sections) >= 3, "Prompt should have Schema, Question, and SQL sections"
    
    # Verify no PHI patterns in any section
    phi_patterns = [
        # Patient identifiers
        "patient_id",
        "patient_name",
        "first_name",
        "last_name",
        "dob",
        "date_of_birth",
        "ssn",
        "social_security",
        # Test results (numeric values that could be results)
        "result_value",
        "numeric_result",
        "alpha_result",
        # Sample identifiers that could be PHI
        "accession_number",
        "sample_id",
        # Dates that could be birth dates
        "birth_date",
        "birthdate",
    ]

    prompt_lower = captured_prompt.lower()
    for pattern in phi_patterns:
        assert pattern not in prompt_lower, (
            f"FR-004 violation: Prompt contains PHI pattern '{pattern}'. "
            f"Prompt should only contain schema metadata and user query."
        )

    # FR-004: Verify no numeric patterns that look like patient IDs or test results
    # (This is a conservative check - we allow schema column types like "numeric(10,0)")
    # but we should not have actual numeric values that could be patient data
    import re
    # Check for standalone numeric values that could be patient IDs (6+ digits)
    # Allow numeric types in schema but not standalone large numbers
    numeric_values = re.findall(r'\b\d{6,}\b', captured_prompt)
    # Filter out schema-related numbers (like "numeric(10,0)" which is a type definition)
    suspicious_numbers = [
        n for n in numeric_values
        if "numeric(" not in captured_prompt[max(0, captured_prompt.find(n) - 20):captured_prompt.find(n) + 20].lower()
    ]
    assert len(suspicious_numbers) == 0, (
        f"FR-004 violation: Prompt contains suspicious numeric values that could be "
        f"patient IDs or test results: {suspicious_numbers}. "
        f"Prompt should only contain schema metadata and user query."
    )

    # Verify result is valid
    assert result["sql"] == "SELECT COUNT(*) FROM test"
    assert mock_schema in result["schema"]
