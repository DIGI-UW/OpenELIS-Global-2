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

export const UserSessionDetailsContext = createContext({
  userSessionDetails: testSession,
  setUserSessionDetails: () => {},
});

export const ConfigurationContext = createContext({
  configurationProperties: {
    DEFAULT_DATE_LOCALE: "en",
    DEFAULT_LANG_LOCALE: "en",
    currentDateAsText: "01/01/2026",
    currentTimeAsText: "08:00",
  },
  reloadConfiguration: () => {},
  supportedLocales: ["en"],
  enabledLanguages: [{ localeCode: "en", displayName: "English" }],
});

export const NotificationContext = createContext({
  notificationVisible: false,
  setNotificationVisible: () => {},
  notifications: [],
  addNotification: () => {},
  removeNotification: () => {},
});
