import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import MicrobiologyCaseView from "../MicrobiologyCaseView";
import messages from "../../../languages/en.json";

const caseDetail = {
  id: "case-1",
  sampleItemId: "1001",
  workflowType: "BACTERIOLOGY",
  stage: "RECEIVED",
  activities: [
    { id: "a1", activityType: "CASE_CREATED", note: "Case created" },
  ],
  isolates: [],
};

const renderCase = (service) =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <MicrobiologyCaseView caseId="case-1" service={service} />
      </IntlProvider>
    </MemoryRouter>,
  );

const astServiceStubs = {
  getAstPanels: vi.fn().mockResolvedValue([]),
  getAntibiotics: vi.fn().mockResolvedValue([]),
  getAstRunsForIsolate: vi.fn().mockResolvedValue([]),
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
  getWhonetReadiness: vi.fn().mockResolvedValue({
    whonetReady: true,
    blockers: [],
  }),
  releaseFinalReport: vi.fn(),
};

describe("MicrobiologyCaseView", () => {
  it("loads case details and records setup activity", async () => {
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

    renderCase(service);

    expect(await screen.findByText("Microbiology case")).toBeInTheDocument();
    expect(screen.getAllByText("Received").length).toBeGreaterThan(0);
    fireEvent.change(screen.getByLabelText("Activity note"), {
      target: { value: "setup complete" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Start inoculation" }));

    await waitFor(() =>
      expect(service.recordCaseActivity).toHaveBeenCalledWith("case-1", {
        nextStage: "SETUP_RECORDED",
        note: "setup complete",
      }),
    );
    await waitFor(() =>
      expect(screen.getAllByText("Setup Recorded").length).toBeGreaterThan(0),
    );
    expect(screen.getByText(/setup complete/)).toBeInTheDocument();
  });

  it("refreshes the case timeline after creating an isolate", async () => {
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

    renderCase(service);

    expect(await screen.findByText("Microbiology case")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Preliminary organism"), {
      target: { value: "Escherichia coli" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create isolate" }));

    await waitFor(() =>
      expect(service.createIsolate).toHaveBeenCalledWith({
        caseId: "case-1",
        isolateLabel: "ISO-1",
        preliminaryOrganismText: "Escherichia coli",
        significance: "CLINICALLY_SIGNIFICANT",
      }),
    );
    expect(
      await screen.findByText(
        (_, element) =>
          element?.tagName.toLowerCase() === "li" &&
          element.textContent.includes("ISO-1: Escherichia coli"),
      ),
    ).toBeInTheDocument();
    expect(screen.getByText(/Isolate Created/)).toBeInTheDocument();
  });
});
