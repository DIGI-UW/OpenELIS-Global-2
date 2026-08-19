import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LotDetailsPanel from "./LotDetailsPanel";
import { TransactionAPI, UsageAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  TransactionAPI: { getByLot: vi.fn() },
  UsageAPI: { getByLot: vi.fn() },
}));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  TransactionAPI.getByLot.mockResolvedValue([]);
  UsageAPI.getByLot.mockResolvedValue([]);
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

describe("LotDetailsPanel — barcode visibility", () => {
  const baseLot = {
    id: 7001,
    lotNumber: "LOT-2025-001",
    barcode: "TEST_REAGENT_A_LOT_2025_001",
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
      await screen.findByText("TEST_REAGENT_A_LOT_2025_001"),
    ).toBeInTheDocument();
  });
});
