# Manual Testing Guide for T105 - Analyzer Query Service

## Overview

This guide provides step-by-step instructions for manually testing the
`AnalyzerQueryServiceImpl` implementation (T105) against the verification
checklist.

## Prerequisites

1. **OpenELIS Running**: Ensure OpenELIS is running and accessible

   ```bash
   docker compose -f dev.docker-compose.yml up -d
   ```

2. **ASTM Mock Server Running**: Start the mock server

   ```bash
   # Option 1: Via Docker Compose (recommended)
   docker compose -f dev.docker-compose.yml -f docker-compose.astm-test.yml up -d openelis-astm-simulator

   # Option 2: Direct Python execution
   cd tools/astm-mock-server
   python server.py --port 5000 --analyzer-type HEMATOLOGY
   ```

3. **Test Analyzer Configured**: Verify analyzer ID 1000 is configured
   ```sql
   SELECT a.id, a.name, ac.ip_address, ac.port, ac.status
   FROM analyzer a
   JOIN analyzer_configuration ac ON a.id = ac.analyzer_id
   WHERE a.id = '1000';
   ```

## Test Scenarios

### Test 1: Query Analyzer via UI

**Objective**: Verify end-to-end query workflow from UI

**Steps**:

1. Log into OpenELIS as administrator
2. Navigate to **Analyzers** → **Field Mappings** (or direct URL:
   `/analyzers/field-mapping`)
3. Select analyzer **"Mock Hematology Analyzer"** (ID: 1000) from the list
4. Click **"Query Analyzer"** button
5. Observe:
   - Job ID is returned immediately (async pattern)
   - Progress indicator shows 0% → 100%
   - Connection logs appear in real-time
   - Fields appear in source panel after completion

**Expected Results**:

- ✅ Job starts immediately (no blocking)
- ✅ Progress updates: 0% → 10% (starting) → 20% (connecting) → 30% (connected)
  → 40% (handshake) → 50% (query sent) → 60% (receiving) → 80% (parsing) → 100%
  (complete)
- ✅ Connection logs show: "TCP connection established", "Sending ENQ",
  "Received ACK", etc.
- ✅ Fields appear in source panel with correct metadata:
  - Field names: WBC, RBC, HGB, HCT, PLT, etc.
  - Field types: NUMERIC (correctly identified)
  - Units: 10^3/μL, g/dL, %, etc. (correctly parsed)
  - ASTM references: R|1|^^^WBC, etc.

**Verification Checklist**:

- [ ] Background job executes asynchronously (returns job ID immediately)
- [ ] Job status updates progress (0% → 100%) with connection logs
- [ ] Extracts field identifiers from R (Result) records
- [ ] Parses field metadata: fieldName, astmRef, fieldType, unit
- [ ] Robust parsing handles units with special characters (e.g., `10^3/μL`)
- [ ] Field type validation against AnalyzerField.FieldType enum
- [ ] Stores extracted fields in AnalyzerField entity

### Test 2: Query Status Polling

**Objective**: Verify job status polling endpoint

**Steps**:

1. Start a query (as in Test 1)
2. Note the job ID returned
3. Poll status endpoint (via browser DevTools Network tab or API client):
   ```
   GET /rest/analyzer/query/status?analyzerId=1000&jobId={jobId}
   ```
4. Observe status updates over time

**Expected Results**:

- ✅ Status object contains:
  - `analyzerId`: "1000"
  - `jobId`: UUID string
  - `state`: "pending" → "in_progress" → "completed"
  - `progress`: 0 → 100
  - `logs`: Array of log messages
  - `fields`: Array of extracted fields (after completion)
  - `error`: null (if successful)

**Verification Checklist**:

- [ ] Status endpoint returns correct job information
- [ ] Progress updates in real-time
- [ ] Logs accumulate during execution
- [ ] Fields array populated after completion

### Test 3: Timeout Handling

**Objective**: Verify query timeout configuration

**Steps**:

1. Check SiteInformation for timeout:
   ```sql
   SELECT * FROM clinlims.site_information WHERE name = 'analyzer.query.timeout';
   ```
2. If missing, add default (5 minutes):
   ```sql
   INSERT INTO clinlims.site_information (id, name, lastupdated, description, encrypted, domain_id, value_type, value, "group")
   VALUES (
     nextval('clinlims.site_information_seq'),
     'analyzer.query.timeout',
     NOW(),
     'Query analyzer timeout in minutes',
     false,
     (SELECT id FROM clinlims.site_information_domain WHERE name = 'siteIdentity'),
     'text',
     '5',
     0
   );
   ```
3. Start query with mock server that delays response (simulate timeout)
4. Observe timeout behavior

**Expected Results**:

- ✅ Query uses timeout from SiteInformation
- ✅ If SiteInformation missing, defaults to 5 minutes
- ✅ Timeout error displayed gracefully
- ✅ Job status shows "failed" state with error message

**Verification Checklist**:

- [ ] Reads timeout from SiteInformation (default: 5 minutes if missing/invalid)
- [ ] Handles connection errors gracefully (sets state to "failed", includes
      error message)

### Test 4: Connection Error Handling

**Objective**: Verify graceful handling of connection failures

**Steps**:

1. Stop the mock server (or use invalid IP/port)
2. Update analyzer configuration with invalid IP:
   ```sql
   UPDATE analyzer_configuration
   SET ip_address = '192.0.2.1', port = 5000
   WHERE analyzer_id = '1000';
   ```
3. Attempt to query analyzer
4. Observe error handling

**Expected Results**:

- ✅ Connection error caught gracefully
- ✅ Job status shows "failed" state
- ✅ Error message displayed: "Connection refused" or "Connection timeout"
- ✅ No application crash or unhandled exception

**Verification Checklist**:

- [ ] Handles connection errors gracefully (sets state to "failed", includes
      error message)

### Test 5: Field Parsing Verification

**Objective**: Verify R-record parsing handles all edge cases

**Steps**:

1. Query analyzer successfully (as in Test 1)
2. Verify extracted fields in database:
   ```sql
   SELECT field_name, astm_ref, field_type, unit
   FROM analyzer_field
   WHERE analyzer_id = '1000'
   ORDER BY field_name;
   ```
3. Compare with expected fields from `tools/astm-mock-server/fields.json`

**Expected Results**:

- ✅ All fields from mock server are extracted
- ✅ Field names match: WBC, RBC, HGB, HCT, PLT, etc.
- ✅ Units correctly parsed: `10^3/μL`, `g/dL`, `%`, etc.
- ✅ Field types correctly identified: NUMERIC
- ✅ ASTM references correctly parsed: `R|1|^^^WBC`, etc.

**Verification Checklist**:

- [ ] Extracts field identifiers from R (Result) records per FR-002 requirement
- [ ] Parses field metadata: fieldName, astmRef, fieldType, unit (handles
      R-record format: `R|seq|astm_ref|field_name||unit|||field_type`)
- [ ] Robust parsing handles units with special characters (e.g., `10^3/μL`) and
      composite delimiters
- [ ] Field type validation against AnalyzerField.FieldType enum (defaults to
      NUMERIC if invalid)
- [ ] Stores extracted fields in AnalyzerField entity via AnalyzerFieldService

### Test 6: TCP Protocol Verification

**Objective**: Verify ASTM LIS2-A2 protocol implementation

**Steps**:

1. Monitor network traffic (optional: use Wireshark or tcpdump)
2. Start query and observe protocol handshake
3. Verify protocol sequence:
   - Client → Server: ENQ (0x05)
   - Server → Client: ACK (0x06)
   - Client → Server: STX + Frame + ETX + Checksum + CR + LF
   - Server → Client: ACK
   - Client → Server: EOT (0x04)
   - Server → Client: ENQ (0x05) [server initiates response]
   - Client → Server: ACK (0x06)
   - Server → Client: STX + Data Frames + ETX + Checksum + CR + LF
   - Client → Server: ACK (for each frame)
   - Server → Client: EOT (0x04)

**Expected Results**:

- ✅ ENQ/ACK handshake completes successfully
- ✅ Frame format correct: `<STX><FN><data><ETX><checksum><CR><LF>`
- ✅ Checksum calculation correct
- ✅ Frame acknowledgment works
- ✅ EOT properly terminates transmission

**Verification Checklist**:

- [ ] Implementation connects to analyzer via TCP using IP:Port from
      AnalyzerConfiguration
- [ ] Sends ASTM LIS2-A2 query message (ENQ/ACK handshake, header frame, EOT)
- [ ] Receives and parses ASTM response frames (STX/FN/data/ETX/checksum/CR/LF)

## API Testing (Alternative to UI)

### Using curl

```bash
# Start query
curl -X POST "https://localhost:8443/api/OpenELIS-Global/rest/analyzer/query/start?analyzerId=1000" \
  -H "Cookie: JSESSIONID=..." \
  -k

# Response: {"jobId": "uuid-here"}

# Poll status
curl "https://localhost:8443/api/OpenELIS-Global/rest/analyzer/query/status?analyzerId=1000&jobId={jobId}" \
  -H "Cookie: JSESSIONID=..." \
  -k

# Response: {"analyzerId": "1000", "jobId": "...", "state": "completed", "progress": 100, "fields": [...]}
```

### Using Postman/Insomnia

1. Create POST request to `/rest/analyzer/query/start?analyzerId=1000`
2. Extract `jobId` from response
3. Create GET request to
   `/rest/analyzer/query/status?analyzerId=1000&jobId={jobId}`
4. Poll status endpoint until `state` is "completed" or "failed"

## Troubleshooting

### Mock Server Not Responding

```bash
# Check if server is running
docker ps | grep astm

# Check server logs
docker logs openelis-astm-simulator

# Test connection manually
nc localhost 5000
# Press Ctrl+C, then type: \x05 (ENQ character)
```

### Analyzer Configuration Missing

```bash
# Check analyzer configuration
docker exec openelisglobal-database psql -U clinlims -d clinlims -c \
  "SELECT a.id, a.name, ac.ip_address, ac.port FROM analyzer a LEFT JOIN analyzer_configuration ac ON a.id = ac.analyzer_id WHERE a.id = '1000';"

# If missing, insert test data
docker exec -i openelisglobal-database psql -U clinlims -d clinlims < src/test/resources/analyzer-test-data.sql
```

### Fields Not Appearing

1. Check query job status for errors
2. Verify mock server is returning R-records
3. Check application logs for parsing errors
4. Verify `analyzer_field` table has entries:
   ```sql
   SELECT * FROM analyzer_field WHERE analyzer_id = '1000';
   ```

## Success Criteria

All verification checklist items should pass:

- ✅ Implementation connects to analyzer via TCP
- ✅ Sends ASTM LIS2-A2 query message correctly
- ✅ Receives and parses ASTM response frames
- ✅ Extracts field identifiers from R records
- ✅ Parses field metadata correctly
- ✅ Handles units with special characters
- ✅ Validates field types against enum
- ✅ Stores fields in AnalyzerField entity
- ✅ Background job executes asynchronously
- ✅ Job status updates progress with logs
- ✅ Reads timeout from SystemConfiguration
- ✅ Handles connection errors gracefully
- ✅ Integration test verifies actual ASTM query execution
- ✅ Manual test: Query analyzer 1000 and verify fields appear in UI

## Next Steps

After successful manual testing:

1. Mark T105 as complete in `tasks.md`
2. Update verification checklist with test results
3. Document any issues found during testing
4. Proceed with remaining Phase 8 polish tasks
