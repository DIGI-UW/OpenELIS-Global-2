#!/bin/sh
set -eu
# =============================================================================
# Umbrella router entrypoint — per-domain cert resolution + template render.
#
# Generalises nginx-proxy/docker-entrypoint.sh (single LETSENCRYPT_DOMAIN) to a
# fixed set of domains, each with its OWN cert pair rendered into the template,
# so one nginx process can terminate TLS for both subdomains at once.
#
# For each domain: use the Let's Encrypt lineage under
# /etc/letsencrypt/live/<domain>/ if present, else generate a per-domain
# self-signed fallback so nginx can start before ACME has ever run.
# =============================================================================

: "${AMR_DOMAIN:?AMR_DOMAIN is required}"
: "${ANALYZERS_DOMAIN:?ANALYZERS_DOMAIN is required}"
: "${GRIST_DOMAIN:?GRIST_DOMAIN is required}"

LE_LIVE=/etc/letsencrypt/live
SELF_DIR=/etc/nginx/selfsigned

resolve_cert() {
  # $1 = domain; sets CERT and KEY (echoed as "CERT KEY")
  domain="$1"
  if [ -f "$LE_LIVE/$domain/fullchain.pem" ] && [ -f "$LE_LIVE/$domain/privkey.pem" ]; then
    echo "$LE_LIVE/$domain/fullchain.pem $LE_LIVE/$domain/privkey.pem"
    return
  fi
  # self-signed fallback, generated once per domain
  d="$SELF_DIR/$domain"
  if [ ! -f "$d/self.crt" ] || [ ! -f "$d/self.key" ]; then
    mkdir -p "$d"
    openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
      -keyout "$d/self.key" -out "$d/self.crt" \
      -subj "/CN=$domain" >/dev/null 2>&1
    echo "[router] generated self-signed fallback for $domain" >&2
  fi
  echo "$d/self.crt $d/self.key"
}

set -- $(resolve_cert "$AMR_DOMAIN")
export AMR_CERT="$1" AMR_KEY="$2"
set -- $(resolve_cert "$ANALYZERS_DOMAIN")
export ANALYZERS_CERT="$1" ANALYZERS_KEY="$2"
set -- $(resolve_cert "$GRIST_DOMAIN")
export GRIST_CERT="$1" GRIST_KEY="$2"

echo "[router] AMR_DOMAIN=$AMR_DOMAIN cert=$AMR_CERT"
echo "[router] ANALYZERS_DOMAIN=$ANALYZERS_DOMAIN cert=$ANALYZERS_CERT"
echo "[router] GRIST_DOMAIN=$GRIST_DOMAIN cert=$GRIST_CERT"

# Render the template. Restrict the substituted vars so nginx runtime $variables
# (e.g. $host, $scheme, the $amr_oe upstream vars) survive envsubst untouched.
envsubst '${AMR_DOMAIN} ${ANALYZERS_DOMAIN} ${GRIST_DOMAIN} ${AMR_CERT} ${AMR_KEY} ${ANALYZERS_CERT} ${ANALYZERS_KEY} ${GRIST_CERT} ${GRIST_KEY}' \
  < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

mkdir -p /var/cache/nginx/uat

nginx -t
exec nginx -g "daemon off;"
