import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/contexts";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import CorrectiveActions from "./CorrectiveActions";

vi.mock("./api", () => ({
  fetchCorrectiveActions: vi.fn(() => Promise.resolve([])),
  createCorrectiveAction: vi.fn(),
  updateCorrectiveAction: vi.fn(),
  completeCorrectiveAction: vi.fn(),
  retractCorrectiveAction: vi.fn(),
  fetchDevices: vi.fn(() =>
    Promise.resolve([{ id: 1, name: "Freezer A", active: true }]),
  ),
  fetchUsers: vi.fn(() => Promise.resolve([])),
  fetchLocations: vi.fn(() => Promise.resolve([])),
  createDevice: vi.fn(),
  createRoom: vi.fn(),
}));

const renderFor = (roles) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: { roles, userId: "1" } }}
      >
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <CorrectiveActions />
        </NotificationContext.Provider>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

const openLogModal = async () => {
  fireEvent.click(
    await screen.findByRole("button", { name: "Add New Action" }),
  );
};

const CREATE_DEVICE_LINK = "Don't see your device? Create a new one";

/**
 * POST /rest/coldstorage/devices requires ADMIN, so the create-device link
 * inside the Log Corrective Action modal must not be offered to Reception -
 * the role the /FreezerMonitoring route is built around.
 */
describe("CorrectiveActions create-device link by role", () => {
  it("hides the create-device link from a non-admin", async () => {
    renderFor(["Reception"]);
    await openLogModal();

    expect(screen.queryByText(CREATE_DEVICE_LINK)).not.toBeInTheDocument();
  });

  it("shows the create-device link to a global administrator", async () => {
    renderFor(["Global Administrator"]);
    await openLogModal();

    expect(await screen.findByText(CREATE_DEVICE_LINK)).toBeInTheDocument();
  });
});
