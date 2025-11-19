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

# Ensure directories exist
mkdir -p "$(dirname "$NGINX_CERT")" "$(dirname "$NGINX_KEY")"

# Function to check if a file exists and is not a broken symlink
file_exists() {
    [ -f "$1" ] && [ ! -L "$1" ] || ([ -L "$1" ] && [ -e "$1" ])
}

if file_exists "$LETSENCRYPT_CERT" && file_exists "$LETSENCRYPT_KEY"; then
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
elif file_exists "$NGINX_CERT" && file_exists "$NGINX_KEY"; then
    echo "✓ Using existing self-signed certificates from certs service"
else
    echo "⚠ Certificates not found. Waiting for certs service to generate them..."
    # Wait up to 30 seconds for certs to be generated
    for i in $(seq 1 30); do
        if file_exists "$NGINX_CERT" && file_exists "$NGINX_KEY"; then
            echo "✓ Certificates found after ${i} seconds"
            break
        fi
        sleep 1
    done

    if ! file_exists "$NGINX_CERT" || ! file_exists "$NGINX_KEY"; then
        echo "⚠ Certificates still not found. Generating temporary self-signed certificate..."
        # Remove broken symlinks if they exist
        rm -f "$NGINX_CERT" "$NGINX_KEY"
        # Generate a temporary self-signed certificate
        openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
            -keyout "$NGINX_KEY" \
            -out "$NGINX_CERT" \
            -subj "/CN=localhost" \
            2>/dev/null || {
            echo "ERROR: Failed to generate temporary certificate and certificates not found"
            echo "Please ensure certs service is running or Let's Encrypt certificates are available"
            exit 1
        }
        echo "✓ Temporary self-signed certificate generated"
    fi
fi

# Verify certificates exist before testing nginx config
if ! file_exists "$NGINX_CERT" || ! file_exists "$NGINX_KEY"; then
    echo "ERROR: Certificates not available at expected paths:"
    echo "  Certificate: $NGINX_CERT"
    echo "  Key: $NGINX_KEY"
    exit 1
fi

# Test the nginx configuration
nginx -t

# Start nginx
exec nginx -g "daemon off;"
