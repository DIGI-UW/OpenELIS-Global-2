import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor, within } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import AstEntryPanel from "../AstEntryPanel";
import messages from "../../../languages/en.json";

const isolate = {
  id: "iso-1",
  isolateLabel: "ISO-1",
  significance: "CLINICALLY_SIGNIFICANT",
};

const orderedAntibiotic = {
  antibioticId: "abx-1",
  displayOrder: 1,
  tier: 1,
  reportBehavior: "ALWAYS",
};

const inProgressRun = {
  id: "run-1",
  isolateId: "iso-1",
  panelId: "panel-1",
  panelVersion: 3,
  panelProvenance: "ORGANISM_DEFAULT",
  breakpointStandardId: "std-clsi",
  breakpointVersion: "2026",
  technique: "ETEST",
  measurementType: "MIC",
  status: "IN_PROGRESS",
  orderedAntibiotics: [orderedAntibiotic],
  readings: [],
};

const reading = {
  id: "reading-1",
  antibioticId: "abx-1",
  interpretation: "SUSCEPTIBLE",
  method: "MIC",
  rawValue: 4,
  source: "MANUAL_ENTRY",
  matchedBy: "ORGANISM",
  units: "ug/mL",
  overrideHistory: [],
};

const runWithReading = {
  ...inProgressRun,
  readings: [reading],
};

const overriddenReading = {
  ...reading,
  overrideInterpretation: "RESISTANT",
  overrideReason: "confirmed manually",
  overrideHistory: [
    {
      id: "event-1",
      action: "OVERRIDE",
      fromInterpretation: "SUSCEPTIBLE",
      toInterpretation: "RESISTANT",
      reason: "confirmed manually",
      performedByDisplay: "Lab Supervisor",
    },
  ],
};

const runWithOverride = {
  ...inProgressRun,
  readings: [overriddenReading],
};

const revertedReading = {
  ...reading,
  overrideHistory: [
    ...overriddenReading.overrideHistory,
    {
      id: "event-2",
      action: "REVERT",
      fromInterpretation: "RESISTANT",
      toInterpretation: "SUSCEPTIBLE",
      reason: "override entered in error",
      performedByDisplay: "Lab Supervisor",
    },
  ],
};

const runAfterRevert = {
  ...inProgressRun,
  readings: [revertedReading],
};

const reviewedRun = {
  ...runAfterRevert,
  status: "REVIEWED",
};

const renderPanel = (service) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AstEntryPanel
        caseId="case-1"
        workflowType="BACTERIOLOGY"
        isolates={[isolate]}
        service={service}
        saving={false}
      />
    </IntlProvider>,
  );

const readiness = {
  finalReleaseReady: false,
  blockers: ["AST_REVIEW_REQUIRED"],
};

const serviceFor = () => ({
  getAstPanels: vi
    .fn()
    .mockResolvedValue([{ id: "panel-1", label: "Enterobacterales panel" }]),
  getAstSetupForIsolate: vi.fn().mockResolvedValue({
    isolateId: "iso-1",
    orderedPanelId: "panel-1",
    orderedPanelLabel: "Enterobacterales panel",
    orderedPanelVersion: 3,
    panelProvenance: "ORGANISM_DEFAULT",
  }),
  getAstPanelAntibiotics: vi.fn().mockResolvedValue([orderedAntibiotic]),
  getAntibiotics: vi.fn().mockResolvedValue([
    { id: "abx-1", label: "Ciprofloxacin" },
    { id: "abx-2", label: "Gentamicin" },
  ]),
  getBreakpointStandards: vi
    .fn()
    .mockResolvedValue([{ id: "std-clsi", label: "CLSI 2026" }]),
  getAstRunsForIsolate: vi.fn().mockResolvedValue([]),
  getCaseReadiness: vi.fn().mockResolvedValue(readiness),
  startAstRun: vi.fn().mockResolvedValue(inProgressRun),
  recordAstReading: vi.fn().mockResolvedValue(reading),
  overrideAstReading: vi.fn().mockResolvedValue(overriddenReading),
  revertAstOverride: vi.fn().mockResolvedValue(revertedReading),
  reviewAstRun: vi.fn().mockResolvedValue(reviewedRun),
});

describe("AstEntryPanel", () => {
  it("records, audits, reverts, and reviews the ordered manual AST work", async () => {
    const user = userEvent.setup();
    const service = serviceFor();
    service.getAstRunsForIsolate
      .mockReset()
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([inProgressRun])
      .mockResolvedValueOnce([runWithReading])
      .mockResolvedValueOnce([runWithOverride])
      .mockResolvedValueOnce([runAfterRevert])
      .mockResolvedValueOnce([reviewedRun]);

    renderPanel(service);

    expect(await screen.findByText("Manual AST")).toBeInTheDocument();
    expect(
      await screen.findByText("Enterobacterales panel v3"),
    ).toBeInTheDocument();
    await user.selectOptions(
      screen.getByLabelText("Laboratory technique"),
      "ETEST",
    );
    expect(screen.getByText("MIC concentration")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Start AST run" }));

    expect(service.startAstRun).toHaveBeenCalledWith({
      isolateId: "iso-1",
      panelId: "panel-1",
      breakpointStandardId: "std-clsi",
      technique: "ETEST",
      orderedAntibioticIds: ["abx-1"],
    });

    expect(await screen.findByText("In Progress")).toBeInTheDocument();
    await user.click(
      screen.getByRole("button", { name: "Record AST reading" }),
    );

    expect(
      await screen.findByRole("cell", { name: "Organism" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("cell", { name: "Manual entry" }),
    ).toBeInTheDocument();
    expect(screen.getByText("4 ug/mL")).toBeInTheDocument();
    expect(service.recordAstReading).toHaveBeenCalledWith("run-1", {
      antibioticId: "abx-1",
      rawValue: "4",
    });

    await user.type(
      screen.getByLabelText("Override reason"),
      "confirmed manually",
    );
    await user.click(screen.getByRole("button", { name: "Apply override" }));
    expect(await screen.findByText("Lab Supervisor")).toBeInTheDocument();
    expect(
      within(screen.getByRole("list", { name: "Override history" })).getByText(
        "confirmed manually",
      ),
    ).toBeInTheDocument();

    await user.type(
      screen.getByLabelText("Revert reason"),
      "override entered in error",
    );
    await user.click(screen.getByRole("button", { name: /Revert override$/ }));
    expect(service.revertAstOverride).toHaveBeenCalledWith("reading-1", {
      overrideReason: "override entered in error",
    });

    await user.click(screen.getByRole("button", { name: "Review AST run" }));
    expect(await screen.findByText("Reviewed")).toBeInTheDocument();
  });

  it("requires a reason when the ordered drug set is adjusted", async () => {
    const user = userEvent.setup();
    const service = serviceFor();
    renderPanel(service);

    await screen.findByText("Enterobacterales panel v3");
    await user.selectOptions(
      screen.getByLabelText("Laboratory technique"),
      "DISK_DIFFUSION",
    );
    expect(screen.getByText("Zone diameter")).toBeInTheDocument();
    await user.click(
      screen.getByRole("checkbox", { name: "Customize ordered antibiotics" }),
    );
    await user.click(screen.getByRole("checkbox", { name: "Gentamicin" }));

    expect(
      screen.getByRole("button", { name: "Start AST run" }),
    ).toBeDisabled();
    await user.type(
      screen.getByLabelText("Panel adjustment reason"),
      "additional resistance screen",
    );
    await user.click(screen.getByRole("button", { name: "Start AST run" }));

    await waitFor(() =>
      expect(service.startAstRun).toHaveBeenCalledWith({
        isolateId: "iso-1",
        panelId: "panel-1",
        breakpointStandardId: "std-clsi",
        panelAdjustmentReason: "additional resistance screen",
        technique: "DISK_DIFFUSION",
        orderedAntibioticIds: ["abx-1", "abx-2"],
      }),
    );
  });
});
