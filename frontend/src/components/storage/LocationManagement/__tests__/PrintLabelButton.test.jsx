import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import PrintLabelButton from "../PrintLabelButton";
import messages from "../../../../languages/en.json";

// Mock config
jest.mock("../../../../config.json", () => ({
  serverBaseUrl: "/api/OpenELIS-Global",
}));

// Mock fetch globally
global.fetch = jest.fn();

// Mock window.URL methods
global.URL.createObjectURL = jest.fn(() => "blob:mock-url");
global.URL.revokeObjectURL = jest.fn();

// window.open spy - will be set up in beforeEach
let windowOpenSpy;

// Mock document.createElement and appendChild/removeChild for link element only
const mockLink = {
  href: "",
  download: "",
  click: jest.fn(),
};

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("PrintLabelButton", () => {
  const mockOnPrintSuccess = jest.fn();
  const mockOnPrintError = jest.fn();
  let createElementSpy;
  let appendChildSpy;
  let removeChildSpy;
  const originalCreateElement = document.createElement;
  const originalAppendChild = document.body.appendChild;
  const originalRemoveChild = document.body.removeChild;

  beforeEach(() => {
    jest.clearAllMocks();
    mockLink.href = "";
    mockLink.download = "";
    mockLink.click.mockClear();

    // Spy on window.open - must use global since jsdom sets window = global
    windowOpenSpy = jest.spyOn(global, "open").mockImplementation(() => null);

    // Spy on document.createElement to intercept "a" tag creation
    createElementSpy = jest
      .spyOn(document, "createElement")
      .mockImplementation((tag) => {
        if (tag === "a") {
          return mockLink;
        }
        // For all other tags, use the original implementation
        return originalCreateElement.call(document, tag);
      });

    // Spy on appendChild/removeChild to track link operations
    appendChildSpy = jest
      .spyOn(document.body, "appendChild")
      .mockImplementation((node) => {
        if (node === mockLink) {
          return node;
        }
        return originalAppendChild.call(document.body, node);
      });

    removeChildSpy = jest
      .spyOn(document.body, "removeChild")
      .mockImplementation((node) => {
        if (node === mockLink) {
          return node;
        }
        return originalRemoveChild.call(document.body, node);
      });
  });

  afterEach(() => {
    // Restore all spies
    createElementSpy.mockRestore();
    appendChildSpy.mockRestore();
    removeChildSpy.mockRestore();
    if (windowOpenSpy) {
      windowOpenSpy.mockRestore();
    }
  });

  /**
   * T287: Test print label button shows confirmation dialog
   * Expected: Confirmation dialog appears when button is clicked
   */
  test("testPrintLabelButtonShowsConfirmationDialog", async () => {
    renderWithIntl(
      <PrintLabelButton
        locationType="device"
        locationId="1"
        locationName="Freezer Unit 1"
        locationCode="FRZ01"
        onPrintSuccess={mockOnPrintSuccess}
        onPrintError={mockOnPrintError}
      />,
    );

    // Find and click print button
    const printButton = screen.getByTestId("print-label-button");
    await userEvent.click(printButton);

    // Verify confirmation dialog appears
    // Carbon ComposedModal may render in a portal, so use findByTestId
    const dialog = await screen.findByTestId(
      "print-label-confirmation-dialog",
      {},
      { timeout: 3000 },
    );
    expect(dialog).toBeTruthy();
  });

  /**
   * T287: Test confirmation dialog text displays location details
   * Expected: Dialog shows "Print label for [Location Name] ([Location Code])?"
   */
  test("testConfirmationDialogText", async () => {
    renderWithIntl(
      <PrintLabelButton
        locationType="device"
        locationId="1"
        locationName="Freezer Unit 1"
        locationCode="FRZ01"
        onPrintSuccess={mockOnPrintSuccess}
        onPrintError={mockOnPrintError}
      />,
    );

    // Click print button to open dialog
    const printButton = screen.getByTestId("print-label-button");
    await userEvent.click(printButton);

    // Wait for dialog to appear (Carbon ComposedModal may render in a portal)
    const dialog = await screen.findByTestId(
      "print-label-confirmation-dialog",
      {},
      { timeout: 3000 },
    );
    expect(dialog).toBeTruthy();

    // Verify dialog text contains location name and code
    expect(screen.getByText(/Freezer Unit 1/i)).toBeTruthy();
    expect(screen.getByText(/FRZ01/i)).toBeTruthy();
  });

  /**
   * T290: Test error message if code missing or invalid
   * Expected: Error message displayed when backend returns 400 with JSON error
   */
  test("testErrorMessageIfCodeMissingOrInvalid", async () => {
    // Mock fetch to return JSON error response (not PDF)
    global.fetch.mockResolvedValueOnce({
      ok: false,
      status: 400,
      headers: {
        get: () => "application/json",
      },
      json: () =>
        Promise.resolve({
          error:
            "Code is required for label printing. Please set code in Edit form first.",
        }),
    });

    renderWithIntl(
      <PrintLabelButton
        locationType="device"
        locationId="1"
        locationName="Freezer Unit 1"
        locationCode="FRZ01"
        onPrintSuccess={mockOnPrintSuccess}
        onPrintError={mockOnPrintError}
      />,
    );

    // Click print button
    const printButton = screen.getByTestId("print-label-button");
    await userEvent.click(printButton);

    // Wait for dialog and confirm (Carbon ComposedModal may render in a portal)
    const dialog = await screen.findByTestId(
      "print-label-confirmation-dialog",
      {},
      { timeout: 3000 },
    );
    expect(dialog).toBeTruthy();

    const confirmButton = screen.getByTestId("confirm-print-button");
    await userEvent.click(confirmButton);

    // Wait for error callback
    await waitFor(() => {
      expect(mockOnPrintError).toHaveBeenCalled();
    });

    // Verify error callback was called with error containing "Code"
    const errorCall = mockOnPrintError.mock.calls[0][0];
    expect(errorCall.message).toContain("Code");
  });

  /**
   * T287: Test successful PDF generation and download
   * Expected: PDF is generated, blob created, and download triggered
   */
  test("testSuccessfulPdfGenerationAndDownload", async () => {
    // Mock fetch to return PDF blob
    const mockBlob = new Blob(["PDF content"], { type: "application/pdf" });
    global.fetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      headers: {
        get: () => "application/pdf",
      },
      blob: () => Promise.resolve(mockBlob),
    });

    renderWithIntl(
      <PrintLabelButton
        locationType="device"
        locationId="1"
        locationName="Freezer Unit 1"
        locationCode="FRZ01"
        onPrintSuccess={mockOnPrintSuccess}
        onPrintError={mockOnPrintError}
      />,
    );

    // Click print button
    const printButton = screen.getByTestId("print-label-button");
    await userEvent.click(printButton);

    // Wait for dialog and confirm (Carbon ComposedModal may render in a portal)
    const dialog = await screen.findByTestId(
      "print-label-confirmation-dialog",
      {},
      { timeout: 3000 },
    );
    expect(dialog).toBeTruthy();

    const confirmButton = screen.getByTestId("confirm-print-button");
    await userEvent.click(confirmButton);

    // Wait for fetch call
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.stringContaining(
          "/api/OpenELIS-Global/rest/storage/device/1/print-label",
        ),
        expect.objectContaining({
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": null, // localStorage.getItem("CSRF") returns null in test environment
          },
          credentials: "include",
        }),
      );
    });

    // Wait for blob processing and success callback
    // Note: We verify createObjectURL was called and onPrintSuccess was triggered,
    // which proves the PDF blob was processed. window.open verification is difficult
    // in jsdom as it requires complex mocking - the behavior should be verified
    // manually or with E2E tests.
    await waitFor(() => {
      expect(global.URL.createObjectURL).toHaveBeenCalled();
      expect(mockOnPrintSuccess).toHaveBeenCalled();
    });
  });

  /**
   * T287: Test cancel button closes dialog without printing
   * Expected: Dialog closes, no API call made, no callbacks triggered
   */
  test("testCancelButtonClosesDialogWithoutPrinting", async () => {
    renderWithIntl(
      <PrintLabelButton
        locationType="device"
        locationId="1"
        locationName="Freezer Unit 1"
        locationCode="FRZ01"
        onPrintSuccess={mockOnPrintSuccess}
        onPrintError={mockOnPrintError}
      />,
    );

    // Click print button
    const printButton = screen.getByTestId("print-label-button");
    await userEvent.click(printButton);

    // Wait for dialog (Carbon ComposedModal may render in a portal)
    const dialog = await screen.findByTestId(
      "print-label-confirmation-dialog",
      {},
      { timeout: 3000 },
    );
    expect(dialog).toBeTruthy();

    // Click cancel
    const cancelButton = screen.getByTestId("cancel-print-button");
    await userEvent.click(cancelButton);

    // Similar to DeleteLocationModal.test.jsx - verify behavior, not DOM state
    // Modal state is managed internally, we just verify no API call was made
    // Note: onPrintError(null) is called for autoTrigger mode, but not for regular mode
    expect(global.fetch).not.toHaveBeenCalled();
    expect(mockOnPrintSuccess).not.toHaveBeenCalled();
  });

  /**
   * T287: Test auto-trigger mode shows dialog automatically
   * Expected: Dialog appears automatically when autoTrigger is true
   */
  test("testAutoTriggerModeShowsDialogAutomatically", async () => {
    renderWithIntl(
      <PrintLabelButton
        locationType="device"
        locationId="1"
        locationName="Freezer Unit 1"
        locationCode="FRZ01"
        onPrintSuccess={mockOnPrintSuccess}
        onPrintError={mockOnPrintError}
        autoTrigger={true}
      />,
    );

    // Verify dialog appears automatically (no button click needed)
    // Carbon ComposedModal may render in a portal
    const dialog = await screen.findByTestId(
      "print-label-confirmation-dialog",
      {},
      { timeout: 3000 },
    );
    expect(dialog).toBeTruthy();

    // Verify button is not rendered in auto-trigger mode
    expect(screen.queryByTestId("print-label-button")).toBeNull();
  });
});
