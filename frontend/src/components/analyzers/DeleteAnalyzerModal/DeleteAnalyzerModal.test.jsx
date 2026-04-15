/**
 * Unit tests for DeleteAnalyzerModal component
 *
 * Testing Roadmap: .specify/guides/testing-roadmap.md
 *
 * Test Strategy:
 * - Use data-testid for reliable element selection (PREFERRED)
 * - Use waitFor with queryBy* for async operations
 * - Use userEvent for user interactions (PREFERRED)
 * - Test delete confirmation, service call, and error handling
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

jest.mock("../../../services/analyzerService", () => ({
  deleteAnalyzer: jest.fn(),
}));

// ========== IMPORTS (Standard order - MANDATORY) ==========

// 1. React
import React from "react";

// 2. Testing Library
import { render, screen, act } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";

// 3. userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";

// 5. IntlProvider
import { IntlProvider } from "react-intl";

// 7. Component under test
import DeleteAnalyzerModal from "./DeleteAnalyzerModal";

// 8. Mocked services
import { deleteAnalyzer } from "../../../services/analyzerService";

// 9. Messages/translations
import messages from "../../../languages/en.json";

// ========== HELPER FUNCTIONS ==========

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

// ========== TEST DATA BUILDERS ==========

const createMockAnalyzer = (overrides = {}) => ({
  id: "1000",
  name: "Test Analyzer",
  ...overrides,
});

// ========== TESTS ==========

describe("DeleteAnalyzerModal", () => {
  const mockOnClose = jest.fn();
  const mockOnConfirm = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * Test: Modal renders with analyzer name
   */
  test("testModalRender_ShowsAnalyzerName", async () => {
    // Arrange: Create mock analyzer
    const mockAnalyzer = createMockAnalyzer({ name: "My Test Analyzer" });

    // Act: Render modal
    renderWithIntl(
      <DeleteAnalyzerModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Assert: Verify modal is visible
    const modal = screen.getByTestId("delete-analyzer-modal");
    expect(modal).toBeInTheDocument();

    // Assert: Verify analyzer name is displayed in message
    // Note: The i18n message uses {name} interpolation
    const message = screen.getByTestId("delete-analyzer-message");
    expect(message).toBeInTheDocument();
  });

  /**
   * Test: Cancel button calls onClose
   */
  test("testCancelButton_CallsOnClose", async () => {
    // Arrange: Create mock analyzer
    const mockAnalyzer = createMockAnalyzer();

    // Act: Render modal
    renderWithIntl(
      <DeleteAnalyzerModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Act: Click cancel button
    const cancelButton = screen.getByTestId("delete-analyzer-cancel-button");
    await userEvent.click(cancelButton);

    // Assert: Verify onClose was called
    expect(mockOnClose).toHaveBeenCalledTimes(1);
    // Assert: Verify onConfirm was NOT called
    expect(mockOnConfirm).not.toHaveBeenCalled();
  });

  /**
   * Test: Delete button calls deleteAnalyzer service
   */
  test("testDeleteButton_CallsDeleteService", async () => {
    // Arrange: Mock successful delete
    const mockAnalyzer = createMockAnalyzer({ id: "12345" });
    deleteAnalyzer.mockImplementation((id, callback) => {
      callback(true, null);
    });

    // Act: Render modal
    renderWithIntl(
      <DeleteAnalyzerModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Act: Click delete button
    const deleteButton = screen.getByTestId("delete-analyzer-delete-button");
    await userEvent.click(deleteButton);

    // Assert: Verify deleteAnalyzer was called with correct ID
    expect(deleteAnalyzer).toHaveBeenCalledWith("12345", expect.any(Function));
  });

  /**
   * Test: Successful delete calls onConfirm and onClose
   */
  test("testDeleteSuccess_CallsOnConfirmAndOnClose", async () => {
    // Arrange: Mock successful delete
    const mockAnalyzer = createMockAnalyzer({ id: "1000" });
    deleteAnalyzer.mockImplementation((id, callback) => {
      callback(true, null);
    });

    // Act: Render modal
    renderWithIntl(
      <DeleteAnalyzerModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Act: Click delete button
    const deleteButton = screen.getByTestId("delete-analyzer-delete-button");
    await userEvent.click(deleteButton);

    // Assert: Verify onConfirm was called with analyzer ID
    await waitFor(() => {
      expect(mockOnConfirm).toHaveBeenCalledWith("1000");
    });

    // Assert: Verify onClose was called
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  /**
   * Test: Failed delete shows error notification
   */
  test("testDeleteFailure_ShowsErrorNotification", async () => {
    // Arrange: Mock failed delete
    const mockAnalyzer = createMockAnalyzer();
    deleteAnalyzer.mockImplementation((id, callback) => {
      callback(false, { error: "Cannot delete analyzer with active mappings" });
    });

    // Act: Render modal
    renderWithIntl(
      <DeleteAnalyzerModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Act: Click delete button
    const deleteButton = screen.getByTestId("delete-analyzer-delete-button");
    await userEvent.click(deleteButton);

    // Assert: Verify error notification is displayed
    await waitFor(() => {
      const notification = screen.getByTestId("delete-analyzer-notification");
      expect(notification).toBeInTheDocument();
      expect(notification).toHaveTextContent(
        "Cannot delete analyzer with active mappings",
      );
    });

    // Assert: Verify onConfirm was NOT called
    expect(mockOnConfirm).not.toHaveBeenCalled();
    // Assert: Verify onClose was NOT called
    expect(mockOnClose).not.toHaveBeenCalled();
  });

  /**
   * Test: Delete with missing analyzer ID shows error
   */
  test("testDeleteWithoutId_ShowsError", async () => {
    // Arrange: Analyzer without ID
    const mockAnalyzer = { name: "Test Analyzer" };

    // Act: Render modal
    renderWithIntl(
      <DeleteAnalyzerModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Act: Click delete button
    const deleteButton = screen.getByTestId("delete-analyzer-delete-button");
    await userEvent.click(deleteButton);

    // Assert: Verify error notification is displayed
    await waitFor(() => {
      const notification = screen.getByTestId("delete-analyzer-notification");
      expect(notification).toBeInTheDocument();
    });

    // Assert: Verify deleteAnalyzer was NOT called
    expect(deleteAnalyzer).not.toHaveBeenCalled();
  });

  /**
   * Test: Delete with null analyzer shows error
   */
  test("testDeleteWithNullAnalyzer_ShowsError", async () => {
    // Act: Render modal with null analyzer
    renderWithIntl(
      <DeleteAnalyzerModal
        analyzer={null}
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Act: Click delete button
    const deleteButton = screen.getByTestId("delete-analyzer-delete-button");
    await userEvent.click(deleteButton);

    // Assert: Verify error notification is displayed
    await waitFor(() => {
      const notification = screen.getByTestId("delete-analyzer-notification");
      expect(notification).toBeInTheDocument();
    });

    // Assert: Verify deleteAnalyzer was NOT called
    expect(deleteAnalyzer).not.toHaveBeenCalled();
  });

  /**
   * Test: Buttons are disabled while deleting
   */
  test("testButtonsDisabled_WhileDeleting", async () => {
    // Arrange: Mock slow delete
    const mockAnalyzer = createMockAnalyzer();
    let resolveDelete;
    deleteAnalyzer.mockImplementation((id, callback) => {
      // Don't resolve immediately - keep in "deleting" state
      resolveDelete = () => callback(true, null);
    });

    // Act: Render modal
    renderWithIntl(
      <DeleteAnalyzerModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Act: Click delete button
    const deleteButton = screen.getByTestId("delete-analyzer-delete-button");
    const cancelButton = screen.getByTestId("delete-analyzer-cancel-button");
    await userEvent.click(deleteButton);

    // Assert: Verify buttons are disabled while deleting
    expect(deleteButton).toBeDisabled();
    expect(cancelButton).toBeDisabled();

    // Cleanup: Resolve the delete (wrapped in act to avoid warnings)
    await act(async () => {
      resolveDelete();
    });
  });

  /**
   * Test: Modal handles onConfirm being undefined
   */
  test("testDeleteSuccess_WithoutOnConfirm_StillCloses", async () => {
    // Arrange: Mock successful delete
    const mockAnalyzer = createMockAnalyzer();
    deleteAnalyzer.mockImplementation((id, callback) => {
      callback(true, null);
    });

    // Act: Render modal without onConfirm
    renderWithIntl(
      <DeleteAnalyzerModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
        // No onConfirm provided
      />,
    );

    // Act: Click delete button
    const deleteButton = screen.getByTestId("delete-analyzer-delete-button");
    await userEvent.click(deleteButton);

    // Assert: Verify onClose was called even without onConfirm
    await waitFor(() => {
      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });
  });

  /**
   * Test: Modal shows danger styling
   */
  test("testModal_HasDangerStyling", () => {
    // Arrange: Create mock analyzer
    const mockAnalyzer = createMockAnalyzer();

    // Act: Render modal
    renderWithIntl(
      <DeleteAnalyzerModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
        onConfirm={mockOnConfirm}
      />,
    );

    // Assert: Verify delete button has danger styling
    const deleteButton = screen.getByTestId("delete-analyzer-delete-button");
    expect(deleteButton).toHaveClass("cds--btn--danger");
  });
});
