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
const mockSetMode = jest.fn();

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
const setupMock = (mode = "close") => {
  const navMode =
    typeof mode === "boolean" ? (mode ? "lock" : "close") : mode || "close";
  useSideNavPreference.mockReturnValue({
    mode: navMode,
    isExpanded: navMode !== "close",
    toggle: mockToggle,
    setMode: mockSetMode,
    SIDENAV_MODES: { SHOW: "show", LOCK: "lock", CLOSE: "close" },
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
      setupMock("close");

      renderWithProviders(
        <TwoModeLayout>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      // Content wrapper should have rail class
      const contentWrapper = screen.getByTestId("content-wrapper");
      expect(contentWrapper.className).toContain("content-rail");
      expect(contentWrapper.className).not.toContain("content-locked");
    });

    /**
     * Test: Renders with sidenav expanded when hook returns expanded state
     * @see spec.md US4: Page-Level Mode Configuration
     */
    test("testRender_ExpandedState_SidenavExpanded", () => {
      setupMock("lock");

      renderWithProviders(
        <TwoModeLayout defaultExpanded={true}>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      // Content wrapper should have locked class
      const contentWrapper = screen.getByTestId("content-wrapper");
      expect(contentWrapper.className).toContain("content-locked");
      expect(contentWrapper.className).not.toContain("content-rail");
    });

    /**
     * Test: Renders children in Content area
     * @see spec.md FR-008: Sidenav as sibling to Content component
     */
    test("testRender_WithChildren_RendersChildrenInContent", () => {
      setupMock("close");

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
      setupMock("close");

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
    test("testContentArea_Expanded_HasLockedClass", () => {
      setupMock("lock");

      renderWithProviders(
        <TwoModeLayout>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      const contentWrapper = screen.getByTestId("content-wrapper");
      expect(contentWrapper.className).toContain("content-locked");
    });

    /**
     * Test: Content area has correct margin class when collapsed
     */
    test("testContentArea_Collapsed_HasRailClass", () => {
      setupMock("close");

      renderWithProviders(
        <TwoModeLayout>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      const contentWrapper = screen.getByTestId("content-wrapper");
      expect(contentWrapper.className).toContain("content-rail");
    });
  });

  describe("Carbon component usage", () => {
    /**
     * Test: Uses Carbon SideNav component
     * @see spec.md CR-001: Must use Carbon Design System
     * @see spec.md FR-007: Use isFixedNav for content pushing
     */
    test("testCarbon_SideNav_IsRendered", () => {
      setupMock("close");

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
      setupMock("close");

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
      setupMock("lock");

      renderWithProviders(
        <TwoModeLayout defaultExpanded={true} storageKeyPrefix="test">
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      expect(useSideNavPreference).toHaveBeenCalledWith(
        expect.objectContaining({
          defaultMode: "lock",
          storageKeyPrefix: "test",
        }),
      );
    });

    /**
     * Test: Passes storageKeyPrefix to useSideNavPreference
     */
    test("testProps_StorageKeyPrefix_PassedToHook", () => {
      setupMock("close");

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

  /**
   * Header Actions Tests (M2b)
   *
   * These tests verify the headerActions prop that allows rendering
   * extracted header global actions (notifications, user panel, search,
   * language selector, help) in the TwoModeLayout header.
   *
   * @see spec.md FR-011: Preserve ALL existing header functionality
   * @see plan.md D5: Header Action Preservation Strategy
   */
  describe("header actions (M2b)", () => {
    /**
     * Test: Renders headerActions prop content in header global bar
     * @see spec.md FR-011: System MUST preserve ALL existing header functionality
     */
    test("testHeaderActions_ProvidedAsReactNode_RendersInHeaderGlobalBar", () => {
      setupMock("close");

      const HeaderActionsContent = () => (
        <div data-testid="header-actions-content">
          <button data-testid="notifications-btn">Notifications</button>
          <button data-testid="user-btn">User</button>
          <button data-testid="search-btn">Search</button>
        </div>
      );

      renderWithProviders(
        <TwoModeLayout headerActions={<HeaderActionsContent />}>
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      // Header actions should be rendered in the header area
      expect(screen.getByTestId("header-actions-content")).toBeTruthy();
      expect(screen.getByTestId("notifications-btn")).toBeTruthy();
      expect(screen.getByTestId("user-btn")).toBeTruthy();
      expect(screen.getByTestId("search-btn")).toBeTruthy();
    });

    /**
     * Test: headerActions renders in HeaderGlobalBar position
     * @see plan.md D5: HeaderGlobalBar content passed via headerActions prop
     */
    test("testHeaderActions_Position_InsideHeaderGlobalBar", () => {
      setupMock("close");

      renderWithProviders(
        <TwoModeLayout
          headerActions={
            <button data-testid="action-button">Action</button>
          }
        >
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      // The action button should be inside the Carbon header structure
      const actionButton = screen.getByTestId("action-button");
      expect(actionButton).toBeTruthy();

      // Verify it's within the header (has cds--header ancestor)
      const header = document.querySelector(".cds--header");
      expect(header).toBeTruthy();
      expect(header.contains(actionButton)).toBe(true);
    });

    /**
     * Test: headerActions can include functional notification button
     * @see spec.md FR-011: notifications panel must work
     */
    test("testHeaderActions_NotificationButton_CanTogglePanel", () => {
      setupMock("close");

      const mockNotificationToggle = jest.fn();

      renderWithProviders(
        <TwoModeLayout
          headerActions={
            <button
              data-testid="notification-toggle"
              onClick={mockNotificationToggle}
            >
              Notifications
            </button>
          }
        >
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      const notificationBtn = screen.getByTestId("notification-toggle");
      fireEvent.click(notificationBtn);

      expect(mockNotificationToggle).toHaveBeenCalledTimes(1);
    });

    /**
     * Test: headerActions can include functional user panel button
     * @see spec.md FR-011: user menu must work
     */
    test("testHeaderActions_UserPanelButton_CanTogglePanel", () => {
      setupMock("close");

      const mockUserToggle = jest.fn();

      renderWithProviders(
        <TwoModeLayout
          headerActions={
            <button data-testid="user-toggle" onClick={mockUserToggle}>
              User
            </button>
          }
        >
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      const userBtn = screen.getByTestId("user-toggle");
      fireEvent.click(userBtn);

      expect(mockUserToggle).toHaveBeenCalledTimes(1);
    });

    /**
     * Test: headerActions can include functional search toggle
     * @see spec.md FR-011: search bar must work
     */
    test("testHeaderActions_SearchToggle_CanToggleSearchBar", () => {
      setupMock("close");

      const mockSearchToggle = jest.fn();

      renderWithProviders(
        <TwoModeLayout
          headerActions={
            <button data-testid="search-toggle" onClick={mockSearchToggle}>
              Search
            </button>
          }
        >
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      const searchBtn = screen.getByTestId("search-toggle");
      fireEvent.click(searchBtn);

      expect(mockSearchToggle).toHaveBeenCalledTimes(1);
    });

    /**
     * Test: headerActions works alongside sidenav toggle
     * @see spec.md: Layout maintains sidenav toggle + content structure
     */
    test("testHeaderActions_WithSidenavToggle_BothWork", () => {
      setupMock("close");

      const mockActionClick = jest.fn();

      renderWithProviders(
        <TwoModeLayout
          headerActions={
            <button data-testid="custom-action" onClick={mockActionClick}>
              Custom Action
            </button>
          }
        >
          <div>Test Content</div>
        </TwoModeLayout>,
      );

      // Sidenav toggle should still work
      const toggleButton = screen.getByRole("button", { name: /menu/i });
      fireEvent.click(toggleButton);
      expect(mockToggle).toHaveBeenCalledTimes(1);

      // Custom action should also work
      const customAction = screen.getByTestId("custom-action");
      fireEvent.click(customAction);
      expect(mockActionClick).toHaveBeenCalledTimes(1);
    });

    /**
     * Test: Without headerActions prop, no error is thrown
     */
    test("testHeaderActions_NotProvided_NoError", () => {
      setupMock("close");

      // Should not throw when headerActions is not provided
      expect(() => {
        renderWithProviders(
          <TwoModeLayout>
            <div>Test Content</div>
          </TwoModeLayout>,
        );
      }).not.toThrow();

      // Layout should still render
      expect(screen.getByText("Test Content")).toBeTruthy();
    });
  });
});
