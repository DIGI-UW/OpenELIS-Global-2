import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import BatchWorkplan from "./BatchWorkplan";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../utils/Utils";

vi.mock("../utils/Utils", () => ({
  convertAlphaNumLabNumForDisplay: vi.fn((value) => value),
  getFromOpenElisServer: vi.fn(),
  hasPermissionOrGlobalAdmin: vi.fn(() => false),
  postToOpenElisServerFullResponse: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

const renderWithIntl = () =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <BatchWorkplan />
      </IntlProvider>
    </MemoryRouter>,
  );

const pending = [
  {
    analysisId: "11",
    accessionNumber: "DEV-001",
    patientName: "Doe, Jane",
    testId: "1",
    testName: "Hemoglobin",
    testSectionId: "5",
    testSectionName: "Hematology",
    panelId: "9",
    panelName: "CBC",
    priority: "STAT",
    orderDate: "2026-10-27",
    orderDateForDisplay: "10/27/2026",
    sampleType: "Whole blood",
    statusName: "Not started",
    nonconforming: false,
  },
  {
    analysisId: "12",
    accessionNumber: "DEV-002",
    patientName: "Roe, John",
    testId: "2",
    testName: "Glucose",
    testSectionId: "6",
    testSectionName: "Biochemistry",
    priority: "ROUTINE",
    orderDate: "2026-10-20",
    orderDateForDisplay: "10/20/2026",
    sampleType: "Serum",
    statusName: "Not started",
    nonconforming: false,
  },
];

const overdueQc = {
  status: "OVERDUE",
  blocking: false,
  overridable: true,
  evaluationPending: false,
  instrumentName: null,
  testsWithoutControlLot: [],
  lots: [
    {
      controlLotId: "lot-1",
      lotNumber: "L1-2026",
      controlLevel: "NORMAL",
      testId: "1",
      testName: "Hemoglobin",
      testSectionId: "5",
      bench: true,
      status: "OVERDUE",
      frequencyType: "DAILY",
      canRecordInline: true,
      ruleCodes: [],
      openViolationIds: [],
      lastPassingAt: "2026-10-27T08:00:00Z",
      lastRun: {
        id: "run-1",
        runDateTime: "2026-10-27T08:00:00Z",
        performedBy: "Tech One",
        value: 100,
        resultStatus: "ACCEPTED",
        ruleCodes: [],
      },
      recentRuns: [],
    },
  ],
};

const batches = [
  {
    id: 7,
    name: "Morning chemistry",
    status: "DRAFT",
    itemCount: 1,
    createdAt: "2026-10-28T06:00:00Z",
    reagentId: 70,
    reagentName: "Hemoglobin reagent",
    reagentLotId: 80,
    reagentLotNumber: "R-80",
    items: [{ analysisId: "13", accessionNumber: "DEV-003" }],
    qc: overdueQc,
    releases: [],
  },
  {
    id: 8,
    name: "Released yesterday",
    status: "ACTIVE",
    itemCount: 1,
    createdAt: "2026-10-27T06:00:00Z",
    items: [{ analysisId: "14", accessionNumber: "DEV-004" }],
    releases: [
      {
        controlLotId: "lot-1",
        qcStatus: "NOT_RUN",
        overridden: true,
        nceId: 5,
        nceNumber: "NCE-0005",
      },
    ],
  },
];

beforeEach(() => {
  getFromOpenElisServer.mockReset();
  postToOpenElisServerFullResponse.mockReset();
  putToOpenElisServerFullResponse.mockReset();
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("pending-tests")) {
      callback(pending);
      return;
    }
    if (url.includes("setup-options")) {
      callback({ reagents: [], instruments: [] });
      return;
    }
    if (url.endsWith("/batches")) {
      callback(batches);
      return;
    }
    callback([]);
  });
});

describe("BatchWorkplan", () => {
  it("lists pending tests with patient, panel and priority", () => {
    renderWithIntl();

    expect(screen.getAllByText("Batch Workplan").length).toBeGreaterThan(0);
    expect(screen.getByText("DEV-001")).toBeInTheDocument();
    expect(screen.getByText("Doe, Jane")).toBeInTheDocument();
    expect(screen.getByText("CBC")).toBeInTheDocument();
    expect(screen.getByText("STAT (Urgent)")).toBeInTheDocument();
  });

  it("narrows the list as the user searches", () => {
    renderWithIntl();

    fireEvent.change(screen.getByLabelText("Search"), {
      target: { value: "glucose" },
    });

    expect(screen.queryByText("DEV-001")).not.toBeInTheDocument();
    expect(screen.getByText("DEV-002")).toBeInTheDocument();
  });

  it("sends the batch name with the selected analyses and opens Batches", async () => {
    postToOpenElisServerFullResponse.mockImplementation(
      (_url, _body, callback) =>
        callback({
          ok: true,
          json: async () => ({ id: 9, name: "AM run" }),
        }),
    );
    renderWithIntl();

    fireEvent.click(screen.getByLabelText("Select DEV-001"));
    fireEvent.change(screen.getByLabelText("Batch name (optional)"), {
      target: { value: "AM run" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: /create batch \(1\)/i }),
    );

    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/batch-workplans/batches",
      JSON.stringify({ name: "AM run", analysisIds: ["11"] }),
      expect.any(Function),
    );
    expect(
      await screen.findByText(/Batch "AM run" created/),
    ).toBeInTheDocument();
  });

  it("refuses a batch name over 100 characters before sending it", () => {
    renderWithIntl();

    fireEvent.click(screen.getByLabelText("Select DEV-001"));
    fireEvent.change(screen.getByLabelText("Batch name (optional)"), {
      target: { value: "x".repeat(101) },
    });

    expect(
      screen.getByText("Batch name cannot exceed 100 characters."),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /create batch \(1\)/i }),
    ).toBeDisabled();
  });

  it("opens a batch whose QC is overdue and shows its tags", () => {
    renderWithIntl();
    fireEvent.click(screen.getByRole("tab", { name: "Batches (2)" }));

    const overdue = screen.getByTestId("batch-tile-7");
    expect(overdue).toHaveTextContent("QC overdue");
    expect(screen.getByRole("button", { name: "Run QC" })).toBeInTheDocument();
    expect(screen.getByTestId("batch-tile-8")).toHaveTextContent(
      "NCE: NCE-0005",
    );
    expect(screen.getByText("NCE: NCE-0005").closest("a")).toHaveAttribute(
      "href",
      "/ViewNonConformingEvent?nceNumber=NCE-0005",
    );
  });

  it("clears every batch only after confirmation", async () => {
    postToOpenElisServerFullResponse.mockImplementation(
      (_url, _body, callback) => callback({ ok: true }),
    );
    renderWithIntl();
    fireEvent.click(screen.getByRole("tab", { name: "Batches (2)" }));

    fireEvent.click(screen.getByRole("button", { name: /clear all batches/i }));
    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: /clear all$/i }));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
        "/rest/batch-workplans/batches/archive-all",
        "{}",
        expect.any(Function),
      ),
    );
  });
});
