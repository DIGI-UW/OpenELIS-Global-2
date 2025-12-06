# Architecture Analysis: Sidenav Implementation Issues & Remediation

**Date**: December 5, 2025  
**Status**: ✅ Phase 1 & 2 Complete - Ready for Browser Verification  
**Branch**: `feat/OGC-009-sidenav/m2b-rollout`  
**Latest Commit**: `8fbd02109`

## Problem Diagnosis

We are experiencing regressions and "hacky" implementation patterns because of a
fundamental conflict between the legacy codebase architecture and the modern
Carbon Design System patterns we are trying to introduce.

### 1. State Fragmentation (The Root Cause)

- **Current State**: Both `Header.js` and `Layout.js` independently initialize
  the `useSideNavPreference` hook.
- **Consequence**: This creates two disconnected state instances. When `Header`
  toggles the menu, `Layout` (which controls the content margin) doesn't react
  immediately. It only syncs on page reload when both read from localStorage
  again.
- **Symptom**: "Content is not pushed by default" and "Sliding the nav out has
  no effect on the page."

### 2. Lack of Context Awareness

- **Current State**: Both components hardcode `storageKeyPrefix: "main"`.
- **Consequence**: The application cannot distinguish between "Home" (default
  closed) and "Storage" (default locked) contexts.
- **Symptom**: "On home page where nav should be collapsed by default it still
  shows."

### 3. Fighting the Framework (Carbon)

- **Current State**: We removed `HeaderContainer` (good), but we are still
  manually manipulating CSS margins (`.content-nav-locked`) instead of fully
  trusting Carbon's layout shell.
- **Consequence**: We have to maintain custom CSS that fights against Carbon's
  internal styles (like `overflow` handling).
- **Symptom**: "Ugly scrollbar" and "Dark mode inputs" leaking through because
  of global style conflicts in `index.scss` vs `Theme` wrapper.

### 4. Legacy "Big Ball of Mud" Styles

- **Current State**: `Style.css` contains broad, aggressive selectors like
  `.inputText { --cds-text-primary: black; }`.
- **Consequence**: These override Carbon's theme tokens, causing dark mode
  inputs on light backgrounds because `index.scss` sets a global dark theme that
  these classes inherit from.
- **Symptom**: "Dark mode inputs" (Black text on dark background or white text
  on white background depending on the specific clash).

## Remediation Plan (The "Right Way")

### Phase 1: Architecture Correction (State Lifting)

**Goal**: Make `Layout.js` the single source of truth for navigation state.

1. **Refactor `Layout.js`**:

   - Use `useLocation` to detect current route.
   - Determine `storageKeyPrefix` and `defaultMode` dynamically:
     - `/Storage` -> `key="storage"`, `default="lock"`
     - `/` -> `key="main"`, `default="close"`
   - Initialize `useSideNavPreference` ONCE here.
   - Pass `mode`, `toggle`, `isExpanded` to `Header` as props.

2. **Refactor `Header.js`**:
   - Remove `useSideNavPreference` internal call.
   - Become a pure controlled component (consumes props from Layout).

### Phase 2: Style Debt Removal

**Goal**: Stop fighting Carbon's styling system.

1. **Clean `Style.css`**:

   - Remove `.inputText`, `.inputSearch` overrides that force black text.
   - Remove `custom-sidenav-button` styles that force blue backgrounds.
   - Trust `<Theme theme="white">` to handle input colors correctly.

2. **Fix Global Theme**:
   - Investigate `index.scss`. If it forces a global dark theme, we must ensure
     the `Content` area creates a robust "light theme island" (which we did with
     `<Theme theme="white">`), but we must remove the CSS rules that pierce this
     island.

### Phase 3: Verification

1. **Automated E2E**: Run the new `sidenavNavigation.cy.js` to prove the state
   sync works without reloading.
2. **Manual Check**: Verify Storage defaults to locked, Home defaults to closed.

## Why This Happened

We attempted an "Enhancement" strategy (M2b) to avoid rewriting the complex
`Header.js`. However, "enhancing" a component by giving it _more_ independent
state responsibility when it should have had _less_ (lifting state up) was an
architectural mistake. We treated `Header.js` as a smart container when it
should be a dumb presentational component for the Layout's state.

**Critical Misunderstanding**: "Preserve functionality" was misinterpreted as
"Don't touch the code structure". The correct interpretation is:

- ✅ "Preserve all features (notifications, search, user menu, logout, language
  selector)"
- ✅ "Refactor the architecture to be modern and correct"
- ❌ "Don't change the component structure or state management patterns"

## Current Progress (As of Commit `fafde0689`)

### ✅ Completed Fixes

1.  **Removed `HeaderContainer`**: Eliminated framework state conflict.
2.  **Direct State Control**: Connected `HeaderMenuButton` and `SideNav`
    directly to state.
3.  **State-Aware Toggle Icons**: Hamburger → Lock → X based on mode.
4.  **React Router Navigation**: Replaced `window.location.href` with
    `history.push` (no page reloads).
5.  **Accordion Behavior**: Only one top-level menu section unfurled at a time.
6.  **Click-Outside**: Auto-collapse from SHOW mode when clicking outside.
7.  **Simplified Active Styles**: Removed dark-blue backgrounds.
8.  **Scrollbar Fix**: Attempted via `overflow: visible` on nav.

### ❌ Remaining Issues

1.  **State Fragmentation**: Both `Header.js` and `Layout.js` still
    independently call `useSideNavPreference`, creating two disconnected states.
2.  **No Route Awareness**: Neither component checks current path to determine
    storage vs main context.
3.  **Content Not Pushing**: `Layout.js` doesn't react to `Header.js` state
    changes in real-time.
4.  **Dark Mode Inputs**: `Style.css` and `index.scss` global theme conflicts
    cause black input backgrounds.
5.  **Scrollbar Still Visible**: The `overflow: visible` fix may be insufficient
    or overridden.

## Remediation Execution (Current State)

### Phase 1: State Lifting ✅ COMPLETE

**Goal**: Make `Layout.js` the single source of truth.

**Completed (Commit `8fbd02109`):**

1.  ✅ Added `useLocation` to `Layout.js`.
2.  ✅ Implemented route detection logic:
    ```javascript
    const isStorageContext =
      location.pathname.startsWith("/Storage") ||
      location.pathname.startsWith("/FreezerMonitoring");
    const layoutConfig = isStorageContext
      ? { storageKeyPrefix: "storage", defaultMode: "lock" }
      : { storageKeyPrefix: "main", defaultMode: "close" };
    ```
3.  ✅ Lifted state: `Layout.js` calls `useSideNavPreference(layoutConfig)`
    once.
4.  ✅ Updated `Layout.js` to pass props to `Header`.
5.  ✅ Refactored `Header.js` to controlled component (accepts `mode`,
    `isExpanded`, `toggleSideNav`, `setMode`, `SIDENAV_MODES` as props).
6.  ✅ Removed `useSideNavPreference` import from `Header.js`.
7.  ✅ Updated `Header.test.js` to mock the props that `Layout` provides.
8.  ✅ Enhanced `useSideNavPreference` to react to `storageKeyPrefix` changes.

### Phase 2: Style Debt Removal ✅ COMPLETE

**Goal**: Stop fighting Carbon's styling system.

**Completed (Commit `8fbd02109`):**

1.  ✅ Removed `--cds-text-primary: black` overrides from `.inputText`,
    `.inputSearch`, `.inputSelect`, `.selectSampleType`.
2.  ✅ Removed forced dark-blue backgrounds from `.custom-sidenav-button` (now
    uses `transparent` and Carbon hover tokens).
3.  ✅ Verified `<Theme theme="white">` wraps content in `Layout.js`.
4.  ✅ All 14 unit tests passing (state sync verified).

### Phase 3: Verification ⏳ READY

**Automated Testing:**

1.  ✅ Unit tests passing:
    `npm test -- --testPathPattern="(Header.test|Layout.integration)"` (14/14).
2.  ⏳ E2E test: `npx cypress run --spec "cypress/e2e/sidenavNavigation.cy.js"`
    (requires backend running).

**Manual Browser Testing Required:**

1.  ⏳ Navigate to `/` (Home) → Verify sidenav defaults to CLOSE (collapsed).
2.  ⏳ Navigate to `/Storage` → Verify sidenav defaults to LOCK (expanded,
    content pushed).
3.  ⏳ Toggle states: CLOSE (Hamburger) → SHOW (Lock icon) → LOCK (X icon) →
    CLOSE.
4.  ⏳ Verify no page reloads when clicking nav items.
5.  ⏳ Verify content pushes correctly in LOCK mode (margin-left: 16rem).
6.  ⏳ Verify inputs in content area have light backgrounds, dark text (not
    black-on-black).
7.  ⏳ Verify scrollbar is removed (whole page scrolls, not just nav).
8.  ⏳ Verify only one menu section unfurled at a time (accordion behavior).
9.  ⏳ Verify click-outside in SHOW mode collapses to CLOSE.

## Architecture Decision: The Correct Pattern

**Correct Component Responsibility:**

- **`Layout.js`**: Smart container. Owns navigation state. Determines defaults
  based on route.
- **`Header.js`**: Presentation component. Receives state as props. Renders UI.
  Fires callbacks.

**This is NOT "avoiding touching Header.js"**. This IS properly refactoring it
to follow React best practices (controlled components, single source of truth,
unidirectional data flow).

## Summary of Changes (Commit `8fbd02109`)

### Files Modified:

1.  **`Layout.js`**: Added route detection, lifted state, passes props to
    Header.
2.  **`Header.js`**: Removed internal hook call, accepts props, refactored to
    controlled component.
3.  **`useSideNavPreference.js`**: Added `useEffect` to react to
    `storageKeyPrefix` changes.
4.  **`Style.css`**: Removed dark input text overrides, simplified nav button
    backgrounds.
5.  **`Header.test.js`**: Updated to mock props from Layout.
6.  **`architecture-analysis.md`**: This document.

### Expected Behavior After These Changes:

- ✅ Home page (`/`) defaults to CLOSE (collapsed rail).
- ✅ Storage pages (`/Storage`, `/FreezerMonitoring`) default to LOCK (expanded,
  content pushed).
- ✅ Toggling state updates both Header and Layout in real-time (no more async
  localStorage sync issues).
- ✅ Content pushes immediately when locked (`.content-nav-locked` class applied
  by Layout).
- ✅ Inputs in content area render with light backgrounds (Carbon theme tokens
  work correctly).
- ✅ Nav button backgrounds are transparent/simplified (no dark blue forced).
- ✅ Toggle button shows: Hamburger → Lock → X based on mode.
- ✅ Navigation uses `history.push` (no page reloads).
- ✅ Accordion behavior (only one section unfurled).
- ✅ Click-outside in SHOW mode collapses.

### Next Step:

**Browser Testing** to verify the above behaviors and identify any remaining
edge cases.
