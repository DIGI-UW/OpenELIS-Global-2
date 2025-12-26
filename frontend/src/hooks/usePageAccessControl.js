import { useCallback, useMemo, useEffect, useState } from "react";
import { usePermissions } from "./usePermissions";

/**
 * Custom hook for page-level access control in notebook workflows.
 *
 * Handles:
 * - Checking if user has access to individual pages based on allowedRoles
 * - Adding hasAccess property to each page
 * - Auto-navigating to first accessible page if current page is restricted
 * - Preventing navigation to restricted pages
 *
 * @param {Array} pages - Array of page objects (may include allowedRoles property)
 * @param {Array} defaultPages - Default pages to use if no pages provided
 * @param {number} initialActivePage - Initial active page index (default: 0)
 * @param {Object} options - Optional configuration
 * @param {boolean} options.isCreating - If true, bypasses page-level role restrictions
 *                                       (used during new entry creation to show all pages)
 * @returns {Object} - { effectivePages, activePage, setActivePage, handlePageChange }
 *
 * @example
 * // During entry creation - show all pages
 * const { effectivePages } = usePageAccessControl(pages, DEFAULT_PAGES, 0, { isCreating: true });
 *
 * // When viewing/editing existing entry - apply role restrictions
 * const { effectivePages } = usePageAccessControl(pages, DEFAULT_PAGES, 0, { isCreating: false });
 */
export const usePageAccessControl = (
  pages,
  defaultPages = [],
  initialActivePage = 0,
  options = {},
) => {
  const { isCreating = false } = options;
  const { hasRoleForCurrentLabUnit } = usePermissions();
  const [activePage, setActivePage] = useState(initialActivePage);

  /**
   * Check if user has access to a specific page based on its allowedRoles.
   * If no roles are defined, access is granted to everyone.
   * If isCreating is true, access is always granted (show all pages during entry creation).
   */
  const hasPageAccess = useCallback(
    (page) => {
      // During entry creation, show all pages - no restrictions
      if (isCreating) {
        return true;
      }

      const pageRoles = page.allowedRoles
        ? Array.isArray(page.allowedRoles)
          ? page.allowedRoles
          : Array.from(page.allowedRoles)
        : [];

      // No roles defined = no restriction = allow everyone
      if (pageRoles.length === 0) {
        return true;
      }

      // Check if user has any of the page's required roles
      return hasRoleForCurrentLabUnit(pageRoles);
    },
    [hasRoleForCurrentLabUnit, isCreating],
  );

  /**
   * Pages with access info computed.
   * Uses provided pages if available, otherwise falls back to defaultPages.
   */
  const effectivePages = useMemo(() => {
    let pagesToUse = defaultPages;
    if (pages && pages.length > 0) {
      pagesToUse = pages;
    }

    // Add hasAccess property to each page for navigation
    return pagesToUse.map((page) => ({
      ...page,
      hasAccess: hasPageAccess(page),
    }));
  }, [pages, defaultPages, hasPageAccess]);

  /**
   * Auto-navigate to first accessible page if current page is restricted.
   */
  useEffect(() => {
    if (effectivePages.length > 0) {
      const currentPage = effectivePages[activePage];
      // If current page is not accessible, find and navigate to first accessible page
      if (!currentPage || !currentPage.hasAccess) {
        const firstAccessibleIndex = effectivePages.findIndex(
          (page) => page.hasAccess,
        );
        if (firstAccessibleIndex >= 0 && firstAccessibleIndex !== activePage) {
          setActivePage(firstAccessibleIndex);
        }
      }
    }
  }, [effectivePages, activePage]);

  /**
   * Handle page change with access control.
   * Prevents navigation to pages the user doesn't have access to.
   */
  const handlePageChange = useCallback(
    (pageIndex) => {
      if (effectivePages[pageIndex] && !effectivePages[pageIndex].hasAccess) {
        return; // Don't navigate to restricted pages
      }
      setActivePage(pageIndex);
    },
    [effectivePages],
  );

  /**
   * Check if any page is accessible to the user.
   */
  const hasAnyAccessiblePage = useMemo(() => {
    return effectivePages.some((page) => page.hasAccess);
  }, [effectivePages]);

  /**
   * Get the first accessible page index.
   */
  const firstAccessiblePageIndex = useMemo(() => {
    return effectivePages.findIndex((page) => page.hasAccess);
  }, [effectivePages]);

  return {
    effectivePages,
    activePage,
    setActivePage,
    handlePageChange,
    hasPageAccess,
    hasAnyAccessiblePage,
    firstAccessiblePageIndex,
  };
};

export default usePageAccessControl;
