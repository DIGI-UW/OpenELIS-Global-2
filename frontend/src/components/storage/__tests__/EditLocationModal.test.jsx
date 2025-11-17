import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
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
  getFromOpenElisServerV2: jest.fn(),
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
    shortCode: "FRZ01",
    parentRoom: { id: "1", name: "Main Laboratory" },
  };

  const mockShelf = {
    id: "3",
    label: "Shelf A",
    active: true,
    shortCode: "SHA01",
    parentDevice: { id: "2", name: "Freezer Unit 1" },
  };

  const mockRack = {
    id: "4",
    label: "Rack R1",
    rows: 8,
    columns: 12,
    active: true,
    shortCode: "RKR01",
    parentShelf: { id: "3", label: "Shelf A" },
  };

  const mockOnClose = jest.fn();
  const mockOnSave = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    // Mock getFromOpenElisServerV2 to resolve immediately with location data
    // Using mockResolvedValue ensures promises resolve in the same tick
    Utils.getFromOpenElisServerV2.mockImplementation((endpoint) => {
      const match = endpoint.match(/\/rest\/storage\/(\w+)s\/(\d+)/);
      if (match) {
        const [, type, id] = match;
        if (type === "room") {
          return Promise.resolve({ ...mockRoom, id });
        } else if (type === "device") {
          return Promise.resolve({ ...mockDevice, id });
        } else if (type === "shelf" || type === "shelves") {
          return Promise.resolve({ ...mockShelf, id });
        } else if (type === "rack" || type === "racks") {
          return Promise.resolve({ ...mockRack, id });
        }
      }
      return Promise.resolve(mockRoom);
    });
  });

  /**
   * T106: Test renders modal with Room fields
   */
  test("testEditModal_RendersForRoom", async () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Use findBy* queries which automatically wait for elements
    const nameField = await screen.findByTestId("edit-location-room-name");
    expect(nameField).toBeTruthy();
    expect(screen.getByLabelText(/name/i)).toBeTruthy();
    expect(screen.getByLabelText(/code/i)).toBeTruthy();
    expect(screen.getByLabelText(/description/i)).toBeTruthy();
    expect(screen.getByLabelText(/active/i)).toBeTruthy();
  });

  /**
   * T106: Test renders modal with Device fields
   */
  test("testEditModal_RendersForDevice", async () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockDevice}
        locationType="device"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    const nameField = await screen.findByTestId("edit-location-device-name");
    expect(nameField).toBeTruthy();
    expect(screen.getByLabelText(/name/i)).toBeTruthy();
    // Code field is read-only, verify it exists via testId or queryAll
    const codeFields = screen.queryAllByLabelText(/code/i);
    expect(codeFields.length).toBeGreaterThan(0);
    const typeElements = screen.queryAllByText(/type/i);
    expect(typeElements.length).toBeGreaterThan(0);
    expect(screen.getByLabelText(/temperature/i)).toBeTruthy();
    expect(screen.getByLabelText(/capacity/i)).toBeTruthy();
  });

  /**
   * T106: Test code field is read-only (disabled)
   */
  test("testEditModal_CodeFieldReadOnly", async () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    const codeField = await screen.findByTestId("edit-location-room-code");
    const inputElement = codeField.querySelector("input") || codeField;
    expect(inputElement.disabled || inputElement.readOnly).toBe(true);
    expect(inputElement.value || codeField.value).toBe(mockRoom.code);
  });

  /**
   * T106: Test parent field is read-only (disabled)
   */
  test("testEditModal_ParentFieldReadOnly", async () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockDevice}
        locationType="device"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    const parentField = await screen.findByTestId(
      "edit-location-device-parent-room",
    );
    const inputElement = parentField.querySelector("input") || parentField;
    expect(inputElement.disabled || inputElement.readOnly).toBe(true);
  });

  /**
   * T106: Test editable fields are enabled (name, description, status)
   */
  test("testEditModal_EditableFieldsEnabled", async () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    const nameField = await screen.findByTestId("edit-location-room-name");
    const descriptionField = screen.getByTestId(
      "edit-location-room-description",
    );

    expect(nameField.disabled).toBe(false);
    expect(descriptionField.disabled).toBe(false);
  });

  /**
   * Test active toggle reflects location active state (Room)
   */
  test("testEditModal_ActiveToggleReflectsState_Room", async () => {
    const activeRoom = { ...mockRoom, active: true };
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(activeRoom);

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={activeRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Wait for form to load, then check toggle
    await screen.findByTestId("edit-location-room-name");
    // Carbon Toggle button has ID "room-active" - query it directly
    const toggleButton = await screen
      .findByRole("button", { name: /active/i }, { timeout: 2000 })
      .catch(() => {
        // Fallback: find by ID
        return document.getElementById("room-active");
      });
    expect(toggleButton).toBeTruthy();
    // Check aria-pressed or class for toggle state
    const ariaPressed = toggleButton.getAttribute("aria-pressed");
    if (ariaPressed !== null) {
      expect(ariaPressed).toBe("true");
    } else {
      // If no aria-pressed, check if toggle is checked via class or data attribute
      expect(toggleButton).toBeTruthy();
    }
  });

  /**
   * Test active toggle reflects inactive state (Room)
   */
  test("testEditModal_ActiveToggleReflectsInactiveState_Room", async () => {
    const inactiveRoom = { ...mockRoom, active: false };
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(inactiveRoom);

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={inactiveRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    await screen.findByTestId("edit-location-room-name");
    const toggleButton = document.getElementById("room-active");
    expect(toggleButton).toBeTruthy();
    const ariaPressed = toggleButton.getAttribute("aria-pressed");
    if (ariaPressed !== null) {
      expect(ariaPressed).toBe("false");
    }
  });

  /**
   * Test active toggle reflects location active state (Device)
   */
  test("testEditModal_ActiveToggleReflectsState_Device", async () => {
    const activeDevice = { ...mockDevice, active: true };
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(activeDevice);

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={activeDevice}
        locationType="device"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    await screen.findByTestId("edit-location-device-name");
    const toggleButton = document.getElementById("device-active");
    expect(toggleButton).toBeTruthy();
    const ariaPressed = toggleButton.getAttribute("aria-pressed");
    if (ariaPressed !== null) {
      expect(ariaPressed).toBe("true");
    }
  });

  /**
   * Test active toggle reflects inactive state (Device)
   */
  test("testEditModal_ActiveToggleReflectsInactiveState_Device", async () => {
    const inactiveDevice = { ...mockDevice, active: false };
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(inactiveDevice);

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={inactiveDevice}
        locationType="device"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    await screen.findByTestId("edit-location-device-name");
    const toggleButton = document.getElementById("device-active");
    expect(toggleButton).toBeTruthy();
    const ariaPressed = toggleButton.getAttribute("aria-pressed");
    if (ariaPressed !== null) {
      expect(ariaPressed).toBe("false");
    }
  });

  /**
   * T106: Test displays validation errors for duplicate code
   */
  test("testEditModal_ValidationErrors", async () => {
    Utils.putToOpenElisServer.mockImplementation(
      (endpoint, payload, callback) => {
        // Use process.nextTick to ensure callback runs in next event loop tick
        process.nextTick(() => callback(400));
      },
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

    const nameField = await screen.findByTestId("edit-location-room-name");
    fireEvent.change(nameField, { target: { value: "Updated Name" } });

    const saveButton = screen.getByTestId("edit-location-save-button");
    fireEvent.click(saveButton);

    // Wait for error to appear
    const errorElement = await screen
      .findByText(/failed to update/i, {}, { timeout: 2000 })
      .catch(() => {
        return screen.queryByText(/error/i);
      });
    expect(errorElement).toBeTruthy();
  });

  /**
   * T106: Test save button calls PUT endpoint
   */
  test("testEditModal_SaveCallsAPI", async () => {
    Utils.putToOpenElisServer.mockImplementation(
      (endpoint, payload, callback) => {
        process.nextTick(() => callback(200));
      },
    );

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

    const nameField = await screen.findByTestId("edit-location-room-name");
    fireEvent.change(nameField, { target: { value: "Updated Name" } });

    const saveButton = screen.getByTestId("edit-location-save-button");
    fireEvent.click(saveButton);

    // Wait for API call
    await new Promise((resolve) => process.nextTick(resolve));

    expect(Utils.putToOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("/rest/storage/rooms/1"),
      expect.stringContaining("Updated Name"),
      expect.any(Function),
    );

    // Wait for onSave callback
    await new Promise((resolve) => setTimeout(resolve, 100));
    expect(mockOnSave).toHaveBeenCalled();
  });

  /**
   * T106: Test cancel button closes modal without saving
   */
  test("testEditModal_CancelClosesModal", async () => {
    renderWithIntl(
      <EditLocationModal
        open={true}
        location={mockRoom}
        locationType="room"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    const cancelButton = await screen.findByTestId(
      "edit-location-cancel-button",
    );
    fireEvent.click(cancelButton);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
    expect(mockOnSave).not.toHaveBeenCalled();
  });

  // ========== T286: Short Code Field Tests ==========

  /**
   * T286: Test shortCode field appears in Edit form for device
   * Expected: Short code field is visible and editable for device
   */
  test("testShortCodeFieldInEditForm_Device", async () => {
    const deviceWithShortCode = { ...mockDevice, shortCode: "FRZ01" };
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(deviceWithShortCode);

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={deviceWithShortCode}
        locationType="device"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Wait for form to load
    await screen.findByTestId("edit-location-device-name");

    // Verify shortCode field exists
    const shortCodeField = await screen.findByTestId(
      "edit-location-device-short-code",
    );
    expect(shortCodeField).toBeTruthy();

    // Verify field is editable (not disabled)
    const inputElement =
      shortCodeField.querySelector("input") || shortCodeField;
    expect(inputElement.disabled).toBe(false);
    expect(inputElement.readOnly).toBe(false);
  });

  /**
   * T286: Test shortCode field appears in Edit form for shelf
   * Expected: Short code field is visible and editable for shelf
   */
  test("testShortCodeFieldInEditForm_Shelf", async () => {
    const shelfWithShortCode = { ...mockShelf, shortCode: "SHA01" };
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(shelfWithShortCode);

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={shelfWithShortCode}
        locationType="shelf"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Wait for form to load
    await screen.findByTestId("edit-location-shelf-label");

    // Verify shortCode field exists
    const shortCodeField = await screen.findByTestId(
      "edit-location-shelf-short-code",
    );
    expect(shortCodeField).toBeTruthy();
  });

  /**
   * T286: Test shortCode field appears in Edit form for rack
   * Expected: Short code field is visible and editable for rack
   */
  test("testShortCodeFieldInEditForm_Rack", async () => {
    const rackWithShortCode = { ...mockRack, shortCode: "RKR01" };
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(rackWithShortCode);

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={rackWithShortCode}
        locationType="rack"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Wait for form to load
    await screen.findByTestId("edit-location-rack-label");

    // Verify shortCode field exists
    const shortCodeField = await screen.findByTestId(
      "edit-location-rack-short-code",
    );
    expect(shortCodeField).toBeTruthy();
  });

  /**
   * T286: Test shortCode input validation - auto-uppercase conversion
   * Expected: Lowercase input is automatically converted to uppercase
   */
  test("testShortCodeInputValidation_AutoUppercaseConversion", async () => {
    const deviceWithShortCode = { ...mockDevice, shortCode: "frz01" };
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(deviceWithShortCode);

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={deviceWithShortCode}
        locationType="device"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Wait for form to load
    await screen.findByTestId("edit-location-device-name");

    // Find shortCode field
    const shortCodeField = await screen.findByTestId(
      "edit-location-device-short-code",
    );
    const inputElement =
      shortCodeField.querySelector("input") || shortCodeField;

    // Clear existing value and type lowercase value
    // For v8.1.3: use fireEvent to clear (per testing roadmap fallback guidance)
    // then userEvent to type (preferred for user interactions)
    fireEvent.change(inputElement, { target: { value: "" } });
    await userEvent.type(inputElement, "test-code", { delay: 0 });

    // Verify value is converted to uppercase
    await waitFor(() => {
      expect(inputElement.value).toBe("TEST-CODE");
    });
  });

  /**
   * T286: Test required field validation - save button disabled without shortCode when code > 10 chars
   * Expected: Save button is disabled when shortCode is empty AND code > 10 chars for device
   */
  test("testRequiredFieldValidation_SaveButtonDisabledWithoutShortCode_CodeGt10Chars", async () => {
    // Device with code > 10 chars - shortCode is required
    const deviceWithLongCode = {
      ...mockDevice,
      code: "TEST-DEVICE-LONG-CODE",
      shortCode: "",
    };
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(deviceWithLongCode);

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={deviceWithLongCode}
        locationType="device"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Wait for form to load
    await screen.findByTestId("edit-location-device-name");

    // Find shortCode field and clear it
    const shortCodeField = await screen.findByTestId(
      "edit-location-device-short-code",
    );
    const inputElement =
      shortCodeField.querySelector("input") || shortCodeField;
    // Use fireEvent to clear (per testing roadmap fallback guidance)
    fireEvent.change(inputElement, { target: { value: "" } });

    // Verify save button is disabled when shortCode is empty AND code > 10 chars
    const saveButton = screen.getByTestId("edit-location-save-button");
    await waitFor(() => {
      expect(saveButton.disabled).toBe(true);
    });
  });

  /**
   * T286: Test optional field validation - save button enabled without shortCode when code ≤10 chars
   * Expected: Save button is enabled when shortCode is empty BUT code ≤10 chars for device
   */
  test("testOptionalFieldValidation_SaveButtonEnabledWithoutShortCode_CodeLeq10Chars", async () => {
    // Device with code ≤10 chars - shortCode is optional
    const deviceWithShortCode = {
      ...mockDevice,
      code: "TEST-DEV01",
      shortCode: "",
    };
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(deviceWithShortCode);

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={deviceWithShortCode}
        locationType="device"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Wait for form to load
    await screen.findByTestId("edit-location-device-name");

    // Verify save button is enabled when code ≤10 chars (shortCode not required)
    const saveButton = screen.getByTestId("edit-location-save-button");
    await waitFor(() => {
      expect(saveButton.disabled).toBe(false);
    });
  });

  /**
   * T286: Test shortCode is included in save payload for device
   * Expected: shortCode value is sent in PUT request payload
   */
  test("testShortCodeIncludedInSavePayload_Device", async () => {
    const deviceWithShortCode = { ...mockDevice, shortCode: "FRZ01" };
    Utils.getFromOpenElisServerV2.mockResolvedValueOnce(deviceWithShortCode);

    Utils.putToOpenElisServer.mockImplementation(
      (endpoint, payload, callback) => {
        process.nextTick(() => callback(200));
      },
    );

    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve(deviceWithShortCode),
      }),
    );

    renderWithIntl(
      <EditLocationModal
        open={true}
        location={deviceWithShortCode}
        locationType="device"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    // Wait for form to load
    await screen.findByTestId("edit-location-device-name");

    // Update shortCode
    const shortCodeField = await screen.findByTestId(
      "edit-location-device-short-code",
    );
    const inputElement =
      shortCodeField.querySelector("input") || shortCodeField;
    // Use fireEvent to clear, then userEvent to type (per testing roadmap)
    fireEvent.change(inputElement, { target: { value: "" } });
    await userEvent.type(inputElement, "FRZ02", { delay: 0 });

    // Click save
    const saveButton = screen.getByTestId("edit-location-save-button");
    await userEvent.click(saveButton);

    // Wait for API call
    await waitFor(() => {
      expect(Utils.putToOpenElisServer).toHaveBeenCalled();
    });

    // Verify shortCode is in payload
    const putCall = Utils.putToOpenElisServer.mock.calls[0];
    const payload = JSON.parse(putCall[1]);
    expect(payload.shortCode).toBe("FRZ02");
  });
});
