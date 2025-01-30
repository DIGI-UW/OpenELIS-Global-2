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

  const userSwitchRef = createRef();
  const headerPanelRef = createRef();
  const scrollRef = useRef(window.scrollY);
  const [isOpen, setIsOpen] = useState(false);

  const intl = useIntl();

  const [switchCollapsed, setSwitchCollapsed] = useState(true);
  const [menus, setMenus] = useState({
    menu: [{ menu: {}, childMenus: [] }],
    menu_billing: { menu: {}, childMenus: [] },
    menu_nonconformity: { menu: {}, childMenus: [] },
  });
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showRead, setShowRead] = useState(false);
  const [unReadNotifications, setUnReadNotifications] = useState([]);
  const [readNotifications, setReadNotifications] = useState([]);
  const [searchBar, setSearchBar] = useState(false);
  scrollRef.current = window.scrollY;
  useLayoutEffect(() => {
    window.scrollTo(0, scrollRef.current);
  }, []);

  useEffect(() => {
    getFromOpenElisServer("/rest/menu", (res) => {
      handleMenuItems("menu", res);
    });
  }, []);

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

  const toggleSlideOver = () => {
    setIsOpen(!isOpen);
  };

  const clickPanelSwitch = () => {
    setSwitchCollapsed(!switchCollapsed);
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
  const handleSearch = () => {
    setSearchBar(!searchBar);
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
            >
              {renderSideNavMenuItemLabel(menuItem, level)}
            </SideNavMenuItem>
          </span>
        );
      } else {
        return (
          <span id={menuItem.menu.elementId} key={path}>
            <SideNavMenuItem
              className="reduced-padding-nav-menu-item"
              href={menuItem.menu.actionURL}
              target={menuItem.menu.openInNewWindow ? "_blank" : ""}
              style={{ width: "100%" }}
              rel="noreferrer"
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
    if (menuItem.menu.elementId === "menu_reports_routine") {
      console.log("reports");
    }
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
              render={({ isSideNavExpanded, onClickSideNavExpand }) => (
                <Header id="mainHeader" className="mainHeader" aria-label="">
                  {userSessionDetails.authenticated && (
                    <HeaderMenuButton
                      aria-label={
                        isSideNavExpanded ? "Close menu" : "Open menu"
                      }
                      onClick={onClickSideNavExpand}
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
                          aria-label="Search"
                          onClick={handleSearch}
                        >
                          {!searchBar ? (
                            <Search size={20} />
                          ) : (
                            <Close size={20} />
                          )}
                        </HeaderGlobalAction>
                        <HeaderGlobalAction
                          aria-label="Notifications"
                          onClick={toggleSlideOver}
                        >
                          <div
                            style={{
                              position: "relative",
                              display: "inline-block",
                            }}
                          >
                            <Notification size={20} />
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
                                {unReadNotifications?.length}
                              </span>
                            )}
                          </div>
                        </HeaderGlobalAction>
                      </>
                    )}
                    <HeaderGlobalAction
                      aria-label={panelSwitchLabel()}
                      onClick={clickPanelSwitch}
                      ref={userSwitchRef}
                    >
                      {panelSwitchIcon()}
                    </HeaderGlobalAction>
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
                            className="userDetails clickableUserDetails"
                            onClick={logout}
                          >
                            <Logout
                              id="sign-out"
                              style={{ marginRight: "3px" }}
                            />
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
                          <SelectItem text="English" value="en" />
                          <SelectItem text="French" value="fr" />
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
                        isPersistent={false}
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
              )}
            />
            <div style={{ flex: 1 }}>
              <SlideOver
                open={isOpen}
                setOpen={setIsOpen}
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
