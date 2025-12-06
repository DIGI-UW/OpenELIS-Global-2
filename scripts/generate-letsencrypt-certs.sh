#!/bin/sh
set -e

# Standalone script to generate Let's Encrypt certificates
# Usage: ./scripts/generate-letsencrypt-certs.sh
#
# Required environment variables:
#   LETSENCRYPT_EMAIL - Email address for Let's Encrypt notifications
#   LETSENCRYPT_DOMAIN - Domain name (default: storage.openelis-global.org)
#
# Optional environment variables:
#   LETSENCRYPT_STAGING - Set to "true" to use Let's Encrypt staging environment

DOMAIN=${LETSENCRYPT_DOMAIN:-storage.openelis-global.org}
EMAIL=${LETSENCRYPT_EMAIL}
STAGING=${LETSENCRYPT_STAGING:-false}

if [ -z "$EMAIL" ]; then
    echo "ERROR: LETSENCRYPT_EMAIL environment variable is required"
    echo "Usage: export LETSENCRYPT_EMAIL='your-email@example.com' && ./scripts/generate-letsencrypt-certs.sh"
    exit 1
fi

# Ensure volume directory exists
mkdir -p ./volume/letsencrypt
mkdir -p ./volume/nginx/certbot

echo "Generating Let's Encrypt certificate for ${DOMAIN}..."
echo "Email: ${EMAIL}"
if [ "$STAGING" = "true" ]; then
    echo "Using Let's Encrypt STAGING environment (for testing)"
fi

# Check if proxy is running (required for ACME challenge)
if ! docker ps | grep -q openelisglobal-proxy; then
    echo "ERROR: Proxy container (openelisglobal-proxy) must be running for ACME challenge"
    echo "Start it with: docker compose -f dev.docker-compose.yml up -d proxy"
    exit 1
fi

CERT_PATH="./volume/letsencrypt/live/${DOMAIN}/fullchain.pem"

if [ -f "$CERT_PATH" ]; then
    echo "Certificate for ${DOMAIN} already exists."
    printf "Do you want to renew it? (y/N) "
    read REPLY
    case "$REPLY" in
        [Yy]|[Yy][Ee][Ss])
            echo "Renewing certificate for ${DOMAIN}..."
            ;;
        *)
            echo "Skipping renewal. Certificate already exists at ${CERT_PATH}"
            exit 0
            ;;
    esac
    docker run --rm \
        -v "$(pwd)/volume/letsencrypt:/etc/letsencrypt" \
        -v "$(pwd)/volume/nginx/certbot:/var/www/certbot" \
        certbot/certbot:latest \
        renew --webroot --webroot-path=/var/www/certbot

    if [ $? -eq 0 ]; then
        echo "✓ Certificate renewed successfully for ${DOMAIN}"
        echo ""
        echo "Next steps:"
        echo "1. Restart the proxy to use renewed certificates:"
        echo "   docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml restart proxy"
    else
        echo "✗ Failed to renew certificate for ${DOMAIN}"
        exit 1
    fi
else
    echo "Generating new certificate for ${DOMAIN}..."
    STAGING_FLAG=""
    if [ "$STAGING" = "true" ]; then
        STAGING_FLAG="--staging"
    fi

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

    if [ $? -eq 0 ]; then
        echo "✓ Certificate successfully generated for ${DOMAIN}"
        echo "Certificate location: ${CERT_PATH}"
        echo ""
        echo "Next steps:"
        echo "1. Restart the proxy to use Let's Encrypt certificates:"
        echo "   docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml restart proxy"
        echo "2. Or restart all services:"
        echo "   docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml up -d"
    else
        echo "✗ Failed to generate certificate for ${DOMAIN}"
        exit 1
    fi
fi
