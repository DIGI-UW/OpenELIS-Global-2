import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import StorageManagementPage from "./StorageManagementPage";
import messages from "../../languages/en.json";

// Each panel's list page is covered by its own test file; here we only care
// which one the container decides to show.
vi.mock("./StorageDashboard/StorageLocationsMetricCard", () => ({
  default: () => <div>metric-card</div>,
}));
vi.mock("./pages/SampleItemsPage", () => ({
  default: () => <div>sample-items-panel</div>,
}));
vi.mock("./pages/InventoryLotsPage", () => ({
  default: () => <div>inventory-lots-panel</div>,
}));
vi.mock("./pages/RoomsPage", () => ({ default: () => <div>rooms-panel</div> }));
vi.mock("./pages/DevicesPage", () => ({
  default: () => <div>devices-panel</div>,
}));
vi.mock("./pages/ShelvesPage", () => ({
  default: () => <div>shelves-panel</div>,
}));
vi.mock("./pages/RacksPage", () => ({ default: () => <div>racks-panel</div> }));
vi.mock("./pages/BoxesPage", () => ({ default: () => <div>boxes-panel</div> }));

const renderAt = (path) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={[path]}>
        <Route path="/Storage/:resource?">
          <StorageManagementPage />
        </Route>
      </MemoryRouter>
    </IntlProvider>,
  );

describe("StorageManagementPage", () => {
  it("gathers storage into four tabs instead of seven sidenav entries", () => {
    renderAt("/Storage");

    expect(screen.getByRole("tab", { name: "Overview" })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Locations" })).toBeInTheDocument();
    expect(
      screen.getByRole("tab", { name: "Sample Items" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("tab", { name: "Inventory Lots" }),
    ).toBeInTheDocument();
  });

  it("lands on Overview at /Storage", () => {
    renderAt("/Storage");

    expect(screen.getByRole("tab", { name: "Overview" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    expect(screen.getByText("metric-card")).toBeInTheDocument();
  });

  // The five levels used to be sidenav siblings; their URLs must keep working.
  it.each([
    ["/Storage/rooms", "rooms-panel", "Rooms"],
    ["/Storage/devices", "devices-panel", "Devices"],
    ["/Storage/shelves", "shelves-panel", "Shelves"],
    ["/Storage/racks", "racks-panel", "Racks"],
    ["/Storage/boxes", "boxes-panel", "Boxes"],
  ])("deep link %s selects Locations with %s", (path, panel, switchLabel) => {
    renderAt(path);

    expect(screen.getByRole("tab", { name: "Locations" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    expect(screen.getByText(panel)).toBeInTheDocument();
    expect(screen.getByText(switchLabel)).toBeInTheDocument();
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

  it("switching level navigates so the URL stays shareable", () => {
    renderAt("/Storage/rooms");

    fireEvent.click(screen.getByText("Racks"));

    expect(screen.getByText("racks-panel")).toBeInTheDocument();
  });
});
