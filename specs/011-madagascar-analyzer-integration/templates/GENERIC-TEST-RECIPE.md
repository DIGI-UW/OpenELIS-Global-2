# Generic Analyzer Test Recipe

**Version:** 1.0.0  
**Date:** 2026-02-02  
**Feature:** 011-madagascar-analyzer-integration

## Purpose

This document provides a step-by-step recipe for testing ANY analyzer type (HL7,
ASTM TCP, ASTM RS232, FILE) using the unified test harness. This recipe applies
to all supported analyzers in the Madagascar inventory.

---

## Prerequisites

- Docker and Docker Compose installed
- OpenELIS development environment built
  (`mvn clean install -DskipTests -Dmaven.test.skip=true`)
- Mock server and bridge tools available in `tools/`

---

## Step 1: Start Full Test Stack

### Single Startup Command

```bash
cd /home/ubuntu/OpenELIS-Global-2

# Start OpenELIS with analyzer infrastructure (mock server + ASTM bridge + virtual serial)
docker compose -f dev.docker-compose.yml -f analyzer-setup.docker-compose.yml up -d

# Wait for services to be ready (60-90 seconds)
docker compose logs -f oe.openelis.org | grep "Started"
```

**What this brings up**:

- OpenELIS webapp (`oe.openelis.org`) at `https://localhost`
- PostgreSQL database (`openelisglobal-database`)
- ASTM mock server (`astm-simulator`) at `172.20.1.100:5000`
- ASTM-HTTP bridge (`openelis-analyzer-bridge`) at `172.20.1.101:12001`
- Virtual serial ports (`virtual-serial`) at `/dev/serial/ttyVUSB0-4`

---

## Step 2: Load Analyzer Fixtures

```bash
# Load all analyzer test data (Feature 004 + 011)
./src/test/resources/load-analyzer-test-data.sh --all

# OR load only Madagascar fixtures (Feature 011)
./src/test/resources/load-analyzer-test-data.sh --dataset-011
```

**Expected Output:**

```
======================================
  OpenELIS Analyzer Test Data Loader
======================================

[INFO] Load Mode: ALL
[INFO] Operation: REFRESH

[INFO] Loading dataset: testdata/analyzer-mapping-test-data.xml
[INFO] Loading dataset: testdata/madagascar-analyzer-test-data.xml

[INFO] Verifying loaded data...

Feature 004 Data (IDs 1000-1004):
 entity        | count
---------------+-------
 analyzers     |     5

Feature 011 Data (IDs 2000-2011):
 entity                  | count
-------------------------+-------
 analyzers               |    11
 analyzer_configurations |    11
 serial_configs          |     5
 file_configs            |     3

[INFO] Analyzer test data loading complete!
```

---

## Step 3: Verify Dashboard

### Access UI

1. Open browser to `https://localhost/`
2. Login with:
   - Username: `admin`
   - Password: `adminADMIN!`
3. Navigate to **Analyzers** menu (or `/AnalyzerDashboard`)

### Expected Dashboard State

All 11 analyzers should be visible with correct badges:

| ID   | Analyzer                | Category    | Protocol Badge | Transport Badge |
| ---- | ----------------------- | ----------- | -------------- | --------------- |
| 2000 | Abbott Architect        | IMMUNOLOGY  | HL7            | TCP/IP          |
| 2002 | Cepheid GeneXpert       | MOLECULAR   | FILE           | Filesystem      |
| 2003 | Hain FluoroCycler XT    | MOLECULAR   | FILE           | Filesystem      |
| 2004 | Horiba ABX Micros 60    | HEMATOLOGY  | ASTM           | RS232           |
| 2005 | Horiba ABX Pentra 60 C+ | HEMATOLOGY  | ASTM           | RS232           |
| 2006 | Mindray BA-88A          | CHEMISTRY   | ASTM           | RS232           |
| 2007 | Mindray BC-5380         | HEMATOLOGY  | HL7            | TCP/IP          |
| 2008 | Mindray BS-360E         | CHEMISTRY   | HL7            | TCP/IP          |
| 2009 | QuantStudio 7 Flex      | MOLECULAR   | FILE           | Filesystem      |
| 2010 | Stago STart 4           | COAGULATION | ASTM           | RS232           |
| 2011 | Sysmex XN Series        | HEMATOLOGY  | ASTM           | TCP/IP          |

---

## Step 4: Protocol-Specific Testing

### A. HL7 over TCP/IP (Priority 1)

**Target Analyzers:** Abbott Architect (2000), Mindray BC-5380 (2007), Mindray
BS-360E (2008)

**Test Connection (UI)**:

1. Select analyzer (e.g., Mindray BC-5380)
2. Click "Test Connection" button
3. Expected: Green success message "TCP connection successful - HL7 listener
   reachable"

**Send Test Results (Command Line)**:

```bash
cd tools/analyzer-mock-server

# Start HL7 simulator in push mode
python3 server.py --hl7 --push https://localhost:8443/api/OpenELIS-Global/analyzer/hl7 \
    --template templates/mindray_bc5380.json

# Expected: HTTP 200 response
# Expected: Results appear in OpenELIS AnalyzerResults table
```

**Verify Results:**

```bash
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT id, analyzer_id, accession_number, test_name, result
FROM analyzer_results
WHERE analyzer_id = 2007
ORDER BY last_updated DESC LIMIT 5;
"
```

---

### B. ASTM over TCP/IP via Bridge (Priority 1)

**Target Analyzer:** Sysmex XN Series (2011)

**Architecture Flow:**

```
Mock Server (172.20.1.100:5000)
    ↓ ASTM protocol
ASTM-HTTP Bridge (172.20.1.101:12001)
    ↓ HTTP POST /analyzer/astm
OpenELIS (oe.openelis.org:8443)
```

**Test Connection (UI)**:

1. Select Sysmex XN Series
2. Click "Test Connection" button
3. Expected: Green success message "Connection successful - ACK received"

**Send Test Results (Command Line)**:

```bash
cd tools/analyzer-mock-server

# Push ASTM to bridge (which forwards to OE)
python3 server.py --push http://172.20.1.101:12001 \
    --template templates/sysmex_xn.json \
    --analyzer-type HEMATOLOGY

# Expected: ASTM ENQ/ACK handshake with bridge
# Expected: Bridge forwards HTTP POST to OpenELIS
# Expected: Results inserted via SysmexXN-L plugin
```

**Verify Results:**

```bash
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT id, analyzer_id, accession_number, test_name, result
FROM analyzer_results
WHERE analyzer_id = 2011
ORDER BY last_updated DESC LIMIT 5;
"
```

---

### C. ASTM over RS232 via Virtual Serial (Priority 2)

**Target Analyzers:** Horiba Micros 60 (2004), Horiba Pentra 60 (2005), Mindray
BA-88A (2006), Stago STart 4 (2010)

**Architecture Flow:**

```
Mock Server (serial write to /serial/ttyVUSB0-peer)
    ↓ Virtual serial port pair
OpenELIS reads from /dev/serial/ttyVUSB0
    ↓ SerialAnalyzerReader
AnalyzerImporterPlugin (HoribaPentra60)
    ↓ processData()
AnalyzerResults table
```

**Test Serial Port (UI)**:

1. Navigate to Serial Port Configurations
2. Select SERIAL-2005 (Horiba Pentra 60)
3. Click "Test Connection"
4. Expected: Green success message "Serial port accessible:
   /dev/serial/ttyVUSB1"

**Send Test Results (Command Line)**:

```bash
cd tools/analyzer-mock-server

# Simulate ASTM data on virtual serial port
python3 server.py --serial-port /serial/ttyVUSB1-peer \
    --template templates/horiba_pentra60.json \
    --serial-mode send

# In another terminal, trigger read via API
curl -X POST https://localhost:8443/api/OpenELIS-Global/rest/analyzer/serial-port/configurations/SERIAL-2005/read-once \
    -H "Content-Type: application/json" \
    --insecure
```

**Expected Response:**

```json
{
  "configurationId": "SERIAL-2005",
  "analyzerId": 2005,
  "readSuccess": true,
  "processSuccess": true,
  "message": "Data read and processed successfully"
}
```

---

### D. FILE-Based Import (Priority 3)

**Target Analyzers:** GeneXpert (2002), FluoroCycler XT (2003), QuantStudio 7
Flex (2009)

**Architecture Flow:**

```
Mock Server generates CSV file
    ↓ Writes to /data/analyzer-imports/genexpert/
OpenELIS FileAnalyzerReaderService watches directory
    ↓ Detects file, reads, deletes
AnalyzerImporterPlugin (GeneXpertFile)
    ↓ processData()
AnalyzerResults table
```

**Test File Configuration (UI)**:

1. Select GeneXpert (2002)
2. View File Import Configuration
3. Expected: Import Directory `/data/analyzer-imports/genexpert`, Pattern
   `*.csv`, Active

**Generate Test File (Command Line)**:

```bash
cd tools/analyzer-mock-server

# Generate mock CSV file
python3 server.py --generate-files ./../../volume/analyzer-imports/genexpert \
    --generate-files-analyzer genexpert \
    --template templates/genexpert.json

# Expected: CSV file created in volume/analyzer-imports/genexpert/
# Expected: OpenELIS file watcher picks it up within 30 seconds
```

**Verify File Processed:**

```bash
# Check that file was consumed
ls -la volume/analyzer-imports/genexpert/
# Expected: Directory empty or only .processed files

# Check results
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT id, analyzer_id, accession_number, test_name, result
FROM analyzer_results
WHERE analyzer_id = 2002
ORDER BY last_updated DESC LIMIT 5;
"
```

---

## Step 5: Run Cypress E2E Tests

### Individual Test (During Development)

```bash
cd frontend

# Run generic analyzer dashboard test
npm run cy:spec "cypress/e2e/analyzerDashboardGeneric.cy.js"
```

### Full Analyzer Suite (Before Pushing)

```bash
cd frontend

# Run all analyzer tests with fail-fast
npm run cy:failfast -- --spec "cypress/e2e/analyzer*.cy.js"
```

**Expected**: All tests pass, including:

- analyzerConfiguration.cy.js
- analyzerHappyPathUserStories.cy.js
- analyzerDashboardGeneric.cy.js

---

## Troubleshooting

### Issue: HL7 Connection Test Fails

**Symptoms:**

- "Connection refused" error
- "Invalid response" error

**Diagnosis:**

```bash
# Check if mock server is running
docker ps | grep astm-simulator

# Check mock server logs
docker compose logs astm-simulator

# Verify mock server is listening on port 2575
docker exec openelis-astm-simulator netstat -tuln | grep 2575
```

**Solution:**

```bash
# Restart mock server with HL7 mode
docker compose -f dev.docker-compose.yml -f analyzer-setup.docker-compose.yml down
docker compose -f dev.docker-compose.yml -f analyzer-setup.docker-compose.yml up -d
```

---

### Issue: ASTM Bridge Connection Fails

**Symptoms:**

- "Connection timeout" to bridge (172.20.1.101:12001)
- Bridge healthcheck failing

**Diagnosis:**

```bash
# Check bridge status
docker ps | grep openelis-analyzer-bridge

# Check bridge logs for errors
docker compose logs openelis-analyzer-bridge

# Verify bridge can reach mock server
docker exec openelis-astm-bridge ping -c 3 172.20.1.100
```

**Solution:**

```bash
# Rebuild and restart bridge
docker compose -f dev.docker-compose.yml -f analyzer-setup.docker-compose.yml up -d --no-deps --force-recreate openelis-analyzer-bridge
```

---

### Issue: Serial Port Not Found

**Symptoms:**

- "Serial port not accessible: /dev/serial/ttyVUSB0"
- Test connection fails for RS232 analyzers

**Diagnosis:**

```bash
# Check if virtual-serial container is running
docker ps | grep virtual-serial

# Check if serial volume is mounted
docker exec openelisglobal-webapp ls -la /dev/serial/

# Check socat logs
docker compose logs virtual-serial
```

**Solution:**

```bash
# Recreate virtual serial infrastructure
docker compose -f dev.docker-compose.yml -f analyzer-setup.docker-compose.yml down
docker volume rm openelis-global-2_serial-vol
docker compose -f dev.docker-compose.yml -f analyzer-setup.docker-compose.yml up -d
```

---

### Issue: FILE Import Directory Not Found

**Symptoms:**

- "Import directory not accessible: /data/analyzer-imports/genexpert"
- Files placed in host directory not detected

**Diagnosis:**

```bash
# Check if volume is mounted
docker exec openelisglobal-webapp ls -la /data/analyzer-imports/

# Check host directory exists
ls -la volume/analyzer-imports/genexpert/
```

**Solution:**

```bash
# Create host directories if missing
mkdir -p volume/analyzer-imports/{genexpert,fluorocycler,quantstudio}

# Restart OE container to mount volume
docker compose -f dev.docker-compose.yml restart oe.openelis.org
```

---

### Issue: No Analyzers in Dashboard

**Symptoms:**

- Analyzer Dashboard loads but shows empty list
- "No analyzers configured" message

**Diagnosis:**

```bash
# Check if fixtures are loaded
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT COUNT(*) as analyzer_count FROM analyzer WHERE id BETWEEN 2000 AND 2011;
"
# Expected: 11

# Check analyzer_configuration
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT COUNT(*) as config_count FROM analyzer_configuration WHERE id LIKE 'CONFIG-20%';
"
# Expected: 11
```

**Solution:**

```bash
# Reload fixtures
./src/test/resources/load-analyzer-test-data.sh --dataset-011 --reset

# Clear browser cache and reload dashboard
```

---

## Quick Reference: Mock Server Commands

### HL7 Push Mode

```bash
cd tools/analyzer-mock-server

# Push HL7 ORU^R01 to OpenELIS
python3 server.py --hl7 \
    --push https://localhost:8443/api/OpenELIS-Global/analyzer/hl7 \
    --template templates/mindray_bc5380.json
```

### ASTM Push Mode (via Bridge)

```bash
cd tools/analyzer-mock-server

# Push ASTM to bridge (which forwards to OpenELIS)
python3 server.py --push http://172.20.1.101:12001 \
    --template templates/sysmex_xn.json \
    --analyzer-type HEMATOLOGY
```

### ASTM Serial Mode

```bash
cd tools/analyzer-mock-server

# Send ASTM data to virtual serial port
python3 server.py --serial-port /serial/ttyVUSB1-peer \
    --template templates/horiba_pentra60.json \
    --serial-mode send
```

### FILE Generation Mode

```bash
cd tools/analyzer-mock-server

# Generate CSV file for FILE-based analyzer
python3 server.py --generate-files ../../volume/analyzer-imports/genexpert \
    --generate-files-analyzer genexpert \
    --template templates/genexpert.json
```

---

## Verification Checklist

After running tests for any analyzer type, verify:

- [ ] Dashboard shows analyzer with correct name
- [ ] Protocol badge displays correctly (HL7 / ASTM / FILE)
- [ ] Transport badge displays correctly (TCP/IP / RS232 / Filesystem)
- [ ] Category displays correctly (HEMATOLOGY / MOLECULAR / CHEMISTRY / etc.)
- [ ] Active status indicator shows green
- [ ] Test Connection succeeds (for TCP analyzers)
- [ ] Results appear in AnalyzerResults table
- [ ] No errors in browser console
- [ ] No errors in OpenELIS logs

**Database Query:**

```bash
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT a.id, a.name, a.analyzer_type,
       ac.protocol_version, ac.ip_address, ac.port,
       COUNT(ar.id) as result_count
FROM analyzer a
LEFT JOIN analyzer_configuration ac ON a.id = ac.analyzer_id
LEFT JOIN analyzer_results ar ON a.id = ar.analyzer_id
WHERE a.id BETWEEN 2000 AND 2011
GROUP BY a.id, a.name, a.analyzer_type, ac.protocol_version, ac.ip_address, ac.port
ORDER BY a.id;
"
```

---

## Test Recipe Template (Copy for Each Analyzer)

### Analyzer: {Name} (ID: {XXXX})

**Protocol:** {HL7 / ASTM / FILE}  
**Transport:** {TCP/IP / RS232 / Filesystem}

**Steps:**

1. **Verify Configuration**:

   - [ ] Dashboard shows analyzer with correct badges
   - [ ] Configuration present (IP/port OR serial config OR file config)

2. **Test Connection** (if applicable):

   - [ ] Click "Test Connection" in UI
   - [ ] Expected: Success message

3. **Send Test Data**:

   ```bash
   # {Protocol-specific command from Quick Reference above}
   ```

4. **Verify Results**:

   ```bash
   docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
   SELECT COUNT(*) FROM analyzer_results WHERE analyzer_id = {XXXX};
   "
   # Expected: > 0
   ```

5. **Check Logs** (if errors):
   ```bash
   docker compose logs oe.openelis.org | grep "analyzer"
   ```

---

## Success Criteria

Generic test is successful when ALL of the following pass:

- [ ] All 11 analyzers visible in dashboard
- [ ] Protocol badges correct for all analyzers
- [ ] Test Connection works for HL7 analyzers (TCP only)
- [ ] Test Connection works for ASTM TCP analyzers (ENQ/ACK)
- [ ] Test Connection works for RS232 analyzers (port accessible)
- [ ] Test Connection works for FILE analyzers (directory accessible)
- [ ] Mock server can push HL7 results successfully
- [ ] Mock server can push ASTM results via bridge successfully
- [ ] Mock server can send serial data to virtual ports successfully
- [ ] Mock server can generate files that OpenELIS processes successfully
- [ ] Cypress E2E test `analyzerDashboardGeneric.cy.js` passes
- [ ] No errors in browser console
- [ ] No errors in OpenELIS logs

---

## Related Documentation

- [VERIFICATION-GUIDE.md](../research/VERIFICATION-GUIDE.md) - Manual
  verification steps for Feature 011
- [supported-analyzers.md](../contracts/supported-analyzers.md) - Authoritative
  analyzer inventory
- [template-fixture-mapping.md](../contracts/template-fixture-mapping.md) - Mock
  template to fixture mapping

---

**Maintained By:** OpenELIS Global Feature 011 Team  
**Last Updated:** 2026-02-02
