import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import AstEntryPanel from "../AstEntryPanel";
import messages from "../../../languages/en.json";

const isolate = {
  id: "iso-1",
  isolateLabel: "ISO-1",
  significance: "CLINICALLY_SIGNIFICANT",
};

const inProgressRun = {
  id: "run-1",
  isolateId: "iso-1",
  panelId: "panel-1",
  status: "IN_PROGRESS",
  readings: [],
};

const runWithReading = {
  ...inProgressRun,
  readings: [
    {
      id: "reading-1",
      interpretation: "SUSCEPTIBLE",
      method: "MIC",
      rawValue: 4,
    },
    {
      id: "reading-2",
      interpretation: "INTERMEDIATE",
      method: "ZONE",
      rawValue: 16,
    },
  ],
};

const runWithOverride = {
  ...runWithReading,
  readings: [
    {
      ...runWithReading.readings[0],
      overrideInterpretation: "RESISTANT",
    },
    runWithReading.readings[1],
  ],
};

const reviewedRun = {
  ...runWithOverride,
  status: "REVIEWED",
  attemptType: "ORIGINAL",
  method: "MIC",
  reportable: true,
};

const reviewedRepeatRun = {
  ...reviewedRun,
  id: "run-2",
  attemptType: "REPEAT",
  sourceRunId: "run-1",
  attemptReason: "Control failed",
  method: "ZONE",
  reportable: false,
  readings: [
    {
      id: "reading-3",
      interpretation: "RESISTANT",
      method: "ZONE",
      rawValue: 12,
    },
  ],
};

const renderPanel = (service, props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AstEntryPanel
        caseId="case-1"
        workflowType="BACTERIOLOGY"
        isolates={[isolate]}
        service={service}
        saving={false}
        {...props}
      />
    </IntlProvider>,
  );

describe("AstEntryPanel", () => {
  it("records, overrides, and reviews a manual AST run", async () => {
    const service = {
      getAstPanels: vi.fn().mockResolvedValue([
        {
          id: "panel-1",
          label: "Gram negative AST panel",
        },
      ]),
      getAntibiotics: vi.fn().mockResolvedValue([
        {
          id: "abx-1",
          label: "Ciprofloxacin",
        },
      ]),
      getBreakpointStandards: vi.fn().mockResolvedValue([
        { id: "std-clsi", label: "CLSI 2026" },
        { id: "std-eucast", label: "EUCAST 2026" },
      ]),
      getAstRunsForIsolate: vi
        .fn()
        .mockResolvedValueOnce([])
        .mockResolvedValueOnce([inProgressRun])
        .mockResolvedValueOnce([runWithReading])
        .mockResolvedValueOnce([runWithOverride])
        .mockResolvedValueOnce([reviewedRun]),
      getCaseReadiness: vi
        .fn()
        .mockResolvedValueOnce({
          finalReleaseReady: false,
          blockers: ["AST_REVIEW_REQUIRED"],
        })
        .mockResolvedValueOnce({
          finalReleaseReady: false,
          blockers: ["AST_REVIEW_REQUIRED"],
        })
        .mockResolvedValueOnce({
          finalReleaseReady: false,
          blockers: ["AST_REVIEW_REQUIRED"],
        })
        .mockResolvedValueOnce({
          finalReleaseReady: false,
          blockers: ["AST_REVIEW_REQUIRED"],
        })
        .mockResolvedValueOnce({
          finalReleaseReady: true,
          blockers: [],
        }),
      startAstRun: vi.fn().mockResolvedValue(inProgressRun),
      recordAstReading: vi.fn().mockResolvedValue(runWithReading.readings[0]),
      overrideAstReading: vi
        .fn()
        .mockResolvedValue(runWithOverride.readings[0]),
      reviewAstRun: vi.fn().mockResolvedValue(reviewedRun),
    };

    renderPanel(service);

    expect(await screen.findByText("Manual AST")).toBeInTheDocument();
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Start AST run" }),
      ).not.toBeDisabled(),
    );
    fireEvent.change(screen.getByLabelText("Breakpoint standard"), {
      target: { value: "std-eucast" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Start AST run" }));

    await waitFor(() =>
      expect(service.startAstRun).toHaveBeenCalledWith({
        isolateId: "iso-1",
        panelId: "panel-1",
        breakpointStandardId: "std-eucast",
      }),
    );
    expect((await screen.findAllByText("In Progress"))[0]).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Record AST reading" }));

    expect(
      await screen.findByText(
        (_, element) =>
          element?.tagName.toLowerCase() === "strong" &&
          element.textContent === "SUSCEPTIBLE",
      ),
    ).toBeInTheDocument();
    expect(screen.getAllByTestId("microbiology-ast-reading-row")).toHaveLength(
      2,
    );
    expect(
      screen.getAllByTestId("microbiology-ast-reading-row")[1],
    ).toHaveTextContent("INTERMEDIATE");
    fireEvent.change(screen.getByLabelText("Override reason"), {
      target: { value: "mixed growth confirmed on repeat" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Apply override" }));

    expect(await screen.findByText(/RESISTANT/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Review AST run" }));

    expect((await screen.findAllByText("Reviewed"))[0]).toBeInTheDocument();
    expect(screen.getByText("Final release ready")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Start AST run" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Record AST reading" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Apply override" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Review AST run" }),
    ).toBeDisabled();
    expect(service.recordAstReading).toHaveBeenCalledWith("run-1", {
      antibioticId: "abx-1",
      method: "MIC",
      rawValue: "4",
    });
  });

  it("keeps AST write actions disabled when a final case is locked", async () => {
    const service = {
      getAstPanels: vi
        .fn()
        .mockResolvedValue([{ id: "panel-1", label: "Gram negative panel" }]),
      getAntibiotics: vi.fn().mockResolvedValue([]),
      getBreakpointStandards: vi.fn().mockResolvedValue([]),
      getAstRunsForIsolate: vi.fn().mockResolvedValue([]),
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: true,
        blockers: [],
      }),
    };

    renderPanel(service, { readOnly: true });

    expect(
      await screen.findByRole("button", { name: "Start AST run" }),
    ).toBeDisabled();
  });

  it("starts a repeat attempt from a reviewed run with a required reason", async () => {
    const repeatRun = {
      ...reviewedRepeatRun,
      attemptType: "RETEST",
      status: "IN_PROGRESS",
      readings: [],
    };
    const service = {
      getAstPanels: vi
        .fn()
        .mockResolvedValue([{ id: "panel-1", label: "Gram negative panel" }]),
      getAntibiotics: vi.fn().mockResolvedValue([]),
      getBreakpointStandards: vi
        .fn()
        .mockResolvedValue([{ id: "std-clsi", label: "CLSI 2026" }]),
      getAstRunsForIsolate: vi
        .fn()
        .mockResolvedValueOnce([reviewedRun])
        .mockResolvedValueOnce([reviewedRun, repeatRun]),
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: true,
        blockers: [],
      }),
      startRepeatAstRun: vi.fn().mockResolvedValue(repeatRun),
    };

    renderPanel(service);

    expect(await screen.findByText("Original")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Start repeat attempt" }),
    ).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Reason for repeat or retest"), {
      target: { value: "Control failed" },
    });
    fireEvent.click(screen.getByRole("radio", { name: "Retest" }));
    fireEvent.change(screen.getByLabelText("Attempt method"), {
      target: { value: "ZONE" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Start retest attempt" }),
    );

    await waitFor(() =>
      expect(service.startRepeatAstRun).toHaveBeenCalledWith("run-1", {
        attemptType: "RETEST",
        reason: "Control failed",
        method: "ZONE",
      }),
    );
    expect(await screen.findByText("Retest")).toBeInTheDocument();
  });

  it("shows attempt relationships and requires an explicit reportable selection", async () => {
    const noSelectionOriginal = { ...reviewedRun, reportable: false };
    const selectedRepeat = { ...reviewedRepeatRun, reportable: true };
    const service = {
      getAstPanels: vi
        .fn()
        .mockResolvedValue([{ id: "panel-1", label: "Gram negative panel" }]),
      getAntibiotics: vi.fn().mockResolvedValue([]),
      getBreakpointStandards: vi
        .fn()
        .mockResolvedValue([{ id: "std-clsi", label: "CLSI 2026" }]),
      getAstRunsForIsolate: vi
        .fn()
        .mockResolvedValueOnce([noSelectionOriginal, reviewedRepeatRun])
        .mockResolvedValueOnce([noSelectionOriginal, selectedRepeat]),
      getCaseReadiness: vi
        .fn()
        .mockResolvedValueOnce({
          finalReleaseReady: false,
          blockers: ["REPORTABLE_AST_RUN_REQUIRED"],
        })
        .mockResolvedValueOnce({
          finalReleaseReady: true,
          blockers: [],
        }),
      selectReportableAstRun: vi.fn().mockResolvedValue(selectedRepeat),
    };

    renderPanel(service);

    expect((await screen.findAllByText("Repeat"))[0]).toBeInTheDocument();
    expect(screen.getByText("Control failed")).toBeInTheDocument();
    expect(screen.getAllByText("Attempt 1")).toHaveLength(2);
    expect(screen.getByText("Reportable AST Run Required")).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: "Use attempt 2 for reporting" }),
    );

    await waitFor(() =>
      expect(service.selectReportableAstRun).toHaveBeenCalledWith("run-2"),
    );
    expect(await screen.findByText("Included in report")).toBeInTheDocument();
  });

  it("surfaces named AST conflicts returned by the service", async () => {
    const service = {
      getAstPanels: vi.fn().mockResolvedValue([]),
      getAntibiotics: vi.fn().mockResolvedValue([]),
      getBreakpointStandards: vi.fn().mockResolvedValue([]),
      getAstRunsForIsolate: vi
        .fn()
        .mockResolvedValue([
          { ...reviewedRun, reportable: false },
          reviewedRepeatRun,
        ]),
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: false,
        blockers: ["REPORTABLE_AST_RUN_REQUIRED"],
      }),
      selectReportableAstRun: vi.fn().mockResolvedValue({
        status: 409,
        error: "MICROBIOLOGY_AST_CONFLICT",
        message: "AST_SOURCE_RUN_REVIEW_REQUIRED",
      }),
    };

    renderPanel(service);

    fireEvent.click(
      await screen.findByRole("button", {
        name: "Use attempt 2 for reporting",
      }),
    );

    expect(
      await screen.findByText("AST action could not be completed"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("AST Source Run Review Required"),
    ).toBeInTheDocument();
  });
});
