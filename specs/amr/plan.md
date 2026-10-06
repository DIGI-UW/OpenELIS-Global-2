# AMR engineering plan

## Contents

- [Repository seams](#repository-seams)
- [Source and cutover gate](#source-and-cutover-gate)
- [Runtime disposition](#runtime-disposition)
- [Preservation and acceptance mapping](#preservation-and-acceptance-mapping)
- [Settled functional rules](#settled-functional-rules)
- [Execution rules](#execution-rules)
- [Exact verification gates](#exact-verification-gates)
- [Final integration gate](#final-integration-gate)

## Repository seams

| Concern           | Existing seam                                                                  | Engineering rule                                                                                                                                          |
| ----------------- | ------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Case aggregate    | `MicroCase`, `MicroCaseAnalysis`, `SampleItem`, `MicroOrderRoutingServiceImpl` | Evolve one aggregate with explicit specimen membership. One server grouping rule drives preview and save; preserve IDs/result ownership during migration. |
| Access            | `MicrobiologyCaseAccessServiceImpl`, micro REST controllers                    | Authorize every read/write against the case's working lab unit and action; moving a case does not rewrite catalog sections.                               |
| Context           | `MicroCaseOrderDetailServiceImpl`, Programs, `QuestionnaireStorageService`     | One response per order/program questionnaire; case and reception use it. Distinguish shared patient context from case-specific specimen context.          |
| Case tests        | Analysis/Result, result components, notes, existing chooser                    | Ordinary catalog results and validation; one result identity across placement/moves; no competing editor.                                                 |
| Culture           | `MicroCaseInoculationServiceImpl`, lineage, culture events                     | Row-local readings/outcomes/timing; derive case progress. No protocol/workflow classifier.                                                                |
| Media             | Test reagent links, Inventory items/lots                                       | Culture references are traceability, not stock consumption. Keep reagent/control consumption policy separate.                                             |
| AST/DST           | `MicroAstServiceImpl`, versioned panels, readings/attempts                     | Preserve measurements, QC and reasoned history; explicit interpretation and per-agent reporting, not workflow-type defaults.                              |
| Incoming/referral | Analyzer events, Pending Imports, existing referrals                           | Unmatched-to-case stays in the shared inbox; unmatched-to-row waits for explicit case placement. No guessed destination or duplicate result.              |
| Release/report    | Readiness, report versions/signatures, shared reporting                        | Separate work, validation and release; keep final locking/amendments; no short text substitute for structured printed output.                             |
| Shared extensions | Notes, callbacks, Workplan print records, labels, patient results              | Extend the existing owner; no AMR-only duplicate framework. Freeze each interface before its iteration.                                                   |
| Surveillance      | `MicroWhonetDatasetServiceImpl`, first-isolate selection                       | Facility selection/provenance only; no national compiler or structured electronic-delivery implementation here.                                           |

## Source and cutover gate

The scope and deletion mapping are settled. V00 aligns the functional sources,
removes competing guidance and verifies the gallery and tracking readback.
There is no deletion-confirmation or speculative policy blocker.

Before runtime implementation, freeze the slice's service boundaries and
one-way clinical/audit transformation from current code. V01 rehearses fresh
install, upgrade, collisions and rollback/reapply on disposable PostgreSQL.
Do not combine existing cases because a grouping key now matches. No production
legacy reader, dual write or retained configuration authority is permitted.
Applied Liquibase history remains immutable; future cutover changesets transform
required clinical/audit information and remove obsolete final-schema storage.
This documentation change does not execute those migrations.

## Runtime disposition

| Disposition      | Current families                                                                                                                                                                                      | Owner and cutover obligation                                                                                                                          |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| Retire           | `MicroWorkflowType`, `MicroCaseWorkflowService*`, workflow forms/actions; `MicroCultureSetup`, DAO/admin form, `MicroCaseProtocolService*`, protocol controller/forms, workflow/protocol UI and seeds | V01/V02/V05: transform attributable history, then remove consumers, configuration and obsolete expectations with their runtime replacement.           |
| Retire           | Reception micro section, `MicroCaseOrderDetail` reception draft handling, Program guards and micro draft save expectations                                                                            | V02/V03: preserve shared order-save integrity and required context; remove the competing reception edit path.                                         |
| Rewrite          | `MicroCase` membership, `MicroCaseAnalysis`, `MicroOrderRoutingServiceImpl`                                                                                                                           | V01/V02: one grouping rule for preview/save/edit; preserve member/result ownership, separate transfers and idempotence.                               |
| Rewrite          | `MicroCaseInoculationServiceImpl`, culture timing/events and inventory usage links                                                                                                                    | V05/V06: row-local timing/lineage, usable lots and traceability with no culture stock transaction.                                                    |
| Rewrite          | `MicroWorklistServiceImpl`, incoming-result placement, `MicroReportProjectionServiceImpl`                                                                                                             | V07/V08/V12: scoped worklists, one attributable result, actual release readiness, structured print output and verified electronic summary continuity. |
| Reuse and extend | Ordinary Analysis/Result/components, organism and antibiotic masters, published panels and breakpoint versions                                                                                        | V04/V09/V10/V11: no second result framework; retain original values and historical versions.                                                          |
| Reuse and extend | Amendments, audit, questionnaires, referrals, callbacks, labels, Workplan and export selection                                                                                                        | V03/V09/V12/V13/V07/V15: extend their existing owner and preserve shared behavior.                                                                    |

## Preservation and acceptance mapping

| Surviving requirement                                                           | Primary owner       | Dependent verification                                                                                                                            |
| ------------------------------------------------------------------------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| Released reports, amendments, immutable history and actual release readiness    | V12                 | V01 migration; V08 late arrivals; V16 end-to-end. Carry lasting readiness behavior from closed OGC-1384/1385; closure is not acceptance evidence. |
| Identification history and received-isolate provenance                          | V09                 | V12 historical reports and V15 referred-in exports.                                                                                               |
| Original measurements, repeats/retests, overrides/reverts and panel versions    | V10                 | V11 interpretation, V12 print. Replace whole-run reporting selection with one chosen validated reading per agent.                                 |
| Culture-medium lot traceability and eligibility, including in-house batches     | V05                 | V06 readings and V07 bulk work; stock remains unchanged on create/edit/undo.                                                                      |
| Other reagent eligibility, required links, method-specific lots and QC          | V04/V10             | Shared Inventory regression; do not infer requiredness from unrelated catalog roles.                                                              |
| Organism/antibiotic administration and published panel versions                 | V09/V10             | V12/V15 retain historical names, interpretations and export mappings.                                                                             |
| Breakpoint import validation, activation and historical interpretation          | V10                 | V11 WHO critical concentrations; imports/activation never silently reinterpret history.                                                           |
| Export mapping, populations, deduplication, preview/generation parity and audit | V15                 | V12 released source data; V07 date boundary (OGC-1411).                                                                                           |
| Accessibility, performance and shared order-save integrity                      | Each affected slice | V02 order saves; V07 worklist; V16 keyboard/mobile and measured budgets.                                                                          |

Export period membership uses specimen **collection date**. Independently chosen
first-isolate chronology may use collection or final-release date, with the
existing 7/14/30-day windows or disabled deduplication, same-source choice,
contaminant filtering and changed-interpretation policy. Changing chronology
never changes period membership. Mapping failures exclude only affected rows;
generation is blocked only when no valid rows remain. Preview and generation
use the same population and policy; generation records actor, time, counts,
selection, filename and content fingerprint. Quoted/newline CSV values round-trip.

The complete 112-criterion ownership matrix is in [tasks.md](tasks.md#acceptance-coverage).
It is the acceptance mapping, not a claim that those tests exist or pass.

## Settled functional rules

| Concern                            | Draft rule and verification owner                                                                                                                                                                    |
| ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Routing and transfers              | Tests trigger grouping; Program does not. Transfers keep cases separate, adding tests remains supported and a no-result member may be split with a reason. V02.                                      |
| Admission and surveillance         | Admission date is optional. V03/V15 retain missing values and never invent infection origin or a questionnaire-to-export mapping. Questionnaire CSV export remains later work.                       |
| Placement and report choices       | Keep draft Results rights for placement/moves, In lab only and Report choices. Validation, final release and amendments retain Validation rights. V04/V08/V12 enforce each action and final locking. |
| Access                             | Worklists show only permitted units; direct links remain read-only without unit rights; mutations are rejected. V02 and each write-bearing slice.                                                    |
| Defaults and quantitative cultures | Editable catalog media defaults and existing result components/calculated values; no protocol lane or new Gram result type. V04/V05.                                                                 |
| Reporting versus surveillance      | In lab only controls patient delivery, while exports apply their own eligibility rules. V12/V15.                                                                                                     |
| Callbacks                          | One attributable call attempt in the shared log/report, outcomes and follow-up retained; the micro finding clock begins at validation. V13.                                                          |
| Offline work                       | Previously loaded information may remain readable; all writes are blocked, with no local save/replay queue. V07/V16.                                                                                 |
| Future capabilities                | Micro QC, isolate storage, configurable release rules, regrouping existing cases, questionnaire CSV, antibiogram, GLASS and cluster delivery remain outside V2 claims.                               |

OGC-1386 remains open for V12/V15: distinguish clinical release readiness from
export readiness and investigate displayed antibiotic names using source data.
No display-name fix or deployment is claimed by this cleanup.

## Execution rules

Run from the owning worktree root. `Amr*` tests and `amr-*.spec.ts` below are
planned deliverables, not existing passing tests; create/register them in their
iteration. A missing or zero-test selection fails the gate. Update existing tests
to V2 and remove expectations for deleted functionality.

Use real PostgreSQL/services for data/query behavior, unit tests for pure rules,
component tests for rendering and Playwright for the user journey. Seed only
iteration-owned data, pin time boundaries and prove unauthorized/final-locked
writes leave state unchanged. Schema slices include a migration/ORM regression.

Before a UI gate, reuse/start the worktree stack; supply credentials securely:

```bash
scripts/dev-stack up
eval "$(scripts/dev-stack env)"
: "${TEST_USER:?Set TEST_USER}" "${TEST_PASS:?Set TEST_PASS}"
```

Register each new Playwright spec in its correct bucket. Run the exact gate,
inspect trace/screenshots/console, fix failures and repeat it until green. Compare
desktop/mobile and keyboard behavior with the directly linked mock section.
Shared-owner changes add that owner's focused regression command here before
work begins. Do not substitute an unrelated full-suite pass.

For changed runtime code, run the repository formatters/checks:

```bash
scripts/run-java21 mvn spotless:apply
scripts/run-java21 mvn spotless:check
(cd frontend && npm run format && npm run check-format && npm run pw:guard)
git diff --check
```

Do not deploy a partial cutover with legacy fallbacks. Release a coherent V2 build
with its migration/deletion proof. No commits, pushes, deployment or shared-data
removal without the corresponding user authorization.

## Exact verification gates

Run every command in the iteration's gate; fix and rerun the same gate. Nothing
below is represented as already passing. Do not disable assertions or skip tests.

### V00 — documentation and sources

```bash
git diff --check
npm exec --yes --package=prettier@3.4.2 -- prettier --check specs/amr/spec.md specs/amr/plan.md specs/amr/tasks.md
```

In the paired design worktree run `npm test` and `npm run build`. Check source
and generated catalogs, documentation, sitemap and published design copies for
retired files/links; review local anchors and render clinical/environmental mocks
and shared entry points. Read Jira back for dispositions, ownership, replacement
links and dependency direction; verify the Confluence walkthrough is archived.
Formatting alone does not close V00. No application tests run for this cleanup.

### V01 — migration

The candidate is
`src/main/resources/liquibase/3.6.x.x/20261006-OGC-1426-amr-v2-cutover.xml`.
It is deliberately absent from the active application changelog until its
runtime consumers are replaced. Rehearsal uses the complete current application
changelog, then this candidate on disposable PostgreSQL databases.

The confirmed migration contract is:

- Supply `amr.cutover.mappingFile`: a CSV with `case_id,test_section_id,program_id`
  and one explicit mapping for every existing case. Require existing lab-unit and
  Program references, including for old UNASSIGNED and Program-only cases.
  Unresolved, duplicate or unknown mappings abort the transaction before any
  committed clinical changes. Supply an existing `amr.cutover.actorId` and an
  explicit `amr.cutover.at` timestamp for attributable migration activity.
- Preserve case IDs and their existing specimen/result ownership. Add
  `MicroCase` order/sample-type/working-unit/Program references and
  `micro_case_specimen` membership. The grouping index is not unique: existing
  matching cases remain separate, receive a migration Timeline entry and require
  review. New-case grouping and the review experience belong to V02/V03/V07.
- Remove case/test/panel workflow columns, case protocol selection,
  `micro_culture_setup` and reception draft ownership. Retain case clinical
  context; retain retired draft values, actors and timestamps in the existing
  `configuration_import_run` history with source `AMR_V2_CUTOVER`. That historical
  payload is used only by the migration's rollback, never a production resolver
  or configuration reader. Measurements, repeat attempts, overrides, panel and
  breakpoint versions, report versions, amendments, original results and lot
  usage remain attributable and retain their IDs. V10 still owns replacing the
  current whole-run reporting selection with per-agent selection.
- Structural rollback restores the pre-cutover representation from that history
  only when clinical values, memberships, migration Timeline entries and catalog case settings still match
  the migrated snapshot. It refuses to erase work recorded after cutover.
  Subsequent corrections use V2 clinical/amendment workflows, not a compatibility
  path. Reapply must preserve the same identities and relationships.

```bash
scripts/run-java21 mvn -B -ntp -Dtest=AmrCutoverMigrationTest test
```

### V02 — routing

```bash
scripts/run-java21 mvn -B -ntp -Dtest=MicrobiologyOrderSaveIntegrationTest,MicroOrderRoutingIdempotencyTest,MicrobiologyBenchRestControllerSecurityTest,MicrobiologyOrmValidationTest test
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/microbiology-order-entry.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-order-entry.spec.ts --project=core-app)
```

### V03 — context

```bash
scripts/run-java21 mvn -B -ntp -Dtest=AmrCaseContextIntegrationTest,MicroCaseOrderDetailServiceTest test
(cd frontend && npm run test:unit -- src/components/microbiology/__tests__/OrderDetailPanel.test.jsx)
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/microbiology-case-workbench.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-case-workbench.spec.ts --project=core-app)
```

### V04 — results and notes

```bash
scripts/run-java21 mvn -B -ntp -Dtest=AmrCaseResultsIntegrationTest,MultiComponentResultEntryIntegrationTest,MultiComponentDictionaryRoutingIntegrationTest test
(cd frontend && npm run test:unit -- src/components/microbiology/__tests__/MicrobiologyCaseView.test.jsx)
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/amr-case-results.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/amr-case-results.spec.ts --project=core-app)
```

### V05 — media

```bash
scripts/run-java21 mvn -B -ntp -Dtest=MicroReagentLotTransactionIntegrationTest,MicroCaseInoculationServiceTest test
(cd frontend && npm run test:unit -- src/components/microbiology/__tests__/CaseInoculationPanel.test.jsx src/components/microbiology/__tests__/ReagentLotPicker.test.jsx)
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/microbiology-reagent-lots.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-reagent-lots.spec.ts --project=core-app)
```

### V06 — cultures

```bash
scripts/run-java21 mvn -B -ntp -Dtest=AmrCultureReadingIntegrationTest,MicroCultureAnalyzerEventServiceTest,MicroCaseReadinessServiceTest test
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/microbiology-no-growth-release.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-no-growth-release.spec.ts --project=core-app)
```

### V07 — bench

```bash
scripts/run-java21 mvn -B -ntp -Dtest=AmrBenchWorkIntegrationTest,MicroWorklistServiceTest test
(cd frontend && npm run test:unit -- src/components/microbiology/__tests__/MicrobiologyWorklist.test.jsx)
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/amr-bench-work.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/amr-bench-work.spec.ts --project=core-app)
```

### V08 — incoming

```bash
scripts/run-java21 mvn -B -ntp -Dtest=AmrIncomingResultsIntegrationTest,MicroAstAnalyzerEventServiceTest test
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/amr-incoming-results.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/amr-incoming-results.spec.ts --project=core-app)
```

### V09 — isolate and referral

```bash
scripts/run-java21 mvn -B -ntp -Dtest=AmrIsolateReferralIntegrationTest,MicroIsolateServiceTest test
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/amr-isolate-referral.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/amr-isolate-referral.spec.ts --project=core-app)
```

### V10 — AST

```bash
scripts/run-java21 mvn -B -ntp -Dtest=MicroAstIntegrationTest,MicroAstServiceTest,MicroAstInterpretationServiceTest test
(cd frontend && npm run test:unit -- src/components/microbiology/__tests__/AstEntryPanel.test.jsx)
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/microbiology-repeat-ast.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-repeat-ast.spec.ts --project=core-app)
```

### V11 — DST

```bash
scripts/run-java21 mvn -B -ntp -Dtest=AmrDstIntegrationTest,MicroBreakpointServiceTest test
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/amr-dst-validation.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/amr-dst-validation.spec.ts --project=core-app)
```

### V12 — reports

```bash
scripts/run-java21 mvn -B -ntp -Dtest=AmrReportIntegrationTest,MicroAmendmentIntegrationTest,MicroReportReleaseServiceTest test
(cd frontend && npm run test:unit -- src/components/microbiology/__tests__/ReportReadinessPanel.test.jsx)
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/microbiology-amendment.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-amendment.spec.ts --project=core-app)
```

### V13 — callbacks

```bash
scripts/run-java21 mvn -B -ntp -Dtest=MicroCriticalCommunicationAlertIntegrationTest,AmrCallbackIntegrationTest test
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-worklist-critical.spec.ts --project=core-app)
```

### V14 — history

```bash
scripts/run-java21 mvn -B -ntp -Dtest=AmrHistoryIntegrationTest test
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/amr-patient-history.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/amr-patient-history.spec.ts --project=core-app)
```

### V15 — populations

```bash
scripts/run-java21 mvn -B -ntp -Dtest=AmrEnvironmentalIntegrationTest,MicroWhonetPersistenceIntegrationTest,MicroWhonetDatasetServiceTest test
python3 .ai/skills/playwright/scripts/validate-playwright-project.py playwright/tests/foundational/core/microbiology-whonet-export.spec.ts
(cd frontend && npm run pw:test -- playwright/tests/foundational/core/microbiology-whonet-export.spec.ts --project=core-app)
```

## Final integration gate

Run once on a committed, clean, coherent V2 revision after focused gates pass.
The runner owns disposable stacks; its cleanup must not target clinical or shared
review data. Keep evidence outside this specification directory.

```bash
scripts/run-ci-checks.sh --artifact-dir .devin/artifacts/amr-ci
gh pr checks
```

All three checkpoints must exist and pass: `01 Checkpoint - Backend`,
`02 Checkpoint - Frontend`, `03 Checkpoint - E2E`. A workflow conclusion is not
proof. Missing PR/checkpoint, dependency or approval means blocked, not green.
