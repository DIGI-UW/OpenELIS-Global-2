import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import MicrobiologyCaseView from "../MicrobiologyCaseView";
import messages from "../../../languages/en.json";

const caseDetail = {
  id: "case-1",
  sampleItemId: "1001",
  accessionNumber: "2026-0001",
  requestingLocation: "Emergency department",
  workflowType: "BACTERIOLOGY",
  cultureMethodId: "method-1",
  stage: "RECEIVED",
  activities: [
    { id: "a1", activityType: "CASE_CREATED", note: "Case created" },
  ],
  isolates: [],
  orderDetail: {},
};

const renderCase = (service) =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <MicrobiologyCaseView caseId="case-1" service={service} />
      </IntlProvider>
    </MemoryRouter>,
  );

const serviceStubs = {
  getCultureMethods: vi
    .fn()
    .mockResolvedValue([{ id: "method-1", label: "Routine culture" }]),
  getCaseProtocolOptions: vi.fn().mockResolvedValue([
    {
      id: "method-1",
      label: "Routine culture",
      active: true,
      current: true,
    },
  ]),
  getAstPanels: vi.fn().mockResolvedValue([]),
  getAntibiotics: vi.fn().mockResolvedValue([]),
  getOrganisms: vi.fn().mockResolvedValue([]),
  getBreakpointStandards: vi.fn().mockResolvedValue([]),
  getAstRunsForIsolate: vi.fn().mockResolvedValue([]),
  getCaseReadiness: vi.fn().mockResolvedValue({
    finalReleaseReady: true,
    blockers: [],
  }),
  startAstRun: vi.fn(),
  recordAstReading: vi.fn(),
  overrideAstReading: vi.fn(),
  reviewAstRun: vi.fn(),
  updateIsolateIdentification: vi.fn(),
};

describe("MicrobiologyCaseView", () => {
  it("classifies an unassigned case before exposing profile work", async () => {
    const user = userEvent.setup();
    const unassigned = {
      ...caseDetail,
      workflowType: "UNASSIGNED",
      cultureMethodId: null,
    };
    const service = {
      ...serviceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(unassigned),
      changeCaseWorkflow: vi.fn().mockResolvedValue(caseDetail),
      createIsolate: vi.fn(),
    };

    renderCase(service);

    expect(
      await screen.findByText("Workflow classification required"),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Start inoculation" }),
    ).not.toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("Workflow"), "BACTERIOLOGY");
    await user.selectOptions(
      await screen.findByLabelText("Culture Protocol"),
      "method-1",
    );
    await user.type(screen.getByLabelText("Reason for change"), "Route order");
    await user.click(screen.getByRole("button", { name: "Apply workflow" }));

    expect(service.changeCaseWorkflow).toHaveBeenCalledWith("case-1", {
      workflowType: "BACTERIOLOGY",
      cultureMethodId: "method-1",
      reason: "Route order",
      preserveExistingWorkConfirmed: false,
    });
    expect(
      await screen.findByRole("button", { name: "Start inoculation" }),
    ).toBeEnabled();
  });

  it("records inoculation through the case workbench", async () => {
    const user = userEvent.setup();
    const inoculation = {
      id: "inoculation-1",
      containerIdentifier: "BOTTLE-001",
      media: "Blood agar",
    };
    const service = {
      ...serviceStubs,
      getCaseDetail: vi.fn().mockResolvedValue(caseDetail),
      getCaseInoculations: vi
        .fn()
        .mockResolvedValueOnce([])
        .mockResolvedValueOnce([inoculation]),
      recordCaseInoculation: vi.fn().mockResolvedValue(inoculation),
      createIsolate: vi.fn(),
    };

    renderCase(service);

    await user.click(
      await screen.findByRole("button", { name: "Start inoculation" }),
    );
    await user.type(screen.getByLabelText("Bottle or plate ID"), "BOTTLE-001");
    await user.type(screen.getByLabelText("Media or bottle"), "Blood agar");
    await user.click(screen.getByRole("button", { name: "Save media" }));

    await waitFor(() =>
      expect(service.recordCaseInoculation).toHaveBeenCalledWith("case-1", {
        sourceInoculationId: undefined,
        containerIdentifier: "BOTTLE-001",
        media: "Blood agar",
        incubation: "",
        atmosphere: "",
      }),
    );
    expect(await screen.findByText("BOTTLE-001")).toBeInTheDocument();
  });

  it("creates a Gram-stain-first isolate and refreshes the case", async () => {
    const user = userEvent.setup();
    const refreshedCase = {
      ...caseDetail,
      isolates: [
        {
          id: "iso-1",
          isolateLabel: "ISO-1",
          gramStain: "Gram negative rod",
          colonyMorphology: "Lactose fermenting",
          significance: "CLINICALLY_SIGNIFICANT",
          identificationStatus: "PRELIMINARY",
        },
      ],
    };
    const service = {
      ...serviceStubs,
      getCaseDetail: vi
        .fn()
        .mockResolvedValueOnce(caseDetail)
        .mockResolvedValueOnce(refreshedCase),
      createIsolate: vi.fn().mockResolvedValue({ id: "iso-1" }),
    };

    renderCase(service);

    await user.type(
      await screen.findByLabelText("Gram stain"),
      "Gram negative rod",
    );
    await user.type(
      screen.getByLabelText("Colony morphology"),
      "Lactose fermenting",
    );
    await user.click(screen.getByRole("button", { name: "Create isolate" }));

    await waitFor(() =>
      expect(service.createIsolate).toHaveBeenCalledWith({
        caseId: "case-1",
        isolateLabel: "ISO-1",
        gramStain: "Gram negative rod",
        colonyMorphology: "Lactose fermenting",
        significance: "CLINICALLY_SIGNIFICANT",
      }),
    );
    expect(
      await screen.findByTestId("microbiology-isolates-card"),
    ).toHaveTextContent("ISO-1");
  });
});
