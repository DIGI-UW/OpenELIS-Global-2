#!/bin/sh
set -e

DOMAIN=${LETSENCRYPT_DOMAIN:-storage.openelis-global.org}
EMAIL=${LETSENCRYPT_EMAIL}
STAGING=${LETSENCRYPT_STAGING:-false}

if [ -z "$EMAIL" ]; then
    echo 'ERROR: LETSENCRYPT_EMAIL environment variable is required'
    exit 1
fi

CERT_PATH="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"

if [ -f "$CERT_PATH" ]; then
    echo "Certificate for ${DOMAIN} already exists. Checking renewal..."
    certbot renew --webroot --webroot-path=/var/www/certbot --quiet
else
    echo "Certificate for ${DOMAIN} not found. Generating new certificate..."
    STAGING_FLAG=""
    if [ "$STAGING" = "true" ]; then
        STAGING_FLAG="--staging"
        echo "Using Let's Encrypt staging environment"
    fi

    certbot certonly \
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
    else
        echo "✗ Failed to generate certificate for ${DOMAIN}"
        exit 1
    fi
fi
