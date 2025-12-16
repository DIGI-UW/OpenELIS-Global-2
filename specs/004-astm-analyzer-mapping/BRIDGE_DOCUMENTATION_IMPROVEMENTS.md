# ASTM HTTP Bridge Documentation Improvements

**Date**: 2025-01-28  
**Feature**: 004-astm-analyzer-mapping  
**Purpose**: Improve bridge configuration and documentation for multi-analyzer,
bi-directional workflow

---

## Current Documentation Gaps

### 1. Bridge README (`tools/astm-http-bridge/README`)

**Current State**: Minimal - only basic docker commands  
**Missing**:

- Architecture overview (multi-analyzer, bi-directional)
- Configuration property structure
- Example configurations
- Communication flow diagrams
- Multi-analyzer setup instructions

### 2. Configuration File (`volume/astm-bridge/configuration.yml`)

**Current State**: Uses custom structure that doesn't match Spring Boot
properties  
**Issue**: File structure doesn't match actual property paths:

- Current: `openelis.url`, `astm.port`
- Actual: `org.itech.ahb.forward-http-server.uri`,
  `org.itech.ahb.listen-astm-server.port`

### 3. OpenELIS Documentation (`docs/astm.md`)

**Current State**: Mentions bridge but doesn't explain multi-analyzer setup  
**Missing**:

- Multi-analyzer configuration
- Bi-directional query flow
- How OpenELIS sends queries to analyzers
- Source IP header explanation (once implemented)

---

## Recommended Improvements

### 1. Enhanced Bridge README

**File**: `tools/astm-http-bridge/README.md` (create new, replace existing
README)

**Content Structure**:

```markdown
# ASTM HTTP Bridge

A simple TCP-HTTP protocol router that translates between ASTM TCP protocol
(used by analyzers) and HTTP POST (used by OpenELIS).

## Architecture

The bridge is a **multi-analyzer, bi-directional** protocol translator:

- **Analyzer → OpenELIS**: Receives ASTM messages via TCP, forwards to OpenELIS
  via HTTP POST
- **OpenELIS → Analyzer**: Receives ASTM messages via HTTP POST, forwards to
  analyzer via TCP

### Communication Flows

**Flow 1: Analyzer Results to OpenELIS**
```

Analyzer (TCP) → Bridge (TCP listener) → OpenELIS (HTTP POST)

```

**Flow 2: OpenELIS Queries to Analyzer**
```

OpenELIS (HTTP POST) → Bridge (HTTP endpoint) → Analyzer (TCP)

````

### Multi-Analyzer Support

The bridge supports **multiple concurrent analyzer connections**:
- Single TCP listener port accepts connections from multiple analyzers
- One thread per connection (handles concurrent messages)
- Source analyzer IP included in HTTP forward (for OpenELIS identification)

## Configuration

### Configuration File Location

- **Development**: `volume/astm-bridge/configuration.yml` (mounted in Docker)
- **Production**: `/app/configuration.yml` (inside container)

### Configuration Properties

The bridge uses Spring Boot configuration properties. Example `configuration.yml`:

```yaml
org:
  itech:
    ahb:
      # Forward HTTP server (where to send messages from analyzers to OpenELIS)
      forward-http-server:
        uri: https://oe.openelis.org:8443/api/OpenELIS-Global/analyzer/astm
        # username: optional-username  # Optional HTTP basic auth
        # password: optional-password   # Optional HTTP basic auth
        # healthUri: https://oe.openelis.org:8443/actuator/health  # Optional health check
        # healthMethod: GET  # Optional health check method
        # healthBody: ""     # Optional health check body

      # ASTM Listen Server - LIS1-A protocol (port where analyzers connect)
      listen-astm-server:
        port: 12001  # Default: 12001

      # ASTM Listen Server - E1381-95 protocol
      listen-astm-server:
        e1381-95:
          port: 12011  # Default: 12011

      # ASTM Forward Server (default target for OpenELIS → Analyzer queries)
      # Note: OpenELIS can override via request parameters (forwardAddress, forwardPort)
      forward-astm-server:
        hostName: localhost  # Default analyzer IP (overridden by request params)
        port: 12001          # Default analyzer port (overridden by request params)
````

### Port Configuration

- **LIS1-A Listener**: Port 12001 (default) - accepts connections from analyzers
  using LIS1-A protocol
- **E1381-95 Listener**: Port 12011 (default) - accepts connections from
  analyzers using E1381-95 protocol
- **HTTP Endpoint**: Port 8443 (default) - receives HTTP POST requests from
  OpenELIS

**Docker Port Mapping** (from `docker-compose-dev.yml`):

- Host `12000` → Container `12001` (LIS1-A)
- Host `8442` → Container `8443` (HTTP)

### Multi-Analyzer Setup

**No special configuration needed!** The bridge automatically handles multiple
analyzers:

1. **Configure Analyzers**: Point analyzers to bridge IP and port (e.g.,
   `172.20.1.101:12001`)
2. **Bridge Forwards to OpenELIS**: All messages forward to configured OpenELIS
   endpoint
3. **OpenELIS Identifies Analyzer**: OpenELIS extracts source IP from
   `X-Source-Analyzer-IP` header (once implemented)

**Example**: Three analyzers connecting to same bridge:

- Analyzer 1 (192.168.1.10) → Bridge (12001) → OpenELIS (with
  `X-Source-Analyzer-IP: 192.168.1.10`)
- Analyzer 2 (192.168.1.11) → Bridge (12001) → OpenELIS (with
  `X-Source-Analyzer-IP: 192.168.1.11`)
- Analyzer 3 (192.168.1.12) → Bridge (12001) → OpenELIS (with
  `X-Source-Analyzer-IP: 192.168.1.12`)

### Bi-Directional Query Support

**OpenELIS → Analyzer Queries**:

OpenELIS sends queries to analyzers via HTTP POST to bridge:

```http
POST / HTTP/1.1
Host: bridge-host:8443
Content-Type: text/plain

H|\^&|||...

?forwardAddress=192.168.1.10&forwardPort=5000&forwardAstmVersion=LIS01_A
```

**Request Parameters**:

- `forwardAddress` (required): Target analyzer IP address
- `forwardPort` (required): Target analyzer port
- `forwardAstmVersion` (optional): Protocol version (`LIS01_A` or `E1381_95`,
  default: `LIS01_A`)

**Response**: Bridge forwards query to analyzer, returns analyzer response to
OpenELIS.

## Deployment

### Development

```bash
# Start bridge (included in dev.docker-compose.yml)
docker compose -f dev.docker-compose.yml up -d

# View logs
docker logs -f astm-http-bridge

# Check health
curl http://localhost:8442/actuator/health
```

### Production

See [Bridge GitHub Repository](https://github.com/DIGI-UW/astm-http-bridge) for
production deployment instructions.

## Troubleshooting

### Analyzer Can't Connect

1. **Check bridge is running**: `docker ps | grep astm-bridge`
2. **Check port mapping**: Verify host port 12000 maps to container port 12001
3. **Check firewall**: Ensure port 12000 is accessible from analyzer network
4. **Check logs**: `docker logs astm-http-bridge`

### Messages Not Reaching OpenELIS

1. **Check OpenELIS URL**: Verify `forward-http-server.uri` is correct
2. **Check SSL**: If using HTTPS, verify certificates (development:
   `verify: false`)
3. **Check OpenELIS endpoint**: Verify `/analyzer/astm` endpoint exists and is
   accessible
4. **Check logs**: Look for HTTP forward errors in bridge logs

### OpenELIS Can't Identify Analyzer

1. **Verify source IP header**: Check that `X-Source-Analyzer-IP` header is
   present in OpenELIS logs
2. **Check analyzer IP**: Verify analyzer IP matches OpenELIS
   `AnalyzerConfiguration` records
3. **Check OpenELIS identification logic**: Verify OpenELIS extracts header and
   looks up analyzer

## API Reference

### HTTP Endpoint (OpenELIS → Analyzer)

**Endpoint**: `POST /`

**Request Body**: ASTM message (plain text)

**Query Parameters**:

- `forwardAddress` (optional): Target analyzer IP (default: from config)
- `forwardPort` (optional): Target analyzer port (default: from config)
- `forwardAstmVersion` (optional): Protocol version (`LIS01_A` or `E1381_95`,
  default: `LIS01_A`)

**Response**: `HTTPHandlerServiceResponse` (JSON) with handler status

### Health Check

**Endpoint**: `GET /actuator/health`

**Response**: Health status (UP/DOWN)

## License

[License information]

````

### 2. Corrected Configuration File

**File**: `volume/astm-bridge/configuration.yml`

**Current** (incorrect structure):
```yaml
openelis:
  url: https://oe.openelis.org:8443/api/OpenELIS-Global/analyzer/astm
astm:
  port: 5001
````

**Corrected** (matches Spring Boot properties):

```yaml
# ASTM-HTTP Bridge Configuration
# Reference: https://github.com/DIGI-UW/astm-http-bridge

org:
  itech:
    ahb:
      # Forward HTTP server (where to send messages from analyzers to OpenELIS)
      forward-http-server:
        uri: https://oe.openelis.org:8443/api/OpenELIS-Global/analyzer/astm
        # username: optional-username  # Optional HTTP basic auth
        # password: optional-password   # Optional HTTP basic auth
        # healthUri: https://oe.openelis.org:8443/actuator/health  # Optional health check endpoint
        # healthMethod: GET  # Optional health check HTTP method (GET, POST, etc.)
        # healthBody: ""     # Optional health check request body

      # ASTM Listen Server - LIS1-A protocol (port where analyzers connect)
      listen-astm-server:
        port: 12001  # Default: 12001

      # ASTM Listen Server - E1381-95 protocol
      listen-astm-server:
        e1381-95:
          port: 12011  # Default: 12011

      # ASTM Forward Server (default target for OpenELIS → Analyzer queries)
      # Note: OpenELIS can override via request parameters (forwardAddress, forwardPort)
      forward-astm-server:
        hostName: localhost  # Default analyzer IP (overridden by request params)
        port: 12001          # Default analyzer port (overridden by request params)

# Logging configuration
logging:
  level:
    org.itech.ahb: DEBUG  # Set to INFO for production
```

### 3. Enhanced OpenELIS Documentation

**File**: `docs/astm.md`

**Add Section**: "Multi-Analyzer Configuration"

````markdown
## Multi-Analyzer Configuration

The ASTM-HTTP bridge supports multiple analyzers connecting to the same bridge
instance.

### Setup

1. **Configure Bridge**: No special configuration needed - bridge handles
   multiple connections automatically

   - Single TCP listener port (12001) accepts connections from all analyzers
   - One thread per connection (concurrent message handling)

2. **Configure Analyzers**: Point all analyzers to bridge IP and port

   - Example: Analyzer 1 (192.168.1.10) → Bridge (172.20.1.101:12001)
   - Example: Analyzer 2 (192.168.1.11) → Bridge (172.20.1.101:12001)

3. **Configure OpenELIS**: Register each analyzer in OpenELIS with correct IP
   address
   - OpenELIS identifies analyzer by source IP (from `X-Source-Analyzer-IP`
     header)
   - Analyzer IP must match `AnalyzerConfiguration.ipAddress` field

### Bi-Directional Query Flow

**OpenELIS Sends Query to Analyzer**:

1. OpenELIS constructs ASTM query message
2. OpenELIS sends HTTP POST to bridge:

   ```http
   POST http://bridge-host:8443/?forwardAddress=192.168.1.10&forwardPort=5000
   Content-Type: text/plain

   [ASTM query message]
   ```
````

3. Bridge forwards query to analyzer at specified IP/port
4. Bridge receives response from analyzer
5. Bridge returns response to OpenELIS
6. OpenELIS parses response, extracts fields

**Note**: Job management, timeout handling, and field extraction are handled by
OpenELIS, not the bridge.

```

### 4. Configuration Examples

**File**: `tools/astm-http-bridge/configuration-examples/`

Create example configuration files:

- `configuration-dev.yml.example` - Development setup
- `configuration-prod.yml.example` - Production setup
- `configuration-multi-analyzer.yml.example` - Multi-analyzer setup

### 5. Architecture Diagram

**File**: `tools/astm-http-bridge/docs/architecture.md`

Create visual diagrams showing:
- Multi-analyzer flow (multiple analyzers → bridge → OpenELIS)
- Bi-directional query flow (OpenELIS → bridge → analyzer)
- Port mapping diagram
- Network topology

---

## Implementation Tasks

1. **TXXX**: Create enhanced README.md in bridge repository (`tools/astm-http-bridge/README.md`)
   - Architecture overview (multi-analyzer, bi-directional)
   - Configuration property structure with correct property names
   - Multi-analyzer setup instructions
   - Bi-directional query flow explanation
   - Troubleshooting guide
   - API reference

2. **TXXX**: Update `volume/astm-bridge/configuration.yml` to match Spring Boot properties
   - Replace custom structure (`openelis.url`, `astm.port`) with correct property paths
   - Use `org.itech.ahb.forward-http-server.uri` for OpenELIS endpoint
   - Use `org.itech.ahb.listen-astm-server.port` for LIS1-A listener
   - Use `org.itech.ahb.listen-astm-server.e1381-95.port` for E1381-95 listener
   - Use `org.itech.ahb.forward-astm-server.hostName` and `port` for default analyzer target

3. **TXXX**: Add multi-analyzer section to `docs/astm.md`
   - Multi-analyzer setup instructions
   - Bi-directional query flow explanation
   - Source IP header explanation (once bridge update is implemented)

4. **TXXX**: Create configuration examples directory (`tools/astm-http-bridge/configuration-examples/`)
   - `configuration-dev.yml.example` - Development setup
   - `configuration-prod.yml.example` - Production setup
   - `configuration-multi-analyzer.yml.example` - Multi-analyzer setup

5. **TXXX**: Add architecture diagrams (optional, nice-to-have)
   - Multi-analyzer flow diagram
   - Bi-directional query flow diagram
   - Port mapping diagram

---

## Property Name Rationale

The property names follow a clear pattern:

- **`forward-http-server`**: Server that receives HTTP forwards (messages from analyzers → OpenELIS)
- **`listen-astm-server`**: Server that listens for ASTM messages (analyzers connect here)
- **`forward-astm-server`**: Server that receives ASTM forwards (messages from OpenELIS → analyzers)

The naming convention:
- **`forward-*`**: Where bridge forwards messages TO
- **`listen-*`**: Where bridge listens for incoming messages
- **`-http-server`**: HTTP endpoint (OpenELIS)
- **`-astm-server`**: ASTM/TCP endpoint (analyzers)

This makes it clear:
- `forward-http-server` = Analyzer → Bridge → OpenELIS (HTTP)
- `listen-astm-server` = Analyzer → Bridge (TCP listener)
- `forward-astm-server` = OpenELIS → Bridge → Analyzer (TCP)

## Benefits

1. **Clear Configuration**: Developers understand property structure and naming rationale
2. **Multi-Analyzer Clarity**: Explicit documentation that bridge supports multiple analyzers
3. **Bi-Directional Flow**: Clear explanation of OpenELIS → Analyzer queries
4. **Troubleshooting**: Better debugging guidance
5. **Onboarding**: New developers can understand bridge architecture quickly
6. **Property Naming**: Consistent, logical property names that match their purpose

```
