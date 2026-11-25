import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

/**
 * OGC-1587: when the server refuses a rule it says why (a loop, a rule reading
 * its own result, a second rule for the same test, a name in use). The editor
 * used to show "Duplicate Calculation Name or Error while saving" for every
 * refusal, and a refused switch-on showed a generic save error.
 */

const { postSpy, notificationSpy, answer } = vi.hoisted(() => ({
  postSpy: vi.fn(),
  notificationSpy: vi.fn(),
  answer: { status: 200, body: null as unknown },
}));

vi.mock("../../../utils/Utils", async () => ({
  resolveApiErrorMessage: (
    (await vi.importActual("../../../utils/Utils")) as any
  ).resolveApiErrorMessage,
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback !== "function") return;
    if (url.startsWith("/rest/test-calculations")) {
      callback([
        {
          id: 1,
          name: "Urea doubled",
          sampleId: 1,
          testId: 1,
          result: "",
          note: "",
          toggled: false,
          active: false,
          operations: [
            { id: null, order: 0, type: "INTEGER", value: "1", sampleId: null },
          ],
        },
      ]);
      return;
    }
    callback([]);
  }),
  postToOpenElisServerFullResponse: vi.fn((url, _body, callback) => {
    postSpy(url);
    callback(
      new Response(answer.body === null ? null : JSON.stringify(answer.body), {
        status: answer.status,
        headers: { "Content-Type": "application/json" },
      }),
    );
  }),
}));

vi.mock("../../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: () => {},
    addNotification: (body: unknown) => notificationSpy(body),
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

const lastMessage = () =>
  notificationSpy.mock.calls[notificationSpy.mock.calls.length - 1][0].message;

describe("OGC-1587: a refused rule says why", () => {
  beforeEach(() => {
    cleanup();
    postSpy.mockReset();
    notificationSpy.mockReset();
    answer.status = 200;
    answer.body = null;
  });

  test("a refused save shows the server's reason, not the duplicate-name text", async () => {
    answer.status = 400;
    answer.body = {
      messageKey: "calculation.error.cycle",
      messageArgs: { cycle: "ALT x2 → AST x2 → ALT x2" },
    };
    renderForm();
    await flush();

    const form = document.querySelector("form") as HTMLFormElement;
    fireEvent.submit(form);
    await flush();
    await flush();

    expect(postSpy).toHaveBeenCalledWith("/rest/test-calculation");
    expect(lastMessage()).toBe(
      "These active rules would feed each other in a loop: ALT x2 → AST x2 → ALT x2.",
    );
  });

  test("a save refused without a reason keeps the old message", async () => {
    answer.status = 500;
    renderForm();
    await flush();

    fireEvent.submit(document.querySelector("form") as HTMLFormElement);
    await flush();
    await flush();

    expect(lastMessage()).toBe(
      "Duplicate Calculation Name or Error while saving",
    );
  });

  test("a refused switch-on reverts the switch and shows the reason", async () => {
    answer.status = 400;
    answer.body = {
      messageKey: "calculation.error.selfReference",
      messageArgs: { test: "Urea" },
    };
    const user = userEvent.setup();
    renderForm();
    await flush();

    const toggle = await screen.findByRole("switch", {
      name: /activate.*deactivate|toggle/i,
    });
    await user.click(toggle);
    await flush();
    await flush();

    expect(postSpy).toHaveBeenCalledWith("/rest/activate-test-calculation/1");
    expect(lastMessage()).toBe(
      "This rule reads Urea, the result it writes. A rule cannot use its own result.",
    );
    expect(screen.getByText(/active:\s*false/i)).toBeInTheDocument();
  });
});
