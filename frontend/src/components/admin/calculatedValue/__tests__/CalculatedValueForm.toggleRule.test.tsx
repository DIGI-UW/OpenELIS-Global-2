import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

/**
 * OGC-655 — "Toggle Rule" persists Active state, while display
 * expand/collapse is owned by an Accordion that wraps the editor body.
 *
 * - Clicking Toggle Rule POSTs to /rest/activate-test-calculation/{id} or
 *   /rest/deactivate-test-calculation/{id} and mirrors the new state into
 *   the local `active` flag.
 * - Clicking Toggle Rule does NOT collapse the editor body — the Accordion
 *   chevron is the only display affordance.
 */

const { postSpy } = vi.hoisted(() => ({ postSpy: vi.fn() }));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback !== "function") return;
    if (url === "/rest/test-calculations") {
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

    const toggle = await screen.findByRole("switch", {
      name: /activate.*deactivate|toggle/i,
    });
    await user.click(toggle);
    await flush();

    expect(
      postSpy,
      "OGC-655: toggle off must persist via deactivate endpoint",
    ).toHaveBeenCalledWith("/rest/deactivate-test-calculation/1");
  });

  test("toggle OFF updates Active label without collapsing the editor body", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    // Open the accordion so the editor body is visible for the assertion.
    // The Rule details title is the accordion header (a button).
    const accordionHeader = await screen.findByRole("button", {
      name: /view rule|rule details/i,
    });
    await user.click(accordionHeader);
    await flush();

    // Pre-condition: rule active=true, editor body visible after expanding.
    expect(
      screen.queryByRole("button", { name: /test result/i }),
      "editor body should be visible after expanding the accordion",
    ).not.toBeNull();

    const toggle = await screen.findByRole("switch", {
      name: /activate.*deactivate|toggle/i,
    });
    await user.click(toggle);
    await flush();

    // Active label flips to reflect the new state.
    expect(
      screen.queryByText(/active:\s*false/i),
      "OGC-655: Active label should reflect the new state",
    ).not.toBeNull();

    // Editor body stays expanded — toggle controls activation, not display.
    expect(
      screen.queryByRole("button", { name: /test result/i }),
      "editor body should remain visible after toggle off — Accordion owns display",
    ).not.toBeNull();
  });
});
