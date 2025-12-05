import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import { useIntl } from "react-intl";
import {
  Header,
  HeaderContainer,
  HeaderMenuButton,
  HeaderName,
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
function TwoModeLayout({
  children,
  defaultExpanded = false,
  storageKeyPrefix = "default",
  menus: menusProp, // For testing - allows injecting menu data
}) {
  const intl = useIntl();

  // Use the custom hook for state management and persistence
  const { isExpanded, toggle } = useSideNavPreference({
    defaultExpanded,
    storageKeyPrefix,
  });

  // Menu state
  const [menusRaw, setMenusRaw] = useState(menusProp || { menu: [] });
  const [loadingMenus, setLoadingMenus] = useState(!menusProp);

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
        style={{ paddingLeft: `${level * 0.5 + 1}rem` }}
      >
        {intl.formatMessage({ id: menuItem.menu.displayKey })}
      </SideNavMenuItem>
    );
  };

  return (
    <HeaderContainer
      render={({ isSideNavExpanded, onClickSideNavExpand }) => (
        <Theme theme="g100">
          {/* Header with menu toggle button */}
          <Header aria-label="OpenELIS Global">
            <HeaderMenuButton
              aria-label={isExpanded ? "Close menu" : "Open menu"}
              onClick={toggle}
              isActive={isExpanded}
              aria-expanded={isExpanded}
            />
            <HeaderName href="/" prefix="">
              OpenELIS Global
            </HeaderName>
          </Header>

          {/* Fixed sidenav that pushes content */}
          <SideNav
            aria-label="Side navigation"
            isFixedNav={true}
            isChildOfHeader={true}
            expanded={isExpanded}
          >
            <SideNavItems>
              {menus.menu && menus.menu.length > 0 ? (
                menus.menu.map((menuItem, index) =>
                  generateMenuItems(menuItem, index, 0, `menu[${index}]`),
                )
              ) : (
                <></>
              )}
            </SideNavItems>
          </SideNav>

          {/* Content wrapper with dynamic margin based on sidenav state */}
          <div
            data-testid="content-wrapper"
            className={isExpanded ? "content-expanded" : "content-collapsed"}
          >
            <Content>{children}</Content>
          </div>
        </Theme>
      )}
    />
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
};

TwoModeLayout.defaultProps = {
  children: null,
  defaultExpanded: false,
  storageKeyPrefix: "default",
  menus: null,
};

export default TwoModeLayout;
