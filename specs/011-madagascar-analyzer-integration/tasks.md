# Tasks: Madagascar Analyzer Integration

**Feature**: 011-madagascar-analyzer-integration **Input**: Design documents
from `/specs/011-madagascar-analyzer-integration/` **Prerequisites**: plan.md
(required), spec.md (required), data-model.md, contracts/, research.md,
quickstart.md **Contract Deadline**: 2026-02-28 (37 days)

**Organization**: Tasks are grouped by **Milestone** per Constitution Principle
IX. Each milestone = 1 PR.

**Tests**: **MANDATORY** per Constitution Principle V (TDD). Tests appear before
implementation tasks.

---

## Format: `[ID] [P?] [M#] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete
  tasks)
- **[M#]**: Which milestone this task belongs to (M1, M2, M3, etc.)
- Include exact file paths in descriptions

---

## Milestone Dependency Graph

```
Phase 1: Protocol Adapters (Week 1-2) - Parallel Foundation
┌─────────────────────────────────────────────────────────────┐
│  [P] M1: HL7 Adapter     [P] M2: RS232 Adapter              │
│  [P] M3: File Adapter    [P] M4: Simulator HL7              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
Phase 2: Plugin Validation (Week 2-3)
┌─────────────────────────────────────────────────────────────┐
│  M5: Mindray (→M1,M2)    M6: Sysmex (→M1)                   │
│  M7: GeneXpert (→M1,M3)  M8: QuantStudio (→M3)              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
Phase 3: New Plugins (Week 3-4) - Parallel
┌─────────────────────────────────────────────────────────────┐
│  [P] M9: Pentra (→M2)     [P] M10: Micros (→M2)             │
│  [P] M11: Stago (→M1,M2)  [P] M12: Abbott (→M1)             │
│  [P] M13: FluoroCycler (→M3)                                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
Phase 4: Integration (Week 4-5)
┌─────────────────────────────────────────────────────────────┐
│  M14: Order Export (→M5-M13)                                │
│  M15: Metadata Form (→M14)    M16: Simulator Complete (→M4) │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
Phase 5: Validation (Week 5-6)
┌─────────────────────────────────────────────────────────────┐
│  M17: E2E & Lab Validation (→M14,M15,M16)                   │
└─────────────────────────────────────────────────────────────┘
```

---

## [P] M1: HL7 v2.x Protocol Adapter (3 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m1-hl7-adapter` **Goal**:
Parse HL7 ORU^R01 results and generate HL7 ORM^O01 orders **User Stories**: US-1
(HL7 Analyzer Results Import) **Depends On**: None (foundation milestone)

**Acceptance Criteria**:

1. HL7 ORU^R01 messages parse correctly (patient ID, test codes, results)
2. HL7 ORM^O01 messages generate correctly for order export
3. MSH segment sender ID extracted for analyzer identification
4. Unmapped fields create error records in dashboard
5. Unit test coverage >80%

### Setup for M1

- [ ] T001 [M1] Create branch
      `feat/011-madagascar-analyzer-integration-m1-hl7-adapter` from
      `demo/madagascar`
- [ ] T002 [M1] Add HAPI HL7 v2 dependency to `pom.xml`
      (ca.uhn.hapi:hapi-base:2.4, ca.uhn.hapi:hapi-structures-v251:2.4)

### Tests for M1 (MANDATORY - Write FIRST, ensure they FAIL)

- [ ] T003 [P] [M1] Unit test for HL7MessageService in
      `src/test/java/org/openelisglobal/analyzer/service/HL7MessageServiceTest.java`
- [ ] T004 [P] [M1] Unit test for HL7AnalyzerReader in
      `src/test/java/org/openelisglobal/analyzerimport/analyzerreaders/HL7AnalyzerReaderTest.java`
- [ ] T005 [P] [M1] Create test HL7 message fixtures in
      `src/test/resources/testdata/hl7/mindray-cbc-result.hl7`
- [ ] T006 [P] [M1] Create test HL7 message fixtures in
      `src/test/resources/testdata/hl7/sysmex-result.hl7`
- [ ] T007 [P] [M1] Create test HL7 ORM^O01 expected output in
      `src/test/resources/testdata/hl7/expected-order.hl7`

### Implementation for M1

- [ ] T008 [M1] Create HL7AnalyzerReader in
      `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/HL7AnalyzerReader.java`
      (extends AnalyzerReader, uses HAPI PipeParser)
- [ ] T009 [M1] Create HL7MessageService interface in
      `src/main/java/org/openelisglobal/analyzer/service/HL7MessageService.java`
- [ ] T010 [M1] Create HL7MessageServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/HL7MessageServiceImpl.java`
      (ORU^R01 parsing, ORM^O01 generation)
- [ ] T011 [M1] Implement field extraction for ORU^R01 (PID segment → patient,
      OBR segment → test, OBX segment → results)
- [ ] T012 [M1] Implement MSH segment parsing for analyzer identification
      (sending application, sending facility)
- [ ] T013 [M1] Create MappingAwareHL7AnalyzerLineInserter wrapper in
      `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/MappingAwareHL7AnalyzerLineInserter.java`
- [ ] T014 [M1] Integrate with existing FieldMappingService for test code
      mapping
- [ ] T015 [M1] Add i18n keys for HL7-related messages in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Finalization for M1

- [ ] T016 [M1] Verify all unit tests pass (`mvn test -Dtest="*HL7*"`)
- [ ] T017 [M1] Run Spotless formatting (`mvn spotless:apply`)
- [ ] T018 [M1] Create PR
      `feat/011-madagascar-analyzer-integration-m1-hl7-adapter` →
      `demo/madagascar`

**Checkpoint**: HL7 parsing and generation unit tests pass with >80% coverage

---

## [P] M2: RS232 Serial Communication Adapter (3 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m2-serial-adapter`
**Goal**: Serial port configuration and ASTM message reception via RS232 **User
Stories**: US-3 (RS232 Serial Analyzer Connection) **Depends On**: None
(parallel with M1)

**Acceptance Criteria**:

1. Serial port configuration stored in database
2. Connection established with correct parameters
3. ASTM messages received over serial processed by existing infrastructure
4. Connection status tracked (connected/disconnected/error)
5. Graceful handling of cable disconnection

### Setup for M2

- [ ] T019 [M2] Create branch
      `feat/011-madagascar-analyzer-integration-m2-serial-adapter` from
      `demo/madagascar`
- [ ] T020 [M2] Add jSerialComm dependency to `pom.xml`
      (com.fazecast:jSerialComm:2.10.4)
- [ ] T021 [M2] Create Liquibase changeset
      `src/main/resources/liquibase/3.8.x.x/011-003-create-serial-port-configuration-table.xml`

### Tests for M2 (MANDATORY - Write FIRST)

- [ ] T022 [P] [M2] ORM validation test for SerialPortConfiguration in
      `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [ ] T023 [P] [M2] Unit test for SerialPortService in
      `src/test/java/org/openelisglobal/analyzer/service/SerialPortServiceTest.java`
- [ ] T024 [P] [M2] Unit test for SerialPortConfigurationDAO in
      `src/test/java/org/openelisglobal/analyzer/dao/SerialPortConfigurationDAOTest.java`
- [ ] T025 [P] [M2] Create DBUnit test fixture in
      `src/test/resources/testdata/serial-port-configuration.xml`

### Implementation for M2

- [ ] T026 [M2] Create SerialPortConfiguration entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/SerialPortConfiguration.java`
- [ ] T027 [M2] Create StopBits, Parity, FlowControl enums in
      `src/main/java/org/openelisglobal/analyzer/valueholder/`
- [ ] T028 [M2] Create SerialPortConfigurationDAO interface in
      `src/main/java/org/openelisglobal/analyzer/dao/SerialPortConfigurationDAO.java`
- [ ] T029 [M2] Create SerialPortConfigurationDAOImpl in
      `src/main/java/org/openelisglobal/analyzer/dao/SerialPortConfigurationDAOImpl.java`
- [ ] T030 [M2] Create SerialPortService interface in
      `src/main/java/org/openelisglobal/analyzer/service/SerialPortService.java`
- [ ] T031 [M2] Create SerialPortServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/SerialPortServiceImpl.java`
      (connection lifecycle, jSerialComm integration)
- [ ] T032 [M2] Create SerialAnalyzerReader in
      `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/SerialAnalyzerReader.java`
- [ ] T033 [M2] Implement connection status tracking with event callbacks
- [ ] T034 [M2] Add serial port REST endpoints in
      `src/main/java/org/openelisglobal/analyzer/controller/SerialPortRestController.java`
- [ ] T035 [P] [M2] Add i18n keys for serial config messages in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend for M2

- [ ] T036 [P] [M2] Jest unit test for SerialConfiguration in
      `frontend/src/components/analyzers/SerialConfiguration/__tests__/SerialConfiguration.test.jsx`
- [ ] T037 [M2] Create SerialConfiguration React component in
      `frontend/src/components/analyzers/SerialConfiguration/SerialConfiguration.jsx`
- [ ] T038 [M2] Create serialService.js API client in
      `frontend/src/services/serialService.js`

### Finalization for M2

- [ ] T039 [M2] Update dev.docker-compose.yml with device mapping example for
      `/dev/ttyUSB0`
- [ ] T040 [M2] Verify all unit tests pass
- [ ] T041 [M2] Run Spotless formatting (`mvn spotless:apply`) and frontend
      formatting (`cd frontend && npm run format`)
- [ ] T042 [M2] Create PR
      `feat/011-madagascar-analyzer-integration-m2-serial-adapter` →
      `demo/madagascar`

**Checkpoint**: Serial config entity persists, service can open/close virtual
serial ports

---

## [P] M3: File-Based Import Adapter (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m3-file-adapter` **Goal**:
Directory watcher and CSV/TXT file parsing **User Stories**: US-4 (File-Based
PCR Thermocycler Integration) **Depends On**: None (parallel with M1, M2)

**Acceptance Criteria**:

1. Files detected within 60 seconds of creation
2. CSV rows mapped to results via configured column mappings
3. Processed files moved to archive directory
4. Malformed files moved to error directory with log entry
5. Duplicate detection warns before creating new results

### Setup for M3

- [ ] T043 [M3] Create branch
      `feat/011-madagascar-analyzer-integration-m3-file-adapter` from
      `demo/madagascar`
- [ ] T044 [M3] Add Apache Commons CSV dependency to `pom.xml`
      (org.apache.commons:commons-csv:1.10.0) if not present
- [ ] T045 [M3] Create Liquibase changeset
      `src/main/resources/liquibase/3.8.x.x/011-004-create-file-import-configuration-table.xml`

### Tests for M3 (MANDATORY - Write FIRST)

- [ ] T046 [P] [M3] ORM validation test for FileImportConfiguration in
      `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [ ] T047 [P] [M3] Unit test for FileImportService in
      `src/test/java/org/openelisglobal/analyzer/service/FileImportServiceTest.java`
- [ ] T048 [P] [M3] Unit test for FileAnalyzerReader in
      `src/test/java/org/openelisglobal/analyzerimport/analyzerreaders/FileAnalyzerReaderTest.java`
- [ ] T049 [P] [M3] Create test CSV fixtures in
      `src/test/resources/testdata/files/quantstudio-results.csv`
- [ ] T050 [P] [M3] Create malformed CSV fixture in
      `src/test/resources/testdata/files/malformed-results.csv`

### Implementation for M3

- [ ] T051 [M3] Create FileImportConfiguration entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/FileImportConfiguration.java`
- [ ] T052 [M3] Create FileImportConfigurationDAO in
      `src/main/java/org/openelisglobal/analyzer/dao/FileImportConfigurationDAO.java`
      and `FileImportConfigurationDAOImpl.java`
- [ ] T053 [M3] Create FileImportService interface in
      `src/main/java/org/openelisglobal/analyzer/service/FileImportService.java`
- [ ] T054 [M3] Create FileImportServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/FileImportServiceImpl.java`
      (WatchService integration)
- [ ] T055 [M3] Create FileAnalyzerReader in
      `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/FileAnalyzerReader.java`
      (CSV parsing with Commons CSV)
- [ ] T056 [M3] Implement file archival (move to archive directory on success)
- [ ] T057 [M3] Implement error handling (move to error directory on failure
      with log)
- [ ] T058 [M3] Implement duplicate detection (sample ID + test + timestamp)
- [ ] T059 [M3] Add file import REST endpoints in
      `src/main/java/org/openelisglobal/analyzer/controller/FileImportRestController.java`
- [ ] T060 [P] [M3] Add i18n keys for file import messages in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend for M3

- [ ] T061 [P] [M3] Jest unit test for FileImportConfiguration in
      `frontend/src/components/analyzers/FileImportConfiguration/__tests__/FileImportConfiguration.test.jsx`
- [ ] T062 [M3] Create FileImportConfiguration React component in
      `frontend/src/components/analyzers/FileImportConfiguration/FileImportConfiguration.jsx`
- [ ] T063 [M3] Create fileImportService.js API client in
      `frontend/src/services/fileImportService.js`

### Finalization for M3

- [ ] T064 [M3] Verify all unit tests pass
- [ ] T065 [M3] Run Spotless and frontend formatting
- [ ] T066 [M3] Create PR
      `feat/011-madagascar-analyzer-integration-m3-file-adapter` →
      `demo/madagascar`

**Checkpoint**: File detection + CSV parsing tests pass, archive/error workflow
works

---

## [P] M4: Multi-Protocol Analyzer Simulator (3 days)

**Branch**:
`feat/011-madagascar-analyzer-integration-m4-simulator-multiprotocol` **Goal**:
Expand astm-mock-server to support HL7, RS232, and file-based protocols **User
Stories**: US-9 (Analyzer Simulator for Testing) **Depends On**: None (parallel
with M1-M3)

**Scope**: Extend astm-mock-server to cover 80%+ of 12 analyzers BEFORE
milestone implementation, enabling developers to test M1-M3 adapters and M5-M13
plugins without physical hardware.

**IMPORTANT**: This expands the **Python astm-mock-server** (testing simulator),
NOT the Java astm-http-bridge (production adapter). See plan.md Tool
Architecture section for distinction.

**Acceptance Criteria**:

1. Protocol abstraction layer supports ASTM, HL7, RS232, and File protocols
2. Simulator generates valid HL7 ORU^R01 messages
3. Virtual serial port simulation via socat (Linux)
4. File-based result generation (CSV/TXT)
5. Templates for majority of 12 contract analyzers
6. HTTP API mode for CI/CD integration
7. Messages received and processed correctly by OpenELIS

### Setup for M4

- [ ] T067 [M4] Create branch
      `feat/011-madagascar-analyzer-integration-m4-simulator-multiprotocol` from
      `demo/madagascar`
- [ ] T068 [M4] Create `tools/astm-mock-server/protocols/` directory structure
- [ ] T069 [M4] Create `tools/astm-mock-server/templates/` directory structure
- [ ] T070 [M4] Add pyserial dependency to
      `tools/astm-mock-server/requirements.txt`

### Core Architecture for M4

- [ ] T071 [M4] Create protocol abstraction layer base class in
      `tools/astm-mock-server/protocols/__init__.py` and
      `tools/astm-mock-server/protocols/base_handler.py`
- [ ] T072 [M4] Refactor existing ASTM code into ASTMHandler in
      `tools/astm-mock-server/protocols/astm_handler.py`
- [ ] T073 [M4] Create template schema in
      `tools/astm-mock-server/templates/schema.json`

### HL7 Protocol Implementation (Priority 1 - enables M5-M7, M11-M12)

- [ ] T074 [M4] Implement HL7Handler with ORU^R01 generation in
      `tools/astm-mock-server/protocols/hl7_handler.py`
- [ ] T075 [M4] Create Mindray BC-5380 template in
      `tools/astm-mock-server/templates/mindray_bc5380.json`
- [ ] T076 [M4] Create Sysmex XN template in
      `tools/astm-mock-server/templates/sysmex_xn.json`
- [ ] T077 [M4] Create Abbott Architect HL7 template in
      `tools/astm-mock-server/templates/abbott_architect_hl7.json`
- [ ] T078 [M4] Add HL7 HTTP endpoint `/simulate/hl7/{analyzer}` for CI/CD

### RS232 Protocol Implementation (Priority 2 - enables M5, M9-M11)

- [ ] T079 [M4] Implement SerialHandler with virtual serial ports (socat) in
      `tools/astm-mock-server/protocols/serial_handler.py`
- [ ] T080 [M4] Create Horiba Pentra 60 template in
      `tools/astm-mock-server/templates/horiba_pentra60.json`
- [ ] T081 [M4] Create Horiba Micros 60 template in
      `tools/astm-mock-server/templates/horiba_micros60.json`
- [ ] T082 [M4] Add serial simulation mode (`--serial-port` flag)

### File Protocol Implementation (Priority 3 - enables M8, M13)

- [ ] T083 [M4] Implement FileHandler with CSV/TXT generation in
      `tools/astm-mock-server/protocols/file_handler.py`
- [ ] T084 [M4] Create QuantStudio 7 Flex template in
      `tools/astm-mock-server/templates/quantstudio7.json`
- [ ] T085 [M4] Create Hain FluoroCycler XT template in
      `tools/astm-mock-server/templates/hain_fluorocycler.json`
- [ ] T086 [M4] Add file generation mode (`--generate-files` flag)

### Additional Analyzer Templates

- [ ] T087 [M4] Create Mindray BS-360E template in
      `tools/astm-mock-server/templates/mindray_bs360e.json`
- [ ] T088 [M4] Create Mindray BA-88A template in
      `tools/astm-mock-server/templates/mindray_ba88a.json`
- [ ] T089 [M4] Create GeneXpert template in
      `tools/astm-mock-server/templates/genexpert.json`
- [ ] T090 [M4] Create Stago STart 4 template in
      `tools/astm-mock-server/templates/stago_start4.json`

### Testing & Documentation for M4

- [ ] T091 [P] [M4] Unit tests for all protocol handlers in
      `tools/astm-mock-server/test_protocols.py`
- [ ] T092 [M4] Integration test: HL7 Simulator → OpenELIS reception
- [ ] T093 [M4] Integration test: Serial Simulator → OpenELIS reception
- [ ] T094 [M4] Integration test: File Simulator → OpenELIS import
- [ ] T095 [M4] Update `tools/astm-mock-server/README.md` with multi-protocol
      usage

### Finalization for M4

- [ ] T096 [M4] Verify all protocol handlers generate valid messages
- [ ] T097 [M4] Verify backward compatibility with existing ASTM mode
- [ ] T098 [M4] Create PR
      `feat/011-madagascar-analyzer-integration-m4-simulator-multiprotocol` →
      `demo/madagascar`

**Checkpoint**: Multi-protocol simulator covers 80%+ of 12 analyzers, enabling
CI/CD testing without physical hardware

---

## M5: Mindray Plugin Validation (3 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m5-mindray-validation`
**Goal**: Validate existing Mindray plugin with 4 analyzers **User Stories**:
US-1, US-6 **Depends On**: M1 (HL7), M2 (RS232)

**Analyzers**: BC-5380, BS-360E, BC2000 (HL7), BA-88A (RS232)

**Acceptance Criteria**:

1. BC-5380 receives results via HL7 (simulator)
2. BS-360E receives results via HL7 (simulator)
3. BC2000 receives results via HL7 (simulator)
4. BA-88A receives results via RS232 (virtual serial)
5. Field mappings work with plugin + mapping system

### Setup for M5

- [ ] T099 [M5] Create branch
      `feat/011-madagascar-analyzer-integration-m5-mindray-validation` from
      `demo/madagascar`
- [ ] T100 [M5] Merge M1 and M2 into branch (or ensure dependencies merged to
      `demo/madagascar`)

### Tests for M5 (MANDATORY)

- [ ] T101 [P] [M5] Integration test for Mindray BC-5380 HL7 in
      `src/test/java/org/openelisglobal/analyzer/mindray/MindrayBC5380IntegrationTest.java`
- [ ] T102 [P] [M5] Integration test for Mindray BA-88A RS232 in
      `src/test/java/org/openelisglobal/analyzer/mindray/MindrayBA88AIntegrationTest.java`
- [ ] T103 [P] [M5] Create HL7 test fixtures for BC-5380, BS-360E, BC2000 in
      `src/test/resources/testdata/hl7/mindray/`

### Implementation for M5

- [ ] T104 [M5] Verify existing Mindray plugin compatibility with
      HL7AnalyzerReader
- [ ] T105 [M5] Create MappingAwareMindrayAnalyzerLineInserter wrapper if needed
- [ ] T106 [M5] Configure RS232 parameters for BA-88A in test environment
- [ ] T107 [M5] Validate field mappings for all 4 Mindray analyzers
- [ ] T108 [M5] Document Mindray plugin + protocol adapter integration

### Finalization for M5

- [ ] T109 [M5] Verify all integration tests pass
- [ ] T110 [M5] Run Spotless formatting
- [ ] T111 [M5] Create PR
      `feat/011-madagascar-analyzer-integration-m5-mindray-validation` →
      `demo/madagascar`

**Checkpoint**: 4 Mindray analyzers receive results via appropriate protocols

---

## M6: Sysmex Plugin Validation (1 day)

**Branch**: `feat/011-madagascar-analyzer-integration-m6-sysmex-validation`
**Goal**: Validate existing SysmexXN-L plugin **User Stories**: US-1, US-6
**Depends On**: M1 (HL7)

**Analyzer**: Sysmex XN Series (HL7)

### Setup for M6

- [ ] T112 [M6] Create branch
      `feat/011-madagascar-analyzer-integration-m6-sysmex-validation` from
      `demo/madagascar`

### Tests for M6 (MANDATORY)

- [ ] T113 [M6] Integration test for Sysmex XN in
      `src/test/java/org/openelisglobal/analyzer/sysmex/SysmexXNIntegrationTest.java`
- [ ] T114 [M6] Create HL7 test fixture in
      `src/test/resources/testdata/hl7/sysmex-xn-result.hl7`

### Implementation for M6

- [ ] T115 [M6] Verify SysmexXN-L plugin compatibility with HL7AnalyzerReader
- [ ] T116 [M6] Test override mappings take precedence over plugin defaults
- [ ] T117 [M6] Document Sysmex plugin integration

### Finalization for M6

- [ ] T118 [M6] Verify integration tests pass
- [ ] T119 [M6] Create PR
      `feat/011-madagascar-analyzer-integration-m6-sysmex-validation` →
      `demo/madagascar`

**Checkpoint**: Sysmex XN receives results via HL7, override mappings work

---

## M7: GeneXpert Plugin Validation (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m7-genexpert-validation`
**Goal**: Validate 3 GeneXpert plugin variants **User Stories**: US-6 **Depends
On**: M1 (HL7), M3 (File)

**Analyzer**: Cepheid GeneXpert (ASTM, HL7, File variants)

### Setup for M7

- [ ] T120 [M7] Create branch
      `feat/011-madagascar-analyzer-integration-m7-genexpert-validation` from
      `demo/madagascar`

### Tests for M7 (MANDATORY)

- [ ] T121 [P] [M7] Integration test for GeneXpert HL7 in
      `src/test/java/org/openelisglobal/analyzer/genexpert/GeneXpertHL7IntegrationTest.java`
- [ ] T122 [P] [M7] Integration test for GeneXpert File in
      `src/test/java/org/openelisglobal/analyzer/genexpert/GeneXpertFileIntegrationTest.java`
- [ ] T123 [P] [M7] Create HL7 test fixture in
      `src/test/resources/testdata/hl7/genexpert-result.hl7`
- [ ] T124 [P] [M7] Create file test fixture in
      `src/test/resources/testdata/files/genexpert-results.csv`

### Implementation for M7

- [ ] T125 [M7] Verify GeneXpert ASTM plugin works (existing)
- [ ] T126 [M7] Verify GeneXpertHL7 plugin with HL7AnalyzerReader
- [ ] T127 [M7] Verify GeneXpertFile plugin with FileAnalyzerReader
- [ ] T128 [M7] Test all 3 variants can coexist
- [ ] T129 [M7] Document GeneXpert multi-protocol support

### Finalization for M7

- [ ] T130 [M7] Verify all integration tests pass
- [ ] T131 [M7] Create PR
      `feat/011-madagascar-analyzer-integration-m7-genexpert-validation` →
      `demo/madagascar`

**Checkpoint**: GeneXpert works via ASTM, HL7, and File

---

## M8: QuantStudio Adaptation (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m8-quantstudio-adapt`
**Goal**: Adapt QuantStudio3 plugin for QuantStudio 7 Flex **User Stories**:
US-4, US-6 **Depends On**: M3 (File)

**Analyzer**: Thermo Fisher QuantStudio 7 Flex (File-based)

### Setup for M8

- [ ] T132 [M8] Create branch
      `feat/011-madagascar-analyzer-integration-m8-quantstudio-adapt` from
      `demo/madagascar`

### Tests for M8 (MANDATORY)

- [ ] T133 [M8] Integration test for QuantStudio 7 Flex in
      `src/test/java/org/openelisglobal/analyzer/quantstudio/QuantStudio7FlexIntegrationTest.java`
- [ ] T134 [M8] Create CSV test fixture for QuantStudio 7 Flex format in
      `src/test/resources/testdata/files/quantstudio7-flex-results.csv`
- [ ] T135 [M8] Backward compatibility test for QuantStudio 3 in
      `src/test/java/org/openelisglobal/analyzer/quantstudio/QuantStudio3BackwardCompatTest.java`

### Implementation for M8

- [ ] T136 [M8] Analyze QuantStudio 7 Flex CSV format differences from
      QuantStudio 3
- [ ] T137 [M8] Modify QuantStudio plugin or create FileImportConfiguration for
      column differences
- [ ] T138 [M8] Ensure backward compatibility with QuantStudio 3
- [ ] T139 [M8] Document QuantStudio adaptation

### Finalization for M8

- [ ] T140 [M8] Verify all integration tests pass
- [ ] T141 [M8] Create PR
      `feat/011-madagascar-analyzer-integration-m8-quantstudio-adapt` →
      `demo/madagascar`

**Checkpoint**: QuantStudio 7 Flex and QuantStudio 3 both work

---

## [P] M9: Horiba Pentra 60 Plugin (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m9-plugin-pentra`
**Goal**: Build new Horiba Pentra 60 plugin **User Stories**: US-3 **Depends
On**: M2 (RS232)

**Analyzer**: Horiba ABX Pentra 60 (ASTM over RS232)

### Setup for M9

- [ ] T142 [M9] Create branch
      `feat/011-madagascar-analyzer-integration-m9-plugin-pentra` from
      `demo/madagascar`

### Tests for M9 (MANDATORY)

- [ ] T143 [P] [M9] Unit test for Pentra60AnalyzerLineInserter in
      `src/test/java/org/openelisglobal/analyzer/pentra/Pentra60AnalyzerLineInserterTest.java`
- [ ] T144 [P] [M9] Create ASTM test fixtures for Pentra 60 in
      `src/test/resources/testdata/astm/pentra60-results.txt`

### Implementation for M9

- [ ] T145 [M9] Create Pentra60Analyzer plugin class in
      `src/main/java/org/openelisglobal/analyzer/pentra/Pentra60Analyzer.java`
- [ ] T146 [M9] Create Pentra60AnalyzerLineInserter in
      `src/main/java/org/openelisglobal/analyzer/pentra/Pentra60AnalyzerLineInserter.java`
- [ ] T147 [M9] Implement ASTM message parsing for Pentra 60 format
- [ ] T148 [M9] Integrate with MappingAware wrapper pattern
- [ ] T149 [M9] Document Pentra 60 plugin and RS232 configuration

### Finalization for M9

- [ ] T150 [M9] Verify unit tests pass
- [ ] T151 [M9] Create PR
      `feat/011-madagascar-analyzer-integration-m9-plugin-pentra` →
      `demo/madagascar`

**Checkpoint**: Pentra 60 results import via RS232

---

## [P] M10: Horiba Micros 60 Plugin (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m10-plugin-micros`
**Goal**: Build new Horiba Micros 60 plugin **User Stories**: US-3 **Depends
On**: M2 (RS232)

**Analyzer**: Horiba ABX Micros 60 (ASTM over RS232)

### Setup for M10

- [ ] T152 [M10] Create branch
      `feat/011-madagascar-analyzer-integration-m10-plugin-micros` from
      `demo/madagascar`

### Tests for M10 (MANDATORY)

- [ ] T153 [P] [M10] Unit test for Micros60AnalyzerLineInserter in
      `src/test/java/org/openelisglobal/analyzer/micros/Micros60AnalyzerLineInserterTest.java`
- [ ] T154 [P] [M10] Create ASTM test fixtures for Micros 60 in
      `src/test/resources/testdata/astm/micros60-results.txt`

### Implementation for M10

- [ ] T155 [M10] Create Micros60Analyzer plugin class in
      `src/main/java/org/openelisglobal/analyzer/micros/Micros60Analyzer.java`
- [ ] T156 [M10] Create Micros60AnalyzerLineInserter in
      `src/main/java/org/openelisglobal/analyzer/micros/Micros60AnalyzerLineInserter.java`
- [ ] T157 [M10] Implement ASTM message parsing for Micros 60 format
- [ ] T158 [M10] Integrate with MappingAware wrapper pattern
- [ ] T159 [M10] Document Micros 60 plugin

### Finalization for M10

- [ ] T160 [M10] Verify unit tests pass
- [ ] T161 [M10] Create PR
      `feat/011-madagascar-analyzer-integration-m10-plugin-micros` →
      `demo/madagascar`

**Checkpoint**: Micros 60 results import via RS232

---

## [P] M11: Stago STart 4 Plugin (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m11-plugin-stago`
**Goal**: Build new Stago STart 4 plugin **User Stories**: US-1, US-3 **Depends
On**: M1 (HL7), M2 (RS232)

**Analyzer**: Stago STart 4 (ASTM/HL7 over RS232/Network)

### Setup for M11

- [ ] T162 [M11] Create branch
      `feat/011-madagascar-analyzer-integration-m11-plugin-stago` from
      `demo/madagascar`

### Tests for M11 (MANDATORY)

- [ ] T163 [P] [M11] Unit test for StagoSTart4AnalyzerLineInserter in
      `src/test/java/org/openelisglobal/analyzer/stago/StagoSTart4AnalyzerLineInserterTest.java`
- [ ] T164 [P] [M11] Create test fixtures for Stago in
      `src/test/resources/testdata/stago/`

### Implementation for M11

- [ ] T165 [M11] Create StagoSTart4Analyzer plugin class in
      `src/main/java/org/openelisglobal/analyzer/stago/StagoSTart4Analyzer.java`
- [ ] T166 [M11] Create StagoSTart4AnalyzerLineInserter in
      `src/main/java/org/openelisglobal/analyzer/stago/StagoSTart4AnalyzerLineInserter.java`
- [ ] T167 [M11] Support both ASTM (RS232) and HL7 (Network) modes
- [ ] T168 [M11] Integrate with MappingAware wrapper pattern
- [ ] T169 [M11] Document Stago plugin with dual-protocol support

### Finalization for M11

- [ ] T170 [M11] Verify unit tests pass
- [ ] T171 [M11] Create PR
      `feat/011-madagascar-analyzer-integration-m11-plugin-stago` →
      `demo/madagascar`

**Checkpoint**: Stago STart 4 works via RS232 or Network

---

## [P] M12: Abbott Architect Plugin (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m12-plugin-abbott`
**Goal**: Build new Abbott Architect plugin **User Stories**: US-1 **Depends
On**: M1 (HL7)

**Analyzer**: Abbott Architect (HL7 over RS232/Network)

### Setup for M12

- [ ] T172 [M12] Create branch
      `feat/011-madagascar-analyzer-integration-m12-plugin-abbott` from
      `demo/madagascar`

### Tests for M12 (MANDATORY)

- [ ] T173 [P] [M12] Unit test for AbbottArchitectAnalyzerLineInserter in
      `src/test/java/org/openelisglobal/analyzer/abbott/AbbottArchitectAnalyzerLineInserterTest.java`
- [ ] T174 [P] [M12] Create HL7 test fixtures for Abbott Architect in
      `src/test/resources/testdata/hl7/abbott-architect-result.hl7`

### Implementation for M12

- [ ] T175 [M12] Create AbbottArchitectAnalyzer plugin class in
      `src/main/java/org/openelisglobal/analyzer/abbott/AbbottArchitectAnalyzer.java`
- [ ] T176 [M12] Create AbbottArchitectAnalyzerLineInserter in
      `src/main/java/org/openelisglobal/analyzer/abbott/AbbottArchitectAnalyzerLineInserter.java`
- [ ] T177 [M12] Implement HL7 message parsing for Abbott format
- [ ] T178 [M12] Integrate with MappingAware wrapper pattern
- [ ] T179 [M12] Document Abbott plugin

### Finalization for M12

- [ ] T180 [M12] Verify unit tests pass
- [ ] T181 [M12] Create PR
      `feat/011-madagascar-analyzer-integration-m12-plugin-abbott` →
      `demo/madagascar`

**Checkpoint**: Abbott Architect results import via HL7

---

## [P] M13: Hain FluoroCycler XT Plugin (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m13-plugin-fluorocycler`
**Goal**: Build new Hain FluoroCycler XT plugin **User Stories**: US-4 **Depends
On**: M3 (File)

**Analyzer**: Hain Lifescience FluoroCycler XT (File-based)

### Setup for M13

- [ ] T182 [M13] Create branch
      `feat/011-madagascar-analyzer-integration-m13-plugin-fluorocycler` from
      `demo/madagascar`

### Tests for M13 (MANDATORY)

- [ ] T183 [P] [M13] Unit test for FluoroCyclerXTAnalyzerLineInserter in
      `src/test/java/org/openelisglobal/analyzer/fluorocycler/FluoroCyclerXTAnalyzerLineInserterTest.java`
- [ ] T184 [P] [M13] Create CSV test fixtures for FluoroCycler in
      `src/test/resources/testdata/files/fluorocycler-results.csv`

### Implementation for M13

- [ ] T185 [M13] Create FluoroCyclerXTAnalyzer plugin class in
      `src/main/java/org/openelisglobal/analyzer/fluorocycler/FluoroCyclerXTAnalyzer.java`
- [ ] T186 [M13] Create FluoroCyclerXTAnalyzerLineInserter in
      `src/main/java/org/openelisglobal/analyzer/fluorocycler/FluoroCyclerXTAnalyzerLineInserter.java`
- [ ] T187 [M13] Implement CSV parsing for FluoroCycler format
- [ ] T188 [M13] Integrate with FileAnalyzerReader
- [ ] T189 [M13] Document FluoroCycler plugin

### Finalization for M13

- [ ] T190 [M13] Verify unit tests pass
- [ ] T191 [M13] Create PR
      `feat/011-madagascar-analyzer-integration-m13-plugin-fluorocycler` →
      `demo/madagascar`

**Checkpoint**: FluoroCycler XT CSV import works

---

## M14: Order Export Workflow (4 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m14-order-export`
**Goal**: Manual order export with status tracking **User Stories**: US-2 (Test
Order Export to Analyzers) **Depends On**: M5-M13 (all analyzers operational)

**Acceptance Criteria**:

1. Users can select pending orders and trigger export
2. Orders sent via appropriate protocol (ASTM/HL7)
3. Status tracked: pending → sent → acknowledged → results_received
4. Retry mechanism (3 attempts, exponential backoff)
5. Results automatically matched to exported orders
6. UI displays export status per analyzer

### Setup for M14

- [ ] T192 [M14] Create branch
      `feat/011-madagascar-analyzer-integration-m14-order-export` from
      `demo/madagascar`
- [ ] T193 [M14] Create Liquibase changeset
      `src/main/resources/liquibase/3.8.x.x/011-002-create-order-export-table.xml`

### Tests for M14 (MANDATORY)

- [ ] T194 [P] [M14] ORM validation test for OrderExport in
      `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [ ] T195 [P] [M14] Unit test for OrderExportService in
      `src/test/java/org/openelisglobal/analyzer/service/OrderExportServiceTest.java`
- [ ] T196 [P] [M14] Unit test for OrderExportDAO in
      `src/test/java/org/openelisglobal/analyzer/dao/OrderExportDAOTest.java`
- [ ] T197 [P] [M14] Controller test for OrderExportRestController in
      `src/test/java/org/openelisglobal/analyzer/controller/OrderExportRestControllerTest.java`
- [ ] T198 [P] [M14] Create DBUnit test fixture in
      `src/test/resources/testdata/order-export.xml`

### Backend Implementation for M14

- [ ] T199 [M14] Create OrderExport entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/OrderExport.java`
- [ ] T200 [M14] Create OrderExportStatus and MessageType enums in
      `src/main/java/org/openelisglobal/analyzer/valueholder/`
- [ ] T201 [M14] Create OrderExportDAO interface and impl in
      `src/main/java/org/openelisglobal/analyzer/dao/`
- [ ] T202 [M14] Create OrderExportService interface in
      `src/main/java/org/openelisglobal/analyzer/service/OrderExportService.java`
- [ ] T203 [M14] Create OrderExportServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/OrderExportServiceImpl.java`
- [ ] T204 [M14] Implement ASTM O-segment generation for order export
- [ ] T205 [M14] Implement HL7 ORM^O01 generation for order export (leverage M1
      HL7MessageService)
- [ ] T206 [M14] Implement retry mechanism with exponential backoff
- [ ] T207 [M14] Implement result matching (incoming results → exported orders)
- [ ] T208 [M14] Create OrderExportRestController in
      `src/main/java/org/openelisglobal/analyzer/controller/OrderExportRestController.java`
- [ ] T208a [M14] Implement RBAC permission check for order export
      (LAB_SUPERVISOR role minimum) in OrderExportServiceImpl
- [ ] T208b [M14] Add audit trail logging for order export actions in
      OrderExportServiceImpl
- [ ] T209 [P] [M14] Add i18n keys for order export messages in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend Implementation for M14

- [ ] T210 [P] [M14] Jest unit test for OrderExportList in
      `frontend/src/components/analyzers/OrderExport/__tests__/OrderExportList.test.jsx`
- [ ] T211 [P] [M14] Jest unit test for OrderExportModal in
      `frontend/src/components/analyzers/OrderExport/__tests__/OrderExportModal.test.jsx`
- [ ] T212 [M14] Create OrderExportList React component in
      `frontend/src/components/analyzers/OrderExport/OrderExportList.jsx`
- [ ] T213 [M14] Create OrderExportModal React component in
      `frontend/src/components/analyzers/OrderExport/OrderExportModal.jsx`
- [ ] T214 [M14] Create orderExportService.js API client in
      `frontend/src/services/orderExportService.js`
- [ ] T215 [M14] Integrate OrderExport components into analyzer dashboard

### Finalization for M14

- [ ] T216 [M14] Verify all unit and integration tests pass
- [ ] T217 [M14] Run Spotless and frontend formatting
- [ ] T218 [M14] Create PR
      `feat/011-madagascar-analyzer-integration-m14-order-export` →
      `demo/madagascar`

**Checkpoint**: Order export UI works, orders sent to analyzers, status tracked

---

## M15: Enhanced Instrument Metadata Form (3 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m15-metadata-form`
**Goal**: Comprehensive metadata capture and location history **User Stories**:
US-5 (Comprehensive Instrument Metadata Management) **Depends On**: M14 (order
export operational)

**Acceptance Criteria**:

1. Form captures all required metadata fields
2. Location linked to existing facility hierarchy
3. Location history preserved on relocation
4. Calibration due date warning displayed
5. Validation prevents incomplete registrations

### Setup for M15

- [ ] T219 [M15] Create branch
      `feat/011-madagascar-analyzer-integration-m15-metadata-form` from
      `demo/madagascar`
- [ ] T220 [M15] Create Liquibase changeset
      `src/main/resources/liquibase/3.8.x.x/011-001-create-instrument-metadata-table.xml`
- [ ] T221 [M15] Create Liquibase changeset
      `src/main/resources/liquibase/3.8.x.x/011-005-create-instrument-location-history-table.xml`

### Tests for M15 (MANDATORY)

- [ ] T222 [P] [M15] ORM validation test for InstrumentMetadata in
      `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [ ] T223 [P] [M15] ORM validation test for InstrumentLocationHistory
- [ ] T224 [P] [M15] Unit test for InstrumentMetadataService in
      `src/test/java/org/openelisglobal/analyzer/service/InstrumentMetadataServiceTest.java`
- [ ] T225 [P] [M15] Controller test for InstrumentMetadataRestController in
      `src/test/java/org/openelisglobal/analyzer/controller/InstrumentMetadataRestControllerTest.java`
- [ ] T226 [P] [M15] Create DBUnit test fixtures in
      `src/test/resources/testdata/instrument-metadata.xml`

### Backend Implementation for M15

- [ ] T227 [M15] Create InstrumentMetadata entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/InstrumentMetadata.java`
- [ ] T228 [M15] Create InstrumentLocationHistory entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/InstrumentLocationHistory.java`
- [ ] T229 [M15] Create ServiceStatus enum in
      `src/main/java/org/openelisglobal/analyzer/valueholder/ServiceStatus.java`
- [ ] T230 [M15] Create InstrumentMetadataDAO interface and impl in
      `src/main/java/org/openelisglobal/analyzer/dao/`
- [ ] T231 [M15] Create InstrumentLocationHistoryDAO interface and impl
- [ ] T232 [M15] Create InstrumentMetadataService interface in
      `src/main/java/org/openelisglobal/analyzer/service/InstrumentMetadataService.java`
- [ ] T233 [M15] Create InstrumentMetadataServiceImpl with relocation logic
      (close old location, create new)
- [ ] T234 [M15] Implement calibration due date warning calculation
- [ ] T235 [M15] Create InstrumentMetadataRestController in
      `src/main/java/org/openelisglobal/analyzer/controller/InstrumentMetadataRestController.java`
- [ ] T235a [M15] Implement RBAC permission check for instrument metadata
      modifications (LAB_SUPERVISOR role minimum)
- [ ] T235b [M15] Add audit trail logging for instrument metadata changes
- [ ] T236 [P] [M15] Add i18n keys for metadata form in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend Implementation for M15

- [ ] T237 [P] [M15] Jest unit test for InstrumentMetadataForm in
      `frontend/src/components/analyzers/InstrumentMetadata/__tests__/InstrumentMetadataForm.test.jsx`
- [ ] T238 [M15] Create InstrumentMetadataForm React component in
      `frontend/src/components/analyzers/InstrumentMetadata/InstrumentMetadataForm.jsx`
- [ ] T239 [M15] Implement Organization/Location picker (reuse existing
      components)
- [ ] T240 [M15] Implement location history display
- [ ] T241 [M15] Implement calibration due date warning display
- [ ] T242 [M15] Create instrumentMetadataService.js API client in
      `frontend/src/services/instrumentMetadataService.js`

### Finalization for M15

- [ ] T243 [M15] Verify all tests pass
- [ ] T244 [M15] Run Spotless and frontend formatting
- [ ] T245 [M15] Create PR
      `feat/011-madagascar-analyzer-integration-m15-metadata-form` →
      `demo/madagascar`

**Checkpoint**: Metadata form captures all fields, location history works

---

## M16: Advanced Simulator Features (2 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m16-simulator-advanced`
**Goal**: Advanced simulation features: QC results, error conditions, concurrent
testing, stress testing **User Stories**: US-9 **Depends On**: M4
(multi-protocol simulator base)

**Note**: M4 established the multi-protocol foundation (HL7, RS232, File
handlers and analyzer templates). M16 adds advanced features for
production-ready testing.

**Acceptance Criteria**:

1. QC result generation for all analyzer types
2. Error condition simulation (malformed messages, timeouts, duplicates)
3. Concurrent multi-analyzer simulation works (5+ simultaneous)
4. Stress testing capability (1000+ messages)
5. Test scenario orchestration via HTTP API

### Setup for M16

- [ ] T246 [M16] Create branch
      `feat/011-madagascar-analyzer-integration-m16-simulator-advanced` from
      `demo/madagascar`

### Advanced Features for M16

- [ ] T247 [M16] Add QC result generation templates in
      `tools/astm-mock-server/templates/qc/`
- [ ] T248 [M16] Add error condition templates (malformed, timeout, duplicate)
      in `tools/astm-mock-server/templates/errors/`
- [ ] T249 [M16] Implement concurrent multi-analyzer support in
      `tools/astm-mock-server/server.py`
- [ ] T250 [M16] Implement test scenario orchestration (`/scenarios/{name}`
      endpoint)
- [ ] T251 [M16] Add stress testing mode (`--stress-test --count N`)

### CI/CD Integration for M16

- [ ] T252 [M16] Update Docker configuration for CI/CD integration
- [ ] T253 [M16] Create GitHub Actions workflow for simulator-based tests
- [ ] T254 [M16] Document CI/CD integration in
      `tools/astm-mock-server/docs/CI_CD_INTEGRATION.md`

### Testing for M16

- [ ] T255 [M16] Test concurrent simulation with 5+ analyzers
- [ ] T256 [M16] Test stress scenario with 1000+ messages
- [ ] T257 [M16] Verify all 12 analyzer templates work end-to-end

### Finalization for M16

- [ ] T258 [M16] Verify all advanced features work
- [ ] T259 [M16] Create PR
      `feat/011-madagascar-analyzer-integration-m16-simulator-advanced` →
      `demo/madagascar`

**Checkpoint**: Simulator ready for production CI/CD with advanced testing
capabilities

---

## M17: E2E Validation and Madagascar Lab Testing (5 days)

**Branch**: `feat/011-madagascar-analyzer-integration-m17-e2e-validation`
**Goal**: Comprehensive E2E testing and production validation **User Stories**:
All **Depends On**: M14, M15, M16

**Acceptance Criteria**:

1. All 12 analyzers receive results within 60 seconds
2. All 12 analyzers receive orders via export
3. 5+ analyzers operate simultaneously without issues
4. <5% mapping errors after initial configuration
5. Lab technicians complete configuration in <30 minutes
6. E2E tests pass in CI/CD pipeline

### Setup for M17

- [ ] T260 [M17] Create branch
      `feat/011-madagascar-analyzer-integration-m17-e2e-validation` from
      `demo/madagascar`

### Cypress E2E Tests for M17 (MANDATORY)

- [ ] T261 [P] [M17] E2E test for HL7 analyzer integration in
      `frontend/cypress/e2e/hl7AnalyzerIntegration.cy.js`
- [ ] T262 [P] [M17] E2E test for serial analyzer integration in
      `frontend/cypress/e2e/serialAnalyzerIntegration.cy.js`
- [ ] T263 [P] [M17] E2E test for file import integration in
      `frontend/cypress/e2e/fileImportIntegration.cy.js`
- [ ] T264 [P] [M17] E2E test for order export in
      `frontend/cypress/e2e/orderExport.cy.js`
- [ ] T265 [P] [M17] E2E test for instrument metadata form in
      `frontend/cypress/e2e/instrumentMetadata.cy.js`
- [ ] T266 [M17] Create Cypress fixtures for E2E tests in
      `frontend/cypress/fixtures/analyzers/`

### Integration Testing for M17

- [ ] T267 [M17] Performance test: 5+ concurrent analyzers via simulator
- [ ] T268 [M17] Stress test: 1000+ messages through system
- [ ] T269 [M17] Verify message routing with multiple simultaneous analyzers

### Madagascar Lab Validation for M17

- [ ] T270 [M17] Create user training materials in `docs/madagascar-training/`
- [ ] T271 [M17] Document configuration guide for each analyzer type
- [ ] T272 [M17] Remote validation with Madagascar lab technicians (video
      session)
- [ ] T273 [M17] Collect feedback and create bug fix tickets if needed

### Finalization for M17

- [ ] T274 [M17] Verify all E2E tests pass
- [ ] T275 [M17] Verify all 12 analyzers bidirectional
- [ ] T276 [M17] Run full test suite
      (`mvn verify && cd frontend && npm run cy:run`)
- [ ] T277 [M17] Create PR
      `feat/011-madagascar-analyzer-integration-m17-e2e-validation` →
      `demo/madagascar`
- [ ] T278 [M17] After approval, create final PR `demo/madagascar` → `develop`
      for integration

**Checkpoint**: Contract requirements met - 12 analyzers bidirectional, E2E
tests pass

---

## Dependencies & Execution Order

### Milestone Dependencies

| Milestone | Depends On    | Parallel Group     |
| --------- | ------------- | ------------------ |
| M1        | -             | Foundation (1-4)   |
| M2        | -             | Foundation (1-4)   |
| M3        | -             | Foundation (1-4)   |
| M4        | -             | Foundation (1-4)   |
| M5        | M1, M2        | Validation         |
| M6        | M1            | Validation         |
| M7        | M1, M3        | Validation         |
| M8        | M3            | Validation         |
| M9        | M2            | New Plugins (9-13) |
| M10       | M2            | New Plugins (9-13) |
| M11       | M1, M2        | New Plugins (9-13) |
| M12       | M1            | New Plugins (9-13) |
| M13       | M3            | New Plugins (9-13) |
| M14       | M5-M13        | Integration        |
| M15       | M14           | Integration        |
| M16       | M4            | Integration        |
| M17       | M14, M15, M16 | Validation         |

### Parallel Opportunities

**Week 1-2**: M1, M2, M3, M4 can all proceed in parallel (4 developers) **Week
2-3**: M5, M6, M7, M8 can start as dependencies complete **Week 3-4**: M9, M10,
M11, M12, M13 can all proceed in parallel (5 developers) **Week 4-5**: M14 and
M16 can proceed in parallel **Week 5-6**: M17 after M14, M15, M16

### Within Each Milestone

- Branch creation MUST be first task
- Tests MUST be written and FAIL before implementation
- Implementation follows TDD (Red-Green-Refactor)
- PR creation MUST be last task
- Spotless/formatting MUST run before PR

---

## Implementation Strategy

### MVP First (M1-M5)

1. Complete M1: HL7 Adapter (foundation)
2. Complete M2: RS232 Adapter (parallel)
3. Complete M5: Mindray Validation (4 analyzers working)
4. **STOP and VALIDATE**: 4 analyzers receive results
5. Deploy to staging for early testing

### Incremental Delivery

| Week | Milestones     | Cumulative Analyzers   |
| ---- | -------------- | ---------------------- |
| 1    | M1, M2, M3, M4 | 0 (foundations)        |
| 2    | M5, M6, M7, M8 | 9 analyzers            |
| 3    | M9, M10, M11   | 11 analyzers           |
| 4    | M12, M13, M14  | 12 analyzers + export  |
| 5    | M15, M16       | + metadata + simulator |
| 6    | M17            | E2E validated          |

### Parallel Team Strategy

**With 4+ developers**:

| Developer | Week 1-2       | Week 3-4                 | Week 5-6 |
| --------- | -------------- | ------------------------ | -------- |
| Dev A     | M1 (HL7)       | M5 (Mindray) → M14       | M17      |
| Dev B     | M2 (RS232)     | M9, M10 (Horiba)         | M17      |
| Dev C     | M3 (File)      | M11, M12 (Stago, Abbott) | M15      |
| Dev D     | M4 (Simulator) | M6, M7, M8, M13          | M16      |

---

## Post-Deadline Features (NOT IN TASK COUNT)

The following requirements are **intentionally deferred** to
post-contract-deadline (2026-02-28) per spec.md clarification:

| Requirement      | User Story | Scope                       | Reason                     |
| ---------------- | ---------- | --------------------------- | -------------------------- |
| FR-019 to FR-021 | US-7       | GeneXpert Module Management | P3 priority, complex UI    |
| FR-022 to FR-024 | US-8       | Maintenance Tracking        | P3 priority, non-essential |
| POCT1A Protocol  | -          | Point-of-care devices       | Out of contract scope      |

These will be planned as **separate features** after the contract deadline is
met. No tasks are generated for these requirements in this document.

---

## Task Summary

| Milestone | Total Tasks | Test Tasks | Implementation Tasks |
| --------- | ----------- | ---------- | -------------------- |
| M1        | 18          | 5          | 13                   |
| M2        | 24          | 4          | 20                   |
| M3        | 24          | 5          | 19                   |
| M4        | 32          | 5          | 27                   |
| M5        | 13          | 3          | 10                   |
| M6        | 8           | 2          | 6                    |
| M7        | 12          | 4          | 8                    |
| M8        | 10          | 3          | 7                    |
| M9        | 10          | 2          | 8                    |
| M10       | 10          | 2          | 8                    |
| M11       | 10          | 2          | 8                    |
| M12       | 10          | 2          | 8                    |
| M13       | 10          | 2          | 8                    |
| M14       | 29          | 7          | 22                   |
| M15       | 29          | 5          | 24                   |
| M16       | 14          | 3          | 11                   |
| M17       | 19          | 7          | 12                   |
| **TOTAL** | **282**     | **63**     | **219**              |

---

## Notes

- **[P]** = Can run in parallel (different files, no incomplete dependencies)
- **[M#]** = Milestone tag for traceability
- Tests are MANDATORY per Constitution Principle V
- Each milestone = 1 PR per Constitution Principle IX
- Run `mvn spotless:apply` and `cd frontend && npm run format` before every PR
- Use BOTH flags when skipping tests: `-DskipTests -Dmaven.test.skip=true`
- Run E2E tests individually during development:
  `npm run cy:run -- --spec "cypress/e2e/{test}.cy.js"`

---

**Tasks Generated**: 2026-01-22 | **Updated**: 2026-01-23 (remediation: M4
expanded, M5-M17 task IDs renumbered +20 to avoid collision, post-deadline
section added) **Total Tasks**: 282 **Test Tasks**: 63 (22%) **Task ID Range**:
T001-T278 **Parallel Milestones**: M1-M4, M9-M13 **Contract Deadline**:
2026-02-28
