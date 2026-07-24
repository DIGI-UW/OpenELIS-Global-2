# Grist UAT bridge (MCP authoring + live widget read)

One small service (`server.mjs`) that makes the Grist "UAT Checklists" doc the
single source of truth for the reviewer overlays:

- `POST /mcp` — MCP over Streamable HTTP, **bearer-gated**. The LLM authoring
  surface: create/update/delete checklist steps and per-instance meta.
- `GET /uat/<instance>.json` — **public read**. Returns the exact shape the
  reviewer widget already consumes, computed live from Grist. The router points
  `https://<amr|analyzers>…/__review/uat-<instance>.json` at this, so edits made
  in Grist (by a human) or via MCP (by an LLM) show up immediately — no publish
  step, no static-file regeneration.

The Grist API key lives only inside this service (mounted read-only from the box
state dir); callers never see it. Reads are public but read-only and non-secret;
writes require a token.

## Tools

| Tool | Purpose |
|------|---------|
| `uat_list_instances` | list instances (amr, analyzers) with title + Jira key |
| `uat_get` | full checklist for one instance, each step with its Grist row `id` |
| `uat_upsert_step` | create a step (omit `id`) or update one in place (pass `id`) |
| `uat_delete_step` | delete a step by row `id` |
| `uat_set_meta` | upsert an instance's title / intro / jira |

Upserts target Grist's built-in row `id`, so an LLM editing one step never
clobbers a human editing another — no whole-table replace.

## Connect (Claude Code CLI)

Each person gets their own token. On the box:

```bash
cd /opt/oe-edge/deploy/dual-subdomain/grist/mcp
./mcp-token.sh generate "Alice (CLI)"     # prints the token + the exact add command
./mcp-token.sh list                        # who has a token
./mcp-token.sh revoke "Alice (CLI)"        # cut one person off
```

`generate` prints a ready-to-run line:

```bash
claude mcp add --transport http grist-uat https://grist.openelis-global.org/mcp --header "Authorization: Bearer <token>"
```

Tokens live in `/home/ubuntu/oe-grist/mcp-tokens.json` (box-side, never
committed). The server re-reads that file per request, so `generate`/`revoke`
take effect without a restart.

> **claude.ai web/desktop:** the connector UI expects OAuth and may not let you
> set a static header — the CLI one-liner is the smooth path today. Proper OAuth
> (via the Dex we already run) is the planned follow-up; the bearer check here is
> the exact seam it drops into.

## Auth model

A single verifier checks the caller's `Bearer` token against the box-side file.
That is deliberately the same shape as OAuth token verification, so swapping the
static check for "validate a Dex-issued token" is a local change to one function.
