import os
from dataclasses import dataclass
from typing import Optional


@dataclass(frozen=True)
class DatabaseConfig:
    """Database configuration for MCP server schema extraction."""
    
    host: str
    port: int
    database: str
    username: str
    password: str
    schema: str = "clinlims"  # OpenELIS default schema
    
    @property
    def connection_string(self) -> str:
        """Build PostgreSQL connection string."""
        return (
            f"postgresql://{self.username}:{self.password}@"
            f"{self.host}:{self.port}/{self.database}?options=-csearch_path={self.schema}"
        )


def load_database_config() -> Optional[DatabaseConfig]:
    """
    Load database configuration from environment variables.
    
    M0.0: Returns None (not used, mocks used instead)
    M1+: Returns config for real PostgreSQL connection
    """
    if not os.getenv("MCP_DB_ENABLED", "false").lower() == "true":
        return None
    
    return DatabaseConfig(
        host=os.getenv("MCP_DB_HOST", "db.openelis.org"),
        port=int(os.getenv("MCP_DB_PORT", "5432")),
        database=os.getenv("MCP_DB_NAME", "clinlims"),
        username=os.getenv("MCP_DB_USER", "catalyst_schema_reader"),
        password=os.getenv("MCP_DB_PASSWORD", ""),
        schema=os.getenv("MCP_DB_SCHEMA", "clinlims"),
    )
