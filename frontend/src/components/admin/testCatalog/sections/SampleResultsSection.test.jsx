/**
 * SampleResultsSection — OGC-949 M5 / OGC-749.
 *
 * The network seam (Utils) is mocked; behavior is asserted through rendered DOM
 * and the captured PUT payload (the contract the backend's diff-save consumes).
 * Add/remove are verified through the saved payload — i.e. the change actually
 * reaches the wire — not just a DOM count, so the test fails if the editing
 * wiring breaks.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

vi.mock("../../../layout/Layout", async () => {
  const React = await import("react");
  return {
    NotificationContext: React.createContext({
      addNotification: () => {},
      setNotificationVisible: () => {},
    }),
  };
});

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import SampleResultsSection from "./SampleResultsSection";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const SAMPLE_RESULTS = {
  testId: "7",
  components: [
    {
      id: "C1",
      code: "SYS",
      label: "Systolic",
      displayOrder: 1,
      resultType: "N",
      significantDigits: 0,
      defaultResult: "",
      allowMultipleReadings: false,
      options: [{ id: "O1", value: "Male", sortOrder: 1, normal: true }],
      interpretations: [
        {
          id: "I1",
          valueMatch: ">140",
          text: "High",
          severity: "CRITICAL",
          displayOrder: 1,
        },
      ],
    },
  ],
};

const TWO_COMPONENTS = {
  testId: "7",
  components: [
    {
      id: "C1",
      code: "SYS",
      label: "Systolic",
      displayOrder: 1,
      resultType: "N",
      options: [],
      interpretations: [],
    },
    {
      id: "C2",
      code: "DIA",
      label: "Diastolic",
      displayOrder: 2,
      resultType: "N",
      options: [],
      interpretations: [],
    },
  ],
};

// Deep clone so each test gets a fresh, isolated copy.
const clone = (o) => JSON.parse(JSON.stringify(o));

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <SampleResultsSection testId="7" />
    </IntlProvider>,
  );

const saveButton = () =>
  screen.getByRole("button", { name: messages["label.button.save"] });
const savedPayload = () => JSON.parse(putToOpenElisServer.mock.calls[0][1]);

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/test-list") {
      cb([]);
    } else if (url === "/rest/uom") {
      cb([]);
    } else {
      cb(clone(SAMPLE_RESULTS));
    }
  });
  putToOpenElisServer.mockImplementation((url, body, cb) => cb(200));
  postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
    cb({ components: [] }),
  );
});

describe("SampleResultsSection", () => {
  it("renders loaded components with their options and interpretations", async () => {
    renderSection();
    expect(await screen.findByDisplayValue("SYS")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Systolic")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Male")).toBeInTheDocument(); // option value
    expect(screen.getByDisplayValue(">140")).toBeInTheDocument(); // interpretation match
    expect(screen.getByDisplayValue("High")).toBeInTheDocument(); // interpretation text
  });

  it("shows the empty state when there are no components", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({ testId: "7", components: [] }),
    );
    renderSection();
    expect(
      await screen.findByText(
        messages["label.testCatalog.sampleResults.empty"],
      ),
    ).toBeInTheDocument();
  });

  it("shows an error state when the load fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.editor.loadError"]),
    ).toBeInTheDocument();
  });

  it("saves the full component tree to the section endpoint, coercing numeric fields", async () => {
    renderSection();
    await screen.findByDisplayValue("SYS");

    fireEvent.click(saveButton());

    expect(putToOpenElisServer).toHaveBeenCalledTimes(1);
    expect(putToOpenElisServer.mock.calls[0][0]).toBe(
      "/rest/test-catalog/tests/7/sample-results",
    );
    const payload = savedPayload();
    expect(payload.components[0].code).toBe("SYS");
    // Numeric fields are coerced to numbers, not left as the input's strings.
    expect(payload.components[0].displayOrder).toBe(1);
    expect(payload.components[0].options[0].value).toBe("Male");
    expect(payload.components[0].options[0].sortOrder).toBe(1);
    expect(payload.components[0].interpretations[0].text).toBe("High");
  });

  it("edits a component label and persists the edit", async () => {
    renderSection();
    const labelInput = await screen.findByDisplayValue("Systolic");
    fireEvent.change(labelInput, { target: { value: "Systolic BP" } });

    fireEvent.click(saveButton());
    expect(savedPayload().components[0].label).toBe("Systolic BP");
  });

  it("adds a component and includes it in the saved payload", async () => {
    renderSection();
    await screen.findByDisplayValue("SYS");

    fireEvent.click(screen.getByTestId("add-component"));
    fireEvent.click(saveButton());

    expect(savedPayload().components).toHaveLength(2);
  });

  it("adds an option to a component and includes it in the payload", async () => {
    renderSection();
    await screen.findByDisplayValue("SYS");

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["label.testCatalog.sampleResults.addOption"],
      }),
    );
    fireEvent.click(saveButton());

    expect(savedPayload().components[0].options).toHaveLength(2);
  });

  it("adds an interpretation and includes its fields in the saved payload", async () => {
    const { container } = renderSection();
    await screen.findByDisplayValue("SYS");

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["label.testCatalog.sampleResults.addInterpretation"],
      }),
    );
    // The new (second) interpretation row — fill match, text, and severity.
    fireEvent.change(container.querySelector("#int-match-0-1"), {
      target: { value: "<90" },
    });
    fireEvent.change(container.querySelector("#int-text-0-1"), {
      target: { value: "Low" },
    });
    fireEvent.change(container.querySelector("#int-sev-0-1"), {
      target: { value: "ABNORMAL" },
    });

    fireEvent.click(saveButton());
    const interps = savedPayload().components[0].interpretations;
    expect(interps).toHaveLength(2);
    const added = interps.find((i) => i.text === "Low");
    expect(added.valueMatch).toBe("<90");
    expect(added.severity).toBe("ABNORMAL");
  });

  it("removes a component so it is absent from the saved payload", async () => {
    renderSection();
    await screen.findByDisplayValue("SYS");

    // The remove button carries both an icon and text; match the text and let
    // the click bubble to the button.
    fireEvent.click(
      screen.getByText(
        messages["label.testCatalog.sampleResults.removeComponent"],
      ),
    );
    fireEvent.click(saveButton());

    expect(savedPayload().components).toHaveLength(0);
  });

  it("reorders components and persists the new display order", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb(clone(TWO_COMPONENTS)),
    );
    renderSection();
    await screen.findByDisplayValue("SYS");

    // Two components → two "move up" buttons; the second (DIA) is enabled.
    const upButtons = screen.getAllByRole("button", {
      name: messages["label.testCatalog.sampleResults.moveUp"],
    });
    fireEvent.click(upButtons[1]);

    fireEvent.click(saveButton());
    const payload = savedPayload();
    expect(payload.components[0].code).toBe("DIA");
    expect(payload.components[0].displayOrder).toBe(1);
    expect(payload.components[1].code).toBe("SYS");
    expect(payload.components[1].displayOrder).toBe(2);
  });

  it("picks a unit of measure from the master list and persists it", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/test-list") {
        cb([]);
      } else if (url === "/rest/uom") {
        cb([
          { id: "5", value: "mmHg" },
          { id: "6", value: "mg/dL" },
        ]);
      } else {
        cb(clone(SAMPLE_RESULTS));
      }
    });
    renderSection();
    await screen.findByDisplayValue("SYS");

    fireEvent.change(
      screen.getByLabelText(messages["label.testCatalog.sampleResults.uom"]),
      { target: { value: "5" } },
    );
    fireEvent.click(saveButton());

    expect(savedPayload().components[0].uomId).toBe("5");
  });

  it("copies sample-results configuration from another test", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/test-list") {
        cb([
          { id: "9", value: "Other Test" },
          { id: "7", value: "This Test" },
        ]);
      } else {
        cb(clone(SAMPLE_RESULTS));
      }
    });
    renderSection();
    await screen.findByDisplayValue("SYS");

    fireEvent.change(
      screen.getByLabelText(
        messages["label.testCatalog.sampleResults.copyFrom"],
      ),
      { target: { value: "9" } },
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["label.testCatalog.sampleResults.copyFromButton"],
      }),
    );

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    expect(postToOpenElisServerJsonResponse.mock.calls[0][0]).toBe(
      "/rest/test-catalog/tests/7/sample-results/copy-from/9",
    );
  });
});
