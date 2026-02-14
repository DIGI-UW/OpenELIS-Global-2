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

## 2.5 Querying Through XML-Mapped Entities - Manual Relationship Management

**Issue**: When querying `AnalyzerFieldMapping` (XML-mapped) that references
`AnalyzerField` (annotation-based), Hibernate generates SQL with incorrect table
name "analyzerfield" instead of "analyzer_field".

**Root Cause**: Hibernate doesn't properly resolve `@Table` annotation table
names when annotation-based entities are referenced from XML mappings. When
Hibernate processes the XML mapping and tries to resolve the `AnalyzerField`
entity via `many-to-one` relationships, it defaults to class-name-based table
resolution ("analyzerfield") instead of reading the
`@Table(name = "analyzer_field")` annotation. This causes SQL errors:
`ERROR: missing FROM-clause entry for table "analyzerfield"`.

**Solution Attempts**:

1. **Explicit `table` attribute on `many-to-one`**: Not a valid attribute in
   Hibernate XML DTD
2. **`entity-name` attribute**: Didn't resolve table name issue
3. **Minimal XML mapping for `AnalyzerField`**: Created `AnalyzerField.hbm.xml`
   with explicit table name, registered in `hibernate.cfg.xml` - still generates
   incorrect SQL
4. **Native SQL + manual relationship setting**: Avoids HQL but error persists
   when Hibernate validates/initializes relationship proxies during entity
   loading
5. **Avoiding relationship getters**: Added `getAnalyzerFieldIdByMappingId()`
   method to get field ID via native SQL - error still occurs during entity
   loading

**Final Solution: Manual Relationship Management**

Since Hibernate's relationship management fails when XML-mapped entities
reference annotation-based entities, we've implemented a manual relationship
management pattern that completely avoids Hibernate's relationship handling:

**Implementation**:

1. **XML Mapping Changes** (`AnalyzerFieldMapping.hbm.xml`):

   - Replaced `<many-to-one>` relationships with `<property>` columns for
     `analyzerFieldId` and `analyzerId`
   - This removes Hibernate's relationship validation path entirely

2. **Valueholder Changes** (`AnalyzerFieldMapping.java`):

   - Added `String analyzerFieldId` and `String analyzerId` as persistent fields
     (mapped via XML)
   - Changed `AnalyzerField analyzerField` and `Analyzer analyzer` to
     `transient` fields
   - Updated setters to sync IDs when entities are set (e.g.,
     `setAnalyzerField()` updates both `analyzerFieldId` and `analyzerId`)

3. **DAO Layer** (`AnalyzerFieldMappingDAOImpl.java`):

   - All queries use HQL only (no native SQL) - compliant with AGENTS.md
   - Queries reference ID fields directly: `WHERE afm.analyzerFieldId = :id`
   - No relationship joins or fetches - returns entities with ID fields only

4. **Service Layer Hydrator** (`AnalyzerFieldMappingHydrator.java`):

   - New component that manually loads and sets related entities
   - `hydrateAnalyzerField(mapping)`: Loads `AnalyzerField` and sets it on
     mapping
   - `hydrateAnalyzerFields(mappings)`: Batch loads fields for multiple mappings
     efficiently
   - Uses `AnalyzerFieldDAO.findByIdWithAnalyzer()` which works correctly
     (annotation-based entity)

5. **Service Layer Updates** (`AnalyzerFieldMappingServiceImpl.java`):
   - All methods that need related entities call hydrator after loading mappings
     from DAO
   - Relationships are hydrated within transaction boundaries
   - Business logic accesses hydrated entities via transient fields

**Benefits**:

- ✅ Eliminates Hibernate relationship validation errors
- ✅ DAO layer uses HQL only (compliant with AGENTS.md)
- ✅ Service layer controls when relationships are loaded (eager loading within
  transactions)
- ✅ Clear separation: DAO returns IDs, service hydrates relationships
- ✅ No native SQL workarounds needed

**Trade-offs**:

- ⚠️ Relationships must be explicitly hydrated in service layer (not automatic)
- ⚠️ Transient fields are not persisted (IDs are the source of truth)
- ⚠️ Requires discipline to hydrate relationships before accessing them

**Usage Pattern**:

```java
// DAO returns mappings with ID fields only
List<AnalyzerFieldMapping> mappings = dao.findByAnalyzerId(analyzerId);

// Service hydrates relationships
hydrator.hydrateAnalyzerFields(mappings);

// Now safe to access relationships
for (AnalyzerFieldMapping mapping : mappings) {
    AnalyzerField field = mapping.getAnalyzerField(); // Transient, hydrated
    String fieldName = field.getFieldName();
}
```

**Documentation**:

- DAO class javadoc explains HQL-only approach
- Service class javadoc explains manual relationship management
- Hydrator class javadoc explains hydration pattern
- Valueholder class javadoc explains transient relationship fields

**Future Improvements**:

- Consider database view for read-only queries that join these tables
- Migrate `AnalyzerFieldMapping` to pure annotations once `@Version` conflict
  with `BaseObject` is resolved
- This would allow Hibernate to manage relationships normally again

## 2.5.1 Previous Section (Deprecated)

### Decision: Two-Step Query Pattern or Denormalization for XML-Mapped Entity Traversal

**Context**: Legacy `Analyzer` entity uses Hibernate XML mappings instead of JPA
annotations. This creates challenges when querying annotation-based entities
that traverse relationships through the XML-mapped `Analyzer` entity.

**Limitation**: HQL queries that traverse relationships through XML-mapped
entities (e.g., `AnalyzerFieldMapping` → `AnalyzerField` → `Analyzer`) may fail
when using `JOIN FETCH` with relationship paths in WHERE clauses. Hibernate
generates invalid SQL with "missing FROM-clause entry" errors.

**Pattern**: Use two-step query approach:

1. Native SQL to get IDs:
   `SELECT id FROM analyzer_field WHERE analyzer_id = :analyzerId`
2. HQL with JOIN FETCH:
   `SELECT DISTINCT afm FROM AnalyzerFieldMapping afm LEFT JOIN FETCH afm.analyzerField WHERE afm.analyzerField.id IN :analyzerFieldIds`

**Alternative**: Denormalize by adding direct foreign key (e.g., `analyzer_id`
column directly on `analyzer_field_mapping` table). This eliminates query
complexity and improves performance (direct FK lookup vs join chain) while
maintaining data integrity via FK constraint.

**When to Use**:

- **Two-Step Pattern**: When denormalization is not acceptable or when querying
  through XML-mapped entities is infrequent
- **Denormalization**: When querying by analyzer is common and performance is
  critical (recommended for `AnalyzerFieldMapping` queries)

**Migration Path**: Future refactoring of `Analyzer` entity to annotations will
eliminate this exception and allow standard HQL queries.

**References**:

- `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerFieldMappingDAOImpl.java` -
  Example implementation
- Issue D1 remediation plan - Denormalization approach

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

## 9. Unified Status Field Management

### Decision: Event-Driven Automatic Status Transitions with Manual Override

**Rationale**: A unified status field (replacing separate `active` boolean and
`lifecycle_stage` enum) simplifies the data model and UI while capturing all
necessary states. Event-driven transitions using Spring event listeners provide
decoupled, maintainable status management that responds to analyzer state
changes in real-time.

**Implementation Approach**:

- **Unified Status Field**: Single `status` enum with values: INACTIVE, SETUP,
  VALIDATION, ACTIVE, ERROR_PENDING, OFFLINE
- **Event-Driven Transitions**: Spring `@EventListener` annotations on status
  transition methods that respond to domain events:
  - Mapping creation events → SETUP → VALIDATION
  - Mapping activation events → VALIDATION → ACTIVE (if all required mappings
    active)
  - Error creation events → ACTIVE → ERROR_PENDING
  - Connection test failure events → ACTIVE → OFFLINE
  - Error acknowledgment events → ERROR_PENDING → ACTIVE
  - Connection test success events → OFFLINE → ACTIVE
- **Connection Monitoring**: Periodic connection tests (configurable interval,
  default 5 minutes) automatically update status to OFFLINE if connection fails
- **Error Monitoring**: Error dashboard queries check for unacknowledged errors
  and trigger ERROR_PENDING status if found
- **Manual Override**: Users can always set status to INACTIVE via analyzer edit
  form or inline status dropdown (overrides all automatic transitions)
- **Audit Trail**: All status changes (automatic and manual) logged with user
  ID, timestamp, previous/new status

**Alternatives Considered**:

- **Option A**: Spring @Scheduled for periodic status checks - Rejected because
  event-driven approach provides real-time status updates and is more responsive
  to state changes
- **Option B**: Separate `active` boolean + `lifecycle_stage` enum - Rejected
  because unified field simplifies data model, reduces confusion, and eliminates
  need to derive status from two fields

**Status Transition Rules**:

- Automatic transitions based on analyzer state (see Implementation Approach
  above)
- Manual override to INACTIVE always available
- Other manual status changes restricted (only automatic transitions allowed
  except INACTIVE override)
- Transition validation: Each transition checks prerequisites (e.g., VALIDATION
  → ACTIVE requires all required mappings activated)

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

## 12. ASTM Communication Infrastructure

### Decision: Integrated ASTM-HTTP Bridge for Production-Like Communication

**Rationale**: ASTM analyzers communicate via TCP using the ASTM LIS2-A2
protocol, while OpenELIS receives messages via HTTP POST. The ASTM-HTTP bridge
is a middleware component that translates between these protocols, enabling
seamless communication between analyzers and OpenELIS.

**Implementation Approach**:

- **Bridge Deployment**: ASTM-HTTP bridge is integrated into the standard
  development environment via `dev.docker-compose.yml`
  - Container: `openelis-astm-bridge` (image: `digiuw/astm-http-bridge:latest`)
  - Static IP: `172.20.1.101` (within Docker network)
  - TCP Listener Port: `5001` (exposed on host)
  - Configuration: `volume/astm-bridge/configuration.yml` (mounted as read-only)
- **Communication Flows**:
  - **Flow 1 (Production-Like)**: Analyzer (TCP:5000) → ASTM-HTTP Bridge
    (TCP:5001) → OpenELIS (`/analyzer/astm` via HTTP POST)
  - **Flow 2 (Testing)**: Mock Server (HTTP POST) → OpenELIS (`/analyzer/astm`
    directly)
- **Bridge Configuration**:
  - OpenELIS URL:
    `https://oe.openelis.org:8443/api/OpenELIS-Global/analyzer/astm`
  - ASTM Port: `5001`
  - SSL Verification: Disabled for development (self-signed certs)
  - Logging Level: `DEBUG` for development troubleshooting
- **Integration Points**:
  - Bridge starts automatically with
    `docker compose -f dev.docker-compose.yml up -d`
  - Bridge accessible at `172.20.1.101:5001` from within Docker network
  - Analyzers configured with bridge IP address for TCP communication
  - OpenELIS receives HTTP POST requests from bridge at `/analyzer/astm`
    endpoint

**Alternatives Considered**:

- **Option A**: Direct TCP communication to OpenELIS
  - **Rejected**: OpenELIS uses HTTP-based message processing, requires protocol
    translation
- **Option B**: Separate bridge deployment (not integrated in dev setup)
  - **Rejected**: Bridge is core component for ASTM communication, should be
    part of standard dev environment

**References**:

- `dev.docker-compose.yml` - Bridge service definition
- `volume/astm-bridge/configuration.yml` - Bridge configuration
- `docs/astm.md` - ASTM bi-directional interface documentation
- [DIGI-UW/astm-http-bridge](https://github.com/DIGI-UW/astm-http-bridge) -
  Bridge repository
- [OpenELIS ASTM Communication Documentation](https://uwdigi.atlassian.net/wiki/external/YTllOWIzZWEzMmQ3NDllOWI4MGJlODc3MTQzYTI1MWI) -
  Comprehensive ASTM analyzer communication workflow and requirements

## 13. Analyzer Identification Strategies

### Decision: Multi-Strategy Identification with Fallback Chain

**Rationale**: Incoming ASTM messages must be associated with the correct
analyzer configuration to apply field mappings. Multiple identification
strategies provide robust matching even when message headers are incomplete or
IP addresses change.

**Implementation Approach**:

- **Strategy 1: ASTM Header Parsing** (Primary)
  - Parse H-segment (header) from ASTM message:
    `H|\\^&|||MANUFACTURER^MODEL^VERSION|...`
  - Extract manufacturer and model: `parts[0] + " " + parts[1]` (e.g., "Beckman
    Coulter DxH 800")
  - Lookup `AnalyzerConfiguration` by analyzer name using `getByAnalyzerName()`
  - **Implementation**: `ASTMAnalyzerReader.parseAnalyzerNameFromHeader()`
    method
- **Strategy 2: Client IP Address** (Fallback for Direct HTTP Push)
  - Extract client IP from HTTP request (`HttpServletRequest.getRemoteAddr()`)
  - Handle proxy headers: `X-Forwarded-For` (first IP in comma-separated list),
    `X-Real-IP`
  - Lookup `AnalyzerConfiguration` by IP address using `getByIpAddress()`
  - **Implementation**: `AnalyzerImportController.doPost()` extracts IP,
    `ASTMAnalyzerReader.setClientIpAddress()` stores it
- **Strategy 3: Plugin Identification** (Final Fallback)
  - Use existing plugin system: `plugin.getAnalyzerName()`
  - Lookup `Analyzer` by name using `AnalyzerService.getAnalyzerByName()`
  - **Implementation**: Falls back to plugin when header/IP identification fails

**Identification Flow**:

```java
Optional<Analyzer> identifyAnalyzerFromMessage() {
    // Strategy 1: Parse ASTM header
    String analyzerName = parseAnalyzerNameFromHeader();
    if (analyzerName != null) {
        Optional<AnalyzerConfiguration> config = configService.getByAnalyzerName(analyzerName);
        if (config.isPresent()) return Optional.of(config.get().getAnalyzer());
    }

    // Strategy 2: Client IP address
    if (clientIpAddress != null) {
        Optional<AnalyzerConfiguration> config = configService.getByIpAddress(clientIpAddress);
        if (config.isPresent()) return Optional.of(config.get().getAnalyzer());
    }

    // Strategy 3: Plugin fallback
    if (plugin != null) {
        Analyzer analyzer = analyzerService.getAnalyzerByName(plugin.getAnalyzerName());
        if (analyzer != null) return Optional.of(analyzer);
    }

    return Optional.empty();
}
```

**DAO/Service Methods**:

- `AnalyzerConfigurationDAO.findByIpAddress(String ipAddress)` - HQL query:
  `WHERE ac.ipAddress = :ipAddress`
- `AnalyzerConfigurationDAO.findByAnalyzerName(String name)` - HQL query:
  `JOIN ac.analyzer a WHERE a.name = :name`
- Service layer methods: `AnalyzerConfigurationService.getByIpAddress()`,
  `getByAnalyzerName()`

**Use Cases**:

- **Production (via Bridge)**: Analyzer sends TCP message → Bridge forwards HTTP
  POST → OpenELIS identifies by ASTM header
- **Direct HTTP Push (Testing)**: Mock server sends HTTP POST → OpenELIS
  identifies by client IP or ASTM header
- **Plugin-Based Analyzers**: Legacy analyzers without configuration → Falls
  back to plugin identification

**Alternatives Considered**:

- **Option A**: Single identification strategy (header only)
  - **Rejected**: Too fragile - fails if header format varies or IP-based
    identification needed
- **Option B**: Require explicit analyzer ID in message
  - **Rejected**: Not part of ASTM LIS2-A2 standard, requires analyzer vendor
    modifications

**References**:

- `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java` -
  Identification implementation
- `src/main/java/org/openelisglobal/analyzerimport/action/AnalyzerImportController.java` -
  IP extraction
- `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerConfigurationDAO.java` -
  Lookup methods
- `src/main/java/org/openelisglobal/analyzer/service/AnalyzerConfigurationService.java` -
  Service methods
- [OpenELIS ASTM Communication Documentation](https://uwdigi.atlassian.net/wiki/external/YTllOWIzZWEzMmQ3NDllOWI4MGJlODc3MTQzYTI1MWI) -
  Analyzer identification requirements and workflow

## 14. Field Query Response Parsing

### Decision: Robust R-Record Parsing with Composite Delimiter Handling

**Rationale**: ASTM query responses contain R-records (result records) with
field information. The mock server format
(`R|seq|astm_ref|field_name||unit|||field_type`) requires careful parsing to
correctly extract field name, ASTM reference, unit, and field type, especially
when units contain special characters (e.g., `10^3/μL`).

**Implementation Approach**:

- **R-Record Format**: `R|seq|astm_ref|field_name||unit|||field_type`
  - Example: `R|1|R|1|^^^WBC|WBC||10^3/μL|||NUMERIC`
  - Pipe-delimited segments with composite delimiters (`^`) within segments
- **Parsing Strategy** (aligned with `ASTMQSegmentParserImpl` pattern):
  1. **Split by pipe delimiter**: `line.split("\\|")`
  2. **Identify field name**: First non-empty part after sequence that doesn't
     start with `^` and isn't numeric
  3. **Reconstruct astm_ref**: All parts before field name (may contain `^`
     delimiters)
  4. **Extract unit**: First non-empty part after field name
  5. **Extract field type**: Last non-empty part that matches valid
     `AnalyzerField.FieldType` enum value
- **Edge Case Handling**:
  - **Units with `^` characters**: Only treat parts _starting with_ `^` (like
    `^^^WBC`) as part of `astm_ref` when determining `astmRefEndIndex`
  - **Invalid field types**: Default to `NUMERIC` if extracted value doesn't
    match enum
  - **Missing segments**: Handle gracefully with null checks

**Parsing Logic**:

```java
// Find fieldName as first non-empty part after sequence that doesn't start with ^
int fieldNameIndex = -1;
for (int i = 2; i < parts.length; i++) {
    if (parts[i] != null && !parts[i].trim().isEmpty()) {
        String part = parts[i].trim();
        // Skip parts that start with ^ (part of astm_ref) or are numeric
        if (!part.startsWith("^") && !part.matches("^\\d+$")) {
            fieldNameIndex = i;
            break;
        }
    }
}

// Reconstruct astm_ref from parts before fieldName
StringBuilder astmRefBuilder = new StringBuilder();
for (int i = 2; i < fieldNameIndex; i++) {
    if (parts[i] != null && !parts[i].trim().isEmpty()) {
        if (astmRefBuilder.length() > 0) astmRefBuilder.append("|");
        astmRefBuilder.append(parts[i]);
    }
}
String astmRef = astmRefBuilder.toString();

// Extract unit (first non-empty part after fieldName)
String unit = null;
for (int i = fieldNameIndex + 1; i < parts.length; i++) {
    if (parts[i] != null && !parts[i].trim().isEmpty()) {
        unit = parts[i].trim();
        break;
    }
}

// Extract fieldType (last non-empty part that matches enum)
AnalyzerField.FieldType fieldType = AnalyzerField.FieldType.NUMERIC; // default
for (int i = parts.length - 1; i >= 0; i--) {
    if (parts[i] != null && !parts[i].trim().isEmpty()) {
        try {
            fieldType = AnalyzerField.FieldType.valueOf(parts[i].trim());
            break;
        } catch (IllegalArgumentException e) {
            // Not a valid enum value, continue searching
        }
    }
}
```

**Integration Points**:

- `AnalyzerQueryServiceImpl.parseFieldRecords()` - Main parsing method
- Mock server format: `R|seq|astm_ref|field_name||unit|||field_type`
- Field type validation: Must match `AnalyzerField.FieldType` enum (NUMERIC,
  QUALITATIVE, TEXT, etc.)

**Alternatives Considered**:

- **Option A**: Use existing ASTM parsing library
  - **Rejected**: OpenELIS uses manual parsing for ASTM (not HL7);
    `ASTMQSegmentParserImpl` provides pattern but doesn't handle query response
    format
- **Option B**: Require mock server to use different format
  - **Rejected**: Mock server format aligns with ASTM LIS2-A2 standard; parsing
    should handle standard format

**References**:

- `src/main/java/org/openelisglobal/analyzer/service/AnalyzerQueryServiceImpl.java` -
  Parsing implementation
- `tools/astm-mock-server/server.py` - Mock server R-record format
- `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMQSegmentParserImpl.java` -
  Existing ASTM parsing pattern
- [OpenELIS ASTM Communication Documentation](https://uwdigi.atlassian.net/wiki/external/YTllOWIzZWEzMmQ3NDllOWI4MGJlODc3MTQzYTI1MWI) -
  ASTM message format and parsing requirements

## 15. Message Processing Workflow

### Decision: Integrated Mapping Application with Analyzer Identification

**Rationale**: ASTM messages must be processed through the mapping system when
analyzers have active mappings configured. The workflow integrates analyzer
identification, mapping lookup, and message transformation before data
insertion.

**Message Processing Flow**:

1. **Message Reception**:

   - HTTP POST to `/analyzer/astm` endpoint
     (`AnalyzerImportController.doPost()`)
   - Extract client IP address (with proxy header handling)
   - Create `ASTMAnalyzerReader` instance
   - Pass client IP to reader via `setClientIpAddress()`

2. **Message Parsing**:

   - `ASTMAnalyzerReader.readStream()` parses ASTM message into line segments
   - `setInserterResponder()` identifies analyzer plugin (if available)

3. **Analyzer Identification**:

   - `identifyAnalyzerFromMessage()` attempts identification via three
     strategies:
     - Strategy 1: Parse ASTM H-segment for manufacturer/model → lookup by
       analyzer name
     - Strategy 2: Use client IP address → lookup by IP
     - Strategy 3: Fall back to plugin identification
   - Returns `Optional<Analyzer>` if identified

4. **Mapping Application Decision**:

   - `wrapInserterIfMappingsExist()` checks if analyzer has active mappings:
     - If mappings exist: Wrap plugin inserter with
       `MappingAwareAnalyzerLineInserter`
     - If no mappings: Use original plugin inserter (backward compatibility)

5. **Data Processing**:

   - `processData()` determines message type:
     - **Query message**: Build response using `buildResponseForQuery()`
     - **Result message**: Insert data using wrapped/unwrapped inserter
   - `insertAnalyzerData()` applies mappings (if wrapped) or uses plugin
     directly

6. **Error Handling**:
   - Unmapped fields → Create `AnalyzerError` record
   - Validation failures → Create `AnalyzerError` with validation details
   - Errors queued in Error Dashboard for administrator review

**Integration Points**:

- `ASTMAnalyzerReader.wrapInserterIfMappingsExist()` - Conditional wrapping
  logic
- `MappingAwareAnalyzerLineInserter` - Wrapper that applies mappings before
  delegation
- `MappingApplicationService` - Service that applies field mappings to message
  segments
- `AnalyzerError` - Entity for error queue storage

**Backward Compatibility**:

- Analyzers without mappings: Continue using plugin system directly (no breaking
  changes)
- Analyzers with mappings: Use mapping-aware wrapper (new functionality)
- Plugin identification: Falls back to plugin when configuration-based
  identification fails

**References**:

- `src/main/java/org/openelisglobal/analyzerimport/analyzerreaders/ASTMAnalyzerReader.java` -
  Main reader implementation
- `src/main/java/org/openelisglobal/analyzer/service/MappingAwareAnalyzerLineInserter.java` -
  Mapping wrapper
- `src/main/java/org/openelisglobal/analyzer/service/MappingApplicationService.java` -
  Mapping application logic
- Specification FR-001, FR-011 (mapping application and error handling)
- [OpenELIS ASTM Communication Documentation](https://uwdigi.atlassian.net/wiki/external/YTllOWIzZWEzMmQ3NDllOWI4MGJlODc3MTQzYTI1MWI) -
  Complete message processing workflow and integration requirements

## Summary of Technical Decisions

| Decision Area           | Chosen Approach                                                             | Rationale                                                                           |
| ----------------------- | --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| ASTM Query              | New `AnalyzerQueryService` with TCP connection                              | Leverages existing infrastructure, generic across analyzers                         |
| Analyzer Entity         | Separate `AnalyzerConfiguration` entity (one-to-one with legacy `Analyzer`) | Avoids modifying legacy XML mappings, maintains backward compatibility              |
| Field Mapping           | Many-to-one with type compatibility validation                              | Supports real-world analyzer behavior, prevents unsafe conversions                  |
| Error Queue             | Database-backed `AnalyzerError` entity                                      | Persistent, auditable, integrates with existing infrastructure                      |
| Dual-Panel Layout       | Carbon Grid (50/50 split) with custom CSS for connection lines              | Follows Carbon Design System, responsive, accessible                                |
| Navigation Integration  | Unified tab-navigation using sub-nav items (NO Carbon Tabs components)      | Avoids duplicate navigation, consistent with OpenELIS patterns, backend-driven menu |
| Message Processing      | Mapping-aware wrapper around existing plugin system                         | Maintains backward compatibility, applies mappings before insertion                 |
| Status Management       | Event-driven automatic transitions with unified status field                | Real-time updates, simplified data model, manual override capability                |
| Test Mapping Preview    | Stateless service reusing ASTMAnalyzerReader                                | No code duplication, fast, safe (no persistence)                                    |
| Terminology             | Standardized naming conventions per context                                 | Consistency, maintainability, reduces confusion                                     |
| ASTM Communication      | Integrated ASTM-HTTP bridge in dev setup                                    | Core component for ASTM communication, enables production-like testing              |
| Analyzer Identification | Multi-strategy with fallback chain (header → IP → plugin)                   | Robust matching even when message headers incomplete or IP addresses change         |
| Field Query Parsing     | Robust R-record parsing with composite delimiter handling                   | Handles standard ASTM format including units with special characters                |
| Message Workflow        | Integrated identification and mapping application                           | Seamless message processing with automatic analyzer detection and mapping lookup    |

## Open Questions (Resolved)

All research questions have been resolved. No outstanding technical unknowns.

## Next Steps

1. Generate data model (`data-model.md`) based on entity decisions above
2. Create API contracts (`contracts/`) for REST endpoints
3. Write quickstart guide (`quickstart.md`) for developers
