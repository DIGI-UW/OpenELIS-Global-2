import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
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
  currentQuantity: 3,
  inventoryItem: { id: "MALARIA_RDT" },
  location: null,
};

beforeEach(() => {
  vi.clearAllMocks();
  InventoryItemAPI.getById.mockResolvedValue({
    id: "MALARIA_RDT",
    name: "Malaria RDT",
    itemType: "RDT",
    units: "kits",
  });
  InventoryItemAPI.getItemTypes.mockResolvedValue([
    { code: "REAGENT", label: "Reagent" },
    { code: "RDT", label: "RDT (Rapid Diagnostic Test)" },
    { code: "CONTROL", label: "Control Material" },
  ]);
});

describe("InventoryDashboard type filter", () => {
  it("sources the type filter options from the admin-managed item types API, not a hardcoded list", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([]);
    renderDashboard();

    await waitFor(() => {
      expect(InventoryItemAPI.getItemTypes).toHaveBeenCalled();
    });

    // "Control Material" only exists via the mocked API response — if it
    // appears once the dropdown is opened, the options came from the API,
    // not the old hardcoded REAGENT/RDT/CARTRIDGE array.
    fireEvent.click(document.querySelector("#type-filter button"));
    expect(await screen.findByText("Control Material")).toBeInTheDocument();
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
