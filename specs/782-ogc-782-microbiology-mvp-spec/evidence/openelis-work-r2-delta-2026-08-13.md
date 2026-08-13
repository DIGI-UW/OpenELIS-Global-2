# OpenELIS Work R2 Delta - 2026-08-13

## Authority

OpenELIS Work revision `bf51582766eaf4048dcf83a4810a3cd32a975ad5` is the
functional authority. It supersedes the R1 pin `a1f720d7b3b0` for the changed
M-03 and M-04 behavior. Technical names in that source remain non-binding.

## Confirmed Product Delta

| Surface | Authoritative behavior | R1 repository state | R2 disposition |
|---|---|---|---|
| M-03 Date of Admission | Optional, visible with micro details, disabled for Outpatient, never a case-creation gate | Missing | Add and round-trip |
| M-03 Culture Protocol | Derived read-only display; explicit unset state still advances | Required editable ComboBox and order gate | Replace input with display and remove gate |
| M-03 Critical Notify | No order-entry control | Checkbox persisted with order context | Remove from order-entry contract and UI; retain historical data compatibility |
| M-04 protocol correction | Separate inline Set/Change action in Inoculation with reason and audit | Only available inside Change Workflow | Add protocol-only action |
| M-04 preservation | Protocol change leaves workflow, inoculations, isolates, AST, and surveillance state unchanged | No independent operation | Prove by service tests |
| M-04 release guard | Block after final release; use amendment path | Workflow action has final lock | Reuse the same case lock |

## Engineering Decisions

1. Store Date of Admission as one nullable date on the existing
   `MicroCaseOrderDetail` model. That service owns every other M-03 value and is
   already consumed by case and surveillance projections. This does not make
   the source's suggested table name or storage location a product constraint.
2. Keep the obsolete critical-notification column readable for historical rows
   during R2, but stop accepting or rendering it in order entry. Notification
   behavior continues through existing Test Catalog/M-11 paths.
3. Implement protocol correction as a dedicated service operation and request
   contract. Do not overload workflow reclassification with a mode flag.
4. Reuse case activity history for actor, time, previous/new Method, and reason.
   Do not create a second protocol-history table.
5. Derive worklist incubation bounds from the case's current Method while
   retaining the original inoculation timestamp; no clock-reset write is needed.

## Ambiguities And Defaults

| Topic | Ambiguity | Deterministic R2 default | Status |
|---|---|---|---|
| Invalid Date of Admission | M-03 says show an inline error but also says it is not a save failure | Treat empty as always valid; show invalid entered dates inline without making the surveillance field a case-creation prerequisite; reject only malformed transport values | Accepted engineering interpretation |
| Outpatient transition | Source says disable the field but not whether to erase a date already typed | Keep the typed value in local form state while disabled so an accidental selection is reversible; omit/clear it in the submitted order context while Outpatient is selected | Engineering decision to validate in tests |
| Method summary | Mock shows media/incubation prose but repositories may have partial setup metadata | Render the Method name always and structured available timing/setup metadata; never synthesize missing prose | Accepted degradation |
| Active Method scope | Source says linked to the ordered test and current workflow | Reuse existing Test-Method and workflow compatibility services; do not add a parallel mapping | Resolved by repo pattern |
| Date locale | Source asks locale round-trip while the API can transport an ISO date | Keep the domain value date-only and use ISO transport; Carbon/React Intl owns locale presentation | Engineering decision |

## Acceptance Evidence Map

- JUnit 4: persistence, optional routing, mutation guards, audit, preservation.
- ORM/Liquibase: nullable date mapping and update/rollback/reapply.
- Vitest/RTL: Carbon roles and interactions for both surfaces.
- Playwright: authenticated order -> case -> Inoculation protocol change,
  registered in `core-app`, with no fixed sleeps or forced actions.
- Visual: pinned M-03 and M-04 desktop/mobile screenshots with a deviation log.
- Human UAT: separate M-03 and M-04 stories against the exact deployed R2 SHA.

## M-03 TDD Checkpoint

### Red Evidence

- The focused backend compile initially failed because the order-detail model
  and request/response contracts had no Date of Admission.
- The standalone migration test initially found no `admission_date` column.
- The order-entry response test failed before real culture-setup metadata was
  available beside the derived Method.
- Focused Carbon tests failed against the old editable/required protocol,
  missing admission-date control, and obsolete critical-notification checkbox.

### Green Evidence

- 37 JUnit 4 tests pass across order-detail and routing services, request
  binding, response mapping, ORM boot, service-created routing integration, and
  Liquibase update/rollback/reapply.
- 28 Vitest/RTL tests pass across the reusable DatePicker, microbiology order
  section, loaded-order panel, case summary, order serialization/readiness,
  Program derivation, and test selection.
- The touched Playwright files lint with zero warnings. Playwright discovers the
  updated M-03 flow in `core-app` and its accessibility assertion in both
  `core-accessibility` projects, 19 discovered tests including dependencies.
- Spotless and targeted Prettier formatting pass. No fixed sleep or forced
  interaction was added; `test.setTimeout` remains only a per-test budget.

### Implemented Boundary

- One nullable `date` column was added through Liquibase with rollback.
- The browser presents locale-formatted dates while the API persists ISO
  `yyyy-MM-dd`, avoiding a locale-sensitive server contract.
- Culture Protocol is a read-only Method projection. Missing defaults show the
  promised bench-selection state and no longer block case creation.
- Available media, incubation, and atmosphere values come from the existing
  culture setup through the existing order-entry response. The UI does not
  invent missing recipe prose.
- Legacy order-level critical-notification data remains readable in historical
  rows but is absent from new request/response and UI contracts.

M-03 component and service validation is complete. Deployed Playwright
execution, screenshots, M-04 protocol correction, Grist publication, and human
UAT remain open under T297-T306.
