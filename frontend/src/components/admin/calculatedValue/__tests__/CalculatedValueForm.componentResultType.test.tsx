import React from "react";
import { render, cleanup, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

/**
 * The Final Result is described by its component, not by its test.
 *
 * A test carries components that report different things — a numeric one
 * beside two coded ones whose option sets do not overlap. The builder read the
 * result type off the test, which answers with its primary component's, and
 * read the options off the test's list, which is every component's merged into
 * one. So the moment a test was picked the editor committed to a type nobody
 * had chosen, and offered the whole test's vocabulary whichever component the
 * calculation actually wrote to.
 *
 * The dependency is Sample Type → Test → Component → Result Type → Options,
 * and the component is the level that was being skipped.
 *
 * Separately, the component picker was filtered to numeric components. That
 * restriction belongs to the operands, which do arithmetic; the final result
 * may be a coded interpretation, and the filter left those unselectable.
 */

const EXAMPLE_TEST = {
  id: "1",
  value: "Example Test",
  resultType: "N",
  resultTypes: ["N", "D"],
  // The merged list the builder used to offer for every component.
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

const SAVED_CALCULATION = {
  id: 1,
  name: "Example Calc",
  sampleId: "1",
  testId: "1",
  componentId: "comp-b",
  result: "x",
  note: "",
  toggled: true,
  active: true,
  operations: [
    { id: null, order: 0, type: "INTEGER", value: "1", sampleId: null },
  ],
};

const { postSpy } = vi.hoisted(() => ({ postSpy: vi.fn() }));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback !== "function") return;
    if (url.startsWith("/rest/test-calculations")) {
      // A fresh copy per load: the form edits its rows in place, so a shared
      // object would carry one test's selections into the next.
      callback([JSON.parse(JSON.stringify(SAVED_CALCULATION))]);
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
      callback({ "1": [EXAMPLE_TEST] });
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

import CalculatedValue from "../CalculatedValueForm";

const renderForm = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CalculatedValue />
    </IntlProvider>,
  );

const flush = () => new Promise((r) => setTimeout(r, 0));

const componentPicker = () =>
  document.getElementById("0_finalcomponent") as HTMLSelectElement;

/** The dictionary picker, or null when no coded component is selected. */
const optionPicker = () =>
  document.getElementById("0_resultdictionary") as HTMLSelectElement | null;

const optionLabels = () =>
  Array.from(optionPicker()?.options || [])
    .map((o) => o.text)
    .filter(Boolean);

describe("Final Result is described by its component", () => {
  beforeEach(() => {
    cleanup();
    postSpy.mockReset();
  });

  test("every component may receive the final result, not only numeric ones", async () => {
    renderForm();
    await flush();

    const labels = Array.from(componentPicker().options).map((o) => o.text);

    expect(
      labels,
      "a calculation may write a coded interpretation, so the numeric" +
        " restriction that governs the operands must not apply here",
    ).toEqual(
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

  test("choosing a numeric component takes the coded editor away", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    expect(optionPicker()).not.toBeNull();

    await user.selectOptions(componentPicker(), "comp-a");
    await flush();

    expect(
      optionPicker(),
      "a number is produced by the formula, so no coded value is chosen",
    ).toBeNull();
  });

  test("changing the component clears the value picked under the old one", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    expect(optionPicker()!.value).toBe("x");

    await user.selectOptions(componentPicker(), "comp-c");
    await flush();

    expect(optionPicker()!.value, "x is not one of Component C's options").toBe(
      "",
    );
  });

  test("the chosen component is what gets saved, and the editor's own state is not", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    await user.selectOptions(componentPicker(), "comp-c");
    await flush();
    fireEvent.submit(componentPicker().closest("form") as HTMLFormElement);
    await flush();

    expect(postSpy).toHaveBeenCalled();
    const saved = JSON.parse(postSpy.mock.calls[0][1]);
    expect(saved.componentId, "the component the user chose").toBe("comp-c");
    expect(
      Object.keys(saved),
      "componentPending describes the editor, not the calculation",
    ).not.toContain("componentPending");
  });
});
