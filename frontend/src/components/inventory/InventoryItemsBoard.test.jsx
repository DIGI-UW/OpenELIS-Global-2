import React from "react";
import { render, screen, within, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import InventoryItemsBoard from "./InventoryItemsBoard";
import { NotificationContext } from "../layout/Layout";
import {
  InventoryBoardAPI,
  InventoryCountAPI,
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
    getTags: vi.fn(),
    deactivate: vi.fn(),
    activate: vi.fn(),
  },
  InventoryManagementAPI: { consume: vi.fn() },
  InventoryCountAPI: { record: vi.fn() },
}));

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
vi.mock("./QuickReceiveModal", () =>
  modalStub("quick-receive", (p) => `receive:item:${p.initialItemId}`),
);
vi.mock("./ManageTagsModal", () =>
  modalStub("manage-tags", () => "manage-tags"),
);
vi.mock("./InventoryItemForm", () =>
  modalStub("item-form", (p) =>
    p.item
      ? `item:${p.item.id}:${p.item.name}:observed=${p.observedLeadTime}`
      : "item:none",
  ),
);

vi.mock("./LotDetailsPanel", () => ({
  default: ({ open, lot }) =>
    open ? <div data-testid="lot-details">{lot.lotNumber}</div> : null,
}));

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
  tags: ["Cartridge", "TB", "Cold chain"],
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
  tags: ["Syphilis kit"],
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
  tags: ["RDT", "Malaria"],
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
  countsAsAvailableStock: true,
  location: null,
  ...overrides,
});

// Lot expiry ships as epoch milliseconds (java.sql.Timestamp), not an ISO string.
const expiry = (daysFromNow) => shiftDays(daysFromNow).getTime();

const LOTS = [
  lot({
    id: 101,
    lotNumber: "MTB-2001",
    effectiveExpirationDate: expiry(-10),
    currentQuantity: 5,
    qcStatus: "FAILED",
    availableForUse: false,
    countsAsAvailableStock: false,
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

// An expanded row nests a lot table, so a role query would count its rows too.
const bodyRows = () =>
  [...screen.getAllByRole("table")[0].tBodies[0].children].filter((row) =>
    row.classList.contains("cds--parent-row"),
  );

const rowNamed = (name) =>
  bodyRows().find((row) => within(row).queryByText(name));

beforeEach(() => {
  vi.clearAllMocks();
  InventoryItemAPI.getTags.mockResolvedValue([
    "Cartridge",
    "TB",
    "Cold chain",
    "Syphilis kit",
    "RDT",
    "Malaria",
  ]);
});

describe("InventoryItemsBoard", () => {
  it("renders every column of a projected row from the board payload", async () => {
    await renderBoard();
    const row = rowNamed(CARTRIDGE.name);
    const cells = within(row).getAllByRole("cell");

    // cells[0] is the expand control column.
    expect(cells[1]).toHaveTextContent(CARTRIDGE.name);
    expect(cells[1]).toHaveTextContent("GENEXPERT_MTB_RIF");
    expect(cells[1]).toHaveTextContent("Cartridge");
    expect(cells[1]).toHaveTextContent("TB");
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
    expect(runsOut).toHaveTextContent(`${dayLabel(20)} – ${dayLabel(27)}`);
  });

  it("gives a cold-start row threshold status and no invented date", async () => {
    await renderBoard();
    const cells = within(rowNamed(SYPHILIS.name)).getAllByRole("cell");
    expect(cells[4]).toHaveTextContent(
      "Too few days of use to project a run-out date",
    );
    expect(cells[4]).not.toHaveTextContent("Building data");
    expect(cells[5]).toHaveTextContent("30d (default — set to improve)");
    expect(cells[4].textContent).not.toMatch(/\d{1,2}/);
    expect(cells[5].textContent).not.toMatch(/\b[A-Z][a-z]{2} \d{1,2}\b/);
    expect(cells[6]).toHaveTextContent("Building data");
  });

  it("says out of stock rather than quoting a window that ends today", async () => {
    const empty = {
      ...CARTRIDGE,
      itemId: 9,
      name: "Uni-Gold HIV Kit",
      onHand: 0,
      runOutEarly: isoDay(0),
      runOutLate: isoDay(0),
    };
    await renderBoard([empty], []);
    const runsOut = within(rowNamed(empty.name)).getAllByRole("cell")[4];
    expect(runsOut).toHaveTextContent("Out of stock");
    expect(runsOut).not.toHaveTextContent("–");
  });

  it("shows one run-out date when steady use leaves no spread", async () => {
    const steady = {
      ...CARTRIDGE,
      itemId: 9,
      name: "Steady reagent",
      runOutEarly: isoDay(9),
      runOutLate: isoDay(9),
    };
    await renderBoard([steady], []);
    const runsOut = within(rowNamed(steady.name)).getAllByRole("cell")[4];
    expect(runsOut).toHaveTextContent(dayLabel(9));
    expect(runsOut).not.toHaveTextContent("–");
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

    sortByRunsOut();
    expect(bodyRows().map((row) => row.textContent)[2]).toContain(
      SYPHILIS.name,
    );
    sortByRunsOut();
    expect(bodyRows().map((row) => row.textContent)[2]).toContain(
      SYPHILIS.name,
    );
  });

  it("finds an item by a tag that appears nowhere in its name, code or lots", async () => {
    await renderBoard();
    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "Cold chain" },
    });
    expect(bodyRows()).toHaveLength(1);
    expect(bodyRows()[0]).toHaveTextContent(CARTRIDGE.name);
  });

  it("matches a tag without regard to case", async () => {
    await renderBoard();
    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "COLD CHAIN" },
    });
    expect(bodyRows()).toHaveLength(1);
    expect(bodyRows()[0]).toHaveTextContent(CARTRIDGE.name);
  });

  it("shows an item's tags on its row so the search term is visible", async () => {
    await renderBoard();
    const row = rowNamed(MALARIA.name);
    expect(within(row).getByText("RDT")).toBeInTheDocument();
    expect(within(row).getByText("Malaria")).toBeInTheDocument();
  });

  it("renders a row with no tags without leaving a stray separator", async () => {
    await renderBoard([{ ...CARTRIDGE, tags: [] }], []);
    const row = rowNamed(CARTRIDGE.name);
    expect(within(row).getAllByRole("cell")[1]).toHaveTextContent(
      CARTRIDGE.code,
    );
    expect(within(row).queryByText("Cartridge")).not.toBeInTheDocument();
  });

  it("does not take the keyboard hostage with a dialog role", async () => {
    await renderBoard();

    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toBeInTheDocument();
    expect(document.body).toHaveFocus();
  });

  // FilterableMultiSelect updates through Downshift a tick after the click; await assertions
  const openTagFilter = () =>
    fireEvent.click(screen.getByRole("combobox", { name: /filter by tags/i }));

  // Scoped: the two Select dropdowns in the same toolbar also render options.
  const offeredTags = () =>
    within(document.querySelector(".board-tag-filter"))
      .getAllByRole("option")
      .map((option) => option.textContent);

  // The menu stays open between selections; clicking the field again would close it
  const selectTag = async (name) =>
    fireEvent.click(await screen.findByRole("option", { name }));

  it("matches an item carrying any one of the selected tags", async () => {
    await renderBoard();

    openTagFilter();
    await selectTag("Cold chain");
    await selectTag("Syphilis kit");

    await waitFor(() => expect(bodyRows()).toHaveLength(2));
  });

  it("narrows to one item on a single tag", async () => {
    await renderBoard();

    openTagFilter();
    await selectTag("Cold chain");

    await waitFor(() => expect(bodyRows()).toHaveLength(1));
    expect(bodyRows()[0]).toHaveTextContent(CARTRIDGE.name);
  });

  it("shows each selected tag as a removable chip, not a count", async () => {
    await renderBoard();
    openTagFilter();
    await selectTag("Cold chain");
    await waitFor(() => expect(bodyRows()).toHaveLength(1));

    const chips = document.querySelector(".board-tag-chips");
    expect(chips).toHaveTextContent("Cold chain");

    fireEvent.click(within(chips).getAllByRole("button")[0]);

    await waitFor(() => expect(bodyRows()).toHaveLength(3));
  });

  it("clears every tag at once", async () => {
    await renderBoard();
    openTagFilter();
    await selectTag("Cold chain");
    await waitFor(() => expect(bodyRows()).toHaveLength(1));

    fireEvent.click(screen.getByRole("button", { name: "Clear tags" }));

    await waitFor(() => expect(bodyRows()).toHaveLength(3));
    expect(document.querySelector(".board-tag-chips")).toBeNull();
  });

  it("offers only tags the board's own rows carry", async () => {
    await renderBoard();

    openTagFilter();

    const offered = offeredTags();
    expect(offered).toContain("Cold chain");
    expect(offered).not.toContain("Reagent");
  });

  it("stops offering a retired tag as a filter but keeps showing it on the row", async () => {
    InventoryItemAPI.getTags.mockResolvedValue([
      "Cartridge",
      "Cold chain",
      "Syphilis kit",
      "RDT",
      "Malaria",
    ]);
    await renderBoard();

    openTagFilter();
    const offered = offeredTags();
    expect(offered).not.toContain("TB");
    expect(offered).toContain("Cartridge");

    expect(
      within(rowNamed(CARTRIDGE.name)).getByText("TB"),
    ).toBeInTheDocument();
  });

  it("defines a new item from the toolbar, with no item to edit", async () => {
    await renderBoard();

    fireEvent.click(screen.getByRole("button", { name: /new item/i }));

    expect(screen.getByTestId("item-form-target")).toHaveTextContent(
      "item:none",
    );
  });

  it("asks before deactivating an item and says what it means", async () => {
    await renderBoard();

    await openRowMenu(CARTRIDGE.name);
    fireEvent.click(screen.getByText("Deactivate item"));

    expect(screen.getByText(/Deactivate GeneXpert/)).toBeInTheDocument();
    expect(
      screen.getByText(/leaves the board and stops being offered/i),
    ).toBeInTheDocument();
    expect(screen.getByText(/lots and history are kept/i)).toBeInTheDocument();
    expect(InventoryItemAPI.deactivate).not.toHaveBeenCalled();
  });

  it("deactivates the item once confirmed", async () => {
    InventoryItemAPI.deactivate.mockResolvedValue({});
    await renderBoard();
    await openRowMenu(CARTRIDGE.name);
    fireEvent.click(screen.getByText("Deactivate item"));

    // The row menu item shares this label, so target the dialog's button.
    fireEvent.click(
      document.querySelector(".cds--modal.is-visible .cds--btn--danger"),
    );

    await waitFor(() =>
      expect(InventoryItemAPI.deactivate).toHaveBeenCalledWith(
        CARTRIDGE.itemId,
      ),
    );
  });

  it("asks the server for deactivated rows only when they are wanted", async () => {
    await renderBoard();
    expect(InventoryBoardAPI.get).toHaveBeenCalledWith(false);

    fireEvent.click(screen.getByLabelText("Show deactivated"));

    await waitFor(() =>
      expect(InventoryBoardAPI.get).toHaveBeenCalledWith(true),
    );
  });

  it("marks a deactivated row and offers to bring it back", async () => {
    InventoryItemAPI.activate.mockResolvedValue({});
    await renderBoard([{ ...CARTRIDGE, active: false }, MALARIA]);

    const row = rowNamed(CARTRIDGE.name);
    expect(within(row).getByText("Deactivated")).toBeInTheDocument();

    await openRowMenu(CARTRIDGE.name);
    fireEvent.click(screen.getByText("Reactivate item"));

    await waitFor(() =>
      expect(InventoryItemAPI.activate).toHaveBeenCalledWith(CARTRIDGE.itemId),
    );
  });

  it("leaves an active row unmarked", async () => {
    await renderBoard();

    expect(
      within(rowNamed(CARTRIDGE.name)).queryByText("Deactivated"),
    ).not.toBeInTheDocument();
  });

  it("shows when an item was last counted, and says so when it never was", async () => {
    await renderBoard([
      { ...CARTRIDGE, lastCountedOn: isoDay(-3) },
      { ...MALARIA, lastCountedOn: null },
    ]);

    expect(rowNamed(CARTRIDGE.name)).toHaveTextContent(dayLabel(-3));
    expect(rowNamed(MALARIA.name)).toHaveTextContent("Never counted");
  });

  describe("count mode", () => {
    const enterCountMode = () =>
      fireEvent.click(screen.getByRole("button", { name: "Count mode" }));

    const countField = (lotId) =>
      screen.getByLabelText(new RegExp(`^Counted quantity`, "i"), {
        selector: `#count-${lotId}`,
      });

    beforeEach(() => {
      InventoryCountAPI.record.mockResolvedValue({ adjusted: 1, confirmed: 0 });
    });

    it("turns the on-hand cell into a count entry for an item with one lot", async () => {
      await renderBoard();
      enterCountMode();

      const row = rowNamed(SYPHILIS.name);
      expect(within(row).getByRole("spinbutton")).toBeInTheDocument();
      expect(row).toHaveTextContent("of 8 tests");
    });

    it("gives the row and the lot table their own input for a one-lot item", async () => {
      await renderBoard();
      enterCountMode();
      fireEvent.click(screen.getByRole("button", { name: SYPHILIS.name }));

      const ids = [...document.querySelectorAll('input[id^="count-"]')].map(
        (input) => input.id,
      );
      expect(ids).toContain("count-201");
      expect(new Set(ids).size).toBe(ids.length);
    });

    it("refuses to count an item with several lots from the row", async () => {
      await renderBoard();
      enterCountMode();

      const row = rowNamed(CARTRIDGE.name);
      expect(within(row).queryByRole("spinbutton")).not.toBeInTheDocument();
      expect(row).toHaveTextContent("Count each lot below");
    });

    it("measures against what is on the shelf, not against usable stock", async () => {
      await renderBoard();
      enterCountMode();

      expect(rowNamed(CARTRIDGE.name)).toHaveTextContent(
        "17 tests expected here",
      );
    });

    it("sends only the lots a count was typed into", async () => {
      await renderBoard();
      enterCountMode();
      fireEvent.click(screen.getByRole("button", { name: CARTRIDGE.name }));

      fireEvent.change(countField(102), { target: { value: "6" } });
      fireEvent.click(screen.getByRole("button", { name: "Confirm count" }));

      await waitFor(() => expect(InventoryCountAPI.record).toHaveBeenCalled());
      expect(InventoryCountAPI.record).toHaveBeenCalledWith([
        { lotId: 102, countedQuantity: 6 },
      ]);
    });

    it("cannot confirm a count with nothing counted", async () => {
      await renderBoard();
      enterCountMode();

      expect(
        screen.getByRole("button", { name: "Confirm count" }),
      ).toBeDisabled();
    });

    it("shows the difference rather than leaving it to be worked out", async () => {
      await renderBoard();
      enterCountMode();
      fireEvent.click(screen.getByRole("button", { name: CARTRIDGE.name }));

      fireEvent.change(countField(102), { target: { value: "4" } });

      expect(screen.getByText("-3")).toBeInTheDocument();
    });

    it("says so when a count agrees with the record", async () => {
      await renderBoard();
      enterCountMode();
      fireEvent.click(screen.getByRole("button", { name: CARTRIDGE.name }));

      fireEvent.change(countField(102), { target: { value: "7" } });

      expect(screen.getByText("Matches")).toBeInTheDocument();
    });

    it("abandoning the mode discards every count typed", async () => {
      await renderBoard();
      enterCountMode();
      fireEvent.click(screen.getByRole("button", { name: CARTRIDGE.name }));
      fireEvent.change(countField(102), { target: { value: "4" } });

      fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
      expect(InventoryCountAPI.record).not.toHaveBeenCalled();

      enterCountMode();
      expect(document.querySelector("#count-102")).toHaveValue(null);
    });

    it("says which shelf is being counted when one is scoped", async () => {
      await renderBoard();
      fireEvent.change(screen.getByLabelText("Filter by Location"), {
        target: { value: "Fridge 2" },
      });
      enterCountMode();

      expect(screen.getByText(/Counting Fridge 2/)).toBeInTheDocument();
    });

    it("counts only the lots on the scoped shelf", async () => {
      await renderBoard();
      fireEvent.change(screen.getByLabelText("Filter by Location"), {
        target: { value: "Fridge 2" },
      });
      enterCountMode();

      const row = rowNamed(CARTRIDGE.name);
      expect(within(row).getByRole("spinbutton")).toBeInTheDocument();
      expect(row).toHaveTextContent("of 5 tests");
    });

    it("warns that an unscoped count covers everything", async () => {
      await renderBoard();
      enterCountMode();

      expect(screen.getByText(/Counting everywhere/)).toBeInTheDocument();
    });
  });

  it("opens the tag directory from the toolbar", async () => {
    await renderBoard();

    fireEvent.click(screen.getByRole("button", { name: "Manage tags" }));

    expect(screen.getByTestId("manage-tags")).toBeInTheDocument();
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
    expect(lotRows[0]).toHaveTextContent("Failed");
  });

  it("flags a lot that failed QC but still holds stock for disposal", async () => {
    await renderBoard();
    fireEvent.click(
      within(rowNamed(CARTRIDGE.name)).getByRole("button", {
        name: CARTRIDGE.name,
      }),
    );

    const lotRows = within(screen.getAllByRole("table")[1])
      .getAllByRole("row")
      .slice(1);
    const flagged = lotRows.filter((row) =>
      within(row).queryByText("Failed QC: dispose"),
    );
    expect(flagged).toHaveLength(1);
    expect(flagged[0]).toHaveTextContent("MTB-2001");
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
    // The QC-failed lot counts towards neither location.
    const split = screen.getByText(/By location:/);
    expect(split).toHaveTextContent("Fridge 1 > Shelf B: 7 tests");
    expect(split).toHaveTextContent("Fridge 2: 5 tests");
  });

  it("splits every lot on-hand counts by location, pending and unassigned included", async () => {
    await renderBoard(
      [CARTRIDGE, SYPHILIS, MALARIA],
      [
        ...LOTS,
        lot({
          id: 104,
          lotNumber: "MTB-3001",
          effectiveExpirationDate: expiry("2027-02-01T00:00:00Z"),
          currentQuantity: 4,
          qcStatus: "PENDING",
          availableForUse: false,
          inventoryItem: { id: 1, name: CARTRIDGE.name },
          location: { hierarchicalPath: "Fridge 3" },
        }),
        lot({
          id: 105,
          lotNumber: "MTB-3002",
          effectiveExpirationDate: expiry("2027-03-01T00:00:00Z"),
          currentQuantity: 2,
          inventoryItem: { id: 1, name: CARTRIDGE.name },
        }),
      ],
    );
    fireEvent.click(
      within(rowNamed(CARTRIDGE.name)).getByRole("button", {
        name: CARTRIDGE.name,
      }),
    );

    const split = screen.getByText(/By location:/);
    expect(split).toHaveTextContent("Fridge 3: 4 tests");
    expect(split).toHaveTextContent("Not assigned: 2 tests");
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

      expect(screen.getByTestId("quick-receive-target")).toHaveTextContent(
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
      fireEvent.click(screen.getByText("quick-receive-save"));

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
      const banner = screen.getByRole("status");
      expect(banner).toHaveTextContent(CARTRIDGE.name);
      expect(banner).not.toHaveTextContent(MALARIA.name);
    });

    it("drops an item from the banner once it is marked ordered, without hiding its row", async () => {
      await renderBoard([
        { ...CARTRIDGE, orderedOn: "2026-09-17", orderNote: "PO-1" },
        SYPHILIS,
        MALARIA,
      ]);
      expect(screen.queryByRole("status")).not.toBeInTheDocument();

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

    it("names five items in the banner and counts the rest", async () => {
      const critical = Array.from({ length: 7 }, (_, i) => ({
        ...CARTRIDGE,
        itemId: 900 + i,
        code: `CRIT-${i}`,
        name: `Critical item ${i + 1}`,
      }));
      await renderBoard(critical, []);

      const banner = document.querySelector(".board-critical-banner");
      expect(banner).toHaveTextContent("Critical item 5");
      expect(banner).not.toHaveTextContent("Critical item 6");
      expect(banner).toHaveTextContent("and 2 more");
    });

    it("dates the order-by the way the board does", async () => {
      await openSuggestions();
      const dialog = screen.getByRole("dialog");
      expect(within(dialog).getByText(dayLabel(10))).toBeInTheDocument();
      expect(within(dialog).getByText("past due")).toBeInTheDocument();
      expect(within(dialog).queryByText(isoDay(10))).not.toBeInTheDocument();
    });

    it("lists exactly the rows the board badges, and nothing else", async () => {
      await openSuggestions();
      const dialog = screen.getByRole("dialog");
      expect(within(dialog).getByText(/GeneXpert/)).toBeInTheDocument();
      expect(within(dialog).getByText(/Malaria RDT/)).toBeInTheDocument();
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
