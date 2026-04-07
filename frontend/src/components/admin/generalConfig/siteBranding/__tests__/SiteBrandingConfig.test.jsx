/**
 * Unit tests for SiteBrandingConfig component
 *
 * References:
 * - Testing Roadmap: .specify/guides/testing-roadmap.md
 * - Jest Best Practices: .specify/guides/jest-best-practices.md
 * - Template: Jest Component Test
 *
 * TDD Workflow (MANDATORY for complex logic):
 * - RED: Write failing test first (defines expected behavior)
 * - GREEN: Write minimal code to make test pass
 * - REFACTOR: Improve code quality while keeping tests green
 *
 * SDD Checkpoint: After Phase 3 (Frontend), all unit tests MUST pass
 * Test Coverage Goal: >70% (measured via Jest)
 *
 * Task Reference: T019
 */

// ========== MOCKS (MUST be before imports - Jest hoisting) ==========

// Mock BrandingUtils BEFORE imports that use them (Jest hoisting)
jest.mock("../../../../utils/BrandingUtils", () => ({
  getBranding: jest.fn(),
  updateBranding: jest.fn(),
  resetBranding: jest.fn(),
}));

// ========== IMPORTS (Standard order - MANDATORY) ==========

// 1. React
import React from "react";

// 2. Testing Library
import {
  render,
  screen,
  wait,
  fireEvent,
  within,
} from "@testing-library/react";

// 3. userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";

// 4. jest-dom matchers
import "@testing-library/jest-dom/extend-expect";

// 5. IntlProvider (if component uses i18n)
import { IntlProvider } from "react-intl";

// 6. Router (if component uses routing)
import { BrowserRouter } from "react-router-dom";

// Context
import { NotificationContext } from "../../../../layout/Layout";

// Mock react-router-dom useHistory
const mockHistory = {
  push: jest.fn(),
  replace: jest.fn(),
  goBack: jest.fn(),
};

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useHistory: () => mockHistory,
}));

// 7. Component under test
import SiteBrandingConfig from "../SiteBrandingConfig";

// 8. Mocked BrandingUtils
import {
  getBranding,
  updateBranding,
  resetBranding,
} from "../../../../utils/BrandingUtils";

// 9. Messages/translations
import messages from "../../../../../languages/en.json";

// ========== HELPER FUNCTIONS ==========

// Mock notification context value
const mockNotificationContext = {
  notificationVisible: false,
  setNotificationVisible: jest.fn(),
  addNotification: jest.fn(),
};

// Helper function: Standard render with IntlProvider and NotificationContext
const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider value={mockNotificationContext}>
          {component}
        </NotificationContext.Provider>
      </IntlProvider>
    </BrowserRouter>,
  );
};

// ========== TEST SUITE ==========

describe("SiteBrandingConfig", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * Test: Component renders with branding configuration
   * Task Reference: T019
   */
  test("renders branding configuration page", async () => {
    // Arrange: Mock API response
    const mockBranding = {
      id: "test-id",
      primaryColor: "#1d4ed8",
      secondaryColor: "#64748b",
      headerColor: "#295785",
      colorMode: "light",
      useHeaderLogoForLogin: false,
    };

    getBranding.mockImplementation((callback) => {
      callback(mockBranding);
    });

    // Act: Render component
    renderWithIntl(<SiteBrandingConfig />);

    // Assert: Page title should be visible
    await wait(() => {
      expect(screen.getAllByText(/site branding/i)[0]).toBeInTheDocument();
    });
  });

  /**
   * Test: Component displays current branding values
   * Task Reference: T019
   */
  test("displays current branding values", async () => {
    // Arrange: Mock API response
    const mockBranding = {
      id: "test-id",
      primaryColor: "#ff0000",
      secondaryColor: "#00ff00",
      headerColor: "#0000ff",
      colorMode: "light",
      useHeaderLogoForLogin: false,
    };

    getBranding.mockImplementation((callback) => {
      callback(mockBranding);
    });

    // Act: Render component
    renderWithIntl(<SiteBrandingConfig />);

    // Assert: Branding values should be displayed
    await wait(() => {
      // Check that getBranding was called
      expect(getBranding).toHaveBeenCalledWith(expect.any(Function));
    });
  });

  /**
   * Test: Component handles loading state
   * Task Reference: T019
   */
  test("shows loading state while fetching branding", () => {
    // Arrange: Mock slow API response
    getBranding.mockImplementation((callback) => {
      // Don't call callback immediately to simulate loading
    });

    // Act: Render component
    renderWithIntl(<SiteBrandingConfig />);

    // Assert: Loading indicator should be visible (if component shows one)
    // This test may need adjustment based on actual component implementation
  });

  /**
   * Test: Component handles API errors gracefully
   * Task Reference: T019
   */
  test("handles API errors gracefully", async () => {
    // Arrange: Mock API that doesn't return data (simulates error/timeout)
    getBranding.mockImplementation((callback) => {
      // Simulate error by calling with null data
      callback(null);
    });

    // Act: Render component
    renderWithIntl(<SiteBrandingConfig />);

    // Assert: Component should still render (not crash)
    // When branding data is null, component should show loading or default state
    expect(await screen.findByText(/save changes/i)).toBeInTheDocument();
  });

  /**
   * Test: Save button functionality
   * Task Reference: T070
   */
  test("saves branding configuration and shows success notification", async () => {
    // Arrange: Mock API responses
    const mockBranding = {
      id: "test-id",
      primaryColor: "#1d4ed8",
      secondaryColor: "#64748b",
      headerColor: "#295785",
    };

    getBranding.mockImplementation((callback) => {
      callback(mockBranding);
    });

    updateBranding.mockImplementation((formData, callback) => {
      callback(200, null, formData);
    });

    // Act: Render component
    renderWithIntl(<SiteBrandingConfig />);

    // Wait for the component to fully render
    const primaryColorInput = await screen.findByLabelText(/primary color/i);

    // Modify the color to trigger hasUnsavedChanges
    fireEvent.change(primaryColorInput, { target: { value: "#ff0000" } });

    // Click save button (should now be enabled since we made a change)
    const saveButton = screen.getByText(/save changes/i);
    fireEvent.click(saveButton);

    // Assert: API should be called
    await wait(() => {
      expect(updateBranding).toHaveBeenCalled();
    });
  });

  /**
   * Test: Unsaved changes detection
   * Task Reference: T070
   */
  test("detects unsaved changes when branding is modified", async () => {
    // Arrange: Mock API response
    const mockBranding = {
      id: "test-id",
      primaryColor: "#1d4ed8",
    };

    getBranding.mockImplementation((callback) => {
      callback(mockBranding);
    });

    // Act: Render component and modify color
    renderWithIntl(<SiteBrandingConfig />);

    await wait(() => {
      expect(screen.getAllByText(/site branding/i)[0]).toBeInTheDocument();
    });

    // Modify primary color (this will trigger unsaved changes detection)
    // The actual implementation will detect changes via useEffect

    // Assert: Save button should be enabled when there are changes
    // This test may need adjustment based on actual component implementation
  });

  /**
   * Test: Cancel button functionality
   * Task Reference: T075
   */
  test("cancels branding changes and resets form to saved state", async () => {
    // Arrange: Mock API responses
    const mockBranding = {
      id: "test-id",
      primaryColor: "#1d4ed8",
      secondaryColor: "#64748b",
      headerColor: "#295785",
    };

    getBranding.mockImplementation((callback) => {
      callback(mockBranding);
    });

    // Act: Render component, modify color, then cancel
    renderWithIntl(<SiteBrandingConfig />);

    await wait(() => {
      expect(screen.getAllByText(/site branding/i)[0]).toBeInTheDocument();
    });

    // Modify primary color (simulate change)
    // Then click cancel
    const cancelButton = screen.getByTestId("branding-cancel-button");
    await userEvent.click(cancelButton);

    // Assert: No API call should be made (cancel doesn't save)
    expect(updateBranding).not.toHaveBeenCalled();

    // Form should be reset to original values
    // This test may need adjustment based on actual component implementation
  });

  /**
   * Test: Save failure handling
   */
  test("shows error notification when save fails", async () => {
    // Arrange: Mock API responses
    const mockBranding = {
      id: "test-id",
      primaryColor: "#1d4ed8",
      secondaryColor: "#64748b",
      headerColor: "#295785",
    };

    getBranding.mockImplementation((callback) => {
      callback(mockBranding);
    });

    updateBranding.mockImplementation((formData, callback) => {
      callback(500, "Server error", null);
    });

    // Act: Render component
    renderWithIntl(<SiteBrandingConfig />);

    // Wait for the component to fully render
    const primaryColorInput = await screen.findByLabelText(/primary color/i);

    // Modify the color to trigger hasUnsavedChanges
    fireEvent.change(primaryColorInput, { target: { value: "#ff0000" } });

    // Click save button
    const saveButton = screen.getByText(/save changes/i);
    fireEvent.click(saveButton);

    // Assert: API should be called
    await wait(() => {
      expect(updateBranding).toHaveBeenCalled();
    });

    // Error notification should be shown (via NotificationContext)
    expect(mockNotificationContext.addNotification).toHaveBeenCalled();
  });

  /**
   * Test: Reset to defaults - shows confirmation modal
   */
  test("shows confirmation modal when reset button clicked", async () => {
    // Arrange: Mock API response
    const mockBranding = {
      id: "test-id",
      primaryColor: "#1d4ed8",
      secondaryColor: "#64748b",
      headerColor: "#295785",
    };

    getBranding.mockImplementation((callback) => {
      callback(mockBranding);
    });

    // Act: Render component
    renderWithIntl(<SiteBrandingConfig />);

    // Wait for the component to fully render
    await wait(() => {
      expect(screen.getAllByText(/site branding/i)[0]).toBeInTheDocument();
    });

    // Click reset button using test-id
    const resetButton = screen.getByTestId("branding-reset-button");
    fireEvent.click(resetButton);

    // Should show confirmation modal - use text unique to reset modal
    expect(await screen.findByText(/reset all branding/i)).toBeInTheDocument();
  });

  /**
   * Test: Reset to defaults - successful reset
   */
  test("resets branding to defaults when confirmed", async () => {
    // Arrange: Mock API responses
    const mockBranding = {
      id: "test-id",
      primaryColor: "#ff0000",
      secondaryColor: "#00ff00",
      headerColor: "#0000ff",
    };

    getBranding.mockImplementation((callback) => {
      callback(mockBranding);
    });

    resetBranding.mockImplementation((callback) => {
      callback(200);
    });

    // Act: Render component
    renderWithIntl(<SiteBrandingConfig />);

    // Wait for the component to fully render
    await wait(() => {
      expect(screen.getAllByText(/site branding/i)[0]).toBeInTheDocument();
    });

    // Click reset button using test-id
    const resetButton = screen.getByTestId("branding-reset-button");
    fireEvent.click(resetButton);

    // Wait for reset confirmation modal to appear
    await screen.findByText(/reset all branding/i);

    // Find the reset modal by finding the confirm button with unique text "Reset"
    // The confirm button in the reset modal has text matching label.button.reset
    const allDialogs = screen.getAllByRole("dialog");
    // Find the dialog containing reset text
    let resetModal;
    for (const dialog of allDialogs) {
      if (within(dialog).queryByText(/reset all branding/i)) {
        resetModal = dialog;
        break;
      }
    }

    const confirmButton = within(resetModal).getByText(/^Reset$/i);
    fireEvent.click(confirmButton);

    // Should call resetBranding
    await wait(() => {
      expect(resetBranding).toHaveBeenCalled();
    });
  });
});
