import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import BatchTile from "./BatchTile";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";

vi.mock("../../utils/Utils", () => ({
  convertAlphaNumLabNumForDisplay: vi.fn((value) => value),
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

const lot = (overrides = {}) => ({
  controlLotId: "lot-1",
  lotNumber: "L1-2026",
  controlLevel: "NORMAL",
  testId: "1",
  testName: "Hemoglobin",
  testSectionId: "5",
  bench: true,
  status: "NOT_RUN",
  frequencyType: "DAILY",
  canRecordInline: true,
  ruleCodes: [],
  openViolationIds: [],
  recentRuns: [],
  ...overrides,
});

const qc = (status, overrides = {}) => ({
  status,
  blocking: status === "LOT_EXPIRED" || status === "NO_CONTROL_LOT",
  overridable: !["PASS", "LOT_EXPIRED", "NO_CONTROL_LOT"].includes(status),
  evaluationPending: false,
  instrumentName: null,
  testsWithoutControlLot: [],
  lots: [lot({ status })],
  ...overrides,
});

const draftBatch = (qcStatus) => ({
  id: 7,
  name: "Morning chemistry",
  status: "DRAFT",
  itemCount: 1,
  reagentId: 70,
  reagentName: "Hemoglobin reagent",
  reagentLotId: 80,
  reagentLotNumber: "R-80",
  instrumentId: null,
  items: [{ analysisId: "11", accessionNumber: "DEV-001" }],
  qc: qcStatus ? qc(qcStatus) : undefined,
  releases: [],
});

const setupOptions = {
  reagents: [
    {
      id: 70,
      name: "Hemoglobin reagent",
      testNames: ["Hemoglobin"],
      lots: [
        {
          id: 80,
          lotNumber: "R-80",
          expirationDate: "2027-01-01T00:00:00Z",
          currentQuantity: 5,
          units: "mL",
          suggested: true,
        },
        {
          id: 81,
          lotNumber: "R-81",
          expirationDate: "2027-06-01T00:00:00Z",
          currentQuantity: 9,
          units: "mL",
          suggested: false,
        },
      ],
    },
  ],
  instruments: [{ id: "9", name: "Sysmex" }],
};

const renderTile = (batch, props = {}) => {
  const onChanged = vi.fn();
  const notify = vi.fn();
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <BatchTile
          batch={batch}
          expanded
          onToggle={vi.fn()}
          onChanged={onChanged}
          notify={notify}
          canOverride={false}
          {...props}
        />
      </IntlProvider>
    </MemoryRouter>,
  );
  return { onChanged, notify };
};

const refuseWith = (status, body) =>
  postToOpenElisServerFullResponse.mockImplementation((_url, _body, callback) =>
    callback({ ok: false, status, json: async () => body }),
  );

beforeEach(() => {
  getFromOpenElisServer.mockReset();
  postToOpenElisServerFullResponse.mockReset();
  postToOpenElisServerJsonResponse.mockReset();
  putToOpenElisServerFullResponse.mockReset();
  getFromOpenElisServer.mockImplementation((url, callback) => {
    if (url.includes("setup-options")) {
      callback(setupOptions);
      return;
    }
    callback(null);
  });
});

describe("BatchTile release", () => {
  it("opens the override dialog with the overdue copy and no proceed without the permission", async () => {
    renderTile(draftBatch("OVERDUE"));

    fireEvent.click(screen.getByRole("button", { name: "Generate workplan" }));

    expect(
      await screen.findByText("QC not current for batch"),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "You do not have permission to override QC. Contact your lab manager.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: /proceed and create nce/i }),
    ).toBeDisabled();
    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
  });

  it("opens the dialog from the server's answer when QC changed after the tile loaded", async () => {
    refuseWith(409, { reason: "OVERRIDE_REQUIRED", qc: qc("FAILED") });
    renderTile(draftBatch("PASS"));

    fireEvent.click(screen.getByRole("button", { name: "Generate workplan" }));

    expect(
      await screen.findByText("Most recent QC failed"),
    ).toBeInTheDocument();
    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/batch-workplans/batches/7/generate",
      JSON.stringify({ override: false }),
      expect.any(Function),
    );
  });

  it("proceeds with the override when the user holds the permission", async () => {
    renderTile(draftBatch("VIOLATION"), { canOverride: true });

    fireEvent.click(screen.getByRole("button", { name: "Generate workplan" }));
    const proceed = await screen.findByRole("button", {
      name: /proceed and create nce/i,
    });
    expect(screen.getByText("Active Westgard violation")).toBeInTheDocument();
    postToOpenElisServerFullResponse.mockImplementation(
      (_url, _body, callback) =>
        callback({
          ok: true,
          json: async () => ({ releases: [{ nceNumber: "NCE-0009" }] }),
        }),
    );
    fireEvent.click(proceed);

    expect(postToOpenElisServerFullResponse).toHaveBeenLastCalledWith(
      "/rest/batch-workplans/batches/7/generate",
      JSON.stringify({ override: true }),
      expect.any(Function),
    );
  });

  it("disables release when no current control lot covers the batch", () => {
    renderTile(draftBatch("LOT_EXPIRED"));

    expect(
      screen.getByRole("button", { name: "Generate workplan" }),
    ).toBeDisabled();
    expect(
      screen.getByText(/so it cannot be released\. Contact your QC officer\./),
    ).toBeInTheDocument();
  });

  it("disables release until a reagent lot is assigned", () => {
    const batch = draftBatch(undefined);
    batch.reagentLotId = null;
    renderTile(batch);

    expect(
      screen.getByRole("button", { name: "Generate workplan" }),
    ).toBeDisabled();
    expect(
      screen.getByText(
        "Assign a reagent and lot before generating a workplan.",
      ),
    ).toBeInTheDocument();
  });
});

describe("BatchTile NCE tag", () => {
  const released = (releases) => ({
    ...draftBatch(undefined),
    status: "ACTIVE",
    releases,
  });

  it("shows the NCE of the latest release", () => {
    renderTile(
      released([
        { controlLotId: "lot-1", releasedAt: 200, nceNumber: "NCE-0009" },
      ]),
    );
    expect(screen.getByText("NCE: NCE-0009")).toBeInTheDocument();
  });

  it("drops an earlier override's NCE once the batch is released again on Pass", () => {
    renderTile(
      released([
        { controlLotId: "lot-1", releasedAt: 300, qcStatus: "PASS" },
        { controlLotId: "lot-1", releasedAt: 200, nceNumber: "NCE-0009" },
      ]),
    );
    expect(screen.queryByText("NCE: NCE-0009")).not.toBeInTheDocument();
  });
});

describe("BatchTile setup", () => {
  it("offers the reagent's usable lots with the first-expiry lot suggested and saves the choice", async () => {
    putToOpenElisServerFullResponse.mockImplementation(
      (_url, _body, callback) => callback({ ok: true }),
    );
    const batch = draftBatch(undefined);
    batch.reagentId = null;
    batch.reagentLotId = null;
    const { onChanged } = renderTile(batch);

    fireEvent.change(screen.getByLabelText("Reagent"), {
      target: { value: "70" },
    });
    expect(screen.getByText("R-80")).toBeInTheDocument();
    expect(screen.getByText("FEFO Suggested")).toBeInTheDocument();
    fireEvent.click(screen.getByText("R-81"));
    fireEvent.change(screen.getByLabelText("Instrument"), {
      target: { value: "9" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Save reagent and lot" }),
    );

    expect(putToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/batch-workplans/batches/7/reagent",
      JSON.stringify({ reagentId: 70, reagentLotId: 81, instrumentId: "9" }),
      expect.any(Function),
    );
    await waitFor(() => expect(onChanged).toHaveBeenCalled());
  });
});

describe("BatchTile inline QC", () => {
  it("records a bench control through the bench QC capture and polls the batch's QC", async () => {
    postToOpenElisServerJsonResponse.mockImplementation(
      (_url, _body, callback) => callback({ id: "run-9" }),
    );
    const { onChanged } = renderTile(draftBatch("NOT_RUN"));

    fireEvent.click(screen.getByRole("button", { name: "Run QC" }));
    fireEvent.change(screen.getByLabelText("Value"), {
      target: { value: "101.5" },
    });
    fireEvent.click(screen.getByText("Pass"));
    fireEvent.click(screen.getByRole("button", { name: "Save QC result" }));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const [url, body] = postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/qc/results");
    expect(JSON.parse(body)).toMatchObject({
      source: "MANUAL",
      qualitativeOutcome: "PASS",
      resultValue: "101.5",
      controlLotId: "lot-1",
      testId: "1",
      testSectionId: "5",
    });
    expect(JSON.parse(body).runDateTime).toMatch(
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:00$/,
    );
    await waitFor(() =>
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        "/rest/batch-workplans/batches/7/qc-status",
        expect.any(Function),
      ),
    );
    getFromOpenElisServer.mock.calls
      .filter(([pollUrl]) => pollUrl.endsWith("qc-status"))
      .forEach(([, callback]) => callback({ evaluationPending: false }));
    await waitFor(() => expect(onChanged).toHaveBeenCalled());
  });

  it("says what is missing and sends nothing when the value or result is empty", () => {
    renderTile(draftBatch("OVERDUE"));

    fireEvent.click(screen.getByRole("button", { name: "Run QC" }));
    fireEvent.click(screen.getByRole("button", { name: "Save QC result" }));

    expect(
      screen.getByText("A numeric QC value is required."),
    ).toBeInTheDocument();
    expect(screen.getByText("Select Pass or Fail.")).toBeInTheDocument();
    expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  it("offers no inline entry for an analyzer's control lot", () => {
    const batch = draftBatch("NOT_RUN");
    batch.qc = qc("NOT_RUN", {
      instrumentId: "9",
      instrumentName: "Sysmex",
      lots: [lot({ bench: false, canRecordInline: false, instrumentId: "9" })],
    });
    renderTile(batch);

    expect(
      screen.queryByRole("button", { name: "Run QC" }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByText(/QC for Sysmex is recorded from the analyzer/),
    ).toBeInTheDocument();
  });
});
