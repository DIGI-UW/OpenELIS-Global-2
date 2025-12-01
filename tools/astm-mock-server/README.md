# ASTM LIS2-A2 Mock Server

A simple mock server that simulates an ASTM-compatible laboratory analyzer for
testing the OpenELIS analyzer field mapping feature.

## Overview

This server implements a minimal subset of the ASTM LIS2-A2 protocol sufficient
for:

- Testing analyzer connection (ENQ/ACK handshake)
- Querying available analyzer fields
- Receiving sample results
- Processing QC (Quality Control) data

## Quick Start

### Running Locally

```bash
# Start the server
python server.py

# With custom options
python server.py --port 5000 --analyzer-type HEMATOLOGY --response-delay 100 --verbose
```

### Running with Docker

```bash
# Build the image
docker build -t astm-mock-server .

# Run the container
docker run -p 5000:5000 astm-mock-server

# With environment variables
docker run -p 5000:5000 \
  -e ANALYZER_TYPE=CHEMISTRY \
  -e RESPONSE_DELAY_MS=50 \
  astm-mock-server
```

### Running with Docker Compose (OpenELIS Integration)

From the repository root:

```bash
# Start OpenELIS with mock ASTM server
docker compose -f dev.docker-compose.yml -f docker-compose.astm-test.yml up -d
```

## Configuration

### Environment Variables

| Variable            | Default    | Description              |
| ------------------- | ---------- | ------------------------ |
| `ASTM_PORT`         | 5000       | Server port              |
| `ANALYZER_TYPE`     | HEMATOLOGY | Default analyzer type    |
| `RESPONSE_DELAY_MS` | 100        | Simulated response delay |

### Field Configuration

Edit `fields.json` to customize available analyzer fields:

```json
{
  "HEMATOLOGY": [
    {"name": "WBC", "type": "NUMERIC", "unit": "10^3/μL"},
    ...
  ],
  "CHEMISTRY": [...],
  "IMMUNOLOGY": [...],
  "MICROBIOLOGY": [...]
}
```

## ASTM Protocol Reference

### Control Characters

| Char | Hex  | Name                | Description        |
| ---- | ---- | ------------------- | ------------------ |
| ENQ  | 0x05 | Enquiry             | Start transmission |
| ACK  | 0x06 | Acknowledge         | Positive response  |
| NAK  | 0x15 | Neg Acknowledge     | Negative response  |
| EOT  | 0x04 | End of Transmission | End session        |
| STX  | 0x02 | Start of Text       | Frame start        |
| ETX  | 0x03 | End of Text         | Frame end          |
| CR   | 0x0D | Carriage Return     | Record separator   |
| LF   | 0x0A | Line Feed           |                    |

### Message Flow

1. Client sends ENQ
2. Server responds ACK (ready) or NAK (busy)
3. Client sends data frames: `<STX><FN><data><ETX><checksum><CR><LF>`
4. Server ACKs each frame
5. Client sends EOT to end transmission

### Record Types

| Type | Description | Example |
| ---- | ----------- | ------- | --- | -------------- | --------- | ---------------------- | ---------- | ------ | ---- |
| H    | Header      | `H      | \^& |                |           | Analyzer^Model^Version | ...`       |
| P    | Patient     | `P      | 1   |                | PatientID |                        |            | Gender | DOB` |
| O    | Order       | `O      | 1   | SampleID       |           | TestCode               | ...`       |
| R    | Result      | `R      | 1   | ^^^TestCode    | Value     | Unit                   | Flag`      |
| Q    | QC          | `Q      | 1   | Test^Lot^Level | Value     | Unit                   | Timestamp` |
| L    | Terminator  | `L      | 1   | N`             |

## Push Mode: Sending Results to OpenELIS

The mock server can push ASTM messages directly to OpenELIS via HTTP POST,
simulating an analyzer sending results:

```bash
# Push a single message
python server.py --push https://localhost:8443 --analyzer-type HEMATOLOGY

# Push multiple messages with interval
python server.py --push https://localhost:8443 --analyzer-type CHEMISTRY --push-count 5 --push-interval 2

# Verbose output
python server.py --push https://localhost:8443 --analyzer-type IMMUNOLOGY --verbose
```

**Push Options:**

- `--push URL`: OpenELIS base URL (e.g., `https://localhost:8443`)
- `--push-count N`: Number of messages to push (default: 1, ignored if
  `--push-continuous` used)
- `--push-interval N`: Seconds between pushes (default: 1)
- `--push-continuous`: Continuous push mode - push messages indefinitely
  (simulates real analyzer behavior)
- `--analyzer-type TYPE`: Analyzer type for message generation (HEMATOLOGY,
  CHEMISTRY, etc.)

**Push Modes:**

1. **Batch Mode** (default): Push a specified number of messages

   ```bash
   python server.py --push https://localhost:8443 --push-count 5 --push-interval 2
   ```

2. **Continuous Mode** (simulates real analyzer): Push messages indefinitely
   ```bash
   python server.py --push https://localhost:8443 --push-continuous --push-interval 30
   ```
   This mode simulates how real analyzers push results automatically when ready.
   Press Ctrl+C to stop.

**How It Works:**

1. Generates complete ASTM messages (H, P, O, R, L segments) based on field
   configuration
2. Creates realistic test data (patient IDs, sample IDs, result values)
3. Sends messages via HTTP POST to `/api/OpenELIS-Global/analyzer/astm`
4. Handles SSL verification (disabled for development with self-signed certs)

**Note:**

- Push mode requires OpenELIS to be running and accessible
- Messages are sent without authentication (OpenELIS may require session-based
  auth for production use)
- Continuous mode (`--push-continuous`) simulates real analyzer behavior where
  results are pushed automatically when ready

### API Mode: HTTP API for Triggering Pushes

The mock server can run an HTTP API server that accepts requests to trigger
pushes:

```bash
# Start API server on port 8080
python server.py --push https://localhost:8443 --api-port 8080
```

**API Endpoints:**

1. **POST `/push`** - Trigger a push to OpenELIS

   - Query parameters:
     - `analyzer_type` (optional): HEMATOLOGY, CHEMISTRY, IMMUNOLOGY, etc.
       (default: HEMATOLOGY)
     - `count` (optional): Number of messages to push (default: 1)
   - Request body (JSON, optional):
     ```json
     {
       "analyzer_type": "CHEMISTRY",
       "count": 3
     }
     ```
   - Response:
     ```json
     {
       "status": "completed",
       "total": 3,
       "successful": 3,
       "failed": 0,
       "results": [
         { "message_number": 1, "success": true, "analyzer_type": "CHEMISTRY" },
         { "message_number": 2, "success": true, "analyzer_type": "CHEMISTRY" },
         { "message_number": 3, "success": true, "analyzer_type": "CHEMISTRY" }
       ]
     }
     ```

2. **GET `/health`** - Health check and API information

**Usage Examples:**

```bash
# Trigger single push via curl
curl -X POST "http://localhost:8080/push?analyzer_type=HEMATOLOGY&count=1"

# Trigger multiple pushes with JSON body
curl -X POST http://localhost:8080/push \
  -H "Content-Type: application/json" \
  -d '{"analyzer_type": "CHEMISTRY", "count": 5}'

# Health check
curl http://localhost:8080/health
```

**Use Cases:**

- Automated testing scripts
- Integration with CI/CD pipelines
- Manual testing via HTTP client (Postman, etc.)
- Triggering pushes from other services

## Testing

### Run Communication Test (Recommended)

The `test_communication.py` script demonstrates the complete ASTM communication
pathway:

```bash
# Run comprehensive communication test
python test_communication.py --host localhost --port 5000
```

This test:

- ✅ Tests basic ENQ/ACK handshake
- ✅ Demonstrates complete message exchange (H, P, O, R, L records)
- ✅ Tests QC segment communication
- ✅ Verifies multiple simultaneous connections

See [COMMUNICATION_PATHWAY.md](COMMUNICATION_PATHWAY.md) for detailed protocol
documentation.

### Run Unit Tests

```bash
# Install test dependencies
pip install pytest

# Run unit tests (server must be running)
python server.py &
python -m pytest test_server.py -v
```

### Manual Testing

Use netcat or telnet to test the server:

```bash
# Connect
nc localhost 5000

# Send ENQ (Ctrl+E) and observe ACK response
```

## File Structure

```
tools/astm-mock-server/
├── server.py                    # Main server implementation
├── test_server.py               # Unit tests (TDD)
├── test_communication.py        # Communication pathway test & demo
├── fields.json                  # Analyzer field configuration
├── requirements.txt             # Python dependencies
├── Dockerfile                   # Container build file
├── README.md                    # This file
├── COMMUNICATION_PATHWAY.md     # Protocol documentation
└── ACCESS.md                    # Access guide for OpenELIS integration
```

## References

- [specs/004-astm-analyzer-mapping/research.md](../../specs/004-astm-analyzer-mapping/research.md) -
  ASTM protocol details
- [specs/004-astm-analyzer-mapping/spec.md](../../specs/004-astm-analyzer-mapping/spec.md) -
  Feature specification
- ASTM E1381 / LIS2-A2 Standard
