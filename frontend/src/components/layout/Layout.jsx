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

// User preference: whether the desktop sidenav is pinned open (pushing
// content) or collapsed into an on-demand overlay drawer.
const NAV_PINNED_STORAGE_KEY = "sideNavPinned";

const readNavPinnedPreference = () => {
  try {
    return localStorage.getItem(NAV_PINNED_STORAGE_KEY) !== "false";
  } catch {
    return true;
  }
};

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

  // Nav on desktop: pinned (default) renders it persistently and pushes
  // content; unpinned turns it into the same hamburger-opened overlay drawer
  // used below the breakpoint, closed on navigation.
  const isDesktop = useIsDesktop();
  const [navPinned, setNavPinned] = useState(readNavPinnedPreference);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const navPersistent = isDesktop && navPinned;
  const navOpen = navPersistent || drawerOpen;

  const closeSideNav = useCallback(() => setDrawerOpen(false), []);

  const toggleNavPinned = useCallback(() => {
    setNavPinned((prev) => {
      const next = !prev;
      try {
        localStorage.setItem(NAV_PINNED_STORAGE_KEY, String(next));
      } catch {
        // preference simply won't survive a reload
      }
      return next;
    });
    // Unpinning keeps the nav visible as a drawer (instead of vanishing
    // mid-interaction); pinning hands visibility back to the persistent nav.
    setDrawerOpen(navPinned);
  }, [navPinned]);

  useEffect(() => {
    closeSideNav();
  }, [location.pathname, closeSideNav]);

  // Only push content when the persistent sidenav is actually present
  // (authenticated desktop UX with the nav pinned). Unauthenticated pages
  // like /login have no sidenav to make room for.
  const isLocked = userSessionDetails.authenticated && navPersistent;

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
            navPinned={navPinned}
            navPersistent={navPersistent}
            toggleNavPinned={toggleNavPinned}
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
