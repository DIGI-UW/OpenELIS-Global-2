def get_schema() -> str:
    # TODO: Call MCP server over HTTP in later milestones.
    return "sample\nanalysis\npatient\norganization\ntest"


def get_query_context(_query: str) -> dict:
    """
    Get relevant schema context for a query (M0.2 mock).

    In M1, this will call the real MCP server for RAG-based schema retrieval.
    For M0.2, we return a mock schema context to validate the agent architecture.

    Args:
        query: Natural language query

    Returns:
        Dictionary with tables and schema metadata
    """
    # M0.2: Return mock schema context
    # M1: Will call MCP server get_relevant_tables + get_table_ddl
    return {
        "tables": ["sample", "analysis"],
        "schema": "sample(id, entered_date)\nanalysis(id, sample_id, test_name)",
    }
