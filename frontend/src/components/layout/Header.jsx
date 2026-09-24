import {
  Close,
  Language,
  Logout,
  Password,
  Notification,
  Search,
  UserAvatarFilledAlt,
  LocationFilled,
  Menu,
  Pin,
  PinFilled,
} from "@carbon/icons-react";
import { IconButton, Select, SelectItem } from "@carbon/react";
import HelpMenu from "./HelpMenu";
import AdminSideNav from "../admin/AdminSideNav";
import React, { createRef, useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import ConfiguredSideNav from "./ConfiguredSideNav";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import "../Style.css";
import "./ApplicationSideNav.scss";
import { ConfigurationContext } from "../layout/Layout";
import SlideOver from "../notifications/SlideOver";
import { languages as defaultLanguages } from "../../languages";

import {
  Header,
  HeaderGlobalAction,
  HeaderGlobalBar,
  HeaderName,
  HeaderPanel,
  SideNav,
  SideNavItems,
  Theme,
} from "@carbon/react";
import SlideOverNotifications from "../notifications/SlideOverNotifications";
import { getFromOpenElisServer, putToOpenElisServer } from "../utils/Utils";
import SearchBar from "./search/searchBar";
import { getBranding } from "../utils/BrandingUtils";
import config from "../../config.json";

function OEHeader({
  onChangeLanguage,
  navOpen = true,
  isDesktop = true,
  navPinned = true,
  navPersistent = isDesktop && navPinned,
  toggleNavPinned,
  toggleSideNav,
  closeSideNav,
  navContext = "main",
  showSideNav = true,
}) {
  const { configurationProperties, enabledLanguages } =
    useContext(ConfigurationContext);
  const { userSessionDetails, logout } = useContext(UserSessionDetailsContext);
  // Use enabled languages from config, fall back to default if not loaded yet
  const languages = enabledLanguages || defaultLanguages;
  const [headerLogoUrl, setHeaderLogoUrl] = useState(null);
  const [logoVersion, setLogoVersion] = useState(0); // Version counter for cache-busting

  const userSwitchRef = createRef();
  const headerPanelRef = createRef();

  const intl = useIntl();

  const [switchCollapsed, setSwitchCollapsed] = useState(true);
  const [menus, setMenus] = useState({
    menu: [{ menu: {}, childMenus: [] }],
    menu_billing: { menu: {}, childMenus: [] },
    menu_nonconformity: { menu: {}, childMenus: [] },
  });

  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [showRead, setShowRead] = useState(false);
  const [unReadNotifications, setUnReadNotifications] = useState([]);
  const [readNotifications, setReadNotifications] = useState([]);
  const [searchBar, setSearchBar] = useState(false);
  const [helpOpen, setHelpOpen] = useState(false);
  const [isTrainingInstallation, setIsTrainingInstallation] = useState(false);

  // Load branding configuration for header logo
  // Colors are handled by App.js
  const loadHeaderLogo = () => {
    getBranding((response) => {
      if (response && response.headerLogoUrl) {
        setHeaderLogoUrl(response.headerLogoUrl);
        setLogoVersion((prev) => prev + 1);
      }
    });
  };

  // Load header logo on initial mount (for login page)
  useEffect(() => {
    loadHeaderLogo();
  }, []);

  // Reload header logo when authentication status changes
  useEffect(() => {
    if (userSessionDetails.authenticated) {
      loadHeaderLogo();
    }
  }, [userSessionDetails.authenticated]);

  // Listen for branding update events to refresh logo
  useEffect(() => {
    const handleBrandingUpdate = () => {
      loadHeaderLogo();
    };
    window.addEventListener("branding-updated", handleBrandingUpdate);
    return () => {
      window.removeEventListener("branding-updated", handleBrandingUpdate);
    };
  }, [userSessionDetails.authenticated]);

  useEffect(() => {
    if (userSessionDetails.authenticated) {
      getFromOpenElisServer("/rest/database-cleaning/status", (response) => {
        if (response) {
          setIsTrainingInstallation(response.trainingInstallation);
        }
      });
    }
  }, [userSessionDetails.authenticated]);

  const panelSwitchLabel = () => {
    return userSessionDetails.authenticated
      ? intl.formatMessage({ id: "header.icon.user" })
      : intl.formatMessage({ id: "header.icon.lang" });
  };

  const handleMenuItems = (tag, res) => {
    if (res) {
      // FIX: Initialize expanded property for all menu items
      const initializeExpanded = (items) => {
        return items.map((item) => ({
          ...item,
          expanded: item.expanded === true, // Ensure boolean, default to false
          childMenus: item.childMenus
            ? initializeExpanded(item.childMenus)
            : [],
        }));
      };

      const initializedMenus = initializeExpanded(res);

      // IMPORTANT: use functional setState so we never drop other menu buckets due to stale closures
      setMenus((prev) => ({ ...prev, [tag]: initializedMenus }));
    }
  };

  useEffect(() => {
    if (!userSessionDetails.authenticated || navContext !== "main") {
      return;
    }
    getFromOpenElisServer("/rest/menu", (res) => {
      handleMenuItems("menu", res);
    });
  }, [userSessionDetails.authenticated, navContext]);

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
        setReadNotifications([]);
        setUnReadNotifications([]);
        data?.forEach((element) => {
          if (element.readAt) {
            setReadNotifications((prev) => [...prev, element]);
          } else {
            setUnReadNotifications((prev) => [...prev, element]);
          }
        });
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
        (response) => {
          console.log("Notification marked as read", response);
          getNotifications();
        },
      );
    } catch (error) {
      console.error("Failed to mark notification as read", error);
    }
  };

  const markAllNotificationsAsRead = async () => {
    try {
      putToOpenElisServer(
        `/rest/notification/markasread/all`,
        null,
        (response) => {
          console.log("All Notifications marked as read", response);
          getNotifications();
        },
      );
    } catch (error) {
      console.error("Failed to mark all notifications as read", error);
    }
  };

  useEffect(() => {
    if (!userSessionDetails.authenticated) {
      return;
    }

    const timer = window.setTimeout(() => {
      getNotifications();
    }, 0);

    return () => window.clearTimeout(timer);
  }, [userSessionDetails.authenticated]);

  // Click-outside handler: close the drawer whenever the nav is an overlay
  // (small viewports, or desktop with the nav unpinned)
  useEffect(() => {
    if (navPersistent || !navOpen) return;

    const handleClickOutside = (event) => {
      const sideNav = document.querySelector(".cds--side-nav");
      const menuButton = document.getElementById("sidenav-menu-button");

      if (
        sideNav &&
        !sideNav.contains(event.target) &&
        menuButton &&
        !menuButton.contains(event.target)
      ) {
        closeSideNav();
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [navPersistent, navOpen, closeSideNav]);

  const panelSwitchIcon = () => {
    return userSessionDetails.authenticated ? (
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

  const logo = () => {
    // Use custom header logo if available, otherwise use default
    // Add cache-busting parameter to prevent stale logo display after upload
    const logoSrc = headerLogoUrl
      ? `${config.serverBaseUrl}${headerLogoUrl}?v=${logoVersion}`
      : `/images/openelis_logo.png`;

    return (
      <>
        <picture>
          <img
            className="logo"
            src={logoSrc}
            alt="logo"
            style={{ objectFit: "contain", maxHeight: "71px" }}
            onError={(e) => {
              // Fallback to default logo if custom logo fails to load
              // Clear onError to prevent infinite loop if fallback also fails
              e.target.onerror = null;
              e.target.src = `/images/openelis_logo.png`;
            }}
          />
        </picture>
      </>
    );
  };

  return (
    <>
      <div className="container">
        <div
          style={{
            display: "flex",
            flexDirection: "column",
          }}
        >
          <Header id="mainHeader" className="mainHeader" aria-label="">
            {userSessionDetails.authenticated &&
              !navPersistent &&
              showSideNav && (
                <button
                  id="sidenav-menu-button"
                  data-cy="menuButton"
                  className="cds--header__action cds--header__menu-trigger cds--header__menu-toggle"
                  aria-label={intl.formatMessage({
                    id: navOpen
                      ? "header.icon.menu.close"
                      : "header.icon.menu.open",
                  })}
                  onClick={toggleSideNav}
                  title={intl.formatMessage({
                    id: navOpen
                      ? "header.icon.menu.close"
                      : "header.icon.menu.open",
                  })}
                  type="button"
                >
                  {navOpen ? <Close size={20} /> : <Menu size={20} />}
                </button>
              )}
            <HeaderName href="/" prefix="" style={{ padding: "0px" }}>
              <span id="header-logo">{logo()}</span>
              <div className="banner">
                <h5>{configurationProperties?.BANNER_TEXT}</h5>
                <p>
                  <FormattedMessage id="header.label.version" /> &nbsp;{" "}
                  {configurationProperties?.releaseNumber}
                  {isTrainingInstallation && (
                    <span className="training-installation-badge">
                      <FormattedMessage id="training.installation.message" />
                    </span>
                  )}
                </p>
              </div>
            </HeaderName>
            <HeaderGlobalBar>
              {userSessionDetails.authenticated && (
                <>
                  {searchBar && <SearchBar />}
                  <HeaderGlobalAction
                    id="search-Icon"
                    aria-label={intl.formatMessage({
                      id: "header.icon.search",
                    })}
                    onClick={() => handlePanelToggle(searchBar ? "" : "search")}
                  >
                    {!searchBar ? <Search size={20} /> : <Close size={20} />}
                  </HeaderGlobalAction>
                  <HeaderGlobalAction
                    id="notification-Icon"
                    aria-label={intl.formatMessage({
                      id: "header.icon.notifications",
                    })}
                    onClick={() =>
                      handlePanelToggle(
                        notificationsOpen ? "" : "notifications",
                      )
                    }
                  >
                    <div
                      style={{
                        position: "relative",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        height: "100%",
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
              <HelpMenu
                helpOpen={helpOpen}
                handlePanelToggle={handlePanelToggle}
                enabled={userSessionDetails.authenticated === true}
              />
            </HeaderGlobalBar>
            <HeaderPanel
              aria-label="Header Panel"
              expanded={!switchCollapsed}
              className="headerPanel"
              ref={headerPanelRef}
            >
              <ul>
                {userSessionDetails.authenticated && (
                  <>
                    <li className="userDetails">
                      <UserAvatarFilledAlt
                        size={18}
                        style={{ marginRight: "4px" }}
                      />
                      {userSessionDetails.firstName}{" "}
                      {userSessionDetails.lastName}
                    </li>
                    {userSessionDetails.loginLabUnit && (
                      <li className="userDetails">
                        <LocationFilled
                          size={18}
                          style={{ marginRight: "4px" }}
                        />
                        {userSessionDetails.loginLabUnit}{" "}
                      </li>
                    )}
                  </>
                )}
                <li className="userDetails">
                  {/* Theme wrapper ONLY around Select to make dropdown light */}
                  <Theme theme="white">
                    <Select
                      id="selector"
                      name="selectLocale"
                      className="selectLocale"
                      invalidText="A valid locale value is required"
                      labelText={
                        <FormattedMessage id="header.label.selectlocale" />
                      }
                      onChange={(event) => {
                        onChangeLanguage(event.target.value);
                      }}
                      value={intl.locale}
                    >
                      {Object.entries(languages).map(([code, { label }]) => (
                        <SelectItem key={code} text={label} value={code} />
                      ))}
                    </Select>
                  </Theme>
                </li>
                {userSessionDetails.authenticated && (
                  <>
                    <li
                      data-cy="headerChangePassword"
                      className="userDetails clickableUserDetails"
                      onClick={() => {
                        window.location.href = "/ChangePasswordLogin";
                      }}
                    >
                      <Password style={{ marginRight: "3px" }} />
                      <FormattedMessage id="label.button.changepassword" />
                    </li>
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
                  <label className="cds--label">
                    {" "}
                    <FormattedMessage id="header.label.version" />:{" "}
                    {configurationProperties?.releaseNumber}
                  </label>
                </li>
              </ul>
            </HeaderPanel>
            {userSessionDetails.authenticated && showSideNav && (
              <Theme theme="white">
                <SideNav
                  aria-label="Side navigation"
                  className={`application-side-nav${navContext === "admin" ? " admin-shell-side-nav" : ""}`}
                  expanded={navOpen}
                  // Pinned desktop: always-rendered fixed nav;
                  // unpinned desktop + small viewports: overlay drawer
                  isFixedNav={navPersistent}
                  isPersistent={navPersistent}
                  isChildOfHeader={true}
                >
                  {isDesktop && (
                    <div className="sidenav-pin-row">
                      <IconButton
                        id="sidenav-pin-toggle"
                        data-cy="sidenavPinToggle"
                        data-testid="sidenav-pin-toggle"
                        kind="ghost"
                        size="sm"
                        align="right"
                        label={intl.formatMessage({
                          id: navPinned
                            ? "header.icon.menu.unpin"
                            : "header.icon.menu.pin",
                        })}
                        onClick={toggleNavPinned}
                      >
                        {navPinned ? (
                          <PinFilled size={16} />
                        ) : (
                          <Pin size={16} />
                        )}
                      </IconButton>
                    </div>
                  )}
                  {navContext === "admin" ? (
                    <AdminSideNav
                      isTrainingInstallation={isTrainingInstallation}
                    />
                  ) : (
                    <SideNavItems>
                      <ConfiguredSideNav
                        menus={menus.menu}
                        unifiedResultsOn={
                          configurationProperties?.RESULTS_ENTRY_UNIFIED_ROUTE ===
                          "true"
                        }
                      />
                    </SideNavItems>
                  )}
                </SideNav>
              </Theme>
            )}
          </Header>
          {userSessionDetails.authenticated && (
            <div style={{ flex: 1 }}>
              <SlideOver
                open={notificationsOpen}
                setOpen={(open) => setNotificationsOpen(open)}
                slideFrom="right"
                title="Notifications"
              >
                {notificationsOpen && (
                  <SlideOverNotifications
                    loading={loading}
                    notifications={
                      showRead ? readNotifications : unReadNotifications
                    }
                    showRead={showRead}
                    markNotificationAsRead={markNotificationAsRead}
                    getNotifications={getNotifications}
                    setShowRead={setShowRead}
                    markAllNotificationsAsRead={markAllNotificationsAsRead}
                  />
                )}
              </SlideOver>
            </div>
          )}
        </div>
      </div>
    </>
  );
}

export default OEHeader;
