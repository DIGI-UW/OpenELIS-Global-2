import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import { getFromOpenElisServer } from "../../../utils/Utils";
import { createQueryClient } from "../../../utils/queryClient";
import {
  NotificationContext,
  ConfigurationContext,
} from "../../../layout/contexts";
import UserAddModify from "../UserAddModify";

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  const getFromOpenElisServer = vi.fn();
  return {
    ...actual,
    getFromOpenElisServer,
    fetchFromOpenElisServer: vi.fn(
      (url) =>
        new Promise((resolve, reject) =>
          getFromOpenElisServer(url, (response) =>
            response === undefined
              ? reject(new Error("read failed"))
              : resolve(response),
          ),
        ),
    ),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const user = (loginName) => ({
  systemUser: { id: "5", loginName, firstName: "Ada", lastName: "Lovelace" },
  loginUserId: "5",
  userLoginName: loginName,
  selectedTestSectionLabUnits: {},
  selectedRoles: [],
  systemUserLabUnitRoles: { labUnitRoleMap: [] },
  roles: [],
  testSectionsLabUnits: [],
});

describe("UserAddModify", () => {
  let assign;

  const renderAt = (path) =>
    render(
      <MemoryRouter initialEntries={[path]}>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <ConfigurationContext.Provider
              value={{
                reloadConfiguration: vi.fn(),
                configurationProperties: {},
              }}
            >
              <NotificationContext.Provider
                value={{
                  notificationVisible: false,
                  setNotificationVisible: vi.fn(),
                  addNotification: vi.fn(),
                }}
              >
                <Route path="/MasterListsPage/userManagement/:ID?">
                  <UserAddModify />
                </Route>
                <Route exact path="/MasterListsPage/userManagement">
                  <div>user list</div>
                </Route>
              </NotificationContext.Provider>
            </ConfigurationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/UnifiedSystemUser"))
        return callback(user("ada"));
      if (url.startsWith("/rest/users")) return callback([]);
      return callback(undefined);
    });
    assign = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload: vi.fn(), assign },
    });
  });

  it("goes back to the user list without leaving the app", async () => {
    renderAt("/MasterListsPage/userManagement/5");
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Exit" })).toBeInTheDocument(),
    );

    await userEvent.click(screen.getByRole("button", { name: "Exit" }));

    expect(await screen.findByText("user list")).toBeInTheDocument();
    // Leaving used to download the whole app again to reach a route the router
    // already serves.
    expect(assign).not.toHaveBeenCalled();
  });
});
