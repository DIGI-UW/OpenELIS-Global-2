import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import AnalyserResults from "./AnalyserResults";

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

const renderResults = (resultList = [heldResult]) =>
  render(
    <MemoryRouter initialEntries={["/AnalyzerResults?id=2001"]}>
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
              results={{ resultList }}
              sampleGroup={[resultList[0]]}
              analyzerId="2001"
            />
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </IntlProvider>
    </MemoryRouter>,
  );

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
      "/analyzers/types/genexpert-astm/mapping?revision=3&returnTo=%2FAnalyzerResults%3Fid%3D2001&focusTest=QUAL_RESULT&focusValue=POSITIVE",
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
});
