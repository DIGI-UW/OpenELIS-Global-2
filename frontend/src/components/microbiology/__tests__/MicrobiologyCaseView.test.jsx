import React from "react";
import { render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { vi } from "vitest";
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
  getCultureMethods: vi.fn().mockResolvedValue([]),
  changeCaseWorkflow: vi.fn(),
  getCaseProtocolOptions: vi.fn().mockResolvedValue([]),
  changeCaseProtocol: vi.fn(),
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
  getNceCategories: vi.fn().mockResolvedValue([]),
  getNceReportingUnits: vi.fn().mockResolvedValue([]),
  reportCaseNonconformance: vi.fn(),
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
  getReagentLotOverview: vi.fn().mockResolvedValue({
    requirements: [],
    usages: [],
  }),
  getCaseInoculations: vi.fn().mockResolvedValue([]),
  recordCaseInoculation: vi.fn(),
  getCaseTimeline: vi.fn().mockResolvedValue(caseDetail.activities),
  addCaseNote: vi.fn(),
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
  it("opens primary inoculation from the received next step with canonical URL state", async () => {
    const user = userEvent.setup();
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(caseDetail),
      createIsolate: vi.fn(),
    };

    renderCase(
      service,
      "/Microbiology/cases/case-1?q=UATMICRO001&sort=newest&section=setup",
    );

    const nextStep = await screen.findByTestId("microbiology-next-step");
    await user.click(
      within(nextStep).getByRole("button", { name: "Start inoculation" }),
    );

    expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
      "/Microbiology/cases/case-1?q=UATMICRO001&sort=newest&section=setup&action=start-inoculation",
    );
    expect(screen.getByLabelText("Bottle or plate ID")).toHaveFocus();
  });

  it("mounts only the canonical active accordion body", async () => {
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(caseDetail),
      createIsolate: vi.fn(),
    };

    renderCase(service, "/Microbiology/cases/case-1?section=setup");

    expect(
      await screen.findByTestId("microbiology-case-section-setup"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("microbiology-case-section-timeline"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("microbiology-case-section-isolates"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("microbiology-case-section-ast"),
    ).not.toBeInTheDocument();
    expect(service.getIdentificationHistory).not.toHaveBeenCalled();
    expect(service.getAstRunsForIsolate).not.toHaveBeenCalled();
  });

  it("sets a bench protocol from canonical URL state and retains worklist context", async () => {
    const user = userEvent.setup();
    const protocolOption = {
      id: "method-1",
      label: "Routine blood culture",
      active: true,
      current: false,
      mediaDefaults: "BAP + CHOC",
      incubationDefaults: "48 hours at 35 C",
      atmosphereDefaults: "aerobic + anaerobic",
    };
    const updatedCase = { ...caseDetail, cultureMethodId: "method-1" };
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(caseDetail),
      getCaseProtocolOptions: vi.fn().mockResolvedValue([protocolOption]),
      changeCaseProtocol: vi.fn().mockResolvedValue(updatedCase),
      createIsolate: vi.fn(),
    };

    renderCase(
      service,
      "/Microbiology/cases/case-1?workflow=BACTERIOLOGY&section=setup",
    );

    await user.click(
      await screen.findByRole("button", { name: "Set protocol" }),
    );
    expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
      "workflow=BACTERIOLOGY&section=setup&action=set-protocol",
    );
    await user.selectOptions(
      screen.getByRole("combobox", { name: "Culture protocol" }),
      "method-1",
    );
    await user.type(
      screen.getByRole("textbox", { name: "Reason for protocol change" }),
      "Bench review requires routine media",
    );
    await user.click(screen.getByRole("button", { name: "Save protocol" }));

    await waitFor(() =>
      expect(service.changeCaseProtocol).toHaveBeenCalledWith("case-1", {
        cultureMethodId: "method-1",
        reason: "Bench review requires routine media",
      }),
    );
    expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
      "/Microbiology/cases/case-1?workflow=BACTERIOLOGY&section=setup",
    );
    expect(
      screen.getByTestId("microbiology-current-url"),
    ).not.toHaveTextContent("action=");
    expect(
      await screen.findByText("Routine blood culture"),
    ).toBeInTheDocument();
  });

  it.each([
    {
      action: "mark-positive",
      buttonName: "Confirm positive signal",
      nextStage: "POSITIVE_SIGNAL",
      note: "Culture marked positive",
    },
    {
      action: "mark-no-growth",
      buttonName: "Confirm no growth",
      nextStage: "NO_GROWTH_READY",
      note: "Incubation complete with no growth",
    },
  ])(
    "confirms $action from the case and clears the routed action",
    async ({ action, buttonName, nextStage, note }) => {
      const user = userEvent.setup();
      const updatedCase = {
        ...caseDetail,
        stage: nextStage,
      };
      const service = {
        ...astServiceStubs,
        getCaseDetail: vi.fn().mockResolvedValue({
          ...caseDetail,
          stage: "INCUBATING",
        }),
        recordCaseActivity: vi.fn().mockResolvedValue(updatedCase),
        createIsolate: vi.fn(),
      };

      renderCase(
        service,
        `/Microbiology/cases/case-1?section=setup&action=${action}`,
      );

      const section = await screen.findByTestId(
        "microbiology-case-section-setup",
      );
      const transitionTitle = screen.getByRole("heading", {
        name:
          action === "mark-positive"
            ? "Mark culture positive"
            : "Mark culture as no growth",
      });
      await waitFor(() => expect(transitionTitle).toHaveFocus());
      const confirm = screen.getByRole("button", { name: buttonName });
      confirm.focus();
      await user.keyboard("{Enter}");

      await waitFor(() =>
        expect(service.recordCaseActivity).toHaveBeenCalledWith("case-1", {
          nextStage,
          note,
        }),
      );
      await waitFor(() =>
        expect(
          screen.getByTestId("microbiology-current-url"),
        ).toHaveTextContent("/Microbiology/cases/case-1?section=setup"),
      );
      expect(
        screen.getByTestId("microbiology-current-url"),
      ).not.toHaveTextContent("action=");
      await waitFor(() => expect(section).toHaveFocus());
    },
  );

  it("opens the incubating next action from the case page with canonical URL state", async () => {
    const user = userEvent.setup();
    const incubatingCase = {
      ...caseDetail,
      stage: "INCUBATING",
      activities: [
        ...caseDetail.activities,
        {
          id: "a2",
          activityType: "INOCULATION_RECORDED",
          note: "BOTTLE-001 - Blood culture bottle",
        },
      ],
    };
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(incubatingCase),
      getCaseTimeline: vi.fn().mockResolvedValue(incubatingCase.activities),
      getCaseInoculations: vi.fn().mockResolvedValue([
        {
          id: "inoculation-1",
          containerIdentifier: "BOTTLE-001",
          media: "Blood culture bottle",
        },
      ]),
      recordCaseActivity: vi.fn(),
      createIsolate: vi.fn(),
    };

    renderCase(
      service,
      "/Microbiology/cases/case-1?q=UATMICRO001&sort=newest&section=setup",
    );

    const nextStep = await screen.findByTestId("microbiology-next-step");
    await user.click(
      within(nextStep).getByRole("button", { name: "Mark positive" }),
    );

    expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
      "/Microbiology/cases/case-1?q=UATMICRO001&sort=newest&section=setup&action=mark-positive",
    );
    expect(
      screen.getByRole("heading", { name: "Mark culture positive" }),
    ).toBeInTheDocument();
  });

  it("records primary inoculation with a service-managed timeline and lot", async () => {
    const user = userEvent.setup();
    const requirement = {
      analysisId: "41",
      testId: "22",
      testName: "Blood culture",
      linkId: "link-1",
      reagentId: 13,
      reagentName: "Blood agar",
      usageType: "PRIMARY",
      quantityPerTest: 1,
      quantityUnit: "plate",
      lots: [
        {
          id: 7,
          lotNumber: "MEDIA-FIFO",
          effectiveExpirationDate: "2026-09-01T00:00:00Z",
          currentQuantity: 10,
          qcStatus: "PASSED",
          status: "ACTIVE",
          available: true,
          fefoRecommended: true,
        },
      ],
    };
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi
        .fn()
        .mockResolvedValueOnce(caseDetail)
        .mockResolvedValue({
          ...caseDetail,
          stage: "INCUBATING",
          activities: [
            ...caseDetail.activities,
            {
              id: "a2",
              activityType: "INOCULATION_RECORDED",
              note: "BOTTLE-001 - Blood culture bottle",
            },
          ],
        }),
      getCaseTimeline: vi
        .fn()
        .mockResolvedValueOnce(caseDetail.activities)
        .mockResolvedValue([
          ...caseDetail.activities,
          {
            id: "a2",
            activityType: "INOCULATION_RECORDED",
            note: "BOTTLE-001 - Blood culture bottle",
          },
        ]),
      getReagentLotOverview: vi.fn().mockResolvedValue({
        requirements: [requirement],
        usages: [],
      }),
      getCaseInoculations: vi
        .fn()
        .mockResolvedValueOnce([])
        .mockResolvedValueOnce([
          {
            id: "inoculation-1",
            containerIdentifier: "BOTTLE-001",
            media: "Blood culture bottle",
            incubation: "35 C for 24 hours",
          },
        ]),
      recordCaseInoculation: vi.fn().mockResolvedValue({ id: "inoculation-1" }),
      createIsolate: vi.fn(),
    };

    renderCase(service, "/Microbiology/cases/case-1?section=setup");

    expect(
      await screen.findByRole("heading", { name: "Microbiology case" }),
    ).toBeInTheDocument();
    expect(screen.getByText("Microbiology, UAT")).toBeInTheDocument();
    expect(screen.getAllByText(/UATMICRO001/).length).toBeGreaterThan(0);
    expect(screen.getByText("Blood")).toBeInTheDocument();
    expect(screen.getAllByText("Received").length).toBeGreaterThan(0);
    await user.click(
      within(screen.getByTestId("microbiology-next-step")).getByRole("button", {
        name: "Start inoculation",
      }),
    );
    await user.type(screen.getByLabelText("Bottle or plate ID"), "BOTTLE-001");
    await user.type(
      screen.getByLabelText("Media or bottle"),
      "Blood culture bottle",
    );
    await user.type(screen.getByLabelText("Incubation"), "35 C for 24 hours");
    await user.type(screen.getByLabelText("Atmosphere"), "Ambient");
    await user.click(screen.getByText(/MEDIA-FIFO/).closest("label"));
    await user.click(screen.getByRole("button", { name: "Save media" }));

    await waitFor(() =>
      expect(service.recordCaseInoculation).toHaveBeenCalledWith("case-1", {
        containerIdentifier: "BOTTLE-001",
        media: "Blood culture bottle",
        incubation: "35 C for 24 hours",
        atmosphere: "Ambient",
        lotSelections: [
          {
            analysisId: "41",
            testReagentLinkId: "link-1",
            lotId: 7,
          },
        ],
      }),
    );
    expect(await screen.findByText("BOTTLE-001")).toBeInTheDocument();
    expect(screen.getByText("Primary")).toBeInTheDocument();
    expect(
      screen.getByText(/Incubating. Mark the case positive/),
    ).toBeInTheDocument();
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

  it("canonicalizes an unscoped case to its current AST step", async () => {
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue({
        ...caseDetail,
        stage: "AST_IN_PROGRESS",
      }),
      createIsolate: vi.fn(),
    };

    renderCase(service);

    await screen.findByRole("heading", { name: "Microbiology case" });
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "section=ast",
      ),
    );
    expect(getAccordionButton("Manual AST")).toHaveAttribute(
      "aria-expanded",
      "true",
    );
    expect(
      screen.getByTestId("microbiology-current-step-action"),
    ).toHaveTextContent("Manual AST");
  });

  it("shows the latest activity actor and linked NCE count in the case header", async () => {
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue({
        ...caseDetail,
        lastActivityBy: "Amina Diallo",
        lastActivityAt: "2026-08-05T09:00:00Z",
        nonconformanceCount: 2,
      }),
      createIsolate: vi.fn(),
    };

    renderCase(service);

    expect(await screen.findByText(/Amina Diallo/)).toBeInTheDocument();
    expect(screen.getByText("2 NCEs")).toBeInTheDocument();
  });

  it("preserves an explicitly bookmarked non-current section", async () => {
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue({
        ...caseDetail,
        stage: "AST_IN_PROGRESS",
      }),
      createIsolate: vi.fn(),
    };

    renderCase(service, "/Microbiology/cases/case-1?section=timeline");

    await screen.findByRole("heading", { name: "Microbiology case" });
    expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
      "section=timeline",
    );
    expect(getAccordionButton("Timeline")).toHaveAttribute(
      "aria-expanded",
      "true",
    );
  });

  it("moves focus into an expanded inline section and announces it", async () => {
    const user = userEvent.setup();
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(caseDetail),
      createIsolate: vi.fn(),
    };

    renderCase(service);

    await screen.findByRole("heading", { name: "Microbiology case" });
    await user.click(getAccordionButton("Timeline"));

    const timelineSection = await screen.findByTestId(
      "microbiology-case-section-timeline",
    );
    await waitFor(() => expect(timelineSection).toHaveFocus());
    expect(
      screen.getByRole("status", { name: "Section status" }),
    ).toHaveTextContent("Timeline expanded");
  });

  it("opens a case-targeted critical communication from the header", async () => {
    const user = userEvent.setup();
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(caseDetail),
      createIsolate: vi.fn(),
    };

    renderCase(service);

    await user.click(
      await screen.findByRole("button", {
        name: "Log critical notification",
      }),
    );

    expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
      "section=critical-communication&action=log-critical&targetType=CASE&targetId=case-1",
    );
    expect(screen.getByLabelText("Critical result target")).toBeDisabled();
    expect(screen.getByLabelText("Target record")).toHaveValue("case-1");
  });

  it("opens each nonconformance action from a canonical header URL", async () => {
    const user = userEvent.setup();
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(caseDetail),
      createIsolate: vi.fn(),
    };

    renderCase(service);

    const reportNce = await screen.findByRole("button", {
      name: "Report NCE",
    });
    await user.click(reportNce);
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "section=nonconformance&action=report-nce",
      ),
    );
    expect(
      screen.getByRole("heading", { name: "Report nonconformance" }),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(
        screen.getByRole("heading", { name: "Report nonconformance" }),
      ).toHaveFocus(),
    );

    const markLost = screen.getByRole("button", { name: /Mark lost/ });
    await user.click(markLost);
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "section=nonconformance&action=mark-lost",
      ),
    );
    await waitFor(() =>
      expect(
        screen.getByRole("heading", { name: "Mark specimen lost" }),
      ).toHaveFocus(),
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
    await waitFor(() =>
      expect(
        screen.getByTestId("microbiology-case-section-isolates"),
      ).toHaveFocus(),
    );
    await user.type(screen.getByLabelText("Gram stain"), "Gram negative rods");
    await user.type(
      screen.getByLabelText("Colony morphology"),
      "Lactose fermenting colonies",
    );
    await user.click(screen.getByRole("button", { name: "Create isolate" }));

    await waitFor(() =>
      expect(service.createIsolate).toHaveBeenCalledWith({
        caseId: "case-1",
        isolateLabel: "ISO-1",
        gramStain: "Gram negative rods",
        colonyMorphology: "Lactose fermenting colonies",
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

  it("holds profile-specific actions until an unassigned case is classified", async () => {
    const unassignedCase = {
      ...caseDetail,
      workflowType: "UNASSIGNED",
      siblingCases: [
        {
          id: "case-tb",
          workflowType: "MYCOBACTERIOLOGY_TB",
          stage: "RECEIVED",
        },
      ],
    };
    const service = {
      ...astServiceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(unassignedCase),
      recordCaseActivity: vi.fn(),
      createIsolate: vi.fn(),
    };

    renderCase(service, "/Microbiology/cases/case-1?section=ast");

    expect(
      await screen.findByText("Workflow classification required"),
    ).toBeInTheDocument();
    expect(screen.getByText("Change workflow")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Start AST" }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByLabelText("Mycobacteriology TB (Received)"),
    ).toHaveAttribute("href", "/Microbiology/cases/case-tb?section=case-info");
    await waitFor(() =>
      expect(screen.getByTestId("microbiology-current-url")).toHaveTextContent(
        "/Microbiology/cases/case-1?section=case-info",
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
    expect(screen.getByRole("button", { name: "Edit isolate" })).toBeDisabled();
    expect(
      screen.getByRole("button", { name: "Create isolate" }),
    ).toBeDisabled();
  });
});
