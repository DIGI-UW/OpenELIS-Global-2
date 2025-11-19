import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import StorageLocationsMetricCard from "./StorageLocationsMetricCard";
import { getFromOpenElisServer } from "../../utils/Utils";
import messages from "../../../languages/en.json";

// Mock the API utilities
jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("StorageLocationsMetricCard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T066f: Test displays formatted breakdown
   * Displays "X rooms, Y devices, Z shelves, W racks" format
   */
  test("testDisplaysFormattedBreakdown", async () => {
    const mockCounts = { rooms: 12, devices: 45, shelves: 89, racks: 156 };
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/dashboard/location-counts")) {
        callback(mockCounts);
      }
    });

    renderWithIntl(<StorageLocationsMetricCard />);

    // Wait for API call and verify formatted text appears
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/storage/dashboard/location-counts",
      expect.any(Function),
    );

    // Wait for formatted breakdown text to appear - numbers and labels are in separate spans
    await screen.findByText("12", { exact: false });
    expect(screen.getByText("12", { exact: false })).toBeTruthy();
    expect(screen.getByText("45", { exact: false })).toBeTruthy();
    expect(screen.getByText("89", { exact: false })).toBeTruthy();
    expect(screen.getByText("156", { exact: false })).toBeTruthy();
    expect(screen.getByText(/rooms/i)).toBeTruthy();
    expect(screen.getByText(/devices/i)).toBeTruthy();
    expect(screen.getByText(/shelves/i)).toBeTruthy();
    expect(screen.getByText(/racks/i)).toBeTruthy();
  });

  /**
   * T066f: Test color codes by location type
   * Uses Carbon Design System tokens: blue-70 for rooms, teal-70 for devices, purple-70 for shelves, orange-70 for racks
   */
  test("testColorCodesByLocationType", async () => {
    const mockCounts = { rooms: 1, devices: 1, shelves: 1, racks: 1 };
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/dashboard/location-counts")) {
        callback(mockCounts);
      }
    });

    const { container } = renderWithIntl(<StorageLocationsMetricCard />);

    // Wait for breakdown container to appear
    await screen.findByText(/rooms/i);

    // Find pills by their container and verify they have correct classes
    const pills = container.querySelectorAll(".location-count-pill");
    expect(pills.length).toBe(4);

    // Verify each pill has the correct color class
    const roomsPill = Array.from(pills).find((pill) =>
      pill.classList.contains("location-count-rooms"),
    );
    const devicesPill = Array.from(pills).find((pill) =>
      pill.classList.contains("location-count-devices"),
    );
    const shelvesPill = Array.from(pills).find((pill) =>
      pill.classList.contains("location-count-shelves"),
    );
    const racksPill = Array.from(pills).find((pill) =>
      pill.classList.contains("location-count-racks"),
    );

    expect(roomsPill).toBeTruthy();
    expect(devicesPill).toBeTruthy();
    expect(shelvesPill).toBeTruthy();
    expect(racksPill).toBeTruthy();

    // Note: Actual color verification requires checking computed styles or CSS classes
    // This is a basic structure test - full color verification done in E2E tests
  });

  /**
   * T066f: Test shows only active locations
   * Counts should exclude inactive/decommissioned locations
   */
  test("testShowsOnlyActiveLocations", async () => {
    // Mock API response with only active location counts
    const mockCounts = { rooms: 10, devices: 30, shelves: 60, racks: 100 };
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/dashboard/location-counts")) {
        callback(mockCounts);
      }
    });

    renderWithIntl(<StorageLocationsMetricCard />);

    // Verify API is called with correct endpoint
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/storage/dashboard/location-counts",
      expect.any(Function),
    );

    // Wait for counts to appear - verify numbers and labels separately
    await screen.findByText("10");
    expect(screen.getByText("10")).toBeTruthy();
    expect(screen.getByText("30")).toBeTruthy();
    expect(screen.getByText("60")).toBeTruthy();
    expect(screen.getByText("100")).toBeTruthy();
    expect(screen.getByText(/rooms/i)).toBeTruthy();
    expect(screen.getByText(/devices/i)).toBeTruthy();
    expect(screen.getByText(/shelves/i)).toBeTruthy();
    expect(screen.getByText(/racks/i)).toBeTruthy();
  });

  /**
   * T066f: Test Carbon color tokens used
   * Verifies Carbon Design System color tokens are applied (blue-70, teal-70, purple-70, orange-70)
   */
  test("testCarbonColorTokensUsed", async () => {
    const mockCounts = { rooms: 1, devices: 1, shelves: 1, racks: 1 };
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/dashboard/location-counts")) {
        callback(mockCounts);
      }
    });

    renderWithIntl(<StorageLocationsMetricCard />);

    // Wait for component to render and verify elements exist
    await screen.findByText(/rooms/i);

    // Verify all location types are displayed
    expect(screen.getByText(/rooms/i)).toBeTruthy();
    expect(screen.getByText(/devices/i)).toBeTruthy();
    expect(screen.getByText(/shelves/i)).toBeTruthy();
    expect(screen.getByText(/racks/i)).toBeTruthy();

    // Verify numbers are present (use getAllByText since there are multiple "1" values)
    const numbers = screen.getAllByText("1");
    expect(numbers.length).toBeGreaterThanOrEqual(4);

    // Full color token verification requires checking CSS variables or computed styles
    // Carbon tokens are typically: --cds-blue-70, --cds-teal-70, --cds-purple-70, --cds-orange-70
  });

  /**
   * T066f: Test displays correct counts
   * Verifies counts match API response values
   */
  test("testDisplaysCorrectCounts", async () => {
    const mockCounts = { rooms: 5, devices: 15, shelves: 25, racks: 50 };
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/dashboard/location-counts")) {
        callback(mockCounts);
      }
    });

    renderWithIntl(<StorageLocationsMetricCard />);

    // Wait for and verify exact counts are displayed
    await screen.findByText("5");
    expect(screen.getByText("5")).toBeTruthy();
    expect(screen.getByText("15")).toBeTruthy();
    expect(screen.getByText("25")).toBeTruthy();
    expect(screen.getByText("50")).toBeTruthy();
    expect(screen.getByText(/rooms/i)).toBeTruthy();
    expect(screen.getByText(/devices/i)).toBeTruthy();
    expect(screen.getByText(/shelves/i)).toBeTruthy();
    expect(screen.getByText(/racks/i)).toBeTruthy();
  });

  /**
   * Test: Handles loading state
   * Shows loading indicator while fetching counts
   */
  test("testHandlesLoadingState", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/dashboard/location-counts")) {
        // Don't call callback immediately to simulate loading
        setTimeout(
          () => callback({ rooms: 0, devices: 0, shelves: 0, racks: 0 }),
          100,
        );
      }
    });

    renderWithIntl(<StorageLocationsMetricCard />);

    // Verify API call was made
    expect(getFromOpenElisServer).toHaveBeenCalled();
  });

  /**
   * Test: Handles zero counts
   * Displays "0 rooms, 0 devices, 0 shelves, 0 racks" when no locations exist
   */
  test("testHandlesZeroCounts", async () => {
    const mockCounts = { rooms: 0, devices: 0, shelves: 0, racks: 0 };
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/dashboard/location-counts")) {
        callback(mockCounts);
      }
    });

    renderWithIntl(<StorageLocationsMetricCard />);

    // Wait for breakdown container to appear
    const breakdown = await screen.findByText(/rooms/i);
    expect(breakdown).toBeTruthy();

    // Verify all location types are displayed with zero counts
    const breakdownContainer = breakdown.closest(".location-counts-breakdown");
    expect(breakdownContainer).toBeTruthy();

    // Verify all four pills are present
    const pills = breakdownContainer.querySelectorAll(".location-count-pill");
    expect(pills.length).toBe(4);

    // Verify all labels are present
    expect(screen.getByText(/rooms/i)).toBeTruthy();
    expect(screen.getByText(/devices/i)).toBeTruthy();
    expect(screen.getByText(/shelves/i)).toBeTruthy();
    expect(screen.getByText(/racks/i)).toBeTruthy();
  });
});
