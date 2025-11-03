# Spec-Driven Development with GitHub Spec Kit: A Comprehensive Guide

**Audience**: Engineering teams implementing SDD methodology  
**Context**: OpenELIS Global sample storage management feature  
**Last Updated**: 2025-10-30

---

## Table of Contents

1. [Understanding Spec-Driven Development](#understanding-spec-driven-development)
2. [GitHub Spec Kit Architecture](#github-spec-kit-architecture)
3. [The Slash Command Workflow](#the-slash-command-workflow)
4. [Applying SDD to OpenELIS Storage Management](#applying-sdd-to-openelis-storage-management)
5. [Step-by-Step Implementation Guide](#step-by-step-implementation-guide)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)

---

## Understanding Spec-Driven Development

### The Paradigm Shift

Spec-Driven Development (SDD) represents a fundamental inversion of the
traditional software development model. For decades, **code has been
king**—specifications served as transient scaffolding, discarded once
implementation began. In SDD, **specifications become the source of truth**,
with implementation as the continuously regenerable output.

This is not waterfall planning or exhaustive documentation. SDD captures the
"why" behind technical decisions in evolvable, version-controlled artifacts that
grow with your understanding of the problem space.

### The Communication Problem SDD Solves

Consider a typical cross-functional failure scenario:

- **Product Manager**: "Notification preferences" = per-channel toggles
- **Backend Engineer**: Implements single on/off switch
- **Frontend Developer**: Assumes OS notification integration
- **Designer**: Mocks up UI requiring user service refactor

No individual made an unreasonable assumption. The failure was **lack of shared
context**. SDD surfaces these assumptions early, when changing direction costs
keystrokes rather than sprints.

### SDD in AI-Assisted Development

SDD becomes **especially critical** when AI agents generate code. Specifications
provide:

1. **Steering Context**: Well-defined specs guide AI toward correct solutions
2. **Multi-Variant Generation**: Easily explore multiple implementations from
   single spec
3. **Living Documentation**: Specs evolve alongside code, maintaining alignment
4. **Shared Understanding**: Human teams and AI agents reference same source of
   truth

**Example**: Curious about performance differences between Rust and Go
implementations? Generate both from the same specification. Exploring different
UI approaches? Generate variants based on multiple Figma designs—all grounded in
the same functional requirements.

### The SDD Workflow

```
Idea (vague)
  → /speckit.specify → Functional Specification (PRD)
    → /speckit.clarify → Clarifications & Decisions
      → /speckit.plan → Technical Implementation Plan
        → /speckit.tasks → Executable Task Breakdown
          → Implementation (AI-generated or human-coded)
            → Production Metrics → Spec Updates (continuous loop)
```

**Key Insight**: Specifications are **living documents**, not dusty artifacts.
Updating specs becomes as natural as refactoring code—without touching
implementation.

---

## GitHub Spec Kit Architecture

### Components

**1. Specify CLI**
(`uv tool install specify-cli --from git+https://github.com/github/spec-kit.git`):

- Bootstraps SDD project structure
- Injects slash commands into your coding agent (Cursor, VS Code, etc.)
- Manages feature numbering and branch creation
- Downloads official templates

**2. Templates & Scripts** (`.specify/` directory):

- `feature-specification-template.md` - PRD structure
- `memory/constitution.md` - Project principles, tech stack constraints
- `scripts/` - Helper utilities
- Custom prompts for each slash command

**3. Slash Commands** (injected into Cursor/VS Code):

- `/speckit.specify` - Generate functional specification
- `/speckit.clarify` - Clarify ambiguities via structured Q&A
- `/speckit.plan` - Create technical implementation plan
- `/speckit.tasks` - Break down into executable tasks

### Directory Structure

After `specify init`, your project gains:

```
project-root/
├── .specify/                          # Spec Kit configuration
│   ├── templates/
│   │   └── feature-specification-template.md
│   └── prompts/                       # Custom prompts for slash commands
├── memory/
│   └── constitution.md                # Project principles & constraints
├── specs/
│   ├── 001-feature-name/
│   │   ├── spec.md                    # Functional specification (PRD)
│   │   ├── clarifications.md          # Q&A, decisions log
│   │   ├── plan.md                    # Technical implementation plan
│   │   ├── tasks.md                   # Executable task breakdown
│   │   ├── research.md                # Tech stack research
│   │   ├── data-model.md              # Database schema
│   │   ├── quickstart.md              # Getting started guide
│   │   └── contracts/                 # API specs, FHIR resources
│   └── 002-next-feature/
└── scripts/
    ├── check-prerequisites.sh
    └── create-new-feature.sh
```

### Artifact Relationships

```
memory/constitution.md
        ↓ (constrains)
    spec.md ←──→ clarifications.md
        ↓ (informs)
    plan.md
        ↓ (derives)
  tasks.md → Implementation → Production Metrics
        ↑                              ↓
        └────────── (feedback loop) ────┘
```

---

## The Slash Command Workflow

### `/speckit.specify` - Functional Specification

**Purpose**: Transform a feature description into a comprehensive PRD,
**explicitly excluding technical decisions**.

**What It Does**:

1. Scans existing `specs/` to determine next feature number (001, 002, etc.)
2. Creates semantic branch name from description
3. Copies and customizes feature specification template
4. Generates structured PRD with user stories, acceptance criteria,
   non-functional requirements

**Key Principle**: Focus on **"what" and "why"**, not **"how"**. No tech stack
decisions at this stage.

**Example Prompt**:

```
/speckit.specify

Create a functional specification for sample storage management in OpenELIS Global.

The feature manages physical storage of samples in a hierarchical structure:
Room → Device (fridge/freezer) → Shelf → Rack → Box → Position

KEY REQUIREMENT: Add a Box entity between Rack and Position. Each rack contains boxes,
each box contains a 9×9 or 10×10 grid of positions. Boxes have barcode labels.

User personas:
- Lab Technician: Assigns samples to storage during accessioning
- Storage Manager: Manages capacity, moves samples, deactivates locations
- QA Staff: Audits storage compliance

Critical workflows:
1. Barcode scanning for rapid location selection
2. Sample assignment with cascade validation (parent active, capacity available)
3. Sample movement with reason tracking and dual-authorization
4. Disposal workflow with chain-of-custody

Non-functional requirements:
- Performance: Dashboard <2s with 100k samples
- Security: Role-based access, immutable audit logs
- Accessibility: WCAG 2.1 AA compliance

Base on .dev-docs/original-spec.md but integrate Box entity throughout.
```

**Tip**: "Having a **very detailed first prompt** will produce a much better
specification that the agent can use for further project buildouts." — Microsoft
Spec Kit Blog

**Output**: `specs/001-sample-storage/spec.md`

### `/speckit.clarify` - Structured Clarification

**Purpose**: Identify and resolve ambiguities **before** creating technical
plan. Reduces downstream rework.

**What It Does**:

1. Analyzes `spec.md` for ambiguous requirements
2. Generates structured Q&A format (multiple choice when possible)
3. Records decisions in `clarifications.md`
4. Updates `spec.md` with refined requirements

**When to Use**: **Required before `/speckit.plan`**. Skip only for spikes or
exploratory prototypes (state explicitly).

**Example Prompt**:

```
/speckit.clarify

Review specs/001-sample-storage/spec.md and identify clarifications needed:

Box Entity Questions:
- Can boxes be moved between racks or are they fixed?
- Box barcode uniqueness: global or within-rack?
- Can samples be assigned to box-level without specific position?
- Must position count match box dimensions (rows × columns)?
- What happens to box assignments when parent rack is deactivated?

Data Model:
- Should Position.box_id be FK or should SampleStorageAssignment support both box_id and position_id?
- Capacity enforcement: box-level, rack-level, or both?

Workflows:
- Barcode scanning: scan box then position, or scan full hierarchy?
- Batch operations: can entire boxes be moved at once?

FHIR Integration:
- Is Box mapped to FHIR Location or separate physicalType?
- Does Specimen link via container or extension?
```

**Best Practice**: Use **structured Q&A format**:

```markdown
**Q1: Box Mobility**

- a) Boxes are fixed to racks (default)
- b) Boxes can be moved between racks

**Decision**: Option B - Boxes can be moved. Add "Move Box" workflow.
```

**Output**: `specs/001-sample-storage/clarifications.md` with decisions log

### `/speckit.plan` - Technical Implementation Plan

**Purpose**: Translate functional requirements into **technical architecture**
grounded in project constitution.

**What It Does**:

1. Reads `spec.md` and `clarifications.md`
2. Applies constraints from `memory/constitution.md` (tech stack, patterns,
   standards)
3. Generates implementation plan with:
   - Database schema (Liquibase changesets)
   - API design (REST endpoints, OpenAPI spec)
   - Component architecture
   - FHIR resource mappings
   - Testing strategy
4. Creates supporting artifacts:
   - `data-model.md` - ERD, table schemas
   - `contracts/api-spec.json` - OpenAPI specification
   - `contracts/fhir-spec.md` - FHIR resource definitions
   - `research.md` - Tech stack research (library comparisons)
   - `quickstart.md` - Developer getting started guide

**Example Prompt**:

```
/speckit.plan

Create a technical implementation plan for sample storage management.

Tech stack (from memory/constitution.md):
- Backend: Java 21, Spring Boot 3.x, Hibernate 6.x, PostgreSQL 14+
- Frontend: React 17, Carbon Design System 1.15, SWR
- FHIR: HAPI FHIR R4 (existing local store), IHE mCSD profile
- Barcode: ZXing library

Key constraints:
- All UI must use Carbon Design System components exclusively
- All schema changes via Liquibase (no direct SQL)
- Entities extend BaseObject, services use @Transactional
- FHIR sync on all storage entity mutations
- Frontend follows OpenELIS patterns: functional components, SWR, Cypress E2E

Integration points:
- StorageLocationWidget in LogbookResults (SearchResultForm.js, renderReferral function)
- StorageLocationWidget in SamplePatientEntry (SampleType.js, after collector field)
- FHIR Location resources with IHE mCSD ITI-90 query support

Research areas:
- Carbon DataTable expandable row patterns
- FHIR Location.physicalType custom codes
- PostgreSQL hierarchical query performance (100k samples, 10k boxes)
```

**Output**:

- `specs/001-sample-storage/plan.md`
- `specs/001-sample-storage/data-model.md`
- `specs/001-sample-storage/contracts/`
- `specs/001-sample-storage/research.md`
- `specs/001-sample-storage/quickstart.md`

### `/speckit.tasks` - Task Breakdown

**Purpose**: Convert plan into phased, executable tasks with parallelization
markers.

**What It Does**:

1. Analyzes `plan.md`, `data-model.md`, `contracts/`
2. Derives specific tasks from entities, endpoints, scenarios
3. Identifies dependencies and marks independent tasks `[P]` for parallel
   execution
4. Organizes by user story with checkpoint validations

**Example Output Structure** (`tasks.md`):

```markdown
## User Story 1: Location Management

### Task 1.1: Create StorageBox Entity [backend]

**File**: `src/main/java/org/openelisglobal/storage/valueholder/StorageBox.java`
**Dependencies**: None **Description**: Create JPA entity extending BaseObject
with fhir_uuid, barcode, dimensions **Tests**: Unit test for
calculateCapacity(), generateFhirUuid() **Estimate**: 2 hours

### Task 1.2: Create StorageBox Liquibase Changeset [backend] [P]

**File**:
`src/main/resources/liquibase/changelog/storage/001_create_storage_box.xml`
**Dependencies**: None (parallel with 1.1) **Description**: CREATE TABLE
storage_box with unique constraints on barcode, fhir_uuid **Tests**: Migration
test (up/down)

### Task 1.3: Create StorageBoxDAO [backend]

**File**: `src/main/java/org/openelisglobal/storage/dao/StorageBoxDAO.java`
**Dependencies**: 1.1, 1.2 **Description**: DAO interface + implementation
extending BaseDAOImpl **Tests**: Integration test for CRUD operations

### Checkpoint 1: Verify Box entity persistence

- Run: `mvn test -Dtest=StorageBoxDAOTest`
- Validate: Box with 9×9 dimensions persists, fhir_uuid generated, barcode
  unique

## User Story 2: FHIR Integration

### Task 2.1: Create StorageLocationFhirTransformService [backend]

**File**:
`src/main/java/org/openelisglobal/storage/fhir/service/StorageLocationFhirTransformService.java`
**Dependencies**: 1.1 (StorageBox entity) **Description**: Transform StorageBox
→ FHIR Location with custom physicalType, barcode identifier, dimensions
extension **Tests**: Unit test validates FHIR resource structure

### Task 2.2: Integrate FHIR Sync in StorageBoxService [backend]

**File**:
`src/main/java/org/openelisglobal/storage/serviceimpl/StorageBoxServiceImpl.java`
**Dependencies**: 2.1, 1.3 **Description**: Call fhirService.syncBoxToFhir()
after insert/update **Tests**: Integration test verifies Location resource
created in FHIR store

### Checkpoint 2: Validate FHIR Location creation

- Run: Query local FHIR
  `GET /fhir/Location?identifier=http://openelis-global.org/storage/box-barcode|TEST-BOX`
- Validate: Location resource exists with correct physicalType, extensions

...
```

**Prompt**:

```
/speckit.tasks

Break down the implementation plan into specific, actionable tasks.
Organize by user story with dependencies and parallel execution markers.
Follow TDD: tests before implementation where applicable.
```

**Output**: `specs/001-sample-storage/tasks.md`

---

## Applying SDD to OpenELIS Storage Management

### Current State Analysis

**What We Have**:

- ✅ `memory/constitution.md` - OpenELIS tech stack, patterns, constraints
- ✅ `.dev-docs/original-spec.md` - Original business requirements
- ✅ `specs/001-sample-storage/spec.md` - Functional specification (generated
  via `/speckit.specify`)
- ✅ Figma design extracted (layout reference for Carbon implementation)

**What We Need**:

- ⏭️ `clarifications.md` - Resolve ambiguities (Box mobility, FHIR mapping, UI
  decisions)
- ⏭️ `plan.md` - Technical plan (database schema, REST APIs, FHIR integration,
  Carbon components)
- ⏭️ `tasks.md` - Phased task breakdown with dependencies
- ⏭️ Implementation artifacts (entities, services, controllers, React
  components)

### Why Manual Plan Creation Was Premature

**The Problem**: Creating `plan.md` manually bypasses the AI-powered analysis
that:

- Reads `memory/constitution.md` to apply OpenELIS-specific constraints
- Analyzes `spec.md` comprehensively for edge cases
- Generates interconnected artifacts (data-model.md, contracts/, research.md)
- Ensures consistency across functional requirements and technical decisions

**The Spec Kit Way**: Use `/speckit.plan` with a **detailed prompt** that:

- References specific tech stack decisions (Carbon Design System, HAPI FHIR R4,
  IHE mCSD)
- Specifies integration points (LogbookResults, SamplePatientEntry)
- Identifies research areas (Carbon DataTable patterns, FHIR Location custom
  codes)
- Defines success metrics (performance, coverage, accessibility)

### OpenELIS-Specific Considerations

**From `memory/constitution.md`**:

```markdown
## Tech Stack Constraints

- Backend: Java 21, Spring Boot 3.x, Hibernate 6.x, PostgreSQL 14+
- Frontend: React 17, **Carbon Design System v1.15** (official as of 3.0
  release)
- Database: Liquibase only (no direct SQL), PostgreSQL extensions allowed
- FHIR: HAPI FHIR R4 local store, bidirectional sync

## Architectural Patterns

- Entities: Extend BaseObject<String>, include fhir_uuid for FHIR mapping
- Services: @Service + @Transactional, follow existing service patterns
- REST: Extend BaseRestController, use OpenAPI annotations
- Frontend: Functional components + hooks, Carbon components exclusively
- FHIR: FhirPersistanceService for create/update, FhirTransformService for
  conversions

## Non-Negotiables

- No class components in React (functional only)
- No custom CSS frameworks (Carbon Design System exclusively)
- No ORM bypass (Hibernate only, no JDBC)
- All storage operations logged to immutable audit table
- WCAG 2.1 AA accessibility compliance
```

**These constraints** must inform every technical decision in the plan.

### FHIR Integration Context

**Existing OpenELIS FHIR Infrastructure** (leverage, don't rebuild):

1. **Local HAPI FHIR R4 Store**: `https://fhir.openelis.org:8443/fhir/`

   - Already configured in `volume/properties/common.properties`
   - Stores Patient, Specimen, ServiceRequest, DiagnosticReport, Observation,
     Task resources

2. **FhirPersistanceService**:

   - `createFhirResourceInFhirStore(Resource)` - Creates new FHIR resource
   - `updateFhirResourceInFhirStore(Resource)` - Updates existing FHIR resource
   - Pattern: Call after entity insert/update in service layer

3. **FhirTransformService**:

   - Bidirectional conversion: OpenELIS entity ↔ FHIR R4 resource
   - Example: `transformToOrganization(org.hl7.fhir.r4.model.Organization)`

4. **Facility Registry Import**:
   - `OrganizationImportServiceImpl.importLocationsFromBundle()`
   - Already imports FHIR Location resources from external registries
   - Can be extended for storage room sync

**For Storage Management**:

- Create `StorageLocationFhirTransformService` (similar to
  FhirTransformServiceImpl)
- Map storage hierarchy to FHIR Location resources using **IHE mCSD profile**
- Sync Box → FHIR Location with custom physicalType and extensions
- Link Specimen → Position via `Specimen.container.extension:storageLocation`

**IHE mCSD Profile** ([link](https://build.fhir.org/ig/IHE/ITI.mCSD/)):

- **ITI-90**: Find Matching Care Services (query Location by physicalType,
  identifier, partOf)
- **Location.physicalType**: Standard codes (ro=room, ve=vehicle/equipment,
  area) + custom codes (box, rack, position)
- **Location.partOf**: Hierarchical relationships (Position partOf Box partOf
  Rack partOf ...)
- **Location.identifier**: Barcode identifiers for box/position lookups
- **Location.extension**: Custom extensions for box dimensions, position
  coordinates

---

## Step-by-Step Implementation Guide

### Prerequisites

**Verify Spec Kit Installation**:

```bash
cd /Users/pmanko/code/OpenELIS-Global-2
specify --help
```

**Verify Project Structure**:

```bash
ls -la | grep -E "(specs|memory|scripts|.specify)"
# Should show: specs/, memory/, scripts/, .specify/
```

**Verify Constitution Exists**:

```bash
cat memory/constitution.md
# Should contain OpenELIS tech stack, patterns, constraints
```

### Step 1: Review Existing Functional Specification

**Command**: (manual review)

```bash
cat specs/001-sample-storage/spec.md
```

**Validate**:

- ✓ User stories defined?
- ✓ Box entity integrated into hierarchy?
- ✓ Functional requirements (FR-1 to FR-24) complete?
- ✓ Non-functional requirements (performance, security, accessibility)?
- ✓ UI/UX requirements reference Carbon Design System?

**If incomplete**: Iterate on `/speckit.specify` with more detailed prompt.

### Step 2: Run Clarification Workflow

**Command**: (in Cursor chat)

```
/speckit.clarify

Review specs/001-sample-storage/spec.md and clarify ambiguities:

Box Entity:
- Can boxes be moved between racks? (mobility implications)
- Box barcode uniqueness scope? (global vs within-rack)
- Box-level vs position-level assignment? (data model implications)
- Position count validation? (must match box dimensions?)
- Box deactivation with assigned samples? (validation rules)

Data Model:
- SampleStorageAssignment FK strategy? (box_id OR position_id, both nullable?)
- Capacity enforcement level? (position-only or box-level?)
- Audit log table design? (immutability implementation)

FHIR Mapping:
- Box as FHIR Location physicalType? (standard vs custom code)
- Specimen-to-Position link? (container vs extension)
- Facility registry sync? (import rooms from external FHIR server?)

UI/UX:
- Location selector skip levels? (assign to box without position?)
- Box grid view presentation? (modal vs inline expansion)
- Barcode scan behavior? (auto-populate vs navigate directly)

For each question, provide multiple choice options with recommended default.
Record decisions with rationale.
```

**Expected Duration**: 10-20 minutes of interactive Q&A

**Output**: `specs/001-sample-storage/clarifications.md`

**Manual Validation**:

```bash
cat specs/001-sample-storage/clarifications.md
# Verify: All ambiguities addressed? Decisions logged with rationale?
```

### Step 3: Generate Technical Implementation Plan

**Command**: (in Cursor chat)

```
/speckit.plan

Create a technical implementation plan for sample storage management.

Tech Stack (from memory/constitution.md):
- Backend: Java 21, Spring Boot 3.x, Hibernate 6.x, PostgreSQL 14+
- Frontend: React 17, Carbon Design System 1.15, SWR, Cypress 12.x
- FHIR: HAPI FHIR R4 (existing local store at org.openelisglobal.fhirstore.uri)
- Barcode: ZXing library
- Testing: JUnit 5, Mockito, Cypress, Jest + RTL

Integration Points (per spec.md):
- StorageLocationWidget in LogbookResults expanded view (SearchResultForm.js, renderReferral)
- StorageLocationWidget in SamplePatientEntry (SampleType.js, after collector field)
- FHIR Location resources sync to existing HAPI FHIR server
- Leverage existing FhirPersistanceService and FhirTransformService

Database Design:
- 6 new tables: storage_room, storage_device, storage_shelf, storage_rack, storage_box, storage_position
- 2 assignment tables: sample_storage_assignment, storage_audit_log
- All tables have fhir_uuid for FHIR resource mapping
- Box barcode unique constraint, position occupancy unique index

REST API Design:
- Base path: /rest/storage
- Cascading endpoints: /rooms, /rooms/{id}/devices, /devices/{id}/shelves, ...
- Sample operations: GET /{sampleId}/location, POST /{sampleId}/assign, PUT /{sampleId}/move

FHIR Mapping (IHE mCSD):
- Room → Location (physicalType: "ro")
- Device → Location (physicalType: "ve", temp extension)
- Shelf → Location (physicalType: "area")
- Rack → Location (custom physicalType: "rack")
- Box → Location (custom physicalType: "box", barcode identifier, dimensions extension)
- Position → Location (custom physicalType: "position", coordinates extension)
- Specimen.container.extension → Position Location reference

Frontend Architecture:
- StorageLocationWidget: 6 cascading Carbon Dropdowns
- BoxGridVisualization: Custom grid with Carbon styling tokens
- StorageDashboard: Carbon DataTable, Tabs, Tiles, Search, Pagination
- All styling via Carbon SCSS tokens ($spacing-*, $text-*, $layer-*)

Research Areas:
- Carbon DataTable expandable row patterns (for LogbookResults integration)
- FHIR StructureDefinition for custom Location extensions
- PostgreSQL CTE performance for hierarchical path queries
- ZXing barcode generation for QR codes vs linear barcodes
```

**Expected Duration**: 5-10 minutes of AI generation

**Output**:

- `specs/001-sample-storage/plan.md` (comprehensive technical plan)
- `specs/001-sample-storage/data-model.md` (ERD, Liquibase changesets)
- `specs/001-sample-storage/contracts/api-spec.json` (OpenAPI)
- `specs/001-sample-storage/contracts/fhir-spec.md` (FHIR Location
  StructureDefinitions)
- `specs/001-sample-storage/research.md` (tech stack comparisons)
- `specs/001-sample-storage/quickstart.md` (dev getting started guide)

**Manual Validation**:

```bash
# Review plan
cat specs/001-sample-storage/plan.md

# Check for:
# - Database schema complete (9 tables)?
# - REST endpoints defined with OpenAPI spec?
# - FHIR mappings for all 6 levels + Specimen?
# - Carbon component mapping for all UI?
# - Integration points (LogbookResults, SamplePatientEntry) addressed?
# - Test strategy defined (unit, integration, E2E)?
```

### Step 4: Generate Task Breakdown

**Command**: (in Cursor chat)

```
/speckit.tasks

Break down the implementation plan into specific, actionable tasks.

Organize by user story from spec.md.
Mark dependencies explicitly.
Identify parallelizable tasks with [P] marker.
Follow TDD: tests before implementation where applicable.
Include checkpoint validations after each phase.

Group tasks into phases:
1. Core Data Model (entities, DAOs, Liquibase)
2. Service Layer (business logic, validation)
3. REST API (controllers, DTOs)
4. FHIR Integration (transform service, sync hooks)
5. Frontend Components (StorageLocationWidget, Dashboard, Grid)
6. Integration (LogbookResults, SamplePatientEntry)
7. Testing (unit, integration, E2E)
8. Documentation (user guide, API docs)
```

**Expected Duration**: 3-5 minutes

**Output**: `specs/001-sample-storage/tasks.md`

**Manual Validation**:

```bash
cat specs/001-sample-storage/tasks.md | grep "Task [0-9]"
# Count tasks: Should be 40-60 tasks total

cat specs/001-sample-storage/tasks.md | grep "\[P\]"
# Verify: Parallel tasks identified (DAOs, DTOs, frontend components)

cat specs/001-sample-storage/tasks.md | grep "Checkpoint"
# Verify: Checkpoints after each phase
```

### Step 5: Execute Implementation

**Command**: (in Cursor chat)

```
/speckit.implement

Implement the sample storage management feature following specs/001-sample-storage/tasks.md.

Execute tasks sequentially, respecting dependencies.
Execute parallel tasks [P] concurrently when possible.
Follow TDD approach where specified.
Run mvn spotless:apply after Java changes.
Run npm run format after JS/JSX changes.
Execute checkpoint validations after each phase.
Report progress with task completion status.
```

**What the AI Will Do**:

1. Parse `tasks.md`
2. Execute tasks in dependency order
3. Create files (entities, services, components)
4. Run tests after each phase
5. Execute checkpoints
6. Handle build/test errors
7. Report progress

**Expected Duration**: 2-4 hours (AI execution time), 8-10 weeks (calendar time
with human review)

### Step 6: Review and Iterate

**After Each Phase**:

```bash
# Run tests
mvn test
npm test

# Check formatting
mvn spotless:check
npm run format:check

# Review generated code
git diff

# Update spec if needed
# Re-run /speckit.plan or /speckit.tasks with refinements
```

**SDD Principle**: Specs are **living documents**. If implementation reveals
ambiguities or better approaches, update the spec and regenerate.

---

## Best Practices

### 1. Detailed First Prompts

**From Microsoft Spec Kit Blog**:

> "Having a **very detailed first prompt** will produce a much better
> specification that the agent can use for further project buildouts."

**Good `/speckit.specify` Prompt**:

```
/speckit.specify

[Detailed business context]
[User personas with specific needs]
[Critical workflows with step-by-step flows]
[Non-functional requirements with specific metrics]
[Integration constraints]
[Out-of-scope items]
```

**Bad Prompt**:

```
/speckit.specify
Build a sample storage system.
```

### 2. Constitution Before Specification

**Always create `memory/constitution.md` first**:

- Tech stack locked-in decisions (Carbon Design System, HAPI FHIR R4)
- Architectural patterns (BaseObject, @Transactional services)
- Code standards (Spotless, Prettier)
- Non-negotiables (no class components, Liquibase only, WCAG 2.1 AA)

**Why**: `/speckit.plan` uses constitution to ground technical decisions.
Without it, AI may suggest incompatible tech.

### 3. Clarify Before Planning

**Workflow**:

```
/speckit.specify → spec.md
     ↓
/speckit.clarify → clarifications.md (REQUIRED)
     ↓
/speckit.plan → plan.md
```

**Skipping clarification = rework downstream**. Box entity ambiguities
discovered during implementation require plan regeneration.

### 4. Iterative Refinement

**SDD is not waterfall**. Treat specs as code:

- Review PRs for spec changes
- Version specs in git
- Iterate based on implementation feedback

**Example**:

```bash
# During implementation, discover performance issue
# Update spec.md:
# NFR-10: Dashboard query time <500ms (previously <2s)

# Regenerate plan with new constraint
/speckit.plan
[Update prompt with new performance requirement]

# Regenerate tasks
/speckit.tasks
```

### 5. Checkpoint Validations

**After each task phase**, run checkpoint:

```bash
# Checkpoint: Verify Box entity persistence
mvn test -Dtest=StorageBoxDAOTest
psql -d openelis -c "SELECT * FROM storage_box WHERE barcode = 'TEST-BOX';"

# Checkpoint: Verify FHIR Location sync
curl http://localhost:8081/fhir/Location?identifier=http://openelis-global.org/storage/box-barcode|TEST-BOX

# Checkpoint: Verify UI component renders
npm test -- StorageLocationWidget.test.js
cypress run --spec "cypress/e2e/storage/location-widget.cy.js"
```

**If checkpoint fails**: Stop, debug, update spec/plan if needed, re-generate
tasks.

### 6. Research Artifacts

**Use `/speckit.plan` to generate research**:

- Compare libraries (ZXing vs barcode4j for barcode generation)
- Benchmark approaches (PostgreSQL CTE vs recursive queries)
- Validate compatibility (Carbon DataTable expandable rows + custom expansion
  content)

**Output**: `specs/001-sample-storage/research.md` documents trade-offs and
decisions.

---

## OpenELIS Storage Management: Concrete Example

### Step-by-Step Walkthrough

**Context**: We have `spec.md` created. Now we need to clarify, plan, and
implement the Box entity integration.

#### Step 1: Run Clarification

**In Cursor Chat**:

```
/speckit.clarify

Review specs/001-sample-storage/spec.md and identify clarifications for Box entity integration:

Box Design:
1. Box Mobility
   - a) Fixed to rack (simpler, default)
   - b) Movable between racks (flexible, requires audit trail)
   **Question**: Should boxes be relocatable or permanently assigned to racks?

2. Barcode Uniqueness
   - a) Unique within rack
   - b) Globally unique (recommended)
   **Question**: What is the uniqueness scope for box barcodes?

3. Assignment Granularity
   - a) Position-level only (samples always assigned to specific position)
   - b) Box-level OR position-level (samples can be assigned to box without position)
   **Question**: Does SampleStorageAssignment need box_id OR position_id?

FHIR Mapping:
4. Box as FHIR Location
   - a) Standard physicalType with extension
   - b) Custom physicalType "box" (recommended for mCSD queries)
   **Question**: How should Box map to FHIR Location.physicalType?

5. Specimen Link
   - a) Specimen.container.identifier = box barcode
   - b) Specimen.container.extension = position Location reference
   **Question**: How should Specimen reference storage position?

UI/UX:
6. Location Selector
   - a) All 6 levels required (strict)
   - b) Can stop at any level (flexible, per FR-12)
   **Question**: Can users assign samples to box-level without selecting position?

For each question, record decision with rationale in clarifications.md.
```

**AI Response**: Generates structured Q&A in `clarifications.md` with decisions
log.

#### Step 2: Run Technical Planning

**In Cursor Chat**:

```
/speckit.plan

Create technical implementation plan for sample storage management with FHIR/mCSD integration.

Tech Stack:
- Java 21, Spring Boot 3.x, Hibernate 6.x, PostgreSQL 14+
- React 17, Carbon Design System 1.15 (official OpenELIS UI framework)
- HAPI FHIR R4 (existing at org.openelisglobal.fhirstore.uri)
- ZXing for barcode generation
- Liquibase for migrations

Architecture (per memory/constitution.md):
- Entities: Extend BaseObject<String>, include fhir_uuid UUID column
- Services: @Service + @Transactional, use existing DAO patterns
- REST: Extend BaseRestController, cascading endpoints
- FHIR: Use existing FhirPersistanceService, create StorageLocationFhirTransformService
- Frontend: Functional components, Carbon Dropdown/ComboBox for cascading selector

Database Schema:
- storage_box table with: id, fhir_uuid, rack_id FK, barcode UNIQUE, label, rows, columns, capacity, status
- sample_storage_assignment with: box_id OR position_id (nullable, mutually exclusive via check constraint)
- storage_audit_log (immutable: revoke UPDATE/DELETE from app user)

FHIR Integration:
- Box → Location (physicalType: custom "box", barcode identifier, dimensions extension)
- Position → Location (physicalType: custom "position", coordinates extension)
- Specimen.container.extension:storageLocation → Position Location reference
- Sync on insert/update via FhirPersistanceService.createFhirResourceInFhirStore()

Frontend Components:
- StorageLocationWidget: 6 Carbon Dropdowns (cascading), barcode scan button, grid view button
- Integrate in LogbookResults: SearchResultForm.js, renderReferral function (line 1253)
- Integrate in SamplePatientEntry: SampleType.js, after collector field (line 573)
- BoxGridVisualization: Custom grid (40px cells), Carbon color tokens
- StorageDashboard: Carbon Tabs, DataTable (expandable), Search, Pagination

Research Areas:
- Carbon DataTable expandable row content customization
- FHIR StructureDefinition for custom Location extensions
- PostgreSQL hierarchical query optimization (WITH RECURSIVE vs materialized path)

Generate artifacts:
- plan.md (comprehensive technical plan)
- data-model.md (ERD, Liquibase changesets)
- contracts/api-spec.json (OpenAPI for /rest/storage/*)
- contracts/fhir-spec.md (Location StructureDefinitions)
- research.md (library comparisons)
- quickstart.md (dev setup guide)
```

**Expected AI Artifacts**:

- `plan.md` - 20-30 pages covering architecture, database, backend, frontend,
  FHIR, testing
- `data-model.md` - ERD diagram, table schemas, Liquibase XML
- `contracts/api-spec.json` - OpenAPI 3.0 spec for REST endpoints
- `contracts/fhir-spec.md` - FHIR Location resource definitions, extensions
- `research.md` - Tech stack research (e.g., ZXing vs barcode4j comparison)
- `quickstart.md` - "How to run/test this feature" guide

#### Step 3: Generate Task Breakdown

**In Cursor Chat**:

```
/speckit.tasks

Break down specs/001-sample-storage/plan.md into executable tasks.

Organization:
- Group by user story from spec.md
- Include file paths for each task
- Mark dependencies explicitly
- Identify parallelizable tasks with [P]

Task Structure:
- Task ID (e.g., 1.1, 1.2)
- Description (what to build)
- File path (where to build it)
- Dependencies (which tasks must complete first)
- Tests (unit, integration, E2E)
- Estimate (hours)

Phases:
1. Database (Liquibase changesets) [parallel by table]
2. Entities (JPA classes) [parallel by entity]
3. DAOs [parallel by entity]
4. Services (business logic, FHIR sync)
5. REST Controllers
6. Frontend Components [parallel: Widget, Dashboard, Grid]
7. Integration (LogbookResults, SamplePatientEntry)
8. Testing (E2E scenarios from spec.md)

Checkpoints:
- After Phase 1: Migrations run clean
- After Phase 3: CRUD operations work
- After Phase 5: API endpoints return correct data
- After Phase 7: Full workflows complete end-to-end
```

**Expected Output**: `tasks.md` with 40-60 tasks, organized, prioritized,
estimated.

#### Step 4: Execute Implementation

**In Cursor Chat**:

```
/speckit.implement

Implement specs/001-sample-storage/tasks.md.

Execution strategy:
- Sequential by dependency
- Parallel execution for [P] tasks where possible
- TDD: Write tests before implementation
- Formatting: mvn spotless:apply, npm run format
- Checkpoints: Validate after each phase

Progress reporting:
- Log completed tasks
- Report checkpoint results
- Flag blockers for human review

Start with Phase 1 (Database schema).
```

**AI Execution**: Creates all files, runs tests, reports progress.

---

## Troubleshooting

### Issue: Slash Commands Not Available

**Symptom**: `/speckit.specify` shows "Unknown command"

**Solution**:

```bash
# Verify Spec Kit initialization
ls -la .specify/
# Should show templates/, prompts/, etc.

# Re-initialize if needed
specify init . --ai cursor-agent
```

### Issue: AI Generates Non-Carbon Components

**Symptom**: Plan includes Material-UI, Ant Design, or custom Tailwind

**Solution**: Update `memory/constitution.md` with explicit constraint:

```markdown
## UI Framework (Non-Negotiable)

- Carbon Design System v1.15 EXCLUSIVELY
- No Material-UI, Ant Design, Bootstrap, Tailwind, or custom CSS frameworks
- All styling via Carbon SCSS tokens ($spacing-_, $text-_, $layer-\*)
- Reference: OpenELIS Global 3.0 officially adopted Carbon (August 2024)
```

Re-run `/speckit.plan` after constitution update.

### Issue: FHIR Integration Not Addressed

**Symptom**: Plan lacks FHIR Location mappings

**Solution**: Add to `/speckit.plan` prompt:

```
CRITICAL: OpenELIS has existing HAPI FHIR R4 server.
Use FhirPersistanceService.createFhirResourceInFhirStore() for sync.
Map all storage levels to FHIR Location resources per IHE mCSD profile.
```

### Issue: Tasks Too Granular or Too Coarse

**Symptom**: `tasks.md` has 200+ micro-tasks or 5 giant tasks

**Solution**: Refine `/speckit.tasks` prompt:

```
Target: 40-60 tasks total
Granularity: 2-8 hours per task
Group related operations (e.g., "Create StorageBox entity, DAO, service" = 1 task, not 3)
```

---

## Comparing Approaches: Manual vs CLI

| Aspect                     | Manual Creation (What I Did) | CLI Slash Commands (Proper SDD)             |
| -------------------------- | ---------------------------- | ------------------------------------------- |
| **Generation**             | Static markdown files        | AI-powered with structured prompts          |
| **Consistency**            | Manual effort to align       | Template-based, consistent format           |
| **Completeness**           | May miss edge cases          | Systematic coverage via prompts             |
| **Interconnections**       | Manual cross-references      | Auto-generated references between artifacts |
| **Constitution Grounding** | Manual application           | Automatic constraint application            |
| **Research Artifacts**     | Not generated                | Auto-generates research.md, contracts/      |
| **Time Investment**        | Faster initial creation      | Slower initial, faster iteration            |
| **Quality**                | Depends on author expertise  | Leverages AI pattern recognition            |
| **Maintenance**            | Manual updates               | Regenerate with refined prompts             |

**Verdict**: **CLI approach is superior for SDD methodology**. Manual creation
acceptable for quick prototypes or when CLI unavailable, but loses SDD's core
benefits: AI-powered analysis, systematic completeness, constitution grounding.

---

## Next Steps for OpenELIS Storage

### Recommended Workflow

**Option A: Restart with Proper SDD** (Recommended):

1. Keep existing `spec.md` (already good quality from initial
   `/speckit.specify`)
2. Delete manually created `clarifications.md`, `plan.md`
3. Run `/speckit.clarify` in Cursor chat with detailed Box entity questions
4. Run `/speckit.plan` with comprehensive prompt (tech stack, FHIR, Carbon)
5. Run `/speckit.tasks` to generate executable breakdown
6. Run `/speckit.implement` to execute

**Option B: Hybrid Approach** (Faster, less systematic):

1. Keep manually created artifacts
2. Use as reference but still run CLI commands to validate completeness
3. Merge AI-generated insights into manual artifacts
4. Proceed to implementation

**Option C: Manual Continuation** (Not recommended for SDD):

1. Continue with manually created plan.md
2. Implement directly without task breakdown
3. Lose SDD benefits (systematic analysis, interconnected artifacts)

### My Recommendation

**Run the CLI commands** in Cursor chat. The 10-20 minutes invested in proper
SDD workflow will save hours of rework when implementation uncovers ambiguities
that `/speckit.clarify` would have caught.

---

## Conclusion

GitHub Spec Kit transforms SDD from a documentation burden into a **lightweight,
AI-powered workflow** that produces:

- **Better specifications** (systematic analysis catches edge cases)
- **Grounded plans** (constitution ensures alignment with constraints)
- **Executable tasks** (dependency-aware, parallelizable)
- **Living documentation** (evolves with implementation)

For OpenELIS storage management:

- Spec Kit ensures Carbon Design System compliance
- FHIR/mCSD integration follows existing patterns
- Box entity ambiguities resolved before implementation
- Multi-week project broken into manageable tasks

**The power of SDD**: Specifications become the **source of truth**, with code
as the regenerable output. Update specs, regenerate implementation—explore
variants, refactor architecture, evolve with production feedback.

---

**References**:

- [GitHub Spec Kit Repository](https://github.com/github/spec-kit)
- [Microsoft Spec Kit Blog](https://developer.microsoft.com/blog/spec-driven-development-spec-kit)
- [IHE mCSD Profile](https://build.fhir.org/ig/IHE/ITI.mCSD/)
- [OpenELIS Global Documentation](https://docs.openelis-global.org)
- [Carbon Design System](https://carbondesignsystem.com)

---

# Spec-Driven Development Cheat Sheet for OpenELIS Global

**Quick reference for running GitHub Spec Kit workflow**

---

## Terminal Setup (Run Once)

```bash
# 1. Navigate to main repo (not worktree!)
cd /Users/pmanko/code/OpenELIS-Global-2

# 2. Clean up old Spec Kit artifacts (if restarting)
rm -rf specs/ memory/ scripts/
# Note: .specify/ and .cursor/ are protected - will remain

# 3. Initialize Spec Kit (type 'y' when prompted)
specify init . --ai cursor-agent

# 4. Verify structure
ls -la | grep -E "(specs|memory|scripts|\.specify)"
# Should show: specs/, memory/, scripts/, .specify/

# 5. Check template constitution (will be small ~50 lines)
wc -l memory/constitution.md
```

---

## Cursor Chat Commands (Copy-Paste These)

### Step 1: Create Constitution

````
/speckit.constitution

Create a comprehensive constitution for OpenELIS Global 3.0 defining all technical constraints and development patterns.

## Project Overview
OpenELIS Global is an enterprise-level Laboratory Information System (LIS) for public health laboratories in low- and middle-income countries. Built by I-TECH at University of Washington in partnership with CDC and Global Fund implementers.

Key Challenge: Support country-specific variations (accession number formats, phone formats, patient identifiers, address fields) WITHOUT code fragmentation. Solution: Configuration-driven variation, unified codebase.

## Tech Stack (Non-Negotiable)

### Backend
- **Java 21** (LTS, Maven release target)
- **Spring Boot 3.x** (Spring Framework 6.2.2, Spring Security 6.0.4)
- **Hibernate 6.x** (Hibernate 5.6.15.Final, Hibernate Validator 8.0.2)
- **JPA** for ORM (NO JDBC queries, NO native SQL in code)
- **PostgreSQL 14+** (production database)
- **Liquibase** for schema migrations (NO direct DDL/DML)
- **HAPI FHIR R4** (version 6.6.2, co-habitant FHIR server)
- **Maven** build system (pom.xml)
- **Spotless** code formatter (`tools/OpenELIS_java_formatter.xml`)
- **Tomcat 9** application server
- **JUnit 5 + Mockito** for testing
- **Docker** containerization

### Frontend
- **React 17** (react-scripts 5.0.1)
- **Carbon Design System v1.15** (@carbon/react v1.15.0) - OFFICIAL UI FRAMEWORK (adopted August 2024)
- **Carbon Icons** (@carbon/icons-react v11.17.0)
- **Carbon Charts** (@carbon/charts-react v1.5.2) for data visualization
- **SWR 2.0.3** for data fetching and caching
- **React Router DOM 5.2.0** for routing
- **React Intl 5.20.12** for internationalization
- **Formik 2.2.9** for forms
- **Yup 0.29.2** for validation
- **Sass 1.54.3** for styling
- **Prettier 3.4.2** for formatting
- **ESLint 8.48.0** for linting
- **Cypress 12.17.3** for E2E testing
- **Jest + React Testing Library** for unit testing

### FHIR Integration
- **HAPI FHIR R4 Server** (co-habitant at `https://fhir.openelis.org:8443/fhir/`)
- **IHE mCSD Profile** for Location/Organization resources
- **Consolidated Server** with Shared Health Record (SHR) and International Patient Summary (IPS)
- **OpenMRS 3.x Integration** via Lab on FHIR module
- **Facility Registry Sync** (GoFR, OpenHIM)

### Deployment
- **Docker Compose** multi-container orchestration
- **PostgreSQL** database container
- **HAPI FHIR** server container
- **Nginx** reverse proxy
- **Ubuntu 20.04+** host OS

## Architectural Patterns (Strict Compliance Required)

### Backend Layer Structure

**Package Pattern**: `org.openelisglobal.{module}.{layer}`
Example: `org.openelisglobal.storage.valueholder`, `org.openelisglobal.storage.service`

**Layers**:
1. **Valueholders** (entities): JPA entities mapping to database tables
   - Extend `BaseObject<String>` (provides id, sys_user_id, lastupdated)
   - Include `fhir_uuid UUID` column for all FHIR-mapped entities
   - Use Hibernate XML mappings in `src/main/resources/hibernate/hbm/*.hbm.xml`
   - Validation annotations on fields (`@NotNull`, `@Size`, etc.)
   - ID generation via `@GenericGenerator` with sequence name

2. **DAOs** (data access): Database CRUD operations
   - Interface extends base DAO interface
   - Implementation extends `BaseDAOImpl<Entity, String>`
   - Annotate with `@Component` and `@Transactional`
   - Methods: get, insert, update, delete, custom queries
   - Use HQL (Hibernate Query Language), NOT native SQL

3. **Services** (business logic): Workflows and transactions
   - Interface + Implementation pattern
   - Implementation annotated with `@Service` and `@Transactional`
   - Transactions start here (NOT in controllers)
   - Call DAOs for persistence, FHIR services for sync
   - Validation logic before persistence
   - Logging via `LogEvent.logError()` for errors

4. **Controllers** (REST endpoints): HTTP request handling
   - Extend `BaseRestController`
   - Annotate with `@RestController` and `@RequestMapping("/rest/{module}")`
   - Methods: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
   - Controllers are singletons - NO class-level variables (thread safety)
   - Responsibilities: Form validation, request/response mapping, service calls
   - NOT for business logic (delegate to services)

5. **Forms/DTOs**: Data transfer objects
   - Simple beans for client ↔ server communication
   - Validation annotations here validate client input

**Example Entity**:
```java
@Entity
@Table(name = "storage_box")
public class StorageBox extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "storage_box_seq")
    @GenericGenerator(name = "storage_box_seq", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator",
        parameters = {@Parameter(name = "sequence_name", value = "storage_box_seq")})
    private String id;

    @Column(name = "fhir_uuid", unique = true, nullable = false)
    private UUID fhirUuid;  // REQUIRED for FHIR-mapped entities

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rack_id", nullable = false)
    private StorageRack rack;

    @PrePersist
    public void generateFhirUuid() {
        if (this.fhirUuid == null) {
            this.fhirUuid = UUID.randomUUID();
        }
    }
}
````

### Frontend Architecture

**Component Pattern**: Functional components with hooks (NO class components)

**File Structure**:

```
frontend/src/components/{module}/
├── ComponentName.jsx          # Component
├── ComponentName.scss         # Styling (Carbon tokens)
├── ComponentName.test.js      # Jest unit tests
└── {module}Api.js             # API utilities
```

**React Patterns**:

- Functional components only (`const Component = () => {}`)
- Hooks: `useState`, `useEffect`, `useContext`, `useMemo`, `useCallback`
- PropTypes for type validation (NOT TypeScript, though .tsx files exist)
- SWR for data fetching:
  `const { data, error } = useSWR('/api/endpoint', fetcher)`
- Formik for forms with Yup validation
- React Intl for i18n (`useIntl()`, `<FormattedMessage id="key" />`)

**Carbon Design System Requirements**:

- **EXCLUSIVELY** use `@carbon/react` components
- **NEVER** use Material-UI, Ant Design, Bootstrap, Tailwind, or custom CSS
  frameworks
- Import components:
  `import { Button, DataTable, Grid, Column } from '@carbon/react'`
- Import icons: `import { Add, Edit, Delete } from '@carbon/icons-react'`
- Styling ONLY via Carbon SCSS tokens: `@import '@carbon/react/scss/spacing'`

**Example Component**:

```jsx
import React, { useState } from "react";
import PropTypes from "prop-types";
import { Dropdown, Button, Grid, Column } from "@carbon/react";
import { Add } from "@carbon/icons-react";
import "./ComponentName.scss";

const ComponentName = ({ value, onChange }) => {
  const [state, setState] = useState(value);

  return (
    <Grid narrow>
      <Column lg={16}>
        <Dropdown
          id="example"
          items={[]}
          onChange={({ selectedItem }) => onChange(selectedItem)}
        />
        <Button kind="secondary" renderIcon={Add}>
          Add Item
        </Button>
      </Column>
    </Grid>
  );
};

ComponentName.propTypes = {
  value: PropTypes.string,
  onChange: PropTypes.func.isRequired,
};

export default ComponentName;
```

**Styling Pattern**:

```scss
@import "@carbon/react/scss/spacing";
@import "@carbon/react/scss/type";
@import "@carbon/react/scss/colors";

.component-name {
  padding: $spacing-05; // Carbon token, NOT hardcoded px
  @include type-style("body-short-01"); // Carbon typography
  color: $text-primary; // Carbon color token
}
```

### FHIR Integration Patterns

**Existing Services** (leverage, don't rebuild):

1. **FhirPersistanceService**: Create/update FHIR resources in local store

   ```java
   @Autowired
   private FhirPersistanceService fhirPersistanceService;

   // After entity insert/update
   fhirPersistanceService.createFhirResourceInFhirStore(fhirLocation);
   ```

2. **FhirTransformService**: Bidirectional OpenELIS ↔ FHIR R4 conversion

   - Pattern: Create `{Module}FhirTransformService` following this pattern
   - Example: `StorageLocationFhirTransformService.transformBoxToFhirLocation()`

3. **OrganizationImportServiceImpl**: Imports FHIR Location/Organization from
   external registries
   - Method: `importLocationsFromBundle(IGenericClient, List<Bundle>)`
   - Pattern for facility registry synchronization

**FHIR Sync Pattern**:

```java
@Service
public class StorageBoxServiceImpl implements StorageBoxService {
    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Autowired
    private StorageLocationFhirTransformService fhirTransformService;

    @Override
    @Transactional
    public String insert(StorageBox box) {
        String id = storageBoxDAO.insert(box);

        // Sync to FHIR (secondary - don't fail if FHIR sync fails)
        try {
            org.hl7.fhir.r4.model.Location fhirLocation = fhirTransformService.transformBoxToFhirLocation(box);
            fhirPersistanceService.createFhirResourceInFhirStore(fhirLocation);
        } catch (FhirLocalPersistingException e) {
            LogEvent.logError("Failed to sync to FHIR", e);
            // Continue - don't fail the transaction
        }

        return id;
    }
}
```

## Code Standards (Enforced via CI/CD)

### Backend Java

**Formatting** (`tools/OpenELIS_java_formatter.xml`):

```bash
mvn spotless:apply     # Auto-format code
mvn spotless:check     # Verify formatting (CI check)
```

**Build**:

```bash
mvn clean install -DskipTests          # Build only
mvn clean install                      # Build + tests
mvn verify -Dit.test={TestClassName}   # Integration tests
```

**Testing Requirements**:

- **Minimum 80% code coverage** (Jacoco enforced in CI)
- JUnit 5 + Mockito for unit tests
- Integration tests for service layer (`@SpringBootTest`)
- Test naming: `{Class}Test.java` in `src/test/java/`

**Validation**:

- **Tight validation on all user input** (security requirement)
- String data type most vulnerable - use whitelists when possible
- Validation annotations on valueholders and forms
- Escaping context-aware (XML, HTML, JavaScript)

### Frontend React

**Formatting** (`.prettierrc.json`):

```bash
npm run format         # Auto-format code
npm run check-format   # Verify formatting (CI check)
```

**Linting** (`.eslintrc.js`):

```bash
npm run lint           # Run ESLint
```

**Testing**:

```bash
npm test               # Jest unit tests
npm run cy:run         # Cypress E2E (headless)
npm run cy:open        # Cypress E2E (interactive)
```

**Prettier Config**:

- Double quotes (NOT single quotes)
- Spaces (NOT tabs), 2-space indentation
- Semicolons required
- 80 character line width

**ESLint Rules**:

- No unused vars (warn)
- React prop-types off (but use PropTypes anyway)
- React hooks exhaustive-deps off (but handle deps manually)
- Prettier warnings

### Database Migrations

**Liquibase ONLY** (NO direct SQL in code):

**Location**: `src/main/resources/liquibase/changelog/`

**Pattern**:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog ...>
  <changeSet id="create_storage_box" author="developer-name">
    <createTable tableName="storage_box">
      <column name="id" type="varchar(10)">
        <constraints primaryKey="true"/>
      </column>
      <column name="fhir_uuid" type="uuid">
        <constraints unique="true" nullable="false"/>
      </column>
      <!-- ... -->
    </createTable>

    <createSequence sequenceName="storage_box_seq" startValue="1"/>
  </changeSet>
</databaseChangeLog>
```

**Include in master**:

```xml
<!-- src/main/resources/liquibase/changelog/master-changelog.xml -->
<include file="changelog/storage/001_create_storage_box.xml"/>
```

## Non-Negotiable Constraints

### Security & Compliance

- **CSRF Protection**: Enabled globally (configured in `SecurityConfig`)
- **Input Validation**: Whitelist approach for all user input
- **String Sanitization**: Context-aware escaping (XML, HTML, JS)
- **Audit Trails**: Immutable logs (REVOKE UPDATE/DELETE permissions)
- **Role-Based Access Control** (RBAC): Spring Security roles
- **FDA 21 CFR Part 11**: Electronic signatures, audit trails
- **WCAG 2.1 AA**: Accessibility compliance

### Architecture

- **No ORM Bypass**: Hibernate only, NO raw JDBC
- **No Direct SQL**: Liquibase migrations only
- **Transactional Boundaries**: `@Transactional` on service methods (NOT
  controllers)
- **Singleton Controllers**: NO class-level mutable state (thread safety)
- **Service Interface Pattern**: Services as interfaces + implementations
  (Spring AOP)

### UI/UX

- **Carbon Design System EXCLUSIVELY**: No other CSS frameworks
- **Functional Components Only**: NO class components
- **Accessibility First**: Keyboard navigation, screen reader support, ARIA
  labels
- **Responsive Design**: Mobile-first, works on any Chromium-based browser
- **Internationalization**: All user-facing text via React Intl

### FHIR

- **Sync on Mutations**: All entity create/update triggers FHIR sync
- **Don't Fail Transactions**: FHIR sync errors logged, transaction continues
- **IHE Profiles**: Follow mCSD for Location/Organization
- **Resource Subscriptions**: Task, Patient, ServiceRequest, DiagnosticReport,
  Observation, Specimen, Practitioner, Encounter

## Development Workflow

### Before Commit Checklist

- [ ] `mvn spotless:apply` (backend)
- [ ] `npm run format` (frontend)
- [ ] `mvn test` passes (backend)
- [ ] `npm test` passes (frontend)
- [ ] No hardcoded paths or credentials
- [ ] No modifications to `.gitignore`d files
- [ ] PropTypes defined for all components
- [ ] FHIR sync implemented for entity mutations

### Pull Request Requirements (per PULL_REQUEST_TIPS.md)

- **Branch Naming**: `issue-{number}` (NOT Spec Kit branch naming for PRs)
- **PR Title**: `issue-{number}: {summary}`
- **Target Branch**: `develop` (NOT main/master)
- **Clean Commits**: Focused changes, no unrelated edits
- **Screenshots**: Required for UI changes (before/after)
- **Rebase**: `git pull --rebase upstream develop` before PR
- **Code Review**: Wait for approval before merge
- **Squash**: Reviewers handle final squash on merge

## File Organization Standards

### Backend

- **Controllers**: `src/main/java/org/openelisglobal/{module}/controller/rest/`
- **Services**: `src/main/java/org/openelisglobal/{module}/service/`
- **DAOs**: `src/main/java/org/openelisglobal/{module}/dao/` and `daoimpl/`
- **Valueholders**: `src/main/java/org/openelisglobal/{module}/valueholder/`
- **FHIR**: `src/main/java/org/openelisglobal/{module}/fhir/service/`
- **Hibernate Mappings**: `src/main/resources/hibernate/hbm/{Entity}.hbm.xml`
- **Liquibase**: `src/main/resources/liquibase/changelog/{module}/`

### Frontend

- **Components**: `frontend/src/components/{module}/`
- **Styles**: Co-located `{Component}.scss` using Carbon tokens
- **Tests**: Co-located `{Component}.test.js`
- **API Utilities**: `{module}Api.js` for data fetching
- **Internationalization**: `frontend/src/languages/{locale}.json`

## Performance Requirements

### Backend

- **API Response Time**: <500ms for standard queries
- **Dashboard Load Time**: <2s with 100k records
- **Database Indexing**: All foreign keys, frequently queried columns
- **Query Optimization**: Use JPA Criteria API or HQL, avoid N+1 queries

### Frontend

- **Bundle Size**: Code splitting, lazy loading routes
- **Rendering**: Memoization for expensive computations (`useMemo`,
  `useCallback`)
- **Data Fetching**: SWR caching, revalidation strategies
- **Accessibility**: Lighthouse score ≥90

## Testing Strategy

### Backend Testing

- **Unit Tests**: JUnit 5 + Mockito, test services in isolation
- **Integration Tests**: `@SpringBootTest`, test full request flow
- **Coverage**: Minimum 80% (Jacoco report in CI)
- **Test Data**: Use builders/factories, NOT hardcoded values
- **Assertions**: AssertJ for fluent assertions

### Frontend Testing

- **Unit Tests**: Jest + React Testing Library, test components in isolation
- **E2E Tests**: Cypress, test full user workflows
- **Visual Regression**: Cypress screenshot comparisons
- **Accessibility Tests**: Axe-core integration
- **Coverage**: Aim for >70% (not enforced)

## Configuration Management

### Environment-Specific Config

- **Development**: `dev/properties/eclipse_common.properties` (Eclipse),
  `volume/properties/common.properties` (Docker)
- **Production**: `volume/properties/common.properties` (mounted as secret)
- **NEVER Commit**: Local paths, credentials, machine-specific config
- **Symlinks for Dev**: Point `/run/secrets/` to local `dev/properties/`

### FHIR Server Config (`volume/properties/common.properties`):

```properties
# Local FHIR store
org.openelisglobal.fhirstore.uri=https://fhir.openelis.org:8443/fhir/

# Facility registry
org.openelisglobal.facilitylist.fhirstore={url}
org.openelisglobal.facilitylist.auth=token

# FHIR subscriber (consolidated server)
org.openelisglobal.fhir.subscriber={url}
org.openelisglobal.fhir.subscriber.resources=Task,Patient,ServiceRequest,DiagnosticReport,Observation,Specimen,Practitioner,Encounter
```

## Common Anti-Patterns to Avoid

### Backend

- ❌ Native SQL queries in code (use HQL or Criteria API)
- ❌ Direct JDBC connections (use Hibernate/JPA)
- ❌ Business logic in controllers (put in services)
- ❌ Class-level mutable state in controllers (thread safety)
- ❌ Catching exceptions without logging (`LogEvent.logError()`)
- ❌ Skipping `@Transactional` on service methods
- ❌ Forgetting `fhir_uuid` column on FHIR-mapped entities

### Frontend

- ❌ Class components (use functional)
- ❌ Direct DOM manipulation (React state only)
- ❌ Non-Carbon components (Material-UI, custom frameworks)
- ❌ Hardcoded px values (use Carbon $spacing-\*)
- ❌ Hardcoded colors (use Carbon $text-_, $layer-_)
- ❌ Missing PropTypes
- ❌ Missing accessibility attributes (ARIA labels, roles)

### Database

- ❌ Direct DDL/DML in code (use Liquibase)
- ❌ Missing foreign key constraints
- ❌ Missing indexes on frequently queried columns
- ❌ Mutable audit logs (use immutable design)

## References

- **OpenELIS Docs**:
  [docs.openelis-global.org](https://docs.openelis-global.org)
- **Dev Setup**: `docs/dev_setup.md`, `docs/dev.md`
- **Carbon Design**: [carbondesignsystem.com](https://carbondesignsystem.com)
- **FHIR R4**: [hl7.org/fhir/R4/](https://hl7.org/fhir/R4/)
- **IHE mCSD**:
  [build.fhir.org/ig/IHE/ITI.mCSD/](https://build.fhir.org/ig/IHE/ITI.mCSD/)
- **Pull Request Guide**: `PULL_REQUEST_TIPS.md`
- **Code of Conduct**: `CODE_OF_CONDUCT.md`

```

---

## NOW Run These in Cursor Chat (Sequential Order)

Copy the full `/speckit.constitution` command above, then proceed with:

**Step 2**: `/speckit.specify` (see full prompt in `.dev-docs/spec-driven-development-guide.md` line 1214)

**Step 3**: `/speckit.clarify` (line 1248)

**Step 4**: `/speckit.plan` (line 1278)

**Step 5**: `/speckit.tasks` (line 1314)

**Step 6**: `/speckit.implement` (line 1327)

```
