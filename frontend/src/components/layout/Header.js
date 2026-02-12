import {
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
import { getBranding } from "../utils/BrandingUtils";
import config from "../../config.json";

function OEHeader({
  onChangeLanguage,
  mode,
  isExpanded,
  toggleSideNav,
  setMode,
  SIDENAV_MODES,
  defaultMode = "close",
  storageKeyPrefix = "main",
}) {
  const { configurationProperties } = useContext(ConfigurationContext);
  const { userSessionDetails, logout } = useContext(UserSessionDetailsContext);
  const [headerLogoUrl, setHeaderLogoUrl] = useState(null);
  const [logoVersion, setLogoVersion] = useState(0); // Version counter for cache-busting

  const userSwitchRef = createRef();
  const headerPanelRef = createRef();
  const scrollRef = useRef(window.scrollY);
  const [isOpen, setIsOpen] = useState(false);

  const intl = useIntl();
  const location = useLocation();
  const history = useHistory();

  const [switchCollapsed, setSwitchCollapsed] = useState(true);
  const [menus, setMenus] = useState({
    menu: [{ menu: {}, childMenus: [] }],
    menu_billing: { menu: {}, childMenus: [] },
    menu_nonconformity: { menu: {}, childMenus: [] },
  });

  // Auto-expand menu items based on current route
  const autoExpandedMenus = useMenuAutoExpand(
    menus["menu"],
    `${storageKeyPrefix}ExpandedMap`,
  );

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
    if (!userSessionDetails.authenticated) {
      return;
    }
    getFromOpenElisServer("/rest/menu", (res) => {
      handleMenuItems("menu", res);
    });
  }, [userSessionDetails.authenticated]);

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
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
      const menuButton = document.getElementById("sidenav-menu-button");

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
    // Use custom header logo if available, otherwise use default
    // Add cache-busting parameter to prevent stale logo display after upload
    const logoSrc = headerLogoUrl
      ? `${config.serverBaseUrl}${headerLogoUrl}?v=${logoVersion}`
      : `../images/openelis_logo.png`;

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
              e.target.src = `../images/openelis_logo.png`;
            }}
          />
        </picture>
      </>
    );
  };
  const hideTimerRef = useRef(null);

  /**
   * Returns true if ANY child/grandchild matches currentPath.
   *
   * Important: Do NOT match the item itself here. Otherwise a parent item like
   * /analyzers would be considered an "active child" for /analyzers/errors.
   */
  const hasActiveDescendant = (item, currentPath) => {
    const normalizePath = (url) => {
      if (!url) return "";
      const pathOnly = url.split(/[?#]/)[0] || "";
      if (pathOnly.length > 1 && pathOnly.endsWith("/")) {
        return pathOnly.slice(0, -1);
      }
      return pathOnly;
    };

    const isPathActive = (url) => {
      const normalized = normalizePath(url);
      if (!normalized) return false;
      const exact = currentPath === normalized;
      const prefix =
        normalized.length > 1 && currentPath.startsWith(normalized + "/");
      return exact || prefix;
    };

    const result = item.childMenus?.some(
      (child) =>
        isPathActive(child.menu.actionURL) ||
        hasActiveDescendant(child, currentPath),
    );
    return result;
  };

  /**
   * Check if a menu item has siblings with paths that start with its own path.
   * This helps avoid prefix matching conflicts (e.g., /analyzers matching /analyzers/errors).
   */
  const hasSiblingWithLongerPath = (menuItem, parentMenuItems) => {
    if (!parentMenuItems || !menuItem.menu.actionURL) return false;
    const normalizePath = (url) => {
      if (!url) return "";
      const pathOnly = url.split(/[?#]/)[0] || "";
      if (pathOnly.length > 1 && pathOnly.endsWith("/")) {
        return pathOnly.slice(0, -1);
      }
      return pathOnly;
    };
    const itemPath = normalizePath(menuItem.menu.actionURL);
    if (!itemPath) return false;
    return parentMenuItems.some(
      (sibling) =>
        sibling !== menuItem &&
        sibling.menu.actionURL &&
        normalizePath(sibling.menu.actionURL).startsWith(itemPath + "/"),
    );
  };

  const generateMenuItems = (
    menuItem,
    index,
    level,
    path,
    parentMenuItems = null,
  ) => {
    // Skip inactive menu items
    if (!menuItem.menu.isActive) {
      return (
        <React.Fragment key={menuItem.menu.elementId || path}></React.Fragment>
      );
    }

    // URL matching helpers
    // Normalize to ignore query/hash to fix cases like /WorkPlanByTest?type=test
    const normalizePath = (url) => {
      if (!url) return "";
      const pathOnly = url.split(/[?#]/)[0] || "";
      if (pathOnly.length > 1 && pathOnly.endsWith("/")) {
        return pathOnly.slice(0, -1);
      }
      return pathOnly;
    };

    const currentPath = normalizePath(location.pathname);
    const actionPath = normalizePath(menuItem.menu.actionURL);
    const itemId = menuItem.menu.elementId || "unknown";

    const exactMatch = actionPath && currentPath === actionPath;
    const prefixMatch =
      actionPath &&
      actionPath.length > 1 &&
      currentPath.startsWith(actionPath + "/");
    const hasChildren = menuItem.childMenus.length > 0;

    // Check if this menu item has siblings with paths that start with its own path.
    // If so, only use exact matching to avoid conflicts (e.g., /analyzers vs /analyzers/errors).
    const hasSiblingConflict = hasChildren
      ? false // Parent items don't need this check
      : hasSiblingWithLongerPath(menuItem, parentMenuItems);

    // Check if the current URL has query parameters and this menu item's normalized path matches.
    // If so, we need to compare full URLs (including query params) to avoid conflicts where
    // multiple menu items map to the same route with different query params
    // (e.g., /SampleEdit?type=readonly vs /SampleEdit?type=readwrite).
    // Note: We check this for ALL menu items with matching normalized paths, not just siblings,
    // because items in different branches (like "View" under "Study" vs "Edit Order" under "Order")
    // can still conflict.
    const currentHasQueryParams = location.search && location.search.length > 0;
    const needsFullUrlComparison =
      !hasChildren &&
      currentHasQueryParams &&
      exactMatch &&
      menuItem.menu.actionURL &&
      menuItem.menu.actionURL.includes("?");

    // Active rule:
    // - Parent items: exact match only
    // - Leaf items with query param conflicts: exact match AND full URL match (including query params)
    // - Leaf items with prefix-conflict siblings: exact match only
    // - Other leaf items: exact OR prefix match
    let isLeafActive;
    if (hasChildren) {
      // Parent items: exact match only
      isLeafActive = !!actionPath && exactMatch;
    } else if (needsFullUrlComparison) {
      // When the current URL has query params and this menu item's actionURL also has query params,
      // compare full URLs to ensure only the exact match is active
      // This handles cases like /SampleEdit?type=readonly vs /SampleEdit?type=readwrite
      const currentFullUrl = location.pathname + location.search;
      const actionFullUrl = menuItem.menu.actionURL || "";
      // Normalize both by removing trailing slashes for comparison
      const normalizeUrl = (url) => {
        if (!url) return "";
        const trimmed = url.trim();
        return trimmed.endsWith("/") && trimmed.length > 1
          ? trimmed.slice(0, -1)
          : trimmed;
      };
      const currentNormalized = normalizeUrl(currentFullUrl);
      const actionNormalized = normalizeUrl(actionFullUrl);
      isLeafActive = currentNormalized === actionNormalized;
    } else {
      // Normal case: exact or prefix match (if no sibling conflicts)
      isLeafActive =
        !!actionPath && (exactMatch || (!hasSiblingConflict && prefixMatch));
    }

    // Handler for label click - navigate (leaf items only)
    const handleLabelClick = (e) => {
      e.preventDefault();
      e.stopPropagation();

      if (hasChildren) {
        return; // parent handled by SideNavMenu toggle
      }

      if (menuItem.menu.actionURL) {
        if (menuItem.menu.openInNewWindow) {
          window.open(menuItem.menu.actionURL);
        } else {
          history.push(menuItem.menu.actionURL);
        }
      }
    };

    const hasActiveChild = hasActiveDescendant(menuItem, currentPath);

    // Parent with children: use Carbon SideNavMenu; on expand, optionally navigate to first child
    if (hasChildren) {
      // CRITICAL FIX: Only mark parent menu items as active if they themselves match the path exactly.
      // Do NOT mark them as active just because they have active children - this causes Carbon to
      // apply active styles to ALL submenu buttons, not just the active one.
      // Instead, use expanded state to show which parent has active children.
      const carbonIsActive = isLeafActive; // Only true if this parent item's own path matches
      // Use controlled expanded prop instead of defaultExpanded to ensure proper collapse behavior
      const carbonExpanded =
        !!menuItem.expanded ||
        hasActiveChild ||
        (defaultMode === SIDENAV_MODES.LOCK && hasActiveChild);
      return (
        // Wrapper span with ID for backward compatibility with Cypress selectors (span#menu_xxx)
        <span key={itemId} id={menuItem.menu.elementId}>
          <SideNavMenu
            // IMPORTANT: use stable key (elementId) to prevent React from reusing the wrong subtree
            // when the menu list shape changes (roles/plugins/async load).
            title={intl.formatMessage({ id: menuItem.menu.displayKey })}
            defaultExpanded={carbonExpanded}
            isActive={carbonIsActive}
            onToggle={(expanded) => {
              setMenuItemExpanded(menuItem, path);
            }}
            className={
              level === 0
                ? "top-level-menu-item"
                : "reduced-padding-nav-menu-item"
            }
          >
            <span
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
              }}
            >
              {menuItem.childMenus.map((childMenuItem, childIndex) => {
                return generateMenuItems(
                  childMenuItem,
                  childIndex,
                  level + 1,
                  path + ".childMenus[" + childIndex + "]",
                  menuItem.childMenus, // Pass parent's children for sibling check
                );
              })}
            </span>
          </SideNavMenu>
        </span>
      );
    }

    // Leaf item - wrapped in span for backward compatibility with Cypress selectors
    return (
      <span
        key={itemId}
        id={menuItem.menu.elementId}
        data-cy={`${menuItem.menu.elementId.replace(/[^\w\s]/gi, "_")}`}
      >
        <SideNavMenuItem
          id={menuItem.menu.elementId + "_nav"}
          className={
            level === 0
              ? "top-level-menu-item"
              : "reduced-padding-nav-menu-item"
          }
          isActive={isLeafActive}
          href={menuItem.menu.actionURL || undefined}
          target={menuItem.menu.openInNewWindow ? "_blank" : undefined}
          rel={menuItem.menu.openInNewWindow ? "noreferrer" : undefined}
          onClick={handleLabelClick}
          aria-current={isLeafActive ? "page" : undefined}
          style={level === 0 ? undefined : { width: "100%" }}
        >
          <span
            style={{
              display: "flex",
              width: "100%",
              marginLeft: level === 0 ? 0 : `${(level - 1) * 0.5}rem`,
            }}
          >
            <span style={{ fontSize: `${100 - 5 * Math.max(level - 1, 0)}%` }}>
              <FormattedMessage id={menuItem.menu.displayKey} />
            </span>
          </span>
        </SideNavMenuItem>
      </span>
    );
  };

  const setMenuItemExpanded = (menuItem, path) => {
    // IMPORTANT: functional update avoids stale-state races that can scramble expansion state.
    setMenus((prev) => {
      const newMenus = { ...prev };
      const targetId = menuItem?.menu?.elementId;

      // IMPORTANT: toggle expansion by stable elementId, NOT by index-based JSONPath.
      // Index-based paths can point at the wrong node if the menu shape changes.
      const toggleById = (items) => {
        return (items || []).map((it) => {
          const id = it?.menu?.elementId;
          if (!id) return it;
          if (id === targetId) {
            return { ...it, expanded: !it.expanded };
          }
          if (it.childMenus && it.childMenus.length > 0) {
            return { ...it, childMenus: toggleById(it.childMenus) };
          }
          return it;
        });
      };

      newMenus.menu = toggleById(newMenus.menu || []);

      // Persist expanded state map for this context
      try {
        const expandedMap = {};
        const captureExpanded = (items) => {
          (items || []).forEach((it) => {
            expandedMap[it.menu.elementId] = !!it.expanded;
            if (it.childMenus) {
              captureExpanded(it.childMenus);
            }
          });
        };
        captureExpanded(newMenus.menu || []);
        localStorage.setItem(
          `${storageKeyPrefix}ExpandedMap`,
          JSON.stringify(expandedMap),
        );
      } catch (e) {
        // ignore
      }

      return newMenus;
    });
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
                id="sidenav-menu-button"
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
                    onClick={() => handlePanelToggle(searchBar ? "" : "search")}
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
                  expanded={mode !== SIDENAV_MODES.CLOSE}
                  isFixedNav={mode === SIDENAV_MODES.LOCK}
                  // LOCK mode should be persistent; SHOW mode is temporary overlay
                  isPersistent={mode === SIDENAV_MODES.LOCK}
                  isChildOfHeader={true}
                  onMouseEnter={() => {
                    if (mode === SIDENAV_MODES.SHOW && hideTimerRef.current) {
                      clearTimeout(hideTimerRef.current);
                      hideTimerRef.current = null;
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (mode === SIDENAV_MODES.SHOW) {
                      const target = e.relatedTarget;
                      const navEl = e.currentTarget;
                      const headerEl = document.getElementById("mainHeader");
                      const menuButton = document.getElementById(
                        "sidenav-menu-button",
                      );
                      const isNode =
                        target && typeof target.contains === "function";
                      if (!isNode) {
                        return;
                      }
                      const insideNav = navEl && navEl.contains(target);
                      const insideHeader =
                        headerEl && headerEl.contains(target);
                      const insideMenuButton =
                        menuButton && menuButton.contains(target);

                      if (insideNav || insideHeader || insideMenuButton) {
                        return;
                      }

                      if (hideTimerRef.current) {
                        clearTimeout(hideTimerRef.current);
                      }

                      hideTimerRef.current = setTimeout(() => {
                        setMode(SIDENAV_MODES.CLOSE);
                        hideTimerRef.current = null;
                      }, 350);
                    }
                  }}
                >
                  <SideNavItems>
                    {autoExpandedMenus.map((childMenuItem, index) => {
                      return generateMenuItems(
                        childMenuItem,
                        index,
                        0,
                        "$.menu[" + index + "]",
                        null, // Top level items have no parent siblings
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
