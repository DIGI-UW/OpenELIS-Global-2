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
  const messages = {};
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
});
