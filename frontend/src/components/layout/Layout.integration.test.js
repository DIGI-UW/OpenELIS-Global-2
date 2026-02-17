import React from "react";
import { render } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import Layout from "./Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import messages from "../../languages/en.json";

// Mock Utils
jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

describe("Layout Full Integration (Smoke Tests)", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    const mockGetFromServer = require("../utils/Utils").getFromOpenElisServer;

    mockGetFromServer.mockImplementation((url, callback) => {
      if (url.includes("configuration-properties")) {
        callback({
          releaseNumber: "3.2.1.0",
          BANNER_TEXT: "Test LIMS",
        });
      } else if (url === "/rest/menu") {
        // Realistic menu structure matching database
        callback([
          {
            menu: {
              elementId: "menu_home",
              displayKey: "banner.menu.home",
              actionURL: "/Dashboard",
              isActive: true,
            },
            childMenus: [],
          },
          {
            menu: {
              elementId: "menu_sample",
              displayKey: "banner.menu.sample",
              actionURL: "",
              isActive: true,
            },
            childMenus: [
              {
                menu: {
                  elementId: "menu_sample_add",
                  displayKey: "sidenav.label.addorder",
                  actionURL: "/SamplePatientEntry",
                  isActive: true,
                },
                childMenus: [],
              },
            ],
          },
          {
            menu: {
              elementId: "menu_storage",
              displayKey: "banner.menu.storage",
              actionURL: "",
              isActive: true,
            },
            childMenus: [
              {
                menu: {
                  elementId: "menu_storage_management",
                  displayKey: "storage.nav.dashboard",
                  actionURL: "/Storage",
                  isActive: true,
                },
                childMenus: [],
              },
            ],
          },
          {
            menu: {
              elementId: "menu_admin",
              displayKey: "sidenav.label.admin",
              actionURL: "",
              isActive: true,
            },
            childMenus: [
              {
                menu: {
                  elementId: "menu_admin_usermgt",
                  displayKey: "sidenav.label.admin.usermgt",
                  actionURL: "/MasterListsPage#!usersManagement",
                  isActive: true,
                },
                childMenus: [],
              },
            ],
          },
        ]);
      } else if (url.includes("/notifications")) {
        callback([]);
      }
    });
  });

  test("CRITICAL: renders without infinite loop when authenticated", async () => {
    const mockUserSessionDetails = {
      authenticated: true,
      roles: ["ROLE_USER"],
      userId: "1",
    };

    // Spy on console.error to catch infinite loop warnings
    const consoleErrorSpy = jest
      .spyOn(console, "error")
      .mockImplementation(() => {});

    const { container } = render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: mockUserSessionDetails }}
          >
            <Layout onChangeLanguage={jest.fn()}>
              <div data-testid="test-content">Test Content</div>
            </Layout>
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </BrowserRouter>,
    );

    // Wait for layout to render (Content wrapper should exist)
    await waitFor(
      () => {
        const content = container.querySelector(".cds--content");
        expect(content).toBeTruthy();
      },
      { timeout: 2000 },
    );

    // CRITICAL: Check for infinite loop warning
    const infiniteLoopErrors = consoleErrorSpy.mock.calls.filter(
      (call) =>
        call[0] &&
        typeof call[0] === "string" &&
        (call[0].includes("Maximum update depth") ||
          call[0].includes("Too many re-renders")),
    );

    expect(infiniteLoopErrors.length).toBe(0);

    consoleErrorSpy.mockRestore();
  });

  test("CRITICAL: side navigation renders when authenticated", async () => {
    const mockUserSessionDetails = {
      authenticated: true,
      roles: ["ROLE_USER"],
      userId: "1",
    };

    const { container } = render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: mockUserSessionDetails }}
          >
            <Layout onChangeLanguage={jest.fn()}>
              <div>Test Content</div>
            </Layout>
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </BrowserRouter>,
    );

    // CRITICAL: SideNav must be present when authenticated
    await waitFor(
      () => {
        const sideNav = container.querySelector(".cds--side-nav");
        expect(sideNav).toBeTruthy();
      },
      { timeout: 2000 },
    );
  });

  test("CRITICAL: navigation items render in sidenav when authenticated", async () => {
    const mockUserSessionDetails = {
      authenticated: true,
      roles: ["ROLE_USER"],
      userId: "1",
    };

    const { container } = render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: mockUserSessionDetails }}
          >
            <Layout onChangeLanguage={jest.fn()}>
              <div>Test Content</div>
            </Layout>
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </BrowserRouter>,
    );

    // CRITICAL: Navigation items must render
    await waitFor(
      () => {
        // After refactor: top-level items use onClick instead of href
        // Check by outer span element ID (menu_home, not menu_home_nav)
        const homeLink = container.querySelector("#menu_home");
        expect(homeLink).toBeTruthy();
      },
      { timeout: 2000 },
    );
  });

  test("CRITICAL: header actions render without crash", async () => {
    const mockUserSessionDetails = {
      authenticated: true,
      roles: ["ROLE_USER"],
      userId: "1",
    };

    const { container } = render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: mockUserSessionDetails }}
          >
            <Layout onChangeLanguage={jest.fn()}>
              <div>Test Content</div>
            </Layout>
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </BrowserRouter>,
    );

    // CRITICAL: Header actions (search, notifications, user menu) must render
    await waitFor(
      () => {
        const searchIcon = container.querySelector("#search-Icon");
        const notificationIcon = container.querySelector("#notification-Icon");
        const userIcon = container.querySelector("#user-Icon");

        expect(searchIcon).toBeTruthy();
        expect(notificationIcon).toBeTruthy();
        expect(userIcon).toBeTruthy();
      },
      { timeout: 2000 },
    );
  });

  test("side navigation does NOT render when not authenticated", async () => {
    const mockUserSessionDetails = {
      authenticated: false,
      roles: [],
      userId: null,
    };

    const { container } = render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: mockUserSessionDetails }}
          >
            <Layout onChangeLanguage={jest.fn()}>
              <div>Test Content</div>
            </Layout>
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </BrowserRouter>,
    );

    // SideNav should NOT be present when not authenticated
    await waitFor(
      () => {
        const sideNav = container.querySelector(".cds--side-nav");
        expect(sideNav).toBeFalsy();
      },
      { timeout: 1000 },
    );
  });
});
