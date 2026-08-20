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
import { generateManifestPDF } from "../../../../shipment/utils/pdfGenerator";

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

/** Exactly the keys GET /rest/eqa/cycles/{id}/shipments answers with. */
const ROWS = [
  {
    organizationId: 100,
    organizationName: "District Lab A",
    boxId: 5,
    boxCode: "EQA-C7-100",
    boxState: "READY_TO_SEND",
    courier: "DHL",
    trackingNumber: "TRK-A",
    estimatedDeliveryDate: "2026-09-01 00:00:00",
    shipmentStatus: "PENDING",
    shippedDate: null,
  },
  {
    organizationId: 101,
    organizationName: "District Lab B",
    boxId: null,
    boxCode: null,
    boxState: null,
    courier: null,
    trackingNumber: null,
    estimatedDeliveryDate: null,
    shipmentStatus: null,
    shippedDate: null,
  },
];

const renderWorkbench = (prep = PREP_SHORT, rows = ROWS, samplesByUrl = {}) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.endsWith("/prep")) cb(prep);
    else if (url.endsWith("/shipments")) cb(rows);
    else if (url in samplesByUrl) cb(samplesByUrl[url]);
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
 * Carbon renders this button's hint as a title attribute, so the accessible name
 * is the hint rather than the label — reach it through its visible text.
 */
const readyToShipButton = () =>
  screen.getByText("Mark cycle ready to ship").closest("button");

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

  test("ready-to-ship is offered as soon as the server allows it", () => {
    renderWorkbench({ ...PREP_CLEAR, readyToShipAllowed: true });
    expect(readyToShipButton()).toBeEnabled();
  });

  test("a refused ready-to-ship shows the server's own reason", async () => {
    renderWorkbench({ ...PREP_SHORT, readyToShipAllowed: true });
    patchToOpenElisServerFullResponse.mockImplementation((_u, _p, cb) =>
      cb({
        ok: false,
        json: () =>
          Promise.resolve({
            error:
              "Cannot ship yet: Panel HIV VL panel needs 4 aliquots, has 3",
          }),
      }),
    );

    fireEvent.click(readyToShipButton());

    expect(
      await screen.findByText(
        "Cannot ship yet: Panel HIV VL panel needs 4 aliquots, has 3",
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

    // Lab B has no box yet, so its row cannot be selected for dispatch. Reached by
    // label, which is also the assertion that each row control has an accessible
    // name rather than an empty one.
    expect(
      screen.getByLabelText("Select District Lab A for dispatch"),
    ).toBeEnabled();
    expect(
      screen.getByLabelText("Select District Lab B for dispatch"),
    ).toBeDisabled();
    expect(
      screen.getByLabelText("Courier for District Lab A"),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText("Tracking number for District Lab A"),
    ).toBeInTheDocument();
  });

  test("a pack list is refused rather than produced empty when samples cannot be read", async () => {
    renderWorkbench(PREP_SHORT, ROWS, {
      "/rest/eqa/panels/11/samples": undefined,
    });
    fireEvent.click(screen.getByRole("tab", { name: "Shipments" }));

    // A failed read answers undefined, which looks exactly like an empty panel — a
    // courier must not be handed a blank manifest.
    fireEvent.click(screen.getAllByText("Pack list")[0].closest("button"));

    expect(
      await screen.findByText(
        "The panel samples could not be read, so no pack list was produced. Reload and try again.",
      ),
    ).toBeInTheDocument();
    expect(generateManifestPDF).not.toHaveBeenCalled();
  });

  test("one unreadable panel refuses the whole pack list, never a partial one", async () => {
    // Two-panel cycle, second panel's read fails. Listing only the first panel
    // would hand the courier a manifest for 2 of 4 samples, unmarked.
    const twoPanels = {
      ...PREP_SHORT,
      panels: [
        PREP_SHORT.panels[0],
        { ...PREP_SHORT.panels[0], panelId: 12, panelName: "Syphilis panel" },
      ],
    };
    renderWorkbench(twoPanels, ROWS, {
      "/rest/eqa/panels/11/samples": [
        { id: 1, blindCode: "BLIND-1", analyteName: "HIV-1 RNA" },
        { id: 2, blindCode: "BLIND-2", analyteName: "HIV-1 RNA" },
      ],
      "/rest/eqa/panels/12/samples": undefined,
    });
    fireEvent.click(screen.getByRole("tab", { name: "Shipments" }));

    fireEvent.click(screen.getAllByText("Pack list")[0].closest("button"));

    expect(
      await screen.findByText(
        "The panel samples could not be read, so no pack list was produced. Reload and try again.",
      ),
    ).toBeInTheDocument();
    expect(generateManifestPDF).not.toHaveBeenCalled();
  });

  test("a complete read produces the manifest with every panel's samples", async () => {
    renderWorkbench(PREP_SHORT, ROWS, {
      "/rest/eqa/panels/11/samples": [
        { id: 1, blindCode: "BLIND-1", analyteName: "HIV-1 RNA" },
        { id: 2, sampleCode: "SC-2", analyteName: "HIV-1 RNA" },
      ],
    });
    fireEvent.click(screen.getByRole("tab", { name: "Shipments" }));

    fireEvent.click(screen.getAllByText("Pack list")[0].closest("button"));

    await vi.waitFor(() =>
      expect(generateManifestPDF).toHaveBeenCalledTimes(1),
    );
    const manifest = generateManifestPDF.mock.calls[0][0];
    expect(manifest.samples).toHaveLength(2);
    // Blind code preferred over the real sample code; never a target value.
    expect(manifest.samples[0].accessionNumber).toBe("BLIND-1");
    expect(manifest.samples[1].accessionNumber).toBe("SC-2");
    expect(JSON.stringify(manifest)).not.toContain("targetValue");
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
});
