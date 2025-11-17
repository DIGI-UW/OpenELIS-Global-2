# Research: ASTM Analyzer Field Mapping

**Feature**: 004-astm-analyzer-mapping  
**Date**: 2025-11-14  
**Status**: Complete

This document consolidates technical research and decisions for implementing the ASTM analyzer field mapping feature.

## 1. ASTM Protocol Integration

### Decision: Leverage Existing Plugin-Based Architecture

**Rationale**: OpenELIS uses a plugin-based system for analyzer integration. The existing `ASTMAnalyzerReader` and `AnalyzerImportController` handle ASTM message processing via plugins that implement `AnalyzerImporterPlugin` interface.

**Implementation Approach**:
- **Query Analyzer Functionality**: Create new service `AnalyzerQueryService` that sends ASTM query messages to analyzers via TCP connection (using analyzer IP:Port from configuration)
- **ASTM LIS2-A2 Protocol**: Use existing ASTM protocol infrastructure. Query messages follow ASTM LIS2-A2 standard (ENQ 0x05 / ACK 0x06 handshake)
- **Response Parsing**: Parse ASTM response records to extract field identifiers from message segments (H, P, O, R segments contain test codes, units, qualitative values)
- **Integration Points**:
  - Extend `AnalyzerImportController` with new endpoint `/rest/analyzer/query/{analyzerId}` for query operations
  - Create `AnalyzerQueryServiceImpl` that handles TCP connection, sends query message, parses response
  - Store retrieved fields in `AnalyzerField` entity for mapping configuration

**Alternatives Considered**:
- **Option A**: Create separate ASTM query service independent of existing infrastructure
  - **Rejected**: Would duplicate TCP connection logic and violate DRY principle
- **Option B**: Extend existing plugin system to support query operations
  - **Rejected**: Plugins are analyzer-specific; query functionality should be generic across all ASTM analyzers

**Technical Details**:
- Query timeout: 5 minutes (configurable)
- Maximum fields per query: 500 (configurable)
- Rate limit: 1 query per minute per analyzer (prevents analyzer overload)
- Connection test: TCP handshake validation only (30-second timeout)
- Progress indication: WebSocket or polling for real-time connection logs

**References**:
- `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java`
- `src/main/java/org/openelisglobal/analyzerimport/action/AnalyzerImportController.java`
- `docs/astm.md` - ASTM bi-directional interface documentation

## 2. Legacy Analyzer Entity Integration

### Decision: Extend Existing Analyzer Entity with New Configuration Fields

**Rationale**: The existing `Analyzer` entity uses XML-based Hibernate mappings (legacy, exempt from annotation requirement per Constitution IV). We need to add IP address, port, and protocol version fields without breaking existing functionality.

**Implementation Approach**:
- **Schema Extension**: Add new columns to existing `analyzer` table via Liquibase:
  - `ip_address VARCHAR(15)` - IPv4 address
  - `port INTEGER` - Port number (1-65535)
  - `protocol_version VARCHAR(20)` - Default: "ASTM LIS2-A2"
  - `test_unit_ids TEXT[]` - Array of test unit IDs (PostgreSQL array type)
- **Entity Extension**: Create new annotation-based entity `AnalyzerConfiguration` that references `Analyzer` via foreign key (one-to-one relationship)
  - **Alternative**: Extend legacy `Analyzer` entity with new fields (requires XML mapping update)
  - **Chosen**: Create separate `AnalyzerConfiguration` entity to avoid modifying legacy XML mappings
- **Backward Compatibility**: Existing analyzer plugin system continues to work. New mapping system operates alongside plugins.

**Migration Strategy**:
- Existing analyzers in database: Set default values for new fields (IP: null, Port: null, Protocol: "ASTM LIS2-A2")
- Analyzers without configuration: Cannot use new mapping system until configured
- Plugin-based analyzers: Continue using existing plugin system (no breaking changes)

**Alternatives Considered**:
- **Option A**: Create entirely new `AnalyzerV2` entity and migrate all data
  - **Rejected**: Too disruptive, breaks existing plugin integrations
- **Option B**: Modify legacy `Analyzer` entity XML mappings
  - **Rejected**: Violates principle of not modifying legacy code until refactored

**References**:
- `src/main/java/org/openelisglobal/analyzer/valueholder/Analyzer.java`
- `src/main/resources/hibernate/hbm/Analyzer.hbm.xml`
- `docs/analyzer.md` - Analyzer plugin documentation

## 3. Field Mapping Architecture

### Decision: Many-to-One Mapping with Type Compatibility Validation

**Rationale**: Analyzers may send multiple qualitative values (e.g., "POS", "+", "Reactive") that map to the same OpenELIS coded result. Type compatibility prevents unsafe mappings (e.g., text → numeric).

**Implementation Approach**:
- **Many-to-One Mapping**: `QualitativeResultMapping` entity supports multiple analyzer values mapping to single OpenELIS code
  - Structure: `(analyzerFieldId, analyzerValue) → openelisCode`
  - Example: `("HIV", "POS") → "POSITIVE"`, `("HIV", "+") → "POSITIVE"`
- **Unit Conversion**: `UnitMapping` entity stores conversion factors for unit mismatches
  - Structure: `(analyzerFieldId, analyzerUnit) → (openelisUnit, conversionFactor)`
  - Example: `("Glucose", "mg/dL") → ("mmol/L", 0.0555)`
  - Validation: Reject mappings if conversion factor not provided and units differ
- **Type Compatibility Rules**:
  - **Numeric → Numeric**: Allowed (with unit conversion if needed)
  - **Qualitative → Qualitative**: Allowed (many-to-one supported)
  - **Text → Text**: Allowed
  - **Numeric → Text**: Blocked (data loss)
  - **Text → Numeric**: Blocked (unsafe conversion)
  - **Qualitative → Numeric**: Blocked (semantic mismatch)

**Validation Logic**:
- Service layer validates type compatibility before saving mappings
- Frontend shows warnings for unit mismatches (requires conversion factor)
- Frontend blocks incompatible type mappings (disabled dropdown options)

**Alternatives Considered**:
- **Option A**: One-to-one mapping only (simpler, but less flexible)
  - **Rejected**: Real-world analyzers send multiple values for same result (e.g., "+", "POS", "Reactive" all mean positive)
- **Option B**: Automatic unit conversion using lookup tables
  - **Rejected**: Conversion factors vary by analyte (e.g., glucose vs cholesterol), requires manual configuration

**References**:
- Specification FR-004, FR-005 (unit and qualitative mapping requirements)
- OGC-49 specification (if available in `.dev-docs/OGC-49/`)

## 4. Error Queue and Reprocessing

### Decision: Database-Backed Error Queue with Reprocessing Service

**Rationale**: Failed/unmapped messages must be held for administrator review. Database-backed queue provides persistence, auditability, and integration with existing OpenELIS infrastructure.

**Implementation Approach**:
- **Error Storage**: `AnalyzerError` entity stores failed message details
  - Fields: `errorId`, `analyzerId`, `errorType` (mapping, validation, timeout, protocol), `severity` (critical, error, warning), `message`, `rawMessage`, `timestamp`, `status` (unacknowledged, acknowledged), `acknowledgedBy`, `acknowledgedAt`
- **Error Detection**: Integrate with `ASTMAnalyzerReader.processData()` to catch unmapped fields
  - When mapping not found: Create `AnalyzerError` record, hold message in error queue
  - When validation fails: Create `AnalyzerError` record with validation details
- **Reprocessing Workflow**:
  1. Administrator creates missing mappings via Error Dashboard
  2. Administrator triggers reprocessing for selected errors
  3. `AnalyzerReprocessingService` retrieves raw message from `AnalyzerError.rawMessage`
  4. Re-process message through `ASTMAnalyzerReader` with new mappings
  5. If successful: Delete or mark error as resolved
  6. If still fails: Update error with new failure reason

**Message Queue Pattern**:
- **Not Using**: External message queue (RabbitMQ, Kafka) - adds infrastructure complexity
- **Using**: Database table (`analyzer_error`) as queue - simpler, integrates with existing audit trail

**State Management**:
- Error states: `UNACKNOWLEDGED` → `ACKNOWLEDGED` → `RESOLVED` (after reprocessing)
- Reprocessing states: `PENDING` → `PROCESSING` → `SUCCESS` / `FAILED`

**Alternatives Considered**:
- **Option A**: External message queue (RabbitMQ, Kafka)
  - **Rejected**: Adds infrastructure complexity, requires additional deployment components
- **Option B**: In-memory error queue (Redis)
  - **Rejected**: Not persistent, errors lost on server restart

**References**:
- Specification FR-011, FR-016, FR-017 (error dashboard and reprocessing requirements)

## 5. Carbon Design System Components

### Decision: Carbon Grid for Dual-Panel Layout, CSS for Visual Connection Lines

**Rationale**: Carbon Design System provides Grid component for responsive layouts. Visual connection lines require custom CSS (not provided by Carbon), but must use Carbon design tokens for colors.

**Implementation Approach**:
- **Dual-Panel Layout**: Use Carbon `Grid` and `Column` components
  ```jsx
  <Grid>
    <Column lg={8} md={8} sm={4}>  {/* Left panel: Analyzer Fields */}
      <AnalyzerFieldsPanel />
    </Column>
    <Column lg={8} md={8} sm={4}>  {/* Right panel: Mapping Panel */}
      <MappingPanel />
    </Column>
  </Grid>
  ```
  - Responsive: Stacks vertically on mobile (<1024px) via Carbon breakpoints
  - Equal width: 50/50 split on desktop (lg={8} + lg={8} = 16 columns total)
- **Visual Connection Lines**: Custom CSS using SVG or pseudo-elements
  - Use Carbon design tokens for line colors: `$interactive-01` (primary), `$support-error` (error), `$support-warning` (warning)
  - Lines connect source field (left panel) to target field (right panel)
  - Animated on mapping creation (fade-in effect)
- **OpenELIS Field Selector**: Carbon `ComboBox` with custom filtering
  - Search: Carbon `Search` component (300ms debounce)
  - Category filter: Carbon `MultiSelect` for 8 entity types
  - Field list: Carbon `ListBox` with grouped items
  - Type filtering: Disable incompatible options (Carbon `ComboBox` disabled prop)

**Field Type Color Coding** (Carbon tokens):
- Numeric: `$blue-60`
- Qualitative: `$purple-60`
- Control Test: `$green-60`
- Melting Point: `$teal-60`
- Date/Time: `$cyan-60`
- Text: `$gray-60`

**Alternatives Considered**:
- **Option A**: Use Carbon `DataTable` for both panels
  - **Rejected**: Mapping panel requires custom form fields, not tabular data
- **Option B**: Use third-party dual-panel component library
  - **Rejected**: Violates Carbon Design System First principle (Constitution II)

**References**:
- Carbon Design System: https://carbondesignsystem.com/
- OpenELIS Carbon Guide: https://uwdigi.atlassian.net/wiki/spaces/OG/pages/621346838
- Specification FR-003, FR-008 (dual-panel interface requirements)

## 6. Integration with Existing ASTM Message Processing

### Decision: Intercept Message Processing to Apply Mappings

**Rationale**: Mappings must be applied during message interpretation, before data insertion. Integration point is in `ASTMAnalyzerReader.insertAnalyzerData()` or plugin `AnalyzerLineInserter.insert()`.

**Implementation Approach**:
- **Mapping Application**: Create `MappingApplicationService` that:
  1. Receives raw ASTM message segments
  2. Extracts test codes, units, qualitative values
  3. Queries `AnalyzerFieldMapping` to find mappings for analyzer
  4. Applies mappings: test code → OpenELIS test, unit → canonical unit (with conversion), qualitative value → OpenELIS code
  5. Returns transformed data structure for insertion
- **Integration Point**: Extend `AnalyzerLineInserter` interface or create wrapper
  - **Option A**: Modify existing plugins to use mapping service
    - **Rejected**: Breaks backward compatibility with existing plugins
  - **Option B**: Create mapping-aware inserter wrapper
    - **Chosen**: Wrapper applies mappings before delegating to plugin inserter
- **Fallback Behavior**: If mapping not found:
  - Create `AnalyzerError` record
  - Hold message in error queue (do not insert partial data)
  - Return error to `ASTMAnalyzerReader` for error handling

**Error Handling**:
- Mapping not found → `AnalyzerError` (type: mapping, severity: error)
- Unit conversion failed → `AnalyzerError` (type: validation, severity: warning)
- Type incompatibility → `AnalyzerError` (type: validation, severity: error)

**Alternatives Considered**:
- **Option A**: Post-process inserted data to apply mappings
  - **Rejected**: Data already inserted with wrong values, requires correction workflow
- **Option B**: Replace plugin system entirely with mapping-based system
  - **Rejected**: Breaks backward compatibility, too disruptive

**References**:
- `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java`
- `src/main/java/org/openelisglobal/plugin/AnalyzerLineInserter.java` (interface)
- Specification FR-001, FR-011 (mapping application and error handling)

## Summary of Technical Decisions

| Decision Area | Chosen Approach | Rationale |
|--------------|----------------|-----------|
| ASTM Query | New `AnalyzerQueryService` with TCP connection | Leverages existing infrastructure, generic across analyzers |
| Analyzer Entity | Separate `AnalyzerConfiguration` entity (one-to-one with legacy `Analyzer`) | Avoids modifying legacy XML mappings, maintains backward compatibility |
| Field Mapping | Many-to-one with type compatibility validation | Supports real-world analyzer behavior, prevents unsafe conversions |
| Error Queue | Database-backed `AnalyzerError` entity | Persistent, auditable, integrates with existing infrastructure |
| Dual-Panel Layout | Carbon Grid (50/50 split) with custom CSS for connection lines | Follows Carbon Design System, responsive, accessible |
| Message Processing | Mapping-aware wrapper around existing plugin system | Maintains backward compatibility, applies mappings before insertion |

## Open Questions (Resolved)

All research questions have been resolved. No outstanding technical unknowns.

## Next Steps

1. Generate data model (`data-model.md`) based on entity decisions above
2. Create API contracts (`contracts/`) for REST endpoints
3. Write quickstart guide (`quickstart.md`) for developers

