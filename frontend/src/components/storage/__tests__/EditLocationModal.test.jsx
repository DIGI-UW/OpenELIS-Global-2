import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import EditLocationModal from "../LocationManagement/EditLocationModal";
import messages from "../../../languages/en.json";
import * as Utils from "../../utils/Utils";

// Mock the API utilities
jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServer: jest.fn(),
  putToOpenElisServer: jest.fn(),
}));

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("EditLocationModal", () => {
  const mockRoom = {
    id: "1",
    name: "Main Laboratory",
    code: "MAIN-LAB",
    description: "Primary lab room",
    active: true,
    type: "room",
  };

  const mockDevice = {
    id: "2",
    name: "Freezer Unit 1",
    code: "FRZ-001",
    type: "freezer",
    temperatureSetting: -20,
    capacityLimit: 100,
    active: true,
    parentRoom: { id: "1", name: "Main Laboratory" },
    type: "device",
  };

  const mockOnClose = jest.fn();
  const mockOnSave = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T106: Test renders modal with Room fields
   */
  test("testEditModal_RendersForRoom", () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Verify Room-specific fields are present
    expect(screen.getByLabelText(/name/i)).toBeTruthy();
    expect(screen.getByLabelText(/code/i)).toBeTruthy();
    expect(screen.getByLabelText(/description/i)).toBeTruthy();
    expect(screen.getByLabelText(/active/i)).toBeTruthy();
  });

  /**
   * T106: Test renders modal with Device fields
   */
  test("testEditModal_RendersForDevice", () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockDevice}
        locationType="device"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Verify Device-specific fields are present
    expect(screen.getByLabelText(/name/i)).toBeTruthy();
    expect(screen.getByLabelText(/code/i)).toBeTruthy();
    // Check for type dropdown - Carbon Dropdown may render multiple "Type" texts, so check if any exists
    const typeElements = screen.queryAllByText(/type/i);
    expect(typeElements.length).toBeGreaterThan(0);
    expect(screen.getByLabelText(/temperature/i)).toBeTruthy();
    expect(screen.getByLabelText(/capacity/i)).toBeTruthy();
  });

  /**
   * T106: Test code field is read-only (disabled)
   */
  test("testEditModal_CodeFieldReadOnly", () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    const codeField = screen.getByTestId("edit-location-room-code");
    expect(codeField).toBeTruthy();
    // Carbon TextInput with disabled/readOnly may set it on the input element
    const inputElement = codeField.querySelector("input") || codeField;
    expect(inputElement.disabled || inputElement.readOnly).toBe(true);
    expect(inputElement.value || codeField.value).toBe(mockRoom.code);
  });

  /**
   * T106: Test parent field is read-only (disabled)
   */
  test("testEditModal_ParentFieldReadOnly", () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockDevice}
        locationType="device"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Parent field should be present and disabled
    const parentField = screen.getByTestId("edit-location-device-parent-room");
    expect(parentField).toBeTruthy();
    // Carbon TextInput with disabled may set it on the input element
    const inputElement = parentField.querySelector("input") || parentField;
    expect(inputElement.disabled || inputElement.readOnly).toBe(true);
  });

  /**
   * T106: Test editable fields are enabled (name, description, status)
   */
  test("testEditModal_EditableFieldsEnabled", () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    const nameField = screen.getByTestId("edit-location-room-name");
    const descriptionField = screen.getByTestId("edit-location-room-description");

    expect(nameField.disabled).toBe(false);
    expect(descriptionField.disabled).toBe(false);
  });

  /**
   * T106: Test displays validation errors for duplicate code
   */
  test("testEditModal_ValidationErrors", async () => {
    const { putToOpenElisServer } = require("../../utils/Utils");
    // Mock putToOpenElisServer to call callback with error status
    putToOpenElisServer.mockImplementation((endpoint, payload, callback) => {
      setTimeout(() => {
        callback(400); // Error status
      }, 0);
    });

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    const nameField = screen.getByTestId("edit-location-room-name");
    fireEvent.change(nameField, { target: { value: "Updated Name" } });

    const saveButton = screen.getByTestId("edit-location-save-button");
    fireEvent.click(saveButton);

    // Wait a bit for error to appear
    await new Promise((resolve) => setTimeout(resolve, 100));
    
    // Check that error message appears (component should show error)
    const errorElement = screen.queryByText(/failed to update/i);
    expect(errorElement || screen.queryByText(/error/i)).toBeTruthy();
  });

  /**
   * T106: Test save button calls PUT endpoint
   */
  test("testEditModal_SaveCallsAPI", async () => {
    const { putToOpenElisServer } = require("../../utils/Utils");
    // Mock putToOpenElisServer to call callback with 200 status
    putToOpenElisServer.mockImplementation((endpoint, payload, callback) => {
      // Simulate successful PUT request
      setTimeout(() => {
        callback(200);
      }, 0);
    });

    // Mock fetch for getting updated location
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ ...mockRoom, name: "Updated Name" }),
      }),
    );

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    const nameField = screen.getByTestId("edit-location-room-name");
    fireEvent.change(nameField, { target: { value: "Updated Name" } });

    const saveButton = screen.getByTestId("edit-location-save-button");
    fireEvent.click(saveButton);

    // Wait a bit for async operations
    await new Promise((resolve) => setTimeout(resolve, 100));
    
    expect(putToOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("/rest/storage/rooms/1"),
      expect.stringContaining("Updated Name"),
      expect.any(Function),
    );
    
    // Wait for onSave to be called
    await new Promise((resolve) => setTimeout(resolve, 200));
    expect(mockOnSave).toHaveBeenCalled();
  });

  /**
   * T106: Test cancel button closes modal without saving
   */
  test("testEditModal_CancelClosesModal", () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    const cancelButton = screen.getByTestId("edit-location-cancel-button");
    fireEvent.click(cancelButton);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
    expect(mockOnSave).not.toHaveBeenCalled();
  });
});

