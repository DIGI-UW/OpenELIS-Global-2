import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { createMemoryHistory } from "history";
import { Router } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../languages/en.json";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import AnalyserResults, {
  buildHeldResultResolutionUrl,
} from "./AnalyserResults";

const { postResults, getFromServer } = vi.hoisted(() => ({
  postResults: vi.fn(),
  getFromServer: vi.fn(),
}));

vi.mock("../utils/Utils", () => ({
  convertAlphaNumLabNumForDisplay: (value) => value,
  postToOpenElisServerFullResponse: postResults,
  getFromOpenElisServer: getFromServer,
}));

const heldResult = {
  id: "1004",
  analyzerId: "2001",
  accessionNumber: "ACC654321",
  testName: "QUAL_RESULT",
  result: "POSITIVE",
  rawTestCode: "QUAL_RESULT",
  rawResultValue: "POSITIVE",
  importIssueReason: "unknown_analyzer_result_value",
  sourceProfileId: "genexpert-astm",
  sourceProfileRevision: 3,
  sourceProtocol: "ASTM",
  sourceTransport: "TCP",
  readOnly: true,
  isControl: false,
  sampleGroupingNumber: 1,
};

const mappedQualitativeResult = {
  id: "1005",
  analyzerId: "2001",
  accessionNumber: "ACC654321",
  testName: "MTB-RIF",
  result: "1379",
  testResultType: "D",
  dictionaryResultList: [
    { id: "1378", displayValue: "MTB DETECTED" },
    { id: "1379", displayValue: "NOT DETECTED" },
  ],
  readOnly: false,
  isControl: false,
  sampleGroupingNumber: 1,
};

const renderResults = (
  resultList = [heldResult],
  sampleGroup = [resultList[0]],
) => {
  const history = createMemoryHistory({
    initialEntries: ["/AnalyzerResults?id=2001"],
  });
  const addNotification = vi.fn();
  const view = render(
    <Router history={history}>
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider
          value={{ configurationProperties: { AccessionFormat: "" } }}
        >
          <NotificationContext.Provider
            value={{
              setNotificationVisible: vi.fn(),
              addNotification,
            }}
          >
            <AnalyserResults
              results={{ resultList: resultList.map((row) => ({ ...row })) }}
              sampleGroup={sampleGroup}
              analyzerId="2001"
            />
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </IntlProvider>
    </Router>,
  );
  return { ...view, history, addNotification };
};

const matched = (id, group, extra = {}) => ({
  ...mappedQualitativeResult,
  id,
  accessionNumber: "ACC-" + group,
  sampleGroupingNumber: group,
  placement: {
    state: "RESOLVED",
    match: "ACCESSION",
    tubes: [{ sampleItemId: "10", externalId: "ACC-" + group + "-1" }],
    analyses: [{ analysisId: "50", sampleItemId: "10", awaitingResult: true }],
    proposedSampleItemId: "10",
    proposedAnalysisId: "50",
  },
  ...extra,
});

describe("AnalyserResults", () => {
  beforeEach(() => {
    postResults.mockReset();
    getFromServer.mockReset();
  });

  it("ticks only the groupings whose every result is matched when saving all", () => {
    const unordered = matched("2002", 2, {
      placement: {
        state: "UNORDERED_ONE_FITS",
        match: "ACCESSION",
        tubes: [{ sampleItemId: "11", externalId: "ACC-2-1" }],
        analyses: [],
        proposedSampleItemId: "11",
      },
    });
    const first = matched("2001", 1);
    renderResults([first, unordered], [first, unordered]);

    fireEvent.click(screen.getByLabelText("Save All Results"));

    expect(document.getElementById("resultList2001.isAccepted").checked).toBe(
      true,
    );
    expect(document.getElementById("resultList2002.isAccepted").checked).toBe(
      false,
    );
  });

  it("keeps a result held for placement actionable and posts the chosen analysis", () => {
    const held = matched("3001", 1, {
      importIssueReason: "awaiting_placement",
      placement: {
        state: "MULTI_TUBE",
        match: "ACCESSION",
        tubes: [
          { sampleItemId: "10", externalId: "ACC-1-1" },
          { sampleItemId: "11", externalId: "ACC-1-2" },
        ],
        analyses: [
          { analysisId: "50", sampleItemId: "10", awaitingResult: true },
          { analysisId: "51", sampleItemId: "11", awaitingResult: true },
        ],
      },
    });
    renderResults([held]);

    fireEvent.change(screen.getByLabelText("Analysis this result belongs to"), {
      target: { value: "51" },
    });
    fireEvent.click(document.getElementById("resultList3001.isAccepted"));
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    const submitted = JSON.parse(postResults.mock.calls[0][1]);
    expect(submitted.resultList[0].chosenAnalysisId).toBe("51");
    expect(submitted.resultList[0].isAccepted).toBe(true);
  });

  it("posts the order and reason when a result is placed on another order", () => {
    renderResults([matched("6001", 1)]);

    fireEvent.click(
      screen.getByRole("button", { name: "Place on another order" }),
    );
    fireEvent.change(screen.getByLabelText("Order (lab number)"), {
      target: { value: "DEV01260000000000038" },
    });
    fireEvent.change(screen.getByLabelText("Why this belongs to that order"), {
      target: { value: "Label misread" },
    });
    fireEvent.click(document.getElementById("resultList6001.isAccepted"));
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    const submitted = JSON.parse(postResults.mock.calls[0][1]);
    expect(submitted.resultList[0].redirectAccession).toBe(
      "DEV01260000000000038",
    );
    expect(submitted.resultList[0].redirectReason).toBe("Label misread");
  });

  it("shows the specimen ID the instrument sent when it differs from the accession", () => {
    renderResults([matched("4001", 1, { instrumentSpecimenId: "ACC-1-2" })]);
    expect(screen.getByTestId("InstrumentSpecimenId")).toHaveTextContent(
      "Instrument sent: ACC-1-2",
    );
  });

  it("opens the delivery bundle from the row", async () => {
    getFromServer.mockImplementation((url, callback) =>
      callback({ resourceType: "Bundle" }),
    );
    renderResults([matched("5001", 1, { deliveryReceiptId: "receipt-1" })]);

    fireEvent.click(screen.getByRole("button", { name: "View bundle" }));

    expect(await screen.findByTestId("delivery-bundle")).toHaveTextContent(
      '"resourceType": "Bundle"',
    );
    expect(getFromServer.mock.calls[0][0]).toBe(
      "/rest/analyzer/deliveries/receipt-1/bundle",
    );
  });

  it("keeps a held qualitative result visible and links it to the analyzer's own mapping", async () => {
    renderResults();

    expect(await screen.findByText("Held")).toBeInTheDocument();
    expect(screen.getByText("POSITIVE")).toBeInTheDocument();
    expect(screen.getByText("Analyzer code: QUAL_RESULT")).toBeInTheDocument();

    expect(
      screen.getByRole("link", { name: "Review analyzer mapping" }),
    ).toHaveAttribute(
      "href",
      "/analyzers/2001/mapping?returnTo=%2FAnalyzerResults%3Fid%3D2001&focusTest=QUAL_RESULT&focusValue=POSITIVE",
    );

    expect(
      document.getElementById("resultList1004.isAccepted"),
    ).not.toBeInTheDocument();
    expect(
      document.getElementById("resultList1004.isRejected"),
    ).not.toBeInTheDocument();
    expect(
      document.getElementById("resultList1004.isDeleted"),
    ).not.toBeInTheDocument();
  });

  it("accepts a mapped result after a held row in the same group", async () => {
    renderResults(
      [heldResult, mappedQualitativeResult],
      [mappedQualitativeResult],
    );

    fireEvent.click(document.getElementById("resultList1005.isAccepted"));
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    const submitted = JSON.parse(postResults.mock.calls[0][1]);
    expect(submitted.resultList[0].isAccepted).not.toBe(true);
    expect(submitted.resultList[1].isAccepted).toBe(true);
  });

  it("carries unsaved review choices to mapping and back", () => {
    const { history } = renderResults(
      [heldResult, mappedQualitativeResult],
      [mappedQualitativeResult],
    );

    fireEvent.click(document.getElementById("resultList1005.isAccepted"));
    fireEvent.change(document.getElementById("resultList1005.note"), {
      target: { value: "Review before release" },
    });
    fireEvent.click(
      screen.getByRole("link", { name: "Review analyzer mapping" }),
    );

    expect(history.location.pathname).toBe("/analyzers/2001/mapping");
    expect(history.location.state.worklistDraft).toEqual({
      analyzerId: "2001",
      page: 1,
      edits: {
        1005: { isAccepted: true, note: "Review before release" },
      },
    });
    history.goBack();
    expect(history.location.state.worklistDraft.edits[1005]).toEqual({
      isAccepted: true,
      note: "Review before release",
    });
  });

  it("offers acceptance after choosing a specimen for a held mapped result", async () => {
    const result = {
      ...mappedQualitativeResult,
      importIssueReason: "awaiting_specimen",
      readOnly: false,
      sampleTypeOptions: [{ id: "40", value: "Vaginal Swab" }],
    };
    renderResults([result]);

    fireEvent.change(screen.getByRole("combobox", { name: "Sample type" }), {
      target: { value: "40" },
    });
    fireEvent.click(document.getElementById("resultList1005.isAccepted"));
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    const submitted = JSON.parse(postResults.mock.calls[0][1]);
    expect(submitted.resultList[0].typeOfSampleId).toBe("40");
    expect(submitted.resultList[0].isAccepted).toBe(true);
  });

  it("explains a control held for a missing QC target and links to that test's targets", async () => {
    renderResults([
      {
        ...heldResult,
        testId: "301",
        isControl: true,
        importIssueReason: "qc_target_missing",
      },
    ]);
    fireEvent.click(await screen.findByRole("button", { name: /QC Controls/ }));

    expect(
      await screen.findByText(/no QC target to judge it against/),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Set QC target" })).toHaveAttribute(
      "href",
      "/MasterListsPage/TestCatalogEditor/301/qc-targets",
    );
    expect(
      screen.queryByRole("link", { name: "Review analyzer mapping" }),
    ).not.toBeInTheDocument();
  });

  it("shows a failed run's instrument note and dismisses it as a failed run", async () => {
    postResults.mockImplementation((_endpoint, _body, callback) =>
      callback({ status: 200, json: async () => ({ analysisId: "50" }) }),
    );
    renderResults([
      {
        ...heldResult,
        testId: "301",
        rawResultValue: "ERROR",
        importIssueReason: "run_failed",
        instrumentNote: "Error 2008: pressure abort",
      },
    ]);

    expect(
      await screen.findByText(/Error 2008: pressure abort/),
    ).toBeInTheDocument();
    fireEvent.click(
      screen.getByRole("button", { name: "Dismiss as failed run" }),
    );

    expect(postResults.mock.calls[0][0]).toBe(
      "/rest/analyzer/results/1004/failed-run",
    );
    expect(
      screen.queryByRole("link", { name: "Review analyzer mapping" }),
    ).not.toBeInTheDocument();
  });

  it("reports a dismissal that never reached the server", async () => {
    postResults.mockImplementation((_endpoint, _body, callback) =>
      callback(undefined),
    );
    const { addNotification } = renderResults([
      { ...heldResult, importIssueReason: "run_failed" },
    ]);

    fireEvent.click(
      await screen.findByRole("button", { name: "Dismiss as failed run" }),
    );

    await vi.waitFor(() =>
      expect(addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          kind: "error",
          message: "The failed run could not be dismissed",
        }),
      ),
    );
  });

  it("sends one dismissal while the first is still in flight", async () => {
    postResults.mockImplementation(() => {});
    renderResults([{ ...heldResult, importIssueReason: "run_failed" }]);

    const dismiss = await screen.findByRole("button", {
      name: "Dismiss as failed run",
    });
    fireEvent.click(dismiss);
    fireEvent.click(dismiss);

    expect(postResults).toHaveBeenCalledTimes(1);
    expect(dismiss).toBeDisabled();
  });

  it("offers no failed-run dismissal for a result held for its mapping", async () => {
    renderResults();

    await screen.findByText("Held");
    expect(
      screen.queryByRole("button", { name: "Dismiss as failed run" }),
    ).not.toBeInTheDocument();
  });

  it("links an unknown analyzer test to its mapping and named analyzer", () => {
    const url = buildHeldResultResolutionUrl(
      {
        ...heldResult,
        importIssueReason: "unknown_analyzer_test",
        rawResultValue: null,
      },
      "2001",
    );
    expect(url).toContain("/analyzers/2001/mapping");
    expect(url).toContain("focusTest=QUAL_RESULT");
    expect(url).not.toContain("focusValue=");
  });

  it("shows the lab-facing label for a mapped qualitative result", async () => {
    renderResults([mappedQualitativeResult]);

    expect(await screen.findByText("NOT DETECTED")).toBeInTheDocument();
    expect(screen.queryByDisplayValue("1379")).not.toBeInTheDocument();
  });

  it("submits the result selected for acceptance", async () => {
    renderResults([mappedQualitativeResult]);

    const resultRow = await screen.findByRole("row", {
      name: /MTB-RIF NOT DETECTED/,
    });
    fireEvent.click(within(resultRow).getAllByRole("checkbox")[0]);
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(postResults).toHaveBeenCalledTimes(1);
    const submittedResults = JSON.parse(postResults.mock.calls[0][1]);
    expect(submittedResults.resultList[0].isAccepted).toBe(true);
  });

  it("OGC-1417: a retyped value the server refuses as critical is acknowledged and the batch sent again", async () => {
    const glucose = {
      id: "1001",
      analyzerId: "2001",
      accessionNumber: "ACC123456",
      testName: "Glucose",
      result: "5.6",
      testResultType: "N",
      readOnly: false,
      isControl: false,
      sampleGroupingNumber: 1,
    };
    const refusal = {
      code: "ACKNOWLEDGEMENT_REQUIRED",
      customCriticalMessage: "",
      acknowledgementRequired: [
        {
          kind: "CRITICAL",
          value: "25",
          testName: "Glucose",
          accessionNumber: "ACC123456",
          rowId: "1001",
        },
      ],
    };
    const answers = [
      { status: 422, json: () => Promise.resolve(refusal) },
      { status: 200, text: () => Promise.resolve("") },
    ];
    postResults.mockImplementation((url, body, callback) =>
      callback(answers.shift()),
    );
    renderResults([glucose]);

    const resultRow = await screen.findByRole("row", { name: /Glucose/ });
    fireEvent.change(within(resultRow).getByDisplayValue("5.6"), {
      target: { value: "25" },
    });
    fireEvent.click(within(resultRow).getAllByRole("checkbox")[0]);
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(
      await screen.findByTestId("result-alert-critical-message"),
    ).toHaveTextContent(
      messages["label.results.alert.critical.defaultMessage"],
    );
    fireEvent.click(
      screen.getByText("Acknowledge and save", { selector: "button" }),
    );

    expect(postResults).toHaveBeenCalledTimes(2);
    const resent = JSON.parse(postResults.mock.calls[1][1]);
    expect(resent.resultList[0].result).toBe("25");
    expect(resent.resultList[0].criticalAcknowledged).toBe(true);
  });
});
