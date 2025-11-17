import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import "@testing-library/jest-dom";
import UnifiedBarcodeInput from "./UnifiedBarcodeInput";
import { getFromOpenElisServer } from "../../utils/Utils";

// Mock the API utility
jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

// Mock translations
const messages = {
  "barcode.scanOrType": "Scan barcode or type location",
  "barcode.ready": "Ready to scan",
  "barcode.success": "Location found",
  "barcode.error": "Invalid barcode",
  "barcode.invalidFormat": "Invalid barcode format",
};

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

describe("UnifiedBarcodeInput Integration Tests", () => {
  let mockOnScan;
  let mockOnTypeAhead;
  let mockOnValidationResult;

  beforeEach(() => {
    mockOnScan = jest.fn();
    mockOnTypeAhead = jest.fn();
    mockOnValidationResult = jest.fn();
    jest.clearAllMocks();
  });

  afterEach(() => {
    jest.clearAllTimers();
  });

  describe("API Call on Enter", () => {
    it("should call validation API when Enter key is pressed with barcode", () => {
      const barcode = "MAIN-FRZ01-SHA-RKR1";
      const mockResponse = {
        valid: true,
        room: { id: "1", code: "MAIN", name: "Main Laboratory" },
        device: { id: "2", code: "FRZ01", name: "Freezer Unit 1" },
        shelf: { id: "3", label: "SHA" },
        rack: { id: "4", label: "RKR1" },
      };

      getFromOpenElisServer.mockImplementation((url, onSuccess) => {
        onSuccess(mockResponse);
      });

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onTypeAhead={mockOnTypeAhead}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      // Enter barcode and press Enter
      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        `/rest/storage/barcode/validate?barcode=${encodeURIComponent(barcode)}`,
        expect.any(Function),
        expect.any(Function),
      );
    });

    it("should include correct query parameters in API call", () => {
      const barcode = "ROOM-DEVICE";

      getFromOpenElisServer.mockImplementation((url, onSuccess) => {
        onSuccess({ valid: true });
      });

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onTypeAhead={mockOnTypeAhead}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");
      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      const callUrl = getFromOpenElisServer.mock.calls[0][0];
      expect(callUrl).toContain("/rest/storage/barcode/validate");
      expect(callUrl).toContain(`barcode=${encodeURIComponent(barcode)}`);
    });
  });

  describe("API Call on Blur", () => {
    // Note: Blur testing is unreliable with Carbon components in jsdom
    // These tests verify structure and no-op scenarios

    it("should not call API on blur with empty field", () => {
      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onTypeAhead={mockOnTypeAhead}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");
      fireEvent.blur(input);

      expect(getFromOpenElisServer).not.toHaveBeenCalled();
    });
  });

  describe("Success Response Handling", () => {
    it("should call onValidationResult with success data", () => {
      const barcode = "MAIN-FRZ01-SHA-RKR1";
      const mockResponse = {
        valid: true,
        room: { id: "1", code: "MAIN", name: "Main Laboratory" },
        device: { id: "2", code: "FRZ01", name: "Freezer Unit 1" },
        shelf: { id: "3", label: "SHA" },
        rack: { id: "4", label: "RKR1" },
        hierarchicalPath: "Main Laboratory > Freezer Unit 1 > SHA > RKR1",
      };

      getFromOpenElisServer.mockImplementation((url, onSuccess) => {
        onSuccess(mockResponse);
      });

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onTypeAhead={mockOnTypeAhead}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: true,
        data: mockResponse,
        error: null, // UnifiedBarcodeInput includes error: null when valid
      });
    });

    it("should call onScan callback with barcode", () => {
      const barcode = "MAIN-FRZ01";
      const mockResponse = { valid: true };

      getFromOpenElisServer.mockImplementation((url, onSuccess) => {
        onSuccess(mockResponse);
      });

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onTypeAhead={mockOnTypeAhead}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnScan).toHaveBeenCalledWith(barcode);
    });
  });

  describe("Error Response Handling", () => {
    it("should call onValidationResult with error data", () => {
      const barcode = "INVALID-BARCODE";
      const mockError = {
        valid: false,
        errorMessage: "Location not found",
        errorType: "LOCATION_NOT_FOUND",
      };

      getFromOpenElisServer.mockImplementation((url, onSuccess, onError) => {
        onError(mockError);
      });

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onTypeAhead={mockOnTypeAhead}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        error: mockError,
      });
    });

    it("should handle network errors gracefully", () => {
      const barcode = "MAIN-FRZ01";
      const networkError = new Error("Network error");

      getFromOpenElisServer.mockImplementation((url, onSuccess, onError) => {
        onError(networkError);
      });

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onTypeAhead={mockOnTypeAhead}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        error: networkError,
      });
    });

    it("should handle validation errors", () => {
      const barcode = "INVALID-CODE";
      const validationError = {
        valid: false,
        errorMessage: "Invalid barcode format",
        errorType: "INVALID_FORMAT",
      };

      getFromOpenElisServer.mockImplementation((url, onSuccess, onError) => {
        onError(validationError);
      });

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onTypeAhead={mockOnTypeAhead}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        error: validationError,
      });
    });
  });

  describe("Partial Validation", () => {
    it("should handle partial validation success", () => {
      const barcode = "MAIN-FRZ01-INVALID";
      const mockResponse = {
        valid: false,
        validComponents: {
          room: { id: "1", code: "MAIN" },
          device: { id: "2", code: "FRZ01" },
        },
        errorMessage: "Shelf not found",
      };

      getFromOpenElisServer.mockImplementation((url, onSuccess, onError) => {
        onError(mockResponse);
      });

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onTypeAhead={mockOnTypeAhead}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        error: mockResponse,
      });
    });

    it("should handle complete validation failure", () => {
      const barcode = "NOTFOUND-INVALID";
      const mockResponse = {
        valid: false,
        validComponents: {},
        errorMessage: "No matching locations found",
      };

      getFromOpenElisServer.mockImplementation((url, onSuccess, onError) => {
        onError(mockResponse);
      });

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onTypeAhead={mockOnTypeAhead}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        error: mockResponse,
      });
    });
  });

  describe("Debouncing", () => {
    it("should call API only once per barcode scan", () => {
      jest.useFakeTimers();

      const barcode = "MAIN-FRZ01";
      getFromOpenElisServer.mockImplementation((url, onSuccess) => {
        onSuccess({ valid: true });
      });

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onTypeAhead={mockOnTypeAhead}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      // First scan
      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(getFromOpenElisServer).toHaveBeenCalledTimes(1);

      jest.useRealTimers();
    });
  });
});
