---

name: OGC-253 Track A — dropdown regression remediation (sub-plan) overview:
"Sub-plan of ogc-253_storage_audit_and_regression_full.plan.md. Two discoveries
drove this split: (1) Track A specs running against testing.openelis-global.org
surfaced a real production bug (BUG-1) — clicking the Search input in the Manage
Location modal opens a tree but never loads rooms, leaving the user with nothing
to pick. (2) An architecture audit confirmed the user's intuition that this
subsystem is overengineered. Patching the symptom (BUG-1) without acknowledging
the structural debt would set up the next regression.

This sub-plan covers: pin down BUG-1's root cause, fix it minimally, AND surface
the structural debt with a follow-up plan so the org can decide whether to
invest in simplification." status: in-progress parent_plan:
ogc-253_storage_audit_and_regression_full.plan.md branch:
claude/magical-albattani pr:
https://github.com/DIGI-UW/OpenELIS-Global-2/pull/3428

# ──────────────────────────────────────────────────────────────────────────

# SECTION 1: ARCHITECTURE AUDIT FINDINGS

# ──────────────────────────────────────────────────────────────────────────

architecture_audit: surface_area: total_lines_of_production_code: 4516
largest_components: - EnhancedCascadingMode.jsx: 1837 # 26 useState, 14
useEffect - LocationManagementModal.jsx: 1010 # 19 useState, 5 useEffect -
LocationSearchAndCreate.jsx: 580 # 2 useRef - UnifiedBarcodeInput.jsx: 315 -
StorageLocationSelector.jsx: 226 - LocationTreeView.jsx: 179 -
CascadingDropdownMode.jsx: 174 # legacy - LocationFilterDropdown.jsx: 154
test_lines: 3936 # tests are also large

spec_vs_reality_gap: research_md_proposal: | The original design in
specs/001-sample-storage/research.md §3 shows a Carbon Dropdown cascading
pattern with ~70 lines: - 4 selected\* states - 4 list states - 3 useEffects
(cascade fetches on parent change) - Plain Carbon <Dropdown> per level
actual_implementation: | EnhancedCascadingMode.jsx ballooned to 1837 lines
with: - 26 useState hooks (6× the design) - 14 useEffect hooks (4.7× the
design) - Custom downshift state overrides - Inline-create flow per level
(creating/pending/showAddLink/showConfirm as 4 separate state vars per level × 4
levels = 16 of the 26 states) - "Last-modified wins" tracking from the barcode
amendment - Multiple ComposedModals for confirm-create interpretation: | The
cascading dropdown was correctly designed as a primitive. Each product amendment
added its features as additional state in the same component instead of
decomposing. The result is one giant stateful component with no clear
single-responsibility boundary.

structural_redundancy: three_layer_mode_dispatch: | The location-selection UX
has THREE NESTED MODE TOGGLES:

        StorageLocationSelector
          └── if (workflow === 'orders' || 'results'):
              ├── CompactLocationView                  ← compact display
              └── LocationManagementModal              ← expanded modal
                  ├── LocationSearchAndCreate
                  │   ├── if (!showCreateForm):
                  │   │   └── LocationFilterDropdown   ← search wrapper
                  │   │       ├── if (!showAutocomplete):
                  │   │       │   └── LocationTreeView
                  │   │       └── else:
                  │   │           └── LocationAutocomplete
                  │   └── else:
                  │       └── EnhancedCascadingMode    ← inline create
                  └── UnifiedBarcodeInput
          └── else (legacy):
              ├── if (mode === 'dropdown'): CascadingDropdownMode
              ├── if (mode === 'autocomplete'): AutocompleteMode
              └── if (mode === 'barcode'): BarcodeScanMode

      Each layer adds state, transitions, and edge cases. The three modes
      that toggle in the same flow (workflow → showCreateForm → showAutocomplete)
      create 2³=8 combinations, not all coherent.

    parallel_implementations: |
      Two complete parallel implementations exist for the same
      "select a storage location" capability:

        - LEGACY MODE (StorageLocationSelector.jsx:198-212): used by
          OrderLabel.jsx via mode="autocomplete". Renders one of three
          *Mode subcomponents directly inline.
        - TWO-TIER DESIGN (StorageLocationSelector.jsx:140-180): used by
          StorageDashboard, AddOrder SampleType, ResultEntry via
          workflow="orders"|"results". Renders CompactLocationView +
          LocationManagementModal.

      OrderLabel.jsx is the only remaining caller of the legacy autocomplete
      path. The "mode" prop is effectively dead code in 4 of 5 sites but
      lives on as a bug-multiplier surface.

    cross_directory_coupling: |
      LocationFilterDropdown lives in StorageDashboard/ but is reused inside
      the modal flow (LocationSearchAndCreate imports it from the dashboard
      directory). Its docstring frames it as a "Filter by locations..."
      dropdown. Repurposing it for assignment carries dashboard-filter
      defaults (e.g., allowInactive=true) that are wrong for assignment.

    state_smell_indicators:
      - "EnhancedCascadingMode: 4 inline-create flag families × 4 levels = 16
        boolean useStates that should be a single Map<level, CreateState>"
      - "LocationManagementModal: locationUpdateTrigger useState — manual
        re-render counter, classic React state anti-pattern"
      - "LocationManagementModal tracks initialPositionCoordinate +
        initialConditionNotes alongside the live values to detect changes —
        textbook reducer use case"
      - "LocationSearchAndCreate uses useRef + commented-out 'CRITICAL:' /
        'Don't sync when form just closed' workarounds (lines 36-94) — the
        sync-from-prop logic has been patched repeatedly, indicating a
        stale-state bug that was worked around rather than fixed"

call_sites_actual: - file: frontend/src/components/storage/StorageDashboard.jsx
mode: workflow="orders" (two-tier) - file:
frontend/src/components/order/steps/OrderLabel.jsx mode: mode="autocomplete"
(legacy) - file: frontend/src/components/addOrder/SampleType.js mode:
workflow="orders" (two-tier) - file:
frontend/src/components/resultPage/SearchResultForm.js mode: workflow="results"
(two-tier) - file: frontend/src/components/notebook/NoteBookInstanceEntryForm.js
mode: TBD (also imports it — newly discovered, not in original 4) - file:
frontend/src/components/addOrder/GpsCoordinatesCapture.js mode: TBD (suspicious
— GPS shouldn't need storage selector)

# ──────────────────────────────────────────────────────────────────────────

# SECTION 2: BUG-1 ROOT-CAUSE INVESTIGATION

# ──────────────────────────────────────────────────────────────────────────

bug_1: symptom: | User clicks the Search-for-location input in the
LocationManagementModal. The input gains focus (visible in DOM snapshot as
[active]). The LocationFilterDropdown's dropdown content area renders — but the
inner <ul className="tree-root"> is empty. User has nothing to click.

evidence: - test-results/.../trace.zip network log: -
/api/OpenELIS-Global/rest/storage/rooms returns 200 with 3 rooms at
06:52:46.566Z (page mount of StorageDashboard) - User opens modal at ~06:52:55,
clicks search input ~06:52:56 - NO further /rest/storage/rooms call in the next
12 seconds - DOM accessibility snapshot: - textbox "Search for location..."
[active] - generic > generic > list ← the empty <ul>

hypotheses_to_test_in_order: - id: H1 claim: "LocationTreeView is never mounted
(LocationAutocomplete renders instead)" check: |
LocationFilterDropdown.jsx:108-136 conditionally renders one of
location-autocomplete-container OR location-tree-container based on
showAutocomplete state. Initial state is false → tree should render. BUT —
LocationSearchAndCreate.jsx may pass props or render differently when nested in
the modal vs the dashboard. Investigate showAutocomplete initial value when
LocationFilterDropdown is constructed inside the modal. diagnostic: | Add a
network probe to the spec: assert
page.waitForResponse('\*\*/rest/storage/rooms') fires within 5s of clicking the
search input. If the request never fires, LocationTreeView never mounted. - id:
H2 claim: "LocationTreeView mounts but its useEffect doesn't fire" check: |
useEffect with [] deps SHOULD fire on mount. React 17 strict mode
double-invocation in dev wouldn't apply in prod. Possible: parent memoization
keeps the same component instance across modal open/close cycles, suppressing
re-mount. diagnostic: | If H1 is wrong, log component lifecycle in
LocationTreeView via console.log in useEffect — should see one log on each modal
open. - id: H3 claim: "useEffect fires but the API call fails silently" check: |
getFromOpenElisServer accepts a success and error callback. LocationTreeView's
error path: setRooms([]). If the API call fails (auth, CORS, parse error), rooms
stays empty. diagnostic: | Already disproven by network trace —
/rest/storage/rooms returned 200 at page load. But check whether subsequent
calls would fail (e.g., session expired during modal lifetime — unlikely in
12s).

most_likely_root_cause: | H1 — LocationFilterDropdown's mode-toggle logic, OR
LocationSearchAndCreate's sync-from-prop ref tricks (lines 36-94 of
LocationSearchAndCreate.jsx have explicit "CRITICAL: Don't sync when form just
closed" workarounds), is keeping LocationTreeView from mounting in the modal
context. This is the architectural debt manifesting as a real bug.

# ──────────────────────────────────────────────────────────────────────────

# SECTION 3: REMEDIATION TODOS

# ──────────────────────────────────────────────────────────────────────────

todos:

- id: probe-network content: | Add a single test step to
  storage-assign-dashboard.spec.ts that asserts /rest/storage/rooms is requested
  AFTER the user clicks the search input. This converts the failure mode from
  "selector not found" into "the network request you expected didn't fire" —
  clearer diagnostic than the current empty-tree symptom. status: pending file:
  frontend/playwright/tests/foundational/core/storage-assign-dashboard.spec.ts

- id: confirm-h1-or-h2 content: | Run the probed test. Use the result to decide
  between H1 and H2: no /rest/storage/rooms call → H1 (LocationTreeView never
  mounts); call fires but tree still empty → H2 or H3. status: pending

- id: minimal-fix content: | Apply the smallest possible fix in the production
  code that makes LocationTreeView load rooms when the search dropdown opens
  inside the modal. Likely candidates (depend on H1/H2 outcome): - Force
  LocationTreeView to remount when the modal opens (key prop) - Refactor
  LocationSearchAndCreate's sync-from-prop logic to not suppress the tree-view
  branch - Decouple LocationFilterDropdown's filter-defaults from its
  assignment-flow defaults (allowInactive, placeholder) status: pending

- id: bug-1-passes content: | storage-assign-dashboard.spec.ts → "assigns
  existing storage location via tree/search" passes against
  testing.openelis-global.org. status: pending

- id: site2-navigation-fix content: | BUG-2 mitigation: Site 2 spec needs to
  find an order already at the Label step (not /order/enter). Either query an
  API for orders with currentStep='LABEL', or walk through Steps 1-2 in the
  test. Choose the simpler one based on what REST endpoints expose. status:
  pending file:
  frontend/playwright/tests/foundational/core/storage-assign-order-label.spec.ts

- id: structural-debt-followup content: | File a follow-up plan (or Jira)
  capturing the architecture audit findings. NOT to be addressed in this
  sub-plan, but the audit document needs to outlive this session. Recommended
  deliverable: - Move LocationFilterDropdown out of StorageDashboard/ into a
  shared location, or split into FilterDropdown (dashboard) + SearchPicker
  (modal) with no shared code (their needs diverged) - Decompose
  EnhancedCascadingMode: extract a useCascadingLevel(level) hook that owns one
  level's 6 states. Top component then has 4 hook calls instead of 26
  useStates. - Delete legacy modes (CascadingDropdownMode, AutocompleteMode,
  BarcodeScanMode) and the StorageLocationSelector mode= branch once OrderLabel
  migrates to two-tier - Replace LocationManagementModal's locationUpdateTrigger
  and initial\* tracking with a useReducer status: pending-followup-doc

# ──────────────────────────────────────────────────────────────────────────

# SECTION 4: VERIFICATION

# ──────────────────────────────────────────────────────────────────────────

verification: before_remediation_baseline: against:
BASE_URL=https://testing.openelis-global.org pass: [setup,
dashboard-create-inline, add-order-route, result-route] fail:
[dashboard-select-existing, order-label-assigns, order-label-creates]

acceptance: - All 4 Track A specs pass against testing.openelis-global.org - The
fix is in production code, not test scaffolding - The architecture findings
document survives the session - Parent plan can resume from
todos.audit-allowlist (Phase 4)

resume_command: | cd frontend BASE_URL=https://testing.openelis-global.org
TEST_USER=admin TEST_PASS='adminADMIN!' \
 npx playwright test
"playwright/tests/foundational/core/storage-assign-\*.spec.ts" \
 --project=setup --project=core-app --reporter=list
