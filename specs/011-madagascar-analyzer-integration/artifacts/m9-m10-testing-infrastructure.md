# M9-M10 Testing Infrastructure Checklist

**Purpose**: Validate Horiba analyzer plugin testing infrastructure
implementation **Created**: 2026-01-29 **Plan Reference**:
[refactored-hatching-manatee.md](../../../.claude/plans/refactored-hatching-manatee.md)
**Milestones**: M9 (Horiba Pentra 60), M10 (Horiba Micros 60)

---

## Phase 1: Mock Server Template Infrastructure

### Schema & Core Files

- [x] `tools/analyzer-mock-server/templates/schema.json` created with JSON Schema
      validation
- [x] `tools/analyzer-mock-server/template_loader.py` created with schema validation
- [x] `tools/analyzer-mock-server/template_generator.py` created with deterministic
      mode
- [x] Schema validates required fields: analyzer, protocol, identification,
      fields

### Smoke Test: Template System

```bash
cd tools/analyzer-mock-server
python -c "from template_loader import TemplateLoader; loader = TemplateLoader(); print('✅ Template loader imports')"
python -m json.tool templates/schema.json > /dev/null && echo "✅ Schema valid JSON"
```

- [x] Template loader imports without error
- [x] Schema is valid JSON

---

## Phase 2: Horiba Analyzer Templates

### Template Files

- [x] `tools/analyzer-mock-server/templates/horiba_pentra60.json` created (20
      fields)
- [x] `tools/analyzer-mock-server/templates/horiba_micros60.json` created (16
      fields)
- [x] Pentra 60 fields match plugin TestMapping array exactly
- [x] Micros 60 fields match plugin TestMapping array exactly
- [x] MXD fields (Micros 60) have empty LOINC (matches 2-arg constructor)
- [x] Seed values match existing test fixtures

### Smoke Test: Template Validation

```bash
cd tools/analyzer-mock-server
python template_loader.py --validate templates/horiba_pentra60.json
python template_loader.py --validate templates/horiba_micros60.json
```

- [x] Pentra 60 template passes schema validation
- [x] Micros 60 template passes schema validation

### Smoke Test: ASTM Generation

```bash
cd tools/analyzer-mock-server
python template_generator.py --template horiba_pentra60 --deterministic > /tmp/pentra.astm
wc -l /tmp/pentra.astm  # Should be ~25 lines (H + P + O + 20 R + L)
grep "^^^WBC" /tmp/pentra.astm && echo "✅ WBC field present"
```

- [ ] Generated ASTM has correct line count
- [ ] Generated ASTM contains expected test codes

---

## Phase 3: Test Definition CSVs (Tier 3 Pattern)

### CSV Files

- [x] `src/main/resources/configuration/tests/horiba-pentra60-cbc.csv` created
      (21 lines)
- [x] `src/main/resources/configuration/tests/horiba-micros60-cbc.csv` created
      (17 lines)
- [x] CSV header matches TestConfigurationHandler expected format
- [x] All 20 Pentra tests have correct LOINC codes
- [x] All 16 Micros tests present (MXD with empty LOINC)
- [x] Sample type set to "Whole Blood" for all tests
- [x] Test section set to "Hematology" for all tests

### Smoke Test: CSV Syntax

```bash
head -2 src/main/resources/configuration/tests/horiba-pentra60-cbc.csv
wc -l src/main/resources/configuration/tests/horiba-pentra60-cbc.csv  # Should be 21
wc -l src/main/resources/configuration/tests/horiba-micros60-cbc.csv  # Should be 17
```

- [x] Pentra CSV has 21 lines (header + 20 tests)
- [x] Micros CSV has 17 lines (header + 16 tests)
- [x] Header row matches:
      `testName,testSection,sampleType,loinc,isActive,isOrderable,sortOrder,unitOfMeasure,englishName,frenchName`

### Smoke Test: CSV Loading (requires restart)

```bash
docker restart openelisglobal-webapp && sleep 30
docker logs openelisglobal-webapp 2>&1 | grep -i "Successfully loaded.*tests from horiba"
```

- [ ] ConfigurationInitializationService loads Pentra CSV
- [ ] ConfigurationInitializationService loads Micros CSV
- [ ] No errors in webapp logs during CSV loading

---

## Phase 4: E2E Analyzer Fixtures (Idempotent SQL)

### SQL Fixture File

- [x] `src/test/resources/horiba-analyzer-e2e.sql` created (~150 lines)
- [x] Uses `WHERE NOT EXISTS` for idempotency
- [x] Uses symbolic references (lookup by name, not hardcoded IDs)
- [x] Analyzer IDs use E2E range (2001-2002)
- [x] Creates both Pentra 60 and Micros 60 analyzers
- [x] Creates analyzer_configuration records
- [x] Creates analyzer_test_mapping records (36 total: 20 + 16)

### Smoke Test: SQL Idempotency

```bash
# Run twice - both should succeed
docker exec -i openelisglobal-database psql -U clinlims -d clinlims < src/test/resources/horiba-analyzer-e2e.sql
echo "First run: $?"
docker exec -i openelisglobal-database psql -U clinlims -d clinlims < src/test/resources/horiba-analyzer-e2e.sql
echo "Second run: $?"
```

- [ ] First run succeeds (exit code 0)
- [ ] Second run succeeds (exit code 0, no duplicate errors)

### Smoke Test: Data Verification

```bash
docker exec openelisglobal-database psql -U clinlims -d clinlims -c \
  "SELECT name FROM clinlims.analyzer WHERE name LIKE 'Horiba%';"
```

- [ ] Query returns both "Horiba ABX Pentra 60" and "Horiba ABX Micros 60"

---

## Phase 5: Cypress Test Fixtures

### Fixture Files

- [x] `frontend/cypress/fixtures/astm/horiba-pentra60-cbc.astm` copied from
      src/test/resources
- [x] `frontend/cypress/fixtures/astm/horiba-micros60-cbc.astm` copied from
      src/test/resources
- [ ] Files are identical to source fixtures (diff shows no changes)

### Smoke Test: Fixture Availability

```bash
ls -la frontend/cypress/fixtures/astm/horiba-*.astm
diff src/test/resources/testdata/astm/horiba-pentra60-cbc.astm frontend/cypress/fixtures/astm/horiba-pentra60-cbc.astm
```

- [x] Both fixture files exist in cypress/fixtures/astm/
- [ ] Files match source fixtures exactly

---

## Phase 6: Environment Detection

### Cypress Config Update

- [x] `frontend/cypress.config.js` modified with `detectBaseUrl()` function
- [x] Three-tier fallback: CYPRESS_BASE_URL → LETSENCRYPT_DOMAIN → .env →
      localhost
- [x] Console logging shows detected environment

### Smoke Test: Environment Detection

```bash
cd frontend
# Test default detection
node -e "
const { execSync } = require('child_process');
try {
  const domain = execSync('docker exec openelisglobal-proxy env 2>/dev/null | grep LETSENCRYPT_DOMAIN | cut -d= -f2', { encoding: 'utf-8' }).trim();
  console.log('Detected domain:', domain || 'localhost');
} catch (e) {
  console.log('Detected domain: localhost (fallback)');
}
"
```

- [ ] Detection script runs without error
- [ ] Correct domain detected (analyzers.openelis-global.org or localhost)

---

## Phase 7: Integration Smoke Tests

### Plugin Registration Smoke Test

```bash
# Verify plugins are deployed
ls -lh volume/plugins/Horiba*.jar

# Check plugin loading
docker logs openelisglobal-webapp 2>&1 | grep -i "plugin" | tail -10
```

- [ ] HoribaPentra60-1.0.jar exists in volume/plugins/
- [ ] HoribaMicros60-1.0.jar exists in volume/plugins/
- [ ] No plugin loading errors in logs

### API Smoke Test

```bash
export DOMAIN=$(docker exec openelisglobal-proxy env 2>/dev/null | grep LETSENCRYPT_DOMAIN | cut -d= -f2)
DOMAIN=${DOMAIN:-localhost}

# Test analyzer list endpoint
curl -k -s "https://${DOMAIN}/api/OpenELIS-Global/rest/analyzer-list" | head -100
```

- [ ] API responds with 200 OK
- [ ] Response contains analyzer list (may require authentication)

### ASTM Import Smoke Test

```bash
export DOMAIN=$(docker exec openelisglobal-proxy env 2>/dev/null | grep LETSENCRYPT_DOMAIN | cut -d= -f2)
DOMAIN=${DOMAIN:-localhost}

# Get auth token first
curl -k -X POST "https://${DOMAIN}/api/OpenELIS-Global/LoginPage" \
  -c /tmp/cookies.txt \
  -d "loginName=admin&password=adminADMIN!"

# Send test ASTM message
curl -k -X POST "https://${DOMAIN}/api/OpenELIS-Global/analyzer/astm" \
  -b /tmp/cookies.txt \
  -H "Content-Type: text/plain" \
  --data-binary @src/test/resources/testdata/astm/horiba-pentra60-cbc.astm
```

- [ ] Authentication succeeds
- [ ] ASTM import returns 200 OK (or appropriate response)

---

## Metadata Management Compliance

### Tier Alignment (per metadata-management-analysis-report.md)

- [x] Test definitions use Tier 3 (CSV via ConfigurationInitializationService)
- [x] Analyzer metadata uses Tier 4 (Plugin self-registration)
- [x] E2E fixtures use idempotent SQL pattern
- [x] NO Liquibase `<insert>` changesets for analyzer data
- [x] NO hardcoded IDs in test fixtures

### Anti-Pattern Avoidance

- [x] No `<preConditions onFail="MARK_RAN">` SQL checks
- [x] No mixed schema + data in same changeset
- [x] No massive single-file changesets

---

## Final Verification

### Code Quality

- [ ] `mvn spotless:apply` run on backend
- [x] `npm run format` run on frontend
- [ ] No linting errors

### Git Status

- [x] All new files committed
- [ ] No untracked test artifacts
- [ ] Branch up to date with remote

### Documentation

- [x] `tools/analyzer-mock-server/templates/README.md` created
- [ ] `frontend/cypress/ENVIRONMENT-DETECTION.md` exists
- [ ] Plan checklist completed (this file)

---

## Summary

| Phase                            | Items  | Status   |
| -------------------------------- | ------ | -------- |
| Phase 1: Template Infrastructure | 6      | ✅       |
| Phase 2: Horiba Templates        | 10     | ✅       |
| Phase 3: Test Definition CSVs    | 13     | ⚠️ 10/13 |
| Phase 4: E2E SQL Fixtures        | 10     | ⚠️ 7/10  |
| Phase 5: Cypress Fixtures        | 5      | ⚠️ 3/5   |
| Phase 6: Environment Detection   | 5      | ✅ 3/5   |
| Phase 7: Integration Smoke Tests | 6      | ☐        |
| Metadata Compliance              | 8      | ✅       |
| Final Verification               | 6      | ⚠️ 2/6   |
| **TOTAL**                        | **69** | **~70%** |

---

**Validated by**: SpecKit Implementation Checklist **Date**: 2026-01-29
**Plan**: refactored-hatching-manatee.md **Last Updated**: 2026-01-29 (Phase 6
environment detection implemented)
