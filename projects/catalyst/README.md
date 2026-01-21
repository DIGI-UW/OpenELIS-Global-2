## Catalyst (OGC-70)

This folder contains **Catalyst-specific tooling and supporting services** that
live alongside OpenELIS Global.

### Intended contents

- `projects/catalyst/catalyst-mcp/`: Python MCP server (schema RAG / retrieval)
- `projects/catalyst/catalyst-dev.docker-compose.yml`: Docker Compose for
  Catalyst dev services
- `projects/catalyst/scripts/`: helper scripts (optional)

OpenELIS integration points remain in:

- Backend: `src/main/java/org/openelisglobal/catalyst/`
- Frontend: `frontend/src/components/catalyst/`
- Config: `volume/properties/catalyst.properties`

**Note**: This folder is created to keep Catalyst work scoped to a small surface
area.
