import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/contexts";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import DeviceManagement from "./DeviceManagement";
import { fetchLocations, createDevice } from "../api";

vi.mock("../api", () => ({
  fetchDevices: vi.fn(() =>
    Promise.resolve([
      { id: 1, name: "Freezer A", active: true, protocol: "TCP" },
    ]),
  ),
  fetchLocations: vi.fn(() => Promise.resolve([{ id: 7, name: "Cold Room" }])),
  createDevice: vi.fn(),
  updateDevice: vi.fn(),
  toggleDeviceStatus: vi.fn(),
  deleteDevice: vi.fn(),
  createRoom: vi.fn(),
}));

const renderFor = (roles) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: { roles } }}
      >
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <DeviceManagement />
        </NotificationContext.Provider>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

/**
 * The device writes on FreezerDeviceController require ADMIN, while the route
 * gate on /FreezerMonitoring also admits Reception, so Reception must reach a
 * read-only Device Management rather than controls that fail on click.
 */
describe("DeviceManagement write controls by role", () => {
  const writeControlNames = [
    "Add New Device",
    "Edit device",
    "Deactivate device",
    "Delete Device",
  ];

  it("hides add, edit, toggle and delete from a non-admin", async () => {
    renderFor(["Reception"]);

    expect(await screen.findByText("Freezer A")).toBeInTheDocument();
    writeControlNames.forEach((name) => {
      expect(screen.queryByRole("button", { name })).not.toBeInTheDocument();
    });
    expect(
      screen.queryByRole("columnheader", { name: "Actions" }),
    ).not.toBeInTheDocument();
  });

  it("shows them to a global administrator", async () => {
    renderFor(["Global Administrator"]);

    expect(await screen.findByText("Freezer A")).toBeInTheDocument();
    writeControlNames.forEach((name) => {
      expect(screen.getByRole("button", { name })).toBeInTheDocument();
    });
    expect(
      screen.getByRole("columnheader", { name: "Actions" }),
    ).toBeInTheDocument();
  });
});

/**
 * AddDeviceModal renders locations.map even while closed, so a parsed error
 * body from the rooms fetch would throw out of the whole Settings tree.
 */
describe("DeviceManagement location fetch payloads", () => {
  it("renders the device table when the rooms fetch resolves a non-array", async () => {
    fetchLocations.mockResolvedValueOnce({ status: 400, error: "Bad Request" });

    renderFor(["Global Administrator"]);

    expect(await screen.findByText("Freezer A")).toBeInTheDocument();
  });
});

/**
 * The register is optional, and the modal reports "not configured" as null.
 * Sending 0 instead makes every poll read the temperature register a second
 * time and publish its raw contents as a humidity percentage.
 */
describe("DeviceManagement humidity register", () => {
  it("sends a null humidityRegister for a device created without one", async () => {
    renderFor(["Global Administrator"]);
    expect(await screen.findByText("Freezer A")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Add New Device" }));
    fireEvent.change(screen.getByLabelText("Device Name *"), {
      target: { value: "Ultra-low 1" },
    });
    fireEvent.change(screen.getByLabelText("Room/Facility *"), {
      target: { value: "7" },
    });
    fireEvent.change(screen.getByLabelText("IP Address/Host *"), {
      target: { value: "10.0.0.9" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create" }));

    expect(createDevice).toHaveBeenCalledTimes(1);
    expect(createDevice.mock.calls[0][0].humidityRegister).toBeNull();
  });
});
