# Milestone 3: Initial testing and results

This slice builds on `codex/amr-v2-case-access`. The case shell hosts one
Carbon result table and inline editor for Initial and Additional testing.
The specimen chooser reuses the collection test/panel picker and excludes
already linked tests. Catalog components use the ordinary Results Entry
controls, options, precision, limits and acknowledgement modal.

## Service and HTTP contract

Controllers return compiled forms. Services fetch analysis, test, specimen,
order and lab-unit relationships inside the transaction. Every mutation locks
the case and checks current ownership and terminal/final locks; a remembered
permission from an earlier page load does not authorize a write after transfer.

| Endpoint                                                                        | Body or response                                                                                        |
| ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `GET /rest/microbiology/cases/{caseId}/analyses`                                | List of case test forms                                                                                 |
| `POST /rest/microbiology/cases/{caseId}/analyses`                               | `sampleItemId`, `placement` (`INITIAL` or `ADDITIONAL`), `testIds`, `panelIds`                          |
| `POST /rest/microbiology/cases/{caseId}/analyses/{analysisId}/results`          | `version`, optional `note`, `components`                                                                |
| `POST /rest/microbiology/cases/{caseId}/analyses/{analysisId}/tested-elsewhere` | `version`, `testedElsewhere`, one of `performingLabId`/`performingUserId`, `performedAt` (`YYYY-MM-DD`) |
| `POST /rest/microbiology/cases/{caseId}/analyses/{analysisId}/validate`         | `version`                                                                                               |
| `GET /rest/microbiology/cases/{caseId}/timeline`                                | Chronological attributed activities and case notes                                                      |
| `POST /rest/microbiology/cases/{caseId}/notes`                                  | `text` (1–4000 trimmed characters)                                                                      |

A test form contains `analysisId`, `testId`, `testName`, `placement`, ordinary
analysis `status`, `version`, `enteredBy`, `canEdit`, `canValidate`,
`selfValidationBlocked`, `testedElsewhere`, performer identifiers/display,
`performedAt` and `components`. Components are the existing `TestResultItem`
response, including `testResultComponentId`, result type, dictionary options,
precision, limits, flags and stored values.

A submitted component contains `componentId`, `value`,
`multiSelectResultValues` and acknowledgement booleans `criticalAcknowledged`
and `invalidResultConfirmed`. The server reloads its own rows; the client
cannot replace catalog types, options, limits, attribution or analysis linkage.
Blank components are omitted; clearing a previously saved component removes
only its own result through the ordinary audited result-save pipeline.
At least one nonblank result is required.

Writes require Results rights in the current case lab unit. Validation requires
Validation rights. Known out-of-unit direct links retain the M2 read-only
exception. Invalid values return 400, denied writes 403, unknown linkage 404,
stale versions/final locks 409, and missing critical/invalid acknowledgement
422 with the shared `ACKNOWLEDGEMENT_REQUIRED` response. Failed saves preserve
entered values. Saving provenance refreshes the version without closing the
result draft.

Results save through the ordinary persistence and acknowledgement services and
await validation. FHIR transformation runs after the transaction commits.
The configurable site setting `blockSelfValidation` defaults off, as the V2
product specification permits the laboratory to enable it. When enabled, the
case entry actor or last result-edit actor cannot validate; ordinary validation
uses the same guard, and automated entry cannot finalize a result. A separate
validator can accept it. Validation retains the shared QC gate and electronic
signature interface. Actions and notes record the authenticated actor and time.

## Validation and remaining roadmap work

Focused tests cover independent component persistence and correction, scoped
clearing, stale versions, foreign component/case rejection, unit rights,
ownership changes, catalog selection and duplicate prevention, provenance,
attributed notes, final locks, shared self-validation and auto-validation.
Frontend tests cover permissions, scope switching, invalid precision, failed
drafts, provenance version refresh and notes. The property-gated
`INITIAL_TESTING` scenario supplies catalog tests for the browser journey.
The browser checks reload persistence, external provenance, keyboard catalog
selection, additional testing, notes and contained table scrolling at 390px.

This is the handoff's basic result-entry slice. The broader pinned design's
run-of-one model, required reagent-link metadata and lot column, move/cancel/
In-lab-only menu, full result-history expansion, expert review/return workflow,
and report selection remain later shared or case slices. The current catalog
link model has no Required flag and the current result pipeline has no generic
run-of-one service; this slice does not invent parallel implementations.
The reused chooser currently searches names in its loaded specimen catalog;
code/LOINC paging and the all-unit toggle remain shared chooser work.

Focused validation, full local CI, GitHub checkpoints and owner acceptance are
separate evidence. Consult the PR for the current gate status.
