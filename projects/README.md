## Projects

This directory contains **project-scoped tooling and supporting services** that
are developed alongside OpenELIS Global but are not part of the core application
runtime code.

### Goals

- Keep non-OpenELIS tooling **isolated** (Docker Compose env setup, auxiliary
  services, scripts).
- Allow a project to be scoped to **one or a few folders** under `projects/`
  plus only the **required** OpenELIS integration changes (backend `src/`,
  frontend `frontend/`, config under `volume/`).

### Current projects

- `projects/catalyst/` - OGC-70 Catalyst (LLM-powered lab data assistant)
