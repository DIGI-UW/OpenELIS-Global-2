import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import StorageLocationModal from "./StorageLocationModal";
import messages from "../../languages/en.json";
import * as Utils from "../utils/Utils";

// Mock utilities BEFORE imports (Jest hoisting)
jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  getFromOpenElisServerV2: jest.fn(),
  postToOpenElisServer: jest.fn(),
  postToOpenElisServerJsonResponse: jest.fn(),
  putToOpenElisServer: jest.fn(),
}));

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );
};

describe("StorageLocationModal", () => {
  const mockOnClose = jest.fn();
  const mockOnSave = jest.fn();

  const mockParentRoom = {
    id: "1",
    name: "Main Laboratory",
    code: "MAIN-LAB",
  };

  beforeEach(() => {
    jest.clearAllMocks();
    Utils.getFromOpenElisServerV2.mockResolvedValue([]);
    Utils.postToOpenElisServerJsonResponse.mockImplementation(
      (url, data, callback) => {
        callback({ id: "new-id", ...JSON.parse(data) });
      },
    );
    Utils.putToOpenElisServer.mockImplementation((url, data, callback) => {
      callback(200);
    });
  });

  /**
   * T033: Test renders modal for Room creation
   */
  test("testStorageLocationModal_RendersForRoom_CreateMode", async () => {
    renderWithIntl(
      <StorageLocationModal
        open={true}
        locationType="room"
        mode="create"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    await screen.findByTestId("storage-location-modal");
    expect(document.getElementById("room-name")).toBeTruthy();
    expect(document.getElementById("room-description")).toBeTruthy();
  });

  /**
   * T033: Test renders modal for Device creation with connectivity fields
   */
  test("testStorageLocationModal_RendersForDevice_WithConnectivityFields", async () => {
    renderWithIntl(
      <StorageLocationModal
        open={true}
        locationType="device"
        mode="create"
        parentRoom={mockParentRoom}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    await screen.findByTestId("storage-location-modal");
    expect(document.getElementById("device-name")).toBeTruthy();
    expect(document.getElementById("device-ip-address")).toBeTruthy();
    expect(document.getElementById("device-port")).toBeTruthy();
    expect(
      document.getElementById("device-communication-protocol"),
    ).toBeTruthy();
  });

  /**
   * T035: Test IP address validation (IPv4 format)
   */
  test("testStorageLocationModal_InvalidIPv4_ShowsValidationError", async () => {
    renderWithIntl(
      <StorageLocationModal
        open={true}
        locationType="device"
        mode="create"
        parentRoom={mockParentRoom}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    await screen.findByTestId("storage-location-modal");
    const ipInput = document.getElementById("device-ip-address");
    await userEvent.type(ipInput, "999.999.999.999", { delay: 0 });

    const saveButton = await screen.findByTestId(
      "storage-location-save-button",
    );
    await userEvent.click(saveButton);

    await screen.findByText(/invalid ip address format/i);
  });

  /**
   * T035: Test port validation (1-65535 range)
   */
  test("testStorageLocationModal_InvalidPort_ShowsValidationError", async () => {
    renderWithIntl(
      <StorageLocationModal
        open={true}
        locationType="device"
        mode="create"
        parentRoom={mockParentRoom}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    await screen.findByTestId("storage-location-modal");
    const portInput = document.getElementById("device-port");
    await userEvent.type(portInput, "70000", { delay: 0 });

    const saveButton = await screen.findByTestId(
      "storage-location-save-button",
    );
    await userEvent.click(saveButton);

    await screen.findByText(/port must be between 1 and 65535/i);
  });

  /**
   * T039a: Test uniqueness validation (409 Conflict response)
   */
  test("testStorageLocationModal_DuplicateName_ShowsUniquenessError", async () => {
    Utils.postToOpenElisServerJsonResponse.mockImplementation(
      (url, data, callback) => {
        callback({
          status: 409,
          error: "Room name must be unique",
        });
      },
    );

    renderWithIntl(
      <StorageLocationModal
        open={true}
        locationType="room"
        mode="create"
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    await screen.findByTestId("storage-location-modal");
    const nameInput = document.getElementById("room-name");
    await userEvent.type(nameInput, "Duplicate Room", { delay: 0 });

    const saveButton = await screen.findByTestId(
      "storage-location-save-button",
    );
    await userEvent.click(saveButton);

    await screen.findByTestId("storage-location-error");
    await screen.findByText(/room name must be unique/i);
  });

  /**
   * T033: Test renders modal for Device edit mode
   */
  test("testStorageLocationModal_RendersForDevice_EditMode", async () => {
    const mockDevice = {
      id: "2",
      name: "Freezer Unit 1",
      code: "FRZ01",
      type: "freezer",
      ipAddress: "192.168.1.100",
      port: 502,
      communicationProtocol: "BACnet",
      parentRoom: mockParentRoom,
    };

    renderWithIntl(
      <StorageLocationModal
        open={true}
        locationType="device"
        mode="edit"
        location={mockDevice}
        onClose={mockOnClose}
        onSave={mockOnSave}
      />,
    );

    await screen.findByTestId("storage-location-modal");
    const nameInput = document.getElementById("device-name");
    expect(nameInput.value).toBe("Freezer Unit 1");

    const ipInput = document.getElementById("device-ip-address");
    expect(ipInput.value).toBe("192.168.1.100");

    const portInput = document.getElementById("device-port");
    expect(portInput.value).toBe("502");
  });
});
