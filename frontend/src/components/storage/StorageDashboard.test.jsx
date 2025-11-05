import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import StorageDashboard from "./StorageDashboard";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import messages from "../../languages/en.json";

// Mock the API utilities
jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

// Mock react-router-dom
const mockHistory = {
  replace: jest.fn(),
  push: jest.fn(),
};

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useHistory: () => mockHistory,
  useLocation: () => ({ pathname: "/Storage/samples" }),
}));

// Mock NotificationContext provider
const mockNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: jest.fn(),
  addNotification: jest.fn(),
};

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider value={mockNotificationContext}>
          {component}
        </NotificationContext.Provider>
      </IntlProvider>
    </BrowserRouter>,
  );
};

describe("StorageDashboard Filter UI", () => {
  const mockMetrics = {
    totalSamples: 100,
    active: 95,
    disposed: 5,
    storageLocations: 0,
  };

  const mockRooms = [
    { id: "1", name: "Main Laboratory", code: "MAIN", active: true },
  ];

  const mockDevices = [
    {
      id: "10",
      name: "Freezer Unit 1",
      code: "FRZ01",
      roomId: "1",
      active: true,
    },
  ];

  const mockSamples = [
    {
      id: "sample-1",
      accessionNumber: "S-2025-001",
      status: "active",
      location: "Main Laboratory > Freezer Unit 1",
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/dashboard/metrics")) {
        callback(mockMetrics);
      } else if (url.includes("/rest/storage/rooms")) {
        callback(mockRooms);
      } else if (url.includes("/rest/storage/devices")) {
        callback(mockDevices);
      } else if (url.includes("/rest/storage/samples")) {
        callback(mockSamples);
      } else if (url.includes("/rest/storage/dashboard/location-counts")) {
        callback({ rooms: 1, devices: 1, shelves: 0, racks: 0 });
      }
    });
  });

  /**
   * T062i3: Test Samples tab shows single location dropdown and status filter
   * Samples tab should have single LocationFilterDropdown (not separate room/device dropdowns)
   */
  test("testSamplesTab_ShowsSingleLocationDropdownAndStatusFilter", async () => {
    renderWithIntl(<StorageDashboard />);

    // Wait for dashboard to load
    await waitFor(() => {
      expect(
        screen.getByText(/Storage Management Dashboard/i),
      ).toBeInTheDocument();
    });

    // Verify single location filter dropdown exists (not separate room/device)
    const locationFilter = screen.queryByTestId("location-filter-dropdown");
    expect(locationFilter).toBeInTheDocument();

    // Verify status filter exists
    const statusFilter = screen.getByTestId("status-filter");
    expect(statusFilter).toBeInTheDocument();
  });

  /**
   * T062i3: Test Rooms tab shows status filter
   * Rooms tab should only have status filter
   */
  test("testRoomsTab_ShowsStatusFilter", async () => {
    // Mock location to be on rooms tab
    jest.spyOn(require("react-router-dom"), "useLocation").mockReturnValue({
      pathname: "/Storage/rooms",
    });

    renderWithIntl(<StorageDashboard />);

    await waitFor(() => {
      expect(
        screen.getByText(/Storage Management Dashboard/i),
      ).toBeInTheDocument();
    });

    // Verify only status filter is visible
    const statusFilter = screen.getByTestId("status-filter");
    expect(statusFilter).toBeInTheDocument();

    // Verify location filter is NOT visible
    expect(
      screen.queryByTestId("location-filter-dropdown"),
    ).not.toBeInTheDocument();
  });

  /**
   * T062i3: Test Devices tab shows type, room, and status filters
   * Devices tab should have type, room, and status filters
   */
  test("testDevicesTab_ShowsTypeRoomStatusFilters", async () => {
    jest.spyOn(require("react-router-dom"), "useLocation").mockReturnValue({
      pathname: "/Storage/devices",
    });

    renderWithIntl(<StorageDashboard />);

    await waitFor(() => {
      expect(
        screen.getByText(/Storage Management Dashboard/i),
      ).toBeInTheDocument();
    });

    // Verify all three filters are visible
    expect(screen.getByTestId("type-filter")).toBeInTheDocument();
    expect(screen.getByTestId("room-filter")).toBeInTheDocument();
    expect(screen.getByTestId("status-filter")).toBeInTheDocument();
  });

  /**
   * T062i3: Test Shelves tab shows device, room, and status filters
   */
  test("testShelvesTab_ShowsDeviceRoomStatusFilters", async () => {
    jest.spyOn(require("react-router-dom"), "useLocation").mockReturnValue({
      pathname: "/Storage/shelves",
    });

    renderWithIntl(<StorageDashboard />);

    await waitFor(() => {
      expect(
        screen.getByText(/Storage Management Dashboard/i),
      ).toBeInTheDocument();
    });

    expect(screen.getByTestId("device-filter")).toBeInTheDocument();
    expect(screen.getByTestId("room-filter")).toBeInTheDocument();
    expect(screen.getByTestId("status-filter")).toBeInTheDocument();
  });

  /**
   * T062i3: Test Racks tab shows room, shelf, device, and status filters
   */
  test("testRacksTab_ShowsRoomShelfDeviceStatusFilters", async () => {
    jest.spyOn(require("react-router-dom"), "useLocation").mockReturnValue({
      pathname: "/Storage/racks",
    });

    renderWithIntl(<StorageDashboard />);

    await waitFor(() => {
      expect(
        screen.getByText(/Storage Management Dashboard/i),
      ).toBeInTheDocument();
    });

    expect(screen.getByTestId("room-filter")).toBeInTheDocument();
    expect(screen.getByTestId("shelf-filter")).toBeInTheDocument();
    expect(screen.getByTestId("device-filter")).toBeInTheDocument();
    expect(screen.getByTestId("status-filter")).toBeInTheDocument();
  });

  /**
   * T062i3: Test Racks tab displays room column
   * Racks table should include a "Room" column showing room name
   */
  test("testRacksTab_DisplaysRoomColumn", async () => {
    jest.spyOn(require("react-router-dom"), "useLocation").mockReturnValue({
      pathname: "/Storage/racks",
    });

    const mockRacks = [
      {
        id: "30",
        label: "Rack R1",
        roomId: "1",
        roomName: "Main Laboratory",
        shelfId: "20",
        deviceId: "10",
      },
    ];

    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/racks")) {
        callback(mockRacks);
      } else {
        getFromOpenElisServer.mockImplementation((url, callback) => {
          if (url.includes("/rest/storage/dashboard/location-counts")) {
            callback({ rooms: 1, devices: 1, shelves: 0, racks: 1 });
          }
        });
      }
    });

    renderWithIntl(<StorageDashboard />);

    await waitFor(() => {
      // Verify room column header exists
      expect(screen.getByText(/Room/i)).toBeInTheDocument();
      // Verify room name is displayed in table
      expect(screen.getByText("Main Laboratory")).toBeInTheDocument();
    });
  });

  /**
   * T062i3: Test Clear Filters resets all filters
   * Clear Filters button should reset all active filters
   */
  test("testClearFilters_ResetsAllFilters", async () => {
    renderWithIntl(<StorageDashboard />);

    await waitFor(() => {
      expect(
        screen.getByText(/Storage Management Dashboard/i),
      ).toBeInTheDocument();
    });

    // Set a filter
    const locationFilter = screen.getByTestId("location-filter-dropdown");
    fireEvent.change(locationFilter, { target: { value: "1" } });

    // Click Clear Filters button
    const clearButton = screen.getByText(/Clear Filters/i);
    fireEvent.click(clearButton);

    // Verify filter is reset
    await waitFor(() => {
      expect(locationFilter.value || locationFilter.textContent).toBe("");
    });
  });

  /**
   * T062i3: Test location filter uses downward inclusive filtering
   * Selecting a location should show all samples within that location's hierarchy
   */
  test("testLocationFilter_DownwardInclusive_ShowsAllSamplesInHierarchy", async () => {
    const mockSamplesInHierarchy = [
      {
        id: "sample-1",
        accessionNumber: "S-2025-001",
        location: "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1",
      },
      {
        id: "sample-2",
        accessionNumber: "S-2025-002",
        location: "Main Laboratory > Freezer Unit 1 > Shelf-B > Rack R2",
      },
    ];

    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (
        url.includes("/rest/storage/samples") &&
        url.includes("location_id=10")
      ) {
        // When filtering by device (id=10), return all samples in device and children
        callback(mockSamplesInHierarchy);
      } else if (url.includes("/rest/storage/samples")) {
        callback([]);
      }
    });

    renderWithIntl(<StorageDashboard />);

    await waitFor(() => {
      expect(
        screen.getByText(/Storage Management Dashboard/i),
      ).toBeInTheDocument();
    });

    // Select a device in location filter
    const locationFilter = screen.getByTestId("location-filter-dropdown");
    fireEvent.change(locationFilter, { target: { value: "10" } });

    // Verify API called with location_id and location_type parameters
    await waitFor(() => {
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        expect.stringContaining("location_id=10"),
        expect.any(Function),
        expect.any(Function),
      );
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        expect.stringContaining("location_type=device"),
        expect.any(Function),
        expect.any(Function),
      );
    });

    // Verify samples from device hierarchy are shown
    await waitFor(() => {
      expect(screen.getByText("S-2025-001")).toBeInTheDocument();
      expect(screen.getByText("S-2025-002")).toBeInTheDocument();
    });
  });
});
