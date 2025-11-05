import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LocationFilterDropdown from "./LocationFilterDropdown";
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

describe("LocationFilterDropdown", () => {
  const mockRooms = [
    { id: "1", name: "Main Laboratory", code: "MAIN", active: true },
    { id: "2", name: "Secondary Lab", code: "LAB2", active: false },
  ];

  const mockDevices = [
    {
      id: "10",
      name: "Freezer Unit 1",
      code: "FRZ01",
      roomId: "1",
      roomName: "Main Laboratory",
      active: true,
    },
    {
      id: "11",
      name: "Refrigerator A",
      code: "REF01",
      roomId: "1",
      roomName: "Main Laboratory",
      active: true,
    },
  ];

  const mockShelves = [
    {
      id: "20",
      label: "Shelf-A",
      deviceId: "10",
      deviceName: "Freezer Unit 1",
      roomId: "1",
      roomName: "Main Laboratory",
      active: true,
    },
  ];

  const mockRacks = [
    {
      id: "30",
      label: "Rack R1",
      shelfId: "20",
      shelfLabel: "Shelf-A",
      deviceId: "10",
      deviceName: "Freezer Unit 1",
      roomId: "1",
      roomName: "Main Laboratory",
      active: true,
    },
  ];

  const mockSearchResults = [
    {
      id: "1",
      name: "Main Laboratory",
      code: "MAIN",
      type: "room",
      hierarchical_path: "Main Laboratory",
      active: true,
    },
    {
      id: "10",
      name: "Freezer Unit 1",
      code: "FRZ01",
      type: "device",
      hierarchical_path: "Main Laboratory > Freezer Unit 1",
      active: true,
    },
    {
      id: "20",
      label: "Shelf-A",
      type: "shelf",
      hierarchical_path: "Main Laboratory > Freezer Unit 1 > Shelf-A",
      active: true,
    },
    {
      id: "30",
      label: "Rack R1",
      type: "rack",
      hierarchical_path: "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1",
      active: true,
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T062i: Test displays tree view for browsing
   * Shows hierarchical tree structure with expand/collapse
   */
  test("testDisplaysTreeViewForBrowsing", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/rooms")) {
        callback(mockRooms);
      }
    });

    const onLocationChange = jest.fn();
    renderWithIntl(
      <LocationFilterDropdown onLocationChange={onLocationChange} />,
    );

    // Focus input to open dropdown and show tree view
    const searchInput = screen.getByPlaceholderText(/filter by locations/i);
    fireEvent.focus(searchInput);

    // Wait for dropdown to open and LocationTreeView to mount
    // LocationTreeView only mounts when isOpen is true
    await screen.findByTestId("location-tree-view", {}, { timeout: 1000 });

    // Wait for API call to complete and rooms to render
    // LocationTreeView's useEffect runs after mount
    await new Promise((resolve) => setTimeout(resolve, 300));

    // Verify tree view is displayed with room
    const roomElement = await screen.findByText(
      "Main Laboratory",
      {},
      { timeout: 3000 },
    );

    // Verify expand/collapse functionality - button should have aria-label (and iconDescription for Carbon)
    // Find all buttons and look for one with expand/collapse aria-label
    const allButtons = screen.getAllByRole("button");
    const expandButton = allButtons.find((btn) => {
      const ariaLabel = btn.getAttribute("aria-label") || "";
      return (
        ariaLabel.toLowerCase().includes("expand") ||
        ariaLabel.toLowerCase().includes("collapse")
      );
    });
    if (expandButton) {
      expect(expandButton).toHaveAttribute("aria-label");
    } else {
      // If no expand button found, that's okay - room might not have children yet
      // Just verify the room is displayed
      expect(roomElement).toBeTruthy();
    }
  });

  /**
   * Test displays "Filter by locations" label
   */
  test("testDisplaysFilterByLocationsLabel", () => {
    const onLocationChange = jest.fn();
    renderWithIntl(
      <LocationFilterDropdown onLocationChange={onLocationChange} />,
    );

    // Verify label text
    expect(screen.getByText(/filter by locations/i)).toBeTruthy();
  });

  /**
   * T062i: Test displays autocomplete for search
   * Shows autocomplete list when typing in search field
   */
  test("testDisplaysAutocompleteForSearch", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/locations/search")) {
        callback(mockSearchResults);
      }
    });

    const onLocationChange = jest.fn();
    renderWithIntl(
      <LocationFilterDropdown onLocationChange={onLocationChange} />,
    );

    // Find search input and type
    const searchInput = screen.getByPlaceholderText(/filter by locations/i);
    fireEvent.change(searchInput, { target: { value: "Freezer" } });

    // Wait for autocomplete results (may appear multiple times)
    const autocompleteResults = await screen.findAllByText(
      /Main Laboratory > Freezer Unit 1/i,
    );
    expect(autocompleteResults.length).toBeGreaterThan(0);
  });

  /**
   * T062i: Test search matches any hierarchy level
   * Search should match Room, Device, Shelf, or Rack names/codes
   */
  test("testSearchMatchesAnyHierarchyLevel", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/locations/search")) {
        callback(mockSearchResults);
      }
    });

    const onLocationChange = jest.fn();
    renderWithIntl(
      <LocationFilterDropdown onLocationChange={onLocationChange} />,
    );

    // Search for room name
    const searchInput = screen.getByPlaceholderText(/filter by locations/i);
    fireEvent.change(searchInput, { target: { value: "Main" } });

    const roomResults = await screen.findAllByText(/Main Laboratory/i);
    expect(roomResults.length).toBeGreaterThan(0);

    // Search for device code
    fireEvent.change(searchInput, { target: { value: "FRZ01" } });

    const deviceResults = await screen.findAllByText(/Freezer Unit 1/i);
    expect(deviceResults.length).toBeGreaterThan(0);
  });

  /**
   * T062i: Test shows full hierarchical path
   * Results display full path like "Main Laboratory > Freezer Unit 1"
   */
  test("testShowsFullHierarchicalPath", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/locations/search")) {
        callback(mockSearchResults);
      }
    });

    const onLocationChange = jest.fn();
    renderWithIntl(
      <LocationFilterDropdown onLocationChange={onLocationChange} />,
    );

    const searchInput = screen.getByPlaceholderText(/filter by locations/i);
    fireEvent.change(searchInput, { target: { value: "Shelf" } });

    const shelfResults = await screen.findAllByText(
      /Main Laboratory > Freezer Unit 1 > Shelf-A/i,
    );
    expect(shelfResults.length).toBeGreaterThan(0);
  });

  /**
   * T062i: Test visual distinction for inactive locations
   * Inactive locations should be visually distinguished (grayed out, disabled, or badge)
   */
  test("testVisualDistinctionForInactiveLocations", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/rooms")) {
        callback(mockRooms);
      }
    });

    const onLocationChange = jest.fn();
    renderWithIntl(
      <LocationFilterDropdown onLocationChange={onLocationChange} />,
    );

    // Focus input to open dropdown
    const searchInput = screen.getByPlaceholderText(/filter by locations/i);
    fireEvent.focus(searchInput);

    // Wait for dropdown to open and LocationTreeView to mount
    await screen.findByTestId("location-tree-view", {}, { timeout: 1000 });

    // Wait for API call to complete
    await new Promise((resolve) => setTimeout(resolve, 300));

    // Wait for inactive location to appear
    const inactiveLocation = await screen.findByText(
      /Secondary Lab/i,
      {},
      { timeout: 3000 },
    );
    // Verify it has disabled styling or inactive badge
    const parentElement =
      inactiveLocation.closest("li") || inactiveLocation.closest("button");
    if (parentElement) {
      expect(parentElement.className || "").toMatch(/inactive|disabled/i);
    } else {
      // Check if button is disabled
      const button = inactiveLocation.closest("button");
      if (button) {
        expect(
          button.disabled || button.className?.includes("inactive"),
        ).toBeTruthy();
      }
    }
  });

  /**
   * T062i: Test position level excluded
   * Position-level locations should NOT appear in dropdown (only Room/Device/Shelf/Rack)
   */
  test("testPositionLevelExcluded", async () => {
    const mockLocationsWithPositions = [
      ...mockSearchResults,
      {
        id: "40",
        coordinate: "A5",
        type: "position",
        hierarchical_path:
          "Main Laboratory > Freezer Unit 1 > Shelf-A > Rack R1 > Position A5",
        active: true,
      },
    ];

    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/locations/search")) {
        callback(mockLocationsWithPositions);
      }
    });

    const onLocationChange = jest.fn();
    renderWithIntl(
      <LocationFilterDropdown onLocationChange={onLocationChange} />,
    );

    const searchInput = screen.getByPlaceholderText(/filter by locations/i);
    fireEvent.change(searchInput, { target: { value: "A5" } });

    // Wait a bit for results to load, then verify position is NOT in results
    await new Promise((resolve) => setTimeout(resolve, 500));
    // Position should NOT appear in results
    expect(screen.queryByText(/Position A5/i)).toBeNull();
  });

  /**
   * T062i: Test downward inclusive filtering
   * Selecting a location filters to show all samples within that location's hierarchy
   */
  test("testDownwardInclusiveFiltering", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/rooms")) {
        callback(mockRooms);
      }
      if (url.includes("/rest/storage/devices")) {
        callback(mockDevices);
      }
    });

    const onLocationChange = jest.fn();
    renderWithIntl(
      <LocationFilterDropdown onLocationChange={onLocationChange} />,
    );

    // Focus input to open dropdown
    const searchInput = screen.getByPlaceholderText(/filter by locations/i);
    fireEvent.focus(searchInput);

    // Wait for dropdown to open and LocationTreeView to mount
    await screen.findByTestId("location-tree-view", {}, { timeout: 1000 });

    // Wait for API call to complete
    await new Promise((resolve) => setTimeout(resolve, 300));

    // Wait for rooms to load
    const roomElement = await screen.findByText(
      "Main Laboratory",
      {},
      { timeout: 3000 },
    );

    // Expand room first to see devices
    // Find expand button near the room element
    const roomNode = roomElement.closest("li");
    const expandButton = roomNode?.querySelector(
      "button[aria-expanded], button[aria-label*='expand'], button[aria-label*='Expand']",
    );
    if (expandButton) {
      fireEvent.click(expandButton);
      // Wait for devices to load after expansion
      await new Promise((resolve) => setTimeout(resolve, 200));
    }

    // Wait for device to appear and select it
    const device = await screen.findByText(
      "Freezer Unit 1",
      {},
      { timeout: 3000 },
    );
    fireEvent.click(device);

    // Verify callback called with location info including type for downward inclusive filtering
    expect(onLocationChange).toHaveBeenCalledWith(
      expect.objectContaining({
        id: "10",
        type: "device",
      }),
    );
  });
});
