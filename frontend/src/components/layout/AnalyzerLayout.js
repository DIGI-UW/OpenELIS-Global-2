/**
 * AnalyzerLayout Component
 *
 * A parallel POC layout that copies Header.js functionality but fixes the Carbon architecture.
 * This serves as a POC for correct Carbon usage where:
 * - We control the SideNav state directly (bypassing HeaderContainer)
 * - Sidenav is expanded by default and state persists in localStorage
 * - Content is a sibling to Header/SideNav for proper layout
 * - SideNav uses isFixedNav for proper content pushing
 *
 * This layout is isolated to /analyzers routes and does not affect other pages.
 */

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
import { useLocation } from "react-router-dom";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import "../Style.css";
import { ConfigurationContext } from "./Layout";
import SlideOver from "../notifications/SlideOver";
import { languages } from "../../languages";
import {
  Header,
  HeaderGlobalAction,
  HeaderGlobalBar,
  HeaderMenuButton,
  HeaderName,
  HeaderPanel,
  Content,
  SideNav,
  SideNavItems,
  SideNavMenu,
  SideNavMenuItem,
  Theme,
} from "@carbon/react";
import SlideOverNotifications from "../notifications/SlideOverNotifications";
import { getFromOpenElisServer, putToOpenElisServer } from "../utils/Utils";
import SearchBar from "./search/searchBar";
import Footer from "./Footer";

const AnalyzerLayout = ({ children, onChangeLanguage }) => {
  const { configurationProperties } = useContext(ConfigurationContext);
  const { userSessionDetails, logout } = useContext(UserSessionDetailsContext);
  const location = useLocation();
  const intl = useIntl();

  const userSwitchRef = createRef();
  const headerPanelRef = createRef();
  const scrollRef = useRef(window.scrollY);

  // Layout State - Controlled directly
  const [isSideNavExpanded, setIsSideNavExpanded] = useState(() => {
    // Default to expanded for analyzer pages, check localStorage for user preference
    const saved = localStorage.getItem("analyzerSideNavExpanded");
    return saved !== null ? saved === "true" : true;
  });

  const [switchCollapsed, setSwitchCollapsed] = useState(true);
  const [menus, setMenus] = useState({
    menu: [{ menu: {}, childMenus: [] }],
    menu_billing: { menu: {}, childMenus: [] },
    menu_nonconformity: { menu: {}, childMenus: [] },
  });
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showRead, setShowRead] = useState(false);
  const [unReadNotifications, setUnReadNotifications] = useState([]);
  const [readNotifications, setReadNotifications] = useState([]);
  const [searchBar, setSearchBar] = useState(false);
  const [helpOpen, setHelpOpen] = useState(false);

  // Scroll restoration
  scrollRef.current = window.scrollY;
  useLayoutEffect(() => {
    window.scrollTo(0, scrollRef.current);
  }, []);

  // Menu loading
  useEffect(() => {
    userSessionDetails.authenticated
      ? getFromOpenElisServer("/rest/menu", (res) => {
          handleMenuItems("menu", res);
        })
      : console.log("User not authenticated, not getting menu");
  }, [userSessionDetails.authenticated]);

  // Notifications loading
  useEffect(() => {
    getNotifications();
  }, []);

  // SideNav Toggle Handler
  const handleSideNavToggle = () => {
    const newExpanded = !isSideNavExpanded;
    setIsSideNavExpanded(newExpanded);
    localStorage.setItem("analyzerSideNavExpanded", String(newExpanded));
  };

  // Auto-expand menu on route change
  useEffect(() => {
    setMenus((prevMenus) => {
      const newMenus = JSON.parse(JSON.stringify(prevMenus));
      const markActiveExpanded = (items) => {
        let isActiveBranch = false;
        items.forEach((item) => {
          if (item.childMenus && item.childMenus.length > 0) {
            if (markActiveExpanded(item.childMenus)) {
              item.expanded = true;
              isActiveBranch = true;
            }
          }
          if (
            item.menu.actionURL === location.pathname ||
            location.pathname.startsWith(item.menu.actionURL + "/")
          ) {
            isActiveBranch = true;
          }
        });
        return isActiveBranch;
      };

      if (newMenus.menu) {
        markActiveExpanded(newMenus.menu);
      }
      return newMenus;
    });
  }, [location.pathname]);

  const panelSwitchLabel = () => {
    return userSessionDetails.authenticated ? "User" : "Lang";
  };

  const handleMenuItems = (tag, res) => {
    if (res) {
      // Recursive function to mark active branch expanded
      const markActiveExpanded = (items) => {
        let isActiveBranch = false;
        items.forEach((item) => {
          if (item.childMenus && item.childMenus.length > 0) {
            if (markActiveExpanded(item.childMenus)) {
              item.expanded = true;
              isActiveBranch = true;
            }
          }
          if (
            item.menu.actionURL === location.pathname ||
            location.pathname.startsWith(item.menu.actionURL + "/")
          ) {
            isActiveBranch = true;
          }
        });
        return isActiveBranch;
      };

      // Clone res to avoid mutation issues before setting state (though we are setting a new state object)
      const menuItems = JSON.parse(JSON.stringify(res));
      markActiveExpanded(menuItems);

      setMenus((prevMenus) => ({
        ...prevMenus,
        [tag]: menuItems,
      }));
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
    // Filter out Field Mappings from nav (temporary fix until backend is updated)
    if (
      menuItem.menu.displayKey === "analyzer.navigation.fieldMappings" ||
      menuItem.menu.displayKey === "analyzer.page.hierarchy.mappings" ||
      (menuItem.menu.actionURL && menuItem.menu.actionURL.includes("/mappings"))
    ) {
      return <React.Fragment key={path}></React.Fragment>;
    }

    // menuItem.menu.isActive from backend means "visible/enabled", not "currently selected"
    if (menuItem.menu.isActive) {
      if (level === 0 && menuItem.childMenus.length > 0) {
        return (
          <span id={menuItem.menu.elementId} key={path}>
            <span
              id={menuItem.menu.elementId + "_dropdown"}
              onClick={(e) => {
                setMenuItemExpanded(e, menuItem, path);
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
                key={"menu_" + index + "_" + level}
                defaultExpanded={menuItem.expanded}
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
            >
              {renderSideNavMenuItemLabel(menuItem, level)}
            </SideNavMenuItem>
          </span>
        );
      } else {
        return (
          <span
            data-cy={`${menuItem.menu.elementId.replace(/[^\w\s]/gi, "_")}`}
            id={menuItem.menu.elementId}
            key={path}
          >
            <SideNavMenuItem
              className="reduced-padding-nav-menu-item"
              href={menuItem.menu.actionURL}
              target={menuItem.menu.openInNewWindow ? "_blank" : ""}
              style={{ width: "100%" }}
            >
              <span style={{ display: "flex", width: "100%" }}>
                {!menuItem.menu.actionURL && !hasActiveChildMenu(menuItem)}
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
    return (
      <button
        data-cy="single-sidenav-button"
        className={"custom-sidenav-button"}
        style={{ width: "100%", marginLeft: marginValue }}
        id={menuItem.menu.elementId + "_nav"}
        onClick={() => {
          if (menuItem.menu.openInNewWindow) {
            window.open(menuItem.menu.actionURL);
          } else {
            window.location.href = menuItem.menu.actionURL;
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
    return (
      <>
        <button
          id={menuItem.menu.elementId + "_nav"}
          className={
            menuItem.menu.actionURL
              ? "custom-sidenav-button"
              : "custom-sidenav-button-unclickable"
          }
          style={{ marginLeft: marginValue }}
          onClick={() => {
            if (menuItem.menu.openInNewWindow) {
              window.open(menuItem.menu.actionURL);
            } else {
              window.location.href = menuItem.menu.actionURL;
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
    const newMenuItem = { ...menuItem };
    newMenuItem.expanded = !newMenuItem.expanded;
    var jp = require("jsonpath");
    jp.value(newMenus, path, newMenuItem);
    setMenus(newMenus);
  };

  // Default onChangeLanguage if not provided
  const handleLanguageChange =
    onChangeLanguage ||
    ((lang) => {
      console.log("Language change requested:", lang);
    });

  return (
    <>
      <div className="container">
        <Theme>
          <div
            style={{
              display: "flex",
              flexDirection: "column",
            }}
          >
            {/* Header Component - Directly Controlled */}
            <Header id="mainHeader" className="mainHeader" aria-label="">
              {userSessionDetails.authenticated && (
                <HeaderMenuButton
                  data-cy="menuButton"
                  aria-label={isSideNavExpanded ? "Close menu" : "Open menu"}
                  onClick={handleSideNavToggle}
                  isActive={isSideNavExpanded}
                  isCollapsible={true}
                />
              )}
              <HeaderName
                href="/analyzers"
                prefix=""
                style={{ padding: "0px" }}
              >
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
                    <Select
                      id="selector"
                      name="selectLocale"
                      className="selectLocale"
                      invalidText="A valid locale value is required"
                      labelText={
                        <FormattedMessage id="header.label.selectlocale" />
                      }
                      onChange={(event) => {
                        handleLanguageChange(event.target.value);
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
              {userSessionDetails.authenticated && (
                <>
                  <SideNav
                    aria-label="Side navigation"
                    expanded={isSideNavExpanded}
                    isFixedNav={true}
                    isChildOfHeader={true}
                  >
                    <SideNavItems>
                      {menus["menu"].map((childMenuItem, index) => {
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

            {/* Content Sibling - Properly Pushed by Fixed SideNav */}
            <div
              className={
                isSideNavExpanded ? "content-expanded" : "content-collapsed"
              }
              style={{
                marginLeft: isSideNavExpanded ? "16rem" : "3rem",
                width: isSideNavExpanded
                  ? "calc(100% - 16rem)"
                  : "calc(100% - 3rem)",
                transition:
                  "margin-left 0.11s cubic-bezier(0.2, 0, 1, 0.9), width 0.11s cubic-bezier(0.2, 0, 1, 0.9)",
              }}
            >
              <Theme theme="white">
                <Content className="analyzer-content" style={{ marginLeft: 0 }}>
                  {children}
                </Content>
              </Theme>
            </div>

            {/* Footer Sibling */}
            <Footer />

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
        </Theme>
      </div>
    </>
  );
};

export default AnalyzerLayout;
