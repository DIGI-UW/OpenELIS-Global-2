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

const renderAt = (path) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={[path]}>
        <Route path="/Storage/:resource?">
          <StorageManagementPage />
        </Route>
        <Route
          path="*"
          render={({ location }) => (
            <span data-testid="path">{location.pathname}</span>
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

  it("navigates to a level listing when its tile is clicked", async () => {
    const { container } = renderAt("/Storage");
    await waitFor(() =>
      expect(container.querySelector(".storage-metric-tile")).toBeTruthy(),
    );

    fireEvent.click(screen.getByText("Racks").closest("button, a"));

    await waitFor(() =>
      expect(screen.getByTestId("path")).toHaveTextContent("/Storage/racks"),
    );
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
});
