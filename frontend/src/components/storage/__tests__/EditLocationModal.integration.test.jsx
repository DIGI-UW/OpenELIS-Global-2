import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import EditLocationModal from "../LocationManagement/EditLocationModal";
import messages from "../../../languages/en.json";

// Mock utilities BEFORE imports (Jest hoisting)
jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServer: jest.fn(),
  putToOpenElisServer: jest.fn(),
  getFromOpenElisServerV2: jest.fn(),
}));

import * as Utils from "../../utils/Utils";

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );
};

/**
 * Integration test for parent data flow from dashboard to modal
 * Tests that parent room data is correctly loaded and displayed in the Dropdown
 */
describe("EditLocationModal Integration - Parent Data Flow", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("device edit modal displays parent room name from location prop", async () => {
    // Arrange: Device with parent room data (as returned by API)
    const deviceWithParent = {
      id: "DEV-001",
      name: "Freezer 1",
      code: "FRZ01",
      parentRoomId: "ROOM-001",
      parentRoomName: "Main Laboratory",
      type: "freezer",
      active: true,
      temperatureSetting: -20,
      capacityLimit: 100,
    };

    const mockRooms = [
      { id: "ROOM-001", name: "Main Laboratory", active: true },
      { id: "ROOM-002", name: "Secondary Lab", active: true },
    ];

    // Mock API calls: first for full device data, then for rooms list
    // Component calls: 1) /rest/storage/devices/{id}, 2) /rest/storage/rooms
    Utils.getFromOpenElisServerV2
      .mockResolvedValueOnce({
        ...deviceWithParent,
        // API might return nested object OR flat field
        parentRoom: { id: "ROOM-001", name: "Main Laboratory" },
        parentRoomId: "ROOM-001",
      }) // Full location fetch (called FIRST)
      .mockResolvedValueOnce(mockRooms); // Rooms list for dropdown (called SECOND)

    // Act: Render modal with device data
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={deviceWithParent}
        locationType="device"
        onClose={jest.fn()}
        onSave={jest.fn()}
      />,
    );

    // Assert: Wait for room name to appear in dropdown (indicates both API calls completed)
    await screen.findByText("Main Laboratory");
  });

  test("device edit modal displays parent room name when API returns flat field", async () => {
    // Arrange: API returns parentRoomName as flat field (not nested)
    const deviceWithFlatParent = {
      id: "DEV-002",
      name: "Refrigerator 1",
      code: "REF01",
      parentRoomId: "ROOM-002",
      parentRoomName: "Secondary Lab", // Flat field from API
      type: "refrigerator",
      active: true,
    };

    const mockRooms = [
      { id: "ROOM-001", name: "Main Laboratory", active: true },
      { id: "ROOM-002", name: "Secondary Lab", active: true },
    ];

    // Mock API calls: first for full device data, then for rooms list
    // Component calls: 1) /rest/storage/devices/{id}, 2) /rest/storage/rooms
    Utils.getFromOpenElisServerV2
      .mockResolvedValueOnce({
        ...deviceWithFlatParent,
        parentRoomId: "ROOM-002",
      }) // Full location fetch (called FIRST)
      .mockResolvedValueOnce(mockRooms); // Rooms list for dropdown (called SECOND)

    // Act
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={deviceWithFlatParent}
        locationType="device"
        onClose={jest.fn()}
        onSave={jest.fn()}
      />,
    );

    // Assert: Wait for room name to appear in dropdown (indicates both API calls completed)
    await screen.findByText("Secondary Lab");
  });
});
