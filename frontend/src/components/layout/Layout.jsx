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
import { useSideNavPreference } from "./useSideNavPreference";
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
  const isAdminContext = isAdminNavRoute(location.pathname);
  const navContext = isAdminContext ? "admin" : "main";

  const layoutConfig = {
    storageKeyPrefix: pageStorageKeyPrefix
      ? pageStorageKeyPrefix
      : isAdminContext
        ? "admin"
        : isStorageContext
          ? "storage"
          : isAnalyzerContext
            ? "analyzer"
            : "main",
    // Admin, storage, and analyzer workflows benefit from locked navigation.
    // All other routes default to collapsed (rail) mode.
    defaultMode: pageDefaultMode
      ? pageDefaultMode
      : isAdminContext || isStorageContext || isAnalyzerContext
        ? "lock"
        : "close",
  };

  // Lock mode support - push content when sidenav is locked
  const { mode, isExpanded, toggle, setMode, SIDENAV_MODES } =
    useSideNavPreference(layoutConfig);

  useEffect(() => {
    if (isAdminContext && mode === SIDENAV_MODES.CLOSE) {
      setMode(SIDENAV_MODES.LOCK);
    }
  }, [isAdminContext, mode, setMode, SIDENAV_MODES]);

  // Only push content when sidenav is actually present (authenticated UX).
  // Otherwise, a persisted LOCK mode would incorrectly shift unauthenticated pages
  // like /login to the right (no sidenav toggle available there).
  const isLocked =
    userSessionDetails.authenticated && mode === SIDENAV_MODES.LOCK;

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
            mode={mode}
            isExpanded={isExpanded}
            toggleSideNav={toggle}
            setMode={setMode}
            SIDENAV_MODES={SIDENAV_MODES}
            defaultMode={layoutConfig.defaultMode}
            storageKeyPrefix={layoutConfig.storageKeyPrefix}
            navContext={navContext}
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
