# Quickstart Guide: Carbon Design System Sidenav

**Feature**: 009-carbon-sidenav  
**Feature Branch**: `feat/OGC-009-sidenav`  
**Date**: December 4, 2025

## Overview

This guide helps developers implement the two-mode sidenav feature following
Carbon Design System best practices. The sidenav can be toggled between expanded
(256px) and collapsed (48px) modes, with user preference persisted to
localStorage.

## Milestone-Based Development (Principle IX)

This feature is broken into 4 milestones per Constitution Principle IX:

| Milestone | Branch                             | Scope                               | Can Start   |
| --------- | ---------------------------------- | ----------------------------------- | ----------- |
| M1        | `feat/OGC-009-sidenav/m1-core`     | Core layout, tri-state toggle       | ✅ COMPLETE |
| M2a       | `feat/OGC-009-sidenav/m2a-nav`     | Hierarchical nav, auto-expand       | ✅ COMPLETE |
| M2b       | `feat/OGC-009-sidenav/m2b-rollout` | Global rollout, header preservation | After M2a   |
| M3        | `feat/OGC-009-sidenav/m3-polish`   | Icons, responsive, E2E tests        | After M2b   |

**Workflow**:

1. M1 (Core Layout) - COMPLETE
2. M2a (Navigation) - COMPLETE
3. M2b (Global Rollout + Header Preservation) - IN PROGRESS
4. M3 (Polish & E2E) - After M2b merged

## Prerequisites

- OpenELIS Global 2 development environment set up
- Node.js 16+ installed
- Familiarity with React 17 and Carbon Design System

## Quick Start

### 1. Create Milestone Branch

```bash
cd OpenELIS-Global-2
git fetch origin
git checkout develop
git pull

# For Milestone 2b (Global Rollout - current work):
git checkout feat/OGC-009-sidenav/m2a-nav
git checkout -b feat/OGC-009-sidenav/m2b-rollout

# For Milestone 3 (after M2b merged):
git checkout develop && git pull
git checkout -b feat/OGC-009-sidenav/m3-polish
```

### 2. Install Dependencies (if needed)

```bash
cd frontend
npm install
```

### 3. Start Development Server

```bash
npm start
```

### 4. Access the Application

- React UI: https://localhost/

## Implementation Steps (M2b: Enhance Header.js)

### Step 1: Revert to Working State (DONE)

Ensure Header.js and Layout.js are reverted to develop baseline:

```bash
git checkout develop -- frontend/src/components/layout/Header.js
git checkout develop -- frontend/src/components/layout/Layout.js
```

### Step 2: Import Hooks into Header.js

Add imports to existing `frontend/src/components/layout/Header.js`:

```jsx
// Add these imports
import { useSideNavPreference } from "./useSideNavPreference";
import { useMenuAutoExpand } from "./useMenuAutoExpand";
import { useIntl } from "react-intl"; // Replace injectIntl
import { useLocation } from "react-router-dom"; // Replace withRouter
```

### Step 3: Migrate from HOCs to Hooks

Replace the HOC wrapping pattern:

```jsx
// BEFORE (HOCs)
export default withRouter(injectIntl(OEHeader));

// AFTER (Hooks inside component)
function OEHeader(props) {
  const intl = useIntl();
  const location = useLocation();
  // ... rest of component
}
export default OEHeader;
```

### Step 4: Add Lock Mode Support

Inside Header.js, add lock mode logic:

```jsx
// Inside OEHeader component
const { mode, isExpanded: isLocked, toggle: toggleLock } = useSideNavPreference({
  storageKeyPrefix: "global",
  defaultMode: "close"
});

// Modify SideNav to support lock mode
<SideNav
  expanded={mode === "lock" || isSideNavExpanded}
  isPersistent={mode === "lock"}
/>

// Add lock button to HeaderGlobalBar
<HeaderGlobalAction onClick={toggleLock} aria-label="Lock navigation">
  {mode === "lock" ? <Pin size={20} /> : <PinOutline size={20} />}
</HeaderGlobalAction>
```

### Step 5: Add Auto-Expand with Hook

Replace jsonpath-based expansion with hook:

```jsx
// Replace jsonpath menu expansion
const menusWithAutoExpand = useMenuAutoExpand(menus.menu || []);

// Use in render
{
  menusWithAutoExpand.map((menuItem, index) =>
    generateMenuItems(menuItem, index, 0, `$.menu[${index}]`)
  );
}
```

### Step 6: Add Content Push to Layout.js

Update `frontend/src/components/layout/Layout.js`:

```jsx
import { useSideNavPreference } from "./useSideNavPreference";

function Layout(props) {
  const { mode } = useSideNavPreference({ storageKeyPrefix: "global" });
  const contentClass = mode === "lock" ? "content-nav-locked" : "";

  return (
    // ... providers ...
    <Content className={contentClass}>{children}</Content>
  );
}
```

### Step 7: Add CSS for Lock Mode

Add to `frontend/src/components/Style.css`:

```css
.content-nav-locked {
  margin-left: 16rem;
  transition: margin-left 0.11s cubic-bezier(0.2, 0, 0.38, 0.9);
}
```

## Testing

### Run Unit Tests

```bash
cd frontend
npm test -- --testPathPattern="TwoModeLayout"
```

### Run E2E Tests

```bash
# Individual test file (recommended during development)
npm run pw:test sidenav.spec.ts
```

### Manual Testing Checklist

- [ ] Toggle sidenav between expanded/collapsed modes
- [ ] Verify content pushes (not overlays) when expanded
- [ ] Navigate to a different page - preference persists
- [ ] Refresh browser - preference persists
- [ ] Clear localStorage - falls back to page default
- [ ] Test on viewport < 1056px - sidenav overlays content
- [ ] Verify menu auto-expands to show current page

## Key Files

| File                                                        | Purpose                          |
| ----------------------------------------------------------- | -------------------------------- |
| `frontend/src/components/layout/Header.js`                  | Enhanced with lock mode + hooks  |
| `frontend/src/components/layout/Layout.js`                  | Content margin for lock mode     |
| `frontend/src/components/layout/useSideNavPreference.js`    | Tri-state hook (show/lock/close) |
| `frontend/src/components/layout/useMenuAutoExpand.js`       | Route-based menu auto-expansion  |
| `frontend/src/components/layout/Layout.integration.test.js` | Critical smoke tests             |
| `frontend/src/components/Style.css`                         | Lock mode content margin CSS     |
| `frontend/playwright/tests/sidenav.spec.ts`                 | E2E tests                        |

## Reference Documentation

- [Specification](spec.md) - Feature requirements and user stories
- [Implementation Plan](plan.md) - Technical approach and testing strategy
- [Research](research.md) - Design decisions with POC code examples
- [Data Model](data-model.md) - Component state and props interfaces
- [Component Contract](contracts/layout-props.md) - API documentation

## POC Reference

The validated proof-of-concept is available in the `analyzer-layout-poc` branch:

```bash
git checkout analyzer-layout-poc
# See frontend/src/components/layout/AnalyzerLayout.js
```

## Troubleshooting

### Content not pushing when sidenav expands

Ensure:

1. `isFixedNav={true}` is set on SideNav
2. Content wrapper is a sibling to Header, not nested
3. CSS classes are applied correctly

### localStorage preference not persisting

Check:

1. Browser is not in private/incognito mode
2. No JavaScript errors in console
3. `storageKeyPrefix` is consistent

### Menu items not auto-expanding

Verify:

1. `useEffect` is triggered on `location.pathname` change
2. `markActiveExpanded` function is called after menu data loads
3. Route URLs match menu item `actionURL` values

## Support

- Check [spec.md](spec.md) for detailed requirements
- Check [research.md](research.md) for design rationale
- Post questions in GitHub Discussions
