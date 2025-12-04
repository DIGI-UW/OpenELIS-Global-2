import React from "react";
import PropTypes from "prop-types";
import {
  Header,
  HeaderContainer,
  HeaderMenuButton,
  HeaderName,
  SideNav,
  SideNavItems,
  Content,
  Theme,
} from "@carbon/react";
import { useSideNavPreference } from "./useSideNavPreference";
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
}) {
  // Use the custom hook for state management and persistence
  const { isExpanded, toggle } = useSideNavPreference({
    defaultExpanded,
    storageKeyPrefix,
  });

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
              {/* Navigation items will be populated in M2 (Navigation milestone) */}
              {/* This milestone (M1) focuses on the core layout and toggle functionality */}
              {/* Empty placeholder required by Carbon SideNavItems */}
              <></>
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
};

TwoModeLayout.defaultProps = {
  children: null,
  defaultExpanded: false,
  storageKeyPrefix: "default",
};

export default TwoModeLayout;
