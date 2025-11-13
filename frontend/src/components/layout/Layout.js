import React, {
  createContext,
  useState,
  useEffect,
  useContext,
  useCallback,
  useMemo,
} from "react";
import Header from "./Header";
import Footer from "./Footer";
import { Content, Theme } from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../utils/Utils";

export const ConfigurationContext = createContext(null);
export const NotificationContext = createContext(null);
export const ThemePreferenceContext = createContext({
  theme: "white",
  setTheme: () => undefined,
  toggleTheme: () => undefined,
  isDarkMode: false,
});

const CARBON_THEMES = new Set(["white", "g10", "g90", "g100"]);
const THEME_STORAGE_KEY = "openelis:preferred-theme";

const getInitialTheme = () => {
  if (typeof window === "undefined") {
    return "white";
  }

  const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
  if (storedTheme && CARBON_THEMES.has(storedTheme)) {
    return storedTheme;
  }

  const prefersDark = window.matchMedia?.(
    "(prefers-color-scheme: dark)",
  )?.matches;
  return prefersDark ? "g90" : "white";
};

export default function Layout(props) {
  const { children } = props;
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [resetConfig, setResetConfig] = useState(false);
  const [configurationProperties, setConfigurationProperties] = useState({});
  const [notificationVisible, setNotificationVisible] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [theme, setTheme] = useState(getInitialTheme);

  const addNotification = (notificationBody) => {
    setNotifications([...notifications, notificationBody]);
  };

  const removeNotification = (index) => {
    const newNotifications = [...notifications];
    newNotifications.splice(index, 1);
    setNotifications(newNotifications);
  };

  const fetchConfigurationProperties = (res) => {
    setConfigurationProperties(res);
  };

  useEffect(() => {
    if (userSessionDetails.authenticated) {
      getFromOpenElisServer(
        "/rest/configuration-properties",
        fetchConfigurationProperties,
      );
    } else {
      getFromOpenElisServer(
        "/rest/open-configuration-properties",
        fetchConfigurationProperties,
      );
    }
    setResetConfig(false);
  }, [userSessionDetails.authenticated, resetConfig]);

  const setThemePreference = useCallback((nextTheme) => {
    const resolvedTheme = CARBON_THEMES.has(nextTheme) ? nextTheme : "white";
    setTheme(resolvedTheme);
    if (typeof window !== "undefined") {
      window.localStorage.setItem(THEME_STORAGE_KEY, resolvedTheme);
    }
  }, []);

  const toggleTheme = useCallback(() => {
    setThemePreference(theme === "g90" ? "white" : "g90");
  }, [setThemePreference, theme]);

  useEffect(() => {
    if (typeof document !== "undefined") {
      document.documentElement.setAttribute("data-carbon-theme", theme);
    }
  }, [theme]);

  const themeContextValue = useMemo(
    () => ({
      theme,
      setTheme: setThemePreference,
      toggleTheme,
      isDarkMode: theme === "g90",
    }),
    [theme, setThemePreference, toggleTheme],
  );

  return (
    <ThemePreferenceContext.Provider value={themeContextValue}>
      <Theme theme={theme}>
        <ConfigurationContext.Provider
          value={{
            configurationProperties: configurationProperties,
            reloadConfiguration: () => {
              setResetConfig(true);
            },
          }}
        >
          <NotificationContext.Provider
            value={{
              notificationVisible,
              setNotificationVisible,
              notifications,
              addNotification,
              removeNotification,
            }}
          >
            <div className="d-flex flex-column min-vh-100">
              <Header onChangeLanguage={props.onChangeLanguage} />
              <Content>{children}</Content>
              <Footer />
            </div>
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </Theme>
    </ThemePreferenceContext.Provider>
  );
}
