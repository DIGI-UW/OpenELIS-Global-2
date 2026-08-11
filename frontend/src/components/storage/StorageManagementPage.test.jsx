import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import StorageManagementPage from "./StorageManagementPage";
import * as Utils from "../utils/Utils";
import messages from "../../languages/en.json";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

// Each panel's list page has its own tests; here we only assert which one the
// container shows.
vi.mock("./pages/SampleItemsPage", () => ({
  default: () => <div>sample-items-panel</div>,
}));
vi.mock("./pages/InventoryLotsPage", () => ({
  default: () => <div>inventory-lots-panel</div>,
}));
vi.mock("./pages/RoomsPage", () => ({ default: () => <div>rooms-table</div> }));
vi.mock("./pages/DevicesPage", () => ({
  default: () => <div>devices-table</div>,
}));
vi.mock("./pages/ShelvesPage", () => ({
  default: () => <div>shelves-table</div>,
}));
vi.mock("./pages/RacksPage", () => ({ default: () => <div>racks-table</div> }));
vi.mock("./pages/BoxesPage", () => ({ default: () => <div>boxes-table</div> }));

const renderAt = (path) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={[path]}>
        <Route path="/Storage/:resource?">
          <StorageManagementPage />
        </Route>
        <Route
          path="*"
          render={({ location, history }) => (
            <>
              <span data-testid="path">{location.pathname}</span>
              {/* Stands in for what create/delete does: stamp ?t= to refresh. */}
              <button
                onClick={() =>
                  history.replace(`${location.pathname}?t=${Date.now()}`)
                }
              >
                stamp-refresh
              </button>
            </>
          )}
        />
      </MemoryRouter>
    </IntlProvider>,
  );

beforeEach(() => {
  Utils.getFromOpenElisServer.mockReset();
  Utils.getFromOpenElisServer.mockImplementation((url, cb) =>
    cb({ rooms: 2, devices: 3, shelves: 4, racks: 5, boxes: 6 }),
  );
});

describe("StorageManagementPage", () => {
  it("follows the Inventory Management shell — breadcrumb, orderLegendBody, heading", () => {
    const { container } = renderAt("/Storage");

    expect(screen.getByText("Home")).toBeInTheDocument();
    expect(container.querySelector(".orderLegendBody")).toBeInTheDocument();
    expect(container.querySelector(".orderLegendBody h2")).toHaveTextContent(
      "Storage Management",
    );
  });

  it("has exactly three tabs", () => {
    renderAt("/Storage");

    expect(screen.getAllByRole("tab")).toHaveLength(3);
    expect(screen.getByRole("tab", { name: "Dashboard" })).toBeInTheDocument();
    expect(
      screen.getByRole("tab", { name: "Sample Items" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("tab", { name: "Inventory Lots" }),
    ).toBeInTheDocument();
  });

  it("shows one counted tile per hierarchy level on the dashboard", async () => {
    const { container } = renderAt("/Storage");

    await waitFor(() =>
      expect(container.querySelectorAll(".storage-metric-tile")).toHaveLength(
        5,
      ),
    );
    ["Rooms", "Devices", "Shelves", "Racks", "Boxes"].forEach((label) =>
      expect(screen.getByText(label)).toBeInTheDocument(),
    );
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("6")).toBeInTheDocument();
  });

  it("shows one table under the tiles, defaulting to rooms", async () => {
    renderAt("/Storage");

    expect(await screen.findByText("rooms-table")).toBeInTheDocument();
    expect(screen.queryByText("racks-table")).not.toBeInTheDocument();
  });

  it("swaps which level the table shows when a tile is picked", async () => {
    const { container } = renderAt("/Storage");
    await waitFor(() =>
      expect(container.querySelector(".storage-metric-tile")).toBeTruthy(),
    );

    fireEvent.click(screen.getByText("Racks").closest("button, a"));

    expect(await screen.findByText("racks-table")).toBeInTheDocument();
    expect(screen.queryByText("rooms-table")).not.toBeInTheDocument();
    // The URL follows so the view stays shareable.
    expect(screen.getByTestId("path")).toHaveTextContent("/Storage/racks");
  });

  it("marks the tile whose level the table is showing", async () => {
    const { container } = renderAt("/Storage/shelves");

    await waitFor(() =>
      expect(
        container.querySelectorAll(".storage-metric-tile--selected"),
      ).toHaveLength(1),
    );
    const selected = container.querySelector(".storage-metric-tile--selected");
    expect(selected).toHaveTextContent("Shelves");
  });

  it("a level deep link stays on the Dashboard tab", async () => {
    renderAt("/Storage/boxes");

    expect(screen.getByRole("tab", { name: "Dashboard" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    expect(await screen.findByText("boxes-table")).toBeInTheDocument();
  });

  it("deep link /Storage/sample-items selects the Samples tab", () => {
    renderAt("/Storage/sample-items");

    expect(screen.getByRole("tab", { name: "Sample Items" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    expect(screen.getByText("sample-items-panel")).toBeInTheDocument();
  });

  it("deep link /Storage/inventory-lots selects the Inventory Lots tab", () => {
    renderAt("/Storage/inventory-lots");

    expect(screen.getByRole("tab", { name: "Inventory Lots" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    expect(screen.getByText("inventory-lots-panel")).toBeInTheDocument();
  });

  it("lands on the Dashboard tab at /Storage", () => {
    renderAt("/Storage");

    expect(screen.getByRole("tab", { name: "Dashboard" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
  });

  it("re-reads the counts when a location is created, so tiles do not go stale", async () => {
    // Creating stamps ?t= on the URL to refresh the table; the tiles must follow.
    let call = 0;
    Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
      call += 1;
      cb({ rooms: call, devices: 3, shelves: 4, racks: 5, boxes: 6 });
    });

    renderAt("/Storage/rooms");
    await waitFor(() => expect(screen.getByText("1")).toBeInTheDocument());

    fireEvent.click(screen.getByText("stamp-refresh"));

    await waitFor(() => expect(screen.getByText("2")).toBeInTheDocument());
  });
});
