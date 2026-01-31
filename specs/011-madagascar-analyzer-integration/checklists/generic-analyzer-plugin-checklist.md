# Generic Analyzer Plugin Implementation Checklist

**Feature**: Unified Analyzer Dashboard with Generic Plugin Support **Plan**:
[generic-analyzer-plugin-plan.md](../plans/generic-analyzer-plugin-plan.md)
**Date**: 2026-01-30

---

## Pre-Implementation

- [ ] Review existing AnalyzerConfiguration entity
- [ ] Review existing plugin architecture (PluginLoader, PluginAnalyzerService)
- [ ] Confirm no conflicts with existing 004 dashboard features
- [ ] Identify existing test patterns to follow

---

## Phase 1: Database Schema

### Liquibase Changeset

- [ ] Create
      `src/main/resources/liquibase/2.9.x/analyzer_identifier_pattern.xml`
- [ ] Add `identifier_pattern` column (VARCHAR(255), nullable)
- [ ] Add `is_generic_plugin` column (BOOLEAN, default FALSE)
- [ ] Include changeset in `db.changelog-2.9.xml`

### Verification

- [ ] Run `mvn liquibase:update -Dliquibase.contexts=default`
- [ ] Verify columns exist in database:
  ```sql
  SELECT column_name FROM information_schema.columns
  WHERE table_name='analyzer_configuration'
  AND column_name IN ('identifier_pattern', 'is_generic_plugin');
  ```

---

## Phase 2: Entity Updates

### AnalyzerConfiguration.java

- [ ] Add `identifierPattern` field with `@Column` annotation
- [ ] Add `isGenericPlugin` field with `@Column` annotation
- [ ] Add getter/setter for `identifierPattern`
- [ ] Add getter/setter for `isGenericPlugin`

### Verification

- [ ] Run `mvn compile` - no errors
- [ ] Entity field mapping matches database columns

---

## Phase 3: Service Layer

### AnalyzerConfigurationService Interface

- [ ] Add `findByIdentifierPatternMatch(String analyzerIdentifier)` method
- [ ] Add `getGenericPluginConfigurations()` method

### AnalyzerConfigurationServiceImpl

- [ ] Implement `findByIdentifierPatternMatch()` with regex matching
- [ ] Handle `PatternSyntaxException` gracefully
- [ ] Implement `getGenericPluginConfigurations()`

### Unit Tests

- [ ] Create test for pattern matching (exact match)
- [ ] Create test for pattern matching (regex match)
- [ ] Create test for no match scenario
- [ ] Create test for invalid regex pattern handling

### Verification

- [ ] Run `mvn test -Dtest=AnalyzerConfigurationServiceTest`

---

## Phase 4: GenericASTM Plugin

### Directory Structure

- [ ] Create `plugins/analyzers/GenericASTM/` directory
- [ ] Create `src/main/java/org/openelisglobal/plugins/analyzer/genericastm/`
      package

### pom.xml

- [ ] Create pom.xml with correct parent
- [ ] Add plugin dependencies (openelisglobal-core)
- [ ] Configure maven-jar-plugin for manifest

### plugin.xml

- [ ] Create plugin descriptor
- [ ] Set version to "1.0"
- [ ] Define extension point for analyzerImporter

### GenericASTMAnalyzer.java

- [ ] Implement `AnalyzerImporterPlugin` interface
- [ ] Implement `isTargetAnalyzer()` - parse H-segment, query DB for pattern
      match
- [ ] Implement `getAnalyzerLineInserter()` - return GenericASTMLineInserter
- [ ] Implement `isAnalyzerResult()` - check for R records
- [ ] Implement `connect()` - register plugin WITHOUT creating analyzer entries
- [ ] Add `getAnalyzerResponder()` - return null (no bidirectional support
      initially)

### GenericASTMLineInserter.java

- [ ] Extend `AnalyzerLineInserter`
- [ ] Load test mappings from DB in `insert()` method
- [ ] Parse ASTM P (Patient), O (Order), R (Result) records
- [ ] Apply test mappings (analyzer_test_name → test_id)
- [ ] Insert results via AnalyzerResultsService
- [ ] Handle unmapped tests with warning log

### ASTMMessageParser.java

- [ ] Create utility class for ASTM parsing
- [ ] Parse H-segment for analyzer identification
- [ ] Parse P-segment for patient info
- [ ] Parse O-segment for sample/order info
- [ ] Parse R-segments for results
- [ ] Handle `^^^CODE` format in test field

### Build & Deploy

- [ ] Run `cd plugins/analyzers/GenericASTM && mvn clean package`
- [ ] Copy JAR to `/var/lib/openelis-global/plugins/`
- [ ] Restart OpenELIS
- [ ] Verify plugin loads: check logs for "GenericASTM plugin registered"

---

## Phase 5: REST API

### AnalyzerRestController.java

- [ ] Add `@PostMapping("/generic")` endpoint
- [ ] Create `GenericAnalyzerCreateDTO` class
- [ ] Accept: name, description, identifierPattern, host, port, testMappings[]
- [ ] Create Analyzer with configuration
- [ ] Create test mappings from DTO
- [ ] Return created analyzer DTO

### Verification

- [ ] Test with curl:
  ```bash
  curl -X POST https://localhost/api/v1/analyzers/generic \
    -H "Content-Type: application/json" \
    -d '{"name":"Test Analyzer","identifierPattern":"TEST\\^ASTM.*"}'
  ```

---

## Phase 6: Frontend UI

### AnalyzerConfigForm.jsx

- [ ] Add TextInput for `identifierPattern`
- [ ] Add Checkbox for `isGenericPlugin`
- [ ] Add helper text explaining pattern syntax
- [ ] Show info notification when generic plugin enabled
- [ ] Wire up onChange handlers

### Localization

- [ ] Add `analyzer.identifierPattern` translation key
- [ ] Add `analyzer.identifierPattern.help` translation key
- [ ] Add `analyzer.isGenericPlugin` translation key
- [ ] Add `analyzer.genericPlugin.info.title` translation key
- [ ] Add `analyzer.genericPlugin.info.subtitle` translation key

### Verification

- [ ] Run `cd frontend && npm run format`
- [ ] Run `cd frontend && npm run build`
- [ ] Visual test: open Analyzer form, verify new fields appear

---

## Phase 7: Integration Testing

### Create Test Analyzer

- [ ] Navigate to Admin → Analyzer Management
- [ ] Click "Add Analyzer"
- [ ] Enter name: "Integration Test Analyzer"
- [ ] Enter identifier pattern: `INTTEST\\^ASTM.*`
- [ ] Check "Generic Plugin Managed"
- [ ] Save analyzer

### Configure Test Mappings

- [ ] Navigate to test mappings for new analyzer
- [ ] Add mapping: WBC → White Blood Cells test
- [ ] Add mapping: RBC → Red Blood Cells test
- [ ] Save mappings

### Test ASTM Import

- [ ] Send test ASTM message:
  ```bash
  curl -X POST https://localhost/api/OpenELIS-Global/analyzer/astm \
    -H "Content-Type: text/plain" \
    -d 'H|\^&|||INTTEST^ASTM^1.0|||...
  P|1||PAT001
  O|1|SAMPLE001
  R|1|^^^WBC|5.8|10^3/uL
  R|2|^^^RBC|4.5|10^6/uL
  L|1|N'
  ```
- [ ] Verify 200 response
- [ ] Check analyzer_results table for new entries
- [ ] Verify test_id populated correctly from mappings

### Legacy Plugin Verification

- [ ] Confirm HoribaPentra60 plugin still works (if available)
- [ ] Confirm message routing order: legacy plugins checked first
- [ ] Confirm no changes to PluginLoader behavior

---

## Final Verification

### Code Quality

- [ ] Run `mvn spotless:apply`
- [ ] Run `cd frontend && npm run format`
- [ ] No compilation errors
- [ ] No linter warnings

### Regression Testing

- [ ] Run backend unit tests: `mvn test`
- [ ] Run frontend tests: `cd frontend && npm test`
- [ ] Spot check 3+ legacy plugins still function

### Documentation

- [ ] Update 004 dashboard documentation (if exists)
- [ ] Add inline code comments for complex logic
- [ ] Update AGENTS.md if needed

### Commit Checklist

- [ ] All new files added to git
- [ ] Commit message follows convention
- [ ] No sensitive data committed
- [ ] PR description includes testing steps

---

## Post-Implementation

### Future Enhancements (Out of Scope for Now)

- [ ] GenericHL7 plugin (same pattern as GenericASTM)
- [ ] GenericCSV plugin (same pattern as GenericASTM)
- [ ] Test message simulator in UI
- [ ] Dry-run import mode
- [ ] Plugin auto-discovery of configured patterns

### Monitoring

- [ ] Monitor logs for pattern matching errors
- [ ] Monitor for unmapped test warnings
- [ ] Track usage of generic vs legacy plugins
