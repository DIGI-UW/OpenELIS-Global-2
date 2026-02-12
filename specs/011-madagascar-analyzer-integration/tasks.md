# Tasks: Madagascar Analyzer Integration

**Feature**: 011-madagascar-analyzer-integration **Input**: Design documents
from `/specs/011-madagascar-analyzer-integration/` **Prerequisites**: plan.md
(required), spec.md (required), data-model.md, contracts/, research.md,
quickstart.md **Contract Deadline**: 2026-02-28 (32 days from plan update)

**Organization**: Tasks are grouped by **Milestone** per Constitution Principle
IX. Each milestone = 1 PR.

**Tests**: **MANDATORY** per Constitution Principle V (TDD). Tests appear before
implementation tasks.

---

## Format: `[ID] [P?] [M#] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete
  tasks)
- **[M#]**: Which milestone this task belongs to (M0, M1, M2, etc.)
- Include exact file paths in descriptions

---

> **⚠️ ARCHITECTURAL GUARDRAIL**: All new analyzer plugin code (M9-M13) MUST be
> created in `plugins/analyzers/{PluginName}/` as external JAR plugins. Do NOT
> create analyzer classes in `src/main/java/org/openelisglobal/analyzer/`. See
> `docs/analyzer.md` for the mandatory external plugin pattern.

---

## Workstreams & Dependencies

> See **plan.md** for full parallel workstream diagram and milestone dependency
> graph. Summary: 5 workstreams (A: ASTM, B: HL7, C: RS232, D: File, E:
> Simulator) starting in parallel. M0 only blocks M7.

---

## [P] M0: ASTM Setup Validation (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m0-astm-stabilize`
**Goal**: Validate existing ASTM infrastructure from Feature 004 works correctly
with the mock analyzer **User Stories**: US-6 **Depends On**: None (can run
parallel with M1-M4) **Workstream**: A (ASTM)

**What Already Exists** (from Feature 004):

- Mock ASTM Server: `tools/analyzer-mock-server/` (Python 3.11, 1,221 lines)
- Docker Setup: `docker-compose.astm-test.yml` (3 analyzer types configured)
- Test Fixtures: `src/test/resources/astm-samples/` (7 files, 387 lines)
- ASTMAnalyzerReader: Implemented and working
- Q-Segment Parser: TDD-implemented with unit + integration tests
- API Endpoint: `POST /analyzer/astm` live in AnalyzerImportController

**Scope**: Test and validate what exists. NOT building new infrastructure.

**Acceptance Criteria**:

1. Mock server starts via `docker-compose.astm-test.yml`
2. Integration test passes: mock → `/analyzer/astm` → results stored
3. Field mappings work with existing fixtures (hematology, chemistry, QC)
4. Error dashboard captures ASTM-specific issues
5. Integration tests pass with >90% reliability

### Setup for M0

- [ ] T001 [M0] Create branch
      `feat/011-madagascar-analyzer-integration-m0-astm-stabilize` from
      `demo/madagascar`
- [ ] T002 [M0] Verify `tools/analyzer-mock-server/` runs via docker-compose

### Tests for M0 (MANDATORY - Write FIRST)

- [ ] T003 [P] [M0] Integration test: mock server → `/analyzer/astm` → results
      stored in
      `src/test/java/org/openelisglobal/analyzer/astm/ASTMMockIntegrationTest.java`
- [ ] T004 [P] [M0] Integration test: error dashboard captures ASTM failures in
      `src/test/java/org/openelisglobal/analyzer/astm/ASTMErrorDashboardTest.java`
- [ ] T005 [P] [M0] Verify existing fixtures work
      (`src/test/resources/astm-samples/`)

### Implementation for M0

- [ ] T006 [M0] Create/verify analyzer config targeting mock (docker static IP
      172.20.1.100)
- [ ] T007 [M0] Verify field mappings with existing ASTM fixtures
- [ ] T008 [M0] Test with hematology, chemistry, immunology mock types
- [ ] T009 [M0] Verify QC results (Q-segment) processing works

### Finalization for M0

- [ ] T010 [M0] Run full integration test suite
- [ ] T011 [M0] Run Spotless formatting (`mvn spotless:apply`)
- [ ] T012 [M0] Create PR
      `feat/011-madagascar-analyzer-integration-m0-astm-stabilize` →
      `demo/madagascar`

**Checkpoint**: ASTM mock → OpenELIS flow validated, ready for plugin work

**NOT in M0**: External plugin cloning (distributed to M5, M7, M8, M14)

---

## [P] M1: HL7 v2.x Protocol Adapter (3 days)

<!-- M1 PR #2602 merged 2026-01-28 (SHA: 7ba6ee272) - Tasks T021-T038 completed -->

**Branch**: `feat/011-madagascar-analyzer-integration-m1-hl7-adapter` **Goal**:
Parse HL7 ORU^R01 results and generate HL7 ORM^O01 orders **User Stories**: US-1
(HL7 Analyzer Results Import) **Depends On**: None (parallel with M0, M2-M4)
**Workstream**: B (HL7)

**Acceptance Criteria**:

1. HL7 ORU^R01 messages parse correctly (patient ID, test codes, results)
2. HL7 ORM^O01 messages generate correctly for order export
3. MSH segment sender ID extracted for analyzer identification
4. Unmapped fields create error records in dashboard
5. Unit test coverage >80%

### Setup for M1

- [x] T021 [M1] Create branch
      `feat/011-madagascar-analyzer-integration-m1-hl7-adapter` from
      `demo/madagascar`
- [x] T022 [M1] Add HAPI HL7 v2 dependency to `pom.xml`
      (ca.uhn.hapi:hapi-base:2.4, ca.uhn.hapi:hapi-structures-v251:2.4)

### Tests for M1 (MANDATORY - Write FIRST, ensure they FAIL)

- [x] T023 [P] [M1] Unit test for HL7MessageService in
      `src/test/java/org/openelisglobal/analyzer/service/HL7MessageServiceTest.java`
- [x] T024 [P] [M1] Unit test for HL7AnalyzerReader in
      `src/test/java/org/openelisglobal/analyzerimport/analyzerreaders/HL7AnalyzerReaderTest.java`
- [x] T025 [P] [M1] Create test HL7 message fixtures in
      `src/test/resources/testdata/hl7/mindray-cbc-result.hl7`
- [x] T026 [P] [M1] Create test HL7 message fixtures in
      `src/test/resources/testdata/hl7/sysmex-result.hl7`
- [x] T027 [P] [M1] Create test HL7 ORM^O01 expected output in
      `src/test/resources/testdata/hl7/expected-order.hl7`

### Implementation for M1

- [x] T028 [M1] Create HL7AnalyzerReader in
      `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/HL7AnalyzerReader.java`
      (extends AnalyzerReader, uses HAPI PipeParser)
- [x] T029 [M1] Create HL7MessageService interface in
      `src/main/java/org/openelisglobal/analyzer/service/HL7MessageService.java`
- [x] T030 [M1] Create HL7MessageServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/HL7MessageServiceImpl.java`
      (ORU^R01 parsing, ORM^O01 generation)
- [x] T031 [M1] Implement field extraction for ORU^R01 (PID segment → patient,
      OBR segment → test, OBX segment → results)
- [x] T032 [M1] Implement MSH segment parsing for analyzer identification
      (sending application, sending facility)
- [x] T033 [M1] Create MappingAwareHL7AnalyzerLineInserter wrapper in
      `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/MappingAwareHL7AnalyzerLineInserter.java`
- [x] T034 [M1] Integrate with existing FieldMappingService for test code
      mapping
- [x] T035 [M1] Add i18n keys for HL7-related messages in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Finalization for M1

- [x] T036 [M1] Verify all unit tests pass (`mvn test -Dtest="*HL7*"`)
- [x] T037 [M1] Run Spotless formatting (`mvn spotless:apply`)
- [x] T038 [M1] Create PR
      `feat/011-madagascar-analyzer-integration-m1-hl7-adapter` →
      `demo/madagascar`

**Checkpoint**: HL7 parsing and generation unit tests pass with >80% coverage

---

<!-- M2 PR #2600 merged 2026-01-28 (SHA: acdfa95b2) - D1 RESOLVED: Manual analyzer_id FK pattern - D2 RESOLVED: ORM validation test included - Tasks T039-T065 completed -->

## [P] M2: RS232 Bridge Extension (3 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m2-rs232-bridge` **Goal**:
Extend ASTM-HTTP Bridge to handle RS232→TCP conversion locally **User Stories**:
US-3 (RS232 Serial Analyzer Connection) **Depends On**: None (parallel with M0,
M1, M3-M4) **Workstream**: C (RS232)

**IMPORTANT**: Per clarification session (2026-01-27), RS232 connectivity is
provided via the **extended ASTM-HTTP Bridge** running on a lab PC with
USB-to-serial adapter. The bridge converts RS232→ASTM→HTTP for forwarding to
OpenELIS server. This keeps architecture consistent and avoids Docker serial
passthrough complexity.

**Acceptance Criteria**:

1. ASTM-HTTP bridge accepts RS232 connections via USB-serial adapter
2. Serial data converted to ASTM frames correctly
3. Messages forwarded to OpenELIS via existing HTTP endpoint
4. Configuration supports baud rate, parity, stop bits, flow control
5. Virtual serial port tests pass (socat)

### Setup for M2

- [x] T039 [M2] Create branch
      `feat/011-madagascar-analyzer-integration-m2-rs232-bridge` from
      `demo/madagascar`
- [x] T040 [M2] Add jSerialComm dependency to
      `tools/openelis-analyzer-bridge/pom.xml` (com.fazecast:jSerialComm:2.10.4)

### Tests for M2 (MANDATORY - Write FIRST)

- [x] T041 [P] [M2] Unit test for SerialPortListener in
      `tools/openelis-analyzer-bridge/src/test/java/serial/SerialPortListenerTest.java`
- [x] T042 [P] [M2] Unit test for SerialToAstmTranslator in
      `tools/openelis-analyzer-bridge/src/test/java/serial/SerialToAstmTranslatorTest.java`
- [x] T043 [P] [M2] Integration test with virtual serial port (socat) in
      `tools/openelis-analyzer-bridge/src/test/java/serial/VirtualSerialIntegrationTest.java`
- [x] T044 [P] [M2] Create test serial data fixtures in
      `tools/openelis-analyzer-bridge/src/test/resources/serial-test-data/`

### Implementation for M2 (in openelis-analyzer-bridge)

- [x] T045 [M2] Create SerialPortListener in
      `tools/openelis-analyzer-bridge/src/main/java/.../serial/SerialPortListener.java`
      (jSerialComm integration)
- [x] T046 [M2] Create SerialPortConfiguration in
      `tools/openelis-analyzer-bridge/src/main/java/.../serial/SerialPortConfiguration.java`
- [x] T047 [M2] Create SerialToAstmTranslator in
      `tools/openelis-analyzer-bridge/src/main/java/.../serial/SerialToAstmTranslator.java`
      (RS232→ASTM frame conversion)
- [x] T048 [M2] Add serial port configuration to application.yml
- [x] T049 [M2] Implement connection status tracking with event callbacks
- [x] T050 [M2] Implement graceful handling of cable disconnection
- [x] T051 [M2] Add health check endpoint for serial port status
- [x] T052 [M2] Document bridge deployment with USB-serial adapter in
      `tools/openelis-analyzer-bridge/docs/RS232_SETUP.md`

### OpenELIS Configuration UI for M2

- [x] T053 [M2] Create Liquibase changeset
      `src/main/resources/liquibase/3.8.x.x/011-001-create-serial-port-configuration-table.xml`
- [x] T054 [M2] Create SerialPortConfiguration entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/SerialPortConfiguration.java`
- [x] T055 [M2] Create StopBits, Parity, FlowControl enums in
      `src/main/java/org/openelisglobal/analyzer/valueholder/`
- [x] T056 [M2] Create SerialPortConfigurationDAO interface and impl in
      `src/main/java/org/openelisglobal/analyzer/dao/`
- [x] T057 [M2] Create SerialPortService interface and impl in
      `src/main/java/org/openelisglobal/analyzer/service/`
- [x] T058 [M2] Add serial port REST endpoints in
      `src/main/java/org/openelisglobal/analyzer/controller/SerialPortRestController.java`
- [x] T059 [P] [M2] Add i18n keys for serial config messages in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend for M2

- [x] T060 [P] [M2] Jest unit test for SerialConfiguration in
      `frontend/src/components/analyzers/SerialConfiguration/__tests__/SerialConfiguration.test.jsx`
- [x] T061 [M2] Create SerialConfiguration React component in
      `frontend/src/components/analyzers/SerialConfiguration/SerialConfiguration.jsx`
- [x] T062 [M2] Create serialService.js API client in
      `frontend/src/services/serialService.js`

### Finalization for M2

- [x] T063 [M2] Verify all unit tests pass
- [x] T064 [M2] Run Spotless formatting (`mvn spotless:apply`) and frontend
      formatting (`cd frontend && npm run format`)
- [x] T065 [M2] Create PR
      `feat/011-madagascar-analyzer-integration-m2-rs232-bridge` →
      `demo/madagascar`

**Checkpoint**: ASTM-HTTP bridge handles RS232→TCP, virtual serial tests pass

---

<!-- M3 PR #2599 merged 2026-01-28 (SHA: e640fb98e) - D1 RESOLVED: Manual analyzer_id FK pattern - D2 RESOLVED: ORM validation test included - Tasks T066-T089 completed -->

## [P] M3: File-Based Import Adapter (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m3-file-adapter` **Goal**:
Directory watcher and CSV/TXT file parsing **User Stories**: US-4 (File-Based
PCR Thermocycler Integration) **Depends On**: None (parallel with M0-M2, M4)
**Workstream**: D (File)

**Acceptance Criteria**:

1. Files detected within 60 seconds of creation
2. CSV rows mapped to results via configured column mappings
3. Processed files moved to archive directory
4. Malformed files moved to error directory with log entry
5. Duplicate detection warns before creating new results

### Setup for M3

- [x] T066 [M3] Create branch
      `feat/011-madagascar-analyzer-integration-m3-file-adapter` from
      `demo/madagascar`
- [x] T067 [M3] Add Apache Commons CSV dependency to `pom.xml`
      (org.apache.commons:commons-csv:1.10.0) if not present
- [x] T068 [M3] Create Liquibase changeset
      `src/main/resources/liquibase/3.8.x.x/011-002-create-file-import-configuration-table.xml`

### Tests for M3 (MANDATORY - Write FIRST)

- [x] T069 [P] [M3] ORM validation test for FileImportConfiguration in
      `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [x] T070 [P] [M3] Unit test for FileImportService in
      `src/test/java/org/openelisglobal/analyzer/service/FileImportServiceTest.java`
- [x] T071 [P] [M3] Unit test for FileAnalyzerReader in
      `src/test/java/org/openelisglobal/analyzerimport/analyzerreaders/FileAnalyzerReaderTest.java`
- [x] T072 [P] [M3] Create test CSV fixtures in
      `src/test/resources/testdata/files/quantstudio-results.csv`
- [x] T073 [P] [M3] Create malformed CSV fixture in
      `src/test/resources/testdata/files/malformed-results.csv`

### Implementation for M3

- [x] T074 [M3] Create FileImportConfiguration entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/FileImportConfiguration.java`
- [x] T075 [M3] Create FileImportConfigurationDAO in
      `src/main/java/org/openelisglobal/analyzer/dao/FileImportConfigurationDAO.java`
      and `FileImportConfigurationDAOImpl.java`
- [x] T076 [M3] Create FileImportService interface in
      `src/main/java/org/openelisglobal/analyzer/service/FileImportService.java`
- [x] T077 [M3] Create FileImportServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/FileImportServiceImpl.java`
      (WatchService integration)
- [x] T078 [M3] Create FileAnalyzerReader in
      `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/FileAnalyzerReader.java`
      (CSV parsing with Commons CSV)
- [x] T079 [M3] Implement file archival (move to archive directory on success)
- [x] T080 [M3] Implement error handling (move to error directory on failure
      with log)
- [x] T081 [M3] Implement duplicate detection (sample ID + test + timestamp)
- [x] T082 [M3] Add file import REST endpoints in
      `src/main/java/org/openelisglobal/analyzer/controller/FileImportRestController.java`
- [x] T083 [P] [M3] Add i18n keys for file import messages in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend for M3

- [x] T084 [P] [M3] Jest unit test for FileImportConfiguration in
      `frontend/src/components/analyzers/FileImportConfiguration/__tests__/FileImportConfiguration.test.jsx`
- [x] T085 [M3] Create FileImportConfiguration React component in
      `frontend/src/components/analyzers/FileImportConfiguration/FileImportConfiguration.jsx`
- [x] T086 [M3] Create fileImportService.js API client in
      `frontend/src/services/fileImportService.js`

### Finalization for M3

- [x] T087 [M3] Verify all unit tests pass
- [x] T088 [M3] Run Spotless and frontend formatting
- [x] T089 [M3] Create PR
      `feat/011-madagascar-analyzer-integration-m3-file-adapter` →
      `demo/madagascar`

**Checkpoint**: File detection + CSV parsing tests pass, archive/error workflow
works

---

<!-- M4 PR #2601 merged 2026-01-28 (SHA: 3f5286ff0) - Simulator templates: 12/12 analyzers - Protocol handlers: ASTM, HL7, File, Serial - Tasks T090-T121 completed -->

## [P] M4: Multi-Protocol Analyzer Simulator (3 days)

**Branch**:
`feat/011-madagascar-analyzer-integration-m4-simulator-multiprotocol` **Goal**:
Expand analyzer-mock-server to support HL7, RS232, and file-based protocols
**User Stories**: US-9 (Analyzer Simulator for Testing) **Depends On**: None
(parallel with M0-M3) **Workstream**: E (Simulator)

**Scope**: Extend analyzer-mock-server to cover 80%+ of 12 analyzers BEFORE
milestone implementation, enabling developers to test M1-M3 adapters and M5-M13
plugins without physical hardware.

**IMPORTANT**: This expands the **Python analyzer-mock-server** (testing
simulator), NOT the Java openelis-analyzer-bridge (production adapter). See
plan.md Tool Architecture section for distinction.

**Acceptance Criteria**:

1. Protocol abstraction layer supports ASTM, HL7, RS232, and File protocols
2. Simulator generates valid HL7 ORU^R01 messages
3. Virtual serial port simulation via socat (Linux)
4. File-based result generation (CSV/TXT)
5. Templates for majority of 12 contract analyzers
6. HTTP API mode for CI/CD integration
7. Messages received and processed correctly by OpenELIS

### Setup for M4

- [x] T090 [M4] Create branch
      `feat/011-madagascar-analyzer-integration-m4-simulator-multiprotocol` from
      `demo/madagascar`
- [x] T091 [M4] Create `tools/analyzer-mock-server/protocols/` directory
      structure
- [x] T092 [M4] Create `tools/analyzer-mock-server/templates/` directory
      structure
- [x] T093 [M4] Add pyserial dependency to
      `tools/analyzer-mock-server/requirements.txt`

### Core Architecture for M4

- [x] T094 [M4] Create protocol abstraction layer base class in
      `tools/analyzer-mock-server/protocols/__init__.py` and
      `tools/analyzer-mock-server/protocols/base_handler.py`
- [x] T095 [M4] Refactor existing ASTM code into ASTMHandler in
      `tools/analyzer-mock-server/protocols/astm_handler.py`
- [x] T096 [M4] Create template schema in
      `tools/analyzer-mock-server/templates/schema.json`

### HL7 Protocol Implementation (Priority 1 - enables M5-M7, M11-M12)

- [x] T097 [M4] Implement HL7Handler with ORU^R01 generation in
      `tools/analyzer-mock-server/protocols/hl7_handler.py`
- [x] T098 [M4] Create Mindray BC-5380 template in
      `tools/analyzer-mock-server/templates/mindray_bc5380.json`
- [x] T099 [M4] Create Sysmex XN template in
      `tools/analyzer-mock-server/templates/sysmex_xn.json`
- [x] T100 [M4] Create Abbott Architect HL7 template in
      `tools/analyzer-mock-server/templates/abbott_architect_hl7.json`
- [x] T101 [M4] Add HL7 HTTP endpoint `/simulate/hl7/{analyzer}` for CI/CD

### RS232 Protocol Implementation (Priority 2 - enables M5, M9-M11)

- [x] T102 [M4] Implement SerialHandler with virtual serial ports (socat) in
      `tools/analyzer-mock-server/protocols/serial_handler.py`
- [x] T103 [M4] Create Horiba Pentra 60 template in
      `tools/analyzer-mock-server/templates/horiba_pentra60.json`
- [x] T104 [M4] Create Horiba Micros 60 template in
      `tools/analyzer-mock-server/templates/horiba_micros60.json`
- [x] T105 [M4] Add serial simulation mode (`--serial-port` flag)

### File Protocol Implementation (Priority 3 - enables M8, M13)

- [x] T106 [M4] Implement FileHandler with CSV/TXT generation in
      `tools/analyzer-mock-server/protocols/file_handler.py`
- [x] T107 [M4] Create QuantStudio 7 Flex template in
      `tools/analyzer-mock-server/templates/quantstudio7.json`
- [x] T108 [M4] Create Hain FluoroCycler XT template in
      `tools/analyzer-mock-server/templates/hain_fluorocycler.json`
- [x] T109 [M4] Add file generation mode (`--generate-files` flag)

### Additional Analyzer Templates

- [x] T110 [M4] Create Mindray BS-360E template in
      `tools/analyzer-mock-server/templates/mindray_bs360e.json`
- [x] T111 [M4] Create Mindray BA-88A template in
      `tools/analyzer-mock-server/templates/mindray_ba88a.json`
- [x] T112 [M4] Create GeneXpert template in
      `tools/analyzer-mock-server/templates/genexpert.json`
- [x] T113 [M4] Create Stago STart 4 template in
      `tools/analyzer-mock-server/templates/stago_start4.json`

### Testing & Documentation for M4

- [x] T114 [P] [M4] Unit tests for all protocol handlers in
      `tools/analyzer-mock-server/test_protocols.py`
- [x] T115 [M4] Integration test: HL7 Simulator → OpenELIS reception
- [x] T116 [M4] Integration test: Serial Simulator → OpenELIS reception
- [x] T117 [M4] Integration test: File Simulator → OpenELIS import
- [x] T118 [M4] Update `tools/analyzer-mock-server/README.md` with
      multi-protocol usage

### Finalization for M4

- [x] T119 [M4] Verify all protocol handlers generate valid messages
- [x] T120 [M4] Verify backward compatibility with existing ASTM mode
- [x] T121 [M4] Create PR
      `feat/011-madagascar-analyzer-integration-m4-simulator-multiprotocol` →
      `demo/madagascar`

**Checkpoint**: Multi-protocol simulator covers 80%+ of 12 analyzers, enabling
CI/CD testing without physical hardware

---

<!-- M5 PR #2665 merged 2026-02-01 (SHA: 633abc91d) - Mindray BC-5380, BS-360E HL7 validation - Tasks T122-T133 completed -->

## M5: Mindray HL7 Plugin Validation (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m5-mindray-hl7` **Goal**:
Validate existing Mindray plugin with HL7 analyzers (BC-5380, BS-360E) **User
Stories**: US-1, US-6 **Depends On**: M1 (HL7 Adapter) **Workstream**: B (HL7)

**Plugins submodule**: Use parallel branch `feat/011-m5-mindray-hl7` in
`plugins` (DIGI-UW/openelisglobal-plugins). Open a PR there for any plugin
changes; main-repo PR holds integration tests and HL7 adapter work.

**Analyzers**: Mindray BC-5380, BS-360E (HL7 over Network) - Priority P1

**Acceptance Criteria**:

1. BC-5380 receives results via HL7 (simulator)
2. BS-360E receives results via HL7 (simulator)
3. Field mappings work with plugin + mapping system

### Setup for M5

- [x] T122 [M5] Create branch
      `feat/011-madagascar-analyzer-integration-m5-mindray-hl7` from
      `demo/madagascar`
- [x] T123 [M5] Ensure M1 is merged to `demo/madagascar`
- [x] T123a [M5] Check if Mindray plugin exists in `plugins/analyzers/`
      submodule; if not, create new plugin or source from DIGI-UW
- [x] T123b [M5] Build and verify Mindray plugin loads

### Tests for M5 (MANDATORY)

- [x] T124 [P] [M5] Integration test for Mindray BC-5380 HL7 in
      `src/test/java/org/openelisglobal/analyzer/mindray/MindrayBC5380IntegrationTest.java`
- [x] T125 [P] [M5] Integration test for Mindray BS-360E HL7 in
      `src/test/java/org/openelisglobal/analyzer/mindray/MindrayBS360EIntegrationTest.java`
- [x] T126 [P] [M5] Create HL7 test fixtures for BC-5380, BS-360E in
      `src/test/resources/testdata/hl7/mindray/`

### Implementation for M5

- [x] T127 [M5] Verify existing Mindray plugin compatibility with
      HL7AnalyzerReader
- [x] T128 [M5] Create MappingAwareMindrayAnalyzerLineInserter wrapper if needed
- [x] T129 [M5] Validate field mappings for BC-5380, BS-360E
- [x] T130 [M5] Document Mindray HL7 plugin integration

### Finalization for M5

- [x] T131 [M5] Verify all integration tests pass
- [x] T132 [M5] Run Spotless formatting
- [x] T133 [M5] Create PR
      `feat/011-madagascar-analyzer-integration-m5-mindray-hl7` →
      `demo/madagascar`

**Checkpoint**: Mindray BC-5380, BS-360E receive results via HL7

---

<!-- M6 PR #2675 merged 2026-02-03 (SHA: cb24fd5ed) - Mindray BA-88A RS232 integration test and fixture - Tasks T134-T142 completed -->

## M6: Mindray Serial Plugin Validation (1 day)

**Branch**: `feat/011-madagascar-analyzer-integration-m6-mindray-serial`
**Goal**: Validate Mindray BA-88A via RS232 bridge **User Stories**: US-3, US-6
**Depends On**: M2 (RS232 Bridge) **Workstream**: C (RS232)

**Analyzer**: Mindray BA-88A (RS232/ASTM) - Priority P1

**Acceptance Criteria**:

1. BA-88A receives results via RS232 bridge (virtual serial)
2. Field mappings work with existing Mindray plugin
3. Connection status properly tracked

### Setup for M6

- [x] T134 [M6] Create branch
      `feat/011-madagascar-analyzer-integration-m6-mindray-serial` from
      `demo/madagascar`

### Tests for M6 (MANDATORY)

- [x] T135 [M6] Integration test for Mindray BA-88A RS232 in
      `src/test/java/org/openelisglobal/analyzer/mindray/MindrayBA88AIntegrationTest.java`
- [x] T136 [M6] Create ASTM test fixtures for BA-88A in
      `src/test/resources/testdata/astm/mindray-ba88a-result.txt`

### Implementation for M6

- [x] T137 [M6] Configure RS232 parameters for BA-88A in bridge
- [x] T138 [M6] Verify existing Mindray plugin handles BA-88A format
- [x] T139 [M6] Validate field mappings for BA-88A
- [x] T140 [M6] Document BA-88A RS232 configuration

### Finalization for M6

- [x] T141 [M6] Verify integration tests pass
- [x] T142 [M6] Create PR
      `feat/011-madagascar-analyzer-integration-m6-mindray-serial` →
      `demo/madagascar`

**Checkpoint**: BA-88A receives results via RS232 bridge

---

## M7: GeneXpert Multi-Protocol Validation (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m7-genexpert-multi`
**Goal**: Validate all 3 GeneXpert plugin variants (ASTM, HL7, File) **User
Stories**: US-6 **Depends On**: M0, M1, M3 **Workstream**: A, B, D

**Analyzer**: Cepheid GeneXpert (ASTM/HL7/File variants) - Priority P1

### Setup for M7

- [x] T143 [M7] Create branch
      `feat/011-madagascar-analyzer-integration-m7-genexpert-multi` from
      `demo/madagascar`
- [x] T143a [M7] Check if GeneXpert plugins exist in `plugins/analyzers/`
      submodule; if not, create plugins for ASTM, HL7, and File variants
- [x] T143b [M7] Build and verify all 3 GeneXpert plugin variants load

### Tests for M7 (MANDATORY)

- [x] T144 [P] [M7] Integration test for GeneXpert HL7 in
      `src/test/java/org/openelisglobal/analyzer/genexpert/GeneXpertHL7IntegrationTest.java`
- [x] T145 [P] [M7] Integration test for GeneXpert File in
      `src/test/java/org/openelisglobal/analyzer/genexpert/GeneXpertFileIntegrationTest.java`
- [x] T146 [P] [M7] Create HL7 test fixture in
      `src/test/resources/testdata/hl7/genexpert-result.hl7`
- [x] T147 [P] [M7] Create file test fixture in
      `src/test/resources/testdata/files/genexpert-results.csv`

### Implementation for M7

- [x] T148 [M7] Verify GeneXpert ASTM plugin works (confirmed in M0)
- [x] T149 [M7] Verify GeneXpertHL7 plugin with HL7AnalyzerReader
- [x] T150 [M7] Verify GeneXpertFile plugin with FileAnalyzerReader
- [x] T151 [M7] Test all 3 variants can coexist (different configurations)
- [x] T152 [M7] Document GeneXpert multi-protocol support

### Finalization for M7

- [ ] T153 [M7] Verify all integration tests pass
- [ ] T154 [M7] Create PR
      `feat/011-madagascar-analyzer-integration-m7-genexpert-multi` →
      `demo/madagascar`

**Checkpoint**: GeneXpert works via ASTM, HL7, and File

---

<!-- M8 PR #2676 merged 2026-02-03 (SHA: 32d81b072) - QuantStudio 7 Flex analyzer plugin integration - Tasks T155-T164 completed -->

## M8: QuantStudio 7 Flex Adaptation (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m8-quantstudio` **Goal**:
Adapt QuantStudio3 plugin for QuantStudio 7 Flex **User Stories**: US-4, US-6
**Depends On**: M3 (File Adapter) **Workstream**: D (File)

**Analyzer**: Thermo Fisher QuantStudio 7 Flex (File-based) - Priority P1

### Setup for M8

- [x] T155 [M8] Create branch
      `feat/011-madagascar-analyzer-integration-m8-quantstudio` from
      `demo/madagascar`
- [x] T155a [M8] Check if QuantStudio plugin exists in `plugins/analyzers/`
      submodule; if not, create new plugin for QuantStudio 7 Flex
- [x] T155b [M8] Build and verify QuantStudio plugin loads

### Tests for M8 (MANDATORY)

- [x] T156 [M8] Integration test for QuantStudio 7 Flex in
      `src/test/java/org/openelisglobal/analyzer/quantstudio/QuantStudio7FlexIntegrationTest.java`
- [x] T157 [M8] Create CSV test fixture for QuantStudio 7 Flex format in
      `src/test/resources/testdata/files/quantstudio7-flex-results.csv`
- [x] T158 [M8] Backward compatibility test for QuantStudio 3 in
      `src/test/java/org/openelisglobal/analyzer/quantstudio/QuantStudio3BackwardCompatTest.java`

### Implementation for M8

- [x] T159 [M8] Analyze QuantStudio 7 Flex CSV format differences from
      QuantStudio 3
- [x] T160 [M8] Modify QuantStudio plugin or create FileImportConfiguration for
      column differences
- [x] T161 [M8] Ensure backward compatibility with QuantStudio 3
- [x] T162 [M8] Document QuantStudio adaptation

### Finalization for M8

- [x] T163 [M8] Verify all integration tests pass
- [x] T164 [M8] Create PR
      `feat/011-madagascar-analyzer-integration-m8-quantstudio` →
      `demo/madagascar`

**Checkpoint**: QuantStudio 7 Flex and QuantStudio 3 both work

---

<!-- M9-M10 Combined PR #2643 merged 2026-01-31 (SHA: 6f34267e1) - HoribaPentra60 + HoribaMicros60 plugins - Tasks T165-T184 completed -->

## [P] M9: Horiba Pentra 60 Plugin (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m9-m10-horiba` (combined
with M10) **Goal**: Build new Horiba Pentra 60 plugin **User Stories**: US-3
**Depends On**: M2 (RS232 Bridge) **Workstream**: C (RS232) **Status**: ✅
Plugin built, committed (b93e04b), documented (59d454d), PR created (#33)

**Analyzer**: Horiba ABX Pentra 60 (ASTM over RS232) - Priority P1

### Setup for M9

- [x] T165 [M9] Create branch
      `feat/011-madagascar-analyzer-integration-m9-pentra` from
      `demo/madagascar`

### Tests for M9 (MANDATORY)

- [x] T166 [P] [M9] Unit test for HoribaPentra60AnalyzerLineInserter in
      `plugins/analyzers/HoribaPentra60/src/test/java/uw/edu/itech/HoribaPentra60/HoribaPentra60AnalyzerLineInserterTest.java`
- [x] T167 [P] [M9] Create ASTM test fixtures for Pentra 60 in
      `src/test/resources/testdata/astm/pentra60-results.txt`

### Implementation for M9

- [x] T168 [M9] Create HoribaPentra60Analyzer external plugin class in
      `plugins/analyzers/HoribaPentra60/src/main/java/uw/edu/itech/HoribaPentra60/HoribaPentra60Analyzer.java`
- [x] T169 [M9] Create HoribaPentra60AnalyzerLineInserter in
      `plugins/analyzers/HoribaPentra60/src/main/java/uw/edu/itech/HoribaPentra60/HoribaPentra60AnalyzerLineInserter.java`
- [x] T170 [M9] Implement ASTM message parsing for Pentra 60 format
- [x] T171 [M9] Integrate with MappingAware wrapper pattern
- [x] T172 [M9] Document Pentra 60 plugin and RS232 bridge configuration

### Finalization for M9

- [x] T173 [M9] Verify unit tests pass
- [x] T174 [M9] Create PR `feat/011-madagascar-analyzer-integration-m9-pentra` →
      `demo/madagascar`

**Checkpoint**: Pentra 60 results import via RS232 bridge

---

## [P] M10: Horiba Micros 60 Plugin (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m9-m10-horiba` (combined
with M9) **Goal**: Build new Horiba Micros 60 plugin **User Stories**: US-3
**Depends On**: M2 (RS232 Bridge) **Workstream**: C (RS232) **Status**: ✅
Plugin built, committed (b93e04b), documented (59d454d), PR created (#33)

**Analyzer**: Horiba ABX Micros 60 (ASTM over RS232) - Priority P1

### Setup for M10

- [x] T175 [M10] Create branch
      `feat/011-madagascar-analyzer-integration-m10-micros` from
      `demo/madagascar`

### Tests for M10 (MANDATORY)

- [x] T176 [P] [M10] Unit test for HoribaMicros60AnalyzerLineInserter in
      `plugins/analyzers/HoribaMicros60/src/test/java/uw/edu/itech/HoribaMicros60/HoribaMicros60AnalyzerLineInserterTest.java`
- [x] T177 [P] [M10] Create ASTM test fixtures for Micros 60 in
      `src/test/resources/testdata/astm/micros60-results.txt`

### Implementation for M10

- [x] T178 [M10] Create HoribaMicros60Analyzer external plugin class in
      `plugins/analyzers/HoribaMicros60/src/main/java/uw/edu/itech/HoribaMicros60/HoribaMicros60Analyzer.java`
- [x] T179 [M10] Create HoribaMicros60AnalyzerLineInserter in
      `plugins/analyzers/HoribaMicros60/src/main/java/uw/edu/itech/HoribaMicros60/HoribaMicros60AnalyzerLineInserter.java`
- [x] T180 [M10] Implement ASTM message parsing for Micros 60 format
- [x] T181 [M10] Integrate with MappingAware wrapper pattern
- [x] T182 [M10] Document Micros 60 plugin

### Finalization for M10

- [x] T183 [M10] Verify unit tests pass
- [x] T184 [M10] Create PR `feat/011-madagascar-analyzer-integration-m10-micros`
      → `demo/madagascar`

**Checkpoint**: Micros 60 results import via RS232 bridge

---

## [P] M11: Stago STart 4 Plugin (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m11-stago` **Goal**: Build
new Stago STart 4 plugin **User Stories**: US-1, US-3 **Depends On**: M1 (HL7),
M2 (RS232) **Workstream**: B, C

**Analyzer**: Stago STart 4 (ASTM/HL7 over RS232/Network) - Priority P1-M

### Setup for M11

- [x] T185 [M11] Create branch
      `feat/011-madagascar-analyzer-integration-m11-stago` from
      `demo/madagascar`

### Tests for M11 (MANDATORY)

- [x] T186 [P] [M11] Unit test for StagoSTart4AnalyzerLineInserter in
      `plugins/analyzers/StagoSTart4/src/test/java/uw/edu/itech/StagoSTart4/StagoSTart4AnalyzerLineInserterTest.java`
- [x] T187 [P] [M11] Create test fixtures for Stago in
      `src/test/resources/testdata/stago/`

### Implementation for M11

- [x] T188 [M11] Create StagoSTart4Analyzer external plugin class in
      `plugins/analyzers/StagoSTart4/src/main/java/uw/edu/itech/StagoSTart4/StagoSTart4Analyzer.java`
- [x] T189 [M11] Create StagoSTart4AnalyzerLineInserter in
      `plugins/analyzers/StagoSTart4/src/main/java/uw/edu/itech/StagoSTart4/StagoSTart4AnalyzerLineInserter.java`
- [x] T190 [M11] Support both ASTM (RS232) and HL7 (Network) modes
- [x] T191 [M11] Integrate with MappingAware wrapper pattern
- [x] T192 [M11] Document Stago plugin with dual-protocol support

### Finalization for M11

- [x] T193 [M11] Verify unit tests pass
- [x] T194 [M11] Create PR `feat/011-madagascar-analyzer-integration-m11-stago`
      → `demo/madagascar`
- [x] T194a [M11] Create parallel plugin submodule PR
      `feat/011-madagascar-analyzer-integration-m11-stago` → `develop` in
      `openelisglobal-plugins` repository

**Checkpoint**: Stago STart 4 works via RS232 or Network

---

<!-- M12 PR #2662 merged 2026-02-01 (SHA: ecb250ba5) - Abbott Architect plugin - Tasks T195-T204 completed -->

## [P] M12: Abbott Architect Plugin (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m12-abbott` **Goal**:
Build new Abbott Architect plugin **User Stories**: US-1 **Depends On**: M1
(HL7) **Workstream**: B (HL7)

**Analyzer**: Abbott Architect (HL7 over RS232/Network) - Priority P1

### Setup for M12

- [x] T195 [M12] Create branch
      `feat/011-madagascar-analyzer-integration-m12-abbott` from
      `demo/madagascar`

### Tests for M12 (MANDATORY)

- [x] T196 [P] [M12] Unit test for AbbottArchitectAnalyzerLineInserter in
      `plugins/analyzers/AbbottArchitect/src/test/java/uw/edu/itech/AbbottArchitect/AbbottArchitectAnalyzerLineInserterTest.java`
- [x] T197 [P] [M12] Create HL7 test fixtures for Abbott Architect in
      `src/test/resources/testdata/hl7/abbott-architect-result.hl7`

### Implementation for M12

- [x] T198 [M12] Create AbbottArchitectAnalyzer external plugin class in
      `plugins/analyzers/AbbottArchitect/src/main/java/uw/edu/itech/AbbottArchitect/AbbottArchitectAnalyzer.java`
- [x] T199 [M12] Create AbbottArchitectAnalyzerLineInserter in
      `plugins/analyzers/AbbottArchitect/src/main/java/uw/edu/itech/AbbottArchitect/AbbottArchitectAnalyzerLineInserter.java`
- [x] T200 [M12] Implement HL7 message parsing for Abbott format
- [x] T201 [M12] Integrate with MappingAware wrapper pattern
- [x] T202 [M12] Document Abbott plugin

### Finalization for M12

- [x] T203 [M12] Verify unit tests pass
- [x] T204 [M12] Create PR `feat/011-madagascar-analyzer-integration-m12-abbott`
      → `demo/madagascar`

**Checkpoint**: Abbott Architect results import via HL7

---

<!-- M13 PR #2664 merged 2026-02-01 (SHA: eb6a495a0) - Hain FluoroCycler XT plugin - Tasks T205-T214 completed -->

## [P] M13: Hain FluoroCycler XT Plugin (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m13-fluorocycler`
**Goal**: Build new Hain FluoroCycler XT plugin **User Stories**: US-4 **Depends
On**: M3 (File) **Workstream**: D (File)

**Analyzer**: Hain Lifescience FluoroCycler XT (File-based) - Priority P1

### Setup for M13

- [x] T205 [M13] Create branch
      `feat/011-madagascar-analyzer-integration-m13-fluorocycler` from
      `demo/madagascar`

### Tests for M13 (MANDATORY)

- [x] T206 [P] [M13] Unit test for FluoroCyclerXTAnalyzerLineInserter in
      `plugins/analyzers/FluoroCyclerXT/src/test/java/uw/edu/itech/FluoroCyclerXT/FluoroCyclerXTAnalyzerLineInserterTest.java`
- [x] T207 [P] [M13] Create CSV test fixtures for FluoroCycler in
      `src/test/resources/testdata/files/fluorocycler-results.csv`

### Implementation for M13

- [x] T208 [M13] Create FluoroCyclerXTAnalyzer external plugin class in
      `plugins/analyzers/FluoroCyclerXT/src/main/java/uw/edu/itech/FluoroCyclerXT/FluoroCyclerXTAnalyzer.java`
- [x] T209 [M13] Create FluoroCyclerXTAnalyzerLineInserter in
      `plugins/analyzers/FluoroCyclerXT/src/main/java/uw/edu/itech/FluoroCyclerXT/FluoroCyclerXTAnalyzerLineInserter.java`
- [x] T210 [M13] Implement CSV parsing for FluoroCycler format
- [x] T211 [M13] Integrate with FileAnalyzerReader
- [x] T212 [M13] Document FluoroCycler plugin

### Finalization for M13

- [x] T213 [M13] Verify unit tests pass
- [x] T214 [M13] Create PR
      `feat/011-madagascar-analyzer-integration-m13-fluorocycler` →
      `demo/madagascar`

**Checkpoint**: FluoroCycler XT CSV import works

---

## M14: P2 Analyzer Validation (1 day)

**Branch**: `feat/011-madagascar-analyzer-integration-m14-p2-validation`
**Goal**: Validate P2 analyzers (BC2000, Sysmex XN) with existing plugins **User
Stories**: US-1, US-6 **Depends On**: M5 (Mindray HL7) **Workstream**: B (HL7)

**Analyzers**: Mindray BC2000, Sysmex XN Series (HL7 over Network) - Priority P2

### Setup for M14

- [x] T215 [M14] Create branch
      `feat/011-madagascar-analyzer-integration-m14-p2-validation` from
      `demo/madagascar`
- [x] T215a [M14] Check if SysmexXN-L plugin exists; if not, adapt existing
      Sysmex plugins (SysmeXT, Sysmex2000i, SysmexXT4000i) for XN-L series
- [x] T215b [M14] Build and verify SysmexXN-L plugin loads

### Tests for M14 (MANDATORY)

- [x] T216 [P] [M14] Integration test for Mindray BC2000 in
      `src/test/java/org/openelisglobal/analyzer/mindray/MindrayBC2000IntegrationTest.java`
- [x] T217 [P] [M14] Integration test for Sysmex XN in
      `src/test/java/org/openelisglobal/analyzer/sysmex/SysmexXNIntegrationTest.java`
- [x] T218 [P] [M14] Create HL7 test fixtures in
      `src/test/resources/testdata/hl7/sysmex-xn-result.hl7`

### Implementation for M14

- [x] T219 [M14] Verify Mindray plugin handles BC2000 (shares BC-5380 config)
- [x] T220 [M14] Verify SysmexXN-L plugin compatibility with HL7AnalyzerReader
- [x] T221 [M14] Test override mappings take precedence over plugin defaults
- [x] T222 [M14] Document P2 analyzer integration

### Finalization for M14

- [x] T223 [M14] Verify integration tests pass
- [x] T224 [M14] Create PR
      `feat/011-madagascar-analyzer-integration-m14-p2-validation` →
      `demo/madagascar` (PR #2674)

**Checkpoint**: All 12 analyzers receive results

---

## M15: Order Export Workflow (3 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m15-order-export`
**Goal**: Manual order export with status tracking **User Stories**: US-2 (Test
Order Export to Analyzers) **Depends On**: M5-M14 (all analyzers operational)
**Workstream**: All

**Acceptance Criteria**:

1. Users can select pending orders and trigger export
2. Orders sent via appropriate protocol (ASTM/HL7)
3. Status tracked: pending → sent → acknowledged → results_received
4. Retry mechanism (3 attempts, exponential backoff)
5. Results automatically matched to exported orders
6. UI displays export status per analyzer

### Setup for M15

- [ ] T225 [M15] Create branch
      `feat/011-madagascar-analyzer-integration-m15-order-export` from
      `demo/madagascar`
- [ ] T226 [M15] Create Liquibase changeset
      `src/main/resources/liquibase/3.8.x.x/011-003-create-order-export-table.xml`

### Tests for M15 (MANDATORY)

- [ ] T227 [P] [M15] ORM validation test for OrderExport in
      `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [ ] T228 [P] [M15] Unit test for OrderExportService in
      `src/test/java/org/openelisglobal/analyzer/service/OrderExportServiceTest.java`
- [ ] T229 [P] [M15] Unit test for OrderExportDAO in
      `src/test/java/org/openelisglobal/analyzer/dao/OrderExportDAOTest.java`
- [ ] T230 [P] [M15] Controller test for OrderExportRestController in
      `src/test/java/org/openelisglobal/analyzer/controller/OrderExportRestControllerTest.java`
- [ ] T231 [P] [M15] Create DBUnit test fixture in
      `src/test/resources/testdata/order-export.xml`

### Backend Implementation for M15

- [ ] T232 [M15] Create OrderExport entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/OrderExport.java`
- [ ] T233 [M15] Create OrderExportStatus and MessageType enums in
      `src/main/java/org/openelisglobal/analyzer/valueholder/`
- [ ] T234 [M15] Create OrderExportDAO interface and impl in
      `src/main/java/org/openelisglobal/analyzer/dao/`
- [ ] T235 [M15] Create OrderExportService interface in
      `src/main/java/org/openelisglobal/analyzer/service/OrderExportService.java`
- [ ] T236 [M15] Create OrderExportServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/OrderExportServiceImpl.java`
- [ ] T237 [M15] Implement ASTM O-segment generation for order export
- [ ] T238 [M15] Implement HL7 ORM^O01 generation (leverage M1
      HL7MessageService)
- [ ] T239 [M15] Implement retry mechanism with exponential backoff
- [ ] T240 [M15] Implement result matching (incoming results → exported orders)
- [ ] T241 [M15] Create OrderExportRestController in
      `src/main/java/org/openelisglobal/analyzer/controller/OrderExportRestController.java`
- [ ] T242 [M15] Implement RBAC permission check (LAB_SUPERVISOR role minimum)
- [ ] T243 [M15] Add audit trail logging for order export actions
- [ ] T244 [P] [M15] Add i18n keys for order export messages in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend Implementation for M15

- [ ] T245 [P] [M15] Jest unit test for OrderExportList in
      `frontend/src/components/analyzers/OrderExport/__tests__/OrderExportList.test.jsx`
- [ ] T246 [P] [M15] Jest unit test for OrderExportModal in
      `frontend/src/components/analyzers/OrderExport/__tests__/OrderExportModal.test.jsx`
- [ ] T247 [M15] Create OrderExportList React component in
      `frontend/src/components/analyzers/OrderExport/OrderExportList.jsx`
- [ ] T248 [M15] Create OrderExportModal React component in
      `frontend/src/components/analyzers/OrderExport/OrderExportModal.jsx`
- [ ] T249 [M15] Create orderExportService.js API client in
      `frontend/src/services/orderExportService.js`
- [ ] T250 [M15] Integrate OrderExport components into analyzer dashboard

### Finalization for M15

- [ ] T251 [M15] Verify all unit and integration tests pass
- [ ] T252 [M15] Run Spotless and frontend formatting
- [ ] T253 [M15] Create PR
      `feat/011-madagascar-analyzer-integration-m15-order-export` →
      `demo/madagascar`

**Checkpoint**: Order export UI works, orders sent to analyzers, status tracked

---

## M16: Enhanced Instrument Metadata Form (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m16-metadata-form`
**Goal**: Comprehensive metadata capture and location history **User Stories**:
US-5 (Comprehensive Instrument Metadata Management) **Depends On**: M15
**Workstream**: All

**Acceptance Criteria**:

1. Form captures all required metadata fields
2. Location linked to existing facility hierarchy
3. Location history preserved on relocation
4. Calibration due date warning displayed
5. Validation prevents incomplete registrations

### Setup for M16

- [ ] T254 [M16] Create branch
      `feat/011-madagascar-analyzer-integration-m16-metadata-form` from
      `demo/madagascar`
- [ ] T255 [M16] Create Liquibase changeset
      `src/main/resources/liquibase/3.8.x.x/011-004-create-instrument-metadata-table.xml`
- [ ] T256 [M16] Create Liquibase changeset
      `src/main/resources/liquibase/3.8.x.x/011-005-create-instrument-location-history-table.xml`

### Tests for M16 (MANDATORY)

- [ ] T257 [P] [M16] ORM validation test for InstrumentMetadata in
      `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [ ] T258 [P] [M16] ORM validation test for InstrumentLocationHistory
- [ ] T259 [P] [M16] Unit test for InstrumentMetadataService in
      `src/test/java/org/openelisglobal/analyzer/service/InstrumentMetadataServiceTest.java`
- [ ] T260 [P] [M16] Controller test for InstrumentMetadataRestController in
      `src/test/java/org/openelisglobal/analyzer/controller/InstrumentMetadataRestControllerTest.java`
- [ ] T261 [P] [M16] Create DBUnit test fixtures in
      `src/test/resources/testdata/instrument-metadata.xml`

### Backend Implementation for M16

- [ ] T262 [M16] Create InstrumentMetadata entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/InstrumentMetadata.java`
- [ ] T263 [M16] Create InstrumentLocationHistory entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/InstrumentLocationHistory.java`
- [ ] T264 [M16] Create ServiceStatus enum in
      `src/main/java/org/openelisglobal/analyzer/valueholder/ServiceStatus.java`
- [ ] T265 [M16] Create InstrumentMetadataDAO interface and impl in
      `src/main/java/org/openelisglobal/analyzer/dao/`
- [ ] T266 [M16] Create InstrumentLocationHistoryDAO interface and impl
- [ ] T267 [M16] Create InstrumentMetadataService interface in
      `src/main/java/org/openelisglobal/analyzer/service/InstrumentMetadataService.java`
- [ ] T268 [M16] Create InstrumentMetadataServiceImpl with relocation logic
- [ ] T269 [M16] Implement calibration due date warning calculation
- [ ] T270 [M16] Create InstrumentMetadataRestController in
      `src/main/java/org/openelisglobal/analyzer/controller/InstrumentMetadataRestController.java`
- [ ] T271 [M16] Implement RBAC permission check (LAB_SUPERVISOR role minimum)
- [ ] T272 [M16] Add audit trail logging for instrument metadata changes
- [ ] T273 [P] [M16] Add i18n keys for metadata form in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend Implementation for M16

- [ ] T274 [P] [M16] Jest unit test for InstrumentMetadataForm in
      `frontend/src/components/analyzers/InstrumentMetadata/__tests__/InstrumentMetadataForm.test.jsx`
- [ ] T275 [M16] Create InstrumentMetadataForm React component in
      `frontend/src/components/analyzers/InstrumentMetadata/InstrumentMetadataForm.jsx`
- [ ] T276 [M16] Implement Organization/Location picker (reuse existing
      components)
- [ ] T277 [M16] Implement location history display
- [ ] T278 [M16] Implement calibration due date warning display
- [ ] T279 [M16] Create instrumentMetadataService.js API client in
      `frontend/src/services/instrumentMetadataService.js`

### Finalization for M16

- [ ] T280 [M16] Verify all tests pass
- [ ] T281 [M16] Run Spotless and frontend formatting
- [ ] T282 [M16] Create PR
      `feat/011-madagascar-analyzer-integration-m16-metadata-form` →
      `demo/madagascar`

**Checkpoint**: Metadata form captures all fields, location history works

---

## M17: Advanced Simulator Features (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m17-simulator-advanced`
**Goal**: Advanced simulation: QC results, error conditions, concurrent testing
**User Stories**: US-9 **Depends On**: M4 (Simulator Base) **Workstream**: E
(Simulator)

**Acceptance Criteria**:

1. QC result generation for all analyzer types
2. Error condition simulation (malformed, timeouts, duplicates)
3. Concurrent multi-analyzer simulation (5+ simultaneous)
4. Stress testing capability (1000+ messages)
5. Test scenario orchestration via HTTP API

### Setup for M17

- [ ] T283 [M17] Create branch
      `feat/011-madagascar-analyzer-integration-m17-simulator-advanced` from
      `demo/madagascar`

### Advanced Features for M17

- [ ] T284 [M17] Add QC result generation templates in
      `tools/analyzer-mock-server/templates/qc/`
- [ ] T285 [M17] Add error condition templates (malformed, timeout, duplicate)
      in `tools/analyzer-mock-server/templates/errors/`
- [ ] T286 [M17] Implement concurrent multi-analyzer support in
      `tools/analyzer-mock-server/server.py`
- [ ] T287 [M17] Implement test scenario orchestration (`/scenarios/{name}`
      endpoint)
- [ ] T288 [M17] Add stress testing mode (`--stress-test --count N`)

### CI/CD Integration for M17

- [ ] T289 [M17] Update Docker configuration for CI/CD integration
- [ ] T290 [M17] Create GitHub Actions workflow for simulator-based tests
- [ ] T291 [M17] Document CI/CD integration in
      `tools/analyzer-mock-server/docs/CI_CD_INTEGRATION.md`

### Testing for M17

- [ ] T292 [M17] Test concurrent simulation with 5+ analyzers
- [ ] T293 [M17] Test stress scenario with 1000+ messages
- [ ] T294 [M17] Verify all 12 analyzer templates work end-to-end

### Finalization for M17

- [ ] T295 [M17] Verify all advanced features work
- [ ] T296 [M17] Create PR
      `feat/011-madagascar-analyzer-integration-m17-simulator-advanced` →
      `demo/madagascar`

**Checkpoint**: Simulator ready for production CI/CD with advanced testing

---

## M18: E2E Validation (3 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m18-e2e-validation`
**Goal**: Comprehensive E2E testing and production validation **User Stories**:
All **Depends On**: M15, M16, M17 **Workstream**: All

**Acceptance Criteria**:

1. All 12 analyzers receive results within 60 seconds
2. All 12 analyzers receive orders via export
3. 5+ analyzers operate simultaneously without issues
4. <5% mapping errors after initial configuration
5. E2E tests pass in CI/CD pipeline
6. Simulator-only validation sufficient per clarification session

### Setup for M18

- [ ] T297 [M18] Create branch
      `feat/011-madagascar-analyzer-integration-m18-e2e-validation` from
      `demo/madagascar`

### Cypress E2E Tests for M18 (MANDATORY)

- [ ] T298 [P] [M18] E2E test for HL7 analyzer integration in
      `frontend/cypress/e2e/hl7AnalyzerIntegration.cy.js`
- [ ] T299 [P] [M18] E2E test for serial analyzer integration in
      `frontend/cypress/e2e/serialAnalyzerIntegration.cy.js`
- [ ] T300 [P] [M18] E2E test for file import integration in
      `frontend/cypress/e2e/fileImportIntegration.cy.js`
- [ ] T301 [P] [M18] E2E test for order export in
      `frontend/cypress/e2e/orderExport.cy.js`
- [ ] T302 [P] [M18] E2E test for instrument metadata form in
      `frontend/cypress/e2e/instrumentMetadata.cy.js`
- [ ] T303 [M18] Create Cypress fixtures for E2E tests in
      `frontend/cypress/fixtures/analyzers/`

### Integration Testing for M18

- [ ] T304 [M18] Performance test: 5+ concurrent analyzers via simulator
- [ ] T305 [M18] Stress test: 1000+ messages through system
- [ ] T306 [M18] Verify message routing with multiple simultaneous analyzers
- [ ] T306a [M18] RS232 reliability soak test (8+ hours) using RS232 bridge +
      simulator (or virtual serial ports) and verify no message loss (SC-007)
- [ ] T306b [M18] Validate analyzer communication service uptime measurement and
      monitoring approach for lab operating hours (SC-010)
- [ ] T306c [M18] Verify timestamps are stored in UTC and rendered correctly for
      analyzer results and order export workflows (Edge Case: time zones)

### Documentation for M18

- [ ] T307 [M18] Create user training materials in `docs/madagascar-training/`
- [ ] T308 [M18] Document configuration guide for each analyzer type
- [ ] T309 [M18] Create deployment checklist for Madagascar labs

### Finalization for M18

- [ ] T310 [M18] Verify all E2E tests pass
- [ ] T311 [M18] Verify all 12 analyzers bidirectional
- [ ] T312 [M18] Run full test suite
      (`mvn verify && cd frontend && npm run cy:run`)
- [ ] T313 [M18] Create PR
      `feat/011-madagascar-analyzer-integration-m18-e2e-validation` →
      `demo/madagascar`
- [ ] T314 [M18] After approval, create final PR `demo/madagascar` → `develop`

**Checkpoint**: Contract requirements met - 12 analyzers bidirectional, E2E
tests pass

---

## Dependencies & Execution Order

### Milestone Dependencies

| Milestone | Depends On    | Workstream    | Parallel Group              |
| --------- | ------------- | ------------- | --------------------------- |
| **M0**    | -             | A (ASTM)      | Foundation (0-4) [PARALLEL] |
| **M1**    | -             | B (HL7)       | Foundation (0-4) [PARALLEL] |
| **M2**    | -             | C (RS232)     | Foundation (0-4) [PARALLEL] |
| **M3**    | -             | D (File)      | Foundation (0-4) [PARALLEL] |
| **M4**    | -             | E (Simulator) | Foundation (0-4) [PARALLEL] |
| M5        | M1            | B (HL7)       | Plugin Validation           |
| M6        | M2            | C (RS232)     | Plugin Validation           |
| M7        | M0, M1, M3    | A, B, D       | Plugin Validation           |
| M8        | M3            | D (File)      | Plugin Validation           |
| M9        | M2            | C (RS232)     | New Plugins (9-13) [P]      |
| M10       | M2            | C (RS232)     | New Plugins (9-13) [P]      |
| M11       | M1, M2        | B, C          | New Plugins (9-13) [P]      |
| M12       | M1            | B (HL7)       | New Plugins (9-13) [P]      |
| M13       | M3            | D (File)      | New Plugins (9-13) [P]      |
| M14       | M5            | B (HL7)       | P2 Validation               |
| M15       | M5-M14        | All           | Integration                 |
| M16       | M15           | All           | Integration                 |
| M17       | M4            | E (Simulator) | Integration [P]             |
| M18       | M15, M16, M17 | All           | Final Validation            |

**Key**: M0-M4 are ALL parallel (no dependencies between them). M0 only blocks
M7 (GeneXpert Multi requires M0 for ASTM variant).

### Parallel Opportunities

**Week 1** (All Foundation Milestones Start Together):

- M0, M1, M2, M3, M4 ALL start in parallel (5 parallel tracks)
- No dependencies between them

**Week 1-2**:

- M5, M6, M8, M9-M13 start as their adapter dependencies complete
- M7 starts after M0 + M1 + M3 (only milestone blocked by M0)

**Week 2-3**:

- M9, M10, M11, M12, M13 can all proceed in parallel (5 developers max)

**Week 3-4**:

- M14 after M5
- M15 and M17 can proceed in parallel

**Week 4-5**:

- M16 after M15
- M18 after M15, M16, M17

### Within Each Milestone

- Branch creation MUST be first task
- Tests MUST be written and FAIL before implementation (TDD)
- Implementation follows Red-Green-Refactor
- PR creation MUST be last task
- Spotless/formatting MUST run before PR

---

## Implementation Strategy

### Parallel Foundation Strategy (Per 2026-01-28 Clarification)

1. **M0-M4**: All start in parallel (no dependencies between them)
   - M0: Validate existing ASTM mock setup
   - M1-M4: Build adapters simultaneously
2. **M5-M13**: Validate/build plugins as adapters complete (P1 first)
3. **M7**: Requires M0 + M1 + M3 (only milestone blocked by M0)
4. **M14**: P2 analyzers last
5. **M15-M18**: Integration and validation

### MVP First (M0-M4 parallel + M5)

1. Complete M0-M4 in parallel: Foundation (all start together)
2. Complete M5: Mindray HL7 (4 analyzers working)
3. **STOP and VALIDATE**: 5+ analyzers receive results
4. Deploy to staging for early testing

### Incremental Delivery

| Week | Milestones                 | Cumulative Analyzers       |
| ---- | -------------------------- | -------------------------- |
| 1    | M0, M1, M2, M3, M4 (all P) | Adapters ready             |
| 2    | M5, M6, M7, M8             | 7 analyzers                |
| 3    | M9, M10, M11, M12, M13     | 12 analyzers               |
| 4    | M14, M15                   | + P2 validation + export   |
| 5    | M16, M17, M18              | + metadata + E2E validated |

### Parallel Team Strategy

**With 4+ developers** (M0-M4 all start Week 1):

| Developer | Week 1         | Week 2-3         | Week 4-5 |
| --------- | -------------- | ---------------- | -------- |
| Dev A     | M0 (ASTM) + M1 | M5 → M14 → M15   | M18      |
| Dev B     | M2 (RS232)     | M6, M9, M10      | M18      |
| Dev C     | M3 (File)      | M7, M8, M11, M12 | M16      |
| Dev D     | M4 (Simulator) | M13 → M17        | M18      |

---

## Post-Deadline Features (NOT IN TASK COUNT)

The following are **intentionally deferred** to post-contract-deadline:

| Requirement      | User Story | Scope                       | Reason                     |
| ---------------- | ---------- | --------------------------- | -------------------------- |
| FR-019 to FR-021 | US-7       | GeneXpert Module Management | P3 priority, complex UI    |
| FR-022 to FR-024 | US-8       | Maintenance Tracking        | P3 priority, non-essential |
| POCT1A Protocol  | -          | Point-of-care devices       | Out of contract scope      |

---

## Task Summary

| Milestone | Total Tasks | Test Tasks | Implementation Tasks |
| --------- | ----------- | ---------- | -------------------- |
| **M0**    | 12          | 3          | 9                    |
| M1        | 18          | 5          | 13                   |
| M2        | 27          | 4          | 23                   |
| M3        | 24          | 5          | 19                   |
| M4        | 32          | 5          | 27                   |
| M5        | 14          | 3          | 11                   |
| M6        | 9           | 2          | 7                    |
| M7        | 14          | 4          | 10                   |
| M8        | 12          | 3          | 9                    |
| M9        | 10          | 2          | 8                    |
| M10       | 10          | 2          | 8                    |
| M11       | 10          | 2          | 8                    |
| M12       | 10          | 2          | 8                    |
| M13       | 10          | 2          | 8                    |
| M14       | 12          | 3          | 9                    |
| M15       | 29          | 5          | 24                   |
| M16       | 29          | 5          | 24                   |
| M17       | 14          | 3          | 11                   |
| M18       | 18          | 6          | 12                   |
| **TOTAL** | **~304**    | **66**     | **~238**             |

**Note**: M0 reduced from 20 to 12 tasks (plugin cloning moved to M5, M7, M8,
M14). Plugin tasks added to respective milestones.

---

## Notes

- **[P]** = Can run in parallel (different files, no incomplete dependencies)
- **[M#]** = Milestone tag for traceability
- **M0-M4 ALL PARALLEL** - No dependencies between foundation milestones
- **M0 only blocks M7** - GeneXpert Multi requires M0 for ASTM variant
- Tests are MANDATORY per Constitution Principle V
- Each milestone = 1 PR per Constitution Principle IX
- **Plugins submodule prerequisite** (M5-M13): ensure `plugins/` is initialized
  before starting plugin milestone work (`git submodule update --init plugins`).
- Run `mvn spotless:apply` and `cd frontend && npm run format` before every PR
- Use BOTH flags when skipping tests: `-DskipTests -Dmaven.test.skip=true`
- Run E2E tests individually during development:
  `npm run cy:run -- --spec "cypress/e2e/{test}.cy.js"`

### Plugin Submodule

The `plugins/` directory is a git submodule from
`I-TECH-UW/openelisglobal-plugins`. **All new analyzer plugins MUST be built as
external JARs in this submodule** per `docs/analyzer.md`.

**Existing plugins** (on develop branch): Mindray, SysmexXN-L, GeneXpert
(ASTM/HL7/File), QuantStudio3, Cobas, FACS, Sysmex variants, Weber.

**New plugins to build in `plugins/analyzers/`** (M9-M13):

- `HoribaPentra60/` — Horiba Pentra 60 (M9, ASTM/RS232)
- `HoribaMicros60/` — Horiba Micros 60 (M10, ASTM/RS232)
- `StagoSTart4/` — Stago STart 4 (M11, ASTM/HL7)
- `AbbottArchitect/` — Abbott Architect (M12, HL7)
- `FluoroCyclerXT/` — Hain FluoroCycler XT (M13, File)

> **⚠️ ARCHITECTURE RULE**: Do NOT create analyzer classes in `src/main/java/`.
> All analyzers use external plugin JARs. See `docs/analyzer.md`.

---

**Tasks Generated**: 2026-01-27 | **Updated**: 2026-02-07 (completion
annotations for M6, M8; traceability for additional merged PRs) **Total Tasks**:
~304 **Test Tasks**: 66 (22%) **Task ID Range**: T001-T314 (with T0##a/b
additions) **Milestones**: 19 (M0-M18) **Parallel Milestones**: M0-M4 (all),
M9-M13 **Contract Deadline**: 2026-02-28

---

## PR Traceability Log

Merged PRs into `demo/madagascar` not covered by inline milestone annotations:

| PR                                                              | Title                                                                           | Merged     | Scope                |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------- | ---------- | -------------------- |
| [#2683](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2683) | feat(011): Add Madagascar analyzer dashboard fixtures and test infrastructure   | 2026-02-07 | Fixtures, test infra |
| [#2694](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2694) | Sidenav revamp: analyzer workflow focus                                         | 2026-02-04 | UI/UX                |
| [#2714](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2714) | M4: File Watcher + CSV Analyzer Support                                         | 2026-02-06 | M4 supplement        |
| [#2718](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2718) | feat(analyzer): Enhance HL7 endpoint with Universal Bridge header support (M2b) | 2026-02-06 | M2 supplement        |
| [#2726](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2726) | feat: M7 Message Normalizer — unified routing pipeline                          | 2026-02-06 | Bridge M7            |
| [#2731](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2731) | feat: M8 Observability milestone for analyzer bridge                            | 2026-02-07 | Bridge M8            |
| [#2732](https://github.com/DIGI-UW/OpenELIS-Global-2/pull/2732) | test(011): Migrate analyzer E2E tests to Playwright                             | 2026-02-07 | E2E tests            |

**Submodule rename**: `tools/astm-http-bridge` →
`tools/openelis-analyzer-bridge` (commit b71bdbfc6 on `demo/madagascar`)

---

## Plugin System Unification (PR #2802)

**Branch**: `fix/011-sync-remediation` ->
`feat/011-madagascar-analyzer-integration` **Status**: COMPLETE **All
milestones**: M19-M22

### [P] M19: Routing Unification (1 day)

- [x] T300 [M19] Remove type-pattern matching Stage 2 from
      InstanceAwareAnalyzerRouter
- [x] T301 [M19] Write 8 unit tests for 2-stage router (IP match + Plugin)
- [x] T302 [M19] Verify no `findMatchingType()` calls remain

### M20: Sync Remediation — Gaps 1-4 (2 days)

- [x] T303 [M20] Gap 1: Orphan deactivation at startup (later revised in M21)
- [x] T304 [M20] Gap 2: Legacy linking — linkLegacyAnalyzersToTypes() in
      PluginRegistryService
- [x] T305 [M20] Gap 3: REST pluginLoaded — per-request JAR availability check
- [x] T306 [M20] Gap 4: UI plugin health indicators in analyzer list
- [x] T307 [M20] Write PluginRegistrySyncTest for Gaps 1-3

### M21: Table Merge — analyzer_configuration into analyzer (3 days)

- [x] T308 [M21] Liquibase migration: add columns, migrate data, drop
      analyzer_configuration
- [x] T309 [M21] Entity changes: Analyzer gains ip_address, port, status,
      identifier_pattern, etc.
- [x] T310 [M21] Delete AnalyzerConfiguration, AnalyzerConfigurationDAO,
      AnalyzerConfigurationService (5 files)
- [x] T311 [M21] Move query methods to AnalyzerDAO/AnalyzerService
      (getByIpAddress, findByIdentifierPatternMatch)
- [x] T312 [M21] Update 16 consumer files: AnalyzerConfiguration -> Analyzer
      type changes
- [x] T313 [M21] Update InstanceAwareAnalyzerRouter: IP lookup via
      analyzerService.getByIpAddress()
- [x] T314 [M21] Update AnalyzerRestController: single-table reads/writes,
      remove config service
- [x] T315 [M21] Remove orphan deactivation/reactivation from
      PluginRegistryService (Phase 4)
- [x] T316 [M21] Update/rewrite all test files for merged model (8 test files)
- [x] T317 [M21] Fix stale DELETE FROM analyzer_configuration in 5 integration
      tests
- [x] T318 [M21] Fix FK ordering bug in AnalyzerRestControllerTest cleanup
- [x] T319 [M21] Fix OptimisticLockException in
      AnalyzerFieldMappingServiceIntegrationTest
- [x] T320 [M21] Verify 431 backend tests pass

### M22: UI Plugin Flow — Phase 5 (1 day)

- [x] T321 [M22] Add pluginTypeId field to AnalyzerForm.java
- [x] T322 [M22] Wire pluginTypeId in AnalyzerRestController
      create/update/response
- [x] T323 [M22] Frontend: conditional rendering — generic plugins show
      identifierPattern + default configs
- [x] T324 [M22] Add i18n keys (en.json + fr.json) for identifierPattern
- [x] T325 [M22] Update AnalyzerForm.defaultConfigs.test for conditional
      dropdown
- [x] T326 [M22] Update InstanceAwareAnalyzerRouterTest for merged model
      (stacked PR fix)
