import React, { createContext, useState, useEffect, useContext, useMemo } from "react";
import { useLocation } from "react-router-dom";
import Footer from "./Footer";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../utils/Utils";
import TwoModeLayout from "./TwoModeLayout";

export const ConfigurationContext = createContext(null);
export const NotificationContext = createContext(null);

export default function Layout(props) {
  const { children } = props;
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const location = useLocation();
  const [resetConfig, setResetConfig] = useState(false);
  const [configurationProperties, setConfigurationProperties] = useState({});
  const [notificationVisible, setNotificationVisible] = useState(false);
  const [notifications, setNotifications] = useState([]);

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

  // Default mode: LOCK for storage/multi-tab pages, else CLOSE (rail)
  const defaultMode = useMemo(() => {
    const path = location.pathname || "/";
    const lower = path.toLowerCase();
    if (
      lower.startsWith("/storage") ||
      lower.startsWith("/freezermonitoring") ||
      lower.startsWith("/analyzers") ||
      lower.startsWith("/notebook") ||
      lower.startsWith("/pathology") ||
      lower.startsWith("/cytology")
    ) {
      return "lock";
    }
    return "close";
  }, [location.pathname]);

  // Storage key prefix per top-level segment to keep preferences distinct
  const storageKeyPrefix = useMemo(() => {
    const segment = (location.pathname || "").split("/")[1];
    return segment ? segment : "default";
  }, [location.pathname]);

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
          <TwoModeLayout
            defaultMode={defaultMode}
            storageKeyPrefix={storageKeyPrefix}
          >
            {children}
          </TwoModeLayout>
          <Footer />
        </div>
      </NotificationContext.Provider>
    </ConfigurationContext.Provider>
  );
}
