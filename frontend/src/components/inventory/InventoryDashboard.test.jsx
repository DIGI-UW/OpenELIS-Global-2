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
import { resolveMessagesForLocale } from "../../languages";

vi.mock("./InventoryService", () => ({
  InventoryItemAPI: {
    getAll: vi.fn(),
    getById: vi.fn(),
    getItemTypes: vi.fn(),
    getLowStock: vi.fn(),
  },
  InventoryLotAPI: {
    getAll: vi.fn(),
    printLabel: vi.fn(),
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
            reason: "Consolidating stock",
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

  it("labels a QC-failed lot Failed QC rather than Pending QC", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([
      { ...lotWithLocation, qcStatus: "FAILED" },
    ]);
    renderDashboard();

    await screen.findByText("LOT-100");
    const table = document.querySelector("table");
    expect(within(table).getByText("Failed QC")).toBeInTheDocument();
    expect(within(table).queryByText("Pending QC")).not.toBeInTheDocument();
  });

  it("labels a quarantined lot Quarantined rather than Pending QC", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([
      { ...lotWithLocation, qcStatus: "QUARANTINED" },
    ]);
    renderDashboard();

    await screen.findByText("LOT-100");
    const table = document.querySelector("table");
    // Once in the QC Status column, once in Stock Status.
    expect(within(table).getAllByText("Quarantined")).toHaveLength(2);
    expect(within(table).queryByText("Pending QC")).not.toBeInTheDocument();
  });

  it("labels a lot quarantined by status Quarantined though its QC is pending", async () => {
    // What a lot received into quarantine looks like: the consume API calls this
    // quarantined, so the dashboard must not call it Pending QC.
    InventoryLotAPI.getAll.mockResolvedValue([
      { ...lotWithLocation, status: "QUARANTINED", qcStatus: "PENDING" },
    ]);
    renderDashboard();

    await screen.findByText("LOT-100");
    const table = document.querySelector("table");
    expect(within(table).getByText("Quarantined")).toBeInTheDocument();
    expect(within(table).queryByText("Pending QC")).not.toBeInTheDocument();
  });

  it("shows each lot's QC status in its own column", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([lotWithLocation]);
    renderDashboard();

    await screen.findByText("LOT-100");
    const table = document.querySelector("table");
    expect(within(table).getByText("Passed")).toBeInTheDocument();
  });

  it("flags a QC-pending lot as Pending QC even when its item is low on stock", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([pendingQcLot]);
    InventoryItemAPI.getLowStock.mockResolvedValue([
      { id: "MALARIA_RDT", name: "Malaria RDT" },
    ]);
    renderDashboard();

    await screen.findByText("LOT-100");
    const table = document.querySelector("table");
    expect(within(table).getByText("Pending QC")).toBeInTheDocument();
    expect(within(table).queryByText("Low Stock")).not.toBeInTheDocument();
  });
});

describe("InventoryDashboard status filter", () => {
  it("filters the rows client-side and offers Disposed", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([
      lotWithLocation,
      { ...lotWithoutLocation, lotNumber: "LOT-GONE", status: "DISPOSED" },
    ]);
    renderDashboard();

    await screen.findByText("LOT-GONE");
    fireEvent.click(
      document.querySelector("#inventory-dashboard-status-filter button"),
    );
    fireEvent.click(await screen.findByRole("option", { name: "Disposed" }));

    const table = document.querySelector("table");
    expect(within(table).getByText("LOT-GONE")).toBeInTheDocument();
    expect(within(table).queryByText("LOT-100")).not.toBeInTheDocument();
    // The controller ignores ?status=, so the filter must not refetch.
    expect(InventoryLotAPI.getAll).toHaveBeenCalledTimes(1);
    expect(InventoryLotAPI.getAll).toHaveBeenCalledWith();
  });
});

describe("InventoryDashboard tab activation", () => {
  it("refetches when it becomes the active tab again", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([lotWithLocation]);
    const wrap = (active) => (
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider value={mockNotificationContext}>
          <InventoryDashboard active={active} />
        </NotificationContext.Provider>
      </IntlProvider>
    );
    const { rerender } = render(wrap(true));

    await screen.findByText("LOT-100");
    expect(InventoryLotAPI.getAll).toHaveBeenCalledTimes(1);

    rerender(wrap(false));
    rerender(wrap(true));

    await waitFor(() =>
      expect(InventoryLotAPI.getAll).toHaveBeenCalledTimes(2),
    );
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

    fireEvent.click(
      document.querySelector("#inventory-dashboard-type-filter button"),
    );
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
          reason: "Consolidating stock",
        }),
      );
    });
    expect(InventoryLotStorageAPI.assignLocation).not.toHaveBeenCalled();
  });

  it("acts on the clicked row after the table has been re-sorted", async () => {
    // Fetch order is the reverse of lot-number order, so sorting by Lot
    // Number moves the assigned lot (id 1) into the first rendered row
    // while the fetch-order array still holds the unassigned lot there.
    InventoryLotAPI.getAll.mockResolvedValue([
      { ...lotWithoutLocation, lotNumber: "ZZZ-200" },
      { ...lotWithLocation, lotNumber: "AAA-100" },
    ]);
    InventoryLotStorageAPI.moveLocation.mockResolvedValue({ movementId: "1" });
    renderDashboard();

    await screen.findByText("ZZZ-200");
    fireEvent.click(screen.getByText("Lot Number"));

    const lotNumberCells = document.querySelectorAll(
      "tbody tr td:nth-child(2)",
    );
    expect(lotNumberCells[0]).toHaveTextContent("AAA-100");

    fireEvent.click(document.querySelectorAll("button.cds--overflow-menu")[0]);

    // The assigned lot's row offers Move, not Assign.
    fireEvent.click(await screen.findByText(/move storage location/i));
    fireEvent.click(await screen.findByText("mock-confirm-location"));

    await waitFor(() => {
      expect(InventoryLotStorageAPI.moveLocation).toHaveBeenCalledWith(
        expect.objectContaining({ inventoryLotId: "1", locationId: "9" }),
      );
    });
    expect(InventoryLotStorageAPI.assignLocation).not.toHaveBeenCalled();
  });
});

describe("InventoryDashboard barcode search", () => {
  const barcodedLot = { ...lotWithLocation, barcode: "BC-ALPHA-1" };
  const otherLot = { ...lotWithoutLocation, barcode: "BC-BETA-2" };

  const typeSearch = (value) => {
    const input = document.querySelector("input.cds--search-input");
    fireEvent.change(input, { target: { value } });
  };

  it("finds a lot by its barcode", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([barcodedLot, otherLot]);
    renderDashboard();
    await screen.findByText("LOT-100");

    typeSearch("BC-ALPHA-1");

    const table = document.querySelector("table");
    expect(within(table).getByText("LOT-100")).toBeInTheDocument();
    expect(within(table).queryByText("LOT-200")).not.toBeInTheDocument();
  });

  it("matches a barcode case-insensitively", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([barcodedLot, otherLot]);
    renderDashboard();
    await screen.findByText("LOT-100");

    typeSearch("bc-alpha-1");

    const table = document.querySelector("table");
    expect(within(table).getByText("LOT-100")).toBeInTheDocument();
    expect(within(table).queryByText("LOT-200")).not.toBeInTheDocument();
  });

  it("still matches on lot number and item name", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([barcodedLot, otherLot]);
    renderDashboard();
    await screen.findByText("LOT-100");

    typeSearch("LOT-200");

    const table = document.querySelector("table");
    expect(within(table).getByText("LOT-200")).toBeInTheDocument();
    expect(within(table).queryByText("LOT-100")).not.toBeInTheDocument();
  });

  // A reworded pre-existing key never reaches en_US; a new key id does.
  it("tells a regional English user the box searches barcodes", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([barcodedLot]);
    render(
      <IntlProvider
        locale="en-US"
        messages={resolveMessagesForLocale("en_US").messages}
      >
        <NotificationContext.Provider value={mockNotificationContext}>
          <InventoryDashboard />
        </NotificationContext.Provider>
      </IntlProvider>,
    );
    await screen.findByText("LOT-100");

    expect(
      document.querySelector("input.cds--search-input").placeholder,
    ).toMatch(/barcode/i);
  });

  it("does not match a barcodeless lot on a query a stringified null would hit", async () => {
    const ultraLot = { ...lotWithLocation, barcode: "BC-ULTRA-9" };
    const noBarcode = { ...lotWithoutLocation, barcode: null };
    InventoryLotAPI.getAll.mockResolvedValue([ultraLot, noBarcode]);
    renderDashboard();
    await screen.findByText("LOT-100");

    // "ul" is inside "null": reading the barcode without ?. would match LOT-200.
    typeSearch("ul");

    const table = document.querySelector("table");
    expect(within(table).getByText("LOT-100")).toBeInTheDocument();
    expect(within(table).queryByText("LOT-200")).not.toBeInTheDocument();
  });
});

describe("InventoryDashboard print label", () => {
  const barcodedLot = { ...lotWithLocation, barcode: "TEST-REAGENT-A-LOT-100" };

  const openRowMenu = async () => {
    await screen.findByText("LOT-100");
    fireEvent.click(document.querySelector("button.cds--overflow-menu"));
  };

  it("downloads the generated PDF when Print label is chosen", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([barcodedLot]);
    InventoryLotAPI.printLabel.mockResolvedValue({
      data: new Blob(["%PDF-1.4"], { type: "application/pdf" }),
      contentType: "application/pdf",
      filename: "lot-TEST-REAGENT-A-LOT-100.pdf",
    });
    const createObjectURL = vi.fn(() => "blob:mock");
    const revokeObjectURL = vi.fn();
    window.URL.createObjectURL = createObjectURL;
    window.URL.revokeObjectURL = revokeObjectURL;

    renderDashboard();
    await openRowMenu();
    fireEvent.click(await screen.findByText(/print label/i));

    await waitFor(() =>
      expect(InventoryLotAPI.printLabel).toHaveBeenCalledWith(1),
    );
    await waitFor(() => expect(createObjectURL).toHaveBeenCalled());
    expect(revokeObjectURL).toHaveBeenCalled();
  });

  it("disables Print label for a lot that has no barcode", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([
      { ...lotWithLocation, barcode: null },
    ]);
    renderDashboard();
    await openRowMenu();

    const item = (await screen.findByText(/print label/i)).closest("button");
    expect(item).toBeDisabled();
  });

  it("notifies rather than failing silently when label generation fails", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([barcodedLot]);
    InventoryLotAPI.printLabel.mockRejectedValue(new Error("boom"));

    renderDashboard();
    await openRowMenu();
    fireEvent.click(await screen.findByText(/print label/i));

    await waitFor(() =>
      expect(mockNotificationContext.addNotification).toHaveBeenCalledWith(
        expect.objectContaining({
          message: "Could not generate the label. Please try again.",
        }),
      ),
    );
  });
});

// Narrowing the set while on a later page used to leave the table empty: the
// page index survived the filter and pointed past the end of the shorter list.
describe("InventoryDashboard filters and the current page", () => {
  const reagent = {
    id: "BUFFER",
    name: "Wash Buffer",
    itemType: "REAGENT",
    units: "mL",
    lowStockThreshold: 1,
  };

  // One page of 20 plus five: the oldest lot sits on page 2 under the
  // newest-first order, and is the only row every one of these filters keeps.
  const oddLotOut = {
    ...lotWithLocation,
    id: 99,
    lotNumber: "LOT-99",
    status: "DISPOSED",
    inventoryItem: { id: "BUFFER" },
  };

  // The empty state is a single-cell row, so fall back to it and let a blank
  // table read as its own message rather than a TypeError.
  const lotNumbersShown = () =>
    Array.from(document.querySelectorAll("table tbody tr")).map(
      (row) => row.cells[1]?.textContent ?? row.cells[0].textContent,
    );

  const renderOnPageTwo = async () => {
    InventoryItemAPI.getAll.mockResolvedValue([
      {
        id: "MALARIA_RDT",
        name: "Malaria RDT",
        itemType: "RDT",
        units: "kits",
        lowStockThreshold: 20,
      },
      reagent,
    ]);
    InventoryLotAPI.getAll.mockResolvedValue([
      ...Array.from({ length: 24 }, (_, n) => ({
        ...lotWithLocation,
        id: 100 + n,
        lotNumber: `LOT-${100 + n}`,
      })),
      oddLotOut,
    ]);
    renderDashboard();
    await screen.findByText("LOT-123");

    fireEvent.click(screen.getByLabelText("Next page"));
    await waitFor(() => expect(lotNumbersShown()).toContain("LOT-99"));
  };

  // The tiles reset the page too, and this is the only one of the resets that
  // nothing pinned: removing it from toggleMetricFilter failed no test.
  it("returns to page one when a metric tile narrows the table", async () => {
    const expiredOn = new Date();
    expiredOn.setDate(expiredOn.getDate() - 10);
    InventoryItemAPI.getAll.mockResolvedValue([
      {
        id: "MALARIA_RDT",
        name: "Malaria RDT",
        itemType: "RDT",
        units: "kits",
        lowStockThreshold: 20,
      },
      reagent,
    ]);
    InventoryLotAPI.getAll.mockResolvedValue([
      ...Array.from({ length: 24 }, (_, n) => ({
        ...lotWithLocation,
        id: 100 + n,
        lotNumber: `LOT-${100 + n}`,
      })),
      {
        ...lotWithLocation,
        id: 99,
        lotNumber: "LOT-EXPIRED",
        expirationDate: expiredOn.toISOString(),
      },
    ]);
    renderDashboard();
    await screen.findByText("LOT-123");
    fireEvent.click(screen.getByLabelText("Next page"));
    await waitFor(() => expect(lotNumbersShown()).toContain("LOT-EXPIRED"));

    fireEvent.click(
      within(document.querySelector(".inventory-metrics-grid"))
        .getByText("Expired")
        .closest(".inventory-metric-tile"),
    );

    // One match, so a page index left at 2 slices past the end and shows nothing.
    await waitFor(() => expect(lotNumbersShown()).toEqual(["LOT-EXPIRED"]));
  });

  it("returns to page one when the search box narrows the table", async () => {
    await renderOnPageTwo();

    fireEvent.change(document.querySelector("input[type='search']"), {
      target: { value: "LOT-99" },
    });

    await waitFor(() => expect(lotNumbersShown()).toEqual(["LOT-99"]));
  });

  it("returns to page one when the type dropdown narrows the table", async () => {
    await renderOnPageTwo();

    fireEvent.click(
      document.querySelector("#inventory-dashboard-type-filter button"),
    );
    fireEvent.click(await screen.findByRole("option", { name: "Reagent" }));

    await waitFor(() => expect(lotNumbersShown()).toEqual(["LOT-99"]));
  });

  it("returns to page one when the status dropdown narrows the table", async () => {
    await renderOnPageTwo();

    fireEvent.click(
      document.querySelector("#inventory-dashboard-status-filter button"),
    );
    fireEvent.click(await screen.findByRole("option", { name: "Disposed" }));

    await waitFor(() => expect(lotNumbersShown()).toEqual(["LOT-99"]));
  });
});

describe("InventoryDashboard metric tile filters", () => {
  const inDays = (days) =>
    new Date(Date.now() + days * 24 * 60 * 60 * 1000).toISOString();

  const expiredLot = {
    ...lotWithLocation,
    id: 11,
    lotNumber: "LOT-EXPIRED",
    expirationDate: inDays(-10),
  };
  const expiringLot = {
    ...lotWithLocation,
    id: 12,
    lotNumber: "LOT-SOON",
    expirationDate: inDays(5),
  };
  const freshLot = {
    ...lotWithLocation,
    id: 13,
    lotNumber: "LOT-FRESH",
    expirationDate: inDays(400),
  };
  // Outside the 30-day alert window the tiles use, so a widened rule pulls it
  // into the Expiring Soon tile and the row tag has to follow.
  const laterLot = {
    ...lotWithLocation,
    id: 15,
    lotNumber: "LOT-LATER",
    expirationDate: inDays(40),
  };

  // "Expired" is also a stock-status tag in the table, so a tile lookup has to
  // be scoped to the tile grid or it matches a row.
  const tile = (label) =>
    within(document.querySelector(".inventory-metrics-grid"))
      .getByText(label)
      .closest(".inventory-metric-tile");

  const tileCount = (label) =>
    Number(tile(label).querySelector(".metric-value").textContent);

  const rowCount = () => document.querySelectorAll("table tbody tr").length;

  const lotNumbersShown = () =>
    Array.from(document.querySelectorAll("table tbody tr")).map(
      (row) => row.cells[1].textContent,
    );

  it("filters the table to a tile's lots and restores the rest on a second click", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([
      expiredLot,
      expiringLot,
      freshLot,
    ]);
    renderDashboard();
    await screen.findByText("LOT-FRESH");

    fireEvent.click(tile("Expired"));
    await waitFor(() => expect(lotNumbersShown()).toEqual(["LOT-EXPIRED"]));

    fireEvent.click(tile("Expired"));
    await waitFor(() => expect(rowCount()).toBe(3));
  });

  it("shows exactly as many rows as the lot-based tile counted", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([
      expiredLot,
      { ...expiredLot, id: 14, lotNumber: "LOT-EXPIRED-2" },
      expiringLot,
      freshLot,
    ]);
    renderDashboard();
    await screen.findByText("LOT-FRESH");

    for (const label of ["Total Lots", "Expiring Soon", "Expired"]) {
      const counted = tileCount(label);
      expect(counted).toBeGreaterThan(0);
      fireEvent.click(tile(label));
      await waitFor(() => expect(rowCount()).toBe(counted));
      fireEvent.click(tile(label));
      await waitFor(() => expect(rowCount()).toBe(4));
    }
  });

  it("tags every row an expiry tile filters to with that tile's own status", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([
      expiredLot,
      expiringLot,
      freshLot,
      laterLot,
    ]);
    renderDashboard();
    await screen.findByText("LOT-FRESH");

    const rowTexts = () =>
      Array.from(document.querySelectorAll("table tbody tr")).map(
        (row) => row.textContent,
      );

    fireEvent.click(tile("Expired"));
    await waitFor(() => expect(rowCount()).toBe(1));
    rowTexts().forEach((text) => expect(text).toMatch(/Expired/));

    fireEvent.click(tile("Expired"));
    fireEvent.click(tile("Expiring Soon"));
    await waitFor(() => expect(rowCount()).toBeGreaterThan(0));
    rowTexts().forEach((text) => expect(text).toMatch(/Expiring \(\d+d\)/));
  });

  it("filters Low Stock to the lots of low-stock items, which outnumber the items counted", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([
      { ...lotWithLocation, id: 21, lotNumber: "LOT-LOW-A" },
      { ...lotWithLocation, id: 22, lotNumber: "LOT-LOW-B" },
      {
        ...lotWithLocation,
        id: 23,
        lotNumber: "LOT-OK",
        inventoryItem: { id: "OTHER" },
      },
    ]);
    InventoryItemAPI.getAll.mockResolvedValue([
      { id: "MALARIA_RDT", name: "Malaria RDT", itemType: "RDT" },
      { id: "OTHER", name: "Other Kit", itemType: "RDT" },
    ]);
    InventoryItemAPI.getLowStock.mockResolvedValue([
      { id: "MALARIA_RDT", name: "Malaria RDT" },
    ]);
    renderDashboard();
    await screen.findByText("LOT-OK");
    await waitFor(() => expect(tileCount("Low Stock")).toBe(1));

    fireEvent.click(tile("Low Stock"));

    // One item below threshold, two of its lots: the tile counts items and the
    // table lists lots, so the mismatch is the design and not a drift.
    await waitFor(() =>
      expect(lotNumbersShown()).toEqual(["LOT-LOW-B", "LOT-LOW-A"]),
    );
    expect(tileCount("Low Stock")).toBe(1);
  });

  it("narrows together with the search box and the status dropdown", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([
      expiredLot,
      { ...expiredLot, id: 15, lotNumber: "LOT-EXPIRED-2", status: "DISPOSED" },
      { ...freshLot, status: "DISPOSED" },
    ]);
    renderDashboard();
    await screen.findByText("LOT-FRESH");

    fireEvent.click(tile("Expired"));
    await waitFor(() => expect(rowCount()).toBe(2));

    fireEvent.click(
      document.querySelector("#inventory-dashboard-status-filter button"),
    );
    fireEvent.click(await screen.findByRole("option", { name: "Disposed" }));
    await waitFor(() => expect(lotNumbersShown()).toEqual(["LOT-EXPIRED-2"]));

    fireEvent.change(document.querySelector("input[type='search']"), {
      target: { value: "LOT-EXPIRED-2" },
    });
    await waitFor(() => expect(lotNumbersShown()).toEqual(["LOT-EXPIRED-2"]));

    fireEvent.change(document.querySelector("input[type='search']"), {
      target: { value: "LOT-FRESH" },
    });
    // The empty state occupies a row of its own, so count the message.
    await waitFor(() =>
      expect(screen.getByText("No inventory items found")).toBeInTheDocument(),
    );
  });

  it("marks the active tile with aria-pressed", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([expiredLot, freshLot]);
    renderDashboard();
    await screen.findByText("LOT-FRESH");

    expect(tile("Expired")).toHaveAttribute("aria-pressed", "false");

    fireEvent.click(tile("Expired"));
    await waitFor(() =>
      expect(tile("Expired")).toHaveAttribute("aria-pressed", "true"),
    );
    expect(tile("Total Lots")).toHaveAttribute("aria-pressed", "false");
    expect(
      document.querySelectorAll(".inventory-metric-tile--selected"),
    ).toHaveLength(1);

    fireEvent.click(tile("Expired"));
    await waitFor(() =>
      expect(tile("Expired")).toHaveAttribute("aria-pressed", "false"),
    );
  });
});

describe("InventoryDashboard lot order", () => {
  // The Storage Inventory Lots table lists the same lots newest first. The
  // endpoint answers oldest first, which put a lot just added on the last page.
  it("lists the newest lot first, whatever order the endpoint answers in", async () => {
    InventoryLotAPI.getAll.mockResolvedValue([
      { ...lotWithLocation, id: 5, lotNumber: "LOT-OLDEST" },
      { ...lotWithLocation, id: 40, lotNumber: "LOT-NEWEST" },
      { ...lotWithLocation, id: 12, lotNumber: "LOT-MIDDLE" },
    ]);
    renderDashboard();

    await screen.findByText("LOT-NEWEST");
    expect(
      Array.from(document.querySelectorAll("table tbody tr")).map(
        (row) => row.cells[1].textContent,
      ),
    ).toEqual(["LOT-NEWEST", "LOT-MIDDLE", "LOT-OLDEST"]);
  });
});
