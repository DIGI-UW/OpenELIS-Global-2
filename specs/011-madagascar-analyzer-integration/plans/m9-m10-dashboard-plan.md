# Unified Analyzer Dashboard with Generic Plugin Support

**Feature**: 004-analyzer-management + 011-madagascar-analyzer-integration
**Branch**: `feat/011-madagascar-analyzer-integration-m9-m10-horiba` **Date**:
2026-01-30 **Context**: Dashboard as unified entrypoint for ALL analyzers;
generic plugins for new analyzer types

---

## Executive Summary

### Approach: Dashboard as Unified Entrypoint

The 004 Dashboard becomes the **single entrypoint** for configuring ALL
analyzers - both legacy plugin-based and new generic plugin-based. All legacy
workflows remain intact.

```
Architecture:
                    ┌─────────────────────────────────────┐
                    │     004 ANALYZER DASHBOARD          │
                    │   (Unified Configuration UI)        │
                    └─────────────────┬───────────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              │                       │                       │
              ▼                       ▼                       ▼
    ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
    │  Legacy Plugins │     │  Generic Plugins │     │  Future Plugins │
    │  (HoribaPentra, │     │  (GenericASTM,   │     │  (GenericHL7,   │
    │   Sysmex, etc.) │     │   configured via │     │   GenericCSV)   │
    │                 │     │   dashboard)     │     │                 │
    │ - Self-register │     │ - Dashboard-     │     │                 │
    │ - Hardcoded     │     │   configured     │     │                 │
    │   mappings      │     │ - DB mappings    │     │                 │
    └─────────────────┘     └─────────────────┘     └─────────────────┘
              │                       │                       │
              └───────────────────────┴───────────────────────┘
                                      │
                    ┌─────────────────┴───────────────────┐
                    │    EXISTING PLUGIN INFRASTRUCTURE   │
                    │    (PluginLoader, PluginAnalyzer-   │
                    │     Service - ALL UNCHANGED)        │
                    └─────────────────────────────────────┘
```

**Key Principles**:

1. Dashboard is the **unified UI** for ALL analyzer configuration
2. Legacy plugins continue working **exactly as before** (no code changes)
3. Generic plugins enable **new analyzers without custom Java code**
4. **Zero legacy workflow changes** - all existing paths preserved

---

## Goals

1. ✅ **Unified Entrypoint**: Dashboard manages ALL analyzers (legacy + generic)
2. ✅ **Legacy Preserved**: All 20+ existing plugins work exactly as before, no
   workflow changes
3. ✅ **Generic Plugins**: Create GenericASTM (and later GenericHL7, GenericCSV)
   for new analyzers
4. ✅ **Dashboard-Configured**: Generic plugins read configuration from DB (set
   via UI)
5. ✅ **No Core Changes**: Work within existing
   PluginLoader/PluginAnalyzerService infrastructure
6. ✅ **Migration Path**: New analyzers use generic plugins; legacy plugins stay
   as-is forever

---

## Architecture

### Dashboard as Unified Entrypoint

The 004 Dashboard provides a **single UI** for all analyzer management:

| Function          | Legacy Plugins                       | Generic Plugins                          |
| ----------------- | ------------------------------------ | ---------------------------------------- |
| **View Analyzer** | ✅ Shows plugin-registered analyzers | ✅ Shows dashboard-created analyzers     |
| **Edit Config**   | ✅ Connection settings, status       | ✅ Full configuration including mappings |
| **Test Mappings** | ✅ Override via MappingAwareInserter | ✅ Primary configuration method          |
| **Create New**    | ❌ Requires Java plugin              | ✅ Via dashboard + generic plugin        |

### How Generic Plugins Differ from Legacy Plugins

| Aspect               | Legacy Plugin (e.g., HoribaPentra60) | Generic Plugin (e.g., GenericASTM)                       |
| -------------------- | ------------------------------------ | -------------------------------------------------------- |
| **Registration**     | `connect()` creates analyzer in DB   | Dashboard creates analyzer in DB                         |
| **Test Mappings**    | Hardcoded in `connect()`             | Read from `analyzer_test_mapping` (UI-configured)        |
| **Analyzer ID**      | Hardcoded `isTargetAnalyzer()`       | Pattern from `analyzer_configuration.identifier_pattern` |
| **Cardinality**      | Plugin = 1 specific analyzer         | Plugin = many analyzers (each with own config)           |
| **Configuration**    | Edit Java code, rebuild JAR          | Edit via 004 Dashboard UI                                |
| **Workflow Changes** | None - works exactly as before       | None - uses same insert flow                             |

### Legacy Workflows Preserved (NO CHANGES)

The following workflows remain **100% unchanged**:

1. **Plugin Loading**: `PluginLoader` scans `/var/lib/openelis-global/plugins/`
   at startup
2. **Plugin Registration**: Plugin `connect()` → `addAnalyzerDatabaseParts()` →
   analyzer in DB
3. **Message Routing**: `ASTMAnalyzerReader.setInserterResponder()` iterates all
   plugins
4. **Plugin Matching**: Each plugin's `isTargetAnalyzer(lines)` called in
   sequence
5. **Result Insertion**: Plugin's `AnalyzerLineInserter.insert()` processes
   results
6. **Mapping Override**: `MappingAwareAnalyzerLineInserter` wraps plugin
   inserters (004 feature)

### Generic Plugin Flow (NEW - Additive)

```
1. ASTM Message arrives at /analyzer/astm endpoint
                    ↓
2. ASTMAnalyzerReader calls setInserterResponder()
                    ↓
3. Iterates ALL plugins (legacy first, then generic):
   - HoribaPentra60.isTargetAnalyzer() → false
   - HoribaMicros60.isTargetAnalyzer() → false
   - ... other legacy plugins ...
   - GenericASTMPlugin.isTargetAnalyzer() → checks DB patterns
                    ↓
4. GenericASTMPlugin:
   a. Parse H-segment for manufacturer/model
   b. Query analyzer_configuration WHERE identifier_pattern MATCHES
   c. If match found → return TRUE
                    ↓
5. GenericASTMPlugin.getAnalyzerLineInserter()
   → Returns GenericASTMLineInserter(matchedAnalyzerId)
                    ↓
6. GenericASTMLineInserter.insert(lines, userId):
   a. Parse ASTM message into results
   b. Load test mappings from DB for this analyzer
   c. Apply mappings (analyzer_test_name → test_id)
   d. Insert into analyzer_results
```

**Note**: Generic plugin is just another plugin in the iteration - legacy
plugins still get first chance to match.

---

## Implementation Phases

### Phase 1: Database Schema Enhancement

Add `identifier_pattern` column to `analyzer_configuration` for pattern
matching:

**File**: `src/main/resources/liquibase/2.9.x/analyzer_identifier_pattern.xml`

```xml
<changeSet id="add-identifier-pattern-to-analyzer-config" author="claude">
  <addColumn tableName="analyzer_configuration" schemaName="clinlims">
    <column name="identifier_pattern" type="VARCHAR(255)">
      <comment>Regex pattern to match analyzer from ASTM H-segment (e.g., "GENERIC\\^ASTM.*")</comment>
    </column>
    <column name="is_generic_plugin" type="BOOLEAN" defaultValueBoolean="false">
      <comment>TRUE if managed by a generic plugin (GenericASTM, etc.)</comment>
    </column>
  </addColumn>
</changeSet>
```

### Phase 2: GenericASTM Plugin

**Directory**: `plugins/analyzers/GenericASTM/`

```
plugins/analyzers/GenericASTM/
├── pom.xml
├── plugin.xml
└── src/main/java/org/openelisglobal/plugins/analyzer/genericastm/
    ├── GenericASTMAnalyzer.java       # Main plugin class
    ├── GenericASTMLineInserter.java   # Line inserter
    └── ASTMMessageParser.java         # ASTM parsing logic
```

**GenericASTMAnalyzer.java**:

```java
public class GenericASTMAnalyzer implements AnalyzerImporterPlugin {

    private AnalyzerConfigurationService configService;
    private String matchedAnalyzerId;

    @Override
    public boolean isTargetAnalyzer(List<String> lines) {
        // Parse H-segment to get analyzer identifier
        String analyzerIdent = parseAnalyzerIdentifier(lines);
        if (analyzerIdent == null) return false;

        // Query DB for matching configuration with identifier_pattern
        configService = SpringContext.getBean(AnalyzerConfigurationService.class);
        Optional<AnalyzerConfiguration> config = configService
            .findByIdentifierPatternMatch(analyzerIdent);

        if (config.isPresent() && config.get().getIsGenericPlugin()) {
            matchedAnalyzerId = config.get().getAnalyzer().getId();
            return true;
        }
        return false;
    }

    @Override
    public AnalyzerLineInserter getAnalyzerLineInserter() {
        return new GenericASTMLineInserter(matchedAnalyzerId);
    }

    @Override
    public boolean isAnalyzerResult(List<String> lines) {
        // Check if message contains R (Result) records
        return lines.stream().anyMatch(line -> line.startsWith("R|"));
    }

    private String parseAnalyzerIdentifier(List<String> lines) {
        // Find H-segment: H|\^&|||MANUFACTURER^MODEL^VERSION|...
        for (String line : lines) {
            if (line.startsWith("H|")) {
                String[] segments = line.split("\\|");
                if (segments.length >= 5) {
                    return segments[4].trim(); // e.g., "GENERIC^ASTM^1.0"
                }
            }
        }
        return null;
    }

    @Override
    public void connect() {
        // Register plugin but DON'T create analyzer entries
        // Analyzers are created via dashboard, not plugin registration
        PluginAnalyzerService.getInstance().registerAnalyzerPlugin(this);
        LogEvent.logInfo(this.getClass().getName(), "connect",
            "GenericASTM plugin registered - analyzers configured via dashboard");
    }
}
```

**GenericASTMLineInserter.java**:

```java
public class GenericASTMLineInserter extends AnalyzerLineInserter {

    private final String analyzerId;
    private final AnalyzerTestMappingService mappingService;
    private final AnalyzerResultsService resultsService;

    public GenericASTMLineInserter(String analyzerId) {
        this.analyzerId = analyzerId;
        this.mappingService = SpringContext.getBean(AnalyzerTestMappingService.class);
        this.resultsService = SpringContext.getBean(AnalyzerResultsService.class);
    }

    @Override
    public boolean insert(List<String> lines, String currentUserId) {
        try {
            // 1. Load test mappings from database (UI-configured)
            List<AnalyzerTestMapping> mappings = mappingService.getByAnalyzerId(analyzerId);
            Map<String, String> testNameToId = mappings.stream()
                .collect(Collectors.toMap(
                    AnalyzerTestMapping::getAnalyzerTestName,
                    AnalyzerTestMapping::getTestId
                ));

            // 2. Parse ASTM message
            List<AnalyzerResults> results = parseASTMResults(lines);

            // 3. Apply test mappings
            for (AnalyzerResults result : results) {
                String testId = testNameToId.get(result.getAnalyzerTestName());
                if (testId != null) {
                    result.setTestId(testId);
                    result.setAnalyzerId(analyzerId);
                    result.setSysUserId(currentUserId);
                } else {
                    LogEvent.logWarn(this.getClass().getName(), "insert",
                        "No mapping found for test: " + result.getAnalyzerTestName());
                }
            }

            // 4. Insert results
            resultsService.insertAll(results);
            return true;

        } catch (Exception e) {
            error = "Failed to process ASTM message: " + e.getMessage();
            LogEvent.logError(e);
            return false;
        }
    }

    private List<AnalyzerResults> parseASTMResults(List<String> lines) {
        // Implementation: Parse P, O, R records from ASTM message
        // ...
    }
}
```

### Phase 3: AnalyzerConfigurationService Enhancement

**File**:
`src/main/java/org/openelisglobal/analyzer/service/AnalyzerConfigurationService.java`

Add method to find by pattern match:

```java
public interface AnalyzerConfigurationService {
    // Existing methods...

    /**
     * Find analyzer configuration where identifier_pattern matches the given identifier
     * Used by generic plugins to identify which analyzer sent the message
     */
    Optional<AnalyzerConfiguration> findByIdentifierPatternMatch(String analyzerIdentifier);

    /**
     * Get all configurations marked as generic plugin managed
     */
    List<AnalyzerConfiguration> getGenericPluginConfigurations();
}
```

### Phase 4: 004 Dashboard UI Updates

**File**:
`frontend/src/components/admin/AnalyzerManagement/AnalyzerConfigForm.jsx`

Add fields for generic plugin configuration:

```jsx
{/* Identifier Pattern - for generic plugins */}
<TextInput
  id="identifier-pattern"
  labelText={intl.formatMessage({ id: "analyzer.identifierPattern" })}
  helperText={intl.formatMessage({ id: "analyzer.identifierPattern.help" })}
  value={formData.identifierPattern || ''}
  onChange={(e) => handleChange('identifierPattern', e.target.value)}
  placeholder="MANUFACTURER\\^MODEL.*"
/>

<Checkbox
  id="is-generic-plugin"
  labelText={intl.formatMessage({ id: "analyzer.isGenericPlugin" })}
  checked={formData.isGenericPlugin || false}
  onChange={(e) => handleChange('isGenericPlugin', e.target.checked)}
/>
```

### Phase 5: REST API Updates

**File**:
`src/main/java/org/openelisglobal/analyzer/controller/rest/AnalyzerRestController.java`

Add endpoint for creating generic-plugin-managed analyzers:

```java
@PostMapping("/generic")
public ResponseEntity<AnalyzerDTO> createGenericAnalyzer(
        @RequestBody GenericAnalyzerCreateDTO dto) {
    // Create analyzer and configuration with generic plugin settings
    // ...
}
```

---

## Critical Files

### To Create (6 files)

1. `src/main/resources/liquibase/2.9.x/analyzer_identifier_pattern.xml` - Schema
2. `plugins/analyzers/GenericASTM/pom.xml` - Plugin build
3. `plugins/analyzers/GenericASTM/plugin.xml` - Plugin descriptor
4. `plugins/analyzers/GenericASTM/src/.../GenericASTMAnalyzer.java` - Plugin
   main
5. `plugins/analyzers/GenericASTM/src/.../GenericASTMLineInserter.java` -
   Inserter
6. `plugins/analyzers/GenericASTM/src/.../ASTMMessageParser.java` - Parser

### To Modify (5 files)

1. `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerConfiguration.java` -
   Add fields
2. `src/main/java/org/openelisglobal/analyzer/service/AnalyzerConfigurationService.java` -
   Pattern match
3. `src/main/java/org/openelisglobal/analyzer/service/AnalyzerConfigurationServiceImpl.java` -
   Impl
4. `frontend/src/components/admin/AnalyzerManagement/AnalyzerConfigForm.jsx` -
   UI fields
5. `src/main/resources/liquibase/2.9.x/db.changelog-2.9.xml` - Include changeset

---

## What This Plan Does NOT Change

To be absolutely clear, the following are **NOT modified**:

1. ❌ `PluginLoader.java` - No changes
2. ❌ `PluginAnalyzerService.java` - No changes
3. ❌ `ASTMAnalyzerReader.setInserterResponder()` - No changes (generic is just
   another plugin)
4. ❌ `AnalyzerImporterPlugin` interface - No changes
5. ❌ Any existing plugin code - No changes
6. ❌ Plugin JAR loading mechanism - No changes
7. ❌ Message routing logic - No changes

---

## Summary

| Aspect              | This Approach                            |
| ------------------- | ---------------------------------------- |
| **Scope**           | 6 new files, 5 modified (all additive)   |
| **Risk**            | Low - purely additive, no legacy changes |
| **Legacy Impact**   | Zero - all workflows preserved           |
| **Time Estimate**   | 1-2 weeks                                |
| **Dashboard Role**  | Unified entrypoint for ALL analyzers     |
| **Generic Plugins** | Enable new analyzers without Java code   |

The dashboard becomes the **single configuration UI** for all analyzers while
the generic plugin pattern enables creating new analyzers without writing custom
Java plugins.
