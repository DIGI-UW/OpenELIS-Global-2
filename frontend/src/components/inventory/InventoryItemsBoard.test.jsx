import React from "react";
import { render, screen, within, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import InventoryItemsBoard from "./InventoryItemsBoard";
import { NotificationContext } from "../layout/Layout";
import {
  InventoryBoardAPI,
  InventoryItemAPI,
  InventoryLotAPI,
  InventoryManagementAPI,
} from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryBoardAPI: { get: vi.fn() },
  InventoryLotAPI: { getAll: vi.fn() },
  InventoryItemAPI: {
    getById: vi.fn(),
    markOrdered: vi.fn(),
    clearOrdered: vi.fn(),
  },
  InventoryManagementAPI: { consume: vi.fn() },
}));

// Each action modal is exercised by its own suite. What matters here is the
// wiring: which modal opens, and against which row or lot. Each stand-in
// reports the identity it was handed and can fire its onSave.
const { modalStub } = vi.hoisted(() => ({
  modalStub: (testId, describe) => ({
    default: (props) =>
      props.open
        ? React.createElement(
            "div",
            { "data-testid": testId },
            React.createElement(
              "span",
              { "data-testid": `${testId}-target` },
              describe(props),
            ),
            React.createElement(
              "button",
              { onClick: props.onSave },
              `${testId}-save`,
            ),
            React.createElement(
              "button",
              { onClick: props.onClose },
              `${testId}-close`,
            ),
          )
        : null,
  }),
}));

vi.mock("./LotEntryModal", () =>
  modalStub("lot-entry", (p) =>
    p.lot ? `edit:${p.lot.lotNumber}` : `receive:item:${p.item?.id}`,
  ),
);
vi.mock("./LotAdjustmentModal", () =>
  modalStub("adjust", (p) => `lot:${p.lot.id}`),
);
vi.mock("./UpdateQCStatusModal", () =>
  modalStub("qc", (p) => `lot:${p.lot.id}`),
);
vi.mock("./DisposeLotModal", () =>
  modalStub("dispose", (p) => `lot:${p.lot.id}`),
);
vi.mock("./InventoryItemForm", () =>
  modalStub(
    "item-form",
    (p) => `item:${p.item.id}:${p.item.name}:observed=${p.observedLeadTime}`,
  ),
);

// The details panel is opened from a lot number and has its own tests; the
// board only owns the wiring, which is asserted through the click handler.
vi.mock("./LotDetailsPanel", () => ({
  default: ({ open, lot }) =>
    open ? <div data-testid="lot-details">{lot.lotNumber}</div> : null,
}));

// Fixture dates are built relative to today so "past due" and the formatted
// day labels cannot rot as the calendar moves.
const TODAY = new Date();
const shiftDays = (days) => {
  const date = new Date(TODAY);
  date.setDate(date.getDate() + days);
  return date;
};
const isoDay = (days) => {
  const date = shiftDays(days);
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${date.getFullYear()}-${month}-${day}`;
};
const dayLabel = (days) =>
  shiftDays(days).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
  });

const CARTRIDGE = {
  itemId: 1,
  code: "GENEXPERT_MTB_RIF",
  name: "GeneXpert MTB/RIF cartridge",
  itemType: "CARTRIDGE",
  units: "tests",
  onHand: 12,
  lowStockThreshold: 20,
  medianDailyUse: 4,
  basisDate: isoDay(-1),
  stale: false,
  runOutEarly: isoDay(8),
  runOutLate: isoDay(12),
  leadTimeDays: 14,
  leadTimeTier: "SET",
  orderByDate: isoDay(-6),
  trendPercent: 40,
  status: "REORDER_NOW",
  orderedOn: null,
  orderExpectedDate: null,
  orderNote: null,
};

const SYPHILIS = {
  itemId: 2,
  code: "SYPHILIS_RDT_KIT",
  name: "Syphilis RDT kit",
  itemType: "SYPHILIS_KIT",
  units: "tests",
  onHand: 8,
  lowStockThreshold: 10,
  medianDailyUse: null,
  basisDate: null,
  stale: true,
  runOutEarly: null,
  runOutLate: null,
  leadTimeDays: 30,
  leadTimeTier: "DEFAULT",
  orderByDate: null,
  trendPercent: null,
  status: "BUILDING_DATA",
  orderedOn: null,
  orderExpectedDate: null,
  orderNote: null,
};

const MALARIA = {
  itemId: 3,
  code: "MALARIA_RDT",
  name: "Malaria RDT (P.f/P.v)",
  itemType: "RDT",
  units: "tests",
  onHand: 60,
  lowStockThreshold: 25,
  medianDailyUse: 2,
  basisDate: isoDay(-40),
  stale: true,
  runOutEarly: isoDay(20),
  runOutLate: isoDay(27),
  leadTimeDays: 10,
  leadTimeTier: "OBSERVED",
  orderByDate: isoDay(10),
  trendPercent: 2,
  status: "REORDER_SOON",
  orderedOn: null,
  orderExpectedDate: null,
  orderNote: null,
};

const lot = (overrides) => ({
  qcStatus: "PASSED",
  status: "ACTIVE",
  availableForUse: true,
  location: null,
  ...overrides,
});

// Lot expiry ships as epoch milliseconds, not an ISO string — java.sql.Timestamp
// falls through the registered JavaTimeModule and Jackson's
// WRITE_DATES_AS_TIMESTAMPS default stands. Pinned by
// InventoryLotRestControllerIntegrationTest on the server side. Relative to
// today for the same reason the board dates are: a fixed date silently stops
// exercising the expiry warnings once it passes.
const expiry = (daysFromNow) => shiftDays(daysFromNow).getTime();

// The cartridge's earliest-expiring lot has failed QC, so "use first" must skip
// it. A naive earliest-expiry-wins rule would mark MTB-2001 instead.
const LOTS = [
  lot({
    id: 101,
    lotNumber: "MTB-2001",
    effectiveExpirationDate: expiry(-10),
    currentQuantity: 5,
    qcStatus: "FAILED",
    availableForUse: false,
    inventoryItem: { id: 1, name: CARTRIDGE.name },
    location: { hierarchicalPath: "Fridge 1 > Shelf B" },
  }),
  lot({
    id: 102,
    lotNumber: "MTB-2451",
    effectiveExpirationDate: expiry(20),
    currentQuantity: 7,
    inventoryItem: { id: 1, name: CARTRIDGE.name },
    location: { hierarchicalPath: "Fridge 1 > Shelf B" },
  }),
  lot({
    id: 103,
    lotNumber: "MTB-2900",
    effectiveExpirationDate: expiry(200),
    currentQuantity: 5,
    inventoryItem: { id: 1, name: CARTRIDGE.name },
    location: { hierarchicalPath: "Fridge 2" },
  }),
  lot({
    id: 201,
    lotNumber: "SYPH-77",
    effectiveExpirationDate: expiry(220),
    currentQuantity: 8,
    inventoryItem: { id: 2, name: SYPHILIS.name },
    location: { hierarchicalPath: "Room 2" },
  }),
  lot({
    id: 301,
    lotNumber: "MAL-5150",
    barcode: "0034567890123",
    effectiveExpirationDate: expiry(240),
    currentQuantity: 60,
    inventoryItem: { id: 3, name: MALARIA.name },
    location: { hierarchicalPath: "Room 2" },
  }),
];

const notificationContext = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  addNotification: vi.fn(),
};

const renderBoard = async (
  board = [CARTRIDGE, SYPHILIS, MALARIA],
  lots = LOTS,
) => {
  InventoryBoardAPI.get.mockResolvedValue(board);
  InventoryLotAPI.getAll.mockResolvedValue(lots);
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={notificationContext}>
        <InventoryItemsBoard />
      </NotificationContext.Provider>
    </IntlProvider>,
  );
  await screen.findByRole("table");
  return view;
};

const openRowMenu = async (name) => {
  const row = rowNamed(name);
  fireEvent.click(within(row).getByRole("button", { name: /Actions for/ }));
};

// Only the board's own item rows. An expanded row nests a whole lot table
// inside the outer table, so a role-based query would sweep those lot rows up
// too and quietly change what every assertion below is counting.
const bodyRows = () =>
  [...screen.getAllByRole("table")[0].tBodies[0].children].filter((row) =>
    row.classList.contains("cds--parent-row"),
  );

const rowNamed = (name) =>
  bodyRows().find((row) => within(row).queryByText(name));

beforeEach(() => {
  vi.clearAllMocks();
});

describe("InventoryItemsBoard", () => {
  it("renders every column of a projected row from the board payload", async () => {
    await renderBoard();
    const row = rowNamed(CARTRIDGE.name);
    const cells = within(row).getAllByRole("cell");

    // cells[0] is the expand control column.
    expect(cells[1]).toHaveTextContent(CARTRIDGE.name);
    expect(cells[1]).toHaveTextContent("GENEXPERT_MTB_RIF");
    expect(cells[1]).toHaveTextContent("Analyzer Cartridge");
    expect(cells[2]).toHaveTextContent("12 tests");
    expect(cells[3]).toHaveTextContent("+40% / 30d");
    expect(cells[4]).toHaveTextContent(`${dayLabel(8)} – ${dayLabel(12)}`);
    expect(cells[4]).toHaveTextContent(
      `based on usage through ${dayLabel(-1)}`,
    );
    expect(cells[5]).toHaveTextContent("past due");
    expect(cells[5]).toHaveTextContent("14d (set)");
    expect(cells[6]).toHaveTextContent("Reorder now");
  });

  it("shows a future order-by date with the tier it was resolved from", async () => {
    await renderBoard();
    const cells = within(rowNamed(MALARIA.name)).getAllByRole("cell");
    expect(cells[5]).toHaveTextContent(dayLabel(10));
    expect(cells[5]).not.toHaveTextContent("past due");
    expect(cells[5]).toHaveTextContent("~10d (observed)");
  });

  it("hedges a stale row instead of quoting the usage it is based on", async () => {
    await renderBoard();
    const runsOut = within(rowNamed(MALARIA.name)).getAllByRole("cell")[4];
    expect(runsOut).toHaveTextContent("Watch — usage data may be out of date");
    expect(runsOut).not.toHaveTextContent("based on usage through");
    // The window itself is still shown: stale flags an estimate, it does not
    // withhold one.
    expect(runsOut).toHaveTextContent(`${dayLabel(20)} – ${dayLabel(27)}`);
  });

  it("gives a cold-start row threshold status and no invented date", async () => {
    await renderBoard();
    const cells = within(rowNamed(SYPHILIS.name)).getAllByRole("cell");
    expect(cells[4]).toHaveTextContent(
      "Too few days of use to project a run-out date",
    );
    // The "Building data" label lives in the status column only, so a row that
    // has no projection but is below threshold does not read as both.
    expect(cells[4]).not.toHaveTextContent("Building data");
    expect(cells[5]).toHaveTextContent("30d (default — set to improve)");
    // No fabricated dates in either projection cell.
    expect(cells[4].textContent).not.toMatch(/\d{1,2}/);
    expect(cells[5].textContent).not.toMatch(/\b[A-Z][a-z]{2} \d{1,2}\b/);
    expect(cells[6]).toHaveTextContent("Building data");
  });

  it("reads a near-flat trend as steady rather than a percentage", async () => {
    await renderBoard();
    expect(
      within(rowNamed(MALARIA.name)).getAllByRole("cell")[3],
    ).toHaveTextContent("steady");
    expect(
      within(rowNamed(SYPHILIS.name)).getAllByRole("cell")[3],
    ).toHaveTextContent("—");
  });

  it("keeps the server's urgency ordering until a column is sorted", async () => {
    await renderBoard();
    expect(
      bodyRows().map((row) => within(row).getAllByRole("cell")[1].textContent),
    ).toEqual([
      expect.stringContaining(CARTRIDGE.name),
      expect.stringContaining(SYPHILIS.name),
      expect.stringContaining(MALARIA.name),
    ]);

    const sortByOnHand = () =>
      fireEvent.click(
        within(
          screen.getByRole("columnheader", { name: /On hand/i }),
        ).getByRole("button"),
      );

    sortByOnHand();
    expect(
      bodyRows().map((row) =>
        within(row).getAllByRole("cell")[2].textContent.trim(),
      ),
    ).toEqual(["8 tests", "12 tests", "60 tests"]);

    sortByOnHand();
    expect(
      bodyRows().map((row) =>
        within(row).getAllByRole("cell")[2].textContent.trim(),
      ),
    ).toEqual(["60 tests", "12 tests", "8 tests"]);
  });

  it("narrows in place by status", async () => {
    await renderBoard();
    fireEvent.change(screen.getByLabelText("Filter by Status"), {
      target: { value: "REORDER_SOON" },
    });
    expect(bodyRows()).toHaveLength(1);
    expect(bodyRows()[0]).toHaveTextContent(MALARIA.name);
  });

  it("narrows in place by storage location", async () => {
    await renderBoard();
    fireEvent.change(screen.getByLabelText("Filter by Location"), {
      target: { value: "Fridge 2" },
    });
    expect(bodyRows()).toHaveLength(1);
    expect(bodyRows()[0]).toHaveTextContent(CARTRIDGE.name);
  });

  it("finds an item by a lot number that appears nowhere in its name or code", async () => {
    await renderBoard();
    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "MAL-5150" },
    });
    expect(bodyRows()).toHaveLength(1);
    expect(bodyRows()[0]).toHaveTextContent(MALARIA.name);
  });

  it("finds an item by a scanned lot barcode", async () => {
    await renderBoard();
    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "0034567890123" },
    });
    expect(bodyRows()).toHaveLength(1);
    expect(bodyRows()[0]).toHaveTextContent(MALARIA.name);
  });

  it("leaves rows with no value last however the column is sorted", async () => {
    await renderBoard();
    const sortByRunsOut = () =>
      fireEvent.click(
        within(
          screen.getByRole("columnheader", { name: /Runs out/i }),
        ).getByRole("button"),
      );

    // Syphilis has no run-out date at all. It is not the earliest and not the
    // latest; it belongs at the bottom of both orderings.
    sortByRunsOut();
    expect(bodyRows().map((row) => row.textContent)[2]).toContain(
      SYPHILIS.name,
    );
    sortByRunsOut();
    expect(bodyRows().map((row) => row.textContent)[2]).toContain(
      SYPHILIS.name,
    );
  });

  it("offers a recovery hint when filters match nothing", async () => {
    await renderBoard();
    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "no-such-item" },
    });
    expect(
      screen.getByText(
        "No items match your search and filters. Clear them to see the whole catalog.",
      ),
    ).toBeInTheDocument();
  });

  it("tells an empty catalog what to do next", async () => {
    await renderBoard([], []);
    expect(
      screen.getByText(
        "No inventory items yet. Add an item to start tracking it, then receive stock.",
      ),
    ).toBeInTheDocument();
  });

  it("marks the earliest-expiring usable lot use-first, skipping one that failed QC", async () => {
    await renderBoard();
    fireEvent.click(
      within(rowNamed(CARTRIDGE.name)).getByRole("button", {
        name: CARTRIDGE.name,
      }),
    );

    const lotTable = screen.getAllByRole("table")[1];
    const lotRows = within(lotTable).getAllByRole("row").slice(1);
    expect(
      lotRows.map((row) => within(row).getAllByRole("cell")[0].textContent),
    ).toEqual(["MTB-2001", "MTB-2451", "MTB-2900"]);

    const flagged = lotRows.filter((row) =>
      within(row).queryByText("Use first"),
    );
    expect(flagged).toHaveLength(1);
    expect(flagged[0]).toHaveTextContent("MTB-2451");
    // The earlier lot is still listed, with the QC status that disqualified it.
    expect(lotRows[0]).toHaveTextContent("Failed");
  });

  it("spells out the median daily use and the per-location split", async () => {
    await renderBoard();
    fireEvent.click(
      within(rowNamed(CARTRIDGE.name)).getByRole("button", {
        name: CARTRIDGE.name,
      }),
    );
    expect(
      screen.getByText(/Median daily use: 4 tests\/day \(last 30 days/),
    ).toBeInTheDocument();
    // 7 usable in Fridge 1 and 5 in Fridge 2; the failed 5 count towards
    // neither, matching the on-hand figure the board projects from.
    const split = screen.getByText(/By location:/);
    expect(split).toHaveTextContent("Fridge 1 > Shelf B: 7 tests");
    expect(split).toHaveTextContent("Fridge 2: 5 tests");
  });

  it("opens the existing lot details panel from a lot number", async () => {
    await renderBoard();
    fireEvent.click(
      within(rowNamed(CARTRIDGE.name)).getByRole("button", {
        name: CARTRIDGE.name,
      }),
    );
    fireEvent.click(screen.getByRole("button", { name: "MTB-2451" }));
    expect(screen.getByTestId("lot-details")).toHaveTextContent("MTB-2451");
  });

  it("expands and collapses from the keyboard", async () => {
    await renderBoard();
    const expander = within(rowNamed(CARTRIDGE.name)).getByRole("button", {
      name: CARTRIDGE.name,
    });
    expander.focus();
    expect(expander).toHaveFocus();

    fireEvent.click(expander);
    expect(screen.getAllByRole("table")).toHaveLength(2);
    // The nested lot table must not read as extra board rows.
    expect(bodyRows()).toHaveLength(3);

    fireEvent.click(expander);
    expect(screen.getAllByRole("table")).toHaveLength(1);
    expect(bodyRows()).toHaveLength(3);
  });

  it("reports a failed load instead of rendering an empty catalog", async () => {
    InventoryBoardAPI.get.mockRejectedValue(new Error("board is down"));
    InventoryLotAPI.getAll.mockResolvedValue([]);
    render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider value={notificationContext}>
          <InventoryItemsBoard />
        </NotificationContext.Provider>
      </IntlProvider>,
    );
    await waitFor(() =>
      expect(
        screen.getByText("The items board could not be loaded."),
      ).toBeInTheDocument(),
    );
    expect(screen.getByText("board is down")).toBeInTheDocument();
  });

  describe("row actions", () => {
    it("opens Receive stock against the clicked item, preselected", async () => {
      await renderBoard();
      await openRowMenu(MALARIA.name);
      fireEvent.click(screen.getByText("Receive stock"));

      expect(screen.getByTestId("lot-entry-target")).toHaveTextContent(
        `receive:item:${MALARIA.itemId}`,
      );
    });

    it("opens the item editor on the fetched item, never on the board row", async () => {
      await renderBoard();
      InventoryItemAPI.getById.mockResolvedValue({
        id: MALARIA.itemId,
        name: MALARIA.name,
        category: "kits",
        manufacturer: "Acme",
      });

      await openRowMenu(MALARIA.name);
      fireEvent.click(screen.getByText("Edit item details"));

      await waitFor(() =>
        expect(InventoryItemAPI.getById).toHaveBeenCalledWith(MALARIA.itemId),
      );
      // A board row carries itemId, not id, and omits half the editable fields.
      // Handing it over directly would PUT to /items/undefined and blank them.
      expect(screen.getByTestId("item-form-target")).toHaveTextContent(
        `item:${MALARIA.itemId}:${MALARIA.name}`,
      );
    });

    it("offers a learned lead time to the editor only when that is the tier in use", async () => {
      await renderBoard([
        { ...MALARIA, leadTimeTier: "OBSERVED", leadTimeDays: 12 },
        CARTRIDGE,
      ]);
      InventoryItemAPI.getById.mockResolvedValue({
        id: MALARIA.itemId,
        name: MALARIA.name,
      });

      await openRowMenu(MALARIA.name);
      fireEvent.click(screen.getByText("Edit item details"));

      await waitFor(() =>
        expect(screen.getByTestId("item-form-target")).toHaveTextContent(
          "observed=12",
        ),
      );
    });

    it("offers nothing to the editor when the lab already set a lead time", async () => {
      // CARTRIDGE is tier SET. Offering the learned figure here would invite a
      // silent overwrite of a value the lab chose.
      await renderBoard();
      InventoryItemAPI.getById.mockResolvedValue({
        id: CARTRIDGE.itemId,
        name: CARTRIDGE.name,
      });

      await openRowMenu(CARTRIDGE.name);
      fireEvent.click(screen.getByText("Edit item details"));

      await waitFor(() =>
        expect(screen.getByTestId("item-form-target")).toHaveTextContent(
          "observed=null",
        ),
      );
    });

    it("opens each lot action against the lot clicked, not the row position", async () => {
      await renderBoard();
      // Sort first: Carbon reorders rendered rows, and the cartridge moves.
      fireEvent.click(
        within(
          screen.getByRole("columnheader", { name: /On hand/i }),
        ).getByRole("button"),
      );
      fireEvent.click(
        within(rowNamed(CARTRIDGE.name)).getByRole("button", {
          name: CARTRIDGE.name,
        }),
      );

      const lotTable = screen.getAllByRole("table")[1];
      const secondLotRow = within(lotTable).getAllByRole("row")[2];
      expect(secondLotRow).toHaveTextContent("MTB-2451");

      fireEvent.click(
        within(secondLotRow).getByRole("button", { name: /Actions for lot/ }),
      );
      fireEvent.click(screen.getByText("Dispose Lot"));

      expect(screen.getByTestId("dispose-target")).toHaveTextContent("lot:102");
    });

    it("refreshes the board and the lots after an action succeeds", async () => {
      await renderBoard();
      expect(InventoryBoardAPI.get).toHaveBeenCalledTimes(1);
      expect(InventoryLotAPI.getAll).toHaveBeenCalledTimes(1);

      await openRowMenu(MALARIA.name);
      fireEvent.click(screen.getByText("Receive stock"));
      fireEvent.click(screen.getByText("lot-entry-save"));

      // Both, because a write changes on-hand and on-hand is what the run-out
      // projection is computed from.
      await waitFor(() =>
        expect(InventoryBoardAPI.get).toHaveBeenCalledTimes(2),
      );
      expect(InventoryLotAPI.getAll).toHaveBeenCalledTimes(2);
      expect(notificationContext.addNotification).toHaveBeenCalled();
      expect(screen.queryByTestId("lot-entry")).not.toBeInTheDocument();
    });

    it("warns on a lot that is expired or close to it", async () => {
      await renderBoard();
      fireEvent.click(
        within(rowNamed(CARTRIDGE.name)).getByRole("button", {
          name: CARTRIDGE.name,
        }),
      );
      const lotTable = screen.getAllByRole("table")[1];
      const rows = within(lotTable).getAllByRole("row");
      expect(rows[1]).toHaveTextContent("MTB-2001");
      expect(rows[1]).toHaveTextContent("Expired");
      expect(rows[2]).toHaveTextContent("MTB-2451");
      expect(rows[2]).toHaveTextContent("Expires in 20d");
      // Far enough out to need no warning at all.
      expect(rows[3]).toHaveTextContent("MTB-2900");
      expect(rows[3]).not.toHaveTextContent("Expire");
    });

    it("calls a lot that went off earlier today expired, not due in zero days", async () => {
      await renderBoard(
        [CARTRIDGE],
        [
          lot({
            id: 999,
            lotNumber: "MTB-TODAY",
            effectiveExpirationDate: Date.now() - 3600000,
            currentQuantity: 3,
            inventoryItem: { id: 1, name: CARTRIDGE.name },
          }),
        ],
      );
      fireEvent.click(
        within(rowNamed(CARTRIDGE.name)).getByRole("button", {
          name: CARTRIDGE.name,
        }),
      );
      const lotRow = within(screen.getAllByRole("table")[1]).getAllByRole(
        "row",
      )[1];
      expect(lotRow).toHaveTextContent("Expired");
      expect(lotRow).not.toHaveTextContent("Expires in 0d");
    });
  });

  describe("quick log usage", () => {
    const openQuickLog = async () => {
      await renderBoard();
      fireEvent.click(screen.getByRole("button", { name: "Log usage" }));
    };

    it("posts a consumption for the chosen item, not an adjustment", async () => {
      InventoryManagementAPI.consume.mockResolvedValue({});
      await openQuickLog();

      fireEvent.click(screen.getByRole("combobox", { name: /Item Name/i }));
      fireEvent.click(screen.getByText(`${MALARIA.name} (${MALARIA.code})`));
      fireEvent.change(screen.getByRole("spinbutton"), {
        target: { value: "7" },
      });
      fireEvent.click(screen.getByRole("button", { name: "Record Usage" }));

      await waitFor(() =>
        expect(InventoryManagementAPI.consume).toHaveBeenCalledWith({
          itemId: String(MALARIA.itemId),
          quantity: 7,
        }),
      );
      // An adjustment would move the same number and leave the projection
      // describing consumption that never happened.
      await waitFor(() =>
        expect(InventoryBoardAPI.get).toHaveBeenCalledTimes(2),
      );
    });

    it("preselects the item when opened from a row", async () => {
      InventoryManagementAPI.consume.mockResolvedValue({});
      await renderBoard();
      await openRowMenu(CARTRIDGE.name);
      fireEvent.click(screen.getByText("Record Usage"));
      fireEvent.click(screen.getByRole("button", { name: "Record Usage" }));

      await waitFor(() =>
        expect(InventoryManagementAPI.consume).toHaveBeenCalledWith({
          itemId: String(CARTRIDGE.itemId),
          quantity: 1,
        }),
      );
    });

    it("refuses a fractional quantity instead of letting the database reject it", async () => {
      await openQuickLog();
      fireEvent.click(screen.getByRole("combobox", { name: /Item Name/i }));
      fireEvent.click(screen.getByText(`${MALARIA.name} (${MALARIA.code})`));
      fireEvent.change(screen.getByRole("spinbutton"), {
        target: { value: "0.5" },
      });
      fireEvent.click(screen.getByRole("button", { name: "Record Usage" }));

      expect(
        screen.getByText("Enter a whole number of units, at least 1."),
      ).toBeInTheDocument();
      expect(InventoryManagementAPI.consume).not.toHaveBeenCalled();
    });

    it("shows the server's shortfall message rather than a generic failure", async () => {
      InventoryManagementAPI.consume.mockRejectedValue(
        new Error("Insufficient inventory for item: 3. Available: 60"),
      );
      await openQuickLog();
      fireEvent.click(screen.getByRole("combobox", { name: /Item Name/i }));
      fireEvent.click(screen.getByText(`${MALARIA.name} (${MALARIA.code})`));
      fireEvent.click(screen.getByRole("button", { name: "Record Usage" }));

      await waitFor(() =>
        expect(
          screen.getByText("Insufficient inventory for item: 3. Available: 60"),
        ).toBeInTheDocument(),
      );
      expect(InventoryBoardAPI.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("reorder suggestions and the critical banner", () => {
    const openSuggestions = async (board) => {
      await renderBoard(board);
      fireEvent.click(
        screen.getByRole("button", { name: /^Reorder suggestions/ }),
      );
    };

    it("banners only the items that are critical and not already ordered", async () => {
      await renderBoard();
      const banner = screen.getByRole("alertdialog");
      // The cartridge is REORDER_NOW; the malaria row is only REORDER_SOON and
      // must not be shouted about.
      expect(banner).toHaveTextContent(CARTRIDGE.name);
      expect(banner).not.toHaveTextContent(MALARIA.name);
    });

    it("drops an item from the banner once it is marked ordered, without hiding its row", async () => {
      await renderBoard([
        { ...CARTRIDGE, orderedOn: "2026-09-17", orderNote: "PO-1" },
        SYPHILIS,
        MALARIA,
      ]);
      expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();

      // Still on the board, still short, now tagged.
      const row = rowNamed(CARTRIDGE.name);
      expect(row).toBeInTheDocument();
      expect(within(row).getAllByRole("cell")[6]).toHaveTextContent(
        "Reorder now",
      );
      expect(within(row).getAllByRole("cell")[6]).toHaveTextContent("On order");
    });

    it("shows the expected delivery date beside the on-order tag", async () => {
      await renderBoard([
        {
          ...CARTRIDGE,
          orderedOn: isoDay(-2),
          orderExpectedDate: isoDay(9),
          orderNote: "PO-1",
        },
        SYPHILIS,
        MALARIA,
      ]);
      const status = within(rowNamed(CARTRIDGE.name)).getAllByRole("cell")[6];
      expect(status).toHaveTextContent(`On order · ${dayLabel(9)}`);
    });

    it("lists exactly the rows the board badges, and nothing else", async () => {
      await openSuggestions();
      const dialog = screen.getByRole("dialog");
      expect(within(dialog).getByText(/GeneXpert/)).toBeInTheDocument();
      expect(within(dialog).getByText(/Malaria RDT/)).toBeInTheDocument();
      // Building data is not a suggestion: there is no projection to act on.
      expect(within(dialog).queryByText(/Syphilis/)).not.toBeInTheDocument();
    });

    it("marks the whole selection in one call and refreshes the board", async () => {
      InventoryItemAPI.markOrdered.mockResolvedValue({ changed: 2 });
      await openSuggestions();

      fireEvent.click(screen.getByRole("checkbox", { name: /select all/i }));
      fireEvent.click(screen.getByText("Mark as ordered"));

      await waitFor(() =>
        expect(InventoryItemAPI.markOrdered).toHaveBeenCalledTimes(1),
      );
      const call = InventoryItemAPI.markOrdered.mock.calls[0][0];
      expect(call.itemIds).toEqual(
        expect.arrayContaining([CARTRIDGE.itemId, MALARIA.itemId]),
      );
      expect(call.itemIds).toHaveLength(2);
      await waitFor(() =>
        expect(InventoryBoardAPI.get).toHaveBeenCalledTimes(2),
      );
    });

    it("carries the note and expected date into the mark", async () => {
      InventoryItemAPI.markOrdered.mockResolvedValue({ changed: 1 });
      await openSuggestions();

      fireEvent.change(screen.getByLabelText("Note"), {
        target: { value: "PO-4471" },
      });
      fireEvent.click(screen.getByRole("checkbox", { name: /select all/i }));
      fireEvent.click(screen.getByText("Mark as ordered"));

      await waitFor(() =>
        expect(InventoryItemAPI.markOrdered).toHaveBeenCalledWith(
          expect.objectContaining({ note: "PO-4471" }),
        ),
      );
    });

    it("offers a way back, and only for rows that actually carry a mark", async () => {
      InventoryItemAPI.clearOrdered.mockResolvedValue({ changed: 1 });
      await openSuggestions([
        { ...CARTRIDGE, orderedOn: "2026-09-17", orderNote: "PO-1" },
        MALARIA,
      ]);

      fireEvent.click(screen.getByRole("checkbox", { name: /select all/i }));
      const undo = screen.getByText("Not ordered after all");
      fireEvent.click(undo);

      await waitFor(() =>
        expect(InventoryItemAPI.clearOrdered).toHaveBeenCalledWith(
          expect.arrayContaining([CARTRIDGE.itemId, MALARIA.itemId]),
        ),
      );
      await waitFor(() =>
        expect(InventoryBoardAPI.get).toHaveBeenCalledTimes(2),
      );
    });

    it("disables the way back when nothing selected is on order", async () => {
      await openSuggestions([MALARIA, { ...CARTRIDGE }]);
      fireEvent.click(screen.getByRole("checkbox", { name: /select all/i }));

      expect(
        screen.getByText("Not ordered after all").closest("button"),
      ).toBeDisabled();
    });

    it("says so plainly when nothing needs ordering", async () => {
      await openSuggestions([{ ...MALARIA, status: "ADEQUATE" }]);
      expect(
        screen.getByText("Nothing needs ordering right now."),
      ).toBeInTheDocument();
      expect(screen.queryByText("Mark as ordered")).not.toBeInTheDocument();
    });

    it("keeps the panel open and shows why when marking fails", async () => {
      InventoryItemAPI.markOrdered.mockRejectedValue(
        new Error("could not reach the server"),
      );
      await openSuggestions();
      fireEvent.click(screen.getByRole("checkbox", { name: /select all/i }));
      fireEvent.click(screen.getByText("Mark as ordered"));

      await waitFor(() =>
        expect(
          screen.getByText("could not reach the server"),
        ).toBeInTheDocument(),
      );
      expect(screen.getByRole("dialog")).toBeInTheDocument();
      expect(InventoryBoardAPI.get).toHaveBeenCalledTimes(1);
    });
  });
});
