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

- `projects/analyzer-harness/` — internal Compose configuration, scenario
  seeding, and analyzer testing/recording tools. The public source-development
  launcher is `scripts/dev-stack`; full local CI is `scripts/run-ci-checks.sh`.
- `projects/reporting-uat/` — runtime qualification tools for the isolated
  reporting UAT deployment.
