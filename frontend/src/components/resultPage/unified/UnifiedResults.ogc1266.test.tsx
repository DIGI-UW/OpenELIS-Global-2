import React from "react";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * OGC-1266 — saving a result that fires a calculated or reflex rule re-reads
 * the worklist to show the generated analyses. That re-read used to replace
 * every row, so the results a technician had typed on other rows but not yet
 * saved were silently discarded.
 */

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  },
}));

vi.mock("../../utils/Utils", () => utilsMock);

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: { allowResultRejection: "false" },
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

// The e-signature ceremony has its own tests; here it must only stand in for
// the Save control and report the meaning it was handed.
vi.mock("../../esignature/ESignatureButton", () => ({
  __esModule: true,
  default: ({ children, onSign, disabled, meaning }: any) => (
    <button
      type="button"
      data-testid="save-button"
      data-meaning={meaning}
      disabled={disabled}
      onClick={() => onSign({})}
    >
      {children}
    </button>
  ),
  SignatureMeaning: {
    AUTHORED: "AUTHORED",
    MODIFIED: "MODIFIED",
    VALIDATED_AND_RELEASED: "VALIDATED_AND_RELEASED",
    REJECTED: "REJECTED",
  },
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import UnifiedResults from "./UnifiedResults";

const row = (analysisId: string, testName: string, resultValue = "") => ({
  id: "r-" + analysisId,
  analysisId,
  accessionNumber: "DEV01260000000000227",
  testName,
  resultType: "N",
  resultValue,
  rawResultValue: resultValue,
  significantDigits: 1,
  analysisStatusId: "4",
  analysisLastupdated: "1700000000000",
  normalRange: "",
  sampleType: "Serum",
});

describe("OGC-1266 — a rule firing on save keeps other rows' unsaved results", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    window.localStorage.clear();
  });

  it("keeps the value typed on another row after the worklist is re-read", () => {
    let worklist = [
      row("61", "Creatinine(Serum)"),
      row("62", "Hemoglobin(Whole Blood)"),
    ];
    utilsMock.getFromOpenElisServer.mockImplementation(
      (endPoint: string, callback: any) => {
        if (endPoint === "/rest/results-entry/lab-units")
          return callback([
            { id: "56", value: "Biochemistry", domain: "CLINICAL" },
          ]);
        if (endPoint === "/rest/analysis-status-types")
          return callback([{ id: "4", value: "Not started" }]);
        if (endPoint.startsWith("/rest/LogbookResults"))
          return callback({ testResult: worklist });
        return callback([]);
      },
    );
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (url: string, _body: string, callback: any) => {
        if (!url.endsWith("/result")) {
          return callback({});
        }
        worklist = [
          row("61", "Creatinine(Serum)", "6"),
          row("62", "Hemoglobin(Whole Blood)"),
          row("63", "Amylase(Serum)", "12"),
        ];
        callback({ resultId: "91", calculated: ["DEV01260000000000227"] });
      },
    );
    render(
      <IntlProvider locale="en" messages={messages}>
        <UnifiedResults />
      </IntlProvider>,
    );
    fireEvent.change(screen.getByLabelText(/Lab Unit/i), {
      target: { value: "56" },
    });
    const fields = () =>
      Array.from(
        document.querySelectorAll('input[id^="unifiedResultValue-"]'),
      ) as HTMLInputElement[];
    fireEvent.change(fields()[0], { target: { value: "6" } });
    fireEvent.change(fields()[1], { target: { value: "11.9" } });
    fireEvent.click(screen.getAllByTestId("save-button")[0]);

    expect(
      document.querySelectorAll("tbody tr[data-row-key], tbody tr").length,
    ).toBeGreaterThan(2);
    expect(screen.getByText("Amylase(Serum)")).toBeInTheDocument();
    const hemoglobin = fields().find((input) => input.value === "11.9");
    expect(hemoglobin).toBeDefined();
    expect(screen.getAllByTestId("save-button")).toHaveLength(1);
  });
});
