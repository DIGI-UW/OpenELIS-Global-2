import React, {
  createContext,
  useState,
  useEffect,
  useContext,
  useCallback,
} from "react";
import { useLocation } from "react-router-dom";
import Header from "./Header";
import Footer from "./Footer";
import { Content, Theme } from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../utils/Utils";
import {
  languages as defaultLanguages,
  buildLanguagesFromConfig,
} from "../../languages";

export const ConfigurationContext = createContext(null);
export const NotificationContext = createContext(null);

const isAdminNavRoute = (pathname) =>
  pathname === "/admin" ||
  pathname.startsWith("/admin/") ||
  pathname === "/MasterListsPage" ||
  pathname.startsWith("/MasterListsPage/");

// Must match the .content-nav-locked media query in Style.css
const DESKTOP_MEDIA_QUERY = "(min-width: 1024px)";

/**
 * True when the viewport is desktop-sized.
 * On desktop the sidenav is always rendered; below it collapses into a
 * hamburger-opened overlay drawer.
 */
function useIsDesktop() {
  const [isDesktop, setIsDesktop] = useState(
    () => window.matchMedia(DESKTOP_MEDIA_QUERY).matches,
  );

  useEffect(() => {
    const mediaQuery = window.matchMedia(DESKTOP_MEDIA_QUERY);
    const handler = (e) => setIsDesktop(e.matches);
    mediaQuery.addEventListener("change", handler);
    return () => mediaQuery.removeEventListener("change", handler);
  }, []);

  return isDesktop;
}

export default function Layout(props) {
  const { children } = props;
  const location = useLocation();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [resetConfig, setResetConfig] = useState(false);
  const [configurationProperties, setConfigurationProperties] = useState({});
  const [notificationVisible, setNotificationVisible] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [supportedLocales, setSupportedLocales] = useState([]);
  const [enabledLanguages, setEnabledLanguages] = useState(defaultLanguages);

  // Determine layout config from props or route-based fallbacks
  const isStorageContext =
    location.pathname.startsWith("/Storage") ||
    location.pathname.startsWith("/FreezerMonitoring");

  const isAnalyzerContext =
    location.pathname.startsWith("/analyzers") ||
    location.pathname.startsWith("/AnalyzerManagement");
  const isAdminContext = isAdminNavRoute(location.pathname);
  const navContext = isAdminContext ? "admin" : "main";

  // Used by Header to persist per-context menu expansion state
  const storageKeyPrefix = isAdminContext
    ? "admin"
    : isStorageContext
      ? "storage"
      : isAnalyzerContext
        ? "analyzer"
        : "main";

  // Nav is always rendered on desktop; below the breakpoint it's an
  // ephemeral hamburger-opened drawer, closed on navigation.
  const isDesktop = useIsDesktop();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const navOpen = isDesktop || drawerOpen;

  const closeSideNav = useCallback(() => setDrawerOpen(false), []);

  useEffect(() => {
    closeSideNav();
  }, [location.pathname, closeSideNav]);

  // Only push content when sidenav is actually present (authenticated UX).
  // Unauthenticated pages like /login have no sidenav to make room for.
  const isLocked = userSessionDetails.authenticated && isDesktop;

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

  const loadConfigurationProperties = useCallback(
    (afterLoad) => {
      const handleConfigurationProperties = (res) => {
        fetchConfigurationProperties(res);
        if (afterLoad) {
          afterLoad();
        }
      };

      if (userSessionDetails.authenticated) {
        getFromOpenElisServer(
          "/rest/configuration-properties",
          handleConfigurationProperties,
        );
      } else {
        getFromOpenElisServer(
          "/rest/open-configuration-properties",
          handleConfigurationProperties,
        );
      }
    },
    [userSessionDetails.authenticated],
  );

  useEffect(() => {
    loadConfigurationProperties();
  }, [loadConfigurationProperties]);

  useEffect(() => {
    if (!resetConfig) {
      return;
    }
    loadConfigurationProperties(() => setResetConfig(false));
  }, [loadConfigurationProperties, resetConfig]);

  // Fetch supported locales from backend
  useEffect(() => {
    getFromOpenElisServer("/rest/supportedlocales/active", (response) => {
      if (response && Array.isArray(response)) {
        setSupportedLocales(response);
        const builtLanguages = buildLanguagesFromConfig(response);
        setEnabledLanguages(builtLanguages);
      }
    });
  }, []);

  return (
    <ConfigurationContext.Provider
      value={{
        configurationProperties: configurationProperties,
        reloadConfiguration: () => {
          setResetConfig(true);
        },
        supportedLocales: supportedLocales,
        enabledLanguages: enabledLanguages,
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
          <Header
            onChangeLanguage={props.onChangeLanguage}
            navOpen={navOpen}
            isDesktop={isDesktop}
            toggleSideNav={() => setDrawerOpen((open) => !open)}
            closeSideNav={closeSideNav}
            storageKeyPrefix={storageKeyPrefix}
            navContext={navContext}
          />
          {/* Theme wrapper creates white theme zone for content area */}
          {/* Global SCSS theme = blue header/nav, this = light content */}
          <Theme theme="white">
            <Content
              data-testid="content-wrapper"
              className={`${isLocked ? "content-nav-locked" : ""}${
                isAdminContext ? " content-admin-context" : ""
              }`.trim()}
            >
              {children}
            </Content>
          </Theme>
          <Footer />
        </div>
      </NotificationContext.Provider>
    </ConfigurationContext.Provider>
  );
}
