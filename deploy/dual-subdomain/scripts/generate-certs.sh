#!/usr/bin/env bash
set -euo pipefail
# =============================================================================
# Issue Let's Encrypt certs for BOTH subdomains against the umbrella router.
#
# Two independent single-domain certs (not one multi-SAN) — decoupled per the
# deploy decision. Reuses the exact `certbot certonly --webroot` invocation from
# scripts/generate-letsencrypt-certs.sh; the only adaptation is that it targets
# the umbrella router (oe-edge-router) + its shared webroot, not openelisglobal-proxy.
#
# Precondition: DNS for both domains resolves to this host, and the router is up
# on :80 (deploy.sh guarantees this before calling here). Idempotent: skips a
# domain whose lineage already exists.
#
# Env: AMR_DOMAIN, ANALYZERS_DOMAIN, LETSENCRYPT_EMAIL, [LETSENCRYPT_STAGING],
#      LETSENCRYPT_DIR (host path mounted at the router's /etc/letsencrypt),
#      CERTBOT_WEBROOT (host path mounted at the router's /var/www/certbot),
#      [ROUTER_CONTAINER_NAME=oe-edge-router]
# =============================================================================

: "${AMR_DOMAIN:?}"; : "${ANALYZERS_DOMAIN:?}"; : "${LETSENCRYPT_EMAIL:?}"
: "${LETSENCRYPT_DIR:?}"; : "${CERTBOT_WEBROOT:?}"
ROUTER="${ROUTER_CONTAINER_NAME:-oe-edge-router}"
STAGING_FLAG=""; [ "${LETSENCRYPT_STAGING:-false}" = "true" ] && STAGING_FLAG="--staging"

if ! docker ps --format '{{.Names}}' | grep -qx "$ROUTER"; then
  echo "ERROR: router container '$ROUTER' must be running on :80 for the ACME challenge" >&2
  exit 1
fi

issue_one() {
  domain="$1"
  if [ -f "$LETSENCRYPT_DIR/live/$domain/fullchain.pem" ]; then
    echo "✓ cert for $domain already exists — skipping (use certbot-renew.sh to renew)"
    return 0
  fi
  echo ">> issuing cert for $domain ${STAGING_FLAG:+(staging)}"
  docker run --rm \
    -v "$LETSENCRYPT_DIR:/etc/letsencrypt" \
    -v "$CERTBOT_WEBROOT:/var/www/certbot" \
    certbot/certbot:latest \
    certonly --webroot --webroot-path=/var/www/certbot \
    --email "$LETSENCRYPT_EMAIL" --agree-tos --no-eff-email --non-interactive \
    $STAGING_FLAG -d "$domain"
}

issue_one "$AMR_DOMAIN"
issue_one "$ANALYZERS_DOMAIN"
[ -n "${GRIST_DOMAIN:-}" ] && issue_one "$GRIST_DOMAIN"

echo ">> reloading router so it serves the issued certs (entrypoint re-resolves LE paths)"
docker restart "$ROUTER" >/dev/null
echo "✓ done — both domains should now serve valid Let's Encrypt certificates"
