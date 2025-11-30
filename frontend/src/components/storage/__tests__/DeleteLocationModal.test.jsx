import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import DeleteLocationModal from "../LocationManagement/DeleteLocationModal";
import messages from "../../../languages/en.json";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";

// Mock the API utilities
const mockGetFromOpenElisServer = jest.fn();
const mockPostToOpenElisServer = jest.fn();

jest.mock("../../utils/Utils", () => ({
  ...jest.requireActual("../../utils/Utils"),
  getFromOpenElisServer: (...args) => mockGetFromOpenElisServer(...args),
  postToOpenElisServer: (...args) => mockPostToOpenElisServer(...args),
}));

const mockUserSessionDetailsAdmin = {
  roles: ["Global Administrator"],
};

const mockUserSessionDetailsNonAdmin = {
  roles: [],
};

const renderWithIntl = (component, isAdmin = true) => {
  const userSessionDetails = isAdmin
    ? mockUserSessionDetailsAdmin
    : mockUserSessionDetailsNonAdmin;
  return render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: userSessionDetails }}
      >
        {component}
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );
};

describe("DeleteLocationModal", () => {
  const mockLocation = {
    id: "1",
    name: "Main Laboratory",
    code: "MAIN-LAB",
    type: "room",
  };

  const mockOnClose = jest.fn();
  const mockOnDelete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockGetFromOpenElisServer.mockClear();
    mockPostToOpenElisServer.mockClear();
    global.fetch = jest.fn();
  });

  /**
   * T107: Test shows error message if constraints exist
   */
  test("testDeleteModal_WithConstraints_ShowsError", async () => {
    // Mock constraint check - simulate 409 Conflict response
    mockGetFromOpenElisServer.mockImplementation((endpoint, callback) => {
      setTimeout(() => {
        callback({
          status: 409,
          data: {
            error: "Cannot delete room",
            message:
              "Cannot delete Room 'Main Laboratory' because it contains 8 device(s)",
          },
        });
      }, 0);
    });

    // Use non-admin user to test error message display
    renderWithIntl(
      <DeleteLocationModal
        open={true}
        location={mockLocation}
        locationType="room"
        onClose={mockOnClose}
        onDelete={mockOnDelete}
      />,
      false, // isAdmin = false
    );

    // Wait for constraint check to complete
    await new Promise((resolve) => setTimeout(resolve, 100));

    const constraintsError = screen.getByTestId(
      "delete-location-constraints-error",
    );
    expect(constraintsError).toBeTruthy();
    const constraintsMessage = screen.getByTestId(
      "delete-location-constraints-message",
    );
    expect(constraintsMessage.textContent).toContain("contains 8 device");

    // Delete button should not be available when constraints exist
    expect(screen.queryByTestId("delete-location-confirm-button")).toBeNull();
  });

  /**
   * T107: Test shows confirmation dialog if no constraints
   */
  test("testDeleteModal_NoConstraints_ShowsConfirmation", async () => {
    // Mock constraint check - no constraints (200 OK or no error)
    mockGetFromOpenElisServer.mockImplementation((endpoint, callback) => {
      setTimeout(() => {
        callback({
          status: 200,
          data: { canDelete: true },
        });
      }, 0);
    });

    renderWithIntl(
      <DeleteLocationModal
        open={true}
        location={mockLocation}
        locationType="room"
        onClose={mockOnClose}
        onDelete={mockOnDelete}
      />,
    );

    // Wait for constraint check to complete
    await new Promise((resolve) => setTimeout(resolve, 100));

    expect(screen.getByTestId("delete-location-are-you-sure")).toBeTruthy();
    expect(screen.getByTestId("delete-location-cannot-be-undone")).toBeTruthy();

    // Confirmation checkbox and delete button should be present
    expect(
      screen.getByTestId("delete-location-confirmation-checkbox"),
    ).toBeTruthy();
    expect(screen.getByTestId("delete-location-confirm-button")).toBeTruthy();
  });

  /**
   * T107: Test confirm button disabled until user confirms
   */
  test("testDeleteModal_ConfirmationRequired", async () => {
    mockGetFromOpenElisServer.mockImplementation((endpoint, callback) => {
      setTimeout(() => {
        callback({
          status: 200,
          data: { canDelete: true },
        });
      }, 0);
    });

    renderWithIntl(
      <DeleteLocationModal
        open={true}
        location={mockLocation}
        locationType="room"
        onClose={mockOnClose}
        onDelete={mockOnDelete}
      />,
    );

    await new Promise((resolve) => setTimeout(resolve, 100));

    const confirmButton = screen.getByTestId("delete-location-confirm-button");
    expect(confirmButton.disabled).toBe(true);

    // Check the confirmation checkbox
    const checkbox = screen.getByTestId(
      "delete-location-confirmation-checkbox",
    );
    const checkboxInput = checkbox.querySelector("input") || checkbox;
    fireEvent.click(checkboxInput);

    // Now confirm button should be enabled
    await new Promise((resolve) => setTimeout(resolve, 100));
    const enabledButton = screen.getByTestId("delete-location-confirm-button");
    expect(enabledButton.disabled).toBe(false);
  });

  /**
   * T107: Test delete button calls DELETE endpoint
   */
  test("testDeleteModal_DeleteCallsAPI", async () => {
    mockGetFromOpenElisServer.mockImplementation((endpoint, callback) => {
      setTimeout(() => {
        callback({
          status: 200,
          data: { canDelete: true },
        });
      }, 0);
    });

    // Mock fetch for DELETE request
    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        status: 204,
        json: () => Promise.resolve({}),
      }),
    );

    renderWithIntl(
      <DeleteLocationModal
        open={true}
        location={mockLocation}
        locationType="room"
        onClose={mockOnClose}
        onDelete={mockOnDelete}
      />,
    );

    await new Promise((resolve) => setTimeout(resolve, 100));

    const checkbox = screen.getByTestId(
      "delete-location-confirmation-checkbox",
    );
    const checkboxInput = checkbox.querySelector("input") || checkbox;
    fireEvent.click(checkboxInput);

    await new Promise((resolve) => setTimeout(resolve, 100));

    const confirmButton = screen.getByTestId("delete-location-confirm-button");
    fireEvent.click(confirmButton);

    await new Promise((resolve) => setTimeout(resolve, 200));

    // Note: The component uses fetch directly, not postToOpenElisServer
    // So we need to mock fetch instead
    expect(mockOnDelete).toHaveBeenCalled();
  });

  /**
   * T107: Test cancel button closes modal without deleting
   */
  test("testDeleteModal_CancelClosesModal", async () => {
    mockGetFromOpenElisServer.mockImplementation((endpoint, callback) => {
      setTimeout(() => {
        callback({
          status: 200,
          data: { canDelete: true },
        });
      }, 0);
    });

    renderWithIntl(
      <DeleteLocationModal
        open={true}
        location={mockLocation}
        locationType="room"
        onClose={mockOnClose}
        onDelete={mockOnDelete}
      />,
    );

    await new Promise((resolve) => setTimeout(resolve, 100));

    const cancelButton = screen.getByTestId("delete-location-cancel-button");
    fireEvent.click(cancelButton);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
    expect(mockOnDelete).not.toHaveBeenCalled();
  });

  /**
   * OGC-75: Test shelf deletion calls correct endpoint with "shelves" (not "shelfs")
   */
  test("testDeleteModal_ShelfType_UsesCorrectPlural", async () => {
    const mockShelfLocation = {
      id: "20",
      name: "Shelf A",
      code: "SHELF-A",
      type: "shelf",
    };

    let capturedEndpoint = null;
    mockGetFromOpenElisServer.mockImplementation((endpoint, callback) => {
      capturedEndpoint = endpoint;
      setTimeout(() => {
        callback({
          status: 200,
          data: { canDelete: true },
        });
      }, 0);
    });

    renderWithIntl(
      <DeleteLocationModal
        open={true}
        location={mockShelfLocation}
        locationType="shelf"
        onClose={mockOnClose}
        onDelete={mockOnDelete}
      />,
    );

    await new Promise((resolve) => setTimeout(resolve, 100));

    // Verify endpoint uses "shelves" not "shelfs"
    expect(capturedEndpoint).toBe("/rest/storage/shelves/20/can-delete");
  });

  /**
   * OGC-75: Test all location types generate correct plural URLs
   */
  test("testDeleteModal_AllLocationTypes_GenerateCorrectPlurals", async () => {
    const locationTypes = [
      { type: "room", expected: "rooms" },
      { type: "device", expected: "devices" },
      { type: "shelf", expected: "shelves" },
      { type: "rack", expected: "racks" },
    ];

    for (const { type, expected } of locationTypes) {
      jest.clearAllMocks();
      let capturedEndpoint = null;

      const mockLocation = {
        id: "1",
        name: `Test ${type}`,
        code: `TEST-${type.toUpperCase()}`,
        type: type,
      };

      mockGetFromOpenElisServer.mockImplementation((endpoint, callback) => {
        capturedEndpoint = endpoint;
        setTimeout(() => {
          callback({
            status: 200,
            data: { canDelete: true },
          });
        }, 0);
      });

      renderWithIntl(
        <DeleteLocationModal
          open={true}
          location={mockLocation}
          locationType={type}
          onClose={mockOnClose}
          onDelete={mockOnDelete}
        />,
      );

      await new Promise((resolve) => setTimeout(resolve, 100));

      expect(capturedEndpoint).toBe(`/rest/storage/${expected}/1/can-delete`);
    }
  });
});
