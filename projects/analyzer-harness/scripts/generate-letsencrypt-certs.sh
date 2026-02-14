#!/usr/bin/env sh
# Generate Let's Encrypt certificates for analyzer harness (e.g. madagascar.openelis-global.org).
# Run from analyzer-harness directory. Writes to repo root volume/letsencrypt so the
# proxy (which mounts ../../volume/letsencrypt) sees the certs.
#
# Required: LETSENCRYPT_EMAIL (or set in .env)
# Default domain: madagascar.openelis-global.org

set -e

HARNESS_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPO_ROOT="$(cd "$HARNESS_DIR/../.." && pwd)"
cd "$HARNESS_DIR"

# Load .env from repo root or harness so LETSENCRYPT_* are set when run by hand
if [ -f "$REPO_ROOT/.env" ]; then set -a; . "$REPO_ROOT/.env"; set +a; fi
if [ -f "$HARNESS_DIR/.env" ]; then set -a; . "$HARNESS_DIR/.env"; set +a; fi

DOMAIN="${LETSENCRYPT_DOMAIN:-madagascar.openelis-global.org}"
EMAIL="${LETSENCRYPT_EMAIL}"
STAGING="${LETSENCRYPT_STAGING:-false}"

if [ -z "$EMAIL" ]; then
  echo "ERROR: LETSENCRYPT_EMAIL is required (set in .env or export)"
  exit 1
fi

# Use repo root volume so proxy bind mount (../../volume/letsencrypt) sees certs
LE_VOLUME="$REPO_ROOT/volume/letsencrypt"
CERTBOT_WEBROOT="$REPO_ROOT/volume/nginx/certbot"
mkdir -p "$LE_VOLUME"
mkdir -p "$CERTBOT_WEBROOT"

echo "Generating Let's Encrypt certificate for ${DOMAIN}..."
echo "Email: ${EMAIL}"
echo "Certificate path: $LE_VOLUME/live/${DOMAIN}/"
echo ""
echo "Preflight: LETSENCRYPT_DOMAIN ($DOMAIN) must point to this host's public IP, and TCP port 80 must be open to the internet (Let's Encrypt HTTP-01 challenge)."
echo ""

# Proxy must be running for ACME webroot (harness proxy name varies by compose project)
if ! docker ps --format '{{.Names}}' | grep -q proxy; then
  echo "ERROR: Proxy container must be running for ACME challenge."
  echo "Start harness: docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml up -d proxy"
  exit 1
fi

CERT_PATH="$LE_VOLUME/live/${DOMAIN}/fullchain.pem"

if [ -f "$CERT_PATH" ]; then
  echo "Certificate for ${DOMAIN} already exists. Renewing..."
  docker run --rm \
    -v "$LE_VOLUME:/etc/letsencrypt" \
    -v "$CERTBOT_WEBROOT:/var/www/certbot" \
    certbot/certbot:latest \
    renew --webroot --webroot-path=/var/www/certbot
  echo "Done. Restart proxy to pick up certs: docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml restart proxy"
  exit 0
fi

STAGING_FLAG=""
[ "$STAGING" = "true" ] && STAGING_FLAG="--staging"

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
  $STAGING_FLAG \
  -d "$DOMAIN"; then
  echo ""
  echo "Let's Encrypt failed. Common causes:"
  echo "  - Timeout/connection: Port 80 is not reachable from the internet (firewall/NAT). Open TCP 80 and ensure DNS for $DOMAIN points to this host."
  echo "  - Wrong domain: If this host is reached at a different hostname (e.g. madagascar.openelis-global.org), set LETSENCRYPT_DOMAIN in .env to that hostname."
  echo "  - Local only: For local dev, use self-signed certs (do not set LETSENCRYPT_* or use --skip-letsencrypt)."
  exit 1
fi

echo "Certificate generated at $CERT_PATH"
echo "Restart proxy: docker compose -f docker-compose.dev.yml -f docker-compose.analyzer-test.yml -f docker-compose.letsencrypt.yml restart proxy"
