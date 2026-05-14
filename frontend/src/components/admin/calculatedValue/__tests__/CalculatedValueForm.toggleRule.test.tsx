import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

/**
 * Regression for OGC-655 — "Toggle Rule" on Calculated Values has no
 * persistence path.
 *
 * Bug-by-design: clicking the Toggle Rule switch on a calculation only
 * mutates local React state (`toggleCalculation` at
 * CalculatedValueForm.tsx:622-626). It does not fire any API request, and
 * the GET endpoint that loads calculations clobbers `toggled` to false on
 * every read (CalculatedValueRestController.java:78). So even if local
 * state persisted across reloads, the BE would erase it.
 *
 * This test asserts the *current* behavior — local state changes, no fetch
 * fires — to lock the bug-confirmed state. When OGC-655 is fixed (the
 * toggle starts persisting), this test will need to flip its assertions.
 */

// Capture POST/PATCH calls so we can prove no API fires on toggle click.
// Hoisted so the vi.mock factory below (which runs at top-of-file before
// regular let/const initializations) can reference it.
const { postSpy } = vi.hoisted(() => ({ postSpy: vi.fn() }));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback !== "function") return;
    if (url === "/rest/test-calculations") {
      // One ACTIVE calculation with toggled=true, so the editor body renders
      // (gated by `{calculation.toggled && (...)}`) and we can assert a
      // toggle click HIDES it (proving local state changed).
      callback([
        {
          id: 1,
          name: "Test Calc 1",
          sampleId: 1,
          testId: 1,
          result: "1",
          note: "",
          toggled: true,
          active: true,
          operations: [
            {
              id: null,
              order: 0,
              type: "INTEGER",
              value: "1",
              sampleId: null,
            },
          ],
        },
      ]);
      return;
    }
    if (url === "/rest/math-functions") {
      callback([{ id: "ABS", value: "abs" }]);
      return;
    }
    if (url === "/rest/displayList/SAMPLE_TYPE_ACTIVE") {
      callback([{ id: "1", value: "Blood" }]);
      return;
    }
    if (url.startsWith("/rest/test-display-beans-map")) {
      callback({});
      return;
    }
    if (url.startsWith("/rest/test-display-beans")) {
      callback([]);
      return;
    }
    callback([]);
  }),
  postToOpenElisServer: postSpy,
}));

vi.mock("../../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: () => {},
    addNotification: () => {},
  }),
}));

vi.mock("../../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { success: "success", error: "error" },
}));

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: () => null,
}));

vi.mock("../../../common/AutoComplete", () => ({
  default: () => <input data-testid="autocomplete-mock" />,
}));

import CalculatedValue from "../CalculatedValueForm";

const renderForm = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CalculatedValue />
    </IntlProvider>,
  );

const flush = () => new Promise((r) => setTimeout(r, 0));

describe("OGC-655 — Calculated Values 'Toggle Rule' has no persistence path", () => {
  beforeEach(() => {
    postSpy.mockReset();
  });

  test("clicking Toggle Rule does not fire any POST/PATCH (no API call)", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    // Carbon Toggle renders an input[role="switch"]. The id is `${index}_toggle`
    // per CalculatedValueForm.tsx:668.
    const toggle = await screen.findByRole("switch", {
      name: /toggle/i,
    });

    await user.click(toggle);
    await flush();

    expect(
      postSpy,
      "OGC-655: Toggle Rule click must not fire any persistence call (current bug-by-design state)",
    ).not.toHaveBeenCalled();
  });

  test("Toggle Rule click HIDES the editor body (local state changes only)", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    // The editor body is gated by `{calculation.toggled && ...}`. With
    // toggled=true (initial state we mocked), the "Add" buttons inside the
    // editor render. Confirm one is visible before the click.
    expect(
      screen.queryByRole("button", { name: /test result/i }),
      "editor body should be visible while toggled=true",
    ).not.toBeNull();

    const toggle = await screen.findByRole("switch", { name: /toggle/i });
    await user.click(toggle);
    await flush();

    // After clicking, toggled flips to false; the gated editor unmounts.
    expect(
      screen.queryByRole("button", { name: /test result/i }),
      "editor body should be hidden after toggle off",
    ).toBeNull();
  });
});
