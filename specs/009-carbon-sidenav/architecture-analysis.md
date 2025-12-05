# Architecture Analysis: Sidenav Implementation Issues & Remediation

**Date**: December 5, 2025  
**Status**: Active Remediation - Phase 1 In Progress  
**Branch**: `feat/OGC-009-sidenav/m2b-rollout`

## Problem Diagnosis

We are experiencing regressions and "hacky" implementation patterns because of a fundamental conflict between the legacy codebase architecture and the modern Carbon Design System patterns we are trying to introduce.

### 1. State Fragmentation (The Root Cause)
- **Current State**: Both `Header.js` and `Layout.js` independently initialize the `useSideNavPreference` hook.
- **Consequence**: This creates two disconnected state instances. When `Header` toggles the menu, `Layout` (which controls the content margin) doesn't react immediately. It only syncs on page reload when both read from localStorage again.
- **Symptom**: "Content is not pushed by default" and "Sliding the nav out has no effect on the page."

### 2. Lack of Context Awareness
- **Current State**: Both components hardcode `storageKeyPrefix: "main"`.
- **Consequence**: The application cannot distinguish between "Home" (default closed) and "Storage" (default locked) contexts.
- **Symptom**: "On home page where nav should be collapsed by default it still shows."

### 3. Fighting the Framework (Carbon)
- **Current State**: We removed `HeaderContainer` (good), but we are still manually manipulating CSS margins (`.content-nav-locked`) instead of fully trusting Carbon's layout shell.
- **Consequence**: We have to maintain custom CSS that fights against Carbon's internal styles (like `overflow` handling).
- **Symptom**: "Ugly scrollbar" and "Dark mode inputs" leaking through because of global style conflicts in `index.scss` vs `Theme` wrapper.

### 4. Legacy "Big Ball of Mud" Styles
- **Current State**: `Style.css` contains broad, aggressive selectors like `.inputText { --cds-text-primary: black; }`.
- **Consequence**: These override Carbon's theme tokens, causing dark mode inputs on light backgrounds because `index.scss` sets a global dark theme that these classes inherit from.
- **Symptom**: "Dark mode inputs" (Black text on dark background or white text on white background depending on the specific clash).

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
   - Investigate `index.scss`. If it forces a global dark theme, we must ensure the `Content` area creates a robust "light theme island" (which we did with `<Theme theme="white">`), but we must remove the CSS rules that pierce this island.

### Phase 3: Verification
1. **Automated E2E**: Run the new `sidenavNavigation.cy.js` to prove the state sync works without reloading.
2. **Manual Check**: Verify Storage defaults to locked, Home defaults to closed.

## Why This Happened
We attempted an "Enhancement" strategy (M2b) to avoid rewriting the complex `Header.js`. However, "enhancing" a component by giving it *more* independent state responsibility when it should have had *less* (lifting state up) was an architectural mistake. We treated `Header.js` as a smart container when it should be a dumb presentational component for the Layout's state.

**Critical Misunderstanding**: "Preserve functionality" was misinterpreted as "Don't touch the code structure". The correct interpretation is:
- ✅ "Preserve all features (notifications, search, user menu, logout, language selector)"
- ✅ "Refactor the architecture to be modern and correct"
- ❌ "Don't change the component structure or state management patterns"

## Current Progress (As of Commit `fafde0689`)

### ✅ Completed Fixes
1.  **Removed `HeaderContainer`**: Eliminated framework state conflict.
2.  **Direct State Control**: Connected `HeaderMenuButton` and `SideNav` directly to state.
3.  **State-Aware Toggle Icons**: Hamburger → Lock → X based on mode.
4.  **React Router Navigation**: Replaced `window.location.href` with `history.push` (no page reloads).
5.  **Accordion Behavior**: Only one top-level menu section unfurled at a time.
6.  **Click-Outside**: Auto-collapse from SHOW mode when clicking outside.
7.  **Simplified Active Styles**: Removed dark-blue backgrounds.
8.  **Scrollbar Fix**: Attempted via `overflow: visible` on nav.

### ❌ Remaining Issues
1.  **State Fragmentation**: Both `Header.js` and `Layout.js` still independently call `useSideNavPreference`, creating two disconnected states.
2.  **No Route Awareness**: Neither component checks current path to determine storage vs main context.
3.  **Content Not Pushing**: `Layout.js` doesn't react to `Header.js` state changes in real-time.
4.  **Dark Mode Inputs**: `Style.css` and `index.scss` global theme conflicts cause black input backgrounds.
5.  **Scrollbar Still Visible**: The `overflow: visible` fix may be insufficient or overridden.

## Current Remediation (In Progress)

### Phase 1: State Lifting ✅ STARTED
**Goal**: Make `Layout.js` the single source of truth.

**Completed:**
1.  ✅ Added `useLocation` to `Layout.js`.
2.  ✅ Implemented route detection logic in `Layout.js`:
    ```javascript
    const isStorageContext = location.pathname.startsWith("/Storage") ||
                             location.pathname.startsWith("/FreezerMonitoring");
    const layoutConfig = isStorageContext
      ? { storageKeyPrefix: "storage", defaultMode: "lock" }
      : { storageKeyPrefix: "main", defaultMode: "close" };
    ```
3.  ✅ Lifted state: `Layout.js` calls `useSideNavPreference(layoutConfig)` once.
4.  ✅ Updated `Layout.js` to pass props to `Header`:
    ```jsx
    <Header
      onChangeLanguage={props.onChangeLanguage}
      mode={mode}
      isExpanded={isExpanded}
      toggleSideNav={toggle}
      setMode={setMode}
      SIDENAV_MODES={SIDENAV_MODES}
    />
    ```

**In Progress:**
5.  🔄 Updating `Header.js` to accept props and remove internal hook call.

**Remaining:**
6.  ⏳ Remove `useSideNavPreference` import from `Header.js`.
7.  ⏳ Update all references in `Header.js` from internal state to props.
8.  ⏳ Test state sync between `Layout` and `Header`.

### Phase 2: Style Debt Removal ⏳ PENDING
**Goal**: Stop fighting Carbon's styling system.

**Remaining Tasks:**
1.  ⏳ Remove `.inputText`, `.inputSearch` CSS overrides from `Style.css`.
2.  ⏳ Remove `custom-sidenav-button` styles forcing blue backgrounds.
3.  ⏳ Investigate `index.scss` global theme and ensure `<Theme theme="white">` creates a proper "light island".
4.  ⏳ Test inputs in content area to verify they render light background, dark text.

### Phase 3: Verification ⏳ PENDING
1.  ⏳ Run unit tests: `npm test -- --testPathPattern="(Header.test|Layout.integration)"`.
2.  ⏳ Run E2E test: `npx cypress run --spec "cypress/e2e/sidenavNavigation.cy.js" --browser electron`.
3.  ⏳ Manual browser test: Verify `/Storage` defaults to locked, `/` defaults to closed.
4.  ⏳ Manual browser test: Verify no page reloads on navigation.
5.  ⏳ Manual browser test: Verify content pushes when locked.

## Architecture Decision: The Correct Pattern

**Correct Component Responsibility:**
- **`Layout.js`**: Smart container. Owns navigation state. Determines defaults based on route.
- **`Header.js`**: Presentation component. Receives state as props. Renders UI. Fires callbacks.

**This is NOT "avoiding touching Header.js"**. This IS properly refactoring it to follow React best practices (controlled components, single source of truth, unidirectional data flow).

