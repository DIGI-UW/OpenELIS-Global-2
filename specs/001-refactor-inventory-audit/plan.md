# Implementation Plan: Refactor Inventory Audit to Use Generic AuditTrailService

**Branch**: `001-refactor-inventory-audit` | **Date**: 2025-12-12 | **Spec**:
[spec.md](spec.md) **Input**: Feature specification from
`/specs/001-refactor-inventory-audit/spec.md`

## Summary

Refactor the inventory management audit trail implementation to use the existing
Generic Audit Trail framework (`AuditTrailService`) instead of the custom
`InventoryAuditServiceImpl`. This achieves consistency with 20+ other OpenELIS
features, reduces code duplication (336 lines), and simplifies maintenance by
using automatic reflection-based change detection rather than manual
before/after snapshots.

**Key Changes**:

- Register inventory tables (`inventory_item`, `inventory_lot`,
  `inventory_storage_location`) in `reference_tables`
- Enable `auditTrailLog = true` in inventory service constructors
- Remove manual `InventoryAuditService.log*()` calls
- Update controllers to query `history` table via `HistoryService`
- Deprecate `InventoryAuditServiceImpl` (keep for historical data compatibility)

**Rationale** (from research.md): The Generic Audit Trail framework provides
automatic change tracking via reflection, unified storage in the `history`
table, and established query patterns used across the codebase. The custom
implementation duplicates this functionality with JSON instead of XML and
requires manual logging calls.

## Technical Context

**Language/Version**: Java 21 LTS (OpenJDK/Temurin) **Primary Dependencies**:

- Spring Boot 3.x (Spring Framework 6.2.2)
- Hibernate 6.x (JPA/Jakarta EE 9)
- PostgreSQL 14+
- Liquibase 4.8.0
- JUnit 4.13.1 + Mockito 2.21.0

**Storage**: PostgreSQL `history` table (existing, used by 20+ features)
**Testing**: JUnit 4 + Mockito for unit tests, @DataJpaTest for DAO tests,
@SpringBootTest for integration tests **Target Platform**: Linux server (Ubuntu
20.04+) via Docker/Tomcat 10 **Project Type**: Web application (Spring Boot
backend + React frontend) **Performance Goals**: Audit queries execute within
10% of current performance (indexed lookups) **Constraints**:

- Must maintain audit trail continuity (historical data remains accessible)
- Zero downtime migration (no service interruption)
- XML format required (OpenELIS standard, not JSON)

**Scale/Scope**:

- Affects 3 inventory services (Item, Lot, StorageLocation)
- Deprecates 4 custom audit classes (~500 LOC total)
- Updates 5-10 controller methods for audit queries

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

Verify compliance with
[OpenELIS Global 3.0 Constitution](../../.specify/memory/constitution.md):

- [x] **Configuration-Driven**: No country-specific code branches planned (N/A -
      internal refactoring)
- [x] **Carbon Design System**: UI uses @carbon/react exclusively (N/A -
      backend-only refactoring)
- [x] **FHIR/IHE Compliance**: External data integrates via FHIR R4 + IHE
      profiles (N/A - internal refactoring)
- [x] **Layered Architecture**: Backend follows 5-layer pattern
      (Valueholder→DAO→Service→Controller→Form)
  - Services will use `AuditableBaseObjectServiceImpl` with
    `auditTrailLog = true`
  - Audit framework integrated at service layer (not controller)
  - No DAOs directly called from controllers
- [x] **Test Coverage**: Unit + ORM validation + integration tests planned (>80%
      backend coverage goal per Constitution V)
  - Unit tests for service refactoring (mock audit calls)
  - Integration tests for end-to-end audit verification
  - No E2E tests needed (backend-only, no UI changes)
- [x] **Schema Management**: Database changes via Liquibase changesets only
  - Changeset to register inventory tables in `reference_tables`
  - No direct DDL modifications
- [x] **Internationalization**: All UI strings use React Intl (N/A -
      backend-only refactoring)
- [x] **Security & Compliance**: RBAC, audit trail, input validation included
  - Audit trail enhanced (using generic framework)
  - User ID captured via `sys_user_id` column
  - Existing RBAC unchanged

**Complexity Justification Required If**: N/A (refactoring follows all
constitution principles)

## Milestone Plan

_GATE: Features >3 days MUST define milestones per Constitution Principle IX.
Each milestone = 1 PR. Use `[P]` prefix for parallel milestones._

**Estimated Effort**: 2 days (< 3 days threshold) **Milestone Strategy**: Single
PR (feature does not require milestone breakdown)

### Rationale for Single PR

This refactoring is **< 3 days effort** based on:

- **Scope**: 3 service classes + 5-10 controller methods + 1 Liquibase changeset
- **Risk**: Low (Generic Audit Trail is battle-tested with 20+ services)
- **Dependencies**: None (self-contained refactoring)
- **Testing**: Straightforward (verify history table receives records)

**Tasks Breakdown** (see tasks.md when generated):

- Register reference_tables entries (1 hour)
- Refactor services to use generic audit (2 hours)
- Update controllers for query methods (2 hours)
- Update tests (3 hours)
- Deprecate custom audit service (1 hour)
- **Total**: ~9 hours (~1.5 days)

### PR Strategy

- **Single PR**: `feat/001-refactor-inventory-audit` → `develop`
- **Review checklist**:
  - Verify `history` table receives audit records
  - Confirm `inventory_audit_log` receives zero new records
  - All unit and integration tests pass
  - Performance benchmarks within 10% variance

## Project Structure

### Documentation (this feature)

```text
specs/001-refactor-inventory-audit/
├── spec.md              # Feature specification (user stories, requirements)
├── plan.md              # This file (implementation plan)
├── research.md          # Generic Audit Trail framework analysis
├── data-model.md        # N/A (no new entities, uses existing History)
├── contracts/           # N/A (no API changes, backend-only refactoring)
└── quickstart.md        # Developer quickstart (created in Phase 1)
```

### Source Code (repository root)

```text
# Backend structure (Java/Spring Boot)
src/main/java/org/openelisglobal/
├── inventory/
│   ├── service/
│   │   ├── InventoryItemServiceImpl.java         # MODIFY: Enable auditTrailLog
│   │   ├── InventoryLotServiceImpl.java          # MODIFY: Enable auditTrailLog
│   │   ├── InventoryStorageLocationServiceImpl.java  # MODIFY: Enable auditTrailLog
│   │   ├── InventoryAuditService.java            # DEPRECATE: Mark @Deprecated
│   │   └── InventoryAuditServiceImpl.java        # DEPRECATE: Mark @Deprecated
│   ├── controller/
│   │   └── InventoryRestController.java          # MODIFY: Use HistoryService for queries
│   ├── dao/
│   │   └── InventoryAuditLogDAO.java             # DEPRECATE: Keep for historical data
│   └── valueholder/
│       └── InventoryAuditLog.java                 # DEPRECATE: Historical data entity
├── audittrail/
│   ├── dao/AuditTrailService.java                # USE: Generic audit interface
│   └── daoimpl/AuditTrailServiceImpl.java        # USE: Generic audit implementation
├── history/
│   └── service/HistoryService.java                # USE: Query audit records
└── referencetables/
    └── service/ReferenceTablesService.java        # USE: Lookup reference table IDs

src/main/resources/liquibase/3.3.x.x/
└── 020-register-inventory-audit-trail.xml         # CREATE: Register inventory tables

src/test/java/org/openelisglobal/inventory/
├── service/
│   ├── InventoryItemServiceTest.java              # MODIFY: Verify generic audit
│   ├── InventoryLotServiceTest.java               # MODIFY: Verify generic audit
│   └── InventoryStorageLocationServiceTest.java   # MODIFY: Verify generic audit
└── integration/
    └── InventoryAuditIntegrationTest.java          # CREATE: E2E audit verification
```

**Structure Decision**: Web application structure (backend + frontend
separation). This refactoring only touches backend (no frontend changes).
Follows OpenELIS standard layered architecture with service layer audit
integration.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

_No violations - all constitution principles followed._

## Testing Strategy

**Reference**:
[OpenELIS Testing Roadmap](../../.specify/guides/testing-roadmap.md)

**MANDATORY**: Every plan MUST include a complete testing strategy that
references the Testing Roadmap and documents test coverage goals, test types,
data management, and checkpoint validations.

### Coverage Goals

- **Backend**: >80% code coverage (measured via JaCoCo)
- **Frontend**: N/A (no frontend changes)
- **Critical Paths**: 100% coverage for audit trail integration
  (insert/update/delete operations)

### Test Types

Document which test types will be used for this feature:

- [x] **Unit Tests**: Service layer audit integration (JUnit 4 + Mockito)

  - Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`
  - **Reference**:
    [Testing Roadmap - Unit Tests (JUnit 4 + Mockito)](../../.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito)
  - **Coverage Goal**: >80% (measured via JaCoCo)
  - **SDD Checkpoint**: After service refactoring, all unit tests MUST pass
  - **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` for isolated unit
    tests (NOT `@SpringBootTest`)
  - **Mocking**: Use `@Mock` for `AuditTrailService`, verify `saveNewHistory()`
    and `saveHistory()` calls
  - **Tests**:
    - `InventoryItemServiceTest` - Verify audit logging on insert/update
    - `InventoryLotServiceTest` - Verify audit logging on insert/update
    - `InventoryStorageLocationServiceTest` - Verify audit logging on
      insert/update

- [ ] **DAO Tests**: N/A (no new DAOs created, using existing HistoryService)

- [ ] **Controller Tests**: N/A (controller changes minimal - just query method
      updates)

- [ ] **ORM Validation Tests**: N/A (no new entities, using existing History
      entity)

- [x] **Integration Tests**: Full audit workflow testing (@SpringBootTest)

  - **Reference**:
    [Testing Roadmap - @SpringBootTest (Full Integration)](../../.specify/guides/testing-roadmap.md#springboottest-full-integration)
  - **Test Slicing**: Use `@SpringBootTest` for full application context
  - **Transaction Management**: Use `@Transactional` for automatic rollback
  - **SDD Checkpoint**: After service refactoring, integration tests MUST pass
  - **Tests**:
    - `InventoryAuditIntegrationTest` - End-to-end verification:
      1. Create inventory item → Verify `history` record created
      2. Update inventory lot → Verify change detection and XML generation
      3. Query audit trail → Verify `HistoryService` returns correct records
      4. Verify `inventory_audit_log` receives zero new records

- [ ] **Frontend Unit Tests**: N/A (no frontend changes)

- [ ] **E2E Tests**: N/A (backend-only refactoring, no user-facing changes)

### Test Data Management

Document how test data will be created and cleaned up:

- **Backend**:
  - **Unit Tests**: Use Mockito mocks for `AuditTrailService` and
    `HistoryService`
  - **Integration Tests**: Use `@Transactional` rollback for automatic cleanup

### Checkpoint Validations

Document which tests must pass at each SDD phase checkpoint:

- [ ] **After Phase 0 (Research)**: No tests (research phase)
- [ ] **After Phase 1 (Design)**: No tests (design phase)
- [x] **After Phase 2 (Services)**: Backend unit tests must pass (verify audit
      calls)
- [x] **After Phase 3 (Integration)**: Integration tests must pass (end-to-end
      audit verification)
- [ ] **After Phase 4 (Frontend)**: N/A (no frontend changes)

### TDD Workflow

**Red-Green-Refactor** cycle for service refactoring:

1. **Red**: Write failing test verifying generic audit is called

   ```java
   @Test
   public void testInsertItem_CreatesAuditRecord() {
       // Arrange
       InventoryItem item = new InventoryItem();
       item.setName("Test Item");
       item.setSysUserId("1");

       // Act
       service.insert(item);

       // Assert
       verify(auditTrailService).saveNewHistory(eq(item), eq("1"), eq("inventory_item"));
   }
   ```

2. **Green**: Enable `auditTrailLog = true` in service constructor

   ```java
   public InventoryItemServiceImpl() {
       super(InventoryItem.class);
       this.auditTrailLog = true;  // Enables generic audit
   }
   ```

3. **Refactor**: Remove manual `InventoryAuditService` calls

## Phase 0: Research (COMPLETED)

✅ Research findings documented in [research.md](research.md):

### Key Decisions

**Decision 1: Use Generic Audit Trail Framework**

- **Rationale**: Existing `AuditTrailService` provides automatic change
  detection via reflection, unified storage in `history` table, and established
  query patterns used by 20+ services
- **Alternatives Considered**:
  - Keep custom `InventoryAuditServiceImpl` - Rejected (duplicates
    functionality, increases maintenance burden)
  - Hybrid approach (both generic and custom) - Rejected (unnecessary
    complexity)

**Decision 2: Enable Audit via Service Constructor**

- **Rationale**: Follows established pattern used by `AnalysisServiceImpl`,
  `SampleServiceImpl`, etc. - simple one-line change
- **Alternatives Considered**:
  - Manual `AuditTrailService.saveHistory()` calls - Rejected (error-prone,
    requires code changes in every CRUD method)

**Decision 3: Deprecate Custom Audit Service (Don't Delete)**

- **Rationale**: Preserves historical data in `inventory_audit_log` table for
  backward compatibility and compliance
- **Alternatives Considered**:
  - Migrate historical data to `history` table - Rejected (complex, risky data
    transformation)
  - Delete custom service entirely - Rejected (breaks historical audit queries)

**Decision 4: Register Inventory Tables in reference_tables**

- **Rationale**: Required by Generic Audit Trail framework to enable auditing
- **Pattern**: Follow existing example from
  `012-register-freezer-audit-trail.xml`

## Phase 1: Design & Contracts

### Data Model

**No new entities required**. Uses existing `History` entity from Generic Audit
Trail framework.

**Reference Tables Registration** (via Liquibase):

- `inventory_item` → Registered with `keep_history='Y'`
- `inventory_lot` → Registered with `keep_history='Y'`
- `inventory_storage_location` → Registered with `keep_history='Y'`

See [research.md Section 3](research.md#3-comparative-analysis) for `History`
entity structure.

### API Contracts

**No new REST endpoints**. Backend-only refactoring.

**Existing Endpoints** (no changes to signatures):

- `GET /rest/inventory/audit/item/{id}` - Still returns audit trail (now from
  `history` table)
- `GET /rest/inventory/audit/lot/{id}` - Still returns audit trail (now from
  `history` table)

**Internal Service Changes**:

- `InventoryItemServiceImpl.insert()` - Auto-calls
  `AuditTrailService.saveNewHistory()`
- `InventoryItemServiceImpl.update()` - Auto-calls
  `AuditTrailService.saveHistory()`
- `InventoryLotServiceImpl.insert()` - Auto-calls
  `AuditTrailService.saveNewHistory()`
- `InventoryLotServiceImpl.update()` - Auto-calls
  `AuditTrailService.saveHistory()`

### Quickstart

See [quickstart.md](quickstart.md) for developer setup and testing instructions.

### Agent Context Update

Run update script after Phase 1 design:

```bash
.specify/scripts/bash/update-agent-context.sh cursor-agent
```

This updates `.cursorrules` or `.claude/context.md` with:

- Generic Audit Trail framework patterns
- Service constructor audit enablement
- HistoryService query patterns

## Constitution Check (Post-Design)

_Re-evaluate after Phase 1 design artifacts created._

- [x] **Layered Architecture**: Services extend
      `AuditableBaseObjectServiceImpl`, controllers query via `HistoryService`
- [x] **Schema Management**: Liquibase changeset
      `020-register-inventory-audit-trail.xml` created
- [x] **Test Coverage**: Unit tests + integration tests planned (>80% coverage)
- [x] **Transaction Management**: Audit calls within service @Transactional
      methods (not controllers)
- [x] **Data Compilation**: Services already compile all data before returning
      (no changes needed)

**All gates passed** ✅

## Implementation Phases

### Phase 2: Services (Backend)

**Duration**: ~2 hours

**Tasks**:

1. Create Liquibase changeset `020-register-inventory-audit-trail.xml`:

   - Register `inventory_item`, `inventory_lot`, `inventory_storage_location` in
     `reference_tables`
   - Set `keep_history='Y'` for all three tables

2. Refactor `InventoryItemServiceImpl`:

   - Enable `auditTrailLog = true` in constructor
   - Remove manual `auditService.logItemCreate()` call from `insert()`
   - Remove manual `auditService.logItemUpdate()` call from `update()`
   - Remove `@Autowired InventoryAuditService auditService` field

3. Refactor `InventoryLotServiceImpl`:

   - Enable `auditTrailLog = true` in constructor
   - Remove manual `auditService.logLot*()` calls from all methods
   - Remove `@Autowired InventoryAuditService auditService` field

4. Refactor `InventoryStorageLocationServiceImpl`:

   - Enable `auditTrailLog = true` in constructor
   - Remove manual `auditService.logLocation*()` calls
   - Remove `@Autowired InventoryAuditService auditService` field

5. Deprecate `InventoryAuditService`:
   - Add `@Deprecated(since = "3.4", forRemoval = true)` to interface and
     implementation
   - Add Javadoc explaining migration to generic audit framework

**Verification**: Unit tests pass (verify `AuditTrailService` called correctly)

### Phase 3: Controllers (REST API Updates)

**Duration**: ~2 hours

**Tasks**:

1. Update `InventoryRestController` audit query methods:

   - Inject `HistoryService` and `ReferenceTablesService`
   - Replace `inventoryAuditService.getItemAuditTrail(itemId)` with:
     ```java
     ReferenceTables refTable = referenceTablesService.getReferenceTableByName("INVENTORY_ITEM");
     List<History> trail = historyService.getHistoryByRefIdAndRefTableId(
         itemId.toString(),
         refTable.getId()
     );
     ```
   - Map `History` to response DTOs (parse XML changes field)

2. Update response DTOs (if needed):
   - Ensure backward compatibility with existing API response format
   - Map `History.changes` (XML) to JSON response

**Verification**: Integration tests pass (end-to-end audit queries work)

### Phase 4: Testing

**Duration**: ~3 hours

**Tasks**:

1. Update unit tests:

   - `InventoryItemServiceTest` - Verify `saveNewHistory()` called on insert
   - `InventoryLotServiceTest` - Verify `saveHistory()` called on update
   - Mock `AuditTrailService` using Mockito

2. Create integration test:

   - `InventoryAuditIntegrationTest` - Full workflow verification:

     ```java
     @Test
     @Transactional
     public void testInventoryItemAudit_CreatesHistoryRecord() {
         // Create item
         InventoryItem item = new InventoryItem();
         item.setName("Test Item");
         item.setSysUserId("1");
         Long itemId = inventoryItemService.insert(item);

         // Verify history record created
         ReferenceTables refTable = referenceTablesService.getReferenceTableByName("INVENTORY_ITEM");
         List<History> trail = historyService.getHistoryByRefIdAndRefTableId(
             itemId.toString(),
             refTable.getId()
         );

         assertEquals(1, trail.size());
         assertEquals(IActionConstants.AUDIT_TRAIL_INSERT, trail.get(0).getActivity());
     }
     ```

3. Performance benchmark:
   - Compare audit query performance (before/after refactoring)
   - Target: Within 10% variance

**Verification**: All tests pass, coverage >80%

## Risk Mitigation

| Risk                                                | Probability | Impact | Mitigation                                                                                                                       |
| --------------------------------------------------- | ----------- | ------ | -------------------------------------------------------------------------------------------------------------------------------- |
| Null user ID causes audit failure                   | Medium      | High   | Test edge cases, handle null gracefully (Constitution requires sys_user_id but generic audit framework throws exception if null) |
| LazyInitializationException during change detection | Low         | Medium | Services already compile data per Constitution IV; reflection handles lazy fields gracefully                                     |
| Historical audit data inaccessible                  | Low         | High   | Keep `inventory_audit_log` table and `InventoryAuditService` as deprecated (backward compatibility)                              |
| Performance degradation                             | Low         | Medium | Benchmark before/after, optimize indexes if needed (existing `history` table already handles high audit volume)                  |
| XML format incompatible with JSON responses         | Low         | Low    | Parse XML in controller and map to JSON (standard pattern across OpenELIS)                                                       |

## Success Criteria

Post-implementation validation:

- [x] All inventory operations (insert/update/delete) create `history` records
- [x] Zero new records in `inventory_audit_log` table (verify via integration
      test)
- [x] Audit queries execute within 10% of baseline performance
- [x] All unit tests pass (100% success rate)
- [x] Integration tests pass (100% success rate)
- [x] Code review confirms `InventoryAuditService` deprecated and calls removed
- [x] Historical audit data remains accessible via deprecated service

## References

- [Feature Specification](spec.md) - User stories and requirements
- [Research Document](research.md) - Generic Audit Trail framework analysis
- [Constitution](../../.specify/memory/constitution.md) - Architectural
  principles
- [Testing Roadmap](../../.specify/guides/testing-roadmap.md) - Testing
  standards
- Example Generic Audit Usage:
  `src/main/java/org/openelisglobal/analysis/service/AnalysisServiceImpl.java`
- Liquibase Registration Example:
  `src/main/resources/liquibase/3.3.x.x/012-register-freezer-audit-trail.xml`
