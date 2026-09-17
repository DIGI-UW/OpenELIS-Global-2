import React from "react";
import { render, screen, within, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import InventoryItemsBoard from "./InventoryItemsBoard";
import { InventoryBoardAPI, InventoryLotAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  InventoryBoardAPI: { get: vi.fn() },
  InventoryLotAPI: { getAll: vi.fn() },
}));

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
const expiry = (iso) => Date.parse(iso);

const LOTS = [
  lot({
    id: 101,
    lotNumber: "MTB-2001",
    effectiveExpirationDate: expiry("2026-11-01T00:00:00Z"),
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
    effectiveExpirationDate: expiry("2026-12-01T00:00:00Z"),
    currentQuantity: 7,
    inventoryItem: { id: 1, name: CARTRIDGE.name },
    location: { hierarchicalPath: "Fridge 1 > Shelf B" },
  }),
  lot({
    id: 103,
    lotNumber: "MTB-2900",
    effectiveExpirationDate: expiry("2027-01-01T00:00:00Z"),
    currentQuantity: 5,
    inventoryItem: { id: 1, name: CARTRIDGE.name },
    location: { hierarchicalPath: "Fridge 2" },
  }),
  lot({
    id: 201,
    lotNumber: "SYPH-77",
    effectiveExpirationDate: expiry("2027-02-01T00:00:00Z"),
    currentQuantity: 8,
    inventoryItem: { id: 2, name: SYPHILIS.name },
    location: { hierarchicalPath: "Room 2" },
  }),
  lot({
    id: 301,
    lotNumber: "MAL-5150",
    barcode: "0034567890123",
    effectiveExpirationDate: expiry("2027-03-01T00:00:00Z"),
    currentQuantity: 60,
    inventoryItem: { id: 3, name: MALARIA.name },
    location: { hierarchicalPath: "Room 2" },
  }),
];

const renderBoard = async (
  board = [CARTRIDGE, SYPHILIS, MALARIA],
  lots = LOTS,
) => {
  InventoryBoardAPI.get.mockResolvedValue(board);
  InventoryLotAPI.getAll.mockResolvedValue(lots);
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <InventoryItemsBoard />
    </IntlProvider>,
  );
  await screen.findByRole("table");
  return view;
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
        <InventoryItemsBoard />
      </IntlProvider>,
    );
    await waitFor(() =>
      expect(
        screen.getByText("The items board could not be loaded."),
      ).toBeInTheDocument(),
    );
    expect(screen.getByText("board is down")).toBeInTheDocument();
  });
});
