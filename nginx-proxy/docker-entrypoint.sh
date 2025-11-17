#!/bin/sh
set -e

# Get domain from environment variable or use default
DOMAIN="${LETSENCRYPT_DOMAIN:-storage.openelis-global.org}"

# Check if Let's Encrypt certificates exist for the domain
LETSENCRYPT_CERT="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"
LETSENCRYPT_KEY="/etc/letsencrypt/live/${DOMAIN}/privkey.pem"

# Paths where nginx expects certificates (from volumes)
NGINX_CERT="/etc/nginx/certs/apache-selfsigned.crt"
NGINX_KEY="/etc/nginx/keys/apache-selfsigned.key"

if [ -f "$LETSENCRYPT_CERT" ] && [ -f "$LETSENCRYPT_KEY" ]; then
    echo "✓ Let's Encrypt certificates found for ${DOMAIN}"
    echo "Creating symlinks to Let's Encrypt certificates..."
    
    # Remove existing files/symlinks if they exist
    rm -f "$NGINX_CERT" "$NGINX_KEY"
    
    # Create symlinks to Let's Encrypt certificates
    ln -sf "$LETSENCRYPT_CERT" "$NGINX_CERT"
    ln -sf "$LETSENCRYPT_KEY" "$NGINX_KEY"
    
    echo "✓ Symlinks created:"
    echo "  $NGINX_CERT -> $LETSENCRYPT_CERT"
    echo "  $NGINX_KEY -> $LETSENCRYPT_KEY"
else
    echo "⚠ Let's Encrypt certificates not found for ${DOMAIN}"
    echo "Using self-signed certificates from certs service"
fi

# Test the nginx configuration
nginx -t

# Start nginx
exec nginx -g "daemon off;"
