# Tasks: OGC-788 Macro Library

## Phase 0 - Spec And Crosswalk

- [x] T001 Ground scope in OGC-788, Confluence workflow v2.1, M-08 design/mock,
      and the OGC-782 M4 repo state.
- [x] T002 Audit `Dictionary`, `Note`, existing administration patterns, and the
      actual microbiology narrative fields; record why no existing model is an
      appropriate phrase-definition owner.
- [x] T003 Classify product gaps, implementation leakage, contradictions, and
      engineering decisions; keep technical choices out of product requirements.
- [x] T004 Define two validation milestones and the clinical-review gate for
      packaged defaults.
- [x] T005 Publish ready PR #3986 as the next native GitHub stack #3985 entry.
  The product-safe OGC-788 scope and unresolved default-content decision are
  recorded here; Jira synchronization remains part of final reconciliation.

## M1 Phase 1 - Tests First

- [ ] T101 Add failing JUnit 4 service tests for normalization, uniqueness,
      context matching, active filtering, pagination, and authenticated actor.
- [ ] T102 Add failing controller tests for authenticated lookup, admin writes,
      invalid input, unauthorized writes, and absence of client actor control.
- [ ] T103 Add failing ORM and Liquibase update/rollback tests for phrase and
      context persistence.
- [ ] T104 Add failing Vitest/RTL tests for caret-aware replacement, surrounding
      text preservation, keyboard behavior, announcements, contexts, and reusable
      controlled-field integration.
- [ ] T105 Add failing admin-page tests for Carbon interactions and canonical
      `q`, `context`, `status`, `sort`, `page`, `pageSize`, and create/edit state.
- [ ] T106 Register a focused Playwright `core-app` journey before production
      implementation; use accessible roles/labels and no arbitrary waits.

## M1 Phase 2 - Backend

- [ ] T107 Add the Liquibase model and rollback for phrase definitions and
      applicable contexts, with generated IDs and normalized uniqueness.
- [ ] T108 Add entity, DAO, service, and DTO layers with service transactions,
      eager response compilation, and server-derived audit actor.
- [ ] T109 Add authenticated runtime lookup and admin-only management endpoints.
- [ ] T110 Extend the property-gated UAT scenario through services only with a
      small safe demonstration set; use no SQL, DAO bypass, or fixed persisted IDs.

## M1 Phase 3 - Frontend

- [ ] T111 Build the reusable controlled narrative field with discoverable
      Carbon suggestions, keyboard selection, Escape, focus/caret restoration, and
      screen-reader announcements.
- [ ] T112 Integrate it with culture activity notes, clinical history, and
      antibiotic exposure while preserving existing form payloads.
- [ ] T113 Build the Carbon administration workflow with linkable breadcrumb
      path, stable query state, validation, pagination, and active-state controls.
- [ ] T114 Add the administration route/navigation using existing patterns and
      add English React Intl source strings only.

## M1 Phase 4 - Validation And Publication

- [ ] T115 Run focused backend, controller, ORM, Liquibase, frontend, a11y, and
      Playwright tests; review browser console output and failure artifacts. Record
      reproducible active-context lookup and 500-phrase administration evidence.
- [ ] T116 Run pinned `tools/code-qa` alignment, coverage, simplicity, and
      evidence checks; resolve or explicitly disposition every finding.
- [ ] T117 Compare deterministic desktop/mobile screenshots against M-08
      workflow intent and record all intentional differences.
- [ ] T118 Produce the standard titled MP4 walkthrough, contact sheet, manifest,
      and exact test/evidence index for M1.
- [ ] T119 Add M1 Grist story/steps, preserve prior required M1-M4 UAT, verify
      the live `uat-amr` revision, and mark automated evidence separately from human
      acceptance.
- [ ] T120 Commit and push promptly, open the M1 PR on the spec branch, append it
      to the native GitHub stack, update the PR body with exact evidence, and deploy
      the exact top SHA to `amr.openelis-global.org`.
- [ ] T121 Verify the deployed app SHA guard, live Review-overlay M1 steps, and
      complete the M1 human-UAT handoff without watching CI.

## M2 - Reviewed Package And Broader Administration

- [ ] T201 Record the exact default phrase source, content, version, clinical
      approver, and collision policy. This blocks only the package tasks below.
- [ ] T202 Add tests and implementation for reviewed-package restore/import that
      preserves local phrases and reports collisions.
- [ ] T203 Add tests and implementation for export and explicit bulk operations.
- [ ] T204 Extend the reusable field to approved additional consumers with no
      duplicated parser or suggestion logic.
- [ ] T205 Repeat focused tests, code-qa, visual/a11y comparison, standard video,
      Grist sync, stacked PR publication, exact-SHA AMR deployment, and human-UAT
      handoff.

## Completion Gate

- [ ] T301 Reconcile OGC-788, OGC-782 roadmap status, M-08 design gaps, PR stack,
      and UAT ledger without marking unimplemented draft scope complete.
- [ ] T302 Confirm all required M1 and M2 acceptance scenarios have automated
      evidence and human UAT rulings; record accepted risks and follow-on issues.
