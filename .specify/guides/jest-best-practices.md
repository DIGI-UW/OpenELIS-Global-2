# Jest Best Practices Quick Reference

**Quick Reference Guide** for common Jest + React Testing Library patterns in
OpenELIS Global 2.

**For Comprehensive Guidance**: See
[Testing Roadmap](.specify/guides/testing-roadmap.md) for detailed patterns and
examples.

**For Official Documentation**: See
[Jest React Tutorial](https://jestjs.io/docs/tutorial-react).

---

## Import Order Cheat Sheet

**STRICT Order** (Jest hoisting requires mocks before imports):

1. **React**

   ```javascript
   import React from "react";
   ```

2. **Testing Library**

   ```javascript
   import {
     render,
     screen,
     fireEvent,
     waitFor,
     within,
     act,
   } from "@testing-library/react";
   ```

3. **userEvent** (PREFERRED for user interactions)

   ```javascript
   import userEvent from "@testing-library/user-event";
   ```

4. **jest-dom matchers** (MUST be imported)

   ```javascript
   import "@testing-library/jest-dom";
   ```

5. **IntlProvider** (if component uses i18n)

   ```javascript
   import { IntlProvider } from "react-intl";
   ```

6. **Router** (if component uses routing)

   ```javascript
   import { BrowserRouter } from "react-router-dom";
   ```

7. **Component under test**

   ```javascript
   import ComponentName from "./ComponentName";
   ```

8. **Utilities**

   ```javascript
   import { getFromOpenElisServer } from "../utils/Utils";
   ```

9. **Messages/translations**
   ```javascript
   import messages from "../../../languages/en.json";
   ```

**Mocks MUST be before imports**:

```javascript
// Mocks FIRST (before imports)
jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

// Then imports
import ComponentName from "./ComponentName";
```

---

## Query Method Cheat Sheet

**Decision Tree**:

1. **Element MUST exist** → `getBy*`

   ```javascript
   screen.getByText("Submit");
   screen.getByRole("button", { name: /submit/i });
   screen.getByTestId("submit-button");
   ```

2. **Checking if element does NOT exist** → `queryBy*`

   ```javascript
   expect(screen.queryByText("Error")).not.toBeInTheDocument();
   ```

3. **Element appears after async operation** → `findBy*`

   ```javascript
   const element = await screen.findByText("Loaded Data");
   ```

4. **Inside waitFor** → `queryBy*` (NOT `getBy*`)
   ```javascript
   await waitFor(() => {
     const element = screen.queryByText("Loaded Data");
     expect(element).toBeInTheDocument();
   });
   ```

---

## userEvent vs fireEvent Decision Tree

**PREFERRED: userEvent** (for user interactions):

- ✅ Clicking buttons
- ✅ Typing in inputs
- ✅ Keyboard navigation
- ✅ More realistic (triggers all events)

**FALLBACK: fireEvent** (only when userEvent doesn't work):

- ⚠️ Rare edge cases
- ⚠️ Programmatic events (not user-initiated)

**Examples**:

```javascript
// ✅ CORRECT: userEvent for user interactions
await userEvent.click(button);
await userEvent.type(input, "text", { delay: 0 });

// ⚠️ FALLBACK: fireEvent when userEvent doesn't work
fireEvent.click(button);
fireEvent.change(input, { target: { value: "text" } });
```

---

## Async Testing Patterns

**DO - waitFor with queryBy\***:

```javascript
await waitFor(
  () => {
    const element = screen.queryByText("Loaded Data");
    expect(element).toBeInTheDocument();
  },
  { timeout: 5000 }
);
```

**DO - findBy\***:

```javascript
const element = await screen.findByText("Loaded Data", {}, { timeout: 5000 });
```

**DO - act() for state updates**:

```javascript
await act(async () => {
  await userEvent.type(input, "Test");
});
```

**DON'T - setTimeout**:

```javascript
// ❌ WRONG: No retry logic
await new Promise((resolve) => setTimeout(resolve, 1000));
```

**DON'T - getBy\* in waitFor**:

```javascript
// ❌ WRONG: Throws during retries
await waitFor(() => {
  expect(screen.getByText("Loaded Data")).toBeInTheDocument();
});
```

---

## Carbon Component Quick Patterns

**TextInput**:

```javascript
const input = screen.getByLabelText(/name/i);
await userEvent.type(input, "Test Name", { delay: 0 });
```

**ComboBox**:

```javascript
const input = screen.getByRole("combobox", { name: /room/i });
await userEvent.type(input, "Main Laboratory", { delay: 0 });
await waitFor(() => {
  const menu = document.querySelector('[role="listbox"]');
  expect(menu && menu.children.length > 0).toBeTruthy();
});
const option = await screen.findByRole("option", { name: /main laboratory/i });
await userEvent.click(option);
```

**OverflowMenu**:

```javascript
const menuButton = screen.getByTestId("overflow-menu-button");
await userEvent.click(menuButton);
await waitFor(() => {
  const menu = screen.queryByRole("menu");
  expect(menu).toBeInTheDocument();
});
const menuItem = await screen.findByRole("menuitem", { name: /delete/i });
await userEvent.click(menuItem);
```

**DataTable**:

```javascript
const table = await screen.findByRole("table");
const row = within(table).getByText("Row Data");
const actionButton = within(row).getByRole("button", { name: /edit/i });
await userEvent.click(actionButton);
```

**Modal/Dialog**:

```javascript
const openButton = screen.getByRole("button", { name: /open modal/i });
await userEvent.click(openButton);
const modal = await screen.findByRole("dialog");
const confirmButton = within(modal).getByRole("button", { name: /confirm/i });
await userEvent.click(confirmButton);
```

---

## Edge Case Testing

**Null Values**:

```javascript
renderWithIntl(<ComponentName value={null} />);
expect(screen.getByText("N/A")).toBeInTheDocument();
```

**Empty Values**:

```javascript
renderWithIntl(<ComponentName items={[]} />);
expect(screen.getByText("No items")).toBeInTheDocument();
```

**Boundary Values**:

```javascript
renderWithIntl(<ComponentName maxLength={100} />);
const input = screen.getByLabelText(/name/i);
await userEvent.type(input, "a".repeat(100));
expect(input.value.length).toBe(100);
```

---

## Anti-Patterns Checklist

**DO NOT**:

- ❌ Use `setTimeout` (use `waitFor` instead)
- ❌ Use `getBy*` in `waitFor` (use `queryBy*` instead)
- ❌ Use `fireEvent` when `userEvent` works (prefer `userEvent`)
- ❌ Test implementation details (test user-visible behavior)
- ❌ Test internal state (test outputs)
- ❌ Test function call counts (test outcomes)
- ❌ Use inconsistent import order
- ❌ Skip mocking utilities before imports

---

## TDD Workflow Quick Reference

**Red-Green-Refactor Cycle**:

1. **Red**: Write failing test first

   ```javascript
   test("testSubmitForm_ShowsSuccess", async () => {
     renderWithIntl(<ComponentName />);
     // Test will fail - component doesn't exist yet
   });
   ```

2. **Green**: Write minimal code to pass

   ```javascript
   // Implement just enough to make test pass
   ```

3. **Refactor**: Improve while keeping tests green
   ```javascript
   // Refactor code, tests should still pass
   ```

---

## Test Organization

**File Naming**:

- `ComponentName.test.jsx` (co-located)
- OR `__tests__/ComponentName.test.jsx` (separate)

**Test Naming**:

- Format: `test{Scenario}_{ExpectedResult}`
- Example: `testSubmitForm_WithValidData_ShowsSuccessMessage`

**Grouping**:

```javascript
describe("ComponentName Form Validation", () => {
  // Related tests grouped together
});
```

---

## Helper Functions Quick Reference

**renderWithIntl**:

```javascript
const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>
  );
};
```

**setupApiMocks**:

```javascript
const setupApiMocks = (overrides = {}) => {
  const defaults = { rooms: [], devices: [] };
  const data = { ...defaults, ...overrides };
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("/rest/storage/rooms")) {
      callback(data.rooms);
    }
  });
};
```

**createMockRoom** (builder pattern):

```javascript
const createMockRoom = (overrides = {}) => ({
  id: "1",
  name: "Main Laboratory",
  code: "MAIN",
  active: true,
  ...overrides,
});
```

---

## What to Test

**DO Test**:

- ✅ User-visible behavior
- ✅ User interactions
- ✅ Inputs and outputs
- ✅ Edge cases (null, empty, boundary)
- ✅ Error states

**DON'T Test**:

- ❌ Internal component state
- ❌ Function call counts
- ❌ Prop values (unless user-visible)
- ❌ Implementation details

---

**Last Updated**: 2025-01-XX  
**Reference**: [Testing Roadmap](.specify/guides/testing-roadmap.md) for
comprehensive guidance
