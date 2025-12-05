import {
  ChevronDown,
  ChevronUp,
  Close,
  Language,
  Logout,
  Notification,
  Search,
  UserAvatarFilledAlt,
  LocationFilled,
  Menu,
  Pin,
} from "@carbon/icons-react";
import { Select, SelectItem } from "@carbon/react";
import HelpMenu from "./HelpMenu";
import React, {
  createRef,
  useContext,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useLocation, useHistory } from "react-router-dom";
import { useMenuAutoExpand } from "./useMenuAutoExpand";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import "../Style.css";
import { ConfigurationContext } from "../layout/Layout";
import SlideOver from "../notifications/SlideOver";
import { languages } from "../../languages";

import {
  Header,
  HeaderGlobalAction,
  HeaderGlobalBar,
  HeaderMenuButton,
  HeaderName,
  HeaderPanel,
  SideNav,
  SideNavItems,
  SideNavMenu,
  SideNavMenuItem,
  Theme,
} from "@carbon/react";
import SlideOverNotifications from "../notifications/SlideOverNotifications";
import { getFromOpenElisServer, putToOpenElisServer } from "../utils/Utils";
import SearchBar from "./search/searchBar";
function OEHeader({
  onChangeLanguage,
  mode,
  isExpanded,
  toggleSideNav,
  setMode,
  SIDENAV_MODES,
}) {
  const { configurationProperties } = useContext(ConfigurationContext);
  const { userSessionDetails, logout } = useContext(UserSessionDetailsContext);

  const userSwitchRef = createRef();
  const headerPanelRef = createRef();
  const scrollRef = useRef(window.scrollY);
  const [isOpen, setIsOpen] = useState(false);

  const intl = useIntl();
  const location = useLocation();
  const history = useHistory();

  // Lock mode support - tri-state sidenav (show/lock/close)
  // State is managed by Layout.js and passed via props
  const isLocked = mode === SIDENAV_MODES.LOCK;

  const [switchCollapsed, setSwitchCollapsed] = useState(true);
  const [menus, setMenus] = useState({
    menu: [{ menu: {}, childMenus: [] }],
    menu_billing: { menu: {}, childMenus: [] },
    menu_nonconformity: { menu: {}, childMenus: [] },
  });

  // Auto-expand menu items based on current route
  const autoExpandedMenus = useMenuAutoExpand(menus["menu"]);

  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showRead, setShowRead] = useState(false);
  const [unReadNotifications, setUnReadNotifications] = useState([]);
  const [readNotifications, setReadNotifications] = useState([]);
  const [searchBar, setSearchBar] = useState(false);
  const [helpOpen, setHelpOpen] = useState(false);
  scrollRef.current = window.scrollY;
  useLayoutEffect(() => {
    window.scrollTo(0, scrollRef.current);
  }, []);

  useEffect(() => {
    userSessionDetails.authenticated
      ? getFromOpenElisServer("/rest/menu", (res) => {
          handleMenuItems("menu", res);
        })
      : console.log("User not authenticated, not getting menu");
  }, [userSessionDetails.authenticated]);

  const panelSwitchLabel = () => {
    return userSessionDetails.authenticated ? "User" : "Lang";
  };

  const handleMenuItems = (tag, res) => {
    if (res) {
      let newMenus = menus;
      newMenus[tag] = res;
      setMenus(newMenus);
    }
  };

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
    getNotifications();
  }, []);

  // Click-outside handler: Close nav when in SHOW mode and user clicks outside
  useEffect(() => {
    if (mode !== SIDENAV_MODES.SHOW) return; // Only active in SHOW mode

    const handleClickOutside = (event) => {
      const sideNav = document.querySelector(".cds--side-nav");
      const menuButton = document.querySelector('[data-cy="menuButton"]');

      if (
        sideNav &&
        !sideNav.contains(event.target) &&
        menuButton &&
        !menuButton.contains(event.target)
      ) {
        // Click outside in SHOW mode - collapse to CLOSE
        setMode(SIDENAV_MODES.CLOSE);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [mode, SIDENAV_MODES, setMode]);

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
    return (
      <>
        <picture>
          <img
            className="logo"
            src={`../images/openelis_logo.png`}
            alt="logo"
          />
        </picture>
      </>
    );
  };
  const generateMenuItems = (menuItem, index, level, path) => {
    if (menuItem.menu.isActive) {
      if (level === 0 && menuItem.childMenus.length > 0) {
        return (
          <span id={menuItem.menu.elementId} key={path}>
            <span
              id={menuItem.menu.elementId + "_dropdown"}
              onClick={(e) => {
                setMenuItemExpanded(e, menuItem, path);
                // Also navigate to first active child
                const firstActiveChild = menuItem.childMenus.find(
                  (c) => c.menu.isActive,
                );
                if (firstActiveChild?.menu.actionURL) {
                  history.push(firstActiveChild.menu.actionURL);
                }
              }}
            >
              <SideNavMenu
                className="top-level-menu-item"
                aria-label={intl.formatMessage({
                  id: menuItem.menu.displayKey,
                })}
                title={intl.formatMessage({
                  id: menuItem.menu.displayKey,
                })}
                key={`${menuItem.menu.elementId}-${menuItem.expanded}`}
                defaultExpanded={menuItem.expanded}
                // onClick={(e) => { // not supported yet, but if it becomes so we can simplify the functionality here by having this here and not have a span around it
                //   setMenuItemExpanded(e, menuItem, path);
                // }}
              >
                <span
                  onClick={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                  }}
                >
                  {menuItem.childMenus.map((childMenuItem, index) => {
                    return generateMenuItems(
                      childMenuItem,
                      index,
                      level + 1,
                      path + ".childMenus[" + index + "]",
                    );
                  })}
                </span>
              </SideNavMenu>
            </span>
          </span>
        );
      } else if (level === 0) {
        return (
          <span key={path} id={menuItem.menu.elementId}>
            <SideNavMenuItem
              id={menuItem.menu.elementId + "_nav"}
              href={menuItem.menu.actionURL}
              target={menuItem.menu.openInNewWindow ? "_blank" : ""}
              className="top-level-menu-item"
              isActive={
                !!menuItem.menu.actionURL &&
                location.pathname === menuItem.menu.actionURL
              }
            >
              {renderSideNavMenuItemLabel(menuItem, level)}
            </SideNavMenuItem>
          </span>
        );
      } else {
        // Level > 0 items: use custom buttons for navigation
        // DO NOT set href on SideNavMenuItem - custom buttons handle navigation
        // This prevents double navigation and flickering
        return (
          <span
            data-cy={`${menuItem.menu.elementId.replace(/[^\w\s]/gi, "_")}`}
            id={menuItem.menu.elementId}
            key={path}
          >
            <SideNavMenuItem
              className="reduced-padding-nav-menu-item"
              style={{ width: "100%" }}
              isActive={
                !!menuItem.menu.actionURL &&
                location.pathname === menuItem.menu.actionURL
              }
            >
              <span style={{ display: "flex", width: "100%" }}>
                {!menuItem.menu.actionURL &&
                  !hasActiveChildMenu(menuItem) &&
                  console.warn("menu entry has no action url and no child")}
                {!hasActiveChildMenu(menuItem) &&
                  renderSingleNavButton(menuItem, index, level, path)}
                {!menuItem.menu.actionURL &&
                  hasActiveChildMenu(menuItem) &&
                  renderSingleDropdownButton(menuItem, index, level, path)}
                {menuItem.menu.actionURL &&
                  hasActiveChildMenu(menuItem) &&
                  renderDualNavDropdownButton(menuItem, index, level, path)}
              </span>
            </SideNavMenuItem>
            {menuItem.childMenus.map((childMenuItem, index) => {
              return (
                <span
                  key={path + ".childMenus[" + index + "].span"}
                  style={{ display: menuItem.expanded ? "" : "none" }}
                >
                  {generateMenuItems(
                    childMenuItem,
                    index,
                    level + 1,
                    path + ".childMenus[" + index + "]",
                  )}
                </span>
              );
            })}
          </span>
        );
      }
    } else {
      return <React.Fragment key={path}></React.Fragment>;
    }
  };

  const hasActiveChildMenu = (menuItem) => {
    return (
      menuItem.childMenus.length >= 1 &&
      menuItem.childMenus.some((element) => {
        return element.menu.isActive;
      })
    );
  };

  const renderSingleNavButton = (menuItem, index, level, path) => {
    const marginValue = (level - 1) * 0.5 + "rem";
    const isActive =
      menuItem.menu.actionURL && location.pathname === menuItem.menu.actionURL;
    return (
      <button
        data-cy="single-sidenav-button"
        className={`custom-sidenav-button ${isActive ? "active" : ""}`}
        style={{ width: "100%", marginLeft: marginValue }}
        id={menuItem.menu.elementId + "_nav"}
        onClick={() => {
          if (menuItem.menu.openInNewWindow) {
            window.open(menuItem.menu.actionURL);
          } else {
            history.push(menuItem.menu.actionURL);
          }
        }}
      >
        {renderSideNavMenuItemLabel(menuItem, level)}
      </button>
    );
  };

  const renderSingleDropdownButton = (menuItem, index, level, path) => {
    const marginValue = (level - 1) * 0.5 + "rem";
    return (
      <button
        data-cy="sidenav-button"
        id={menuItem.menu.displayKey + "_dropdown"}
        className={"custom-sidenav-button"}
        style={{ marginLeft: marginValue }}
        onClick={(e) => {
          onClickSideNavItem(e, menuItem, path);
        }}
      >
        {renderSideNavMenuItemLabel(menuItem, level)}
        {renderSideNavChevron(menuItem)}
      </button>
    );
  };

  const renderDualNavDropdownButton = (menuItem, index, level, path) => {
    const marginValue = (level - 1) * 0.5 + "rem";
    const isActive =
      menuItem.menu.actionURL && location.pathname === menuItem.menu.actionURL;
    return (
      <>
        <button
          id={menuItem.menu.elementId + "_nav"}
          className={
            menuItem.menu.actionURL
              ? `custom-sidenav-button ${isActive ? "active" : ""}`
              : "custom-sidenav-button-unclickable"
          }
          style={{ marginLeft: marginValue }}
          onClick={() => {
            if (menuItem.menu.openInNewWindow) {
              window.open(menuItem.menu.actionURL);
            } else {
              history.push(menuItem.menu.actionURL);
            }
          }}
        >
          {renderSideNavMenuItemLabel(menuItem, level)}
        </button>
        {menuItem.childMenus.length > 0 && (
          <button
            data-cy={`sidenav-button-${menuItem.menu.elementId}`}
            id={menuItem.menu.displayKey + "_dropdown"}
            className="custom-sidenav-button"
            onClick={(e) => {
              onClickSideNavItem(e, menuItem, path);
            }}
          >
            {renderSideNavChevron(menuItem)}
          </button>
        )}
      </>
    );
  };

  const renderSideNavChevron = (menuItem) => {
    return (
      <>
        {menuItem.expanded && (
          <div className="cds--side-nav__icon cds--side-nav__icon--small cds--side-nav__submenu-chevron">
            <ChevronUp />
          </div>
        )}
        {!menuItem.expanded && (
          <div className="cds--side-nav__icon cds--side-nav__icon--small cds--side-nav__submenu-chevron">
            <ChevronDown />
          </div>
        )}
      </>
    );
  };

  const renderSideNavMenuItemLabel = (menuItem, level) => {
    const fontPercent = 100 - 5 * (level - 1) + "%";
    return (
      <span style={{ fontSize: fontPercent }}>
        <FormattedMessage id={menuItem.menu.displayKey} />
      </span>
    );
  };

  const onClickSideNavItem = (e, menuItem, path) => {
    e.preventDefault();
    e.stopPropagation();
    setMenuItemExpanded(e, menuItem, path);
  };

  const setMenuItemExpanded = (e, menuItem, path) => {
    const newMenus = { ...menus };

    // Accordion behavior: Only one top-level section open at a time
    if (path.startsWith("$.menu[")) {
      // This is a top-level item (level 0)
      // Collapse all other top-level items
      newMenus.menu = newMenus.menu.map((item) => {
        if (item.menu.elementId === menuItem.menu.elementId) {
          // This is the item being toggled
          return { ...item, expanded: !item.expanded };
        } else {
          // Collapse all siblings
          return { ...item, expanded: false };
        }
      });
    } else {
      // Nested item - just toggle it without affecting siblings
      const newMenuItem = { ...menuItem };
      newMenuItem.expanded = !newMenuItem.expanded;
      var jp = require("jsonpath");
      jp.value(newMenus, path, newMenuItem);
    }

    setMenus(newMenus);
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
              {userSessionDetails.authenticated && (
                <button
                  data-cy="menuButton"
                  className="cds--header__action cds--header__menu-trigger cds--header__menu-toggle"
                  aria-label={
                    mode === SIDENAV_MODES.CLOSE
                      ? "Open menu"
                      : mode === SIDENAV_MODES.SHOW
                        ? "Pin menu"
                        : "Close menu"
                  }
                  onClick={toggleSideNav}
                  title={
                    mode === SIDENAV_MODES.CLOSE
                      ? "Open menu"
                      : mode === SIDENAV_MODES.SHOW
                        ? "Pin menu"
                        : "Close menu"
                  }
                  type="button"
                >
                  {mode === SIDENAV_MODES.CLOSE && <Menu size={20} />}
                  {mode === SIDENAV_MODES.SHOW && <Pin size={20} />}
                  {mode === SIDENAV_MODES.LOCK && <Close size={20} />}
                </button>
              )}
              <HeaderName href="/" prefix="" style={{ padding: "0px" }}>
                <span id="header-logo">{logo()}</span>
                <div className="banner">
                  <h5>{configurationProperties?.BANNER_TEXT}</h5>
                  <p>
                    <FormattedMessage id="header.label.version" /> &nbsp;{" "}
                    {configurationProperties?.releaseNumber}
                  </p>
                </div>
              </HeaderName>
              <HeaderGlobalBar>
                {userSessionDetails.authenticated && (
                  <>
                    {searchBar && <SearchBar />}
                    <HeaderGlobalAction
                      id="search-Icon"
                      aria-label="Search"
                      onClick={() =>
                        handlePanelToggle(searchBar ? "" : "search")
                      }
                    >
                      {!searchBar ? <Search size={20} /> : <Close size={20} />}
                    </HeaderGlobalAction>
                    <HeaderGlobalAction
                      id="notification-Icon"
                      aria-label="Notifications"
                      onClick={() =>
                        handlePanelToggle(
                          notificationsOpen ? "" : "notifications",
                        )
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
                  onClick={() =>
                    handlePanelToggle(switchCollapsed ? "user" : "")
                  }
                  ref={userSwitchRef}
                >
                  {panelSwitchIcon()}
                </HeaderGlobalAction>
                <HelpMenu
                  helpOpen={helpOpen}
                  handlePanelToggle={handlePanelToggle}
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
                  <li className="userDetails">
                    <label className="cds--label">
                      {" "}
                      <FormattedMessage id="header.label.version" />:{" "}
                      {configurationProperties?.releaseNumber}
                    </label>
                  </li>
                </ul>
              </HeaderPanel>
              {userSessionDetails.authenticated && (
                <>
                  <SideNav
                    aria-label="Side navigation"
                    expanded={isExpanded}
                    isFixedNav={isLocked}
                    isPersistent={false}
                    isChildOfHeader={true}
                  >
                    <SideNavItems>
                      {autoExpandedMenus.map((childMenuItem, index) => {
                        return generateMenuItems(
                          childMenuItem,
                          index,
                          0,
                          "$.menu[" + index + "]",
                        );
                      })}
                    </SideNavItems>
                  </SideNav>
                </>
              )}
            </Header>
            <div style={{ flex: 1 }}>
              <SlideOver
                open={notificationsOpen}
                setOpen={(open) => setNotificationsOpen(open)}
                slideFrom="right"
                title="Notifications"
              >
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
              </SlideOver>
            </div>
          </div>
      </div>
    </>
  );
}

export default OEHeader;
