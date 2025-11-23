# AGENTS.md - README for AI Coding Agents

> **Purpose:** This file provides comprehensive project context for ALL AI
> coding agents (Claude, Cursor, Copilot, Jules, Aider, etc.). It contains
> everything an AI agent needs to know to work effectively on OpenELIS Global 2.

> **For Humans:** See [README.md](README.md) for project overview and
> [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

---

## Project Identity

**What is OpenELIS Global 2?**

OpenELIS Global is an open-source Laboratory Information Management System
(LIMS) designed for public health laboratories in resource-limited settings. It
manages the complete laboratory workflow from sample collection to result
reporting, serving 30+ countries worldwide.

**Key Characteristics:**

- **Healthcare Mission-Critical**: Used for HIV/AIDS testing, TB diagnostics,
  malaria surveillance, COVID-19 testing
- **Regulatory Compliance**: Meets SLIPTA (Stepwise Laboratory Quality
  Improvement Process Towards Accreditation) and ISO 15189 standards
- **Multilingual**: Supports en, fr, ar, es, hi, pt, sw (English, French,
  Arabic, Spanish, Hindi, Portuguese, Swahili)
- **Interoperability**: FHIR R4 + IHE standards for integration with national
  health information exchanges
- **Specification-Driven Development**: Uses GitHub SpecKit for rigorous feature
  development workflow

**Governance:**

- **Constitution Authority**: `.specify/memory/constitution.md` (v1.7.0) is the
  authoritative governance document
- **All code changes MUST comply with constitutional principles**
- **Constitution supersedes all other documentation in case of conflict**

**Repository:**

- GitHub: `DIGI-UW/OpenELIS-Global-2`
- Branch strategy: `develop` (main development), `main` (production releases)
- Feature branches: `{###-feature-name}` or `issue-{###}-{feature-name}`

**Tech Stack:** Java 21 + Spring Boot 3.x backend, React 17 + Carbon Design
System frontend, PostgreSQL 14+ database, HAPI FHIR R4 for interoperability

**Architecture:** Strict 5-layer pattern (Valueholder → DAO → Service →
Controller → Form)

**Development Methodology:** Test-Driven Development (TDD) with SpecKit workflow

---

## Critical Prerequisites

### Java Version (MANDATORY)

**CRITICAL:** This project REQUIRES Java 21 LTS. Build WILL FAIL with Java 8,
11, or 17.

```bash
# Verify Java version
java -version  # Must show "openjdk version 21.x.x"

# Use SDKMAN for automatic version switching (recommended)
sdk env  # Automatically switches to Java 21 based on .sdkmanrc

# Manual install (if needed)
sdk install java 21.0.1-tem
sdk use java 21.0.1-tem
```

**Why Java 21?**

- Maven compiler plugin requires Java 21 for `--release 21` flag
- Spring Boot 3.x requires Java 17+ (we use 21 for LTS)
- Jakarta EE 9 APIs require Java 17+

### Test Skipping (CRITICAL)

**IMPORTANT:** Use BOTH flags to properly skip tests during development builds:

```bash
# CORRECT (skips all tests including Surefire and Failsafe)
mvn clean install -DskipTests -Dmaven.test.skip=true

# WRONG (only skips Surefire tests, Failsafe integration tests still run)
mvn clean install -DskipTests
```

**Why both flags?**

- `-DskipTests`: Skips Surefire unit test execution
- `-Dmaven.test.skip=true`: Skips test compilation AND execution (including
  Failsafe integration tests)

### Other Prerequisites

- **Docker + Docker Compose**: For container orchestration
- **PostgreSQL 14+**: Database (runs in Docker)
- **Maven 3.8+**: Build system
- **Node.js 16+**: Frontend development
- **Git with submodules**: `git submodule update --init --recursive`

---

## Technology Stack

### Backend (Java) - NON-NEGOTIABLE

**Core Framework:**

- **Java 21 LTS** (OpenJDK/Temurin) - MANDATORY
- **Spring Boot 3.x** (Spring Framework 6.2.2)
- **Hibernate 6.x** (Hibernate ORM 5.6.15.Final)
- **Jakarta EE 9** (NOT javax._ - use jakarta.persistence._)
- **PostgreSQL 14+** (production database)
- **Liquibase 4.8.0** (schema migrations)
- **HAPI FHIR R4** (version 6.6.2)
- **Maven 3.8+** (build system)
- **Tomcat 10** (Jakarta EE 9 compatible)

**Testing Framework:**

- **JUnit 4** (4.13.1) - NOT JUnit 5
  - Use `import org.junit.Test;` (NOT `org.junit.jupiter.api.Test`)
  - Use `org.junit.Assert.*` (NOT `org.junit.jupiter.api.Assertions.*`)
  - Assertion order: `assertEquals(expected, actual)` for JUnit 4
- **Mockito 2.21.0** for mocking
- **Spring Test**: `@RunWith(SpringRunner.class)` for integration tests

**Code Quality:**

- **Spotless** formatter: `mvn spotless:apply` (MUST run before commit)
- **Formatter config**: `tools/OpenELIS_java_formatter.xml`

### Frontend (React) - NON-NEGOTIABLE

**Core Framework:**

- **React 17** (react-scripts 5.0.1)
- **Carbon Design System v1.15** (@carbon/react v1.15.0) - OFFICIAL UI FRAMEWORK
- **Carbon Icons** (@carbon/icons-react v11.17.0)
- **Carbon Charts** (@carbon/charts-react v1.5.2)

**State & Data:**

- **SWR 2.0.3** (data fetching + caching)
- **React Router DOM 5.2.0** (routing)

**Forms & Validation:**

- **Formik 2.2.9** (form management)
- **Yup 0.29.2** (validation schemas)

**Internationalization (MANDATORY):**

- **React Intl 5.20.12** - ALL user-facing strings MUST use this
- Message files: `frontend/src/languages/{locale}.json`
- Usage: `intl.formatMessage({ id: 'key' })`

**Styling:**

- **Sass 1.54.3** (Carbon token overrides only)
- NO custom CSS frameworks (NO Bootstrap, NO Tailwind)

**Testing:**

- **Cypress 12.17.3** (E2E tests)
- **Jest + React Testing Library** (unit tests)

**Code Quality:**

- **Prettier 3.4.2**: `npm run format` (MUST run before commit)
- **ESLint 8.48.0** (linting)

### FHIR Integration

- **HAPI FHIR R4 Server** (co-habitant at
  `https://fhir.openelis.org:8443/fhir/`)
- **IHE mCSD Profile** (Mobile Care Services Discovery) for
  Location/Organization
- **IHE Lab Profiles** for DiagnosticReport, Observation, Specimen,
  ServiceRequest
- **Consolidated Server** with SHR (Shared Health Record) + IPS (International
  Patient Summary)
- **OpenMRS 3.x Integration** via Lab on FHIR module

### Deployment

- **Docker + Docker Compose** (multi-container orchestration)
- **PostgreSQL** container
- **HAPI FHIR** server container (Bitnami Tomcat image)
- **Nginx** reverse proxy (SSL termination, load balancing)
- **Ubuntu 20.04+** host OS

### Prohibited Technologies

❌ **NO Custom CSS Frameworks** (Tailwind, Bootstrap) - Use Carbon Design System
only ❌ **NO Direct SQL** (JDBC, JdbcTemplate) - Use JPA/Hibernate only ❌ **NO
Native DDL/DML** - Use Liquibase for all schema changes ❌ **NO Hardcoded
Strings** - Use React Intl for all user-facing text ❌ **NO Class-Level
Variables in Controllers** - Thread safety violation ❌ **NO JUnit 5** - Use
JUnit 4 (existing codebase standard) ❌ **NO javax.persistence** - Use
jakarta.persistence (Jakarta EE 9)

---

## Constitution Principles Summary

> **Full Document:** `.specify/memory/constitution.md` (1147 lines, v1.7.0)

The constitution defines 8 core principles that ALL code changes MUST follow:

### I. Configuration-Driven Variation

**Rule:** Country-specific customizations MUST be implemented via configuration,
NOT code branching.

**Why:** OpenELIS serves 30+ countries. Code fragmentation creates
unmaintainable technical debt.

**How:**

- Use database-driven configuration (`SystemConfiguration`,
  `LocalizationConfiguration`)
- Validation patterns via properties files
- NO country-specific code branches or forks

**Example:** Accession number format "YYYY-NNNNN" vs "LAB-YYYY-MM-NNNNN"
configured via `common.properties`

### II. Carbon Design System First

**Rule:** All new UI components MUST use Carbon Design System exclusively.

**Why:** Ensures UI/UX consistency, accessibility (WCAG 2.1 AA), and alignment
with modern design systems.

**How:**

- Use `@carbon/react` v1.15+ components exclusively
- Styling via Carbon tokens (`$spacing-*`, `$text-*`, `$layer-*`)
- Typography: IBM Plex Sans (Carbon default)
- Layout: Carbon Grid + Column system
- Icons: `@carbon/icons-react` v11.17+
- Customize via Carbon theme tokens, NOT custom CSS

**Prohibited:** NO Bootstrap, NO Tailwind, NO custom CSS frameworks

**Reference:**
[OpenELIS Carbon Design Guide](https://uwdigi.atlassian.net/wiki/spaces/OG/pages/621346838)

### III. FHIR/IHE Standards Compliance

**Rule:** All healthcare data interoperability MUST use HL7 FHIR R4 + IHE
profiles.

**Why:** National health information exchanges require standards-based
interoperability.

**How:**

- HAPI FHIR R4 (v6.6.2) for local FHIR store
- IHE mCSD for Location/Organization resources
- IHE Lab profiles for clinical resources
- All entities with external exposure MUST have `fhir_uuid UUID` column
- Use `FhirPersistanceService` for CRUD, `FhirTransformService` for entity ↔
  FHIR conversion
- Sync to consolidated server on insert/update operations

**Prohibited:** NO proprietary APIs for external integration

### IV. Layered Architecture Pattern

**Rule:** All backend features MUST follow strict 5-layer structure.

**Layers:**

1. **Valueholders** (JPA Entities): `org.openelisglobal.{module}.valueholder`

   - Extend `BaseObject<String>`
   - Include `fhir_uuid UUID` for FHIR-mapped entities
   - Use JPA/Hibernate annotations (NOT XML mappings)
   - ID generation via `@GenericGenerator`

2. **DAOs** (Data Access): `org.openelisglobal.{module}.dao`

   - Interface + Implementation extends `BaseDAOImpl<Entity, String>`
   - Annotate with `@Component` + `@Transactional`
   - Use HQL (Hibernate Query Language) ONLY - NO native SQL

3. **Services** (Business Logic): `org.openelisglobal.{module}.service`

   - Interface + Implementation with `@Service` + `@Transactional`
   - **Transactions start here (NOT in controllers)**
   - **CRITICAL - Data Compilation Rule:** Services MUST eagerly fetch ALL data
     needed for response within the transaction using `JOIN FETCH`
   - Controllers MUST NOT traverse entity relationships (prevents
     LazyInitializationException)
   - Validation logic before persistence
   - Call DAOs for persistence, FHIR services for sync

4. **Controllers** (REST Endpoints): `org.openelisglobal.{module}.controller`

   - Extend `BaseRestController`
   - Annotate with `@RestController` + `@RequestMapping("/rest/{module}")`
   - **Controllers are singletons** - NO class-level variables
   - **PROHIBITED:** NO `@Transactional` annotations (belongs in service layer)
   - Handle HTTP request/response mapping only, delegate to services

5. **Forms/DTOs**: `org.openelisglobal.{module}.form`
   - Simple beans for client ↔ server communication
   - Validation annotations

**Anti-Patterns:**

- ❌ Controllers calling DAOs directly
- ❌ Business logic in DAOs
- ❌ Native SQL in Java code
- ❌ Class-level variables in controllers
- ❌ Controllers accessing entity relationships (e.g.,
  `position.getParentRack().getParentShelf()`)
- ❌ `@Transactional` annotations in controllers

### V. Test-Driven Development

**Rule:** New features MUST include automated tests. TDD workflow ENCOURAGED for
complex logic.

**Test Pyramid:**

1. **Unit Tests** (JUnit 4 + Mockito) - Business logic validation
2. **ORM Validation Tests** - Framework configuration validation (<5s, no
   database)
3. **Integration Tests** - Full stack with database
4. **E2E Tests** (Cypress) - User workflow validation

**Coverage Goal:** >70% for new code (JaCoCo)

**Section V.4: ORM Validation Tests**

- MUST include test that builds `SessionFactory` or `EntityManagerFactory`
- Validates all entity mappings load without errors
- Verifies no JavaBean getter/setter conflicts
- Verifies property name consistency
- Executes in <5 seconds without database

**Section V.5: Cypress E2E Testing Best Practices**

- **Test Execution:** Run tests INDIVIDUALLY during development (not full suite)
  - Maximum 5-10 test cases per execution during development
  - Full suite runs only in CI/CD pipeline or pre-merge validation
- **Configuration:** Video disabled by default, screenshots enabled for failures
- **Browser Console Logging:** MUST be enabled and reviewed after each test run
- **Post-Run Review:** Mandatory checklist (console logs, screenshots, test
  output)
- **Test Organization:** Map directly to user stories, avoid test bloat
- **Performance Target:** Individual test <30s, full suite <5 minutes

**Anti-Patterns:**

- ❌ Video recording enabled by default
- ❌ Arbitrary time delays (use Cypress retry-ability)
- ❌ Missing element readiness checks
- ❌ Not reviewing browser console logs after test runs
- ❌ Running full E2E suite during development

### VI. Database Schema Management

**Rule:** All database changes MUST go through Liquibase. NO direct DDL/DML in
production.

**How:**

- Schema migrations in `src/main/resources/liquibase/{module}/`
- Changesets with unique IDs: `{module}-{sequence}-{description}`
- Use Liquibase XML format (NOT raw SQL unless necessary)
- Rollback scripts MUST be provided for structural changes
- Test migrations on empty database AND production-like data volume

**Prohibited:** NO `ALTER TABLE` or `CREATE TABLE` via psql/pgAdmin in deployed
environments

### VII. Internationalization First

**Rule:** All user-facing strings MUST be externalized via React Intl. NO
hardcoded English text.

**How:**

- Message files: `frontend/src/languages/{locale}.json`
- Use `intl.formatMessage({ id: 'storage.location.label' })`
- Supported locales: en, fr, ar, es, hi, pt, sw
- New features MUST provide translations for at least en + fr
- Date/time formatting via `intl.formatDate()`, `intl.formatTime()`
- Number formatting via `intl.formatNumber()`

**Example:**

```javascript
// ❌ BAD
<Button>Save Location</Button>

// ✅ GOOD
<Button>{intl.formatMessage({ id: 'button.save.location' })}</Button>
```

### VIII. Security & Compliance

**Rule:** OpenELIS MUST meet SLIPTA and ISO 15189 requirements.

**Requirements:**

- Authentication: Spring Security 6.0.4
- Authorization: Role-based access control (RBAC)
- Audit Trail: All data changes logged with user ID + timestamp
- Data Privacy: PHI access logged, GDPR-ready deletion workflows
- Secure Transport: HTTPS enforced
- Password Policy: Configurable complexity requirements
- Input Validation: Hibernate Validator + Formik validation

**Compliance Checklist:**

- [ ] Role-based access control implemented
- [ ] Audit trail captures user actions
- [ ] Input validated against injection attacks (SQL, XSS)
- [ ] Sensitive data encrypted at rest (if applicable)
- [ ] HTTPS endpoints only (NO HTTP for PHI)

---

## Development Workflow

### Initial Setup

```bash
# Clone repository with submodules
git clone https://github.com/DIGI-UW/OpenELIS-Global-2.git
cd OpenELIS-Global-2
git submodule update --init --recursive

# Verify Java version
java -version  # Must be Java 21
# OR use SDKMAN
sdk env  # Automatically switches to Java 21

# Build DataExport submodule
cd dataexport
mvn clean install -DskipTests -Dmaven.test.skip=true
cd ..

# Build OpenELIS WAR
mvn clean install -DskipTests -Dmaven.test.skip=true

# Start development containers
docker compose -f dev.docker-compose.yml up -d
```

**Access Points:**

- React UI: https://localhost/
- Legacy UI: https://localhost/api/OpenELIS-Global/
- FHIR Server: https://fhir.openelis.org:8443/fhir/

### SpecKit Workflow (Specification-Driven Development)

This project uses GitHub SpecKit for rigorous feature development. The workflow
enforces constitution compliance at every stage.

**Available Commands:**

- `/speckit.specify` - Create/update feature specification from description
- `/speckit.clarify` - Identify underspecified areas (max 5 clarification
  questions)
- `/speckit.plan` - Generate implementation plan with constitution check and
  research
- `/speckit.tasks` - Generate actionable, dependency-ordered tasks.md
- `/speckit.implement` - Execute implementation plan (process tasks.md)
- `/speckit.analyze` - Cross-artifact consistency analysis
- `/speckit.constitution` - Create/update project constitution
- `/speckit.checklist` - Generate custom quality validation checklist

**Standard Workflow:**

1. **Specify:** `/speckit.specify "Feature description"` → Creates
   `specs/{###-feature-name}/spec.md`
2. **Clarify:** `/speckit.clarify` → Resolves ambiguities (max 3 rounds
   recommended)
3. **Plan:** `/speckit.plan` → Creates `plan.md` with architecture, research,
   constitution check
4. **Tasks:** `/speckit.tasks` → Creates `tasks.md` with dependency-ordered task
   breakdown
5. **Implement:** `/speckit.implement` → Executes tasks using TDD workflow
6. **Analyze:** `/speckit.analyze` → Validates consistency across
   spec/plan/tasks

**Feature Structure:**

```
specs/{###-feature-name}/
├── spec.md              # Feature specification (user stories, acceptance criteria)
├── plan.md              # Implementation plan (architecture, research, constitution check)
├── tasks.md             # Actionable tasks (dependency-ordered, TDD)
├── quickstart.md        # Step-by-step developer guide
├── data-model.md        # Entity relationship documentation
├── research.md          # Background research and analysis
├── checklists/          # Quality validation checklists
└── contracts/           # API contracts, FHIR mappings
```

### Common Development Commands

**Backend:**

```bash
# Build (skip tests for fast iteration)
mvn clean install -DskipTests -Dmaven.test.skip=true

# Run tests (when ready)
mvn test

# Format code (MUST run before commit)
mvn spotless:apply

# Check formatting
mvn spotless:check

# Hot reload (after code changes)
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org
```

**Frontend:**

```bash
cd frontend

# Install dependencies
npm install

# Start development server (with hot reload)
npm start

# Format code (MUST run before commit)
npm run format

# Run unit tests
npm test

# Run E2E tests (individual file during development)
npm run cy:run -- --spec "cypress/e2e/{feature}.cy.js"

# Run full E2E suite (CI/CD only, not during development)
npm run cy:run
```

**Docker:**

```bash
# Start development environment
docker compose -f dev.docker-compose.yml up -d

# Stop all containers
docker compose -f dev.docker-compose.yml down

# Rebuild specific container (after code changes)
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org

# View logs
docker compose -f dev.docker-compose.yml logs -f oe.openelis.org
```

### Branch Strategy

- **`develop`** - Main development branch (ALL PRs target this)
- **`main`** - Production releases only (reviewers backport from develop)
- **Feature branches:** `{###-feature-name}` (e.g., `001-sample-storage`) or
  `issue-{###}-{feature-name}`
- **Hotfix branches:** `hotfix-{description}` (merged to develop + main)

**Creating Feature Branch:**

```bash
git checkout develop
git pull --rebase upstream develop
git checkout -b 001-new-feature
```

### Pre-Commit Checklist

**MANDATORY before EVERY commit:**

```bash
# 1. Format code (BOTH commands required)
mvn spotless:apply
cd frontend && npm run format && cd ..

# 2. Verify build passes
mvn clean install -DskipTests -Dmaven.test.skip=true

# 3. Run relevant tests
mvn test  # Unit + integration tests
npm run cy:run -- --spec "cypress/e2e/{feature}.cy.js"  # Individual E2E test

# 4. Verify constitution compliance
# Check .specify/memory/constitution.md for relevant principles
```

**Before Creating PR:**

- [ ] All tests pass
- [ ] Code formatted (spotless + prettier)
- [ ] No hardcoded strings (React Intl used)
- [ ] Constitution compliance verified
- [ ] Liquibase changesets for schema changes
- [ ] FHIR resources validated (if applicable)
- [ ] UI screenshots attached (for UI changes)

---

## Architecture Overview

### 5-Layer Pattern (MANDATORY)

```
┌─────────────────────────────────────────────────────────┐
│                     REST Client                          │
└────────────────────┬────────────────────────────────────┘
                     │ HTTP
┌────────────────────▼────────────────────────────────────┐
│  CONTROLLER (Request/Response Mapping)                   │
│  - @RestController                                       │
│  - @GetMapping, @PostMapping, etc.                       │
│  - Form validation                                       │
│  - NO business logic                                     │
│  - NO @Transactional (belongs in service layer)         │
│  - NO entity relationship traversal                      │
└────────────────────┬────────────────────────────────────┘
                     │ Delegate
┌────────────────────▼────────────────────────────────────┐
│  SERVICE (Business Logic + Transaction Management)       │
│  - @Service                                              │
│  - @Transactional (transactions START here)             │
│  - Data compilation (eagerly fetch ALL data)            │
│  - Validation logic                                      │
│  - Call DAOs for persistence                             │
│  - Call FHIR services for sync                           │
└────────────────────┬────────────────────────────────────┘
                     │ Persistence
┌────────────────────▼────────────────────────────────────┐
│  DAO (Data Access)                                       │
│  - @Component + @Transactional                           │
│  - Extends BaseDAOImpl<Entity, String>                   │
│  - HQL queries ONLY (NO native SQL)                      │
│  - CRUD methods: get, insert, update, delete             │
└────────────────────┬────────────────────────────────────┘
                     │ ORM
┌────────────────────▼────────────────────────────────────┐
│  VALUEHOLDER (JPA Entity)                                │
│  - @Entity, @Table                                       │
│  - Extends BaseObject<String>                            │
│  - JPA annotations (NOT XML mappings)                    │
│  - fhir_uuid UUID column (for FHIR sync)                 │
└────────────────────┬────────────────────────────────────┘
                     │ SQL
┌────────────────────▼────────────────────────────────────┐
│             PostgreSQL Database                          │
└─────────────────────────────────────────────────────────┘
```

### Transaction Boundary Management

**CRITICAL RULES:**

1. **Transactions start in service layer ONLY**

   - Services annotated with `@Transactional`
   - Controllers MUST NOT have `@Transactional` (architectural violation)

2. **Data Compilation in Services**
   - Services MUST eagerly fetch ALL data needed for response using `JOIN FETCH`
   - Controllers MUST NOT traverse entity relationships
   - **Why:** Prevents `LazyInitializationException` when transaction closes

**Example Anti-Pattern (WRONG):**

```java
// Controller (WRONG - traversing relationships outside transaction)
@GetMapping("/sample/{id}")
public ResponseEntity<?> getSample(@PathVariable String id) {
    Sample sample = sampleService.getSampleById(id);
    // ❌ WRONG: Transaction closed, lazy loading will fail
    String locationName = sample.getStorageLocation().getParentRack().getName();
    return ResponseEntity.ok(locationName);
}
```

**Correct Pattern:**

```java
// Service (CORRECT - eagerly fetch all data within transaction)
@Service
@Transactional
public class SampleServiceImpl {
    public Map<String, Object> getSampleWithLocation(String id) {
        String hql = "SELECT s FROM Sample s " +
                     "LEFT JOIN FETCH s.storageLocation loc " +
                     "LEFT JOIN FETCH loc.parentRack rack " +
                     "WHERE s.id = :id";
        Sample sample = query.setParameter("id", id).getSingleResult();

        // Compile all data within transaction
        Map<String, Object> result = new HashMap<>();
        result.put("sampleId", sample.getId());
        result.put("locationName", sample.getStorageLocation().getParentRack().getName());
        return result;  // Return complete data structure
    }
}

// Controller (CORRECT - receives complete data)
@GetMapping("/sample/{id}")
public ResponseEntity<?> getSample(@PathVariable String id) {
    Map<String, Object> result = sampleService.getSampleWithLocation(id);
    return ResponseEntity.ok(result);
}
```

### FHIR Synchronization Pattern

All entities with external exposure MUST:

1. Include `fhir_uuid UUID` column
2. Implement bidirectional transform (Entity ↔ FHIR Resource)
3. Sync to consolidated FHIR server on insert/update

**Pattern:**

```java
@Service
@Transactional
public class SampleServiceImpl {
    @Autowired
    private FhirPersistanceService fhirService;

    @Autowired
    private FhirTransformService transformService;

    public void saveSample(Sample sample) {
        // 1. Save to local database
        sampleDAO.save(sample);

        // 2. Transform to FHIR Specimen resource
        Specimen specimen = transformService.transformToFhir(sample);

        // 3. Sync to consolidated FHIR server
        fhirService.createUpdateFhirResource(specimen);
    }
}
```

---

## Testing Strategy

**Reference**: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)

The Testing Roadmap is the authoritative source for all testing practices,
patterns, and procedures. This section provides a high-level overview. For
detailed guidance, see the Testing Roadmap.

**Key Resources**:

- **Testing Roadmap**: `.specify/guides/testing-roadmap.md` - Comprehensive
  testing guide for both agents and humans
- **Test Templates**: `.specify/templates/testing/` - Standardized test
  templates for all test types
- **Constitution**: `.specify/memory/constitution.md` (Principle V) - High-level
  testing requirements

### Test-Driven Development (TDD) Workflow

**MANDATORY for complex features. ENCOURAGED for all features.**

**Red-Green-Refactor Cycle:**

1. **Red:** Write failing test first (defines expected behavior)
2. **Green:** Write minimal code to make test pass
3. **Refactor:** Improve code quality while keeping tests green

**Benefits:**

- Catches bugs early
- Enforces clear requirements
- Enables confident refactoring
- Serves as living documentation

### Test Pyramid

```
        ┌─────────────┐
        │   E2E (5%)   │  Cypress - User workflows
        │   Slow       │
        └──────┬───────┘
       ┌───────▼────────┐
       │ Integration    │  Spring Test - Full stack
       │   (15%)        │
       │   Medium       │
       └───────┬────────┘
    ┌──────────▼───────────┐
    │  ORM Validation (5%) │  Hibernate SessionFactory build
    │    Fast              │
    └──────────┬───────────┘
 ┌─────────────▼──────────────┐
 │    Unit Tests (75%)         │  JUnit 4 + Mockito - Business logic
 │    Very Fast                │
 └─────────────────────────────┘
```

### Backend Testing

**For Comprehensive Guidance**: See
[Testing Roadmap - Backend Testing](.specify/guides/testing-roadmap.md#backend-testing)
for detailed patterns, code examples, and best practices.

**For Quick Reference**: See
[Backend Testing Best Practices Guide](.specify/guides/backend-testing-best-practices.md)
for common patterns and cheat sheets.

**TDD Workflow (MANDATORY for complex logic):**

- **Red**: Write failing test first (defines expected behavior)
- **Green**: Write minimal code to make test pass
- **Refactor**: Improve code quality while keeping tests green

**SDD Checkpoint Requirements:**

- **After Phase 1 (Entities)**: ORM validation tests MUST pass
- **After Phase 2 (Services)**: Unit tests MUST pass
- **After Phase 3 (Controllers)**: Integration tests MUST pass
- **Coverage Goal**: >80% (measured via JaCoCo)

### Test Slicing Strategy

**CRITICAL**: Use focused test slices when possible for faster execution.

**Decision Tree**:

1. **Testing REST controller HTTP layer only?** → Use `@WebMvcTest` ✅
2. **Testing DAO/repository persistence layer only?** → Use `@DataJpaTest` ✅
3. **Testing complete workflow with full application context?** → Use
   `@SpringBootTest` ✅
4. **Legacy integration tests with Testcontainers/DBUnit?** → Use
   `BaseWebContextSensitiveTest` ⚠️

**When to Use Each**:

| Test Type          | Annotation                    | Use Case               | Speed  | Context        |
| ------------------ | ----------------------------- | ---------------------- | ------ | -------------- |
| Controller         | `@WebMvcTest`                 | HTTP layer only        | Fast   | Web layer only |
| DAO                | `@DataJpaTest`                | Persistence layer only | Fast   | JPA layer only |
| Integration        | `@SpringBootTest`             | Full workflow          | Medium | Full context   |
| Legacy Integration | `BaseWebContextSensitiveTest` | Testcontainers/DBUnit  | Slow   | Full context   |

**Reference**:
[Testing Roadmap - Test Slicing Strategy Decision Tree](.specify/guides/testing-roadmap.md#test-slicing-strategy-decision-tree)

### Unit Tests (JUnit 4 + Mockito)

**Location:** `src/test/java/org/openelisglobal/{module}/service/`

**Pattern:**

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SampleServiceTest {
    @Mock  // ✅ Use @Mock for isolated unit tests (NOT @MockBean)
    private SampleDAO sampleDAO;

    @InjectMocks
    private SampleServiceImpl sampleService;

    @Test
    public void testGetSampleById_ReturnsCorrectSample() {
        // Arrange
        Sample expected = SampleBuilder.create()
            .withId("123")
            .build();
        when(sampleDAO.get("123")).thenReturn(expected);

        // Act
        Sample actual = sampleService.getSampleById("123");

        // Assert
        assertNotNull("Sample should not be null", actual);
        assertEquals("Sample ID should match", "123", actual.getId());
    }
}
```

**Key Points:**

- Use JUnit 4 imports (`org.junit.Test`, NOT `org.junit.jupiter.api.Test`)
- Use `@Mock` for isolated unit tests (NOT `@MockBean` - that's for Spring
  context tests)
- Assertion order: `assertEquals(expected, actual)`
- Mock DAO layer, test service logic only
- Use builders/factories for test data (NOT hardcoded values)
- Test business logic only (mock dependencies)

**Template:** `.specify/templates/testing/JUnit4ServiceTest.java.template`

### Controller Tests (@WebMvcTest)

**Location:** `src/test/java/org/openelisglobal/{module}/controller/`

**Use for**: Testing REST controllers in isolation with mocked services.

**Pattern:**

```java
@RunWith(SpringRunner.class)
@WebMvcTest(StorageLocationRestController.class)
public class StorageLocationRestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean  // ✅ Use @MockBean for Spring context tests (NOT @Mock)
    private StorageLocationService storageLocationService;

    @Test
    public void testGetLocation_WithValidId_ReturnsLocation() throws Exception {
        StorageRoom room = StorageRoomBuilder.create()
            .withId("ROOM-001")
            .withName("Main Laboratory")
            .build();
        when(storageLocationService.getLocationById("ROOM-001")).thenReturn(room);

        mockMvc.perform(get("/rest/storage/rooms/ROOM-001")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("ROOM-001"));
    }
}
```

**Key Points:**

- Use `@MockBean` (NOT `@Mock`) for Spring context mocking
- Use `MockMvc` for HTTP request/response testing
- Use JSONPath for response assertions
- Mock service layer, test HTTP layer only

**Template:** `.specify/templates/testing/WebMvcTestController.java.template`

### DAO Tests (@DataJpaTest)

**Location:** `src/test/java/org/openelisglobal/{module}/dao/`

**Use for**: Testing persistence layer in isolation.

**Pattern:**

```java
@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StorageLocationDAOTest {
    @Autowired
    private TestEntityManager entityManager;  // ✅ Use TestEntityManager (NOT JdbcTemplate)

    @Autowired
    private StorageLocationDAO storageLocationDAO;

    @Test
    public void testFindByParentId_WithValidParent_ReturnsChildLocations() {
        StorageRoom room = StorageRoomBuilder.create()
            .withId("ROOM-001")
            .withName("Main Lab")
            .build();
        entityManager.persist(room);

        StorageDevice device = StorageDeviceBuilder.create()
            .withId("DEV-001")
            .withName("Freezer 1")
            .withParentRoom(room)
            .build();
        entityManager.persist(device);
        entityManager.flush();

        List<StorageDevice> devices = storageLocationDAO.findByParentId("ROOM-001");

        assertEquals("Should return one device", 1, devices.size());
        assertEquals("Device ID should match", "DEV-001", devices.get(0).getId());
    }
}
```

**Key Points:**

- Use `TestEntityManager` for test data (NOT JdbcTemplate)
- Automatic transaction rollback (no manual cleanup needed)
- Test HQL queries, CRUD operations, relationships

**Template:** `.specify/templates/testing/DataJpaTestDao.java.template`

### Frontend Unit Tests (Jest + React Testing Library)

**Location:** `frontend/src/components/{feature}/*.test.jsx` or
`frontend/src/components/{feature}/__tests__/*.test.jsx`

**For Comprehensive Guidance**: See
[Testing Roadmap - Jest + React Testing Library](.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests)
for detailed patterns, code examples, and best practices.

**For Quick Reference**: See
[Jest Best Practices Guide](.specify/guides/jest-best-practices.md) for common
patterns and cheat sheets.

**TDD Workflow (MANDATORY for complex logic):**

- **Red**: Write failing test first (defines expected behavior)
- **Green**: Write minimal code to make test pass
- **Refactor**: Improve code quality while keeping tests green

**SDD Checkpoint:** After Phase 4 (Frontend), all unit tests MUST pass  
**Coverage Goal:** >70% (measured via Jest)

**Pattern:**

```javascript
import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import ComponentName from "./ComponentName";
import messages from "../../../languages/en.json";

// Mock utilities BEFORE imports (Jest hoisting)
jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>
  );
};

describe("ComponentName", () => {
  test("testUserInteraction_ShowsExpectedResult", async () => {
    // Arrange
    renderWithIntl(<ComponentName />);

    // Act: Use userEvent for user interactions (PREFERRED)
    const input = screen.getByLabelText(/name/i);
    await userEvent.type(input, "Test Name", { delay: 0 });

    const button = screen.getByRole("button", { name: /submit/i });
    await userEvent.click(button);

    // Assert: Wait for async element (use queryBy* in waitFor)
    await waitFor(() => {
      const element = screen.queryByText("Success");
      expect(element).toBeInTheDocument();
    });
  });
});
```

**Key Points:**

- **Import Order**: React → Testing Library → userEvent → jest-dom → Intl →
  Router → Component → Utils → Messages
- **userEvent vs fireEvent**: Prefer `userEvent` for user interactions (more
  realistic)
- **Async Testing**: Use `waitFor` with `queryBy*` (NOT `getBy*`) or `findBy*`
  for async elements
- **DON'T**: Use `setTimeout` (no retry logic - use `waitFor` instead)
- **Carbon Components**: Use `userEvent`, `waitFor` for portals, `within()` for
  scoped queries
- **Test Behavior**: Test user-visible behavior, NOT implementation details
- **Edge Cases**: Test null, empty, boundary values

**Anti-Patterns:**

- ❌ Using `setTimeout` for async operations (use `waitFor` instead)
- ❌ Using `getBy*` in `waitFor` (use `queryBy*` instead)
- ❌ Using `fireEvent` when `userEvent` works (prefer `userEvent`)
- ❌ Testing implementation details (test user-visible behavior)
- ❌ Inconsistent import order

**Template:** `.specify/templates/testing/JestComponent.test.jsx.template`

### ORM Validation Tests (Constitution V.4)

**Location:**
`src/test/java/org/openelisglobal/{module}/HibernateMappingValidationTest.java`

**Purpose:** Catch ORM configuration errors in <5 seconds without database

**Pattern:**

```java
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class HibernateMappingValidationTest {
    @Test
    public void testHibernateMappingsLoadSuccessfully() {
        Configuration config = new Configuration();
        config.addAnnotatedClass(Sample.class);
        config.addAnnotatedClass(StorageLocation.class);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        SessionFactory sf = config.buildSessionFactory();
        assertNotNull("All Hibernate mappings should load without errors", sf);
        sf.close();
    }
}
```

**What it catches:**

- Getter/setter conflicts (e.g., `getActive()` vs `isActive()`)
- Property name mismatches
- Missing annotations
- Invalid relationship mappings

### Integration Tests (@SpringBootTest)

**Location:** `src/test/java/org/openelisglobal/{module}/controller/` or
`src/test/java/org/openelisglobal/{module}/service/`

**Use for**: Testing complete workflows that require full application context.

**Pattern:**

```java
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional  // ✅ PREFERRED: Automatic rollback
public class SampleServiceIntegrationTest {
    @Autowired
    private SampleService sampleService;

    @Test
    public void testSaveSample_PersistsToDatabase() {
        Sample sample = SampleBuilder.create()
            .withAccessionNumber("2025-00001")
            .build();

        sampleService.save(sample);

        Sample retrieved = sampleService.getSampleByAccessionNumber("2025-00001");
        assertNotNull("Sample should be persisted", retrieved);
    }
}
```

### E2E Tests (Cypress)

**Location:** `frontend/cypress/e2e/{feature}.cy.js`

**For Comprehensive Guidance**: See
[Testing Roadmap - Cypress E2E Testing](.specify/guides/testing-roadmap.md#cypress-e2e-testing)
for detailed patterns, code examples, and best practices.

**For Quick Reference**: See
[Cypress Best Practices Guide](.specify/guides/cypress-best-practices.md) for
common patterns and cheat sheets.

**For Functional Requirements**: See
[Constitution Section V.5](.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices)
for E2E testing requirements.

**Execution Strategy (Constitution V.5):**

- **Development:** Run INDIVIDUAL test files (max 5-10 test cases)
- **CI/CD:** Run full suite

```bash
# Development (CORRECT - run individual test)
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"

# CI/CD only (NOT during development)
npm run cy:run
```

**Configuration (`cypress.config.js`):**

```javascript
module.exports = defineConfig({
  video: false, // MUST be disabled by default (Constitution V.5)
  screenshotOnRunFailure: true, // MUST be enabled (Constitution V.5)
  defaultCommandTimeout: 10000,
  e2e: {
    baseUrl: "https://localhost",
    testIsolation: true, // Default: true (cy.session() handles caching)
  },
  viewportWidth: 1025, // Desktop default
  viewportHeight: 900,
});
```

**Post-Run Review (MANDATORY - Constitution V.5):**

After each test execution, review:

1. **Console Logs:** Review browser console in Cypress UI for errors, failed API
   requests, warnings
2. **Screenshots:** Review failure screenshots for UI state at failure point
3. **Test Output:** Review Cypress command log for execution order and timeouts

**Pattern:**

```javascript
describe("User Story P1: Sample Storage Assignment", () => {
  before(() => {
    // Login runs ONCE per test file (cy.session() pattern)
    cy.login("admin", "password");
  });

  beforeEach(() => {
    cy.viewport(1025, 900); // Set viewport before visit
    cy.visit("/storage");
  });

  it("should assign sample to storage location via barcode scan", () => {
    // Arrange: Set up API intercept BEFORE action
    cy.intercept("POST", "/rest/storage/assign").as("assignRequest");

    // Act: User workflow (what user does)
    cy.get('[data-testid="barcode-input"]').type("SAMPLE-001{enter}");
    cy.get('[data-testid="location-input"]').type("RACK-A1{enter}");
    cy.get('[data-testid="submit-button"]')
      .should("be.visible")
      .should("not.be.disabled")
      .click();

    // Assert: Wait for API call and verify success
    cy.wait("@assignRequest").its("response.statusCode").should("eq", 200);
    cy.get('[data-testid="success-message"]').should("be.visible");
  });
});
```

**Anti-Patterns:**

- ❌ `cy.wait(5000)` - Use Cypress retry-ability instead (`.should()`)
- ❌ Not setting up intercepts before actions
- ❌ Not reviewing console logs after failures
- ❌ Running full suite during development (slow feedback)
- ❌ Not using data-testid selectors
- ❌ Ineffective DOM queries (not scoped, no viewport management)
- ❌ Recreating test data via UI (use API-based setup)
- ❌ Starting new sessions unnecessarily (use cy.session())

### Testing Resources

**Comprehensive Guides**:

- **Testing Roadmap**: `.specify/guides/testing-roadmap.md` - Comprehensive
  testing guide for all test types (backend and frontend)
- **Backend Testing Best Practices**:
  `.specify/guides/backend-testing-best-practices.md` - Quick reference for
  backend Java/Spring Boot testing patterns
- **Jest Best Practices**: `.specify/guides/jest-best-practices.md` - Quick
  reference for Jest + React Testing Library patterns
- **Cypress Best Practices**: `.specify/guides/cypress-best-practices.md` -
  Quick reference for Cypress patterns

**Templates**:

- **Test Templates**: `.specify/templates/testing/` - Standardized test
  templates for all test types
  - JUnit Service:
    `.specify/templates/testing/JUnit4ServiceTest.java.template` - Unit tests
    (JUnit 4 + Mockito)
  - WebMvc Controller:
    `.specify/templates/testing/WebMvcTestController.java.template` - Controller
    tests (@WebMvcTest)
  - DataJpa DAO: `.specify/templates/testing/DataJpaTestDao.java.template` - DAO
    tests (@DataJpaTest)
  - Jest Component:
    `.specify/templates/testing/JestComponent.test.jsx.template` - Frontend unit
    tests
  - Cypress E2E: `.specify/templates/testing/CypressE2E.cy.js.template` - E2E
    tests

**Constitution**:

- **Constitution Section V**: `.specify/memory/constitution.md` (Principle V) -
  High-level testing requirements
  - Section V.4: ORM Validation Tests - Requirements for Hibernate mapping
    validation
  - Section V.5: Cypress E2E Testing Best Practices - Functional requirements
    and mandates

---

## Common Pitfalls & Gotchas

### Java Version Mismatch

**Symptom:** Build fails with compiler errors

**Cause:** Using Java 8, 11, or 17 instead of Java 21

**Solution:**

```bash
java -version  # Must show "21.x.x"
sdk env        # Use SDKMAN for automatic switching
```

### JUnit 4 vs JUnit 5 Confusion

**Symptom:** Test compilation errors, annotations not recognized

**Cause:** Using JUnit 5 imports instead of JUnit 4

**Wrong:**

```java
import org.junit.jupiter.api.Test;  // JUnit 5
import org.junit.jupiter.api.Assertions.*;  // JUnit 5
```

**Correct:**

```java
import org.junit.Test;  // JUnit 4
import org.junit.Assert.*;  // JUnit 4
```

### Incomplete Test Skipping

**Symptom:** "Skipping tests" message shown but tests still run (Failsafe
integration tests)

**Cause:** Using only `-DskipTests` flag

**Wrong:**

```bash
mvn clean install -DskipTests
```

**Correct:**

```bash
mvn clean install -DskipTests -Dmaven.test.skip=true
```

### @Transactional in Controllers

**Symptom:** Architectural violation, transaction boundary unclear

**Cause:** Placing `@Transactional` annotation on controller methods

**Wrong:**

```java
@RestController
public class SampleController {
    @GetMapping("/sample/{id}")
    @Transactional(readOnly = true)  // ❌ WRONG
    public ResponseEntity<?> getSample(@PathVariable String id) { }
}
```

**Correct:**

```java
@Service
public class SampleServiceImpl {
    @Transactional(readOnly = true)  // ✅ CORRECT
    public Sample getSampleById(String id) { }
}
```

### LazyInitializationException

**Symptom:**
`LazyInitializationException: could not initialize proxy - no Session`

**Cause:** Controller traversing entity relationships after service transaction
closed

**Wrong:**

```java
// Controller
@GetMapping("/sample/{id}")
public ResponseEntity<?> getSample(@PathVariable String id) {
    Sample sample = sampleService.getSampleById(id);
    // ❌ Transaction closed - lazy loading fails
    String location = sample.getStorageLocation().getName();
    return ResponseEntity.ok(location);
}
```

**Correct:**

```java
// Service - eagerly fetch all data within transaction
@Transactional
public Map<String, Object> getSampleWithLocation(String id) {
    String hql = "SELECT s FROM Sample s " +
                 "LEFT JOIN FETCH s.storageLocation " +
                 "WHERE s.id = :id";
    Sample sample = query.setParameter("id", id).getSingleResult();

    Map<String, Object> result = new HashMap<>();
    result.put("sampleId", sample.getId());
    result.put("locationName", sample.getStorageLocation().getName());
    return result;  // Complete data, no lazy loading needed
}
```

### Hardcoded Strings

**Symptom:** Internationalization violation, constitution non-compliance

**Cause:** English text hardcoded in JSX

**Wrong:**

```javascript
<Button>Save</Button>
```

**Correct:**

```javascript
<Button>{intl.formatMessage({ id: "button.save" })}</Button>
```

**Translation file (`frontend/src/languages/en.json`):**

```json
{
  "button.save": "Save"
}
```

### Using Bootstrap/Tailwind Instead of Carbon

**Symptom:** Design system violation, constitution non-compliance

**Cause:** Importing Bootstrap or Tailwind CSS

**Wrong:**

```javascript
import 'bootstrap/dist/css/bootstrap.min.css';  // ❌ WRONG
<div className="container">  // Bootstrap classes
```

**Correct:**

```javascript
import { Grid, Column } from "@carbon/react"; // ✅ CORRECT
<Grid>
  <Column lg={16}>{/* Content */}</Column>
</Grid>;
```

### Running Full E2E Suite During Development

**Symptom:** Slow feedback (>15 minutes), difficult debugging

**Cause:** Running all E2E tests instead of individual test files

**Wrong:**

```bash
npm run cy:run  # Runs ALL tests (60+ test cases)
```

**Correct:**

```bash
# Run individual test file (5-10 test cases)
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"
```

### javax.persistence vs jakarta.persistence

**Symptom:** Compilation errors, annotation not found

**Cause:** Using old javax.persistence imports (pre-Jakarta EE 9)

**Wrong:**

```java
import javax.persistence.Entity;  // ❌ WRONG
```

**Correct:**

```java
import jakarta.persistence.Entity;  // ✅ CORRECT
```

---

## Pull Request Requirements

### 15-Point Mandatory Checklist

Before creating PR, verify ALL items:

1. **GitHub Issue Reference:**

   - PR title includes issue number: `issue-123: Add storage location widget` or
     `001-sample-storage: Implement barcode scanning`

2. **Branch Naming:**

   - Branch name matches: `issue-{###}-{feature-name}` or `{###-feature-name}`

3. **Target Branch:**

   - Always target `develop` (unless hotfix to `main`)

4. **Code Formatting (MANDATORY):**

   - Backend: `mvn spotless:apply` - MUST run before commit
   - Frontend: `npm run format` - MUST run before commit
   - Pre-commit hooks recommended

5. **Build Verification:**

   - `mvn clean install -DskipTests -Dmaven.test.skip=true` passes locally

6. **Tests Included:**

   - Unit tests for business logic
   - ORM validation tests (if new entities)
   - Integration tests for API endpoints
   - E2E tests for user workflows (if UI changes)

7. **Test Coverage:**

   - > 70% coverage for new code (JaCoCo report)

8. **UI Screenshots:**

   - Attach before/after images for UI changes

9. **Single Concern:**

   - PR addresses ONE issue only (no mixed refactoring + features)

10. **Constitution Compliance:**

    - [ ] Layered architecture respected (Principle IV)
    - [ ] Carbon Design System used exclusively (Principle II)
    - [ ] FHIR compliance for external data (Principle III)
    - [ ] Configuration-driven variation (Principle I)
    - [ ] Internationalization complete (Principle VII)
    - [ ] Liquibase for schema changes (Principle VI)
    - [ ] Security/compliance requirements met (Principle VIII)

11. **No Hardcoded Strings:**

    - All user-facing text uses React Intl

12. **Liquibase Changesets:**

    - Schema changes via Liquibase XML (NOT direct SQL)
    - Rollback scripts provided

13. **FHIR Resources Validated:**

    - If FHIR-mapped entities, test FHIR transformation

14. **Documentation Updated:**

    - Update spec.md, plan.md, quickstart.md if applicable

15. **Review Assignment:**
    - Request review from appropriate team members

### CI/CD Pipeline

**GitHub Actions workflows (MUST pass):**

- `ci.yml` - Maven build + JaCoCo coverage report
- `publish-and-test.yml` - Docker image build + integration tests
- `frontend-qa.yml` - Cypress E2E tests
- `build-installer.yml` - Offline installer packaging

### Code Review Standards

**Reviewers MUST verify:**

- ✅ Constitution compliance (all 8 principles)
- ✅ Layered architecture (no DAO calls from controllers)
- ✅ No `@Transactional` in controllers
- ✅ Services compile all data within transaction
- ✅ FHIR resources validated (if applicable)
- ✅ Internationalization complete
- ✅ Carbon Design System used exclusively
- ✅ Tests included and passing
- ✅ No security vulnerabilities (SQL injection, XSS, etc.)

**Approval Required:** Minimum 1 approval from core team before merge

---

## Additional Resources

### Documentation

- **Constitution:** `.specify/memory/constitution.md` (authoritative governance,
  v1.7.0)
- **README:** `README.md` (project overview, setup)
- **Contributing:** `CONTRIBUTING.md` (contribution process)
- **Pull Request Tips:** `PULL_REQUEST_TIPS.md` (15-point checklist)
- **Code of Conduct:** `CODE_OF_CONDUCT.md` (community standards)
- **Dev Setup:** `docs/dev_setup.md` (detailed development environment setup)

### SpecKit Templates

- **Spec Template:** `.specify/templates/spec-template.md`
- **Plan Template:** `.specify/templates/plan-template.md`
- **Tasks Template:** `.specify/templates/tasks-template.md`
- **Checklist Template:** `.specify/templates/checklist-template.md`

### Example Feature (Comprehensive Reference)

- **Feature:** `specs/001-sample-storage/`
  - Fully implemented sample storage management feature
  - Complete spec.md, plan.md, tasks.md, quickstart.md
  - Shows TDD workflow, constitution compliance, SpecKit integration
  - 100+ tasks executed with Red-Green-Refactor cycle
  - 45/45 Cypress E2E tests passing

### External Resources

- **Carbon Design System:** https://carbondesignsystem.com/
- **OpenELIS Carbon Guide:**
  https://uwdigi.atlassian.net/wiki/spaces/OG/pages/621346838
- **HL7 FHIR R4:** https://hl7.org/fhir/R4/
- **IHE Lab Profiles:**
  https://wiki.ihe.net/index.php/Laboratory_Technical_Framework
- **HAPI FHIR:** https://hapifhir.io/
- **GitHub SpecKit:** https://github.com/anthropics/github-speckit

---

## Quick Reference Card

### Critical Commands

```bash
# Build backend (proper test skipping)
mvn clean install -DskipTests -Dmaven.test.skip=true

# Format code (MUST before commit)
mvn spotless:apply && cd frontend && npm run format && cd ..

# Hot reload backend
mvn clean install -DskipTests -Dmaven.test.skip=true
docker compose -f dev.docker-compose.yml up -d --no-deps --force-recreate oe.openelis.org

# Run individual E2E test (development)
npm run cy:run -- --spec "cypress/e2e/{feature}.cy.js"

# Verify Java version
java -version  # Must be 21.x.x
sdk env        # SDKMAN auto-switch
```

### Critical Files

- **Constitution:** `.specify/memory/constitution.md`
- **Project Instructions:** `AGENTS.md` (this file), `CLAUDE.md`
- **Setup:** `README.md`, `docs/dev_setup.md`
- **Feature Example:** `specs/001-sample-storage/quickstart.md`

### Critical Rules

- ✅ Java 21 MANDATORY
- ✅ Test skip: `-DskipTests -Dmaven.test.skip=true`
- ✅ JUnit 4 (NOT JUnit 5)
- ✅ jakarta.persistence (NOT javax.persistence)
- ✅ @Transactional in services ONLY (NOT controllers)
- ✅ Services compile all data within transaction
- ✅ Carbon Design System ONLY (NO Bootstrap/Tailwind)
- ✅ React Intl for ALL strings (NO hardcoded text)
- ✅ Liquibase for ALL schema changes
- ✅ Format before commit (spotless + prettier)
- ✅ Individual E2E tests during dev (NOT full suite)

---

**Last Updated:** 2025-11-09 **Constitution Version:** 1.7.0 **Maintained By:**
OpenELIS Global Core Team **Questions?** Post in GitHub Discussions or weekly
developer sync
