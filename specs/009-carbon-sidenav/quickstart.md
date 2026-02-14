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

This feature is broken into 3 milestones per Constitution Principle IX:

| Milestone | Branch                           | Scope                            | Can Start        |
| --------- | -------------------------------- | -------------------------------- | ---------------- |
| M1        | `feat/OGC-009-sidenav/m1-core`   | Core layout, toggle, persistence | Immediately      |
| M2 [P]    | `feat/OGC-009-sidenav/m2-nav`    | Hierarchical nav, auto-expand    | Parallel with M1 |
| M3        | `feat/OGC-009-sidenav/m3-polish` | Icons, responsive, E2E tests     | After M1 + M2    |

**Workflow**:

1. Work on M1 and M2 in parallel (or sequentially)
2. Create PR for each milestone when complete
3. After M1 + M2 merged, start M3
4. Final PR completes the feature

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

# For Milestone 1:
git checkout -b feat/OGC-009-sidenav/m1-core

# For Milestone 2 (can be parallel):
git checkout -b feat/OGC-009-sidenav/m2-nav
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

## Implementation Steps

### Step 1: Create the Custom Hook

Create `frontend/src/components/layout/useSideNavPreference.js`:

```jsx
import { useState, useCallback } from "react";

export function useSideNavPreference({
  defaultExpanded = false,
  storageKeyPrefix = "default",
} = {}) {
  const storageKey = `${storageKeyPrefix}SideNavExpanded`;

  const [isExpanded, setIsExpanded] = useState(() => {
    try {
      const saved = localStorage.getItem(storageKey);
      return saved !== null ? saved === "true" : defaultExpanded;
    } catch (e) {
      console.warn("localStorage unavailable");
      return defaultExpanded;
    }
  });

  const toggle = useCallback(() => {
    setIsExpanded((prev) => {
      const newValue = !prev;
      try {
        localStorage.setItem(storageKey, String(newValue));
      } catch (e) {
        console.warn("Could not persist preference");
      }
      return newValue;
    });
  }, [storageKey]);

  const setExpanded = useCallback(
    (value) => {
      setIsExpanded(value);
      try {
        localStorage.setItem(storageKey, String(value));
      } catch (e) {
        console.warn("Could not persist preference");
      }
    },
    [storageKey]
  );

  return { isExpanded, toggle, setExpanded };
}
```

### Step 2: Create the Layout Component

See `research.md` Appendix A1 for the complete component structure.

Key points:

- Use `isFixedNav={true}` and `isChildOfHeader={true}` on SideNav
- Content wrapper must be a sibling to Header (not nested)
- Apply dynamic margin classes based on `isExpanded` state

### Step 3: Add CSS Styles

See `research.md` Appendix A2 for the complete CSS.

Key points:

- Use 16rem margin for expanded, 3rem for collapsed
- Use Carbon's transition timing (0.11s cubic-bezier)
- Add media query for mobile responsiveness at 1056px

### Step 4: Configure Routes

Update `App.js` to use the new layout for specific routes:

```jsx
// Analyzer pages - expanded by default
<Route path="/analyzers">
  <TwoModeLayout defaultExpanded={true} storageKeyPrefix="analyzer">
    {/* analyzer routes */}
  </TwoModeLayout>
</Route>
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
npm run cy:run -- --spec "cypress/e2e/sidenavNavigation.cy.js"
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

| File                                                     | Purpose                           |
| -------------------------------------------------------- | --------------------------------- |
| `frontend/src/components/layout/TwoModeLayout.js`        | Main layout component             |
| `frontend/src/components/layout/TwoModeLayout.css`       | Layout styles                     |
| `frontend/src/components/layout/useSideNavPreference.js` | Custom hook for state/persistence |
| `frontend/cypress/e2e/sidenavNavigation.cy.js`           | E2E tests                         |

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
