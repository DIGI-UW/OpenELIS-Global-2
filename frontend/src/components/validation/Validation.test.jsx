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
  return { ...actual, postToOpenElisServer: vi.fn() };
});

vi.mock("../esignature/ESignatureButton", () => ({
  default: ({ children }) => <button type="button">{children}</button>,
  SignatureMeaning: { VALIDATED_AND_RELEASED: "VALIDATED_AND_RELEASED" },
}));

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

const renderValidation = (rows) =>
  render(
    <ConfigurationContext.Provider
      value={{ configurationProperties: { AccessionFormat: "" } }}
    >
      <NotificationContext.Provider
        value={{ setNotificationVisible: vi.fn(), addNotification: vi.fn() }}
      >
        <IntlProvider locale="en" messages={messages}>
          <Validation
            params=""
            results={{ resultList: rows, qcFailureList: [] }}
          />
        </IntlProvider>
      </NotificationContext.Provider>
    </ConfigurationContext.Provider>,
  );

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
