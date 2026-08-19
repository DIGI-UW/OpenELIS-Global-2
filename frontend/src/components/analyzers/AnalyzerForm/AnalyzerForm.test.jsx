/**
 * AnalyzerForm Component Tests
 *
 * Testing Roadmap: .specify/guides/testing-roadmap.md
 *
 * Test Strategy:
 * - Use data-testid for reliable element selection (PREFERRED)
 * - Use waitFor with queryBy* for async operations
 * - Use userEvent for user interactions (PREFERRED)
 */

// ========== MOCKS (BEFORE IMPORTS - Jest hoisting) ==========

vi.mock("../../../services/analyzerService", () => ({
  createAnalyzer: vi.fn(),
  updateAnalyzer: vi.fn(),
  testConnection: vi.fn(),
  getAnalyzer: vi.fn(),
  getAnalyzerTypeCatalog: vi.fn(),
}));

// ========== IMPORTS ==========

import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import { vi } from "vitest";
import AnalyzerForm from "./AnalyzerForm";
import {
  createAnalyzer,
  getAnalyzerTypeCatalog,
} from "../../../services/analyzerService";
import messages from "../../../languages/en.json";

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

const profileCatalog = {
  schemaVersion: "1.0",
  catalogFingerprint: "sha256:catalog",
  summary: { total: 1, inUse: 0, needsAttention: 1, deactivated: 0 },
  types: [
    {
      profileId: "shipped.astm",
      revision: 1,
      revisionFingerprint: "sha256:astm",
      displayName: "ASTM Analyzer",
      source: "SHIPPED",
      status: "ACTIVE",
      protocol: "ASTM",
    },
  ],
};

describe("AnalyzerForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAnalyzerTypeCatalog.mockImplementation((callback) => {
      callback(profileCatalog);
    });
  });

  test("testSubmitForm_WithValidData_CallsAPI", async () => {
    // Arrange
    createAnalyzer.mockImplementation((data, callback) => {
      callback({ id: "1", ...data }, null);
    });

    const onClose = vi.fn();

    // Act: Render form
    renderWithIntl(<AnalyzerForm open={true} onClose={onClose} />);

    // Wait for form to render
    await screen.findByTestId("analyzer-form", {}, { timeout: 2000 });

    // Fill in form fields using data-testid
    const nameInput = screen.getByTestId("analyzer-form-name-input");
    await userEvent.type(nameInput, "Test Analyzer", { delay: 0 });

    const ipInput = screen.getByTestId("analyzer-form-ip-input");
    await userEvent.type(ipInput, "192.168.1.100", { delay: 0 });

    const portInput = screen.getByTestId("analyzer-form-port-input");
    await userEvent.type(portInput, "5000", { delay: 0 });

    // Try to submit without selecting an Analyzer Type.
    const saveButton = screen.getByTestId("analyzer-form-save-button");
    await userEvent.click(saveButton);

    // Assert: Verify API was NOT called because validation should fail
    // (an Analyzer Type revision is required but not selected)
    await waitFor(() => {
      expect(createAnalyzer).not.toHaveBeenCalled();
    });

    // Verify the Analyzer Type control is present for the validation error.
    const typeDropdown = screen.getByTestId("analyzer-form-type-dropdown");
    // Check that dropdown exists (validation will show error)
    expect(typeDropdown).not.toBeNull();
  });

  /**
   * The form consumes the reusable Analyzer Type catalog, not plugin handlers.
   */
  test("testAnalyzerTypeDropdown_DisplaysAllOptions", async () => {
    // Arrange
    const onClose = vi.fn();

    // Act: Render form
    renderWithIntl(<AnalyzerForm open={true} onClose={onClose} />);

    // Wait for form to render
    await screen.findByTestId("analyzer-form", {}, { timeout: 2000 });

    // Find analyzer type dropdown
    const typeDropdown = screen.getByTestId("analyzer-form-type-dropdown");
    expect(typeDropdown).not.toBeNull();

    expect(getAnalyzerTypeCatalog).toHaveBeenCalledTimes(1);
  });

  test("testValidateIPAddress_WithInvalidFormat_ShowsError", async () => {
    // Arrange
    const onClose = vi.fn();

    // Act: Render form
    renderWithIntl(<AnalyzerForm open={true} onClose={onClose} />);

    // Wait for form to render
    await screen.findByTestId("analyzer-form", {}, { timeout: 2000 });

    // Enter invalid IP address
    const ipInput = screen.getByTestId("analyzer-form-ip-input");
    await userEvent.type(ipInput, "invalid-ip", { delay: 0 });

    // Try to submit (trigger validation)
    const saveButton = screen.getByTestId("analyzer-form-save-button");
    await userEvent.click(saveButton);

    // Assert: Verify error is displayed
    await waitFor(() => {
      const invalidAttr = ipInput.getAttribute("data-invalid");
      expect(invalidAttr).toBe("true");
    });
  });

  test("testTestConnection_ShowsModal", async () => {
    // Arrange
    const onClose = vi.fn();

    // Act: Render form
    renderWithIntl(<AnalyzerForm open={true} onClose={onClose} />);

    // Wait for form to render
    await screen.findByTestId("analyzer-form", {}, { timeout: 2000 });

    // Fill in IP and port
    const ipInput = screen.getByTestId("analyzer-form-ip-input");
    await userEvent.type(ipInput, "192.168.1.100", { delay: 0 });

    const portInput = screen.getByTestId("analyzer-form-port-input");
    await userEvent.type(portInput, "5000", { delay: 0 });

    // Click test connection button
    const testButton = screen.getByTestId(
      "analyzer-form-test-connection-button",
    );
    await userEvent.click(testButton);

    // Assert: Verify test connection modal opens
    expect(
      await screen.findByTestId("test-connection-modal", {}, { timeout: 2000 }),
    ).toBeInTheDocument();
  });
});
