from src.tools.schema_tools import get_schema


def test_get_schema_returns_expected_tables():
    schema = get_schema()
    for table_name in ["sample", "test", "analysis", "patient", "organization"]:
        assert table_name in schema
