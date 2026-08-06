import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import AstEntryPanel from "../AstEntryPanel";
import messages from "../../../languages/en.json";

const isolate = {
  id: "iso-1",
  isolateLabel: "ISO-1",
  significance: "CLINICALLY_SIGNIFICANT",
  organismId: "organism-1",
  identificationStatus: "CONFIRMED",
};

const inProgressRun = {
  id: "run-1",
  isolateId: "iso-1",
  panelId: "panel-1",
  technique: "VITEK_2",
  measurementType: "MIC",
  status: "IN_PROGRESS",
  orderedAntibiotics: [
    {
      antibioticId: "abx-1",
      displayOrder: 1,
      tier: 1,
      reportBehavior: "ALWAYS",
    },
  ],
  readings: [],
};

const runWithReading = {
  ...inProgressRun,
  readings: [
    {
      id: "reading-1",
      antibioticId: "abx-1",
      interpretation: "SUSCEPTIBLE",
      method: "MIC",
      rawValue: 4,
      source: "MANUAL_ENTRY",
      matchedBy: "ORGANISM",
      units: "ug/mL",
    },
    {
      id: "reading-2",
      antibioticId: "abx-1",
      interpretation: "INTERMEDIATE",
      method: "ZONE",
      rawValue: 16,
      source: "MANUAL_ENTRY",
      matchedBy: "NONE",
      units: "mm",
    },
  ],
};

const runWithOverride = {
  ...runWithReading,
  readings: [
    {
      ...runWithReading.readings[0],
      overrideInterpretation: "RESISTANT",
      overrideReason: "Clinical exception",
      overrideHistory: [
        {
          id: "override-1",
          action: "OVERRIDE",
          fromInterpretation: "SUSCEPTIBLE",
          toInterpretation: "RESISTANT",
          reason: "Clinical exception",
          performedAt: "2026-08-05T18:30:00Z",
          performedByDisplay: "Olivia Mendez",
        },
      ],
    },
    runWithReading.readings[1],
  ],
};

const reviewedRun = {
  ...runWithOverride,
  status: "REVIEWED",
  attemptType: "ORIGINAL",
  technique: "VITEK_2",
  measurementType: "MIC",
  reportable: true,
};

const reviewedRepeatRun = {
  ...reviewedRun,
  id: "run-2",
  attemptType: "REPEAT",
  sourceRunId: "run-1",
  attemptReason: "Control failed",
  technique: "DISK_DIFFUSION",
  measurementType: "ZONE",
  reportable: false,
  readings: [
    {
      id: "reading-3",
      antibioticId: "abx-1",
      interpretation: "RESISTANT",
      method: "ZONE",
      rawValue: 12,
    },
  ],
};

const reagentRequirements = [
  {
    analysisId: "41",
    testId: "22",
    testName: "Blood culture",
    linkId: "link-1",
    reagentName: "AST card",
    usageType: "SECONDARY",
    quantityPerTest: 1,
    quantityUnit: "card",
    lots: [
      {
        id: 17,
        lotNumber: "AST-FIFO",
        effectiveExpirationDate: "2026-10-01T00:00:00Z",
        currentQuantity: 8,
        status: "ACTIVE",
        qcStatus: "PASSED",
        available: true,
        fefoRecommended: true,
      },
    ],
  },
];

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
  it("shows immutable override history and requires a reason to revert", async () => {
    const user = userEvent.setup();
    const service = {
      getAstPanels: vi
        .fn()
        .mockResolvedValue([
          { id: "panel-1", label: "Gram negative AST panel" },
        ]),
      getAstSetupForIsolate: vi.fn().mockResolvedValue({
        isolateId: "iso-1",
        orderedPanelId: "panel-1",
        orderedPanelLabel: "Gram negative AST panel",
        orderedPanelVersion: 3,
        panelProvenance: "ORGANISM_DEFAULT",
      }),
      getAntibiotics: vi
        .fn()
        .mockResolvedValue([{ id: "abx-1", label: "Ciprofloxacin" }]),
      getBreakpointStandards: vi
        .fn()
        .mockResolvedValue([{ id: "std-clsi", label: "CLSI 2026" }]),
      getAstRunsForIsolate: vi
        .fn()
        .mockResolvedValueOnce([runWithOverride])
        .mockResolvedValueOnce([runWithReading]),
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: false,
        blockers: ["AST_REVIEW_REQUIRED"],
      }),
      revertAstOverride: vi.fn().mockResolvedValue(runWithReading.readings[0]),
    };

    renderPanel(service);

    await user.click(
      await screen.findByRole("button", { name: "Show original" }),
    );
    expect(screen.getByText(/Susceptible to Resistant/)).toBeInTheDocument();
    expect(screen.getByText(/Olivia Mendez/)).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /Revert to original/ }),
    ).toBeDisabled();
    await user.type(
      screen.getByLabelText("Reason for reverting override"),
      "Repeat confirmed original",
    );
    await user.click(
      screen.getByRole("button", { name: /Revert to original/ }),
    );

    await waitFor(() =>
      expect(service.revertAstOverride).toHaveBeenCalledWith("reading-1", {
        overrideReason: "Repeat confirmed original",
      }),
    );
  });

  it("records, overrides, and reviews a manual AST run", async () => {
    const user = userEvent.setup();
    const service = {
      getAstPanels: vi.fn().mockResolvedValue([
        {
          id: "panel-1",
          label: "Gram negative AST panel",
        },
      ]),
      getAstSetupForIsolate: vi.fn().mockResolvedValue({
        isolateId: "iso-1",
        orderedPanelId: "panel-1",
        orderedPanelLabel: "Gram negative AST panel",
        orderedPanelVersion: 3,
        panelProvenance: "ORGANISM_DEFAULT",
      }),
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

    renderPanel(service, { reagentRequirements });

    expect(await screen.findByText("Manual AST")).toBeInTheDocument();
    expect(
      await screen.findByText("Gram negative AST panel v3"),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Start AST run" }),
      ).not.toBeDisabled(),
    );
    await user.selectOptions(
      screen.getByLabelText("Breakpoint standard"),
      "std-eucast",
    );
    await user.click(screen.getByLabelText(/AST-FIFO/));
    await user.click(screen.getByRole("button", { name: "Start AST run" }));

    await waitFor(() =>
      expect(service.startAstRun).toHaveBeenCalledWith({
        isolateId: "iso-1",
        panelId: "panel-1",
        breakpointStandardId: "std-eucast",
        technique: "VITEK_2",
        lotSelections: [
          {
            analysisId: "41",
            testReagentLinkId: "link-1",
            lotId: 17,
          },
        ],
      }),
    );
    expect((await screen.findAllByText("In Progress"))[0]).toBeInTheDocument();
    await user.click(
      screen.getByRole("button", { name: "Record AST reading" }),
    );

    expect(
      await screen.findByText(
        (_, element) =>
          element?.tagName.toLowerCase() === "strong" &&
          element.textContent === "Susceptible",
      ),
    ).toBeInTheDocument();
    expect(screen.getAllByTestId("microbiology-ast-reading-row")).toHaveLength(
      2,
    );
    expect(
      screen.getAllByTestId("microbiology-ast-reading-row")[1],
    ).toHaveTextContent("Intermediate");
    expect(
      screen.getAllByTestId("microbiology-ast-reading-row")[0],
    ).toHaveTextContent("Manual Entry");
    expect(
      screen.getAllByTestId("microbiology-ast-reading-row")[0],
    ).toHaveTextContent("Organism");
    expect(
      screen.getAllByTestId("microbiology-ast-reading-row")[0],
    ).toHaveTextContent("4 ug/mL");
    expect(
      screen.getByText(
        "No standard breakpoint. Interpret this reading according to the local SOP.",
      ),
    ).toBeInTheDocument();
    await user.type(
      screen.getByLabelText("Override reason"),
      "mixed growth confirmed on repeat",
    );
    await user.click(screen.getByRole("button", { name: "Apply override" }));

    await waitFor(() =>
      expect(
        screen.getAllByTestId("microbiology-ast-reading-row")[0],
      ).toHaveTextContent("Resistant"),
    );
    const reviewButton = screen.getByRole("button", {
      name: "Review AST run",
    });
    await waitFor(() => expect(reviewButton).not.toBeDisabled());
    await user.click(reviewButton);

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
      rawValue: "4",
    });
  });

  it("requires a reason before adjusting the ordered AST panel", async () => {
    const user = userEvent.setup();
    const service = {
      getAstPanels: vi.fn().mockResolvedValue([
        { id: "panel-1", label: "GN-STD" },
        { id: "panel-2", label: "URINE-GN" },
      ]),
      getAstSetupForIsolate: vi.fn().mockResolvedValue({
        isolateId: "iso-1",
        orderedPanelId: "panel-1",
        orderedPanelLabel: "GN-STD",
        orderedPanelVersion: 3,
        panelProvenance: "ORGANISM_DEFAULT",
      }),
      getAntibiotics: vi.fn().mockResolvedValue([
        { id: "abx-1", label: "Ciprofloxacin" },
        { id: "abx-2", label: "Gentamicin" },
      ]),
      getAstPanelAntibiotics: vi.fn().mockImplementation((panelId) =>
        Promise.resolve([
          {
            antibioticId: panelId === "panel-2" ? "abx-2" : "abx-1",
            displayOrder: 1,
          },
        ]),
      ),
      getBreakpointStandards: vi
        .fn()
        .mockResolvedValue([{ id: "std-clsi", label: "CLSI 2026" }]),
      getAstRunsForIsolate: vi
        .fn()
        .mockResolvedValueOnce([])
        .mockResolvedValueOnce([inProgressRun]),
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: false,
        blockers: ["AST_REVIEW_REQUIRED"],
      }),
      startAstRun: vi.fn().mockResolvedValue(inProgressRun),
    };

    renderPanel(service);

    await user.click(
      await screen.findByRole("button", { name: "Adjust panel" }),
    );
    await user.selectOptions(screen.getByLabelText("AST panel"), "panel-2");
    expect(
      screen.getByRole("button", { name: "Start AST run" }),
    ).toBeDisabled();
    await user.type(
      screen.getByLabelText("Reason for panel adjustment"),
      "Urine-specific panel required",
    );
    await user.click(screen.getByRole("button", { name: "Start AST run" }));

    await waitFor(() =>
      expect(service.startAstRun).toHaveBeenCalledWith({
        isolateId: "iso-1",
        panelId: "panel-2",
        breakpointStandardId: "std-clsi",
        technique: "VITEK_2",
        orderedAntibioticIds: ["abx-2"],
        panelAdjustmentReason: "Urine-specific panel required",
      }),
    );
  });

  it("adds an individual antibiotic through the Carbon order adjustment control", async () => {
    const user = userEvent.setup();
    const service = {
      getAstPanels: vi
        .fn()
        .mockResolvedValue([{ id: "panel-1", label: "GN-STD" }]),
      getAstSetupForIsolate: vi.fn().mockResolvedValue({
        isolateId: "iso-1",
        orderedPanelId: "panel-1",
        orderedPanelLabel: "GN-STD",
        orderedPanelVersion: 3,
        panelProvenance: "ORGANISM_DEFAULT",
      }),
      getAntibiotics: vi.fn().mockResolvedValue([
        { id: "abx-1", label: "Ciprofloxacin" },
        { id: "abx-2", label: "Gentamicin" },
      ]),
      getAstPanelAntibiotics: vi
        .fn()
        .mockResolvedValue([{ antibioticId: "abx-1", displayOrder: 1 }]),
      getBreakpointStandards: vi
        .fn()
        .mockResolvedValue([{ id: "std-clsi", label: "CLSI 2026" }]),
      getAstRunsForIsolate: vi
        .fn()
        .mockResolvedValueOnce([])
        .mockResolvedValueOnce([inProgressRun]),
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: false,
        blockers: ["AST_REVIEW_REQUIRED"],
      }),
      startAstRun: vi.fn().mockResolvedValue(inProgressRun),
    };

    renderPanel(service);

    await user.click(
      await screen.findByRole("button", { name: "Adjust panel" }),
    );
    const drugSelector = await screen.findByRole("combobox", {
      name: /Antibiotics to test Total items selected:\s+1\./,
    });
    await user.click(drugSelector);
    await user.click(await screen.findByRole("option", { name: "Gentamicin" }));
    expect(
      screen.getByRole("button", { name: "Start AST run" }),
    ).toBeDisabled();
    await user.type(
      screen.getByLabelText("Reason for panel adjustment"),
      "Add reserve drug",
    );
    await user.click(screen.getByRole("button", { name: "Start AST run" }));

    await waitFor(() =>
      expect(service.startAstRun).toHaveBeenCalledWith({
        isolateId: "iso-1",
        panelId: "panel-1",
        breakpointStandardId: "std-clsi",
        technique: "VITEK_2",
        orderedAntibioticIds: ["abx-1", "abx-2"],
        panelAdjustmentReason: "Add reserve drug",
      }),
    );
  });

  it("limits manual entry to the run's snapshotted ordered antibiotics", async () => {
    const service = {
      getAstPanels: vi
        .fn()
        .mockResolvedValue([{ id: "panel-1", label: "GN-STD" }]),
      getAstSetupForIsolate: vi.fn().mockResolvedValue({
        isolateId: "iso-1",
        orderedPanelId: "panel-1",
        orderedPanelLabel: "GN-STD",
        orderedPanelVersion: 3,
        panelProvenance: "ORGANISM_DEFAULT",
      }),
      getAntibiotics: vi.fn().mockResolvedValue([
        { id: "abx-1", label: "Ciprofloxacin" },
        { id: "abx-2", label: "Gentamicin" },
      ]),
      getBreakpointStandards: vi.fn().mockResolvedValue([]),
      getAstRunsForIsolate: vi.fn().mockResolvedValue([inProgressRun]),
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: false,
        blockers: ["AST_REVIEW_REQUIRED"],
      }),
    };

    renderPanel(service);

    expect(
      await screen.findByRole("option", { name: "Ciprofloxacin" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("option", { name: "Gentamicin" }),
    ).not.toBeInTheDocument();
  });

  it("keeps review disabled until every ordered antibiotic has a reading", async () => {
    const incompleteRun = {
      ...inProgressRun,
      orderedAntibiotics: [
        ...inProgressRun.orderedAntibiotics,
        {
          antibioticId: "abx-2",
          displayOrder: 2,
          tier: 1,
          reportBehavior: "ALWAYS",
        },
      ],
      readings: [runWithReading.readings[0]],
    };
    const service = {
      getAstPanels: vi
        .fn()
        .mockResolvedValue([{ id: "panel-1", label: "GN-STD" }]),
      getAntibiotics: vi.fn().mockResolvedValue([
        { id: "abx-1", label: "Ciprofloxacin" },
        { id: "abx-2", label: "Gentamicin" },
      ]),
      getBreakpointStandards: vi.fn().mockResolvedValue([]),
      getAstRunsForIsolate: vi.fn().mockResolvedValue([incompleteRun]),
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: false,
        blockers: ["AST_REVIEW_REQUIRED"],
      }),
    };

    renderPanel(service);

    expect(
      await screen.findByRole("button", { name: "Review AST run" }),
    ).toBeDisabled();
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

  it("keeps AST setup disabled until the selected isolate is identified", async () => {
    const service = {
      getAstPanels: vi
        .fn()
        .mockResolvedValue([{ id: "panel-1", label: "Gram negative panel" }]),
      getAntibiotics: vi.fn().mockResolvedValue([]),
      getBreakpointStandards: vi.fn().mockResolvedValue([]),
      getAstRunsForIsolate: vi.fn().mockResolvedValue([]),
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: false,
        blockers: ["ISOLATE_IDENTIFICATION_REQUIRED"],
      }),
    };

    renderPanel(service, {
      isolates: [
        {
          ...isolate,
          organismId: null,
          identificationStatus: "PRELIMINARY",
        },
      ],
    });

    expect(
      await screen.findByRole("button", { name: "Start AST run" }),
    ).toBeDisabled();
    expect(
      screen.getAllByText("Identify the isolate before setting up AST."),
    ).not.toHaveLength(0);
  });

  it("keeps earlier culture usage visible with AST usage", async () => {
    const service = {
      getAstPanels: vi
        .fn()
        .mockResolvedValue([{ id: "panel-1", label: "Gram negative panel" }]),
      getAntibiotics: vi.fn().mockResolvedValue([]),
      getBreakpointStandards: vi.fn().mockResolvedValue([]),
      getAstRunsForIsolate: vi.fn().mockResolvedValue([]),
      getCaseReadiness: vi.fn().mockResolvedValue({
        finalReleaseReady: false,
        blockers: ["AST_REVIEW_REQUIRED"],
      }),
    };

    renderPanel(service, {
      reagentUsages: [
        {
          id: "culture-usage",
          usageContext: "CULTURE_SETUP",
          reagentName: "Blood agar",
          lotNumber: "MEDIA-FEFO",
          quantityUsed: 1,
          quantityUnit: "plate",
          currentLotStatus: "ACTIVE",
        },
        {
          id: "ast-usage",
          usageContext: "AST_SETUP",
          reagentName: "AST card",
          lotNumber: "CARD-FEFO",
          quantityUsed: 1,
          quantityUnit: "card",
          currentLotStatus: "ACTIVE",
        },
      ],
    });

    expect(await screen.findByText("MEDIA-FEFO")).toBeInTheDocument();
    expect(screen.getByText("Culture setup")).toBeInTheDocument();
    expect(screen.getByText("CARD-FEFO")).toBeInTheDocument();
    expect(screen.getByText("AST setup")).toBeInTheDocument();
  });

  it("starts a repeat attempt from a reviewed run with a required reason", async () => {
    const user = userEvent.setup();
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

    await user.type(
      screen.getByLabelText("Reason for repeat or retest"),
      "Control failed",
    );
    await user.click(screen.getByRole("radio", { name: "Retest" }));
    await user.selectOptions(
      screen.getByLabelText("Attempt method"),
      "DISK_DIFFUSION",
    );
    await user.click(
      screen.getByRole("button", { name: "Start retest attempt" }),
    );

    await waitFor(() =>
      expect(service.startRepeatAstRun).toHaveBeenCalledWith("run-1", {
        attemptType: "RETEST",
        reason: "Control failed",
        technique: "DISK_DIFFUSION",
      }),
    );
    expect(await screen.findByText("Retest")).toBeInTheDocument();
    expect(screen.getByLabelText("Zone diameter (mm)")).toBeInTheDocument();
  });

  it("shows attempt relationships and requires an explicit reportable selection", async () => {
    const user = userEvent.setup();
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

    await user.click(
      screen.getByRole("button", { name: "Use attempt 2 for reporting" }),
    );

    await waitFor(() =>
      expect(service.selectReportableAstRun).toHaveBeenCalledWith("run-2"),
    );
    expect(await screen.findByText("Included in report")).toBeInTheDocument();
  });

  it("surfaces named AST conflicts returned by the service", async () => {
    const user = userEvent.setup();
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

    await user.click(
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
