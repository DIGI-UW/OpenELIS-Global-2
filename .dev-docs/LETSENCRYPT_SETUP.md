# Let's Encrypt Certificate Setup

This guide explains how to set up Let's Encrypt certificates for the
storage.openelis-global.org domain using a simplified standalone approach.

## Quick Start

1. **Set required environment variables:**

   ```bash
   export LETSENCRYPT_EMAIL="your-email@example.com"
   export LETSENCRYPT_DOMAIN="storage.openelis-global.org"  # Optional, this is the default
   ```

2. **Start the proxy service (required for ACME challenge):**

   ```bash
   docker compose -f dev.docker-compose.yml up -d proxy
   ```

3. **Generate Let's Encrypt certificates:**

   ```bash
   ./scripts/generate-letsencrypt-certs.sh
   ```

4. **Start services with Let's Encrypt support:**

   ```bash
   docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml up -d
   ```

The proxy will automatically detect and use Let's Encrypt certificates if they
exist, falling back to self-signed certificates if not found.

## Environment Variables

- **LETSENCRYPT_EMAIL** (required for cert generation): Email address for Let's
  Encrypt notifications and account recovery
- **LETSENCRYPT_DOMAIN** (optional): Domain name for the certificate. Defaults
  to `storage.openelis-global.org`
- **LETSENCRYPT_STAGING** (optional): Set to `"true"` to use Let's Encrypt
  staging environment for testing. Defaults to `false`

## Prerequisites

1. **DNS Configuration:**

   - The domain must point to your server's public IP address
   - Port 80 must be accessible from the internet (required for ACME challenge
     validation)

2. **Proxy Service Running:**

   - The proxy service must be running before generating certificates
   - It handles the ACME challenge validation at `/.well-known/acme-challenge/`

## How It Works

The simplified setup uses a standalone certificate generation script and
automatic certificate detection:

1. **Certificate Generation**: Run `./scripts/generate-letsencrypt-certs.sh` to
   generate certificates using certbot in a one-off container
2. **Certificate Storage**: Certificates are stored in `./volume/letsencrypt/`
3. **Automatic Detection**: The proxy entrypoint automatically creates symlinks
   from Let's Encrypt certificates to the expected nginx paths if certificates
   exist
4. **Fallback**: If Let's Encrypt certificates don't exist, the proxy uses
   self-signed certificates from the certs service

## Certificate Generation

### First-Time Generation

```bash
export LETSENCRYPT_EMAIL="your-email@example.com"
./scripts/generate-letsencrypt-certs.sh
```

### Renewal

Certificates are valid for 90 days. To renew:

```bash
export LETSENCRYPT_EMAIL="your-email@example.com"
./scripts/generate-letsencrypt-certs.sh
```

The script will detect existing certificates and prompt for renewal.

### Testing with Staging Environment

Before using production certificates, test with Let's Encrypt's staging
environment:

```bash
export LETSENCRYPT_EMAIL="your-email@example.com"
export LETSENCRYPT_STAGING="true"
./scripts/generate-letsencrypt-certs.sh
```

**Note:** Staging certificates will cause browser security warnings, but they're
useful for testing the setup process.

## Manual Certificate Operations

### Check Certificate Status

```bash
docker run --rm \
  -v "$(pwd)/volume/letsencrypt:/etc/letsencrypt" \
  certbot/certbot:latest \
  certificates
```

### View Certificate Files

```bash
ls -la ./volume/letsencrypt/live/storage.openelis-global.org/
```

### Force Certificate Renewal

```bash
docker run --rm \
  -v "$(pwd)/volume/letsencrypt:/etc/letsencrypt" \
  -v "$(pwd)/volume/nginx/certbot:/var/www/certbot" \
  certbot/certbot:latest \
  renew --force-renewal --webroot --webroot-path=/var/www/certbot
```

After renewal, restart the proxy:

```bash
docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml restart proxy
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

3. **Verify Proxy is Running:**

   ```bash
   docker ps | grep openelisglobal-proxy
   ```

   The proxy must be running before generating certificates.

4. **Check Script Output:**

   Review the output of `./scripts/generate-letsencrypt-certs.sh` for specific
   error messages.

### Proxy Not Using Let's Encrypt Certificates

1. **Check proxy logs:**

   ```bash
   docker logs openelisglobal-proxy
   ```

   Look for messages about Let's Encrypt certificates or symlinks.

2. **Verify certificates exist:**

   ```bash
   ls -la ./volume/letsencrypt/live/storage.openelis-global.org/
   ```

   Should show `fullchain.pem` and `privkey.pem`.

3. **Check symlinks in container:**

   ```bash
   docker exec openelisglobal-proxy ls -la /etc/nginx/certs/
   docker exec openelisglobal-proxy ls -la /etc/nginx/keys/
   ```

   Should show symlinks to Let's Encrypt certificates if they exist.

4. **Restart proxy:**

   ```bash
   docker compose -f dev.docker-compose.yml -f docker-compose.letsencrypt.yml restart proxy
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

## Architecture

The simplified setup works as follows:

1. **Certificate Generation**: Standalone script runs certbot in a one-off
   container, generating certificates to `./volume/letsencrypt/`
2. **Proxy Override**: `docker-compose.letsencrypt.yml` mounts the Let's Encrypt
   volume and uses a custom entrypoint
3. **Entrypoint Logic**: The entrypoint script checks for Let's Encrypt
   certificates and creates symlinks to the expected nginx paths if they exist
4. **No nginx.conf Changes**: The nginx configuration remains unchanged; it
   references the same certificate paths, which are symlinked to Let's Encrypt
   certs when available

This approach ensures:

- No changes to the base proxy setup when Let's Encrypt isn't used
- Simple certificate generation workflow
- Automatic certificate detection
- Easy renewal process
