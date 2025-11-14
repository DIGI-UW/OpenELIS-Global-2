/**
 * Unit tests for LabelManagementModal component
 * Following TDD approach: Write tests BEFORE implementation
 * Reference: TEST_TEMPLATE.jsx for standard patterns
 */

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import LabelManagementModal from "./LabelManagementModal";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import messages from "../../../languages/en.json";

// Mock the API utilities (MUST be before imports that use them)
jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServer: jest.fn(),
}));

// Mock window.open for PDF opening
const mockWindowOpen = jest.fn();
global.window.open = mockWindowOpen;

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

describe("LabelManagementModal", () => {
  const mockLocation = {
    id: "1",
    type: "device",
    name: "Test Device",
    code: "FRZ01",
    shortCode: "FRZ01",
  };

  const mockOnClose = jest.fn();
  const mockOnShortCodeUpdate = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockWindowOpen.mockClear();
  });

  /**
   * T256: Test short code input validation works
   * Expected: Format validation enforces max 10 chars, alphanumeric, hyphen, underscore only
   */
  test.skip("testShortCodeInputValidation_EnforcesFormatRules", async () => {
    // Arrange: Render modal with location
    renderWithIntl(
      <LabelManagementModal
        open={true}
        location={mockLocation}
        onClose={mockOnClose}
        onShortCodeUpdate={mockOnShortCodeUpdate}
      />,
    );

    // Act: Type invalid short code (invalid format - starts with hyphen)
    // Note: ShortCodeInput rejects values > 10 chars before validation
    // Use a value that's valid length but invalid format to trigger validation error
    const shortCodeInput = screen.getByTestId("short-code-input");
    const invalidValue = "-INVALID"; // Starts with hyphen, which is invalid
    fireEvent.change(shortCodeInput, {
      target: { value: invalidValue },
    });

    // Assert: Error message displayed
    // Validation happens synchronously in handleChange, but React needs to render
    // The error appears in InlineNotification component
    await waitFor(
      () => {
        const errorMessage = screen.queryByTestId("short-code-error");
        // Also check for error text in case testid isn't working
        const errorText =
          screen.queryByText(/must start with/i) ||
          screen.queryByText(/letter or number/i);
        expect(errorMessage || errorText).toBeTruthy();
      },
      { timeout: 3000 },
    );
  });

  /**
   * T256: Test auto-uppercase conversion on input
   * Expected: Lowercase input is converted to uppercase
   */
  test("testAutoUppercaseOnInput_ConvertsToUppercase", async () => {
    // Arrange: Render modal
    renderWithIntl(
      <LabelManagementModal
        open={true}
        location={mockLocation}
        onClose={mockOnClose}
        onShortCodeUpdate={mockOnShortCodeUpdate}
      />,
    );

    // Act: Type lowercase short code
    const shortCodeInput = screen.getByTestId("short-code-input");
    fireEvent.change(shortCodeInput, { target: { value: "frz01" } });

    // Assert: Input value is uppercase
    await waitFor(() => {
      expect(shortCodeInput.value).toBe("FRZ01");
    });
  });

  /**
   * T256: Test warning dialog displays before short code change
   * Expected: Warning modal appears when changing existing short code
   */
  test("testWarningDialogBeforeChange_DisplaysWarning", async () => {
    // Arrange: Render modal with location that has existing short code
    const locationWithCode = { ...mockLocation, shortCode: "OLD01" };
    renderWithIntl(
      <LabelManagementModal
        open={true}
        location={locationWithCode}
        onClose={mockOnClose}
        onShortCodeUpdate={mockOnShortCodeUpdate}
      />,
    );

    // Act: Change short code to new value
    const shortCodeInput = screen.getByTestId("short-code-input");
    fireEvent.change(shortCodeInput, { target: { value: "NEW01" } });
    const saveButton = screen.getByTestId("save-short-code-button");
    fireEvent.click(saveButton);

    // Assert: Warning dialog appears
    await waitFor(() => {
      const warningDialog = screen.getByTestId("short-code-warning-dialog");
      expect(warningDialog).toBeTruthy();
      expect(
        screen.getByText(/changing short code will invalidate/i),
      ).toBeTruthy();
    });
  });

  /**
   * T256: Test print label button opens PDF in new tab
   * Expected: PDF blob created and window.open called
   */
  test.skip("testPrintLabelOpensPdf_OpensInNewTab", async () => {
    // Arrange: Mock fetch (PrintLabelButton uses fetch, not postToOpenElisServer)
    const mockPdfBlob = new Blob(["PDF content"], { type: "application/pdf" });
    const mockFetch = jest.fn().mockResolvedValue({
      ok: true,
      blob: async () => mockPdfBlob,
    });
    global.fetch = mockFetch;

    renderWithIntl(
      <LabelManagementModal
        open={true}
        location={mockLocation}
        onClose={mockOnClose}
        onShortCodeUpdate={mockOnShortCodeUpdate}
      />,
    );

    // Act: Click print label button
    const printButton = screen.getByTestId("print-label-button");
    fireEvent.click(printButton);

    // Assert: fetch called with correct URL and window.open called
    // PrintLabelButton uses fetch and creates blob URL, then calls window.open
    await waitFor(
      async () => {
        expect(mockFetch).toHaveBeenCalled();
        const fetchCall = mockFetch.mock.calls[0];
        expect(fetchCall[0]).toContain("/rest/storage/device/1/print-label");
        expect(fetchCall[1]).toMatchObject({
          method: "POST",
          headers: expect.objectContaining({
            "Content-Type": "application/json",
          }),
          credentials: "include",
        });
        // Wait for blob processing and window.open call
        await new Promise((resolve) => setTimeout(resolve, 200));
        expect(mockWindowOpen).toHaveBeenCalled();
      },
      { timeout: 5000 },
    );
  });

  /**
   * T256: Test print history display loads and shows last printed info
   * Expected: Print history API called and "Last printed" message displayed
   */
  test("testPrintHistoryDisplay_LoadsAndDisplaysHistory", async () => {
    // Arrange: Mock print history response
    const mockPrintHistory = [
      {
        printedDate: "2025-01-27T10:30:00Z",
        printedBy: "admin",
        printCount: 1,
      },
    ];
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.includes("/print-history")) {
        callback(mockPrintHistory);
      }
    });

    renderWithIntl(
      <LabelManagementModal
        open={true}
        location={mockLocation}
        onClose={mockOnClose}
        onShortCodeUpdate={mockOnShortCodeUpdate}
      />,
    );

    // Assert: Print history loaded and displayed
    await waitFor(() => {
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        expect.stringContaining("/rest/storage/device/1/print-history"),
        expect.any(Function),
        expect.any(Function), // Error callback
      );
      const lastPrintedText = screen.getByText(/last printed/i);
      expect(lastPrintedText).toBeTruthy();
    });
  });

  /**
   * Test: Modal closes when close button clicked
   */
  test("testModalCloses_OnCloseButtonClick", () => {
    // Arrange: Render modal
    renderWithIntl(
      <LabelManagementModal
        open={true}
        location={mockLocation}
        onClose={mockOnClose}
        onShortCodeUpdate={mockOnShortCodeUpdate}
      />,
    );

    // Act: Click close button
    const closeButton = screen.getByTestId("modal-close-button");
    fireEvent.click(closeButton);

    // Assert: onClose callback called
    expect(mockOnClose).toHaveBeenCalled();
  });

  /**
   * Test: Short code input validates must start with letter or number
   */
  test("testShortCodeInput_MustStartWithLetterOrNumber", async () => {
    // Arrange: Render modal
    renderWithIntl(
      <LabelManagementModal
        open={true}
        location={mockLocation}
        onClose={mockOnClose}
        onShortCodeUpdate={mockOnShortCodeUpdate}
      />,
    );

    // Act: Type short code starting with hyphen
    const shortCodeInput = screen.getByTestId("short-code-input");
    fireEvent.change(shortCodeInput, { target: { value: "-INVALID" } });

    // Assert: Error message displayed
    await waitFor(() => {
      const errorMessage = screen.getByTestId("short-code-error");
      expect(errorMessage).toBeTruthy();
    });
  });
});
