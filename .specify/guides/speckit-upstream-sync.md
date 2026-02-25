# SpecKit (vendored from upstream)

This project vendors SpecKit scaffolding under `.specify/`, based on upstream
**`github/spec-kit`**: [github/spec-kit](https://github.com/github/spec-kit).

Pinned upstream reference (last sync):

- Tag: `v0.0.90`
- Commit: `9111699cd27879e3e6301651a03e502ecb6dd65d`

What we keep in sync (as needed):

- `.specify/templates/commands/*` (upstream command templates)
- `.specify/scripts/powershell/*` (PowerShell parity for our bash scripts)
- `.specify/core/commands/*` (upstream command definitions)

Project-specific files we intentionally maintain separately:

- `.specify/memory/constitution.md`
- `.specify/guides/*`
- OpenELIS-specific constraints in `.specify/templates/*` and OpenELIS
  branch/spec-dir mapping behavior in `.specify/scripts/*`
- `.specify/oe/commands/*` (OpenELIS command extensions)
