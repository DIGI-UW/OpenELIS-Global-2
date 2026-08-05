# Feature Specification: OGC-788 Macro Library

**Feature Branch**: `spec/788-ogc-788-macro-library`  
**Jira**: [OGC-788](https://uwdigi.atlassian.net/browse/OGC-788)  
**Parent**: [OGC-782 Microbiology MVP](../782-ogc-782-microbiology-mvp-spec/spec.md)  
**Workflow source**: [Microbiology Module workflow walkthrough](https://uwdigi.atlassian.net/wiki/spaces/oeg/pages/1315209256)  
**Design intent**: [M-08 Macro Library](https://github.com/DIGI-UW/openelis-work/blob/main/designs/microbiology/m-08-macro-library.md), [interactive mock](https://digi-uw.github.io/openelis-work/designs/microbiology/m-08-macro-library.html)

## Scope

The Macro Library gives laboratory staff a fast, discoverable way to insert
approved phrases into narrative fields without sacrificing the final text's
clarity or editability. An authorized administrator manages the shared library;
an authenticated user types a short dot-prefixed code or chooses a matching
suggestion and receives the phrase as ordinary text in the field.

The capability is cross-cutting. Routine bacteriology is its first consumer
because repetitive culture descriptions are a high-frequency workflow, but the
library must not be coupled to microbiology case storage.

The first implementation milestone covers managed active phrases and real
expansion in culture activity notes, clinical history, and antibiotic exposure.
A follow-on milestone covers a clinically reviewed default package, restore or
import behavior, export, bulk administration, and rollout to additional fields.

## User Stories

### US1 - Insert an approved phrase (Priority: P1)

As a laboratory user, I can insert an approved phrase while entering a
narrative so repetitive observations are faster and more consistent.

**Acceptance scenarios**

1. Typing `.` in a supported empty or partial narrative presents active phrases
   that apply to that field's context.
2. Typing a complete code followed by Space or Tab replaces only that code with
   the phrase and preserves text before and after it.
3. Choosing a suggestion with the keyboard or pointer performs the same
   replacement and returns focus to the field.
4. The expanded phrase remains ordinary editable text; the submitted record
   contains the phrase, never a hidden dependency on the shortcut.
5. An unknown, incomplete, inactive, or out-of-context code remains unchanged.

### US2 - Discover and operate shortcuts accessibly (Priority: P1)

As a keyboard or assistive-technology user, I can discover and select matching
phrases without losing my place in the form.

**Acceptance scenarios**

1. Suggestions have an accessible name and announce the result count and the
   selected phrase.
2. Arrow keys move through suggestions, Enter or Tab accepts the focused
   suggestion, and Escape closes suggestions without changing the text.
3. The phrase list does not cover the active field or primary form action at
   supported desktop and mobile widths.
4. Loading and empty states are conveyed without arbitrary focus movement.

### US3 - Manage the shared library (Priority: P1)

As an authorized laboratory administrator, I can create, find, update,
activate, and deactivate phrases used by staff.

**Acceptance scenarios**

1. The library can be searched and filtered by context and active status; the
   canonical URL restores the same visible state after reload.
2. Creating or editing a phrase requires a unique dot-prefixed code, non-empty
   expansion text, at least one supported context, and an explicit active
   status.
3. A successful change is attributable to the authenticated administrator and
   is available to a newly loaded supported form.
4. Deactivating a phrase prevents new expansion but does not alter text already
   saved in laboratory records.
5. A non-administrator can use active phrases but cannot change the library.

### US4 - Maintain a reviewed default package (Priority: P2)

As a laboratory administrator, I can adopt reviewed OpenELIS phrases while
retaining locally authored content.

**Acceptance scenarios**

1. The system clearly distinguishes packaged phrases from local phrases.
2. Importing or restoring the reviewed package never silently deletes or
   overwrites a local phrase.
3. Administrators can export the effective library and perform explicit bulk
   activation, deactivation, or removal of eligible local phrases.
4. No clinical phrase is shipped merely because it appears in a draft mock or
   ticket; packaged text has a named reviewed source and version.

## Functional Requirements

- **FR-001**: Active phrases MUST be discoverable by code and phrase text within
  the current field context.
- **FR-002**: Direct expansion MUST replace only the dot-prefixed token adjacent
  to the caret and preserve surrounding text and caret position.
- **FR-003**: The saved clinical narrative MUST contain expanded editable text,
  not a macro identifier or deferred rendering instruction.
- **FR-004**: Inactive, unknown, incomplete, and out-of-context shortcuts MUST
  not expand.
- **FR-005**: Phrase codes MUST be normalized and unique without relying on a
  fixed primary key or client-supplied audit actor.
- **FR-006**: Administrative writes MUST record the authenticated actor and
  reject unauthorized access.
- **FR-007**: Administrative list and create/edit state MUST be bookmarkable and
  canonical, including search, context, status, sort, page, page size, and the
  selected action.
- **FR-008**: Runtime use MUST be keyboard complete, screen-reader observable,
  and usable at desktop and mobile widths.
- **FR-009**: Changes MUST become visible on a subsequent supported-form load;
  no particular client cache or invalidation mechanism is a product
  requirement.
- **FR-010**: The first implementation MUST support culture activity notes,
  microbiology clinical history, and antibiotic exposure without changing the
  underlying clinical-record contracts.
- **FR-011**: Packaged defaults MUST have clinically reviewed content and a
  versioned provenance before they are made available to laboratories.
- **FR-012**: Existing locally authored phrases and already expanded clinical
  text MUST survive package refreshes and phrase deactivation.

## Clarifications And Artifact Health

| Topic                                                                                                                                          | Classification                   | Ruling                                                                                                                                    |
| ---------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| Jira and M-08 prescribe `macro_library`, service/cache names, React component names, exact routes, modals, permission keys, and table columns. | Implementation leakage           | These are non-binding examples. Engineering chooses repo-aligned boundaries and Carbon interaction patterns.                              |
| M-08 calls for 85 defaults but says the full list is still to be determined during implementation.                                             | Product gap / real contradiction | No unreviewed clinical text is seeded. Exact content, provenance, and reviewer are acceptance inputs for the follow-on package milestone. |
| The mock presents a fixed category list while describing a cross-cutting capability.                                                           | Engineering decision needed      | User-visible contexts remain controlled and translatable; internal representation must permit later consumers without schema churn.       |
| The mock requires next-page-load freshness and also prescribes a particular cache invalidation design.                                         | Implementation leakage           | Only the observable freshness requirement is binding.                                                                                     |
| The mock alternates between Add/Edit/Delete modals and broad inline-interaction guidance.                                                      | Design inconsistency             | Use the clearest accessible Carbon flow; schema, route, modal, and component structure are not part of product acceptance.                |
| OGC-788 names fifteen sub-stories, but Jira contains no child issues.                                                                          | Planning inconsistency           | This spec's milestone tasks are the deterministic delivery ledger; Jira should be reconciled without inventing completed children.        |

## Success Criteria

- **SC-001**: A user can insert an active culture phrase by keyboard into a
  culture activity note and save the expanded text through the existing case
  workflow.
- **SC-002**: The same reusable interaction expands applicable phrases in
  clinical history and antibiotic exposure without duplicating macro logic.
- **SC-003**: An administrator can create, edit, search, filter, deactivate, and
  reactivate a phrase; reload from the canonical URL preserves list state.
- **SC-004**: A spoofed actor value cannot affect attribution, and a
  non-administrator cannot mutate the library.
- **SC-005**: Focused service, controller, ORM, Liquibase update/rollback,
  frontend, accessibility, and Playwright tests pass without SQL fixtures,
  fixed persisted IDs, or arbitrary waits.
- **SC-006**: Each implementation milestone has pinned `code-qa` reports,
  validated desktop/mobile screenshots, an MP4 walkthrough, synchronized Grist
  UAT steps, and an exact-SHA AMR deployment before manual acceptance.
