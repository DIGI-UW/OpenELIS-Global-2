# OpenELIS Work Authoritative Alignment Record

**Date:** 2026-08-05
**Authority revision:**
[`DIGI-UW/openelis-work@a1f720d7b3b0`](https://github.com/DIGI-UW/openelis-work/commit/a1f720d7b3b01db63387361495f4aa6589105003)
**Implementation baseline:** `08b5b3888af4ba9f1c506fc555138218e0d043a4`
**Remediation code through:** `bd27e76d9`
**Remediation branch:**
`feat/782-ogc-782-microbiology-r1-authoritative-alignment`

## Authority Contract

OpenELIS Work is authoritative for actors, laboratory outcomes, visible
workflow order, information shown or captured, control meaning, requiredness,
defaults, state transitions, and observable acceptance behavior.

OpenELIS Work does not dictate database structures, Java classes, services,
controllers, API payloads, React component boundaries, or route names. Those
remain engineering decisions, provided the supported OpenELIS route presents
the authoritative behavior. An intentional behavior deviation requires an
explicit product ruling and evidence.

## Status Vocabulary

- **Not started:** no implementation evidence.
- **Implemented, evidence pending:** code exists but the complete behavior has
  not passed the required automated evidence.
- **Automated evidence passed, UAT pending:** automated evidence covers the
  authoritative behavior; a human ruling is still required.
- **Accepted with documented deviations:** human UAT passed and every source
  deviation is explicitly ruled.
- **Complete:** all required behavior and evidence gates passed.

No aggregate module claim may use **Complete** while one required behavior is
unmapped or human UAT is pending.

## Source Reconciliation

| Topic                  | Conflicting source behavior                                                                                                               | Proposed default                                                                                                                                                                                                | Status                                             |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| M-03 Program trigger   | The visual mock shows manual Program selection; the v2 FRS derives Program from selected tests with manual fallback                       | A typed culture test owns a visible, disabled Microbiology Program value. Manual fallback remains available only when no typed culture workflow applies; leaving it with entered details requires confirmation. | Resolved and browser-proven by M-03 section 2.1    |
| Untyped test fallback  | Earlier wording defaulted to bacteriology; M-03 section 2.1a reconciles it to an explicit deployment default or `UNASSIGNED`              | Use an explicit deployment default; otherwise `UNASSIGNED`                                                                                                                                                      | Resolved by M-03 section 2.1a                      |
| Bacteriology plus TB   | Earlier wording requested a second specimen; M-03 section 2A and the parent workflow require sibling cases on the same specimen           | Create two linked cases on one SampleItem with independent lifecycle/history                                                                                                                                    | Resolved by M-03 section 2A                        |
| Physical specimen loss | M-04 gives sibling cases independent clinical lifecycles while all siblings refer to one physical SampleItem that can be lost or rejected | Reject the shared SampleItem and all nonterminal analyses; close every open sibling as lost while recording a separate activity in each case; block when a sibling is final/amended                             | Proposed engineering default; Piotr ruling pending |

The detailed v2 reconciliation sections supersede stale summary wording. No
product ruling remains for these three topics; source cleanup is still needed
to remove the contradictory remnants.

## Guided Workflow Crosswalk

The product modules below follow the order in
[`amr-micro-workflow-flow.html`](https://github.com/DIGI-UW/openelis-work/blob/a1f720d7b3b01db63387361495f4aa6589105003/designs/microbiology/amr-micro-workflow-flow.html#L103).
The implementation status is deliberately narrower than “code exists.”

| Step            | Product behavior                                                           | Downstream location                    | Current evidence                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | Status / required action                                                                                                          |
| --------------- | -------------------------------------------------------------------------- | -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------- |
| 1. M-01         | Administer microbiology reference data and view deployment Patient Origins | M9 reference-admin plus R1 T284/T286   | Organism/antibiotic administration and the deployment-backed six-value Patient Origin vocabulary exist; Patient Origin has a shared read-only Carbon list and no Phase 1B mutation actions                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Automated evidence exists; authoritative visual/deployed UAT pending                                                              |
| 2. M-02         | Load, review, and activate breakpoint versions                             | M9 reference-admin spec/tasks          | Import/activation code and automated tests exist                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             | Automated evidence exists; authoritative visual UAT pending                                                                       |
| 3. M-08         | Author and use reusable text macros                                        | Separate Macro Library stack           | Managed runtime and administration code exist above the micro stack                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Separate cross-cutting feature; extract to its own PR/UAT stack                                                                   |
| 4. M-12         | Link tests and reagent/card lots and capture the lot used                  | M8 plus R1 T259-T262/T288-T289         | Culture and AST reuse one picker; visible QC/FEFO eligibility, exact scanner entry, controlled Carbon selection, locked save-time race handling, specific corrective messages, Inventory usage, and no-partial-link behavior have automated evidence. The registered PostgreSQL journey persists culture and AST provenance. Exact-SHA local p95, keyboard, and desktop/mobile axe qualification pass. Shared administration/reverse-link behavior remains external, and required/optional/substitute policy cannot be inferred from current catalog roles.                                                                                                                                                  | Implemented consuming slice with automated qualification; deployed UAT pending, catalog policy blocked by T288                    |
| 5. Test Catalog | Configure the microbiology workflow on culture-capable tests               | MVP M1 and Test Catalog implementation | Configuration is implemented and tested                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Automated evidence passed; human UAT pending                                                                                      |
| 6. M-03         | Derive visible Program and collect culture details during order entry      | FR-001/FR-002; Phase 14                | Supported order entry preserves workflow/Method metadata, derives and locks Program for typed culture work, renders the ruled controls, confirms discard, and supports safe manual fallback only outside typed culture workflow. Typed booleans, bounds, default-Method resolution, a durable Sample-owned draft across the enter/collect reload, order-search projection, service-created routing/detail/idempotency integration, and the configured-route browser stories pass. Patient Origin now comes from the six-value deployment vocabulary, rejects free text, retains WHONET codes, and can default through explicit requesting-organization configuration. Clinical History is not macro-enabled. | Partial; Macro consumer integration, visual/deployed evidence, and human UAT remain                                               |
| 7. M-04         | Create linked workflow cases for one specimen                              | MVP M2/M3 plus Phase 14                | Typed sibling creation and safe unassigned fallback are implemented                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Automated evidence exists; complete shared-specimen visual UAT pending                                                            |
| 8. M-07         | Work from the shared Culture/AST queue                                     | MVP M6 and worklist remediation        | Canonical Culture/AST grain and status state, bounded accession/patient/specimen/panel/activity/timing context, active AST-run projection, pending-setup isolates, Carbon grain switch/cards/tables, exact isolate/run navigation, sibling context, critical flags, complete row menus, positive-signal progression/emphasis, analyzer resistance strip, recent activity, stable 30-second refresh, day-aware incubation fallback, full-row pointer/keyboard navigation, repository-role access enforcement, and registered desktop/mobile/keyboard/reload/200-case tests exist                                                                                                                              | Partial; manual resistance confirmation, exact-SHA performance/visual evidence, separate Culture/AST UAT, and human review remain |
| 9. M-04         | Work the culture case by current step                                      | MVP M4 plus T237-T242                  | Deterministic current-step focus, collapsed order/origin/location/sets/exposure context, clinical-history-first expansion, last-activity actor, sticky stage action, explicit inoculation/subculture lineage, shared lots, timeline Notes, critical entry points, shared NCE reporting, and lost-specimen rejection are implemented                                                                                                                                                                                                                                                                                                                                                                          | Implemented in code and automated tests; exact-SHA runtime evidence and human UAT pending                                         |
| 10. M-04        | Record isolate work-up and identification                                  | MVP M4 plus T239                       | Two-pass Gram/morphology work-up, pending/identified status, method/confidence/significance, AST gating, preliminary report projection, and immutable reidentification history are implemented                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | Implemented in code and automated tests; deployed visual UAT remains                                                              |
| 11. M-05        | Enter and interpret AST                                                    | MVP M5 plus M8 and T243-T250           | Multi-reading AST, selectable standards, scoped repeat/retest, case/NCE/worklist convergence, truthful run/setup/identification progress, override/revert history, ordered panel/drug snapshots, justified adjustment, analyzer pending/results/QC states, provenance, review blockers, and durable event reconciliation are implemented                                                                                                                                                                                                                                                                                                                                                                     | Partial; exact-SHA interaction evidence, deployment, and human UAT remain                                                         |
| 12. M-06        | Review expert-rule findings                                                | Deferred product outcome               | No expert-rule workflow                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Not started; separate future milestone                                                                                            |
| 13. M-11        | Log and follow critical communication                                      | MVP M6/M7 remediation                  | Clinical record and Alert synchronization exist                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | Automated evidence exists; human UAT pending                                                                                      |
| 14. M-04        | Reclassify an unassigned or misrouted case                                 | US8; T234-T242                         | Inline classification, compatible-Method validation, authenticated audit history, final lock, held profile actions, sibling links, focused tests, and a registered service-created Playwright journey exist                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | Implemented, evidence pending: deployed Playwright, separate Grist story, and human UAT remain                                    |
| 15. M-14        | Run the operational TB workflow                                            | Deferred product outcome               | Workflow enum/sibling scaffolding only                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Not started; separate future milestone                                                                                            |
| 16. M-13        | Produce cumulative antibiograms                                            | Deferred product outcome               | No antibiogram workflow                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Not started; separate future milestone                                                                                            |
| 17. M-09        | Validate and export WHONET data                                            | M10 WHONET spec/tasks                  | Manual long-format configure/preview/repair/generate exists                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | Partial; exact package/compatibility and later delivery scope remain                                                              |

M-10 hub subscription and M-15 GLASS/FHIR remain valid module outcomes outside
the 17-screen walkthrough. Neither is implemented.

## M-03 Requirement Trace

| Source behavior                                                    | Product requirement           | Engineering task    | Current code/evidence                                                                                                                                                                                                                                                                         | Remediation state                                                                             |
| ------------------------------------------------------------------ | ----------------------------- | ------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| Culture test derives Program = Microbiology                        | US1 scenario 1; FR-001        | T210/T214/T215      | Direct and panel selections preserve workflow/Method metadata; typed culture work derives a disabled visible Program, while manual fallback is limited to untyped work                                                                                                                        | Component and configured-route Playwright evidence passed; deployed UAT pending               |
| Details appear in the supported Add Order flow                     | US1 scenario 1; FR-002        | T211/T216           | Shared Carbon detail controls render in `/order/enter`; the compatibility flow consumes the same controls                                                                                                                                                                                     | Component and configured-route Playwright evidence passed; deployed UAT pending               |
| Culture Method is required/defaulted/adjustable                    | FR-002                        | T211/T216/T218      | The shared Method control is required, the service-created catalog supplies a real default `TestMethod`, and routing resolves it before legacy fallback                                                                                                                                       | Component, service, migration, and complete supported-save browser proof passed               |
| Patient Origin uses deployment reference data                      | FR-002                        | T211/T216/T284/T286 | One active deployment vocabulary supplies the six ruled choices and WHONET codes to order entry and the case panel; writes normalize and validate the stable code; an explicit optional Organization mapping supplies a default; the shared admin shell exposes the same vocabulary read-only | Runtime and Phase 1A reference-list behavior aligned; visual/deployed UAT remains             |
| Number of Sets uses ruled bounds/default                           | FR-002                        | T211/T216/T218      | UI and service enforce 1-10; the browser journey persists and restores the typed value across collection                                                                                                                                                                                      | Focused automated and complete save-path browser evidence passed                              |
| Clinical History accepts multi-line text and macros when available | FR-002                        | T211/T216/T274/T285 | Multiline input and the 1000-character service bound exist; `clinical` macros wait on the separate Macro Library base                                                                                                                                                                         | Partial; macro consumer integration remains explicitly blocked                                |
| Antibiotic Exposure is a checkbox                                  | FR-002                        | T211/T216/T218      | UI and persistence use binary checkbox/boolean semantics                                                                                                                                                                                                                                      | Focused automated and complete save-path browser evidence passed                              |
| Critical Notify is a checkbox with ruled default                   | FR-002                        | T211/T216/T218      | UI and persistence use binary checkbox/boolean semantics                                                                                                                                                                                                                                      | Browser persistence passed; specimen-sensitive default proof remains open                     |
| Removing the last culture test confirms before discarding details  | US1 scenario 4; FR-002        | T211/T217           | Final culture-test removal and a manual Program departure both confirm before clearing details                                                                                                                                                                                                | Component and configured-route Playwright evidence passed; UAT pending                        |
| Saving exposes the details on the created case without duplication | US1 scenario 5; FR-001/FR-002 | T212/T218           | Service-created integration proves Sample-owned draft persistence, order-search projection, routing consumption, case compilation, and repeated-route idempotency; Playwright exercises the real REST save/collect route and observes exactly one case                                        | Runtime behavior and browser proof passed; direct Java `persistData` integration remains T212 |
| Non-culture selection keeps details hidden and creates no case     | US1 scenario 2; FR-001        | T210/T211/T212/T213 | Focused UI/routing evidence and configured-route Playwright prove the order remains absent from the microbiology worklist                                                                                                                                                                     | Automated evidence passed; deployed UAT pending                                               |
| Mixed bacteriology/TB follows the ruled sibling behavior           | US1 scenario 3; FR-003        | T213/T218           | Service-created catalog and configured-route Playwright prove exactly one Bacteriology and one Mycobacteriology TB sibling on one accession                                                                                                                                                   | Automated evidence passed; deployed UAT pending                                               |

## R1 M-03 Supported-Route Evidence - 2026-08-06

- The UAT scenario service creates the patient/sample catalog, a real default
  `TestMethod`, bacteriology and TB culture tests, and a non-culture control
  through application services. It does not use SQL, DAO bypass, or fixed
  primary keys.
- The order draft is owned by the persisted `Sample` until collection and is
  then consumed by idempotent case routing. This models the actual
  `/order/enter` to `/order/collect` boundary instead of synchronizing a browser
  draft to an unrelated store.
- `npm run pw:test -- playwright/tests/foundational/core/microbiology-order-entry.spec.ts --project=core-app --workers=1`
  passed setup plus four user stories in 24.5 seconds: durable details and one
  case, non-culture exclusion, guarded discard, and same-specimen
  bacteriology/TB siblings. The order-entry assertions now also prove all six
  deployment Patient Origin choices, including Long-term Care and Unknown,
  before selecting and persisting the stable code. The tests use Carbon-accessible interactions and
  readiness assertions, with no arbitrary waits. The repository bucket guard
  and project-registration validator pass, and the selector audit found no
  forced actions, blind first/last selectors, or response-status assertions.
- Focused Vitest passed 25 tests in 1.77 seconds across order payload/state, Program behavior,
  selected-test metadata, and the worklist. Focused JUnit passed 12 tests across
  service-created fixture behavior, ORM construction, and PostgreSQL
  update/rollback/reapply for both schema corrections.
- Runtime validation exposed and repaired two pre-existing schema assumptions:
  the canonical `MICROBIOLOGY` Program code exceeded the old 10-character
  column, and numeric authenticated user IDs could not join the varchar
  activity actor column. Migrations `083` and `084` contain only those durable
  model corrections and have explicit rollback tests.
- Focused Patient Origin service/controller tests, five Carbon component
  interactions, ORM registration, and PostgreSQL migration
  update/rollback/reapply all pass. Seed identifiers are database-generated;
  no test SQL, fixed primary key, or DAO-bypass fixture was added.
- The touched worklist no longer uses Carbon's deprecated `ariaLabel` prop.
  The shared harness still records a URL-less shell 404 message. The M-12
  performance harness uses a stable-page same-origin fetch and no longer creates
  aborted-request noise while collecting samples; the remaining shell 404 is
  separate evidence-harness noise to diagnose.
- Remaining M-03 gaps are Macro Library consumption after the separate macro
  feature exists (T285),
  source-mock screenshots, Grist publication, exact-SHA deployment, and human
  acceptance. The direct Java `SamplePatientEntryService.persistData`
  integration requested by T212 is also still open even though the browser
  journey now proves the real runtime path.

### Patient Origin source-quality ruling

- The authoritative product behavior is a controlled six-value vocabulary,
  WHONET-ready identity, and an optional requesting-location default. That is
  implemented without client-owned choices or free-text persistence.
- M-01's instruction to use a particular table and foreign key is
  implementation leakage. The engineering model uses microbiology-owned
  reference entities and validates the stored stable code at the service
  boundary; product acceptance does not depend on a particular column layout.
- The source never defines how a requesting unit or ward maps to an origin.
  The deterministic default is explicit Organization-to-origin configuration;
  no configured mapping means no default. Organization-name inference is
  intentionally prohibited because it could silently misclassify clinical
  context.
- M-01 separately requires a Phase 1A read-only Patient Origin administration
  list. That surface was absent from the original roadmap; T286 adds it to the
  shared reference-data section registry, with canonical search/filter/sort/page
  state and no mutation controls. Phase 1B CRUD remains out of this remediation
  slice.
- M-01 calls Patient Origin the fifth sidenav item. That ordinal is stale:
  Breakpoint Standards and the existing Method-backed Culture Methods surface
  are also present in the current shared reference shell. Patient Origin is
  discoverable in that shell, but its numeric position is not treated as a
  product requirement.
- The Patient Origin Playwright story exposed a pre-existing shared admin defect:
  canonical query parameters were written to the URL but `@ModelAttribute`
  could not bind `MicroReferenceAdminQueryForm` because it had public fields
  without JavaBean accessors. The shared form now binds all supported search,
  filter, sort, and paging fields, so the visible table and bookmarkable URL no
  longer diverge across reference sections.
- The first real filtered response then exposed a second shared race: Carbon
  could retain its previous row model for one render while the page had already
  installed the new response array. Status rendering no longer cross-looks up
  those two states, and row actions fail closed during the transition instead
  of dereferencing a removed row.
- The complete reference-admin browser suite also exposed two stale shared-test
  assumptions: its AST helper omitted the now-required Gram/colony isolate
  work-up, and the common edit modal could finish an async save after its URL
  state had already unmounted it. Fixtures now remain service-created but send
  clinically valid work-up, and the modal cancels post-unmount state updates.
- That same browser path exposed an AST lifecycle defect: a found generic
  standard-level breakpoint was labeled `matchedBy=NONE`, which the review gate
  correctly reserves for a missing breakpoint. The service now records generic
  rules as `STANDARD`; focused JUnit coverage and the full reference-admin
  Playwright file prove that valid interpreted work reviews without weakening
  the missing-breakpoint blocker.
- Validation on 2026-08-06: focused reference service/controller tests passed;
  ORM validation compiled the real paging HQL; three frontend files passed 26
  Carbon interaction tests; strict ESLint, Prettier, Spotless, Playwright bucket
  guard, and project validation passed; the complete registered reference-admin
  Playwright file passed 7/7 in 26.0 seconds without arbitrary waits.

## Full-Stack Acceptance Drift

This matrix prevents the roadmap from treating an implemented vertical slice as
the complete source module. “Partial” means at least one authoritative behavior
in the named acceptance criterion is absent or unproven.

| Module                  | Automated evidence already present                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Material implementation drift                                                                                                                                                                                                                                                                                                                                                    | Roadmap/spec correction                                                                                                                                                               |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| M-04 Case Workbench     | Case creation, deterministic current-step focus, progress rail, compact and expanded Case Info, last-activity context, sticky stage action, typed inoculation/subculture lineage, automatic timeline plus shared Note observations, two-pass isolate work-up and identification, amendments, report release, final lock, workflow classification, sibling navigation, critical entry points, shared NCE reporting, NCE badge, lost-specimen rejection, and analyzer-event reconciliation                                                                                                                                                                                                                                                                                                                                              | Exact-SHA NCE/lost/analyzer runtime, source-mock comparison, separated Grist stories, and visual/human UAT remain open                                                                                                                                                                                                                                                           | Keep each remaining behavior in independent TDD/UAT stories; implemented slices are not accepted until deployed UAT                                                                   |
| M-05 AST                | Manual readings, breakpoint interpretation, selectable and version-snapshotted standard, organism-default panel confirmation, immutable ordered-drug snapshot, reasoned panel adjustment, distinct laboratory technique with server-derived measurement type, reading source/matched-basis/units, no-breakpoint guidance, immutable override/revert history, analyzer result/QC lifecycle and provenance, review blockers, whole-panel/single-drug repeat, case/NCE/worklist repeat convergence, truthful progress, reagent capture                                                                                                                                                                                                                                                                                                   | No deployed exact-SHA interaction evidence or human acceptance                                                                                                                                                                                                                                                                                                                   | Preserve the delivered AST slices and complete acceptance evidence explicitly                                                                                                         |
| M-07 Worklist           | Shared Culture/AST page, canonical grain/status plus shared URL state, bounded accession/patient/specimen/panel/latest-activity/timing projection, active-run and pending-setup projection, Carbon status cards and source-aligned grain tables, exact isolate/run navigation, automatic-results guidance, distinct positive-signal progression and refreshed-row emphasis, analyzer positive-event reconciliation, case-scoped positive/no-growth commands, complete row menus, full-row pointer/keyboard navigation, preserved repeat routing, sibling marker, critical flag, analyzer resistance strip, recent activity, responsive table, stable 30-second/manual refresh, elapsed freshness status, day-aware incubation fallback, repository-role worklist access, and registered desktop/mobile/keyboard/reload/200-case tests | Manual resistance confirmation is undefined. Reviewed runs are included and labeled In progress to expose repeat, while the authoritative active queue says accepted results clear and does not define a Reviewed state. Exact-SHA p95 measurement, source-mock screenshots, separate Culture/AST UAT, and human review remain open.                                             | T199, T258, T277, T282, and the T283 reviewed-run ruling complete the remaining semantics and evidence                                                                                |
| M-12 Reagent Lot Picker | One Carbon component is reused in culture and AST setup; it shows QC for every lot, preserves blocked-lot reasons, marks the FEFO recommendation, explains ordering, accepts exact scanner-style lot entry, controls the nested Carbon radio state, and formats save-time inventory conflicts with reagent, lot, reason, and corrective action. Inventory performs locked revalidation/consumption and the service proves a conflict cannot create a partial microbiology usage link. The registered journey exposed and closed the inoculation Method-ID mapping defect, then persisted both culture and AST provenance in 7.2 seconds. Exact-SHA local qualification passes the `<500 ms` ceiling, desktop/mobile axe scans in both hosts, and the complete keyboard journey. Repeat metadata and usage history remain visible.     | Exact AMR deployment and human acceptance remain open. The source asks for required/optional/substitute policy but the shared Test Catalog only defines `PRIMARY`/`SECONDARY`; treating those as equivalent would be unsafe. The source also prescribes schema, routes, class names, and component signatures despite assigning administration to shared Test Catalog/Inventory. | Keep Microbiology ownership to shared consuming behavior; T262 records automated runtime qualification and T288 tracks the external catalog-policy contract without duplicate schema. |
| M-NFR                   | Focused accessibility, keyboard, repeat/amendment, and synthetic performance specs exist                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | No offline queue/conflict resolution; scale evidence is not yet a service-created 200-case deployment measurement; audit guarantees and accessibility evidence are incomplete across new actions                                                                                                                                                                                 | Add explicit qualification tasks and prohibit unsupported NFR completion claims                                                                                                       |

## R1 M-07 Worklist Evidence

- **Code:** `398a69003` adds the canonical Culture/AST grain and
  grain-specific status contract, projects one live row per active AST run,
  represents clinically significant isolates with no active run as pending
  setup, and excludes invalidated, rerun-required, and cancelled attempts from
  the live queue. `bd27e76d9` adds the Carbon grain switch, view-specific
  summary controls and tables, explicit automatic-analyzer guidance, compact
  source-aligned layout, and exact isolate/run focus in the case.
- **Progression closure:** `1d838d72a` restores the authoritative incubating to
  positive-signal to confirmed-growth progression, keeps no-growth as a
  separate incubation outcome, accepts idempotent normalized analyzer positive
  events with reconciliation, and routes manual queue commands into case-scoped
  confirmations. Migration `079` adds only the missing durable state and also
  aligns the database constraint with the existing lost-specimen states.
- **Menu closure:** `88afdd369` adds the remaining AST "Set up new AST run"
  command for reviewed runs. It carries the exact isolate/source-run/action in
  the URL, focuses the existing case-scoped repeat form, calls the same
  preserved repeat service used by the case and NCE paths, and replaces the URL
  with the new run after creation.
- **Reviewed-run contradiction:** the authoritative M-07 page is an active
  queue, defines Pending setup, In progress, and Results in/review states, and
  says the results-in flag clears when accepted. The current service and UI also
  include `REVIEWED` runs under In progress so the repeat command above remains
  reachable. This is not accepted as parity by inference. T283 requires a
  product ruling; UAT uses an unambiguously active `IN_PROGRESS` run meanwhile.
- **Operational context closure:** `b7b025f98` projects the authoritative
  accession, patient, specimen, AST panel name, and latest-activity actor in a
  fixed number of batch reads. The Culture and AST Carbon tables now use the
  source columns while retaining workflow linkage and critical state as
  secondary row context. No patient/specimen data or schema was duplicated.
- **Situational context closure:** `a99493671` adds the latest 25 typed
  activities in one bounded query, today-scoped counts for structured analyzer
  ESBL/MRSA/CRE/VRE/MDR flags, real analyzer flags in AST rows, and Carbon
  tooltips for deferred Expert Rules and worklist WHONET controls. It does not
  parse free-text AST override reasons.
- **Automated checks passed locally:** focused `MicroWorklistServiceTest`,
  `MicroCaseStateServiceTest`, analyzer-event service/controller tests; route-
  state, Carbon worklist, case confirmation, and AST panel-focus tests. The
  latest focused frontend checkpoint contains 55 passing tests plus targeted
  ESLint, Prettier, Spotless, Liquibase XML validation, and `git diff --check`.
  The PostgreSQL rollback harness is committed but did not execute locally
  because Testcontainers could not find a Docker daemon.
- **Validation boundary:** the focused service test proves fixed-count
  enrichment and search; the component checkpoint proves source columns and
  values. The exact production-HQL validation test is committed but could not
  start locally because the repository test context requires Docker and no
  daemon was available; CI remains required evidence.
- **Refresh and access closure:** `7d162d7ce` adds a shared 30-second refresh
  constant, explicit Refresh now action, elapsed freshness status, mounted-table
  revalidation, and repository-role enforcement for the worklist route/API. A
  fake-clock Carbon test proves no early request and preservation of canonical
  URL, search focus, and table scroll. A Spring security slice proves
  unauthenticated and unrelated-role rejection plus Results, Validation, and
  Admin access. OpenELIS Work's `micro.case.view` is not a repository authority;
  the role mapping is an engineering interpretation of its
  analyst/validator/manager intent rather than a new permission invention.
- **Culture-timing closure:** `3792bd13f` extends the existing Method-bound
  culture setup with validated structured timing, reads first inoculation and
  maximum-day context in bounded batches, and renders `Day n of max` only when
  both inputs exist. Missing configuration remains visibly `Incubating`; the
  implementation does not parse free-text defaults or create a second protocol
  master. Twenty-one focused backend tests and twenty-four Carbon tests pass;
  committed HQL/ORM and Liquibase rollback tests require Docker and therefore
  remain CI evidence on this host.
- **Still open:** the source does not define how a user manually confirms a
  resistance category even though it requires "manual overrides in 1A"; T277
  keeps that semantic gap visible. `05c165de7` closes the previously omitted
  refreshed-positive emphasis and full-row keyboard navigation with fake-clock
  and Carbon interaction evidence. `3792bd13f` closes T280-T281 without parsing
  free text or inventing worklist configuration. T257 now has registered
  desktop/mobile, keyboard, canonical-reload, and service-backed 200-case
  coverage without sleeps. Exact-SHA p95 and mock-comparison evidence, separate
  Grist Culture and AST stories,
  exact-SHA deployment, and human UAT also remain open.

## R1 M-12 Qualification Evidence

- **Exact application revision:** `586056623` on the R1 remediation branch,
  exercised against the local disposable PostgreSQL/OpenELIS stack with the
  qualification guard enabled.
- **Performance method:** two warmups followed by ten measured browser samples,
  using nearest-rank-ceiling p95. With two linked requirements and five
  eligible/blocked lots, same-origin service fetch plus JSON parse measured
  p50 `5.8 ms`, p95/max `6.8 ms`; Carbon picker interaction-to-visible render
  measured p50 `32.8 ms`, p95/max `34.6 ms`. Both pass the source `<500 ms`
  ceiling. Playwright retains every raw sample in JSON and a Markdown summary
  as external test attachments.
- **Accessibility and interaction:** explicit axe scans pass for the shared
  picker inside both Culture setup and AST setup at desktop and mobile
  viewports. The keyboard-only journey passes accession filtering, link
  navigation, two-pass Gram/colony isolate creation, organism/method/confidence
  identification, exact card-lot scanning, two AST readings, run review, and
  amendment release. The registered culture-to-AST provenance journey also
  passes with Carbon-visible eligibility, QC, FEFO, scanner feedback, and usage
  history assertions.
- **Fixture integrity:** all clinical and reference setup remains behind the
  property-gated service-created UAT scenario. The WHONET helper now supplies
  the required audited panel-adjustment reason through the REST/service path;
  no SQL, fixed primary key, DAO bypass, or weakened production validation was
  added.
- **Still open:** AMR deployment at this exact revision, separated Grist
  stories, and Piotr's human UAT remain T268/T272/T273. Required, optional, and
  substitute policy remains an external shared Test Catalog decision in T288.

## Product-Source Health Findings

OpenELIS Work remains authoritative for observable behavior, but several files
mix product requirements with proposed implementation. These details must be
carried only as engineering hypotheses:

| Source | Implementation leakage                                                                                                                                                                                         | Product-safe reading                                                                                                                                                                      |
| ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| M-04   | SQL query text, table/column names, proposed event and transition stores, named Java history services                                                                                                          | Show live sibling cases, preserve an auditable workflow history, and avoid duplicate user entry                                                                                           |
| M-05   | Proposed run/result/override tables, named lookup service, exact status storage                                                                                                                                | Interpret readings against the selected standard, preserve originals, require review, and expose analyzer/QC states                                                                       |
| M-07   | Entity/table projection language, unused assignment-column guidance, and "manual overrides in 1A" without a defined confirmation action or structured category                                                 | Present one shared state-driven queue with culture and AST views and no default ownership; count only resistance classifications with explicit provenance                                 |
| M-12   | Proposed linkage schema, UUID-shaped component signature, named inventory classes/routes, plus contradictory required/optional/substitute acceptance despite delegating linkage administration to Test Catalog | Reuse the application’s reagent inventory, select safe lots consistently, retain consumption provenance, and require the shared catalog to define any selection policy before enforcement |
| M-NFR  | Prescribed local queue and merge mechanics                                                                                                                                                                     | Preserve work during connectivity loss and give users a deterministic conflict-resolution experience                                                                                      |

These are not objections to the requested features. They are safeguards against
letting a mock or product FRS force a schema, service, or component boundary.

## Evidence Rule

For each required row, the implementation task must identify:

1. the pinned OpenELIS Work source;
2. the product requirement;
3. the production code path;
4. focused unit/component/service evidence;
5. a Playwright journey through configured navigation;
6. a separate Grist story and human ruling;
7. the deployed application and checklist revisions.

Direct navigation to an unsupported/legacy route, label-only assertions, API
inspection in place of visible behavior, or an unreviewed screenshot cannot
promote a behavior to Complete.

## R1 M-04 Classification Evidence

- **Code:** `cfbdb9025` implements the transactional workflow transition,
  named conflict behavior, authenticated actor history, final lock, held
  profile actions, canonical section state, and sibling links.
- **Fixture and journey:** `a10a2d13e` provisions the unassigned and typed
  sibling cases through application services and registers the visible
  classification journey in the `core-app` Playwright project.
- **Automated checks passed locally:** focused
  `MicrobiologyUatScenarioServiceTest` and `MicroCaseWorkflowServiceTest`;
  Playwright project-registration validation; selector-policy scan; source
  formatting and `git diff --check`.
- **Still open:** no OpenELIS stack was listening on local HTTPS, so the new
  Playwright journey has not run against this exact SHA. Deployment, separate
  Grist story publication, live overlay verification, and human UAT remain
  pending.

## R1 M-04 Inoculation And Subculture Evidence

- **Code:** `131ba1471` adds one durable inoculation record with a nullable
  same-case parent, Method reference, authenticated actor, media/container
  details, automatic typed activity, and shared inventory usage in one service
  transaction. It removes the unreachable generic setup form rather than
  retaining two write paths.
- **Migration:** `070-microbiology-inoculation-lineage.xml` is the only schema
  change for this slice and includes rollback. Its XML and test harness compile;
  focused PostgreSQL update/rollback execution has not yet been rerun in this
  checkpoint.
- **Automated checks passed locally:** focused service/controller/architecture
  tests, Java main and test compilation, 19 microbiology frontend files with 67
  tests, targeted ESLint, XML validation, Playwright project registration and
  selector-policy scan, formatting, and `git diff --check`.
- **Still open:** the registered primary/subculture Playwright journey has not
  run against this exact SHA because no local OpenELIS HTTPS stack is running.
  Desktop/mobile visual comparison, separate Grist story, deployed overlay,
  migration runtime, and human UAT remain pending.

## R1 M-04 Timeline Note Evidence

- **Code:** `cf8ac8c21` limits manual timeline entry to Add note, stores the
  content as an INTERNAL Note bound to the existing SampleItem with a stable
  case subject, and collates it chronologically with typed case activities.
  Shared Note auditing remains authoritative; no microbiology note table or
  second generic event writer was added.
- **Automated checks passed locally:** focused timeline service/controller and
  architecture tests, Java test compilation, 20 microbiology frontend files
  with 68 tests, targeted ESLint, and registered Playwright source/policy
  validation.
- **Still open:** the exact-SHA Playwright run, visual comparison, separate
  Grist story, deployment/overlay verification, and human UAT.

## R1 M-04 Two-Pass Isolate Evidence

- **Code:** `14164589f` records Gram stain and colony morphology before organism
  identification, requires identification method and confidence, keeps AST
  unavailable until confirmed identification, and projects preliminary work-up
  into the standard report path. Existing amendment history now also preserves
  identification method and confidence.
- **Migration:** `071-microbiology-isolate-workup.xml` contains only the durable
  isolate/history fields required by this behavior and includes explicit
  rollback. XML validation and the rollback harness passed compilation; runtime
  update/rollback has not yet been rerun in this checkpoint.
- **Automated checks passed locally:** 50 focused backend tests; 20 microbiology
  frontend files with 69 tests; targeted ESLint; Playwright project-registration
  and selector-policy validation; Spotless, XML validation, and `git diff --check`.
- **Still open:** the registered journey has not run against this exact SHA.
  Desktop/mobile mock comparison, a separate Grist story, deployment/overlay
  verification, ORM/migration runtime execution, and human UAT remain pending.

## R1 M-04 Critical And Nonconformance Evidence

- **Critical entry points:** `28669ef18` adds canonical case- and isolate-target
  entry actions while retaining the existing clinical communication and Alert
  synchronization path.
- **Shared backend:** `b2ad1013a` makes existing NCE creation atomic, derives the
  reporter from the authenticated request, removes GET-time draft persistence,
  and adds a reusable service-layer sample rejection operation. It creates no
  microbiology-specific NCE or rejection storage and requires no migration.
- **Case actions:** `f7aa5e20e` adds Carbon Report NCE and Mark Lost workflows,
  canonical `section`/`action` state, configured category/type/reporting-unit
  choices, an explicit missing-lost-type blocker, and terminal case visibility.
- **Case context parity:** `f3f1f3a4f` makes an unscoped case resolve to its
  current work section and drives the sticky action from the same projection.
  `a41ad5617` completes the collapsed order/origin/requesting-location/set/
  exposure summary, keeps Clinical History first when expanded, resolves Last
  activity by for display, defaults ordinary case NCE entry to the configured
  Pre-analytical category, and renders the linked NCE count in the header. The
  existing SampleOrganization is exposed as Requesting location because the
  repository has no separate microbiology ward field; no duplicate location or
  NCE storage was introduced.
- **Automated checks passed locally:** focused JUnit service/controller tests;
  23 microbiology frontend files with 94 tests; targeted ESLint; Playwright
  `core-app` registration and selector-policy validation. `8e3337870` registers
  browser journeys for flag-only NCE and a separate destructive lost-specimen
  scenario without SQL fixtures, fixed primary keys, or arbitrary waits.
- **Still open:** the new Playwright journey has not run against this exact SHA;
  Grist stories, deployed overlay verification, stable desktop/mobile mock
  comparison, and human UAT are pending. The sibling-loss cascade remains the
  explicit product ruling above.

## R1 M-05 Ordered Panel Provenance Evidence

- **Code:** `92fb708c8` resolves the identified organism's configured AST panel
  on the server, presents it as a confirmation rather than an arbitrary first
  dropdown choice, requires a reason before selecting a different panel, and
  persists the selected panel/version/provenance/reason plus the selected
  breakpoint-standard version on the run. Repeat/retest runs carry those
  snapshots forward. The actor remains request-derived.
- **Migration:** `072-microbiology-ast-provenance.xml` adds only the immutable
  run snapshot fields required by this behavior and includes explicit rollback.
  `073-microbiology-ast-reading-provenance.xml` adds only reading source,
  matched-basis, and unit fields needed to retain and display the interpretation
  context. `074-microbiology-ast-override-history.xml` adds the append-only
  override/revert event record. `075-microbiology-ast-technique.xml` adds the
  durable technique, backfills historical rows to explicit legacy-unspecified
  values, and constrains future values.
  `076-microbiology-ast-ordered-antibiotics.xml` snapshots exact ordered panel
  membership per run and backfills existing runs from their current panel as
  the only available historical source.
  `077-microbiology-ast-analyzer-lifecycle.xml` adds only the run/reading
  lifecycle and provenance needed for analyzer review and removes the invalid
  one-reading-per-run/antibiotic constraint so revised readings remain valid.
  `078-analyzer-event-reconciliation.xml` adds the shared durable, idempotent
  event envelope. Migrations `072` through `078` have explicit rollback
  harnesses and pass update/rollback/reapply execution against PostgreSQL.
  `079-microbiology-positive-culture-stage.xml` adds the missing positive stage
  and reconciles the existing stage constraint; its rollback harness is
  committed, while local execution is blocked by the unavailable Docker daemon.
- **Automated checks passed locally:** focused `MicroAstServiceTest`,
  `MicroAstRestControllerTest`, `MicroBreakpointAdminServiceTest`, ORM and
  reference-data integration tests; all seven AST PostgreSQL migration
  update/rollback tests; focused Carbon ordered-set interaction coverage; 23
  microbiology frontend files with at least 97 tests; targeted
  ESLint, Prettier, Spotless, Liquibase XML validation, contract YAML and
  reference validation, and `git diff --check`.
- **Ordered-set closure:** `1a198a765` snapshots exact panel membership,
  preserves it on repeats, rejects readings outside it, and restricts the
  Carbon entry control. `2dfb8221b` adds a controlled Carbon adjustment for
  panel switching and individual drug add/drop; the server validates active
  additions and requires one actor-linked reason for any changed set.
  `df56940f9` blocks review until every ordered drug has a reading and limits
  patient-report projection to one latest reading per snapshotted drug in
  snapshot order, excluding superseded and unordered rows. The repo has no
  authoritative Antibiotic-to-Test mapping, so the linked culture `Analysis`
  remains the standard patient-report anchor rather than creating unsupported
  per-drug analyses.
  **Focused closure checks:** 32 Java tests and 11 Carbon tests passed for
  adjustment, followed by 36 Java tests and 12 Carbon tests for complete-set
  reconciliation; Spotless, Prettier, targeted ESLint, and `git diff --check`
  passed.
  `b0dd23456` persists and displays manual-entry source, organism/group/specimen
  match basis, and measurement units with local-SOP guidance when no breakpoint
  matches. `72ef3b152` closes T243 by separating laboratory technique from
  MIC/zone measurement type, deriving measurement server-side, and matching
  technique-aware breakpoints before explicit legacy fallback; `dd7e63216`
  exposes the corrected reading response name. `30d8e988a` adds immutable
  actor/time/reason history, inline original-to-override display, and a
  justified Validation/Admin revert without modifying the original reading.
  `5f1b89922` adds analyzer awaiting/results/QC states, provenance, acceptance
  blockers, supervised resolutions, and preserved invalidation/repeat.
  `0c33223fb` routes normalized result and QC events through an idempotent
  durable envelope and lists failed events in the existing Analyzer Import
  Issues surface. `87f09d0bc` adds Carbon setup/review/recovery states with 16
  focused interaction tests; targeted ESLint and Prettier pass.
  `ee4e4c6e2`, `f0d51a5cd`, and `c7d84c535` add whole-panel/single-drug scope
  and converge case and NCE Retest entry on the same preserved-run service,
  with 45 backend and 17 focused Carbon/NCE tests. No migration was needed
  because the ordered-work snapshot already represents scope.
  `ca7aa0491` adds server-derived active-run completion, awaiting-setup, and
  pending-identification counts, excludes preserved invalid history from the
  denominator, renders the combined M-04 count, and closes the previously
  missing unidentified-isolate final-release blocker.
  `88afdd369` also closes the worklist repeat origin through the same
  preserved-run service. **Still open:** exact-SHA Playwright, separated Grist
  stories, deployment, visual comparison, and human UAT.
