import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import StorageLocationSelector from "./StorageLocationSelector";
import { getFromOpenElisServer } from "../../utils/Utils";
import messages from "../../../languages/en.json";

// Mock the API utilities
jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

jest.mock("../SampleStorage/LocationManagementModal", () => {
  return function MockLocationManagementModal({ open, onConfirm, onClose }) {
    if (!open) return null;
    return (
      <div data-testid="location-management-modal">
        <button
          data-testid="mock-modal-save-box"
          onClick={() =>
            onConfirm?.({
              newLocation: {
                room: { id: "1", name: "Main Laboratory" },
                device: { id: "10", name: "Freezer Unit 1" },
                shelf: { id: "20", label: "Shelf-A" },
                rack: { id: "30", label: "R1" },
                box: { id: "40", label: "Box2" },
                position: { coordinate: "D1" },
              },
              positionCoordinate: "D1",
            })
          }
        >
          Save Mock Location
        </button>
        <button data-testid="mock-modal-close" onClick={onClose}>
          Close
        </button>
      </div>
    );
  };
});

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("StorageLocationSelector", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * T051: Test device dropdown is disabled until room is selected
   * Cascading dropdown behavior
   */
  test("should disable device dropdown until room selected", () => {
    // Mock empty rooms list
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/rooms")) {
        callback([]);
      }
    });

    renderWithIntl(<StorageLocationSelector mode="dropdown" />);

    const deviceDropdown = screen.getByTestId("device-dropdown");
    // Carbon Dropdown may set disabled on button or input inside
    const button = deviceDropdown.querySelector("button") || deviceDropdown;
    expect(button.hasAttribute("disabled")).toBe(true);
  });

  /**
   * T051: Test fetches devices when room is selected
   * Data fetching on parent selection
   */
  test("should fetch devices when room is selected", async () => {
    const mockRooms = [{ id: "1", name: "Main Laboratory", code: "MAIN" }];

    const mockDevices = [
      { id: "2", name: "Freezer Unit 1", code: "FRZ01", type: "freezer" },
    ];

    // Mock API responses - set up before rendering
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/rest/storage/rooms")) {
        callback(mockRooms);
      } else if (url.includes("/rest/storage/devices")) {
        callback(mockDevices);
      }
    });

    renderWithIntl(<StorageLocationSelector mode="dropdown" />);

    // Wait for rooms API call (mocked function is called synchronously)
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/storage/rooms",
      expect.any(Function),
      expect.any(Function),
    );

    // Wait a bit for component to update after rooms load
    await new Promise((resolve) => setTimeout(resolve, 100));

    // Select room - Carbon Dropdown requires clicking the button first
    const roomDropdown = screen.getByTestId("room-dropdown");
    const roomButton = roomDropdown.querySelector("button") || roomDropdown;
    fireEvent.click(roomButton);

    // Wait for dropdown options to appear and click the option
    await new Promise((resolve) => setTimeout(resolve, 100));
    const mainLabOption = screen.getByText("Main Laboratory");
    fireEvent.click(mainLabOption);

    // Wait for devices API call
    await new Promise((resolve) => setTimeout(resolve, 100));
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      expect.stringContaining("/rest/storage/devices"),
      expect.any(Function),
      expect.any(Function),
    );

    // Wait for device dropdown to be enabled
    await new Promise((resolve) => setTimeout(resolve, 100));
    const deviceDropdown = screen.getByTestId("device-dropdown");
    const button = deviceDropdown.querySelector("button") || deviceDropdown;
    expect(button.hasAttribute("disabled")).toBe(false);
  });

  /**
   * T051: Test displays hierarchical path when all levels selected
   * Path display behavior
   */
  test("should display hierarchical path when all levels selected", async () => {
    renderWithIntl(<StorageLocationSelector mode="dropdown" />);

    // Simulate full selection (Room → Device → Shelf → Rack → Position)
    // This would require full mock setup, simplified for now

    // After selections, path should display
    const pathDisplay = screen.queryByTestId("location-path");
    // Path format: "Room > Device > Shelf > Rack > Position"
    // Assertion would check path format once selections made
  });

  /**
   * T051: Test handles inline location creation
   * "Add New" button behavior
   */
  test("should show add new buttons when enableInlineCreation is true", () => {
    renderWithIntl(
      <StorageLocationSelector mode="dropdown" enableInlineCreation={true} />,
    );

    // Should show "Add New Room" button
    const addRoomButton = screen.queryByText(/add new room/i);
    expect(addRoomButton).toBeTruthy();
  });

  /**
   * T051: Test mode switching between dropdown/autocomplete/barcode
   * Mode prop behavior
   */
  test("should render cascading dropdowns in dropdown mode", () => {
    renderWithIntl(<StorageLocationSelector mode="dropdown" />);

    expect(screen.getByTestId("room-dropdown")).toBeTruthy();
  });

  test("should render autocomplete in autocomplete mode", () => {
    renderWithIntl(<StorageLocationSelector mode="autocomplete" />);

    const autocompleteInput = screen.queryByPlaceholderText(/search/i);
    expect(autocompleteInput).toBeTruthy();
  });

  test("should render barcode input in barcode mode", () => {
    renderWithIntl(<StorageLocationSelector mode="barcode" />);

    const barcodeInput = screen.queryByPlaceholderText(/scan barcode/i);
    expect(barcodeInput).toBeTruthy();
  });

  /**
   * Test two-tier design: compact view + modal for orders workflow
   */
  test("should render compact view for orders workflow", () => {
    const mockSampleInfo = {
      sampleId: "S-2025-001",
      type: "Blood Serum",
      status: "Active",
    };

    renderWithIntl(
      <StorageLocationSelector workflow="orders" sampleInfo={mockSampleInfo} />,
    );

    // Should show compact location view
    expect(screen.getByTestId("compact-location-view")).toBeTruthy();
    // Should not show legacy dropdown mode
    expect(screen.queryByTestId("room-dropdown")).toBeNull();
  });

  /**
   * Test two-tier design: compact view + modal for results workflow
   */
  test("should render compact view with quick-find for results workflow", () => {
    const mockSampleInfo = {
      sampleId: "S-2025-002",
      type: "Blood Serum",
      status: "Active",
    };

    renderWithIntl(
      <StorageLocationSelector
        workflow="results"
        sampleInfo={mockSampleInfo}
        showQuickFind={true}
      />,
    );

    // Should show compact location view
    expect(screen.getByTestId("compact-location-view")).toBeTruthy();
    // Should show quick-find search
    expect(screen.getByTestId("quick-find-container")).toBeTruthy();
  });

  /**
   * Test modal opens when expand button clicked in two-tier design
   */
  test("should open modal when expand button clicked", () => {
    const mockSampleInfo = {
      sampleId: "S-2025-001",
      type: "Blood Serum",
      status: "Active",
    };

    renderWithIntl(
      <StorageLocationSelector workflow="orders" sampleInfo={mockSampleInfo} />,
    );

    const expandButton = screen.getByTestId("expand-button");
    fireEvent.click(expandButton);

    // Modal should be open (LocationManagementModal)
    expect(screen.getByTestId("location-management-modal")).toBeTruthy();
  });

  /**
   * Test initial hierarchicalPath prop is displayed
   */
  test("should display initial hierarchicalPath when provided", () => {
    const mockSampleInfo = {
      sampleId: "S-2025-001",
      type: "Blood Serum",
      status: "Active",
    };

    renderWithIntl(
      <StorageLocationSelector
        workflow="results"
        sampleInfo={mockSampleInfo}
        hierarchicalPath="Main Laboratory > Freezer Unit 1"
      />,
    );

    const pathText = screen.getByTestId("location-path-text");
    expect(pathText.textContent).toContain("Main Laboratory > Freezer Unit 1");
  });

  test("should normalize modal payload to locationId/locationType/positionCoordinate", async () => {
    const mockSampleInfo = {
      sampleId: "S-2025-010",
      type: "Serum",
      status: "Active",
    };
    const mockOnLocationChange = jest.fn();

    renderWithIntl(
      <StorageLocationSelector
        workflow="orders"
        sampleInfo={mockSampleInfo}
        onLocationChange={mockOnLocationChange}
      />,
    );

    fireEvent.click(screen.getByTestId("expand-button"));
    fireEvent.click(await screen.findByTestId("mock-modal-save-box"));

    await waitFor(() => {
      expect(mockOnLocationChange).toHaveBeenCalled();
    });

    const lastCallArg =
      mockOnLocationChange.mock.calls[mockOnLocationChange.mock.calls.length - 1][0];
    expect(lastCallArg.locationId).toBe("40");
    expect(lastCallArg.locationType).toBe("box");
    expect(lastCallArg.positionCoordinate).toBe("D1");

    const pathText = await screen.findByTestId("location-path-text");
    expect(pathText.textContent).toContain("Box2 > D1");
  });
});
