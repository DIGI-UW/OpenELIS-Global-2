# Carbon Design System & React Best Practices Review

## Executive Summary

The Storage Location CRUD interface has several critical violations of Carbon
Design System and React best practices that are causing the UI issues,
particularly the **Active toggle visibility problem**.

---

## 🚨 Critical Issues Found

### 1. **MISSING LOADING STATE UI** (HIGH PRIORITY)

**Problem**: `isLoading` state variable is declared but **NEVER USED** in the
UI.

**Location**: `EditLocationModal.jsx` lines 58, 123, 157, 170, 175, 191

```jsx
const [isLoading, setIsLoading] = useState(false);

useEffect(() => {
  if (location && open) {
    setIsLoading(true); // ✅ Set to true
    // ...fetch data...
    setIsLoading(false); // ✅ Set to false
  }
}, [location, open]);

// ❌ BUT: isLoading is NEVER checked in JSX!
```

**Impact**:

- Modal shows stale/incorrect data while fetching
- No visual feedback that data is loading
- Form fields are interactive with potentially incorrect values
- **This is why the toggle appears to have wrong state**

**Carbon Best Practice Violation**:

> "Loading states should use skeleton states for initial loads and inline
> loading for actions. Skeleton states prevent layout shifts and maintain
> structure." -
> [Carbon Loading Patterns](https://carbondesignsystem.com/patterns/loading)

**Fix Required**:

```jsx
import { SkeletonText, SkeletonPlaceholder } from "@carbon/react";

<ModalBody>
  {isLoading ? (
    <div className="edit-location-form">
      <SkeletonText heading />
      <SkeletonText />
      <SkeletonText />
      <SkeletonText />
      <SkeletonPlaceholder style={{ height: "48px" }} />
    </div>
  ) : (
    <div className="edit-location-form">{/* Actual form fields */}</div>
  )}
</ModalBody>;
```

---

### 2. **MODAL CONTENT OVERFLOW** (HIGH PRIORITY)

**Problem**: Modal body doesn't show all form fields. Only 4 fields visible in
screenshot, but device form has 7 fields (Name, Code, Parent Room, Type,
Temperature, Capacity, **Active toggle**).

**Evidence**: Screenshot `09-toggle-not-visible.png` shows:

- ✅ Location Name (visible)
- ✅ Location Code (visible)
- ✅ Parent Room (visible)
- ✅ Type (visible)
- ❌ Temperature Setting (NOT VISIBLE)
- ❌ Capacity Limit (NOT VISIBLE)
- ❌ **Active toggle (NOT VISIBLE)**

**Root Cause**: Missing `overflow-y: auto` on modal body or CSS cutting off
content.

**Carbon Best Practice**: Modal body should scroll if content exceeds height.

**Fix Required**:

```css
/* EditLocationModal.css */
.edit-location-modal .cds--modal-body {
  overflow-y: auto;
  max-height: 60vh; /* Ensure scrolling */
}

.edit-location-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding-bottom: 1rem; /* Ensure last field visible */
}
```

---

### 3. **TOGGLE CONTROLLED COMPONENT ISSUE** (MEDIUM PRIORITY)

**Problem**: Toggle uses strict boolean equality check that may fail with type
coercion.

**Location**: Line 571

```jsx
<Toggle
  toggled={formData.active === true} // ❌ Strict check
  onToggle={(checked) => handleFieldChange("active", checked)}
/>
```

**Issue**: If `formData.active` is:

- `"true"` (string) → evaluates to FALSE ❌
- `1` (number) → evaluates to FALSE ❌
- `null` or `undefined` → evaluates to FALSE ❌

**React Best Practice Violation**:

> "Controlled components should handle all possible input types and normalize
> values." -
> [React Forms](https://react.dev/reference/react-dom/components/input#controlling-an-input-with-a-state-variable)

**Fix Required**:

```jsx
<Toggle
  toggled={Boolean(formData.active)} // ✅ Explicit boolean conversion
  onToggle={(checked) => handleFieldChange("active", checked)}
/>
```

Or better yet, use the existing `normalizeActive` helper:

```jsx
<Toggle
  toggled={normalizeActive(formData.active)} // ✅ Uses existing helper
  onToggle={(checked) => handleFieldChange("active", checked)}
/>
```

---

### 4. **DOUBLE STATE INITIALIZATION RACE CONDITION** (MEDIUM PRIORITY)

**Problem**: Form data is initialized TWICE - once from prop, then from API.

**Location**: Lines 120-191

```jsx
useEffect(() => {
  if (location && open) {
    // FIRST INITIALIZATION (from prop)
    setFormData(initializeFormDataFromLocation(location)); // ❌ Sets state

    setIsLoading(true);

    // SECOND INITIALIZATION (from API) - happens async
    getFromOpenElisServerV2(endpoint).then((fullLocation) => {
      setFormData({ ...fullLocation }); // ❌ Sets state AGAIN
      setIsLoading(false);
    });
  }
}, [location, open]);
```

**React Best Practice Violation**:

> "Avoid setting state twice in rapid succession. Use a loading state to prevent
> showing stale data." -
> [React useState](https://react.dev/reference/react/useState#avoiding-recreating-the-initial-state)

**Impact**:

- Toggle may briefly show wrong state before API returns
- User sees "flicker" of old data → new data
- Unnecessary re-renders (performance issue)

**Fix Required**:

```jsx
useEffect(() => {
  if (location && open) {
    // DON'T initialize from prop - wait for API
    setIsLoading(true);
    setError(null);

    getFromOpenElisServerV2(endpoint).then((fullLocation) => {
      setFormData(initializeFormDataFromLocation(fullLocation));
      setIsLoading(false);
    });
  }
}, [location, open]);
```

---

### 5. **MISSING FORM FIELD LABELS FOR ACCESSIBILITY** (LOW PRIORITY)

**Problem**: Some disabled/read-only fields don't follow Carbon accessibility
patterns.

**Carbon Accessibility Requirement**:

> "All form inputs must have associated labels. Use `labelText` prop for Carbon
> components." -
> [Carbon Form Accessibility](https://carbondesignsystem.com/components/form/accessibility)

**Current Implementation**: ✅ Most fields have `labelText`

**Recommendation**: Add `helperText` to read-only parent fields to explain why
they're disabled.

---

## 📋 Recommended Implementation Order

### Phase 1: Fix Visibility (URGENT)

1. Add `overflow-y: auto` to modal body CSS
2. Add `padding-bottom` to form to ensure last field visible
3. Test that all 7 device fields are visible

### Phase 2: Add Loading State (HIGH)

1. Import `SkeletonText` and `SkeletonPlaceholder` from `@carbon/react`
2. Conditionally render skeleton while `isLoading === true`
3. Disable Save button while loading

### Phase 3: Fix Toggle State (MEDIUM)

1. Replace `formData.active === true` with `Boolean(formData.active)`
2. OR use existing `normalizeActive(formData.active)` helper
3. Add console.log to debug toggle state changes

### Phase 4: Fix Race Condition (MEDIUM)

1. Remove initial `setFormData(initializeFormDataFromLocation(location))`
2. Only initialize form data AFTER API returns
3. Show skeleton during entire loading period

---

## 🎯 Carbon Design System Compliance Checklist

| Requirement                            | Status  | Fix                                    |
| -------------------------------------- | ------- | -------------------------------------- |
| Loading states use skeletons           | ❌ FAIL | Add `<SkeletonText>` during load       |
| Modal body scrolls if needed           | ❌ FAIL | Add `overflow-y: auto` CSS             |
| Controlled components handle all types | ❌ FAIL | Use `Boolean()` or `normalizeActive()` |
| No unnecessary re-renders              | ❌ FAIL | Remove double initialization           |
| All form inputs have labels            | ✅ PASS | Already implemented                    |
| Save button disabled during submit     | ✅ PASS | Already implemented                    |
| Error messages use InlineNotification  | ✅ PASS | Already implemented                    |
| Internationalization (React Intl)      | ✅ PASS | Already implemented                    |

---

## 🔍 Root Cause of Toggle Visibility Issue

**The toggle isn't actually "visibility: hidden" - it's BELOW THE FOLD and NOT
SCROLLABLE.**

**Evidence**:

1. Screenshot shows only 4 of 7 fields visible
2. Toggle is field #7 (last in device form)
3. Modal body has no `overflow-y: auto` CSS
4. Content is cut off by modal height constraint

**The fix is simple**: Add scrolling to modal body.

---

## 📚 References

- [Carbon Loading Patterns](https://carbondesignsystem.com/patterns/loading)
- [Carbon Toggle Usage](https://carbondesignsystem.com/components/toggle/usage)
- [Carbon Modal Guidelines](https://carbondesignsystem.com/components/modal/usage)
- [React Controlled Components](https://react.dev/reference/react-dom/components/input)
- [React useState Best Practices](https://react.dev/reference/react/useState)

---

## 🎓 Learning Points

1. **Always use loading states** - Don't show stale data
2. **Modal content must scroll** - Don't cut off fields
3. **Controlled components need type safety** - Boolean coercion is critical
4. **Avoid double initialization** - Fetch once, render once
5. **Test with real data** - Mocks hide integration issues (this is why tests
   didn't catch bugs)
