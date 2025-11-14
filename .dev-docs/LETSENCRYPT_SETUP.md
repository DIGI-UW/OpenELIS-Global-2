# Let's Encrypt Certificate Setup

This guide explains how to set up Let's Encrypt certificates for the
storage.openelis-global.org domain.

## Quick Start

1. **Set required environment variables:**

   ```bash
   export LETSENCRYPT_EMAIL="your-email@example.com"
   export LETSENCRYPT_DOMAIN="storage.openelis-global.org"  # Optional, this is the default
   ```

2. **Start services with Let's Encrypt support:**

   ```bash
   docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml up -d
   ```

3. **The certbot-init service will automatically:**
   - Check if certificates already exist
   - Generate new certificates if they don't exist
   - Renew existing certificates if they're close to expiration

## Environment Variables

- **LETSENCRYPT_EMAIL** (required): Email address for Let's Encrypt
  notifications and account recovery
- **LETSENCRYPT_DOMAIN** (optional): Domain name for the certificate. Defaults
  to `storage.openelis-global.org`
- **LETSENCRYPT_STAGING** (optional): Set to `"true"` to use Let's Encrypt
  staging environment for testing. Defaults to `false`

## Prerequisites

1. **DNS Configuration:**

   - The domain must point to your server's public IP address
   - Port 80 must be accessible from the internet (required for ACME challenge
     validation)

2. **Port 80 Access:**
   - Let's Encrypt uses HTTP-01 challenge validation
   - The proxy service must be running and accessible on port 80
   - The nginx configuration already includes the ACME challenge handler at
     `/.well-known/acme-challenge/`

## Services

The `docker-compose.letsencrypt.yml` override file adds three components:

1. **certbot-init**: One-time certificate generation service

   - Runs on startup to check for existing certificates
   - Generates new certificates if they don't exist
   - Should exit after completion (one-time use)

2. **certbot-renew**: Automatic renewal service

   - Runs continuously and checks for certificate renewal needs
   - Automatically renews certificates when they're within 30 days of expiration
   - Restarts the proxy service after successful renewal

3. **proxy**: Enhanced proxy service
   - Mounts the Let's Encrypt certificate volume
   - The entrypoint script automatically detects and uses Let's Encrypt
     certificates if available

## Testing with Staging Environment

Before using production certificates, you can test with Let's Encrypt's staging
environment:

```bash
export LETSENCRYPT_EMAIL="your-email@example.com"
export LETSENCRYPT_STAGING="false"
docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml up -d
```

**Note:** Staging certificates will cause browser security warnings, but they're
useful for testing the setup process.

## Manual Certificate Operations

### Check Certificate Status

```bash
docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml run --rm certbot-init certbot certificates
```

### Force Certificate Renewal

```bash
docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml run --rm certbot-renew certbot renew --force-renewal --webroot --webroot-path=/var/www/certbot
```

### View Certificates

```bash
ls -la ./volume/letsencrypt/live/storage.openelis-global.org/
```

## Troubleshooting

### Certificate Generation Fails

1. **Check DNS:**

   ```bash
   dig storage.openelis-global.org
   ```

   Ensure the domain points to your server's IP.

2. **Check Port 80 Access:**

   ```bash
   curl -I http://storage.openelis-global.org/.well-known/acme-challenge/test
   ```

   Should return 404 (not connection refused), which means nginx is accessible.

3. **Check certbot-init logs:**
   ```bash
   docker logs openelisglobal-certbot-init
   ```

### Certificate Renewal Fails

1. **Check certbot-renew logs:**

   ```bash
   docker logs openelisglobal-certbot-renew
   ```

2. **Verify proxy is running:**

   ```bash
   docker ps | grep proxy
   ```

3. **Check certificate expiration:**
   ```bash
   docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml run --rm certbot-init certbot certificates
   ```

### Proxy Not Using Let's Encrypt Certificates

1. **Check proxy logs:**

   ```bash
   docker logs openelisglobal-proxy
   ```

   Look for messages about Let's Encrypt certificates.

2. **Verify certificates exist:**

   ```bash
   ls -la ./volume/letsencrypt/live/storage.openelis-global.org/
   ```

3. **Restart proxy:**
   ```bash
   docker restart openelisglobal-proxy
   ```

## Certificate Storage

Certificates are stored in `./volume/letsencrypt/` directory:

- `live/storage.openelis-global.org/` - Current certificates (symlinks)
- `archive/storage.openelis-global.org/` - Certificate history
- `renewal/storage.openelis-global.org.conf` - Renewal configuration

**Important:** The `volume/letsencrypt/` directory should be backed up regularly
as it contains your SSL certificates.

## Running Without Let's Encrypt

To run without Let's Encrypt support (using self-signed certificates):

```bash
docker compose -f dev.docker-compose.yml up -d
```

The proxy will automatically fall back to self-signed certificates if Let's
Encrypt certificates are not found.
