# Download CI Logs

When the user invokes `/download-ci-logs` with optional arguments, run the shim
script so that CI logs are downloaded with safe defaults when the user does not
specify a target.

## Behavior

- **Explicit target**: If the user passes `--pr <number>`, `--branch <name>`, or
  `--run-id <id>`, pass all arguments through to the downloader script.
- **Free text / no target**: If the user passes only free text or no arguments,
  run the shim with **current branch** and **failed-only** (so the downloader is
  invoked with `--branch <current-branch>` and `--failed`).

## Invocation

The agent should parse user arguments and construct a safe command call. Only
pass recognized flags - do NOT pass raw user input directly to the shell.

Run from the repository root:

```bash
.specify/scripts/bash/download-ci-logs-shim.sh [parsed-flags]
```

Where `[parsed-flags]` are validated options from the table below. The shim will
call `scripts/download-ci-logs.sh` with the appropriate options.

## Examples

| User input                            | Effect                           |
| ------------------------------------- | -------------------------------- |
| `/download-ci-logs`                   | Current branch, failed runs only |
| `/download-ci-logs latest failures`   | Current branch, failed runs only |
| `/download-ci-logs --pr 123`          | PR 123                           |
| `/download-ci-logs --pr 123 --failed` | PR 123, failed only              |
| `/download-ci-logs --run-id 12345678` | Specific run by ID               |
| `/download-ci-logs --branch develop`  | Branch develop                   |

## Options (pass-through)

| Option              | Description                                  |
| ------------------- | -------------------------------------------- |
| `--pr <number>`     | PR number to get logs for                    |
| `--branch <name>`   | Branch name to get logs for                  |
| `--run-id <id>`     | Download a specific run by ID                |
| `--workflow <name>` | Filter to specific workflow (e.g., `ci.yml`) |
| `--failed`          | Only download failed runs                    |
| `--list`            | List available runs without downloading      |
| `--limit <n>`       | Max runs to check (default: 10)              |

## Output

Logs are saved under `.cursor/ci-logs/{identifier}-{timestamp}/`. Each workflow
run has its own directory with `summary.txt` and job log files.

## Prerequisites

- `gh` CLI installed and authenticated
- Run from within the repository
