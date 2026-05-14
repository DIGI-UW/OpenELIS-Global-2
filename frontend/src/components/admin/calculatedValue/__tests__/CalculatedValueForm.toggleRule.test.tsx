import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

/**
 * OGC-655 — "Toggle Rule" on Calculated Values must persist Active state.
 *
 * Fixed behavior (this spec): clicking Toggle Rule on an existing rule (one
 * with an id) immediately POSTs to /rest/activate-test-calculation/{id} or
 * /rest/deactivate-test-calculation/{id} and mirrors the new state into the
 * local `active` flag so the read-only Active display stays in sync.
 *
 * Backend side of the fix:
 *  - CalculatedValueRestController.getReflexRules seeds `toggled` from the
 *    persisted `active`, so reload reflects what was saved.
 *  - CalculatedValueRestController.deactivateReflexRule / activateReflexRule
 *    return proper HTTP status (no more silent empty-catch).
 */

// Capture POST/PATCH calls so we can prove no API fires on toggle click.
// Hoisted so the vi.mock factory below (which runs at top-of-file before
// regular let/const initializations) can reference it.
const { postSpy } = vi.hoisted(() => ({ postSpy: vi.fn() }));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback !== "function") return;
    if (url === "/rest/test-calculations") {
      // One ACTIVE calculation with toggled=true (BE now seeds toggled from
      // active per OGC-655 fix), so the editor body renders.
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
  // Default the post callback to a 200 status so toggle clicks don't revert.
  // Utils.js#postToOpenElisServer passes response.status (a NUMBER) — mirror
  // that here so the test catches strict-equality regressions in callers.
  postToOpenElisServer: vi.fn((url, _body, callback) => {
    postSpy(url);
    if (typeof callback === "function") callback(200);
  }),
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

describe("OGC-655 — Calculated Values 'Toggle Rule' persists Active state", () => {
  beforeEach(() => {
    postSpy.mockReset();
  });

  test("toggle OFF fires POST /rest/deactivate-test-calculation/{id}", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    const toggle = await screen.findByRole("switch", { name: /toggle/i });
    await user.click(toggle);
    await flush();

    expect(
      postSpy,
      "OGC-655: toggle off must persist via deactivate endpoint",
    ).toHaveBeenCalledWith("/rest/deactivate-test-calculation/1");
  });

  test("toggle OFF hides the editor body AND updates Active label", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    // Pre-condition: rule starts active; editor body visible.
    expect(
      screen.queryByRole("button", { name: /test result/i }),
      "editor body should be visible while toggled=true",
    ).not.toBeNull();

    const toggle = await screen.findByRole("switch", { name: /toggle/i });
    await user.click(toggle);
    await flush();

    // Post-condition: editor body collapses + Active display flips to false.
    expect(
      screen.queryByRole("button", { name: /test result/i }),
      "editor body should be hidden after toggle off",
    ).toBeNull();
    expect(
      screen.queryByText(/active:\s*false/i),
      "OGC-655: Active label should reflect the new state",
    ).not.toBeNull();
  });
});
