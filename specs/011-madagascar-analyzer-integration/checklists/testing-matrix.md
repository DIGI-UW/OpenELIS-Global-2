# Analyzer Testing Matrix

**Version:** 1.0.0  
**Date:** 2026-02-02  
**Feature:** 011-madagascar-analyzer-integration

## Purpose

This document defines the testing coverage for each supported analyzer,
specifying which testing modes are supported and the validation approach for
each protocol/transport combination.

---

## Testing Matrix

| Analyzer ID | Analyzer Name        | Protocol      | Transport    | Manual Test | Integration Test | E2E Test (Cypress) | Notes                                      |
| ----------- | -------------------- | ------------- | ------------ | ----------- | ---------------- | ------------------ | ------------------------------------------ |
| 2000        | Abbott Architect     | HL7 v2.5.1    | TCP/IP       | ✅ Yes      | ✅ Yes           | ✅ Yes             | HL7 ORU^R01 via mock server                |
| 2002        | Cepheid GeneXpert    | FILE          | Filesystem   | ✅ Yes      | ✅ Yes           | ✅ Yes             | CSV file import via mock generator         |
| 2003        | Hain FluoroCycler XT | FILE          | Filesystem   | ✅ Yes      | ✅ Yes           | ✅ Yes             | CSV file import via mock generator         |
| 2004        | Horiba ABX Micros 60 | ASTM LIS2-A2  | RS232 Serial | ✅ Yes      | ✅ Yes           | ⚠️ Limited         | Requires ASTM-HTTP Bridge with RS232       |
| 2005        | Horiba ABX Pentra 60 | ASTM LIS2-A2  | RS232 Serial | ✅ Yes      | ✅ Yes           | ⚠️ Limited         | Requires ASTM-HTTP Bridge with RS232       |
| 2006        | Mindray BA-88A       | ASTM          | RS232 Serial | ✅ Yes      | ✅ Yes           | ⚠️ Limited         | Requires ASTM-HTTP Bridge with RS232       |
| 2007        | Mindray BC-5380      | HL7 v2.3.1    | TCP/IP       | ✅ Yes      | ✅ Yes           | ✅ Yes             | HL7 ORU^R01 via mock server (M5 validated) |
| 2008        | Mindray BS-360E      | HL7 v2.3.1    | TCP/IP       | ✅ Yes      | ✅ Yes           | ✅ Yes             | HL7 ORU^R01 via mock server (M5 validated) |
| 2009        | QuantStudio 7 Flex   | FILE          | Filesystem   | ✅ Yes      | ✅ Yes           | ✅ Yes             | CSV file import via mock generator         |
| 2010        | Stago STart 4        | ASTM LIS2-A2  | RS232 Serial | ✅ Yes      | ✅ Yes           | ⚠️ Limited         | Requires ASTM-HTTP Bridge with RS232       |
| 2011        | Sysmex XN Series     | ASTM E1381-02 | TCP/IP       | ✅ Yes      | ✅ Yes           | ✅ Yes             | ASTM over TCP via mock server              |

**Legend:**

- ✅ **Yes**: Fully supported with automated test coverage
- ⚠️ **Limited**: Supported but requires additional infrastructure (RS232
  bridge, virtual serial)
- ❌ **No**: Not supported in current test configuration

---

## Testing Modes Explained

### 1. Manual Testing

**Purpose:** Human-driven validation during development

**Setup:**

```bash
# Start OpenELIS + mock server
docker compose -f dev.docker-compose.yml -f docker-compose.analyzer-test.yml up -d

# Load Feature 011 fixtures
./src/test/resources/load-analyzer-test-data.sh --dataset-011

# Navigate to Analyzer Dashboard
# URL: https://localhost/AnalyzerDashboard
```

**Validation:**

- Verify all 12 analyzers appear in dashboard
- Check analyzer details (name, type, status)
- Verify serial/file configurations display correctly

**Supported:** All 12 analyzers

---

### 2. Integration Testing (Backend)

**Purpose:** Automated backend validation using JUnit 4 + Spring Test

**Test Type:** `BaseWebContextSensitiveTest`

**Coverage:**

- DBUnit XML parsing
- Fixture loading via loader script
- Database queries to verify analyzer rows
- Serial/file configuration validation

**Example:**

```java
@Test
public void testLoadMadagascarFixtures_AllAnalyzersPresent() {
    // Load fixtures
    executeDataSetWithStateManagement("testdata/madagascar-analyzer-test-data.xml");

    // Verify analyzer count
    String hql = "SELECT COUNT(*) FROM Analyzer WHERE id BETWEEN 2000 AND 2011";
    Long count = (Long) session.createQuery(hql).uniqueResult();
    assertEquals("Should have 11 analyzers", 11L, count.longValue());
}
```

**Supported:** All 12 analyzers

---

### 3. E2E Testing (Cypress)

**Purpose:** End-to-end user workflow validation

**Setup:**

```javascript
describe("Analyzer Dashboard - Madagascar Analyzers", () => {
  before(() => {
    cy.login("admin", "password");
    cy.loadMadagascarAnalyzerFixtures();
  });

  it("should display all 12 Madagascar analyzers", () => {
    cy.visit("/AnalyzerDashboard");

    // Verify HL7 analyzers
    cy.contains("Abbott Architect").should("be.visible");
    cy.contains("Mindray BC-5380").should("be.visible");

    // Verify FILE analyzers
    cy.contains("Cepheid GeneXpert").should("be.visible");
    cy.contains("QuantStudio 7 Flex").should("be.visible");

    // Verify RS232 analyzers (with serial config indicator)
    cy.contains("Horiba ABX Pentra 60").should("be.visible");
    cy.contains("Stago STart 4").should("be.visible");
  });
});
```

**Limitations:**

- RS232 analyzers have ⚠️ **Limited** E2E support (dashboard display only)
- Full RS232 message flow requires virtual serial ports (not practical in
  Cypress)
- Integration tests provide deeper RS232 validation

**Fully Supported (✅):**

- HL7 analyzers: Abbott Architect, Mindray BC-5380/BS-360E, Sysmex XN
- FILE analyzers: GeneXpert, FluoroCycler XT, QuantStudio 7

**Limited Support (⚠️):**

- RS232 analyzers: Horiba Micros60/Pentra60, Mindray BA-88A, Stago STart4

---

## Protocol-Specific Testing Approaches

### HL7 over TCP/IP (5 analyzers)

**Mock Server Command:**

```bash
python server.py --hl7 --push https://localhost:8443 --hl7-template mindray_bc5380
```

**Testing Coverage:**

- Message generation (ORU^R01)
- MLLP framing validation
- MSH sender identification
- Result parsing and storage
- Field mapping validation

**E2E Support:** ✅ Full (mock server pushes via HTTP)

---

### ASTM over RS232 (5 analyzers)

**Mock Server Command:**

```bash
# Create virtual serial pair
socat -d -d pty,raw,echo=0 pty,raw,echo=0
# Output: /dev/pts/X and /dev/pts/Y

# Send ASTM over serial
python server.py --serial-port /dev/pts/X --serial-analyzer horiba_pentra60
```

**Testing Coverage:**

- ENQ/ACK handshake
- ASTM frame structure
- Checksum validation
- Serial port configuration
- ASTM-HTTP Bridge integration

**E2E Support:** ⚠️ Limited (dashboard display only; full message flow in
integration tests)

**Why Limited:**

- Virtual serial port setup adds complexity to E2E environment
- ASTM-HTTP Bridge required as intermediary
- Integration tests provide sufficient coverage for protocol validation

---

### FILE-based Import (3 analyzers)

**Mock Server Command:**

```bash
python server.py --generate-files /tmp/import --generate-files-analyzer genexpert
```

**Testing Coverage:**

- File generation (CSV format)
- Directory watching (60-second detection)
- CSV parsing and field extraction
- File archival and error handling

**E2E Support:** ✅ Full (generate file + verify import)

---

## Demo Path Documentation

### Quick Demo Setup (All Protocols)

```bash
# 1. Start environment
docker compose -f dev.docker-compose.yml -f docker-compose.analyzer-test.yml up -d

# 2. Load Feature 011 fixtures
./src/test/resources/load-analyzer-test-data.sh --dataset-011

# 3. Access Analyzer Dashboard
# URL: https://localhost/AnalyzerDashboard

# 4. Trigger mock messages
# HL7 (Mindray BC-5380)
python tools/analyzer-mock-server/server.py --hl7 --push https://localhost:8443 --hl7-template mindray_bc5380

# FILE (GeneXpert)
python tools/analyzer-mock-server/server.py --generate-files /tmp/genexpert --generate-files-analyzer genexpert

# RS232 (Horiba Pentra 60) - requires bridge + virtual serial
socat -d -d pty,raw,echo=0 pty,raw,echo=0
python tools/analyzer-mock-server/server.py --serial-port /dev/pts/X --serial-analyzer horiba_pentra60
```

---

## Testing Priority by Protocol

### Priority 1: HL7 + FILE (8 analyzers) - Full E2E

These analyzers have the simplest testing path and should be prioritized for E2E
coverage:

1. Abbott Architect (HL7)
2. Mindray BC-5380 (HL7)
3. Mindray BS-360E (HL7)
4. Sysmex XN (ASTM/TCP - similar to HL7 complexity)
5. GeneXpert (FILE)
6. FluoroCycler XT (FILE)
7. QuantStudio 7 (FILE)

**Rationale:** No additional infrastructure required beyond mock server

---

### Priority 2: RS232 (5 analyzers) - Integration Tests

RS232 analyzers require ASTM-HTTP Bridge and virtual serial ports. Prioritize
integration test coverage:

1. Horiba Pentra 60
2. Horiba Micros 60
3. Mindray BA-88A
4. Stago STart 4

**Rationale:** Virtual serial setup adds complexity; integration tests provide
sufficient protocol validation

---

## Test Execution Commands

### Run Integration Tests

```bash
# Backend integration tests (includes DBUnit validation)
mvn test -Dtest="*AnalyzerTest"

# Verify fixtures load without errors
./src/test/resources/load-analyzer-test-data.sh --dataset-011 --no-verify
```

### Run E2E Tests

```bash
cd frontend

# Run analyzer dashboard tests (if they exist)
npm run cy:spec "cypress/e2e/AnalyzerDashboard.cy.js"

# Run with fail-fast (before pushing)
npm run cy:failfast
```

### Manual Verification

```bash
# 1. Load fixtures
./src/test/resources/load-analyzer-test-data.sh --dataset-011

# 2. Query database directly
docker exec openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT id, name, analyzer_type, description
FROM analyzer
WHERE id BETWEEN 2000 AND 2011
ORDER BY id;
"

# Expected output: 11 rows (IDs 2000, 2002-2011)
```

---

## Related Documentation

- **Supported Analyzers Contract:** `contracts/supported-analyzers.md`
- **Template-Fixture Mapping:** `contracts/template-fixture-mapping.md`
- **Testing Roadmap:** `.specify/guides/testing-roadmap.md`
- **Cypress Best Practices:** `.specify/guides/cypress-best-practices.md`

---

**Maintained By:** OpenELIS Global Feature 011 Team  
**Last Updated:** 2026-02-02
