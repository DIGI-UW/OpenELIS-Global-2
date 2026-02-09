import React, {
  useState,
  useEffect,
  useMemo,
  useRef,
  useCallback,
  useContext,
} from "react";
import PropTypes from "prop-types";
import { FormattedMessage, useIntl } from "react-intl";
import { useLocation } from "react-router-dom";
import {
  Header,
  HeaderContainer,
  HeaderMenuButton,
  HeaderName,
  HeaderGlobalBar,
  SideNav,
  SideNavItems,
  SideNavMenu,
  SideNavMenuItem,
  Content,
  Theme,
} from "@carbon/react";
import { useSideNavPreference } from "./useSideNavPreference";
import { useMenuAutoExpand } from "./useMenuAutoExpand";
import { getFromOpenElisServer } from "../utils/Utils";
import { ConfigurationContext } from "./Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import "../Style.css";
import "./TwoModeLayout.css";

/**
 * TwoModeLayout - A two-mode sidenav layout using Carbon Design System
 *
 * This component provides:
 * - Fixed sidenav with toggle between expanded (256px) and collapsed (48px rail)
 * - Content pushing (not overlay) when sidenav expands/collapses
 * - Persistence of user preference via localStorage
 * - Carbon transition timing (0.11s cubic-bezier)
 * - Support for page-level configuration via props
 *
 * @see spec.md User Story 1: Toggle Sidenav Between Modes (P1)
 * @see spec.md FR-001 through FR-008
 * @see data-model.md TwoModeLayoutProps interface
 *
 * @example
 * // Basic usage
 * <TwoModeLayout>
 *   <MyPageContent />
 * </TwoModeLayout>
 *
 * @example
 * // With page-specific configuration
 * <TwoModeLayout defaultExpanded={true} storageKeyPrefix="analyzer">
 *   <AnalyzerPageContent />
 * </TwoModeLayout>
 */
const SIDENAV_MODES = {
  SHOW: "show", // overlay
  LOCK: "lock", // push content
  CLOSE: "close", // rail
};

function TwoModeLayout({
  children,
  defaultExpanded, // deprecated: maps to lock/close
  defaultMode,
  storageKeyPrefix = "default",
  menus: menusProp, // For testing - allows injecting menu data
  headerActions,
}) {
  const intl = useIntl();
  const location = useLocation();
  const sideNavRef = useRef(null);
  const { configurationProperties } = useContext(ConfigurationContext) || {};
  const { userSessionDetails } = useContext(UserSessionDetailsContext) || {};

  /**
   * Logo component matching legacy Header.js
   */
  const logo = () => (
    <picture>
      <img className="logo" src={`../images/openelis_logo.png`} alt="logo" />
    </picture>
  );

  const normalizedDefaultMode = useMemo(() => {
    if (typeof defaultExpanded === "boolean") {
      return defaultExpanded ? SIDENAV_MODES.LOCK : SIDENAV_MODES.CLOSE;
    }
    if (defaultMode && Object.values(SIDENAV_MODES).includes(defaultMode)) {
      return defaultMode;
    }
    return SIDENAV_MODES.CLOSE;
  }, [defaultMode, defaultExpanded]);

  // Use the custom hook for state management and persistence
  const { mode, setMode } = useSideNavPreference({
    defaultMode: normalizedDefaultMode,
    defaultExpanded,
    storageKeyPrefix,
  });

  // Menu state
  const [menusRaw, setMenusRaw] = useState(menusProp || { menu: [] });
  const [, setLoadingMenus] = useState(!menusProp);

  // Fetch menus from API (unless provided via props for testing)
  useEffect(() => {
    if (!menusProp) {
      setLoadingMenus(true);
      getFromOpenElisServer("/rest/menu", (response) => {
        if (response) {
          setMenusRaw(response);
        }
        setLoadingMenus(false);
      });
    }
  }, [menusProp]);

  // Auto-expand menu based on current route
  const menusWithAutoExpand = useMenuAutoExpand(menusRaw.menu || []);

  // Combine auto-expanded menus back into menu structure
  const menus = { ...menusRaw, menu: menusWithAutoExpand };

  /**
   * Active route helper
   */
  const isRouteActive = useCallback(
    (actionURL) => {
      if (!actionURL) return false;
      if (location.pathname === actionURL) return true;
      if (location.pathname.startsWith(actionURL + "/")) return true;
      return false;
    },
    [location.pathname],
  );

  /**
   * Click outside to close when in SHOW (overlay) mode
   */
  useEffect(() => {
    if (mode !== SIDENAV_MODES.SHOW) {
      return undefined;
    }
    const handleClickOutside = (event) => {
      if (sideNavRef.current && !sideNavRef.current.contains(event.target)) {
        setMode(SIDENAV_MODES.CLOSE);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [mode, setMode]);

  /**
   * Recursive menu item generator supporting:
   * - Parent items with children (expandable)
   * - Leaf items (navigable)
   * - Up to 4 levels of nesting
   * - Internationalized labels
   *
   * @see research.md A4: Menu Item Rendering with Hierarchical Support
   */
  const generateMenuItems = (menuItem, index, level, path) => {
    if (!menuItem.menu.isActive) {
      return null;
    }

    const active = isRouteActive(menuItem.menu.actionURL);

    // Top-level item with children - render as expandable menu
    if (level === 0 && menuItem.childMenus && menuItem.childMenus.length > 0) {
      return (
        <SideNavMenu
          key={path}
          aria-label={intl.formatMessage({ id: menuItem.menu.displayKey })}
          title={intl.formatMessage({ id: menuItem.menu.displayKey })}
          defaultExpanded={menuItem.expanded || false}
        >
          {menuItem.childMenus.map((child, idx) =>
            generateMenuItems(
              child,
              idx,
              level + 1,
              `${path}.childMenus[${idx}]`,
            ),
          )}
        </SideNavMenu>
      );
    }

    // Top-level item without children - render as direct link
    if (level === 0) {
      return (
        <SideNavMenuItem
          key={path}
          href={menuItem.menu.actionURL}
          target={menuItem.menu.openInNewWindow ? "_blank" : undefined}
          rel={
            menuItem.menu.openInNewWindow ? "noopener noreferrer" : undefined
          }
          isActive={active}
          aria-current={active ? "page" : undefined}
        >
          {intl.formatMessage({ id: menuItem.menu.displayKey })}
        </SideNavMenuItem>
      );
    }

    // Nested item with children - render as nested menu (up to level 3)
    if (menuItem.childMenus && menuItem.childMenus.length > 0 && level < 3) {
      return (
        <SideNavMenu
          key={path}
          aria-label={intl.formatMessage({ id: menuItem.menu.displayKey })}
          title={intl.formatMessage({ id: menuItem.menu.displayKey })}
          defaultExpanded={menuItem.expanded || false}
        >
          {menuItem.childMenus.map((child, idx) =>
            generateMenuItems(
              child,
              idx,
              level + 1,
              `${path}.childMenus[${idx}]`,
            ),
          )}
        </SideNavMenu>
      );
    }

    // Nested item (leaf or max depth reached) - render with indentation
    return (
      <SideNavMenuItem
        key={path}
        href={menuItem.menu.actionURL}
        target={menuItem.menu.openInNewWindow ? "_blank" : undefined}
        rel={menuItem.menu.openInNewWindow ? "noopener noreferrer" : undefined}
        isActive={active}
        aria-current={active ? "page" : undefined}
        style={{ paddingLeft: `${level * 0.5 + 1}rem` }}
      >
        {intl.formatMessage({ id: menuItem.menu.displayKey })}
      </SideNavMenuItem>
    );
  };

  return (
    <div className="container">
      <Theme>
        {/* Theme wrapper uses custom blue theme from index.scss */}
        <div style={{ display: "flex", flexDirection: "column" }}>
          <HeaderContainer
            render={({ isSideNavExpanded, onClickSideNavExpand }) => (
              <Header
                id="mainHeader"
                className="mainHeader"
                aria-label="OpenELIS Global"
              >
                {userSessionDetails?.authenticated && (
                  <HeaderMenuButton
                    data-cy="menuButton"
                    aria-label={isSideNavExpanded ? "Close menu" : "Open menu"}
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
                <HeaderGlobalBar>{headerActions}</HeaderGlobalBar>
                {/* SideNav only shown when authenticated - matches develop Header.js */}
                {userSessionDetails?.authenticated && (
                  <SideNav
                    aria-label="Side navigation"
                    expanded={isSideNavExpanded}
                    isPersistent={false}
                  >
                    <SideNavItems>
                      {menus.menu && menus.menu.length > 0 ? (
                        menus.menu.map((menuItem, index) =>
                          generateMenuItems(
                            menuItem,
                            index,
                            0,
                            `menu[${index}]`,
                          ),
                        )
                      ) : (
                        <></>
                      )}
                    </SideNavItems>
                  </SideNav>
                )}
              </Header>
            )}
          />
        </div>
      </Theme>
      {/* Content area - white theme for proper form styling */}
      <Theme theme="white">
        <Content data-testid="content-wrapper">{children}</Content>
      </Theme>
    </div>
  );
}

TwoModeLayout.propTypes = {
  /**
   * Page content to render in the main content area
   */
  children: PropTypes.node,

  /**
   * Default expansion state when no preference is stored.
   * Individual pages can override this for page-specific defaults.
   * @see spec.md US4: Page-Level Mode Configuration
   */
  defaultExpanded: PropTypes.bool,

  /**
   * Default mode when no preference is stored. supersedes defaultExpanded.
   * Values: 'show' | 'lock' | 'close'
   */
  defaultMode: PropTypes.oneOf(["show", "lock", "close"]),

  /**
   * Prefix for the localStorage key. Allows different pages to have
   * independent sidenav preferences.
   * Format: `{storageKeyPrefix}SideNavExpanded`
   * @see data-model.md localStorage Key Format
   */
  storageKeyPrefix: PropTypes.string,

  /**
   * Optional menu data (for testing). If provided, bypasses API fetch.
   * @see data-model.md MenuStructure interface
   */
  menus: PropTypes.shape({
    menu: PropTypes.arrayOf(PropTypes.object),
  }),

  /**
   * Header global actions (notifications, user menu, search, language, help)
   * Rendered inside Carbon HeaderGlobalBar
   */
  headerActions: PropTypes.node,
};

TwoModeLayout.defaultProps = {
  children: null,
  defaultExpanded: false,
  defaultMode: undefined,
  storageKeyPrefix: "default",
  menus: null,
  headerActions: null,
};

export default TwoModeLayout;
