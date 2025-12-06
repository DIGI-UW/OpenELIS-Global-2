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

  // Lock mode support - push content when sidenav is locked
  const { mode, isExpanded, toggle, setMode, SIDENAV_MODES } =
    useSideNavPreference(layoutConfig);
  const isLocked = mode === SIDENAV_MODES.LOCK;

  // Track previous context to detect context switches (main ↔ storage)
  const prevContextRef = useRef(isStorageContext);
  const prevPathnameRef = useRef(location.pathname);
  const autoCloseTimeoutRef = useRef(null);
  const prevModeRef = useRef(mode);

  // Log mode changes for debugging
  useEffect(() => {
    /* eslint-disable no-console */
    console.log("[Layout] mode change", {
      prevMode: prevModeRef.current,
      mode,
      isExpanded,
      isLocked,
      pathname: location.pathname,
    });
    /* eslint-enable no-console */
    prevModeRef.current = mode;
  }, [mode, isExpanded, isLocked, location.pathname]);

  // Auto-close SHOW mode when navigating to a different page
  useEffect(() => {
    const pathnameChanged = prevPathnameRef.current !== location.pathname;
    
    if (pathnameChanged && mode === SIDENAV_MODES.SHOW) {
      // SHOW is temporary: on navigation, revert to page default and collapse
      console.log("[Layout] route change; mode SHOW -> default", {
        from: prevPathnameRef.current,
        to: location.pathname,
        targetMode: layoutConfig.defaultMode,
      });
      setMode(layoutConfig.defaultMode);
    }
    
    prevPathnameRef.current = location.pathname;
  }, [location.pathname, mode, setMode, SIDENAV_MODES.SHOW, layoutConfig.defaultMode]);

  // Auto-close shortly after load for pages whose default (or user choice) is CLOSE.
  // Keep LOCK intact; only collapse temporary SHOW on close-default pages.
  useEffect(() => {
    // Clear any pending timeout
    if (autoCloseTimeoutRef.current) {
      clearTimeout(autoCloseTimeoutRef.current);
    }

    const pathnameChanged = prevPathnameRef.current !== location.pathname;

    // Auto-close (or auto-lock) after navigation to a new route
    // SHOW is temporary: switch to the page’s default mode after route change
    const shouldAutoClose = pathnameChanged && mode === SIDENAV_MODES.SHOW;

    if (shouldAutoClose) {
      console.log("[Layout] auto-close timer start", {
        from: prevPathnameRef.current,
        to: location.pathname,
        targetMode: layoutConfig.defaultMode,
      });
      autoCloseTimeoutRef.current = setTimeout(() => {
        console.log("[Layout] auto-close timer firing", {
          targetMode: layoutConfig.defaultMode,
        });
        setMode(layoutConfig.defaultMode);
      }, 250); // small delay to avoid flicker during initial render
    }

    return () => {
      if (autoCloseTimeoutRef.current) {
        clearTimeout(autoCloseTimeoutRef.current);
      }
    };
  }, [
    layoutConfig.defaultMode,
    mode,
    setMode,
    SIDENAV_MODES.CLOSE,
    SIDENAV_MODES.SHOW,
    location.pathname,
    prevPathnameRef,
  ]);

  // Track context changes for future debugging if needed
  useEffect(() => {
    prevContextRef.current = isStorageContext;
  }, [isStorageContext]);

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
