import React, { createContext, useState, useEffect, useContext } from "react";
import { useLocation } from "react-router-dom";
import Header from "./Header";
import Footer from "./Footer";
import { Content, Theme } from "@carbon/react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../utils/Utils";
import { useSideNavPreference } from "./useSideNavPreference";

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

  // Determine layout config from props or route-based fallbacks
  const isStorageContext =
    location.pathname.startsWith("/Storage") ||
    location.pathname.startsWith("/FreezerMonitoring");

  const isAnalyzerContext =
    location.pathname.startsWith("/analyzers") ||
    location.pathname.startsWith("/AnalyzerManagement");

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

  return (
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
          <Header
            onChangeLanguage={props.onChangeLanguage}
            mode={mode}
            isExpanded={isExpanded}
            toggleSideNav={toggle}
            setMode={setMode}
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
