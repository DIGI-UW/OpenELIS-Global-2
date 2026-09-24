import React from "react";
import { vi } from "vitest";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/contexts";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import AlertSettings from "./AlertSettings";

vi.mock("../api", () => ({
  fetchAlertConfig: vi.fn(() => Promise.resolve({ alertConfigs: {} })),
  saveAlertConfig: vi.fn(),
}));

// The component now authorises on coldstorage:manage rather than on a role name, so the
// harness derives the privilege the way the backend does: an admin session
// carries it, other roles do not. Test bodies keep naming roles because that is
// how a reader thinks about "admin vs Reception".
const ADMIN_PRIVILEGES = ["coldstorage:manage"];
const privilegesFor = (roles = []) =>
  roles.includes("Global Administrator") ? ADMIN_PRIVILEGES : [];

const renderFor = (roles) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <UserSessionDetailsContext.Provider
        value={{
          userSessionDetails: { roles, privileges: privilegesFor(roles) },
        }}
      >
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <AlertSettings />
        </NotificationContext.Provider>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

const SAVE_BUTTON = "Save Notification Preferences";

/**
 * AlertNotificationConfigRestController is ADMIN-only at class level, so the
 * Save button must not be offered to the other roles the /FreezerMonitoring
 * route admits.
 */
describe("AlertSettings save control by role", () => {
  it("hides the save button from a non-admin", async () => {
    renderFor(["Reception"]);

    expect(await screen.findByText("Alert Configuration")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: SAVE_BUTTON }),
    ).not.toBeInTheDocument();
  });

  it("shows the save button to a global administrator", async () => {
    renderFor(["Global Administrator"]);

    expect(
      await screen.findByRole("button", { name: SAVE_BUTTON }),
    ).toBeInTheDocument();
  });
});
