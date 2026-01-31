# M9-M10 Testing Checklist: Horiba Analyzer Plugins

**Branch**: `feat/011-madagascar-analyzer-integration-m9-m10-horiba` **PR**:
[#33 (plugins)](https://github.com/DIGI-UW/openelisglobal-plugins/pull/33),
[#2643 (outer)](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2643)
**Analyzers**: Horiba ABX Pentra 60 + Micros 60 **Status**: Ready for testing

---

## Pre-Testing Verification ✅

- [x] **Branch clean**: No uncommitted changes
- [x] **Formatting applied**: `mvn spotless:apply` run
- [x] **Plugin JARs built**:
  - `plugins/analyzers/HoribaPentra60/target/HoribaPentra60-1.0.jar` (8.3KB) ✅
  - `plugins/analyzers/HoribaMicros60/target/HoribaMicros60-1.0.jar` (8.2KB) ✅
- [x] **Documentation updated**: READMEs + project catalog ✅
- [x] **Commits pushed**: To plugins submodule branch ✅
- [x] **PRs created**: Both repos ✅

---

## Testing Scope

### What's Being Tested (M9-M10)

- ✅ External plugin JAR architecture (NOT `@Component`-based)
- ✅ ASTM message parsing for Horiba Pentra 60 (26 params, 5-part diff)
- ✅ ASTM message parsing for Horiba Micros 60 (18 params, 3-part diff with MXD)
- ✅ Test code mapping via `AnalyzerTestNameCache`
- ✅ Plugin registration via `connect()` method
- ✅ LOINC code mapping (2-arg vs 3-arg `TestMapping`)

### What's NOT Being Tested (Out of Scope)

- ❌ RS232 serial communication (requires M2 bridge + physical hardware)
- ❌ Integration with OpenELIS UI (requires deployed instance)
- ❌ End-to-end workflow (requires M15 order export + full stack)

---

## Test Plan

### 1. Architecture Verification (CRITICAL)

**Verify external plugin JAR pattern is correct:**

```bash
# Check NO Spring annotations in plugin classes
cd plugins/analyzers
grep -r "@Component\|@Service\|@PostConstruct" Horiba*/src/main/java/ && echo "❌ FAIL: Found Spring annotations" || echo "✅ PASS: No Spring annotations"

# Check connect() method exists
grep -r "public void connect()" Horiba*/src/main/java/ && echo "✅ PASS: connect() found" || echo "❌ FAIL: Missing connect()"

# Check PluginAnalyzerService registration
grep -r "PluginAnalyzerService.getInstance().addAnalyzerDatabaseParts" Horiba*/src/main/java/ && echo "✅ PASS: Registration found" || echo "❌ FAIL: Missing registration"
```

**Expected**: All PASS ✅

---

### 2. Build Verification

```bash
# Build both plugins
cd /home/ubuntu/OpenELIS-Global-2/plugins
mvn clean package -pl analyzers/HoribaPentra60,analyzers/HoribaMicros60

# Check JAR sizes (should be ~8KB each)
ls -lh analyzers/HoribaPentra60/target/*.jar
ls -lh analyzers/HoribaMicros60/target/*.jar
```

**Expected**:

- BUILD SUCCESS ✅
- JARs present with size ~8KB ✅

---

### 3. Test Mapping Verification

**Check test mappings are correctly defined:**

```bash
# Pentra 60: Should have 20 mappings (10 CBC + 10 5-part diff)
grep -c "addAnalyzerDatabaseParts.*TestMapping" plugins/analyzers/HoribaPentra60/src/main/java/uw/edu/itech/HoribaPentra60/HoribaPentra60Analyzer.java

# Micros 60: Should have 16 mappings (10 CBC + 6 3-part diff)
grep -c "addAnalyzerDatabaseParts.*TestMapping" plugins/analyzers/HoribaMicros60/src/main/java/uw/edu/itech/HoribaMicros60/HoribaMicros60Analyzer.java
```

**Expected**:

- Pentra 60: 20 mappings ✅
- Micros 60: 16 mappings ✅

---

### 4. ASTM Message Parsing (Unit Test Level)

**Test with fixtures:**

```bash
# Check test fixtures exist
ls -la src/test/resources/testdata/astm/horiba-*

# Expected files:
# - horiba-pentra60-cbc.astm (20 R-segments)
# - horiba-micros60-cbc.astm (16 R-segments)
```

**Manual parsing test** (if fixtures exist):

- Verify R-segment format:
  `R|seq|^^^TEST_CODE|value|units|ref_range|flag||status|timestamp`
- Verify accession number extraction from O-segment
- Verify test code extraction from R-segment field[2] component[3]

---

### 5. Documentation Review

**Check READMEs:**

```bash
cat plugins/analyzers/HoribaPentra60/README.md
cat plugins/analyzers/HoribaMicros60/README.md
cat plugins/README.md | grep -A5 "Horiba"
```

**Expected content**:

- ✅ Analyzer specifications
- ✅ Test mapping tables with LOINC codes
- ✅ Build/deploy instructions
- ✅ MXD LOINC note (Micros 60 only)
- ✅ Project catalog updated

---

## Integration Testing (Requires Full Stack)

**NOTE**: These tests require a deployed OpenELIS instance + ASTM-HTTP bridge.
**NOT** part of M9-M10 scope.

### Future Integration Tests (M2 + M9-M10)

1. ☐ Deploy plugins to `/var/lib/openelis-global/plugins/`
2. ☐ Configure ASTM-HTTP bridge for RS232 (M2)
3. ☐ Send ASTM test fixture via bridge
4. ☐ Verify results appear in analyzer import queue
5. ☐ Verify test codes mapped correctly via cache

---

## Acceptance Criteria (From tasks.md)

### M9: Horiba Pentra 60

- [x] Plugin class created: `HoribaPentra60Analyzer.java` ✅
- [x] LineInserter created: `HoribaPentra60AnalyzerLineInserter.java` ✅
- [x] ASTM parsing implemented ✅
- [x] Test mappings registered (20 mappings) ✅
- [x] README documented ✅
- [x] JAR builds successfully ✅
- [ ] **Integration test pending** (requires M2 bridge + deployed instance)

### M10: Horiba Micros 60

- [x] Plugin class created: `HoribaMicros60Analyzer.java` ✅
- [x] LineInserter created: `HoribaMicros60AnalyzerLineInserter.java` ✅
- [x] ASTM parsing implemented ✅
- [x] Test mappings registered (16 mappings, MXD uses 2-arg) ✅
- [x] README documented ✅
- [x] JAR builds successfully ✅
- [ ] **Integration test pending** (requires M2 bridge + deployed instance)

---

## Known Limitations

1. **MXD (Mixed cells) on Micros 60**: No standard LOINC code. Uses 2-arg
   `TestMapping` constructor. Requires manual LOINC mapping in OpenELIS admin UI
   after deployment.

2. **RS232 testing**: Requires physical hardware + USB-serial adapter. Virtual
   serial testing covered by M2 (RS232 bridge milestone).

3. **End-to-end workflow**: Order export (M15) and metadata form (M16) not yet
   implemented.

---

## Sign-Off Checklist

Before merging PRs:

- [ ] All architecture verification tests PASS
- [ ] Build verification PASS
- [ ] Test mapping counts correct
- [ ] Documentation review complete
- [ ] No Spring annotations in plugin code
- [ ] Plugin JARs deploy to `/var/lib/openelis-global/plugins/` successfully
- [ ] Integration tests scheduled for post-merge (requires M2 merge +
      deployment)

---

**Testing Date**: **\*\***\_\_\_**\*\*** **Tester**: **\*\***\_\_\_**\*\***
**Result**: ☐ PASS ☐ FAIL (see notes) **Notes**:

---

**Next Steps After M9-M10 Approval**:

1. Merge plugins PR #33 → `develop`
2. Update outer repo submodule pointer
3. Merge outer repo PR #2643 → `develop`
4. Proceed with M11-M13 (remaining new plugins: Stago, Abbott, Hain)
