# Feature Specification: Microbiology Reference and Breakpoint Administration

**Feature Branch**: `feat/782-ogc-782-microbiology-m9-reference-mapping-admin`
**Created**: 2026-08-04
**Status**: Implemented and deployed; automated validation complete; human UAT
pending
**Parent**: OGC-782
**Primary Jira**: OGC-786, OGC-787

## Product Boundary

This milestone gives authorized laboratory administrators a dependable way to
maintain the microbiology reference information already consumed by culture and
AST workflows. It does not prescribe tables, services, routes, component
structure, or import internals.

Source artifacts are evidence of workflow intent, not implementation contracts:

- [M-01 AMR Reference Data](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-01-amr-reference-data.md)
- [M-02 Breakpoint Catalog](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-02-breakpoint-catalog.md)
- [M-01 Organism Master visual mock](https://digi-uw.github.io/openelis-work/designs/microbiology/m-01-organism-master.html)
- [M-02 Breakpoint Catalog visual mock](https://digi-uw.github.io/openelis-work/designs/microbiology/m-02-breakpoint-catalog.html)
- [Microbiology workflow narrative](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1315209256)
- OGC-854 through OGC-864, with OGC-916 explicitly excluded from this milestone

## User Stories

### US1 - Maintain organism and antibiotic vocabularies (P1)

As a laboratory manager, I can find, add, correct, activate, and deactivate the
organisms and antibiotics used by the laboratory so bench workflows use a
single, current vocabulary.

**Acceptance scenarios**

1. Searching or filtering either vocabulary changes the bookmarkable page state
   and a reload produces the same rows.
2. Saving rejects missing names, invalid or duplicate WHONET codes, and names
   that duplicate an existing entry.
3. Deactivation explains the downstream effect, removes the entry from new
   workflow selections, and preserves historical cases and results.
4. Re-activation restores the entry to new workflow selections.
5. Organism records show the default AST panel and initial significance used as
   workflow suggestions; antibiotic records show class and route.

### US2 - Maintain versioned AST panels (P1)

As a laboratory manager, I can define which antibiotics belong to an AST panel,
their order and reporting tier, and publish a new panel version without changing
the panel used by an existing AST run.

**Acceptance scenarios**

1. A panel contains an ordered set of active antibiotics with an explicit tier
   and report behavior.
2. Changing a published panel creates a visibly newer version after confirmation.
3. New AST setup offers the current version; existing runs retain their original
   panel version and readings.
4. A panel cannot be deactivated without an impact warning; historical runs
   remain readable after deactivation.

### US3 - Manage culture workflow defaults (P1)

As a laboratory manager, I can review and maintain the culture instructions
associated with an existing laboratory Method so case setup receives consistent
media, incubation, and atmosphere defaults.

**Acceptance scenarios**

1. Culture configuration is attached to an existing Method rather than creating
   a competing method vocabulary.
2. An administrator can maintain media, incubation, and atmosphere guidance and
   see which microbiology workflow consumes it.
3. A culture order continues to resolve its default Method using the existing
   Test Catalog relationship.

### US4 - Browse and activate breakpoint standards (P1)

As a laboratory manager, I can inspect loaded breakpoint standards and their
rules, understand which version is active, and activate a different loaded
version with an effective date.

**Acceptance scenarios**

1. The catalog distinguishes Active, Loaded, and Archived standards and explains
   that historical AST runs retain their original standard.
2. Standard detail supports bookmarkable search and filters for organism/group,
   antibiotic, method, and specimen context.
3. At most one standard per publisher is active for new runs.
4. Activation requires an effective date, identifies the authenticated actor,
   and does not recalculate prior AST readings.
5. Archived standards remain readable but cannot be selected for new runs.

### US5 - Import breakpoint updates safely (P1)

As a laboratory manager, I can upload a breakpoint CSV, review row-level
validation, and apply valid rows without losing local corrections.

**Acceptance scenarios**

1. Import validates all rows before applying changes and reports each rejected
   row with its row number and actionable reason.
2. Valid rows can be applied while rejected rows remain unapplied, and rejected
   rows can be downloaded for correction.
3. Unknown organisms, groups, antibiotics, methods, malformed thresholds, and
   duplicate rule identities are reported deterministically.
4. Re-import is idempotent for unchanged imported rows.
5. Locally corrected rules are visibly marked and are never silently overwritten.
6. OpenELIS does not distribute proprietary CLSI/EUCAST breakpoint content as
   test or demo seed data; UAT uses clearly labeled synthetic values.

### US6 - Navigate the administration workflow (P1)

As an authorized reviewer, I can reach each reference surface from Admin
navigation and follow linkable breadcrumbs.

**Acceptance scenarios**

1. Organisms, antibiotics, AST panels, culture methods, and breakpoint standards
   each have a stable URL and an Admin breadcrumb path.
2. Search, filters, selected standard, sort, page, and page size survive reload
   and browser Back/Forward.

## Functional Requirements

- **FR-001**: Authorized administrators MUST be able to create and update active
  organism and antibiotic records and deactivate/reactivate them safely.
- **FR-002**: New workflow pickers MUST return active reference entries only;
  historical views MUST resolve inactive referenced entries.
- **FR-003**: WHONET codes MUST be normalized and unique when present.
- **FR-004**: Published AST panel changes MUST create a new version and MUST NOT
  mutate a version referenced by an existing AST run.
- **FR-005**: Culture configuration MUST reuse the existing Method identity and
  Test Catalog default-Method behavior.
- **FR-006**: Breakpoint standards MUST support Active, Loaded, and Archived
  lifecycle states, with at most one Active standard per publisher.
- **FR-007**: Activation MUST retain effective date and authenticated actor and
  MUST NOT change historical interpretations.
- **FR-008**: CSV import MUST provide deterministic row-level validation,
  idempotent application, and protection for local corrections.
- **FR-009**: Every admin list/detail state MUST be representable by its URL.
- **FR-010**: All user-facing text MUST be internationalized and all controls
  MUST use Carbon components and interaction semantics.
- **FR-011**: Test and UAT data MUST be provisioned through application services;
  no direct SQL, fixed primary keys, or DAO bypass is permitted.
- **FR-012**: Actor identity MUST come from the authenticated request, not a
  client-supplied identifier.

## Explicit Exclusions

- WHONET file generation, deduplication, export history, and scheduling
- WHO-TB critical-concentration standards and operational TB DST
- Catalog Subscription / remote publisher synchronization
- Expert-rule execution and intrinsic-resistance inference
- Bundled clinical breakpoint datasets
- Hard deletion of referenced clinical vocabulary

## Success Criteria

- **SC-001**: A reviewer can reach every M3 administration surface from Admin
  navigation without pasting a route.
- **SC-002**: Reloading any filtered list or breakpoint detail reproduces the
  same visible state.
- **SC-003**: Existing AST runs retain their selected panel and breakpoint
  standard after newer versions are published or activated.
- **SC-004**: A mixed-validity CSV produces deterministic imported/skipped
  counts and the expected row-specific errors.
- **SC-005**: Focused backend, ORM, Liquibase update/rollback, component,
  accessibility, and Playwright evidence passes against the deployed SHA.
