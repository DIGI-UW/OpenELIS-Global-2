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
});
