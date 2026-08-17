# OGC-782 Microbiology Execution Roadmap

This is the single execution roadmap and the only delivery-status document for
OGC-782. It is intentionally concise. Detailed implementation history belongs
in Git, pull requests, and immutable evidence reports, not in a second roadmap.

## Authority Chain

1. Repository-owned feature and engineering specifications:
   - [Feature specification](./spec.md)
   - [Engineering plan](./plan.md)
   - [Research](./research.md)
   - [Data model](./data-model.md)
   - [API contract](./contracts/microbiology-openapi.yaml)
   - [Clinical completeness specification](../782-ogc-782-microbiology-m8-clinical-completeness/spec.md)
   - [Reference administration specification](../782-ogc-782-microbiology-m9-reference-mapping-admin/spec.md)
   - [WHONET export specification](../782-ogc-782-microbiology-m10-whonet-export/spec.md)
2. This roadmap, derived from those specifications.
3. [OpenELIS Work microbiology artifacts](https://github.com/DIGI-UW/openelis-work/tree/main/designs/microbiology)
   for functional requirements, workflows, mocks, and visual intent only.

OpenELIS Work does not dictate schema, APIs, routes, component structure, or
service ownership. When its functional intent differs from repository specs,
record the ambiguity here and obtain a product ruling before implementation.

Grist is the live source of truth for UAT. Do not copy changing story totals,
step totals, titles, or checklist revisions into repository specifications.
An exported review report may retain its exact revision as immutable acceptance
evidence.

Files under `evidence/`, older gap analyses, slice-status notes, code-qa reports,
and subordinate milestone task ledgers are historical records. Do not maintain
them as parallel status documents.

## Roadmap Principles

These principles govern every future OGC-782 iteration and change only through
an explicit product or engineering ruling:

1. **Repository specifications are authoritative.** Product behavior belongs
   in `spec.md`; technical design belongs in `plan.md` and its supporting
   repository specifications.
2. **This roadmap is the single execution control.** Maintain current scope,
   order, blockers, and coarse delivery state here. Do not create or revive a
   second status ledger.
3. **OpenELIS Work is functional and visual input only.** Use it to understand
   actors, workflows, interactions, and visual intent. Do not inherit its table,
   API, route, class, ownership, or storage suggestions as requirements.
4. **Resolve drift into the repository spec.** When OpenELIS Work changes or
   conflicts with implemented behavior, identify the product implication and
   reconcile `spec.md` before changing code. Ask Piotr when the intended
   behavior is genuinely ambiguous.
5. **Do not mirror transient metadata.** Commit identifiers, deployment
   identifiers, test totals, Grist revisions, and checklist inventories belong
   in their native systems or immutable acceptance reports, not in living specs,
   the roadmap, or PR prose.
6. **Grist owns live UAT.** Repository tests prove Review integration and
   application behavior; they do not duplicate the current Grist catalog.
7. **PR descriptions are snapshots derived from this roadmap.** Keep them
   concise and update them when scope, blockers, deployment state, or human
   acceptance materially changes, not after every commit or test run.
8. **Evidence is immutable, not operational.** Prior gap analyses, visual
   comparisons, code-qa reports, videos, and exported UAT reports remain useful
   evidence but never compete with this roadmap for current status.
9. **Validation must be proportional.** Use focused TDD, relevant integration
   and Playwright coverage, and one meaningful slice-level code-qa pass. Avoid
   repetitive validation and bookkeeping that do not reduce implementation or
   acceptance risk.
10. **Only runtime value justifies deployment.** Develop and validate locally;
    deploy a coherent user-visible slice for review, never documentation-only or
    test-only changes.

## Status Vocabulary

- **Planned**: scope and acceptance behavior are clear; implementation has not
  started.
- **In progress**: tests or implementation are underway.
- **Review-ready**: implementation and focused automated validation are
  complete; human acceptance or merge remains.
- **Accepted**: required human review is complete and the slice is merged.
- **External dependency**: another product or platform decision must land
  first.

## Current Baseline

The official microbiology PR chain is:

`#3789 -> #3972 -> #3981 -> #3984 -> #4004 -> #4051`

| Slice | Durable scope | Status |
| --- | --- | --- |
| Routine bacteriology MVP (#3789) | Test configuration, order routing, case workbench, isolates, manual AST, shared worklist, critical communication, report propagation, final lock, WHONET readiness | Review-ready |
| Clinical completeness (#3972) | Amendment and re-identification history, repeat/retest AST, reagent/card-lot traceability, initial NFR qualification | Review-ready |
| Reference administration (#3981) | Organism and antibiotic vocabularies, AST panel versions, culture defaults, breakpoint lifecycle and guarded import | Review-ready |
| WHONET export (#3984) | Readiness, mapping repair, preview, and audited manual CSV export | Review-ready |
| Functional alignment (#4004) | Supported order flow, complete bench workflow, AST provenance and review, shared Culture/AST worklist, reagent selection, accessibility and security corrections | Review-ready |
| Order and bench alignment (#4051) | Current order context, protocol handling, visible inoculation/subculture/culture-progression actions, and deterministic navigation | Review-ready |

The top runtime implementation is available on the AMR review site. Later
documentation or test-only changes are intentionally not deployment events.
Human UAT remains pending in Grist and is not inferred from automated checks.

Macro Library is a separate OGC-788 product stack, not another OGC-782
milestone. Microbiology consumes approved clinical macros after that shared
capability is available; it does not own macro authoring or administration.

## Baseline Reconciliation

- [x] Establish the authority chain above.
- [x] Keep `spec.md` behavior-focused and `plan.md` engineering-focused.
- [x] Make this file the sole execution and delivery-status roadmap.
- [x] Freeze prior status, gap-analysis, mock-comparison, and code-qa documents
  as historical evidence.
- [x] Record the approved separate no-growth review/release behavior in the
  repository specification and OpenELIS Work functional artifacts.
- [x] Reduce the deployed Review integration test to durable routing/loading
  behavior without mirroring the live Grist catalog.
- [x] Publish the OpenELIS Work functional clarification for review.
- [x] Update PR #4051 with a concise snapshot linking to `spec.md` and this
  roadmap; do not copy transient commit, deployment, or UAT inventory data.
- [x] Commit and push the reconciliation with a clean worktree.

## Next Slice: Separate No-Growth Review And Release

**Status**: Planned

**Delivery**: Implement as the next official stacked PR based on #4051. Do not
expand #4051's runtime scope during baseline reconciliation.

**Goal**: Make the routine negative-culture path clinically explicit. Recording
the bench observation must not also perform the authorized report release.

### Acceptance Criteria

1. On an incubating case, **Record no growth** records the authenticated actor,
   time, and bench outcome in the case history.
2. Recording no growth moves the case to a review-ready state and creates no
   patient result or final report.
3. The case clearly presents the separate next action to review and release the
   final negative report.
4. A user without final-release authority cannot release the report, and a
   submitted actor identifier cannot replace the authenticated actor.
5. An authorized reviewer can release the final negative report through the
   standard patient-report path.
6. Final release records the reviewer and time, publishes the negative result,
   and locks culture, isolate, AST, and protocol mutation.
7. Later growth after final negative release uses the controlled amendment
   path and preserves the prior final report.
8. The case route and active section remain bookmarkable and refresh-stable;
   keyboard focus and status announcements follow existing Carbon patterns.

### TDD Tasks

- [ ] Add failing service tests for no-growth recording, review readiness, no
  patient-result projection, authorized final release, and final-case lock.
- [ ] Add failing controller/security tests for authenticated actor derivation,
  release permission, spoofed actor input, and named conflict responses.
- [ ] Add failing report-projection integration coverage proving that only final
  negative release reaches the existing patient-result path.
- [ ] Add failing Carbon interaction tests for the two distinct actions,
  review-ready status, release blockers, focus, and locked controls.
- [ ] Add a registered `core-app` Playwright journey using service-created
  fixtures, accessible Carbon interactions, response/DOM readiness, and no
  arbitrary waits or forced actions.
- [ ] Inspect the current durable state before changing the model. Add a
  Liquibase migration only if the existing case/activity model cannot preserve
  the review-ready no-growth state; include rollback and ORM validation if a
  migration is required.
- [ ] Implement the smallest service, controller, and UI changes that satisfy
  the tests and reuse the existing final-release/report infrastructure.
- [ ] Compare stable desktop and mobile states with the OpenELIS Work M-04
  specification and prototype; record only intentional functional deviations.
- [ ] Run focused backend, frontend, Playwright, formatting, and diff checks.
- [ ] Run the relevant `tools/code-qa` alignment, meaningful-coverage, and
  simplicity checks once for the completed slice.
- [ ] Update or add focused Grist UAT stories after the local flow passes. Keep
  each coherent reviewer outcome separate and do not mirror Grist inventory in
  this file.
- [ ] Deploy only the runtime-bearing top-of-stack change, run automated
  pre-UAT, and hand the live story to the human reviewer.

## Remaining Roadmap

### Phase 1A Closure

1. **Accept the current stack** - Complete human UAT in Grist, remediate real
   findings in manageable slices, and merge the stack bottom-up.
2. **Supported order-save integration proof** - Add the missing direct
   integration test around the complete supported save path without SQL,
   fixed primary keys, or DAO bypass.
3. **Clinical and NFR qualification** - Finish representative-volume worklist
   and case measurements, keyboard/screen-reader review, and a clear decision
   on shared offline/conflict behavior. Do not build a microbiology-only offline
   queue.
4. **Analyzer ingress security** - Introduce a least-privilege Bridge service
   identity and prove legitimate delivery plus unrelated-user denial. Coordinate
   this with the analyzer workstream rather than inventing an AMR-only role.
5. **Reagent policy dependency** - Enforce required, optional, and substitute
   reagent behavior only after Test Catalog provides an authoritative shared
   policy model; do not infer it from legacy role names.
6. **Close remaining worklist decisions** - Validate explicit resistance
   classification provenance and the intended disposition of already-reviewed
   AST work against the feature specification before implementation.

### Phase 1B Clinical Depth

1. Implement expert rules and macro-driven bench workflows after the shared
   Macro Library consumer contract is available.
2. Complete analyzer-result review and QC with representative instrument
   traffic and reconciliation evidence.
3. Complete WHONET packaging, remaining vocabulary mappings, scheduling, and
   delivery beyond the current manual export.
4. Complete any remaining reference-data administration required by those
   workflows.

### Full Module

1. Operational mycobacteriology/TB workflow (M-14).
2. Antibiogram reporting (M-13).
3. GLASS reporting through consolidated FHIR after routine bacteriology,
   WHONET, and TB outputs are stable.
4. Catalog subscription or cross-site distribution capabilities as separate
   platform work, consumed by microbiology rather than owned by it.

## Dependency Order

1. Baseline reconciliation.
2. Separate no-growth review/release.
3. Human acceptance of the routine bacteriology stack.
4. Phase 1A closure work that does not depend on another platform.
5. Shared dependencies: Macro Library, Test Catalog reagent policy, Bridge
   service identity, and shared offline behavior.
6. Phase 1B clinical depth.
7. TB and antibiogram work after the routine/reference foundation is accepted.
8. GLASS last, after its source data and upstream outputs are stable.

## Iteration Contract

For every implementation slice:

1. Read the relevant repository specification and this roadmap.
2. Compare only the affected workflow and mock in OpenELIS Work.
3. If behavior is ambiguous or contradictory, record one concise clarification
   here and ask Piotr before coding. Do not guess from Jira or implementation
   language in a mock.
4. Write focused failing tests at the service/controller/component/E2E levels
   appropriate to the behavior.
5. Implement the smallest coherent change using existing OpenELIS patterns.
6. Use migrations only for data-model changes. Build test fixtures through
   services; never seed feature tests with SQL, fixed IDs, or DAO bypasses.
7. Use Carbon-accessible interactions in frontend tests and Playwright. Wait on
   observable readiness, never arbitrary timeouts.
8. Validate locally before deployment: focused tests, formatting, migration/ORM
   checks when relevant, registered Playwright, and visual comparison for UI.
9. Run `tools/code-qa` once at the completed slice boundary, not as repetitive
   bookkeeping during development.
10. Update Grist for human UAT without copying its changing catalog into the
    repository. Review-tooling behavior changes belong in the review-tooling
    repository.
11. Update one roadmap row and the PR snapshot. Do not create another status
    ledger or rewrite historical evidence.
12. Deploy only when the slice contains reviewable runtime behavior. Do not
    deploy documentation-only or test-only commits.

## PR Snapshot Contract

PR descriptions should contain only:

- the user-visible slice and its place in the stack;
- implemented behavior and explicit exclusions;
- validation categories completed and any real blocker;
- deployment and human-UAT state in plain language;
- links to `spec.md`, `plan.md`, and this roadmap.

Do not copy commit identifiers, deployment identifiers, Grist revisions,
checklist inventories, or continuously changing test totals into PR prose.
