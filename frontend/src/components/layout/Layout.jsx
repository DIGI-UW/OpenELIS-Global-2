import React, { createContext, useState, useEffect, useContext } from "react";
import { useLocation } from "react-router-dom";
import Header from "./Header";
import Footer from "./Footer";
import { Content, Theme } from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../utils/Utils";
import { useSideNavPreference } from "./useSideNavPreference";
import {
  languages as defaultLanguages,
  buildLanguagesFromConfig,
} from "../../languages";

export const ConfigurationContext = createContext(null);
export const NotificationContext = createContext(null);

export default function Layout(props) {
  const {
    children,
    defaultMode: pageDefaultMode,
    storageKeyPrefix: pageStorageKeyPrefix,
  } = props;
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

  // Admin pages render their own internal sub-nav. The side drawer can't
  // share horizontal space with that sub-nav, so we force the drawer
  // closed while on these routes — without mutating the user's persisted
  // mode preference. When the user navigates back to a non-admin page,
  // the drawer auto-restores to whatever mode they had before (LOCK,
  // SHOW, or CLOSE).
  const isAdminContext =
    location.pathname.startsWith("/admin") ||
    location.pathname.startsWith("/MasterListsPage");

  const layoutConfig = {
    storageKeyPrefix: pageStorageKeyPrefix
      ? pageStorageKeyPrefix
      : isStorageContext
        ? "storage"
        : isAnalyzerContext
          ? "analyzer"
          : "main",
    // Storage and analyzer workflows benefit from locked (persistent) sidenav
    // All other routes default to collapsed (rail) mode
    defaultMode: pageDefaultMode
      ? pageDefaultMode
      : isStorageContext || isAnalyzerContext
        ? "lock"
        : "close",
  };

  // Lock mode support - push content when sidenav is locked
  const { mode, isExpanded, toggle, setMode, SIDENAV_MODES } =
    useSideNavPreference(layoutConfig);

  // Admin routes default to CLOSE so the page's internal sub-nav has
  // full width. If the user explicitly opens the drawer while on an
  // admin route (toggle button or setMode), record an override so we
  // respect their choice for the rest of the admin session. The
  // override resets when they navigate back to a non-admin route, so
  // the next admin entry gets the auto-close default again.
  const [adminOverride, setAdminOverride] = useState(false);
  useEffect(() => {
    if (!isAdminContext) setAdminOverride(false);
  }, [isAdminContext]);

  const effectiveMode = isAdminContext
    ? adminOverride
      ? mode
      : SIDENAV_MODES.CLOSE
    : mode;
  const effectiveIsExpanded = effectiveMode !== SIDENAV_MODES.CLOSE;

  // Wrap toggle/setMode so explicit user interaction while on admin
  // flips the override flag. The wrappers are passed to Header in
  // place of the bare hook returns.
  const handleToggle = () => {
    if (isAdminContext) setAdminOverride(true);
    toggle();
  };
  const handleSetMode = (newMode) => {
    if (isAdminContext) setAdminOverride(true);
    setMode(newMode);
  };

  // Only push content when sidenav is actually present (authenticated UX).
  // Otherwise, a persisted LOCK mode would incorrectly shift unauthenticated pages
  // like /login to the right (no sidenav toggle available there).
  const isLocked =
    userSessionDetails.authenticated && effectiveMode === SIDENAV_MODES.LOCK;

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
            mode={effectiveMode}
            isExpanded={effectiveIsExpanded}
            toggleSideNav={handleToggle}
            setMode={handleSetMode}
            SIDENAV_MODES={SIDENAV_MODES}
            defaultMode={layoutConfig.defaultMode}
            storageKeyPrefix={layoutConfig.storageKeyPrefix}
          />
          {/* Theme wrapper creates white theme zone for content area */}
          {/* Global SCSS theme = blue header/nav, this = light content */}
          <Theme theme="white">
            <Content
              data-testid="content-wrapper"
              className={isLocked ? "content-nav-locked" : ""}
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
