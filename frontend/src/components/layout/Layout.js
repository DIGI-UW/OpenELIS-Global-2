import React, { createContext, useState, useEffect, useContext, useRef } from "react";
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
  const { children } = props;
  const location = useLocation();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [resetConfig, setResetConfig] = useState(false);
  const [configurationProperties, setConfigurationProperties] = useState({});
  const [notificationVisible, setNotificationVisible] = useState(false);
  const [notifications, setNotifications] = useState([]);

  // Determine layout config based on route
  // Storage pages default to LOCK, others to CLOSE
  // Use separate storage keys to persist preferences independently
  const isStorageContext =
    location.pathname.startsWith("/Storage") ||
    location.pathname.startsWith("/FreezerMonitoring");

  const layoutConfig = isStorageContext
    ? { storageKeyPrefix: "storage", defaultMode: "lock" }
    : { storageKeyPrefix: "main", defaultMode: "close" };

  console.log(`[Layout] Route changed:`, {
    pathname: location.pathname,
    isStorageContext,
    layoutConfig,
  });

  // Lock mode support - push content when sidenav is locked
  const { mode, isExpanded, toggle, setMode, SIDENAV_MODES } =
    useSideNavPreference(layoutConfig);
  const isLocked = mode === SIDENAV_MODES.LOCK;

  console.log(`[Layout] Sidenav state:`, {
    mode,
    isExpanded,
    isLocked,
    contentClass: isLocked ? "content-nav-locked" : "none",
  });

  // Track previous pathname to detect actual route changes
  const prevPathnameRef = useRef(location.pathname);

  // Auto-close SHOW mode to DEFAULT mode for new route (not always CLOSE!)
  useEffect(() => {
    const pathnameChanged = prevPathnameRef.current !== location.pathname;
    
    if (pathnameChanged && mode === SIDENAV_MODES.SHOW) {
      // Close to the DEFAULT mode for the NEW route (respects page-specific defaults!)
      const newDefaultMode = layoutConfig.defaultMode;
      console.log(`[Layout] Route changed while in SHOW mode - auto-closing to default for new route`, {
        previousPath: prevPathnameRef.current,
        currentPath: location.pathname,
        closingTo: newDefaultMode,
      });
      setMode(newDefaultMode);
    }
    
    // Update ref for next comparison
    prevPathnameRef.current = location.pathname;
  }, [location.pathname, mode, setMode, SIDENAV_MODES.SHOW, layoutConfig.defaultMode]);

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
          />
          {/* Theme wrapper creates white theme zone for content area */}
          {/* Global SCSS theme = blue header/nav, this = light content */}
          <Theme theme="white">
            <Content className={isLocked ? "content-nav-locked" : ""}>
              {children}
            </Content>
          </Theme>
          <Footer />
        </div>
      </NotificationContext.Provider>
    </ConfigurationContext.Provider>
  );
}
