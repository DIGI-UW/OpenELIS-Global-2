import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import InventoryManagement from "./InventoryManagement";
import { NotificationContext } from "../layout/Layout";
import { InventoryItemAPI, InventoryLotAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryItemAPI: {
    getAll: vi.fn(),
    getItemTypes: vi.fn(),
    getLowStock: vi.fn(),
  },
  InventoryLotAPI: { getAll: vi.fn() },
  InventoryLotStorageAPI: {},
}));

// Only the dashboard's refetch on tab activation is under test.
vi.mock("./InventoryCatalog", () => ({ default: () => null }));
vi.mock("./InventoryReports", () => ({ default: () => null }));
vi.mock("./LotEntryModal", () => ({ default: () => null }));
vi.mock("./RecordUsageModal", () => ({ default: () => null }));
vi.mock("./LotAdjustmentModal", () => ({ default: () => null }));
vi.mock("./DisposeLotModal", () => ({ default: () => null }));
vi.mock("./UpdateQCStatusModal", () => ({ default: () => null }));
vi.mock("./LotDetailsPanel", () => ({ default: () => null }));
vi.mock("../storage/LocationPicker/LocationPickerModal", () => ({
  default: () => null,
}));

const notificationContext = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  notifications: [],
  addNotification: vi.fn(),
  removeNotification: vi.fn(),
};

beforeEach(() => {
  vi.clearAllMocks();
  InventoryItemAPI.getAll.mockResolvedValue([]);
  InventoryItemAPI.getLowStock.mockResolvedValue([]);
  InventoryItemAPI.getItemTypes.mockResolvedValue([]);
  InventoryLotAPI.getAll.mockResolvedValue([]);
});

describe("InventoryManagement tabs", () => {
  // Carbon keeps unselected TabPanels mounted, so a threshold edited in the
  // Catalog tab is stale on the Dashboard until it reloads its data.
  it("reloads the dashboard when the user returns to its tab", async () => {
    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <NotificationContext.Provider value={notificationContext}>
            <InventoryManagement />
          </NotificationContext.Provider>
        </IntlProvider>
      </MemoryRouter>,
    );

    await waitFor(() =>
      expect(InventoryLotAPI.getAll).toHaveBeenCalledTimes(1),
    );

    fireEvent.click(
      screen.getByRole("tab", { name: messages["inventory.tab.catalog"] }),
    );
    fireEvent.click(
      screen.getByRole("tab", { name: messages["inventory.tab.dashboard"] }),
    );

    await waitFor(() =>
      expect(InventoryLotAPI.getAll).toHaveBeenCalledTimes(2),
    );
  });
});
