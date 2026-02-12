/**
 * Unit tests for TestMappingModal component
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
 * Test Naming: test{Scenario}_{ExpectedResult}
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

jest.mock("../../../services/analyzerService", () => ({
  previewMapping: jest.fn(),
}));

// ========== IMPORTS (Standard order - MANDATORY) ==========

// 1. React
import React from "react";

// 2. Testing Library
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";

// 3. userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";

// 5. IntlProvider
import { IntlProvider } from "react-intl";

// 6. Router (not needed for modal, but included for consistency)
import { BrowserRouter } from "react-router-dom";

// 7. Component under test
import TestMappingModal from "./TestMappingModal";

// 8. Utilities
import { previewMapping } from "../../../services/analyzerService";

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

// ========== TESTS ==========

describe("TestMappingModal", () => {
  const mockOnClose = jest.fn();
  const defaultProps = {
    open: true,
    onClose: mockOnClose,
    analyzerId: "analyzer-1",
    analyzerName: "Test Analyzer",
    analyzerType: "Chemistry Analyzer",
    activeMappingsCount: 15,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * Test: Submit message with valid message displays preview
   *
   * When a valid ASTM message is entered and preview is clicked,
   * the modal should display preview results.
   */
  test("testSubmitMessage_WithValidMessage_DisplaysPreview", async () => {
    // Arrange: Mock successful preview response
    const mockPreviewResult = {
      parsedFields: [
        {
          fieldName: "GLUCOSE",
          astmRef: "R|1|^GLUCOSE^...",
          rawValue: "123",
          mappedTo: "TEST-001",
          interpretation: "Mapped",
        },
      ],
      appliedMappings: [
        {
          analyzerFieldName: "GLUCOSE",
          openelisFieldType: "TEST",
          openelisFieldId: "TEST-001",
          mappedValue: "123",
        },
      ],
      entityPreview: {
        entityType: "Test",
        jsonPreview: { id: "TEST-001", name: "Glucose" },
      },
      warnings: [],
      errors: [],
    };

    previewMapping.mockImplementation((analyzerId, data, callback) => {
      callback(mockPreviewResult, null);
    });

    // Arrange: Render modal
    renderWithIntl(<TestMappingModal {...defaultProps} />);

    // Act: Enter ASTM message
    const messageInput = await screen.findByTestId(
      "test-mapping-message-input",
    );
    await userEvent.type(messageInput, "H|\\^&|||PSM^Micro^2.0|");

    // Act: Click preview button
    const previewButton = await screen.findByTestId(
      "test-mapping-preview-button",
    );
    await userEvent.click(previewButton);

    // Assert: Wait for preview results to appear
    const results = await screen.findByTestId("test-mapping-results");
    expect(results).toBeTruthy();

    // Assert: Parsed fields table should be visible
    expect(screen.getByTestId("test-mapping-parsed-fields")).toBeTruthy();

    // Assert: Applied mappings section should be visible
    expect(screen.getByTestId("test-mapping-applied-mappings")).toBeTruthy();

    // Assert: Entity preview should be visible
    expect(screen.getByTestId("test-mapping-entity-preview")).toBeTruthy();
  });

  /**
   * Test: Validation with invalid format shows error
   *
   * When an invalid ASTM message format is submitted, the modal
   * should display an error message.
   */
  test("testValidation_WithInvalidFormat_ShowsError", async () => {
    // Arrange: Mock error response
    previewMapping.mockImplementation((analyzerId, data, callback) => {
      callback({ error: "Invalid ASTM message format" }, null);
    });

    // Arrange: Render modal
    renderWithIntl(<TestMappingModal {...defaultProps} />);

    // Act: Enter invalid message
    const messageInput = await screen.findByTestId(
      "test-mapping-message-input",
    );
    await userEvent.type(messageInput, "Invalid message");

    // Act: Click preview button
    const previewButton = await screen.findByTestId(
      "test-mapping-preview-button",
    );
    await userEvent.click(previewButton);

    // Assert: Wait for error to appear
    const errorElement = await screen.findByTestId("test-mapping-error");
    expect(errorElement).toBeTruthy();

    // Assert: Error message should be displayed
    expect(screen.getByText("Invalid ASTM message format")).toBeTruthy();
  });

  /**
   * Test: Validation with message too large shows error
   *
   * When an ASTM message exceeds the maximum size (10KB), the modal
   * should display an error message.
   */
  test("testValidation_WithMessageTooLarge_ShowsError", async () => {
    // Arrange: Render modal
    renderWithIntl(<TestMappingModal {...defaultProps} />);

    // Act: Enter message that exceeds 10KB limit
    const messageInput = await screen.findByTestId(
      "test-mapping-message-input",
    );
    const largeMessage = "A".repeat(10241); // 10KB + 1 byte
    // Use direct value assignment for large text to avoid timeout
    fireEvent.change(messageInput, { target: { value: largeMessage } });

    // Act: Click preview button
    const previewButton = await screen.findByTestId(
      "test-mapping-preview-button",
    );
    await userEvent.click(previewButton);

    // Assert: Wait for error to appear (validation happens synchronously)
    const errorElement = await screen.findByTestId("test-mapping-error");
    expect(errorElement).toBeTruthy();

    // Assert: Error message should indicate message too large
    expect(screen.getByText(/ASTM message exceeds maximum size/i)).toBeTruthy();
  });

  /**
   * Test: Clear form with Test Another resets state
   *
   * When "Test Another Message" button is clicked after a preview,
   * the form should be reset and results cleared.
   */
  test("testClearForm_WithTestAnother_ResetsState", async () => {
    // Arrange: Mock successful preview response
    const mockPreviewResult = {
      parsedFields: [
        {
          fieldName: "GLUCOSE",
          astmRef: "R|1|^GLUCOSE^...",
          rawValue: "123",
          mappedTo: "TEST-001",
          interpretation: "Mapped",
        },
      ],
      appliedMappings: [],
      entityPreview: {},
      warnings: [],
      errors: [],
    };

    previewMapping.mockImplementation((analyzerId, data, callback) => {
      callback(mockPreviewResult, null);
    });

    // Arrange: Render modal
    renderWithIntl(<TestMappingModal {...defaultProps} />);

    // Act: Enter message and preview
    const messageInput = await screen.findByTestId(
      "test-mapping-message-input",
    );
    await userEvent.type(messageInput, "H|\\^&|||PSM^Micro^2.0|");

    const previewButton = await screen.findByTestId(
      "test-mapping-preview-button",
    );
    await userEvent.click(previewButton);

    // Assert: Wait for results to appear
    const results = await screen.findByTestId("test-mapping-results");
    expect(results).toBeTruthy();

    // Act: Click "Test Another Message" button
    const testAnotherButton = await screen.findByTestId(
      "test-mapping-test-another",
    );
    await userEvent.click(testAnotherButton);

    // Assert: Results should be cleared
    await waitFor(() => {
      expect(screen.queryByTestId("test-mapping-results")).toBeNull();
    });

    // Assert: Message input should be cleared
    expect(messageInput.value).toBe("");

    // Assert: "No results" message should be visible
    expect(screen.getByTestId("test-mapping-no-results")).toBeTruthy();
  });

  /**
   * Test: Preview button disabled when message is empty
   *
   * When the ASTM message input is empty, the preview button
   * should be disabled.
   */
  test("testPreview_WithEmptyMessage_DisablesButton", async () => {
    // Arrange: Render modal
    renderWithIntl(<TestMappingModal {...defaultProps} />);

    // Assert: Preview button should be disabled
    const previewButton = await screen.findByTestId(
      "test-mapping-preview-button",
    );
    expect(previewButton.disabled).toBe(true);
  });

  /**
   * Test: Modal displays analyzer information
   *
   * When the modal is opened, it should display analyzer information
   * including name, type, and active mappings count.
   */
  test("testDisplayModal_ShowsAnalyzerInfo", async () => {
    // Arrange: Render modal
    renderWithIntl(<TestMappingModal {...defaultProps} />);

    // Assert: Analyzer info section should be visible
    const analyzerInfo = await screen.findByTestId(
      "test-mapping-analyzer-info",
    );
    expect(analyzerInfo).toBeTruthy();

    // Assert: Analyzer name should be displayed
    expect(screen.getByText("Test Analyzer")).toBeTruthy();

    // Assert: Analyzer type should be displayed
    expect(screen.getByText("Chemistry Analyzer")).toBeTruthy();

    // Assert: Active mappings count should be displayed
    expect(screen.getByText(/15 active mappings/i)).toBeTruthy();
  });
});
