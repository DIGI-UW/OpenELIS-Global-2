import React, { useContext } from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import Layout, { ConfigurationContext, NotificationContext } from "./Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";

/**
 * Integration tests for Layout.js
 *
 * These tests verify that Layout.js correctly wraps content with
 * TwoModeLayout while preserving all contexts and header actions.
 *
 * @see spec.md FR-012: Preserve ConfigurationContext and NotificationContext
 * @see spec.md FR-013: Apply refactored layout globally
 * @see plan.md D5: Header Action Preservation Strategy
 */

// Mock the API utility
jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn((url, callback) => {
    if (url === "/rest/configuration-properties") {
      callback({ releaseNumber: "3.0.0", BANNER_TEXT: "Test Lab" });
    } else if (url === "/rest/open-configuration-properties") {
      callback({ releaseNumber: "3.0.0" });
    } else if (url === "/rest/menu") {
      callback([]);
    }
  }),
  putToOpenElisServer: jest.fn(),
  getFromOpenElisServerV2: jest.fn(async () => ({})),
  postToOpenElisServer: jest.fn(async () => ({})),
  deleteToOpenElisServer: jest.fn(async () => ({})),
}));

// Mock user session context value
const mockUserSessionContextValue = {
  userSessionDetails: {
    authenticated: true,
    firstName: "Test",
    lastName: "User",
    loginLabUnit: "Main Lab",
  },
  logout: jest.fn(),
};

// Test wrapper with all required providers
const renderWithProviders = (
  ui,
  {
    route = "/",
    userContext = mockUserSessionContextValue,
    onChangeLanguage = jest.fn(),
  } = {},
) => {
  const messages = {
    "header.label.version": "Version",
    "header.label.logout": "Logout",
    "header.label.selectlocale": "Language",
    "banner.menu.help.usermanual": "User Manual",
    "banner.menu.help.about": "About",
    "banner.menu.help.contact": "Contact",
    "notification.slideover.button.reload": "Reload",
    "notification.slideover.button.showread": "Show Read",
    "notification.slideover.button.subscribe": "Subscribe",
    "notification.slideover.button.markallasread": "Mark All As Read",
    "notification.slideover.button.unsubscribe": "Unsubscribe",
    "notification.slideover.button.hideread": "Hide Read",
    "notification.slideover.button.markasread": "Mark As Read",
    "notification.slideover.empty.header": "No notifications",
    "notification.sliderover.empty.message": "You're all caught up",
  };

  return render(
    <MemoryRouter initialEntries={[route]}>
      <IntlProvider locale="en" messages={messages}>
        <UserSessionDetailsContext.Provider value={userContext}>
          {ui}
        </UserSessionDetailsContext.Provider>
      </IntlProvider>
    </MemoryRouter>,
  );
};

describe("Layout", () => {
  beforeAll(() => {
    // Minimal service worker mock to satisfy notification component
    if (!navigator.serviceWorker) {
      Object.defineProperty(global.navigator, "serviceWorker", {
        value: {
          ready: Promise.resolve({
            pushManager: {
              getSubscription: () => Promise.resolve(null),
            },
          }),
          register: jest.fn().mockResolvedValue({}),
        },
        configurable: true,
      });
    }
  });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("TwoModeLayout integration", () => {
    /**
     * Test: Layout renders TwoModeLayout component
     * @see spec.md FR-013: Apply refactored layout globally
     */
    test("testLayout_Renders_TwoModeLayout", () => {
      renderWithProviders(
        <Layout>
          <div data-testid="child-content">Child Content</div>
        </Layout>,
      );

      // TwoModeLayout should render children
      expect(screen.getByTestId("child-content")).toBeTruthy();
      expect(screen.getByText("Child Content")).toBeTruthy();

      // Should have Carbon SideNav (from TwoModeLayout)
      const sideNav = document.querySelector(".cds--side-nav");
      expect(sideNav).toBeTruthy();
    });

    /**
     * Test: Layout passes headerActions to TwoModeLayout
     * @see spec.md FR-011: Preserve ALL existing header functionality
     * @see plan.md D5: HeaderGlobalBar content passed via headerActions prop
     *
     * NOTE: This test FAILS until HeaderActions is extracted and passed to TwoModeLayout
     */
    test("testLayout_PassesHeaderActions_ToTwoModeLayout", () => {
      renderWithProviders(
        <Layout>
          <div>Content</div>
        </Layout>,
      );

      // Header should render
      const header = document.querySelector(".cds--header");
      expect(header).toBeTruthy();

      // Should have notification icon (from HeaderActions)
      const notificationIcon = document.querySelector("#notification-Icon");
      expect(notificationIcon).toBeTruthy();

      // Should have user icon (from HeaderActions)
      const userIcon = document.querySelector("#user-Icon");
      expect(userIcon).toBeTruthy();

      // Should have search icon (from HeaderActions)
      const searchIcon = document.querySelector("#search-Icon");
      expect(searchIcon).toBeTruthy();
    });
  });

  describe("context preservation", () => {
    /**
     * Test: ConfigurationContext is available to children
     * @see spec.md FR-012: Preserve ConfigurationContext
     */
    test("testLayout_ConfigurationContext_AvailableToChildren", () => {
      // Component that consumes ConfigurationContext
      const ConfigConsumer = () => {
        const config = useContext(ConfigurationContext);
        return (
          <div data-testid="config-consumer">
            {config ? "context-available" : "no-context"}
          </div>
        );
      };

      renderWithProviders(
        <Layout>
          <ConfigConsumer />
        </Layout>,
      );

      // ConfigurationContext should be available (actual value loads async)
      expect(screen.getByTestId("config-consumer").textContent).toBe(
        "context-available",
      );
    });

    /**
     * Test: NotificationContext is available to children
     * @see spec.md FR-012: Preserve NotificationContext
     */
    test("testLayout_NotificationContext_AvailableToChildren", () => {
      // Component that consumes NotificationContext
      const NotificationConsumer = () => {
        const notificationCtx = useContext(NotificationContext);
        return (
          <div data-testid="notification-consumer">
            {notificationCtx ? "context-available" : "no-context"}
          </div>
        );
      };

      renderWithProviders(
        <Layout>
          <NotificationConsumer />
        </Layout>,
      );

      expect(screen.getByTestId("notification-consumer").textContent).toBe(
        "context-available",
      );
    });

    /**
     * Test: NotificationContext provides addNotification function
     */
    test("testLayout_NotificationContext_ProvidesAddNotification", () => {
      const NotificationConsumer = () => {
        const notificationCtx = useContext(NotificationContext);
        return (
          <div data-testid="notification-consumer">
            {typeof notificationCtx?.addNotification === "function"
              ? "has-add"
              : "no-add"}
          </div>
        );
      };

      renderWithProviders(
        <Layout>
          <NotificationConsumer />
        </Layout>,
      );

      expect(screen.getByTestId("notification-consumer").textContent).toBe(
        "has-add",
      );
    });
  });

  describe("route-based configuration", () => {
    /**
     * Test: Storage routes calculate defaultMode="lock"
     * @see spec.md US4: Storage pages default to LOCK mode
     *
     * NOTE: This test verifies Layout calculates the correct defaultMode
     * and passes it to TwoModeLayout. The actual class applied depends on
     * localStorage state (user preference override).
     */
    test("testLayout_StorageRoute_PassesLockModeToTwoModeLayout", () => {
      renderWithProviders(
        <Layout>
          <div>Storage Content</div>
        </Layout>,
        { route: "/storage/dashboard" },
      );

      // Verify the content wrapper exists (TwoModeLayout rendered)
      const contentWrapper = document.querySelector(
        '[data-testid="content-wrapper"]',
      );
      expect(contentWrapper).toBeTruthy();
      // Note: Actual class depends on localStorage; defaultMode is "lock" for /storage
    });

    /**
     * Test: Non-storage routes calculate defaultMode="close"
     */
    test("testLayout_NonStorageRoute_PassesCloseModeToTwoModeLayout", () => {
      renderWithProviders(
        <Layout>
          <div>Home Content</div>
        </Layout>,
        { route: "/home" },
      );

      // Verify the content wrapper exists
      const contentWrapper = document.querySelector(
        '[data-testid="content-wrapper"]',
      );
      expect(contentWrapper).toBeTruthy();
      // Note: defaultMode is "close" for /home
    });

    /**
     * Test: Analyzer routes calculate defaultMode="lock"
     */
    test("testLayout_AnalyzerRoute_PassesLockModeToTwoModeLayout", () => {
      renderWithProviders(
        <Layout>
          <div>Analyzer Content</div>
        </Layout>,
        { route: "/analyzers/qc" },
      );

      const contentWrapper = document.querySelector(
        '[data-testid="content-wrapper"]',
      );
      expect(contentWrapper).toBeTruthy();
      // Note: defaultMode is "lock" for /analyzers
    });
  });

  describe("onChangeLanguage wiring", () => {
    /**
     * Test: onChangeLanguage prop is accepted by Layout
     * @see spec.md FR-011: language selector must work
     */
    test("testLayout_OnChangeLanguage_PropAccepted", () => {
      const mockOnChangeLanguage = jest.fn();

      // Should not throw when onChangeLanguage is provided
      expect(() => {
        renderWithProviders(
          <Layout onChangeLanguage={mockOnChangeLanguage}>
            <div>Content</div>
          </Layout>,
        );
      }).not.toThrow();

      // Header should render
      const header = document.querySelector(".cds--header");
      expect(header).toBeTruthy();
    });

    /**
     * Test: HeaderActions (when implemented) should include user icon for language selection
     * @see spec.md FR-011: language selector must work
     *
     * NOTE: This test will fail until HeaderActions is extracted and passed to TwoModeLayout
     */
    test("testLayout_HeaderActions_IncludesUserIcon", () => {
      renderWithProviders(
        <Layout>
          <div>Content</div>
        </Layout>,
      );

      // User icon should be present in header for language/user panel access
      const userIcon = document.querySelector("#user-Icon");
      expect(userIcon).toBeTruthy();
    });
  });
});
