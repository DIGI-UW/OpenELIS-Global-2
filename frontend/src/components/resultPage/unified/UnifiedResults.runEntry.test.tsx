import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * OGC-1200 slice 3 (FR-E10, FR-E10a, FR-E13): a run row opens the Results
 * Entry row of its pending test with the analyzer's value prefilled and
 * editable, and accepting it is the Results Entry save naming the run row.
 */

const { utilsMock, notify } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  },
  notify: { addNotification: vi.fn(), setNotificationVisible: vi.fn() },
}));

vi.mock("../../utils/Utils", () => utilsMock);

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    ...notify,
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: { allowResultRejection: "false" },
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

vi.mock("../../esignature/ESignatureButton", () => ({
  __esModule: true,
  default: ({ children, onSign }: any) => (
    <button type="button" data-testid="save-button" onClick={() => onSign({})}>
      {children}
    </button>
  ),
  SignatureMeaning: { AUTHORED: "AUTHORED", MODIFIED: "MODIFIED" },
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import UnifiedResults, { RunEntry } from "./UnifiedResults";

const row = (overrides = {}) => ({
  id: "r-1",
  analysisId: "61",
  accessionNumber: "DEV0126000000000400",
  testId: "5",
  testName: "Glucose",
  resultType: "N",
  resultValue: "",
  analysisStatusId: "4",
  analysisLastupdated: "1700000000000",
  normalRange: "",
  sampleType: "Serum",
  ...overrides,
});

const CREATININE = row({
  id: "r-2",
  analysisId: "62",
  testId: "9",
  testName: "Creatinine",
  unitsOfMeasure: "mg/dL",
  waitingRun: { runId: 7, label: "RUN-7", source: "ANALYZER" },
});

const entry = (overrides: Partial<RunEntry> = {}): RunEntry => ({
  runId: 7,
  runRowId: "55",
  testId: "9",
  componentId: null,
  value: "1.2",
  units: "mg/dL",
  analyzerId: "9",
  onAccepted: vi.fn(),
  ...overrides,
});

const renderEntry = (rows: any[], runEntry: RunEntry) => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (endPoint: string, callback: any) => {
      if (endPoint.startsWith("/rest/LogbookResults"))
        return callback({ testResult: rows });
      return callback([]);
    },
  );
  render(
    <IntlProvider locale="en" messages={messages}>
      <UnifiedResults
        embedded
        accessionNumber="DEV0126000000000400"
        runEntry={runEntry}
      />
    </IntlProvider>,
  );
};

const resultField = () =>
  document.querySelector(
    'input[id^="unifiedResultValue-"]',
  ) as HTMLInputElement | null;

describe("OGC-1200 slice 3: a run row accepted through Results Entry", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    notify.addNotification.mockReset();
    window.localStorage.clear();
  });

  it("shows only the pending row the value belongs to, prefilled and tagged", () => {
    renderEntry([row(), CREATININE], entry());

    expect(screen.getAllByText("Creatinine").length).toBeGreaterThan(0);
    expect(screen.queryByText("Glucose")).not.toBeInTheDocument();
    expect(resultField()).toHaveValue("1.2");
    expect(screen.getByTestId("run-entry-analyzer-reported")).toHaveTextContent(
      "Analyzer-reported",
    );
    const accept = screen.getAllByTestId("save-button");
    expect(accept.length).toBeGreaterThan(0);
    accept.forEach((button) => expect(button).toHaveTextContent("Accept"));
    expect(
      screen.queryByTestId("run-entry-units-differ"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText(/^All \(/)).not.toBeInTheDocument();
  });

  it("gives the expanded panel's result field an id of its own", () => {
    renderEntry([CREATININE], entry());

    const ids = Array.from(document.querySelectorAll("[id]")).map(
      (element) => element.id,
    );
    expect(ids.filter((id) => id === "unifiedResultValue-62-primary")).toEqual([
      "unifiedResultValue-62-primary",
    ]);
    expect(
      document.getElementById("unifiedPanelResultValue-62-primary"),
    ).toHaveValue("1.2");
  });

  it("prefills a dictionary value with its choice and saves the choice's id", () => {
    const covid = row({
      id: "r-3",
      analysisId: "63",
      testId: "11",
      testName: "SARS-CoV-2",
      resultType: "D",
      dictionaryResults: [
        { id: "1398", value: "Positive" },
        { id: "1399", value: "Negative" },
      ],
    });
    const runEntry = entry({
      testId: "11",
      value: "1398",
      reported: "POS",
      units: null,
    });
    renderEntry([covid], runEntry);
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: any) =>
        callback({
          analysisLastupdated: "1700000000999",
          resultValue: "1398",
          analysisStatusId: "15",
        }),
    );
    const choice = document.querySelector(
      'select[id^="unifiedResultValue-"]',
    ) as HTMLSelectElement;

    expect(choice).toHaveValue("1398");
    expect(
      screen.getByTestId("run-entry-analyzer-reported"),
    ).toBeInTheDocument();
    fireEvent.click(screen.getAllByTestId("save-button")[0]);

    const saves = utilsMock.postToOpenElisServerJsonResponse.mock.calls.filter(
      ([url]: [string]) => url.endsWith("/result"),
    );
    expect(JSON.parse(saves[0][1]).testResult.resultValue).toBe("1398");
  });

  it("names the instrument's own text when a dictionary choice is changed", () => {
    const covid = row({
      id: "r-3",
      analysisId: "63",
      testId: "11",
      testName: "SARS-CoV-2",
      resultType: "D",
      dictionaryResults: [
        { id: "1398", value: "Positive" },
        { id: "1399", value: "Negative" },
      ],
    });
    renderEntry(
      [covid],
      entry({ testId: "11", value: "1398", reported: "POS", units: null }),
    );

    fireEvent.change(
      document.querySelector(
        'select[id^="unifiedResultValue-"]',
      ) as HTMLSelectElement,
      { target: { value: "1399" } },
    );

    expect(screen.getByTestId("run-entry-edited")).toHaveTextContent(
      "Edited, analyzer reported POS",
    );
  });

  it("says what the analyzer reported once the value is edited", () => {
    renderEntry([CREATININE], entry());

    fireEvent.change(resultField()!, { target: { value: "1.3" } });

    expect(screen.getByTestId("run-entry-edited")).toHaveTextContent(
      "Edited, analyzer reported 1.2",
    );
    expect(
      screen.queryByTestId("run-entry-analyzer-reported"),
    ).not.toBeInTheDocument();
  });

  it("accepts through the Results Entry save, naming the run row, and tells the run", () => {
    const runEntry = entry();
    renderEntry([CREATININE], runEntry);
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: any) =>
        callback({
          analysisLastupdated: "1700000000999",
          resultValue: "1.2",
          analysisStatusId: "7",
        }),
    );

    fireEvent.click(screen.getAllByTestId("save-button")[0]);

    const saves = utilsMock.postToOpenElisServerJsonResponse.mock.calls.filter(
      ([url]: [string]) => url.endsWith("/result"),
    );
    expect(saves).toHaveLength(1);
    const [url, body] = saves[0];
    expect(url).toBe("/rest/results-entry/analysis/62/result");
    const sent = JSON.parse(body);
    expect(sent.runRowId).toBe("55");
    expect(sent.testResult.analysisId).toBe("62");
    expect(sent.testResult.resultValue).toBe("1.2");
    expect(sent.testResult.analysisLastupdated).toBe("1700000000000");
    expect(sent.testResult.analyzerId).toBe("9");
    expect(runEntry.onAccepted).toHaveBeenCalledTimes(1);
  });

  it("does not tell the run when the server refuses the value", () => {
    const runEntry = entry();
    renderEntry([CREATININE], runEntry);
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: any) =>
        callback({
          status: 409,
          error:
            "This value is no longer waiting in an open run for this test. Reload the run to see what is left to accept.",
        }),
    );

    fireEvent.click(screen.getAllByTestId("save-button")[0]);

    expect(runEntry.onAccepted).not.toHaveBeenCalled();
    expect(notify.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        message: expect.stringContaining("no longer waiting in an open run"),
      }),
    );
  });

  it("offers nothing when the test is already resulted", () => {
    renderEntry([{ ...CREATININE, resultValue: "1.1" }], entry());

    expect(
      screen.getByText(/No pending test on this order matches this value/),
    ).toBeInTheDocument();
    expect(screen.queryByTestId("save-button")).not.toBeInTheDocument();
  });

  it("flags units that differ between the analyzer and the test", () => {
    renderEntry([CREATININE], entry({ units: "mmol/L" }));

    expect(screen.getByTestId("run-entry-units-differ")).toHaveTextContent(
      "Units differ: analyzer mmol/L, test mg/dL",
    );
  });
});
