# Tasks: Lab Units Management Redesign — Increment 2 (OGC-189)

**Epic**: [OGC-189](https://uwdigi.atlassian.net/browse/OGC-189) — In Progress.
**Increment 1 shipped**: `52937e6f6` (PR #4121, 2026-08-24) — list view, Basic
Info, Assigned Tests, Display Order. This file covers **what increment 1 left
undone**, scoped by the 2026-09-03 decisions recorded below.

**Organization**: by **Milestone** (Constitution Principle IX). Detailed
acceptance criteria are authoritative in the Jira Epic description and comments
37311 / 37313; tasks reference them, they do not restate them.

**Format**: `- [ ] T### [P?] description — path (AC: …)`.
`[P]` = parallelizable within its milestone.

**Task-ID ranges** (pre-allocated so inserts never renumber):
M1 `T001–T049` · M2 `T050–T099` · M3 `T100–T149` · M4 `T150–T199` ·
M5 `T200–T249` · deferred stubs `T900+`.

---

## Decisions of record (2026-09-03)

These were open questions in Jira comment 37313 §8/§9. Settled; they drive the
milestone shapes below and should be mirrored into the Epic description.

| # | Question | Decision |
| --- | --- | --- |
| D1 | Does an inactive lab unit stop new analyses? | **Yes — gate all order routes.** `effectiveActive = test.active && labUnit.isActive`, enforced server-side in the ordering path: manual, reflex, analyzer, FHIR, incoming electronic orders. Carries the reflex-safety obligation (M4). |
| D2 | Default deactivation option? | **SUPERSEDED 2026-09-07.** Was "reassign where reflex/calculation targets are present". The reflex-target warning and count were removed (see T159 note): the count named tests *assigned to* the unit, while the reflex gate tests the unit the work *lands in*, so it flagged reflexes that keep firing and missed the ones that break. `recommendedOption` is now simply `keep` when the unit holds no tests, else `deactivate_all`. All three options still ship (D6). |
| D3 | Block analyzer results for analyses that already exist? | **No — let them flow.** Completion of existing work is not a new order. Upholds the completion guardrail. |
| D4 | Do choosers filter out inactive units? | **Partially.** The test editor's lab unit picker filters on `isActive`; **reassign destinations stay unfiltered**, honouring the 2026-09-01 ruling ("fine with moving to a switched off lab") as a deliberate exception. Grandfathered-select still required on the editor picker. |
| D5 | Do reflexes fire into a deactivated lab unit? | **No — block, and alert.** A reflex whose target test sits in an inactive unit does not generate an analysis. Because a silently-unfired susceptibility reflex is a patient-safety event, the block must raise a visible alert rather than failing quietly (comment 37313 §7). |
| D6 | Keep the "keep assignments" deactivation option? | **Yes, keep all three options.** T108 is settled: keep / deactivate all / reassign all ship. Per D2, reassign is still the *default* when reflex or calculation targets are present. |

**Deferred by decision (2026-09-03), not in this increment**: the Workflows,
Panels, Programs and Projects tabs. See the deferred stubs at the end.

---

## Phase M1 — Shipped-increment defect (Tier A) ✅ DONE

**Status: complete** — commit `22fa139f5`, branch `fix/ogc-189-m1-name-length-on-edit`.
Both layers fixed, 5 tests added, each verified by inversion. Independent of M2–M5; land it first, it is small.
Regression pinned in `DIGI-UW/OpenELIS-QA` as `LU-W-3`.

The 20-character lab unit name cap is enforced on Add but **not on Edit**, on
both layers. A 32-character name saves and persists. `test_section.NAME` is
VARCHAR(20); renames currently write to `localization_value`, which is why this
has not thrown — confirm no path still writes `section.setName` with an
unchecked value.

- [x] T001 RED: failing backend test — `PUT /rest/lab-units-management/{id}` with a 21-char fallback-locale name must return 422 — `src/test/java/.../LabUnitManagementRestControllerSecurityTest.java` (or a new validation slice test)
- [x] T002 GREEN: add the `NAME_MAX_LENGTH` check to the update handler — it validates description only at [LabUnitManagementRestController.java:357](../../src/main/java/org/openelisglobal/common/management/controller/rest/LabUnitManagementRestController.java#L357), while create validates the name at [:283](../../src/main/java/org/openelisglobal/common/management/controller/rest/LabUnitManagementRestController.java#L283). Validate every supplied locale's name, not just the fallback
- [x] T003 [P] Ungate the client check: drop the `view === "add"` condition at [LabUnitManagement.jsx:384](../../frontend/src/components/admin/labUnitManagement/LabUnitManagement.jsx#L384) so Edit validates too
- [x] T004 [P] Add `maxLength={NAME_MAX_LENGTH}` to the Name input — Description already carries `maxlength="60"`; Name carries none
- [x] T005 Audited: `setTestSectionName` is written **only on create** ([controller:310](../../src/main/java/org/openelisglobal/common/management/controller/rest/LabUnitManagementRestController.java#L310)); the update path never touches the VARCHAR(20) column. Renames write `localization_value.value` (`text`, unbounded), which is why the over-length name persisted rather than throwing — the cap is a product rule, not a storage constraint
- [x] T006 Verify `LU-W-3` now fails (flip-when-fixed) and hand the rewrite signal to QA

---

## Phase M2 — Lab unit visibility: chooser / viewer inversion (Tier A) ✅ DONE

**Status: complete.** Viewer half and chooser half both landed; 5 new tests,
each verified by inversion. Depends on nothing; **blocks M3** — this is the
guard that stops the first populated unit somebody deactivates from stranding
its pending analyses. Regression: `test-catalog-lab-unit-visibility.spec.ts`
(`G-1`..`G-4`).

Measured 2026-09-02: the filtering is applied to exactly the wrong half. The
test's lab unit picker offers all 35 units including 20 inactive ones, while the
`/Results` viewer filter shows 13 options for 34 units, hiding all 22 inactive —
including any that still hold in-flight work. Both halves move, in opposite
directions, per **D4**.

### Viewer half — must land with or before M3

- [x] T050 RED: failing test — an inactive lab unit that still holds pending analyses must appear in the `/Results` lab unit filter
- [x] T051 GREEN: change the viewer endpoint to `isActive OR hasContent` — `/rest/results-entry/lab-units`, consumed at [UnifiedResults.tsx:222](../../frontend/src/components/resultPage/unified/UnifiedResults.tsx#L222). "hasContent" = holds tests OR in-flight analyses, so a unit self-cleans out of the list once its work finishes
- [x] T052 [P] **Inventory done — Casey's guess confirmed.** The same pattern sat in Workplan, Logbook results and both validation controllers, all reached through `GET /rest/user-test-sections/{roleName}` ([DisplayListController.java:459](../../src/main/java/org/openelisglobal/common/rest/DisplayListController.java#L459)). Every frontend consumer of that endpoint is a viewer (workplan picker, Results + Validation search filters, report selectors, dashboard), so one endpoint change covered all of them
- [x] T053 **CORRECTED — the guardrail did NOT hold "by construction"; this was a real miss.** Found by the user 2026-09-07: order a test, deactivate its lab unit, and `/result?accessionNumber=...` came back empty while the dashboard still counted the work. Cause: `filterAnalysesByLabUnitRoles` / `filterAnalysisResultsByLabUnitRoles` ([UserServiceImpl](../../src/main/java/org/openelisglobal/systemuser/service/UserServiceImpl.java)) filter the **analyses themselves** through `getUserTestSections` (active-only) — a narrowing path M2 never audited because it only looked at picker endpoints. Both now use `getUserViewerTestSections`, plus six viewer pickers (workplan, logbook results ×2, three validation controllers). Authorization-scoping callers (dashboard, menu, order search) deliberately left on the active-only set — changing those would widen access, not fix stranding.
- [x] T053-note Superseded claim, kept for the record: "holds by construction: the viewer change only ever *widens* a picker's option list; no analysis query gained a lab-unit-status filter. **Nothing was narrowed anywhere**, so no surface can strand in-flight work. Per-surface deactivation tests are still worth adding when M4 lands the gate that could actually narrow them — noted as T157

### Chooser half — the data-loss guard

- [x] T054 RED: failing test — the **grandfathered-select** case. A test already assigned to an inactive unit must render that unit as the current value; saving must not write a blank back. This is the OGC-1191 loss class (`G-3`)
- [x] T055 GREEN: filter the **test editor's** lab unit picker on `isActive`, with the current value always present, displayed as `Parasitology (inactive)`, disabled, with the lock explanation beneath
- [x] T056 Leave **reassign destinations unfiltered** per D4 — add a regression test pinning this as deliberate, not an oversight, so it is not "fixed" later by mistake
- [x] T057 D4's exception is pinned **in the code** at the reassign destination selector ([AssignedTestsSection.jsx](../../frontend/src/components/admin/labUnitManagement/sections/AssignedTestsSection.jsx)) with a "do not fix this by filtering" comment. **Still to do by hand: post the D1–D6 decision table as an Epic comment** so Jira reflects it

---

## Phase M3 — Deactivation guarding flow (Tier A) ✅ DONE

**Status: complete.** Impact-summary endpoint + guarded deactivation endpoint +
the editor modal. 5 backend validation cases and 5 component cases, the
component set verified by inversion against the old silent-save toggle.

Today the Basic Info Active toggle saves silently with tests still attached —
only an inline warning at [LabUnitManagement.jsx:1243](../../frontend/src/components/admin/labUnitManagement/LabUnitManagement.jsx#L1243), whose own
comment defers the flow to "a later increment of OGC-189". That is this one.

- [x] T100 Backend: impact-summary endpoint — for a lab unit, return assigned test count, **pending analysis count**, historical analysis count, and the count of tests that are **reflex or calculation targets** (sources: `src/main/java/org/openelisglobal/testreflex/`, `.../testcalculated/`). A flat test count hides the dangerous ones (AC: comment 37313 §6)
- [x] T101 RED: failing tests for the three options — keep / deactivate all / reassign — including that **"keep" leaves every test's own `active`/`orderable` config unmutated**
- [x] T102 GREEN: implement the three options. Per **D2**, default to **reassign** when reflex/calculation targets are present
- [x] T103 Typed confirmation: bulk deactivation requires typing `DEACTIVATE`
- [x] T104 When pending analyses > 0, state plainly in the modal that the unit stays in worklists until those complete (follows from T053)
- [x] T105 Activate flow: offer to activate inactive assigned items (AC: Epic, Activation/Deactivation)
- [x] T106 [P] Frontend: replace the inline warning with the impact-summary modal — `frontend/src/components/admin/labUnitManagement/LabUnitManagement.jsx`
- [x] T107 [P] i18n keys in `en.json` **only** (Transifex owns the rest) — impact summary, three options, typed confirmation, reflex-target line
- [x] T108 **Settled (D6): all three options ship**, including "keep assignments". Reassign remains the default where reflex/calculation targets are present (D2)
- [x] T109 `LU-W-10` should now flip (it asserts "no prompt / no impact summary today"). **Not run by me** — it lives in `DIGI-UW/OpenELIS-QA` against a deployed instance, so QA owns the rewrite signal

---

## Phase M4 — `effectiveActive` cascade (Tier A) ✅ DONE

**Status: complete.** One guard service, four gated routes, 8 DB-backed tests
(3 verified by inversion against the pre-M4 defect). Per **D1**. Depends on **M2/T053** (guardrail
tests must exist before the gate lands, or in-flight work strands). Regression:
`test-catalog-orderability-semantics.spec.ts` `TO-1`..`TO-5`, and
`LU-W-11`/`LU-W-12`.

Measured 2026-09-02: a lab unit's status does not participate in orderability at
all. `getActiveTestsBySampleTypeIdAndTestUnit` filters on the **test's** active
flag — [TypeOfSampleServiceImpl.java:101](../../src/main/java/org/openelisglobal/typeofsample/service/TypeOfSampleServiceImpl.java#L101), a single stream filter, which is the
natural seam for the gate. Called from
[SampleEntryTestsForTypeProviderRestController.java:157](../../src/main/java/org/openelisglobal/common/rest/provider/SampleEntryTestsForTypeProviderRestController.java#L157) and
[SampleEntryTestsForTypeProvider.java:100](../../src/main/java/org/openelisglobal/common/provider/query/SampleEntryTestsForTypeProvider.java#L100).

**Derived, never written**: the test's own `active`/`orderable` config is never
mutated. That is what makes reactivation free and lossless — flip the unit back
on and every test returns to what its own config says, with no restore step and
no shadow column.

- [x] T150 RED: failing test — an active test in an inactive lab unit must NOT be creatable as a new analysis (`LU-W-11`). Include the **positive control** so the exclusion cannot pass vacuously (`TO-2`)
- [x] T151 GREEN: implement `effectiveActive = test.active && labUnit.isActive` at the ordering-path seam. Enforce **server-side**, not in the UI
- [x] T152 `effectiveOrderable = test.orderable && effectiveActive` for manual picker visibility. Preserve `active: true, orderable: false` as the intentional category it is — reflex-ordered susceptibility, confirmation tests, analyzer CT values (`Genie III` ids 44/45/46, `Stat-Pak` 53/54/55 are live examples)
- [x] T153 **Inventory done — there is no single choke point.** `new Analysis()` appears at ~20 sites; `AnalysisServiceImpl.buildAnalysis` is a shared factory but only ~6 callers use it, and the routes that matter each construct directly. So one guard service is called at four entry points rather than retrofitting every site: manual picker ([TypeOfSampleServiceImpl:104](../../src/main/java/org/openelisglobal/typeofsample/service/TypeOfSampleServiceImpl.java#L104)), manual save ([SamplePatientEntryServiceImpl:509](../../src/main/java/org/openelisglobal/sample/service/SamplePatientEntryServiceImpl.java#L509)), reflex ([ReflexAction:91](../../src/main/java/org/openelisglobal/testreflex/action/util/ReflexAction.java#L91)) and both analyzer sites ([AnalyzerResultsAcceptServiceImpl](../../src/main/java/org/openelisglobal/analyzerresults/service/AnalyzerResultsAcceptServiceImpl.java)). **FHIR/HL7 electronic orders still to verify** — they land through the manual-save path, so they are covered if and only if they route via `persistAnalyses`; not yet confirmed end to end (T158)
- [x] T154 **D3**: analyzer results for analyses that **already exist** continue to flow. Add an explicit test — an inactive unit must still accept results for a pre-existing analysis
- [x] T155 **Reflex safety (D5): block, and alert.** A reflex whose target test sits in an inactive unit must not generate an analysis, and the block must be visible — a silently-unfired susceptibility reflex is a patient-safety event. Gate at the reflex creation path and raise an alert the lab can see (AC: comment 37313 §7). Do not close M4 without this
- [ ] T156 Verify `LU-W-11`, `LU-W-12`, `TO-1`..`TO-5` flip
- [ ] T158 Confirm FHIR / incoming electronic orders reach the gate — they are expected to route through `SamplePatientEntryServiceImpl.persistAnalyses`, but that was not verified end to end in M4
- [x] T159 **Settled by the user (2026-09-07): "if the reflex test is attached to the ordering unit then it should work, otherwise it shouldn't be attached to a disabled one."** The rule is about where the work LANDS. Since a reflexed analysis is filed under the parent's lab unit, the gate now tests that unit rather than the reflexed test's own configured one — check and filing agree. Fixes the case that violated the rule (test's unit active, parent's unit disabled → previously fired, landing in a disabled unit) and stops over-blocking the inverse (test's unit disabled, parent's active → was blocked, now fires). The test's own active flag still applies. Routing itself is unchanged
- [x] T157 **Completion guardrail verified end to end by the user (2026-09-07)** across order → complete → view → report, with the lab unit AND the test both deactivated. Took four fixes, each surfacing only when the next stage was exercised: (1) `filterAnalyses*ByLabUnitRoles` on the active-only set `0aca64e41`; (2) `filterResultsByLabUnitRoles`, the method the accession lookup actually uses, missed on the first trace `3af8390ec`; (3) `getTestsByTestSectionIds` filtering `isActive='Y'` — **pre-existing on develop**, stranded work when the TEST was deactivated `65190b2e3`; (4) `hasContent` counted pending analyses only, so entering a result finalized the analysis and hid it again `d8c450faa`. Automated per-surface tests still worth adding — verification was manual
- [ ] T157b Automated per-surface guardrail tests: results entry, validation, workplan, by-unit reports, patient history and `/Results` must each still show work after its lab unit is deactivated (manual verification done; regression cover missing): results entry, validation, workplan, by-unit reports, patient history and `/Results` must each still show an in-flight analysis after its lab unit is deactivated

---

## Phase M5 — Remaining Epic ACs (Tier A)

**Status: elaborated.** Independent of M1–M4; parallelizable.

### Import / Export tab

Nothing exists — no export, no import, no `FileUploader` in the surface.

- [ ] T200 Add the `import-export` section to [sectionConfig.js](../../frontend/src/components/admin/labUnitManagement/sectionConfig.js) (currently `basic-info`, `assigned-tests`, `display-order` only) and the contextual SideNav
- [ ] T201 Export to JSON with the three option sets — config only / with tests / full. **Must include the `domain` field**
- [ ] T202 [P] Export to CSV for review
- [ ] T203 Import: validate before applying, **including Domain enum values** (CLINICAL / ENVIRONMENTAL / VECTOR — there is no `BOTH`)
- [ ] T204 Import preview showing the changes to be applied
- [ ] T205 Import modes: create only / update only / both
- [ ] T206 Import handles missing references gracefully (AC: Epic, Import/Export)

### List view gaps

- [ ] T210 [P] Drag-and-drop reordering — not implemented; display order works via the move endpoint only, no drag handlers exist. Backend `moveToSortOrderPosition` (1-based, dense renumber) already supports it, so this is frontend-only — `sections/DisplayOrderSection.jsx`
- [ ] T211 [P] Domain filter must be a Carbon **`MultiSelect`** combining with other filters via AND. Shipped as a single-value `Select` — [LabUnitManagement.jsx:741](../../frontend/src/components/admin/labUnitManagement/LabUnitManagement.jsx#L741), with `unit.domain === domainFilter` at [:251](../../frontend/src/components/admin/labUnitManagement/LabUnitManagement.jsx#L251)
- [ ] T212 [P] Quick summary panel showing assignment counts (AC: Epic, List View)
- [ ] T213 Confirm "display order affects system-wide dropdowns and menus" end-to-end, not just within the admin list

---

## Deferred stubs — not on any branch

Deferred by decision 2026-09-03. Recorded so the Epic's AC set stays honest
about what remains.

- [ ] T900 DEFERRED — **Workflows tab.** Custom workflows per lab unit, one settable as default. Check first whether a per-section workflow entity exists at all; may be net-new schema
- [ ] T901 DEFERRED — **Panels tab.** **Likely obsolete as specced**: the 2026-07-13 design ruling (comment 34215) is that panels are *not* scoped to a single lab unit — a panel can span sections and is scoped by Domain instead. Resolve the contradiction with the Epic description before any build; the likely outcome is dropping this tab
- [ ] T902 DEFERRED — **Programs tab.** Blocked on [OGC-781](https://uwdigi.atlassian.net/browse/OGC-781) (Programs Management Rework, In Progress)
- [ ] T903 DEFERRED — **Projects tab.** Lab notebook projects, assignable with details visible and removable

---

## Notes

- **Domain is already built.** `test_section.domain` with the CLINICAL /
  ENVIRONMENTAL / VECTOR check constraint and backfill-to-CLINICAL shipped in
  [059-results-r1-unified-worklist.xml](../../src/main/resources/liquibase/3.5.x.x/059-results-r1-unified-worklist.xml) via OGC-1020; the default lives on
  [TestSection.java:57](../../src/main/java/org/openelisglobal/test/valueholder/TestSection.java#L57). The July design comments (34354, 34357) claiming "no
  `test_section` migration exists in the repo" are **stale** — no Domain
  migration task is needed, and [OGC-361](https://uwdigi.atlassian.net/browse/OGC-361) is not a blocker.
- **Terminology**: "Lab Unit" everywhere in user-facing strings, never "test
  section" (design v2.0). The entity is `TEST_SECTION`; there is **no code
  field** — no such column exists. Description is NOT NULL / required.
- **Nothing is ever orphaned in the data sense** — every test has exactly one
  lab unit, every analysis records its unit. "Orphaned" throughout M2 means
  unreachable through the UI, which makes it a picker-population problem, not a
  data-model one.
- **Sequencing**: M1 anytime · M2 before M3 and M4 · M4/T053 guardrail tests
  before T151 lands · M5 parallel throughout.
- **Constitution**: new i18n keys in `en.json` only; `@Transactional` in
  services not controllers; services compile all data inside the transaction;
  `mvn spotless:apply` + `npm run format` before every commit.
