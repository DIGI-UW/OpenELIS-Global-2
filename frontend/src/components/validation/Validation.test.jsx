/**
 * OGC-1027 (Validation v4 slice V1) — the triage surface on the Validation page:
 * "Check before release" chips render only for rows carrying risk, filter chips
 * carry live counts over the whole queue, and a filter narrows the visible rows.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import Validation from "./Validation";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    postToOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    getFromOpenElisServer: vi.fn(),
  };
});

vi.mock("../esignature/ESignatureButton", () => ({
  default: ({ children, onSign, disabled }) => (
    <button
      type="button"
      disabled={disabled}
      onClick={() => onSign && onSign()}
    >
      {children}
    </button>
  ),
  SignatureMeaning: { VALIDATED_AND_RELEASED: "VALIDATED_AND_RELEASED" },
}));

import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

const row = (id, overrides = {}) => ({
  id,
  analysisId: String(100 + id),
  accessionNumber: `ACC${id}`,
  testName: `Test ${id}(Serum)`,
  normalRange: "10 - 20",
  normal: true,
  qcStatus: "PASS",
  result: "15",
  resultType: "N",
  nceOpen: false,
  modified: false,
  ackPending: false,
  nonconforming: false,
  critical: false,
  ...overrides,
});

const renderValidation = (
  rows,
  configurationProperties = { AccessionFormat: "" },
  params = "",
) =>
  render(
    <ConfigurationContext.Provider value={{ configurationProperties }}>
      <NotificationContext.Provider
        value={{ setNotificationVisible: vi.fn(), addNotification: vi.fn() }}
      >
        <IntlProvider locale="en" messages={messages}>
          <Validation
            params={params}
            results={{ resultList: rows, qcFailureList: [] }}
          />
        </IntlProvider>
      </NotificationContext.Provider>
    </ConfigurationContext.Provider>,
  );

const BULK_ON = { AccessionFormat: "", ALLOW_BULK_RELEASE_CLEAR: "true" };

describe("Validation — Check before release (OGC-1027)", () => {
  it("renders a chip only for rows carrying a signal; a clean row is blank", () => {
    renderValidation([row(0), row(1, { nceOpen: true, modified: true })]);

    expect(screen.queryByTestId("check-before-release-0")).toBeNull();
    const flagged = screen.getByTestId("check-before-release-1");
    expect(flagged).toHaveTextContent("NCE open");
    expect(flagged).toHaveTextContent("Modified");
    expect(flagged).not.toHaveTextContent("Ack pending");
  });

  it("filter chips carry live counts computed over the whole queue", () => {
    renderValidation([
      row(0),
      row(1, { nceOpen: true }),
      row(2, { normal: false }),
    ]);

    expect(screen.getByTestId("triage-filter-all")).toHaveTextContent(
      "All (3)",
    );
    expect(screen.getByTestId("triage-filter-needsReview")).toHaveTextContent(
      "Needs review (2)",
    );
    expect(screen.getByTestId("triage-filter-nce")).toHaveTextContent(
      "NCE (1)",
    );
    expect(screen.getByTestId("triage-filter-abnormal")).toHaveTextContent(
      "Abnormal (1)",
    );
    expect(screen.getByTestId("validation-lane-summary")).toHaveTextContent(
      "Clear: 1",
    );
    expect(screen.getByTestId("validation-lane-summary")).toHaveTextContent(
      "Needs review: 2",
    );
  });

  it("a filter narrows the visible rows but leaves the counts alone", () => {
    renderValidation([row(0), row(1, { nceOpen: true })]);
    expect(screen.getByText("ACC0")).toBeInTheDocument();
    expect(screen.getByText("ACC1")).toBeInTheDocument();

    fireEvent.click(screen.getByTestId("triage-filter-nce"));

    expect(screen.queryByText("ACC0")).toBeNull();
    expect(screen.getByText("ACC1")).toBeInTheDocument();
    expect(screen.getByTestId("triage-filter-all")).toHaveTextContent(
      "All (2)",
    );
    expect(screen.getByTestId("triage-filter-nce")).toHaveAttribute(
      "aria-pressed",
      "true",
    );
  });

  it("expanding a row opens its review panel (OGC-1028)", () => {
    renderValidation([
      row(0, { methodName: "ICP-MS", analyzerName: "Leonardo" }),
      row(1),
    ]);
    expect(screen.queryByTestId("validation-review-panel-0")).toBeNull();

    fireEvent.click(screen.getAllByRole("button", { name: /expand row/i })[0]);

    const panel = screen.getByTestId("validation-review-panel-0");
    expect(panel).toBeInTheDocument();
    expect(panel).toHaveTextContent("ICP-MS");
    expect(panel).toHaveTextContent("Leonardo");
    expect(screen.queryByTestId("validation-review-panel-1")).toBeNull();
  });

  it("the queue has no Notes input of its own; the panel's note feeds the batch release (OGC-1028)", () => {
    const results = { resultList: [row(0)], qcFailureList: [] };
    render(
      <ConfigurationContext.Provider
        value={{ configurationProperties: { AccessionFormat: "" } }}
      >
        <NotificationContext.Provider
          value={{ setNotificationVisible: vi.fn(), addNotification: vi.fn() }}
        >
          <IntlProvider locale="en" messages={messages}>
            <Validation params="" results={results} />
          </IntlProvider>
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>,
    );
    expect(screen.queryByRole("textbox")).toBeNull();

    fireEvent.click(screen.getAllByRole("button", { name: /expand row/i })[0]);
    const composer = screen.getByLabelText("Validation note");
    expect(screen.getAllByRole("textbox")).toHaveLength(1);

    fireEvent.change(composer, { target: { value: "Batch note" } });

    expect(results.resultList[0].note).toBe("Batch note");
    expect(results.resultList[0].noteVisibility).toBe("E");
    expect(results.resultList[0].noteContext).toBe("VALIDATION");
  });

  it("'Release all clear' is absent when the lab has bulk release turned off (OGC-1029)", () => {
    renderValidation([row(0)]);
    expect(screen.queryByTestId("release-all-clear")).toBeNull();
    expect(screen.getByTestId("release-all-clear-disabled")).toHaveTextContent(
      "Bulk release is turned off",
    );
  });

  it("'Release all clear (N)' counts the Clear lane and is disabled when it is empty (OGC-1029)", () => {
    renderValidation(
      [row(0, { nceOpen: true }), row(1, { normal: false })],
      BULK_ON,
    );
    const button = screen.getByTestId("release-all-clear");
    expect(button).toHaveTextContent("Release all clear (0)");
    expect(button).toBeDisabled();
  });

  it("the confirm list holds only Clear-lane rows and the signed release posts them with the page's search key (OGC-1029)", () => {
    postToOpenElisServerJsonResponse.mockReset();
    renderValidation(
      [row(0), row(1, { nceOpen: true }), row(2)],
      BULK_ON,
      "?type=order&accessionNumber=ACC0",
    );
    const button = screen.getByTestId("release-all-clear");
    expect(button).toHaveTextContent("Release all clear (2)");

    fireEvent.click(button);
    expect(screen.getByTestId("release-all-clear-row-0")).toBeInTheDocument();
    expect(screen.getByTestId("release-all-clear-row-2")).toBeInTheDocument();
    expect(screen.queryByTestId("release-all-clear-row-1")).toBeNull();

    fireEvent.click(
      screen.getByTestId("release-all-clear-sign").querySelector("button"),
    );
    const [url, body] = postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/AccessionValidation/release-clear");
    const payload = JSON.parse(body);
    expect(payload.doRange).toBe(false);
    expect(payload.accessionNumber).toBe("ACC0");
    expect(payload.rows.map((r) => r.analysisId)).toEqual(["100", "102"]);
  });

  it("a needs-review row offers 'Review →' which opens its panel (OGC-1029)", () => {
    renderValidation([row(0), row(1, { normal: false })], BULK_ON);
    expect(screen.queryByTestId("review-row-0")).toBeNull();
    expect(screen.queryByTestId("validation-review-panel-1")).toBeNull();

    fireEvent.click(screen.getByTestId("review-row-1"));

    expect(screen.getByTestId("validation-review-panel-1")).toBeInTheDocument();
  });

  it("the legacy per-row Validate/Retest checkboxes and batch button are gone (OGC-1030)", () => {
    renderValidation([row(0)], BULK_ON);
    expect(screen.queryAllByRole("checkbox")).toHaveLength(0);
    expect(screen.queryByText(/^Validate$/)).toBeNull();
  });

  it("'Include auto-validated' fetches the accession's auto-validated rows into a read-only list (OGC-1030)", () => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/AccessionValidation/auto-validated")) {
        callback([
          {
            analysisId: "900",
            accessionNumber: "ACC0",
            testName: "Auto Test(Serum)",
            result: "7",
            normalRange: "5 - 9",
            resultDate: "01/09/2026",
            autoValidated: true,
          },
        ]);
      }
    });
    renderValidation([row(0)], BULK_ON, "?type=order&accessionNumber=ACC0");
    expect(screen.queryByTestId("auto-validated-section")).toBeNull();
    const toggle = screen.getByTestId("auto-validated-toggle");
    expect(toggle).toHaveTextContent("Include auto-validated");

    fireEvent.click(toggle.querySelector('[role="switch"], button, input'));

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/AccessionValidation/auto-validated?accessionNumber=ACC0",
      expect.any(Function),
    );
    expect(screen.getByTestId("auto-validated-row-900")).toHaveTextContent(
      "Auto Test(Serum)",
    );
    expect(screen.getByTestId("auto-validated-row-900")).toHaveTextContent(
      "Auto-validated",
    );
    expect(screen.queryAllByRole("checkbox")).toHaveLength(0);
  });

  it("fail-safe: QC not evaluated keeps a chip-less row out of the Clear lane", () => {
    renderValidation([row(0, { qcStatus: "UNKNOWN" })]);

    expect(screen.queryByTestId("check-before-release-0")).toBeNull();
    expect(screen.getByTestId("validation-lane-summary")).toHaveTextContent(
      "Clear: 0",
    );
    expect(screen.getByTestId("validation-lane-summary")).toHaveTextContent(
      "Needs review: 1",
    );
  });
});
