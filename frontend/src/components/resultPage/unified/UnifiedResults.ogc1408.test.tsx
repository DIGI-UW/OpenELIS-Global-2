import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * OGC-1408 — a save whose request never got an answer (a dropped connection,
 * a proxy reset, a laptop closed mid-save) reaches the page as status 0. The
 * page read that as a success: the editor closed on a value nothing had
 * stored, the drafts typed with it were discarded, and the technician moved
 * on. The row has to stay in edit with everything typed, and say the result
 * was not saved.
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

/** A numeric result not yet entered: the row the ticket was reproduced on. */
const worklistRow = () => ({
  id: null,
  analysisId: "60",
  testResultComponentId: "c-n2",
  accessionNumber: "DEV01260000000001029",
  testName: "Hemoglobin",
  resultType: "N",
  resultValue: "",
  rawResultValue: "",
  significantDigits: 0,
  analysisStatusId: "4",
  analysisLastupdated: "1700000000000",
  normalRange: "",
  sampleType: "Whole Blood",
});

const LAB_UNITS = [{ id: "165", value: "Hematology", domain: "CLINICAL" }];
const STATUSES = [
  { id: "4", value: "Not started" },
  { id: "15", value: "Accepted by technician" },
];

const NO_RESPONSE = {
  error: "Failed to fetch",
  message: "Failed to fetch",
  status: 0,
};

const renderPage = async () => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (endPoint: string, callback: any) => {
      if (endPoint === "/rest/results-entry/lab-units")
        return callback(LAB_UNITS);
      if (endPoint === "/rest/analysis-status-types") return callback(STATUSES);
      if (endPoint.startsWith("/rest/LogbookResults"))
        return callback({ testResult: [worklistRow()] });
      return callback([]);
    },
  );
  render(
    <IntlProvider locale="en" messages={messages}>
      <UnifiedResults />
    </IntlProvider>,
  );
  fireEvent.change(screen.getByLabelText(/Lab Unit/i), {
    target: { value: "165" },
  });
  expect(document.querySelector("tbody tr")).not.toBeNull();
};

const resultField = () =>
  document.querySelector(
    'input[id^="unifiedResultValue-"]',
  ) as HTMLInputElement;

const noteField = () =>
  document.querySelector('textarea[id^="note-text-"]') as HTMLTextAreaElement;

const answerSaveWith = (response: unknown) =>
  utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
    (_url: string, _body: string, callback: any) => callback(response),
  );

/** The row's own Save: the expanded panel carries a second one. */
const saveButton = () => screen.getAllByTestId("save-button")[0];

const typeAndSave = (value: string) => {
  fireEvent.change(resultField(), { target: { value } });
  fireEvent.click(saveButton());
};

const saveRequests = () =>
  utilsMock.postToOpenElisServerJsonResponse.mock.calls.filter((c: any[]) =>
    String(c[0]).startsWith("/rest/results-entry/analysis/60/result"),
  );

describe("OGC-1408 — a result save that gets no answer", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    notify.addNotification.mockReset();
    window.localStorage.clear();
  });

  it("keeps the row in edit with the typed value and says the result was not saved", async () => {
    await renderPage();
    answerSaveWith(NO_RESPONSE);

    typeAndSave("7");

    expect(saveRequests()).toHaveLength(1);
    expect(resultField().value).toBe("7");
    expect(saveButton()).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /Edit/i }),
    ).not.toBeInTheDocument();
    expect(notify.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: messages["error.results.save.noResponse"],
      }),
    );
  });

  it("keeps the note typed alongside the value", async () => {
    await renderPage();
    answerSaveWith(NO_RESPONSE);
    fireEvent.change(resultField(), { target: { value: "7" } });
    fireEvent.click(screen.getByRole("button", { name: "Expand row" }));
    fireEvent.change(noteField(), { target: { value: "Haemolysed sample" } });

    fireEvent.click(saveButton());

    expect(saveRequests()).toHaveLength(1);
    expect(JSON.parse(saveRequests()[0][1]).testResult.note).toBe(
      "Haemolysed sample",
    );
    expect(noteField().value).toBe("Haemolysed sample");
    expect(resultField().value).toBe("7");
  });

  it("treats a refusal the same way, with the server's own wording", async () => {
    await renderPage();
    answerSaveWith({
      error: "Request failed (HTTP 500 Internal Server Error)",
      message: "Request failed (HTTP 500 Internal Server Error)",
      status: 500,
    });

    typeAndSave("7");

    expect(resultField().value).toBe("7");
    expect(saveButton()).toBeInTheDocument();
    expect(notify.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message: "Request failed (HTTP 500 Internal Server Error)",
      }),
    );
  });

  it("still closes the editor on a save the server answered", async () => {
    await renderPage();
    answerSaveWith({
      resultId: "71",
      analysisStatusId: "15",
      analysisLastupdated: "1700000009999",
      resultValue: "7",
      rawResultValue: "7",
    });

    typeAndSave("7");

    expect(screen.queryByTestId("save-button")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Edit/i })).toBeInTheDocument();
    expect(notify.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "success" }),
    );
  });
});
