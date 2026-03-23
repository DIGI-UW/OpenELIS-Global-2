# ASTM Message Processing Flow in OpenELIS

**Date**: 2025-12-01  
**Feature**: 004-astm-analyzer-mapping  
**Purpose**: Document how ASTM analyzer messages are received, processed, and
stored in OpenELIS

---

## Overview

When an analyzer sends results through the ASTM-HTTP-Bridge to OpenELIS, the
message follows a multi-stage processing pipeline that includes:

1. HTTP endpoint reception
2. ASTM message parsing
3. Analyzer identification (plugin-based)
4. Field mapping application (if configured)
5. Staging in `analyzer_results` table
6. Human validation via UI
7. Final result creation

---

## Complete Processing Flow

```
                         ASTM-HTTP-Bridge                              OpenELIS
┌───────────────────┐    TCP → HTTP       ┌──────────────────────────────────────────────────────────────┐
│   Analyzer        │ ─────────────────→  │  POST /analyzer/astm                                         │
│   (Mock/Real)     │                     │                                                              │
└───────────────────┘                     │  ┌──────────────────────────────────────────────────────┐   │
                                          │  │ AnalyzerImportController.doPost()                     │   │
                                          │  │ • Extracts client IP (for analyzer identification)    │   │
                                          │  │ • Creates ASTMAnalyzerReader                          │   │
                                          │  └────────────────────┬─────────────────────────────────┘   │
                                          │                       │                                      │
                                          │                       ▼                                      │
                                          │  ┌──────────────────────────────────────────────────────┐   │
                                          │  │ ASTMAnalyzerReader.readStream()                       │   │
                                          │  │ • Reads ASTM message from HTTP body                   │   │
                                          │  │ • Detects character encoding                          │   │
                                          │  │ • Parses into lines (H|, P|, O|, R|, L| segments)    │   │
                                          │  │ • setInserterResponder() - finds matching plugin     │   │
                                          │  └────────────────────┬─────────────────────────────────┘   │
                                          │                       │                                      │
                                          │                       ▼                                      │
                                          │  ┌──────────────────────────────────────────────────────┐   │
                                          │  │ ASTMAnalyzerReader.processData()                      │   │
                                          │  │ • Is this a query or results?                         │   │
                                          │  │   ├─ Query (header-only) → buildResponseForQuery()    │   │
                                          │  │   └─ Results → insertAnalyzerData()                   │   │
                                          │  └────────────────────┬─────────────────────────────────┘   │
                                          │                       │                                      │
                                          │                       ▼                                      │
                                          │  ┌──────────────────────────────────────────────────────┐   │
                                          │  │ wrapInserterIfMappingsExist()                         │   │
                                          │  │ • identifyAnalyzerFromMessage() - IP or H-segment    │   │
                                          │  │ • Check if analyzer has active field mappings         │   │
                                          │  │   ├─ YES → Wrap with MappingAwareAnalyzerLineInserter│   │
                                          │  │   └─ NO  → Use original plugin inserter              │   │
                                          │  └────────────────────┬─────────────────────────────────┘   │
                                          │                       │                                      │
                                          │          ┌────────────┴────────────┐                         │
                                          │          ▼                         ▼                         │
                                          │  ┌─────────────────┐    ┌─────────────────────────────┐     │
                                          │  │ Plugin Inserter │    │ MappingAwareAnalyzerLine-   │     │
                                          │  │ (legacy mode)   │    │ Inserter (mapping mode)     │     │
                                          │  │                 │    │ • applyMappings()           │     │
                                          │  │                 │    │ • processQCSegments()       │     │
                                          │  │                 │    │ • Create errors if unmapped │     │
                                          │  └────────┬────────┘    └──────────────┬──────────────┘     │
                                          │           │                            │                     │
                                          │           └────────────┬───────────────┘                     │
                                          │                        ▼                                     │
                                          │  ┌──────────────────────────────────────────────────────┐   │
                                          │  │ AnalyzerResultsService.insertAnalyzerResults()       │   │
                                          │  │ • Creates AnalyzerResults records in DB               │   │
                                          │  │ • Links to Sample by accession number                 │   │
                                          │  │ • Associates with Test/Analysis                       │   │
                                          │  └────────────────────┬─────────────────────────────────┘   │
                                          │                       │                                      │
                                          │                       ▼                                      │
                                          │  ┌──────────────────────────────────────────────────────┐   │
                                          │  │ analyzer_results TABLE (PENDING state)               │   │
                                          │  │ • Results wait for technician review/validation      │   │
                                          │  └────────────────────┬─────────────────────────────────┘   │
                                          │                       │                                      │
                                          │                       ▼  (User Action - UI)                 │
                                          │  ┌──────────────────────────────────────────────────────┐   │
                                          │  │ AnalyzerResultsController.showAnalyzerResultsSave()  │   │
                                          │  │ • Technician reviews/accepts results via UI          │   │
                                          │  │ • persistAnalyzerResults() creates actual Result     │   │
                                          │  │ • Analysis status updated                            │   │
                                          │  └────────────────────────────────────────────────────────┘   │
                                          │                                                              │
                                          └──────────────────────────────────────────────────────────────┘
```

---

## Key Components

### 1. Entry Point: `AnalyzerImportController`

**Location**:
`src/main/java/org/openelisglobal/analyzerimport/action/AnalyzerImportController.java`

**Endpoint**: `POST /analyzer/astm`

**Responsibilities**:

- Receives HTTP POST request from ASTM-HTTP-Bridge
- Extracts client IP address (for analyzer identification)
- Handles proxy headers (`X-Forwarded-For`, `X-Real-IP`)
- Creates `ASTMAnalyzerReader` instance
- Delegates to reader for processing

**Code Reference**:

```java
@PostMapping("/analyzer/astm")
public void doPost(HttpServletRequest request, HttpServletResponse response) {
    ASTMAnalyzerReader reader = AnalyzerReaderFactory.getReaderFor("astm");

    // Extract client IP for analyzer identification
    String clientIp = request.getRemoteAddr();
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isEmpty()) {
        clientIp = forwardedFor.split(",")[0].trim();
    }
    reader.setClientIpAddress(clientIp);

    reader.readStream(request.getInputStream());
    reader.processData(getSysUserId(request));
}
```

---

### 2. Message Parsing: `ASTMAnalyzerReader`

**Location**:
`src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java`

**Responsibilities**:

- Reads ASTM message from HTTP request body
- Detects character encoding (UTF-8, ISO-8859-1, etc.)
- Parses message into lines (ASTM segments: `H|`, `P|`, `O|`, `R|`, `L|`)
- Identifies matching analyzer plugin
- Determines if message is a query or results
- Routes to appropriate handler

**Plugin Identification**:

```java
private void setInserterResponder() {
    for (AnalyzerImporterPlugin plugin : pluginAnalyzerService.getAnalyzerPlugins()) {
        if (plugin.isTargetAnalyzer(lines)) {
            this.plugin = plugin;
            inserter = plugin.getAnalyzerLineInserter();
            responder = plugin.getAnalyzerResponder();
            return;
        }
    }
}
```

**Query vs Results Detection**:

```java
public boolean processData(String currentUserId) {
    if (plugin.isAnalyzerResult(lines)) {
        return insertAnalyzerData(currentUserId);  // Results
    } else {
        responseBody = buildResponseForQuery();     // Query
        hasResponse = true;
        return true;
    }
}
```

---

### 3. Analyzer Identification Strategies

**Location**: `ASTMAnalyzerReader.identifyAnalyzerFromMessage()`

The system uses **multi-strategy identification** to determine which analyzer
sent the message:

| Strategy                   | Method                                    | Priority |
| -------------------------- | ----------------------------------------- | -------- |
| **1. ASTM Header Parsing** | Parse H-segment for manufacturer/model    | Highest  |
| **2. Client IP Address**   | Lookup `AnalyzerConfiguration` by IP:Port | Medium   |
| **3. Plugin Matching**     | Match by analyzer name from plugin        | Fallback |

**Code Reference**:

```java
private Optional<Analyzer> identifyAnalyzerFromMessage() {
    // Strategy 1: Parse ASTM H-segment
    String analyzerName = parseAnalyzerNameFromHeader();
    if (analyzerName != null) {
        Optional<AnalyzerConfiguration> config =
            configService.getByAnalyzerName(analyzerName);
        if (config.isPresent()) {
            return Optional.of(config.get().getAnalyzer());
        }
    }

    // Strategy 2: Client IP address
    if (clientIpAddress != null) {
        Optional<AnalyzerConfiguration> config =
            configService.getByIpAndPort(clientIpAddress, port);
        if (config.isPresent()) {
            return Optional.of(config.get().getAnalyzer());
        }
    }

    // Strategy 3: Plugin matching (fallback)
    // ...
}
```

---

### 4. Field Mapping Integration (Feature 004)

**Location**: `ASTMAnalyzerReader.wrapInserterIfMappingsExist()`

If an analyzer has **active field mappings** configured, the system wraps the
plugin inserter with a mapping-aware wrapper:

```java
private AnalyzerLineInserter wrapInserterIfMappingsExist(AnalyzerLineInserter originalInserter) {
    Optional<Analyzer> analyzer = identifyAnalyzerFromMessage();

    if (!analyzer.isPresent()) {
        return originalInserter;  // Cannot identify - use legacy mode
    }

    MappingApplicationService mappingService = SpringContext.getBean(MappingApplicationService.class);

    if (mappingService.hasActiveMappings(analyzer.get().getId())) {
        // Wrap with mapping-aware inserter
        return new MappingAwareAnalyzerLineInserter(originalInserter, analyzer.get());
    }

    // No mappings - use legacy plugin inserter (backward compatibility)
    return originalInserter;
}
```

**Mapping-Aware Processing**:

- **Location**:
  `src/main/java/org/openelisglobal/analyzer/service/MappingAwareAnalyzerLineInserter.java`
- **Responsibilities**:
  1. Apply field mappings to transform ASTM codes → OpenELIS codes
  2. Handle unmapped fields (create `AnalyzerError` records)
  3. Process Q-segments (QC results) if present
  4. Delegate transformed data to original plugin inserter

---

### 5. Plugin-Based Result Parsing

**Interface**: `AnalyzerImporterPlugin`

Each analyzer has a **plugin** that implements:

- `isTargetAnalyzer(lines)` - Check if message matches this analyzer
- `getAnalyzerLineInserter()` - Return line-by-line parser
- `getAnalyzerResponder()` - Return query response builder (optional)

**Example Plugins**:

- `CobasC311Reader` - Roche Cobas C311 chemistry analyzer
- `FACSCantoReader` - BD FACSCanto flow cytometer
- `EvolisReader` - Bio-Rad Evolis analyzer

**Plugin Inserter Pattern**:

```java
public class CobasC311Reader extends AnalyzerLineInserter {
    @Override
    public boolean insert(List<String> lines, String currentUserId) {
        List<AnalyzerResults> results = new ArrayList<>();

        // Parse each line
        for (String line : lines) {
            AnalyzerResults result = parseLine(line);
            results.add(result);
        }

        // Persist to staging table
        return persistImport(currentUserId, results);
    }
}
```

---

### 6. Result Staging: `analyzer_results` Table

**Purpose**: Results are stored in a **staging table** before human validation

**Key Columns**:

| Column             | Description                               |
| ------------------ | ----------------------------------------- |
| `accession_number` | Links to `sample` table                   |
| `test_id`          | OpenELIS Test ID (from mapping or plugin) |
| `result`           | The value from analyzer                   |
| `units`            | Unit of measure                           |
| `analyzer_id`      | Which analyzer sent it                    |
| `is_read_only`     | Already validated?                        |
| `complete_date`    | When analyzer completed test              |

**Service**: `AnalyzerResultsService.insertAnalyzerResults()`

**Responsibilities**:

- Create `AnalyzerResults` records
- Link to `Sample` by accession number
- Associate with `Test` and `Analysis`
- Handle sample creation if not exists

---

### 7. Human Validation: UI Workflow

**Page**: `/AnalyzerResults` (legacy UI)

**Workflow**:

1. **Technician Views Pending Results**: UI displays all results in
   `analyzer_results` table
2. **Review & Validation**: User can:
   - Accept results as-is
   - Modify values
   - Reject results
   - Add notes
3. **Save Action**: `AnalyzerResultsController.showAnalyzerResultsSave()`
   - Creates `Result` records from `AnalyzerResults`
   - Updates `Analysis` status
   - Links to `Sample` and `Patient`
   - Removes from staging table

**Code Reference**:

```java
@RequestMapping(value = "/AnalyzerResults", method = RequestMethod.POST)
public ModelAndView showAnalyzerResultsSave(HttpServletRequest request, ...) {
    List<AnalyzerResultItem> actionableResults = extractActionableResult(resultItemList);
    List<SampleGrouping> sampleGroupList = new ArrayList<>();

    createResultsFromItems(actionableResults, sampleGroupList);

    // Persist to final result tables
    analyzerResultsService.persistAnalyzerResults(
        deletableAnalyzerResults,
        sampleGroupList,
        getSysUserId(request)
    );
}
```

---

## Two-Stage Processing Model

OpenELIS uses a **two-stage processing model** for analyzer results:

### Stage 1: Automatic Ingestion (Staging)

- ✅ Message received and parsed
- ✅ Analyzer identified
- ✅ Field mappings applied (if configured)
- ✅ Results stored in `analyzer_results` (staging)
- ⏸️ **PENDING** - Awaiting human validation

### Stage 2: Human Validation (Final)

- 👤 Technician reviews pending results
- ✅ Accept/modify/reject decisions
- ✅ Results copied to `result` table
- ✅ `Analysis` status updated
- ✅ Results available in patient record

**Why Two Stages?**

- **Data Quality**: Catch analyzer errors before finalization
- **Flexibility**: Allow manual corrections
- **Audit Trail**: Track who validated what and when
- **Compliance**: Meets ISO 15189 and SLIPTA requirements

---

## Error Handling

### Unmapped Fields

If field mappings are incomplete or missing:

1. **Error Detection**: `MappingAwareAnalyzerLineInserter` detects unmapped
   fields
2. **Error Creation**: Creates `AnalyzerError` record
3. **Error Queue**: Message held in error queue (per FR-011)
4. **Error Dashboard**: Displayed in Error Dashboard for resolution
5. **Reprocessing**: After mapping created, message can be reprocessed

**Code Reference**:

```java
if (!result.getUnmappedFields().isEmpty()) {
    String errorMessage = "Unmapped fields detected: " +
        String.join(", ", result.getUnmappedFields());
    createError(errorMessage, lines);
    // Continue processing - partial data may still be useful
}
```

### QC Segment Processing

**Location**: `MappingAwareAnalyzerLineInserter.processQCSegments()`

When ASTM messages contain Q-segments (Quality Control results):

1. **Parse Q-Segments**: Extract QC data (control lot, level, value, timestamp)
2. **Apply QC Mappings**: Map QC fields using configured mappings
3. **Persist QC Results**: Call `QCResultService.createQCResult()` (Feature 003)
4. **Error Handling**: If QC mappings incomplete, queue in Error Dashboard

**Per FR-021**: QC results processed within same transaction as patient results.

---

## Backward Compatibility

The system maintains **backward compatibility** with legacy plugin-based
analyzers:

| Scenario                        | Behavior                                                   |
| ------------------------------- | ---------------------------------------------------------- |
| **Analyzer with mappings**      | Uses `MappingAwareAnalyzerLineInserter` → applies mappings |
| **Analyzer without mappings**   | Uses original plugin inserter directly                     |
| **Analyzer not identified**     | Falls back to plugin matching (legacy mode)                |
| **Mapping service unavailable** | Uses original plugin inserter (graceful degradation)       |

---

## References

- **Controller**:
  `src/main/java/org/openelisglobal/analyzerimport/action/AnalyzerImportController.java`
- **Reader**:
  `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java`
- **Mapping Inserter**:
  `src/main/java/org/openelisglobal/analyzer/service/MappingAwareAnalyzerLineInserter.java`
- **Plugin Interface**:
  `src/main/java/org/openelisglobal/plugin/AnalyzerImporterPlugin.java`
- **Results Service**:
  `src/main/java/org/openelisglobal/analyzerresults/service/AnalyzerResultsService.java`
- **Feature Specification**: `specs/004-astm-analyzer-mapping/spec.md` (FR-001,
  FR-011, FR-021)
