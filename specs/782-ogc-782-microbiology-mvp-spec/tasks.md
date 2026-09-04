# OGC-782 Microbiology Execution Roadmap

This is the single execution roadmap and the only delivery-status document for
OGC-782. Detailed implementation history belongs in Git and pull requests, not
in a second roadmap.

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
step totals, titles, or checklist revisions into repository specifications. An
exported review report may retain its exact revision as immutable acceptance
evidence.

Dated evidence reports, old gap analyses, slice-status notes, code-qa output,
and subordinate task ledgers are review artifacts. Do not maintain them as
parallel status documents.

## Roadmap Principles

1. **Repository specifications are authoritative.** Product behavior belongs
   in `spec.md`; technical design belongs in `plan.md` and its supporting
   repository specifications.
2. **This roadmap is the single execution control.** Maintain current scope,
   order, blockers, and coarse delivery state here. Do not create or revive a
   second status ledger.
3. **OpenELIS Work is functional and visual input only.** Use it for actors,
   workflows, interactions, and visual intent. Do not inherit implementation
   suggestions as requirements.
4. **Resolve drift into the repository spec.** Reconcile `spec.md` before code
   when functional intent changes or conflicts. Ask Piotr only when behavior is
   genuinely ambiguous.
5. **Do not mirror transient metadata.** Commit identifiers, deployment
   identifiers, test totals, Grist revisions, and checklist inventories remain
   in their native systems or immutable acceptance reports.
6. **Grist owns live UAT.** Repository tests prove application and Review
   integration behavior; they do not duplicate the current Grist catalog.
7. **Validation must be proportional.** Use focused TDD, relevant integration
   and Playwright coverage, and one meaningful slice-level code-qa pass.
8. **Only runtime value justifies deployment.** Deploy coherent user-visible
   behavior for review, not documentation-only or test-only changes.
9. **One product stack is authoritative.** Remediation code belongs in its
   retained product slice. Generic development tooling belongs in its independent
   PR; review-overlay and checklist-authoring code belongs in
   `openelis-review-tooling`.
10. **Preserve behavior while simplifying history.** A stack reconstruction may
    change commits and PR bases, but not stable routes, public contracts, or
    clinical behavior without an explicit spec change.

## Iteration Status

- `[ ]` means a future iteration that has not started.
- `[*]` means the single iteration currently being worked.
- `[x]` means implementation and focused automated validation are complete,
  required pull-request checks are green, user-visible runtime behavior is
  deployed to the review host, and the iteration is ready for user validation.
- `[✓]` means the iteration has been explicitly validated by the user.

Keep only these four durable markers here. Detailed PR and CI state stays in
GitHub, deployment state stays on the review host, and UAT results stay in
Grist.

## Active Iteration

- [*] **Consolidate the product stack and recover CI**

  Preserve the cumulative OGC-782 behavior while replacing the remediation
  chain with one independently merged specification PR, one independent
  development-tooling PR, and this official eight-PR product stack:

  | Order | PR | Owning behavior |
  | ----- | -- | --------------- |
  | 1 | #3789 | Foundations, data model, order routing, service-created fixtures, and supported order-save proof |
  | 2 | #4134 | Case workbench, isolates, manual AST, review, overrides, and repeat behavior |
  | 3 | #4135 | Worklist, canonical URLs, breadcrumbs, critical communication, and Alert synchronization |
  | 4 | #4136 | No-growth review, preliminary/final release, patient-report projection, and final locking |
  | 5 | #3972 | Amendments, re-identification, repeat AST metadata, reagent/card lots, accessibility, and performance |
  | 6 | #3981 | Reference administration, breakpoint configuration, culture defaults, and culture purpose |
  | 7 | #4092 | Analyzer-produced AST, provenance, QC review, reconciliation, and secured machine ingress |
  | 8 | #3984 | WHONET mapping, export, filters, AST scope, and first-isolate policy |

  The active iteration is complete only when:

  1. #3782 contains feature and engineering specifications only and is green and
     mergeable independently.
  2. Generic isolated-stack, Compose readiness, CI readiness, and reusable
     analyzer-harness work is green and mergeable in one independent tooling PR.
  3. The eight retained branches form the exact order above in GitHub and the
     cumulative top preserves the prior top's valuable behavior. Equivalence is
     verified by `git range-diff` and path-limited tree comparison.
  4. Reviewed AST runs reject mutation, a newer unreviewed repeat blocks
     readiness, and breakpoint provenance has no hardcoded historical fallback.
  5. The worklist is paged in the database, significant reviewed isolates route
     to case review, and failed critical-communication submission preserves the
     form and presents the server error.
  6. Preliminary release is rejected after finalization, release clients reject
     server errors, and WHONET readiness requires finalized reportable data.
  7. Analyzer ingress uses protected CI credentials, stateless machine
     authentication, and service-created fixtures.
  8. Focused backend, frontend, migration/ORM, and registered Playwright checks
     pass in each owning slice; the cumulative backend, frontend, and E2E
     checkpoints pass without SQL fixture seeding, fixed IDs, arbitrary waits,
     forced interactions, or hidden legacy failures.
  9. Every Copilot/Codex review finding is fixed or explicitly rebutted and its
     GitHub thread is formally resolved.
  10. GitHub reports **Able to merge as a stack**, superseded remediation PRs
      link to their retained owner before closure, the worktree is clean, and no
      more than one local container stack is running.

  Remediation ownership is fixed as follows:

  | Superseded work | Retained owner |
  | --------------- | -------------- |
  | #4075 | #3789 |
  | Workbench and AST portions of #4004 and #4051 | #4134 |
  | #4091 and worklist/communication portions of #4004 and #4051 | #4135 |
  | #4074 and no-growth/release portions of #4004 and #4051 | #4136 |
  | #4085 and clinical/NFR portions of #4004 | #3972 |
  | #4117 and reference/culture-purpose portions of #4004 | #3981 |
  | #4116 and analyzer portions of #4004 | #4092 |
  | #4097, #4103, #4120, #4124, and WHONET portions of #4004 | #3984 |
  | Generic development tooling from any product/remediation PR | Independent tooling PR |

## Approved Functional Rulings

### Separate No-Growth Review And Release

1. **Record no growth** records the authenticated actor, time, and bench outcome
   and moves the case to a clearly named review-ready state.
2. Recording no growth does not create or publish a patient result.
3. The case presents final review and release as the separate next action.
4. Only an authorized reviewer can release the final negative through the
   standard patient-report path.
5. Release records the reviewer and time and locks ordinary culture, isolate,
   AST, and protocol mutation.
6. Missing report content or prerequisites produce specific blockers and no
   partial release.

### Worklist And Surveillance Boundaries

1. Culture purpose is explicit: Clinical diagnosis/treatment, Active
   screening/carriage, or Unspecified for historical records. It is not inferred
   from arbitrary program names.
2. WHONET export is reached through one Reports navigation entry that points to
   the stable existing route; the page is not duplicated.
3. Reporting-period membership uses specimen collection date. Operational
   worklist state does not silently change the export population.
4. Advanced first-isolate controls cover 7/14/30-day windows, date basis, source
   scope, contaminant handling, and susceptibility-profile sensitivity.
   Episode-based behavior waits for a defined functional episode boundary.

### Shared Capabilities

Macro Library is a separate OGC-788 product stack. Microbiology consumes its
approved clinical macros after the shared capability is available; OGC-782 does
not own macro authoring or administration.

## Future Iterations

- [ ] **Human acceptance and stack merge** - Run the current Grist stories on
  the deployed top, remediate real findings in manageable owning slices, and
  merge the product stack bottom-up.
- [ ] **Shared offline and conflict behavior** - Adopt an application-wide
  offline pattern; do not build a microbiology-only queue or replay system.
- [ ] **Configurable worklist refresh** - Make the refresh interval deployment
  configurable while preserving canonical URL state, focus, scroll, row
  context, and offline pause behavior.
- [ ] **Shared-specimen loss ruling** - Decide how a lost physical specimen
  affects sibling workflows, especially after a sibling is released or amended.
- [ ] **Reagent policy dependency** - Enforce required, optional, and substitute
  reagent behavior after Test Catalog provides an authoritative shared policy.
- [ ] **Remaining WHONET readiness and qualification** - Complete patient
  origin/type, department, breakpoint, and phenotype repair; qualify supported
  CSV/TXT and antibiotic-column contracts against a current WHONET import.
- [ ] **Episode-based first-isolate selection** - Implement only after the
  functional episode boundary is approved.
- [ ] **Scheduled surveillance delivery** - Add saved filters, history,
  scheduling, failure notification, and approved transport after product and
  deployment rulings.
- [ ] **Expert rules and macro-driven workflows** - Implement after the shared
  Macro Library consumer contract is available.
- [ ] **Operational mycobacteriology and TB** - Deliver the separate M-14
  workflow on the bacteriology/reference foundation.
- [ ] **Antibiogram reporting** - Deliver M-13 after routine bacteriology and
  reference data are accepted.
- [ ] **FHIR, GLASS, and shared analytics outputs** - Build on stable AMR and TB
  outputs without implying direct WHO submission or a microbiology-only
  dashboard.

## Iteration Contract

For every implementation slice:

1. Read the relevant repository specification and this roadmap.
2. Compare the affected workflow and mock in OpenELIS Work.
3. If behavior is ambiguous or contradictory, record one concise clarification
   here and ask Piotr before coding. Do not guess from Jira or implementation
   language in a mock.
4. Write focused failing tests at the service, controller, component, and E2E
   levels appropriate to the behavior.
5. Implement the smallest coherent change using existing OpenELIS patterns.
6. Use migrations only for data-model changes. Build test fixtures through
   services; never seed feature tests with SQL, fixed IDs, or DAO bypasses.
7. Use semantic Carbon interactions in frontend tests and Playwright. Wait on
   observable readiness, never arbitrary timeouts or forced actions.
8. Validate locally before deployment: focused tests, formatting, migration/ORM
   checks when relevant, registered Playwright, and visual comparison for UI.
9. Run `tools/code-qa` once at the completed slice boundary.
10. Update Grist for human UAT without copying its changing catalog into the
    repository. Review-tooling behavior changes belong in
    `openelis-review-tooling`.
11. Update only this roadmap row and the owning PR description. Do not create a
    second status ledger.
12. Commit and push a coherent slice promptly. Deploy only when it contains
    reviewable runtime behavior.

## PR Snapshot Contract

PR descriptions contain the user-visible slice and stack position, implemented
behavior and exclusions, validation categories, real blockers, deployment and
human-UAT state in plain language, and links to the specification, plan, and
roadmap. Do not copy commit identifiers, deployment identifiers, Grist
revisions, checklist inventories, or continuously changing test totals into PR
prose.
