# Certificate Setup Analysis and Let's Encrypt Configuration Report

## Executive Summary

This report analyzes the current certificate architecture in OpenELIS Global 2
and provides a comprehensive guide for setting up Let's Encrypt certificates for
the `storage.openelis-global.org` subdomain on port 443 in the development
environment.

## Current Certificate Architecture

### 1. Certificate Generation

**Current Setup:**

- The project uses a Docker container (`itechuw/certgen:main`) to generate
  **self-signed certificates** for development
- Certificates are generated in the `certs` service (lines 2-15 in
  `dev.docker-compose.yml`)
- Generated certificates include:
  - **Self-signed certificate**: `/etc/ssl/certs/apache-selfsigned.crt`
  - **Private key**: `/etc/ssl/private/apache-selfsigned.key`
  - **Java keystore**: `/etc/openelis-global/keystore` (PKCS12 format)
  - **Java truststore**: `/etc/openelis-global/truststore` (PKCS12 format)

**Certificate Details:**

- Subject: `CN=localhost`
- Subject Alternative Name: `DNS:*.openelis.org`
- Validity: 365 days
- Format: Self-signed X.509 certificate

### 2. Certificate Distribution

Certificates are distributed via Docker volumes:

```yaml
volumes:
  - key_trust-store-volume:/etc/openelis-global # Java keystores/truststores
  - certs-vol:/etc/nginx/certs/ # Nginx certificates
  - keys-vol:/etc/nginx/keys/ # Nginx private keys
```

**Services Using Certificates:**

1. **Nginx Proxy** (`proxy` service):

   - Uses certificates from `certs-vol` and `keys-vol` volumes
   - Listens on ports 80 (HTTP) and 443 (HTTPS)
   - Current nginx.conf redirects all HTTP traffic to HTTPS
   - Uses generic `server_name __` (matches any hostname)

2. **OpenELIS Webapp** (`oe.openelis.org` service):

   - Mounts `key_trust-store-volume` for Java SSL communication
   - Uses keystore for outbound HTTPS connections
   - Uses truststore to validate peer certificates

3. **FHIR API** (`fhir.openelis.org` service):
   - Uses Java keystores/truststores via `JAVA_OPTS` environment variables
   - Configured for mutual TLS (mTLS) communication

### 3. Current Limitations

1. **Self-signed certificates** cause browser security warnings
2. **No subdomain-specific configuration** - all domains use the same
   certificate
3. **No automatic renewal** - certificates must be manually regenerated
4. **Generic server_name** - nginx accepts requests for any hostname
5. **No Let's Encrypt integration** - production-ready certificates not
   configured

## Proposed Solution: Let's Encrypt for storage.openelis-global.org

### Architecture Overview

To add Let's Encrypt support for `storage.openelis-global.org`, we need to:

1. Add a Certbot container for automatic certificate management
2. Configure nginx to handle ACME challenges for certificate validation
3. Add subdomain-specific server block in nginx configuration
4. Set up automatic certificate renewal
5. Update Docker Compose configuration

### Step-by-Step Implementation

#### Step 1: Update `dev.docker-compose.yml`

Add a new Certbot service and update the proxy service:

```yaml
services:
  certbot:
    image: certbot/certbot:latest
    container_name: openelisglobal-certbot
    volumes:
      - certs-vol:/etc/letsencrypt/certs
      - certbot-challenge-vol:/var/www/certbot
      - certbot-config-vol:/etc/letsencrypt
    command:
      certonly --webroot --webroot-path=/var/www/certbot --keep-until-expiring
      --email your-email@example.com --agree-tos --no-eff-email -d
      storage.openelis-global.org
    networks:
      - default
    depends_on:
      - proxy

  proxy:
    image: itechuw/openelis-global-2-proxy:develop
    container_name: openelisglobal-proxy
    platform: linux/amd64
    ports:
      - 80:80
      - 443:443
    volumes:
      - certs-vol:/etc/nginx/certs/
      - keys-vol:/etc/nginx/keys/
      - ./volume/nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - certbot-challenge-vol:/var/www/certbot
      - ./volume/letsencrypt:/etc/letsencrypt:ro
    networks:
      - default
    restart: unless-stopped
    depends_on:
      - certs
```

Add new volumes:

```yaml
volumes:
  # ... existing volumes ...
  certbot-challenge-vol:
  certbot-config-vol:
```

#### Step 2: Create Updated nginx.conf

Create a new nginx configuration that supports both self-signed (for other
services) and Let's Encrypt certificates (for storage subdomain):

```nginx
worker_processes 1;

events { worker_connections 1024; }

http {
    # HTTP server for Let's Encrypt ACME challenge
    server {
        listen 80;
        server_name storage.openelis-global.org;

        location /.well-known/acme-challenge/ {
            root /var/www/certbot;
        }

        location / {
            return 301 https://$host$request_uri;
        }
    }

    # Default HTTP server (redirect to HTTPS)
    server {
        listen 80;
        server_name _;
        return 301 https://$host$request_uri;
    }

    # HTTPS server for storage.openelis-global.org with Let's Encrypt
    server {
        listen 443 ssl;
        server_name storage.openelis-global.org;

        ssl_certificate /etc/letsencrypt/live/storage.openelis-global.org/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/storage.openelis-global.org/privkey.pem;

        # SSL configuration best practices
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384';
        ssl_prefer_server_ciphers off;
        ssl_session_cache shared:SSL:10m;
        ssl_session_timeout 10m;

        proxy_set_header X-Forwarded-For $proxy_protocol_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $host;
        absolute_redirect off;

        # Add your storage-specific routing here
        location / {
            proxy_pass https://frontend.openelis.org:3000;
            proxy_redirect off;
        }

        location /api/ {
            proxy_pass https://oe.openelis.org:8443/api/;
            proxy_redirect off;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Host $server_name;
        }
    }

    # Default HTTPS server (for other domains, uses self-signed)
    server {
        listen [::]:443 ssl;
        listen 443 ssl default;
        server_name __;

        ssl_certificate /etc/nginx/certs/apache-selfsigned.crt;
        ssl_certificate_key /etc/nginx/keys/apache-selfsigned.key;

        proxy_set_header X-Forwarded-For $proxy_protocol_addr;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Host $host;
        absolute_redirect off;

        location / {
            proxy_pass https://frontend.openelis.org:3000;
            proxy_redirect off;
        }

        location /api/ {
            proxy_pass https://oe.openelis.org:8443/api/;
            proxy_redirect off;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Host $server_name;
        }
    }
}
```

#### Step 3: Initial Certificate Request

Before starting the services, ensure DNS is configured:

1. **DNS Configuration:**

   - Point `storage.openelis-global.org` A record to your server's public IP
   - Ensure port 80 is accessible from the internet (required for ACME
     challenge)

2. **First-time Certificate Request:**

   ```bash
   docker compose -f dev.docker-compose.yml run --rm certbot certonly \
     --webroot \
     --webroot-path=/var/www/certbot \
     --email your-email@example.com \
     --agree-tos \
     --no-eff-email \
     -d storage.openelis-global.org
   ```

3. **Start Services:**
   ```bash
   docker compose -f dev.docker-compose.yml up -d
   ```

#### Step 4: Automatic Renewal Setup

Create a renewal script and add it to a cron job or use a scheduled container:

**Option A: Scheduled Container (Recommended)**

Add to `dev.docker-compose.yml`:

```yaml
certbot-renew:
  image: certbot/certbot:latest
  container_name: openelisglobal-certbot-renew
  volumes:
    - certbot-config-vol:/etc/letsencrypt
    - certbot-challenge-vol:/var/www/certbot
  command: renew --webroot --webroot-path=/var/www/certbot
  networks:
    - default
  restart: unless-stopped
  depends_on:
    - proxy
```

**Option B: Cron Job on Host**

Add to host crontab (use absolute path or `cd` pattern for reliability):

```bash
# Using cd pattern (recommended for cron jobs)
0 3 * * * cd /path/to/OpenELIS-Global-2 && docker compose -f dev.docker-compose.yml run --rm certbot renew --webroot --webroot-path=/var/www/certbot && docker compose -f dev.docker-compose.yml restart proxy

# OR using absolute paths
0 3 * * * docker compose -f /path/to/OpenELIS-Global-2/dev.docker-compose.yml run --rm certbot renew --webroot --webroot-path=/var/www/certbot && docker compose -f /path/to/OpenELIS-Global-2/dev.docker-compose.yml restart proxy
```

### Alternative: Using Nginx Proxy with Automatic Let's Encrypt

For a more production-ready solution, consider using `nginx-proxy` with
`letsencrypt-nginx-proxy-companion`:

```yaml
services:
  nginx-proxy:
    image: nginxproxy/nginx-proxy:latest
    container_name: nginx-proxy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - certs-vol:/etc/nginx/certs:ro
      - vhost-vol:/etc/nginx/vhost.d
      - html-vol:/usr/share/nginx/html
      - /var/run/docker.sock:/tmp/docker.sock:ro
    networks:
      - default

  letsencrypt:
    image: nginxproxy/acme-companion:latest
    container_name: nginx-proxy-letsencrypt
    volumes:
      - certs-vol:/etc/nginx/certs:rw
      - vhost-vol:/etc/nginx/vhost.d
      - html-vol:/usr/share/nginx/html
      - /var/run/docker.sock:/var/run/docker.sock:ro
    environment:
      - DEFAULT_EMAIL=your-email@example.com
    networks:
      - default
    depends_on:
      - nginx-proxy

  storage:
    # Your storage service
    environment:
      - VIRTUAL_HOST=storage.openelis-global.org
      - LETSENCRYPT_HOST=storage.openelis-global.org
      - LETSENCRYPT_EMAIL=your-email@example.com
    networks:
      - default
```

## Security Considerations

### 1. Certificate Storage

- **Never commit certificates to git** - use `.gitignore` for certificate
  directories
- **Use Docker secrets** for sensitive certificate passwords
- **Restrict file permissions** on certificate files (600 for keys, 644 for
  certs)

### 2. Private Key Protection

- Private keys should be stored in Docker volumes with restricted access
- Consider using Docker secrets for keystore passwords
- Rotate keys periodically

### 3. Certificate Renewal

- Set up monitoring for certificate expiration
- Test renewal process before certificates expire
- Have a fallback mechanism if renewal fails

### 4. Production Recommendations

- Use a staging Let's Encrypt environment for testing
- Implement certificate expiration monitoring
- Set up automated alerts for renewal failures
- Consider using a managed certificate service (AWS ACM, Cloudflare, etc.)

## Testing the Configuration

### 1. Verify DNS Resolution

```bash
dig storage.openelis-global.org
nslookup storage.openelis-global.org
```

### 2. Test HTTP to HTTPS Redirect

```bash
curl -I http://storage.openelis-global.org
# Should return 301 redirect to HTTPS
```

### 3. Verify Certificate

```bash
openssl s_client -connect storage.openelis-global.org:443 -servername storage.openelis-global.org
```

### 4. Check Certificate Details

```bash
echo | openssl s_client -servername storage.openelis-global.org -connect storage.openelis-global.org:443 2>/dev/null | openssl x509 -noout -dates -subject
```

## Troubleshooting

### Common Issues

1. **ACME Challenge Fails**

   - Ensure port 80 is accessible from the internet
   - Verify DNS points to correct IP
   - Check nginx is serving `.well-known/acme-challenge/` path

2. **Certificate Not Found**

   - Verify certificate location:
     `/etc/letsencrypt/live/storage.openelis-global.org/`
   - Check nginx can read certificate files
   - Ensure proper file permissions

3. **Nginx Won't Start**

   - Check nginx configuration syntax: `nginx -t`
   - Verify certificate paths in nginx.conf
   - Review Docker logs: `docker logs openelisglobal-proxy`

4. **Certificate Renewal Fails**
   - Check Certbot logs: `docker logs openelisglobal-certbot-renew`
   - Verify nginx is running during renewal
   - Ensure challenge path is accessible

## Migration Path

### Phase 1: Development (Current)

- Continue using self-signed certificates for local development
- Add Let's Encrypt for `storage.openelis-global.org` subdomain only

### Phase 2: Staging

- Test Let's Encrypt renewal process
- Verify all services work with new certificates
- Document any issues

### Phase 3: Production

- Migrate all subdomains to Let's Encrypt
- Remove self-signed certificate generation
- Implement certificate monitoring

## Conclusion

The current certificate setup uses self-signed certificates suitable for
development but not production. To add Let's Encrypt support for
`storage.openelis-global.org`:

1. Add Certbot container to `dev.docker-compose.yml`
2. Update nginx configuration with subdomain-specific server block
3. Configure ACME challenge handling
4. Set up automatic renewal

The implementation can be done incrementally without affecting existing
services, as the default server block will continue to use self-signed
certificates for other domains.

## References

- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Certbot Documentation](https://certbot.eff.org/)
- [Nginx SSL Configuration](https://nginx.org/en/docs/http/configuring_https_servers.html)
- [Docker Compose Volumes](https://docs.docker.com/compose/compose-file/compose-file-v3/#volumes)
