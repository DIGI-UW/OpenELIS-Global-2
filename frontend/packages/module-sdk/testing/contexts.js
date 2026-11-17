/**
 * Double for `openelis_core/contexts`: the three contexts with the shape the
 * shell provides, defaulting to an administrator session with an empty
 * configuration, so a page renders without a provider. A test that needs other
 * values wraps the page in `<ConfigurationContext.Provider value={...}>`.
 */
import { createContext } from "react";

export const testSession = {
  authenticated: true,
  userId: "1",
  loginName: "admin",
  firstName: "Open",
  lastName: "ELIS",
  roles: ["Global Administrator"],
  permissions: [],
};

/** The shape App.jsx provides (UserSessionContextValue in the declarations). */
export const UserSessionDetailsContext = createContext({
  userSessionDetails: testSession,
  errorLoadingSessionDetails: false,
  isCheckingLogin: () => false,
  logout: () => {},
  refresh: async () => {},
});

export const testLocale = {
  id: "1",
  localeCode: "en",
  displayName: "English",
  active: true,
  fallback: true,
  sortOrder: 1,
};

/** The shape Layout provides (ConfigurationContextValue in the declarations). */
export const ConfigurationContext = createContext({
  configurationProperties: {
    DEFAULT_DATE_LOCALE: "en-US",
    DEFAULT_LANG_LOCALE: "en",
    currentDateAsText: "01/01/2026",
    currentTimeAsText: "08:00",
  },
  reloadConfiguration: () => {},
  supportedLocales: [testLocale],
  enabledLanguages: { en: { label: "English", fallback: true } },
});

export const NotificationContext = createContext({
  notificationVisible: false,
  setNotificationVisible: () => {},
  notifications: [],
  addNotification: () => {},
  removeNotification: () => {},
});
