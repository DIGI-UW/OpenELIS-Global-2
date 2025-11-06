import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import EnhancedCascadingMode from "./EnhancedCascadingMode";
import messages from "../../../languages/en.json";

// Mock the API utilities
const mockGetFromOpenElisServer = jest.fn();
const mockPostToOpenElisServer = jest.fn();

jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: (url, callback, errorCallback) => {
    mockGetFromOpenElisServer(url, callback, errorCallback);
  },
  postToOpenElisServer: (url, data, callback, errorCallback) => {
    mockPostToOpenElisServer(url, data, callback, errorCallback);
  },
}));

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
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
      if (url.includes("/rest/storage/rooms") && !url.includes("/rest/storage/rooms/")) {
        callback(mockRooms);
      } else if (url.includes("/rest/storage/devices") && url.includes("roomId=1")) {
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

    // Find room combobox and type a new room name
    const roomCombobox = screen.getByTestId("room-combobox");
    expect(roomCombobox).toBeTruthy();

    // Type a new room name that doesn't exist
    // Carbon ComboBox onInputChange expects { inputValue } object
    const input = roomCombobox.querySelector("input");
    if (input) {
      fireEvent.change(input, { target: { value: "New Test Room" } });
      // Also trigger onInputChange manually
      fireEvent.input(input, { target: { value: "New Test Room" } });
    }

    // Verify "(add new room)" link appears
    await new Promise((resolve) => setTimeout(resolve, 200));
    const addNewRoomLink = screen.queryByTestId("add-new-room-link");
    expect(addNewRoomLink).toBeTruthy();
    expect(addNewRoomLink.textContent).toMatch(/add new room/i);
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

    mockPostToOpenElisServer.mockImplementation(
      (url, data, callback) => {
        if (url.includes("/rest/storage/rooms")) {
          callback(createdRoom);
        }
      },
    );

    renderWithIntl(
      <EnhancedCascadingMode onLocationChange={mockOnLocationChange} />,
    );

    // Wait for rooms to load
    await new Promise((resolve) => setTimeout(resolve, 100));

    // Type a new room name
    const roomCombobox = screen.getByTestId("room-combobox");
    const input = roomCombobox.querySelector("input");
    if (input) {
      fireEvent.change(input, { target: { value: "New Test Room" } });
      fireEvent.input(input, { target: { value: "New Test Room" } });
    }

    // Wait for link to appear
    await new Promise((resolve) => setTimeout(resolve, 100));
    const addNewRoomLink = screen.getByTestId("add-new-room-link");
    expect(addNewRoomLink).toBeTruthy();

    // Click the "(add new room)" link
    fireEvent.click(addNewRoomLink);

    // Wait for API call
    await new Promise((resolve) => setTimeout(resolve, 200));

    // Verify room creation API was called
    expect(mockPostToOpenElisServer).toHaveBeenCalledWith(
      "/rest/storage/rooms",
      expect.objectContaining({
        name: "New Test Room",
        code: "NEW TEST ROOM",
        active: true,
      }),
      expect.any(Function),
      expect.any(Function),
    );

    // Verify link disappears after creation
    await new Promise((resolve) => setTimeout(resolve, 100));
    expect(screen.queryByTestId("add-new-room-link")).toBeNull();
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
    expect(deviceCombobox.hasAttribute("disabled")).toBe(true);

    // Select an existing room from dropdown
    const roomCombobox = screen.getByTestId("room-combobox");
    const input = roomCombobox.querySelector("input");
    if (input) {
      // Type to match existing room
      fireEvent.change(input, { target: { value: "Main Laboratory" } });
      fireEvent.input(input, { target: { value: "Main Laboratory" } });
    }

    // Wait for devices to load
    await new Promise((resolve) => setTimeout(resolve, 200));

    // Device should now be enabled
    const deviceComboboxAfter = screen.getByTestId("device-combobox");
    await new Promise((resolve) => setTimeout(resolve, 100));
    expect(deviceComboboxAfter.hasAttribute("disabled")).toBe(false);
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

    mockPostToOpenElisServer.mockImplementation(
      (url, data, callback) => {
        if (url.includes("/rest/storage/rooms")) {
          callback(createdRoom);
        }
      },
    );

    renderWithIntl(
      <EnhancedCascadingMode onLocationChange={mockOnLocationChange} />,
    );

    // Wait for rooms to load
    await new Promise((resolve) => setTimeout(resolve, 100));

    // Type new room name
    const roomCombobox = screen.getByTestId("room-combobox");
    const input = roomCombobox.querySelector("input");
    if (input) {
      fireEvent.change(input, { target: { value: "New Test Room" } });
      fireEvent.input(input, { target: { value: "New Test Room" } });
    }

    // Wait for link to appear
    await new Promise((resolve) => setTimeout(resolve, 100));
    const addNewRoomLink = screen.getByTestId("add-new-room-link");
    expect(addNewRoomLink).toBeTruthy();

    // Click link to create room
    fireEvent.click(addNewRoomLink);

    // Wait for room to be created
    await new Promise((resolve) => setTimeout(resolve, 200));
    expect(mockPostToOpenElisServer).toHaveBeenCalled();

    // Device should now be enabled
    await new Promise((resolve) => setTimeout(resolve, 200));
    const deviceCombobox = screen.getByTestId("device-combobox");
    expect(deviceCombobox.hasAttribute("disabled")).toBe(false);
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
    expect(deviceCombobox.hasAttribute("disabled")).toBe(false);

    // Type a new device name
    const deviceInput = deviceCombobox.querySelector("input");
    if (deviceInput) {
      fireEvent.change(deviceInput, { target: { value: "New Freezer" } });
      fireEvent.input(deviceInput, { target: { value: "New Freezer" } });
    }

    // Verify "(add new device)" link appears
    await new Promise((resolve) => setTimeout(resolve, 100));
    const addNewDeviceLink = screen.queryByTestId("add-new-device-link");
    expect(addNewDeviceLink).toBeTruthy();
    expect(addNewDeviceLink.textContent).toMatch(/add new device/i);
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
    const roomCombobox = screen.getByTestId("room-combobox");
    const input = roomCombobox.querySelector("input");
    if (input) {
      fireEvent.change(input, { target: { value: "Main Laboratory" } });
      fireEvent.input(input, { target: { value: "Main Laboratory" } });
    }

    // Link should not appear for existing room
    await new Promise((resolve) => setTimeout(resolve, 100));
    const addNewRoomLink = screen.queryByTestId("add-new-room-link");
    expect(addNewRoomLink).toBeNull();
  });

  /**
   * Test: Link appears for device, shelf, and rack levels
   */
  test("testAddNewLinksAppearForAllLevels", async () => {
    const createdRoom = {
      id: "3",
      name: "New Room",
      code: "NEW ROOM",
      active: true,
    };
    const createdDevice = {
      id: "3",
      name: "New Device",
      code: "NEW DEVICE",
      parentRoomId: "3",
      active: true,
    };
    const createdShelf = {
      id: "3",
      label: "New Shelf",
      parentDeviceId: "3",
      active: true,
    };

    mockPostToOpenElisServer.mockImplementation((url, data, callback) => {
      if (url.includes("/rest/storage/rooms")) {
        callback(createdRoom);
      } else if (url.includes("/rest/storage/devices")) {
        callback(createdDevice);
      } else if (url.includes("/rest/storage/shelves")) {
        callback(createdShelf);
      }
    });

    mockGetFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/rooms") && !url.includes("/rest/storage/rooms/")) {
        callback(mockRooms);
      } else if (url.includes("/rest/storage/devices")) {
        callback(mockDevices);
      } else if (url.includes("/rest/storage/shelves")) {
        callback([]);
      } else if (url.includes("/rest/storage/racks")) {
        callback([]);
      }
    });

    renderWithIntl(
      <EnhancedCascadingMode onLocationChange={mockOnLocationChange} />,
    );

    // Wait for initial load
    await new Promise((resolve) => setTimeout(resolve, 100));

    // Create room first
    const roomCombobox = screen.getByTestId("room-combobox");
    const roomInput = roomCombobox.querySelector("input");
    if (roomInput) {
      fireEvent.change(roomInput, { target: { value: "New Room" } });
      fireEvent.input(roomInput, { target: { value: "New Room" } });
    }
    await new Promise((resolve) => setTimeout(resolve, 200));
    expect(screen.getByTestId("add-new-room-link")).toBeTruthy();
    fireEvent.click(screen.getByTestId("add-new-room-link"));

    // Wait for room creation and device to be enabled
    await new Promise((resolve) => setTimeout(resolve, 300));
    const deviceCombobox = screen.getByTestId("device-combobox");
    expect(deviceCombobox.hasAttribute("disabled")).toBe(false);

    // Type new device
    const deviceInput = deviceCombobox.querySelector("input");
    if (deviceInput) {
      fireEvent.change(deviceInput, { target: { value: "New Device" } });
      fireEvent.input(deviceInput, { target: { value: "New Device" } });
    }
    await new Promise((resolve) => setTimeout(resolve, 200));
    expect(screen.getByTestId("add-new-device-link")).toBeTruthy();

    // Create device
    fireEvent.click(screen.getByTestId("add-new-device-link"));

    // Wait for device creation and shelf to be enabled
    await new Promise((resolve) => setTimeout(resolve, 300));
    const shelfCombobox = screen.getByTestId("shelf-combobox");
    expect(shelfCombobox.hasAttribute("disabled")).toBe(false);

    // Type new shelf
    const shelfInput = shelfCombobox.querySelector("input");
    if (shelfInput) {
      fireEvent.change(shelfInput, { target: { value: "New Shelf" } });
      fireEvent.input(shelfInput, { target: { value: "New Shelf" } });
    }
    await new Promise((resolve) => setTimeout(resolve, 200));
    expect(screen.getByTestId("add-new-shelf-link")).toBeTruthy();

    // Create shelf
    fireEvent.click(screen.getByTestId("add-new-shelf-link"));

    // Wait for shelf creation and rack to be enabled
    await new Promise((resolve) => setTimeout(resolve, 300));
    const rackCombobox = screen.getByTestId("rack-combobox");
    expect(rackCombobox.hasAttribute("disabled")).toBe(false);

    // Type new rack
    const rackInput = rackCombobox.querySelector("input");
    if (rackInput) {
      fireEvent.change(rackInput, { target: { value: "New Rack" } });
      fireEvent.input(rackInput, { target: { value: "New Rack" } });
    }
    await new Promise((resolve) => setTimeout(resolve, 200));
    expect(screen.getByTestId("add-new-rack-link")).toBeTruthy();
  });
});

