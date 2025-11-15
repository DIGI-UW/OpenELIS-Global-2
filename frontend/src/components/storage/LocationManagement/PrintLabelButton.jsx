import React, { useState, useEffect } from "react";
import { Button } from "@carbon/react";
import { Printer } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";
import PrintLabelConfirmationDialog from "./PrintLabelConfirmationDialog";

/**
 * PrintLabelButton - Component that triggers label printing
 * Shows confirmation dialog before printing
 * Can be used as a button or auto-triggered when location is provided
 *
 * Props:
 * - locationType: string - "device" | "shelf" | "rack"
 * - locationId: string - Location ID
 * - locationName: string - Location name for confirmation dialog
 * - locationCode: string - Location code for confirmation dialog
 * - onPrintSuccess: function - Optional callback when print succeeds
 * - onPrintError: function - Optional callback when print fails
 * - autoTrigger: boolean - If true, automatically shows dialog when component mounts (for overflow menu usage)
 */
const PrintLabelButton = ({
  locationType,
  locationId,
  locationName,
  locationCode,
  onPrintSuccess,
  onPrintError,
  autoTrigger = false,
}) => {
  const intl = useIntl();
  const [showConfirmation, setShowConfirmation] = useState(false);
  const [isPrinting, setIsPrinting] = useState(false);

  // Auto-trigger confirmation dialog when component mounts (for overflow menu usage)
  useEffect(() => {
    if (autoTrigger && locationId) {
      setShowConfirmation(true);
    }
  }, [autoTrigger, locationId]);

  const handlePrintClick = () => {
    setShowConfirmation(true);
  };

  const handleConfirmPrint = async () => {
    setIsPrinting(true);
    setShowConfirmation(false);

    try {
      const endpoint = `/rest/storage/${locationType}/${locationId}/print-label`;

      // Fetch PDF using POST request
      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
      });

      // Check if response is PDF or error JSON
      const contentType = response.headers.get("content-type");
      if (contentType && contentType.includes("application/pdf")) {
        // PDF response - create blob and download
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `label-${locationType}-${locationId}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

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

      <PrintLabelConfirmationDialog
        open={showConfirmation}
        locationName={locationName}
        locationCode={locationCode}
        onConfirm={handleConfirmPrint}
        onCancel={handleCancelPrint}
      />
    </>
  );
};

PrintLabelButton.propTypes = {
  locationType: PropTypes.oneOf(["device", "shelf", "rack"]).isRequired,
  locationId: PropTypes.string.isRequired,
  locationName: PropTypes.string,
  locationCode: PropTypes.string,
  onPrintSuccess: PropTypes.func,
  onPrintError: PropTypes.func,
  autoTrigger: PropTypes.bool,
};

PrintLabelButton.defaultProps = {
  locationName: "",
  locationCode: "",
  onPrintSuccess: () => {},
  onPrintError: () => {},
  autoTrigger: false,
};

export default PrintLabelButton;
