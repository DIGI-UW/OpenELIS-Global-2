/**
 * Unit tests for ErrorDetailsModal component
 *
 * Testing Roadmap: .specify/guides/testing-roadmap.md
 *
 * Test Strategy:
 * - Use data-testid for reliable element selection (PREFERRED)
 * - Use waitFor with queryBy* for async operations
 * - Use userEvent for user interactions (PREFERRED)
 * - Test modal visibility and content display
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

// No mocks needed - component is self-contained

// ========== IMPORTS (Standard order - MANDATORY) ==========

// 1. React
import React from "react";

// 2. Testing Library
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";

// 3. userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";

// 5. IntlProvider
import { IntlProvider } from "react-intl";

// 6. Router (not needed for modal, but included for consistency)
import { BrowserRouter } from "react-router-dom";

// 7. Component under test
import ErrorDetailsModal from "./ErrorDetailsModal";

// 9. Messages/translations
import messages from "../../../languages/en.json";

// ========== HELPER FUNCTIONS ==========

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );
};

// ========== TEST DATA BUILDERS ==========

const createMockError = (overrides = {}) => ({
  id: "ERROR-001",
  analyzerId: "1000",
  analyzerName: "Test Analyzer 1",
  errorType: "MAPPING",
  severity: "ERROR",
  errorMessage: "No mapping found for field: UNMAPPED_FIELD_001",
  rawMessage: "H|1|UNMAPPED_FIELD_001",
  status: "UNACKNOWLEDGED",
  timestamp: new Date().toISOString(),
  analyzerLogs: [
    {
      timestamp: "10:00:00.123",
      level: "ERROR",
      message: "Connection failed",
    },
  ],
  ...overrides,
});

// ========== TESTS ==========

describe("ErrorDetailsModal", () => {
  const mockOnClose = jest.fn();
  const mockOnAcknowledge = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * Test: Display error details shows full context
   */
  test("testDisplayErrorDetails_ShowsFullContext", async () => {
    // Arrange: Create mock error with full details
    const mockError = createMockError({
      id: "ERROR-001",
      analyzerName: "Test Analyzer",
      errorType: "MAPPING",
      severity: "CRITICAL",
      errorMessage: "No mapping found",
    });

    // Act: Render modal with error
    renderWithIntl(
      <ErrorDetailsModal
        error={mockError}
        open={true}
        onClose={mockOnClose}
        onAcknowledge={mockOnAcknowledge}
      />,
    );

    // Assert: Verify modal is visible
    const modal = await screen.findByTestId("error-details-modal");
    expect(modal).not.toBeNull();

    // Assert: Verify error details are displayed
    expect(await screen.findByText("ERROR-001")).not.toBeNull();
    expect(await screen.findByText("Test Analyzer")).not.toBeNull();
    expect(await screen.findByText("No mapping found")).not.toBeNull();

    // Assert: Verify error type and severity badges are displayed
    await waitFor(() => {
      const errorType = screen.queryByText("Mapping");
      expect(errorType).not.toBeNull();
    });
  });

  /**
   * Test: Modal displays acknowledged error correctly
   */
  test("testDisplayAcknowledgedError_ShowsAcknowledgmentInfo", async () => {
    // Arrange: Create acknowledged error
    const mockError = createMockError({
      status: "ACKNOWLEDGED",
      acknowledgedBy: "admin",
      acknowledgedDate: new Date().toISOString(),
    });

    // Act: Render modal
    renderWithIntl(
      <ErrorDetailsModal
        error={mockError}
        open={true}
        onClose={mockOnClose}
        onAcknowledge={mockOnAcknowledge}
      />,
    );

    // Assert: Verify acknowledgment info is displayed
    await waitFor(() => {
      const acknowledgedText = screen.queryByText(/Acknowledged/i);
      expect(acknowledgedText).not.toBeNull();
    });
  });

  /**
   * Test: Modal displays analyzer logs section
   */
  test("testDisplayAnalyzerLogs_ShowsLogsSection", async () => {
    // Arrange: Create error with logs
    const mockError = createMockError({
      analyzerLogs: [
        {
          timestamp: "10:00:00.123",
          level: "ERROR",
          message: "Connection failed",
        },
      ],
    });

    // Act: Render modal
    renderWithIntl(
      <ErrorDetailsModal
        error={mockError}
        open={true}
        onClose={mockOnClose}
        onAcknowledge={mockOnAcknowledge}
      />,
    );

    // Wait for modal to render
    await screen.findByTestId("error-details-modal");

    // Assert: Verify logs accordion section exists (even if collapsed)
    const logsAccordion = await screen.findByTestId("analyzer-logs-accordion");
    expect(logsAccordion).not.toBeNull();
  });

  /**
   * Test: Close button calls onClose handler
   */
  test("testCloseButton_CallsOnClose", async () => {
    // Arrange: Create mock error
    const mockError = createMockError();

    // Act: Render modal
    renderWithIntl(
      <ErrorDetailsModal
        error={mockError}
        open={true}
        onClose={mockOnClose}
        onAcknowledge={mockOnAcknowledge}
      />,
    );

    // Wait for modal
    await screen.findByTestId("error-details-modal");

    // Act: Click close button
    const closeButton = await screen.findByTestId("error-details-close");
    await userEvent.click(closeButton);

    // Assert: Verify onClose was called
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  /**
   * Test: Acknowledge button calls onAcknowledge handler
   */
  test("testAcknowledgeButton_CallsOnAcknowledge", async () => {
    // Arrange: Create unacknowledged error
    const mockError = createMockError({
      status: "UNACKNOWLEDGED",
    });

    // Act: Render modal
    renderWithIntl(
      <ErrorDetailsModal
        error={mockError}
        open={true}
        onClose={mockOnClose}
        onAcknowledge={mockOnAcknowledge}
      />,
    );

    // Wait for modal
    await screen.findByTestId("error-details-modal");

    // Act: Click acknowledge button
    const acknowledgeButton = await screen.findByTestId(
      "error-details-acknowledge",
    );
    await userEvent.click(acknowledgeButton);

    // Assert: Verify onAcknowledge was called with error ID
    expect(mockOnAcknowledge).toHaveBeenCalledWith("ERROR-001");
    // Assert: Verify onClose was also called
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  /**
   * Test: Modal does not render when closed
   */
  test("testModal_WhenClosed_NotVisible", async () => {
    // Arrange: Create mock error
    const mockError = createMockError();

    // Act: Render modal with open=false
    renderWithIntl(
      <ErrorDetailsModal
        error={mockError}
        open={false}
        onClose={mockOnClose}
        onAcknowledge={mockOnAcknowledge}
      />,
    );

    // Assert: Verify modal is not visible (check aria-hidden attribute)
    await waitFor(() => {
      const modal = screen.queryByTestId("error-details-modal");
      if (modal) {
        const ariaHidden = modal.getAttribute("aria-hidden");
        expect(ariaHidden).toBe("true");
      } else {
        // Modal might not be in DOM at all when closed
        expect(modal).toBeNull();
      }
    });
  });

  /**
   * Test: Modal does not render when error is null
   */
  test("testModal_WhenErrorNull_NotRendered", () => {
    // Act: Render modal with null error
    renderWithIntl(
      <ErrorDetailsModal
        error={null}
        open={true}
        onClose={mockOnClose}
        onAcknowledge={mockOnAcknowledge}
      />,
    );

    // Assert: Verify modal is not in DOM
    const modal = screen.queryByTestId("error-details-modal");
    expect(modal).toBeNull();
  });
});
