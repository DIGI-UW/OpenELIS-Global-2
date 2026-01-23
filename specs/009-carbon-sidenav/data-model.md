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
interface TwoModeLayoutState {
  // Sidenav expansion state
  isSideNavExpanded: boolean;

  // Panel states (for header actions)
  switchCollapsed: boolean; // User panel expanded/collapsed
  searchBar: boolean; // Search bar visible
  notificationsOpen: boolean; // Notifications panel open
  helpOpen: boolean; // Help panel open

  // Menu data (from API)
  menus: MenuState;

  // Notifications (from API)
  notifications: Notification[];
  unReadNotifications: Notification[];
  readNotifications: Notification[];
  loading: boolean;
  showRead: boolean;
}

interface MenuState {
  menu: MenuItem[]; // Main navigation menu
  menu_billing: MenuItem[]; // Billing submenu (optional)
  menu_nonconformity: MenuItem[]; // Non-conformity submenu (optional)
}
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
interface TwoModeLayoutProps {
  /**
   * Child content to render in the main content area
   */
  children: React.ReactNode;

  /**
   * Callback when user changes language
   */
  onChangeLanguage?: (locale: string) => void;

  /**
   * Default sidenav state for this layout (before user preference is loaded)
   * @default false (collapsed)
   */
  defaultExpanded?: boolean;

  /**
   * Unique identifier for localStorage key
   * Full key will be: `${storageKeyPrefix}SideNavExpanded`
   * @default 'default'
   */
  storageKeyPrefix?: string;
}
```

### useSideNavPreference Hook

```typescript
interface UseSideNavPreferenceOptions {
  /**
   * Default state when no preference is stored
   * @default false
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
   * Current sidenav expansion state
   */
  isExpanded: boolean;

  /**
   * Toggle function (also persists to localStorage)
   */
  toggle: () => void;

  /**
   * Programmatically set state (also persists to localStorage)
   */
  setExpanded: (expanded: boolean) => void;
}

// Usage
const { isExpanded, toggle, setExpanded } = useSideNavPreference({
  defaultExpanded: true,
  storageKeyPrefix: "analyzer",
});
```

## localStorage Schema

### Key Format

```
{storageKeyPrefix}SideNavExpanded
```

### Examples

| Context        | Key                       | Value                 |
| -------------- | ------------------------- | --------------------- |
| Analyzer pages | `analyzerSideNavExpanded` | `"true"` or `"false"` |
| Default layout | `defaultSideNavExpanded`  | `"true"` or `"false"` |
| Admin pages    | `adminSideNavExpanded`    | `"true"` or `"false"` |

### Value Type

- Stored as string: `"true"` or `"false"`
- Parsed on read: `saved === "true"`

## CSS Class Model

### Layout Container Classes

```css
/* Applied to content wrapper div */
.content-expanded {
  margin-left: 16rem; /* 256px - Carbon SideNav expanded width */
  width: calc(100% - 16rem);
  transition: margin-left 0.11s cubic-bezier(0.2, 0, 1, 0.9), width 0.11s
      cubic-bezier(0.2, 0, 1, 0.9);
}

.content-collapsed {
  margin-left: 3rem; /* 48px - Carbon SideNav rail width */
  width: calc(100% - 3rem);
  transition: margin-left 0.11s cubic-bezier(0.2, 0, 1, 0.9), width 0.11s
      cubic-bezier(0.2, 0, 1, 0.9);
}

/* Responsive override - below Carbon lg breakpoint */
@media (max-width: 1056px) {
  .content-expanded,
  .content-collapsed {
    margin-left: 0;
    width: 100%;
  }
}
```

## State Transitions

### Sidenav Toggle

```
User clicks HeaderMenuButton
    ↓
handleSideNavToggle()
    ↓
setIsSideNavExpanded(!current)
    ↓
localStorage.setItem(key, newValue)
    ↓
Re-render with new margin class
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
If found: parse and use stored value
If not found: use defaultExpanded prop
    ↓
Initial render with correct state
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
