import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import InventoryLotsPage from "./InventoryLotsPage";
import * as Utils from "../../utils/Utils";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <InventoryLotsPage />
      </MemoryRouter>
    </IntlProvider>,
  );

const assignedLot = {
  id: 7000,
  lotNumber: "LOT-2025-001",
  barcode: "TEST_REAGENT_A_LOT_2025_001",
  itemName: "Test Reagent A",
  quantity: 10,
  status: "ACTIVE",
  location: "Main Lab > Freezer 1 > Shelf A",
  positionCoordinate: "A1",
};

const releasedLot = {
  id: 7001,
  lotNumber: "LOT-2025-002",
  barcode: "TEST_REAGENT_A_LOT_2025_002",
  itemName: "Test Reagent A",
  quantity: 0,
  status: "DISPOSED",
  location: "",
  positionCoordinate: "",
};

beforeEach(() => {
  Utils.getFromOpenElisServer.mockReset();
});

describe("InventoryLotsPage", () => {
  it("lists lots against the inventory-lots storage endpoint", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([assignedLot]),
    );
    renderPage();

    expect(await screen.findByText("LOT-2025-001")).toBeInTheDocument();
    const [url] = Utils.getFromOpenElisServer.mock.calls[0];
    expect(url).toContain("/rest/storage/inventory-lots");
  });

  it("shows the resolved location with its position", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([assignedLot]),
    );
    renderPage();

    expect(
      await screen.findByText("Main Lab > Freezer 1 > Shelf A · A1"),
    ).toBeInTheDocument();
  });

  it("marks a released lot as not assigned rather than hiding it", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([releasedLot]),
    );
    renderPage();

    expect(await screen.findByText("LOT-2025-002")).toBeInTheDocument();
    expect(screen.getByText("Not assigned")).toBeInTheDocument();
  });

  it("shows the barcode so a scanned label can be matched to a row", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([assignedLot]),
    );
    renderPage();

    expect(
      await screen.findByText("TEST_REAGENT_A_LOT_2025_001"),
    ).toBeInTheDocument();
  });

  it("filters on barcode as well as lot number", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([assignedLot, releasedLot]),
    );
    renderPage();
    await screen.findByText("LOT-2025-001");

    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "TEST_REAGENT_A_LOT_2025_002" },
    });

    const table = document.querySelector("table");
    expect(within(table).getByText("LOT-2025-002")).toBeInTheDocument();
    expect(within(table).queryByText("LOT-2025-001")).not.toBeInTheDocument();
  });

  it("does not refetch while the user types, since the filter is local", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([assignedLot, releasedLot]),
    );
    renderPage();
    await screen.findByText("LOT-2025-001");
    const before = Utils.getFromOpenElisServer.mock.calls.length;

    "LOT-".split("").forEach((_c, i) =>
      fireEvent.change(screen.getByRole("searchbox"), {
        target: { value: "LOT-".slice(0, i + 1) },
      }),
    );

    expect(Utils.getFromOpenElisServer.mock.calls.length).toBe(before);
  });

  it("does not refetch when the page is turned, since the slice is local", async () => {
    const lots = Array.from({ length: 30 }, (_, i) => ({
      ...assignedLot,
      id: 8000 + i,
      lotNumber: `LOT-${i}`,
    }));
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => cb(lots));
    renderPage();
    await screen.findByText("LOT-0");
    const before = Utils.getFromOpenElisServer.mock.calls.length;

    fireEvent.click(screen.getByLabelText("Next page"));
    expect(await screen.findByText("LOT-5")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/items per page/i), {
      target: { value: "50" },
    });

    expect(Utils.getFromOpenElisServer.mock.calls.length).toBe(before);
  });

  it("counts what the search actually matched, not the unfiltered listing", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([assignedLot, releasedLot]),
    );
    renderPage();
    await screen.findByText("LOT-2025-001");

    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "no-such-lot" },
    });

    expect(screen.getByText(/of 0 items/i)).toBeInTheDocument();
  });

  it("labels the search box with the lots placeholder", async () => {
    Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([assignedLot]),
    );
    renderPage();

    expect(
      await screen.findByPlaceholderText(
        "Search by lot number, barcode, item or location...",
      ),
    ).toBeInTheDocument();
  });
});
