import React, { createRef, useContext, useEffect, useRef, useState } from "react";
import { HeaderGlobalAction, HeaderPanel, Select, SelectItem } from "@carbon/react";
import {
  Close,
  Language,
  Logout,
  Notification,
  Search,
  UserAvatarFilledAlt,
  LocationFilled,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import HelpMenu from "./HelpMenu";
import SlideOver from "../notifications/SlideOver";
import SlideOverNotifications from "../notifications/SlideOverNotifications";
import SearchBar from "./search/searchBar";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { ConfigurationContext } from "./Layout";
import { getFromOpenElisServer, putToOpenElisServer } from "../utils/Utils";
import { languages } from "../../languages";

/**
 * HeaderActions
 *
 * Extracted from legacy Header.js. Provides:
 * - Search toggle
 * - Notifications toggle + panel
 * - User panel (user info, language selector, logout)
 * - Help menu
 *
 * @see spec.md FR-011: Preserve ALL existing header functionality
 * @see plan.md D5: Header Action Preservation Strategy
 */
export default function HeaderActions({ onChangeLanguage }) {
  const { configurationProperties } = useContext(ConfigurationContext) || {};
  const { userSessionDetails, logout } = useContext(UserSessionDetailsContext);

  const intl = useIntl();

  const userSwitchRef = createRef();
  const headerPanelRef = createRef();
  const scrollRef = useRef(window.scrollY);

  const [switchCollapsed, setSwitchCollapsed] = useState(true);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showRead, setShowRead] = useState(false);
  const [unReadNotifications, setUnReadNotifications] = useState([]);
  const [readNotifications, setReadNotifications] = useState([]);
  const [searchBar, setSearchBar] = useState(false);
  const [helpOpen, setHelpOpen] = useState(false);

  useEffect(() => {
    window.scrollTo(0, scrollRef.current);
  }, []);

  const handlePanelToggle = (panel) => {
    setSearchBar(panel === "search");
    setNotificationsOpen(panel === "notifications");
    setSwitchCollapsed(panel !== "user");
    setHelpOpen(panel === "help");
  };

  const getNotifications = async () => {
    setLoading(true);
    try {
      getFromOpenElisServer("/rest/notifications", (data) => {
        const read = [];
        const unread = [];
        data?.forEach((element) => {
          if (element.readAt) {
            read.push(element);
          } else {
            unread.push(element);
          }
        });
        setReadNotifications(read);
        setUnReadNotifications(unread);
        setNotifications(data || []);
      });
    } catch (error) {
      console.error("Failed to fetch notifications", error);
    } finally {
      setLoading(false);
    }
  };

  const markNotificationAsRead = async (notificationId) => {
    try {
      putToOpenElisServer(
        `/rest/notification/markasread/${notificationId}`,
        null,
        () => {
          getNotifications();
        },
      );
    } catch (error) {
      console.error("Failed to mark notification as read", error);
    }
  };

  const markAllNotificationsAsRead = async () => {
    try {
      putToOpenElisServer("/rest/notification/markasread/all", null, () => {
        getNotifications();
      });
    } catch (error) {
      console.error("Failed to mark all notifications as read", error);
    }
  };

  useEffect(() => {
    if (!userSessionDetails?.authenticated) {
      setNotifications([]);
      setUnReadNotifications([]);
      setReadNotifications([]);
      setNotificationsOpen(false);
      setShowRead(false);
      setLoading(false);
      return;
    }
    getNotifications();
  }, [userSessionDetails?.authenticated]);

  const panelSwitchLabel = () => {
    return userSessionDetails?.authenticated ? "User" : "Lang";
  };

  const panelSwitchIcon = () => {
    return userSessionDetails?.authenticated ? (
      switchCollapsed ? (
        <UserAvatarFilledAlt size={20} />
      ) : (
        <Close size={20} />
      )
    ) : switchCollapsed ? (
      <Language size={20} />
    ) : (
      <Close size={20} />
    );
  };

  return (
    <>
      {userSessionDetails?.authenticated && (
        <>
          {searchBar && <SearchBar />}
          <HeaderGlobalAction
            id="search-Icon"
            aria-label="Search"
            onClick={() => handlePanelToggle(searchBar ? "" : "search")}
          >
            {!searchBar ? <Search size={20} /> : <Close size={20} />}
          </HeaderGlobalAction>
          <HeaderGlobalAction
            id="notification-Icon"
            aria-label="Notifications"
            onClick={() =>
              handlePanelToggle(notificationsOpen ? "" : "notifications")
            }
          >
            <div
              style={{
                position: "relative",
                display: "inline-block",
              }}
            >
              {!notificationsOpen ? (
                <Notification size={20} />
              ) : (
                <Close size={20} />
              )}
              {unReadNotifications?.length > 0 && (
                <span
                  style={{
                    position: "absolute",
                    top: "-5px",
                    right: "-5px",
                    backgroundColor: "red",
                    color: "white",
                    borderRadius: "50%",
                    width: "22px",
                    height: "22px",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: "12px",
                    animation: "pulse 5s infinite",
                    opacity: 1,
                    transition: "background-color 0.3s ease-in-out",
                  }}
                >
                  {unReadNotifications.length}
                </span>
              )}
            </div>
          </HeaderGlobalAction>
        </>
      )}
      <HeaderGlobalAction
        id="user-Icon"
        aria-label={panelSwitchLabel()}
        onClick={() => handlePanelToggle(switchCollapsed ? "user" : "")}
        ref={userSwitchRef}
      >
        {panelSwitchIcon()}
      </HeaderGlobalAction>
      <HelpMenu helpOpen={helpOpen} handlePanelToggle={handlePanelToggle} />

      <HeaderPanel
        aria-label="Header Panel"
        expanded={!switchCollapsed}
        className="headerPanel"
        ref={headerPanelRef}
      >
        <ul>
          {userSessionDetails?.authenticated && (
            <>
              <li className="userDetails">
                <UserAvatarFilledAlt size={18} style={{ marginRight: "4px" }} />
                {userSessionDetails.firstName} {userSessionDetails.lastName}
              </li>
              {userSessionDetails.loginLabUnit && (
                <li className="userDetails">
                  <LocationFilled size={18} style={{ marginRight: "4px" }} />
                  {userSessionDetails.loginLabUnit}{" "}
                </li>
              )}
              <li
                data-cy="logOut"
                className="userDetails clickableUserDetails"
                onClick={logout}
              >
                <Logout style={{ marginRight: "3px" }} />
                <FormattedMessage id="header.label.logout" />
              </li>
            </>
          )}
          <li className="userDetails">
            <Select
              id="selector"
              name="selectLocale"
              className="selectLocale"
              invalidText="A valid locale value is required"
              labelText={<FormattedMessage id="header.label.selectlocale" />}
              onChange={(event) => {
                if (onChangeLanguage) {
                  onChangeLanguage(event.target.value);
                }
              }}
              value={intl.locale}
            >
              {Object.entries(languages).map(([code, { label }]) => (
                <SelectItem key={code} text={label} value={code} />
              ))}
            </Select>
          </li>
          <li className="userDetails">
            <label className="cds--label">
              {" "}
              <FormattedMessage id="header.label.version" />:{" "}
              {configurationProperties?.releaseNumber}
            </label>
          </li>
        </ul>
      </HeaderPanel>

      <SlideOver
        open={notificationsOpen}
        setOpen={(open) => setNotificationsOpen(open)}
        slideFrom="right"
        title="Notifications"
      >
        <SlideOverNotifications
          loading={loading}
          notifications={showRead ? readNotifications : unReadNotifications}
          showRead={showRead}
          markNotificationAsRead={markNotificationAsRead}
          getNotifications={getNotifications}
          setShowRead={setShowRead}
          markAllNotificationsAsRead={markAllNotificationsAsRead}
        />
      </SlideOver>
    </>
  );
}


