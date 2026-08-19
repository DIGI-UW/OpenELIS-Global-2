import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../../../../languages/en.json";
import ProviderWorkbenchPage from "../ProviderWorkbenchPage";
import {
  getFromOpenElisServer,
  patchToOpenElisServerFullResponse,
  postToOpenElisServerFullResponse,
} from "../../../../utils/Utils";

vi.mock("../../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  patchToOpenElisServerFullResponse: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  resolveApiErrorMessage: (_intl, payload, fallbackId) =>
    payload?.error || fallbackId,
  toLocalIsoDate: (d) => (d ? "2026-09-01" : ""),
}));

vi.mock("../../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

vi.mock("../../../../shipment/utils/pdfGenerator", () => ({
  generateManifestPDF: vi.fn(),
  generateLabelPDF: vi.fn(),
}));

const PREP_SHORT = {
  cycleId: 7,
  cycleName: "2026 Round 1",
  cycleStatus: "PREP_IN_PROGRESS",
  participantCount: 2,
  panels: [
    {
      panelId: 11,
      panelName: "HIV VL panel",
      sampleCount: 2,
      aliquotsProduced: 3,
      aliquotsReserved: 0,
      aliquotsShipped: 0,
      aliquotsNeeded: 4,
      shortfall: 1,
      homogeneityQcPassed: false,
      homogeneityQcNotes: "",
    },
  ],
  blockers: [
    "Panel HIV VL panel has not passed homogeneity QC",
    "Panel HIV VL panel needs 4 aliquots, has 3",
  ],
  readyToShipAllowed: false,
};

const PREP_CLEAR = {
  ...PREP_SHORT,
  cycleStatus: "READY_TO_SHIP",
  panels: [
    {
      ...PREP_SHORT.panels[0],
      aliquotsProduced: 4,
      shortfall: 0,
      homogeneityQcPassed: true,
    },
  ],
  blockers: [],
  readyToShipAllowed: false,
};

const ROWS = [
  {
    cycleId: 7,
    enrollmentId: 1,
    organizationId: 100,
    organizationName: "District Lab A",
    boxId: 5,
    boxCode: "EQA-C7-100",
    boxState: "READY_TO_SEND",
    courier: "DHL",
    trackingNumber: "TRK-A",
    estimatedDeliveryDate: "2026-09-01 00:00:00",
    shipmentId: 5,
    shipmentStatus: "PENDING",
    shippedDate: null,
  },
  {
    cycleId: 7,
    enrollmentId: 2,
    organizationId: 101,
    organizationName: "District Lab B",
    boxId: null,
    boxCode: null,
    boxState: null,
    courier: null,
    trackingNumber: null,
    estimatedDeliveryDate: null,
    shipmentId: null,
    shipmentStatus: null,
    shippedDate: null,
  },
];

const renderWorkbench = (prep = PREP_SHORT, rows = ROWS) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.endsWith("/prep")) cb(prep);
    else if (url.endsWith("/shipments")) cb(rows);
    else cb([]);
  });
  return render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={["/qa/eqa/provider/cycles/7/workbench"]}>
        <Route path="/qa/eqa/provider/cycles/:cycleId/workbench">
          <ProviderWorkbenchPage />
        </Route>
      </MemoryRouter>
    </IntlProvider>,
  );
};

/**
 * Carbon renders the button's hint as a title and hides checkbox inputs behind
 * a visually-hidden class, so both are reached the way a user does: through
 * their visible text / label, not by accessible-name lookup.
 */
const readyToShipButton = () =>
  screen.getByText("Mark cycle ready to ship").closest("button");

const renderPicker = (cycles) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/eqa/provider/cycles") cb(cycles);
    else cb([]);
  });
  return render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={["/qa/eqa/provider/workbench"]}>
        <Route path="/qa/eqa/provider/workbench">
          <ProviderWorkbenchPage />
        </Route>
      </MemoryRouter>
    </IntlProvider>,
  );
};

describe("ProviderWorkbenchPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("prep tab shows the shortfall and every gate blocker", () => {
    renderWorkbench();

    expect(screen.getByText("1 short")).toBeInTheDocument();
    expect(
      screen.getByText("Panel HIV VL panel needs 4 aliquots, has 3"),
    ).toBeInTheDocument();
  });

  test("ready-to-ship is disabled until the server says the gate is clear", () => {
    renderWorkbench();
    expect(readyToShipButton()).toBeDisabled();
  });

  test("ready-to-ship is enabled only while the cycle is still in prep", () => {
    // Gate arithmetic satisfied, but the cycle already left prep_in_progress —
    // the transition would be an illegal edge, so the button stays disabled.
    renderWorkbench({ ...PREP_CLEAR });
    expect(readyToShipButton()).toBeDisabled();
  });

  test("a refused ready-to-ship shows the server's own reason", async () => {
    renderWorkbench({ ...PREP_SHORT, readyToShipAllowed: true });
    patchToOpenElisServerFullResponse.mockImplementation((_u, _p, cb) =>
      cb({
        ok: false,
        json: () =>
          Promise.resolve({
            error:
              "Cannot ship until panel HIV VL panel has 4 aliquots produced (has 3)",
          }),
      }),
    );

    fireEvent.click(readyToShipButton());

    expect(
      await screen.findByText(
        "Cannot ship until panel HIV VL panel has 4 aliquots produced (has 3)",
      ),
    ).toBeInTheDocument();
  });

  test("shipments tab lists one row per participant, box or not", () => {
    renderWorkbench();
    fireEvent.click(screen.getByRole("tab", { name: "Shipments" }));

    expect(screen.getByText("District Lab A")).toBeInTheDocument();
    expect(screen.getByText("District Lab B")).toBeInTheDocument();
    expect(screen.getByText("EQA-C7-100")).toBeInTheDocument();
  });

  test("only participants with a prepared box can be dispatched", () => {
    renderWorkbench();
    fireEvent.click(screen.getByRole("tab", { name: "Shipments" }));

    // Lab B has no box yet, so its row cannot be selected for dispatch.
    expect(document.getElementById("select-100")).not.toBeDisabled();
    expect(document.getElementById("select-101")).toBeDisabled();
  });

  test("dispatch posts the selected participants and reports the count", async () => {
    renderWorkbench();
    fireEvent.click(screen.getByRole("tab", { name: "Shipments" }));
    postToOpenElisServerFullResponse.mockImplementation((_u, _p, cb) =>
      cb({ ok: true, json: () => Promise.resolve([ROWS[0]]) }),
    );

    fireEvent.click(screen.getByLabelText("Select all pending"));
    fireEvent.click(screen.getByText("Mark 1 shipped").closest("button"));

    expect(postToOpenElisServerFullResponse).toHaveBeenCalledWith(
      "/rest/eqa/cycles/7/shipments/ship",
      JSON.stringify({ organizationIds: [100] }),
      expect.any(Function),
    );
    expect(
      await screen.findByText("1 participant shipments dispatched."),
    ).toBeInTheDocument();
  });

  test("the picker lists provider cycles when no cycle is in the URL", () => {
    renderPicker([
      {
        id: 7,
        cycleNumber: 1,
        cycleName: "2026 Round 1",
        status: "PREP_IN_PROGRESS",
        schemeName: "National HIV VL PT",
        participantCount: 2,
        panelCount: 1,
      },
    ]);

    expect(screen.getByText("2026 Round 1")).toBeInTheDocument();
    expect(screen.getByText("National HIV VL PT")).toBeInTheDocument();
    expect(screen.getByText("Prep in progress")).toBeInTheDocument();
  });
});
