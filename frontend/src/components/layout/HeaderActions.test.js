import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import HeaderActions from "./HeaderActions";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { ConfigurationContext, NotificationContext } from "./Layout";
import messages from "../../languages/en.json";

// Mock Utils
jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

const renderWithProviders = (component, { authenticated = true } = {}) => {
  const mockUserSessionDetails = {
    authenticated,
    roles: ["ROLE_USER"],
  };

  const mockConfigContext = {
    configurationProperties: {
      releaseNumber: "3.2.1.0",
      BANNER_TEXT: "Test LIMS",
    },
    reloadConfiguration: jest.fn(),
  };

  const mockNotificationContext = {
    notificationVisible: false,
    setNotificationVisible: jest.fn(),
    notifications: [],
    addNotification: jest.fn(),
    removeNotification: jest.fn(),
  };

  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <UserSessionDetailsContext.Provider value={{ userSessionDetails: mockUserSessionDetails }}>
          <ConfigurationContext.Provider value={mockConfigContext}>
            <NotificationContext.Provider value={mockNotificationContext}>
              {component}
            </NotificationContext.Provider>
          </ConfigurationContext.Provider>
        </UserSessionDetailsContext.Provider>
      </IntlProvider>
    </BrowserRouter>
  );
};

describe("HeaderActions integration tests", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Mock successful API response
    require("../utils/Utils").getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/notification/pnconfig")) {
        callback({ subscribed: false });
      } else if (url.includes("/notification")) {
        callback([]);
      }
    });
  });

  test("renders without infinite loop when authenticated", async () => {
    const { container } = renderWithProviders(<HeaderActions onChangeLanguage={jest.fn()} />, {
      authenticated: true,
    });

    // Should render without crashing or infinite loop
    await waitFor(
      () => {
        const searchIcon = container.querySelector("#search-Icon");
        expect(searchIcon).toBeInTheDocument();
      },
      { timeout: 1000 }
    );

    // Verify no infinite re-renders by checking console error wasn't called
    expect(console.error).not.toHaveBeenCalledWith(
      expect.stringContaining("Maximum update depth")
    );
  });

  test("renders user menu icon when not authenticated", async () => {
    const { container } = renderWithProviders(<HeaderActions onChangeLanguage={jest.fn()} />, {
      authenticated: false,
    });

    await waitFor(
      () => {
        const userIcon = container.querySelector("#user-Icon");
        expect(userIcon).toBeInTheDocument();
      },
      { timeout: 1000 }
    );
  });

  test("does not call getNotifications infinitely", async () => {
    const mockGetFromServer = require("../utils/Utils").getFromOpenElisServer;
    
    renderWithProviders(<HeaderActions onChangeLanguage={jest.fn()} />, {
      authenticated: true,
    });

    await waitFor(() => {
      const notificationIcon = document.querySelector("#notification-Icon");
      expect(notificationIcon).toBeInTheDocument();
    });

    // Should be called once for initial load, not continuously
    const callCount = mockGetFromServer.mock.calls.filter(
      ([url]) => url.includes("/notification")
    ).length;
    
    expect(callCount).toBeLessThan(5); // Allow initial + maybe 1-2 retries, but not infinite
  });
});

