# Data Model: Carbon Design System Sidenav

**Feature**: 009-carbon-sidenav  
**Date**: December 4, 2025

## Overview

This feature is frontend-only and does not require database changes. The data
model describes React component state, props interfaces, and the structure of
data consumed from existing APIs.

## Component State Model

### TwoModeLayout State

```typescript
// Tri-state sidenav mode
type SideNavMode = "show" | "lock" | "close";

interface TwoModeLayoutState {
  // Sidenav mode state (tri-state)
  mode: SideNavMode;
  // Derived: isExpanded = mode !== 'close'
  isExpanded: boolean;

  // Menu data (from API)
  menus: MenuState;
  loadingMenus: boolean;
}

interface MenuState {
  menu: MenuItem[]; // Main navigation menu
  menu_billing: MenuItem[]; // Billing submenu (optional)
  menu_nonconformity: MenuItem[]; // Non-conformity submenu (optional)
}

// Note: Panel states (notifications, user, search) are managed in the Header component
```

### MenuItem Structure (from /rest/menu API)

```typescript
interface MenuItem {
  menu: MenuMetadata;
  childMenus: MenuItem[];
  expanded?: boolean; // Client-side state for submenu expansion
}

interface MenuMetadata {
  elementId: string; // DOM element ID
  displayKey: string; // i18n key for label
  actionURL: string | null; // Navigation URL (null for parent-only items)
  isActive: boolean; // Whether item is visible/enabled
  openInNewWindow: boolean; // Open in new tab
}
```

### Notification Structure (from /rest/notifications API)

```typescript
interface Notification {
  id: string;
  title: string;
  message: string;
  createdAt: string;
  readAt: string | null; // null = unread
}
```

## Props Interfaces

### TwoModeLayout Props

```typescript
type SideNavMode = "show" | "lock" | "close";

interface TwoModeLayoutProps {
  /**
   * Child content to render in the main content area
   */
  children: React.ReactNode;

  /**
   * Header actions (user menu, notifications, search, language, help)
   * Rendered in HeaderGlobalBar position
   * @see HeaderActions component
   */
  headerActions?: React.ReactNode;

  /**
   * Default sidenav mode for this layout (before user preference is loaded)
   * - 'show': Sidenav expanded, overlays content
   * - 'lock': Sidenav expanded, pushes content (margin-left: 16rem)
   * - 'close': Sidenav collapsed to rail (48px)
   * @default 'close'
   */
  defaultMode?: SideNavMode;

  /**
   * @deprecated Use defaultMode instead
   * Maps to: true → 'lock', false → 'close'
   */
  defaultExpanded?: boolean;

  /**
   * Unique identifier for localStorage key
   * Full key will be: `${storageKeyPrefix}SideNavMode`
   * @default 'default'
   */
  storageKeyPrefix?: string;

  /**
   * Optional menu data (for testing). If provided, bypasses API fetch.
   */
  menus?: MenuState;
}
```

### useSideNavPreference Hook

```typescript
type SideNavMode = "show" | "lock" | "close";

interface UseSideNavPreferenceOptions {
  /**
   * Default mode when no preference is stored
   * @default 'close'
   */
  defaultMode?: SideNavMode;

  /**
   * @deprecated Use defaultMode instead
   */
  defaultExpanded?: boolean;

  /**
   * Prefix for localStorage key
   * @default 'default'
   */
  storageKeyPrefix?: string;
}

interface UseSideNavPreferenceReturn {
  /**
   * Current sidenav mode (tri-state)
   */
  mode: SideNavMode;

  /**
   * Derived: true if mode !== 'close'
   */
  isExpanded: boolean;

  /**
   * Cycle through modes: close → show → lock → close
   */
  toggle: () => void;

  /**
   * Programmatically set mode (also persists to localStorage)
   */
  setMode: (mode: SideNavMode) => void;
}

// Usage
const { mode, isExpanded, toggle, setMode } = useSideNavPreference({
  defaultMode: "lock",
  storageKeyPrefix: "storage",
});
```

## localStorage Schema

### Key Format

```
{storageKeyPrefix}SideNavMode
```

### Examples

| Context        | Key                  | Value                             |
| -------------- | -------------------- | --------------------------------- |
| Storage pages  | `storageSideNavMode` | `"show"` or `"lock"` or `"close"` |
| Default layout | `defaultSideNavMode` | `"show"` or `"lock"` or `"close"` |
| Admin pages    | `adminSideNavMode`   | `"show"` or `"lock"` or `"close"` |

### Value Type

- Stored as string: `"show"` | `"lock"` | `"close"`
- Parsed on read: direct string comparison
- Invalid values fall back to defaultMode

## CSS Class Model

### Layout Container Classes

```css
/* Applied to content wrapper div based on mode */

/* LOCK mode: sidenav expanded, content pushed right */
.content-locked {
  margin-left: 16rem; /* 256px - Carbon SideNav expanded width */
  width: calc(100% - 16rem);
  transition: margin-left 0.11s cubic-bezier(0.2, 0, 1, 0.9), width 0.11s
      cubic-bezier(0.2, 0, 1, 0.9);
}

/* SHOW mode: sidenav expanded, overlays content (no push) */
.content-overlay {
  margin-left: 0;
  width: 100%;
  transition: margin-left 0.11s cubic-bezier(0.2, 0, 1, 0.9), width 0.11s
      cubic-bezier(0.2, 0, 1, 0.9);
}

/* CLOSE mode: sidenav collapsed to rail */
.content-rail {
  margin-left: 3rem; /* 48px - Carbon SideNav rail width */
  width: calc(100% - 3rem);
  transition: margin-left 0.11s cubic-bezier(0.2, 0, 1, 0.9), width 0.11s
      cubic-bezier(0.2, 0, 1, 0.9);
}

/* Active menu item styling (left border indicator) */
.cds--side-nav__link[aria-current="page"] {
  border-left: 4px solid var(--cds-link-primary);
  background-color: var(--cds-layer-selected-01);
}

/* Responsive override - below Carbon lg breakpoint */
@media (max-width: 1056px) {
  .content-locked,
  .content-overlay,
  .content-rail {
    margin-left: 0;
    width: 100%;
  }
}
```

## State Transitions

### Sidenav Toggle (Tri-State Cycle)

```
User clicks HeaderMenuButton
    ↓
toggle() - cycles through modes: close → show → lock → close
    ↓
setMode(nextMode)
    ↓
localStorage.setItem(key, nextMode)
    ↓
Re-render with new margin class (content-rail / content-overlay / content-locked)
```

### Click Outside (SHOW mode only)

```
User clicks outside sidenav (while in SHOW mode)
    ↓
handleClickOutside event listener
    ↓
setMode('close')
    ↓
localStorage.setItem(key, 'close')
    ↓
Re-render with content-rail class
```

### Route Change (Auto-Expand)

```
Router navigates to new path
    ↓
useEffect detects location.pathname change
    ↓
markActiveExpanded(menus) recursive call
    ↓
Parent items in path get expanded=true
    ↓
setMenus(newMenus)
    ↓
Re-render with expanded submenus
```

### Page Load (Preference Restore)

```
Component mounts
    ↓
useState initializer runs
    ↓
localStorage.getItem(key)
    ↓
If found: parse and use stored value (must be 'show'|'lock'|'close')
If not found or invalid: use defaultMode prop (default: 'close')
    ↓
Initial render with correct mode
```

## No Database Changes

This feature does not require any database schema changes. All data is:

- **Consumed from existing APIs**: `/rest/menu`, `/rest/notifications`
- **Stored client-side**: localStorage for user preference

## Related APIs (No Changes Required)

| API                                  | Method | Purpose                | Data Structure   |
| ------------------------------------ | ------ | ---------------------- | ---------------- |
| `/rest/menu`                         | GET    | Fetch navigation menu  | `MenuItem[]`     |
| `/rest/notifications`                | GET    | Fetch notifications    | `Notification[]` |
| `/rest/notification/markasread/{id}` | PUT    | Mark notification read | N/A              |
| `/rest/notification/markasread/all`  | PUT    | Mark all read          | N/A              |
