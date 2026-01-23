# Tasks: Madagascar Analyzer Integration

**Feature**: 150-madagascar-analyzer-integration
**Input**: Design documents from `/specs/150-madagascar-analyzer-integration/`
**Prerequisites**: plan.md (required), spec.md (required), data-model.md, contracts/, research.md, quickstart.md
**Contract Deadline**: 2026-02-28 (37 days)

**Organization**: Tasks are grouped by **Milestone** per Constitution Principle IX. Each milestone = 1 PR.

**Tests**: **MANDATORY** per Constitution Principle V (TDD). Tests appear before implementation tasks.

---

## Format: `[ID] [P?] [M#] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
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

**Branch**: `feat/150-madagascar-analyzer-integration-m1-hl7-adapter`
**Goal**: Parse HL7 ORU^R01 results and generate HL7 ORM^O01 orders
**User Stories**: US-1 (HL7 Analyzer Results Import)
**Depends On**: None (foundation milestone)

**Acceptance Criteria**:
1. HL7 ORU^R01 messages parse correctly (patient ID, test codes, results)
2. HL7 ORM^O01 messages generate correctly for order export
3. MSH segment sender ID extracted for analyzer identification
4. Unmapped fields create error records in dashboard
5. Unit test coverage >80%

### Setup for M1

- [ ] T001 [M1] Create branch `feat/150-madagascar-analyzer-integration-m1-hl7-adapter` from `demo/madagascar`
- [ ] T002 [M1] Add HAPI HL7 v2 dependency to `pom.xml` (ca.uhn.hapi:hapi-base:2.4, ca.uhn.hapi:hapi-structures-v251:2.4)

### Tests for M1 (MANDATORY - Write FIRST, ensure they FAIL)

- [ ] T003 [P] [M1] Unit test for HL7MessageService in `src/test/java/org/openelisglobal/analyzer/service/HL7MessageServiceTest.java`
- [ ] T004 [P] [M1] Unit test for HL7AnalyzerReader in `src/test/java/org/openelisglobal/analyzerimport/analyzerreaders/HL7AnalyzerReaderTest.java`
- [ ] T005 [P] [M1] Create test HL7 message fixtures in `src/test/resources/testdata/hl7/mindray-cbc-result.hl7`
- [ ] T006 [P] [M1] Create test HL7 message fixtures in `src/test/resources/testdata/hl7/sysmex-result.hl7`
- [ ] T007 [P] [M1] Create test HL7 ORM^O01 expected output in `src/test/resources/testdata/hl7/expected-order.hl7`

### Implementation for M1

- [ ] T008 [M1] Create HL7AnalyzerReader in `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/HL7AnalyzerReader.java` (extends AnalyzerReader, uses HAPI PipeParser)
- [ ] T009 [M1] Create HL7MessageService interface in `src/main/java/org/openelisglobal/analyzer/service/HL7MessageService.java`
- [ ] T010 [M1] Create HL7MessageServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/HL7MessageServiceImpl.java` (ORU^R01 parsing, ORM^O01 generation)
- [ ] T011 [M1] Implement field extraction for ORU^R01 (PID segment → patient, OBR segment → test, OBX segment → results)
- [ ] T012 [M1] Implement MSH segment parsing for analyzer identification (sending application, sending facility)
- [ ] T013 [M1] Create MappingAwareHL7AnalyzerLineInserter wrapper in `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/MappingAwareHL7AnalyzerLineInserter.java`
- [ ] T014 [M1] Integrate with existing FieldMappingService for test code mapping
- [ ] T015 [M1] Add i18n keys for HL7-related messages in `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Finalization for M1

- [ ] T016 [M1] Verify all unit tests pass (`mvn test -Dtest="*HL7*"`)
- [ ] T017 [M1] Run Spotless formatting (`mvn spotless:apply`)
- [ ] T018 [M1] Create PR `feat/150-madagascar-analyzer-integration-m1-hl7-adapter` → `demo/madagascar`

**Checkpoint**: HL7 parsing and generation unit tests pass with >80% coverage

---

## [P] M2: RS232 Serial Communication Adapter (3 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m2-serial-adapter`
**Goal**: Serial port configuration and ASTM message reception via RS232
**User Stories**: US-3 (RS232 Serial Analyzer Connection)
**Depends On**: None (parallel with M1)

**Acceptance Criteria**:
1. Serial port configuration stored in database
2. Connection established with correct parameters
3. ASTM messages received over serial processed by existing infrastructure
4. Connection status tracked (connected/disconnected/error)
5. Graceful handling of cable disconnection

### Setup for M2

- [ ] T019 [M2] Create branch `feat/150-madagascar-analyzer-integration-m2-serial-adapter` from `demo/madagascar`
- [ ] T020 [M2] Add jSerialComm dependency to `pom.xml` (com.fazecast:jSerialComm:2.10.4)
- [ ] T021 [M2] Create Liquibase changeset `src/main/resources/liquibase/3.8.x.x/150-003-create-serial-port-configuration-table.xml`

### Tests for M2 (MANDATORY - Write FIRST)

- [ ] T022 [P] [M2] ORM validation test for SerialPortConfiguration in `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [ ] T023 [P] [M2] Unit test for SerialPortService in `src/test/java/org/openelisglobal/analyzer/service/SerialPortServiceTest.java`
- [ ] T024 [P] [M2] Unit test for SerialPortConfigurationDAO in `src/test/java/org/openelisglobal/analyzer/dao/SerialPortConfigurationDAOTest.java`
- [ ] T025 [P] [M2] Create DBUnit test fixture in `src/test/resources/testdata/serial-port-configuration.xml`

### Implementation for M2

- [ ] T026 [M2] Create SerialPortConfiguration entity in `src/main/java/org/openelisglobal/analyzer/valueholder/SerialPortConfiguration.java`
- [ ] T027 [M2] Create StopBits, Parity, FlowControl enums in `src/main/java/org/openelisglobal/analyzer/valueholder/`
- [ ] T028 [M2] Create SerialPortConfigurationDAO interface in `src/main/java/org/openelisglobal/analyzer/dao/SerialPortConfigurationDAO.java`
- [ ] T029 [M2] Create SerialPortConfigurationDAOImpl in `src/main/java/org/openelisglobal/analyzer/dao/SerialPortConfigurationDAOImpl.java`
- [ ] T030 [M2] Create SerialPortService interface in `src/main/java/org/openelisglobal/analyzer/service/SerialPortService.java`
- [ ] T031 [M2] Create SerialPortServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/SerialPortServiceImpl.java` (connection lifecycle, jSerialComm integration)
- [ ] T032 [M2] Create SerialAnalyzerReader in `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/SerialAnalyzerReader.java`
- [ ] T033 [M2] Implement connection status tracking with event callbacks
- [ ] T034 [M2] Add serial port REST endpoints in `src/main/java/org/openelisglobal/analyzer/controller/SerialPortRestController.java`
- [ ] T035 [P] [M2] Add i18n keys for serial config messages in `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend for M2

- [ ] T036 [P] [M2] Jest unit test for SerialConfiguration in `frontend/src/components/analyzers/SerialConfiguration/__tests__/SerialConfiguration.test.jsx`
- [ ] T037 [M2] Create SerialConfiguration React component in `frontend/src/components/analyzers/SerialConfiguration/SerialConfiguration.jsx`
- [ ] T038 [M2] Create serialService.js API client in `frontend/src/services/serialService.js`

### Finalization for M2

- [ ] T039 [M2] Update dev.docker-compose.yml with device mapping example for `/dev/ttyUSB0`
- [ ] T040 [M2] Verify all unit tests pass
- [ ] T041 [M2] Run Spotless formatting (`mvn spotless:apply`) and frontend formatting (`cd frontend && npm run format`)
- [ ] T042 [M2] Create PR `feat/150-madagascar-analyzer-integration-m2-serial-adapter` → `demo/madagascar`

**Checkpoint**: Serial config entity persists, service can open/close virtual serial ports

---

## [P] M3: File-Based Import Adapter (2 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m3-file-adapter`
**Goal**: Directory watcher and CSV/TXT file parsing
**User Stories**: US-4 (File-Based PCR Thermocycler Integration)
**Depends On**: None (parallel with M1, M2)

**Acceptance Criteria**:
1. Files detected within 60 seconds of creation
2. CSV rows mapped to results via configured column mappings
3. Processed files moved to archive directory
4. Malformed files moved to error directory with log entry
5. Duplicate detection warns before creating new results

### Setup for M3

- [ ] T043 [M3] Create branch `feat/150-madagascar-analyzer-integration-m3-file-adapter` from `demo/madagascar`
- [ ] T044 [M3] Add Apache Commons CSV dependency to `pom.xml` (org.apache.commons:commons-csv:1.10.0) if not present
- [ ] T045 [M3] Create Liquibase changeset `src/main/resources/liquibase/3.8.x.x/150-004-create-file-import-configuration-table.xml`

### Tests for M3 (MANDATORY - Write FIRST)

- [ ] T046 [P] [M3] ORM validation test for FileImportConfiguration in `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [ ] T047 [P] [M3] Unit test for FileImportService in `src/test/java/org/openelisglobal/analyzer/service/FileImportServiceTest.java`
- [ ] T048 [P] [M3] Unit test for FileAnalyzerReader in `src/test/java/org/openelisglobal/analyzerimport/analyzerreaders/FileAnalyzerReaderTest.java`
- [ ] T049 [P] [M3] Create test CSV fixtures in `src/test/resources/testdata/files/quantstudio-results.csv`
- [ ] T050 [P] [M3] Create malformed CSV fixture in `src/test/resources/testdata/files/malformed-results.csv`

### Implementation for M3

- [ ] T051 [M3] Create FileImportConfiguration entity in `src/main/java/org/openelisglobal/analyzer/valueholder/FileImportConfiguration.java`
- [ ] T052 [M3] Create FileImportConfigurationDAO in `src/main/java/org/openelisglobal/analyzer/dao/FileImportConfigurationDAO.java` and `FileImportConfigurationDAOImpl.java`
- [ ] T053 [M3] Create FileImportService interface in `src/main/java/org/openelisglobal/analyzer/service/FileImportService.java`
- [ ] T054 [M3] Create FileImportServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/FileImportServiceImpl.java` (WatchService integration)
- [ ] T055 [M3] Create FileAnalyzerReader in `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/FileAnalyzerReader.java` (CSV parsing with Commons CSV)
- [ ] T056 [M3] Implement file archival (move to archive directory on success)
- [ ] T057 [M3] Implement error handling (move to error directory on failure with log)
- [ ] T058 [M3] Implement duplicate detection (sample ID + test + timestamp)
- [ ] T059 [M3] Add file import REST endpoints in `src/main/java/org/openelisglobal/analyzer/controller/FileImportRestController.java`
- [ ] T060 [P] [M3] Add i18n keys for file import messages in `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend for M3

- [ ] T061 [P] [M3] Jest unit test for FileImportConfiguration in `frontend/src/components/analyzers/FileImportConfiguration/__tests__/FileImportConfiguration.test.jsx`
- [ ] T062 [M3] Create FileImportConfiguration React component in `frontend/src/components/analyzers/FileImportConfiguration/FileImportConfiguration.jsx`
- [ ] T063 [M3] Create fileImportService.js API client in `frontend/src/services/fileImportService.js`

### Finalization for M3

- [ ] T064 [M3] Verify all unit tests pass
- [ ] T065 [M3] Run Spotless and frontend formatting
- [ ] T066 [M3] Create PR `feat/150-madagascar-analyzer-integration-m3-file-adapter` → `demo/madagascar`

**Checkpoint**: File detection + CSV parsing tests pass, archive/error workflow works

---

## [P] M4: Simulator HL7 Support (2 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m4-simulator-hl7`
**Goal**: Expand astm-mock-server with HL7 message generation
**User Stories**: US-9 (Analyzer Simulator for Testing)
**Depends On**: None (parallel with M1-M3)

**Acceptance Criteria**:
1. Simulator generates valid HL7 ORU^R01 messages
2. Messages received and processed correctly by OpenELIS
3. Configurable analyzer type (template selection)
4. HTTP API mode for CI/CD integration

### Setup for M4

- [ ] T067 [M4] Create branch `feat/150-madagascar-analyzer-integration-m4-simulator-hl7` from `demo/madagascar`
- [ ] T068 [M4] Create `tools/astm-http-bridge/src/hl7/` directory structure

### Implementation for M4

- [ ] T069 [M4] Create HL7MessageGenerator in `tools/astm-http-bridge/src/hl7/HL7MessageGenerator.java`
- [ ] T070 [M4] Create HL7Server (TCP server) in `tools/astm-http-bridge/src/hl7/HL7Server.java`
- [ ] T071 [M4] Create Mindray BC-5380 message template in `tools/astm-http-bridge/configs/mindray-bc5380.json`
- [ ] T072 [M4] Create Sysmex XN message template in `tools/astm-http-bridge/configs/sysmex-xn.json`
- [ ] T073 [M4] Add HTTP API endpoint `/simulate/hl7/{analyzer-type}` for CI/CD
- [ ] T074 [M4] Update `tools/astm-http-bridge/docker-compose.yml` to expose HL7 port 4012

### Testing for M4

- [ ] T075 [M4] Integration test: Simulator → OpenELIS HL7 reception (manual verification)
- [ ] T076 [M4] Document simulator usage in `tools/astm-http-bridge/README.md`

### Finalization for M4

- [ ] T077 [M4] Verify simulator generates valid HL7 messages
- [ ] T078 [M4] Create PR `feat/150-madagascar-analyzer-integration-m4-simulator-hl7` → `demo/madagascar`

**Checkpoint**: Simulator generates HL7 messages that OpenELIS can receive

---

## M5: Mindray Plugin Validation (3 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m5-mindray-validation`
**Goal**: Validate existing Mindray plugin with 4 analyzers
**User Stories**: US-1, US-6
**Depends On**: M1 (HL7), M2 (RS232)

**Analyzers**: BC-5380, BS-360E, BC2000 (HL7), BA-88A (RS232)

**Acceptance Criteria**:
1. BC-5380 receives results via HL7 (simulator)
2. BS-360E receives results via HL7 (simulator)
3. BC2000 receives results via HL7 (simulator)
4. BA-88A receives results via RS232 (virtual serial)
5. Field mappings work with plugin + mapping system

### Setup for M5

- [ ] T079 [M5] Create branch `feat/150-madagascar-analyzer-integration-m5-mindray-validation` from `demo/madagascar`
- [ ] T080 [M5] Merge M1 and M2 into branch (or ensure dependencies merged to `demo/madagascar`)

### Tests for M5 (MANDATORY)

- [ ] T081 [P] [M5] Integration test for Mindray BC-5380 HL7 in `src/test/java/org/openelisglobal/analyzer/mindray/MindrayBC5380IntegrationTest.java`
- [ ] T082 [P] [M5] Integration test for Mindray BA-88A RS232 in `src/test/java/org/openelisglobal/analyzer/mindray/MindrayBA88AIntegrationTest.java`
- [ ] T083 [P] [M5] Create HL7 test fixtures for BC-5380, BS-360E, BC2000 in `src/test/resources/testdata/hl7/mindray/`

### Implementation for M5

- [ ] T084 [M5] Verify existing Mindray plugin compatibility with HL7AnalyzerReader
- [ ] T085 [M5] Create MappingAwareMindrayAnalyzerLineInserter wrapper if needed
- [ ] T086 [M5] Configure RS232 parameters for BA-88A in test environment
- [ ] T087 [M5] Validate field mappings for all 4 Mindray analyzers
- [ ] T088 [M5] Document Mindray plugin + protocol adapter integration

### Finalization for M5

- [ ] T089 [M5] Verify all integration tests pass
- [ ] T090 [M5] Run Spotless formatting
- [ ] T091 [M5] Create PR `feat/150-madagascar-analyzer-integration-m5-mindray-validation` → `demo/madagascar`

**Checkpoint**: 4 Mindray analyzers receive results via appropriate protocols

---

## M6: Sysmex Plugin Validation (1 day)

**Branch**: `feat/150-madagascar-analyzer-integration-m6-sysmex-validation`
**Goal**: Validate existing SysmexXN-L plugin
**User Stories**: US-1, US-6
**Depends On**: M1 (HL7)

**Analyzer**: Sysmex XN Series (HL7)

### Setup for M6

- [ ] T092 [M6] Create branch `feat/150-madagascar-analyzer-integration-m6-sysmex-validation` from `demo/madagascar`

### Tests for M6 (MANDATORY)

- [ ] T093 [M6] Integration test for Sysmex XN in `src/test/java/org/openelisglobal/analyzer/sysmex/SysmexXNIntegrationTest.java`
- [ ] T094 [M6] Create HL7 test fixture in `src/test/resources/testdata/hl7/sysmex-xn-result.hl7`

### Implementation for M6

- [ ] T095 [M6] Verify SysmexXN-L plugin compatibility with HL7AnalyzerReader
- [ ] T096 [M6] Test override mappings take precedence over plugin defaults
- [ ] T097 [M6] Document Sysmex plugin integration

### Finalization for M6

- [ ] T098 [M6] Verify integration tests pass
- [ ] T099 [M6] Create PR `feat/150-madagascar-analyzer-integration-m6-sysmex-validation` → `demo/madagascar`

**Checkpoint**: Sysmex XN receives results via HL7, override mappings work

---

## M7: GeneXpert Plugin Validation (2 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m7-genexpert-validation`
**Goal**: Validate 3 GeneXpert plugin variants
**User Stories**: US-6
**Depends On**: M1 (HL7), M3 (File)

**Analyzer**: Cepheid GeneXpert (ASTM, HL7, File variants)

### Setup for M7

- [ ] T100 [M7] Create branch `feat/150-madagascar-analyzer-integration-m7-genexpert-validation` from `demo/madagascar`

### Tests for M7 (MANDATORY)

- [ ] T101 [P] [M7] Integration test for GeneXpert HL7 in `src/test/java/org/openelisglobal/analyzer/genexpert/GeneXpertHL7IntegrationTest.java`
- [ ] T102 [P] [M7] Integration test for GeneXpert File in `src/test/java/org/openelisglobal/analyzer/genexpert/GeneXpertFileIntegrationTest.java`
- [ ] T103 [P] [M7] Create HL7 test fixture in `src/test/resources/testdata/hl7/genexpert-result.hl7`
- [ ] T104 [P] [M7] Create file test fixture in `src/test/resources/testdata/files/genexpert-results.csv`

### Implementation for M7

- [ ] T105 [M7] Verify GeneXpert ASTM plugin works (existing)
- [ ] T106 [M7] Verify GeneXpertHL7 plugin with HL7AnalyzerReader
- [ ] T107 [M7] Verify GeneXpertFile plugin with FileAnalyzerReader
- [ ] T108 [M7] Test all 3 variants can coexist
- [ ] T109 [M7] Document GeneXpert multi-protocol support

### Finalization for M7

- [ ] T110 [M7] Verify all integration tests pass
- [ ] T111 [M7] Create PR `feat/150-madagascar-analyzer-integration-m7-genexpert-validation` → `demo/madagascar`

**Checkpoint**: GeneXpert works via ASTM, HL7, and File

---

## M8: QuantStudio Adaptation (2 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m8-quantstudio-adapt`
**Goal**: Adapt QuantStudio3 plugin for QuantStudio 7 Flex
**User Stories**: US-4, US-6
**Depends On**: M3 (File)

**Analyzer**: Thermo Fisher QuantStudio 7 Flex (File-based)

### Setup for M8

- [ ] T112 [M8] Create branch `feat/150-madagascar-analyzer-integration-m8-quantstudio-adapt` from `demo/madagascar`

### Tests for M8 (MANDATORY)

- [ ] T113 [M8] Integration test for QuantStudio 7 Flex in `src/test/java/org/openelisglobal/analyzer/quantstudio/QuantStudio7FlexIntegrationTest.java`
- [ ] T114 [M8] Create CSV test fixture for QuantStudio 7 Flex format in `src/test/resources/testdata/files/quantstudio7-flex-results.csv`
- [ ] T115 [M8] Backward compatibility test for QuantStudio 3 in `src/test/java/org/openelisglobal/analyzer/quantstudio/QuantStudio3BackwardCompatTest.java`

### Implementation for M8

- [ ] T116 [M8] Analyze QuantStudio 7 Flex CSV format differences from QuantStudio 3
- [ ] T117 [M8] Modify QuantStudio plugin or create FileImportConfiguration for column differences
- [ ] T118 [M8] Ensure backward compatibility with QuantStudio 3
- [ ] T119 [M8] Document QuantStudio adaptation

### Finalization for M8

- [ ] T120 [M8] Verify all integration tests pass
- [ ] T121 [M8] Create PR `feat/150-madagascar-analyzer-integration-m8-quantstudio-adapt` → `demo/madagascar`

**Checkpoint**: QuantStudio 7 Flex and QuantStudio 3 both work

---

## [P] M9: Horiba Pentra 60 Plugin (2 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m9-plugin-pentra`
**Goal**: Build new Horiba Pentra 60 plugin
**User Stories**: US-3
**Depends On**: M2 (RS232)

**Analyzer**: Horiba ABX Pentra 60 (ASTM over RS232)

### Setup for M9

- [ ] T122 [M9] Create branch `feat/150-madagascar-analyzer-integration-m9-plugin-pentra` from `demo/madagascar`

### Tests for M9 (MANDATORY)

- [ ] T123 [P] [M9] Unit test for Pentra60AnalyzerLineInserter in `src/test/java/org/openelisglobal/analyzer/pentra/Pentra60AnalyzerLineInserterTest.java`
- [ ] T124 [P] [M9] Create ASTM test fixtures for Pentra 60 in `src/test/resources/testdata/astm/pentra60-results.txt`

### Implementation for M9

- [ ] T125 [M9] Create Pentra60Analyzer plugin class in `src/main/java/org/openelisglobal/analyzer/pentra/Pentra60Analyzer.java`
- [ ] T126 [M9] Create Pentra60AnalyzerLineInserter in `src/main/java/org/openelisglobal/analyzer/pentra/Pentra60AnalyzerLineInserter.java`
- [ ] T127 [M9] Implement ASTM message parsing for Pentra 60 format
- [ ] T128 [M9] Integrate with MappingAware wrapper pattern
- [ ] T129 [M9] Document Pentra 60 plugin and RS232 configuration

### Finalization for M9

- [ ] T130 [M9] Verify unit tests pass
- [ ] T131 [M9] Create PR `feat/150-madagascar-analyzer-integration-m9-plugin-pentra` → `demo/madagascar`

**Checkpoint**: Pentra 60 results import via RS232

---

## [P] M10: Horiba Micros 60 Plugin (2 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m10-plugin-micros`
**Goal**: Build new Horiba Micros 60 plugin
**User Stories**: US-3
**Depends On**: M2 (RS232)

**Analyzer**: Horiba ABX Micros 60 (ASTM over RS232)

### Setup for M10

- [ ] T132 [M10] Create branch `feat/150-madagascar-analyzer-integration-m10-plugin-micros` from `demo/madagascar`

### Tests for M10 (MANDATORY)

- [ ] T133 [P] [M10] Unit test for Micros60AnalyzerLineInserter in `src/test/java/org/openelisglobal/analyzer/micros/Micros60AnalyzerLineInserterTest.java`
- [ ] T134 [P] [M10] Create ASTM test fixtures for Micros 60 in `src/test/resources/testdata/astm/micros60-results.txt`

### Implementation for M10

- [ ] T135 [M10] Create Micros60Analyzer plugin class in `src/main/java/org/openelisglobal/analyzer/micros/Micros60Analyzer.java`
- [ ] T136 [M10] Create Micros60AnalyzerLineInserter in `src/main/java/org/openelisglobal/analyzer/micros/Micros60AnalyzerLineInserter.java`
- [ ] T137 [M10] Implement ASTM message parsing for Micros 60 format
- [ ] T138 [M10] Integrate with MappingAware wrapper pattern
- [ ] T139 [M10] Document Micros 60 plugin

### Finalization for M10

- [ ] T140 [M10] Verify unit tests pass
- [ ] T141 [M10] Create PR `feat/150-madagascar-analyzer-integration-m10-plugin-micros` → `demo/madagascar`

**Checkpoint**: Micros 60 results import via RS232

---

## [P] M11: Stago STart 4 Plugin (2 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m11-plugin-stago`
**Goal**: Build new Stago STart 4 plugin
**User Stories**: US-1, US-3
**Depends On**: M1 (HL7), M2 (RS232)

**Analyzer**: Stago STart 4 (ASTM/HL7 over RS232/Network)

### Setup for M11

- [ ] T142 [M11] Create branch `feat/150-madagascar-analyzer-integration-m11-plugin-stago` from `demo/madagascar`

### Tests for M11 (MANDATORY)

- [ ] T143 [P] [M11] Unit test for StagoSTart4AnalyzerLineInserter in `src/test/java/org/openelisglobal/analyzer/stago/StagoSTart4AnalyzerLineInserterTest.java`
- [ ] T144 [P] [M11] Create test fixtures for Stago in `src/test/resources/testdata/stago/`

### Implementation for M11

- [ ] T145 [M11] Create StagoSTart4Analyzer plugin class in `src/main/java/org/openelisglobal/analyzer/stago/StagoSTart4Analyzer.java`
- [ ] T146 [M11] Create StagoSTart4AnalyzerLineInserter in `src/main/java/org/openelisglobal/analyzer/stago/StagoSTart4AnalyzerLineInserter.java`
- [ ] T147 [M11] Support both ASTM (RS232) and HL7 (Network) modes
- [ ] T148 [M11] Integrate with MappingAware wrapper pattern
- [ ] T149 [M11] Document Stago plugin with dual-protocol support

### Finalization for M11

- [ ] T150 [M11] Verify unit tests pass
- [ ] T151 [M11] Create PR `feat/150-madagascar-analyzer-integration-m11-plugin-stago` → `demo/madagascar`

**Checkpoint**: Stago STart 4 works via RS232 or Network

---

## [P] M12: Abbott Architect Plugin (2 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m12-plugin-abbott`
**Goal**: Build new Abbott Architect plugin
**User Stories**: US-1
**Depends On**: M1 (HL7)

**Analyzer**: Abbott Architect (HL7 over RS232/Network)

### Setup for M12

- [ ] T152 [M12] Create branch `feat/150-madagascar-analyzer-integration-m12-plugin-abbott` from `demo/madagascar`

### Tests for M12 (MANDATORY)

- [ ] T153 [P] [M12] Unit test for AbbottArchitectAnalyzerLineInserter in `src/test/java/org/openelisglobal/analyzer/abbott/AbbottArchitectAnalyzerLineInserterTest.java`
- [ ] T154 [P] [M12] Create HL7 test fixtures for Abbott Architect in `src/test/resources/testdata/hl7/abbott-architect-result.hl7`

### Implementation for M12

- [ ] T155 [M12] Create AbbottArchitectAnalyzer plugin class in `src/main/java/org/openelisglobal/analyzer/abbott/AbbottArchitectAnalyzer.java`
- [ ] T156 [M12] Create AbbottArchitectAnalyzerLineInserter in `src/main/java/org/openelisglobal/analyzer/abbott/AbbottArchitectAnalyzerLineInserter.java`
- [ ] T157 [M12] Implement HL7 message parsing for Abbott format
- [ ] T158 [M12] Integrate with MappingAware wrapper pattern
- [ ] T159 [M12] Document Abbott plugin

### Finalization for M12

- [ ] T160 [M12] Verify unit tests pass
- [ ] T161 [M12] Create PR `feat/150-madagascar-analyzer-integration-m12-plugin-abbott` → `demo/madagascar`

**Checkpoint**: Abbott Architect results import via HL7

---

## [P] M13: Hain FluoroCycler XT Plugin (2 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m13-plugin-fluorocycler`
**Goal**: Build new Hain FluoroCycler XT plugin
**User Stories**: US-4
**Depends On**: M3 (File)

**Analyzer**: Hain Lifescience FluoroCycler XT (File-based)

### Setup for M13

- [ ] T162 [M13] Create branch `feat/150-madagascar-analyzer-integration-m13-plugin-fluorocycler` from `demo/madagascar`

### Tests for M13 (MANDATORY)

- [ ] T163 [P] [M13] Unit test for FluoroCyclerXTAnalyzerLineInserter in `src/test/java/org/openelisglobal/analyzer/fluorocycler/FluoroCyclerXTAnalyzerLineInserterTest.java`
- [ ] T164 [P] [M13] Create CSV test fixtures for FluoroCycler in `src/test/resources/testdata/files/fluorocycler-results.csv`

### Implementation for M13

- [ ] T165 [M13] Create FluoroCyclerXTAnalyzer plugin class in `src/main/java/org/openelisglobal/analyzer/fluorocycler/FluoroCyclerXTAnalyzer.java`
- [ ] T166 [M13] Create FluoroCyclerXTAnalyzerLineInserter in `src/main/java/org/openelisglobal/analyzer/fluorocycler/FluoroCyclerXTAnalyzerLineInserter.java`
- [ ] T167 [M13] Implement CSV parsing for FluoroCycler format
- [ ] T168 [M13] Integrate with FileAnalyzerReader
- [ ] T169 [M13] Document FluoroCycler plugin

### Finalization for M13

- [ ] T170 [M13] Verify unit tests pass
- [ ] T171 [M13] Create PR `feat/150-madagascar-analyzer-integration-m13-plugin-fluorocycler` → `demo/madagascar`

**Checkpoint**: FluoroCycler XT CSV import works

---

## M14: Order Export Workflow (4 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m14-order-export`
**Goal**: Manual order export with status tracking
**User Stories**: US-2 (Test Order Export to Analyzers)
**Depends On**: M5-M13 (all analyzers operational)

**Acceptance Criteria**:
1. Users can select pending orders and trigger export
2. Orders sent via appropriate protocol (ASTM/HL7)
3. Status tracked: pending → sent → acknowledged → results_received
4. Retry mechanism (3 attempts, exponential backoff)
5. Results automatically matched to exported orders
6. UI displays export status per analyzer

### Setup for M14

- [ ] T172 [M14] Create branch `feat/150-madagascar-analyzer-integration-m14-order-export` from `demo/madagascar`
- [ ] T173 [M14] Create Liquibase changeset `src/main/resources/liquibase/3.8.x.x/150-002-create-order-export-table.xml`

### Tests for M14 (MANDATORY)

- [ ] T174 [P] [M14] ORM validation test for OrderExport in `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [ ] T175 [P] [M14] Unit test for OrderExportService in `src/test/java/org/openelisglobal/analyzer/service/OrderExportServiceTest.java`
- [ ] T176 [P] [M14] Unit test for OrderExportDAO in `src/test/java/org/openelisglobal/analyzer/dao/OrderExportDAOTest.java`
- [ ] T177 [P] [M14] Controller test for OrderExportRestController in `src/test/java/org/openelisglobal/analyzer/controller/OrderExportRestControllerTest.java`
- [ ] T178 [P] [M14] Create DBUnit test fixture in `src/test/resources/testdata/order-export.xml`

### Backend Implementation for M14

- [ ] T179 [M14] Create OrderExport entity in `src/main/java/org/openelisglobal/analyzer/valueholder/OrderExport.java`
- [ ] T180 [M14] Create OrderExportStatus and MessageType enums in `src/main/java/org/openelisglobal/analyzer/valueholder/`
- [ ] T181 [M14] Create OrderExportDAO interface and impl in `src/main/java/org/openelisglobal/analyzer/dao/`
- [ ] T182 [M14] Create OrderExportService interface in `src/main/java/org/openelisglobal/analyzer/service/OrderExportService.java`
- [ ] T183 [M14] Create OrderExportServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/OrderExportServiceImpl.java`
- [ ] T184 [M14] Implement ASTM O-segment generation for order export
- [ ] T185 [M14] Implement HL7 ORM^O01 generation for order export (leverage M1 HL7MessageService)
- [ ] T186 [M14] Implement retry mechanism with exponential backoff
- [ ] T187 [M14] Implement result matching (incoming results → exported orders)
- [ ] T188 [M14] Create OrderExportRestController in `src/main/java/org/openelisglobal/analyzer/controller/OrderExportRestController.java`
- [ ] T189 [P] [M14] Add i18n keys for order export messages in `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend Implementation for M14

- [ ] T190 [P] [M14] Jest unit test for OrderExportList in `frontend/src/components/analyzers/OrderExport/__tests__/OrderExportList.test.jsx`
- [ ] T191 [P] [M14] Jest unit test for OrderExportModal in `frontend/src/components/analyzers/OrderExport/__tests__/OrderExportModal.test.jsx`
- [ ] T192 [M14] Create OrderExportList React component in `frontend/src/components/analyzers/OrderExport/OrderExportList.jsx`
- [ ] T193 [M14] Create OrderExportModal React component in `frontend/src/components/analyzers/OrderExport/OrderExportModal.jsx`
- [ ] T194 [M14] Create orderExportService.js API client in `frontend/src/services/orderExportService.js`
- [ ] T195 [M14] Integrate OrderExport components into analyzer dashboard

### Finalization for M14

- [ ] T196 [M14] Verify all unit and integration tests pass
- [ ] T197 [M14] Run Spotless and frontend formatting
- [ ] T198 [M14] Create PR `feat/150-madagascar-analyzer-integration-m14-order-export` → `demo/madagascar`

**Checkpoint**: Order export UI works, orders sent to analyzers, status tracked

---

## M15: Enhanced Instrument Metadata Form (3 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m15-metadata-form`
**Goal**: Comprehensive metadata capture and location history
**User Stories**: US-5 (Comprehensive Instrument Metadata Management)
**Depends On**: M14 (order export operational)

**Acceptance Criteria**:
1. Form captures all required metadata fields
2. Location linked to existing facility hierarchy
3. Location history preserved on relocation
4. Calibration due date warning displayed
5. Validation prevents incomplete registrations

### Setup for M15

- [ ] T199 [M15] Create branch `feat/150-madagascar-analyzer-integration-m15-metadata-form` from `demo/madagascar`
- [ ] T200 [M15] Create Liquibase changeset `src/main/resources/liquibase/3.8.x.x/150-001-create-instrument-metadata-table.xml`
- [ ] T201 [M15] Create Liquibase changeset `src/main/resources/liquibase/3.8.x.x/150-005-create-instrument-location-history-table.xml`

### Tests for M15 (MANDATORY)

- [ ] T202 [P] [M15] ORM validation test for InstrumentMetadata in `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java`
- [ ] T203 [P] [M15] ORM validation test for InstrumentLocationHistory
- [ ] T204 [P] [M15] Unit test for InstrumentMetadataService in `src/test/java/org/openelisglobal/analyzer/service/InstrumentMetadataServiceTest.java`
- [ ] T205 [P] [M15] Controller test for InstrumentMetadataRestController in `src/test/java/org/openelisglobal/analyzer/controller/InstrumentMetadataRestControllerTest.java`
- [ ] T206 [P] [M15] Create DBUnit test fixtures in `src/test/resources/testdata/instrument-metadata.xml`

### Backend Implementation for M15

- [ ] T207 [M15] Create InstrumentMetadata entity in `src/main/java/org/openelisglobal/analyzer/valueholder/InstrumentMetadata.java`
- [ ] T208 [M15] Create InstrumentLocationHistory entity in `src/main/java/org/openelisglobal/analyzer/valueholder/InstrumentLocationHistory.java`
- [ ] T209 [M15] Create ServiceStatus enum in `src/main/java/org/openelisglobal/analyzer/valueholder/ServiceStatus.java`
- [ ] T210 [M15] Create InstrumentMetadataDAO interface and impl in `src/main/java/org/openelisglobal/analyzer/dao/`
- [ ] T211 [M15] Create InstrumentLocationHistoryDAO interface and impl
- [ ] T212 [M15] Create InstrumentMetadataService interface in `src/main/java/org/openelisglobal/analyzer/service/InstrumentMetadataService.java`
- [ ] T213 [M15] Create InstrumentMetadataServiceImpl with relocation logic (close old location, create new)
- [ ] T214 [M15] Implement calibration due date warning calculation
- [ ] T215 [M15] Create InstrumentMetadataRestController in `src/main/java/org/openelisglobal/analyzer/controller/InstrumentMetadataRestController.java`
- [ ] T216 [P] [M15] Add i18n keys for metadata form in `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

### Frontend Implementation for M15

- [ ] T217 [P] [M15] Jest unit test for InstrumentMetadataForm in `frontend/src/components/analyzers/InstrumentMetadata/__tests__/InstrumentMetadataForm.test.jsx`
- [ ] T218 [M15] Create InstrumentMetadataForm React component in `frontend/src/components/analyzers/InstrumentMetadata/InstrumentMetadataForm.jsx`
- [ ] T219 [M15] Implement Organization/Location picker (reuse existing components)
- [ ] T220 [M15] Implement location history display
- [ ] T221 [M15] Implement calibration due date warning display
- [ ] T222 [M15] Create instrumentMetadataService.js API client in `frontend/src/services/instrumentMetadataService.js`

### Finalization for M15

- [ ] T223 [M15] Verify all tests pass
- [ ] T224 [M15] Run Spotless and frontend formatting
- [ ] T225 [M15] Create PR `feat/150-madagascar-analyzer-integration-m15-metadata-form` → `demo/madagascar`

**Checkpoint**: Metadata form captures all fields, location history works

---

## M16: Complete Multi-Protocol Simulator (3 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m16-simulator-complete`
**Goal**: Simulator supports all 12 analyzers + CI/CD integration
**User Stories**: US-9
**Depends On**: M4 (HL7 simulator base)

**Acceptance Criteria**:
1. Simulator supports ASTM, HL7, RS232, File protocols
2. Templates for all 12 analyzers with realistic data
3. QC results, patient results, error conditions
4. CI/CD pipeline can trigger test scenarios via HTTP
5. Concurrent multi-analyzer simulation works

### Setup for M16

- [ ] T226 [M16] Create branch `feat/150-madagascar-analyzer-integration-m16-simulator-complete` from `demo/madagascar`

### Implementation for M16

- [ ] T227 [M16] Create RS232 simulation via virtual serial ports (socat integration) in `tools/astm-http-bridge/src/serial/`
- [ ] T228 [M16] Create file generation for file-based analyzers in `tools/astm-http-bridge/src/file/CSVResultGenerator.java`
- [ ] T229 [M16] Create message templates for remaining 10 analyzers in `tools/astm-http-bridge/configs/`
- [ ] T230 [M16] Add QC result generation templates
- [ ] T231 [M16] Add error condition templates
- [ ] T232 [M16] Implement HTTP API test scenario endpoints (`/scenarios/{name}`)
- [ ] T233 [M16] Add concurrent multi-analyzer support
- [ ] T234 [M16] Update Docker configuration for CI/CD integration
- [ ] T235 [M16] Document all simulator capabilities in `tools/astm-http-bridge/README.md`

### Testing for M16

- [ ] T236 [M16] Test simulator with all 12 analyzer types
- [ ] T237 [M16] Test CI/CD HTTP API endpoints

### Finalization for M16

- [ ] T238 [M16] Verify simulator works with all protocols
- [ ] T239 [M16] Create PR `feat/150-madagascar-analyzer-integration-m16-simulator-complete` → `demo/madagascar`

**Checkpoint**: Simulator supports all 12 analyzers for CI/CD testing

---

## M17: E2E Validation and Madagascar Lab Testing (5 days)

**Branch**: `feat/150-madagascar-analyzer-integration-m17-e2e-validation`
**Goal**: Comprehensive E2E testing and production validation
**User Stories**: All
**Depends On**: M14, M15, M16

**Acceptance Criteria**:
1. All 12 analyzers receive results within 60 seconds
2. All 12 analyzers receive orders via export
3. 5+ analyzers operate simultaneously without issues
4. <5% mapping errors after initial configuration
5. Lab technicians complete configuration in <30 minutes
6. E2E tests pass in CI/CD pipeline

### Setup for M17

- [ ] T240 [M17] Create branch `feat/150-madagascar-analyzer-integration-m17-e2e-validation` from `demo/madagascar`

### Cypress E2E Tests for M17 (MANDATORY)

- [ ] T241 [P] [M17] E2E test for HL7 analyzer integration in `frontend/cypress/e2e/hl7AnalyzerIntegration.cy.js`
- [ ] T242 [P] [M17] E2E test for serial analyzer integration in `frontend/cypress/e2e/serialAnalyzerIntegration.cy.js`
- [ ] T243 [P] [M17] E2E test for file import integration in `frontend/cypress/e2e/fileImportIntegration.cy.js`
- [ ] T244 [P] [M17] E2E test for order export in `frontend/cypress/e2e/orderExport.cy.js`
- [ ] T245 [P] [M17] E2E test for instrument metadata form in `frontend/cypress/e2e/instrumentMetadata.cy.js`
- [ ] T246 [M17] Create Cypress fixtures for E2E tests in `frontend/cypress/fixtures/analyzers/`

### Integration Testing for M17

- [ ] T247 [M17] Performance test: 5+ concurrent analyzers via simulator
- [ ] T248 [M17] Stress test: 1000+ messages through system
- [ ] T249 [M17] Verify message routing with multiple simultaneous analyzers

### Madagascar Lab Validation for M17

- [ ] T250 [M17] Create user training materials in `docs/madagascar-training/`
- [ ] T251 [M17] Document configuration guide for each analyzer type
- [ ] T252 [M17] Remote validation with Madagascar lab technicians (video session)
- [ ] T253 [M17] Collect feedback and create bug fix tickets if needed

### Finalization for M17

- [ ] T254 [M17] Verify all E2E tests pass
- [ ] T255 [M17] Verify all 12 analyzers bidirectional
- [ ] T256 [M17] Run full test suite (`mvn verify && cd frontend && npm run cy:run`)
- [ ] T257 [M17] Create PR `feat/150-madagascar-analyzer-integration-m17-e2e-validation` → `demo/madagascar`
- [ ] T258 [M17] After approval, create final PR `demo/madagascar` → `develop` for integration

**Checkpoint**: Contract requirements met - 12 analyzers bidirectional, E2E tests pass

---

## Dependencies & Execution Order

### Milestone Dependencies

| Milestone | Depends On | Parallel Group |
|-----------|------------|----------------|
| M1 | - | Foundation (1-4) |
| M2 | - | Foundation (1-4) |
| M3 | - | Foundation (1-4) |
| M4 | - | Foundation (1-4) |
| M5 | M1, M2 | Validation |
| M6 | M1 | Validation |
| M7 | M1, M3 | Validation |
| M8 | M3 | Validation |
| M9 | M2 | New Plugins (9-13) |
| M10 | M2 | New Plugins (9-13) |
| M11 | M1, M2 | New Plugins (9-13) |
| M12 | M1 | New Plugins (9-13) |
| M13 | M3 | New Plugins (9-13) |
| M14 | M5-M13 | Integration |
| M15 | M14 | Integration |
| M16 | M4 | Integration |
| M17 | M14, M15, M16 | Validation |

### Parallel Opportunities

**Week 1-2**: M1, M2, M3, M4 can all proceed in parallel (4 developers)
**Week 2-3**: M5, M6, M7, M8 can start as dependencies complete
**Week 3-4**: M9, M10, M11, M12, M13 can all proceed in parallel (5 developers)
**Week 4-5**: M14 and M16 can proceed in parallel
**Week 5-6**: M17 after M14, M15, M16

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

| Week | Milestones | Cumulative Analyzers |
|------|------------|---------------------|
| 1 | M1, M2, M3, M4 | 0 (foundations) |
| 2 | M5, M6, M7, M8 | 9 analyzers |
| 3 | M9, M10, M11 | 11 analyzers |
| 4 | M12, M13, M14 | 12 analyzers + export |
| 5 | M15, M16 | + metadata + simulator |
| 6 | M17 | E2E validated |

### Parallel Team Strategy

**With 4+ developers**:

| Developer | Week 1-2 | Week 3-4 | Week 5-6 |
|-----------|----------|----------|----------|
| Dev A | M1 (HL7) | M5 (Mindray) → M14 | M17 |
| Dev B | M2 (RS232) | M9, M10 (Horiba) | M17 |
| Dev C | M3 (File) | M11, M12 (Stago, Abbott) | M15 |
| Dev D | M4 (Simulator) | M6, M7, M8, M13 | M16 |

---

## Task Summary

| Milestone | Total Tasks | Test Tasks | Implementation Tasks |
|-----------|-------------|------------|---------------------|
| M1 | 18 | 5 | 13 |
| M2 | 24 | 4 | 20 |
| M3 | 24 | 5 | 19 |
| M4 | 12 | 2 | 10 |
| M5 | 13 | 3 | 10 |
| M6 | 8 | 2 | 6 |
| M7 | 12 | 4 | 8 |
| M8 | 10 | 3 | 7 |
| M9 | 10 | 2 | 8 |
| M10 | 10 | 2 | 8 |
| M11 | 10 | 2 | 8 |
| M12 | 10 | 2 | 8 |
| M13 | 10 | 2 | 8 |
| M14 | 27 | 7 | 20 |
| M15 | 27 | 5 | 22 |
| M16 | 14 | 2 | 12 |
| M17 | 19 | 7 | 12 |
| **TOTAL** | **258** | **59** | **199** |

---

## Notes

- **[P]** = Can run in parallel (different files, no incomplete dependencies)
- **[M#]** = Milestone tag for traceability
- Tests are MANDATORY per Constitution Principle V
- Each milestone = 1 PR per Constitution Principle IX
- Run `mvn spotless:apply` and `cd frontend && npm run format` before every PR
- Use BOTH flags when skipping tests: `-DskipTests -Dmaven.test.skip=true`
- Run E2E tests individually during development: `npm run cy:run -- --spec "cypress/e2e/{test}.cy.js"`

---

**Tasks Generated**: 2026-01-22
**Total Tasks**: 258
**Test Tasks**: 59 (23%)
**Parallel Milestones**: M1-M4, M9-M13
**Contract Deadline**: 2026-02-28
