# MCP server add-on

An OpenELIS add-on that lets AI agents outside OpenELIS (Claude Code, Claude
Desktop, any [Model Context Protocol](https://modelcontextprotocol.io) client)
read one laboratory's orders, results, worklists, statistics and test catalog.
It uses the add-on mechanism (`docs/addons/design.md`) and changes nothing in
OpenELIS.

- **Read-only.** There is no tool that writes. Every tool is marked
  `readOnlyHint`.
- **One token per agent.** An administrator creates a token for each agent on
  the add-on's page inside OpenELIS (Administration → Extensions → MCP server
  for AI agents → MCP access). The token is shown once; the add-on keeps only
  its SHA-256. A token can expire, be limited to some tools, and be revoked at
  any time.
- **Names and identifiers need two grants.** The add-on asks OpenELIS for
  `patient.identity`, which the administrator sees on the permission screen
  before turning the add-on on. And each agent token has its own "Include
  patient names and identifiers" switch, off by default. Without both, the
  patient tools are not even listed for that agent. Its calls ask OpenELIS for
  the tool's own scopes only (`X-Addon-Scopes`, which can narrow but never widen
  what the add-on holds), so OpenELIS itself returns no name or identifier for
  them; the add-on also removes any that would slip through.
- **Who and why reach OpenELIS.** Every call the add-on makes carries
  `X-Addon-Actor: agent:<token id> <agent name>` and `X-Addon-Purpose: <tool>`,
  so the OpenELIS access log shows which agent asked for what. The agent's token
  is never passed to OpenELIS; the add-on uses its own service key.
- **Limited and recorded.** Calls per token per minute and per day are capped,
  and every tool call is recorded by the add-on, including refused ones.

## Tools

| Tool                  | Needs (scopes granted to the add-on / token switch)               | Returns                                                                                              |
| --------------------- | ----------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| `get_order`           | `orders.search`, `orders.read`, `results.read`                    | an order by lab number and all its results (code, LOINC, name, value, unit, range, status, flag)     |
| `list_orders`         | `orders.search`, `orders.read`                                    | orders received between two dates (≤ 31 days), by status and section, ≤ 100 per page                 |
| `get_worklist`        | `orders.search`                                                   | tests waiting for result entry or validation, oldest first, with waiting time                        |
| `find_patients`       | `patient.identity` · identifiers                                  | ≤ 20 patients by name or exact identifier                                                            |
| `get_patient_results` | `patient.identity`, `results.read`, `orders.search` · identifiers | a patient's newest orders (≤ 20) with results                                                        |
| `lab_statistics`      | `stats.read`                                                      | order counts by day, section or status; turnaround time (median, 90th percentile) by section or test |
| `list_tests`          | `catalog.read`                                                    | the test catalog                                                                                     |

A tool is listed for a token only if OpenELIS currently grants the add-on all its
scopes (the add-on asks OpenELIS every minute), the token's identifiers switch
allows it, and the token's tool list (if any) includes it. Calling a tool that
is not listed gives the tool error `forbidden`. Inputs are checked before
anything reaches OpenELIS: lab numbers `[A-Za-z0-9._-]{1,64}`, dates
`YYYY-MM-DD`, ranges of at most 31 days, page sizes as above.

Each answer has `structuredContent` (described by the tool's `outputSchema`)
and the same data as text after a one-line summary, for clients that read only
text.

## Connecting an agent

Create a token on the add-on's page; it also shows the endpoint, which is
`https://<your OpenELIS>/addons/mcp/mcp`.

Claude Code:

```bash
claude mcp add --transport http openelis https://lims.example.org/addons/mcp/mcp \
  --header "Authorization: Bearer oemcp_1a2b3c4d_…"
```

Claude Desktop (through [`mcp-remote`](https://www.npmjs.com/package/mcp-remote)),
in `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "openelis": {
      "command": "npx",
      "args": [
        "mcp-remote",
        "https://lims.example.org/addons/mcp/mcp",
        "--header",
        "Authorization: Bearer ${OPENELIS_MCP_TOKEN}"
      ],
      "env": { "OPENELIS_MCP_TOKEN": "oemcp_1a2b3c4d_…" }
    }
  }
}
```

MCP Inspector: `npx @modelcontextprotocol/inspector`, transport "Streamable
HTTP", the URL above, and an `Authorization: Bearer …` header.

The endpoint speaks the current protocol revision (2026-07-28, stateless
Streamable HTTP) and also serves 2025-11-25 clients (`initialize`), statelessly,
from the same tool definitions (official TypeScript SDK v2,
`createMcpHandler`). It answers in JSON; `GET` and `DELETE` are 405. A request
with an `Origin` header is refused (403) unless that origin is the laboratory's
own address or listed in `allowedOrigins`.

A request without a valid token gets 401 with
`WWW-Authenticate: Bearer resource_metadata="https://<lab>/.well-known/oauth-protected-resource/addons/mcp/mcp"`,
and that document (RFC 9728) is served. It names no authorization server yet:
see [Next: OAuth](#next-oauth).

## The record of calls

Every tool call is one line in `/data/audit-YYYY-MM-DD.jsonl` (UTC date):

```json
{
  "at": "2026-10-22T08:15:02.114Z",
  "agent": "a7f3c2d1",
  "agentName": "Claude Code — Dr Lan",
  "client": "claude-code/2.1",
  "proto": "2026-07-28",
  "ip": "100.64.0.9",
  "tool": "get_order",
  "args": { "labNumber": "0221026-0004" },
  "outcome": "ok",
  "rows": 12,
  "core": [
    { "endpoint": "/orders/{accession}", "status": 200 },
    { "endpoint": "/orders/{accession}/results", "status": 200 }
  ],
  "ms": 84
}
```

`outcome` is `ok`, `denied` (tool not allowed, or a scope OpenELIS refused),
`invalid`, `rate_limited`, `not_found`, `error`, or `unauthorized` (a request
without a valid token; no agent). Arguments are recorded, search text included:
this is the laboratory's own record, readable only by administrators on the
add-on's page (latest 200, by agent). Files older than `auditRetentionDays` are
deleted daily. OpenELIS keeps its own access log of what the add-on read.

## Configuration (Administration → Extensions)

| Key                  | Default | Meaning                                                                               |
| -------------------- | ------- | ------------------------------------------------------------------------------------- |
| `ratePerMinute`      | 30      | calls per token per minute (token bucket)                                             |
| `ratePerDay`         | 1000    | calls per token per day                                                               |
| `timeZone`           | `UTC`   | the laboratory's day for `ratePerDay` (IANA name, e.g. `Asia/Ho_Chi_Minh`)            |
| `auditRetentionDays` | 180     | days the record of calls is kept; local regulation may require longer                 |
| `allowedOrigins`     | empty   | browser origins allowed to call `/mcp`, comma separated; empty = `PUBLIC_ORIGIN` only |

Over a limit, a call gets the tool error `rate_limited` with the seconds to
wait.

Environment of the container:

| Variable                                | Meaning                                                                          |
| --------------------------------------- | -------------------------------------------------------------------------------- |
| `OPENELIS_API_URL`, `OPENELIS_API_KEY`  | from the compose snippet OpenELIS prints at install                              |
| `PUBLIC_ORIGIN`                         | the laboratory's address as agents reach it, e.g. `https://lims.example.org`     |
| `NODE_EXTRA_CA_CERTS`                   | a PEM file with the CA of OpenELIS's certificate, to verify it (recommended)     |
| `OPENELIS_TLS_INSECURE=1`               | accept OpenELIS's self-signed certificate without verifying it (default: verify) |
| `MCP_PUBLIC_PATH`                       | path of the endpoint at `PUBLIC_ORIGIN` (default `/addons/mcp/mcp`)              |
| `MCP_METADATA_PATH`                     | path of the RFC 9728 metadata (default derived from `MCP_PUBLIC_PATH`)           |
| `PORT` (8080), `MCP_DATA_DIR` (`/data`) |                                                                                  |

## Run

```bash
./pack.sh pack mcp.zip                  # the package (manifest + ui/); sign it: ./pack.sh pack mcp.zip <publisher>
podman build -t openelis-addon-mcp:1.0.0 .
```

Install the package, then add the service from the compose snippet OpenELIS
prints, with a volume and the public address:

```yaml
mcp:
  image: ghcr.io/digitalversion/openelis-addon-mcp@sha256:…
  restart: unless-stopped
  read_only: true
  cap_drop: [ALL]
  security_opt: ["no-new-privileges:true"]
  user: "65534:65534"
  mem_limit: 128m
  environment:
    OPENELIS_API_URL: https://oe.openelis.org:8443/api/OpenELIS-Global/rest/addon-api/v1
    OPENELIS_API_KEY: oeak_…
    OPENELIS_TLS_INSECURE: "1"
    PUBLIC_ORIGIN: https://lims.example.org
  volumes: ["mcp-data:/data"] # tokens (hashes) and the record of calls
```

The add-on needs nothing outside OpenELIS (no `network.egress`): put it on an
internal network with the OpenELIS webapp only.

Reverse proxy (in the laboratory's server block): the endpoint and the admin
page's API under one prefix, without cookies either way, and the metadata
document at the root:

```nginx
location /addons/mcp/ {
    proxy_pass http://mcp:8080/;
    proxy_set_header Cookie "";
    proxy_hide_header Set-Cookie;
    proxy_set_header X-Forwarded-For $remote_addr;   # the audit takes the first address
}
location = /.well-known/oauth-protected-resource/addons/mcp/mcp {
    proxy_pass http://mcp:8080/.well-known/oauth-protected-resource/addons/mcp/mcp;
    proxy_set_header Cookie "";
}
```

A site that already routes `/addons/<id>/api/` to each add-on (for the add-on
pages' own API) needs no new location: set `MCP_PUBLIC_PATH=/addons/mcp/api/mcp`;
the endpoint is then `https://<lab>/addons/mcp/api/mcp` and the metadata
`https://<lab>/addons/mcp/api/.well-known/oauth-protected-resource`.

Review the permissions on the Extensions page (note `patient.identity`) and turn
the add-on on.

## Service API

|                                                              |                                                                 |
| ------------------------------------------------------------ | --------------------------------------------------------------- |
| `POST /mcp`                                                  | the MCP endpoint (agent token)                                  |
| `GET /api/status`                                            | granted scopes, limits, endpoint URL                            |
| `GET /api/agents`                                            | tokens, without secrets                                         |
| `POST /api/agents`                                           | `{name, identifiers, tools?, expiresInDays?}` → the token, once |
| `POST /api/agents/{id}/revoke`                               |                                                                 |
| `GET /api/audit?from&to&agent&limit`                         | the record of calls, newest first (≤ 500)                       |
| `GET /.well-known/oauth-protected-resource[/addons/mcp/mcp]` | RFC 9728 metadata                                               |
| `GET /health`                                                | no authentication                                               |

`/api/*` is called by the add-on's page with the frame's OpenELIS UI token. The
service asks OpenELIS who that token belongs to (`whoami` with that token) and
accepts it only if it is a user token issued for `addon-settings-page-slot`,
which OpenELIS gives to administrators only. CORS: any origin, no credentials
(the page has an opaque origin).

## Next: OAuth

Claude Desktop and claude.ai custom connectors prefer OAuth 2.1. The next step
adds an authorization server to the add-on: authorization server metadata,
dynamic client registration, PKCE (S256), tokens bound to this endpoint
(`resource`), and an approval step that an administrator completes on the
add-on's page inside OpenELIS, with the identifiers switch off by default.
OAuth grants will sit in the same store as the bearer tokens and be revoked the
same way. The 401 challenge already points to the metadata document, so bearer
clients keep working when it arrives.

## Tests

`npm test` (vitest): token creation, hashing, expiry and revocation; tool
filtering by granted scopes, the identifiers switch and the token's tool list;
removal of patient data without identifiers; input caps; the per-minute and
per-day limits (day in the laboratory's time zone); the Origin check (403); the
401 challenge; the admin API's slot check; the shape of a record line; and an
end-to-end run against a fake OpenELIS with the official MCP client, once on
2026-07-28 and once as a 2025-11-25 client. `npm run typecheck` and
`npm run lint` (prettier) check types and formatting.
