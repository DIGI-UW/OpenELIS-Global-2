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
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { withRouter } from "react-router-dom";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import "../Style.css";
import { ConfigurationContext } from "../layout/Layout";
import SlideOver from "../notifications/SlideOver";
import { languages } from "../../languages";

import {
  Header,
  HeaderContainer,
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
function OEHeader(props) {
  const { configurationProperties } = useContext(ConfigurationContext);
  const { userSessionDetails, logout } = useContext(UserSessionDetailsContext);
  const history = props.history; // Get history from withRouter

  const userSwitchRef = createRef();
  const headerPanelRef = createRef();
  const scrollRef = useRef(window.scrollY);
  const [isOpen, setIsOpen] = useState(false);

  const intl = useIntl();
  const currentPath = props.location?.pathname || "";
  
  // Helper function to check if a menu item is active based on current path
  // ONLY exact match - no prefix matching to avoid highlighting all analyzer routes
  const isMenuItemActive = (actionURL) => {
    if (!actionURL || !currentPath) return false;
    return actionURL === currentPath;
  };

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
  
  // Check if we're on an analyzer route (for isPersistent and defaultExpanded)
  const isAnalyzerRoute = props.location?.pathname?.startsWith("/analyzers") ?? false;
  
  // Track if we've initialized analyzer sidebar to expanded
  const [analyzerSidebarInitialized, setAnalyzerSidebarInitialized] = useState(false);
  const [lastRouteType, setLastRouteType] = useState(null); // Track if last route was analyzer or not
  
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
    // Contextual visibility: Hide "Field Mappings" sub-nav unless on /analyzers/:id/mappings route
    if (
      menuItem.menu.elementId === "menu_analyzers_field_mappings" &&
      !currentPath.match(/^\/analyzers\/[^/]+\/mappings$/)
    ) {
      return <React.Fragment key={path}></React.Fragment>;
    }

    if (menuItem.menu.isActive) {
      if (level === 0 && menuItem.childMenus.length > 0) {
        // Check if this is the Analyzers parent menu and we're on an analyzer route
        const isAnalyzersParent = menuItem.menu.elementId === "menu_analyzers";
        const hasActiveAnalyzerChild = currentPath.startsWith("/analyzers") && 
          menuItem.childMenus.some(child => 
            child.menu.actionURL && isMenuItemActive(child.menu.actionURL)
          );
        const isParentActive = isAnalyzersParent && currentPath.startsWith("/analyzers");
        
        return (
          <span id={menuItem.menu.elementId} key={path}>
            <span
              id={menuItem.menu.elementId + "_dropdown"}
              onClick={(e) => {
                setMenuItemExpanded(e, menuItem, path);
                // For Analyzers parent menu, also navigate to first child when expanding
                if (isAnalyzersParent && !menuItem.expanded && menuItem.childMenus.length > 0) {
                  const firstChild = menuItem.childMenus.find(child => child.menu.isActive && child.menu.actionURL);
                  if (firstChild) {
                    history.push(firstChild.menu.actionURL);
                  }
                }
              }}
            >
              <SideNavMenu
                className={`top-level-menu-item ${isParentActive ? "analyzer-parent-active" : ""}`}
                aria-label={intl.formatMessage({
                  id: menuItem.menu.displayKey,
                })}
                title={intl.formatMessage({
                  id: menuItem.menu.displayKey,
                })}
                key={"menu_" + index + "_" + level}
                defaultExpanded={menuItem.expanded || isParentActive}
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
        const isActive = isMenuItemActive(menuItem.menu.actionURL);
        return (
          <span key={path} id={menuItem.menu.elementId}>
            <SideNavMenuItem
              id={menuItem.menu.elementId + "_nav"}
              href={menuItem.menu.actionURL}
              target={menuItem.menu.openInNewWindow ? "_blank" : ""}
              className={`top-level-menu-item ${isActive ? "sidenav-item-active" : ""}`}
              isActive={isActive}
              onClick={(e) => {
                // Prevent default href navigation for internal routes
                if (!menuItem.menu.openInNewWindow && menuItem.menu.actionURL?.startsWith("/")) {
                  e.preventDefault();
                  history.push(menuItem.menu.actionURL);
                }
              }}
            >
              {renderSideNavMenuItemLabel(menuItem, level)}
            </SideNavMenuItem>
          </span>
        );
      } else {
        const isActive = isMenuItemActive(menuItem.menu.actionURL);
        return (
          <span
            data-cy={`${menuItem.menu.elementId.replace(/[^\w\s]/gi, "_")}`}
            id={menuItem.menu.elementId}
            key={path}
          >
            <SideNavMenuItem
              className={`reduced-padding-nav-menu-item ${isActive ? "sidenav-item-active" : ""}`}
              href={menuItem.menu.actionURL}
              target={menuItem.menu.openInNewWindow ? "_blank" : ""}
              style={{ width: "100%" }}
              rel="noreferrer"
              isActive={isActive}
              onClick={(e) => {
                // Prevent default href navigation for internal routes
                if (!menuItem.menu.openInNewWindow && menuItem.menu.actionURL?.startsWith("/")) {
                  e.preventDefault();
                  history.push(menuItem.menu.actionURL);
                }
              }}
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
    const isActive = isMenuItemActive(menuItem.menu.actionURL);
    return (
      <button
        data-cy="single-sidenav-button"
        className={`custom-sidenav-button ${isActive ? "sidenav-button-active" : ""}`}
        style={{ width: "100%", marginLeft: marginValue }}
        id={menuItem.menu.elementId + "_nav"}
        onClick={() => {
          if (menuItem.menu.openInNewWindow) {
            window.open(menuItem.menu.actionURL);
          } else if (menuItem.menu.actionURL) {
            // Use React Router for client-side navigation (no page reload)
            const url = menuItem.menu.actionURL;
            if (url.startsWith("/")) {
              history.push(url);
            } else {
              // External URL - use window.location
              window.location.href = url;
            }
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
    const isActive = isMenuItemActive(menuItem.menu.actionURL);
    return (
      <>
        <button
          id={menuItem.menu.elementId + "_nav"}
          className={
            menuItem.menu.actionURL
              ? `custom-sidenav-button ${isActive ? "sidenav-button-active" : ""}`
              : "custom-sidenav-button-unclickable"
          }
          style={{ marginLeft: marginValue }}
          onClick={() => {
            if (menuItem.menu.openInNewWindow) {
              window.open(menuItem.menu.actionURL);
            } else if (menuItem.menu.actionURL) {
              // Use React Router for client-side navigation (no page reload)
              const url = menuItem.menu.actionURL;
              if (url.startsWith("/")) {
                history.push(url);
              } else {
                // External URL - use window.location
                window.location.href = url;
              }
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
    // Remove dynamic font sizing - let CSS handle consistent sizing across all levels
    return (
      <span>
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
            <HeaderContainer
              render={({ isSideNavExpanded, onClickSideNavExpand }) => {
                // On first render of analyzer route, expand the sidebar
                // On non-analyzer routes, ensure it's collapsed
                useEffect(() => {
                  // Reset initialization state when route type changes
                  if (lastRouteType !== null && lastRouteType !== isAnalyzerRoute) {
                    setAnalyzerSidebarInitialized(false);
                  }
                  
                  if (isAnalyzerRoute && !analyzerSidebarInitialized && !isSideNavExpanded) {
                    console.log("[AnalyzerNav] Initializing analyzer sidebar to expanded");
                    onClickSideNavExpand();
                    setAnalyzerSidebarInitialized(true);
                    setLastRouteType(true);
                  } else if (!isAnalyzerRoute && !analyzerSidebarInitialized) {
                    // Collapse sidebar on non-analyzer routes if it's expanded
                    if (isSideNavExpanded) {
                      console.log("[AnalyzerNav] Collapsing sidebar on non-analyzer route");
                      onClickSideNavExpand();
                    }
                    setAnalyzerSidebarInitialized(true);
                    setLastRouteType(false);
                  } else if (lastRouteType === null) {
                    // Initial load - set route type
                    setLastRouteType(isAnalyzerRoute);
                  }
                }, [isAnalyzerRoute, isSideNavExpanded, analyzerSidebarInitialized, lastRouteType]);
                
                console.log("[AnalyzerNav] Render", {
                  isAnalyzerRoute,
                  isSideNavExpanded,
                  analyzerSidebarInitialized,
                  currentPath,
                });

                const handleToggle = () => {
                  console.log("[AnalyzerNav] Toggle clicked BEFORE", {
                    isAnalyzerRoute,
                    currentPath,
                    wasSideNavExpanded: isSideNavExpanded,
                  });
                  onClickSideNavExpand();
                  // Use setTimeout to log state after Carbon updates it
                  setTimeout(() => {
                    const sideNav = document.querySelector('.cds--side-nav');
                    console.log("[AnalyzerNav] Toggle clicked AFTER", {
                      isSideNavExpanded,
                      sideNavClasses: sideNav?.className,
                      ariaExpanded: sideNav?.getAttribute('aria-expanded'),
                      isExpanded: sideNav?.classList.contains('cds--side-nav--expanded'),
                      computedDisplay: window.getComputedStyle(sideNav).display,
                      computedVisibility: window.getComputedStyle(sideNav).visibility,
                      computedTransform: window.getComputedStyle(sideNav).transform,
                    });
                  }, 100);
                };

                return (
                <Header id="mainHeader" className="mainHeader" aria-label="">
                  {userSessionDetails.authenticated && (
                    <HeaderMenuButton
                      data-cy="menuButton"
                      aria-label={
                        isSideNavExpanded ? "Close menu" : "Open menu"
                      }
                      onClick={handleToggle}
                      isActive={isSideNavExpanded}
                      isCollapsible={true}
                    />
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
                          {!searchBar ? (
                            <Search size={20} />
                          ) : (
                            <Close size={20} />
                          )}
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
                                  transition:
                                    "background-color 0.3s ease-in-out",
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
                            props.onChangeLanguage(event.target.value);
                          }}
                          value={props.intl.locale}
                        >
                          {Object.entries(languages).map(
                            ([code, { label }]) => (
                              <SelectItem
                                key={code}
                                text={label}
                                value={code}
                              />
                            ),
                          )}
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
                        className={
                          isAnalyzerRoute
                            ? "analyzer-persistent-sidebar"
                            : ""
                        }
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
                );
              }}
            />
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
}

export default withRouter(injectIntl(OEHeader));

