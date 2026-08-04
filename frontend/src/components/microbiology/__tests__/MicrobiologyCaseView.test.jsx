import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import MicrobiologyCaseView from "../MicrobiologyCaseView";
import messages from "../../../languages/en.json";

const caseDetail = {
  id: "case-1",
  sampleItemId: "1001",
  patientId: "patient-1",
  patientName: "Microbiology, UAT",
  accessionNumber: "UATMICRO001",
  specimenType: "Blood",
  workflowType: "BACTERIOLOGY",
  stage: "RECEIVED",
  activities: [
    { id: "a1", activityType: "CASE_CREATED", note: "Case created" },
  ],
  isolates: [],
};

const renderCase = (service, initialEntry = "/Microbiology/cases/case-1") =>
  render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <IntlProvider locale="en" messages={messages}>
        <MicrobiologyCaseView caseId="case-1" service={service} />
        <Route
          render={({ location }) => (
            <output data-testid="microbiology-current-url">
              {location.pathname}
              {location.search}
            </output>
          )}
        />
      </IntlProvider>
    </MemoryRouter>,
  );

const astServiceStubs = {
  getAstPanels: vi.fn().mockResolvedValue([]),
  getAntibiotics: vi.fn().mockResolvedValue([]),
  getBreakpointStandards: vi.fn().mockResolvedValue([]),
  getAstRunsForIsolate: vi.fn().mockResolvedValue([]),
  saveOrderDetail: vi.fn().mockResolvedValue({}),
  getCaseReadiness: vi.fn().mockResolvedValue({
    finalReleaseReady: true,
    blockers: [],
  }),
  startAstRun: vi.fn(),
  recordAstReading: vi.fn(),
  overrideAstReading: vi.fn(),
  reviewAstRun: vi.fn(),
  getCriticalCommunications: vi.fn().mockResolvedValue([]),
  logCriticalCommunication: vi.fn(),
  acknowledgeCriticalCommunication: vi.fn(),
  closeCriticalCommunication: vi.fn(),
  getOrganisms: vi.fn().mockResolvedValue([]),
  getWhonetReadiness: vi.fn().mockResolvedValue({
    whonetReady: true,
    blockers: [],
  }),
  getReportProjection: vi.fn().mockResolvedValue({
    reportableContent: true,
    mappingConfigured: true,
    content: "Escherichia coli: Ciprofloxacin S",
    projectedResultIds: ["result-1"],
  }),
  releasePreliminaryReport: vi.fn(),
  releaseFinalReport: vi.fn(),
  getCaseAmendments: vi.fn().mockResolvedValue([]),
  getCaseReportVersions: vi.fn().mockResolvedValue([]),
  openCaseAmendment: vi.fn(),
  cancelCaseAmendment: vi.fn(),
  releaseAmendedReport: vi.fn(),
  getIdentificationHistory: vi.fn().mockResolvedValue([]),
};

const getAccordionButton = (name) => {
  const button = screen
    .getAllByRole("button", { name })
    .find(
      (candidate) =>
        candidate.hasAttribute("aria-controls") &&
        candidate.hasAttribute("aria-expanded"),
    );
  if (!button) {
    throw new Error(`Accordion section not found: ${name}`);
  }
  return button;
};

describe("MicrobiologyCaseView", () => {
  it("loads case details and records setup activity", async () => {
    const user = userEvent.setup();
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(caseDetail),
      recordCaseActivity: vi.fn().mockResolvedValue({
        ...caseDetail,
        stage: "SETUP_RECORDED",
        activities: [
          ...caseDetail.activities,
          { id: "a2", activityType: "STAGE_CHANGED", note: "setup complete" },
        ],
      }),
      createIsolate: vi.fn(),
    };

    renderCase(service, "/Microbiology/cases/case-1?section=setup");

    expect(
      await screen.findByRole("heading", { name: "Microbiology case" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Microbiology, UAT")).toBeInTheDocument();
    expect(screen.getByText("UATMICRO001")).toBeInTheDocument();
    expect(screen.getByText("Blood")).toBeInTheDocument();
    expect(screen.getAllByText("Received").length).toBeGreaterThan(0);
    await user.type(
      screen.getByLabelText("Media or bottle"),
      "Blood culture bottle",
    );
    await user.type(screen.getByLabelText("Incubation"), "35 C for 24 hours");
    await user.type(screen.getByLabelText("Atmosphere"), "Ambient");
    await user.type(screen.getByLabelText("Activity note"), "setup complete");
    await user.click(screen.getByRole("button", { name: "Start inoculation" }));

    await waitFor(() =>
      expect(service.recordCaseActivity).toHaveBeenCalledWith("case-1", {
        nextStage: "SETUP_RECORDED",
        note: "Media or bottle: Blood culture bottle; Incubation: 35 C for 24 hours; Atmosphere: Ambient; setup complete",
      }),
    );
    await waitFor(() =>
      expect(screen.getAllByText("Setup Recorded").length).toBeGreaterThan(0),
    );
    expect(screen.getAllByText("Setup Recorded").length).toBeGreaterThan(0);
  });

  it("links the report workflow to the patient results page", async () => {
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(caseDetail),
      recordCaseActivity: vi.fn(),
      createIsolate: vi.fn(),
    };

    renderCase(service, "/Microbiology/cases/case-1?section=reports");

    expect(
      await screen.findByRole("link", { name: "View patient results" }),
    ).toHaveAttribute("href", "/PatientResults/patient-1");
  });

  it("opens critical communication from its canonical section URL", async () => {
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(caseDetail),
      recordCaseActivity: vi.fn(),
      createIsolate: vi.fn(),
    };

    renderCase(
      service,
      "/Microbiology/cases/case-1?section=critical-communication",
    );

    await screen.findByRole("heading", { name: "Microbiology case" });
    expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
      "section=critical-communication",
    );
    expect(getAccordionButton("Critical communication")).toHaveAttribute(
      "aria-expanded",
      "true",
    );
  });

  it("opens amendment history from its canonical section URL", async () => {
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue({
        ...caseDetail,
        stage: "FINAL_RELEASED",
        finalReleaseState: "FINAL_RELEASED",
      }),
      recordCaseActivity: vi.fn(),
      createIsolate: vi.fn(),
    };

    renderCase(service, "/Microbiology/cases/case-1?section=amendment");

    await screen.findByRole("heading", { name: "Microbiology case" });
    expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
      "section=amendment",
    );
    expect(getAccordionButton("Amendments")).toHaveAttribute(
      "aria-expanded",
      "true",
    );
    expect(
      screen.getByRole("button", { name: "Open amendment" }),
    ).toBeDisabled();
  });

  it("refreshes the case timeline after creating an isolate", async () => {
    const user = userEvent.setup();
    const refreshedCase = {
      ...caseDetail,
      activities: [
        ...caseDetail.activities,
        {
          id: "a2",
          activityType: "ISOLATE_CREATED",
          note: "ISO-1 Escherichia coli",
        },
      ],
      isolates: [
        {
          id: "iso-1",
          isolateLabel: "ISO-1",
          preliminaryOrganismText: "Escherichia coli",
        },
      ],
    };
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi
        .fn()
        .mockResolvedValueOnce(caseDetail)
        .mockResolvedValueOnce(refreshedCase),
      recordCaseActivity: vi.fn(),
      createIsolate: vi.fn().mockResolvedValue({ id: "iso-1" }),
    };

    renderCase(service, "/Microbiology/cases/case-1?section=isolates");

    expect(
      await screen.findByRole("heading", { name: "Microbiology case" }),
    ).toBeInTheDocument();
    await user.type(
      screen.getByLabelText("Preliminary organism"),
      "Escherichia coli",
    );
    await user.click(screen.getByRole("button", { name: "Create isolate" }));

    await waitFor(() =>
      expect(service.createIsolate).toHaveBeenCalledWith({
        caseId: "case-1",
        isolateLabel: "ISO-1",
        preliminaryOrganismText: "Escherichia coli",
        significance: "CLINICALLY_SIGNIFICANT",
      }),
    );
    await waitFor(() =>
      expect(
        screen.getByTestId("microbiology-isolates-card"),
      ).toHaveTextContent("ISO-1: Escherichia coli"),
    );
  });

  it("keeps worklist context while selecting a case section and returning", async () => {
    const user = userEvent.setup();
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(caseDetail),
      recordCaseActivity: vi.fn(),
      createIsolate: vi.fn(),
    };

    renderCase(
      service,
      "/Microbiology/cases/case-1?workflow=BACTERIOLOGY&urgency=HIGH&sort=newest",
    );

    await screen.findByRole("heading", { name: "Microbiology case" });
    await user.click(getAccordionButton("Isolates"));

    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/cases/case-1?workflow=BACTERIOLOGY&urgency=HIGH&sort=newest&section=isolates",
      ),
    );

    await user.click(
      screen.getByRole("link", { name: "Microbiology worklist" }),
    );
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/worklist?workflow=BACTERIOLOGY&urgency=HIGH&sort=newest",
      ),
    );
  });

  it("shows a final case as read-only and disables isolate mutation", async () => {
    const finalCase = {
      ...caseDetail,
      stage: "FINAL_RELEASED",
      finalReleaseState: "FINAL_RELEASED",
      isolates: [
        {
          id: "iso-1",
          isolateLabel: "ISO-1",
          preliminaryOrganismText: "Escherichia coli",
          significance: "CLINICALLY_SIGNIFICANT",
          identificationStatus: "CONFIRMED",
        },
      ],
    };
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(finalCase),
      recordCaseActivity: vi.fn(),
      createIsolate: vi.fn(),
    };

    renderCase(service, "/Microbiology/cases/case-1?section=isolates");

    expect(
      await screen.findByText("Final case is read-only"),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Update identification" }),
    ).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Create isolate" }),
    ).toBeDisabled();
  });
});
