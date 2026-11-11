import React from "react";
import { render, cleanup, fireEvent, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

const PCR_TEST = {
  id: "7",
  value: "Ebola PCR",
  resultType: "D",
  resultTypes: ["D", "N"],
  components: [
    {
      id: "call",
      value: "Target call",
      resultType: "D",
      primary: false,
      resultList: [
        { id: "101", value: "Detected" },
        { id: "102", value: "Not detected" },
      ],
    },
    {
      id: "ic",
      value: "Internal control",
      resultType: "D",
      primary: false,
      resultList: [
        { id: "201", value: "IC present" },
        { id: "202", value: "IC absent" },
      ],
    },
    { id: "ct", value: "Ct", resultType: "N", primary: false },
    {
      id: "result",
      value: "Interpretation",
      resultType: "D",
      primary: true,
      resultList: [
        { id: "301", value: "Positive" },
        { id: "302", value: "Negative" },
      ],
    },
  ],
};

const TEXT_TEST = {
  id: "8",
  value: "Free text note",
  resultType: "A",
  resultTypes: ["A"],
  components: [{ id: "note", value: "Note", resultType: "A", primary: true }],
};

const SAVED_CALCULATION = {
  id: 3,
  name: "Ebola call",
  sampleId: "1",
  testId: "7",
  componentId: "result",
  result: "301",
  note: "",
  toggled: true,
  active: true,
  operations: [
    {
      id: 1,
      order: 0,
      type: "TEST_RESULT",
      value: "7",
      componentId: "call",
      sampleId: "1",
    },
    { id: 2, order: 1, type: "MATH_FUNCTION", value: "==", sampleId: null },
    { id: 3, order: 2, type: "ANSWER", value: "101", sampleId: null },
  ],
};

const { postSpy } = vi.hoisted(() => ({ postSpy: vi.fn() }));

vi.mock("../../../utils/Utils", async () => ({
  resolveApiErrorMessage: (
    (await vi.importActual("../../../utils/Utils")) as any
  ).resolveApiErrorMessage,
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback !== "function") return;
    if (url.startsWith("/rest/test-calculations")) {
      callback([JSON.parse(JSON.stringify(SAVED_CALCULATION))]);
    } else if (url === "/rest/math-functions") {
      callback([{ id: "==", value: "Equals" }]);
    } else if (url === "/rest/displayList/SAMPLE_TYPE_ACTIVE") {
      callback([{ id: "1", value: "Blood" }]);
    } else if (url.startsWith("/rest/test-display-beans-map")) {
      callback({ "1": [PCR_TEST, TEXT_TEST] });
    } else {
      callback([]);
    }
  }),
  postToOpenElisServerFullResponse: vi.fn((url, body, callback) => {
    postSpy(url, body);
    callback(new Response(null, { status: 200 }));
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
  default: ({ id, suggestions }) => (
    <ul data-testid={id}>
      {(suggestions || []).map((s) => (
        <li key={s.id}>{s.value}</li>
      ))}
    </ul>
  ),
}));

import CalculatedValue from "../CalculatedValueForm";

const renderForm = async () => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <CalculatedValue />
    </IntlProvider>,
  );
  await new Promise((r) => setTimeout(r, 0));
};

const select = (id: string) => document.getElementById(id) as HTMLSelectElement;

const optionLabels = (id: string) =>
  Array.from(select(id).options)
    .map((o) => o.text)
    .filter(Boolean);

describe("Calculated Value compares coded results with answers", () => {
  beforeEach(() => {
    cleanup();
    postSpy.mockReset();
  });

  test("a coded component is offered as an operand", async () => {
    await renderForm();

    expect(screen.getByTestId("0_0_testresult")).toHaveTextContent("Ebola PCR");
    expect(screen.getByTestId("0_0_testresult")).not.toHaveTextContent(
      "Free text note",
    );
    expect(optionLabels("0_0_component")).toEqual(
      expect.arrayContaining(["Target call", "Internal control", "Ct"]),
    );
    expect(select("0_0_component").value).toBe("call");
  });

  test("an Answer lists the answers of the coded operand before it", async () => {
    const user = userEvent.setup();
    await renderForm();

    expect(optionLabels("0_2_answer")).toEqual(["Detected", "Not detected"]);

    await user.selectOptions(select("0_0_component"), "ic");
    expect(optionLabels("0_2_answer")).toEqual(["IC present", "IC absent"]);

    await user.selectOptions(select("0_2_addoperation"), "ANSWER");
    expect(optionLabels("0_3_answer")).toEqual(["IC present", "IC absent"]);
  });

  test("a saved rule shows the answer's name and posts its id back", async () => {
    await renderForm();

    expect(
      screen.getByText("Detected", { selector: "div" }),
    ).toBeInTheDocument();
    expect(screen.queryByText("101", { selector: "div" })).toBeNull();
    expect(select("0_2_answer").value).toBe("101");

    fireEvent.submit(select("0_2_answer").closest("form") as HTMLFormElement);

    const saved = JSON.parse(postSpy.mock.calls[0][1]);
    expect(saved.operations[2]).toEqual(
      expect.objectContaining({ type: "ANSWER", value: "101" }),
    );
    expect(saved.operations[0].componentId).toBe("call");
    expect(saved.componentId).toBe("result");
    expect(saved.result).toBe("301");
  });
});
