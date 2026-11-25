import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LotDetailsPanel from "./LotDetailsPanel";
import {
  InventoryLotStorageAPI,
  TransactionAPI,
  UsageAPI,
} from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  postToOpenElisServerForBlob: vi.fn(),
}));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

// Spy on the real API objects: vi.spyOn throws when the method is missing,
// so a wrapper the panel depends on cannot be mocked into existence.
beforeEach(() => {
  vi.restoreAllMocks();
  vi.spyOn(TransactionAPI, "getByLot").mockResolvedValue([]);
  vi.spyOn(UsageAPI, "getByLot").mockResolvedValue([]);
  vi.spyOn(InventoryLotStorageAPI, "getMovements").mockResolvedValue([]);
});

describe("LotDetailsPanel — storage location visibility (OGC-657)", () => {
  const baseLot = {
    id: 7000,
    lotNumber: "OGC657-LOT-001",
    inventoryItem: { name: "Malaria RDT", itemType: "RDT", units: "kits" },
    qcStatus: "PASSED",
    initialQuantity: 10,
    currentQuantity: 10,
    receiptDate: "2026-01-01",
    expirationDate: "2026-12-31",
  };

  it("shows the hierarchical path when the lot has an assigned location", async () => {
    const lot = {
      ...baseLot,
      location: { hierarchicalPath: "Main Lab > Freezer 1" },
    };
    renderWithIntl(<LotDetailsPanel open lot={lot} onClose={vi.fn()} />);

    expect(await screen.findByText("Main Lab > Freezer 1")).toBeInTheDocument();
  });

  it("shows 'Not assigned' when the lot has no location", async () => {
    const lot = { ...baseLot, location: null };
    renderWithIntl(<LotDetailsPanel open lot={lot} onClose={vi.fn()} />);

    await waitFor(() => expect(TransactionAPI.getByLot).toHaveBeenCalled());
    expect(await screen.findByText(/not assigned/i)).toBeInTheDocument();
  });
});

describe("LotDetailsPanel — movement history (OGC-657)", () => {
  const baseLot = {
    id: 7000,
    lotNumber: "OGC657-LOT-001",
    inventoryItem: { name: "Malaria RDT", itemType: "RDT", units: "kits" },
    qcStatus: "PASSED",
    initialQuantity: 10,
    currentQuantity: 10,
    receiptDate: "2026-01-01",
    expirationDate: "2026-12-31",
  };

  it("renders the lot's movement rows from the movements endpoint", async () => {
    InventoryLotStorageAPI.getMovements.mockResolvedValue([
      {
        id: 1,
        previousLocationType: "device",
        previousLocationId: 7000,
        previousPositionCoordinate: null,
        newLocationType: "box",
        newLocationId: 7000,
        newPositionCoordinate: "A1",
        movedByUserName: "Ana Tester",
        movementDate: "2026-02-01T10:00:00",
        reason: "Consolidating stock",
      },
    ]);

    renderWithIntl(<LotDetailsPanel open lot={baseLot} onClose={vi.fn()} />);

    await waitFor(() =>
      expect(InventoryLotStorageAPI.getMovements).toHaveBeenCalledWith(7000),
    );
    expect(await screen.findByText("device #7000")).toBeInTheDocument();
    expect(screen.getByText("box #7000 (A1)")).toBeInTheDocument();
    expect(screen.getByText("Ana Tester")).toBeInTheDocument();
    expect(screen.getByText("Consolidating stock")).toBeInTheDocument();
  });

  it("shows the empty state when the lot has never been moved", async () => {
    renderWithIntl(<LotDetailsPanel open lot={baseLot} onClose={vi.fn()} />);

    expect(
      await screen.findByText(/no movements recorded for this lot/i),
    ).toBeInTheDocument();
  });
});

describe("LotDetailsPanel in the reader language", () => {
  const baseLot = {
    id: 7002,
    lotNumber: "LOT-2025-002",
    inventoryItem: { name: "Test Reagent A", itemType: "REAGENT", units: "mL" },
    qcStatus: "PASSED",
    initialQuantity: 10,
    currentQuantity: 9,
    receiptDate: "2026-01-01",
    expirationDate: "2026-12-31",
  };

  const french = {
    ...messages,
    "button.close": "Fermer",
    "lot.details.loading": "Chargement du lot...",
    "lot.details.tabs": "Onglets du lot",
    "transaction.type.RECEIPT": "Réception",
    "transaction.type.QC_TEST": "Contrôle qualité",
  };

  const renderInFrench = () =>
    render(
      <IntlProvider locale="fr" messages={french}>
        <LotDetailsPanel open lot={baseLot} onClose={vi.fn()} />
      </IntlProvider>,
    );

  it("names each transaction type, the close button and the tab list from the message bundle", async () => {
    TransactionAPI.getByLot.mockResolvedValue([
      {
        id: 1,
        transactionType: "RECEIPT",
        quantityChange: 10,
        transactionDate: "2026-01-01T09:00:00",
        performedByUser: "admin",
      },
      {
        id: 2,
        transactionType: "QC_TEST",
        quantityChange: -1,
        transactionDate: "2026-01-02T09:00:00",
        performedByUser: "admin",
      },
    ]);
    renderInFrench();

    expect(await screen.findByText("Réception")).toBeInTheDocument();
    expect(screen.getByText("Contrôle qualité")).toBeInTheDocument();
    expect(screen.queryByText("RECEIPT")).not.toBeInTheDocument();
    expect(screen.queryByText("QC_TEST")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Fermer" })).toBeInTheDocument();
    expect(
      screen.getByRole("tablist", { name: "Onglets du lot" }),
    ).toBeInTheDocument();
  });

  it("names the loading indicator from the message bundle", () => {
    TransactionAPI.getByLot.mockReturnValue(new Promise(() => {}));
    renderInFrench();

    expect(screen.getByTitle("Chargement du lot...")).toBeInTheDocument();
  });
});

describe("LotDetailsPanel — barcode visibility", () => {
  const baseLot = {
    id: 7001,
    lotNumber: "LOT-2025-001",
    barcode: "TEST-REAGENT-A-LOT-2025-001",
    inventoryItem: { name: "Test Reagent A", itemType: "REAGENT", units: "mL" },
    qcStatus: "PASSED",
    initialQuantity: 10,
    currentQuantity: 10,
    receiptDate: "2026-01-01",
    expirationDate: "2026-12-31",
  };

  it("shows the generated barcode so it can be matched against a printed label", async () => {
    renderWithIntl(<LotDetailsPanel open lot={baseLot} onClose={vi.fn()} />);

    expect(
      await screen.findByText("TEST-REAGENT-A-LOT-2025-001"),
    ).toBeInTheDocument();
  });

  it("shows a dash rather than a blank row for a lot with no barcode", async () => {
    const { container } = renderWithIntl(
      <LotDetailsPanel
        open
        lot={{ ...baseLot, barcode: null }}
        onClose={vi.fn()}
      />,
    );

    await screen.findByText("LOT-2025-001");
    expect(container.querySelector(".lot-details-barcode").textContent).toBe(
      "-",
    );
  });
});
