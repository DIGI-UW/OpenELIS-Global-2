import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * OGC-1417 — on the unified Results page a critical value is acknowledged, and
 * a value outside the valid range confirmed when Result Configuration asks for
 * it, before the save goes through. The custom critical message is shown as
 * entered; the server refuses a save that skips either, and the page answers
 * that refusal with the same modal.
 */

const { utilsMock, notify, config } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  },
  notify: { addNotification: vi.fn(), setNotificationVisible: vi.fn() },
  config: {
    configurationProperties: {
      allowResultRejection: "false",
      ALERT_FOR_INVALID_RESULTS: "false",
      customCriticalMessage: "",
    } as Record<string, string>,
  },
}));

vi.mock("../../utils/Utils", () => utilsMock);

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    ...notify,
  }),
  ConfigurationContext: React.createContext(config),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

const { signed } = vi.hoisted(() => ({ signed: vi.fn() }));

/** Runs onBeforeSign first and signs only when it resolves, as the real button does. */
vi.mock("../../esignature/ESignatureButton", () => ({
  __esModule: true,
  default: ({ children, onSign, onBeforeSign, disabled }: any) => (
    <button
      type="button"
      data-testid="save-button"
      disabled={disabled}
      onClick={async () => {
        try {
          if (onBeforeSign) {
            await onBeforeSign();
          }
        } catch {
          return;
        }
        signed();
        onSign({});
      }}
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

/** Serum Uric Acid as QA configured it: normal 20-80, valid 0-100, critical <10 or >90. */
const worklistRow = () => ({
  id: null,
  analysisId: "992",
  accessionNumber: "DEV01260000000001303",
  testName: "Serum Uric Acid",
  resultType: "N",
  resultValue: "",
  rawResultValue: "",
  significantDigits: 0,
  analysisStatusId: "4",
  analysisLastupdated: "1700000000000",
  resultLimitId: "77",
  lowerNormalRange: 20,
  upperNormalRange: 80,
  lowerAbnormalRange: 0,
  upperAbnormalRange: 100,
  lowerCritical: 10,
  higherCritical: 90,
  sampleType: "Serum",
});

const LAB_UNITS = [{ id: "165", value: "Biochemistry", domain: "CLINICAL" }];
const STATUSES = [{ id: "4", value: "Not started" }];

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

const saveButton = () => screen.getAllByTestId("save-button")[0];

const saveRequests = () =>
  utilsMock.postToOpenElisServerJsonResponse.mock.calls.filter((c: any[]) =>
    String(c[0]).startsWith("/rest/results-entry/analysis/992/result"),
  );

const savedItem = (index: number) =>
  JSON.parse(saveRequests()[index][1]).testResult;

const answerSavesWith = (...responses: unknown[]) => {
  const queue = [...responses];
  utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
    (url: string, _body: string, callback: any) => {
      if (!String(url).startsWith("/rest/results-entry/analysis/992/result")) {
        return callback({});
      }
      return callback(queue.length > 1 ? queue.shift() : queue[0]);
    },
  );
};

/** Carbon keeps its modal out of the accessibility tree in jsdom. */
const modalButton = (name: string) =>
  screen.getByText(name, { selector: "button" });

/** React 17 hears a field being left as focusout. */
const leaveField = () => fireEvent.focusOut(resultField());

const SAVED = { resultId: "501", analysisStatusId: "15" };

describe("OGC-1417 — critical and invalid results on the unified page", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    notify.addNotification.mockReset();
    signed.mockReset();
    window.localStorage.clear();
    config.configurationProperties.ALERT_FOR_INVALID_RESULTS = "false";
    config.configurationProperties.customCriticalMessage = "";
  });

  it("asks for the critical value to be acknowledged, showing the custom message, before anything is saved", async () => {
    config.configurationProperties.customCriticalMessage =
      "QA critical message 1002: call the clinician now";
    await renderPage();
    answerSavesWith(SAVED);

    fireEvent.change(resultField(), { target: { value: "95" } });
    fireEvent.click(saveButton());

    expect(saveRequests()).toHaveLength(0);
    expect(
      screen.getByTestId("result-alert-critical-message"),
    ).toHaveTextContent("QA critical message 1002: call the clinician now");
    expect(screen.getByTestId("result-alert-item")).toHaveTextContent(
      "Serum Uric Acid: 95",
    );

    fireEvent.click(modalButton("Acknowledge and save"));

    await waitFor(() => expect(saveRequests()).toHaveLength(1));
    expect(signed).toHaveBeenCalledTimes(1);
    expect(savedItem(0).criticalAcknowledged).toBe(true);
    expect(savedItem(0).invalidResultConfirmed).toBe(false);
  });

  it("falls back to the translated default when no custom message is set", async () => {
    await renderPage();
    fireEvent.change(resultField(), { target: { value: "5" } });
    fireEvent.click(saveButton());

    expect(
      screen.getByTestId("result-alert-critical-message"),
    ).toHaveTextContent(
      messages["label.results.alert.critical.defaultMessage"],
    );
  });

  it("keeps the typed value and saves nothing when the user goes back to correct it", async () => {
    await renderPage();
    fireEvent.change(resultField(), { target: { value: "95" } });
    fireEvent.click(saveButton());

    await screen.findByTestId("result-alert-modal");
    fireEvent.click(modalButton("Correct the value"));

    await waitFor(() =>
      expect(
        screen.queryByTestId("result-alert-modal"),
      ).not.toBeInTheDocument(),
    );
    expect(signed).not.toHaveBeenCalled();
    expect(saveRequests()).toHaveLength(0);
    expect(screen.queryByTestId("result-alert-modal")).not.toBeInTheDocument();
    expect(resultField().value).toBe("95");
  });

  it("saves an abnormal value without any pop-up", async () => {
    config.configurationProperties.ALERT_FOR_INVALID_RESULTS = "true";
    await renderPage();
    answerSavesWith(SAVED);
    fireEvent.change(resultField(), { target: { value: "85" } });
    leaveField();
    fireEvent.click(saveButton());

    await waitFor(() => expect(saveRequests()).toHaveLength(1));
    expect(screen.queryByTestId("result-alert-modal")).not.toBeInTheDocument();
    expect(savedItem(0).criticalAcknowledged).toBe(false);
  });

  it("questions a value outside the valid range on leaving the field when the setting is on", async () => {
    config.configurationProperties.ALERT_FOR_INVALID_RESULTS = "true";
    await renderPage();
    answerSavesWith(SAVED);
    fireEvent.change(resultField(), { target: { value: "150" } });
    leaveField();

    expect(
      screen.getByText("Value outside the valid range"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("result-alert-item")).toHaveTextContent(
      "(0 to 100)",
    );

    fireEvent.click(modalButton("Keep this value"));
    expect(resultField().value).toBe("150");
    fireEvent.click(saveButton());

    // 150 is past the critical bound too: kept as a value, it still owes its
    // critical acknowledgement, and only that is asked at Save
    expect(await screen.findAllByTestId("result-alert-item")).toHaveLength(1);
    fireEvent.click(modalButton("Acknowledge and save"));
    await waitFor(() => expect(saveRequests()).toHaveLength(1));
    expect(savedItem(0).invalidResultConfirmed).toBe(true);
    expect(savedItem(0).criticalAcknowledged).toBe(true);
  });

  it("does not question the valid range on leaving the field when the setting is off", async () => {
    await renderPage();
    answerSavesWith(SAVED);
    fireEvent.change(resultField(), { target: { value: "110" } });
    leaveField();

    expect(screen.queryByTestId("result-alert-modal")).not.toBeInTheDocument();
    fireEvent.click(saveButton());
    // only the critical bound it is past is asked about
    expect(await screen.findAllByTestId("result-alert-item")).toHaveLength(1);
    fireEvent.click(modalButton("Acknowledge and save"));
    await waitFor(() => expect(saveRequests()).toHaveLength(1));
    expect(savedItem(0).invalidResultConfirmed).toBe(false);
  });

  it("answers the server's refusal with the modal and sends the save again once confirmed", async () => {
    await renderPage();
    answerSavesWith(
      {
        status: 422,
        code: "ACKNOWLEDGEMENT_REQUIRED",
        error: "Acknowledge it before saving.",
        customCriticalMessage: "",
        acknowledgementRequired: [
          {
            kind: "INVALID",
            value: "150",
            testName: "Serum Uric Acid",
            accessionNumber: "DEV01260000000001303",
            lowValid: 0,
            highValid: 100,
            analysisId: "992",
          },
        ],
      },
      SAVED,
    );
    fireEvent.change(resultField(), { target: { value: "88" } });
    fireEvent.click(saveButton());

    await waitFor(() => expect(saveRequests()).toHaveLength(1));
    expect(notify.addNotification).not.toHaveBeenCalled();
    fireEvent.click(
      await screen.findByText("Confirm and save", { selector: "button" }),
    );

    await waitFor(() => expect(saveRequests()).toHaveLength(2));
    expect(savedItem(1).invalidResultConfirmed).toBe(true);
    expect(notify.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "success" }),
    );
  });

  it("asks for the critical acknowledgement of a value beyond the valid range too, even with the invalid alert off", async () => {
    await renderPage();
    answerSavesWith(SAVED);
    fireEvent.change(resultField(), { target: { value: "150" } });
    fireEvent.click(saveButton());

    const items = await screen.findAllByTestId("result-alert-item");
    expect(items).toHaveLength(1);
    expect(screen.getByTestId("result-alert-critical-message")).toBeVisible();
    fireEvent.click(modalButton("Acknowledge and save"));
    await waitFor(() => expect(saveRequests()).toHaveLength(1));
    expect(savedItem(0).criticalAcknowledged).toBe(true);
    expect(savedItem(0).invalidResultConfirmed).toBe(false);
  });

  it("reads a value written with a comparator as the number it carries", async () => {
    await renderPage();
    answerSavesWith(SAVED);
    fireEvent.change(resultField(), { target: { value: "<5" } });
    fireEvent.click(saveButton());

    await screen.findByTestId("result-alert-critical-message");
    fireEvent.click(modalButton("Acknowledge and save"));
    await waitFor(() => expect(saveRequests()).toHaveLength(1));
    expect(savedItem(0).criticalAcknowledged).toBe(true);
  });

  it("does not swallow Save when the field is left for the Save button", async () => {
    config.configurationProperties.ALERT_FOR_INVALID_RESULTS = "true";
    await renderPage();
    answerSavesWith(SAVED);
    fireEvent.change(resultField(), { target: { value: "105" } });
    fireEvent.focusOut(resultField(), { relatedTarget: saveButton() });

    expect(screen.queryByTestId("result-alert-modal")).not.toBeInTheDocument();
    fireEvent.click(saveButton());
    // 105 is past the valid range and past the critical bound: one pop-up asks both
    expect(await screen.findAllByTestId("result-alert-item")).toHaveLength(2);
    fireEvent.click(modalButton("Acknowledge and save"));
    await waitFor(() => expect(saveRequests()).toHaveLength(1));
    expect(savedItem(0).invalidResultConfirmed).toBe(true);
    expect(savedItem(0).criticalAcknowledged).toBe(true);
  });
});
