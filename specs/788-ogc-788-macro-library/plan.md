# Implementation Plan: OGC-788 Macro Library

## Technical Context

OGC-788 stacks on the deployed OGC-782 M4 WHONET branch. The repo has no
quick-text or macro capability. The existing `Dictionary` model represents
coded result vocabularies, while `Note` represents persisted authored notes;
neither is a reusable phrase definition. Reusing either would blur clinical
semantics and administration boundaries.

The first consumers already exist as Carbon text areas in
`CaseTimelinePanel.jsx` and `MicrobiologyOrderDetailFields.jsx`. The library is
therefore implemented as a cross-cutting module with a reusable frontend field
adapter, while microbiology supplies only the field context.

## Architecture Decisions

1. A dedicated phrase-definition entity owns code, expansion text, applicable
   contexts, active status, provenance, and audit metadata. It is not a
   microbiology case entity and it does not store expanded clinical text.
2. Contexts are controlled application values exposed as translatable labels.
   A phrase can apply to multiple contexts; adding a future consumer does not
   require a new phrase table or column.
3. The database enforces normalized code uniqueness. The service owns
   validation, filtering, authorization-compatible behavior, and authenticated
   audit attribution.
4. Read access is authenticated; administrative writes require the existing
   admin role. The write contract contains no actor field.
5. Runtime lookup returns only active phrases matching the supplied context and
   query. Administrative lookup supports active and inactive phrases with
   server-side search, filter, sort, and pagination.
6. A reusable Carbon-compatible text-area adapter owns token detection,
   suggestion state, keyboard behavior, replacement, and announcements. It
   remains controlled by the parent form and submits plain expanded text.
7. The canonical administration URL is selected by current router conventions,
   not by the mock. It preserves `q`, `context`, `status`, `sort`, `page`,
   `pageSize`, and the active create/edit action.
8. Runtime results are fetched on first focus/use after a supported form loads
   and may use SWR's normal request cache. Observable freshness on the next
   form load is required; custom cache infrastructure is not.
9. The property-gated UAT scenario service creates test phrases through the
   phrase service. No SQL fixture, DAO bypass, fixed persisted ID, or fixture
   migration is permitted.
10. Liquibase is used only for the phrase data model. Routes, UI, tests,
    fixtures, and navigation receive no migration.
11. M2 export is a deterministic UTF-8 CSV of the effective library, ordered by
    canonical code. Contexts and package provenance are preserved so the file is
    useful for review and future import without exposing audit actors or database
    identifiers.
12. M2 bulk administration is an explicit, confirmed operation over at most 100
    selected phrases. Activation and deactivation apply to local or packaged
    phrases; irreversible removal is limited to local phrases. The service loads
    and validates the complete selection before changing any row.
13. Package import remains blocked until its clinical source and collision policy
    are approved. Export and bulk administration use the M1 model and therefore
    require no Liquibase migration.

## Milestones

### M1 - Managed Runtime Vertical Slice

- Persist and audit phrase definitions.
- Admin create, edit, activate/deactivate, search, filter, sort, and pagination.
- Reusable keyboard-accessible expansion in culture activity note and clinical
  history. Antibiotic Exposure remains the authoritative binary order choice.
- Service-created UAT fixture with a small non-clinical demonstration set.
- Focused backend/frontend/Playwright/accessibility evidence, pinned code-qa,
  synchronized Grist steps, video, and exact-SHA phrases deployment.

M1 deliberately proves the end-to-end behavior without claiming the draft 85
clinical defaults are approved.

### M2 - Broader Administration; Reviewed Package Gated

- Import or restore a versioned, clinically reviewed default package.
- Preserve and visibly distinguish local phrases.
- Export effective library and provide explicit bulk operations.
- Extend the reusable interaction to approved additional text-entry surfaces.
- Repeat the same validation, evidence, UAT, and deployment gates.

M2 cannot start its default-package task until the phrase source, exact content,
version, and clinical approver are recorded.

The current M2 stack delivers the independent broader-administration slice:
deterministic export, explicit bulk activation/deactivation, and local-only
removal. Reviewed package import/restore and additional field consumers remain
undelivered because their product inputs are not approved. This is a partial M2
delivery, not evidence that those gated outcomes are complete.

## Data Flow

1. A supported field requests active phrases for its context when it first
   receives focus or is used after form load.
2. The user types a dot token or opens matching suggestions.
3. The reusable adapter replaces the token in the controlled field value and
   returns focus and caret to the text area.
4. The existing workflow submits the expanded text unchanged through its
   current service path.
5. Administration reads and writes phrase definitions through a separate
   controller/service boundary; authenticated actor attribution occurs on the
   server.

## Database Change

M1 adds phrase definitions and phrase-to-context associations with generated
IDs, normalized uniqueness, active status, provenance, authenticated audit
metadata, and rollback. M2 adds a data-model migration only if reviewed package
provenance cannot be represented by the M1 model; package content itself must
not be inserted as an unreviewed test fixture.

## Test Strategy

- JUnit 4/Mockito: normalization, uniqueness, validation, context matching,
  active filtering, pagination, audit actor, and authorization boundary.
- Controller: accessible read contract, admin writes, malformed input, actor
  spoof resistance through absence of client actor input.
- ORM/Liquibase: mapping registration plus update and rollback on empty and
  populated databases.
- Vitest/RTL: token parsing and replacement at caret boundaries, surrounding
  text preservation, context filtering, keyboard navigation, Escape, focus
  restoration, announcements, admin canonical state, and Carbon controls.
- Playwright `core-app`: admin creates a phrase, returns to a seeded culture
  case through stable URLs, expands and saves the phrase, reloads the case, and
  verifies persisted expanded text. Interactions use roles and labels with no
  arbitrary waits.
- Visual evidence: deterministic desktop/mobile admin and culture-note states
  compared with M-08 workflow intent; intentional deviations documented.
- Performance evidence: active-context lookup and a 500-phrase admin list are
  measured under a reproducible fixture, with results recorded rather than a
  mock-derived implementation promise.

### Reusable Test Boundaries

- Service tests own CSV ordering/escaping and all-or-nothing bulk rules; controller
  tests own media headers, authorization, and authenticated actor propagation.
- A shared attachment helper owns browser download lifecycle. A shared Carbon
  confirmation modal owns focus-safe confirmation content for batch actions.
- Admin component tests use Carbon checkboxes, buttons, and dialogs by accessible
  role/name. Playwright composes the existing text-macro fixture and navigation
  helpers, waits on observable responses/UI state, and contains no time-based
  functional waits.
- M2 visual and accessibility evidence reuses the M1 screenshot, WCAG, and titled
  video helpers so milestone proof has one format rather than a custom recorder.

## Constitution Check

- Configuration-driven: contexts and phrases are data, not country branches.
- Carbon first: existing Carbon controls and tokens; no new design framework.
- Layered architecture: entity, DAO, service transaction, REST mapping, DTO.
- TDD: failing focused tests precede each behavior slice.
- Schema management: Liquibase only for the new data model, with rollback.
- i18n: new user-facing keys go to `en.json` only.
- security/audit: admin writes, server-derived actor, no client actor input.
- milestone delivery: spec PR followed by one stacked PR per validation
  milestone.
- legacy avoidance: Dictionary and Note are not extended beyond their current
  clinical meanings.

## Explicit Non-Goals

AI-authored phrases, patient-specific variables, conditional templates, rich
text, automatic clinical interpretation, silent expansion, per-user private
libraries, unreviewed clinical defaults, and replacement of existing clinical
record APIs.
