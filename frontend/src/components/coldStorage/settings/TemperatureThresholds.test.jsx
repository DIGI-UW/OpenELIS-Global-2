import React from "react";
import { vi } from "vitest";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/contexts";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import TemperatureThresholds from "./TemperatureThresholds";
import { fetchDevices, fetchFreezerStatus } from "../api";

vi.mock("../api", () => ({
  fetchDevices: vi.fn(),
  fetchFreezerStatus: vi.fn(),
  updateDeviceThresholds: vi.fn(),
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
          <TemperatureThresholds />
        </NotificationContext.Provider>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

const SAVE_BUTTON = "Save Threshold Configuration";

/**
 * PUT /devices/{id}/thresholds requires ADMIN while the /FreezerMonitoring
 * route also admits Reception, so Reception must reach a read-only panel
 * rather than a Save button that fails on click.
 */
describe("TemperatureThresholds save control by role", () => {
  beforeEach(() => {
    fetchDevices.mockResolvedValue([{ id: 1, name: "Freezer A" }]);
  });

  it("hides the save button from a non-admin", async () => {
    renderFor(["Reception"]);

    expect(await screen.findByText("Freezer A")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: SAVE_BUTTON }),
    ).not.toBeInTheDocument();
  });

  it("shows the save button to a global administrator", async () => {
    renderFor(["Global Administrator"]);

    expect(await screen.findByText("Freezer A")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: SAVE_BUTTON }),
    ).toBeInTheDocument();
  });
});

/**
 * Carbon mounts every Settings TabPanel eagerly, so an array method called on
 * a parsed error body here throws out of the whole /FreezerMonitoring tree.
 */
describe("TemperatureThresholds device fetch payloads", () => {
  it("renders the empty state when the device fetch resolves a non-array", async () => {
    fetchDevices.mockResolvedValue({
      status: 400,
      error: "Bad Request",
    });

    renderFor(["Global Administrator"]);

    expect(
      await screen.findByText("No Devices Configured"),
    ).toBeInTheDocument();
  });
});

/**
 * Humidity alerts fire against the device's assigned threshold profile, which
 * no screen showed (issue #4261).
 */
describe("TemperatureThresholds humidity bands", () => {
  beforeEach(() => {
    fetchDevices.mockResolvedValue([
      { id: 1, name: "Freezer A" },
      { id: 2, name: "Freezer B" },
    ]);
    fetchFreezerStatus.mockResolvedValue([
      {
        freezerId: 1,
        thresholdProfileName: "Ultra-Low",
        humidityWarningMin: 30,
        humidityWarningMax: 70,
        humidityCriticalMin: null,
        humidityCriticalMax: 80,
      },
      { freezerId: 2, thresholdProfileName: null },
    ]);
  });

  it("shows the assigned profile's humidity bands, and says when none is assigned", async () => {
    renderFor(["Reception"]);

    expect(
      await screen.findByText(
        "Humidity alerting (profile Ultra-Low): warning 30% to 70%, critical unset to 80%",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "No threshold profile is assigned, so humidity is not alerted on.",
      ),
    ).toBeInTheDocument();
  });

  it("still renders the devices when the status fetch fails", async () => {
    fetchFreezerStatus.mockRejectedValue(new Error("boom"));

    renderFor(["Reception"]);

    expect(await screen.findByText("Freezer A")).toBeInTheDocument();
  });
});
