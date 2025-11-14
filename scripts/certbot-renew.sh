#!/bin/sh
set -e

# Install docker CLI if not present (needed to restart proxy)
if ! command -v docker > /dev/null 2>&1; then
    echo "Installing docker CLI..."
    apk add --no-cache docker-cli || apt-get update && apt-get install -y docker.io || true
fi

DOMAIN=${LETSENCRYPT_DOMAIN:-storage.openelis-global.org}
CERT_PATH="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"

# Check for certificates on startup
if [ ! -f "$CERT_PATH" ]; then
    echo "Certificate for ${DOMAIN} not found. Waiting for certbot-init to generate it..."
    # Wait for certbot-init to complete (up to 5 minutes)
    for i in $(seq 1 60); do
        sleep 5
        if [ -f "$CERT_PATH" ]; then
            echo "Certificate found, starting renewal service..."
            break
        fi
    done
    if [ ! -f "$CERT_PATH" ]; then
        echo "Certificate not found after waiting. Renewal service will check periodically."
    fi
fi

# Run renewal check twice daily (every 12 hours)
while true; do
    if [ -f "$CERT_PATH" ]; then
        echo "[$(date)] Checking certificate renewal for ${DOMAIN}..."
        # certbot renew only renews certificates within 30 days of expiration
        # Capture output to check if renewal occurred
        RENEW_OUTPUT=$(certbot renew --webroot --webroot-path=/var/www/certbot 2>&1)
        RENEW_EXIT=$?

        if [ $RENEW_EXIT -eq 0 ]; then
            # Check if certbot actually renewed (it prints specific messages)
            if echo "$RENEW_OUTPUT" | grep -q "Congratulations\|No renewals were attempted"; then
                if echo "$RENEW_OUTPUT" | grep -q "Congratulations"; then
                    echo "[$(date)] ✓ Certificates renewed successfully for ${DOMAIN}"
                    echo "[$(date)] Restarting proxy to load new certificates..."
                    docker restart openelisglobal-proxy || true
                else
                    echo "[$(date)] Certificates for ${DOMAIN} are up to date (not yet due for renewal)"
                fi
            else
                echo "[$(date)] Certificate renewal check completed for ${DOMAIN}"
            fi
        else
            echo "[$(date)] ✗ Certificate renewal check failed for ${DOMAIN}"
            echo "$RENEW_OUTPUT"
        fi
    else
        echo "[$(date)] Certificate not found, waiting for certbot-init..."
    fi

    # Sleep for 12 hours before next check
    sleep 43200
done
