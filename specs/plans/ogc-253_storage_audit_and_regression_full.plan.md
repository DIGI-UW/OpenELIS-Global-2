---

name: OGC-253 storage audit + dropdown regression (parent plan) overview: "Two
coupled goals on branch claude/magical-albattani / PR #3428: (1) wire storage
entities into the system-level audit trail introduced by PR #3284 (OGC-253 left
storage out of the 17 audited entities); (2) restore + protect the broken
location selection/creation UX that silently regressed because the existing
Cypress storageAssignment.cy.js had .skip() on every meaningful suite. Each user
story (assign / move / dispose) must work AND emit audit events viewable in
/AuditTrailReport.

This plan is being paused so we can pivot to a focused sub-plan on the
regression remediation (Track A specs + the LocationTreeView dropdown bug they
surfaced). Resume this plan from the open todos below once the sub-plan
completes." status: paused-pending-sub-plan branch: claude/magical-albattani pr:
https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3428 sub_plan:

- regression-remediation:
  ogc-253_track-a_dropdown_regression_remediation.plan.md related: jira: OGC-253
  prior_pr: "#3284" guides: - .specify/guides/playwright-best-practices.md -
  .specify/guides/playwright-e2e-quality-report.md

context: why_this_exists: | OGC-253 specified a system-level audit trail
covering "Storage/Freezer: Locations, sample storage/retrieval/moves,
temperature logs." PR #3284 delivered audit for 17 entity types but omitted
every storage entity. Independently, the location selection/creation UI is
reported broken on testing.openelis-global.org (manually verified by user). CI
was green because frontend/cypress/e2e/storageAssignment.cy.js has .skip() on
all four meaningful suites (Cascading Dropdowns, Type-Ahead, Barcode Scan,
Capacity Warning), with comments citing "Carbon ComboBox interactions are
complex — typing doesn't properly trigger React state."

approach: - Use Playwright (real user events, not Cypress's synthetic .type()) -
Tests written first as failing specs against testing.openelis-global.org so
failure traces drive the diagnosis (not theory) - Audit wiring via Option A
(explicit AuditTrailService calls in StorageLocationServiceImpl +
SampleStorageServiceImpl) — Option B (refactor to
AuditableBaseObjectServiceImpl) would ripple through every caller of the
polymorphic services

architecture_findings: storage_audit: - StorageLocationServiceImpl is
polymorphic (insert(Object) / update(Object) / delete(Object) with instanceof
branching across 5 entity types) — can't cleanly extend
AuditableBaseObjectServiceImpl - Storage entities all extend
BaseObject<Integer>, so getStringId() works for the audit trail -
BaseObject.sysUserId is @Transient String — separate from entities' own
persisted Integer SYS_USER_ID column. Both must be set. -
SampleStorageServiceImpl hardcodes assignedByUserId/movedByUserId = 1 at lines
356, 648, 690, 887, 938 — needs real user propagation -
SystemAuditEventRestController has hard-coded SYSTEM_ENTITY_TABLE_NAMES
allowlist at line 52 — even with reference_tables registration + history writes,
storage tables won't appear in the UI without allowlist update - Table names
@Table(name=...) are case-sensitive: 5 uppercase (STORAGE_ROOM, STORAGE_DEVICE,
STORAGE_SHELF, SAMPLE_STORAGE_ASSIGNMENT, SAMPLE_STORAGE_MOVEMENT) and 2
lowercase (storage_rack, storage_box)

storage_selector: - 4 usage sites for StorageLocationSelector /
LocationSearchAndCreate: Storage Dashboard (/Storage/samples, in modal), Order
Label & Store (/order/label, inline), Add Order Sample Type
(/SamplePatientEntry, two- tier), Result Entry (/result, expandable row) -
LocationManagementModal renders LocationSearchAndCreate directly without the
higher-level StorageLocationSelector wrapper — so the
'storage-location-selector' data-testid is NOT in the modal subtree - Carbon
ComboBox: data-testid is forwarded to the wrapper, role=combobox goes elsewhere;
semantic role selectors (getByRole) work better - Carbon TextInput: renders as
role=textbox (not combobox) - Carbon OverflowMenu: the testid'd element IS the
trigger button (no nested <button>) - Order workflow: /order/label?labNumber=X
redirects to /order/enter when the order isn't yet at the label step
(intentional step-ordering) - Frontend prepends serverBaseUrl
("/api/OpenELIS-Global") from config.json — Playwright's request fixture must
too

production_bugs_surfaced_by_specs:

- id: BUG-1 title: Search dropdown tree never repopulates after first page load
  severity: blocking evidence:

  - test-results/.../trace.zip shows /api/OpenELIS-Global/rest/storage/rooms
    called once at page-load (06:52:46.566Z, 200) and never again after the user
    opens the modal + clicks the search input
  - DOM snapshot: <ul className="tree-root"> renders empty even though API
    returned 3 rooms when called via curl
  - Failure mode: user clicks search input → tree opens → empty list → nothing
    to click suspected_cause: | LocationFilterDropdown renders
    LocationSearchAndCreate's tree-container conditionally on isOpen=true.
    LocationTreeView's useEffect should fetch /rest/storage/rooms on mount, but
    the network trace shows that fetch never fires after the modal opens. Either
    (a) LocationTreeView is never mounting (LocationAutocomplete renders instead
    because showAutocomplete is true), or (b) it mounts but the useEffect is
    suppressed by some React lifecycle quirk (memoization, stale closure, parent
    re-render with same props). next_step: | Add a probe in the spec that
    asserts /rest/storage/rooms is called after the search-input click — if not
    called, LocationTreeView isn't mounting and the bug is in
    LocationFilterDropdown's branch logic. If called but empty, the bug is
    downstream.

- id: BUG-2 title: Order workflow can't deep-link past Step 1 severity:
  blocks-test (not production) evidence:
  - Site 2 spec snapshot shows breadcrumb "Home / Add Order / Enter Order" after
    navigating to /order/label?labNumber=X suspected_cause: | OrderProvider /
    OrderWorkflowLayout enforces step ordering — likely intentional. Not a
    production bug; just means our Site 2 test must walk Steps 1-2 first OR find
    an order already at Step 3. next_step: | Either find an existing API to list
    orders with their step state, or walk through the Enter → Collect → Label
    flow programmatically.

todos:

# Phase 1: Liquibase

- id: liquibase-register content: "Register 7 storage tables in reference_tables
  with keep_history='Y'" status: completed commit: 4bf7e9b27 files:
  - src/main/resources/liquibase/3.5.x.x/005-register-storage-audit-trail.xml
  - src/main/resources/liquibase/3.5.x.x/base.xml

# Phase 2: Track A regression specs

- id: shared-pom content: "Storage location selector Page Object — semantic role
  selectors per playwright-best-practices.md" status: completed commits:
  [1218f510a, 90b407e32] file:
  frontend/playwright/fixtures/storage-location-selector.ts

- id: site1-dashboard-spec content: "Storage Dashboard regression spec (select
  existing + create inline)" status: completed-with-1-passing-1-failing commit:
  3945f1303 file:
  frontend/playwright/tests/foundational/core/storage-assign-dashboard.spec.ts
  note: | create-inline test PASSES. select-existing test FAILS — surfaces BUG-1
  (production bug, not test issue).

- id: site2-order-label-spec content: "Order Label & Store regression spec"
  status: completed-with-2-failing commit: 8d423b50f file:
  frontend/playwright/tests/foundational/core/storage-assign-order-label.spec.ts
  note: Both tests fail due to BUG-2 (deep-link redirect)

- id: site3-add-order-spec content: "Add Order Sample Type route smoke spec"
  status: completed-passing commit: d7542d41a file:
  frontend/playwright/tests/foundational/core/storage-assign-add-order.spec.ts

- id: site4-result-spec content: "Result Entry route smoke spec" status:
  completed-passing commit: d7542d41a file:
  frontend/playwright/tests/foundational/core/storage-assign-result.spec.ts

# Phase 3: Diagnose & fix the production bugs surfaced by the specs

# ⇨ MOVED TO SUB-PLAN: ogc-253_track-a_dropdown_regression_remediation.plan.md

- id: regression-fix content: | Pivoted to focused sub-plan. Sub-plan covers:
  pin down BUG-1 root cause, fix LocationTreeView mount/data-load behaviour, get
  all 4 Track A specs green, fix BUG-2 (Site 2 navigation strategy), polish
  toast assertions. status: pivoted-to-subplan

# Phase 4: Backend audit wiring (paused — depends on regression-fix being

# at least at a stable failing point so spec changes don't pile up)

- id: audit-allowlist content: "Add 7 storage tables to
  SystemAuditEventRestController.SYSTEM_ENTITY_TABLE_NAMES (line 52)" status:
  pending file:
  src/main/java/org/openelisglobal/audittrail/controller/rest/SystemAuditEventRestController.java

- id: audit-storage-location-service content: | Wire AuditTrailService into
  StorageLocationServiceImpl (Option A): inject AuditTrailService +
  EntityManager (detach), add resolveTableName(Object) helper using the existing
  DAO fields, call saveNewHistory/saveHistory in each polymorphic
  insert/update/delete branch +
  createRoom/updateRoom/deleteRoom/deleteLocationWithCascade. status: pending

- id: audit-sample-storage-service content: | Wire AuditTrailService into
  SampleStorageServiceImpl, propagate real sysUserId. Add String sysUserId param
  to interface methods (assignSampleItemWithLocation,
  moveSampleItemWithLocation, disposeSampleItem, updateAssignmentMetadata).
  Replace hardcoded userId=1 at lines 356, 648, 690, 887, 938. Update controller
  to pass getSysUserId(request) (inherited from BaseRestController →
  ControllerUtills). Update existing unit tests' call sites. status: pending

# Phase 5: Backend integration test

- id: audit-integration-test content: | StorageAuditTrailIntegrationTest — model
  on SystemAuditTrailIntegrationTest. Replace mocked AuditTrailService via
  ReflectionTestUtils, exercise insert/update/delete on StorageRoom,
  assign/move/dispose on SampleItem, verify History rows appear. status: pending
  file:
  src/test/java/org/openelisglobal/storage/service/StorageAuditTrailIntegrationTest.java

# Phase 6: Track B (story)

- id: story-spec content: | End-to-end story spec under demo/core/: assign →
  move → dispose → verify in /AuditTrailReport UI + CSV export. Becomes passable
  once Phase 4-5 land. status: pending file:
  frontend/playwright/tests/demo/core/storage-audit-trail-story.spec.ts

# Phase 7: Cleanup

- id: delete-skipped-cypress content: | Delete
  frontend/cypress/e2e/storageAssignment.cy.js (skipped scaffolding replaced by
  Playwright). Update STORAGE_TESTS_README.md with pointer to new Playwright
  specs. Delete StorageAssignmentPage.js if unreferenced. status: pending

# Phase 8: Final

- id: final-verification content: | mvn spotless:apply, cd frontend && npm run
  format, gh pr checks green, mark draft PR #3428 ready for review. status:
  pending

commits_so_far:

- 4bf7e9b27: feat(audit) register storage tables in reference_tables
- 1218f510a: test(storage) initial Playwright fixture
- 3945f1303: test(storage) Storage Dashboard regression spec
- 8d423b50f: test(storage) Order Label & Store regression spec
- d7542d41a: test(storage) Sites 3+4 route smoke specs
- 3d26556ee: test(storage) playwright-best-practices.md compliance
- 8fac33ffd: test(storage) iteration 2 — fix discovery against testing server
- 90b407e32: test(storage) rewrite POM with semantic role selectors

verification: current_track_a_status: | Against
BASE_URL=https://testing.openelis-global.org: - setup auth: pass -
storage-assign-dashboard create inline: pass - storage-assign-dashboard select
existing: FAIL (BUG-1, real bug) - storage-assign-order-label assigns existing:
FAIL (BUG-2, navigation) - storage-assign-order-label creates inline: FAIL
(BUG-2, navigation) - storage-assign-add-order route loads: pass -
storage-assign-result route loads: pass

resume_check: - cd frontend && BASE_URL=https://testing.openelis-global.org
TEST_USER=admin TEST_PASS='adminADMIN!' npx playwright test
"playwright/tests/foundational/core/storage-assign-\*.spec.ts" --project=setup
--project=core-app --reporter=list - gh pr checks 3428
