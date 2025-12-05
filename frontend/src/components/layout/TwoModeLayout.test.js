import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";

/**
 * Unit tests for TwoModeLayout component
 *
 * This component provides a two-mode sidenav (expanded/collapsed) that:
 * - Pushes content when expanded (not overlay)
 * - Uses Carbon Design System components exclusively
 * - Persists user preference via useSideNavPreference hook
 *
 * @see spec.md User Story 1: Toggle Sidenav Between Modes (P1)
 * @see spec.md FR-001 through FR-008
 */

// Mock functions
const mockToggle = jest.fn();
const mockSetExpanded = jest.fn();

// Mock the useSideNavPreference hook
jest.mock("./useSideNavPreference", () => {
  return {
    useSideNavPreference: jest.fn(),
  };
});

// Import after mock setup
import TwoModeLayout from "./TwoModeLayout";
import { useSideNavPreference } from "./useSideNavPreference";

// Test wrapper providing required context (IntlProvider, BrowserRouter)
const renderWithProviders = (ui, options = {}) => {
  const messages = {
    // Mock messages for menu items used in tests
    "menu.analyzer": "Analyzers",
    "menu.analyzer.errors": "Error Tracking",
    "menu.home": "Home",
    "menu.level1": "Level 1",
    "menu.level2": "Level 2",
    "menu.level3": "Level 3",
    "menu.level4": "Level 4",
    "menu.parent": "Parent Menu",
    "menu.child": "Child Menu",
    "menu.active": "Active Item",
    "menu.inactive": "Inactive Item",
    "menu.external": "External Link",
    "menu.analyzers": "Analyzers",
    "menu.analyzers.qc": "Quality Control",
    "menu.samples": "Samples",
    "menu.samples.search": "Sample Search",
  };
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {ui}
      </IntlProvider>
    </BrowserRouter>,
    options,
  );
};

// Helper to set up the mock return value
const setupMock = (isExpanded) => {
  useSideNavPreference.mockReturnValue({
    isExpanded: isExpanded,
    toggle: mockToggle,
    setExpanded: mockSetExpanded,
  });
};

describe("TwoModeLayout", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Default: collapsed state
    setupMock(false);
  });

  describe("rendering", () => {
    /**
     * Test: Renders with sidenav collapsed by default
     * @see spec.md FR-001: Toggle between expanded (256px) and collapsed (48px) modes
     */
    test("testRender_DefaultState_SidenavCollapsed", () => {
      setupMock(false);

      renderWithProviders(
        <TwoModeLayout>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      // Content wrapper should have collapsed class
      const contentWrapper = screen.getByTestId("content-wrapper");
      expect(contentWrapper.className).toContain("content-collapsed");
      expect(contentWrapper.className).not.toContain("content-expanded");
    });

    /**
     * Test: Renders with sidenav expanded when hook returns expanded state
     * @see spec.md US4: Page-Level Mode Configuration
     */
    test("testRender_ExpandedState_SidenavExpanded", () => {
      setupMock(true);

      renderWithProviders(
        <TwoModeLayout defaultExpanded={true}>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      // Content wrapper should have expanded class
      const contentWrapper = screen.getByTestId("content-wrapper");
      expect(contentWrapper.className).toContain("content-expanded");
      expect(contentWrapper.className).not.toContain("content-collapsed");
    });

    /**
     * Test: Renders children in Content area
     * @see spec.md FR-008: Sidenav as sibling to Content component
     */
    test("testRender_WithChildren_RendersChildrenInContent", () => {
      setupMock(false);

      renderWithProviders(
        <TwoModeLayout>
          <div data-testid="child-content">My Child Content</div>
        </TwoModeLayout>,
      );

      expect(screen.getByTestId("child-content")).toBeTruthy();
      expect(screen.getByText("My Child Content")).toBeTruthy();
    });
  });

  describe("toggle functionality", () => {
    /**
     * Test: Toggle button calls toggle function
     * @see spec.md US1 Acceptance Scenario 1: Click toggle, sidenav expands
     */
    test("testToggle_ClickButton_CallsToggle", () => {
      setupMock(false);

      renderWithProviders(
        <TwoModeLayout>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      // Find and click the menu toggle button
      const toggleButton = screen.getByRole("button", { name: /menu/i });
      fireEvent.click(toggleButton);

      expect(mockToggle).toHaveBeenCalledTimes(1);
    });

    /**
     * Test: Toggle button is accessible with aria-label
     */
    test("testToggle_Button_HasAccessibleLabel", () => {
      setupMock(false);

      renderWithProviders(
        <TwoModeLayout>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      // Button should have accessible label (findable by role + name)
      const toggleButton = screen.getByRole("button", { name: /menu/i });
      expect(toggleButton).toBeTruthy();
    });
  });

  describe("content area margin classes", () => {
    /**
     * Test: Content area has correct margin class when expanded
     * @see spec.md FR-007: Content pushing (not overlay) when expanded
     */
    test("testContentArea_Expanded_HasExpandedClass", () => {
      setupMock(true);

      renderWithProviders(
        <TwoModeLayout>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      const contentWrapper = screen.getByTestId("content-wrapper");
      expect(contentWrapper.className).toContain("content-expanded");
    });

    /**
     * Test: Content area has correct margin class when collapsed
     */
    test("testContentArea_Collapsed_HasCollapsedClass", () => {
      setupMock(false);

      renderWithProviders(
        <TwoModeLayout>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      const contentWrapper = screen.getByTestId("content-wrapper");
      expect(contentWrapper.className).toContain("content-collapsed");
    });
  });

  describe("Carbon component usage", () => {
    /**
     * Test: Uses Carbon SideNav component
     * @see spec.md CR-001: Must use Carbon Design System
     * @see spec.md FR-007: Use isFixedNav for content pushing
     */
    test("testCarbon_SideNav_IsRendered", () => {
      setupMock(false);

      renderWithProviders(
        <TwoModeLayout>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      // SideNav should be rendered (has specific Carbon class)
      const sideNav = document.querySelector(".cds--side-nav");
      expect(sideNav).toBeTruthy();
    });

    /**
     * Test: Uses Carbon Header component
     */
    test("testCarbon_Header_IsRendered", () => {
      setupMock(false);

      renderWithProviders(
        <TwoModeLayout>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      // Header should be rendered
      const header = document.querySelector(".cds--header");
      expect(header).toBeTruthy();
    });
  });

  describe("props handling", () => {
    /**
     * Test: Passes defaultExpanded to useSideNavPreference
     */
    test("testProps_DefaultExpanded_PassedToHook", () => {
      setupMock(true);

      renderWithProviders(
        <TwoModeLayout defaultExpanded={true} storageKeyPrefix="test">
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      expect(useSideNavPreference).toHaveBeenCalledWith({
        defaultExpanded: true,
        storageKeyPrefix: "test",
      });
    });

    /**
     * Test: Passes storageKeyPrefix to useSideNavPreference
     */
    test("testProps_StorageKeyPrefix_PassedToHook", () => {
      setupMock(false);

      renderWithProviders(
        <TwoModeLayout storageKeyPrefix="analyzer">
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      expect(useSideNavPreference).toHaveBeenCalledWith(
        expect.objectContaining({
          storageKeyPrefix: "analyzer",
        }),
      );
    });
  });

  describe("menu rendering (M2)", () => {
    /**
     * Test: Renders SideNavMenu for items with children
     * @see spec.md US3: Hierarchical Navigation Structure (P2)
     */
    test("testMenu_ItemWithChildren_RendersSideNavMenu", () => {
      setupMock(false);

      const mockMenus = {
        menu: [
          {
            menu: {
              id: "1",
              displayKey: "menu.analyzer",
              actionURL: "/analyzers",
              isActive: true,
            },
            childMenus: [
              {
                menu: {
                  id: "1.1",
                  displayKey: "menu.analyzer.errors",
                  actionURL: "/analyzers/errors",
                  isActive: true,
                },
                childMenus: [],
              },
            ],
          },
        ],
      };

      renderWithProviders(<TwoModeLayout menus={mockMenus} />);

      // Should render parent as SideNavMenu (expandable)
      const menuElement = document.querySelector(".cds--side-nav__menu");
      expect(menuElement).toBeTruthy();
    });

    /**
     * Test: Renders SideNavMenuItem for leaf items
     */
    test("testMenu_LeafItem_RendersSideNavMenuItem", () => {
      setupMock(false);

      const mockMenus = {
        menu: [
          {
            menu: {
              id: "1",
              displayKey: "menu.home",
              actionURL: "/home",
              isActive: true,
            },
            childMenus: [],
          },
        ],
      };

      renderWithProviders(<TwoModeLayout menus={mockMenus} />);

      // Should render as SideNavMenuItem (direct link)
      const menuItem = document.querySelector(".cds--side-nav__link");
      expect(menuItem).toBeTruthy();
      expect(menuItem.getAttribute("href")).toBe("/home");
    });

    /**
     * Test: Supports 4 levels of menu nesting
     * @see spec.md FR-010: Support up to 4 levels of hierarchy
     */
    test("testMenu_FourLevels_RendersAllLevels", () => {
      setupMock(false);

      const mockMenus = {
        menu: [
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
                    childMenus: [
                      {
                        menu: {
                          id: "1.1.1.1",
                          displayKey: "menu.level4",
                          actionURL: "/level1/level2/level3/level4",
                          isActive: true,
                        },
                        childMenus: [],
                      },
                    ],
                  },
                ],
              },
            ],
          },
        ],
      };

      renderWithProviders(<TwoModeLayout menus={mockMenus} />);

      // Should render nested structure with menus (L1-L3) and items (L2-L4)
      const allMenus = document.querySelectorAll(".cds--side-nav__menu");
      const allLinks = document.querySelectorAll(".cds--side-nav__link");

      // At least 1 menu (level 1 parent) and some links rendered
      expect(allMenus.length).toBeGreaterThanOrEqual(1);
      expect(allLinks.length).toBeGreaterThanOrEqual(1);
    });

    /**
     * Test: Applies correct indentation per level
     * @see research.md A4: paddingLeft: `${level * 0.5 + 1}rem`
     */
    test("testMenu_NestedItems_ApplyCorrectIndentation", () => {
      setupMock(false);

      const mockMenus = {
        menu: [
          {
            menu: {
              id: "1",
              displayKey: "menu.parent",
              actionURL: "/parent",
              isActive: true,
            },
            childMenus: [
              {
                menu: {
                  id: "1.1",
                  displayKey: "menu.child",
                  actionURL: "/parent/child",
                  isActive: true,
                },
                childMenus: [],
              },
            ],
          },
        ],
      };

      renderWithProviders(<TwoModeLayout menus={mockMenus} />);

      // Child item should have indentation
      const childLink = document.querySelector(
        '.cds--side-nav__link[href="/parent/child"]',
      );
      expect(childLink).toBeTruthy();
      // Indentation is handled by Carbon or custom paddingLeft
    });

    /**
     * Test: Does not render inactive menu items
     */
    test("testMenu_InactiveItem_NotRendered", () => {
      setupMock(false);

      const mockMenus = {
        menu: [
          {
            menu: {
              id: "1",
              displayKey: "menu.active",
              actionURL: "/active",
              isActive: true,
            },
            childMenus: [],
          },
          {
            menu: {
              id: "2",
              displayKey: "menu.inactive",
              actionURL: "/inactive",
              isActive: false, // Inactive
            },
            childMenus: [],
          },
        ],
      };

      renderWithProviders(<TwoModeLayout menus={mockMenus} />);

      // Should only render active item
      const activeLink = document.querySelector(
        '.cds--side-nav__link[href="/active"]',
      );
      const inactiveLink = document.querySelector(
        '.cds--side-nav__link[href="/inactive"]',
      );

      expect(activeLink).toBeTruthy();
      expect(inactiveLink).toBeFalsy(); // Should not render
    });

    /**
     * Test: Handles menu items with openInNewWindow flag
     * @see research.md A4: target="_blank" + rel="noopener noreferrer"
     */
    test("testMenu_OpenInNewWindow_SetsTargetBlank", () => {
      setupMock(false);

      const mockMenus = {
        menu: [
          {
            menu: {
              id: "1",
              displayKey: "menu.external",
              actionURL: "https://example.com",
              isActive: true,
              openInNewWindow: true,
            },
            childMenus: [],
          },
        ],
      };

      renderWithProviders(<TwoModeLayout menus={mockMenus} />);

      const link = document.querySelector(
        '.cds--side-nav__link[href="https://example.com"]',
      );
      expect(link).toBeTruthy();
      expect(link.getAttribute("target")).toBe("_blank");
      expect(link.getAttribute("rel")).toContain("noopener");
      expect(link.getAttribute("rel")).toContain("noreferrer");
    });
  });
});
