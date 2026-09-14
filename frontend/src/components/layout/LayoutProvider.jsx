import React, { useState, useEffect, useContext, useCallback } from "react";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../utils/Utils";
import {
  languages as defaultLanguages,
  buildLanguagesFromConfig,
} from "../../languages";
import TranslationOverrideProvider from "../../languages/TranslationOverrideProvider";
import { ConfigurationContext, NotificationContext } from "./contexts";

/**
 * The state behind ConfigurationContext and NotificationContext, apart from the
 * chrome that used to hold it.
 *
 * <p>Layout renders Header, and Header reaches most of the component library, so
 * a signed-out page that only needed these two contexts was pulling the whole
 * application in to get them. Providing them here lets /login render without
 * Layout, which is what keeps Layout off the first-paint path.
 */
export default function LayoutProvider({ children }) {
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [resetConfig, setResetConfig] = useState(false);
  const [configurationProperties, setConfigurationProperties] = useState({});
  const [notificationVisible, setNotificationVisible] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [supportedLocales, setSupportedLocales] = useState([]);
  const [enabledLanguages, setEnabledLanguages] = useState(defaultLanguages);

  const addNotification = (notificationBody) => {
    setNotifications([...notifications, notificationBody]);
  };

  const removeNotification = (index) => {
    const newNotifications = [...notifications];
    newNotifications.splice(index, 1);
    setNotifications(newNotifications);
  };

  const loadConfigurationProperties = useCallback(
    (afterLoad) => {
      const handleConfigurationProperties = (res) => {
        setConfigurationProperties(res);
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

  useEffect(() => {
    getFromOpenElisServer("/rest/supportedlocales/active", (response) => {
      if (response && Array.isArray(response)) {
        setSupportedLocales(response);
        setEnabledLanguages(buildLanguagesFromConfig(response));
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
      <TranslationOverrideProvider>
        <NotificationContext.Provider
          value={{
            notificationVisible,
            setNotificationVisible,
            notifications,
            addNotification,
            removeNotification,
          }}
        >
          {children}
        </NotificationContext.Provider>
      </TranslationOverrideProvider>
    </ConfigurationContext.Provider>
  );
}
