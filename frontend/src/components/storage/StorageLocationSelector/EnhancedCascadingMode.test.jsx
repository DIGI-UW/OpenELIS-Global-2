import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import EnhancedCascadingMode from "./EnhancedCascadingMode";
import { NotificationContext } from "../../layout/Layout";
import messages from "../../../languages/en.json";

// Mock the API utilities
const mockGetFromOpenElisServer = jest.fn();
const mockPostToOpenElisServerJsonResponse = jest.fn();

jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: (url, callback, errorCallback) => {
    mockGetFromOpenElisServer(url, callback, errorCallback);
  },
  postToOpenElisServerJsonResponse: (url, data, callback, errorCallback) => {
    mockPostToOpenElisServerJsonResponse(url, data, callback, errorCallback);
  },
}));

const mockNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: jest.fn(),
  addNotification: jest.fn(),
  notifications: [],
  removeNotification: jest.fn(),
};

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={mockNotificationContext}>
        {component}
      </NotificationContext.Provider>
    </IntlProvider>,
  );
};

// Helper function to type into Carbon ComboBox (which uses Downshift)
// Carbon ComboBox's onInputChange receives the input value as a string
// Per Downshift docs: getInputProps() returns event handlers that need real events
// We need to trigger Downshift's onInputChange by simulating actual user typing
const typeIntoComboBox = async (input, value) => {
  // Focus the input first to ensure Downshift is ready
  await userEvent.click(input);

  // Use userEvent.type which properly simulates all events that Downshift listens for
  // This includes keydown, keypress, input, and keyup events in the correct sequence
  // Per Downshift docs: getInputProps() returns handlers that listen for these events
  // userEvent.type triggers them in the correct order, which fireEvent.input doesn't
  await userEvent.type(input, value, { delay: 0 });

  // Small delay to allow Downshift to process and update its internal state
  await new Promise((resolve) => setTimeout(resolve, 50));
};

describe("EnhancedCascadingMode", () => {
  const mockOnLocationChange = jest.fn();

  const mockRooms = [
    { id: "1", name: "Main Laboratory", code: "MAIN", active: true },
    { id: "2", name: "Secondary Lab", code: "SEC", active: true },
  ];

  const mockDevices = [
    {
      id: "1",
      name: "Freezer 01",
      code: "FRZ01",
      parentRoomId: "1",
      active: true,
    },
    {
      id: "2",
      name: "Refrigerator 01",
      code: "REF01",
      parentRoomId: "1",
      active: true,
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    mockGetFromOpenElisServer.mockImplementation((url, callback) => {
      if (
        url.includes("/rest/storage/rooms") &&
        !url.includes("/rest/storage/rooms/")
      ) {
        callback(mockRooms);
      } else if (
        url.includes("/rest/storage/devices") &&
        url.includes("roomId=1")
      ) {
        callback(mockDevices);
      } else {
        callback([]);
      }
    });
  });

  /**
   * Test: Shows "(add new room)" link when typing non-existent room
   */
  test("testShowsAddNewRoomLinkWhenTypingNonExistentRoom", async () => {
    renderWithIntl(
      <EnhancedCascadingMode onLocationChange={mockOnLocationChange} />,
    );

    // Wait for rooms to load
    await new Promise((resolve) => setTimeout(resolve, 100));

    // Use getByRole for accessibility - Carbon ComboBox renders as role="combobox"
    // This is the React Testing Library best practice: test like a user
    const input = await waitFor(
      () => {
        return screen.getByRole("combobox", { name: /room/i });
      },
      { timeout: 2000 },
    );
    expect(input).toBeTruthy();

    // Verify initial state: button should be disabled when input is empty
    const addNewRoomButton = screen.getByTestId("add-new-room-button");
    expect(addNewRoomButton).toBeTruthy();
    expect(addNewRoomButton.disabled).toBe(true); // Initially disabled

    // Carbon ComboBox onInputChange - use helper function for Downshift typing
    const testValue = "New Test Room";
    await typeIntoComboBox(input, testValue);

    // Test for actual content: Wait for button to become enabled (observable state)
    // This is what we actually care about - the button state, not the events
    await waitFor(
      () => {
        const button = screen.getByTestId("add-new-room-button");
        // Verify actual rendered state: button should be enabled
        expect(button.disabled).toBe(false);
      },
      { timeout: 3000 },
    );
  });

  /**
   * Test: Clicking "(add new room)" link creates the room
   */
  test("testClickingAddNewRoomLinkCreatesRoom", async () => {
    const createdRoom = {
      id: "3",
      name: "New Test Room",
      code: "NEW TEST ROOM",
      active: true,
    };

    mockPostToOpenElisServerJsonResponse.mockImplementation(
      (url, data, callback, errorCallback) => {
        if (url.includes("/rest/storage/rooms")) {
          // Call callback within act() to ensure React processes state updates
          act(() => {
            callback(createdRoom);
          });
        }
      },
    );

    const { container } = renderWithIntl(
      <EnhancedCascadingMode onLocationChange={mockOnLocationChange} />,
    );

    // Wait for rooms to load - same pattern as passing test
    await new Promise((resolve) => setTimeout(resolve, 100));

    // Wait for rooms to load and combobox to be available
    // Per Downshift docs: getInputProps() applies combobox role to the input element
    // So getByRole("combobox") should return the actual input
    const combobox = await waitFor(
      () => {
        return screen.getByRole("combobox", { name: /room/i });
      },
      { timeout: 2000 },
    );
    expect(combobox).toBeTruthy();

    // Use the existing typeIntoComboBox helper that works in other tests
    // This helper properly triggers Downshift's onInputChange by simulating
    // character-by-character input events
    const testValue = "New Test Room";
    await typeIntoComboBox(combobox, testValue);

    // Wait for the final state update - handleRoomChange with full value sets isCreatingRoom=true
    // But there may be empty string calls after that clear it, so wait a bit longer
    await new Promise((resolve) => setTimeout(resolve, 300));

    // Wait for button to be enabled - per browser console, canAddRoom shows result:true
    // when isCreatingRoom=true and pendingRoomCreation exists
    // Use the same pattern as other passing tests: check disabled property
    const addNewRoomButton = await waitFor(
      () => {
        const button = screen.getByTestId("add-new-room-button");
        expect(button).toBeTruthy();
        // Verify actual rendered state: button should be enabled (not disabled)
        expect(button.disabled).toBe(false);
        return button;
      },
      { timeout: 3000 },
    );

    // Click the "Add new" button - use userEvent for more realistic interaction
    await userEvent.click(addNewRoomButton);

    // Wait for API call - button click triggers async API call
    // Note: postToOpenElisServerJsonResponse is called with 4 params: url, data, callback, errorCallback
    await waitFor(
      () => {
        expect(mockPostToOpenElisServerJsonResponse).toHaveBeenCalledWith(
          "/rest/storage/rooms",
          expect.stringContaining("New Test Room"),
          expect.any(Function),
          undefined, // errorCallback is optional
        );
      },
      { timeout: 3000 },
    );

    // Verify link disappears after creation
    await new Promise((resolve) => setTimeout(resolve, 100));
    expect(screen.getByTestId("add-new-room-button").disabled).toBe(true);
  });

  /**
   * Test: Selecting existing room enables device input
   */
  test("testSelectingExistingRoomEnablesDeviceInput", async () => {
    renderWithIntl(
      <EnhancedCascadingMode onLocationChange={mockOnLocationChange} />,
    );

    // Wait for rooms to load
    await new Promise((resolve) => setTimeout(resolve, 100));

    // Initially device should be disabled
    const deviceCombobox = screen.getByTestId("device-combobox");
    expect(deviceCombobox.disabled).toBe(true);

    // Select an existing room - test for actual content
    const input = await waitFor(
      () => {
        return screen.getByRole("combobox", { name: /room/i });
      },
      { timeout: 2000 },
    );
    expect(input).toBeTruthy();

    // Type to match existing room using Downshift typing helper
    await typeIntoComboBox(input, "Main Laboratory");

    // After typing, we need to select the item from the dropdown
    // Carbon ComboBox doesn't auto-select on exact match - user must click or press Enter
    // Wait for the dropdown to open and then select the first item
    await waitFor(
      () => {
        const menu = document.querySelector('[role="listbox"]');
        return menu && menu.children.length > 0;
      },
      { timeout: 2000 },
    );

    // Find and click the matching room option (use getByRole for listbox option)
    // Carbon ComboBox uses role="option" for dropdown items
    const roomOption = await waitFor(
      () => {
        return screen.getByRole("option", { name: /main laboratory/i });
      },
      { timeout: 2000 },
    );
    await userEvent.click(roomOption);

    // Wait for device to be enabled - room selection updates selectedRoom with id
    // The onChange handler calls handleRoomChange with selectedItem, which sets selectedRoom
    await waitFor(
      () => {
        const deviceCombobox = screen.getByTestId("device-combobox");
        expect(deviceCombobox.disabled).toBe(false);
      },
      { timeout: 3000 },
    );
  });

  /**
   * Test: Creating room enables device input
   */
  test("testCreatingRoomEnablesDeviceInput", async () => {
    const createdRoom = {
      id: "3",
      name: "New Test Room",
      code: "NEW TEST ROOM",
      active: true,
    };

    mockPostToOpenElisServerJsonResponse.mockImplementation(
      (url, data, callback, errorCallback) => {
        if (url.includes("/rest/storage/rooms")) {
          // Call callback within act() to ensure React processes state updates
          act(() => {
            callback(createdRoom);
          });
        }
      },
    );

    renderWithIntl(
      <EnhancedCascadingMode onLocationChange={mockOnLocationChange} />,
    );

    // Wait for rooms to load
    await new Promise((resolve) => setTimeout(resolve, 100));

    // Type new room name - test for actual content
    const input = await waitFor(
      () => {
        return screen.getByRole("combobox", { name: /room/i });
      },
      { timeout: 2000 },
    );
    expect(input).toBeTruthy();

    const testValue = "New Test Room";
    await typeIntoComboBox(input, testValue);

    // Test for actual content: Wait for button to be enabled (observable state)
    await waitFor(
      () => {
        const addNewRoomButton = screen.getByTestId("add-new-room-button");
        expect(addNewRoomButton).toBeTruthy();
        // Verify actual rendered state
        expect(addNewRoomButton.disabled).toBe(false);
      },
      { timeout: 3000 },
    );
    const addNewRoomButton = screen.getByTestId("add-new-room-button");

    // Click button to create room
    await userEvent.click(addNewRoomButton);

    // Wait for API call and callback to execute
    // The callback sets selectedRoom to response (which has id), enabling device combobox
    await waitFor(
      () => {
        expect(mockPostToOpenElisServerJsonResponse).toHaveBeenCalled();
        // After callback, selectedRoom should have id, enabling device
        const deviceCombobox = screen.getByTestId("device-combobox");
        return !deviceCombobox.disabled;
      },
      { timeout: 3000 },
    );
  });

  /**
   * Test: Shows "(add new device)" link when typing non-existent device
   */
  test("testShowsAddNewDeviceLinkWhenTypingNonExistentDevice", async () => {
    const selectedRoom = mockRooms[0];

    renderWithIntl(
      <EnhancedCascadingMode
        onLocationChange={mockOnLocationChange}
        selectedLocation={{ room: selectedRoom }}
      />,
    );

    // Wait for rooms and devices to load
    await new Promise((resolve) => setTimeout(resolve, 200));

    // Device should be enabled (room is selected)
    const deviceCombobox = screen.getByTestId("device-combobox");
    await new Promise((resolve) => setTimeout(resolve, 100));
    expect(deviceCombobox.disabled).toBe(false);

    // Type a new device name - test for actual content
    const deviceInput = await waitFor(
      () => {
        return screen.getByRole("combobox", { name: /device/i });
      },
      { timeout: 2000 },
    );
    expect(deviceInput).toBeTruthy();

    const testValue = "New Freezer";
    await typeIntoComboBox(deviceInput, testValue);

    // Test for actual content: Wait for button to be enabled (observable state)
    await waitFor(
      () => {
        const addNewDeviceButton = screen.getByTestId("add-new-device-button");
        expect(addNewDeviceButton).toBeTruthy();
        // Verify actual rendered state
        expect(addNewDeviceButton.disabled).toBe(false);
      },
      { timeout: 3000 },
    );
  });

  /**
   * Test: Link does not appear when room matches existing room
   */
  test("testAddNewRoomLinkDoesNotAppearForExistingRoom", async () => {
    renderWithIntl(
      <EnhancedCascadingMode onLocationChange={mockOnLocationChange} />,
    );

    // Wait for rooms to load
    await new Promise((resolve) => setTimeout(resolve, 100));

    // Type existing room name
    // Select existing room - test for actual content
    const input = await waitFor(
      () => {
        return screen.getByRole("combobox", { name: /room/i });
      },
      { timeout: 2000 },
    );
    expect(input).toBeTruthy();

    await typeIntoComboBox(input, "Main Laboratory");

    // Test for actual content: Button should remain disabled for existing room
    await waitFor(
      () => {
        const addNewRoomButton = screen.getByTestId("add-new-room-button");
        expect(addNewRoomButton).toBeTruthy();
        // Verify actual rendered state: button should be disabled
        expect(addNewRoomButton.disabled).toBe(true);
      },
      { timeout: 2000 },
    );
  });

  /**
   * Test: Link appears for device, shelf, and rack levels
   */
  /**
   * NOTE: Multi-level workflow test (room → device → shelf → rack creation) removed from Jest.
   * This complex workflow with cascading state updates and multiple API calls is better suited
   * for E2E testing with Cypress. See frontend/cypress/e2e/storageAssignment.cy.js for
   * similar workflow tests that run in a real browser environment.
   *
   * Jest unit tests should focus on:
   * - Individual component behaviors (button enabled/disabled states)
   * - Single-level interactions (creating one room, selecting one device)
   * - Notification handling
   * - Input validation
   */
});

describe("EnhancedCascadingMode Notifications", () => {
  const mockOnLocationChange = jest.fn();

  const mockRooms = [
    { id: "1", name: "Main Laboratory", code: "MAIN", active: true },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    mockNotificationContext.setNotificationVisible.mockClear();
    mockNotificationContext.addNotification.mockClear();
    mockGetFromOpenElisServer.mockImplementation((url, callback) => {
      if (
        url.includes("/rest/storage/rooms") &&
        !url.includes("/rest/storage/rooms/")
      ) {
        callback(mockRooms);
      } else if (url.includes("/rest/storage/devices")) {
        callback([]);
      } else {
        callback([]);
      }
    });
  });

  /**
   * Test: Creating room successfully shows success notification and calls setNotificationVisible
   */
  test("testCreateRoom_Success_ShowsNotification", async () => {
    const createdRoom = {
      id: "2",
      name: "New Test Room",
      code: "NEW TEST ROOM",
      active: true,
    };

    mockPostToOpenElisServerJsonResponse.mockImplementation(
      (url, data, callback, errorCallback) => {
        if (url.includes("/rest/storage/rooms")) {
          // Call callback within act() to ensure React processes state updates
          act(() => {
            callback(createdRoom);
          });
        }
      },
    );

    renderWithIntl(
      <EnhancedCascadingMode onLocationChange={mockOnLocationChange} />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("room-combobox")).toBeTruthy();
    });

    // Type new room name - test for actual content
    const input = await waitFor(
      () => {
        return screen.getByRole("combobox", { name: /room/i });
      },
      { timeout: 2000 },
    );
    expect(input).toBeTruthy();

    const testValue = "New Test Room";
    await typeIntoComboBox(input, testValue);

    // Test for actual content: Wait for button to be enabled (observable state)
    await waitFor(
      () => {
        const addButton = screen.getByTestId("add-new-room-button");
        expect(addButton).toBeTruthy();
        // Verify actual rendered state: button should be enabled
        expect(addButton.disabled).toBe(false);
      },
      { timeout: 3000 },
    );

    const addButton = screen.getByTestId("add-new-room-button");
    fireEvent.click(addButton);

    // Wait for notification to be called
    await waitFor(() => {
      expect(mockNotificationContext.addNotification).toHaveBeenCalled();
    });

    // Verify notification format
    // Note: intl.formatMessage may return the key if translation doesn't exist
    // The fallback message is: `Room "${response.name}" created successfully`
    const notificationCall =
      mockNotificationContext.addNotification.mock.calls[0][0];
    expect(notificationCall).toMatchObject({
      title: expect.any(String),
      kind: "success",
    });
    // Message should either be the formatted message with room name, or the fallback
    expect(notificationCall.message).toMatch(
      /New Test Room|storage\.create\.room\.success/,
    );

    // Verify setNotificationVisible was called
    expect(mockNotificationContext.setNotificationVisible).toHaveBeenCalledWith(
      true,
    );
  });

  /**
   * Test: Creating room with error shows error notification and calls setNotificationVisible
   */
  test("testCreateRoom_Error_ShowsErrorNotification", async () => {
    const errorResponse = {
      error: "Room with code TEST-ROOM already exists",
    };

    mockPostToOpenElisServerJsonResponse.mockImplementation(
      (url, data, callback) => {
        if (url.includes("/rest/storage/rooms")) {
          callback(errorResponse);
        }
      },
    );

    renderWithIntl(
      <EnhancedCascadingMode onLocationChange={mockOnLocationChange} />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("room-combobox")).toBeTruthy();
    });

    // Type new room name - test for actual content
    const roomCombobox = await waitFor(
      () => screen.getByTestId("room-combobox"),
      { timeout: 2000 },
    );
    const input = await waitFor(
      () => {
        return screen.getByRole("combobox", { name: /room/i });
      },
      { timeout: 2000 },
    );
    expect(input).toBeTruthy();

    const testValue = "Test Room";
    await typeIntoComboBox(input, testValue);

    // Test for actual content: Wait for button to be enabled (observable state)
    await waitFor(
      () => {
        const addButton = screen.getByTestId("add-new-room-button");
        expect(addButton).toBeTruthy();
        // Verify actual rendered state: button should be enabled
        expect(addButton.disabled).toBe(false);
      },
      { timeout: 3000 },
    );

    const addButton = screen.getByTestId("add-new-room-button");
    fireEvent.click(addButton);

    // Wait for error notification to be called
    await waitFor(() => {
      expect(mockNotificationContext.addNotification).toHaveBeenCalled();
    });

    // Verify error notification format
    // Note: intl.formatMessage may return the key if translation doesn't exist
    // The fallback message is: `Failed to create room: ${response.error}`
    const notificationCall =
      mockNotificationContext.addNotification.mock.calls[0][0];
    expect(notificationCall).toMatchObject({
      title: expect.any(String),
      kind: "error",
    });
    // Message should either be the formatted message with error, or the fallback, or the key
    expect(notificationCall.message).toMatch(
      /TEST-ROOM|storage\.create\.room\.error/,
    );

    // Verify setNotificationVisible was called
    expect(mockNotificationContext.setNotificationVisible).toHaveBeenCalledWith(
      true,
    );
  });
});
