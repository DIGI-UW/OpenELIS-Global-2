#!/bin/bash
set -e

# Copy the nginx.conf to a writable location
cp /etc/nginx/nginx.conf /tmp/nginx.conf

# Get domain from environment variable or use default
DOMAIN="${LETSENCRYPT_DOMAIN:-storage.openelis-global.org}"

# Check if Let's Encrypt certificates exist for the domain
LETSENCRYPT_CERT="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"
LETSENCRYPT_KEY="/etc/letsencrypt/live/${DOMAIN}/privkey.pem"

if [ -f "$LETSENCRYPT_CERT" ] && [ -f "$LETSENCRYPT_KEY" ]; then
    echo "✓ Let's Encrypt certificates found, using them for ${DOMAIN}"
    # Replace self-signed cert paths with Let's Encrypt paths in the HTTPS server block
    # The first "listen 443 ssl;" (without "default") is the storage.openelis-global.org block
    sed -i '/listen 443 ssl;$/,/^[[:space:]]*server[[:space:]]*{/ {
        /^[[:space:]]*server[[:space:]]*{/b
        s|ssl_certificate /etc/nginx/certs/apache-selfsigned.crt;|ssl_certificate /etc/letsencrypt/live/'${DOMAIN}'/fullchain.pem;|g
        s|ssl_certificate_key /etc/nginx/keys/apache-selfsigned.key;|ssl_certificate_key /etc/letsencrypt/live/'${DOMAIN}'/privkey.pem;|g
    }' /tmp/nginx.conf
else
    echo "⚠ Let's Encrypt certificates not found, using self-signed certificates for ${DOMAIN}"
fi

# Test the nginx configuration
nginx -t -c /tmp/nginx.conf

# Start nginx with the modified config
exec nginx -g "daemon off;" -c /tmp/nginx.conf
