import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import InventoryCatalog from "./InventoryCatalog";
import { NotificationContext } from "../layout/Layout";
import { InventoryItemAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryItemAPI: {
    getAll: vi.fn(),
    getItemTypes: vi.fn(),
    activate: vi.fn(),
    deactivate: vi.fn(),
  },
}));

// The Add/Edit modal pulls in a lot of unrelated Carbon form machinery; only
// the catalog table's Code column + search are under test here.
vi.mock("./InventoryItemForm", () => ({ default: () => null }));

const mockNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  notifications: [],
  addNotification: vi.fn(),
  removeNotification: vi.fn(),
};

const renderCatalog = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={mockNotificationContext}>
        <InventoryCatalog />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

const ITEMS = [
  {
    id: "REAGENT_A",
    name: "Reagent A",
    itemType: "REAGENT",
    units: "mL",
    lowStockThreshold: 10,
    isActive: "Y",
  },
  {
    id: "RDT_KIT",
    name: "RDT Kit",
    itemType: "RDT",
    units: "kits",
    lowStockThreshold: 5,
    isActive: "Y",
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  InventoryItemAPI.getItemTypes.mockResolvedValue(["REAGENT", "RDT"]);
  InventoryItemAPI.getAll.mockResolvedValue(ITEMS);
});

describe("InventoryCatalog — Code column and search (OGC-658 Part C)", () => {
  it("shows a Code column with each item's stable code", async () => {
    renderCatalog();

    expect(await screen.findByText("REAGENT_A")).toBeInTheDocument();
    expect(screen.getByText("RDT_KIT")).toBeInTheDocument();
  });

  it("filters the table when searching by code", async () => {
    renderCatalog();
    await screen.findByText("REAGENT_A");

    const search = screen.getByPlaceholderText(/search by item name or code/i);
    fireEvent.change(search, { target: { value: "RDT_KIT" } });

    expect(screen.queryByText("Reagent A")).not.toBeInTheDocument();
    expect(screen.getByText("RDT Kit")).toBeInTheDocument();
  });

  it("still filters by item name (unaffected by the code search addition)", async () => {
    renderCatalog();
    await screen.findByText("REAGENT_A");

    const search = screen.getByPlaceholderText(/search by item name or code/i);
    fireEvent.change(search, { target: { value: "Reagent A" } });

    expect(screen.getByText("Reagent A")).toBeInTheDocument();
    expect(screen.queryByText("RDT Kit")).not.toBeInTheDocument();
  });
});
