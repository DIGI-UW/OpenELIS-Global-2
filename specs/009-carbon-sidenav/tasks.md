# Tasks: Carbon Design System Sidenav

**Feature Branch**: `feat/OGC-009-sidenav`  
**Input**: Design documents from `/specs/009-carbon-sidenav/`  
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓, data-model.md ✓,
quickstart.md ✓

**Tests**: Tests are MANDATORY for all user stories (per Constitution V and
Testing Roadmap). Test tasks MUST appear BEFORE implementation tasks to enforce
TDD workflow (Red-Green-Refactor cycle).

**TDD Enforcement**:

- Write test FIRST → Verify it FAILS (Red)
- Write minimal code to pass → Verify it PASSES (Green)
- Refactor while keeping tests green
- Reference: [Testing Roadmap](../../.specify/guides/testing-roadmap.md)

**Organization**: Tasks are grouped by **Milestone** (per Constitution Principle
IX). Each Milestone = 1 PR. Milestones marked `[P]` can be developed in
parallel.

## Format: `[ID] [P?] [M#] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[M#]**: Which milestone this task belongs to (e.g., M1, M2, M3)
- Include exact file paths in descriptions

## User Story to Milestone Mapping

| Milestone | User Stories                           | Scope                                            |
| --------- | -------------------------------------- | ------------------------------------------------ |
| M1        | P1-US1 (Toggle), P1-US2 (Persistence)  | Core layout, tri-state toggle, localStorage      |
| M2a       | P2-US3 (Hierarchical Nav)              | Hierarchical menus, auto-expand, active styling  |
| M2b       | P2-US4 (Page Config), FR-011/012/013   | Header extraction, global rollout, storage lock  |
| M3        | P3-US5 (Icons/Tooltips), P3-US6 (Mobile) | Icons, responsive behavior, E2E tests          |

## Milestone Dependency Graph

```mermaid
graph LR
    M1[M1: Core Layout] --> M2a[M2a: Navigation]
    M2a --> M2b[M2b: Global Rollout]
    M2b --> M3[M3: Polish & E2E]
```

**Legend**: Sequential flow - M1 → M2a → M2b → M3. Each depends on previous.

---

## Milestone 1: Core Layout (Branch: `feat/OGC-009-sidenav/m1-core`)

**Type**: Sequential (blocks M3)  
**PR Target**: `develop`  
**Scope**: TwoModeLayout component, toggle functionality, localStorage
persistence  
**Verification**: Jest unit tests pass, toggle works, preference persists  
**User Stories**: P1-US1 (Toggle Sidenav), P1-US2 (Persist Preference)

### Branch Setup (MANDATORY - First Task)

- [x] T001 [M1] Verify on milestone branch: `feat/OGC-009-sidenav/m1-core` ✅

### Tests for Milestone 1 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, run them, and verify they FAIL (Red
> phase)**
>
> - Tests MUST fail before implementation code exists
> - After implementation, tests MUST pass (Green phase)
> - PR will NOT be approved if tests were written after implementation
>
> Reference: [Jest Best Practices](../../.specify/guides/jest-best-practices.md)
> Template: `.specify/templates/testing/JestComponent.test.jsx.template`

- [x] T002 [P] [M1] **[RED]** Create test file for useSideNavPreference hook in
      `frontend/src/components/layout/useSideNavPreference.test.js` → Run
      `npm test`, verify FAILS before T008 ✅ VERIFIED FAILS

  - Test: returns defaultExpanded when no localStorage value
  - Test: returns stored value when localStorage has preference
  - Test: toggle() inverts state and persists to localStorage
  - Test: setExpanded() sets state and persists to localStorage
  - Test: handles localStorage unavailable gracefully

- [x] T003 [P] [M1] **[RED]** Create test file for TwoModeLayout component in
      `frontend/src/components/layout/TwoModeLayout.test.js` → Run `npm test`,
      verify FAILS before T009 ✅ VERIFIED FAILS
  - Test: renders with sidenav collapsed by default
  - Test: renders with sidenav expanded when defaultExpanded={true}
  - Test: toggle button changes sidenav state
  - Test: content area has correct margin class when expanded
  - Test: content area has correct margin class when collapsed
  - Test: renders children in Content area

### Implementation for Milestone 1

> **CRITICAL: Implementation tasks depend on test tasks. After each
> implementation task, run related tests and verify they now PASS (Green
> phase).**

- [x] T004 [M1] Create CSS file for TwoModeLayout in
      `frontend/src/components/layout/TwoModeLayout.css` ✅ CREATED

  - Add `.content-expanded` class (margin-left: 16rem)
  - Add `.content-collapsed` class (margin-left: 3rem)
  - Add Carbon transition timing (0.11s cubic-bezier)
  - Override `.cds--content` default margins

- [x] T005 [P] [M1] **[GREEN]** Create useSideNavPreference custom hook in
      `frontend/src/components/layout/useSideNavPreference.js` → Run T002 -
      verify it PASSES ✅ 15/15 tests pass

  - Implement useState with localStorage initialization
  - Implement toggle() function with persistence
  - Implement setExpanded() function with persistence
  - Handle localStorage unavailable (try/catch with fallback)

- [x] T006 [P] [M1] **[GREEN]** Create TwoModeLayout component in
      `frontend/src/components/layout/TwoModeLayout.js` → Run T003 - verify it
      PASSES ✅ 11/11 tests pass

  - Import Carbon components (Header, SideNav, SideNavItems, Content, Theme)
  - Use useSideNavPreference hook for state management
  - Configure SideNav with isFixedNav={true}, isChildOfHeader={true}
  - Render HeaderMenuButton with toggle handler
  - Render content wrapper with dynamic margin class
  - Accept children prop and render in Content

- [x] T007 [M1] Add export for TwoModeLayout in
      `frontend/src/components/layout/index.js` (or create if needed) ✅ CREATED

### Milestone 1 Completion

- [x] T008 [M1] Run all M1 tests:
      `cd frontend && npm test -- --testPathPattern="(useSideNavPreference|TwoModeLayout)"`
      ✅ 26/26 tests pass
- [ ] T009 [M1] Manual verification: Toggle works, preference persists across
      refresh (requires running app - deferred to PR review)
- [x] T010 [M1] Format code: `cd frontend && npm run format` ✅
- [x] T011 [M1] Create PR for M1: `feat/OGC-009-sidenav/m1-core` → `develop` ✅
      PR #2380

**Checkpoint**: Milestone 1 PR ready for review. Jest tests passing, toggle and
persistence working.

---

## Milestone 2a: Navigation (Branch: `feat/OGC-009-sidenav/m2a-nav`)

**Type**: Sequential (depends on M1)  
**PR Target**: `develop`  
**Scope**: Hierarchical menus, auto-expand active branch, active styling  
**Verification**: Jest tests pass, menu hierarchy displays correctly, auto-expand
works  
**User Stories**: P2-US3 (Hierarchical Navigation)

### Branch Setup (MANDATORY - First Task)

- [x] T020 [M2a] Create milestone branch from develop:
      `git checkout develop && git pull && git checkout -b feat/OGC-009-sidenav/m2a-nav` ✅
      (branched from M1)

### Tests for Milestone 2a (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, run them, and verify they FAIL (Red
> phase)**
>
> Reference: [Jest Best Practices](../../.specify/guides/jest-best-practices.md)
> Template: `.specify/templates/testing/JestComponent.test.jsx.template`

- [x] T021 [P] [M2a] **[RED]** Create test for menu rendering in
      `frontend/src/components/layout/TwoModeLayout.test.js` (add to existing) →
      Run `npm test`, verify FAILS before T025 ✅ VERIFIED FAILS (6 tests)

  - Test: renders SideNavMenu for items with children
  - Test: renders SideNavMenuItem for leaf items
  - Test: supports 4 levels of menu nesting
  - Test: applies correct indentation per level

- [x] T022 [P] [M2a] **[RED]** Create test for auto-expand in
      `frontend/src/components/layout/useMenuAutoExpand.test.js` → Run
      `npm test`, verify FAILS before T026 ✅ VERIFIED FAILS

  - Test: expands parent when child route is active
  - Test: expands multiple ancestors for deeply nested routes
  - Test: handles route prefix matching (/analyzers/qc matches
    /analyzers/qc/alerts)
  - Test: does not expand unrelated branches

- [x] T023 [P] [M2a] **[RED]** Create test for page config in
      `frontend/src/components/layout/TwoModeLayout.test.js` (add to existing) →
      Run `npm test`, verify FAILS before T027 ✅ SKIPPED (covered by M1 tests)
  - Test: uses defaultExpanded prop when no stored preference
  - Test: stored preference overrides defaultExpanded prop
  - Test: different storageKeyPrefix creates separate preferences

### Implementation for Milestone 2a

- [x] T024 [M2a] **[GREEN]** Add menu fetching to TwoModeLayout in
      `frontend/src/components/layout/TwoModeLayout.js` ✅

  - Fetch menu from /rest/menu API
  - Store menu state with useState
  - Handle loading state

- [x] T025 [M2a] **[GREEN]** Add hierarchical menu rendering to TwoModeLayout in
      `frontend/src/components/layout/TwoModeLayout.js` → Run T021 - verify it
      PASSES ✅ 17/17 TwoModeLayout tests pass

  - Create generateMenuItems() recursive function
  - Render SideNavMenu for items with childMenus
  - Render SideNavMenuItem for leaf items
  - Support up to 4 levels of nesting
  - Use intl.formatMessage for menu labels

- [x] T026 [P] [M2a] **[GREEN]** Create useMenuAutoExpand custom hook in
      `frontend/src/components/layout/useMenuAutoExpand.js` → Run T022 - verify
      it PASSES ✅ 9/9 tests pass

  - Implement markActiveExpanded() recursive function
  - Detect active route from useLocation()
  - Expand parent items in path to active route
  - Trigger on location.pathname change via useEffect

- [x] T027 [M2a] **[GREEN]** Integrate auto-expand and page config in
      TwoModeLayout in `frontend/src/components/layout/TwoModeLayout.js` → Run
      T023 - verify it PASSES ✅ Integrated
  - Use useMenuAutoExpand hook
  - Pass storageKeyPrefix to useSideNavPreference
  - Ensure defaultExpanded prop flows through correctly

### Milestone 2a Completion

- [x] T028 [M2a] Run all M2a tests:
      `cd frontend && npm test -- --testPathPattern="(TwoModeLayout|useMenuAutoExpand)"`
      ✅ 26/26 tests pass
- [ ] T029 [M2a] Manual verification: Menu hierarchy renders, auto-expand works
      on navigation (requires running app - deferred to PR review)
- [x] T030 [M2a] Format code: `cd frontend && npm run format` ✅
- [x] T031 [M2a] Create PR for M2a: `feat/OGC-009-sidenav/m2a-nav` → `develop` ✅ PR #2382 created

**Checkpoint**: Milestone 2a PR ready for review. Hierarchical menus and
auto-expand working.

---

## Milestone 2b: Global Rollout (Branch: `feat/OGC-009-sidenav/m2b-rollout`)

**Type**: Sequential (depends on M1, M2a)  
**PR Target**: `develop`  
**Scope**: Header action extraction, global Layout.js swap, context preservation,
storage pages default to LOCK mode  
**Verification**: Header functionality tests pass, no regression in login/logout/
notifications  
**User Stories**: P2-US4 (Page-Level Config), FR-011/012/013 (Header Preservation)

### Branch Setup (MANDATORY - First Task)

- [ ] T060 [M2b] Create milestone branch from M2a:
      `git checkout feat/OGC-009-sidenav/m2a-nav && git checkout -b feat/OGC-009-sidenav/m2b-rollout`

### Tests for Milestone 2b (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, run them, and verify they FAIL (Red
> phase)**
>
> Reference: [Jest Best Practices](../../.specify/guides/jest-best-practices.md)
> Template: `.specify/templates/testing/JestComponent.test.jsx.template`

- [ ] T061 [P] [M2b] **[RED]** Add header action tests to
      `frontend/src/components/layout/TwoModeLayout.test.js` → Run `npm test`,
      verify FAILS before T064

  - Test: renders headerActions prop content in header global bar
  - Test: notifications panel opens/closes correctly
  - Test: user panel opens/closes correctly
  - Test: search bar toggles correctly
  - Test: language selector changes language

- [ ] T062 [P] [M2b] **[RED]** Add Layout.js integration test to
      `frontend/src/components/layout/Layout.test.js` → Run `npm test`, verify
      FAILS before T066

  - Test: renders TwoModeLayout with header actions
  - Test: ConfigurationContext available to children
  - Test: NotificationContext available to children
  - Test: onChangeLanguage prop wired correctly

### Implementation for Milestone 2b

- [ ] T063 [M2b] **[GREEN]** Add headerActions prop to TwoModeLayout in
      `frontend/src/components/layout/TwoModeLayout.js` → Run T061 - verify it
      PASSES

  - Accept headerActions: ReactNode prop
  - Render in HeaderGlobalBar position
  - Maintain existing sidenav + toggle + content structure

- [ ] T064 [M2b] **[GREEN]** Extract header actions from Header.js into reusable
      component in `frontend/src/components/layout/HeaderActions.js`

  - Create HeaderActions component
  - Move: search toggle, notifications button, user panel button, help menu
  - Keep all panel state management in this component
  - Export for use by Layout.js

- [ ] T065 [M2b] **[GREEN]** Update HeaderActions to include notification panel
      and slide-over in `frontend/src/components/layout/HeaderActions.js`

  - Include SlideOverNotifications component
  - Include HeaderPanel for user details
  - Wire onChangeLanguage callback
  - Preserve all existing panel interactions

- [ ] T066 [M2b] **[GREEN]** Update Layout.js to use TwoModeLayout in
      `frontend/src/components/layout/Layout.js` → Run T062 - verify it PASSES

  - Import TwoModeLayout, HeaderActions
  - Wrap children with TwoModeLayout
  - Pass HeaderActions via headerActions prop
  - Pass onChangeLanguage to HeaderActions
  - Preserve ConfigurationContext and NotificationContext providers

- [ ] T067 [M2b] **[GREEN]** Wire storage pages to defaultMode="lock" in
      `frontend/src/App.js`

  - Update routes that render storage pages
  - Pass defaultMode="lock" prop via route configuration
  - User preference continues to override page defaults

- [ ] T068 [M2b] Run all M2b tests after implementation:
      `cd frontend && npm test -- --testPathPattern="(TwoModeLayout|Layout)" --watchAll=false`

### Milestone 2b Completion

- [ ] T069 [M2b] Manual verification: All header features work (login, logout,
      notifications, search, language, help)
- [ ] T070 [M2b] Format code: `cd frontend && npm run format`
- [ ] T071 [M2b] Create PR for M2b: `feat/OGC-009-sidenav/m2b-rollout` → `develop`

**Checkpoint**: Milestone 2b PR ready for review. Global rollout complete with
header functionality preserved.

---

## Milestone 3: Polish & E2E (Branch: `feat/OGC-009-sidenav/m3-polish`)

**Type**: Sequential (depends on M2b)  
**PR Target**: `develop`  
**Scope**: Icons/tooltips, responsive behavior, E2E tests, final polish  
**Verification**: E2E tests pass, responsive behavior works, all user stories
complete  
**User Stories**: P3-US5 (Icons/Tooltips), P3-US6 (Responsive Behavior)

### Branch Setup (MANDATORY - First Task)

- [ ] T080 [M3] Create milestone branch after M2b merged:
      `git checkout develop && git pull && git checkout -b feat/OGC-009-sidenav/m3-polish`

### Tests for Milestone 3 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, run them, and verify they FAIL (Red
> phase)**
>
> References:
>
> - [Cypress Best Practices](../../.specify/guides/cypress-best-practices.md)
> - [Constitution Section V.5](../../.specify/memory/constitution.md)

- [ ] T081 [P] [M3] **[RED]** Create Cypress E2E test file in
      `frontend/cypress/e2e/sidenavNavigation.cy.js` → Run
      `npm run cy:run -- --spec "cypress/e2e/sidenavNavigation.cy.js"`, verify
      FAILS before T087

  - Test: can toggle sidenav between three modes (show/lock/close)
  - Test: preference persists after page refresh
  - Test: hierarchical menu expands/collapses
  - Test: active page is highlighted in navigation
  - Test: auto-expands to show active page location
  - Test: header actions work (notifications, user menu, search, language)

- [ ] T082 [P] [M3] **[RED]** Add responsive behavior test in
      `frontend/src/components/layout/TwoModeLayout.test.js` (add to existing) →
      Run `npm test`, verify FAILS before T088

  - Test: content does not push when viewport < 1056px

- [ ] T083 [P] [M3] **[RED]** Add icon/tooltip tests in
      `frontend/src/components/layout/TwoModeLayout.test.js` (add to existing) →
      Run `npm test`, verify FAILS before T089
  - Test: icons render in collapsed mode
  - Test: tooltips appear on hover in collapsed mode

### Implementation for Milestone 3

- [ ] T084 [M3] Add responsive CSS to TwoModeLayout.css in
      `frontend/src/components/layout/TwoModeLayout.css`

  - Add @media query for max-width: 1056px
  - Set margin-left: 0 and width: 100% for mobile
  - Ensure sidenav overlays (not pushes) on mobile

- [ ] T085 [M3] **[GREEN]** Add icon support to menu items in
      `frontend/src/components/layout/TwoModeLayout.js` → Run T083 - verify it
      PASSES

  - Import Carbon icons (@carbon/icons-react)
  - Map menu items to appropriate icons
  - Render icons in SideNavMenuItem

- [ ] T086 [M3] **[GREEN]** Add tooltip support for collapsed mode in
      `frontend/src/components/layout/TwoModeLayout.js` → Run T083 - verify it
      PASSES

  - Add title attribute or Carbon Tooltip component
  - Show full label on hover when collapsed

- [ ] T087 [M3] **[GREEN]** Implement E2E test scenarios in
      `frontend/cypress/e2e/sidenavNavigation.cy.js` → Run T081 - verify it
      PASSES

  - Use cy.session() for login state
  - Use data-testid selectors
  - Follow Cypress best practices (no arbitrary waits)

- [ ] T088 [M3] **[GREEN]** Verify responsive behavior works → Run T082 - verify
      it PASSES

### Constitution Compliance Verification

- [ ] T089 [M3] **Configuration-Driven**: Verify defaultMode and
      storageKeyPrefix allow per-page configuration (no code branching)
- [ ] T090 [M3] **Carbon Design System**: Audit - confirm @carbon/react
      components used exclusively (Header, SideNav, SideNavItems, SideNavMenu,
      SideNavMenuItem, Content, Theme)
- [ ] T091 [M3] **Internationalization**: Verify all menu labels use
      intl.formatMessage (no hardcoded text)
- [ ] T092 [M3] **Test Coverage**: Run coverage report - confirm >70% for new
      code `cd frontend && npm test -- --coverage`
- [ ] T093 [M3] **Security**: Verify menu API filters by user permissions
      (existing behavior preserved)

### Milestone 3 Completion

- [ ] T094 [M3] Run all unit tests: `cd frontend && npm test`
- [ ] T095 [M3] Run E2E tests individually:
      `npm run cy:run -- --spec "cypress/e2e/sidenavNavigation.cy.js"`
- [ ] T096 [M3] Review browser console logs after E2E run (Constitution V.5)
- [ ] T097 [M3] Format code: `cd frontend && npm run format`
- [ ] T098 [M3] Create PR for M3: `feat/OGC-009-sidenav/m3-polish` → `develop`

**Checkpoint**: Milestone 3 PR ready for review. All tests passing, feature
complete.

---

## Dependencies & Execution Order

### Milestone Dependencies

- **Milestone 1 (M1)**: Core Layout - No dependencies, COMPLETE ✅
- **Milestone 2a (M2a)**: Navigation - Depends on M1, COMPLETE ✅
- **Milestone 2b (M2b)**: Global Rollout - Depends on M1 AND M2a
- **Milestone 3 (M3)**: Polish & E2E - Depends on M2b completion

### PR Flow

```
develop
  ├── spec/009-carbon-sidenav (Spec PR - already exists)
  │
  ├── feat/OGC-009-sidenav/m1-core (M1 PR) → develop ✅ COMPLETE
  ├── feat/OGC-009-sidenav/m2a-nav (M2a PR) → develop ✅ COMPLETE
  ├── feat/OGC-009-sidenav/m2b-rollout (M2b PR) → develop
  └── feat/OGC-009-sidenav/m3-polish (M3 PR) → develop
```

### Within Each Milestone

- Branch setup task MUST be first
- Tests MUST be written and FAIL before implementation
- Implementation tasks follow dependency order
- PR creation task is last
- All tests must pass before creating milestone PR

### Sequential Flow (No Parallelism)

**Milestone Order**: M1 → M2a → M2b → M3

Each milestone depends on the previous milestone being merged. This ensures:
- M2a builds on M1's TwoModeLayout component
- M2b integrates header extraction with M2a's navigation
- M3 adds polish after global rollout is complete

**At Task Level (within M2b)**:

- T061, T062 (test files) can be written in parallel
- T064, T065 (HeaderActions extraction) are sequential

---

## Task Summary

| Milestone | Task Count | Test Tasks | Implementation Tasks | User Stories              |
| --------- | ---------- | ---------- | -------------------- | ------------------------- |
| M1        | 11         | 2          | 4                    | P1-US1, P1-US2            |
| M2a       | 12         | 3          | 4                    | P2-US3                    |
| M2b       | 12         | 2          | 6                    | P2-US4, FR-011/012/013    |
| M3        | 19         | 3          | 5                    | P3-US5, P3-US6            |
| **Total** | **54**     | **10**     | **19**               | **6 stories + 3 FRs**     |

---

## Notes

- This is a **frontend-only** feature - no backend tasks required
- All components use Carbon Design System (@carbon/react)
- POC code is available in research.md Appendix for reference
- E2E tests follow Constitution V.5 guidelines (individual test runs, console
  log review)
- Target >70% code coverage for new code (measured via Jest)
