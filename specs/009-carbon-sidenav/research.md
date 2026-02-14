# Research: Carbon Design System Sidenav

**Feature**: 009-carbon-sidenav  
**Date**: December 4, 2025  
**Status**: Complete

## Research Topics

### R1: Carbon UI Shell SideNav Patterns

**Question**: What is the correct Carbon pattern for a two-mode sidenav that
pushes content?

**Findings**:

Carbon Design System provides two main SideNav patterns:

1. **Overlay Pattern** (`isPersistent={false}`)

   - SideNav overlays content when expanded
   - Content does not shift
   - Best for mobile/tablet or when sidebar is rarely used
   - Current `Header.js` implementation uses this

2. **Fixed/Persistent Pattern** (`isFixedNav={true}` + `isChildOfHeader={true}`)
   - SideNav pushes content when expanded
   - Content area shrinks to accommodate sidenav
   - Best for desktop when sidebar is primary navigation
   - POC `AnalyzerLayout.js` uses this pattern

**Decision**: Use Fixed/Persistent pattern with `isFixedNav={true}` and
`isChildOfHeader={true}`.

**Rationale**: Desktop users in laboratory settings need persistent navigation.
The POC validated this pattern works correctly with content pushing.

**Alternatives Considered**:

- Overlay pattern - Rejected because it obscures content and requires explicit
  close action
- `isPersistent={true}` without `isFixedNav` - Rejected because it doesn't push
  content properly

**References**:

- [Carbon UI Shell Left Panel Usage](https://carbondesignsystem.com/components/UI-shell-left-panel/usage/)
- [Carbon UI Shell Left Panel Code](https://carbondesignsystem.com/components/UI-shell-left-panel/code/)

---

### R2: HeaderContainer vs Direct State Control

**Question**: Should we use Carbon's `HeaderContainer` render prop or control
sidenav state directly?

**Findings**:

`HeaderContainer` provides a render prop pattern:

```jsx
<HeaderContainer
  render={({ isSideNavExpanded, onClickSideNavExpand }) => (
    <Header>
      <SideNav expanded={isSideNavExpanded} ... />
    </Header>
  )}
/>
```

**Limitations**:

- State is managed internally by Carbon
- Cannot persist state to localStorage
- Cannot programmatically control expand/collapse
- Cannot set different defaults per page

**Direct State Control** bypasses `HeaderContainer`:

```jsx
const [isSideNavExpanded, setIsSideNavExpanded] = useState(() => {
  const saved = localStorage.getItem("sideNavExpanded");
  return saved !== null ? saved === "true" : defaultExpanded;
});

<Header>
  <HeaderMenuButton onClick={() => setIsSideNavExpanded(!isSideNavExpanded)} />
  <SideNav expanded={isSideNavExpanded} isFixedNav={true} />
</Header>;
```

**Decision**: Use direct state control, bypassing `HeaderContainer`.

**Rationale**:

- Enables localStorage persistence
- Enables page-level default configuration
- Provides full control over expand/collapse behavior
- POC validated this approach works correctly

**Alternatives Considered**:

- `HeaderContainer` with external state sync - Rejected because it's complex and
  fights the component design
- Context-based state management (Redux/Zustand) - Rejected because localStorage
  is sufficient for this use case

---

### R3: Content Layout Structure

**Question**: How should Content be positioned relative to Header/SideNav?

**Findings**:

**Current Header.js Structure** (incorrect for fixed nav):

```jsx
<HeaderContainer
  render={() => (
    <Header>
      <SideNav isPersistent={false} ... />
    </Header>
  )}
/>
<Content>{children}</Content>  // Sibling but doesn't push properly
```

**POC AnalyzerLayout.js Structure** (correct):

```jsx
<Header>
  <SideNav isFixedNav={true} isChildOfHeader={true} />
</Header>
<div className={isSideNavExpanded ? 'content-expanded' : 'content-collapsed'}>
  <Content>{children}</Content>
</div>
```

**Key CSS for content pushing**:

```css
.content-expanded {
  margin-left: 16rem; /* 256px - SideNav expanded width */
  width: calc(100% - 16rem);
  transition: margin-left 0.11s cubic-bezier(0.2, 0, 1, 0.9);
}

.content-collapsed {
  margin-left: 3rem; /* 48px - SideNav rail width */
  width: calc(100% - 3rem);
}
```

**Decision**: Content wrapper as sibling to Header, with dynamic classes for
margin adjustment.

**Rationale**: Carbon's `isFixedNav` requires content to be a sibling, not
nested within `HeaderContainer` render prop.

---

### R4: localStorage Persistence Strategy

**Question**: How should user preference be persisted and restored?

**Findings**:

**Key Structure**:

- Key: `{layoutName}SideNavExpanded` (e.g., `analyzerSideNavExpanded`)
- Value: `"true"` or `"false"` (string)

**Initialization Pattern**:

```jsx
const [isSideNavExpanded, setIsSideNavExpanded] = useState(() => {
  try {
    const saved = localStorage.getItem("sideNavExpanded");
    if (saved !== null) {
      return saved === "true";
    }
    return defaultExpanded; // Page-level default prop
  } catch (e) {
    console.warn("localStorage unavailable, using default");
    return defaultExpanded;
  }
});
```

**Update Pattern**:

```jsx
const handleSideNavToggle = () => {
  const newExpanded = !isSideNavExpanded;
  setIsSideNavExpanded(newExpanded);
  try {
    localStorage.setItem("sideNavExpanded", String(newExpanded));
  } catch (e) {
    console.warn("Could not persist sidenav preference");
  }
};
```

**Decision**: Use layout-specific localStorage keys with graceful fallback.

**Rationale**:

- Layout-specific keys allow different defaults per section
- Graceful fallback handles private browsing mode
- String values (not JSON) for simplicity

---

### R5: Menu Auto-Expansion Algorithm

**Question**: How should we auto-expand menu items to show the active page?

**Findings**:

**Algorithm** (from POC):

```javascript
const markActiveExpanded = (items) => {
  let isActiveBranch = false;
  items.forEach((item) => {
    // Recursively check children first
    if (item.childMenus && item.childMenus.length > 0) {
      if (markActiveExpanded(item.childMenus)) {
        item.expanded = true;
        isActiveBranch = true;
      }
    }
    // Check if this item matches current route
    if (
      item.menu.actionURL === location.pathname ||
      location.pathname.startsWith(item.menu.actionURL + "/")
    ) {
      isActiveBranch = true;
    }
  });
  return isActiveBranch;
};
```

**Trigger**: Run on route change via `useEffect` with `location.pathname`
dependency.

**Decision**: Use recursive depth-first marking algorithm triggered on route
change.

**Rationale**:

- Depth-first ensures children are processed before parents
- Parent expansion happens automatically when any child is active
- Path prefix matching handles nested routes (e.g., `/analyzers/qc/alerts`
  matches `/analyzers/qc`)

---

### R6: Responsive Behavior

**Question**: How should the sidenav behave on smaller screens?

**Findings**:

**Carbon Breakpoints**:

- `lg` (1056px): Above this, fixed nav pushes content
- Below `lg`: Sidenav should overlay content

**CSS Media Query** (from POC):

```css
@media (max-width: 1056px) {
  .content-expanded {
    margin-left: 0; /* Don't push content on mobile */
  }
  .content-collapsed {
    margin-left: 0;
  }
}
```

**Decision**: Use CSS media queries to switch from push to overlay below 1056px.

**Rationale**:

- On smaller screens, pushing content would make the main area too narrow
- Overlay behavior allows full-width content when sidenav is collapsed
- Aligns with Carbon's responsive design patterns

---

## Summary of Decisions

| Topic             | Decision                     | Key Configuration                             |
| ----------------- | ---------------------------- | --------------------------------------------- |
| SideNav Pattern   | Fixed/Persistent             | `isFixedNav={true}`, `isChildOfHeader={true}` |
| State Control     | Direct React state           | `useState` with localStorage init             |
| Content Structure | Wrapper sibling to Header    | Dynamic margin classes                        |
| Persistence       | Layout-specific localStorage | `{layoutName}SideNavExpanded`                 |
| Auto-Expansion    | Recursive depth-first        | Triggered on route change                     |
| Responsive        | CSS media queries            | Push >1056px, overlay ≤1056px                 |

## Implementation References

- **POC Branch**: `analyzer-layout-poc` (also backed up at
  `backup-009-carbon-sidenav-with-poc`)
- **POC Source**: `frontend/src/components/layout/AnalyzerLayout.js`
- **POC Styles**: `frontend/src/components/layout/AnalyzerLayout.css`
- **Current Header**: `frontend/src/components/layout/Header.js`
- **Carbon Docs**:
  [UI Shell Left Panel](https://carbondesignsystem.com/components/UI-shell-left-panel/usage/)

---

## Appendix: POC Code Reference

The following code snippets are from the validated POC in the
`analyzer-layout-poc` branch. These serve as reference implementation patterns.

### A1: Complete Layout Component Structure

```jsx
/**
 * TwoModeLayout Component (based on AnalyzerLayout POC)
 *
 * Key architectural decisions:
 * - Direct sidenav state control (bypasses HeaderContainer)
 * - SideNav uses isFixedNav for proper content pushing
 * - State persisted to localStorage
 * - Content as sibling to Header/SideNav
 */

import {
  Header,
  HeaderGlobalAction,
  HeaderGlobalBar,
  HeaderMenuButton,
  HeaderName,
  HeaderPanel,
  Content,
  SideNav,
  SideNavItems,
  SideNavMenu,
  SideNavMenuItem,
  Theme,
} from "@carbon/react";
import React, { useContext, useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { useIntl } from "react-intl";

const TwoModeLayout = ({
  children,
  onChangeLanguage,
  defaultExpanded = false,
  storageKeyPrefix = "default",
}) => {
  const location = useLocation();
  const intl = useIntl();

  // Layout State - Controlled directly (bypassing HeaderContainer)
  const [isSideNavExpanded, setIsSideNavExpanded] = useState(() => {
    const key = `${storageKeyPrefix}SideNavExpanded`;
    try {
      const saved = localStorage.getItem(key);
      return saved !== null ? saved === "true" : defaultExpanded;
    } catch (e) {
      console.warn("localStorage unavailable, using default");
      return defaultExpanded;
    }
  });

  const [menus, setMenus] = useState({ menu: [] });

  // SideNav Toggle Handler with persistence
  const handleSideNavToggle = () => {
    const newExpanded = !isSideNavExpanded;
    setIsSideNavExpanded(newExpanded);
    const key = `${storageKeyPrefix}SideNavExpanded`;
    try {
      localStorage.setItem(key, String(newExpanded));
    } catch (e) {
      console.warn("Could not persist sidenav preference");
    }
  };

  // Auto-expand menu on route change
  useEffect(() => {
    setMenus((prevMenus) => {
      const newMenus = JSON.parse(JSON.stringify(prevMenus));
      const markActiveExpanded = (items) => {
        let isActiveBranch = false;
        items.forEach((item) => {
          if (item.childMenus && item.childMenus.length > 0) {
            if (markActiveExpanded(item.childMenus)) {
              item.expanded = true;
              isActiveBranch = true;
            }
          }
          if (
            item.menu.actionURL === location.pathname ||
            location.pathname.startsWith(item.menu.actionURL + "/")
          ) {
            isActiveBranch = true;
          }
        });
        return isActiveBranch;
      };
      if (newMenus.menu) {
        markActiveExpanded(newMenus.menu);
      }
      return newMenus;
    });
  }, [location.pathname]);

  return (
    <div className="container">
      <Theme>
        <div style={{ display: "flex", flexDirection: "column" }}>
          {/* Header with direct state control */}
          <Header id="mainHeader" aria-label="">
            <HeaderMenuButton
              aria-label={isSideNavExpanded ? "Close menu" : "Open menu"}
              onClick={handleSideNavToggle}
              isActive={isSideNavExpanded}
              isCollapsible={true}
            />
            <HeaderName href="/" prefix="">
              {/* Logo and banner */}
            </HeaderName>
            <HeaderGlobalBar>{/* Global actions */}</HeaderGlobalBar>

            {/* SideNav with isFixedNav for content pushing */}
            <SideNav
              aria-label="Side navigation"
              expanded={isSideNavExpanded}
              isFixedNav={true}
              isChildOfHeader={true}
            >
              <SideNavItems>
                {menus.menu.map((item, index) => (
                  /* Menu items rendered here */
                  <SideNavMenuItem key={index} href={item.menu.actionURL}>
                    {intl.formatMessage({ id: item.menu.displayKey })}
                  </SideNavMenuItem>
                ))}
              </SideNavItems>
            </SideNav>
          </Header>

          {/* Content Sibling - Properly Pushed by Fixed SideNav */}
          <div
            className={
              isSideNavExpanded ? "content-expanded" : "content-collapsed"
            }
            style={{
              marginLeft: isSideNavExpanded ? "16rem" : "3rem",
              width: isSideNavExpanded
                ? "calc(100% - 16rem)"
                : "calc(100% - 3rem)",
              transition:
                "margin-left 0.11s cubic-bezier(0.2, 0, 1, 0.9), width 0.11s cubic-bezier(0.2, 0, 1, 0.9)",
            }}
          >
            <Theme theme="white">
              <Content style={{ marginLeft: 0 }}>{children}</Content>
            </Theme>
          </div>
        </div>
      </Theme>
    </div>
  );
};

export default TwoModeLayout;
```

### A2: CSS Styles for Content Pushing

```css
/**
 * TwoModeLayout Styles (based on AnalyzerLayout.css POC)
 */

/* Content area */
.content-expanded {
  margin-left: 16rem !important; /* Width of expanded SideNav (256px) */
  width: calc(100% - 16rem) !important;
  transition: margin-left 0.11s cubic-bezier(0.2, 0, 1, 0.9), width 0.11s
      cubic-bezier(0.2, 0, 1, 0.9);
}

.content-collapsed {
  margin-left: 3rem !important; /* Width of collapsed SideNav rail (48px) */
  width: calc(100% - 3rem) !important;
  transition: margin-left 0.11s cubic-bezier(0.2, 0, 1, 0.9), width 0.11s
      cubic-bezier(0.2, 0, 1, 0.9);
}

/* Override Carbon Content's default margin when inside our wrapper */
.content-expanded .cds--content,
.content-collapsed .cds--content {
  margin-left: 0 !important;
  width: 100% !important;
}

/* Responsive adjustments - below Carbon lg breakpoint */
@media (max-width: 1056px) {
  .content-expanded {
    margin-left: 0 !important;
    width: 100% !important;
  }

  .content-collapsed {
    margin-left: 0 !important;
    width: 100% !important;
  }
}
```

### A3: Route Configuration Example

```jsx
// In App.js - using TwoModeLayout for specific routes
import TwoModeLayout from "./components/layout/TwoModeLayout";

// Analyzer routes - use expanded sidenav by default
<Route path="/analyzers">
  <TwoModeLayout
    defaultExpanded={true}
    storageKeyPrefix="analyzer"
    onChangeLanguage={onChangeLanguage}
  >
    <Switch>
      <SecureRoute path="/analyzers" exact component={AnalyzersPage} />
      <SecureRoute path="/analyzers/errors" component={ErrorDashboardPage} />
      <SecureRoute path="/analyzers/qc" component={QCDashboardPage} />
    </Switch>
  </TwoModeLayout>
</Route>

// Default routes - use collapsed sidenav by default
<Route path="/">
  <TwoModeLayout
    defaultExpanded={false}
    storageKeyPrefix="default"
    onChangeLanguage={onChangeLanguage}
  >
    <Switch>
      <SecureRoute path="/" exact component={Home} />
      {/* ... other routes ... */}
    </Switch>
  </TwoModeLayout>
</Route>
```

### A4: Menu Item Rendering with Hierarchical Support

```jsx
/**
 * Recursive menu item generator supporting:
 * - Parent items with children (expandable)
 * - Leaf items (navigable)
 * - Dual-action items (both navigable AND have children)
 */
const generateMenuItems = (menuItem, index, level, path) => {
  if (!menuItem.menu.isActive) {
    return null;
  }

  // Top-level item with children - render as expandable menu
  if (level === 0 && menuItem.childMenus.length > 0) {
    return (
      <SideNavMenu
        key={path}
        aria-label={intl.formatMessage({ id: menuItem.menu.displayKey })}
        title={intl.formatMessage({ id: menuItem.menu.displayKey })}
        defaultExpanded={menuItem.expanded}
      >
        {menuItem.childMenus.map((child, idx) =>
          generateMenuItems(child, idx, level + 1, `${path}.childMenus[${idx}]`)
        )}
      </SideNavMenu>
    );
  }

  // Top-level item without children - render as direct link
  if (level === 0) {
    return (
      <SideNavMenuItem
        key={path}
        href={menuItem.menu.actionURL}
        target={menuItem.menu.openInNewWindow ? "_blank" : ""}
        rel={menuItem.menu.openInNewWindow ? "noopener noreferrer" : undefined}
      >
        {intl.formatMessage({ id: menuItem.menu.displayKey })}
      </SideNavMenuItem>
    );
  }

  // Nested item - render with indentation
  return (
    <SideNavMenuItem
      key={path}
      href={menuItem.menu.actionURL}
      style={{ paddingLeft: `${level * 0.5 + 1}rem` }}
    >
      {intl.formatMessage({ id: menuItem.menu.displayKey })}
    </SideNavMenuItem>
  );
};
```

---

**Note**: The complete POC implementation can be found in the
`analyzer-layout-poc` branch. The code above is extracted for reference during
implementation of this feature on a clean branch.
