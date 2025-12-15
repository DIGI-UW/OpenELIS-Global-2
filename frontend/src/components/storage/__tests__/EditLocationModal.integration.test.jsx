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
 * This test WILL FAIL currently - proves bug B2/B5
 *
 * Bug: Modal not reading parentRoomName/roomName from API response
 * Expected: Parent room name should display in read-only field
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
      parentRoomName: "Main Laboratory", // This should display
      type: "freezer",
      active: true,
      temperatureSetting: -20,
      capacityLimit: 100,
    };

    // Mock API call when modal opens
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce({
      ...deviceWithParent,
      // API might return nested object OR flat field
      parentRoom: { id: "ROOM-001", name: "Main Laboratory" },
    });

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

    // Assert: Parent room name should be visible in read-only field
    // This WILL FAIL currently - proves bug B2/B5
    await waitFor(() => {
      const parentField = screen.getByTestId(
        "edit-location-device-parent-room",
      );
      const inputElement = parentField.querySelector("input") || parentField;
      expect(inputElement.value).toBe("Main Laboratory");
    });
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

    // Mock API call returns flat field
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(deviceWithFlatParent);

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

    // Assert: Should display parent room name from flat field
    // This WILL FAIL currently - proves bug B2/B5
    await waitFor(() => {
      const parentField = screen.getByTestId(
        "edit-location-device-parent-room",
      );
      const inputElement = parentField.querySelector("input") || parentField;
      expect(inputElement.value).toBe("Secondary Lab");
    });
  });
});
