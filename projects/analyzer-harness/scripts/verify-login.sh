#!/bin/bash
# verify-login.sh - Verify admin login works against the harness
#
# Usage: ./scripts/verify-login.sh [--base-url URL]
#
# Reads TEST_USER and TEST_PASS from the environment (typically loaded from
# repo root .env via `set -a; . .env; set +a`). Falls back to admin/adminADMIN!
#
# Uses --data-urlencode so the ! in the password is correctly sent as %21.

set -e

BASE_URL="${1:-https://localhost}"
# Strip --base-url flag if present
[ "$1" = "--base-url" ] && BASE_URL="${2:-https://localhost}"

USER="${TEST_USER:-admin}"
PASS="${TEST_PASS:-adminADMIN!}"

LOGIN_URL="${BASE_URL}/api/OpenELIS-Global/ValidateLogin?apiCall=true"

RESULT=$(curl -sk \
    --data-urlencode "loginName=${USER}" \
    --data-urlencode "password=${PASS}" \
    "$LOGIN_URL" 2>/dev/null)

if echo "$RESULT" | grep -qE '"authenticated"\s*:\s*true|"success"\s*:\s*true'; then
    echo "Login OK: ${USER} @ ${BASE_URL}"
    exit 0
else
    echo "Login FAILED: ${USER} @ ${BASE_URL}"
    echo "Response: $RESULT"
    exit 1
fi
