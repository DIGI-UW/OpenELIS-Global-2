import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
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

const refreshResults = vi.fn();
const notify = vi.fn();
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
              addNotification: notify,
            }}
          >
            <AnalyserResults
              results={{ resultList }}
              sampleGroup={[resultList[0]]}
              analyzerId="2001"
              refreshResults={refreshResults}
            />
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </IntlProvider>
    </MemoryRouter>,
  );

describe("AnalyserResults", () => {
  beforeEach(() => {
    postResults.mockReset();
    refreshResults.mockReset();
    notify.mockReset();
  });

  it.each([0, 1])(
    "explicitly reprocesses a held result and refreshes with held count %i",
    async (resultsHeld) => {
      postResults.mockImplementation((url, body, callback) =>
        callback({
          status: 200,
          json: async () => ({ resultsHeld }),
        }),
      );
      renderResults();
      fireEvent.click(
        await screen.findByRole("button", { name: "Reprocess held result" }),
      );
      expect(postResults).toHaveBeenCalledWith(
        "/rest/analyzer/analyzers/2001/held-results/1004/reprocess",
        "{}",
        expect.any(Function),
      );
      await waitFor(() => expect(refreshResults).toHaveBeenCalledTimes(1));
      expect(notify).toHaveBeenCalledWith(
        expect.objectContaining({
          message:
            messages[
              resultsHeld
                ? "analyzer.results.held.stillHeld"
                : "analyzer.results.held.reprocessSuccess"
            ],
        }),
      );
    },
  );

  it("keeps the result held and reports a failed reprocessing request", async () => {
    postResults.mockImplementation((url, body, callback) =>
      callback({ status: 409 }),
    );
    renderResults();
    fireEvent.click(
      await screen.findByRole("button", { name: "Reprocess held result" }),
    );
    await waitFor(() =>
      expect(notify).toHaveBeenCalledWith(
        expect.objectContaining({
          message: messages["analyzer.results.held.reprocessError"],
        }),
      ),
    );
    expect(refreshResults).not.toHaveBeenCalled();
    expect(screen.getByText("Held")).toBeInTheDocument();
  });

  it("reports malformed responses and makes reprocessing available again", async () => {
    postResults.mockImplementation((url, body, callback) =>
      callback({
        status: 200,
        json: async () => {
          throw new Error("Invalid JSON");
        },
      }),
    );
    renderResults();
    const button = await screen.findByRole("button", {
      name: "Reprocess held result",
    });
    fireEvent.click(button);
    await waitFor(() =>
      expect(notify).toHaveBeenCalledWith(
        expect.objectContaining({
          message: messages["analyzer.results.held.reprocessError"],
        }),
      ),
    );
    expect(button).toBeEnabled();
    expect(refreshResults).not.toHaveBeenCalled();
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
