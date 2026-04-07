import { useState, useEffect } from "react";
import { useLocation } from "react-router-dom";

/**
 * Custom hook for auto-expanding menu items based on current route.
 *
 * This hook:
 * - Takes a menu structure as input
 * - Returns an updated menu structure with `expanded` flags set
 * - Auto-expands parent items when child route is active
 * - Uses recursive depth-first algorithm
 * - Triggers on route change
 *
 * @see spec.md User Story 3: Hierarchical Navigation Structure (P2)
 * @see research.md R5: Menu Auto-Expansion Algorithm
 *
 * @param {Array} initialMenus - Menu structure with menu items
 * @returns {Array} Updated menu structure with expanded flags
 *
 * @example
 * const menus = useMenuAutoExpand(initialMenus);
 */
export function useMenuAutoExpand(initialMenus) {
  const location = useLocation();
  const [menus, setMenus] = useState(initialMenus);

  useEffect(() => {
    if (!initialMenus || initialMenus.length === 0) {
      setMenus([]);
      return;
    }

    // Deep clone to avoid mutating original
    const newMenus = JSON.parse(JSON.stringify(initialMenus));

    /**
     * Recursive function to mark menu items for expansion.
     * Returns true if this item or any descendant matches the current route.
     *
     * Only expands parents of the active route to ensure navigation context is visible.
     *
     * @param {Array} items - Menu items to process
     * @returns {boolean} true if this branch contains the active route
     */
    const markActiveExpanded = (items) => {
      let isActiveBranch = false;

      items.forEach((item) => {
        // Recursively check children first (depth-first)
        if (item.childMenus && item.childMenus.length > 0) {
          if (markActiveExpanded(item.childMenus)) {
            item.expanded = true; // Expand parent of active child
            isActiveBranch = true;
          }
        }

        // Check if this item matches current route
        // Match exact route or prefix (e.g., /analyzers/qc matches /analyzers/qc/alerts)
        // Guard against empty URLs (parent folders with no actionURL)
        if (
          item.menu.actionURL &&
          item.menu.actionURL.length > 1 && // Must be more than just "/"
          (item.menu.actionURL === location.pathname ||
            location.pathname.startsWith(item.menu.actionURL + "/"))
        ) {
          isActiveBranch = true;
        }
      });

      return isActiveBranch;
    };

    // Process all top-level menu items
    markActiveExpanded(newMenus);

    setMenus(newMenus);
  }, [location.pathname, initialMenus]);

  return menus;
}

export default useMenuAutoExpand;
