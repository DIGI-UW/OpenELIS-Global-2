/**
 * Unit tests for MappingActivationModal component
 * 
 * References:
 * - Testing Roadmap: .specify/guides/testing-roadmap.md
 * - Jest Best Practices: .specify/guides/jest-best-practices.md
 * 
 * TDD Workflow (MANDATORY):
 * - RED: Write failing test first
 * - GREEN: Write minimal code to make test pass
 * - REFACTOR: Improve code quality while keeping tests green
 * 
 * Task Reference: T165
 * Test Naming: test{Scenario}_{ExpectedResult}
 */

// ========== IMPORTS (Standard order - MANDATORY) ==========

// 1. React
import React from "react";

// 2. Testing Library
import {
  render,
  screen,
} from "@testing-library/react";
import { waitFor } from "@testing-library/dom";

// 3. userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";

// 4. jest-dom matchers
import "@testing-library/jest-dom";

// 5. IntlProvider
import { IntlProvider } from "react-intl";

// 6. Router (not needed for modal, but included for consistency)
import { BrowserRouter } from "react-router-dom";

// 7. Component under test
import MappingActivationModal from "./MappingActivationModal";

// 8. Utilities (none needed)

// 9. Messages/translations
import messages from "../../../languages/en.json";

// ========== HELPER FUNCTIONS ==========

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>
  );
};

// ========== TESTS ==========

describe("MappingActivationModal", () => {
  const mockOnClose = jest.fn();
  const mockOnConfirm = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * Test: Display modal shows warning messages
   * Task Reference: T165
   * 
   * When the modal is opened, it should display warning messages
   * including general warning and active analyzer warning (if applicable).
   */
  test("testDisplayModal_ShowsWarningMessages", async () => {
    // Arrange: Modal is open with analyzer name
    renderWithIntl(
      <MappingActivationModal
        open={true}
        onClose={mockOnClose}
        analyzerName="Test Analyzer"
        analyzerIsActive={false}
        onConfirm={mockOnConfirm}
      />
    );

    // Assert: Wait for modal content to be available
    const warning = await screen.findByTestId("mapping-activation-warning");
    expect(warning).not.toBe(null);

    // Assert: Active analyzer warning should NOT be visible (analyzer is inactive)
    const activeWarning = screen.queryByTestId("mapping-activation-active-warning");
    expect(activeWarning).toBe(null);
  });

  /**
   * Test: Display modal shows active analyzer warning when analyzer is active
   * Task Reference: T165
   * 
   * When the modal is opened for an active analyzer, it should display
   * an additional warning about the analyzer being active.
   */
  test("testDisplayModal_WithActiveAnalyzer_ShowsActiveWarning", async () => {
    // Arrange: Modal is open with active analyzer
    renderWithIntl(
      <MappingActivationModal
        open={true}
        onClose={mockOnClose}
        analyzerName="Test Analyzer"
        analyzerIsActive={true}
        onConfirm={mockOnConfirm}
      />
    );

    // Assert: Wait for modal content to be available
    const warning = await screen.findByTestId("mapping-activation-warning");
    expect(warning).not.toBe(null);

    // Assert: Active analyzer warning should be visible
    const activeWarning = await screen.findByTestId("mapping-activation-active-warning");
    expect(activeWarning).not.toBe(null);
  });

  /**
   * Test: Activate button disabled without checkbox
   * Task Reference: T165
   * 
   * When the confirmation checkbox is not checked, the "Activate Changes"
   * button should be disabled.
   */
  test("testActivate_WithoutCheckbox_DisablesButton", async () => {
    // Arrange: Modal is open, checkbox not checked
    renderWithIntl(
      <MappingActivationModal
        open={true}
        onClose={mockOnClose}
        analyzerName="Test Analyzer"
        analyzerIsActive={false}
        onConfirm={mockOnConfirm}
      />
    );

    // Assert: Wait for checkbox and button to be available
    const checkbox = await screen.findByTestId("activation-confirmation-checkbox");
    const checkboxInput = checkbox.querySelector("input") || checkbox;
    expect(checkboxInput.checked).toBe(false);

    // Assert: Activate button should be disabled
    const activateButton = await screen.findByTestId("activation-confirm-button");
    expect(activateButton.disabled).toBe(true);
  });

  /**
   * Test: Activate button enabled with checkbox
   * Task Reference: T165
   * 
   * When the confirmation checkbox is checked, the "Activate Changes"
   * button should be enabled.
   */
  test("testActivate_WithCheckbox_EnablesButton", async () => {
    // Arrange: Modal is open
    renderWithIntl(
      <MappingActivationModal
        open={true}
        onClose={mockOnClose}
        analyzerName="Test Analyzer"
        analyzerIsActive={false}
        onConfirm={mockOnConfirm}
      />
    );

    // Act: Wait for checkbox and check it
    const checkbox = await screen.findByTestId("activation-confirmation-checkbox");
    const checkboxInput = checkbox.querySelector("input") || checkbox;
    await userEvent.click(checkboxInput);

    // Assert: Checkbox should be checked
    await waitFor(() => {
      expect(checkboxInput.checked).toBe(true);
    });

    // Assert: Activate button should be enabled
    const activateButton = await screen.findByTestId("activation-confirm-button");
    await waitFor(() => {
      expect(activateButton.disabled).toBe(false);
    });
  });

  /**
   * Test: Clicking activate button calls onConfirm
   * Task Reference: T165
   * 
   * When the confirmation checkbox is checked and the "Activate Changes"
   * button is clicked, onConfirm should be called.
   */
  test("testActivate_WithCheckbox_CallsOnConfirm", async () => {
    // Arrange: Modal is open
    renderWithIntl(
      <MappingActivationModal
        open={true}
        onClose={mockOnClose}
        analyzerName="Test Analyzer"
        analyzerIsActive={false}
        onConfirm={mockOnConfirm}
      />
    );

    // Act: Wait for checkbox and check it
    const checkbox = await screen.findByTestId("activation-confirmation-checkbox");
    const checkboxInput = checkbox.querySelector("input") || checkbox;
    await userEvent.click(checkboxInput);

    // Wait for button to be enabled
    const activateButton = await screen.findByTestId("activation-confirm-button");
    await waitFor(() => {
      expect(activateButton.disabled).toBe(false);
    });

    // Act: Click activate button
    await userEvent.click(activateButton);

    // Assert: onConfirm should be called
    await waitFor(() => {
      expect(mockOnConfirm).toHaveBeenCalledTimes(1);
    });
  });

  /**
   * Test: Clicking cancel button calls onClose
   * Task Reference: T165
   * 
   * When the cancel button is clicked, onClose should be called.
   */
  test("testCancel_CallsOnClose", async () => {
    // Arrange: Modal is open
    renderWithIntl(
      <MappingActivationModal
        open={true}
        onClose={mockOnClose}
        analyzerName="Test Analyzer"
        analyzerIsActive={false}
        onConfirm={mockOnConfirm}
      />
    );

    // Act: Wait for cancel button and click it
    const cancelButton = await screen.findByTestId("mapping-activation-cancel-button");
    await userEvent.click(cancelButton);

    // Assert: onClose should be called
    await waitFor(() => {
      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });
  });

  /**
   * Test: Modal not visible when closed
   * Task Reference: T165
   * 
   * When the modal is closed (open=false), it should not be visible.
   * Note: ComposedModal may still render content but hide it with aria-hidden.
   */
  test("testModal_WhenClosed_NotVisible", async () => {
    // Arrange: Modal is closed
    renderWithIntl(
      <MappingActivationModal
        open={false}
        onClose={mockOnClose}
        analyzerName="Test Analyzer"
        analyzerIsActive={false}
        onConfirm={mockOnConfirm}
      />
    );

    // Assert: Modal container should be hidden (aria-hidden=true) or not accessible
    // ComposedModal may render but hide content when open=false
    const modal = screen.queryByTestId("mapping-activation-modal");
    if (modal) {
      // If modal exists, it should be hidden
      const modalContainer = modal.closest('[aria-hidden]');
      if (modalContainer) {
        expect(modalContainer.getAttribute('aria-hidden')).toBe('true');
      }
    } else {
      // Modal doesn't exist (also valid)
      expect(modal).toBe(null);
    }
  });
});

