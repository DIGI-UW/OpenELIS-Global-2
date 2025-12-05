import React from "react";
import { render, screen } from "@testing-library/react";
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
      } else if (url.includes("/menu/menu_")) {
        callback({
          menu: [
            {
              elementId: "menu_home",
              displayKey: "banner.menu.home",
              url: "/Dashboard",
            },
            {
              elementId: "menu_sample",
              displayKey: "sidenav.label.sample",
              url: "/SamplePatientEntry",
            },
          ],
        });
      } else if (url.includes("/notification/pnconfig")) {
        callback({ subscribed: false });
      } else if (url.includes("/notification")) {
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
    const consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => {});

    render(
      <BrowserRouter>
        <IntlProvider locale="en" messages={messages}>
          <UserSessionDetailsContext.Provider value={{ userSessionDetails: mockUserSessionDetails }}>
            <Layout onChangeLanguage={jest.fn()}>
              <div>Test Content</div>
            </Layout>
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </BrowserRouter>
    );

    // Wait for content to render
    await waitFor(
      () => {
        expect(screen.getByText("Test Content")).toBeInTheDocument();
      },
      { timeout: 2000 }
    );

    // CRITICAL: Check for infinite loop warning
    const infiniteLoopErrors = consoleErrorSpy.mock.calls.filter(
      (call) =>
        call[0] &&
        (call[0].includes("Maximum update depth") ||
          call[0].includes("Too many re-renders"))
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
          <UserSessionDetailsContext.Provider value={{ userSessionDetails: mockUserSessionDetails }}>
            <Layout onChangeLanguage={jest.fn()}>
              <div>Test Content</div>
            </Layout>
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </BrowserRouter>
    );

    // CRITICAL: SideNav must be present when authenticated
    await waitFor(
      () => {
        const sideNav = container.querySelector(".cds--side-nav");
        expect(sideNav).toBeTruthy();
      },
      { timeout: 2000 }
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
          <UserSessionDetailsContext.Provider value={{ userSessionDetails: mockUserSessionDetails }}>
            <Layout onChangeLanguage={jest.fn()}>
              <div>Test Content</div>
            </Layout>
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </BrowserRouter>
    );

    // CRITICAL: Navigation items must render
    await waitFor(
      () => {
        const homeLink = container.querySelector('[href="/Dashboard"]');
        expect(homeLink).toBeTruthy();
      },
      { timeout: 2000 }
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
          <UserSessionDetailsContext.Provider value={{ userSessionDetails: mockUserSessionDetails }}>
            <Layout onChangeLanguage={jest.fn()}>
              <div>Test Content</div>
            </Layout>
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </BrowserRouter>
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
      { timeout: 2000 }
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
          <UserSessionDetailsContext.Provider value={{ userSessionDetails: mockUserSessionDetails }}>
            <Layout onChangeLanguage={jest.fn()}>
              <div>Test Content</div>
            </Layout>
          </UserSessionDetailsContext.Provider>
        </IntlProvider>
      </BrowserRouter>
    );

    // SideNav should NOT be present when not authenticated
    await waitFor(
      () => {
        const sideNav = container.querySelector(".cds--side-nav");
        expect(sideNav).toBeFalsy();
      },
      { timeout: 1000 }
    );
  });
});

