# Grist authoring layer for the UAT checklists

Light self-hosted [Grist](https://www.getgrist.com/) that owns the **authoring**
side of the reviewer overlay: Casey/Beth edit the UAT steps in a spreadsheet UI,
and a generator turns the table back into the `uat-*.json` the overlay serves.

Feedback stays as-is (the client-side download → Claude). This layer is
authoring-only for now; a future Claude skill can fill the `UAT_Results` table
(already in the schema) from the downloaded feedback reports — no migration needed.

## Flow

```
review/uat-*.json ──seed──▶  Grist (UAT_Meta, UAT_Steps)  ──generate──▶ review/uat-*.json
    (initial content)          edited by Casey/Beth in UI        (served by the router)
```

Grist is the source of truth once seeded; `generate` is the only writer of the
served JSON thereafter. The overlay picks up changes immediately (`no-store`).

## Schema

| Table | Columns | Role |
|---|---|---|
| `UAT_Meta` | instance, title, intro, jira | per-module header |
| `UAT_Steps` | instance, section, section_order, step_order, do, expect, route | the checklist |
| `UAT_Results` | reviewer, instance, step_key, mark, note, page_url, at | **future** aggregation target |

## Run (on the box, as `ubuntu`)

```bash
bash bootstrap.sh            # grist up + mint API key + seed from review/uat-*.json
# → author in the Grist UI (grist.openelis-global.org, once DNS + cert are up)
bash bootstrap.sh generate  # Grist → review/uat-*.json  (overlay updates live)
```

`grist-sync.mjs` (`seed` | `generate`) does the API work; `bootstrap.sh` wraps it
with the container lifecycle + the headless API-key step.

## The headless API-key step (why bootstrap injects it)

Self-hosted Grist has **no env-var path to an API key** — normally you click
through the UI. `bootstrap.sh` mints one non-interactively by writing
`users.api_key` in `home.sqlite3` for the admin user (`GRIST_DEFAULT_EMAIL`), then
uses it as the bearer token. The key is stored box-side in `~/oe-grist/.api-key`
(never committed).

## Public UI access (grist.openelis-global.org)

The container binds no host ports; humans reach the Grist UI only through the
umbrella router vhost, behind HTTP Basic Auth + TLS. Requires:
1. DNS: `grist.openelis-global.org` → the host EIP.
2. `./deploy.sh certs` (issues the third LE cert alongside amr/analyzers).
3. An htpasswd file on the box (`router/grist.htpasswd`) — generated at deploy, creds printed once.

Until DNS is up, the authoring pipeline still works headlessly via `bootstrap.sh`
(it reaches Grist over the internal network).

## Light-mode caveats

`GRIST_SANDBOX_FLAVOR=unsandboxed` — fine for an internal, single-tenant,
basic-auth-gated authoring instance; do not expose formula editing to untrusted
users this way. Data lives in the `oe-grist_grist-data` volume (built-in SQLite,
no external DB).
