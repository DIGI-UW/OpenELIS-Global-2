#!/bin/bash
set -e

# Copy the nginx.conf to a writable location
cp /etc/nginx/nginx.conf /tmp/nginx.conf

# Check if Let's Encrypt certificates exist for storage.openelis-global.org
LETSENCRYPT_CERT="/etc/letsencrypt/live/storage.openelis-global.org/fullchain.pem"
LETSENCRYPT_KEY="/etc/letsencrypt/live/storage.openelis-global.org/privkey.pem"

if [ -f "$LETSENCRYPT_CERT" ] && [ -f "$LETSENCRYPT_KEY" ]; then
    echo "✓ Let's Encrypt certificates found, using them for storage.openelis-global.org"
    # Replace self-signed cert paths with Let's Encrypt paths only in the storage.openelis-global.org server block
    # Use sed with a range pattern from "server_name storage.openelis-global.org;" to the next "server {" block
    sed -i '/server_name storage\.openelis-global\.org;/,/^[[:space:]]*server[[:space:]]*{/ {
        s|ssl_certificate /etc/nginx/certs/apache-selfsigned.crt;|ssl_certificate /etc/letsencrypt/live/storage.openelis-global.org/fullchain.pem;|g
        s|ssl_certificate_key /etc/nginx/keys/apache-selfsigned.key;|ssl_certificate_key /etc/letsencrypt/live/storage.openelis-global.org/privkey.pem;|g
    }' /tmp/nginx.conf
else
    echo "⚠ Let's Encrypt certificates not found, using self-signed certificates for storage.openelis-global.org"
    # Keep the existing self-signed cert paths (they're already in the config)
fi

# Test the nginx configuration
nginx -t -c /tmp/nginx.conf

# Start nginx with the modified config
exec nginx -g "daemon off;" -c /tmp/nginx.conf
