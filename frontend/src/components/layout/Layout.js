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

  // Auto-close SHOW mode ONLY when route actually changes (not when mode changes!)
  useEffect(() => {
    const pathnameChanged = prevPathnameRef.current !== location.pathname;
    
    if (pathnameChanged && mode === SIDENAV_MODES.SHOW) {
      console.log(`[Layout] Route changed while in SHOW mode - auto-closing to CLOSE`, {
        previousPath: prevPathnameRef.current,
        currentPath: location.pathname,
      });
      setMode(SIDENAV_MODES.CLOSE);
    }
    
    // Update ref for next comparison
    prevPathnameRef.current = location.pathname;
  }, [location.pathname, mode, setMode, SIDENAV_MODES.SHOW, SIDENAV_MODES.CLOSE]);

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
