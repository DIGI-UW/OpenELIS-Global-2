/**
 * Site branding utilities and API functions.
 */

import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServerFullResponse,
  deleteFromOpenElisServerFullResponse,
} from "./Utils";
import config from "../../config.json";

export interface BrandingConfiguration {
  headerColor?: string;
  primaryColor?: string;
  secondaryColor?: string;
  faviconUrl?: string;
  [key: string]: unknown;
}

interface BrandingApiError {
  errors?: Record<string, unknown>;
  error?: string;
  message?: string;
  globalErrors?: string[];
}

type BrandingCallback = (branding: BrandingConfiguration | undefined) => void;

// =============================================================================
// API Functions
// =============================================================================

/**
 * Get current branding configuration
 * @param {Function} callback - Callback function to handle response
 */
export const getBranding = (callback: BrandingCallback): void => {
  getFromOpenElisServer<BrandingConfiguration>("/rest/site-branding", callback);
};

/**
 * Update branding configuration
 * @param {Object} formData - Branding configuration data
 * @param {Function} callback - Callback function to handle response (receives status, errorMessage, responseData)
 * @param {Object} extraParams - Additional parameters to pass to callback
 */
export const updateBranding = <TExtra = unknown>(
  formData: BrandingConfiguration,
  callback: (
    status: number,
    errorMessage: string | null,
    responseData: unknown,
    extraParams: TExtra | undefined,
  ) => void,
  extraParams?: TExtra,
): void => {
  const payload = JSON.stringify(formData);
  putToOpenElisServerFullResponse(
    "/rest/site-branding",
    payload,
    async (response, callbackExtraParams) => {
      if (!response) {
        callback(0, "Network error", null, callbackExtraParams);
        return;
      }
      const status = response.status;
      let errorMessage = null;
      let responseData: unknown = null;

      if (status === 200 || status === 201) {
        try {
          responseData = await response.json();
        } catch {
          // Response might be empty, that's okay
        }
      } else {
        try {
          const errorData = (await response.json()) as BrandingApiError;
          console.error("Backend error response:", errorData);
          // Handle validation errors (from @Valid)
          if (errorData.errors && typeof errorData.errors === "object") {
            const validationErrors = Object.entries(errorData.errors)
              .map(([field, message]) => `${field}: ${String(message)}`)
              .join("; ");
            errorMessage = validationErrors || "Validation error";
          } else {
            // Handle other error formats
            errorMessage =
              errorData.error ||
              errorData.message ||
              errorData.globalErrors?.join("; ") ||
              "Unknown error";
          }
        } catch (e) {
          console.error("Error parsing error response:", e);
          errorMessage = `Error ${status}: ${response.statusText}`;
        }
      }

      callback(status, errorMessage, responseData, callbackExtraParams);
    },
    extraParams,
  );
};

/**
 * Remove logo file
 * @param {String} type - Logo type (header, login, favicon)
 * @param {Function} callback - Callback function to handle response
 * @param {Object} extraParams - Additional parameters to pass to callback
 */
export const removeLogo = <TExtra = unknown>(
  type: string,
  callback: (response: Response | undefined, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  deleteFromOpenElisServerFullResponse(
    `/rest/site-branding/logo/${type}`,
    callback,
    extraParams,
  );
};

/**
 * Reset all branding to default values
 * @param {Function} callback - Callback function to handle response
 * @param {Object} extraParams - Additional parameters to pass to callback
 */
export const resetBranding = <TExtra = unknown>(
  callback: (status: number, extraParams?: TExtra) => void,
  extraParams?: TExtra,
): void => {
  const payload = JSON.stringify({});
  postToOpenElisServer(
    "/rest/site-branding/reset",
    payload,
    callback,
    extraParams,
  );
};

// =============================================================================
// DOM Utility Functions
// =============================================================================

/**
 * Apply branding colors to the document root element.
 * Sets CSS custom properties that Carbon components will use.
 * @param {Object} branding - Branding configuration object
 */
export const applyBrandingColors = (
  branding: BrandingConfiguration | null | undefined,
): void => {
  if (!branding) return;

  const root = document.documentElement;

  if (branding.headerColor) {
    root.style.setProperty("--site-branding-header", branding.headerColor);
  }
  if (branding.primaryColor) {
    root.style.setProperty("--cds-interactive-01", branding.primaryColor);
  }
  if (branding.secondaryColor) {
    root.style.setProperty("--cds-interactive-02", branding.secondaryColor);
  }
};

/**
 * Update the document favicon.
 * @param {String} faviconUrl - URL path to the favicon
 */
export const applyFavicon = (faviconUrl: string | null | undefined): void => {
  if (!faviconUrl) return;

  // Remove existing favicon links
  const existingLinks = document.querySelectorAll('link[rel*="icon"]');
  existingLinks.forEach((link) => link.remove());

  // Add new favicon link
  const link = document.createElement("link");
  link.rel = "icon";
  link.type = "image/x-icon";
  link.href = `${config.serverBaseUrl}${faviconUrl}`;
  document.head.appendChild(link);
};

/**
 * Fetch branding configuration and apply it to the DOM.
 * Applies colors and favicon.
 * @param {Function} callback - Optional callback after branding is applied
 */
export const loadAndApplyBranding = (callback?: BrandingCallback): void => {
  getBranding((response) => {
    if (response) {
      applyBrandingColors(response);
      if (response.faviconUrl) {
        applyFavicon(response.faviconUrl);
      }
    }
    if (callback) {
      callback(response);
    }
  });
};
