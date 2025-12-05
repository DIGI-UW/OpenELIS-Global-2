# Issue Analysis: Phase 3 - UX Polish

**Date**: December 5, 2025  
**Status**: Analysis Complete - Ready for Implementation

---

## Issue Summary

| # | Issue | Severity | Root Cause |
|---|-------|----------|------------|
| 1 | No lock/pin icon visible | High | Using invalid `renderIcon` prop on HeaderMenuButton |
| 2 | Ugly scrollbar on sidenav | Medium | Missing scrollbar-hiding CSS |
| 3 | Reports unfurled by default | High | Bug in `useMenuAutoExpand` - empty URL matches all paths |
| 4 | No active styling on current page | High | Missing `isActive` prop on SideNavMenuItem |
| 5 | No hover styling on subnavs | Medium | Missing CSS hover styles |
| 6 | Top-level click should navigate | Low | Click only unfurls, doesn't navigate |
| 7 | Console spam "reports" | Low | Debug console.log left in code |

---

## Detailed Analysis

### Issue 1: Lock/Pin Icon Not Visible

**Current Code (Header.js:510-516)**:
```javascript
renderIcon={() => {
  if (mode === SIDENAV_MODES.CLOSE) return <Menu size={20} />;
  if (mode === SIDENAV_MODES.SHOW) return <Locked size={20} />;
  return <Close size={20} />;
}}
```

**Problem**: `HeaderMenuButton` does NOT support `renderIcon` prop. It only supports:
- `renderMenuIcon` - Shown when not active (closed)
- `renderCloseIcon` - Shown when active (open)

**Console Error**:
```
Warning: React does not recognize the `renderIcon` prop on a DOM element.
```

**Solution Options**:

1. **Custom Button (Recommended)**: Replace `HeaderMenuButton` with a styled `button` that renders the correct icon based on mode.
2. **Conditional Icon Swap**: Since we have 3 states but Carbon only supports 2 slots, we need to conditionally pass different icons to `renderMenuIcon` based on mode.

**User Request**: Use `Pin` and `PinFilled` icons (more semantic for "pin in place").

**Icon Mapping**:
| Mode | Icon | Meaning |
|------|------|---------|
| CLOSE | `Menu` (Hamburger) | Nav is collapsed |
| SHOW | `Pin` (outline) | Nav is open, click to lock |
| LOCK | `Close` (X) | Nav is locked, click to close |

---

### Issue 2: Ugly Scrollbar on Sidenav

**Root Cause**: The sidenav has content overflow but no CSS to hide the scrollbar.

**Solution**: Add scrollbar-hiding CSS to `Style.css`:

```css
/* Hide scrollbar on sidenav - let page scroll */
.cds--side-nav {
  overflow-y: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

.cds--side-nav::-webkit-scrollbar {
  display: none; /* Chrome/Safari/Edge */
}
```

---

### Issue 3: Reports Unfurled by Default

**Root Cause**: Bug in `useMenuAutoExpand.js` at lines 60-65:

```javascript
if (
  item.menu.actionURL === location.pathname ||
  location.pathname.startsWith(item.menu.actionURL + "/")
) {
  isActiveBranch = true;
}
```

**Problem**: When `item.menu.actionURL` is `undefined` or `""` (empty):
- `location.pathname.startsWith("" + "/")` becomes `location.pathname.startsWith("/")`
- This is **ALWAYS TRUE** for any path!

**Why Reports is affected**: The "Reports" top-level menu has no `actionURL` (it's a parent folder). Its children (like "Routine") have URLs, but the parent doesn't. The bug causes the parent to match any route.

**Solution**: Add null/empty guard:

```javascript
if (
  item.menu.actionURL && 
  item.menu.actionURL.length > 1 && // Must be more than just "/"
  (
    item.menu.actionURL === location.pathname ||
    location.pathname.startsWith(item.menu.actionURL + "/")
  )
) {
  isActiveBranch = true;
}
```

---

### Issue 4: No Active Styling on Current Page

**Root Cause**: `SideNavMenuItem` components don't receive `isActive` or `aria-current="page"` props.

**Current Code** (Header.js):
```javascript
<SideNavMenuItem
  href={menuItem.menu.actionURL}
  // Missing: isActive or aria-current
>
```

**Solution**: Pass active state based on route match:

```javascript
<SideNavMenuItem
  href={menuItem.menu.actionURL}
  isActive={location.pathname === menuItem.menu.actionURL}
>
```

Or use `aria-current="page"` for the current page.

**CSS for Active Style** (already in Style.css but may need adjustment):
```css
.cds--side-nav__menu-item .cds--side-nav__link[aria-current="page"],
.cds--side-nav__menu-item .cds--side-nav__link--current {
  border-left: 4px solid var(--cds-link-primary);
  background-color: var(--cds-layer-selected-01);
}
```

---

### Issue 5: No Hover Styling on Subnavs

**Root Cause**: Missing or overridden hover styles for subnav items.

**Solution**: Add explicit hover CSS:

```css
.cds--side-nav__menu-item:hover > .cds--side-nav__link,
.cds--side-nav__menu-item .cds--side-nav__link:hover {
  background-color: var(--cds-layer-hover-01);
}

/* For custom buttons in subnav */
.custom-sidenav-button:hover {
  background-color: var(--cds-layer-hover-01);
}
```

---

### Issue 6: Top-Level Nav Click Should Navigate

**Current Behavior**: Clicking a top-level menu item (e.g., "Reports") only unfurls the submenu.

**Desired Behavior**: Click should unfurl AND navigate to the first child page.

**Solution**: Modify the click handler in `generateMenuItems`:

```javascript
onClick={(e) => {
  setMenuItemExpanded(e, menuItem, path);
  // Also navigate to first active child
  const firstActiveChild = menuItem.childMenus.find(c => c.menu.isActive);
  if (firstActiveChild?.menu.actionURL) {
    history.push(firstActiveChild.menu.actionURL);
  }
}}
```

**Note**: This is marked as "nitpick" - implement if time permits.

---

### Issue 7: Console Spam "reports"

**Location**: Header.js lines 338-341

```javascript
const hasActiveChildMenu = (menuItem) => {
  if (menuItem.menu.elementId === "menu_reports_routine") {
    console.log("reports");  // <-- Remove this
  }
  // ...
}
```

**Solution**: Delete the debug console.log.

---

## Console Errors (Not Our Bugs)

These are pre-existing issues, not caused by our changes:

1. **404 on `/notification/pnconfig`** - Backend endpoint missing (existing)
2. **Controlled/uncontrolled Select warning** - Existing form component issue
3. **single-spa minified message** - Third-party library warning
4. **WebSocket connection failed** - Dev server issue

---

## Implementation Priority

1. **High Priority** (Blocking UX):
   - Issue 1: Fix icon rendering
   - Issue 3: Fix Reports auto-expand bug
   - Issue 4: Add active styling

2. **Medium Priority** (Polish):
   - Issue 2: Hide scrollbar
   - Issue 5: Add hover styling
   - Issue 7: Remove console spam

3. **Low Priority** (Nice-to-have):
   - Issue 6: Top-level click navigation

---

## Files to Modify

1. `Header.js` - Fix icon, add isActive, remove console.log, optionally add navigation
2. `useMenuAutoExpand.js` - Fix empty URL bug
3. `Style.css` - Add scrollbar hiding, hover styles

