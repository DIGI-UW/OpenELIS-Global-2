// Mock utilities BEFORE imports that use them
jest.mock("../../../services/analyzerService", () => ({
  getValidationRules: jest.fn(),
  createValidationRule: jest.fn(),
  updateValidationRule: jest.fn(),
  deleteValidationRule: jest.fn(),
}));

// React
import React from "react";

// Testing Library (all utilities in one import)
import { render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";

// userEvent (PREFERRED for user interactions)
import userEvent from "@testing-library/user-event";

// jest-dom matchers (MUST be imported)
import "@testing-library/jest-dom";

// IntlProvider (component uses i18n)
import { IntlProvider } from "react-intl";

// Router (if component uses routing)
import { BrowserRouter } from "react-router-dom";

// Component under test
import ValidationRuleEditor from "./ValidationRuleEditor";

// Utilities (import functions, not just for mocking)
import {
  getValidationRules,
  createValidationRule,
  updateValidationRule,
} from "../../../services/analyzerService";

// Messages/translations
import messages from "../../../languages/en.json";

/**
 * Unit tests for ValidationRuleEditor component
 *
 * References:
 * - Testing Roadmap: .specify/guides/testing-roadmap.md
 * - Template: Jest Component Test
 *
 * TDD Workflow (MANDATORY for complex logic):
 * - RED: Write failing test first (defines expected behavior)
 * - GREEN: Write minimal code to make test pass
 * - REFACTOR: Improve code quality while keeping tests green
 *
 * Test Coverage Goal: >70% (measured via Jest)
 *
 * Test Naming: test{Scenario}_{ExpectedResult}
 */

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

describe("ValidationRuleEditor Component", () => {
  const mockOnSave = jest.fn();
  const mockOnCancel = jest.fn();
  const customFieldTypeId = "CFT-001";

  beforeEach(() => {
    jest.clearAllMocks();
  });

  /**
   * Test: Select rule type shows relevant fields
   */
  test("testSelectRuleType_ShowsRelevantFields", async () => {
    renderWithIntl(
      <ValidationRuleEditor
        customFieldTypeId={customFieldTypeId}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />,
    );

    // Default should show REGEX fields
    expect(screen.getByTestId("regex-pattern-input")).toBeTruthy();

    // Select RANGE type
    const ruleTypeSelect = screen.getByTestId("rule-type-select");
    await userEvent.selectOptions(ruleTypeSelect, "RANGE");

    // Should show RANGE fields
    await waitFor(() => {
      expect(screen.queryByTestId("range-min-input")).toBeTruthy();
      expect(screen.queryByTestId("range-max-input")).toBeTruthy();
    });

    // Select ENUM type
    await userEvent.selectOptions(ruleTypeSelect, "ENUM");

    // Should show ENUM fields
    await waitFor(() => {
      expect(screen.queryByTestId("enum-input")).toBeTruthy();
    });

    // Select LENGTH type
    await userEvent.selectOptions(ruleTypeSelect, "LENGTH");

    // Should show LENGTH fields
    await waitFor(() => {
      expect(screen.queryByTestId("length-min-input")).toBeTruthy();
      expect(screen.queryByTestId("length-max-input")).toBeTruthy();
    });
  });

  /**
   * Test: Regex rule with invalid pattern shows error
   */
  test("testRegexRule_WithInvalidPattern_ShowsError", async () => {
    renderWithIntl(
      <ValidationRuleEditor
        customFieldTypeId={customFieldTypeId}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />,
    );

    // Enter invalid regex pattern
    const patternInput = screen.getByTestId("regex-pattern-input");
    await userEvent.type(patternInput, "[invalid");

    // Enter rule name
    const ruleNameInput = screen.getByTestId("rule-name-input");
    await userEvent.type(ruleNameInput, "Test Rule");

    // Try to save
    const saveButton = screen.getByTestId("save-rule-button");
    await userEvent.click(saveButton);

    // Should show validation error
    await waitFor(() => {
      expect(screen.queryByText(/invalid regex pattern/i)).toBeTruthy();
    });
  });

  /**
   * Test: Range rule with min greater than max shows error
   */
  test("testRangeRule_WithMinGreaterThanMax_ShowsError", async () => {
    renderWithIntl(
      <ValidationRuleEditor
        customFieldTypeId={customFieldTypeId}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />,
    );

    // Select RANGE type
    const ruleTypeSelect = screen.getByTestId("rule-type-select");
    await userEvent.selectOptions(ruleTypeSelect, "RANGE");

    // Enter min greater than max
    const minInput = await screen.findByTestId("range-min-input");
    const maxInput = screen.getByTestId("range-max-input");

    await userEvent.type(minInput, "100");
    await userEvent.type(maxInput, "50");

    // Enter rule name
    const ruleNameInput = screen.getByTestId("rule-name-input");
    await userEvent.type(ruleNameInput, "Test Rule");

    // Try to save
    const saveButton = screen.getByTestId("save-rule-button");
    await userEvent.click(saveButton);

    // Should show validation error
    await waitFor(() => {
      expect(screen.queryByTestId("range-error")).toBeTruthy();
      expect(screen.queryByText(/minimum cannot be greater/i)).toBeTruthy();
    });
  });

  /**
   * Test: Test validation with sample value displays result
   */
  test("testTestValidation_WithSampleValue_DisplaysResult", async () => {
    renderWithIntl(
      <ValidationRuleEditor
        customFieldTypeId={customFieldTypeId}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />,
    );

    // Enter regex pattern
    const patternInput = screen.getByTestId("regex-pattern-input");
    await userEvent.type(
      patternInput,
      "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
    );

    // Enter test value
    const testValueInput = screen.getByTestId("test-value-input");
    await userEvent.type(testValueInput, "test@example.com");

    // Click test button
    const testButton = screen.getByTestId("test-rule-button");
    await userEvent.click(testButton);

    // Should show test result
    await waitFor(() => {
      expect(screen.queryByTestId("test-result-notification")).toBeTruthy();
    });
  });

  /**
   * Test: Enum rule allows adding and removing values
   */
  test("testEnumRule_AddAndRemoveValues_UpdatesList", async () => {
    renderWithIntl(
      <ValidationRuleEditor
        customFieldTypeId={customFieldTypeId}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />,
    );

    // Select ENUM type
    const ruleTypeSelect = screen.getByTestId("rule-type-select");
    await userEvent.selectOptions(ruleTypeSelect, "ENUM");

    // Add enum value
    const enumInput = await screen.findByTestId("enum-input");
    await userEvent.type(enumInput, "ACTIVE");
    const addButton = screen.getByTestId("add-enum-value-button");
    await userEvent.click(addButton);

    // Should show enum value tag
    await waitFor(() => {
      expect(screen.queryByTestId("enum-value-tag-ACTIVE")).toBeTruthy();
    });

    // Remove enum value
    const tag = screen.getByTestId("enum-value-tag-ACTIVE");
    const closeButton = within(tag).getByRole("button");
    await userEvent.click(closeButton);

    // Should remove tag
    await waitFor(() => {
      expect(screen.queryByTestId("enum-value-tag-ACTIVE")).toBeFalsy();
    });
  });

  /**
   * Test: Save rule with valid data calls onSave
   */
  test("testSaveRule_WithValidData_CallsOnSave", async () => {
    createValidationRule.mockImplementation((id, data, callback) => {
      callback({ id: "RULE-001", ...data }, null);
    });

    renderWithIntl(
      <ValidationRuleEditor
        customFieldTypeId={customFieldTypeId}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />,
    );

    // Fill in form
    const ruleNameInput = screen.getByTestId("rule-name-input");
    await userEvent.type(ruleNameInput, "Email Pattern");

    const patternInput = screen.getByTestId("regex-pattern-input");
    await userEvent.type(
      patternInput,
      "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
    );

    // Save
    const saveButton = screen.getByTestId("save-rule-button");
    await userEvent.click(saveButton);

    // Should call createValidationRule
    await waitFor(() => {
      expect(createValidationRule).toHaveBeenCalledWith(
        customFieldTypeId,
        expect.objectContaining({
          ruleName: "Email Pattern",
          ruleType: "REGEX",
        }),
        expect.any(Function),
        null,
      );
    });

    // Should call onSave callback
    await waitFor(() => {
      expect(mockOnSave).toHaveBeenCalled();
    });
  });

  /**
   * Test: Cancel button calls onCancel
   */
  test("testCancelButton_CallsOnCancel", async () => {
    renderWithIntl(
      <ValidationRuleEditor
        customFieldTypeId={customFieldTypeId}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />,
    );

    const cancelButton = screen.getByTestId("cancel-button");
    await userEvent.click(cancelButton);

    expect(mockOnCancel).toHaveBeenCalled();
  });

  /**
   * Test: Editing existing rule loads rule data
   */
  test("testEditingRule_LoadsRuleData", async () => {
    const editingRule = {
      id: "RULE-001",
      ruleName: "Email Pattern",
      ruleType: "REGEX",
      ruleExpression: "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
      errorMessage: "Invalid email format",
      isActive: true,
    };

    renderWithIntl(
      <ValidationRuleEditor
        customFieldTypeId={customFieldTypeId}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        editingRule={editingRule}
      />,
    );

    // Should load rule data
    expect(screen.getByTestId("rule-name-input").value).toBe("Email Pattern");
    expect(screen.getByTestId("regex-pattern-input").value).toBe(
      "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
    );
  });
});
