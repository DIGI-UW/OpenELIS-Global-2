# Research: ASTM Analyzer Field Mapping

**Feature**: 004-astm-analyzer-mapping  
**Date**: 2025-11-14  
**Status**: Complete

This document consolidates technical research and decisions for implementing the
ASTM analyzer field mapping feature.

## 1. ASTM Protocol Integration

### Decision: Leverage Existing Plugin-Based Architecture

**Rationale**: OpenELIS uses a plugin-based system for analyzer integration. The
existing `ASTMAnalyzerReader` and `AnalyzerImportController` handle ASTM message
processing via plugins that implement `AnalyzerImporterPlugin` interface.

**Implementation Approach**:

- **Query Analyzer Functionality**: Create new service `AnalyzerQueryService`
  that sends ASTM query messages to analyzers via TCP connection (using analyzer
  IP:Port from configuration)
- **ASTM LIS2-A2 Protocol**: Use existing ASTM protocol infrastructure. Query
  messages follow ASTM LIS2-A2 standard (ENQ 0x05 / ACK 0x06 handshake)
- **Response Parsing**: Parse ASTM response records to extract field identifiers
  from message segments (H, P, O, R segments contain test codes, units,
  qualitative values)
- **Integration Points**:
  - Extend `AnalyzerImportController` with new endpoint
    `/rest/analyzer/query/{analyzerId}` for query operations
  - Create `AnalyzerQueryServiceImpl` that handles TCP connection, sends query
    message, parses response
  - Store retrieved fields in `AnalyzerField` entity for mapping configuration

**Alternatives Considered**:

- **Option A**: Create separate ASTM query service independent of existing
  infrastructure
  - **Rejected**: Would duplicate TCP connection logic and violate DRY principle
- **Option B**: Extend existing plugin system to support query operations
  - **Rejected**: Plugins are analyzer-specific; query functionality should be
    generic across all ASTM analyzers

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

**Rationale**: The existing `Analyzer` entity uses XML-based Hibernate mappings
(legacy, exempt from annotation requirement per Constitution IV). We need to add
IP address, port, and protocol version fields without breaking existing
functionality.

**Implementation Approach**:

- **Schema Extension**: Add new columns to existing `analyzer` table via
  Liquibase:
  - `ip_address VARCHAR(15)` - IPv4 address
  - `port INTEGER` - Port number (1-65535)
  - `protocol_version VARCHAR(20)` - Default: "ASTM LIS2-A2"
  - `test_unit_ids TEXT[]` - Array of test unit IDs (PostgreSQL array type)
- **Entity Extension**: Create new annotation-based entity
  `AnalyzerConfiguration` that references `Analyzer` via foreign key (one-to-one
  relationship)
  - **Alternative**: Extend legacy `Analyzer` entity with new fields (requires
    XML mapping update)
  - **Chosen**: Create separate `AnalyzerConfiguration` entity to avoid
    modifying legacy XML mappings
- **Backward Compatibility**: Existing analyzer plugin system continues to work.
  New mapping system operates alongside plugins.

**Migration Strategy**:

- Existing analyzers in database: Set default values for new fields (IP: null,
  Port: null, Protocol: "ASTM LIS2-A2")
- Analyzers without configuration: Cannot use new mapping system until
  configured
- Plugin-based analyzers: Continue using existing plugin system (no breaking
  changes)

**Alternatives Considered**:

- **Option A**: Create entirely new `AnalyzerV2` entity and migrate all data
  - **Rejected**: Too disruptive, breaks existing plugin integrations
- **Option B**: Modify legacy `Analyzer` entity XML mappings
  - **Rejected**: Violates principle of not modifying legacy code until
    refactored

**References**:

- `src/main/java/org/openelisglobal/analyzer/valueholder/Analyzer.java`
- `src/main/resources/hibernate/hbm/Analyzer.hbm.xml`
- `docs/analyzer.md` - Analyzer plugin documentation

## 3. Field Mapping Architecture

### Decision: Many-to-One Mapping with Type Compatibility Validation

**Rationale**: Analyzers may send multiple qualitative values (e.g., "POS", "+",
"Reactive") that map to the same OpenELIS coded result. Type compatibility
prevents unsafe mappings (e.g., text → numeric).

**Implementation Approach**:

- **Many-to-One Mapping**: `QualitativeResultMapping` entity supports multiple
  analyzer values mapping to single OpenELIS code
  - Structure: `(analyzerFieldId, analyzerValue) → openelisCode`
  - Example: `("HIV", "POS") → "POSITIVE"`, `("HIV", "+") → "POSITIVE"`
- **Unit Conversion**: `UnitMapping` entity stores conversion factors for unit
  mismatches
  - Structure:
    `(analyzerFieldId, analyzerUnit) → (openelisUnit, conversionFactor)`
  - Example: `("Glucose", "mg/dL") → ("mmol/L", 0.0555)`
  - Validation: Reject mappings if conversion factor not provided and units
    differ
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
  - **Rejected**: Real-world analyzers send multiple values for same result
    (e.g., "+", "POS", "Reactive" all mean positive)
- **Option B**: Automatic unit conversion using lookup tables
  - **Rejected**: Conversion factors vary by analyte (e.g., glucose vs
    cholesterol), requires manual configuration

**References**:

- Specification FR-004, FR-005 (unit and qualitative mapping requirements)
- OGC-49 specification (if available in `.dev-docs/OGC-49/`)

## 4. Error Queue and Reprocessing

### Decision: Database-Backed Error Queue with Reprocessing Service

**Rationale**: Failed/unmapped messages must be held for administrator review.
Database-backed queue provides persistence, auditability, and integration with
existing OpenELIS infrastructure.

**Implementation Approach**:

- **Error Storage**: `AnalyzerError` entity stores failed message details
  - Fields: `errorId`, `analyzerId`, `errorType` (mapping, validation, timeout,
    protocol), `severity` (critical, error, warning), `message`, `rawMessage`,
    `timestamp`, `status` (unacknowledged, acknowledged), `acknowledgedBy`,
    `acknowledgedAt`
- **Error Detection**: Integrate with `ASTMAnalyzerReader.processData()` to
  catch unmapped fields
  - When mapping not found: Create `AnalyzerError` record, hold message in error
    queue
  - When validation fails: Create `AnalyzerError` record with validation details
- **Reprocessing Workflow**:
  1. Administrator creates missing mappings via Error Dashboard
  2. Administrator triggers reprocessing for selected errors
  3. `AnalyzerReprocessingService` retrieves raw message from
     `AnalyzerError.rawMessage`
  4. Re-process message through `ASTMAnalyzerReader` with new mappings
  5. If successful: Delete or mark error as resolved
  6. If still fails: Update error with new failure reason

**Message Queue Pattern**:

- **Not Using**: External message queue (RabbitMQ, Kafka) - adds infrastructure
  complexity
- **Using**: Database table (`analyzer_error`) as queue - simpler, integrates
  with existing audit trail

**State Management**:

- Error states: `UNACKNOWLEDGED` → `ACKNOWLEDGED` → `RESOLVED` (after
  reprocessing)
- Reprocessing states: `PENDING` → `PROCESSING` → `SUCCESS` / `FAILED`

**Alternatives Considered**:

- **Option A**: External message queue (RabbitMQ, Kafka)
  - **Rejected**: Adds infrastructure complexity, requires additional deployment
    components
- **Option B**: In-memory error queue (Redis)
  - **Rejected**: Not persistent, errors lost on server restart

**References**:

- Specification FR-011, FR-016, FR-017 (error dashboard and reprocessing
  requirements)

## 5. Carbon Design System Components

### Decision: Carbon Grid for Dual-Panel Layout, CSS for Visual Connection Lines

**Rationale**: Carbon Design System provides Grid component for responsive
layouts. Visual connection lines require custom CSS (not provided by Carbon),
but must use Carbon design tokens for colors.

**Implementation Approach**:

- **Dual-Panel Layout**: Use Carbon `Grid` and `Column` components
  ```jsx
  <Grid>
    <Column lg={8} md={8} sm={4}>
      {" "}
      {/* Left panel: Analyzer Fields */}
      <AnalyzerFieldsPanel />
    </Column>
    <Column lg={8} md={8} sm={4}>
      {" "}
      {/* Right panel: Mapping Panel */}
      <MappingPanel />
    </Column>
  </Grid>
  ```
  - Responsive: Stacks vertically on mobile (<1024px) via Carbon breakpoints
  - Equal width: 50/50 split on desktop (lg={8} + lg={8} = 16 columns total)
- **Visual Connection Lines**: Custom CSS using SVG or pseudo-elements
  - Use Carbon design tokens for line colors: `$interactive-01` (primary),
    `$support-error` (error), `$support-warning` (warning)
  - Lines connect source field (left panel) to target field (right panel)
  - Animated on mapping creation (fade-in effect)
- **OpenELIS Field Selector**: Carbon `ComboBox` with custom filtering
  - Search: Carbon `Search` component (300ms debounce)
  - Category filter: Carbon `MultiSelect` for 8 entity types
  - Field list: Carbon `ListBox` with grouped items
  - Type filtering: Disable incompatible options (Carbon `ComboBox` disabled
    prop)
- **Navigation Components**: Carbon `SideNavMenu` and `SideNavMenuItem` for
  left-hand navigation
  - **NO Carbon Tabs/TabList components** - sub-navigation items function as
    tabs
  - Active sub-nav item highlighted using Carbon `SideNavMenuItem` active state
  - Navigation visibility controlled via route metadata or page component props

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
  - **Rejected**: Violates Carbon Design System First principle (Constitution
    II)
- **Option C**: Use Carbon `Tabs`/`TabList` components on analyzer pages
  - **Rejected**: Creates duplicate navigation (left nav + tabs). Unified
    approach uses sub-nav items as tabs per FR-020 clarification.

**References**:

- Carbon Design System: https://carbondesignsystem.com/
- OpenELIS Carbon Guide:
  https://uwdigi.atlassian.net/wiki/spaces/OG/pages/621346838
- Specification FR-003, FR-008 (dual-panel interface requirements)
- Specification FR-020 (unified tab-navigation pattern)

## 6. Navigation Integration with Left-Hand Navigation Bar

### Decision: Unified Tab-Navigation Pattern Using Sub-Navigation Items

**Rationale**: OpenELIS uses a backend-driven menu system (`/rest/menu` API) for
navigation. To avoid duplicate navigation options (left nav + separate tabs),
sub-navigation items in the left-hand navigation bar function as tabs. This
unified approach provides a single, consistent navigation pattern.

**Implementation Approach**:

- **Backend-Driven Menu**: Navigation items stored in database, exposed via
  `/rest/menu` API endpoint
  - "Analyzers" parent menu item (expandable/collapsible) that always anchors
    every analyzer-related page
  - Sub-navigation items (initial delivery): "Analyzers Dashboard" (route
    `/analyzers`), "Error Dashboard" (route `/analyzers/errors`), contextual
    "Field Mappings" (route `/analyzers/:id/mappings`)
  - Quality Control placeholder entries (linking to feature `003-westgard-qc`):
    main QC dashboard (`/analyzers/qc`), "QC Alerts & Violations"
    (`/analyzers/qc/alerts`), and "Corrective Actions"
    (`/analyzers/qc/corrective-actions`) so the hierarchy matches the Figma
    navigation even before QC code lands in this branch
  - Role-based visibility handled server-side by menu API (QC routes only for
    users with QC permissions)
- **Unified Tab-Navigation**: Sub-navigation items act as tabs - NO separate
  Carbon `Tabs`/`TabList` components
  - Clicking sub-nav item navigates to route and highlights that item using
    Carbon `SideNavMenuItem` active state
  - Active tab/page tracked by highlighting corresponding sub-navigation item
    based on current route
  - Pages can require left-hand navigation to be visible and expanded by default
    (via route metadata or page component props)
- **State Preservation**: URL-based routing enables bookmarkable/shareable URLs
  - Filters, search, pagination stored in URL query parameters
  - Scroll position, form drafts stored in sessionStorage
  - Active tab state derived from route (e.g., `/analyzers` highlights
    "Analyzers List")

**Integration Points**:

- **Menu API**: Extend `/rest/menu` endpoint to include analyzer navigation
  items plus QC placeholders, filtered per user roles
- **Frontend Routing**: React Router DOM 5.2.0 for `/analyzers`,
  `/analyzers/errors`, `/analyzers/:id/mappings`, `/analyzers/qc`,
  `/analyzers/qc/alerts`, `/analyzers/qc/corrective-actions`
- **Navigation Component**: Use existing `GlobalSideBar` pattern with Carbon
  `SideNavMenu`/`SideNavMenuItem`
- **Active State Tracking**: Route-based highlighting (compare
  `location.pathname` with menu item routes, including QC links)

**Alternatives Considered**:

- **Option A**: Separate Carbon `Tabs`/`TabList` components on analyzer pages
  - **Rejected**: Creates duplicate navigation (left nav + tabs), violates
    unified navigation pattern
- **Option B**: Frontend-hardcoded navigation items
  - **Rejected**: Inconsistent with existing OpenELIS pattern (backend-driven
    menu), cannot be configured dynamically
- **Option C**: Hybrid approach (backend menu + frontend tabs)
  - **Rejected**: Creates confusion, users don't know which navigation to use

**Technical Details**:

- Menu items stored in database (existing `menu` table structure) with new
  entries for QC dashboard + sub-pages
- Menu API filters items based on user roles (LAB_USER, LAB_SUPERVISOR, System
  Administrator, QC roles)
- Frontend renders menu items dynamically from API response; QC entries can
  display “coming soon” content until feature 003 ships
- Active navigation item highlighted using Carbon `SideNavMenuItem` `isActive`
  prop
- Route-to-menu-item mapping: `/analyzers` → "Analyzers Dashboard",
  `/analyzers/errors` → "Error Dashboard", `/analyzers/:id/mappings` → "Field
  Mappings", `/analyzers/qc` → "Quality Control", `/analyzers/qc/alerts` → "QC
  Alerts & Violations", `/analyzers/qc/corrective-actions` → "Corrective
  Actions"

**References**:

- `frontend/src/components/common/GlobalSideBar.js` - Existing navigation
  component
- `frontend/src/components/layout/Header.js` - Menu API integration
  (`/rest/menu`)
- `src/main/java/org/openelisglobal/menu/controller/MenuController.java` - Menu
  API endpoint
- Specification FR-020 (unified tab-navigation pattern clarification)

## 7. Integration with Existing ASTM Message Processing

### Decision: Intercept Message Processing to Apply Mappings

**Rationale**: Mappings must be applied during message interpretation, before
data insertion. Integration point is in
`ASTMAnalyzerReader.insertAnalyzerData()` or plugin
`AnalyzerLineInserter.insert()`.

**Implementation Approach**:

- **Mapping Application**: Create `MappingApplicationService` that:
  1. Receives raw ASTM message segments
  2. Extracts test codes, units, qualitative values
  3. Queries `AnalyzerFieldMapping` to find mappings for analyzer
  4. Applies mappings: test code → OpenELIS test, unit → canonical unit (with
     conversion), qualitative value → OpenELIS code
  5. Returns transformed data structure for insertion
- **Integration Pattern**: Create `MappingAwareAnalyzerLineInserter` wrapper
  class implementing `AnalyzerLineInserter` interface
  - **Wrapper Logic**:
    1. Receive raw ASTM message segments from `ASTMAnalyzerReader`
    2. Call `MappingApplicationService.applyMappings()` to transform segments
       using configured mappings
    3. If mappings found and transformation successful: Delegate transformed
       data to original plugin inserter
    4. If mappings not found or transformation fails: Create `AnalyzerError`
       record, return error (do not delegate to plugin inserter)
  - **Integration Point**: `ASTMAnalyzerReader.processData()` wraps plugin
    inserter with `MappingAwareAnalyzerLineInserter` if analyzer has mappings
    configured (`AnalyzerConfiguration` has active mappings)
  - **Conditional Wrapping**: Check if analyzer has mappings before wrapping:
    - If analyzer has active mappings: Wrap plugin inserter with
      `MappingAwareAnalyzerLineInserter`
    - If analyzer has no mappings: Use original plugin inserter directly
      (backward compatibility)
- **Fallback Behavior**: If mapping not found:
  - Create `AnalyzerError` record (type: mapping, severity: error)
  - Hold message in error queue (do not insert partial data)
  - Return error to `ASTMAnalyzerReader` for error handling
  - **Alternative Considered**: Modify existing plugins to use mapping service
    - **Rejected**: Breaks backward compatibility with existing plugins,
      requires changes to all plugin implementations

**Error Handling**:

- Mapping not found → `AnalyzerError` (type: mapping, severity: error)
- Unit conversion failed → `AnalyzerError` (type: validation, severity: warning)
- Type incompatibility → `AnalyzerError` (type: validation, severity: error)

**Alternatives Considered**:

- **Option A**: Post-process inserted data to apply mappings
  - **Rejected**: Data already inserted with wrong values, requires correction
    workflow
- **Option B**: Replace plugin system entirely with mapping-based system
  - **Rejected**: Breaks backward compatibility, too disruptive

**References**:

- `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java`
- `src/main/java/org/openelisglobal/plugin/AnalyzerLineInserter.java`
  (interface)
- Specification FR-001, FR-011 (mapping application and error handling)

## 9. Lifecycle Stage Management

### Decision: Spring @Scheduled for Automatic Lifecycle Transitions

**Rationale**: OpenELIS uses Spring Framework's built-in scheduling capabilities
for background jobs. Using `@Scheduled` annotation provides simple, reliable,
and maintainable solution for automatic lifecycle transitions without
introducing additional dependencies.

**Implementation Approach**:

- **Scheduled Job Class**: `AnalyzerLifecycleScheduler.java` with `@Component`
  and `@EnableScheduling`
- **Execution Frequency**: Daily at 2 AM (`@Scheduled(cron = "0 0 2 * * ?")`)
- **Transition Logic**: Query analyzers in GO_LIVE stage with
  `last_activated_date < NOW() - INTERVAL '7 days'`, batch update to MAINTENANCE
  stage
- **Failure Handling**: Individual analyzer failures logged but don't block
  batch
  - each analyzer transition wrapped in try-catch
- **Audit Trail**: All transitions logged with user ID "SYSTEM", timestamp,
  previous/new stage
- **Monitoring**: JMX metrics for transition counts, failure counts, execution
  time

**Alternative Considered**: Quartz Scheduler - Rejected due to unnecessary
complexity for simple daily batch job. Spring's `@Scheduled` is sufficient and
already part of Spring Boot starter dependencies.

**Manual Override**: Admin API endpoint
`POST /rest/analyzer/analyzers/{id}/lifecycle-stage` allows manual stage changes
with reason and approval workflow.

## 10. Test Mapping Preview Architecture

### Decision: Reuse ASTMAnalyzerReader for Parsing, Stateless Preview Service

**Rationale**: Existing `ASTMAnalyzerReader` already handles ASTM message
parsing. Creating separate parser would duplicate code. Preview service should
be stateless (no persistence) for fast, safe testing.

**Implementation Approach**:

- **Service**: `AnalyzerMappingPreviewService` (stateless, NO @Transactional)
- **ASTM Parsing**: Delegate to `ASTMAnalyzerReader.parse(message)` for field
  extraction
- **Mapping Application**: Iterate parsed fields, match to configured mappings,
  generate preview data
- **Entity Construction**: Build Test/Result/Sample entities in memory (NO
  persistence)
- **Validation**: Identify unmapped fields, type mismatches, missing required
  mappings, unit conversion issues

**Response Structure**:

```json
{
  "parsedFields": [
    {
      "fieldName": "GLU",
      "astmRef": "R|1|^^^GLU",
      "rawValue": "105",
      "dataType": "NUMERIC"
    }
  ],
  "appliedMappings": [
    {
      "mappingId": "M-001",
      "analyzerField": "GLU",
      "openelisField": "Glucose",
      "confidence": "HIGH"
    }
  ],
  "entityPreview": {
    "test": { "testCode": "GLU", "testName": "Glucose" },
    "result": { "value": "105", "unit": "mg/dL" }
  },
  "warnings": [
    {
      "type": "UNIT_MISMATCH",
      "message": "Unit conversion applied: mg/dL → mmol/L"
    }
  ],
  "errors": [
    { "type": "UNMAPPED_FIELD", "message": "Field 'HbA1c' has no mapping" }
  ]
}
```

**Performance**: Target <2 seconds response time (synchronous operation). No
caching (always use current mappings for accuracy).

**Security**: No persistence (preview only), user must have analyzer view
permissions, ASTM message content NOT logged (may contain PHI).

## 11. Terminology Standards

### Decision: Standardize Human-Readable vs Code Naming Conventions

**Rationale**: Consistent terminology across specification, code, API responses,
and UI prevents confusion and improves maintainability.

**Standards**:

| Context                   | Convention             | Example                        | Rationale                                |
| ------------------------- | ---------------------- | ------------------------------ | ---------------------------------------- |
| UI Labels (spec.md, i18n) | Title Case with Spaces | "Test Unit", "Analyzer"        | Human-readable, professional, accessible |
| Code (Java variables)     | camelCase              | `testUnits`, `analyzerId`      | Java naming standards                    |
| API (JSON keys)           | camelCase              | `"testUnits": [...]`           | JavaScript/JSON convention               |
| Database (column names)   | snake_case             | `test_unit_ids`, `analyzer_id` | PostgreSQL convention                    |
| Enums (Java)              | UPPERCASE_UNDERSCORE   | `FIELD_TYPE.NUMERIC`           | Java enum convention                     |
| Enums (UI display)        | Title Case             | "Numeric", "Qualitative"       | User-facing display                      |
| Page Titles               | Title Case, Plural     | "Analyzers", "Field Mappings"  | Navigation consistency                   |
| Entity Names (singular)   | PascalCase             | `Analyzer`, `AnalyzerField`    | Java class naming                        |

**Specific Standardizations**:

- **"Test Unit" vs "testUnits"**: Use "Test Unit" in UI/spec, `testUnits` in
  code/API
- **"Analyzer" capitalization**: Always capitalize in page titles ("Analyzers"),
  lowercase in descriptive text ("the analyzer sends...")
- **Field Type Display**: UPPERCASE for enum values (`NUMERIC`, `QUALITATIVE`),
  Title Case for UI labels ("Numeric", "Qualitative")

**Consistency Check**: All new specifications should follow these conventions.
Code review should flag terminology drift.

## Summary of Technical Decisions

| Decision Area          | Chosen Approach                                                             | Rationale                                                                           |
| ---------------------- | --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| ASTM Query             | New `AnalyzerQueryService` with TCP connection                              | Leverages existing infrastructure, generic across analyzers                         |
| Analyzer Entity        | Separate `AnalyzerConfiguration` entity (one-to-one with legacy `Analyzer`) | Avoids modifying legacy XML mappings, maintains backward compatibility              |
| Field Mapping          | Many-to-one with type compatibility validation                              | Supports real-world analyzer behavior, prevents unsafe conversions                  |
| Error Queue            | Database-backed `AnalyzerError` entity                                      | Persistent, auditable, integrates with existing infrastructure                      |
| Dual-Panel Layout      | Carbon Grid (50/50 split) with custom CSS for connection lines              | Follows Carbon Design System, responsive, accessible                                |
| Navigation Integration | Unified tab-navigation using sub-nav items (NO Carbon Tabs components)      | Avoids duplicate navigation, consistent with OpenELIS patterns, backend-driven menu |
| Message Processing     | Mapping-aware wrapper around existing plugin system                         | Maintains backward compatibility, applies mappings before insertion                 |
| Lifecycle Management   | Spring @Scheduled for automatic transitions                                 | Simple, reliable, no additional dependencies                                        |
| Test Mapping Preview   | Stateless service reusing ASTMAnalyzerReader                                | No code duplication, fast, safe (no persistence)                                    |
| Terminology            | Standardized naming conventions per context                                 | Consistency, maintainability, reduces confusion                                     |

## Open Questions (Resolved)

All research questions have been resolved. No outstanding technical unknowns.

## Next Steps

1. Generate data model (`data-model.md`) based on entity decisions above
2. Create API contracts (`contracts/`) for REST endpoints
3. Write quickstart guide (`quickstart.md`) for developers
