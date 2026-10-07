# Restart Development Environment

When the user invokes `/restart-dev-env`, use the supported development launcher
from the owning checkout. Do not invent Compose layering, load SQL fixtures, or
choose a domain for the user.

```text
$ARGUMENTS
```

Inspect the checkout and its changes, then run:

```bash
scripts/dev-stack doctor
scripts/dev-stack up
scripts/dev-stack status
scripts/dev-stack url
```

`up` builds the current source and pinned dependencies, recreates the running
backend, waits for login readiness and prepares development scenarios through
application services. It preserves existing data. The launcher reads `.env` and
owns local or public-domain certificate setup; a missing public-domain setting
must not silently target a shared server.

Supported options:

- `--skip-build`: pass to `dev-stack up` to reuse this checkout's already-built
  WAR and application images. Missing build artifacts must fail.
- `--full-reset`: only when explicitly requested, run
  `scripts/dev-stack down --volumes --yes` before `up`. This deletes the owning
  development stack's data.
- `--skip-fixtures`: obsolete; this path never loads SQL test fixtures. Use
  `dev-stack up --no-scenarios` if the user wants to skip application scenarios.

Stop and report a failed build, readiness or certificate operation. Show relevant
logs through `scripts/dev-stack logs`; do not report success after a failed step.
For CI reproduction, use `scripts/run-ci-checks.sh --job NAME`. A development restart is not CI validation.
