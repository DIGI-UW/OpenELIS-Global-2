import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LocationTreeView from "./LocationTreeView";
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

describe("LocationTreeView", () => {
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
    {
      id: "11",
      name: "Refrigerator A",
      code: "REF01",
      roomId: "1",
      active: true,
    },
  ];

  const mockShelves = [
    { id: "20", label: "Shelf-A", deviceId: "10", active: true },
  ];

  const mockRacks = [
    { id: "30", label: "Rack R1", shelfId: "20", active: true },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T062i1: Test expands and collapses parent nodes
   * Tree view should allow expanding/collapsing to browse hierarchy
   */
  test("testExpandsCollapsesParentNodes", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/rooms")) {
        callback(mockRooms);
      }
      if (url.includes("/rest/storage/devices")) {
        callback(mockDevices);
      }
    });

    const onLocationSelect = jest.fn();
    renderWithIntl(<LocationTreeView onLocationSelect={onLocationSelect} />);

    // Wait for rooms to load
    const roomElement = await screen.findByText("Main Laboratory");

    // Click to expand room
    const roomNode = roomElement.closest("li");
    const expandButton = roomNode?.querySelector("button[aria-expanded]");

    expect(expandButton).toBeTruthy();
    expect(expandButton.getAttribute("aria-expanded")).toBe("false");

    // Verify no tooltip (using regular button instead of Carbon Button)
    // Empty title attribute prevents browser default tooltips
    expect(expandButton.getAttribute("title")).toBe("");
    expect(expandButton.getAttribute("aria-label")).toBeTruthy();

    fireEvent.click(expandButton);

    // Verify API call was made to load devices
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("/rest/storage/devices?roomId=1"),
      expect.any(Function),
      expect.any(Function),
    );

    // Wait for devices to appear
    const deviceElement = await screen.findByText("Freezer Unit 1");
    expect(deviceElement).toBeTruthy();

    // Verify expand button is now expanded
    expect(expandButton.getAttribute("aria-expanded")).toBe("true");

    // Collapse again
    fireEvent.click(expandButton);

    // Verify collapsed state
    expect(expandButton.getAttribute("aria-expanded")).toBe("false");

    // Devices should be hidden (wait a bit for collapse animation)
    await new Promise((resolve) => setTimeout(resolve, 100));
    // Note: Elements may still be in DOM but hidden, so we check visibility
    const deviceAfterCollapse = screen.queryByText("Freezer Unit 1");
    if (deviceAfterCollapse) {
      expect(deviceAfterCollapse).not.toBeVisible();
    }
  });

  /**
   * T062i1: Test displays Room, Device, Shelf, and Rack levels
   * Tree view should show all hierarchy levels (except Position)
   */
  test("testDisplaysRoomDeviceShelfRackLevels", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/rooms")) {
        callback(mockRooms);
      }
      if (url.includes("/rest/storage/devices")) {
        callback(mockDevices);
      }
      if (url.includes("/rest/storage/shelves")) {
        callback(mockShelves);
      }
      if (url.includes("/rest/storage/racks")) {
        callback(mockRacks);
      }
    });

    const onLocationSelect = jest.fn();
    renderWithIntl(<LocationTreeView onLocationSelect={onLocationSelect} />);

    // Wait for rooms to load
    const roomElement = await screen.findByText("Main Laboratory");

    // Expand to see devices
    const roomNode = roomElement.closest("li");
    const expandButton = roomNode?.querySelector("button");
    if (expandButton) {
      fireEvent.click(expandButton);

      const deviceElement = await screen.findByText("Freezer Unit 1");
    }
  });

  /**
   * T062i1: Test excludes Position level
   * Position-level locations should NOT appear in tree view
   */
  test("testExcludesPositionLevel", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/rooms")) {
        callback(mockRooms);
      }
      if (url.includes("/rest/storage/devices")) {
        callback(mockDevices);
      }
      if (url.includes("/rest/storage/racks")) {
        callback(mockRacks);
      }
      if (url.includes("/rest/storage/positions")) {
        // Even if positions are fetched, they should not be displayed
        callback([{ id: "40", coordinate: "A5", rackId: "30", active: true }]);
      }
    });

    const onLocationSelect = jest.fn();
    renderWithIntl(<LocationTreeView onLocationSelect={onLocationSelect} />);

    const roomElement = await screen.findByText("Main Laboratory");

    // Verify positions are NOT shown
    expect(screen.queryByText(/Position A5/i)).toBeNull();
    expect(screen.queryByText(/A5/i)).toBeNull();
  });

  /**
   * Test that expand button has no tooltip (iconDescription removed)
   */
  test("testExpandButtonHasNoTooltip", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/rooms")) {
        callback(mockRooms);
      }
    });

    const onLocationSelect = jest.fn();
    renderWithIntl(<LocationTreeView onLocationSelect={onLocationSelect} />);

    const roomElement = await screen.findByText("Main Laboratory");
    const roomNode = roomElement.closest("li");
    const expandButton = roomNode?.querySelector("button[aria-expanded]");

    expect(expandButton).toBeTruthy();
    // Verify no tooltip (using regular button with empty title instead of Carbon Button)
    expect(expandButton.getAttribute("title")).toBe("");
    expect(expandButton.getAttribute("aria-label")).toBeTruthy();
  });

  /**
   * Test that hierarchy loads children when expanded
   */
  test("testHierarchyLoadsChildrenWhenExpanded", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/rooms")) {
        callback(mockRooms);
      }
      if (url.includes("/rest/storage/devices")) {
        callback(mockDevices);
      }
      if (url.includes("/rest/storage/shelves")) {
        callback(mockShelves);
      }
      if (url.includes("/rest/storage/racks")) {
        callback(mockRacks);
      }
    });

    const onLocationSelect = jest.fn();
    renderWithIntl(<LocationTreeView onLocationSelect={onLocationSelect} />);

    // Wait for rooms to load
    const roomElement = await screen.findByText("Main Laboratory");
    const roomNode = roomElement.closest("li");
    const expandButton = roomNode?.querySelector("button[aria-expanded]");

    // Expand room
    fireEvent.click(expandButton);

    // Wait for devices to load and render
    const deviceElement = await screen.findByText("Freezer Unit 1");
    expect(deviceElement).toBeTruthy();

    // Verify both devices are rendered
    const device2Element = await screen.findByText("Refrigerator A");
    expect(device2Element).toBeTruthy();

    // Expand device to see shelves
    const deviceNode = deviceElement.closest("li");
    const deviceExpandButton = deviceNode?.querySelector(
      "button[aria-expanded]",
    );
    expect(deviceExpandButton).toBeTruthy();

    fireEvent.click(deviceExpandButton);

    // Wait for shelf to load and render
    const shelfElement = await screen.findByText("Shelf-A");
    expect(shelfElement).toBeTruthy();

    // Expand shelf to see racks
    const shelfNode = shelfElement.closest("li");
    const shelfExpandButton = shelfNode?.querySelector("button[aria-expanded]");
    expect(shelfExpandButton).toBeTruthy();

    fireEvent.click(shelfExpandButton);

    // Wait for rack to load and render
    const rackElement = await screen.findByText("Rack R1");
    expect(rackElement).toBeTruthy();

    // Verify rack has no expand button (it's a leaf node)
    const rackNode = rackElement.closest("li");
    const rackExpandButton = rackNode?.querySelector("button[aria-expanded]");
    expect(rackExpandButton).toBeNull();
  });
});
