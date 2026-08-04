import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import AmendmentHistoryPanel from "../AmendmentHistoryPanel";
import messages from "../../../languages/en.json";

const renderPanel = ({
  finalReleaseState = "FINAL_RELEASED",
  service,
  onCaseUpdated = vi.fn(),
} = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AmendmentHistoryPanel
        caseId="case-1"
        finalReleaseState={finalReleaseState}
        service={service}
        onCaseUpdated={onCaseUpdated}
      />
    </IntlProvider>,
  );

const baseService = () => ({
  getCaseAmendments: vi.fn().mockResolvedValue([]),
  getCaseReportVersions: vi.fn().mockResolvedValue([]),
  openCaseAmendment: vi.fn(),
  cancelCaseAmendment: vi.fn(),
  releaseAmendedReport: vi.fn(),
});

describe("AmendmentHistoryPanel", () => {
  it("requires a reason, opens an amendment, and restores focus", async () => {
    const service = baseService();
    const onCaseUpdated = vi.fn().mockResolvedValue();
    service.openCaseAmendment.mockResolvedValue({
      id: "amendment-1",
      sequenceNumber: 1,
      status: "OPEN",
      reason: "Correct organism identification",
    });

    renderPanel({ service, onCaseUpdated });

    const openButton = await screen.findByRole("button", {
      name: "Open amendment",
    });
    expect(openButton).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Amendment reason"), {
      target: { value: "Correct organism identification" },
    });
    fireEvent.click(openButton);

    await waitFor(() =>
      expect(service.openCaseAmendment).toHaveBeenCalledWith("case-1", {
        reason: "Correct organism identification",
      }),
    );
    await waitFor(() => expect(onCaseUpdated).toHaveBeenCalled());
    expect(screen.getByRole("heading", { name: "Amendments" })).toHaveFocus();
  });

  it("renders amendment and immutable report-version history", async () => {
    const service = baseService();
    service.getCaseAmendments.mockResolvedValue([
      {
        id: "amendment-1",
        sequenceNumber: 1,
        status: "RELEASED",
        reason: "Correct organism identification",
        openedAt: "2026-08-03T12:00:00Z",
        openedBy: "reviewer",
      },
    ]);
    service.getCaseReportVersions.mockResolvedValue([
      {
        id: "version-1",
        versionNumber: 1,
        releaseType: "FINAL",
        content: "Escherichia coli: Ciprofloxacin S",
      },
      {
        id: "version-2",
        versionNumber: 2,
        releaseType: "AMENDED_FINAL",
        content: "Klebsiella pneumoniae: Ciprofloxacin R",
        correctsVersionId: "version-1",
      },
    ]);

    renderPanel({ service });

    expect(
      await screen.findByText("Correct organism identification"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Escherichia coli: Ciprofloxacin S"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Klebsiella pneumoniae: Ciprofloxacin R"),
    ).toBeInTheDocument();
    expect(screen.getByText("Version 1")).toBeInTheDocument();
    expect(screen.getByText("Version 2")).toBeInTheDocument();
  });

  it("requires a cancellation reason before relocking without release", async () => {
    const service = baseService();
    const onCaseUpdated = vi.fn().mockResolvedValue();
    service.cancelCaseAmendment.mockResolvedValue({
      id: "amendment-1",
      status: "CANCELLED",
      closingReason: "Correction no longer needed",
    });

    renderPanel({
      finalReleaseState: "AMENDMENT_IN_PROGRESS",
      service,
      onCaseUpdated,
    });

    const cancelButton = await screen.findByRole("button", {
      name: /Cancel amendment/,
    });
    expect(cancelButton).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Cancellation reason"), {
      target: { value: "Correction no longer needed" },
    });
    fireEvent.click(cancelButton);

    await waitFor(() =>
      expect(service.cancelCaseAmendment).toHaveBeenCalledWith("case-1", {
        reason: "Correction no longer needed",
      }),
    );
    await waitFor(() => expect(onCaseUpdated).toHaveBeenCalled());
  });

  it("shows lifecycle errors and only relocks after a successful amended release", async () => {
    const service = baseService();
    const onCaseUpdated = vi.fn().mockResolvedValue();
    service.releaseAmendedReport
      .mockResolvedValueOnce({
        status: 409,
        message: "REPORTABLE_AST_RUN_REQUIRED",
      })
      .mockResolvedValueOnce({ finalReleaseState: "FINAL_RELEASED" });

    renderPanel({
      finalReleaseState: "AMENDMENT_IN_PROGRESS",
      service,
      onCaseUpdated,
    });

    const releaseButton = await screen.findByRole("button", {
      name: "Release amended report",
    });
    fireEvent.click(releaseButton);

    expect(
      await screen.findByText("Reportable AST Run Required"),
    ).toBeInTheDocument();
    expect(onCaseUpdated).not.toHaveBeenCalled();

    fireEvent.click(releaseButton);
    await waitFor(() => expect(onCaseUpdated).toHaveBeenCalled());
    expect(screen.getByRole("heading", { name: "Amendments" })).toHaveFocus();
  });
});
