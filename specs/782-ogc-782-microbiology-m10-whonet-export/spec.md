# Feature Specification: OGC-782 M4 WHONET Manual Export

**Feature Branch**: `feat/782-ogc-782-microbiology-m10-whonet-export`  
**Jira**: [OGC-794](https://uwdigi.atlassian.net/browse/OGC-794), [OGC-878](https://uwdigi.atlassian.net/browse/OGC-878), [OGC-880](https://uwdigi.atlassian.net/browse/OGC-880)  
**Parent**: [OGC-782 Microbiology MVP](../782-ogc-782-microbiology-mvp-spec/spec.md)  
**Design intent**: [M-09 WHONET Export](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-09-whonet-export.md), [painless workflow mock](https://digi-uw.github.io/openelis-work/designs/microbiology/m-09-whonet-painless-prototype.html)

## Scope

This milestone lets an authorized laboratory user preview and download the
current OpenELIS surveillance CSV candidate from finalized routine bacteriology
cases. It is the first usable export slice after the M1-M3 clinical and
reference-data foundation; acceptance as a complete WHONET import profile is not
claimed in this milestone.

The milestone includes manual CSV export, scoped readiness, direct repair links
for organism and antibiotic mapping gaps, a deterministic seven-day
first-isolate option, and an audit record for each generated file.

The milestone does not include scheduled delivery, SFTP or email, TXT output,
lab-profile packaging, TB export, phenotype columns, national aggregation,
GLASS submission, or a claim that the local seven-day rule is a complete WHO or
CLSI implementation.

## User Stories

### US1 - Preview a routine export (Priority: P1)

As a surveillance or laboratory reporting user, I can choose a reporting period
and inclusion policy and preview what will be exported before creating a file.

**Acceptance scenarios**

1. Opening the export page defaults to the previous complete calendar month,
   clinically significant isolates, and the seven-day first-isolate option.
2. Preview reports the finalized cases and isolates found, the counts remaining
   after significance and de-duplication, and the rows eligible for export.
3. Reloading the canonical URL reproduces the selected period, inclusion policy,
   de-duplication policy, preview page, and page size.
4. Only finalized routine bacteriology cases in the selected period contribute
   export rows.

### US2 - Repair mapping gaps in context (Priority: P1)

As a laboratory administrator, I can see mapping gaps in the selected export
set and go directly to the affected reference item.

**Acceptance scenarios**

1. Readiness is calculated only from organisms and antibiotics used by the
   selected export set, not the whole reference catalog.
2. Each unmapped item names its vocabulary and affected item and offers a direct
   repair link.
3. Returning from reference administration restores the export preview URL.
4. Unmapped rows are excluded with a warning. Generation remains available when
   at least one valid row remains and is blocked when no valid rows remain.

### US3 - Generate and audit a CSV (Priority: P1)

As a surveillance or laboratory reporting user, I can generate a CSV from the
validated preview and retain evidence of what was generated.

**Acceptance scenarios**

1. Generate downloads a consistently named CSV containing the same eligible
   rows and policy shown in preview.
2. Patient and specimen values containing commas, quotes, or line breaks remain
   valid CSV fields.
3. Each generated file creates an immutable audit summary containing the period,
   policy, counts, filename, content fingerprint, authenticated actor, and time.
4. A submitted actor identifier cannot replace the authenticated actor.

### US4 - Use a clear, accessible workflow (Priority: P1)

As a keyboard or assistive-technology user, I can understand and complete the
Configure, Preview, and Generate workflow.

**Acceptance scenarios**

1. The page has a linkable Dashboard-to-Reports-to-WHONET breadcrumb path and a
   Microbiology navigation entry.
2. Date, inclusion, preview, mapping repair, generation, table, and pagination
   controls have accessible names and logical keyboard order.
3. Preview and generation status are announced without moving focus
   unexpectedly.
4. Desktop and mobile layouts preserve the workflow order and do not overlap or
   hide controls.

## Functional Requirements

- **FR-001**: The system MUST select finalized routine bacteriology cases by a
  user-visible reporting period.
- **FR-002**: The system MUST default to the previous complete calendar month
  and clinically significant isolates.
- **FR-003**: The system MUST offer no de-duplication and a deterministic
  seven-day first-isolate policy without presenting that local policy as a
  verified WHO or CLSI implementation.
- **FR-004**: Preview MUST show counts before and after inclusion,
  de-duplication, mapping validation, and row generation.
- **FR-005**: Mapping readiness MUST use only reference items present in the
  selected export set.
- **FR-006**: Mapping warnings MUST identify the exact organism or antibiotic
  and provide a direct repair path.
- **FR-007**: Generation MUST be blocked when no valid export row remains.
- **FR-008**: The generated CSV MUST preserve the existing OpenELIS WHONET
  column contract while including reviewed, reportable microbiology AST data.
- **FR-009**: The downloaded filename MUST identify the reporting period and be
  safe for common filesystems.
- **FR-010**: Every successful generation MUST be attributable to the
  authenticated user and retain a content fingerprint and summary counts.
- **FR-011**: User-visible state MUST be bookmarkable and canonical.
- **FR-012**: All user-visible text MUST be internationalized and the workflow
  MUST use established Carbon components and accessibility behavior.

## Clarifications And Deferred Decisions

- The M-09 mock labels its seven-day default “WHO GLASS standard.” This
  milestone uses a deterministic local first-isolate rule and does not make that
  standards claim until the exact current guidance is verified.
- The M-09 and OGC-794 artifacts prescribe tables, routes, column sets,
  sub-pages, scheduling, and delivery mechanisms. Those are engineering ideas,
  not product requirements; this milestone carries only the user-visible
  outcomes above.
- Specimen, origin, patient-type, department, breakpoint-standard, and phenotype
  mapping are not silently invented here. They remain later work because the
  current M3 foundation has authoritative organism and antibiotic mappings but
  not settled ownership for every additional vocabulary.
- The generated file preserves the existing 15-column OpenELIS long-format
  export contract. Import validation against current WHONET, wide-format
  antibiotic columns, method suffixes, and profile packaging remain explicit
  follow-on acceptance work.
- OGC-879 and OGC-881 are marked Done/superseded in Jira, while the current repo
  contains readiness only and no generated microbiology export history. Repo
  behavior is authoritative for implementation status.

## Success Criteria

- **SC-001**: A seeded finalized case with one mapped isolate and two reviewed
  AST readings previews and downloads exactly two CSV data rows.
- **SC-002**: An unmapped item produces an exact repair link and is excluded;
  mapped rows in the same preview can still be generated.
- **SC-003**: Reopening a canonical preview URL reproduces the same filters,
  counts, page, and visible rows.
- **SC-004**: Focused service, controller, ORM, Liquibase update/rollback,
  frontend, accessibility, and Playwright tests pass without arbitrary waits.
