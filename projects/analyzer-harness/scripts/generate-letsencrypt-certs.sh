#!/usr/bin/env sh
# Generate Let's Encrypt certificates for analyzer harness (analyzers.openelis-global.org).
# Run from analyzer-harness directory. Uses same pattern as repo root script.
#
# Required: LETSENCRYPT_EMAIL (or set in .env)
# Default domain: analyzers.openelis-global.org

set -e

HARNESS_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$HARNESS_DIR"

DOMAIN="${LETSENCRYPT_DOMAIN:-analyzers.openelis-global.org}"
EMAIL="${LETSENCRYPT_EMAIL}"
STAGING="${LETSENCRYPT_STAGING:-false}"

if [ -z "$EMAIL" ]; then
  echo "ERROR: LETSENCRYPT_EMAIL is required (set in .env or export)"
  exit 1
fi

mkdir -p ./volume/letsencrypt
mkdir -p ./volume/nginx/certbot

echo "Generating Let's Encrypt certificate for ${DOMAIN}..."
echo "Email: ${EMAIL}"

# Proxy must be running for ACME webroot (harness proxy name varies by compose project)
if ! docker ps --format '{{.Names}}' | grep -q proxy; then
  echo "ERROR: Proxy container must be running for ACME challenge."
  echo "Start harness: docker compose -f docker-compose.dev.yml -f docker-compose.letsencrypt.yml up -d proxy"
  exit 1
fi

CERT_PATH="./volume/letsencrypt/live/${DOMAIN}/fullchain.pem"

if [ -f "$CERT_PATH" ]; then
  echo "Certificate for ${DOMAIN} already exists. Renewing..."
  docker run --rm \
    -v "$(pwd)/volume/letsencrypt:/etc/letsencrypt" \
    -v "$(pwd)/volume/nginx/certbot:/var/www/certbot" \
    certbot/certbot:latest \
    renew --webroot --webroot-path=/var/www/certbot
  echo "Done. Restart proxy to pick up certs: docker compose -f docker-compose.dev.yml -f docker-compose.letsencrypt.yml restart proxy"
  exit 0
fi

STAGING_FLAG=""
[ "$STAGING" = "true" ] && STAGING_FLAG="--staging"

docker run --rm \
  -v "$(pwd)/volume/letsencrypt:/etc/letsencrypt" \
  -v "$(pwd)/volume/nginx/certbot:/var/www/certbot" \
  certbot/certbot:latest \
  certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  --non-interactive \
  $STAGING_FLAG \
  -d "$DOMAIN"

echo "Certificate generated at ${CERT_PATH}"
echo "Restart proxy: docker compose -f docker-compose.dev.yml -f docker-compose.letsencrypt.yml restart proxy"
