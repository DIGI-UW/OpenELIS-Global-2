# Manual Testing Guide: M9-M10 Horiba Analyzer Plugins

**Feature**: 011-madagascar-analyzer-integration **Milestones**: M9
(Pentra 60) + M10 (Micros 60) **Date**: 2026-01-29 **Environment**: Local
development (Docker containers)

---

## Prerequisites

### Environment Detection (IMPORTANT)

**Before testing, determine your environment type:**

```bash
# Check for subdomain deployment
docker exec openelisglobal-proxy env | grep LETSENCRYPT_DOMAIN

# Check .env file
cat .env | grep LETSENCRYPT_DOMAIN

# Test actual domain
curl -k -I https://$(docker exec openelisglobal-proxy env | grep LETSENCRYPT_DOMAIN | cut -d= -f2)/ | head -1
```

**Common environments**:

- **Localhost**: `https://localhost/` (development, self-signed cert)
- **Subdomain**: `https://analyzers.openelis-global.org/`,
  `https://storage.openelis-global.org/`, etc. (staging/production, Let's
  Encrypt cert)

**Set domain variable for commands below**:

```bash
# Auto-detect from Let's Encrypt config
export DOMAIN=$(docker exec openelisglobal-proxy env | grep LETSENCRYPT_DOMAIN | cut -d= -f2)

# Or set manually
export DOMAIN="analyzers.openelis-global.org"  # or "localhost" for local dev

# Verify
echo "Testing on: https://$DOMAIN/"
curl -k -I https://$DOMAIN/ | head -1
```

**For this guide**: All URLs use `${DOMAIN}` variable - set it before running
commands.

### Environment Status

✅ Docker containers running:

- `openelisglobal-webapp` (port 8080/8443)
- `openelisglobal-database` (port 15432)
- `openelisglobal-front-end`
- `openelisglobal-proxy` (port 80/443)
- `external-fhir-api` (port 8081/8444)

**Current deployment**: `analyzers.openelis-global.org` (detected from
LETSENCRYPT_DOMAIN)

### Required for Full Testing

- [ ] Build completed: `mvn clean install -DskipTests -Dmaven.test.skip=true`
- [ ] Plugin JARs built in `plugins/analyzers/Horiba{Pentra60,Micros60}/target/`
- [ ] ASTM-HTTP Bridge running (for RS232 testing) - **OR** use direct
      ASTM-over-TCP mock
- [ ] Test fixtures available in `src/test/resources/testdata/astm/`

---

## Testing Strategy

Since M9-M10 are **external plugin JARs**, testing requires:

1. **Unit-level**: Verify plugin code correctness (architecture, parsing,
   mappings)
2. **Integration-level**: Deploy plugins + send ASTM messages → verify import
3. **E2E-level**: Full workflow (configure analyzer → send results → validate
   UI)

**For this session**: Focus on unit-level verification + integration test
template

---

## Phase 1: Architecture Verification (Unit Level) ✅

### Test 1.1: Verify NO Spring Annotations (CRITICAL)

```bash
cd /home/ubuntu/OpenELIS-Global-2/plugins/analyzers

# Should return NOTHING (clean output)
grep -r "@Component\|@Service\|@PostConstruct\|@Autowired" Horiba*/src/main/java/
```

**Expected**: Empty output (no Spring annotations) ✅ **Why this matters**:
External plugins must NOT use Spring annotations. They register via `connect()`
method called by `PluginLoader`.

---

### Test 1.2: Verify External Plugin Pattern

```bash
# Check connect() method exists
grep -A5 "public boolean connect()" ./analyzers/HoribaPentra60/src/main/java/uw/edu/itech/HoribaPentra60/HoribaPentra60Analyzer.java

# Check PluginAnalyzerService registration
grep "addAnalyzerDatabaseParts" ./analyzers/HoribaPentra60/src/main/java/uw/edu/itech/HoribaPentra60/HoribaPentra60Analyzer.java
```

**Expected**:

```java
public boolean connect() {
  PluginAnalyzerService.getInstance()
    .addAnalyzerDatabaseParts(ANALYZER_NAME, ANALYZER_DESCRIPTION, testMappings, true);
  return true;
}
```

---

### Test 1.3: Verify Test Mappings

```bash
# Count test mappings (Pentra 60: 20 clinical + class constants = 23 total references)
grep -c "TestMapping" ./analyzers/HoribaPentra60/src/main/java/uw/edu/itech/HoribaPentra60/HoribaPentra60Analyzer.java

# Count test mappings (Micros 60: 16 clinical + class constants = 19 total references)
grep -c "TestMapping" ./analyzers/HoribaMicros60/src/main/java/uw/edu/itech/HoribaMicros60/HoribaMicros60Analyzer.java
```

**Expected**:

- Pentra 60: 23 ✅ (20 mappings in arrays + 3 class references)
- Micros 60: 19 ✅ (16 mappings in arrays + 3 class references)

**Test mapping breakdown**:

- **Pentra 60** (26 params total, 20 with LOINC):
  - 10 CBC parameters (WBC, RBC, HGB, HCT, MCV, MCH, MCHC, PLT, RDW-SD, RDW-CV)
  - 10 5-Part Differential (NEU%, LYM%, MONO%, EOS%, BASO%, NEU#, LYM#, MONO#,
    EOS#, BASO#)
- **Micros 60** (18 params total, 14 with LOINC):
  - 10 CBC parameters (same as Pentra)
  - 6 3-Part Differential (LYM%, MXD%, NEU%, LYM#, MXD#, NEU#)
  - **MXD (Mixed cells)**: NO LOINC → uses 2-arg TestMapping constructor

---

### Test 1.4: Verify LOINC Mapping Strategy

```bash
# Pentra 60: All should use 3-arg constructor (has LOINC)
grep "new TestMapping" ./analyzers/HoribaPentra60/src/main/java/uw/edu/itech/HoribaPentra60/HoribaPentra60Analyzer.java | grep -v "LOINC" && echo "⚠️  Found 2-arg mappings" || echo "✅ All 3-arg (with LOINC)"

# Micros 60: Should have 2 MXD mappings with 2-arg constructor (no LOINC)
grep "new TestMapping.*MXD" ./analyzers/HoribaMicros60/src/main/java/uw/edu/itech/HoribaMicros60/HoribaMicros60Analyzer.java
```

**Expected**:

- Pentra 60: All 3-arg constructor (all have LOINC) ✅
- Micros 60: MXD% and MXD# use 2-arg constructor (no LOINC) ✅

---

## Phase 2: Build Verification ✅

### Test 2.1: Clean Build

```bash
cd /home/ubuntu/OpenELIS-Global-2/plugins
mvn clean package -pl analyzers/HoribaPentra60,analyzers/HoribaMicros60
```

**Expected**: BUILD SUCCESS ✅

---

### Test 2.2: JAR Inspection

```bash
# Check JAR contents
jar tf analyzers/HoribaPentra60/target/HoribaPentra60-1.0.jar | grep -E "Analyzer|LineInserter"
jar tf analyzers/HoribaMicros60/target/HoribaMicros60-1.0.jar | grep -E "Analyzer|LineInserter"

# Check JAR sizes
ls -lh analyzers/HoribaPentra60/target/HoribaPentra60-1.0.jar
ls -lh analyzers/HoribaMicros60/target/HoribaMicros60-1.0.jar
```

**Expected**:

- Contains: `uw/edu/itech/HoribaPentra60/HoribaPentra60Analyzer.class` ✅
- Contains:
  `uw/edu/itech/HoribaPentra60/HoribaPentra60AnalyzerLineInserter.class` ✅
- Size: ~8KB per JAR ✅

---

## Phase 3: Integration Testing (Requires Deployment)

### Test 3.1: Deploy Plugins to Development Environment

```bash
# Copy JARs to container's plugin directory
docker cp plugins/analyzers/HoribaPentra60/target/HoribaPentra60-1.0.jar openelisglobal-webapp:/var/lib/openelis-global/plugins/
docker cp plugins/analyzers/HoribaMicros60/target/HoribaMicros60-1.0.jar openelisglobal-webapp:/var/lib/openelis-global/plugins/

# Verify deployment
docker exec openelisglobal-webapp ls -lh /var/lib/openelis-global/plugins/ | grep Horiba
```

**Expected**: Both JARs present in container ✅

---

### Test 3.2: Restart OpenELIS to Load Plugins

```bash
# Restart webapp container to trigger plugin loading
docker compose -f dev.docker-compose.yml restart oe.openelis.org

# Wait 30 seconds for startup
sleep 30

# Check logs for plugin registration
docker logs openelisglobal-webapp 2>&1 | grep -i "horiba\|pentra\|micros" | tail -20
```

**Expected output** (in logs):

- `PluginLoader: Loading plugins from /var/lib/openelis-global/plugins/`
- `Registered analyzer: Horiba ABX Pentra 60`
- `Registered analyzer: Horiba ABX Micros 60`
- NO exceptions or errors ✅

---

### Test 3.3: Verify Plugin Registration via API

```bash
# Login first (get session cookie)
curl -X POST https://${DOMAIN}/api/OpenELIS-Global/LoginPage \
  -k -c cookies.txt \
  -d "loginName=admin" \
  -d "password=adminADMIN!"

# Get analyzer list
curl -X GET https://${DOMAIN}/api/OpenELIS-Global/rest/analyzer-list \
  -k -b cookies.txt \
  -H "Accept: application/json" | jq '.[] | select(.name | contains("Horiba"))'
```

**Expected JSON**:

```json
{
  "id": "...",
  "name": "Horiba ABX Pentra 60",
  "description": "5-Part Differential Hematology Analyzer"
}
{
  "id": "...",
  "name": "Horiba ABX Micros 60",
  "description": "3-Part Differential Hematology Analyzer"
}
```

---

### Test 3.4: Send ASTM Test Messages

**Option A: Using ASTM Mock Server** (if M4 simulator is deployed):

```bash
# Start ASTM mock server in simulator mode
cd tools/analyzer-mock-server
python server.py --analyzer horiba-pentra60 --host localhost --port 9100 --send-once

# Or manually send test fixture via netcat
cat ../../src/test/resources/testdata/astm/horiba-pentra60-cbc.astm | nc localhost 9100
```

**Option B: Using Test Fixtures Directly** (bypass network):

```bash
# Call analyzer import API with ASTM fixture
curl -X POST https://${DOMAIN}/api/OpenELIS-Global/analyzer/astm \
  -k -b cookies.txt \
  -H "Content-Type: text/plain" \
  --data-binary @src/test/resources/testdata/astm/horiba-pentra60-cbc.astm
```

**Expected response**: HTTP 200, results queued for import ✅

---

### Test 3.5: Verify Results in UI

1. Navigate to: https://${DOMAIN}/AnalyzerResults
2. Login: `admin` / `adminADMIN!`
3. Look for imported results from Horiba analyzers
4. Verify:
   - [ ] Analyzer name appears correctly
   - [ ] Accession number extracted from O-segment
   - [ ] Test codes mapped to OpenELIS tests
   - [ ] Values and units present
   - [ ] Timestamp correct

---

## Phase 4: E2E Test Template Creation

Based on manual testing above, create Cypress E2E test:

**File**: `frontend/cypress/e2e/AdminE2E/horibaAnalyzerIntegration.cy.js`

### Test Scenarios (following Constitution V.5)

```javascript
describe("User Story 3: Horiba Analyzer Integration (M9-M10)", () => {
  before(() => {
    cy.session(
      "admin",
      () => {
        cy.login("admin", "adminADMIN!");
      },
      { cacheAcrossSpecs: true }
    );
  });

  it("should list Horiba ABX Pentra 60 in analyzer configuration", () => {
    cy.visit("/AnalyzerConfiguration");
    cy.get('[data-testid="analyzer-list"]').should(
      "contain",
      "Horiba ABX Pentra 60"
    );
  });

  it("should list Horiba ABX Micros 60 in analyzer configuration", () => {
    cy.visit("/AnalyzerConfiguration");
    cy.get('[data-testid="analyzer-list"]').should(
      "contain",
      "Horiba ABX Micros 60"
    );
  });

  it("should process Pentra 60 ASTM message and import results", () => {
    // Arrange: Load test fixture
    cy.fixture("astm/horiba-pentra60-cbc.astm").then((astmMessage) => {
      // Act: Send ASTM message to import endpoint
      cy.request({
        method: "POST",
        url: "/api/OpenELIS-Global/analyzer/astm",
        body: astmMessage,
        headers: { "Content-Type": "text/plain" },
      }).then((response) => {
        // Assert: Import accepted
        expect(response.status).to.eq(200);
      });
    });

    // Navigate to analyzer results
    cy.visit("/AnalyzerResults");

    // Verify results appear in queue
    cy.get('[data-testid="results-table"]').should("be.visible");
    cy.get('[data-testid="analyzer-name"]').should(
      "contain",
      "Horiba ABX Pentra 60"
    );

    // Verify test parameters imported (spot check WBC, RBC, HGB)
    cy.get('[data-testid="test-code"]').should("contain", "WBC");
    cy.get('[data-testid="test-code"]').should("contain", "RBC");
    cy.get('[data-testid="test-code"]').should("contain", "HGB");
  });

  it("should process Micros 60 ASTM message and import results with MXD", () => {
    cy.fixture("astm/horiba-micros60-cbc.astm").then((astmMessage) => {
      cy.request({
        method: "POST",
        url: "/api/OpenELIS-Global/analyzer/astm",
        body: astmMessage,
        headers: { "Content-Type": "text/plain" },
      }).then((response) => {
        expect(response.status).to.eq(200);
      });
    });

    cy.visit("/AnalyzerResults");
    cy.get('[data-testid="analyzer-name"]').should(
      "contain",
      "Horiba ABX Micros 60"
    );

    // Verify 3-part differential (LYM%, MXD%, NEU%)
    cy.get('[data-testid="test-code"]').should("contain", "LYM%");
    cy.get('[data-testid="test-code"]').should("contain", "MXD%"); // Mixed cells (no LOINC)
    cy.get('[data-testid="test-code"]').should("contain", "NEU%");
  });

  it("should display MXD test mapping warning (no LOINC code)", () => {
    cy.visit("/AnalyzerConfiguration");
    cy.get('[data-testid="analyzer-select"]').select("Horiba ABX Micros 60");
    cy.get('[data-testid="test-mappings"]').should("be.visible");

    // MXD should show warning about manual LOINC mapping required
    cy.get('[data-testid="mapping-warning"]').should("contain", "MXD");
    cy.get('[data-testid="mapping-warning"]').should("contain", "manual");
  });
});
```

---

## Phase 5: ASTM Message Parsing Verification

### Test 5.1: Pentra 60 ASTM Message Format

**Fixture**: `src/test/resources/testdata/astm/horiba-pentra60-cbc.astm`

**Expected structure** (LIS2-A2 format):

```
H|\^&|||ABX^PENTRA60^12345|||||||P|1|20260129120000
P|1||ACC123456||PATIENT^TEST||19900101|M|||||||||||||||||||
O|1|ACC123456||^^^WBC^|...
R|1|^^^WBC^|12.5|10^3/uL|4.0-10.0|N||F||20260129120000
R|2|^^^RBC^|4.85|10^6/uL|4.2-5.4|N||F||20260129120000
...
L|1|N
```

**Key extraction points**:

- **Analyzer ID**: H-segment field[4] component[1] = "PENTRA60" ✅
- **Accession**: O-segment field[2] component[0] = "ACC123456" ✅
- **Test code**: R-segment field[2] component[3] = "WBC" ✅
- **Value**: R-segment field[3] = "12.5" ✅
- **Units**: R-segment field[4] = "10^3/uL" ✅

---

### Test 5.2: Micros 60 MXD Handling (NO LOINC)

**Special case**: Micros 60 uses 3-part differential with **MXD (Mixed cells)**
= monocytes + eosinophils + basophils combined.

**Test mapping**:

```java
// Has LOINC (3-arg constructor)
new TestMapping("LYM%", "Lymphocytes %", "26478-8")
new TestMapping("NEU%", "Neutrophils %", "770-8")

// NO LOINC (2-arg constructor)
new TestMapping("MXD%", "Mixed Cells %")  // ← Uses 2-arg, LOINC empty
new TestMapping("MXD#", "Mixed Cells Absolute")
```

**Verification**:

```bash
# Should find 2 MXD mappings with 2-arg constructor
grep "new TestMapping.*MXD" ./analyzers/HoribaMicros60/src/main/java/uw/edu/itech/HoribaMicros60/HoribaMicros60Analyzer.java
```

**Expected**:

- 2 MXD mappings ✅
- Both use 2-arg constructor (no LOINC parameter) ✅

---

## Phase 6: Integration Test Development Template

### Recommended Test File Structure

**Location**:
`src/test/java/org/openelisglobal/analyzer/horiba/HoribaAnalyzerPluginIntegrationTest.java`

```java
@RunWith(SpringRunner.class)
@ContextConfiguration(locations = {"classpath*:/test-applicationContext.xml"})
public class HoribaAnalyzerPluginIntegrationTest {

  @Test
  public void testPentra60PluginRegistersCorrectly() {
    // Verify plugin loaded
    PluginAnalyzerService service = PluginAnalyzerService.getInstance();
    assertNotNull("Plugin service should exist", service);

    // Check analyzer registered
    // (requires access to analyzer cache - implementation detail TBD)
  }

  @Test
  public void testPentra60ParsesASTMMessage() throws Exception {
    // Load ASTM fixture
    String astmMessage = loadTestFixture("testdata/astm/horiba-pentra60-cbc.astm");

    // Get LineInserter
    HoribaPentra60Analyzer analyzer = new HoribaPentra60Analyzer();
    HoribaPentra60AnalyzerLineInserter inserter =
      (HoribaPentra60AnalyzerLineInserter) analyzer.getAnalyzerLineInserter();

    // Parse message
    List<String> lines = Arrays.asList(astmMessage.split("\n"));
    boolean success = inserter.insert(lines, "testuser");

    // Verify parsing
    assertTrue("Should parse successfully", success);
    assertNull("Should have no errors", inserter.getError());
  }

  @Test
  public void testMicros60HandlesMXDWithoutLOINC() {
    // Similar structure, but verify MXD mappings work without LOINC
  }
}
```

---

## Phase 7: End-to-End Testing Workflow

### Prerequisites for E2E

1. ✅ Plugins deployed to `/var/lib/openelis-global/plugins/`
2. ⏳ M2 (RS232 Bridge) merged and deployed **OR** use TCP-based ASTM mock
3. ⏳ M4 (Multi-protocol simulator) available for message generation
4. ✅ OpenELIS UI accessible at https://localhost

### E2E Test Workflow

**Scenario**: Lab technician receives CBC results from Horiba Pentra 60

1. **Arrange**:

   - Login as `LAB_SUPERVISOR`
   - Navigate to Admin → Analyzer Configuration
   - Verify "Horiba ABX Pentra 60" appears in list

2. **Act**:

   - Trigger ASTM message from simulator
   - Navigate to Analyzer Results Import page

3. **Assert**:

   - Results appear in queue within 60 seconds
   - Accession number correct (from O-segment)
   - 20 test parameters present (10 CBC + 10 differential)
   - All values/units mapped correctly
   - LOINC codes attached to test codes

4. **Cleanup**:
   - Archive or delete imported results

---

## Known Limitations & Test Gaps

### Cannot Test in Current Session

- ❌ **RS232 serial communication**: Requires physical USB-to-serial adapter +
  analyzer
- ❌ **Order export**: M15 not yet implemented
- ❌ **Metadata form**: M16 not yet implemented
- ❌ **Full E2E workflow**: Requires deployed instance with plugins loaded

### Can Test Now

- ✅ **Architecture verification**: NO Spring annotations ✅
- ✅ **Build verification**: JARs build successfully ✅
- ✅ **Code inspection**: Test mappings, connect() method, LOINC strategy ✅
- ✅ **Documentation**: READMEs complete ✅

---

## Test Execution Results

**Date**: **\*\***\_\_\_**\*\*** **Tester**: **\*\***\_\_\_**\*\***
**Environment**: ☐ Local Docker ☐ Staging ☐ CI/CD

### Architecture Verification

- [ ] NO Spring annotations (grep test)
- [ ] connect() method exists
- [ ] PluginAnalyzerService registration present
- [ ] Test mapping counts correct (Pentra: 20, Micros: 16 clinical)

### Build Verification

- [ ] Maven build SUCCESS
- [ ] JARs created (~8KB each)
- [ ] JAR contents valid (classes present)

### Integration Testing

- [ ] Plugins deployed to container
- [ ] OpenELIS restarted successfully
- [ ] Plugins registered (logs show confirmation)
- [ ] Analyzers appear in UI list
- [ ] ASTM messages processed correctly
- [ ] Results imported to queue

### E2E Testing

- [ ] Full workflow: config → send → import → validate
- [ ] MXD warning displayed for Micros 60
- [ ] All acceptance criteria met

---

## Next Steps

After M9-M10 approval:

1. Merge plugins PR #33 → `develop`
2. Merge outer repo PR #2643 → `develop`
3. Proceed with M11 (Stago STart 4 plugin)
4. Proceed with M12 (Abbott Architect plugin)
5. Proceed with M13 (Hain FluoroCycler XT plugin)

**Time remaining**: 30 days to contract deadline (2026-02-28) **Estimated
work**: M11-M13 (~6 days) + M15-M18 (~13 days) = 19 days total
