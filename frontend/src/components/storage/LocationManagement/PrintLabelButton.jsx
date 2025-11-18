import React, { useState } from "react";
import { Button } from "@carbon/react";
import { useIntl } from "react-intl";
import { Printer } from "@carbon/icons-react";
import PropTypes from "prop-types";
import { postToOpenElisServer } from "../../utils/Utils";

/**
 * PrintLabelButton - Button to generate and print storage location labels
 *
 * Features:
 * - Calls POST /rest/storage/{type}/{id}/print-label?shortCode={code}
 * - Opens PDF in new tab
 * - Shows loading state during PDF generation
 *
 * Props:
 * - locationType: string - Location type ('device', 'shelf', 'rack')
 * - locationId: string - Location ID
 * - shortCode: string - Short code to print (optional)
 * - disabled: boolean - Disable button
 */
const PrintLabelButton = ({
  locationType,
  locationId,
  shortCode = null,
  disabled = false,
}) => {
  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(false);

  /**
   * Handle print label click
   */
  const handlePrint = () => {
    if (!locationType || !locationId) {
      return;
    }

    setIsLoading(true);
    const url = `/rest/storage/${locationType}/${locationId}/print-label`;
    const params = shortCode
      ? `?shortCode=${encodeURIComponent(shortCode)}`
      : "";
    const fullUrl = url + params;

    // Use postToOpenElisServer to get PDF blob
    // Note: postToOpenElisServer expects JSON, but we need to handle binary PDF
    // We'll use fetch directly for binary response
    fetch(fullUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.blob();
      })
      .then((blob) => {
        // Create object URL and open in new tab
        const blobUrl = window.URL.createObjectURL(blob);
        const newWindow = window.open(blobUrl, "_blank");
        if (!newWindow) {
          // Popup blocked - show error
          alert(
            intl.formatMessage({
              id: "label.print.error.popupBlocked",
              defaultMessage:
                "Popup blocked. Please allow popups for this site to print labels.",
            }),
          );
        }
        // Clean up blob URL after a delay (let browser load it first)
        setTimeout(() => {
          window.URL.revokeObjectURL(blobUrl);
        }, 1000);
        setIsLoading(false);
      })
      .catch((error) => {
        console.error("Error printing label:", error);
        alert(
          intl.formatMessage({
            id: "label.print.error",
            defaultMessage: "Error generating label. Please try again.",
          }),
        );
        setIsLoading(false);
      });
  };

  return (
    <Button
      kind="primary"
      onClick={handlePrint}
      disabled={disabled || isLoading}
      renderIcon={Printer}
      data-testid="print-label-button"
    >
      {isLoading
        ? intl.formatMessage({
            id: "label.print.generating",
            defaultMessage: "Generating...",
          })
        : intl.formatMessage({
            id: "label.print",
            defaultMessage: "Print Label",
          })}
    </Button>
  );
};

PrintLabelButton.propTypes = {
  locationType: PropTypes.oneOf(["device", "shelf", "rack"]).isRequired,
  locationId: PropTypes.string.isRequired,
  shortCode: PropTypes.string,
  disabled: PropTypes.bool,
};

PrintLabelButton.defaultProps = {
  shortCode: null,
  disabled: false,
};

export default PrintLabelButton;
