/**
 * OGC-1028 (Validation v4 slice V2) — the per-row review panel: read-only
 * summary with Method/Analyzer split, "Before releasing, check:" echo,
 * components in display order, dual-axis note, per-row release and modify.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import ValidationReviewPanel from "./ValidationReviewPanel";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
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

import { postToOpenElisServerJsonResponse } from "../utils/Utils";

const row = (overrides = {}) => ({
  id: 0,
  analysisId: "100",
  accessionNumber: "ACC0",
  testName: "Lead(Serum)",
  normalRange: "10 - 20",
  normal: true,
  qcStatus: "PASS",
  result: "15",
  resultType: "N",
  units: "mg/dL (mass)",
  methodName: "ICP-MS",
  analyzerName: "Leonardo",
  enteredBy: "Tech One",
  enteredDate: "01/06/2025 10:00",
  criticalRange: "< 2 or > 40",
  analysisNotes: [],
  nceOpen: false,
  modified: false,
  ackPending: false,
  nonconforming: false,
  critical: false,
  ...overrides,
});

const renderPanel = (data, props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ValidationReviewPanel
        data={data}
        rows={props.rows || [data]}
        configurationProperties={props.configurationProperties || {}}
        qcAck={props.qcAck || { required: false, satisfied: true }}
        onActionDone={props.onActionDone || vi.fn()}
      />
    </IntlProvider>,
  );

const lastPost = () =>
  postToOpenElisServerJsonResponse.mock.calls[
    postToOpenElisServerJsonResponse.mock.calls.length - 1
  ];

describe("ValidationReviewPanel (OGC-1028)", () => {
  beforeEach(() => {
    postToOpenElisServerJsonResponse.mockReset();
    window.localStorage.clear();
  });

  it("leads with a read-only summary: Method and Analyzer as two fields, entered by/when, ranges, QC", () => {
    renderPanel(row());

    expect(screen.getByTestId("review-method")).toHaveTextContent("ICP-MS");
    expect(screen.getByTestId("review-analyzer")).toHaveTextContent("Leonardo");
    expect(screen.getByTestId("review-entered-by")).toHaveTextContent(
      "Tech One",
    );
    expect(screen.getByTestId("review-entered-date")).toHaveTextContent(
      "01/06/2025 10:00",
    );
    expect(screen.getByTestId("review-critical-range")).toHaveTextContent(
      "< 2 or > 40",
    );
    expect(screen.getByTestId("review-result")).toHaveTextContent("15");
    expect(screen.getByTestId("review-result")).toHaveTextContent("mg/dL");
    expect(screen.getByTestId("flag-NORMAL")).toBeInTheDocument();
    expect(screen.getByTestId("review-qc")).toHaveTextContent("QC passed");
    expect(screen.queryByTestId("review-before-release")).toBeNull();
  });

  it("a missing method or analyzer reads 'Not recorded' instead of blank", () => {
    renderPanel(row({ methodName: null, analyzerName: "" }));
    expect(screen.getByTestId("review-method")).toHaveTextContent(
      "Not recorded",
    );
    expect(screen.getByTestId("review-analyzer")).toHaveTextContent(
      "Not recorded",
    );
  });

  it("echoes the row's signals under 'Before releasing, check:'", () => {
    renderPanel(row({ nceOpen: true, modified: true, normal: false }));
    const checklist = screen.getByTestId("review-before-release");
    expect(checklist).toHaveTextContent("Before releasing, check:");
    expect(checklist).toHaveTextContent("NCE open");
    expect(checklist).toHaveTextContent("Modified");
    expect(screen.getByTestId("flag-ABNORMAL")).toBeInTheDocument();
  });

  it("lists a multi-component analysis in display order, primary first", () => {
    const primary = row({ id: 0 });
    const ctE = row({
      id: 1,
      componentLabel: "Ct E",
      componentDisplayOrder: 2,
      result: "31.2",
    });
    const ctN2 = row({
      id: 2,
      componentLabel: "Ct N2",
      componentDisplayOrder: 1,
      result: "28.7",
    });
    renderPanel(primary, {
      rows: [ctE, primary, ctN2, row({ id: 9, analysisId: "200" })],
    });

    const table = screen.getByTestId("review-components");
    const labels = Array.from(
      table.querySelectorAll("tbody tr td:first-child"),
    ).map((cell) => cell.textContent);
    expect(labels).toEqual(["Primary", "Ct N2", "Ct E"]);
    expect(screen.getByTestId("review-component-2")).toHaveTextContent("28.7");
  });

  it("a single-component row has no components table", () => {
    renderPanel(row());
    expect(screen.queryByTestId("review-components")).toBeNull();
  });

  it("the note defaults to internal; choosing 'Send with result' warns about the report", () => {
    renderPanel(row());
    expect(screen.queryByTestId("review-note-external-warning")).toBeNull();

    fireEvent.click(screen.getByLabelText("Send with result"));

    expect(
      screen.getByTestId("review-note-external-warning"),
    ).toHaveTextContent("This note will appear on the patient report.");
  });

  it("Validate & release posts the row with the Validation note context and chosen visibility", () => {
    const onActionDone = vi.fn();
    renderPanel(row(), { onActionDone });

    fireEvent.change(screen.getByLabelText("Validation note"), {
      target: { value: "Reviewed against previous" },
    });
    fireEvent.click(screen.getByLabelText("Send with result"));
    fireEvent.click(screen.getByText("Validate & release"));

    const [url, body, callback] = lastPost();
    expect(url).toBe("/rest/AccessionValidation/analysis/100/release");
    const payload = JSON.parse(body);
    expect(payload.noteContext).toBe("VALIDATION");
    expect(payload.noteVisibility).toBe("E");
    expect(payload.note).toBe("Reviewed against previous");
    expect(payload.analysisId).toBe("100");

    callback({ outcome: "released", analysisId: "100" });
    expect(onActionDone).toHaveBeenCalledWith("released", expect.anything());
  });

  it("a stale row surfaces the server's 409 as a reload hint", () => {
    const onActionDone = vi.fn();
    renderPanel(row(), { onActionDone });

    fireEvent.click(screen.getByText("Validate & release"));
    lastPost()[2]({ error: "notAwaitingValidation", status: 409 });

    expect(screen.getByTestId("review-error")).toHaveTextContent(
      "This result changed since the page loaded",
    );
    expect(onActionDone).not.toHaveBeenCalled();
  });

  it("unacknowledged failed QC blocks per-row release and says why", () => {
    renderPanel(row(), { qcAck: { required: true, satisfied: false } });
    expect(screen.getByText("Validate & release")).toBeDisabled();
    expect(screen.getByTestId("review-qc-ack-hint")).toHaveTextContent(
      "Acknowledge the failed QC below before releasing.",
    );
  });

  it("Modify needs a reason when the lab requires one, then posts the new value as a modification", () => {
    renderPanel(row(), {
      configurationProperties: { notesRequiredForModifyResults: "true" },
    });

    fireEvent.click(screen.getByTestId("review-modify"));
    const save = screen.getByTestId("review-save-modification");
    expect(save).toBeDisabled();
    expect(screen.queryByText("Validate & release")).toBeNull();

    fireEvent.change(screen.getByLabelText("New result"), {
      target: { value: "16" },
    });
    expect(save).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Reason for modification"), {
      target: { value: "Transcription error" },
    });
    expect(save).not.toBeDisabled();
    fireEvent.click(save);

    const [url, body] = lastPost();
    expect(url).toBe("/rest/AccessionValidation/analysis/100/modify");
    const payload = JSON.parse(body);
    expect(payload.result).toBe("16");
    expect(payload.note).toBe("Transcription error");
    expect(payload.noteContext).toBe("MODIFICATION");
    expect(payload.noteVisibility).toBe("I");
  });

  it("multi-select results are not edited here — the panel points to Results Entry", () => {
    renderPanel(row({ resultType: "M", multiSelectResultValues: "{}" }));
    fireEvent.click(screen.getByTestId("review-modify"));
    expect(screen.getByTestId("review-modify-editor")).toHaveTextContent(
      "edited on the Results Entry page",
    );
    expect(screen.getByTestId("review-save-modification")).toBeDisabled();
  });

  it("refer hands off to Results Entry for this accession", () => {
    renderPanel(row());
    expect(screen.getByTestId("review-refer")).toHaveAttribute(
      "href",
      "/result?type=order&doRange=false&accessionNumber=ACC0",
    );
  });

  it("existing notes show their context and visibility tags", () => {
    renderPanel(
      row({
        analysisNotes: [
          {
            text: "Repeat requested",
            noteType: "I",
            subject: "Result Note (Validation)",
            author: "Val One",
            date: "01/06/2025 11:00",
          },
        ],
      }),
    );
    const list = screen.getByTestId("review-notes-list");
    expect(list).toHaveTextContent("Repeat requested");
    expect(list).toHaveTextContent("Result Note (Validation)");
    expect(list).toHaveTextContent("Internal");
    expect(list).toHaveTextContent("Val One");
  });
});
