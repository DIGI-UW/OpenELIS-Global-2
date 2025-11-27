import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import "@testing-library/jest-dom";
import UnifiedBarcodeInput from "./UnifiedBarcodeInput";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

// Mock the API utility
jest.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
  postToOpenElisServerJsonResponse: jest.fn(),
}));

// Mock translations
const messages = {
  "barcode.scanOrType": "Scan barcode or type location",
  "barcode.scan": "Scan barcode",
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
  let mockOnValidationResult;
  let mockOnSampleScan;

  beforeEach(() => {
    mockOnScan = jest.fn();
    mockOnValidationResult = jest.fn();
    mockOnSampleScan = jest.fn();
    jest.clearAllMocks();
    postToOpenElisServerJsonResponse.mockClear();
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

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(mockResponse);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      // Enter barcode and press Enter
      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
        `/rest/storage/barcode/validate`,
        JSON.stringify({ barcode: barcode }),
        expect.any(Function),
      );
    });

    it("should include correct query parameters in API call", () => {
      const barcode = "ROOM-DEVICE";

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess({ valid: true });
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");
      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      const callUrl = postToOpenElisServerJsonResponse.mock.calls[0][0];
      const callPayload = postToOpenElisServerJsonResponse.mock.calls[0][1];
      expect(callUrl).toBe("/rest/storage/barcode/validate");
      expect(JSON.parse(callPayload)).toEqual({ barcode: barcode });
    });
  });

  describe("API Call on Blur", () => {
    // Note: Blur testing is unreliable with Carbon components in jsdom
    // These tests verify structure and no-op scenarios

    it("should not call API on blur with empty field", () => {
      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");
      fireEvent.blur(input);

      expect(postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
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

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(mockResponse);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
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
        firstMissingLevel: null,
        hasAdditionalInvalidLevels: false,
        validComponents: {},
      });
    });

    it("should call onScan callback with barcode", () => {
      const barcode = "MAIN-FRZ01";
      const mockResponse = { valid: true };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(mockResponse);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
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

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(mockError);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      // UnifiedBarcodeInput wraps validation errors when barcodeType is "location" and valid is false
      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        data: mockError,
        firstMissingLevel: mockError.firstMissingLevel || null,
        validComponents: mockError.validComponents || {},
        hasAdditionalInvalidLevels:
          mockError.hasAdditionalInvalidLevels || false,
        error: {
          errorMessage: mockError.errorMessage,
          message: mockError.errorMessage,
        },
      });
    });

    it("should handle network errors gracefully", () => {
      const barcode = "MAIN-FRZ01";
      const networkError = new Error("Network error");

      // postToOpenElisServerJsonResponse passes errors in response object
      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess({
            error: networkError.message || "Network error",
            message: networkError.message || "Network error",
            status: 0,
          });
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      // Network errors are passed directly - component checks response.error || response.status >= 400
      // Returns: { success: false, error: response.error || { message: "Validation failed" } }
      // If response.error is a string, it uses it directly; if it's an object, it uses the object
      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        error: networkError.message || "Network error", // Component uses response.error directly if it's a string
      });
    });

    it("should handle validation errors", () => {
      const barcode = "INVALID-CODE";
      const validationError = {
        valid: false,
        errorMessage: "Invalid barcode format",
        errorType: "INVALID_FORMAT",
      };

      // postToOpenElisServerJsonResponse passes errors in response object
      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(validationError);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      // UnifiedBarcodeInput wraps validation errors in error object when valid is false
      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        data: validationError,
        firstMissingLevel: validationError.firstMissingLevel || null,
        validComponents: validationError.validComponents || {},
        hasAdditionalInvalidLevels:
          validationError.hasAdditionalInvalidLevels || false,
        error: {
          errorMessage: validationError.errorMessage,
          message: validationError.errorMessage,
        },
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

      // postToOpenElisServerJsonResponse passes errors in response object
      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(mockResponse);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      // UnifiedBarcodeInput wraps validation errors in error object when valid is false
      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        data: mockResponse,
        firstMissingLevel: mockResponse.firstMissingLevel || null,
        validComponents: mockResponse.validComponents || {},
        hasAdditionalInvalidLevels:
          mockResponse.hasAdditionalInvalidLevels || false,
        error: {
          errorMessage: mockResponse.errorMessage,
          message: mockResponse.errorMessage,
        },
      });
    });

    it("should handle complete validation failure", () => {
      const barcode = "NOTFOUND-INVALID";
      const mockResponse = {
        valid: false,
        validComponents: {},
        errorMessage: "No matching locations found",
      };

      // postToOpenElisServerJsonResponse passes errors in response object
      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(mockResponse);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      // UnifiedBarcodeInput wraps validation errors in error object when valid is false
      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        data: mockResponse,
        firstMissingLevel: mockResponse.firstMissingLevel || null,
        validComponents: mockResponse.validComponents || {},
        hasAdditionalInvalidLevels:
          mockResponse.hasAdditionalInvalidLevels || false,
        error: {
          errorMessage: mockResponse.errorMessage,
          message: mockResponse.errorMessage,
        },
      });
    });
  });

  describe("Debouncing", () => {
    it("should call API only once per barcode scan", () => {
      jest.useFakeTimers();

      const barcode = "MAIN-FRZ01";
      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess({ valid: true });
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");

      // First scan
      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);

      jest.useRealTimers();
    });
  });

  describe("Comprehensive Validation Scenarios", () => {
    it("should validate invalid format (no hyphens) and return error", () => {
      const invalidFormat = "234";
      const mockResponse = {
        valid: false,
        barcodeType: "location",
        errorMessage:
          "Invalid barcode format. Expected format: ROOM-DEVICE or ROOM-DEVICE-SHELF-RACK-POSITION",
        failedStep: "FORMAT_VALIDATION",
        firstMissingLevel: null,
        validComponents: {},
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(mockResponse);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");
      fireEvent.change(input, { target: { value: invalidFormat } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnScan).toHaveBeenCalledWith(invalidFormat);
      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        data: mockResponse,
        firstMissingLevel: null,
        validComponents: {},
        hasAdditionalInvalidLevels: false,
        error: {
          errorMessage: mockResponse.errorMessage,
          message: mockResponse.errorMessage,
        },
      });
    });

    it("should validate invalid barcode (hyphens but invalid location) and return error", () => {
      const invalidBarcode = "INVALID-CODE";
      const mockResponse = {
        valid: false,
        barcodeType: "location",
        errorMessage: "Location not found: INVALID-CODE",
        failedStep: "LOCATION_EXISTENCE",
        firstMissingLevel: null,
        validComponents: {},
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(mockResponse);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");
      fireEvent.change(input, { target: { value: invalidBarcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnScan).toHaveBeenCalledWith(invalidBarcode);
      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        data: mockResponse,
        firstMissingLevel: null,
        validComponents: {},
        hasAdditionalInvalidLevels: false,
        error: {
          errorMessage: mockResponse.errorMessage,
          message: mockResponse.errorMessage,
        },
      });
    });

    it("should detect sample barcode and call onSampleScan", () => {
      const sampleBarcode = "25-00001";
      const mockResponse = {
        valid: false,
        barcodeType: "sample",
        errorMessage:
          "Scanned barcode appears to be a sample accession number, not a location barcode",
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(mockResponse);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          onSampleScan={mockOnSampleScan}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");
      fireEvent.change(input, { target: { value: sampleBarcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnScan).toHaveBeenCalledWith(sampleBarcode);
      expect(mockOnSampleScan).toHaveBeenCalledWith({
        barcode: sampleBarcode,
        type: "sample",
        data: mockResponse,
      });
      expect(mockOnValidationResult).not.toHaveBeenCalled();
    });

    it("should handle complete validation failure (no valid components)", () => {
      const barcode = "NOTFOUND-INVALID";
      const mockResponse = {
        valid: false,
        barcodeType: "location",
        errorMessage: "No matching locations found",
        failedStep: "LOCATION_EXISTENCE",
        firstMissingLevel: null,
        validComponents: {},
        hasAdditionalInvalidLevels: false,
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(mockResponse);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");
      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        data: mockResponse,
        firstMissingLevel: null,
        validComponents: {},
        hasAdditionalInvalidLevels: false,
        error: {
          errorMessage: mockResponse.errorMessage,
          message: mockResponse.errorMessage,
        },
      });
    });

    it("should handle partial validation (some components valid)", () => {
      const barcode = "MAIN-FRZ01-INVALID";
      const mockResponse = {
        valid: false,
        barcodeType: "location",
        errorMessage: "Shelf not found",
        failedStep: "LOCATION_EXISTENCE",
        firstMissingLevel: "shelf",
        validComponents: {
          room: { id: "1", code: "MAIN", name: "Main Laboratory" },
          device: { id: "2", code: "FRZ01", name: "Freezer Unit 1" },
        },
        hasAdditionalInvalidLevels: false,
      };

      postToOpenElisServerJsonResponse.mockImplementation(
        (url, payload, onSuccess) => {
          onSuccess(mockResponse);
        },
      );

      renderWithIntl(
        <UnifiedBarcodeInput
          onScan={mockOnScan}
          onValidationResult={mockOnValidationResult}
          validationState="ready"
        />,
      );

      const input = screen.getByRole("textbox");
      fireEvent.change(input, { target: { value: barcode } });
      fireEvent.keyDown(input, { key: "Enter", code: "Enter" });

      expect(mockOnValidationResult).toHaveBeenCalledWith({
        success: false,
        data: mockResponse,
        firstMissingLevel: "shelf",
        validComponents: mockResponse.validComponents,
        hasAdditionalInvalidLevels: false,
        error: {
          errorMessage: mockResponse.errorMessage,
          message: mockResponse.errorMessage,
        },
      });
    });
  });
});
