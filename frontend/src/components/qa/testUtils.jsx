import React from "react";
import { render } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";
import messages from "../../languages/en.json";
import { createQueryClient } from "../utils/queryClient";
import { NotificationContext } from "../layout/contexts";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";

/**
 * Renders a QA screen inside everything the app gives it: real en.json (so a
 * missing i18n key breaks a text assertion loudly), a router, a session, the
 * notification context and a query cache of its own — a fresh cache per render,
 * so one test never reads another's server data.
 *
 * Options: `entries` (router history), `permissions` / `roles` (session), and
 * `notifications` (overrides for the notification context spies, which are
 * returned so a test can assert on them).
 */
export const renderQa = (
  ui,
  { entries = ["/"], permissions = [], roles = [], notifications } = {},
) => {
  const notificationContext = {
    notifications: [],
    addNotification: vi.fn(),
    removeNotification: vi.fn(),
    setNotificationVisible: vi.fn(),
    notificationVisible: false,
    ...notifications,
  };

  const view = render(
    <IntlProvider locale="en" messages={messages}>
      <QueryClientProvider client={createQueryClient()}>
        <UserSessionDetailsContext.Provider
          value={{
            userSessionDetails: { authenticated: true, roles, permissions },
            errorLoadingSessionDetails: false,
            isCheckingLogin: () => false,
            logout: vi.fn(),
          }}
        >
          <NotificationContext.Provider value={notificationContext}>
            <MemoryRouter initialEntries={entries}>{ui}</MemoryRouter>
          </NotificationContext.Provider>
        </UserSessionDetailsContext.Provider>
      </QueryClientProvider>
    </IntlProvider>,
  );

  return { ...view, notificationContext };
};

export default renderQa;
