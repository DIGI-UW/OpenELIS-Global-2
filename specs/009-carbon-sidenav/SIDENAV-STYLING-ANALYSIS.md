# Sidenav Styling Inconsistencies Analysis

**Date:** 2025-01-XX  
**Issue:** Conflicting styles and active state not working consistently across all subnavs

## Root Cause Analysis

### 1. **Inconsistent Component Usage**

Three different rendering patterns are used:

| Level | Has Children? | Component | isActive | className |
|-------|---------------|-----------|----------|-----------|
| 0 (top) | Yes | `SideNavMenu` | ❌ NO | `top-level-menu-item` |
| 0 (top) | No | `SideNavMenuItem` | ✅ YES | `top-level-menu-item` |
| >0 (subnav) | Any | `SideNavMenuItem` | ✅ YES | `reduced-padding-nav-menu-item` |

**Problem:** Top-level items with children use `SideNavMenu` which doesn't support `isActive` prop, so they can never show active state.

### 2. **CSS Selector Issues**

Current CSS:
```css
/* Only targets subnavs */
.reduced-padding-nav-menu-item > a.cds--side-nav__link {
  padding-left: 2.5rem !important;
}
```

**Problems:**
- Selector `> a.cds--side-nav__link` assumes direct child `<a>` tag, but Carbon's structure might be different
- Top-level items with `top-level-menu-item` class have no CSS rules
- Active state CSS uses generic `.cds--side-nav__menu-item` which should work, but might be overridden

### 3. **Active State Logic**

The `isActive` calculation:
```javascript
const isActive = 
  location.pathname === menuItem.menu.actionURL || 
  (menuItem.menu.actionURL && location.pathname.startsWith(menuItem.menu.actionURL + "/"));
```

**Problems:**
- Top-level items with children don't get `isActive` applied (they use `SideNavMenu`)
- Parent items should be active if any child is active (for visual hierarchy)

### 4. **Nested Structure Issues**

Subnav items are rendered inside a `<span>` with `display: none` when collapsed:
```javascript
<span style={{ display: menuItem.expanded ? "" : "none" }}>
  {generateMenuItems(...)}
</span>
```

This might interfere with Carbon's internal styling/state management.

## Proposed Fixes

### Fix 1: Unified Active State for Top-Level Items

**Option A (Recommended):** Make top-level items with children also use `SideNavMenuItem` with nested `SideNavMenu` inside.

**Option B:** Add CSS to style `SideNavMenu` when it contains an active child.

### Fix 2: Fix CSS Selectors

Use Carbon's actual DOM structure. Carbon's `SideNavMenuItem` renders as:
```html
<li class="cds--side-nav__menu-item">
  <a class="cds--side-nav__link" href="...">
    <!-- content -->
  </a>
</li>
```

So the selector should be:
```css
.reduced-padding-nav-menu-item.cds--side-nav__menu-item .cds--side-nav__link {
  padding-left: 2.5rem !important;
}
```

### Fix 3: Consistent Active State Application

Ensure ALL items (regardless of level or children) can show active state:
- Top-level with children: Check if any child is active, apply visual indicator
- All levels: Use consistent `isActive` prop or CSS class

### Fix 4: Remove Inline Styles That Conflict

The `marginLeft: marginValue` inline style might conflict with Carbon's padding. Use CSS classes instead.

## Implementation Plan

1. **Audit Carbon's actual DOM structure** - Inspect rendered HTML in browser
2. **Fix CSS selectors** - Match Carbon's actual structure
3. **Unify active state logic** - Ensure all items can show active state
4. **Test across all menu levels** - Verify consistency

## Testing Checklist

- [ ] Top-level item without children shows active state
- [ ] Top-level item with children shows active state when child is active
- [ ] Level 1 subnav shows active state
- [ ] Level 2+ subnav shows active state
- [ ] Hover state works consistently
- [ ] Indentation is consistent across levels
- [ ] No double borders on active items
- [ ] Active state persists on page reload

