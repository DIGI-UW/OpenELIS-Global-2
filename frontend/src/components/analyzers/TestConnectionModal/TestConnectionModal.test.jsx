/**
 * Unit tests for TestConnectionModal component
 *
 * Testing Roadmap: .specify/guides/testing-roadmap.md
 *
 * Test Strategy:
 * - Use data-testid for reliable element selection (PREFERRED)
 * - Use waitFor with queryBy* for async operations
 * - Use userEvent for user interactions (PREFERRED)
 * - Test modal visibility, status transitions, and progress updates
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

jest.mock("../../../services/analyzerService", () => ({
  testConnection: jest.fn(),
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
import TestConnectionModal from "./TestConnectionModal";

// 8. Mocked services
import { testConnection } from "../../../services/analyzerService";

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
  ipAddress: "192.168.1.100",
  port: 5000,
  ...overrides,
});

// ========== TESTS ==========

describe("TestConnectionModal", () => {
  const mockOnClose = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  /**
   * Test: Modal renders with analyzer info
   */
  test("testModalRender_ShowsAnalyzerInfo", async () => {
    // Arrange: Create mock analyzer
    const mockAnalyzer = createMockAnalyzer({
      name: "Test Analyzer 1",
      ipAddress: "192.168.1.50",
      port: 8080,
    });

    // Act: Render modal
    renderWithIntl(
      <TestConnectionModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
      />,
    );

    // Assert: Verify modal is visible
    const modal = screen.getByTestId("test-connection-modal");
    expect(modal).toBeInTheDocument();

    // Assert: Verify analyzer info is displayed
    const analyzerInfo = screen.getByTestId("test-connection-analyzer-info");
    expect(analyzerInfo).toBeInTheDocument();
    expect(screen.getByText("Test Analyzer 1")).toBeInTheDocument();
    expect(screen.getByText("192.168.1.50")).toBeInTheDocument();
    expect(screen.getByText("8080")).toBeInTheDocument();
  });

  /**
   * Test: Modal does not show analyzer info when analyzer is null
   */
  test("testModalRender_WhenAnalyzerNull_NoAnalyzerInfo", async () => {
    // Act: Render modal with null analyzer
    renderWithIntl(
      <TestConnectionModal analyzer={null} open={true} onClose={mockOnClose} />,
    );

    // Assert: Verify modal is visible but no analyzer info
    const modal = screen.getByTestId("test-connection-modal");
    expect(modal).toBeInTheDocument();
    expect(
      screen.queryByTestId("test-connection-analyzer-info"),
    ).not.toBeInTheDocument();
  });

  /**
   * Test: Test button starts connection test
   */
  test("testTestButton_StartsConnectionTest", async () => {
    // Arrange: Create mock analyzer and mock service response
    const mockAnalyzer = createMockAnalyzer();
    testConnection.mockImplementation((id, callback) => {
      // Simulate successful response
      callback({ success: true, message: "Connection successful!" });
    });

    // Act: Render modal
    renderWithIntl(
      <TestConnectionModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
      />,
    );

    // Act: Click test button
    const testButton = screen.getByTestId("test-connection-test-button");
    await userEvent.click(testButton);

    // Assert: Verify testConnection was called with analyzer ID
    expect(testConnection).toHaveBeenCalledWith("1000", expect.any(Function));
  });

  /**
   * Test: Shows error when analyzer ID is missing
   */
  test("testTestButton_WhenNoAnalyzerId_ShowsError", async () => {
    // Arrange: Create analyzer without ID
    const mockAnalyzer = { name: "Test", ipAddress: "1.1.1.1", port: 80 };

    // Act: Render modal
    renderWithIntl(
      <TestConnectionModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
      />,
    );

    // Act: Click test button
    const testButton = screen.getByTestId("test-connection-test-button");
    await userEvent.click(testButton);

    // Assert: Verify error tag is displayed
    await waitFor(() => {
      const errorTag = screen.getByTestId("test-connection-error");
      expect(errorTag).toBeInTheDocument();
    });

    // Assert: Verify log shows error message
    const log = screen.getByTestId("test-connection-log-0");
    expect(log).toHaveTextContent("Analyzer ID is required");
  });

  /**
   * Test: Shows success status after successful connection
   */
  test("testConnectionSuccess_ShowsSuccessTag", async () => {
    // Arrange: Mock successful response
    const mockAnalyzer = createMockAnalyzer();
    testConnection.mockImplementation((id, callback) => {
      callback({ success: true, message: "Connection successful!" });
    });

    // Act: Render modal
    renderWithIntl(
      <TestConnectionModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
      />,
    );

    // Act: Click test button
    const testButton = screen.getByTestId("test-connection-test-button");
    await userEvent.click(testButton);

    // Fast-forward timers for progress animation
    act(() => {
      jest.advanceTimersByTime(2000);
    });

    // Assert: Verify success tag is displayed
    await waitFor(() => {
      const successTag = screen.getByTestId("test-connection-success");
      expect(successTag).toBeInTheDocument();
    });
  });

  /**
   * Test: Shows error status after failed connection
   */
  test("testConnectionFailure_ShowsErrorTag", async () => {
    // Arrange: Mock error response
    const mockAnalyzer = createMockAnalyzer();
    testConnection.mockImplementation((id, callback) => {
      callback({ success: false, error: "Connection refused" });
    });

    // Act: Render modal
    renderWithIntl(
      <TestConnectionModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
      />,
    );

    // Act: Click test button
    const testButton = screen.getByTestId("test-connection-test-button");
    await userEvent.click(testButton);

    // Fast-forward timers
    act(() => {
      jest.advanceTimersByTime(2000);
    });

    // Assert: Verify error tag is displayed
    await waitFor(() => {
      const errorTag = screen.getByTestId("test-connection-error");
      expect(errorTag).toBeInTheDocument();
    });
  });

  /**
   * Test: Close button calls onClose handler
   */
  test("testCloseButton_CallsOnClose", async () => {
    // Arrange: Create mock analyzer
    const mockAnalyzer = createMockAnalyzer();

    // Act: Render modal
    renderWithIntl(
      <TestConnectionModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
      />,
    );

    // Act: Click close button
    const closeButton = screen.getByTestId("test-connection-close-button");
    await userEvent.click(closeButton);

    // Assert: Verify onClose was called
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  /**
   * Test: Logs accordion shows test logs
   */
  test("testLogsAccordion_ShowsLogs", async () => {
    // Arrange: Mock successful response
    const mockAnalyzer = createMockAnalyzer();
    testConnection.mockImplementation((id, callback) => {
      callback({ success: true, message: "All tests passed" });
    });

    // Act: Render modal
    renderWithIntl(
      <TestConnectionModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
      />,
    );

    // Act: Click test button to generate logs
    const testButton = screen.getByTestId("test-connection-test-button");
    await userEvent.click(testButton);

    // Fast-forward timers
    act(() => {
      jest.advanceTimersByTime(2000);
    });

    // Assert: Verify logs accordion is displayed
    await waitFor(() => {
      const logsAccordion = screen.getByTestId("test-connection-logs");
      expect(logsAccordion).toBeInTheDocument();
    });

    // Assert: Verify at least one log entry exists
    const firstLog = screen.getByTestId("test-connection-log-0");
    expect(firstLog).toBeInTheDocument();
  });

  /**
   * Test: Modal resets state when reopened
   */
  test("testModalReopen_ResetsState", async () => {
    // Arrange: Mock successful response
    const mockAnalyzer = createMockAnalyzer();
    testConnection.mockImplementation((id, callback) => {
      callback({ success: true, message: "Success" });
    });

    // Act: Render modal
    const { rerender } = renderWithIntl(
      <TestConnectionModal
        analyzer={mockAnalyzer}
        open={true}
        onClose={mockOnClose}
      />,
    );

    // Act: Click test button to change state
    const testButton = screen.getByTestId("test-connection-test-button");
    await userEvent.click(testButton);

    act(() => {
      jest.advanceTimersByTime(2000);
    });

    // Verify success state
    await waitFor(() => {
      expect(screen.getByTestId("test-connection-success")).toBeInTheDocument();
    });

    // Act: Close and reopen modal
    rerender(
      <IntlProvider locale="en" messages={messages}>
        <TestConnectionModal
          analyzer={mockAnalyzer}
          open={false}
          onClose={mockOnClose}
        />
      </IntlProvider>,
    );

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <TestConnectionModal
          analyzer={mockAnalyzer}
          open={true}
          onClose={mockOnClose}
        />
      </IntlProvider>,
    );

    // Assert: Verify state was reset (no success tag, test button available)
    await waitFor(() => {
      expect(
        screen.queryByTestId("test-connection-success"),
      ).not.toBeInTheDocument();
      expect(
        screen.getByTestId("test-connection-test-button"),
      ).toBeInTheDocument();
    });
  });
});
