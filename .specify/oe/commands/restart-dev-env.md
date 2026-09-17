# Restart Dev Environment

When the user invokes `/restart-dev-env`, restart the current checkout through
`scripts/dev-stack`. This is the supported local development interface; it
owns the build, worktree-scoped containers, readiness, TLS, and application
scenario initialization. Do not reproduce its internals with direct Compose
commands or SQL fixture loaders.

## User Input

```text
$ARGUMENTS
```

Supported arguments:

- Default: rebuild and restart, preserving database volumes.
- `--skip-build`: use existing build artifacts (`dev-stack up --skip-build`).
- `--full-reset`: explicitly request deletion of this worktree's stack volumes.
- `--skip-fixtures`: legacy spelling for skipping application scenario
  initialization (`dev-stack up --no-scenarios`); no SQL fixtures are loaded.

## Workflow

1. Verify the checkout using `git rev-parse --show-toplevel` and
   `git status --short`. Preserve uncommitted work. Use this repository root for
   all following commands. Never print `.env` contents or credentials.
2. Stop the current stack with `scripts/dev-stack down`. Only when the user
   explicitly requests `--full-reset`, use
   `scripts/dev-stack down --volumes --yes` instead; this deletes its volumes.
3. Run `scripts/dev-stack up`, appending `--skip-build` and/or `--no-scenarios`
   only for the corresponding requested options. The launcher waits for
   readiness and initializes application scenarios by default. A failed build,
   readiness check, or scenario initialization is a failure to report, not a
   successful restart.
4. Run `scripts/dev-stack status` and `scripts/dev-stack url`. Report the
   observed status, actual URL, whether the build was skipped, whether volumes
   were reset, and whether scenario initialization succeeded or was skipped.
   Inspect failures using `scripts/dev-stack logs oe.openelis.org`.
5. Before subsequent Playwright commands, export this worktree's endpoints:

   ```bash
   eval "$(scripts/dev-stack env)"
   ```

## Domain Configuration

The launcher reads `.env` and handles local or domain-based TLS. Local mode
uses dynamically assigned loopback ports; do not assume `https://localhost/`
or a shared analyzer domain. For domain mode, configure `LETSENCRYPT_DOMAIN`
and `LETSENCRYPT_EMAIL` in `.env` as described in `AGENTS.md`. If startup fails,
report the actual error and relevant logs rather than claiming a certificate
fallback succeeded.
