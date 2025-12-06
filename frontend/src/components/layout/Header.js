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
      
      // FIX: Initialize expanded property for all menu items
      const initializeExpanded = (items) => {
        return items.map((item) => ({
          ...item,
          expanded: item.expanded === true, // Ensure boolean, default to false
          childMenus: item.childMenus ? initializeExpanded(item.childMenus) : [],
        }));
      };
      
      const initializedMenus = initializeExpanded(res);
      
      let newMenus = { ...menus };
      newMenus[tag] = initializedMenus;
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
  /**
   * Helper: Check if any child menu item (recursively) is active
   * Used to determine if parent should show active state
   */
  const hasActiveChild = (menuItem) => {
    if (!menuItem.childMenus || menuItem.childMenus.length === 0) {
      return false;
    }
    
    return menuItem.childMenus.some((child) => {
      const exactMatch = location.pathname === child.menu.actionURL;
      const prefixMatch = 
        child.menu.actionURL?.length > 1 && 
        location.pathname.startsWith(child.menu.actionURL + "/");
      const childIsActive = !!child.menu.actionURL && (exactMatch || prefixMatch);
      
      // Check recursively
      return childIsActive || hasActiveChild(child);
    });
  };

  const generateMenuItems = (menuItem, index, level, path) => {
    // Skip inactive menu items
    if (!menuItem.menu.isActive) {
      return <React.Fragment key={path}></React.Fragment>;
    }

    // URL matching: exact match OR prefix match (for parent routes)
    const exactMatch = location.pathname === menuItem.menu.actionURL;
    const prefixMatch = 
      menuItem.menu.actionURL?.length > 1 && 
      location.pathname.startsWith(menuItem.menu.actionURL + "/");
    const isActive = !!menuItem.menu.actionURL && (exactMatch || prefixMatch);
    const hasChildren = menuItem.childMenus.length > 0;
    
    // For items with children, also check if any child is active
    const parentIsActive = hasChildren ? hasActiveChild(menuItem) : false;
    const finalIsActive = isActive || parentIsActive;

    // Indentation: level 0 = no indent, level 1+ = progressive indent
    const marginValue = level > 0 ? `${level * 0.5}rem` : "0";

    // Handler for label click - navigate (and expand if has children)
    const handleLabelClick = (e) => {
      e.preventDefault();
      e.stopPropagation();

      // If has children, expand first
      if (hasChildren) {
        setMenuItemExpanded(e, menuItem, path);
      }

      // Then navigate if has URL
      if (menuItem.menu.actionURL) {
        if (menuItem.menu.openInNewWindow) {
          window.open(menuItem.menu.actionURL);
        } else {
          history.push(menuItem.menu.actionURL);
        }
      } else if (hasChildren) {
        // Top-level item with children but no URL - navigate to first child
        const firstChild = menuItem.childMenus.find((c) => c.menu.isActive);
        if (firstChild?.menu.actionURL) {
          history.push(firstChild.menu.actionURL);
        }
      }
    };

    // Handler for chevron click - ONLY expand, don't navigate
    const handleChevronClick = (e) => {
      e.preventDefault();
      e.stopPropagation();
      setMenuItemExpanded(e, menuItem, path);
    };

    // ============================================================================
    // UNIFIED RENDERING: Use SideNavMenuItem for ALL items (top-level + subnavs)
    // ============================================================================
    // This ensures consistent active state support across all levels
    return (
      <span
        data-cy={`${menuItem.menu.elementId.replace(/[^\w\s]/gi, "_")}`}
        id={menuItem.menu.elementId}
        key={path}
      >
        <SideNavMenuItem
          className={level === 0 ? "top-level-menu-item" : "reduced-padding-nav-menu-item"}
          isActive={finalIsActive}
          href={menuItem.menu.actionURL || (hasChildren ? "#" : undefined)}
          onClick={handleLabelClick}
        >
          <div
            style={{
              display: "flex",
              width: "100%",
              alignItems: "center",
              marginLeft: marginValue,
            }}
          >
            <span style={{ flex: 1 }}>
              {renderSideNavMenuItemLabel(menuItem, level)}
            </span>
            {hasChildren && (
              <div
                className="cds--side-nav__icon cds--side-nav__icon--small cds--side-nav__submenu-chevron"
                onClick={handleChevronClick}
                style={{ cursor: "pointer" }}
              >
                {menuItem.expanded ? <ChevronUp /> : <ChevronDown />}
              </div>
            )}
          </div>
        </SideNavMenuItem>

        {/* Render children if expanded - nested SideNavMenuItem items */}
        {hasChildren &&
          menuItem.expanded &&
          menuItem.childMenus.map((childMenuItem, childIndex) => {
            return generateMenuItems(
              childMenuItem,
              childIndex,
              level + 1,
              path + ".childMenus[" + childIndex + "]",
            );
          })}
      </span>
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

  const setMenuItemExpanded = (e, menuItem, path) => {
    const newMenus = { ...menus };

    // REMOVED: Accordion auto-collapse behavior
    // User feedback: "autocollapse causes consistency issues and changes user focus"
    // New behavior: Allow multiple sections to be expanded simultaneously
    if (path.startsWith("$.menu[")) {
      // This is a top-level item (level 0)
      newMenus.menu = newMenus.menu.map((item) => {
        if (item.menu.elementId === menuItem.menu.elementId) {
          // This is the item being toggled
          return { ...item, expanded: !item.expanded };
        } else {
          // Keep siblings as-is (don't collapse)
          return item;
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
