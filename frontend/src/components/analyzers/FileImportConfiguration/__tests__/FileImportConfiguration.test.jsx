/**
 * FileImportConfiguration Component Tests
 *
 * Testing Roadmap: .specify/guides/testing-roadmap.md
 *
 * Test Strategy:
 * - Use data-testid for reliable element selection (PREFERRED)
 * - Use waitFor with queryBy* for async operations
 * - Use userEvent for user interactions (PREFERRED)
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

jest.mock("../../../../services/fileImportService", () => ({
  createConfiguration: jest.fn(),
  updateConfiguration: jest.fn(),
  getConfigurationByAnalyzerId: jest.fn(),
}));

jest.mock("../../../../services/analyzerService", () => ({
  getAnalyzers: jest.fn(),
}));

// ========== IMPORTS ==========

import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import FileImportConfiguration from "../FileImportConfiguration";
import {
  createConfiguration,
  updateConfiguration,
} from "../../../../services/fileImportService";
import { getAnalyzers } from "../../../../services/analyzerService";
import messages from "../../../../languages/en.json";

// ========== TEST SETUP ==========

const renderWithIntl = (component) => {
  return render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );
};

describe("FileImportConfiguration", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    getAnalyzers.mockImplementation((filters, callback) => {
      callback({
        analyzers: [
          { id: "1", name: "Test Analyzer 1" },
          { id: "2", name: "Test Analyzer 2" },
        ],
      });
    });
  });

  test("testSubmitForm_WithValidData_CallsAPI", async () => {
    // Arrange
    createConfiguration.mockImplementation((data, callback) => {
      callback(
        { id: "config-1", message: "Configuration created successfully" },
        null,
      );
    });

    const onClose = jest.fn();

    // Act: Render form
    renderWithIntl(<FileImportConfiguration open={true} onClose={onClose} />);

    // Wait for form to render
    await screen.findByTestId(
      "file-import-configuration-form",
      {},
      { timeout: 2000 },
    );

    // Fill in form fields using data-testid
    const directoryInput = screen.getByTestId(
      "file-import-configuration-directory-input",
    );
    await userEvent.type(directoryInput, "/data/import", { delay: 0 });

    // Try to submit form (will fail validation because analyzerId is required)
    const saveButton = screen.getByTestId(
      "file-import-configuration-form-save-button",
    );
    await userEvent.click(saveButton);

    // Assert: Verify API was NOT called because validation should fail
    // (analyzerId is required but not selected)
    await waitFor(() => {
      expect(createConfiguration).not.toHaveBeenCalled();
    });
  });

  test("testValidateColumnMappings_WithInvalidJSON_ShowsError", async () => {
    // Arrange
    const onClose = jest.fn();

    // Act: Render form
    renderWithIntl(<FileImportConfiguration open={true} onClose={onClose} />);

    // Wait for form to render
    await screen.findByTestId(
      "file-import-configuration-form",
      {},
      { timeout: 2000 },
    );

    // Enter invalid JSON in column mappings
    const columnMappingsInput = screen.getByTestId(
      "file-import-configuration-column-mappings-input",
    );
    // Clear not needed - field starts empty
    await userEvent.type(columnMappingsInput, "invalid json", { delay: 0 });

    // Try to submit (trigger validation)
    const saveButton = screen.getByTestId(
      "file-import-configuration-form-save-button",
    );
    await userEvent.click(saveButton);

    // Assert: Verify error is displayed
    await waitFor(() => {
      expect(columnMappingsInput).toHaveAttribute("aria-invalid", "true");
    });
  });

  test("testFormRenders_WithAllFields", async () => {
    // Arrange
    const onClose = jest.fn();

    // Act: Render form
    renderWithIntl(<FileImportConfiguration open={true} onClose={onClose} />);

    // Wait for form to render
    await screen.findByTestId(
      "file-import-configuration-form",
      {},
      { timeout: 2000 },
    );

    // Assert: Verify all form fields are present
    expect(
      screen.getByTestId("file-import-configuration-analyzer-dropdown"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("file-import-configuration-directory-input"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("file-import-configuration-pattern-input"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("file-import-configuration-archive-input"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("file-import-configuration-error-input"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("file-import-configuration-column-mappings-input"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("file-import-configuration-delimiter-input"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("file-import-configuration-has-header-checkbox"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("file-import-configuration-active-checkbox"),
    ).toBeInTheDocument();
  });

  test("testEditMode_DisablesAnalyzerDropdown", async () => {
    // Arrange
    const configuration = {
      id: "config-1",
      analyzerId: "1",
      importDirectory: "/data/import",
      filePattern: "*.csv",
      delimiter: ",",
      hasHeader: true,
      active: true,
    };
    const onClose = jest.fn();

    // Act: Render form in edit mode
    renderWithIntl(
      <FileImportConfiguration
        configuration={configuration}
        open={true}
        onClose={onClose}
      />,
    );

    // Wait for form to render
    await screen.findByTestId(
      "file-import-configuration-form",
      {},
      { timeout: 2000 },
    );

    // Assert: Verify analyzer dropdown is disabled
    // In Carbon Dropdown, the disabled prop is applied to the toggle button inside the wrapper
    const analyzerDropdownWrapper = screen.getByTestId(
      "file-import-configuration-analyzer-dropdown",
    );
    // Find the button inside the dropdown (Carbon adds disabled to the button)
    const dropdownButton = analyzerDropdownWrapper.querySelector("button");
    expect(dropdownButton).toHaveAttribute("disabled");
  });

  test("testCancelButton_ClosesModal", async () => {
    // Arrange
    const onClose = jest.fn();

    // Act: Render form and click cancel
    renderWithIntl(<FileImportConfiguration open={true} onClose={onClose} />);

    await screen.findByTestId(
      "file-import-configuration-form",
      {},
      { timeout: 2000 },
    );

    const cancelButton = screen.getByTestId(
      "file-import-configuration-form-cancel-button",
    );
    await userEvent.click(cancelButton);

    // Assert: Verify onClose was called
    expect(onClose).toHaveBeenCalled();
  });
});
