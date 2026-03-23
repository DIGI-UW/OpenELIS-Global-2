# Component Contract: TwoModeLayout

**Feature**: 009-carbon-sidenav  
**Date**: December 4, 2025

## Overview

`TwoModeLayout` is a reusable layout component that provides a Carbon Design
System UI Shell with a two-mode sidenav (expanded/collapsed) and preference
persistence.

## Component Interface

### Props

| Prop               | Type                       | Required | Default     | Description                                            |
| ------------------ | -------------------------- | -------- | ----------- | ------------------------------------------------------ |
| `children`         | `React.ReactNode`          | Yes      | -           | Content to render in the main content area             |
| `onChangeLanguage` | `(locale: string) => void` | No       | -           | Callback when user changes language                    |
| `defaultExpanded`  | `boolean`                  | No       | `false`     | Default sidenav state before user preference is loaded |
| `storageKeyPrefix` | `string`                   | No       | `'default'` | Prefix for localStorage key                            |

### Usage Examples

**Basic Usage (collapsed by default)**:

```jsx
import TwoModeLayout from "./components/layout/TwoModeLayout";

function App() {
  return (
    <TwoModeLayout onChangeLanguage={handleLanguageChange}>
      <MyPageContent />
    </TwoModeLayout>
  );
}
```

**Expanded by Default (for analyzer pages)**:

```jsx
<TwoModeLayout
  defaultExpanded={true}
  storageKeyPrefix="analyzer"
  onChangeLanguage={handleLanguageChange}
>
  <AnalyzersPage />
</TwoModeLayout>
```

**Per-Section Configuration**:

```jsx
// In App.js routing
<Route path="/analyzers">
  <TwoModeLayout defaultExpanded={true} storageKeyPrefix="analyzer">
    <Switch>
      <Route path="/analyzers" component={AnalyzersPage} />
    </Switch>
  </TwoModeLayout>
</Route>

<Route path="/admin">
  <TwoModeLayout defaultExpanded={false} storageKeyPrefix="admin">
    <Switch>
      <Route path="/admin" component={AdminPage} />
    </Switch>
  </TwoModeLayout>
</Route>
```

## Hook Interface: useSideNavPreference

### Signature

```typescript
function useSideNavPreference(
  options?: UseSideNavPreferenceOptions
): UseSideNavPreferenceReturn;
```

### Options

| Option             | Type      | Default     | Description                             |
| ------------------ | --------- | ----------- | --------------------------------------- |
| `defaultExpanded`  | `boolean` | `false`     | Default state when no preference stored |
| `storageKeyPrefix` | `string`  | `'default'` | Prefix for localStorage key             |

### Return Value

| Property      | Type                          | Description                     |
| ------------- | ----------------------------- | ------------------------------- |
| `isExpanded`  | `boolean`                     | Current sidenav expansion state |
| `toggle`      | `() => void`                  | Toggle and persist state        |
| `setExpanded` | `(expanded: boolean) => void` | Set and persist state           |

### Usage Example

```jsx
import { useSideNavPreference } from "./hooks/useSideNavPreference";

function CustomLayout() {
  const { isExpanded, toggle } = useSideNavPreference({
    defaultExpanded: true,
    storageKeyPrefix: "custom",
  });

  return (
    <Header>
      <HeaderMenuButton isActive={isExpanded} onClick={toggle} />
      <SideNav expanded={isExpanded} isFixedNav={true} isChildOfHeader={true} />
    </Header>
  );
}
```

## Behavior Contract

### State Persistence

1. **On Toggle**: State is immediately persisted to localStorage
2. **On Mount**: State is restored from localStorage if available
3. **Fallback**: If localStorage unavailable (e.g., private browsing), use
   `defaultExpanded`

### localStorage Key Format

```
{storageKeyPrefix}SideNavExpanded
```

**Examples**:

- `defaultSideNavExpanded` (default)
- `analyzerSideNavExpanded` (analyzer pages)
- `adminSideNavExpanded` (admin pages)

### Menu Auto-Expansion

1. **Trigger**: Route change (detected via `useLocation`)
2. **Behavior**: Parent menu items containing the active route auto-expand
3. **Scope**: Only expands items in the active path, does not collapse others

### Responsive Behavior

| Viewport Width | Sidenav Behavior                |
| -------------- | ------------------------------- |
| > 1056px       | Fixed nav, pushes content       |
| ≤ 1056px       | Overlay nav, content full width |

## CSS Classes (for custom styling)

| Class                | Applied When           | Purpose                   |
| -------------------- | ---------------------- | ------------------------- |
| `.content-expanded`  | `isExpanded === true`  | Sets left margin to 16rem |
| `.content-collapsed` | `isExpanded === false` | Sets left margin to 3rem  |

## Accessibility

- `HeaderMenuButton` provides `aria-label` based on state ("Open menu" / "Close
  menu")
- SideNav items use `aria-label` from internationalized menu labels
- Focus management follows Carbon defaults
- Keyboard navigation: Tab through items, Enter/Space to select

## Error Handling

| Scenario                   | Behavior                                               |
| -------------------------- | ------------------------------------------------------ |
| localStorage unavailable   | Log warning, use `defaultExpanded`                     |
| Menu API failure           | Display empty sidenav, existing error handling applies |
| Invalid localStorage value | Reset to `defaultExpanded`, persist corrected value    |

## Migration from Header.js

To migrate an existing route from `Header.js` to `TwoModeLayout`:

1. **Replace Layout wrapper**:

   ```jsx
   // Before
   <Layout>
     <MyPage />
   </Layout>

   // After
   <TwoModeLayout storageKeyPrefix="mySection">
     <MyPage />
   </TwoModeLayout>
   ```

2. **Configure default state** (optional):

   ```jsx
   <TwoModeLayout defaultExpanded={true}>
   ```

3. **No other changes required** - menu data, notifications, and authentication
   all work the same.

## Dependencies

- `@carbon/react`: Header, SideNav, SideNavItems, SideNavMenu, SideNavMenuItem,
  Content, Theme
- `react-router-dom`: useLocation for route change detection
- `react-intl`: useIntl for internationalized labels
