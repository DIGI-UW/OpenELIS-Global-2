# Real GeneXpert ASTM Validation Checklist

**Feature**: 012 — Generic ASTM Plugin Profiles v1.2  
**Gate**: Bidirectional GeneXpert ASTM MVP  
**Reference**: GeneXpert Rev-E LIS Protocol Specification
(`docs/analyzers/012/`)  
**Status**: Template (execute before MVP sign-off)

---

## Preconditions

All preconditions MUST be verified before executing any pathway gate.

### Infrastructure

- [ ] **Bridge reachable**: `curl -sf https://bridge-host:port/health` returns
      200
  ```bash
  curl -sf https://${BRIDGE_HOST}:${BRIDGE_PORT}/health
  ```
- [ ] **OpenELIS running**: `curl -sf https://localhost/api/health` returns 200
- [ ] **Mock server healthy** (for mock gates):
      `curl -sf http://localhost:8085/health`
- [ ] **GeneXpert device powered on and connected** (for real gates):
  - Device IP: `____________`
  - Device port: `____________`
  - Device serial: `____________`
  - LIS mode enabled in device settings

### OpenELIS Configuration

- [ ] **Analyzer created in OpenELIS** pointing to real GeneXpert IP/port
  - Analyzer ID: `____________`
  - Name: `____________`
  - Protocol: Generic ASTM
  - Connection role: `SERVER` / `CLIENT` (circle one)
- [ ] **Test mappings exist** (from built-in GeneXpert profile) for at least one
      test
  - Mapped test: `____________` → OpenELIS test: `____________`
- [ ] **QC rules configured** (at least one active QC identification rule)
- [ ] **Analyzer status**: `ACTIVE`

### Test Data

- [ ] **Sample with pending analyses exists** in OpenELIS
  - Accession number: `____________`
  - Test(s) ordered: `____________`
  - Patient: `____________` (or test patient)

---

## Gate 1: Results PUSH (Analyzer → OpenELIS)

**Pathway**: GeneXpert runs a test and pushes results to OpenELIS via ASTM.

### Mock Validation

- [ ] Run results-push harness script:
  ```bash
  cd projects/analyzer-harness && ./scripts/test-genexpert-astm.sh 1 bridge
  ```
- [ ] **Expected**: Script exits 0, output shows "Push complete"
- [ ] **Verify**: Check analyzer import queue shows new result
  ```bash
  curl -sf https://localhost/api/AnalyzerResults | python3 -m json.tool
  ```

### Real Device Validation

- [ ] **Action**: Run a test cartridge on the real GeneXpert device
  - Cartridge type: `____________`
  - Module used: `____________`
- [ ] **Expected**: Device sends ASTM result message to OpenELIS
- [ ] **Evidence — OpenELIS logs** (bridge-routed message received):
  ```bash
  docker logs oe.openelis.org 2>&1 | grep -i "ASTM.*process\|analyzer.*import" | tail -20
  ```
  Paste relevant log lines:
  ```
  [paste here]
  ```
- [ ] **Evidence — Bridge logs** (TCP relay succeeded):
  ```bash
  docker logs openelis-analyzer-bridge 2>&1 | grep -i "forward\|relay\|connect" | tail -20
  ```
  Paste relevant log lines:
  ```
  [paste here]
  ```
- [ ] **Evidence — OpenELIS UI**: Result appears in Analyzer Results queue
  - Result value: `____________`
  - Test name: `____________`
  - Timestamp: `____________`
- [ ] **PASS** / **FAIL** (circle one)

---

## Gate 2: Orders PULL (Analyzer → OpenELIS query → OpenELIS responds with orders)

**Pathway**: GeneXpert queries OpenELIS for pending orders via ASTM Q-segment.
OpenELIS responds with H/P/O/L message containing pending analyses.

### Mock Validation

- [ ] Run orders-pull harness script:
  ```bash
  cd projects/analyzer-harness && ./scripts/test-genexpert-orders-pull.sh
  ```
- [ ] **Expected**: Script exits 0, output shows response contains O (order)
      segments
- [ ] **Verify**: Response includes correct test codes mapped from
      `analyzer_test_map`

### Real Device Validation

- [ ] **Precondition**: Sample with pending analyses exists (accession:
      `____________`)
- [ ] **Action**: Trigger "Host Query" on GeneXpert device for the sample
  - Navigation: GeneXpert > Create Test > Host Query (or equivalent per device
    model)
  - Query sample ID: `____________`
- [ ] **Expected**: Device sends Q-segment query; OpenELIS responds with orders
- [ ] **Evidence — OpenELIS logs** (Q-segment received, response sent):
  ```bash
  docker logs oe.openelis.org 2>&1 | grep -i "Q.segment\|query\|respond\|order" | tail -20
  ```
  Paste relevant log lines:
  ```
  [paste here]
  ```
- [ ] **Evidence — Bridge logs** (bidirectional relay):
  ```bash
  docker logs openelis-analyzer-bridge 2>&1 | tail -20
  ```
  Paste relevant log lines:
  ```
  [paste here]
  ```
- [ ] **Evidence — Device-side**: GeneXpert shows received orders / test list
  - Tests received: `____________`
  - Device confirmation (screenshot or description): `____________`
- [ ] **PASS** / **FAIL** (circle one)

---

## Gate 3: Orders PUSH (OpenELIS → Analyzer)

**Pathway**: OpenELIS constructs ASTM order message and sends to GeneXpert via
bridge.

### Mock Validation

- [ ] Run orders-push harness script:
  ```bash
  cd projects/analyzer-harness && ./scripts/test-genexpert-orders-push.sh
  ```
- [ ] **Expected**: Script exits 0, mock server logs show received H/P/O/L
      message
- [ ] **Verify**: Mock server received correct accession number and test codes

### Real Device Validation

- [ ] **Precondition**: Sample with pending analyses exists (accession:
      `____________`)
- [ ] **Action**: Send order via API:
  ```bash
  curl -sf -X POST "https://localhost/rest/analyzer/${ANALYZER_ID}/send-order" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${TOKEN}" \
    -d '{"accessionNumber": "ACCESSION_HERE"}' | python3 -m json.tool
  ```
- [ ] **Expected**: API returns `201` with `success: true` and `orderCount > 0`
  ```json
  {
    "success": true,
    "message": "Order message sent successfully",
    "orderCount": 1
  }
  ```
- [ ] **Evidence — OpenELIS logs** (order message constructed and sent):
  ```bash
  docker logs oe.openelis.org 2>&1 | grep -i "send.*order\|ASTM.*message\|bridge.*post" | tail -20
  ```
  Paste relevant log lines:
  ```
  [paste here]
  ```
- [ ] **Evidence — Bridge logs** (message relayed to device):
  ```bash
  docker logs openelis-analyzer-bridge 2>&1 | tail -20
  ```
  Paste relevant log lines:
  ```
  [paste here]
  ```
- [ ] **Evidence — Device-side**: GeneXpert acknowledges receipt or shows order
  - Device confirmation: `____________`
- [ ] **PASS** / **FAIL** (circle one)

---

## Gate 4: Results PULL (OpenELIS → Analyzer query → Analyzer responds with results)

**Pathway**: OpenELIS constructs ASTM Q-segment query, sends to GeneXpert via
bridge, captures response, and feeds into standard ASTM ingest pipeline.

### Mock Validation

- [ ] Run results-pull harness script:
  ```bash
  cd projects/analyzer-harness && ./scripts/test-genexpert-results-pull.sh
  ```
- [ ] **Expected**: Script exits 0, OpenELIS imports results from mock response
- [ ] **Verify**: Check analyzer import queue shows new result from pull

### Real Device Validation

- [ ] **Precondition**: GeneXpert has completed test results for the sample
      (accession: `____________`)
- [ ] **Action**: Query results via API:
  ```bash
  curl -sf -X POST "https://localhost/rest/analyzer/${ANALYZER_ID}/query-results" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${TOKEN}" \
    -d '{"accessionNumber": "ACCESSION_HERE"}' | python3 -m json.tool
  ```
- [ ] **Expected**: API returns `200` with `importedResultCount > 0`
  ```json
  {
    "success": true,
    "message": "Results queried and imported successfully",
    "importedResultCount": 1
  }
  ```
- [ ] **Evidence — OpenELIS logs** (Q-segment sent, response received, results
      ingested):
  ```bash
  docker logs oe.openelis.org 2>&1 | grep -i "query.*result\|Q.segment\|import\|ingest" | tail -20
  ```
  Paste relevant log lines:
  ```
  [paste here]
  ```
- [ ] **Evidence — Bridge logs** (bidirectional relay):
  ```bash
  docker logs openelis-analyzer-bridge 2>&1 | tail -20
  ```
  Paste relevant log lines:
  ```
  [paste here]
  ```
- [ ] **Evidence — OpenELIS UI**: Pulled result appears in Analyzer Results
      queue
  - Result value: `____________`
  - Test name: `____________`
  - Timestamp: `____________`
- [ ] **PASS** / **FAIL** (circle one)

---

## Summary

| Gate | Pathway                            | Mock     | Real Device |
| ---- | ---------------------------------- | -------- | ----------- |
| 1    | Results PUSH (Analyzer → OE)       | [ ] Pass | [ ] Pass    |
| 2    | Orders PULL (Analyzer queries OE)  | [ ] Pass | [ ] Pass    |
| 3    | Orders PUSH (OE → Analyzer)        | [ ] Pass | [ ] Pass    |
| 4    | Results PULL (OE queries Analyzer) | [ ] Pass | [ ] Pass    |

### Sign-off

- [ ] All 4 mock gates pass
- [ ] All 4 real device gates pass
- [ ] Evidence documented for each real device gate
- [ ] No regressions in existing test-connection or results-push flows

**Validated by**: `____________`  
**Date**: `____________`  
**GeneXpert model/serial**: `____________`  
**OpenELIS version**: `____________`  
**Bridge version**: `____________`
