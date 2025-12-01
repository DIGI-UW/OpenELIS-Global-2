# ASTM Mock Server Access Guide

## Quick Access

The ASTM mock server is accessible via multiple methods:

### From Host Machine

```bash
# Direct connection
nc localhost 5000

# Or via Python
python3 -c "import socket; s=socket.socket(); s.connect(('localhost', 5000)); s.send(b'\x05'); print(s.recv(1).hex())"
```

### From OpenELIS Backend Container

- **Container name**: `openelis-astm-simulator:5000`
- **IP Address**: Check with
  `docker inspect openelis-astm-simulator --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'`

### Via Nginx TCP Proxy

- **Port**: `5000` (proxied through nginx)
- **Access**: `localhost:5000` or `analyzers.openelis-global.org:5000` (if DNS
  configured)

## Test Analyzer Configuration

A test analyzer (ID: 1000) is pre-configured to connect to the ASTM server:

```sql
SELECT a.id, a.name, ac.ip_address, ac.port, ac.status
FROM analyzer a
JOIN analyzer_configuration ac ON a.id = ac.analyzer_id
WHERE a.id = 1000;
```

**Current Configuration:**

- **Name**: Hematology Analyzer 1
- **IP**: 172.20.1.6 (container IP - auto-detected)
- **Port**: 5000
- **Status**: SETUP

## Updating Analyzer Configuration

If the ASTM server container IP changes, update the analyzer:

```bash
# Get current IP
ASTM_IP=$(docker inspect openelis-astm-simulator --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')

# Update analyzer configuration
docker exec openelisglobal-database psql -U clinlims -d clinlims -c \
  "UPDATE analyzer_configuration SET ip_address = '$ASTM_IP', port = 5000 WHERE analyzer_id = 1000;"
```

## Nginx Stream Configuration

The nginx proxy includes a TCP stream proxy for the ASTM server:

```nginx
stream {
    server {
        listen 5000;
        proxy_pass openelis-astm-simulator:5000;
        proxy_timeout 30s;
    }
}
```

This allows the ASTM server to be accessed through the nginx proxy on port 5000.

## Testing Connection

### Test ENQ/ACK Handshake

```python
import socket
s = socket.socket()
s.connect(('localhost', 5000))
s.send(b'\x05')  # ENQ
response = s.recv(1)
assert response == b'\x06'  # ACK
print("✓ Connection successful!")
s.close()
```

### Test from OpenELIS UI

1. Navigate to `/analyzers`
2. Select "Hematology Analyzer 1" (ID: 1000)
3. Click "Test Connection"
4. Should see "Connection successful" within 30 seconds

## Troubleshooting

### Server Not Responding

```bash
# Check container status
docker ps | grep astm-simulator

# Check logs
docker logs openelis-astm-simulator

# Test direct connection
docker exec openelis-astm-simulator python -c "import socket; s=socket.socket(); s.bind(('0.0.0.0', 5000)); print('Port 5000 is listening')"
```

### Nginx Proxy Not Working

```bash
# Check nginx config
docker exec openelisglobal-proxy nginx -t

# Check stream module
docker exec openelisglobal-proxy nginx -V 2>&1 | grep stream

# Restart proxy
docker restart openelisglobal-proxy
```

### Analyzer Connection Fails

- Verify analyzer IP matches container IP:
  `docker inspect openelis-astm-simulator --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'`
- Check analyzer configuration in database
- Verify ASTM server is running: `docker ps | grep astm`
