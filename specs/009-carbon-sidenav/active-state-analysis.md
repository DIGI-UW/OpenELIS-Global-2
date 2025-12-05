# SideNav Active State Analysis

## Problem Statement

The active state styling for navigation items is:
1. **Flickering** - Shows briefly then disappears
2. **Inconsistent** - Works on some items (Cold Storage Monitoring) but not others (Storage Management)
3. **Missing hover** - Subnav items don't show hover states

## Current Architecture (Problematic)

### Data Flow
```
┌─────────────────────────────────────────────────────────────────────┐
│  Menu Data from API (/rest/menu)                                    │
│  { menu: [...], childMenus: [...], expanded: false }                │
└─────────────────────┬───────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  useMenuAutoExpand Hook                                              │
│  - Marks items as expanded based on location.pathname               │
│  - Returns NEW array reference on every location change             │
└─────────────────────┬───────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  generateMenuItems() - Recursive renderer                           │
│  - Renders different components based on level and childMenus       │
│  - Sets isActive prop on SideNavMenuItem                            │
│  - ALSO renders custom buttons inside SideNavMenuItem               │
└─────────────────────┬───────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  CSS Styles (Style.css)                                              │
│  - .cds--side-nav__link--current (Carbon's active class)            │
│  - .custom-sidenav-button.active (custom button active)             │
└─────────────────────────────────────────────────────────────────────┘
```

## Root Causes Identified

### Issue 1: Duplicate Active State Management

For **level > 0** items with `hasActiveChildMenu(menuItem)`, the code renders:

```jsx
<SideNavMenuItem
  isActive={!!menuItem.menu.actionURL && location.pathname === menuItem.menu.actionURL}
>
  <span>
    {renderDualNavDropdownButton(...)}  // <-- ALSO has its own active logic!
  </span>
</SideNavMenuItem>
```

The `renderDualNavDropdownButton` function:
```jsx
const isActive = menuItem.menu.actionURL && location.pathname === menuItem.menu.actionURL;
return (
  <button className={`custom-sidenav-button ${isActive ? "active" : ""}`}>
    ...
  </button>
);
```

**PROBLEM**: Two competing active states:
1. `SideNavMenuItem isActive` → adds `cds--side-nav__link--current` class
2. `custom-sidenav-button.active` class on the inner button

### Issue 2: CSS Selector Conflicts

```css
/* Carbon's active state - applied via isActive prop */
.cds--side-nav__menu-item .cds--side-nav__link--current {
  border-left: 4px solid var(--cds-link-primary);
  background-color: var(--cds-layer-selected-01) !important;
}

/* Custom button active state - applied via className */
.custom-sidenav-button.active {
  border-left: 4px solid var(--cds-link-primary);
  background-color: var(--cds-layer-selected-01);
}

/* BUT the parent overrides all backgrounds! */
.cds--side-nav__menu-item .cds--side-nav__link {
  background-color: transparent !important;
}
```

**PROBLEM**: The `!important` on transparent background overrides the active state!

### Issue 3: Flickering Cause

```jsx
// In generateMenuItems, level > 0 branch:
<SideNavMenuItem
  href={menuItem.menu.actionURL}  // <-- Carbon adds click handler
  isActive={...}
>
  <span onClick={e => { e.stopPropagation(); }}>
    <button onClick={() => history.push(...)}>  // <-- Custom handler
```

**PROBLEM**: When clicking:
1. Custom button fires `history.push()` → URL changes
2. Carbon's `href` fires → causes re-render/flicker
3. `isActive` recalculates → briefly true, then state updates

### Issue 4: Hover Not Working on Subnavs

```css
/* This targets Carbon's link element */
.cds--side-nav__menu-item .cds--side-nav__link:hover {
  background-color: var(--cds-layer-hover-01) !important;
}

/* BUT the actual hover target is the custom button inside! */
.custom-sidenav-button:hover {
  background-color: var(--cds-layer-hover-01);  /* No !important, gets overridden */
}
```

**PROBLEM**: Custom button hover is being overridden by parent styles.

## State Diagram

```
                    MENU ITEM STATES
                    
┌─────────────────────────────────────────────────────────┐
│                                                          │
│   ┌─────────────┐    click      ┌─────────────┐         │
│   │   NORMAL    │──────────────▶│   ACTIVE    │         │
│   │             │               │             │         │
│   │ bg: trans   │◀──────────────│ bg: selected│         │
│   │ border: none│   nav away    │ border: left│         │
│   └──────┬──────┘               └─────────────┘         │
│          │                                               │
│          │ hover                                         │
│          ▼                                               │
│   ┌─────────────┐                                        │
│   │   HOVER     │                                        │
│   │             │                                        │
│   │ bg: hover   │                                        │
│   │ border: none│                                        │
│   └─────────────┘                                        │
│                                                          │
└─────────────────────────────────────────────────────────┘

CURRENT PROBLEM:

┌──────────────────────────────────────────────────────────┐
│                                                           │
│   USER CLICKS NAV ITEM                                    │
│          │                                                │
│          ▼                                                │
│   ┌─────────────────────┐                                │
│   │ 1. custom-sidenav   │                                │
│   │    button onClick   │                                │
│   │    history.push()   │                                │
│   └──────────┬──────────┘                                │
│              │                                            │
│              ▼                                            │
│   ┌─────────────────────┐                                │
│   │ 2. URL changes      │                                │
│   │    location updates │                                │
│   └──────────┬──────────┘                                │
│              │                                            │
│              ▼                                            │
│   ┌─────────────────────┐                                │
│   │ 3. useMenuAutoExpand│                                │
│   │    runs, new array  │                                │
│   └──────────┬──────────┘                                │
│              │                                            │
│              ▼                                            │
│   ┌─────────────────────┐                                │
│   │ 4. Header re-renders│                                │
│   │    isActive=true    │                                │
│   └──────────┬──────────┘                                │
│              │                                            │
│              ▼                                            │
│   ┌─────────────────────┐                                │
│   │ 5. Carbon SideNav   │   FLICKER: active shows        │
│   │    href also fires? │   then CSS transparent         │
│   │    or CSS conflict  │   !important overrides         │
│   └──────────┬──────────┘                                │
│              │                                            │
│              ▼                                            │
│   ┌─────────────────────┐                                │
│   │ 6. Final render     │   RESULT: no visible           │
│   │    active state     │   active state                 │
│   │    lost to CSS      │                                │
│   └─────────────────────┘                                │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

## Solution: Simplify to Single Source of Truth

### Option A: Use ONLY Carbon's `isActive` (Recommended)

1. Remove custom buttons for simple nav items
2. Let Carbon's `SideNavMenuItem` handle active state
3. Only use custom buttons where truly needed (dropdown toggles)

### Option B: Use ONLY Custom Button Active State

1. Remove `isActive` prop from `SideNavMenuItem`
2. Style only via `.custom-sidenav-button.active`
3. Fix CSS specificity issues

### CSS Fix Required (Either Option)

```css
/* REMOVE this - it kills all active states */
.cds--side-nav__menu-item .cds--side-nav__link {
  background-color: transparent !important;  /* DELETE */
}

/* KEEP these */
.cds--side-nav__menu-item .cds--side-nav__link--current,
.cds--side-nav__menu-item .cds--side-nav__link[aria-current="page"] {
  border-left: 4px solid var(--cds-link-primary);
  background-color: var(--cds-layer-selected-01) !important;
}

/* ADD specificity for custom buttons */
.cds--side-nav__menu-item .custom-sidenav-button.active {
  border-left: 4px solid var(--cds-link-primary) !important;
  background-color: var(--cds-layer-selected-01) !important;
}

.cds--side-nav__menu-item .custom-sidenav-button:hover {
  background-color: var(--cds-layer-hover-01) !important;
}
```

## Recommended Fix Steps

1. **CSS Fix** - Remove the `transparent !important` override
2. **CSS Fix** - Add proper specificity for custom button states
3. **JS Fix** - Remove `href` from SideNavMenuItem when using custom buttons (prevents double navigation)
4. **JS Fix** - Consider simplifying to use Carbon's built-in navigation where possible

