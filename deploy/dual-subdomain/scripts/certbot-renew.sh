#!/usr/bin/env bash
set -euo pipefail
# =============================================================================
# Renew all Let's Encrypt lineages, then reload the umbrella router.
#
# Adapted from scripts/certbot-renew.sh. `certbot renew` already auto-discovers
# every lineage under /etc/letsencrypt/renewal/, so no per-domain loop is needed.
# The ONE change from the original is the reload target: the umbrella router
# (oe-edge-router), parameterized — not the hard-coded openelisglobal-proxy.
#
# Installed by `deploy.sh configure` as a twice-daily host cron entry
# (/etc/cron.d/oe-edge-certbot-renew). Run manually anytime to force a check.
#
# Env: LETSENCRYPT_DIR, CERTBOT_WEBROOT, [ROUTER_CONTAINER_NAME=oe-edge-router]
# =============================================================================

: "${LETSENCRYPT_DIR:?}"; : "${CERTBOT_WEBROOT:?}"
ROUTER="${ROUTER_CONTAINER_NAME:-oe-edge-router}"

echo ">> [$(date -u +%FT%TZ)] certbot renew"
docker run --rm \
  -v "$LETSENCRYPT_DIR:/etc/letsencrypt" \
  -v "$CERTBOT_WEBROOT:/var/www/certbot" \
  certbot/certbot:latest \
  renew --webroot --webroot-path=/var/www/certbot

# Reload only if the router is up. Restart makes the entrypoint re-resolve the
# (possibly renewed) LE cert paths; a plain `nginx -s reload` also re-reads them.
if docker ps --format '{{.Names}}' | grep -qx "$ROUTER"; then
  docker exec "$ROUTER" nginx -s reload 2>/dev/null || docker restart "$ROUTER" >/dev/null
  echo ">> reloaded $ROUTER"
fi
