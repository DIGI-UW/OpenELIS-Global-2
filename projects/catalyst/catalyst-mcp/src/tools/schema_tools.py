import os
import re
from typing import Any


def _get_allowed_tables() -> list[str]:
    """Load allowed tables from environment variable with minimal non-PHI default."""
    env_tables = os.getenv("MCP_ALLOWED_TABLES", "").strip()
    if env_tables:
        return [t.strip() for t in env_tables.split(",") if t.strip()]
    
    # Default: minimal non-PHI profile (terminology + test catalog + statuses)
    return [
        "test",
        "test_section",
        "analyte",
        "method",
        "panel",
        "panel_item",
        "type_of_sample",
        "type_of_test_result",
        "unit_of_measure",
        "dictionary",
        "dictionary_category",
        "status_of_sample",
    ]


def get_query_context(user_query: str) -> dict[str, Any]:
    """
    Get query context (schema bundle) for allowed tables only.
    
    Returns a safe schema bundle containing:
    - allowed_tables: list of table names
    - tables: dict mapping table name to schema info (columns, PKs, FKs)
    - notes: optional guidance
    """
    allowed = _get_allowed_tables()
    
    # M0.0: Mock schema data for allowed tables only
    # M1+: Will query pg_catalog/information_schema for real schema
    tables = {}
    
    for table_name in allowed:
        if table_name == "test":
            tables[table_name] = {
                "columns": [
                    {"name": "id", "type": "numeric(10,0)", "nullable": False},
                    {"name": "name", "type": "character varying", "nullable": False},
                    {"name": "description", "type": "character varying", "nullable": False},
                    {"name": "is_active", "type": "character varying(1)", "nullable": True},
                    {"name": "test_section_id", "type": "numeric(10,0)", "nullable": True},
                    {"name": "lastupdated", "type": "timestamp(6)", "nullable": True},
                ],
                "primary_key": ["id"],
                "foreign_keys": [
                    {"column": "test_section_id", "ref_table": "test_section", "ref_column": "id"}
                ],
            }
        elif table_name == "test_section":
            tables[table_name] = {
                "columns": [
                    {"name": "id", "type": "numeric(10,0)", "nullable": False},
                    {"name": "name", "type": "character varying", "nullable": True},
                    {"name": "description", "type": "character varying", "nullable": True},
                    {"name": "is_active", "type": "character varying(1)", "nullable": True},
                    {"name": "lastupdated", "type": "timestamp(6)", "nullable": True},
                ],
                "primary_key": ["id"],
                "foreign_keys": [],
            }
        elif table_name == "dictionary":
            tables[table_name] = {
                "columns": [
                    {"name": "id", "type": "numeric(10,0)", "nullable": False},
                    {"name": "dict_entry", "type": "character varying(4000)", "nullable": True},
                    {"name": "is_active", "type": "character varying(1)", "nullable": True},
                    {"name": "dictionary_category_id", "type": "numeric(10,0)", "nullable": True},
                    {"name": "lastupdated", "type": "timestamp(6)", "nullable": True},
                ],
                "primary_key": ["id"],
                "foreign_keys": [
                    {"column": "dictionary_category_id", "ref_table": "dictionary_category", "ref_column": "id"}
                ],
            }
        elif table_name == "dictionary_category":
            tables[table_name] = {
                "columns": [
                    {"name": "id", "type": "numeric(10,0)", "nullable": False},
                    {"name": "name", "type": "character varying(50)", "nullable": True},
                    {"name": "description", "type": "character varying(60)", "nullable": True},
                    {"name": "lastupdated", "type": "timestamp(6)", "nullable": True},
                ],
                "primary_key": ["id"],
                "foreign_keys": [],
            }
        elif table_name == "status_of_sample":
            tables[table_name] = {
                "columns": [
                    {"name": "id", "type": "numeric(10,0)", "nullable": False},
                    {"name": "name", "type": "character varying", "nullable": True},
                    {"name": "description", "type": "character varying", "nullable": True},
                    {"name": "code", "type": "character varying", "nullable": True},
                    {"name": "is_active", "type": "character varying(1)", "nullable": True},
                    {"name": "lastupdated", "type": "timestamp(6)", "nullable": True},
                ],
                "primary_key": ["id"],
                "foreign_keys": [],
            }
        else:
            # Generic schema for other allowed tables (minimal mock)
            tables[table_name] = {
                "columns": [
                    {"name": "id", "type": "numeric(10,0)", "nullable": False},
                    {"name": "lastupdated", "type": "timestamp(6)", "nullable": True},
                ],
                "primary_key": ["id"],
                "foreign_keys": [],
            }
    
    return {
        "allowed_tables": allowed,
        "tables": tables,
        "notes": [
            "All tables are in the clinlims schema",
            "Use table aliases for clarity in complex queries",
        ],
    }


def _extract_table_references(sql: str) -> tuple[list[str], list[str]]:
    """
    Extract table names referenced in SQL (FROM, JOIN clauses) and CTE names.
    
    Returns: (actual_tables, cte_names)
    - actual_tables: real database tables referenced in FROM/JOIN
    - cte_names: CTE names from WITH clauses (not validated against allowlist)
    
    M0.0: Simple regex-based extraction (conservative)
    M2+: May use sqlglot for more robust parsing
    """
    sql_upper = sql.upper()
    tables = set()
    cte_names = set()
    
    # Extract CTE names first (to exclude from table validation)
    with_pattern = r'\bWITH\s+([a-z_][a-z0-9_]*)\s+AS'
    for match in re.finditer(with_pattern, sql_upper, re.IGNORECASE):
        cte_names.add(match.group(1).lower())
    
    # Extract FROM clause tables (exclude CTEs)
    from_pattern = r'\bFROM\s+([a-z_][a-z0-9_]*)'
    for match in re.finditer(from_pattern, sql_upper, re.IGNORECASE):
        table_name = match.group(1).lower()
        if table_name not in cte_names:  # Don't treat CTEs as tables
            tables.add(table_name)
    
    # Extract JOIN clause tables (exclude CTEs)
    join_pattern = r'\bJOIN\s+([a-z_][a-z0-9_]*)'
    for match in re.finditer(join_pattern, sql_upper, re.IGNORECASE):
        table_name = match.group(1).lower()
        if table_name not in cte_names:  # Don't treat CTEs as tables
            tables.add(table_name)
    
    return (list(tables), list(cte_names))


def validate_sql(sql: str) -> dict[str, Any]:
    """
    Validate SQL against guardrails and allowlist.
    
    Returns validation result with:
    - valid: bool
    - errors: list of error messages
    - warnings: list of warning messages
    - referenced_tables: list of tables found in SQL
    """
    sql_upper = sql.upper().strip()
    errors = []
    warnings = []
    
    # Check for blocked operations (DDL/DML)
    blocked_keywords = ["DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "INSERT", "UPDATE"]
    has_blocked = any(keyword in sql_upper for keyword in blocked_keywords)
    
    if has_blocked:
        errors.append("Query contains blocked operations (DDL/DML not allowed)")
    
    # Check for SELECT/WITH (allowed operations)
    if not (sql_upper.startswith("SELECT") or sql_upper.startswith("WITH")):
        errors.append("Query must start with SELECT or WITH (CTE)")
    
    # Extract referenced tables (excluding CTEs)
    referenced_tables, cte_names = _extract_table_references(sql)
    
    # Check against allowlist (only validate actual tables, not CTEs)
    allowed_tables = {t.lower() for t in _get_allowed_tables()}
    disallowed_tables = [t for t in referenced_tables if t.lower() not in allowed_tables]
    
    if disallowed_tables:
        errors.append(f"Query references non-allowed tables: {', '.join(disallowed_tables)}")
    
    # Warnings
    if "LIMIT" not in sql_upper and "FETCH" not in sql_upper:
        warnings.append("Query missing LIMIT clause - may return large result set")
    
    if "SELECT *" in sql_upper:
        warnings.append("Query uses SELECT * - consider specifying columns explicitly")
    
    return {
        "valid": len(errors) == 0,
        "errors": errors,
        "warnings": warnings,
        "referenced_tables": referenced_tables,
    }
