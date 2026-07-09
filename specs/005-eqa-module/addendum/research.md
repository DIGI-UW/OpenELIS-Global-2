# Research: EQA Enrollment & Navigation Addendum

**Feature**: 005-eqa-module/addendum **Date**: 2026-02-24 **Status**: Complete
**Addendum**: eqa-enrollment-addendum-v3.md

## Research Summary

All technical unknowns from the addendum have been resolved through codebase
exploration. The addendum introduces 4 new data models, navigation
restructuring, and 6 new pages/components.

---

## R-A1: EQA Program Provider Field

**Question**: Does the `eqa_program` table already have a `provider` field
(DM-002)?

**Decision**: Already implemented - no changes needed.

**Rationale**:

- `EQAProgram.java` already has
  `@Column(name = "provider_name", length = 255) private String providerName;`
- The addendum's DM-002 calls for `provider varchar(255) NOT NULL` which maps to
  the existing `provider_name` column
- Typeahead suggestions for provider names can be built from a union query on
  `eqa_program.provider_name` + `eqa_lab_program_enrollment.provider`

**Alternatives Considered**: None needed - field already exists.

---

## R-A2: Self-Enrollment vs Provider Enrollment Architecture

**Question**: How should self-enrollment (My Programs) relate to provider-side
enrollment (Participants)?

**Decision**: Two separate tables with distinct semantics

**Rationale**:

- **Self-enrollment** (`eqa_lab_program_enrollment`): The local lab enrolls
  itself in external programs. Records are local-only, not linked to
  `eqa_program`. Has test/panel mapping for Order Entry pre-population.
- **Provider enrollment** (`eqa_program_enrollment`): The local lab enrolls
  other organizations in programs it manages (linked to `eqa_program`). Tracks
  participant lifecycle (Active → Suspended → Withdrawn).
- These are conceptually different workflows:
  - Self-enrollment = "Which external programs does our lab participate in?"
  - Provider enrollment = "Which organizations participate in programs we
    distribute?"
- The existing `ParticipantsTab.js` component in `EQAProgram/` handles some
  provider-side enrollment already but needs to be extended for the addendum's
  full status lifecycle.

**Alternatives Considered**:

- Single table with `enrollment_type` discriminator: Rejected because the two
  types have very different fields (self-enrollment has test mappings, provider
  enrollment has status lifecycle + withdrawal reason).

---

## R-A3: Navigation Restructure Strategy

**Question**: How to restructure navigation from current flat EQA menu to the
addendum's two-parent structure?

**Decision**: Liquibase migration to restructure `clinlims.menu` table entries

**Rationale**:

- Current menu structure (from `eqa-007-add-eqa-menu-items.xml`):
  ```
  menu_eqa (parent, order 35)
  ├── menu_alerts (order 1) → /Alerts
  ├── menu_eqa_management (order 2) → /EQAManagement
  └── menu_eqa_distribution (order 3) → /EQADistribution
  ```
- Target structure (from addendum):
  ```
  menu_eqa_tests (parent, order 35) — "EQA Tests"
  ├── menu_eqa_orders (order 1) → /EQAOrders
  └── menu_eqa_my_programs (order 2) → /EQAMyPrograms
  menu_eqa_mgmt (parent, order 36) — "EQA Management"
  ├── menu_eqa_mgmt_programs (order 1) → /EQAManagement
  ├── menu_eqa_mgmt_participants (order 2) → /EQAParticipants
  ├── menu_eqa_distribution (order 3) → /EQADistribution
  └── menu_eqa_results (order 4) → /EQAResults
  menu_alerts (top-level, order 38) — "Alerts" (moved out of EQA parent)
  ```
- Approach: New Liquibase changeset that:
  1. Deactivates old `menu_eqa` parent item
  2. Creates `menu_eqa_tests` and `menu_eqa_mgmt` parent items
  3. Moves/creates child items under new parents
  4. Moves `menu_alerts` to top-level (removes parent_id)
- Frontend routes added in `App.js` for new paths

**Alternatives Considered**:

- Modify existing entries in-place: Rejected because it makes rollback harder
  and risks breaking existing references.

---

## R-A4: Test/Panel Mapping for Self-Enrollment

**Question**: How should test and panel mappings work for self-enrollment in
Order Entry?

**Decision**: Join table `eqa_lab_enrollment_test_map` with nullable
`test_id`/`panel_id` columns

**Rationale**:

- DM-010 defines `eqa_lab_enrollment_test_map` with
  `CHECK (test_id IS NOT NULL OR panel_id IS NOT NULL)` - each row is either a
  test or a panel mapping
- When user selects an EQA program in Order Entry, frontend queries
  `/api/eqa/my-programs/{id}` to get mapped test/panel IDs
- Tests pre-populate the test selection UI (Step 3 of Add Sample) but remain
  overridable
- Lab unit mappings (`eqa_lab_enrollment_lab_unit`) pre-filter the lab unit
  selector dropdown
- Existing Order Entry code in `addOrder/Index.js` and
  `OrderEntryAdditionalQuestions.js` already has EQA-aware logic; needs
  extension for program selection from My Programs

**Alternatives Considered**:

- Separate `test_map` and `panel_map` tables: Rejected as unnecessarily complex
  for simple many-to-many relationships.

---

## R-A5: Organization Eligibility for Provider Enrollment

**Question**: Which organizations are eligible for EQA participant enrollment?

**Decision**: All active organizations from `clinlims.organization` table

**Rationale**:

- BR-014 states: "All active organizations in the Organizations table are
  eligible for EQA enrollment"
- No new entity needed; reference existing `organization.id` via FK
- The enrollment modal should show a searchable list of organizations with
  "Already Enrolled" and "This Lab" badges
- Local lab identified by matching `organization.id` to site configuration
  (`SiteInformationDAO`)

**Alternatives Considered**: None - straightforward reuse of existing entity.

---

## R-A6: Provider Typeahead Implementation

**Question**: How should the provider typeahead work across self-enrollment and
managed programs?

**Decision**: Dedicated endpoint returning union of provider names

**Rationale**:

- Addendum specifies `GET /api/eqa/providers` returning distinct provider names
- Query:
  `SELECT DISTINCT provider_name FROM eqa_program UNION SELECT DISTINCT provider FROM eqa_lab_program_enrollment`
- Frontend uses Carbon `TextInput` with custom typeahead dropdown (similar to
  existing `ComboBox` pattern)
- No new entity needed; purely a query aggregation

**Alternatives Considered**:

- Separate provider entity/table: Rejected per BR-012 which explicitly states
  providers are free-text attributes, not separate entities.

---

## R-A7: EQA Orders Page Data Source

**Question**: Where does the EQA Tests → Orders page get its data?

**Decision**: Query `sample_eqa` joined with `sample` table, filtered by
`is_eqa_sample = true`

**Rationale**:

- BR-016 states: "The EQA Tests → Orders page displays orders where
  `is_eqa_sample = true`"
- The existing `SampleEQA` entity already tracks all EQA-specific sample data
- Orders endpoint: `GET /api/eqa/orders` returns SampleEQA records joined with
  Sample for accession number, status, etc.
- Summary endpoint: `GET /api/eqa/orders/summary` returns counts grouped by
  status
- The existing `EQAManagementDashboard.js` already queries
  `/rest/eqa/samples/dashboard` which can be reused/extended

**Alternatives Considered**: None - straightforward query of existing tables.
