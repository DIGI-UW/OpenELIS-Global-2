# Manual Testing Checklist: ASTM Analyzer Field Mapping

**Feature**: 004-astm-analyzer-mapping  
**Version**: 1.0  
**Last Updated**: 2025-01-15

## Overview

This checklist provides step-by-step manual testing procedures for the ASTM
Analyzer Field Mapping feature. It covers all three user stories and their
associated functional requirements from `spec.md`.

## Prerequisites

### Environment Setup

- [ ] OpenELIS development environment running (`dev.docker-compose.yml`)
- [ ] **ASTM mock server running** (see "Mock Server Setup" below)
- [ ] Test data loaded (`load-analyzer-test-data.sh` executed)
- [ ] Admin user credentials available
- [ ] Browser developer tools accessible (for console log review)

### Mock Server Setup

**The ASTM mock server simulates a laboratory analyzer for testing.**

1. **Start the mock server:**

   ```bash
   docker compose -f dev.docker-compose.yml -f docker-compose.astm-test.yml up -d astm-simulator
   ```

2. **Verify it's running:**

   ```bash
   docker ps | grep astm-simulator
   # Should show: openelis-astm-simulator (healthy)
   ```

3. **Test the connection:**

   ```bash
   cd tools/astm-mock-server
   python3 test_communication.py
   # Should show: ✅ All tests passed!
   ```

4. **Mock Server Details:**
   - **Container**: `openelis-astm-simulator`
   - **Static IP**: `172.20.1.100` (configured in docker-compose)
   - **Port**: `5000`
   - **Analyzer Type**: HEMATOLOGY (10 fields: WBC, RBC, HGB, HCT, MCV, MCH,
     MCHC, PLT, RDW, MPV)
   - **Access**: From OpenELIS container use IP `172.20.1.100:5000`

**Note**: Analyzer 1000 is pre-configured to use the mock server at
`172.20.1.100:5000`.

### Test Data Verification

Before testing, verify test data is loaded:

```sql
-- Check analyzers exist
SELECT id, name, analyzer_type, is_active FROM analyzer WHERE id >= 1000;

-- Check analyzer configurations
SELECT id, analyzer_id, status FROM analyzer_configuration;

-- Check analyzer fields
SELECT COUNT(*) FROM analyzer_field WHERE analyzer_id >= 1000;

-- Check mappings exist
SELECT COUNT(*) FROM analyzer_field_mapping WHERE analyzer_id >= 1000;
```

---

## User Story 1: Configure Field Mappings (P1)

**Reference**: spec.md lines 21-49, FR-001 through FR-008

### Test Case 1.1: Register New Analyzer

**Option A: Use Pre-configured Test Analyzer (Recommended)**

- Analyzer ID 1000 ("Hematology Analyzer 1") is already configured
- IP: `172.20.1.100`, Port: `5000` (points to mock server)
- Skip to Test Case 1.2

**Option B: Register New Analyzer**

| Step | Action                                     | Expected Result                   | Pass |
| ---- | ------------------------------------------ | --------------------------------- | ---- |
| 1    | Navigate to `/analyzers`                   | Analyzer management page loads    | ☐    |
| 2    | Click "Add Analyzer" button                | Add analyzer form/modal opens     | ☐    |
| 3    | Enter analyzer name: "Test Hematology 001" | Name field accepts input          | ☐    |
| 4    | Select type: "HEMATOLOGY"                  | Dropdown selection works          | ☐    |
| 5    | Enter IP: **`172.20.1.100`**               | IP field accepts IPv4 address     | ☐    |
| 6    | Enter Port: **`5000`**                     | Port field accepts value          | ☐    |
| 7    | Click "Save"                               | Analyzer created, appears in list | ☐    |
| 8    | Verify status shows "SETUP"                | Initial status is SETUP           | ☐    |

**⚠️ Important**: Use IP `172.20.1.100` (mock server static IP), not container
name. The database field only accepts IPv4 addresses.

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 1.2: Test Analyzer Connection (FR-001)

**Prerequisite**: Mock server must be running (see "Mock Server Setup" above)

| Step | Action                                               | Expected Result                                    | Pass |
| ---- | ---------------------------------------------------- | -------------------------------------------------- | ---- |
| 1    | Select analyzer from list (ID 1000 or newly created) | Analyzer details panel opens                       | ☐    |
| 2    | Click "Test Connection" button                       | Progress indicator appears                         | ☐    |
| 3    | Observe connection log                               | Shows "Connecting to 172.20.1.100:5000..." message | ☐    |
| 4    | Wait for completion (≤30 seconds)                    | Connection test completes                          | ☐    |
| 5    | Verify success message                               | "Connection successful" displayed                  | ☐    |
| 6    | Check log shows ACK received                         | Protocol handshake (ENQ→ACK) logged                | ☐    |

**Failure Scenario** (if mock server not running): | Step | Action | Expected
Result | Pass | |------|--------|-----------------|------| | 1 | Stop mock
server: `docker stop openelis-astm-simulator` | Server stopped | ☐ | | 2 | Click
"Test Connection" | - | ☐ | | 3 | Wait for timeout (30 seconds) | Connection
timeout message | ☐ | | 4 | Verify error logged | Error appears in log | ☐ | | 5
| Restart mock server: `docker start openelis-astm-simulator` | Server restarted
| ☐ |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 1.3: Query Analyzer Fields (FR-002)

| Step | Action                           | Expected Result                    | Pass |
| ---- | -------------------------------- | ---------------------------------- | ---- |
| 1    | With connected analyzer selected | Connection test passed             | ☐    |
| 2    | Click "Query Analyzer" button    | Progress indicator appears         | ☐    |
| 3    | Wait for query completion        | Fields populate in list            | ☐    |
| 4    | Verify field count > 0           | Fields visible in UI               | ☐    |
| 5    | Check field properties display   | Name, type, unit shown             | ☐    |
| 6    | Verify field type indicators     | NUMERIC, QUALITATIVE, TEXT visible | ☐    |

**Expected Fields** (from mock server `tools/astm-mock-server/fields.json`):

- [ ] WBC (NUMERIC, 10^3/μL) - White Blood Cell Count
- [ ] RBC (NUMERIC, 10^6/μL) - Red Blood Cell Count
- [ ] HGB (NUMERIC, g/dL) - Hemoglobin
- [ ] HCT (NUMERIC, %) - Hematocrit
- [ ] MCV (NUMERIC, fL) - Mean Corpuscular Volume
- [ ] MCH (NUMERIC, pg) - Mean Corpuscular Hemoglobin
- [ ] MCHC (NUMERIC, g/dL) - Mean Corpuscular Hemoglobin Concentration
- [ ] PLT (NUMERIC, 10^3/μL) - Platelet Count
- [ ] RDW (NUMERIC, %) - Red Cell Distribution Width
- [ ] MPV (NUMERIC, fL) - Mean Platelet Volume

**Total**: 10 HEMATOLOGY fields should be returned from mock server.

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 1.4: Create Field Mapping (FR-003)

| Step | Action                               | Expected Result                | Pass |
| ---- | ------------------------------------ | ------------------------------ | ---- |
| 1    | Select a queried field (e.g., WBC)   | Field selection highlighted    | ☐    |
| 2    | Click "Create Mapping" or equivalent | Mapping form opens             | ☐    |
| 3    | Search for OpenELIS target: "WBC"    | Search results appear          | ☐    |
| 4    | Select target test/result            | Target associated with mapping | ☐    |
| 5    | Set as required: Yes                 | Required checkbox checked      | ☐    |
| 6    | Click "Save"                         | Mapping saved successfully     | ☐    |
| 7    | Verify mapping appears inactive      | is_active = false (draft)      | ☐    |
| 8    | Check "Draft" badge visible          | UI indicates draft status      | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 1.5: Unit Mapping with Conversion (FR-004)

| Step | Action                                 | Expected Result               | Pass |
| ---- | -------------------------------------- | ----------------------------- | ---- |
| 1    | Select Glucose field                   | Field selected                | ☐    |
| 2    | Create mapping to OpenELIS Glucose     | Mapping form opens            | ☐    |
| 3    | Note analyzer unit: "mg/dL"            | Source unit displayed         | ☐    |
| 4    | Select OpenELIS unit: "mmol/L"         | Different unit selected       | ☐    |
| 5    | Verify conversion factor field appears | Factor input visible          | ☐    |
| 6    | Enter conversion factor: "0.0555"      | Factor accepted               | ☐    |
| 7    | Save mapping                           | Mapping saved with conversion | ☐    |
| 8    | Verify in database                     | `unit_mapping` record created | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 1.6: Qualitative Result Mapping (FR-005)

| Step | Action                                     | Expected Result                            | Pass |
| ---- | ------------------------------------------ | ------------------------------------------ | ---- |
| 1    | Use Immunology analyzer (port 5002)        | Immunology analyzer connected              | ☐    |
| 2    | Query fields - select HIV field            | HIV qualitative field selected             | ☐    |
| 3    | Create mapping to OpenELIS HIV test        | Mapping form opens                         | ☐    |
| 4    | Add value mapping: "POS" → "POSITIVE"      | First value pair added                     | ☐    |
| 5    | Add value mapping: "POSITIVE" → "POSITIVE" | Second value pair added                    | ☐    |
| 6    | Add value mapping: "REACTIVE" → "POSITIVE" | Third value pair added                     | ☐    |
| 7    | Add value mapping: "NEG" → "NEGATIVE"      | Fourth value pair added                    | ☐    |
| 8    | Set default for unmapped: "INDETERMINATE"  | Default value set, is_default=true         | ☐    |
| 9    | Save all mappings                          | Mappings saved                             | ☐    |
| 10   | Verify in database                         | `qualitative_result_mapping` records exist | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 1.7: Test Mapping Preview (FR-007)

| Step | Action                                | Expected Result                  | Pass |
| ---- | ------------------------------------- | -------------------------------- | ---- |
| 1    | Navigate to mapping test/preview area | Preview section visible          | ☐    |
| 2    | Load sample ASTM message file         | `hematology-cbc.astm` content    | ☐    |
| 3    | Click "Preview" or "Test Mapping"     | Processing begins                | ☐    |
| 4    | Verify parsed fields displayed        | Field names and values shown     | ☐    |
| 5    | Check mapped values preview           | OpenELIS target values displayed | ☐    |
| 6    | Verify unit conversions applied       | Converted values shown           | ☐    |
| 7    | Check for unmapped field warnings     | Any unmapped fields highlighted  | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 1.8: Activate Mappings (FR-010)

| Step | Action                         | Expected Result                    | Pass |
| ---- | ------------------------------ | ---------------------------------- | ---- |
| 1    | With draft mappings created    | Multiple draft mappings exist      | ☐    |
| 2    | Click "Save and Activate"      | Confirmation dialog appears        | ☐    |
| 3    | Review confirmation message    | Lists mappings to be activated     | ☐    |
| 4    | Confirm activation             | Mappings activated                 | ☐    |
| 5    | Verify status change           | is_active = true for all           | ☐    |
| 6    | Check analyzer status → ACTIVE | Analyzer configuration updated     | ☐    |
| 7    | Verify audit trail             | sys_user_id, lastupdated populated | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

---

## User Story 2: Maintain Existing Mappings (P2)

**Reference**: spec.md lines 52-77, FR-009, FR-010, FR-013

### Test Case 2.1: Update Existing Mapping (FR-010)

| Step | Action                               | Expected Result                 | Pass |
| ---- | ------------------------------------ | ------------------------------- | ---- |
| 1    | Select analyzer with active mappings | Active mappings visible         | ☐    |
| 2    | Click on a mapping to edit           | Edit form/modal opens           | ☐    |
| 3    | Change target test                   | New target selected             | ☐    |
| 4    | Click "Save as Draft"                | Changes saved as draft          | ☐    |
| 5    | Verify original mapping still active | is_active=true on original      | ☐    |
| 6    | Verify new draft version created     | New record with is_active=false | ☐    |
| 7    | Check "Draft" badge appears          | UI shows draft indicator        | ☐    |
| 8    | Activate new version                 | New version becomes active      | ☐    |
| 9    | Verify old version deactivated       | Previous is_active=false        | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 2.2: Copy Mappings Between Analyzers (FR-006)

| Step | Action                           | Expected Result               | Pass |
| ---- | -------------------------------- | ----------------------------- | ---- |
| 1    | Navigate to target analyzer      | Analyzer without mappings     | ☐    |
| 2    | Click "Copy Mappings"            | Source analyzer selection     | ☐    |
| 3    | Select source analyzer           | Source with mappings selected | ☐    |
| 4    | Select mappings to copy          | Checkbox selection available  | ☐    |
| 5    | Click "Copy"                     | Copy operation executes       | ☐    |
| 6    | Verify mappings appear on target | New mappings in list          | ☐    |
| 7    | Check copied as drafts           | is_active=false on copies     | ☐    |
| 8    | Verify original unchanged        | Source mappings intact        | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 2.3: Retire Mapping (FR-013)

| Step | Action                            | Expected Result             | Pass |
| ---- | --------------------------------- | --------------------------- | ---- |
| 1    | Select an active mapping          | Mapping selected            | ☐    |
| 2    | Click "Retire" or equivalent      | Retirement dialog opens     | ☐    |
| 3    | Enter retirement reason           | Reason text required        | ☐    |
| 4    | Confirm retirement                | Mapping retired             | ☐    |
| 5    | Verify "Retired" badge appears    | UI shows retired status     | ☐    |
| 6    | Check mapping no longer processes | Data flows to error instead | ☐    |
| 7    | Verify audit trail updated        | Retirement reason saved     | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 2.4: Audit Trail Verification (FR-009)

| Step | Action                     | Expected Result       | Pass |
| ---- | -------------------------- | --------------------- | ---- |
| 1    | Create new mapping         | Mapping created       | ☐    |
| 2    | Check database record      | sys_user_id populated | ☐    |
| 3    | Check lastupdated          | Timestamp set         | ☐    |
| 4    | Edit mapping               | Mapping updated       | ☐    |
| 5    | Verify lastupdated changed | New timestamp         | ☐    |
| 6    | Verify user ID tracked     | Correct user ID       | ☐    |

**Verification Query**:

```sql
SELECT id, sys_user_id, last_updated
FROM analyzer_field_mapping
WHERE analyzer_id = [test_analyzer_id]
ORDER BY last_updated DESC;
```

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

---

## User Story 3: Resolve Unmapped Messages (P3)

**Reference**: spec.md lines 80-109, FR-011, FR-016, FR-017

### Test Case 3.1: Error Dashboard Overview (FR-016)

| Step | Action                                         | Expected Result                   | Pass |
| ---- | ---------------------------------------------- | --------------------------------- | ---- |
| 1    | Navigate to `/analyzers/errors`                | Error dashboard loads             | ☐    |
| 2    | Verify statistics cards visible                | Error counts by severity          | ☐    |
| 3    | Check error table displays                     | Error list with columns           | ☐    |
| 4    | Verify columns: Type, Severity, Analyzer, Time | All columns present               | ☐    |
| 5    | Check filtering controls                       | Filter by type/severity available | ☐    |
| 6    | Verify pagination works                        | Can navigate pages if >10 errors  | ☐    |
| 7    | Check sorting functionality                    | Can sort by columns               | ☐    |

**Expected Error Counts** (from test data):

- [ ] MAPPING errors: ≥5
- [ ] VALIDATION warnings: ≥3
- [ ] CONNECTION errors: ≥2
- [ ] PROTOCOL errors: ≥1

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 3.2: View Error Details (FR-016)

| Step | Action                           | Expected Result                   | Pass |
| ---- | -------------------------------- | --------------------------------- | ---- |
| 1    | Click on an error row            | Error detail modal opens          | ☐    |
| 2    | Verify error message visible     | Full error text shown             | ☐    |
| 3    | Check raw ASTM message displayed | Raw message in code block         | ☐    |
| 4    | Verify timestamp visible         | Error timestamp shown             | ☐    |
| 5    | Check analyzer information       | Analyzer name/ID shown            | ☐    |
| 6    | Verify action buttons present    | Create Mapping, Acknowledge, etc. | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 3.3: Create Mapping from Error (FR-011)

| Step | Action                         | Expected Result        | Pass |
| ---- | ------------------------------ | ---------------------- | ---- |
| 1    | Open error with unmapped field | e.g., ERROR-001 (MCHC) | ☐    |
| 2    | Click "Create Mapping" button  | Mapping form opens     | ☐    |
| 3    | Verify field pre-populated     | MCHC field auto-filled | ☐    |
| 4    | Verify analyzer pre-selected   | Correct analyzer shown | ☐    |
| 5    | Select OpenELIS target         | Target test selected   | ☐    |
| 6    | Save mapping                   | Mapping created        | ☐    |
| 7    | Verify mapping appears in list | New mapping visible    | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 3.4: Acknowledge Error

| Step | Action                          | Expected Result       | Pass |
| ---- | ------------------------------- | --------------------- | ---- |
| 1    | Select an unacknowledged error  | UNACKNOWLEDGED status | ☐    |
| 2    | Click "Acknowledge" button      | Confirmation prompt   | ☐    |
| 3    | Confirm acknowledgment          | Error acknowledged    | ☐    |
| 4    | Verify status → ACKNOWLEDGED    | Status updated in UI  | ☐    |
| 5    | Check acknowledged_by populated | User ID recorded      | ☐    |
| 6    | Check acknowledged_at timestamp | Time recorded         | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 3.5: Reprocess After Mapping (FR-017)

| Step | Action                            | Expected Result            | Pass |
| ---- | --------------------------------- | -------------------------- | ---- |
| 1    | Create mapping for unmapped field | Mapping active             | ☐    |
| 2    | Return to error dashboard         | Error still visible        | ☐    |
| 3    | Select related error              | Error with raw_message     | ☐    |
| 4    | Click "Reprocess" button          | Reprocessing begins        | ☐    |
| 5    | Observe processing status         | Progress indicator         | ☐    |
| 6    | Verify success message            | "Reprocessed successfully" | ☐    |
| 7    | Check error status → RESOLVED     | Status updated             | ☐    |
| 8    | Verify result created in OpenELIS | Result data present        | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

---

## QC Processing Tests (FR-021)

### Test Case 4.1: Q-Segment Parse

| Step | Action                          | Expected Result            | Pass |
| ---- | ------------------------------- | -------------------------- | ---- |
| 1    | Load `qc-control-results.astm`  | QC message content         | ☐    |
| 2    | Process through mapping preview | Q-segments parsed          | ☐    |
| 3    | Verify QC fields extracted      | Control lot, level visible | ☐    |
| 4    | Check QC values displayed       | Value and unit shown       | ☐    |
| 5    | Verify timestamp parsed         | QC timestamp correct       | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 4.2: QC Error Generation

| Step | Action                              | Expected Result             | Pass |
| ---- | ----------------------------------- | --------------------------- | ---- |
| 1    | Send message with unmapped QC field | Q-segment for unmapped test | ☐    |
| 2    | Navigate to error dashboard         | Dashboard loads             | ☐    |
| 3    | Locate QC-related error             | QC error visible            | ☐    |
| 4    | Verify error type indicates QC      | QC mapping error shown      | ☐    |
| 5    | Check raw Q-segment in details      | Q-segment visible           | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

---

## Edge Cases and Error Scenarios

### Test Case 5.1: Malformed ASTM Message

| Step | Action                           | Expected Result          | Pass |
| ---- | -------------------------------- | ------------------------ | ---- |
| 1    | Load `malformed.astm` samples    | Invalid message content  | ☐    |
| 2    | Process through system           | Error handling triggered | ☐    |
| 3    | Verify PROTOCOL error created    | Error in dashboard       | ☐    |
| 4    | Check graceful degradation       | No system crash          | ☐    |
| 5    | Verify error message descriptive | Helpful error text       | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 5.2: Unit Mismatch Handling

| Step | Action                                    | Expected Result           | Pass |
| ---- | ----------------------------------------- | ------------------------- | ---- |
| 1    | Load `unit-mismatch.astm`                 | SI unit values            | ☐    |
| 2    | Process with existing mapping             | Mapping applied           | ☐    |
| 3    | If no unit mapping: verify error          | VALIDATION warning        | ☐    |
| 4    | If unit mapping exists: verify conversion | Value converted correctly | ☐    |

**Conversion Verification** (example):

- Input: Glucose 5.4 mmol/L
- Expected: ~97 mg/dL (factor: 18.0182)

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

### Test Case 5.3: Connection Timeout

| Step | Action                                 | Expected Result         | Pass |
| ---- | -------------------------------------- | ----------------------- | ---- |
| 1    | Configure analyzer with unreachable IP | e.g., 192.168.99.99     | ☐    |
| 2    | Click "Test Connection"                | Test begins             | ☐    |
| 3    | Wait for timeout (30 seconds max)      | Timeout message appears | ☐    |
| 4    | Verify CONNECTION error logged         | Error in dashboard      | ☐    |
| 5    | Check analyzer status unchanged        | Still in SETUP          | ☐    |

**Notes**: \***\*\*\*\*\***\*\*\***\*\*\*\*\***\_\_\_\***\*\*\*\*\***\*\*\***\*\*\*\*\***

---

## Post-Test Verification

### Database Consistency Check

```sql
-- Verify no orphaned mappings
SELECT m.id FROM analyzer_field_mapping m
LEFT JOIN analyzer a ON m.analyzer_id = a.id
WHERE a.id IS NULL;

-- Verify no orphaned errors
SELECT e.id FROM analyzer_error e
LEFT JOIN analyzer a ON e.analyzer_id = a.id
WHERE a.id IS NULL;

-- Check audit trail completeness
SELECT id FROM analyzer_field_mapping
WHERE sys_user_id IS NULL OR last_updated IS NULL;
```

### Browser Console Review

- [ ] No JavaScript errors in console
- [ ] No failed API requests (4xx/5xx)
- [ ] No CORS issues
- [ ] No unhandled promise rejections

---

## Test Summary

| Category                | Total Tests | Passed | Failed | Blocked |
| ----------------------- | ----------- | ------ | ------ | ------- |
| US1: Configure Mappings | 8           | ☐      | ☐      | ☐       |
| US2: Maintain Mappings  | 4           | ☐      | ☐      | ☐       |
| US3: Resolve Errors     | 5           | ☐      | ☐      | ☐       |
| QC Processing           | 2           | ☐      | ☐      | ☐       |
| Edge Cases              | 3           | ☐      | ☐      | ☐       |
| **TOTAL**               | **22**      | ☐      | ☐      | ☐       |

**Tester**: \***\*\*\*\*\***\_\_\_\***\*\*\*\*\***  
**Date**: \***\*\*\*\*\***\_\_\_\***\*\*\*\*\***  
**Environment**: \***\*\*\*\*\***\_\_\_\***\*\*\*\*\***  
**Build Version**: \***\*\*\*\*\***\_\_\_\***\*\*\*\*\***

---

## Appendix

### Sample ASTM Message Files

Located in: `src/test/resources/astm-samples/`

| File                        | Purpose                   | Use With           |
| --------------------------- | ------------------------- | ------------------ |
| `hematology-cbc.astm`       | Complete CBC results      | Test Case 1.3, 1.7 |
| `chemistry-panel.astm`      | Chemistry with units      | Test Case 1.5      |
| `immunology-screening.astm` | Qualitative results       | Test Case 1.6      |
| `qc-control-results.astm`   | QC Q-segments             | Test Case 4.1, 4.2 |
| `unmapped-fields.astm`      | Unknown test codes        | Test Case 3.3      |
| `unit-mismatch.astm`        | Unit conversion scenarios | Test Case 5.2      |
| `malformed.astm`            | Invalid format            | Test Case 5.1      |

### Mock Server Testing Tools

**Test Communication Script:**

```bash
cd tools/astm-mock-server
python3 test_communication.py
```

- Tests ENQ/ACK handshake
- Demonstrates complete ASTM message flow
- Verifies QC segment handling
- Tests multiple simultaneous connections

**Documentation:**

- `tools/astm-mock-server/COMMUNICATION_PATHWAY.md` - Protocol details
- `tools/astm-mock-server/ACCESS.md` - Access guide for OpenELIS integration
- `tools/astm-mock-server/README.md` - Server setup and usage
