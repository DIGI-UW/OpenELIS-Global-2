/**
 * CustomFieldTypeManagement Component Tests
 *
 * Testing Roadmap: .specify/guides/testing-roadmap.md
 *
 * Test Strategy:
 * - Use data-testid for reliable element selection (PREFERRED)
 * - Use waitFor with queryBy* for async operations
 * - Use userEvent for user interactions (PREFERRED)
 * - No reliance on async timing - use proper queries
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

jest.mock("../../../services/analyzerService", () => ({
  getCustomFieldTypes: jest.fn(),
  createCustomFieldType: jest.fn(),
  updateCustomFieldType: jest.fn(),
  deleteCustomFieldType: jest.fn(),
}));

// ========== IMPORTS (Standard order - MANDATORY) ==========

// 1. React
import React from "react";

// 2. Testing Library (all utilities in one import)
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";

// 4. jest-dom matchers (MUST be imported)
import "@testing-library/jest-dom";

// 3. userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";

// 5. IntlProvider (if component uses i18n)
import { IntlProvider } from "react-intl";

// 6. Router (if component uses routing)
import { BrowserRouter } from "react-router-dom";

// 7. Component under test
import CustomFieldTypeManagement from "./CustomFieldTypeManagement";

// 8. Utilities (import functions, not just for mocking)
import {
  getCustomFieldTypes,
  createCustomFieldType,
  updateCustomFieldType,
  deleteCustomFieldType,
} from "../../../services/analyzerService";

// 9. Messages/translations
import messages from "../../../languages/en.json";

// ========== TEST SETUP ==========

// Standard render helper with IntlProvider
const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );
};

// Mock data builder
const createMockCustomFieldType = (overrides = {}) => ({
  id: "1",
  typeName: "CUSTOM_NUMERIC",
  displayName: "Custom Numeric Field",
  validationPattern: "^[0-9]+$",
  valueRangeMin: "0",
  valueRangeMax: "100",
  allowedCharacters: null,
  isActive: true,
  ...overrides,
});

describe("CustomFieldTypeManagement", () => {
  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
  });

  /**
   * Test: Renders CustomFieldTypes with data displays table
   */
  test("testRendersCustomFieldTypes_WithData_DisplaysTable", async () => {
    // Arrange
    const mockTypes = [
      createMockCustomFieldType({ id: "1", typeName: "TYPE_1" }),
      createMockCustomFieldType({
        id: "2",
        typeName: "TYPE_2",
        isActive: false,
      }),
    ];

    getCustomFieldTypes.mockImplementation((callback) => {
      callback(mockTypes);
    });

    // Act
    renderWithIntl(<CustomFieldTypeManagement />);

    // Assert - Wait for user-visible content (data rows)
    // Focus on what the user sees - the actual data in the table
    // Use getAllByText for headers since they might appear multiple times
    await waitFor(
      () => {
        // Check for actual data rows (unique content that user sees)
        expect(screen.getByText("TYPE_1")).toBeTruthy();
        expect(screen.getByText("TYPE_2")).toBeTruthy();
      },
      { timeout: 3000 },
    );

    // Verify table headers exist (may appear multiple times, so use getAllByText)
    const typeNameHeaders = screen.getAllByText("Type Name");
    expect(typeNameHeaders.length).toBeGreaterThan(0);
    const displayNameHeaders = screen.getAllByText("Display Name");
    expect(displayNameHeaders.length).toBeGreaterThan(0);
    const statusHeaders = screen.getAllByText("Status");
    expect(statusHeaders.length).toBeGreaterThan(0);
  });

  /**
   * Test: Create CustomFieldType with valid data saves type
   */
  test("testCreateCustomFieldType_WithValidData_SavesType", async () => {
    // Arrange
    getCustomFieldTypes.mockImplementation((callback) => {
      callback([]);
    });

    createCustomFieldType.mockImplementation((data, callback) => {
      const newType = createMockCustomFieldType({
        id: "new-id",
        ...data,
      });
      callback(newType, null);
      // Simulate reload
      getCustomFieldTypes.mockImplementation((cb) => {
        cb([newType]);
      });
    });

    // Act
    renderWithIntl(<CustomFieldTypeManagement />);

    // Open form
    const addButton = screen.getByTestId("add-custom-field-type-button");
    await userEvent.click(addButton);

    // Wait for form fields to appear (user-visible elements, not the modal itself)
    const typeNameInput = await screen.findByLabelText(/Type Name/i);
    const displayNameInput = screen.getByLabelText(/Display Name/i);

    await userEvent.type(typeNameInput, "NEW_TYPE");
    await userEvent.type(displayNameInput, "New Type");

    // Submit form (Carbon Modal primary button)
    const saveButton = screen.getByText("Save");
    await userEvent.click(saveButton);

    // Assert
    await waitFor(() => {
      expect(createCustomFieldType).toHaveBeenCalledWith(
        expect.objectContaining({
          typeName: "NEW_TYPE",
          displayName: "New Type",
        }),
        expect.any(Function),
        null,
      );
    });
  });

  /**
   * Test: Validate pattern with invalid regex shows error
   */
  test("testValidatePattern_WithInvalidRegex_ShowsError", async () => {
    // Arrange
    getCustomFieldTypes.mockImplementation((callback) => {
      callback([]);
    });

    // Act
    renderWithIntl(<CustomFieldTypeManagement />);

    // Open form
    const addButton = screen.getByTestId("add-custom-field-type-button");
    await userEvent.click(addButton);

    // Wait for form fields to appear (user-visible elements, not the modal itself)
    const typeNameInput = await screen.findByLabelText(/Type Name/i);
    const displayNameInput = screen.getByLabelText(/Display Name/i);
    const validationPatternInput = screen.getByLabelText(
      /Validation Pattern \(Regex\)/i,
    );

    await userEvent.type(typeNameInput, "TEST_TYPE");
    await userEvent.type(displayNameInput, "Test Type");
    await userEvent.type(validationPatternInput, "[invalid regex"); // Invalid regex

    // Try to submit
    const saveButton = screen.getByText("Save");
    await userEvent.click(saveButton);

    // Assert - error should appear in the form field's invalidText (Carbon shows it as text content)
    await waitFor(() => {
      // The error message appears as text content in the form field's error area
      const errorMessage = screen.queryByText("Invalid regex pattern");
      expect(errorMessage).toBeTruthy();
    });

    // Verify createCustomFieldType was NOT called
    expect(createCustomFieldType).not.toHaveBeenCalled();
  });
});
