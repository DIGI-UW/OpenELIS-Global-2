import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import InventoryReports, {
  toRequestParams,
  DATE_MODE,
  isReconstructedStock,
} from "./InventoryReports";
import { ReportsAPI, InventoryTagAPI } from "./InventoryService";
import messages from "../../languages/en.json";

vi.mock("./InventoryService", () => ({
  ReportsAPI: { preview: vi.fn(), generate: vi.fn() },
  InventoryTagAPI: { getDirectory: vi.fn() },
}));

const renderReports = async () => {
  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <InventoryReports />
    </IntlProvider>,
  );
  // The tag directory resolves before anything is asserted.
  await screen.findByText("Run report");
  return view;
};

// Carbon puts the id on the list-box wrapper; the toggle is the field inside it.
const openDropdown = (id) =>
  fireEvent.click(document.querySelector(`#${id} .cds--list-box__field`));

const openTagFilter = () =>
  fireEvent.click(screen.getByRole("combobox", { name: /tags/i }));

// Every Carbon menu stays in the DOM whether open or not, so an unscoped
// listbox query matches all three controls on this screen.
const optionsOf = (id) =>
  [...document.querySelectorAll(`#${id} [role="option"]`)].map((o) =>
    o.textContent.trim(),
  );

// The multiselect commits its selection a tick after the click, so waiting for
// the chip is what makes the next step act on the selected tag rather than none.
const pickTag = async (name) => {
  fireEvent.click(await screen.findByRole("option", { name }));
  await waitFor(() =>
    expect(screen.getByRole("option", { name })).toHaveAttribute(
      "aria-selected",
      "true",
    ),
  );
};

const chooseReport = async (label) => {
  openDropdown("reportType");
  fireEvent.click(await screen.findByText(label));
};

beforeEach(() => {
  vi.clearAllMocks();
  InventoryTagAPI.getDirectory.mockResolvedValue([
    { name: "Cartridge", active: true },
    { name: "Consumable", active: true },
    { name: "Retired tag", active: false },
  ]);
  ReportsAPI.preview.mockResolvedValue({
    title: "Received",
    headers: ["Date Received", "Item Code"],
    rows: [["2026-09-18 10:00", "MAL-RDT"]],
  });
  ReportsAPI.generate.mockResolvedValue({
    data: new Blob(["a,b"]),
    contentType: "text/csv",
    filename: "received.csv",
  });
});

describe("InventoryReports — the four period reports", () => {
  it("offers exactly the four reports a period needs, and nothing else", async () => {
    await renderReports();
    openDropdown("reportType");

    expect(optionsOf("reportType")).toEqual([
      "Received",
      "Consumed",
      "Stock on hand",
      "Expiring",
    ]);
  });

  it("no longer offers the three reports the recut dropped", async () => {
    await renderReports();
    openDropdown("reportType");

    const options = optionsOf("reportType").join(" ");
    expect(options).not.toMatch(/low stock/i);
    expect(options).not.toMatch(/lot traceability/i);
    expect(options).not.toMatch(/transaction history/i);
  });

  it("asks for a period on Received, and marks it required", async () => {
    await renderReports();

    expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
    expect(screen.getByText("*")).toBeInTheDocument();
  });

  it("asks for one date on Stock on hand, not a period", async () => {
    await renderReports();
    await chooseReport("Stock on hand");

    expect(screen.getByLabelText(/as of/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/start date/i)).not.toBeInTheDocument();
    expect(screen.queryByText("*")).not.toBeInTheDocument();
  });

  it("labels Expiring's range as an expiry window, not a period", async () => {
    await renderReports();
    await chooseReport("Expiring");

    expect(screen.getByLabelText(/expiring from/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/expiring to/i)).toBeInTheDocument();
  });

  it("offers include-expired only on Expiring", async () => {
    await renderReports();
    await chooseReport("Stock on hand");
    expect(screen.getByLabelText(/include inactive/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/include expired/i)).not.toBeInTheDocument();

    await chooseReport("Expiring");
    expect(screen.getByLabelText(/include expired/i)).toBeInTheDocument();
  });

  it("refuses to run Received without its dates, and asks the server for nothing", async () => {
    await renderReports();

    fireEvent.click(screen.getByText("Run report"));

    expect(
      await screen.findByText(/date range is required/i),
    ).toBeInTheDocument();
    expect(ReportsAPI.preview).not.toHaveBeenCalled();
  });
});

describe("InventoryReports — on screen", () => {
  it("renders the returned table rather than only downloading a file", async () => {
    await renderReports();
    await chooseReport("Stock on hand");

    fireEvent.click(screen.getByText("Run report"));

    expect(await screen.findByText("MAL-RDT")).toBeInTheDocument();
    expect(screen.getByText("Date Received")).toBeInTheDocument();
    expect(ReportsAPI.generate).not.toHaveBeenCalled();
  });

  it("says so plainly when nothing matched", async () => {
    ReportsAPI.preview.mockResolvedValue({
      title: "Stock on Hand",
      headers: ["Item Code"],
      rows: [],
    });
    await renderReports();
    await chooseReport("Stock on hand");

    fireEvent.click(screen.getByText("Run report"));

    expect(await screen.findByText(/nothing matched/i)).toBeInTheDocument();
  });

  it("does not claim reconstruction when no date is given", async () => {
    await renderReports();
    await chooseReport("Stock on hand");

    fireEvent.click(screen.getByText("Run report"));

    expect(await screen.findByText("MAL-RDT")).toBeInTheDocument();
    expect(
      screen.queryByText(/reconstructed from recorded stock movements/i),
    ).not.toBeInTheDocument();
  });

  it("drops a stale result when a filter changes, so the table always matches the form", async () => {
    await renderReports();
    await chooseReport("Stock on hand");
    fireEvent.click(screen.getByText("Run report"));
    expect(await screen.findByText("MAL-RDT")).toBeInTheDocument();

    await chooseReport("Expiring");

    expect(screen.queryByText("MAL-RDT")).not.toBeInTheDocument();
  });
});

describe("InventoryReports — the tag filter", () => {
  it("offers the active tags from the directory and hides retired ones", async () => {
    await renderReports();

    openTagFilter();

    expect(await screen.findByText("Cartridge")).toBeInTheDocument();
    expect(screen.getByText("Consumable")).toBeInTheDocument();
    expect(screen.queryByText("Retired tag")).not.toBeInTheDocument();
  });

  it("sends the selected tags to the server", async () => {
    await renderReports();
    await chooseReport("Stock on hand");
    openTagFilter();
    await pickTag("Cartridge");

    fireEvent.click(screen.getByText("Run report"));

    await waitFor(() => expect(ReportsAPI.preview).toHaveBeenCalled());
    expect(ReportsAPI.preview.mock.calls[0][0].tags).toEqual(["Cartridge"]);
  });

  it("still renders the report when the tag directory cannot be loaded", async () => {
    InventoryTagAPI.getDirectory.mockRejectedValue(new Error("offline"));
    await renderReports();
    await chooseReport("Stock on hand");

    fireEvent.click(screen.getByText("Run report"));

    expect(await screen.findByText("MAL-RDT")).toBeInTheDocument();
  });
});

describe("InventoryReports — export", () => {
  it("exports the chosen format without going through the preview", async () => {
    await renderReports();
    await chooseReport("Stock on hand");

    fireEvent.click(screen.getByText("Export"));

    await waitFor(() => expect(ReportsAPI.generate).toHaveBeenCalled());
    expect(ReportsAPI.generate.mock.calls[0][0].exportFormat).toBe("CSV");
    expect(ReportsAPI.preview).not.toHaveBeenCalled();
  });

  it("sends only the as-of date for Stock on hand, never a period start", async () => {
    await renderReports();
    await chooseReport("Stock on hand");

    fireEvent.click(screen.getByText("Export"));

    await waitFor(() => expect(ReportsAPI.generate).toHaveBeenCalled());
    expect(ReportsAPI.generate.mock.calls[0][0].startDate).toBeNull();
  });
});

describe("isReconstructedStock", () => {
  // The Carbon date picker cannot be driven from jsdom, so the rule is pinned
  // here and the rendered wording is checked in the browser.
  it("is true only for a stock report carrying an as-of date", () => {
    expect(isReconstructedStock("STOCK_ON_HAND", new Date(2026, 8, 4))).toBe(
      true,
    );
  });

  it("is false for stock as it stands now", () => {
    expect(isReconstructedStock("STOCK_ON_HAND", null)).toBe(false);
  });

  it("is false for the period reports, whose dates bound events not stock", () => {
    expect(isReconstructedStock("RECEIVED", new Date(2026, 8, 4))).toBe(false);
    expect(isReconstructedStock("CONSUMED", new Date(2026, 8, 4))).toBe(false);
    expect(isReconstructedStock("EXPIRING", new Date(2026, 8, 4))).toBe(false);
  });
});

describe("toRequestParams", () => {
  const form = {
    reportType: { id: "STOCK_ON_HAND" },
    exportFormat: { id: "CSV" },
    startDate: new Date(2026, 6, 1),
    endDate: new Date(2026, 6, 31),
    includeInactive: false,
    includeExpired: true,
    tags: ["Cartridge"],
  };

  /**
   * Switching report type leaves the previous report's dates in the form, so a
   * single-instant report has a period start sitting there to send by accident.
   */
  it("drops a period start left behind when the report only takes an instant", () => {
    const params = toRequestParams(form, DATE_MODE.STOCK_ON_HAND);

    expect(params.startDate).toBeNull();
    expect(params.endDate).toBe("2026-07-31");
  });

  it("sends both ends for a period report", () => {
    const params = toRequestParams(
      { ...form, reportType: { id: "CONSUMED" } },
      DATE_MODE.CONSUMED,
    );

    expect(params.startDate).toBe("2026-07-01");
    expect(params.endDate).toBe("2026-07-31");
  });

  it("carries the tag filter through", () => {
    expect(toRequestParams(form, DATE_MODE.STOCK_ON_HAND).tags).toEqual([
      "Cartridge",
    ]);
  });
});
