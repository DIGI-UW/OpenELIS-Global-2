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

  const location = useLocation();

  const addNotification = (notificationBody) => {
    

    const exists = notifications.some(
      (n) =>
        n.title === notificationBody.title &&
        n.message === notificationBody.message &&
        n.kind === notificationBody.kind,
    );
    if (!exists) {
      setNotifications((prevNotifications) => [
        ...prevNotifications,
        notificationBody,
      ]);
    }
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
  useEffect(() => {
    setNotifications([]);
  }, [location]);

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
