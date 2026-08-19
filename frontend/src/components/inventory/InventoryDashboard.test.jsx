import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import InventoryDashboard from "./InventoryDashboard";
import { NotificationContext } from "../layout/Layout";
import {
  InventoryItemAPI,
  InventoryLotAPI,
  InventoryLotStorageAPI,
} from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryItemAPI: {
    getAll: vi.fn(),
    getById: vi.fn(),
    getItemTypes: vi.fn(),
    getLowStock: vi.fn(),
  },
  InventoryLotAPI: {
    getAll: vi.fn(),
  },
  InventoryLotStorageAPI: {
    getLocation: vi.fn(),
    assignLocation: vi.fn(),
    moveLocation: vi.fn(),
  },
}));

// LotEntryModal and the other action modals pull in a lot of unrelated
// Carbon form machinery; only the dashboard table + Location column + the
// generalized LocationPickerModal wiring are under test here.
vi.mock("./LotEntryModal", () => ({ default: () => null }));
vi.mock("./RecordUsageModal", () => ({ default: () => null }));
vi.mock("./LotAdjustmentModal", () => ({ default: () => null }));
vi.mock("./DisposeLotModal", () => ({ default: () => null }));
vi.mock("./UpdateQCStatusModal", () => ({ default: () => null }));
vi.mock("./LotDetailsPanel", () => ({ default: () => null }));

// Stand in for the real LocationPickerModal: expose a single button that
// fires onConfirm with a canned payload, so we can assert on the wiring
// (which API method gets called, with what) without exercising Carbon's
// full picker UI (already covered by LocationPickerModal's own tests).
vi.mock("../storage/LocationPicker/LocationPickerModal", () => ({
  default: ({ isOpen, onConfirm }) =>
    isOpen ? (
      <button
        onClick={() =>
          onConfirm({
            selection: { room: { id: 9, name: "Cold Room" } },
            position: null,
            notes: "",
          })
        }
      >
        mock-confirm-location
      </button>
    ) : null,
}));

const mockNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  notifications: [],
  addNotification: vi.fn(),
  removeNotification: vi.fn(),
};

const renderDashboard = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={mockNotificationContext}>
        <InventoryDashboard />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

const lotWithLocation = {
  id: 1,
  lotNumber: "LOT-100",
  status: "ACTIVE",
  qcStatus: "PASSED",
  currentQuantity: 10,
  inventoryItem: { id: "MALARIA_RDT" },
  location: {
    hierarchicalPath: "Main Lab > Freezer 1",
    positionCoordinate: null,
  },
};

const lotWithoutLocation = {
  id: 2,
  lotNumber: "LOT-200",
  status: "ACTIVE",
  qcStatus: "PASSED",
  currentQuantity: 3,
  inventoryItem: { id: "MALARIA_RDT" },
  location: null,
};

beforeEach(() => {
  vi.clearAllMocks();
  const malariaRdt = {
    id: "MALARIA_RDT",
    name: "Malaria RDT",
    itemType: "RDT",
    units: "kits",
    lowStockThreshold: 20,
  };
  InventoryItemAPI.getById.mockResolvedValue(malariaRdt);
  InventoryItemAPI.getAll.mockResolvedValue([malariaRdt]);
  InventoryItemAPI.getLowStock.mockResolvedValue([]);
  InventoryItemAPI.getItemTypes.mockResolvedValue([
    "REAGENT",
    "RDT",
    "CARTRIDGE",
  ]);
});

describe("InventoryDashboard QC gate visibility", () => {
  // A received lot defaults to QC PENDING and FEFO will not consume it, so the
  // table has to say so rather than showing a reassuring "In Stock".
  const pendingQcLot = { ...lotWithLocation, qcStatus: "PENDING" };

  it("flags a QC-pending lot as Pending QC instead of In Stock", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([pendingQcLot]);
    renderDashboard();

    await screen.findByText("LOT-100");
    const table = document.querySelector("table");
    expect(within(table).getByText("Pending QC")).toBeInTheDocument();
    expect(within(table).queryByText("In Stock")).not.toBeInTheDocument();
  });

  it("shows each lot's QC status in its own column", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([lotWithLocation]);
    renderDashboard();

    await screen.findByText("LOT-100");
    const table = document.querySelector("table");
    expect(within(table).getByText("Passed")).toBeInTheDocument();
  });
});

describe("InventoryDashboard low stock", () => {
  // Regression: the tile and badge previously compared a lot's quantity to
  // `item.minimumStockLevel`, a field that does not exist on InventoryItem, so
  // the threshold was always 0 — the tile read 0 and the badge never rendered.
  it("reports the low-stock count from the backend, not a local recomputation", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([lotWithLocation]);
    InventoryItemAPI.getLowStock.mockResolvedValue([
      { id: "MALARIA_RDT", name: "Malaria RDT" },
    ]);
    renderDashboard();

    await waitFor(() =>
      expect(InventoryItemAPI.getLowStock).toHaveBeenCalled(),
    );
    const tile = document.querySelector(".metric-warning .metric-value");
    await waitFor(() => expect(tile).toHaveTextContent("1"));
  });

  // Scope badge assertions to the table: the metric tile's own label is also
  // "Low Stock", so an unscoped query matches it and passes either way.
  it("badges a lot's row as Low Stock when its item is below threshold", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([lotWithLocation]);
    InventoryItemAPI.getLowStock.mockResolvedValue([
      { id: "MALARIA_RDT", name: "Malaria RDT" },
    ]);
    renderDashboard();

    await screen.findByText("LOT-100");
    const table = document.querySelector("table");
    await waitFor(() =>
      expect(within(table).getByText("Low Stock")).toBeInTheDocument(),
    );
  });

  it("shows no low-stock badge when the backend reports nothing low", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([lotWithLocation]);
    InventoryItemAPI.getLowStock.mockResolvedValue([]);
    renderDashboard();

    await screen.findByText("LOT-100");
    const table = document.querySelector("table");
    expect(within(table).queryByText("Low Stock")).not.toBeInTheDocument();
  });

  it("loads items in one batched call rather than one request per lot", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([
      lotWithLocation,
      lotWithoutLocation,
    ]);
    renderDashboard();

    await screen.findByText("LOT-100");
    expect(InventoryItemAPI.getAll).toHaveBeenCalledTimes(1);
    expect(InventoryItemAPI.getById).not.toHaveBeenCalled();
  });
});

describe("InventoryDashboard type filter", () => {
  it("populates the type filter from the item types API with display labels", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([]);
    renderDashboard();

    await waitFor(() => {
      expect(InventoryItemAPI.getItemTypes).toHaveBeenCalled();
    });

    fireEvent.click(document.querySelector("#type-filter button"));
    expect(await screen.findByText("Analyzer Cartridge")).toBeInTheDocument();
  });
});

describe("InventoryDashboard Location column", () => {
  it("shows the lot's hierarchical path when a location is assigned", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([lotWithLocation]);
    renderDashboard();

    expect(await screen.findByText("Main Lab > Freezer 1")).toBeInTheDocument();
  });

  it("shows 'Not assigned' when the lot has no location", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([lotWithoutLocation]);
    renderDashboard();

    expect(await screen.findByText(/not assigned/i)).toBeInTheDocument();
  });

  it("assigns a location via the picker for an unassigned lot and refreshes the table", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([lotWithoutLocation]);
    InventoryLotStorageAPI.assignLocation.mockResolvedValue({
      assignmentId: "1",
    });
    renderDashboard();

    await screen.findByText("LOT-200");

    // The DataTable's sortable "Action" column header also matches an
    // accessible-name query for "action", so target the row-level
    // OverflowMenu trigger by its Carbon class instead.
    const overflowButton = document.querySelector("button.cds--overflow-menu");
    fireEvent.click(overflowButton);

    fireEvent.click(await screen.findByText(/assign storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));

    await waitFor(() => {
      expect(InventoryLotStorageAPI.assignLocation).toHaveBeenCalledWith(
        expect.objectContaining({
          inventoryLotId: "2",
          locationId: "9",
          locationType: "room",
        }),
      );
    });
    // Refetches after a successful assignment.
    expect(InventoryLotAPI.getAll).toHaveBeenCalledTimes(2);
  });

  it("moves an already-assigned lot via the picker", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([lotWithLocation]);
    InventoryLotStorageAPI.moveLocation.mockResolvedValue({
      movementId: "1",
    });
    renderDashboard();

    await screen.findByText("LOT-100");

    // The DataTable's sortable "Action" column header also matches an
    // accessible-name query for "action", so target the row-level
    // OverflowMenu trigger by its Carbon class instead.
    const overflowButton = document.querySelector("button.cds--overflow-menu");
    fireEvent.click(overflowButton);

    fireEvent.click(await screen.findByText(/move storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));

    await waitFor(() => {
      expect(InventoryLotStorageAPI.moveLocation).toHaveBeenCalledWith(
        expect.objectContaining({
          inventoryLotId: "1",
          locationId: "9",
          locationType: "room",
        }),
      );
    });
    expect(InventoryLotStorageAPI.assignLocation).not.toHaveBeenCalled();
  });
});
