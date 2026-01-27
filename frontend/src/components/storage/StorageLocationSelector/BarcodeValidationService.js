import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * BarcodeValidationService - Client-side barcode validation service
 *
 * Provides methods for:
 * - Barcode format validation
 * - Barcode parsing (extract components)
 * - Server-side validation (call REST API)
 *
 * Barcode Format:
 * - 2-level: ROOM-DEVICE
 * - 3-level: ROOM-DEVICE-SHELF
 * - 4-level: ROOM-DEVICE-SHELF-RACK
 * - 5-level: ROOM-DEVICE-SHELF-RACK-POSITION
 *
 * Delimiter: Hyphen (-)
 */

/**
 * Validate barcode format (client-side)
 * Returns true if format is valid (2-5 levels with hyphen delimiter)
 */
export const validateBarcodeFormat = (barcode) => {
  if (!barcode || typeof barcode !== "string") {
    return false;
  }

  // Must contain hyphens (hierarchical format)
  if (!barcode.includes("-")) {
    return false;
  }

  // Split by hyphen delimiter
  const components = barcode.split("-");

  // Must have 2-5 components (minimum: room+device, maximum: room+device+shelf+rack+position)
  if (components.length < 2 || components.length > 5) {
    return false;
  }

  // All components must be non-empty
  if (components.some((component) => !component || component.trim() === "")) {
    return false;
  }

  return true;
};

/**
 * Parse barcode into components
 * Returns array of components or null if invalid
 */
export const parseBarcodeComponents = (barcode) => {
  if (!validateBarcodeFormat(barcode)) {
    return null;
  }

  const components = barcode.split("-").map((c) => c.trim());

  return {
    room: components[0] || null,
    device: components[1] || null,
    shelf: components.length >= 3 ? components[2] : null,
    rack: components.length >= 4 ? components[3] : null,
    position: components.length >= 5 ? components[4] : null,
    level: components.length,
  };
};

/**
 * Validate barcode via server-side API
 * Calls /rest/storage/barcode/validate endpoint
 *
 * @param {string} barcode - Barcode to validate
 * @param {function} onSuccess - Success callback (receives validation response)
 * @param {function} onError - Error callback (receives error object)
 */
export const validateBarcode = (barcode, onSuccess, onError) => {
  // Client-side format validation first
  if (!validateBarcodeFormat(barcode)) {
    const error = {
      valid: false,
      errorMessage:
        "Invalid barcode format. Expected format: ROOM-DEVICE or ROOM-DEVICE-SHELF-RACK-POSITION",
      errorType: "INVALID_FORMAT",
    };
    if (onError) {
      onError(error);
    }
    return;
  }

  // Call server-side validation API
  const url = `/rest/storage/barcode/validate?barcode=${encodeURIComponent(barcode)}`;

  getFromOpenElisServer(
    url,
    (response) => {
      if (onSuccess) {
        onSuccess(response);
      }
    },
    (error) => {
      if (onError) {
        onError(error);
      }
    },
  );
};

/**
 * Get barcode level description
 */
export const getBarcodeLevel = (barcode) => {
  const parsed = parseBarcodeComponents(barcode);
  if (!parsed) {
    return null;
  }

  const levels = {
    2: "Device",
    3: "Shelf",
    4: "Rack",
    5: "Position",
  };

  return levels[parsed.level] || null;
};

/**
 * Check if barcode is minimum valid level (2 levels: room + device)
 */
export const isMinimumLevel = (barcode) => {
  const parsed = parseBarcodeComponents(barcode);
  return parsed && parsed.level >= 2;
};

/**
 * Build hierarchical path from barcode validation response
 */
export const buildHierarchicalPath = (validationResponse) => {
  if (!validationResponse || !validationResponse.valid) {
    return "";
  }

  const parts = [];

  if (validationResponse.room) {
    parts.push(validationResponse.room.name || validationResponse.room.code);
  }
  if (validationResponse.device) {
    parts.push(
      validationResponse.device.name || validationResponse.device.code,
    );
  }
  if (validationResponse.shelf) {
    parts.push(validationResponse.shelf.label || validationResponse.shelf.code);
  }
  if (validationResponse.rack) {
    parts.push(validationResponse.rack.label || validationResponse.rack.code);
  }
  if (validationResponse.position) {
    parts.push(
      validationResponse.position.coordinate ||
        validationResponse.position.code,
    );
  }

  return parts.join(" > ");
};

/**
 * Extract valid components from partial validation response
 * Used when validation fails but some components are valid
 */
export const extractValidComponents = (errorResponse) => {
  if (!errorResponse || !errorResponse.validComponents) {
    return {};
  }

  return errorResponse.validComponents;
};

export default {
  validateBarcodeFormat,
  parseBarcodeComponents,
  validateBarcode,
  getBarcodeLevel,
  isMinimumLevel,
  buildHierarchicalPath,
  extractValidComponents,
};
