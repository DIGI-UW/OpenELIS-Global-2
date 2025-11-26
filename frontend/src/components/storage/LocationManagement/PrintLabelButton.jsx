import React, { useState, useEffect } from "react";
import { Button } from "@carbon/react";
import { Printer } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";
import PrintLabelConfirmationDialog from "./PrintLabelConfirmationDialog";
import config from "../../../config.json";

/**
 * PrintLabelButton - Component that triggers label printing
 *
 * NOTE: This component still contains PrintLabelConfirmationDialog internally for backward compatibility.
 * New code should use the callback pattern: pass onPrintClick callback to parent, which manages dialog at parent level.
 *
 * Props:
 * - locationType: string - "device" | "shelf" | "rack"
 * - locationId: string - Location ID
 * - locationName: string - Location name for confirmation dialog
 * - locationCode: string - Location code for confirmation dialog
 * - onPrintClick: function - Optional callback when button clicked (preferred pattern - parent manages dialog)
 * - onPrintSuccess: function - Optional callback when print succeeds (legacy - used with internal dialog)
 * - onPrintError: function - Optional callback when print fails (legacy - used with internal dialog)
 * - autoTrigger: boolean - If true, automatically shows dialog when component mounts (legacy - for overflow menu usage)
 */
const PrintLabelButton = ({
  locationType,
  locationId,
  locationName,
  locationCode,
  onPrintClick,
  onPrintSuccess,
  onPrintError,
  autoTrigger = false,
}) => {
  const intl = useIntl();
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [isPrinting, setIsPrinting] = useState(false);

  // Auto-trigger confirmation dialog when component mounts (for overflow menu usage - legacy)
  useEffect(() => {
    if (autoTrigger && locationId) {
      setShowConfirmation(true);
    }
  }, [autoTrigger, locationId]);

  const handlePrintClick = () => {
    // If onPrintClick callback provided, use new pattern (parent manages dialog)
    if (onPrintClick) {
      onPrintClick({
        type: locationType,
        id: locationId,
        name: locationName,
        label: locationName,
        code: locationCode,
      });
      return;
    }
    // Otherwise, use legacy pattern (internal dialog)
    setShowConfirmation(true);
  };

  const handleConfirmPrint = async () => {
    setIsPrinting(true);
    setShowConfirmation(false);

    try {
      const endpoint = `${config.serverBaseUrl}/rest/storage/${locationType}/${locationId}/print-label`;

      // Fetch PDF using POST request
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        credentials: "include",
      });

      // Check if response is PDF or error JSON
      const contentType = response.headers.get("content-type");
      if (contentType && contentType.includes("application/pdf")) {
        // PDF response - create blob and open in new tab
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        // Open in new tab instead of downloading
        window.open(url, "_blank");
        // Delay revoking URL to allow the new tab to load the PDF
        setTimeout(() => window.URL.revokeObjectURL(url), 60000);

        setIsPrinting(false);
        if (onPrintSuccess) {
          onPrintSuccess();
        }
      } else {
        // Error response - parse JSON error
        const errorData = await response.json();
        const errorMessage =
          errorData.error ||
          intl.formatMessage({
            id: "label.print.error",
            defaultMessage: "Failed to print label",
          });
        setIsPrinting(false);
        if (onPrintError) {
          onPrintError(new Error(errorMessage));
        }
      }
    } catch (error) {
      setIsPrinting(false);
      console.error("Error printing label:", error);
      if (onPrintError) {
        onPrintError(error);
      }
    }
  };

  const handleCancelPrint = () => {
    setShowConfirmation(false);
    // If auto-triggered, call onPrintError with null to signal cancellation
    if (autoTrigger && onPrintError) {
      onPrintError(null);
    }
  };

  return (
    <>
      {/* Only render button if not auto-triggering */}
      {!autoTrigger && (
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Printer}
          onClick={handlePrintClick}
          disabled={isPrinting}
          data-testid="print-label-button"
        >
          {intl.formatMessage({
            id: "label.printLabel",
            defaultMessage: "Print Label",
          })}
        </Button>
      )}

      {/* Legacy: Internal dialog (only used when onPrintClick not provided) */}
      {!onPrintClick && (
        <PrintLabelConfirmationDialog
          open={showConfirmation}
          locationName={locationName}
          locationCode={locationCode}
          onConfirm={handleConfirmPrint}
          onCancel={handleCancelPrint}
        />
      )}
    </>
  );
};

PrintLabelButton.propTypes = {
  locationType: PropTypes.oneOf(["device", "shelf", "rack"]).isRequired,
  locationId: PropTypes.string.isRequired,
  locationName: PropTypes.string,
  locationCode: PropTypes.string,
  onPrintClick: PropTypes.func, // New pattern: parent manages dialog
  onPrintSuccess: PropTypes.func, // Legacy: used with internal dialog
  onPrintError: PropTypes.func, // Legacy: used with internal dialog
  autoTrigger: PropTypes.bool, // Legacy: auto-trigger internal dialog
};

PrintLabelButton.defaultProps = {
  locationName: "",
  locationCode: "",
  onPrintSuccess: () => {},
  onPrintError: () => {},
  autoTrigger: false,
};

export default PrintLabelButton;
