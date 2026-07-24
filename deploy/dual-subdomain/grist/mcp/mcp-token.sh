#!/usr/bin/env bash
set -euo pipefail
# Manage bearer tokens for the Grist UAT MCP server. Tokens live in a box-side
# JSON file (never committed), mounted read-only into the MCP container. The
# server re-reads the file per request, so generate/revoke take effect at once.
#
#   ./mcp-token.sh generate "Alice (CLI)"   # mint a token, print the connect line
#   ./mcp-token.sh list                      # created / label / token prefix
#   ./mcp-token.sh revoke "Alice (CLI)"      # remove every token with that label
#
# Env: TOKENS_FILE (default /home/ubuntu/oe-grist/mcp-tokens.json),
#      MCP_URL (default https://grist.openelis-global.org/mcp)

TOKENS_FILE="${TOKENS_FILE:-/home/ubuntu/oe-grist/mcp-tokens.json}"
MCP_URL="${MCP_URL:-https://grist.openelis-global.org/mcp}"

ensure() {
  [ -f "$TOKENS_FILE" ] || echo "[]" > "$TOKENS_FILE"
  chmod 600 "$TOKENS_FILE"
  # The MCP container runs as uid 1000 and mounts this file read-only, so it must
  # be owned by 1000 to be readable even when this script runs as root (over SSM).
  chown 1000:1000 "$TOKENS_FILE" 2>/dev/null || true
}

case "${1:-}" in
  generate)
    label="${2:?usage: generate <label>}"
    ensure
    tok="uat_$(openssl rand -hex 24)"
    python3 - "$TOKENS_FILE" "$tok" "$label" <<'PY'
import json, sys, datetime
f, tok, label = sys.argv[1], sys.argv[2], sys.argv[3]
a = json.load(open(f))
a.append({"token": tok, "label": label, "created": datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ")})
json.dump(a, open(f, "w"), indent=2)
PY
    echo "token for '$label':"
    echo "  $tok"
    echo
    echo "connect (Claude Code CLI):"
    echo "  claude mcp add --transport http grist-uat $MCP_URL --header \"Authorization: Bearer $tok\""
    ;;
  list)
    ensure
    python3 - "$TOKENS_FILE" <<'PY'
import json, sys
a = json.load(open(sys.argv[1]))
if not a:
    print("(no tokens)"); raise SystemExit
for e in a:
    print(f'{e.get("created","?"):22}  {e.get("label","?"):32}  {e["token"][:12]}...')
PY
    ;;
  revoke)
    label="${2:?usage: revoke <label>}"
    ensure
    python3 - "$TOKENS_FILE" "$label" <<'PY'
import json, sys
f, label = sys.argv[1], sys.argv[2]
a = json.load(open(f))
keep = [e for e in a if e.get("label") != label]
json.dump(keep, open(f, "w"), indent=2)
print(f"removed {len(a) - len(keep)} token(s) for '{label}'")
PY
    ;;
  *)
    echo "usage: $0 {generate <label> | list | revoke <label>}" >&2
    exit 1
    ;;
esac
