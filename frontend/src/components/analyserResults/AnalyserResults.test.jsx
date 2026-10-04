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

const { postResults } = vi.hoisted(() => ({ postResults: vi.fn() }));

vi.mock("../utils/Utils", () => ({
  convertAlphaNumLabNumForDisplay: (value) => value,
  postToOpenElisServerFullResponse: postResults,
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
  const view = render(
    <Router history={history}>
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider
          value={{ configurationProperties: { AccessionFormat: "" } }}
        >
          <NotificationContext.Provider
            value={{
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
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
  return { ...view, history };
};

describe("AnalyserResults", () => {
  beforeEach(() => {
    postResults.mockReset();
  });

  it("keeps a held qualitative result visible and links it to the shared mapping editor", async () => {
    renderResults();

    expect(await screen.findByText("Held")).toBeInTheDocument();
    expect(screen.getByText("POSITIVE")).toBeInTheDocument();
    expect(screen.getByText("Analyzer code: QUAL_RESULT")).toBeInTheDocument();

    expect(
      screen.getByRole("link", { name: "Review Analyzer Type mapping" }),
    ).toHaveAttribute(
      "href",
      "/analyzers/types/genexpert-astm/mapping?revision=3&analyzerId=2001&returnTo=%2FAnalyzerResults%3Fid%3D2001&focusTest=QUAL_RESULT&focusValue=POSITIVE",
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
      screen.getByRole("link", { name: "Review Analyzer Type mapping" }),
    );

    expect(history.location.pathname).toBe(
      "/analyzers/types/genexpert-astm/mapping",
    );
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

  it("links an unknown analyzer test to its mapping and named analyzer", () => {
    const url = buildHeldResultResolutionUrl(
      {
        ...heldResult,
        importIssueReason: "unknown_analyzer_test",
        rawResultValue: null,
      },
      "2001",
    );
    expect(url).toContain("analyzerId=2001");
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
