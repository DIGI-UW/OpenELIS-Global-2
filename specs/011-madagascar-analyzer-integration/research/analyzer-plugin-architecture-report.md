# OpenELIS Analyzer Plugin Architecture Report

**Created:** 2026-01-30 **Purpose:** Comprehensive analysis of how analyzer
plugins work, their integration with the 004 ASTM dashboard, and bidirectional
workflow support.

---

## Table of Contents

1. [Plugin System Overview](#1-plugin-system-overview)
2. [Plugin Loading and Registration](#2-plugin-loading-and-registration)
3. [Core Plugin Interfaces](#3-core-plugin-interfaces)
4. [Message Flow and Processing](#4-message-flow-and-processing)
5. [Integration with 004 Analyzer Dashboard](#5-integration-with-004-analyzer-dashboard)
6. [Bidirectional Workflows](#6-bidirectional-workflows)
7. [Field Queries and Responder Pattern](#7-field-queries-and-responder-pattern)
8. [Supported Analyzer Types](#8-supported-analyzer-types)
9. [Adding New Analyzers](#9-adding-new-analyzers)

---

## 1. Plugin System Overview

OpenELIS uses a **JAR-based plugin architecture** for analyzer integration. Each
analyzer type is packaged as an independent JAR file that can be deployed
without modifying the core application.

### Key Characteristics

| Aspect               | Description                                         |
| -------------------- | --------------------------------------------------- |
| **Location**         | `/var/lib/openelis-global/plugins/`                 |
| **Discovery**        | Automatic at application startup via `PluginLoader` |
| **Configuration**    | XML descriptor inside each JAR                      |
| **Registration**     | Via `PluginAnalyzerService` singleton               |
| **Protocol Support** | ASTM LIS2-A2, HL7, CSV/File, HTTP                   |

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Plugin Loading Flow                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  /var/lib/openelis-global/plugins/                              │
│     ├── HoribaPentra60.jar                                      │
│     ├── SysmexXN-L.jar                                          │
│     └── GeneXpert.jar                                           │
│              │                                                  │
│              ▼                                                  │
│    ┌─────────────────┐                                          │
│    │  PluginLoader   │  @PostConstruct                          │
│    │  (Spring Bean)  │  Scans JAR files                         │
│    └────────┬────────┘                                          │
│             │                                                   │
│             ▼                                                   │
│    ┌─────────────────┐                                          │
│    │   Parse XML     │  plugin.xml inside each JAR              │
│    │   Descriptor    │                                          │
│    └────────┬────────┘                                          │
│             │                                                   │
│             ▼                                                   │
│    ┌─────────────────┐                                          │
│    │ Load & Connect  │  ClassLoader.loadClass()                 │
│    │ Plugin Instance │  → plugin.connect()                      │
│    └────────┬────────┘                                          │
│             │                                                   │
│             ▼                                                   │
│    ┌───────────────────────┐                                    │
│    │ PluginAnalyzerService │  Singleton registry                │
│    │  • registerAnalyzer() │  of all active plugins             │
│    │  • addAnalyzerParts() │                                    │
│    └───────────────────────┘                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Plugin Loading and Registration

### PluginLoader Component

**File:** `src/main/java/org/openelisglobal/plugin/PluginLoader.java`

```java
@Component
public class PluginLoader {
    public static final String PLUGIN_ANALYZER = "/var/lib/openelis-global/plugins/";

    @PostConstruct
    private void load() {
        File pluginDir = new File(PLUGIN_ANALYZER);
        loadDirectory(pluginDir);  // Recursively scans for JAR files
    }
}
```

### Plugin XML Descriptor Format

Each plugin JAR contains an XML descriptor (e.g., `plugin.xml`):

```xml
<plugin>
  <version>1.0</version>
  <analyzerImporter>
    <extension_point>
      <description value="Horiba ABX Pentra 60 Hematology Analyzer"/>
      <extension path="uw.edu.itech.HoribaPentra60.HoribaPentra60Analyzer"/>
    </extension_point>
  </analyzerImporter>
</plugin>
```

### Registration via PluginAnalyzerService

**File:**
`src/main/java/org/openelisglobal/common/services/PluginAnalyzerService.java`

When a plugin's `connect()` method is called:

```java
@Override
public boolean connect() {
    // 1. Define test mappings (analyzer code → OpenELIS test)
    List<PluginAnalyzerService.TestMapping> testMappings = createTestMappings();

    // 2. Register analyzer in database and cache
    PluginAnalyzerService.getInstance()
        .addAnalyzerDatabaseParts(ANALYZER_NAME, ANALYZER_DESCRIPTION, testMappings, true);

    // 3. Register plugin for message routing
    PluginAnalyzerService.getInstance().registerAnalyzer(this);

    return true;
}
```

---

## 3. Core Plugin Interfaces

### AnalyzerImporterPlugin Interface

**File:** `src/main/java/org/openelisglobal/plugin/AnalyzerImporterPlugin.java`

```java
public interface AnalyzerImporterPlugin extends APlugin {
    // Identify if incoming message is from this analyzer
    boolean isTargetAnalyzer(List<String> lines);

    // Get the line processor for this analyzer
    AnalyzerLineInserter getAnalyzerLineInserter();

    // Check if message contains results (vs. queries)
    default boolean isAnalyzerResult(List<String> lines) { return true; }

    // Get responder for bidirectional communication (optional)
    default AnalyzerResponder getAnalyzerResponder() { return null; }
}
```

### Method Responsibilities

| Method                      | Purpose                                                | Required           |
| --------------------------- | ------------------------------------------------------ | ------------------ |
| `isTargetAnalyzer()`        | Pattern match ASTM header to identify this analyzer    | Yes                |
| `getAnalyzerLineInserter()` | Return processor that parses results → OpenELIS format | Yes                |
| `isAnalyzerResult()`        | Distinguish result messages from query messages        | No (default: true) |
| `getAnalyzerResponder()`    | Return responder for bidirectional queries             | No (default: null) |

### AnalyzerLineInserter Abstract Class

Processes parsed lines into OpenELIS result records:

```java
public abstract class AnalyzerLineInserter {
    public abstract boolean insert(List<String> lines, String systemUserId);
    public String getError() { return error; }
}
```

### AnalyzerResponder Interface

For bidirectional analyzers that send queries:

```java
public interface AnalyzerResponder {
    String buildResponse(List<String> lines);
}
```

---

## 4. Message Flow and Processing

### ASTM Message Reception Flow

**File:**
`src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java`

```
┌──────────────────────────────────────────────────────────────────┐
│                   ASTM Message Processing Flow                   │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  HTTP POST /api/analyzer/import                                  │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────────┐                                         │
│  │  ASTMAnalyzerReader │  readStream()                           │
│  │  • Parse message    │  • Detect charset encoding              │
│  │  • Read lines       │  • Buffer all lines                     │
│  └──────────┬──────────┘                                         │
│             │                                                    │
│             ▼                                                    │
│  ┌─────────────────────┐                                         │
│  │ setInserterResponder│  Find matching plugin:                  │
│  │                     │  for (plugin : plugins) {               │
│  │                     │    if (plugin.isTargetAnalyzer(lines))  │
│  │                     │      inserter = plugin.getLineInserter()│
│  └──────────┬──────────┘                                         │
│             │                                                    │
│             ▼                                                    │
│  ┌─────────────────────┐                                         │
│  │   processData()     │  Branch by message type:                │
│  │                     │                                         │
│  │  if (isAnalyzerResult) ──► insertAnalyzerData()               │
│  │  else ───────────────────► buildResponseForQuery()            │
│  └──────────┬──────────┘                                         │
│             │                                                    │
│             ▼                                                    │
│  ┌─────────────────────────────────────────────┐                 │
│  │         For RESULT messages:                 │                 │
│  │  wrapInserterIfMappingsExist()              │                 │
│  │    ├── Check if analyzer has 004 mappings   │                 │
│  │    ├── If yes: MappingAwareAnalyzerLineInserter              │
│  │    └── If no:  Original plugin inserter     │                 │
│  └─────────────────────────────────────────────┘                 │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Analyzer Identification Pattern

Each plugin implements header matching:

```java
// HoribaMicros60Analyzer.java
@Override
public boolean isTargetAnalyzer(List<String> lines) {
    for (String line : lines) {
        if (line != null && line.startsWith("H|")) {
            String upperLine = line.toUpperCase();
            // Primary: "MICROS60"
            if (upperLine.contains("MICROS60")) return true;
            // Fallback: "ABX" + "MICROS" but not "PENTRA"
            if (upperLine.contains("ABX") &&
                upperLine.contains("MICROS") &&
                !upperLine.contains("PENTRA")) return true;
        }
    }
    return false;
}
```

---

## 5. Integration with 004 Analyzer Dashboard

### Overview

The **004-astm-analyzer-mapping** feature adds a UI-driven mapping layer that
works **alongside** the plugin system, not replacing it.

### Two-Layer Architecture

```
┌───────────────────────────────────────────────────────────────────┐
│                    004 Analyzer Dashboard Integration             │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Layer 1: Plugin-based Mappings (Original)                        │
│  ┌────────────────────────────────────────┐                       │
│  │  • Defined in Java code                │                       │
│  │  • Compiled into plugin JAR            │                       │
│  │  • Fixed at deployment time            │                       │
│  │  • Uses analyzer_test_map table        │                       │
│  └────────────────────────────────────────┘                       │
│                                                                   │
│  Layer 2: 004 UI-configured Mappings (New)                        │
│  ┌────────────────────────────────────────┐                       │
│  │  • Configured via Analyzer Dashboard   │                       │
│  │  • Stored in analyzer_field_mapping    │                       │
│  │  • Modifiable at runtime               │                       │
│  │  • Supports qualitative + unit mapping │                       │
│  └────────────────────────────────────────┘                       │
│                                                                   │
│  Integration Point: MappingAwareAnalyzerLineInserter              │
│  ┌────────────────────────────────────────────────────┐           │
│  │  1. Check if analyzer has active 004 mappings      │           │
│  │  2. If YES: Apply mappings, then delegate to plugin│           │
│  │  3. If NO:  Use plugin directly (backward compat)  │           │
│  └────────────────────────────────────────────────────┘           │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

### Key 004 Tables

| Table                        | Purpose                                                   |
| ---------------------------- | --------------------------------------------------------- |
| `analyzer_configuration`     | Analyzer connection settings (IP, port, protocol, status) |
| `analyzer_field`             | Fields discovered from analyzer via "Query Analyzer"      |
| `analyzer_field_mapping`     | Maps analyzer fields → OpenELIS fields                    |
| `qualitative_result_mapping` | Maps qualitative values (POS→POSITIVE, etc.)              |
| `unit_mapping`               | Unit conversions (mmol/L→mg/dL with factor)               |
| `analyzer_error`             | Errors for unmapped fields, connection issues             |

### Conditional Wrapping Logic

**File:**
`src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java`

```java
private AnalyzerLineInserter wrapInserterIfMappingsExist(AnalyzerLineInserter originalInserter) {
    // Identify analyzer from message
    Optional<Analyzer> analyzer = identifyAnalyzerFromMessage();

    if (!analyzer.isPresent()) {
        // Cannot identify → use original (backward compatibility)
        return originalInserter;
    }

    // Check for 004 UI-configured mappings
    if (mappingApplicationService.hasActiveMappings(analyzer.get().getId())) {
        // Wrap with mapping-aware inserter
        return new MappingAwareAnalyzerLineInserter(originalInserter, analyzer.get());
    }

    // No 004 mappings → use original plugin inserter
    return originalInserter;
}
```

### MappingAwareAnalyzerLineInserter

**File:**
`src/main/java/org/openelisglobal/analyzer/service/MappingAwareAnalyzerLineInserter.java`

Wrapper that:

1. Receives raw ASTM segments from ASTMAnalyzerReader
2. Calls `MappingApplicationService.applyMappings()` to transform using 004
   config
3. Creates `AnalyzerError` records for unmapped fields
4. Delegates transformed data to original plugin inserter

---

## 6. Bidirectional Workflows

### Overview

Some analyzers support **bidirectional communication**:

- **Inbound (Results):** Analyzer → OpenELIS (result data)
- **Outbound (Queries):** Analyzer → OpenELIS (request patient/order info) →
  Response

### Detecting Query vs. Result Messages

```java
// ASTMAnalyzerReader.java
public boolean processData(String currentUserId) {
    if (plugin.isAnalyzerResult(lines)) {
        // This is a result message - insert into database
        return insertAnalyzerData(currentUserId);
    } else {
        // This is a query - build and return response
        responseBody = buildResponseForQuery();
        hasResponse = true;
        return true;
    }
}
```

### Query/Response Flow

```
┌────────────────────────────────────────────────────────────────┐
│                  Bidirectional Query Flow                      │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Analyzer                           OpenELIS                   │
│     │                                  │                       │
│     │ Q-segment query (order info)     │                       │
│     │─────────────────────────────────►│                       │
│     │                                  │                       │
│     │                         ┌────────┴────────┐              │
│     │                         │ isAnalyzerResult│              │
│     │                         │    = false      │              │
│     │                         └────────┬────────┘              │
│     │                                  │                       │
│     │                         ┌────────┴────────┐              │
│     │                         │ getAnalyzer     │              │
│     │                         │   Responder()   │              │
│     │                         └────────┬────────┘              │
│     │                                  │                       │
│     │                         ┌────────┴────────┐              │
│     │                         │ buildResponse() │              │
│     │                         │ Lookup patient  │              │
│     │                         │ Build P/O segs  │              │
│     │                         └────────┬────────┘              │
│     │                                  │                       │
│     │◄─────────────────────────────────┤                       │
│     │  P|1||PATIENT-001|||M|19850101   │                       │
│     │  O|1|SAMPLE-001|||...            │                       │
│     │                                  │                       │
└────────────────────────────────────────────────────────────────┘
```

### Implementing AnalyzerResponder

```java
public class HoribaPentra60Responder implements AnalyzerResponder {
    @Override
    public String buildResponse(List<String> lines) {
        // Parse Q-segment to extract query parameters
        String sampleId = parseQuerySampleId(lines);

        // Lookup patient/order information
        Sample sample = sampleService.getBySampleId(sampleId);
        Patient patient = sample.getPatient();

        // Build ASTM response segments
        StringBuilder response = new StringBuilder();
        response.append("H|\\^&|||OpenELIS^LIS|||||||LIS2-A2\n");
        response.append(buildPatientSegment(patient));
        response.append(buildOrderSegment(sample));
        response.append("L|1|N\n");

        return response.toString();
    }
}
```

---

## 7. Field Queries and Responder Pattern

### Q-Segment Processing

The ASTM Q-segment is used for queries:

```
Q|1|SampleID^PatientID||ALL||||||||
```

Format: `Q|seq|startID^endID|testCodes|dateRange|...`

### Query Types Supported

| Query Type        | Description                      | Response           |
| ----------------- | -------------------------------- | ------------------ |
| **Sample Query**  | Request orders for a sample ID   | P + O segments     |
| **Patient Query** | Request patient demographics     | P segment          |
| **Test Query**    | Request pending tests for sample | O segments         |
| **All**           | Full patient/order/test info     | P + O + R segments |

### 004 Dashboard Integration for Queries

The 004 dashboard can **configure** how queries are handled:

1. **Analyzer Configuration** stores IP/port for query routing
2. **Field Mappings** determine how to translate OpenELIS fields to analyzer
   format
3. **Error Dashboard** logs query failures

---

## 8. Supported Analyzer Types

### Complete Analyzer List

| #   | Name             | Protocol     | Type           | Plugin Path                          |
| --- | ---------------- | ------------ | -------------- | ------------------------------------ |
| 1   | Horiba Pentra 60 | ASTM LIS2-A2 | Hematology     | `plugins/analyzers/HoribaPentra60`   |
| 2   | Horiba Micros 60 | ASTM LIS2-A2 | Hematology     | `plugins/analyzers/HoribaMicros60`   |
| 3   | Sysmex XN-L      | HL7/LIS2-A2  | Hematology     | `plugins/analyzers/SysmexXN-L`       |
| 4   | Sysmex XP        | HL7/LIS2-A2  | Hematology     | `plugins/analyzers/SysmexXP`         |
| 5   | Sysmex XT 2000i  | CSV          | Hematology     | `plugins/analyzers/Sysmex2000i`      |
| 6   | Sysmex pocH-100i | HL7/LIS2-A2  | Hematology     | `plugins/analyzers/pocH-100i`        |
| 7   | Cobas C111       | CSV          | Chemistry      | `plugins/analyzers/CobasC111`        |
| 8   | Mindray          | HTTP         | Chemistry      | `plugins/analyzers/Mindray`          |
| 9   | Fully Automated  | CSV          | Chemistry      | `plugins/analyzers/Fully`            |
| 10  | BD FACSCalibur   | CSV          | Flow Cytometry | `plugins/analyzers/FacsCalibur`      |
| 11  | BD FACSCantoII   | CSV          | Flow Cytometry | `plugins/analyzers/FacsCantoII`      |
| 12  | BD FACSPresto    | CSV          | Flow Cytometry | `plugins/analyzers/FacsPresto`       |
| 13  | GeneXpert (HL7)  | HL7          | Molecular      | `plugins/analyzers/GeneXpertHL7`     |
| 14  | GeneXpert (File) | CSV          | Molecular      | `plugins/analyzers/GeneXpertFile`    |
| 15  | QuantStudio 3    | CSV          | PCR            | `plugins/analyzers/QuantStudio3`     |
| 16  | TaqMan 48 DBS    | TSV          | PCR            | `plugins/analyzers/CobasTaqMan48DBS` |

### Protocol Classification

```
┌─────────────────────────────────────────────────────────────┐
│                   Protocol Categories                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Real-time Protocols (Bidirectional):                       │
│  ├── ASTM LIS2-A2 (Horiba, Sysmex XN/XP, pocH)              │
│  └── HL7 (GeneXpert, Sysmex)                                │
│                                                             │
│  File-based Protocols (Unidirectional):                     │
│  ├── CSV (Cobas, Fully, FACS, GeneXpert File)               │
│  └── TSV (TaqMan DBS)                                       │
│                                                             │
│  Web-based Protocols:                                       │
│  └── HTTP/REST (Mindray)                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 9. Adding New Analyzers

### Step-by-Step Guide

#### 1. Create Plugin Project Structure

```
plugins/analyzers/NewAnalyzer/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── org/openelis/plugin/
│       │       ├── NewAnalyzerPlugin.java      # AnalyzerImporterPlugin
│       │       └── NewAnalyzerLineInserter.java # Line processor
│       └── resources/
│           └── plugin.xml                       # XML descriptor
```

#### 2. Implement AnalyzerImporterPlugin

```java
public class NewAnalyzerPlugin implements AnalyzerImporterPlugin {

    @Override
    public boolean connect() {
        List<PluginAnalyzerService.TestMapping> mappings = new ArrayList<>();
        mappings.add(new TestMapping("ANALZYER_CODE", "OpenELIS Test Name", "LOINC"));

        PluginAnalyzerService.getInstance()
            .addAnalyzerDatabaseParts("New Analyzer", "Description", mappings, true);
        PluginAnalyzerService.getInstance().registerAnalyzer(this);
        return true;
    }

    @Override
    public boolean isTargetAnalyzer(List<String> lines) {
        // Pattern match header to identify this analyzer
        return lines.stream()
            .anyMatch(l -> l.startsWith("H|") && l.contains("NEW_ANALYZER_ID"));
    }

    @Override
    public AnalyzerLineInserter getAnalyzerLineInserter() {
        return new NewAnalyzerLineInserter();
    }

    // For bidirectional support (optional)
    @Override
    public AnalyzerResponder getAnalyzerResponder() {
        return new NewAnalyzerResponder();
    }
}
```

#### 3. Create plugin.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<plugin>
  <version>1.0</version>
  <analyzerImporter>
    <extension_point>
      <description value="New Analyzer Plugin"/>
      <extension path="org.openelis.plugin.NewAnalyzerPlugin"/>
    </extension_point>
  </analyzerImporter>
</plugin>
```

#### 4. Build and Deploy

```bash
cd plugins/analyzers/NewAnalyzer
mvn clean package
cp target/NewAnalyzer.jar /var/lib/openelis-global/plugins/
# Restart OpenELIS for plugin to load
```

#### 5. Configure via 004 Dashboard (Optional)

After deployment, use the Analyzer Dashboard UI to:

1. Set IP/port configuration
2. Query analyzer for available fields
3. Configure field mappings
4. Set up qualitative result mappings
5. Define unit conversions

---

## Summary

The OpenELIS analyzer plugin system provides:

1. **Modular Architecture:** Independent JAR plugins for each analyzer
2. **Automatic Discovery:** Spring-managed plugin loading at startup
3. **Flexible Protocols:** Support for ASTM, HL7, CSV, and HTTP
4. **Bidirectional Support:** Query/response for real-time analyzers
5. **004 Dashboard Integration:** UI-driven mapping overlay without plugin
   changes
6. **Backward Compatibility:** Existing plugins work unchanged; 004 mappings are
   additive
7. **Error Handling:** Centralized error dashboard for unmapped
   fields/connection issues

The 004 ASTM Analyzer Dashboard **enhances** (not replaces) the plugin system by
adding runtime-configurable mappings that can be modified without code changes
or redeployment.
