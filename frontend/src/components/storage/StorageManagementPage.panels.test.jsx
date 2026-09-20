import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import StorageManagementPage from "./StorageManagementPage";
import { NotificationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import * as Utils from "../utils/Utils";
import messages from "../../languages/en.json";

// Unlike StorageManagementPage.test.jsx this file leaves the panel pages real,
// so the requests an unselected tab would fire are actually observable.
vi.mock("../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

const mockNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  addNotification: vi.fn(),
};

const renderAt = (path) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={mockNotificationContext}>
        <UserSessionDetailsContext.Provider
          value={{
            userSessionDetails: { roles: ["Global Administrator"] },
            logout: vi.fn(),
          }}
        >
          <MemoryRouter initialEntries={[path]}>
            <Route path="/Storage/:resource?">
              <StorageManagementPage />
            </Route>
          </MemoryRouter>
        </UserSessionDetailsContext.Provider>
      </NotificationContext.Provider>
    </IntlProvider>,
  );

const requestedUrls = () =>
  Utils.getFromOpenElisServer.mock.calls.map(([url]) => url);

beforeEach(() => {
  Utils.getFromOpenElisServer.mockReset();
  Utils.getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("location-counts")) {
      cb({ rooms: 2, devices: 3, shelves: 4, racks: 5, boxes: 6 });
    } else {
      cb({ items: [], totalItems: 0 });
    }
  });
});

describe("StorageManagementPage panel mounting", () => {
  it("does not fetch for the tabs it is not showing", async () => {
    renderAt("/Storage");

    await waitFor(() =>
      expect(requestedUrls().some((u) => u.includes("/rooms"))).toBe(true),
    );
    expect(requestedUrls().some((u) => u.includes("/sample-items"))).toBe(
      false,
    );
    expect(requestedUrls().some((u) => u.includes("/inventory-lots"))).toBe(
      false,
    );
  });

  it("fetches lots only once the Inventory Lots tab is the one selected", async () => {
    renderAt("/Storage/inventory-lots");

    await waitFor(() =>
      expect(requestedUrls().some((u) => u.includes("/inventory-lots"))).toBe(
        true,
      ),
    );
    expect(requestedUrls().some((u) => u.includes("/sample-items"))).toBe(
      false,
    );
    expect(requestedUrls().some((u) => u.includes("location-counts"))).toBe(
      false,
    );
    expect(screen.getByRole("tab", { name: "Inventory Lots" })).toHaveAttribute(
      "aria-selected",
      "true",
    );
  });
});
