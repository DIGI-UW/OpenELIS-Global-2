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

## Iteration Status

This roadmap uses four simple iteration markers:

- `[ ]` means a future iteration that has not started.
- `[*]` means the single iteration currently being worked.
- `[x]` means implementation is complete and ready for user validation.
- `[✓]` means the iteration has been validated by the user.

Keep only these durable markers in the roadmap. Detailed PR and CI state stays
in GitHub, deployment state stays on the review host, and UAT results stay in
Grist.

### Finished Iterations

- [x] **Routine bacteriology MVP (#3789)** - Test configuration, order routing,
  case workbench, isolates, manual AST, shared worklist, critical communication,
  report propagation, final lock, and WHONET readiness.
- [x] **Clinical completeness (#3972)** - Amendment and re-identification
  history, repeat/retest AST, reagent/card-lot traceability, and initial NFR
  qualification.
- [x] **Reference administration (#3981)** - Organism and antibiotic
  vocabularies, AST panel versions, culture defaults, breakpoint lifecycle, and
  guarded import.
- [x] **WHONET export (#3984)** - Readiness, mapping repair, preview, and audited
  manual CSV export.
- [x] **Functional alignment (#4004)** - Supported order flow, complete bench
  workflow, AST provenance and review, shared Culture/AST worklist, reagent
  selection, and accessibility/security corrections.

### Completed Baseline

- [x] **R2 baseline reconciliation (#4051)** - Establish one authoritative
  roadmap, align the durable specifications, and add the approved no-growth
  review/release behavior to functional artifacts and live UAT without
  changing application behavior.

This iteration is complete because:

- The authority chain, repository spec, engineering plan, OpenELIS Work
  functional intent, and roadmap agree.
- Repository tracing confirms that recording no growth is audited and
  review-ready without publishing, while supervisor release projects the final
  negative and locks the case.
- One dedicated, reviewer-executable Grist story uses its own service-created
  fixture and is available in the live AMR Review overlay.

This baseline does not change application behavior, deploy, or watch CI.

Macro Library is a separate OGC-788 product stack, not another OGC-782
iteration. Microbiology consumes approved clinical macros after that shared
capability is available; it does not own macro authoring or administration.

### Approved No-Growth Behavior

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
7. The case route and active section remain bookmarkable and refresh-stable;
   keyboard focus and status announcements follow existing Carbon patterns.

## Roadmap Status

### Phase 1A Closure

- [x] **No-growth deterministic acceptance proof** - Add focused service,
  Carbon, and registered `core-app` Playwright coverage for the approved
  no-growth path without arbitrary waits or forced actions. Change runtime code
  only if this evidence exposes a real defect; do not deploy test-only changes.
- [ ] **Accept the current stack** - Complete human UAT in Grist, remediate real
   findings in manageable slices, and merge the stack bottom-up.
- [x] **Supported order-save integration proof** - Add the missing direct
   integration test around the complete supported save path without SQL,
   fixed primary keys, or DAO bypass.
- [x] **Initial clinical and NFR qualification evidence** - Establish
   statistically meaningful representative-volume measurements and complete
   the automated keyboard/screen-reader review, fixing defects exposed by that
   evidence.
- [ ] **Shared offline/conflict behavior** - Adopt an application-wide offline
  pattern when its ownership is agreed; do not build a microbiology-only queue.
- [x] **Analyzer ingress security** - Replace general authenticated-user access
   to culture and AST analyzer-event writes with the existing machine-to-machine
   authentication pattern if one exists. An approved Bridge identity must be
   able to submit and audit an event; ordinary browser users must be denied;
   missing or invalid machine credentials must fail closed; and the existing
   stuck-event reconciliation path must continue to work. If the repository has
   no reusable machine-identity boundary, stop with the smallest viable options
   instead of inventing an AMR-only role.
- [ ] **Configurable worklist refresh** - Preserve the current thirty-second
   default while allowing each deployment to configure a value from ten to one
   hundred twenty seconds. Refresh must preserve canonical URL state, focus,
   scroll position, and current row context, and polling must pause while the
   application knows it is offline.
- [ ] **Shared-specimen loss behavior** - Obtain the product ruling for a lost
   physical specimen when sibling workflows share it, including whether a
   released or amended sibling blocks the action. Then align service behavior,
   case guidance, focused tests, and UAT; do not silently change the current
   behavior before that ruling.
- [ ] **Reagent policy dependency** - Enforce required, optional, and substitute
   reagent behavior only after Test Catalog provides an authoritative shared
   policy model; do not infer it from legacy role names.
- [x] **Reviewed AST worklist behavior** - Remove reviewed attempts from the
  default action queue, retain them in a bookmarkable Reviewed view with a
  read-only View action, and keep reasoned repeat/retest setup inside the case.

The active product iteration is M-09 reporting-period presets and the
AST-worklist export deep link in Phase 1B. Human acceptance and the shared
offline and reagent-policy dependencies remain independent work and do not
reorder that product sequence.

### Phase 1B Clinical Depth

- [ ] Implement expert rules and macro-driven bench workflows after the shared
   Macro Library consumer contract is available.
- [x] Complete analyzer-result review and QC with representative instrument
  traffic and reconciliation through the configured Admin -> Stuck analyzer
  events path.
- [x] Complete WHONET specimen-code validation, export projection, and exact
  repair navigation through the owning sample-type administration workflow.
- [x] Complete M-09 export population filters already supported by authoritative
  case data: specimen, organism, patient origin, and significance. Apply the
  same selection to server-side preview and generation, preserve canonical URL
  state, place the single export navigation entry under Reports, and compact the
  touched page toward the M-09 operational layout.
- [x] Capture Culture purpose on microbiology orders as Clinical
  diagnosis/treatment or Active screening/carriage, show it with the case, audit
  pre-release corrections, and complete the M-09 screening filter. New orders
  default visibly to clinical purpose; historical missing values remain
  Unspecified; screening and unspecified records require explicit inclusion.
- [*] Complete M-09 reporting-period presets and enable the AST-worklist Export
  to WHONET deep link so its active scope pre-fills the generator and can be
  cleared.
- [ ] Complete M-09 advanced first-isolate behavior behind a progressive
  disclosure: window length and basis, source scope, contaminant handling,
  repeat-row handling, and susceptibility-profile sensitivity.
- [ ] Complete M-09 readiness and repair for patient origins, patient types,
  departments, breakpoint standards, and phenotype flags, reusing each owning
  catalog and avoiding parallel mapping stores.
- [ ] Complete WHONET-compatible output qualification: current import
  validation, CSV/TXT choices, isolate-wide antibiotic columns, method suffixes,
  phenotype options, demographics policy, and first-destination lab-profile
  packaging.
- [ ] Complete export history and configure-once delivery with saved filters,
  monthly scheduling, failure/unmapped-item notification, and the approved
  deployment transport; resolve SFTP-primary/email-fallback behavior before
  implementation.
- [ ] Complete any remaining reference-data administration required by those
   workflows.

### Full Module

- [ ] Operational mycobacteriology/TB workflow (M-14).
- [ ] Antibiogram reporting (M-13).
- [ ] Publish finalized AMR and TB laboratory results through the shared FHIR
   pathway after routine bacteriology, WHONET, and TB outputs are stable.
- [ ] Produce validated, reproducible GLASS submission files and run history for
   an authorized person to review and upload; do not imply that OpenELIS submits
   directly to WHO.
- [ ] Supply the AMR indicator and data-quality inputs required by the shared
   analytics platform; do not create a separate OpenELIS dashboard unless a
   later product ruling requires one.
- [ ] Catalog subscription or cross-site distribution capabilities as separate
   platform work, consumed by microbiology rather than owned by it.

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
