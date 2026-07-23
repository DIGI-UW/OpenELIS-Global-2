import { renderHook } from "@testing-library/react-hooks";
import { BrowserRouter } from "react-router-dom";
import { useMenuAutoExpand } from "./useMenuAutoExpand";

/**
 * Unit tests for useMenuAutoExpand hook
 *
 * This hook manages auto-expansion of menu items based on current route.
 * Tests cover:
 * - Expanding parent when child route is active
 * - Expanding multiple ancestors for deeply nested routes
 * - Route prefix matching (/analyzers/qc matches /analyzers/qc/alerts)
 * - Not expanding unrelated branches
 *
 * @see spec.md User Story 3: Hierarchical Navigation Structure (P2)
 * @see research.md R5: Menu Auto-Expansion Algorithm
 */

// Wrapper to provide React Router context
const wrapper = ({ children }) => <BrowserRouter>{children}</BrowserRouter>;

describe("useMenuAutoExpand", () => {
  describe("single level expansion", () => {
    /**
     * Test: Expands parent when child route is active
     * @see spec.md US3 Acceptance Scenario 1: Navigate to child, parent auto-expands
     */
    test("testAutoExpand_ActiveChildRoute_ExpandsParent", () => {
      const menus = [
        {
          menu: {
            id: "1",
            displayKey: "menu.analyzers",
            actionURL: "/analyzers",
            isActive: true,
          },
          childMenus: [
            {
              menu: {
                id: "1.1",
                displayKey: "menu.analyzers.errors",
                actionURL: "/analyzers/errors",
                isActive: true,
              },
              childMenus: [],
            },
          ],
          expanded: false,
        },
      ];

      // Simulate being on /analyzers/errors route
      window.history.pushState({}, "", "/analyzers/errors");

      const { result } = renderHook(() => useMenuAutoExpand(menus), {
        wrapper,
      });

      // Parent should be expanded
      const updatedMenus = result.current;
      expect(updatedMenus[0].expanded).toBe(true);
    });

    /**
     * Test: Does not expand parent when on parent route
     */
    test("testAutoExpand_ParentRouteActive_ParentNotExpanded", () => {
      const menus = [
        {
          menu: {
            id: "1",
            displayKey: "menu.analyzers",
            actionURL: "/analyzers",
            isActive: true,
          },
          childMenus: [
            {
              menu: {
                id: "1.1",
                displayKey: "menu.analyzers.errors",
                actionURL: "/analyzers/errors",
                isActive: true,
              },
              childMenus: [],
            },
          ],
          expanded: false,
        },
      ];

      // Simulate being on /analyzers route (parent, not child)
      window.history.pushState({}, "", "/analyzers");

      const { result } = renderHook(() => useMenuAutoExpand(menus), {
        wrapper,
      });

      // Parent should remain collapsed (user on parent page, not child)
      const updatedMenus = result.current;
      expect(updatedMenus[0].expanded).toBe(false);
    });
  });

  describe("nested expansion", () => {
    /**
     * Test: Expands multiple ancestors for deeply nested routes
     * @see spec.md FR-010: Support up to 4 levels of hierarchy
     */
    test("testAutoExpand_DeepNestedRoute_ExpandsAllAncestors", () => {
      const menus = [
        {
          menu: {
            id: "1",
            displayKey: "menu.level1",
            actionURL: "/level1",
            isActive: true,
          },
          childMenus: [
            {
              menu: {
                id: "1.1",
                displayKey: "menu.level2",
                actionURL: "/level1/level2",
                isActive: true,
              },
              childMenus: [
                {
                  menu: {
                    id: "1.1.1",
                    displayKey: "menu.level3",
                    actionURL: "/level1/level2/level3",
                    isActive: true,
                  },
                  childMenus: [],
                },
              ],
              expanded: false,
            },
          ],
          expanded: false,
        },
      ];

      // Simulate being on deep nested route
      window.history.pushState({}, "", "/level1/level2/level3");

      const { result } = renderHook(() => useMenuAutoExpand(menus), {
        wrapper,
      });

      const updatedMenus = result.current;
      // Both level1 and level2 should be expanded
      expect(updatedMenus[0].expanded).toBe(true); // Level 1
      expect(updatedMenus[0].childMenus[0].expanded).toBe(true); // Level 2
    });
  });

  describe("route prefix matching", () => {
    /**
     * Test: Handles route prefix matching
     * @see research.md R5: location.pathname.startsWith(item.menu.actionURL + '/')
     */
    test("testAutoExpand_RoutePrefix_ExpandsParent", () => {
      const menus = [
        {
          menu: {
            id: "1",
            displayKey: "menu.analyzers",
            actionURL: "/analyzers",
            isActive: true,
          },
          childMenus: [
            {
              menu: {
                id: "1.1",
                displayKey: "menu.analyzers.qc",
                actionURL: "/analyzers/qc",
                isActive: true,
              },
              childMenus: [],
            },
          ],
          expanded: false,
        },
      ];

      // Simulate being on sub-route of /analyzers/qc
      window.history.pushState({}, "", "/analyzers/qc/alerts");

      const { result } = renderHook(() => useMenuAutoExpand(menus), {
        wrapper,
      });

      const updatedMenus = result.current;
      // Parent should be expanded (route starts with /analyzers)
      expect(updatedMenus[0].expanded).toBe(true);
    });

    /**
     * Test: Does not match partial segments (e.g., /analyzer should not match /analyzers)
     */
    test("testAutoExpand_PartialSegment_DoesNotMatch", () => {
      const menus = [
        {
          menu: {
            id: "1",
            displayKey: "menu.analyzers",
            actionURL: "/analyzers",
            isActive: true,
          },
          childMenus: [
            {
              menu: {
                id: "1.1",
                displayKey: "menu.analyzers.errors",
                actionURL: "/analyzers/errors",
                isActive: true,
              },
              childMenus: [],
            },
          ],
          expanded: false,
        },
      ];

      // Simulate being on route that partially matches
      window.history.pushState({}, "", "/analyzer");

      const { result } = renderHook(() => useMenuAutoExpand(menus), {
        wrapper,
      });

      const updatedMenus = result.current;
      // Should NOT expand (partial match)
      expect(updatedMenus[0].expanded).toBe(false);
    });
  });

  describe("unrelated branches", () => {
    /**
     * Test: Does not expand unrelated branches
     */
    test("testAutoExpand_UnrelatedRoute_DoesNotExpandBranch", () => {
      const menus = [
        {
          menu: {
            id: "1",
            displayKey: "menu.analyzers",
            actionURL: "/analyzers",
            isActive: true,
          },
          childMenus: [
            {
              menu: {
                id: "1.1",
                displayKey: "menu.analyzers.errors",
                actionURL: "/analyzers/errors",
                isActive: true,
              },
              childMenus: [],
            },
          ],
          expanded: false,
        },
        {
          menu: {
            id: "2",
            displayKey: "menu.samples",
            actionURL: "/samples",
            isActive: true,
          },
          childMenus: [
            {
              menu: {
                id: "2.1",
                displayKey: "menu.samples.search",
                actionURL: "/samples/search",
                isActive: true,
              },
              childMenus: [],
            },
          ],
          expanded: false,
        },
      ];

      // Simulate being on /samples/search
      window.history.pushState({}, "", "/samples/search");

      const { result } = renderHook(() => useMenuAutoExpand(menus), {
        wrapper,
      });

      const updatedMenus = result.current;
      // Analyzers branch should NOT expand
      expect(updatedMenus[0].expanded).toBe(false);
      // Samples branch SHOULD expand
      expect(updatedMenus[1].expanded).toBe(true);
    });
  });

  describe("edge cases", () => {
    /**
     * Test: Handles empty menus array
     */
    test("testAutoExpand_EmptyMenus_ReturnsEmpty", () => {
      const menus = [];

      window.history.pushState({}, "", "/any-route");

      const { result } = renderHook(() => useMenuAutoExpand(menus), {
        wrapper,
      });

      expect(result.current).toEqual([]);
    });

    /**
     * Test: Handles route not matching any menu
     */
    test("testAutoExpand_NoMatchingRoute_AllCollapsed", () => {
      const menus = [
        {
          menu: {
            id: "1",
            displayKey: "menu.analyzers",
            actionURL: "/analyzers",
            isActive: true,
          },
          childMenus: [],
          expanded: false,
        },
      ];

      window.history.pushState({}, "", "/unrelated-route");

      const { result } = renderHook(() => useMenuAutoExpand(menus), {
        wrapper,
      });

      const updatedMenus = result.current;
      expect(updatedMenus[0].expanded).toBe(false);
    });

    /**
     * Test: Preserves existing expanded state for non-active branches
     * UPDATED 2025-12: Hook now preserves manual expansions (user feedback)
     * @see useMenuAutoExpand.js comment about autocollapse being removed
     */
    test("testAutoExpand_NonActiveBranch_PreservesState", () => {
      const menus = [
        {
          menu: {
            id: "1",
            elementId: "menu_analyzers",
            displayKey: "menu.analyzers",
            actionURL: "/analyzers",
            isActive: true,
          },
          childMenus: [],
          expanded: true, // Already expanded - should be preserved
        },
      ];

      window.history.pushState({}, "", "/samples");

      const { result } = renderHook(() => useMenuAutoExpand(menus), {
        wrapper,
      });

      const updatedMenus = result.current;
      // Hook preserves expanded state (does NOT auto-collapse non-active branches)
      // Per user feedback: "autocollapse causes consistency issues and changes user focus"
      expect(updatedMenus[0].expanded).toBe(true);
    });
  });
});
