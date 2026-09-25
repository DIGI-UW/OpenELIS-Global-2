import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { vi } from "vitest";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import Layout from "./Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import messages from "../../languages/en.json";
import { getFromOpenElisServer } from "../utils/Utils";

// Mock Utils
// Stub only the network calls. The rest of Utils must stay real: the sidebar
// now asks menuSubtreeVisible which rows this user may open, and a bare factory
// mock would drop that export (and ROUTE_PRIVILEGES with it), failing the
// render before any assertion here runs.
vi.mock("../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
  getFromOpenElisServerV2: vi.fn().mockResolvedValue({}),
}));

describe("Layout Full Integration (Smoke Tests)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    const mockGetFromServer = getFromOpenElisServer;

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

  // Privilege-based RBAC: the sidebar hides rows the user could not open (see
  // menuSubtreeVisible / ROUTE_PRIVILEGES in Utils.ts), so a session fixture
  // listing roles but no privileges describes a user who can reach nothing, and
  // these specs would assert against an empty nav instead of the rendering
  // behaviour they are about. The privilege list below is deliberately broad for
  // that reason; menuRouteGuards.test.js is where the filtering itself is tested.
  test("CRITICAL: renders without infinite loop when authenticated", async () => {
    const mockUserSessionDetails = {
      authenticated: true,
      roles: ["ROLE_USER"],

      privileges: [
        "order:create",
        "order:view",
        "order:edit",
        "patient:view",
        "result:enter",
        "result:view",
        "result:validate",
        "storage:view",
        "system:configure",
        "catalogue:view",
        "report:run",
      ],
      userId: "1",
    };

    // Spy on console.error to catch infinite loop warnings
    const consoleErrorSpy = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});

    const { container } = render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: mockUserSessionDetails }}
          >
            <Layout onChangeLanguage={vi.fn()}>
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

      privileges: [
        "order:create",
        "order:view",
        "order:edit",
        "patient:view",
        "result:enter",
        "result:view",
        "result:validate",
        "storage:view",
        "system:configure",
        "catalogue:view",
        "report:run",
      ],
      userId: "1",
    };

    const { container } = render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: mockUserSessionDetails }}
          >
            <Layout onChangeLanguage={vi.fn()}>
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

      privileges: [
        "order:create",
        "order:view",
        "order:edit",
        "patient:view",
        "result:enter",
        "result:view",
        "result:validate",
        "storage:view",
        "system:configure",
        "catalogue:view",
        "report:run",
      ],
      userId: "1",
    };

    render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: mockUserSessionDetails }}
          >
            <Layout onChangeLanguage={vi.fn()}>
              <div>Test Content</div>
            </Layout>
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </BrowserRouter>,
    );

    const homeLink = await screen.findByRole("link", { name: "Home" });
    expect(homeLink).toHaveAttribute("href", "/Dashboard");
  });

  test("CRITICAL: header actions render without crash", async () => {
    const mockUserSessionDetails = {
      authenticated: true,
      roles: ["ROLE_USER"],

      privileges: [
        "order:create",
        "order:view",
        "order:edit",
        "patient:view",
        "result:enter",
        "result:view",
        "result:validate",
        "storage:view",
        "system:configure",
        "catalogue:view",
        "report:run",
      ],
      userId: "1",
    };

    const { container } = render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: mockUserSessionDetails }}
          >
            <Layout onChangeLanguage={vi.fn()}>
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

      privileges: [
        "order:create",
        "order:view",
        "order:edit",
        "patient:view",
        "result:enter",
        "result:view",
        "result:validate",
        "storage:view",
        "system:configure",
        "catalogue:view",
        "report:run",
      ],
      userId: null,
    };

    const { container } = render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: mockUserSessionDetails }}
          >
            <Layout onChangeLanguage={vi.fn()}>
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
