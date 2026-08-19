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
first-isolate option, and an audit record for each generated file. Follow-on
roadmap slices extend the same workflow one export vocabulary or delivery
behavior at a time.

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
- **FR-008**: The generated CSV MUST include the finalized isolate and reviewed,
  reportable AST information needed for the current validation workflow.
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
- M-09's tables, routes, and service/storage suggestions are not binding
  implementation constraints. Its user-visible date presets, export filters,
  advanced de-duplication choices, output choices, mapping workflow, history,
  and scheduled-delivery outcomes are functional requirements. They remain
  explicit follow-on slices where this first manual-export milestone does not
  yet implement them.
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

## Follow-on Story - Export Mapped Specimen Codes

As a surveillance user, I need each exported isolate to carry the configured
WHONET specimen code so that a locally named sample type does not produce an
ambiguous surveillance value.

**Acceptance scenarios**

1. Given a finalized routine-bacteriology case whose sample type has a WHONET
   specimen code, when the user previews and generates the export, then the
   preview and CSV use that code rather than the local display name.
2. Given a selected case whose sample type has no WHONET specimen code, when the
   user previews the export, then all rows for that isolate are excluded and one
   warning names the affected sample type and the number of excluded rows.
3. Given a mixed selected set, when one sample type is unmapped, then mapped
   rows remain available for generation and only affected rows are excluded.
4. The warning opens the affected sample type's existing administration
   workflow at its WHONET code control and provides a return path to the exact
   export preview.
5. After an administrator saves the missing code and returns, refreshing the
   preview clears the warning and includes the repaired rows without changing
   the other export settings.

**Requirements**

- **FR-013**: Specimen readiness MUST be evaluated only for sample types used by
  the selected export set.
- **FR-014**: A sample type MUST have one user-visible WHONET specimen code in
  its owning administration workflow; the export workflow MUST NOT introduce a
  parallel specimen-mapping catalog.
- **FR-015**: Missing specimen mapping MUST exclude only affected export rows
  and MUST block generation only when no valid row remains.
- **FR-016**: Mapping repair and return navigation MUST preserve the canonical
  export preview state and remain keyboard accessible.
