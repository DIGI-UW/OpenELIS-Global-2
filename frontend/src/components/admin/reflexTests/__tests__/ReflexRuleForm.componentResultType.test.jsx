import React from "react";
import { render, cleanup, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

/**
 * A reflex condition is described by the component it reads.
 *
 * The condition editor already asks the chosen component what it reports, but
 * it still took the values to offer from the test's own list — and that list is
 * every component's merged into one. Two coded components of the same test with
 * disjoint option sets therefore read identically here: choosing either offered
 * the whole test's vocabulary, including values the named component can never
 * hold.
 *
 * The dependency is Sample Type → Test → Component → Result Type → Options.
 */

const EXAMPLE_TEST = {
  id: "1",
  value: "Example Test",
  resultType: "N",
  resultTypes: ["N", "D"],
  // The merged list the editor used to offer for every component.
  resultList: [
    { id: "x", value: "X" },
    { id: "y", value: "Y" },
    { id: "w", value: "W" },
    { id: "z", value: "Z" },
  ],
  components: [
    { id: "comp-a", value: "Component A", resultType: "N", primary: true },
    {
      id: "comp-b",
      value: "Component B",
      resultType: "D",
      primary: false,
      resultList: [
        { id: "x", value: "X" },
        { id: "y", value: "Y" },
      ],
    },
    {
      id: "comp-c",
      value: "Component C",
      resultType: "D",
      primary: false,
      resultList: [
        { id: "w", value: "W" },
        { id: "z", value: "Z" },
      ],
    },
  ],
};

const SAVED_RULE = {
  id: 1,
  ruleName: "Example Rule",
  overall: "ANY",
  active: true,
  toggled: true,
  conditions: [
    {
      id: 1,
      sampleId: "1",
      componentId: "comp-b",
      testName: "Example Test",
      testId: "1",
      relation: "EQUALS",
      value: "x",
      value2: "0",
      testAnalyteId: null,
    },
  ],
  actions: [],
};

const { postSpy } = vi.hoisted(() => ({ postSpy: vi.fn() }));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback !== "function") return;
    if (url.startsWith("/rest/reflexrules")) {
      // A fresh copy per load: the form edits its rows in place, so a shared
      // object would carry one test's selections into the next.
      callback([JSON.parse(JSON.stringify(SAVED_RULE))]);
      return;
    }
    if (url === "/rest/reflexrule-options") {
      callback({
        generalRelationOptions: [{ id: "EQUALS", value: "Equals" }],
        numericRelationOptions: [{ id: "EQUALS", value: "Equals" }],
        overallOptions: [{ id: "ANY", value: "Any" }],
      });
      return;
    }
    if (url === "/rest/displayList/SAMPLE_TYPE_ACTIVE") {
      callback([{ id: "1", value: "Blood" }]);
      return;
    }
    if (url.startsWith("/rest/test-display-beans-map")) {
      callback({ 1: [EXAMPLE_TEST] });
      return;
    }
    if (url.startsWith("/rest/test-display-beans")) {
      callback([EXAMPLE_TEST]);
      return;
    }
    callback([]);
  }),
  postToOpenElisServer: vi.fn((url, body, callback) => {
    postSpy(url, body);
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

import ReflexRule from "../ReflexRuleForm";

const renderForm = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ReflexRule />
    </IntlProvider>,
  );

const flush = () => new Promise((r) => setTimeout(r, 0));

const componentPicker = () => document.getElementById("0_0_component");

/** The coded value picker, or null when the condition reads a number. */
const valuePicker = () => document.getElementById("0_0_value");

const optionLabels = () =>
  Array.from(valuePicker()?.options || [])
    .map((o) => o.text)
    .filter(Boolean);

describe("reflex condition is described by its component", () => {
  beforeEach(() => {
    cleanup();
    postSpy.mockReset();
  });

  test("every component of the test may be read", async () => {
    renderForm();
    await flush();

    const labels = Array.from(componentPicker().options).map((o) => o.text);

    expect(labels).toEqual(
      expect.arrayContaining(["Component A", "Component B", "Component C"]),
    );
  });

  test("a coded component offers its own options, not the test's merged list", async () => {
    renderForm();
    await flush();

    expect(optionLabels()).toEqual(["X", "Y"]);
    expect(
      optionLabels(),
      "W and Z belong to Component C — the test-level list holds both sets",
    ).not.toContain("W");
  });

  test("changing the component replaces the options with that component's", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    await user.selectOptions(componentPicker(), "comp-c");
    await flush();

    expect(optionLabels()).toEqual(["W", "Z"]);
    expect(
      optionLabels(),
      "Component B's options must not survive the change",
    ).not.toContain("X");
  });

  test("changing the component clears the value picked under the old one", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    expect(valuePicker().value).toBe("x");

    await user.selectOptions(componentPicker(), "comp-c");
    await flush();

    expect(valuePicker().value, "x is not one of Component C's options").toBe(
      "",
    );
  });

  test("choosing a numeric component takes the coded editor away", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    expect(valuePicker().tagName).toBe("SELECT");

    await user.selectOptions(componentPicker(), "comp-a");
    await flush();

    expect(
      valuePicker()?.tagName,
      "a numeric condition is typed, not chosen from a list",
    ).not.toBe("SELECT");
  });

  test("the chosen component is what gets saved, and the editor's own state is not", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    await user.selectOptions(componentPicker(), "comp-c");
    await flush();
    fireEvent.submit(componentPicker().closest("form"));
    await flush();

    expect(postSpy).toHaveBeenCalled();
    const saved = JSON.parse(postSpy.mock.calls[0][1]);
    expect(
      saved.conditions[0].componentId,
      "the component the user chose",
    ).toBe("comp-c");
    expect(
      Object.keys(saved.conditions[0]),
      "componentPending describes the editor, not the rule",
    ).not.toContain("componentPending");
  });
});
