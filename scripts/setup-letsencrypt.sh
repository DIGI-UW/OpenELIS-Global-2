#!/bin/bash
set -e

# Quick setup script for Let's Encrypt certificates
# Usage: ./scripts/setup-letsencrypt.sh
#
# Environment variables (all optional, uses defaults from LETSENCRYPT_SETUP.md):
#   LETSENCRYPT_EMAIL - Email address (default: admin@storage.openelis-global.org)
#   LETSENCRYPT_DOMAIN - Domain name (default: storage.openelis-global.org)
#   LETSENCRYPT_STAGING - Use staging environment (default: false)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo_info() {
    echo -e "${GREEN}✓${NC} $1"
}

echo_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

echo_error() {
    echo -e "${RED}✗${NC} $1"
}

# Set defaults for environment variables (from LETSENCRYPT_SETUP.md)
DOMAIN=${LETSENCRYPT_DOMAIN:-storage.openelis-global.org}
STAGING=${LETSENCRYPT_STAGING:-false}
EMAIL=${LETSENCRYPT_EMAIL:-admin@${DOMAIN}}

# Export variables so they're available to child scripts
export LETSENCRYPT_EMAIL=${EMAIL}
export LETSENCRYPT_DOMAIN=${DOMAIN}
export LETSENCRYPT_STAGING=${STAGING}

echo ""
echo "=========================================="
echo "Let's Encrypt Certificate Setup"
echo "=========================================="
echo "Domain: ${DOMAIN}"
echo "Email: ${EMAIL}"
if [ "$STAGING" = "true" ]; then
    echo_warn "Using Let's Encrypt STAGING environment (for testing)"
fi
echo ""

# Change to project root
cd "$PROJECT_ROOT"

# Step 1: Check if proxy is running, start if needed
echo "Step 1: Checking proxy service..."
if ! docker ps | grep -q openelisglobal-proxy; then
    echo_warn "Proxy service is not running. Starting it..."
    docker compose -f dev.docker-compose.yml up -d proxy
    echo_info "Waiting for proxy to be ready..."
    sleep 5
else
    echo_info "Proxy service is already running"
fi

# Step 2: Generate certificates
echo ""
echo "Step 2: Generating Let's Encrypt certificates..."
if ! "$SCRIPT_DIR/generate-letsencrypt-certs.sh"; then
    echo_error "Failed to generate certificates"
    exit 1
fi

# Step 3: Start services with Let's Encrypt support
echo ""
echo "Step 3: Starting services with Let's Encrypt support..."
docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml up -d

echo ""
echo "=========================================="
echo_info "Setup complete!"
echo "=========================================="
echo ""
echo "Services are now running with Let's Encrypt certificates."
echo ""
echo "To verify:"
echo "  1. Check certificate status:"
echo "     docker run --rm -v \"\$(pwd)/volume/letsencrypt:/etc/letsencrypt\" certbot/certbot:latest certificates"
echo ""
echo "  2. Test HTTPS access:"
echo "     curl -I https://${DOMAIN}/"
echo ""
echo "  3. View proxy logs:"
echo "     docker logs openelisglobal-proxy"
echo ""
