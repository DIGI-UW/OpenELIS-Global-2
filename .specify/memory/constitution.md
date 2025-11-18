# OpenELIS Global 3.0 Constitution

<!--
SYNC IMPACT REPORT - Enhanced Cypress E2E Testing Workflow and Review Requirements
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Version Change: 1.6.0 → 1.7.0
Change Type: MINOR - Materially expanded guidance on test execution workflow and review requirements
Date: 2025-11-07

Added Sections:
  - Principle V (Test-Driven Development) > Section V.5: Test Execution Workflow
    * NEW: Mandate to run tests individually during development (not full suite)
    * NEW: Maximum 5-10 test cases per execution during development
    * NEW: Full suite runs only in CI/CD pipeline or pre-merge validation
    * Rationale: Faster feedback, easier debugging, prevents cascading failures

  - Principle V > Section V.5: Enhanced Browser Console Logging
    * CHANGED: From "strategic" to "MUST be enabled and reviewed"
    * NEW: Mandatory review of browser console logs after each test run
    * NEW: Check for JavaScript errors, API failures, unexpected warnings
    * Rationale: Console logs reveal underlying issues not visible in test output

  - Principle V > Section V.5: Post-Run Review Requirements
    * NEW: Mandatory review checklist (console logs, screenshots, test output)
    * NEW: Review required before marking tests as passing or filing bug reports
    * Rationale: Ensures thorough debugging and prevents false positives

Modified Sections:
  - Principle V > Section V.5: Enhanced Anti-Patterns
    * ADDED: Arbitrary time delays (use Cypress built-in waiting mechanisms)
    * ADDED: Missing element readiness checks
    * ADDED: Not leveraging Cypress retry-ability
    * ADDED: Setting up intercepts after actions
    * REMOVED: Technical code examples (moved to plan.md/research.md per functional vs technical separation)

Rationale for Changes:
  Current Cypress E2E tests have deficiencies: tests don't follow best practices, are messy,
  need to be run in smaller chunks, and lack mandatory review processes. This amendment
  establishes clear workflow requirements (individual execution during development) and
  mandatory review processes (console logs and screenshots) to ensure tests are debuggable
  and maintainable. Enhanced anti-patterns prevent common mistakes validated against
  Cypress official documentation.

  This guidance ensures:
  - Faster feedback through individual test execution
  - Thorough debugging through mandatory console log and screenshot review
  - Prevention of common anti-patterns that lead to flaky tests
  - Clear separation between functional requirements (constitution) and technical implementation (plan/research)

Templates Requiring Updates:
  ✅ .specify/templates/plan-template.md - Updated Constitution Check to reference V.5 for E2E requirements
  ✅ .specify/templates/tasks-template.md - Added E2E test task example with reference to V.5

Follow-up TODOs:
  - Update existing E2E tests to follow new workflow (run individually)
  - Ensure all developers review console logs and screenshots post-run
  - Refactor tests to eliminate anti-patterns (arbitrary waits, missing readiness checks)

Commit Message:
  docs: amend constitution to v1.7.0 (enhanced Cypress E2E testing workflow and review requirements)

  - Add test execution workflow: run tests individually during development
  - Enhance browser console logging: mandatory review after each run
  - Add post-run review requirements: mandatory checklist for console logs and screenshots
  - Expand anti-patterns: arbitrary waits, missing readiness checks, not leveraging retry-ability
  - Remove technical code examples (functional vs technical separation)
  - Ensures debuggable and maintainable E2E tests aligned with Cypress best practices

SYNC IMPACT REPORT - Cypress E2E Testing Best Practices
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Version Change: 1.5.0 → 1.6.0
Change Type: MINOR - New section added to existing principle
Date: 2025-11-05

Added Sections:
  - Principle V (Test-Driven Development) > Section V.5: Cypress E2E Testing Best Practices
    * NEW: Configuration requirements (video disabled by default, screenshots enabled, console logging)
    * NEW: Test organization requirements (user story focus, file structure, avoid test bloat)
    * NEW: Debugging and maintenance guidelines (screenshot review, console logging, performance monitoring)
    * NEW: Example configuration and test structure
    * NEW: Anti-patterns to avoid
    * Rationale: E2E tests must remain fast, debuggable, and focused on user stories

Modified Sections:
  - None (new section only)

Rationale for Changes:
  During implementation of feature 001-sample-storage, Cypress E2E tests grew to 65+ test cases
  with video recording enabled, causing slow execution (>15 minutes) and disk space issues. By
  disabling video recording, using screenshots for debugging, and optimizing test structure,
  execution time reduced to <5 minutes. Console logging and screenshot review provide sufficient
  debugging information without performance overhead.

  This guidance ensures:
  - Fast test execution (target: <5 minutes for full suite)
  - Clear debugging via screenshots and console logging
  - Focus on user stories/use cases rather than implementation details
  - Prevention of test suite bloat and slow execution

Templates Requiring Updates:
  ⚠️ .specify/templates/plan-template.md - Add Cypress E2E testing best practices to Constitution Check
  ⚠️ .specify/templates/spec-template.md - Add E2E testing requirements to test specifications
  ⚠️ .specify/templates/tasks-template.md - Add Cypress E2E task templates with best practices

Follow-up TODOs:
  - Update cypress.config.js to set video: false by default
  - Review existing E2E tests for compliance with best practices
  - Document screenshot review process in test README
  - Add performance monitoring to CI/CD pipeline

Commit Message:
  docs: amend constitution to v1.6.0 (Cypress E2E testing best practices)

  - Add Section V.5: Cypress E2E Testing Best Practices
  - Mandate video disabled by default, screenshots enabled, strategic console logging
  - Require user story focus and test organization best practices
  - Prevents slow/cumbersome test suites while maintaining debugging capability
  - Based on Phase 3 implementation experience (001-sample-storage)

SYNC IMPACT REPORT - Transaction Management in Service Layer Only
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Version Change: 1.4.0 → 1.5.0
Change Type: MINOR - Explicit prohibition of transaction annotations in controllers
Date: 2025-11-05

Added Sections:
  - Principle IV (Layered Architecture) > Controllers: Explicit prohibition of @Transactional
    * NEW: Controllers MUST NOT use @Transactional annotations
    * NEW: All transaction management MUST be in service layer only
    * Rationale: Transaction boundaries belong in service layer, not controller layer

  - Principle IV > Anti-Patterns: Added "@Transactional annotations in controllers"
    * Explicit prohibition: @Transactional in controller methods is anti-pattern
    * Example: `@GetMapping("/endpoint") @Transactional(readOnly = true)` is prohibited

Modified Sections:
  - Principle IV > Services: Strengthened transaction rule language
    * CHANGED: "Transactions start here (NOT in controllers)" to "Transactions start here (NOT in controllers) - @Transactional annotations MUST be on service methods only, NEVER on controller methods"

Rationale for Changes:
  During Phase 3.1 implementation of feature 001-sample-storage (search functionality),
  @Transactional annotations were incorrectly placed on controller methods. This violates
  the layered architecture principle that transaction boundaries belong in the service layer,
  not the controller layer. Controllers should be thin and delegate to services, which
  manage transaction boundaries.

  This is a fundamental architectural principle:
  - Controllers handle HTTP request/response mapping only
  - Services handle business logic AND transaction management
  - Transaction boundaries must be at the service layer for proper error handling and rollback
  - Mixing transaction management in controllers creates unclear boundaries and violates separation of concerns

Templates Requiring Updates:
  ✅ .specify/templates/plan-template.md - Added explicit prohibition of @Transactional in controllers to Constitution Check
  ✅ .specify/templates/spec-template.md - Added transaction management rule to architectural constraints (CR-003)
  ⚠️ .specify/templates/tasks-template.md - Add verification task for transaction annotation placement (pending)

Follow-up TODOs:
  - Review existing controllers for @Transactional violations (SampleStorageRestController has one remaining)
  - Add code review checklist item: "Verify no @Transactional in controllers"
  - Document transaction rollback behavior in service layer

Commit Message:
  docs: amend constitution to v1.5.0 (explicit prohibition of @Transactional in controllers)

  - Add explicit rule: Controllers MUST NOT use @Transactional annotations
  - Add anti-pattern: @Transactional in controller methods
  - Strengthen service layer transaction rule language
  - Clarifies transaction boundary management in layered architecture
  - Based on Phase 3.1 implementation experience (001-sample-storage search functionality)

SYNC IMPACT REPORT - Service Layer Data Compilation Requirement
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Version Change: 1.3.0 → 1.4.0
Change Type: MINOR - Critical architectural principle added
Date: 2025-11-04

Added Sections:
  - Principle IV (Layered Architecture) > Services: Data Compilation Rule
    * NEW: Services MUST eagerly fetch and compile ALL data needed for responses
    * NEW: Controllers MUST NOT traverse entity relationships
    * NEW: Services must return complete DTOs/maps with all hierarchical data resolved
    * Rationale: Prevents LazyInitializationException when transactions close

  - Principle IV > Anti-Patterns: Added "Controllers accessing entity relationships"
    * Explicit prohibition of relationship traversal in controllers
    * Example: `position.getParentRack().getParentShelf()` is anti-pattern

Rationale for Changes:
  During Phase 3 implementation of feature 001-sample-storage, controller attempted
  to build hierarchical paths by traversing relationships:
  `assignment.getStoragePosition().getParentRack().getParentShelf().getParentDevice().getParentRoom()`
  This caused LazyInitializationException because parent relationships are LAZY-loaded
  and the service transaction had already closed. The fix required moving all data
  compilation to the service layer using JOIN FETCH queries to eagerly load the
  entire hierarchy within the transaction.

  This is a fundamental requirement for transactional Hibernate/JPA applications:
  - Lazy loading only works within active transactions
  - Controllers are outside transaction boundaries
  - Services must return complete, ready-to-use data structures

Templates Requiring Updates:
  ⚠️ .specify/templates/plan-template.md - Add service layer data compilation to Constitution Check
  ⚠️ .specify/templates/spec-template.md - Add requirement for service methods to return complete DTOs
  ⚠️ .specify/templates/tasks-template.md - Add task template for service layer data compilation

Follow-up TODOs:
  - Document HQL JOIN FETCH patterns for eager loading hierarchies
  - Add code examples showing service method returning complete maps/DTOs
  - Review existing controllers for relationship traversal violations

Commit Message:
  docs: amend constitution to v1.4.0 (service layer data compilation requirement)

  - Add critical rule: Services must compile all data within transaction
  - Prohibit relationship traversal in controllers
  - Prevents LazyInitializationException in transactional Hibernate applications
  - Based on Phase 3 implementation experience (001-sample-storage)

SYNC IMPACT REPORT - Annotation-Based Hibernate Mappings & Pre-Commit Formatting
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Version Change: 1.2.0 → 1.3.0
Change Type: MINOR - Technical stack requirement update + workflow enforcement
Date: 2025-11-03

Modified Sections:
  - Principle IV (Layered Architecture) > Valueholders: Mandated annotation-based Hibernate mappings
    * CHANGED: From "Hibernate XML mappings in `src/main/resources/hibernate/hbm/*.hbm.xml`"
    * TO: "JPA/Hibernate annotations on entity classes (NO XML mapping files)"
    * Rationale: Modern Hibernate best practice, better IDE support, compile-time validation
    * Migration: Legacy code using XML mappings exempt until refactored

  - Development Workflow > Pull Request Requirements: Enhanced pre-commit formatting requirement
    * CHANGED: From "before committing" (recommended)
    * TO: "MUST run before each commit" (mandatory enforcement)
    * Added: Explicit requirement to run both `npm run format` AND `mvn spotless:apply`
    * Rationale: Prevents formatting inconsistencies in PRs, reduces review noise

  - Principle V > Section V.4 (ORM Validation Tests): Updated to reflect annotation-based mappings
    * CHANGED: Example test from XML-based (`config.addResource("hibernate/hbm/Entity1.hbm.xml")`)
    * TO: Annotation-based (`config.addAnnotatedClass(Entity1.class)`)
    * Updated property validation to check annotations instead of XML files

Rationale for Changes:
  During feature 001-sample-storage implementation, use of XML mapping files created:
  1. Maintenance overhead: changes required in both entity classes AND XML files
  2. IDE limitations: reduced autocomplete and refactoring support
  3. Debugging complexity: mapping errors only discovered at runtime
  Annotation-based mappings provide compile-time validation and better developer experience.

  Pre-commit formatting enforcement prevents common PR issues:
  1. Formatting-only commits that clutter git history
  2. Review time wasted on style discussions
  3. CI/CD failures due to formatting violations

Templates Requiring Updates:
  ✅ .specify/templates/plan-template.md - Updated Constitution Check to reflect annotation requirement
  ✅ .specify/templates/spec-template.md - Updated CR-003 to reflect annotation-based mappings
  ✅ .specify/templates/tasks-template.md - Updated ORM validation test example to use annotations
  ⚠️ README.md - Should mention annotation-based mappings in Hibernate section (pending)
  ⚠️ CONTRIBUTING.md - Should emphasize pre-commit formatting requirement (pending)

Follow-up TODOs:
  - Create migration guide for converting existing XML mappings to annotations
  - Add pre-commit hook script (.git/hooks/pre-commit) to enforce formatting
  - Update CI/CD pipeline to fail fast on formatting violations
  - Document annotation patterns for custom Hibernate types (e.g., LIMSStringNumberUserType)

Commit Message:
  docs: amend constitution to v1.3.0 (annotation-based mappings + pre-commit formatting)

  - Mandate JPA/Hibernate annotations instead of XML mapping files
  - Require `npm run format` and `mvn spotless:apply` before each commit
  - Update ORM validation test examples to use annotations
  - Improves developer experience and prevents formatting inconsistencies
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

SYNC IMPACT REPORT - ORM Validation Test Layer
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Version Change: 1.1.0 → 1.2.0
Change Type: MINOR - New testing requirement for ORM projects
Date: 2025-10-31

Added Sections:
  - Principle V > Section V.4: ORM Validation Tests
    * Mandates framework validation tests between unit and integration tests
    * Requires SessionFactory/EntityManagerFactory build test
    * Requires JavaBean convention validation (no getter conflicts)
    * Requires property name consistency checks
    * Must execute in <5 seconds without database

Rationale:
  During Phase 3 implementation of feature 001-sample-storage, TDD successfully validated
  business logic via unit tests (17/17 passing) but missed ORM configuration errors:
  1. Hibernate getter conflict: getActive() vs isActive()
  2. Property name mismatch: movedByUser vs movedByUserId
  These only appeared at application startup. A 2-second ORM test would catch immediately.

Templates Requiring Updates:
  ⚠️ .specify/templates/plan-template.md - Add ORM validation to test pyramid
  ⚠️ .specify/templates/tasks-template.md - Add ORM validation task templates

Impact:
  - Future Hibernate/JPA features MUST include ORM validation tests
  - Feature 001-sample-storage: ORM test added (HibernateMappingValidationTest.java)

Commit Message:
  docs: amend constitution to v1.2.0 (ORM validation test requirement)

  Add Principle V, Section V.4 mandating ORM validation tests
  Fills gap between unit tests (mocked) and integration tests (full stack)
  Catches mapping errors in <5s without database
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

SYNC IMPACT REPORT - Technical Stack Clarifications
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Version Change: 1.0.0 → 1.1.0
Change Type: MINOR - Technical stack clarifications and corrections
Date: 2025-10-31

Modified Sections:
  - Technical Stack Constraints > Backend (Java): Enhanced Java version requirements
    * Added CRITICAL warning about Java version incompatibility
    * Added .sdkmanrc file reference for SDKMAN users
    * Added verification commands
    * Clarified Jakarta EE 9 requirement (jakarta.persistence NOT javax.persistence)

  - Technical Stack Constraints > Backend (Java): Corrected JUnit version
    * CORRECTED: JUnit 4 (4.13.1) is actual OpenELIS standard (NOT JUnit 5)
    * Added specific import examples (org.junit.Test vs org.junit.jupiter.api.Test)
    * Added assertion syntax guidance (assertEquals parameter order)

  - Principle V (Test-Driven Development): Updated test framework requirements
    * Corrected backend testing to JUnit 4 + Mockito 2.21.0
    * Added JUnit 4 vs JUnit 5 import warnings

Rationale for Changes:
  During sample storage POC implementation (001-sample-storage), developer encountered:
  1. Build failure due to using Java 8 instead of Java 21
  2. Test compilation errors due to using JUnit 5 instead of JUnit 4
  3. Persistence API errors (javax.persistence vs jakarta.persistence confusion)

  These amendments make version requirements EXPLICIT and PROMINENT to prevent future mistakes.

Templates Requiring Updates:
  ⚠️ quickstart.md templates - Add Java version verification step
  ⚠️ README.md - Emphasize Java 21 requirement in prerequisites
  ⚠️ CONTRIBUTING.md - Add JUnit 4 testing examples

Follow-up TODOs:
  - Create .sdkmanrc files in example projects/demos
  - Add Java version check to CI/CD pipeline (fail if not Java 21)
  - Update test templates to show JUnit 4 examples
  - Document Jakarta EE 9 migration patterns for new developers

Commit Message:
  docs: amend constitution to v1.1.0 (Java 21 & JUnit 4 clarifications)

  - Emphasize Java 21 MANDATORY requirement (incompatible with Java 8/11/17)
  - Add .sdkmanrc reference for SDKMAN automatic version switching
  - Clarify Jakarta EE 9 persistence API (jakarta.persistence)
  - CORRECT JUnit version: JUnit 4 (NOT JUnit 5) per actual codebase
  - Add JUnit 4 import examples to prevent test compilation errors

  Prevents build failures and test framework mismatches
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
-->

## Core Principles

### I. Configuration-Driven Variation

**MANDATE**: Country-specific customizations (accession formats, phone formats,
patient identifiers, address fields) MUST be implemented via configuration, NOT
code branching.

**Rules**:

- NO country-specific code branches or forks
- Use database-driven configuration tables (`SystemConfiguration`,
  `LocalizationConfiguration`)
- Validation patterns via properties files, NOT hardcoded logic
- New variations require configuration schema extension, NOT code duplication

**Rationale**: OpenELIS serves 30+ countries with diverse requirements. Code
fragmentation creates unmaintainable technical debt. A unified codebase with
configuration-driven variation enables centralized bug fixes and feature updates
while respecting local needs.

**Example**: Accession number format "YYYY-NNNNN" vs "LAB-YYYY-MM-NNNNN"
configured via `common.properties`, not separate Java classes.

---

### II. Carbon Design System First

**MANDATE** (Effective August 2024): All new UI components MUST use Carbon
Design System exclusively. NO custom CSS frameworks, NO Bootstrap, NO Tailwind.

**Rules**:

- Use `@carbon/react` v1.15+ components exclusively for new features
- Styling via Carbon tokens (`$spacing-*`, `$text-*`, `$layer-*`) ONLY
- Typography: IBM Plex Sans (Carbon default) - NO custom fonts without
  justification
- Layout: Carbon Grid + Column system - NO flexbox/grid outside Carbon patterns
- Icons: `@carbon/icons-react` v11.17+ - NO custom icon libraries
- Customize via Carbon theme tokens, NOT custom CSS overrides

**Rationale**: Strategic adoption of Carbon ensures UI/UX consistency,
accessibility (WCAG 2.1 AA), and alignment with modern design systems used by
major healthcare platforms. Ad-hoc styling creates maintenance debt and
accessibility failures.

**Migration**: Legacy components may use older frameworks until refactored. New
features have NO exemption.

**Reference**:
[OpenELIS Carbon Design Guide](https://uwdigi.atlassian.net/wiki/spaces/OG/pages/621346838)

---

### III. FHIR/IHE Standards Compliance

**MANDATE**: All healthcare data interoperability MUST use HL7 FHIR R4 + IHE
profiles. NO proprietary APIs for external integration.

**Rules**:

- **HAPI FHIR R4** (v6.6.2) for local FHIR store at
  `org.openelisglobal.fhirstore.uri`
- **IHE mCSD** (Mobile Care Services Discovery) for Location/Organization
  resources
- **IHE Lab** profiles for DiagnosticReport, Observation, Specimen,
  ServiceRequest
- All entities with external exposure MUST have `fhir_uuid UUID` column +
  bidirectional transform
- Use `FhirPersistanceService` for CRUD, `FhirTransformService` for entity ↔
  FHIR conversion
- Sync to consolidated server (SHR/IPS) on insert/update operations
- Support subscriptions: Task, Patient, ServiceRequest, DiagnosticReport,
  Observation, Specimen, Practitioner, Encounter

**Rationale**: National health information exchanges require standards-based
interoperability. FHIR ensures OpenELIS integrates with OpenMRS 3.x (Lab on
FHIR), facility registries (GoFR, OpenHIM), and SHR/IPS systems without custom
adapters.

**Configuration** (`common.properties`):

```properties
org.openelisglobal.fhirstore.uri=https://fhir.openelis.org:8443/fhir/
org.openelisglobal.fhir.subscriber=https://consolidated.openelis.org/fhir/
org.openelisglobal.fhir.subscriber.resources=Task,Patient,ServiceRequest,DiagnosticReport,Observation,Specimen,Practitioner,Encounter
```

**Reference**: Existing infrastructure in `org.openelisglobal.fhir.*` packages.

---

### IV. Layered Architecture Pattern

**MANDATE**: All backend features MUST follow strict 5-layer structure. NO
direct database access from controllers, NO business logic in DAOs.

**Layers**:

1. **Valueholders** (JPA Entities): `org.openelisglobal.{module}.valueholder`

   - Extend `BaseObject<String>` (provides id, sys_user_id, lastupdated)
   - Include `fhir_uuid UUID` for FHIR-mapped entities
   - **MANDATORY**: Use JPA/Hibernate annotations on entity classes (`@Entity`,
     `@Table`, `@Id`, `@Column`, `@ManyToOne`, etc.)
   - **PROHIBITED**: NO XML mapping files (`.hbm.xml`) - legacy XML mappings
     exempt until refactored
   - Validation annotations on fields (`@NotNull`, `@Size`, etc.)
   - ID generation via `@GenericGenerator` with sequence name
   - `@PrePersist` hook for fhir_uuid generation

2. **DAOs** (Data Access): `org.openelisglobal.{module}.dao`

   - Interface + Implementation (extends `BaseDAOImpl<Entity, String>`)
   - Annotate with `@Component` + `@Transactional`
   - Methods: get, insert, update, delete, custom queries
   - Use HQL (Hibernate Query Language) ONLY - NO native SQL in code

3. **Services** (Business Logic): `org.openelisglobal.{module}.service`

   - Interface + Implementation (annotate with `@Service` + `@Transactional`)
   - **Transactions start here (NOT in controllers)** - `@Transactional`
     annotations MUST be on service methods only, NEVER on controller methods
   - Call DAOs for persistence, FHIR services for sync
   - Validation logic before persistence
   - Logging via `LogEvent.logError()` for errors
   - **CRITICAL - Data Compilation Rule**: Services MUST eagerly fetch and
     compile ALL data needed for the response within the service transaction.
     Controllers MUST NOT traverse entity relationships (e.g.,
     `assignment.getStoragePosition().getParentRack().getParentShelf()`).
     Services must return complete DTOs/maps with all hierarchical data already
     resolved. This prevents lazy loading exceptions when transactions close.
   - **Rationale**: Hibernate lazy loading only works within an active
     transaction. If controllers access relationships after the service
     transaction commits, `LazyInitializationException` occurs. Services must
     use `JOIN FETCH` in HQL queries to eagerly load all required relationships.

4. **Controllers** (REST Endpoints): `org.openelisglobal.{module}.controller`

   - Extend `BaseRestController`
   - Annotate with `@RestController` + `@RequestMapping("/rest/{module}")`
   - Methods: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
   - **Controllers are singletons** - NO class-level variables (thread safety)
   - Responsibilities: Form validation, request/response mapping, service calls
   - NOT for business logic (delegate to services)
   - **PROHIBITED**: NO `@Transactional` annotations on controller methods -
     transaction management belongs in service layer only

5. **Forms/DTOs**: `org.openelisglobal.{module}.form`
   - Simple beans for client ↔ server communication
   - Validation annotations validate client input

**Rationale**: Layered architecture enforces separation of concerns,
testability, and transaction boundary clarity. Prevents spaghetti code where SQL
mixes with HTTP handling and business rules.

**Anti-Patterns to Avoid**:

- Controllers calling DAOs directly (bypasses business logic layer)
- Business logic in DAOs (query methods should be data retrieval only)
- Native SQL in Java code (breaks database portability, bypasses Hibernate
  caching)
- Class-level variables in controllers (thread safety violations)
- **Controllers accessing entity relationships** (e.g.,
  `position.getParentRack().getParentShelf()`) - Services must return complete
  data structures with all relationships resolved within the transaction
- **@Transactional annotations in controllers** - Transaction management MUST be
  in service layer only. Controllers delegate to services, which manage
  transaction boundaries. Example anti-pattern:
  `@GetMapping("/endpoint") @Transactional(readOnly = true)`

---

### V. Test-Driven Development

**MANDATE**: New features MUST include automated tests. All testing practices
MUST adhere to the standards and procedures outlined in the authoritative
**OpenELIS Testing Roadmap** (`.specify/guides/testing-roadmap.md`).

**Core Requirements**:

- **TDD Workflow**: Red-Green-Refactor cycle is mandatory for complex logic
- **Test Coverage Goals**: >80% backend (JaCoCo), >70% frontend (Jest)
- **Checkpoint Validations**: Tests must pass at each SDD phase checkpoint (per
  Spec-Driven Development workflow)
- **Test Data Management**: Use builders/factories, NOT hardcoded values

**Test Organization**:

- Backend: `src/test/java/org/openelisglobal/{module}/`
- Frontend: `frontend/src/components/{feature}/*.test.js`
- E2E: `frontend/cypress/e2e/{feature}.cy.js`

**CI/CD Gates**:

- `mvn spotless:check` (code formatting)
- `mvn clean install` (build + unit tests)
- `npm run cy:run` (E2E tests)
- All must pass before merge to `develop`

**Rationale**: Healthcare software failures impact patient care. Automated
testing catches regressions, enforces contract compliance, and enables confident
refactoring.

**Reference**: For detailed testing patterns, strategies, and best practices,
see [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md). This roadmap
provides comprehensive guidance for both AI agents and human developers on test
creation, execution, and maintenance.

#### Section V.4: ORM Validation Tests (ADDED 2025-10-31)

**MANDATE**: For projects using Object-Relational Mapping frameworks
(Hibernate/JPA, Entity Framework, TypeORM, SQLAlchemy), the test suite MUST
include framework validation tests that verify ORM configuration correctness
WITHOUT requiring database connection or full application context.

**Purpose**: Fills critical gap between unit tests (pure mocks, no framework)
and integration tests (full stack). Catches ORM configuration errors in <5
seconds rather than at deployment.

**Requirements for Hibernate/JPA Projects**:

- MUST include test that builds `SessionFactory` or `EntityManagerFactory`
- MUST validate all entity mappings load without errors
- MUST verify no JavaBean getter/setter conflicts (e.g., both `getActive()` and
  `isActive()`)
- MUST verify property names match between entity classes and annotations (for
  annotation-based mappings)
- MUST execute in <5 seconds
- MUST NOT require database connection

**Requirements for Liquibase/Flyway Projects**:

- SHOULD include test that parses changesets without executing them
- SHOULD validate XML/SQL syntax
- SHOULD check for common errors (unescaped operators like `<=`, missing CDATA)

**Test Execution Order**:

```
1. Unit Tests (Mockito mocked) - Business logic validation
2. ORM Validation Tests - Framework configuration validation
3. Integration Tests (with database) - Full stack validation
4. E2E Tests (Cypress) - User workflow validation
```

**Example** (Hibernate with annotations):

```java
@Test
public void testHibernateMappingsLoadSuccessfully() {
    Configuration config = new Configuration();
    config.addAnnotatedClass(Entity1.class);
    config.addAnnotatedClass(Entity2.class);
    config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    SessionFactory sf = config.buildSessionFactory();
    assertNotNull("All mappings should load", sf);
    sf.close();
}
```

**Rationale**: During implementation of feature 001-sample-storage (Phase 3),
pure unit tests with mocked DAOs successfully validated business logic but
missed ORM configuration errors that only appeared at application startup: (1)
Getter conflicts: `getActive()` (Boolean) vs `isActive()` (boolean) caused
Hibernate introspection failure, (2) Property mismatches: Entity had
`movedByUser`, annotations expected `movedByUserId`. A 2-second ORM validation
test would have caught both immediately.

**Exception**: Projects without ORM frameworks (pure JDBC, NoSQL, etc.) may skip
this requirement.

#### Section V.5: Cypress E2E Testing Best Practices (ADDED 2025-11-05, ENHANCED 2025-11-07)

**MANDATE**: Cypress E2E tests MUST follow efficiency and maintainability
principles to ensure fast execution, clear debugging, and accurate user story
coverage without becoming slow or cumbersome.

**Purpose**: E2E tests validate complete user workflows end-to-end, but they
must remain fast, debuggable, and focused on user stories/use cases rather than
implementation details.

**Test Execution Workflow**:

- **Individual Execution**: E2E tests MUST be executed individually in small,
  manageable chunks during development. Full test suite runs are for CI/CD only.
  - Run individual test files during development
  - Maximum 5-10 test cases per execution during development
  - Full suite runs only in CI/CD pipeline or pre-merge validation
  - **Rationale**: Running tests individually provides faster feedback, easier
    debugging, and prevents cascading failures from masking root causes.

**Command Examples**:

```bash
# Development (CORRECT - run individual test)
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"

# CI/CD only (NOT during development)
npm run cy:run
```

**Configuration Requirements**:

- **Video Recording**: MUST be disabled by default
  - Video recording slows test execution significantly and consumes disk space
  - Enable only for debugging specific failures when needed
- **Screenshots**: MUST be enabled for failures
  - Screenshots provide visual debugging information without performance
    overhead
  - Review screenshots after test failures (see Post-Run Review Requirements)
- **Browser Console Logging**: MUST be enabled for all test executions and
  reviewed after each run
  - Browser console logging enabled by default (Cypress captures automatically)
  - Review browser console logs in Cypress UI after each test run
  - Check for JavaScript errors, API failures, and unexpected warnings
  - **Rationale**: Console logs reveal underlying issues (network failures,
    JavaScript errors) that may not be visible in test output alone.

**Test Organization Requirements**:

- **User Story Focus**: E2E tests MUST map directly to user stories/use cases
  - One test file per user story (e.g., `storageAssignment.cy.js` for P1)
  - Test cases validate acceptance criteria, not implementation details
  - Avoid testing what unit tests already cover (e.g., form validation logic)
- **Test File Structure**: MUST follow naming convention
  - File: `{feature}.cy.js` (e.g., `storageMovement.cy.js`)
  - Test cases: `it('should {user action} {expected outcome}', ...)`
  - Group related tests: `describe('User Story P1: Basic Assignment', ...)`
- **Avoid Test Bloat**: MUST prevent tests from becoming slow/cumbersome
  - Maximum 20-30 test cases per feature (if more, split into multiple files)
  - Focus on critical paths, not edge cases (edge cases belong in unit tests)
  - Avoid redundant tests (if assignment works via dropdown, don't need separate
    tests for every dropdown interaction)
  - Use parameterized tests for similar scenarios (e.g.,
    `[freezer, refrigerator].forEach(...)`)

**Post-Run Review Requirements**:

- **Mandatory Review**: After each test execution (especially failures),
  developers MUST review console logs and screenshots before marking tests as
  passing or filing bug reports.
- **Review Checklist**:
  1. **Console Logs**: Review browser console in Cypress UI for errors, failed
     API requests, warnings
  2. **Screenshots**: Review failure screenshots for UI state at failure point
  3. **Test Output**: Review Cypress command log for execution order and
     timeouts
- **Documentation**: Document findings in test file comments or PR description
  if issues are discovered.

**Debugging and Maintenance**:

- **Test Isolation**: Use `testIsolation: false` only when necessary (shared
  state across tests)
  - Prefer isolated tests that can run independently
  - If shared state needed, document rationale in test file header
- **Performance Monitoring**: Track test execution time
  - Target: Individual test <30 seconds, full suite <5 minutes
  - If tests exceed targets, refactor to reduce setup/teardown overhead

**Rationale**: During implementation of feature 001-sample-storage, Cypress E2E
tests grew to 65+ test cases with video recording enabled, causing slow
execution (>15 minutes) and disk space issues. By disabling video recording,
using screenshots for debugging, and optimizing test structure, execution time
reduced to <5 minutes. Console logging and screenshot review provide sufficient
debugging information without performance overhead.

**Anti-Patterns to Avoid** (validated against Cypress official documentation):

- ❌ **Video recording enabled by default** - Slows execution, consumes disk
  space
- ❌ **Arbitrary time delays** - Use Cypress's built-in waiting mechanisms
  instead of fixed timeouts
- ❌ **Missing element readiness checks** - Wait for elements to be
  visible/ready before interaction
- ❌ **Testing implementation details** - E2E tests should validate user
  workflows, not internal component logic
- ❌ **Not leveraging Cypress retry-ability** - Use assertions that
  automatically retry instead of immediate checks
- ❌ **Setting up intercepts after actions** - Intercepts must be set up before
  actions that trigger them
- ❌ **Redundant test cases** - Don't test the same workflow multiple times with
  minor variations
- ❌ **Per-test setup/teardown** - Use shared setup hooks for efficiency
- ❌ **No console log review** - Always review browser console logs after test
  execution
- ❌ **Not using data-testid selectors** - Use data-testid as primary selector
  strategy (most stable, survives CSS changes and refactoring)
- ❌ **Ineffective DOM queries** - Use scoped queries, viewport management,
  table filtering (see Testing Roadmap for patterns)
- ❌ **Recreating test data via UI** - Use API-based setup for test data (10x
  faster than UI interactions)
- ❌ **Starting new sessions unnecessarily** - Use cy.session() with
  cacheAcrossSpecs to preserve login state (10-20x faster)

**Reference to Testing Roadmap**:

All Cypress testing practices MUST adhere to the standards and procedures
outlined in the authoritative **OpenELIS Testing Roadmap**
(`.specify/guides/testing-roadmap.md`).

The Testing Roadmap provides comprehensive technical guidance on:

- Selector strategy (data-testid priority, ARIA roles, semantic selectors)
- Session management (cy.session() patterns with OpenELIS adaptation)
- Test data management (API-first approach, fixture patterns)
- DOM query effectiveness (scoped queries, viewport, table filtering)
- Test simplification (happy path focus, user workflow validation)
- Carbon component patterns (ComboBox, DataTable, Modal, OverflowMenu)
- cy.intercept() patterns (official Cypress pattern with aliases)
- Debugging techniques (Chrome DevTools integration)
- Migration strategy (how to migrate existing tests)

**Note**: Technical implementation details (code examples, configuration syntax)
belong in the Testing Roadmap and plan.md, not in the constitution. This section
focuses on functional requirements and principles.

---

### VI. Database Schema Management

**MANDATE**: All database changes MUST go through Liquibase. NO direct DDL/DML
in production.

**Rules**:

- Schema migrations in `src/main/resources/liquibase/{module}/`
- Changesets MUST have unique IDs: `{module}-{sequence}-{description}`
- Use Liquibase XML format (NOT raw SQL unless necessary for performance)
- Rollback scripts MUST be provided for structural changes
- Test migrations on empty database AND production-like data volume
- NO `ALTER TABLE` or `CREATE TABLE` via psql/pgAdmin in deployed environments

**Rationale**: Liquibase ensures repeatable deployments, version control for
schema, and audit trail for compliance (SLIPTA/ISO requirements). Direct SQL
bypasses these safeguards.

**Example Changeset**:

```xml
<changeSet id="storage-001-create-storage-room-table" author="dev-team">
  <createTable tableName="storage_room">
    <column name="id" type="VARCHAR(36)"><constraints primaryKey="true"/></column>
    <column name="fhir_uuid" type="UUID"><constraints nullable="false" unique="true"/></column>
    <column name="name" type="VARCHAR(255)"><constraints nullable="false"/></column>
    <column name="sys_user_id" type="INT"><constraints nullable="false"/></column>
    <column name="lastupdated" type="TIMESTAMP" defaultValueComputed="NOW()"/>
  </createTable>
  <rollback><dropTable tableName="storage_room"/></rollback>
</changeSet>
```

---

### VII. Internationalization First

**MANDATE**: All user-facing strings MUST be externalized via React Intl. NO
hardcoded English text in components.

**Rules**:

- Message files in `frontend/src/languages/{locale}.json`
- Use `intl.formatMessage({ id: 'storage.location.label' })` for all text
- Supported locales: en (English), fr (French), ar (Arabic), es (Spanish), hi
  (Hindi), pt (Portuguese), sw (Swahili)
- New features MUST provide translations for at least en + fr
- Date/time formatting via `intl.formatDate()`, `intl.formatTime()`
- Number formatting via `intl.formatNumber()`

**Rationale**: OpenELIS operates in multilingual countries (e.g., Rwanda: en/fr,
Kenya: en/sw). Hardcoded strings force costly retrofitting and delay deployment.

**Example**:

```javascript
// ❌ BAD
<Button>Save Location</Button>

// ✅ GOOD
<Button>{intl.formatMessage({ id: 'button.save.location' })}</Button>
```

**Translation Files** (`en.json`):

```json
{
  "button.save.location": "Save Location",
  "storage.location.label": "Storage Location",
  "error.location.required": "Storage location is required"
}
```

---

### VIII. Security & Compliance

**MANDATE**: OpenELIS MUST meet SLIPTA (Stepwise Laboratory Quality Improvement
Process Towards Accreditation) and ISO 15189 requirements.

**Requirements**:

- **Authentication**: Spring Security 6.0.4 + session management
- **Authorization**: Role-based access control (RBAC) via `sys_role` table
- **Audit Trail**: All data changes logged with user ID + timestamp
  (BaseObject.sys_user_id, BaseObject.lastupdated)
- **Data Privacy**: PHI (Protected Health Information) access logged, GDPR-ready
  deletion workflows
- **Secure Transport**: HTTPS enforced (Tomcat SSL/TLS config in
  `volume/tomcat/oe_server.xml`)
- **Password Policy**: Configurable via `SystemConfiguration` (min length,
  complexity, expiry)
- **Input Validation**: Hibernate Validator + Formik validation on frontend

**Compliance Checklist** (for new features):

- [ ] Role-based access control implemented
- [ ] Audit trail captures user actions
- [ ] Input validated against injection attacks (SQL, XSS)
- [ ] Sensitive data encrypted at rest (if applicable)
- [ ] HTTPS endpoints only (NO HTTP for PHI)

**Rationale**: Public health laboratories require accreditation to receive
funding and participate in disease surveillance networks. Security failures risk
patient data breaches and legal penalties.

**Reference**:
[OpenELIS Admin Manual Section 4: Security](https://docs.openelis-global.org/)

---

## Technical Stack Constraints

**NON-NEGOTIABLE**: All code MUST use these versions/frameworks. Exceptions
require architecture review + documented justification.

### Backend (Java)

**CRITICAL: Java Version**

- **Java 21 LTS** (OpenJDK/Temurin distribution) - **MANDATORY**
- ⚠️ **NOT compatible with Java 8, 11, or 17** - Build will fail with older
  versions
- Use `.sdkmanrc` file for automatic version switching with SDKMAN
- Verify: `java -version` should show `openjdk version "21.x.x"`
- Maven compiler plugin requires Java 21 for `--release 21` flag

**Frameworks & Dependencies**

- **Spring Boot 3.x** (Spring Framework 6.2.2)
- **Hibernate 6.x** (Hibernate 5.6.15.Final ORM)
- **Jakarta EE 9** persistence API (NOT javax.persistence - use
  jakarta.persistence)
- **JPA** for all database operations (NO JDBC, NO native SQL in code)
- **PostgreSQL 14+** (production database)
- **Liquibase 4.8.0** for schema migrations
- **HAPI FHIR R4** (version 6.6.2)
- **Maven 3.8+** build system
- **Spotless** code formatter (`tools/OpenELIS_java_formatter.xml`)
- **Tomcat 10 / Jakarta EE 9** application server

**Testing Framework**

- **JUnit 4** (4.13.1) - **NOT JUnit 5** - Existing codebase uses JUnit 4
- **Mockito 2.21.0** for mocking
- Use `org.junit.Test` (NOT `org.junit.jupiter.api.Test`)
- Use `org.junit.Assert.*` assertions (NOT `org.junit.jupiter.api.Assertions.*`)
- Spring test support: `@RunWith(SpringRunner.class)` for integration tests

### Frontend (React)

- **React 17** (react-scripts 5.0.1)
- **Carbon Design System v1.15** (@carbon/react v1.15.0) - OFFICIAL UI FRAMEWORK
- **Carbon Icons** (@carbon/icons-react v11.17.0)
- **Carbon Charts** (@carbon/charts-react v1.5.2) for data visualization
- **SWR 2.0.3** for data fetching + caching
- **React Router DOM 5.2.0** for routing
- **React Intl 5.20.12** for i18n (MANDATORY)
- **Formik 2.2.9** + **Yup 0.29.2** for forms/validation
- **Sass 1.54.3** for styling (Carbon token overrides only)
- **Prettier 3.4.2** for formatting
- **ESLint 8.48.0** for linting
- **Cypress 12.17.3** for E2E testing
- **Jest + React Testing Library** for unit tests

### FHIR Integration

- **HAPI FHIR R4 Server** (co-habitant at
  `https://fhir.openelis.org:8443/fhir/`)
- **IHE mCSD Profile** for Location/Organization resources
- **Consolidated Server** with SHR (Shared Health Record) + IPS (International
  Patient Summary)
- **OpenMRS 3.x Integration** via Lab on FHIR module
- **Facility Registry Sync** (GoFR, OpenHIM supported)

### Deployment

- **Docker + Docker Compose** multi-container orchestration
- **PostgreSQL** database container
- **HAPI FHIR** server container (Bitnami Tomcat image)
- **Nginx** reverse proxy (SSL termination, load balancing)
- **Ubuntu 20.04+** host OS

### Prohibited Technologies

- ❌ **Custom CSS Frameworks** (Tailwind, Bootstrap) - Use Carbon only
- ❌ **Direct SQL** (JDBC, JdbcTemplate) - Use JPA/Hibernate
- ❌ **Native DDL/DML** - Use Liquibase
- ❌ **Hardcoded Strings** - Use React Intl
- ❌ **Class-level variables in Controllers** - Thread safety violation

**Rationale**: Technology standardization reduces onboarding time, simplifies
dependency management, and enables predictable troubleshooting. Deviation
creates hidden maintenance costs.

---

## Development Workflow

### Branch Strategy

- **`develop`** - Main development branch (all PRs target this)
- **`main`** - Production releases only (reviewers backport from develop)
- **Feature branches**: `issue-{###}-{feature-name}` (e.g.,
  `issue-123-storage-management`)
- **Hotfix branches**: `hotfix-{description}` (merged to develop + main)

### Pull Request Requirements

**MANDATORY CHECKLIST** (from [PULL_REQUEST_TIPS.md](PULL_REQUEST_TIPS.md)):

1. **GitHub Issue**: PR MUST reference issue number in title (e.g., "issue-123:
   Add storage location widget")
2. **Branch Naming**: Branch name matches `issue-{###}-{feature-name}`
3. **Target Branch**: Always `develop` (unless hotfix)
4. **Code Formatting** (MANDATORY - MUST run before each commit):
   - Backend: `mvn spotless:apply` - MUST run before committing
   - Frontend: `npm run format` (Prettier) - MUST run before committing
   - **ENFORCEMENT**: Pre-commit hooks recommended to prevent formatting-only
     commits
   - **CI/CD**: Formatting violations will cause build failures
5. **Build Verification**: `mvn clean install -DskipTests` passes locally
6. **Tests**: All new features include tests (unit + integration + E2E where
   applicable)
7. **UI Screenshots**: Attach before/after images for UI changes
8. **Single Concern**: PR addresses ONE issue only (NO mixed refactoring +
   features)
9. **Constitution Compliance**: Verify adherence to all 8 core principles
10. **Review Assignment**: Request review from appropriate team members

**CI/CD Pipeline** (GitHub Actions):

- **`ci.yml`**: Maven build + JaCoCo coverage report
- **`publish-and-test.yml`**: Docker image build + integration tests
- **`frontend-qa.yml`**: Cypress E2E tests
- **`build-installer.yml`**: Offline installer packaging

All checks MUST pass before merge.

### Code Review Standards

**Reviewers MUST verify**:

- ✅ Constitution compliance (all 8 principles)
- ✅ Layered architecture respected (no DAO calls from controllers)
- ✅ FHIR resources validated if applicable
- ✅ Internationalization complete (no hardcoded strings)
- ✅ Liquibase changesets for schema changes
- ✅ Carbon Design System used exclusively for UI
- ✅ Tests included and passing
- ✅ No security vulnerabilities (SQL injection, XSS, etc.)

**Approval Required**: Minimum 1 approval from core team before merge.

### Development Environment Setup

**Prerequisites**:

- Docker + Docker Compose
- Java 21 (OpenJDK)
- Maven 3.8+
- Node.js 16+ (for frontend)

**Quick Start** (from [README.md](README.md)):

```bash
# Clone + submodules
git clone https://github.com/DIGI-UW/OpenELIS-Global-2.git
cd OpenELIS-Global-2
git submodule update --init --recursive

# Build DataExport submodule
cd dataexport && mvn clean install -DskipTests && cd ..

# Build OpenELIS WAR
mvn clean install -DskipTests

# Start development containers
docker compose -f dev.docker-compose.yml up -d
```

**Access Points**:

- React UI: https://localhost/
- Legacy UI: https://localhost/api/OpenELIS-Global/
- FHIR Server: https://fhir.openelis.org:8443/fhir/

**Hot Reload**:

- Frontend: Changes in `frontend/src/` auto-reload (Webpack HMR)
- Backend: Rebuild WAR (`mvn clean install -DskipTests`) + recreate container:
  ```bash
  docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
  ```

**Reference**: [dev_setup.md](docs/dev_setup.md)

---

## Governance

### Constitution Authority

**PRECEDENCE**: This constitution supersedes all other documentation, coding
guidelines, and legacy patterns. In case of conflict, constitution wins.

**Scope**: Applies to all code merged to `develop` branch after ratification
date (2025-10-30). Legacy code exempt until refactored.

### Amendment Process

**Proposing Amendments**:

1. Create GitHub issue with `constitution-amendment` label
2. Document principle change rationale, impact analysis, migration plan
3. Architecture review team discusses in weekly sync
4. Approval requires consensus (no blocking objections)

**Versioning** (Semantic Versioning):

- **MAJOR** (X.0.0): Backward incompatible governance/principle removal or
  redefinition
- **MINOR** (0.X.0): New principle/section added or materially expanded guidance
- **PATCH** (0.0.X): Clarifications, wording, typos, non-semantic refinements

**Change Log**: Maintain `SYNC IMPACT REPORT` HTML comment at top of this file
documenting:

- Version change (old → new)
- Modified/added/removed principles
- Templates requiring updates
- Follow-up TODOs

### Compliance Verification

**PR Review Checklist**: Reviewers MUST confirm:

- [ ] Layered architecture respected (Principle IV)
- [ ] Carbon Design System used exclusively (Principle II)
- [ ] FHIR compliance for external data (Principle III)
- [ ] Configuration-driven variation (Principle I)
- [ ] Internationalization complete (Principle VII)
- [ ] Tests included (Principle V)
- [ ] Liquibase for schema changes (Principle VI)
- [ ] Security/compliance requirements met (Principle VIII)

**Quarterly Audits**: Architecture team samples merged PRs to verify
constitution adherence. Violations trigger corrective actions (documentation
updates, training, refactoring).

**Non-Compliance Handling**:

- **Minor violations** (e.g., missed translation): Fix in follow-up PR, document
  lesson learned
- **Major violations** (e.g., native SQL in production code): Revert PR,
  architectural review required

### Developer Guidance

**Onboarding**: New contributors MUST read:

1. This constitution (`.specify/memory/constitution.md`)
2. [README.md](README.md) - Project overview + setup
3. [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution process
4. [PULL_REQUEST_TIPS.md](PULL_REQUEST_TIPS.md) - PR guidelines
5. [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) - Community standards
6. [Carbon Design Guide](https://uwdigi.atlassian.net/wiki/spaces/OG/pages/621346838) -
   UI patterns

**Runtime Guidance**: For implementation patterns, see:

- Backend: `src/main/java/org/openelisglobal/{module}/` existing code examples
- Frontend: `frontend/src/components/` Carbon component usage
- FHIR: `org.openelisglobal.fhir.FhirTransformServiceImpl` transform examples

**Questions/Clarifications**: Post in GitHub Discussions or weekly developer
sync.

---

**Version**: 1.7.0 | **Ratified**: 2025-10-30 | **Last Amended**: 2025-11-07

<!--
  Ratification Signatories: OpenELIS Global Core Team
  Amendment v1.7.0: Enhanced Cypress E2E testing workflow and review requirements (2025-11-07)
  Amendment v1.6.0: Cypress E2E testing best practices (2025-11-05)
  Amendment v1.5.0: Explicit prohibition of @Transactional in controllers (2025-11-05)
  Amendment v1.4.0: Service layer data compilation requirement (2025-11-04)
  Amendment v1.3.0: Annotation-based Hibernate mappings + pre-commit formatting enforcement (2025-11-03)
  Amendment v1.2.0: ORM validation test requirement (2025-10-31)
  Amendment v1.1.0: Technical stack clarifications (Java 21, JUnit 4, Jakarta EE 9)
  Next Review: 2026-01-30 (Quarterly)
-->
