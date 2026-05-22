#!/usr/bin/env sh
# Generate Let's Encrypt certificates for the analyzer harness.
#
# Writes to repo root volume/letsencrypt so the proxy (which bind-mounts
# ../../volume/letsencrypt) sees the certs.
#
# Required env (via .env):
#   LETSENCRYPT_DOMAIN         — primary public hostname (no silent default)
#   LETSENCRYPT_EMAIL          — registration email
#
# Optional env:
#   LETSENCRYPT_BRIDGE_DOMAIN  — bridge upload UI hostname
#                                (defaults to "bridge.<LETSENCRYPT_DOMAIN>")
#   LETSENCRYPT_STAGING=true   — use LE staging CA (untrusted cert, high rate-
#                                limit budget). Replaces the cert lineage with
#                                a staging cert — browser trust warnings until
#                                a prod reissue. Safe against prod rate caps.
#   LETSENCRYPT_DRY_RUN=true   — ACME challenge against staging, issue nothing
#                                to disk. Validates routing without touching
#                                any cert on disk. Can combine with STAGING
#                                (dry-run implicitly uses staging).
#
# A single `certonly --expand` call handles all cases (new lineage, existing
# lineage subset, add/remove SANs). Avoids the silent-drop trap where certbot
# renew on an existing lineage ignores new -d flags.

set -e

HARNESS_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPO_ROOT="$(cd "$HARNESS_DIR/../.." && pwd)"
cd "$HARNESS_DIR"

# Load .env from repo root or harness so LETSENCRYPT_* are set when run by hand
if [ -f "$REPO_ROOT/.env" ]; then set -a; . "$REPO_ROOT/.env"; set +a; fi
if [ -f "$HARNESS_DIR/.env" ]; then set -a; . "$HARNESS_DIR/.env"; set +a; fi

if [ -z "${LETSENCRYPT_DOMAIN:-}" ]; then
  echo "ERROR: LETSENCRYPT_DOMAIN is not set. Add it to .env before running."
  exit 1
fi
if [ -z "${LETSENCRYPT_EMAIL:-}" ]; then
  echo "ERROR: LETSENCRYPT_EMAIL is required (set in .env or export)"
  exit 1
fi

DOMAIN="$LETSENCRYPT_DOMAIN"
BRIDGE_DOMAIN="${LETSENCRYPT_BRIDGE_DOMAIN:-bridge.${DOMAIN}}"
EMAIL="$LETSENCRYPT_EMAIL"
STAGING="${LETSENCRYPT_STAGING:-false}"
DRY_RUN="${LETSENCRYPT_DRY_RUN:-false}"

# Use repo root volume so proxy bind mount (../../volume/letsencrypt) sees certs
LE_VOLUME="$REPO_ROOT/volume/letsencrypt"
CERTBOT_WEBROOT="$REPO_ROOT/volume/nginx/certbot"
mkdir -p "$LE_VOLUME"
mkdir -p "$CERTBOT_WEBROOT"

MODE="prod"
[ "$STAGING" = "true" ] && MODE="staging"
# certbot's --dry-run implicitly uses the staging CA, so the label always
# reflects both. The earlier form used ${STAGING:+/staging}, which expands
# whenever STAGING is non-empty — and STAGING is always "true" or "false",
# so the suffix was never omitted. Explicit label avoids the trap.
[ "$DRY_RUN" = "true" ] && MODE="dry-run/staging"

echo "Let's Encrypt certificate issuance (mode: ${MODE})"
echo "  Primary domain: ${DOMAIN}"
echo "  Bridge domain : ${BRIDGE_DOMAIN}"
echo "  Email         : ${EMAIL}"
echo "  Lineage path  : $LE_VOLUME/live/${DOMAIN}/"
echo ""
echo "Preflight: both domains must resolve to this host's public IP, and TCP"
echo "port 80 must be reachable from the internet (HTTP-01 challenge)."
echo ""

# Proxy must be running for ACME webroot (harness proxy name varies by compose project)
if ! docker ps --format '{{.Names}}' | grep -q proxy; then
  echo "ERROR: Proxy container must be running for ACME challenge."
  echo "Start harness: docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml up -d proxy"
  exit 1
fi

STAGING_FLAG=""
[ "$STAGING" = "true" ] && STAGING_FLAG="--staging"
DRY_RUN_FLAG=""
[ "$DRY_RUN" = "true" ] && DRY_RUN_FLAG="--dry-run"

if ! docker run --rm \
  -v "$LE_VOLUME:/etc/letsencrypt" \
  -v "$CERTBOT_WEBROOT:/var/www/certbot" \
  certbot/certbot:latest \
  certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  --non-interactive \
  --expand \
  $STAGING_FLAG \
  $DRY_RUN_FLAG \
  -d "$DOMAIN" \
  -d "$BRIDGE_DOMAIN"; then
  echo ""
  echo "Let's Encrypt failed. Common causes:"
  echo "  - Timeout/connection: TCP 80 unreachable from the internet (firewall/NAT)."
  echo "  - Wrong DNS: ${DOMAIN} or ${BRIDGE_DOMAIN} does not resolve to this host."
  echo "  - Rate limited: rerun with LETSENCRYPT_STAGING=true to avoid prod caps."
  echo "  - Local only: skip LE entirely (do not pass --letsencrypt) for self-signed."
  exit 1
fi

if [ "$DRY_RUN" = "true" ]; then
  echo ""
  echo "Dry-run succeeded — ACME workflow validated for both domains. No cert"
  echo "was written. Re-run without LETSENCRYPT_DRY_RUN to issue for real."
  exit 0
fi

echo ""
CERT_PATH="$LE_VOLUME/live/${DOMAIN}/fullchain.pem"
echo "Certificate written to $CERT_PATH"
echo "Restart proxy: docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml restart proxy"
