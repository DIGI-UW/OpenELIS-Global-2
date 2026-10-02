/**
 * OGC-1417 — the legacy Results form questions a value outside the valid range
 * only when Result Configuration asks for it (the setting arrives as the
 * string "false", which the form used to read as on), and answers the server's
 * refusal of an unacknowledged critical value with the same modal as the
 * unified page, sending the save again once acknowledged.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import ResultSearchPage from "./SearchResultForm";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const { signed } = vi.hoisted(() => ({ signed: vi.fn() }));

/** Runs onBeforeSign first and signs only when it resolves, as the real button does. */
vi.mock("../esignature/ESignatureButton", () => ({
  default: ({ children, onSign, onBeforeSign, disabled }) => (
    <button
      type="button"
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
        onSign && onSign();
      }}
    >
      {children}
    </button>
  ),
  SignatureMeaning: { AUTHORED: "AUTHORED" },
}));

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

const uricAcid = (overrides = {}) => ({
  ...overrides,
  id: "0",
  analysisId: "992",
  accessionNumber: "DEV01260000000001303",
  testName: "Serum Uric Acid",
  normalRange: "20 - 80",
  resultValue: "",
  resultType: "N",
  reportable: "N",
  lowerNormalRange: 20,
  upperNormalRange: 80,
  lowerAbnormalRange: 0,
  upperAbnormalRange: 100,
  lowerCritical: 10,
  higherCritical: 90,
  resultLimitId: "77",
  ...overrides,
});

let notify;

const renderScreen = (alertWhenInvalid) =>
  render(
    <MemoryRouter initialEntries={["/LogbookResults"]}>
      <ConfigurationContext.Provider
        value={{
          configurationProperties: {
            AccessionFormat: "",
            ALERT_FOR_INVALID_RESULTS: alertWhenInvalid,
          },
        }}
      >
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: notify,
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <ResultSearchPage />
          </IntlProvider>
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>
    </MemoryRouter>,
  );

const valueField = () => document.getElementById("ResultValue0");
const typeAndLeave = (value) => {
  fireEvent.change(valueField(), { target: { value } });
  fireEvent.focusOut(valueField());
};
const modalButton = (name) => screen.getByText(name, { selector: "button" });
const savedRows = (index) =>
  JSON.parse(postToOpenElisServerJsonResponse.mock.calls[index][1]).testResult;

describe("OGC-1417 — legacy Results form", () => {
  beforeEach(() => {
    notify = vi.fn();
    signed.mockReset();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, pathname: "/LogbookResults", search: "" },
    });
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/LogbookResults"))
        return callback({ testResult: [uricAcid()] });
      return callback([]);
    });
    postToOpenElisServerJsonResponse.mockReset();
  });

  it("says nothing about a value outside the valid range when the setting is the string false", async () => {
    renderScreen("false");
    await screen.findByText(/DEV01260000000001303/);

    typeAndLeave("150");

    expect(screen.queryByTestId("result-alert-modal")).not.toBeInTheDocument();
    expect(notify).not.toHaveBeenCalled();
  });

  it("questions it on leaving the field when the setting is on, and saves it confirmed", async () => {
    renderScreen("true");
    await screen.findByText(/DEV01260000000001303/);
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({ reflex: [], calculated: [] }),
    );

    typeAndLeave("150");
    expect(screen.getByTestId("result-alert-item")).toHaveTextContent(
      "(0 to 100)",
    );
    fireEvent.click(modalButton("Keep this value"));
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    // 150 is past the critical bound too, so Save still asks that before signing
    fireEvent.click(
      await screen.findByText("Acknowledge and save", { selector: "button" }),
    );
    await waitFor(() =>
      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1),
    );
    expect(savedRows(0)[0].invalidResultConfirmed).toBe(true);
    expect(savedRows(0)[0].criticalAcknowledged).toBe(true);
  });

  it("answers a refused critical value with the custom message and saves it acknowledged, unchanged otherwise", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/LogbookResults"))
        return callback({ testResult: [uricAcid({ resultLimitId: "" })] });
      return callback([]);
    });
    renderScreen("false");
    await screen.findByText(/DEV01260000000001303/);
    const answers = [
      {
        status: 422,
        code: "ACKNOWLEDGEMENT_REQUIRED",
        customCriticalMessage: "Call the clinician now",
        acknowledgementRequired: [
          {
            kind: "CRITICAL",
            value: "95",
            testName: "Serum Uric Acid",
            analysisId: "992",
          },
        ],
      },
      { reflex: [], calculated: [] },
    ];
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback(answers.shift()),
    );

    typeAndLeave("95");
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    expect(
      await screen.findByTestId("result-alert-critical-message"),
    ).toHaveTextContent("Call the clinician now");
    expect(notify).not.toHaveBeenCalled();
    fireEvent.click(modalButton("Acknowledge and save"));

    await waitFor(() =>
      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(2),
    );
    expect(savedRows(1)[0].criticalAcknowledged).toBe(true);
    expect(savedRows(1)[0].reportable).toBe(false);
    expect(notify).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "success" }),
    );
  });

  it("reports a refused save as a failure in the server's words, not as saved", async () => {
    renderScreen("false");
    await screen.findByText(/DEV01260000000001303/);
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({ status: 500, error: "Update failed" }),
    );

    typeAndLeave("50");
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => expect(notify).toHaveBeenCalled());
    expect(notify).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "error", message: "Update failed" }),
    );
  });

  it("asks for a critical value's acknowledgement before the save is signed, and Correct signs nothing", async () => {
    renderScreen("false");
    await screen.findByText(/DEV01260000000001303/);
    postToOpenElisServerJsonResponse.mockImplementation((url, body, callback) =>
      callback({ reflex: [], calculated: [] }),
    );

    typeAndLeave("95");
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await screen.findByTestId("result-alert-modal");
    expect(signed).not.toHaveBeenCalled();
    fireEvent.click(modalButton("Correct the value"));
    await waitFor(() =>
      expect(screen.queryByTestId("result-alert-modal")).toBeNull(),
    );
    expect(signed).not.toHaveBeenCalled();
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    fireEvent.click(
      await screen.findByText("Acknowledge and save", { selector: "button" }),
    );
    await waitFor(() =>
      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1),
    );
    expect(signed).toHaveBeenCalledTimes(1);
    expect(savedRows(0)[0].criticalAcknowledged).toBe(true);
  });
});
