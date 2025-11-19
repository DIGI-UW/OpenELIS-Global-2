import React from "react";
import { useIntl } from "react-intl";
import { Checkmark, Error, Renew } from "@carbon/icons-react";
import PropTypes from "prop-types";

/**
 * BarcodeVisualFeedback - Visual feedback component for barcode validation states
 *
 * States:
 * - ready: Animation/pulse (ready to scan)
 * - success: Green checkmark (validation successful)
 * - error: Red X with message (validation failed)
 *
 * Props:
 * - state: Current validation state (ready, success, error)
 * - errorMessage: Error message to display (for error state)
 */
const BarcodeVisualFeedback = ({ state = "ready", errorMessage = "" }) => {
  const intl = useIntl();

  /**
   * Render ready state (animated pulse)
   */
  const renderReadyState = () => {
    return (
      <div
        className="barcode-feedback barcode-feedback--ready"
        data-state="ready"
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          width: "32px",
          height: "32px",
        }}
        title={intl.formatMessage({
          id: "barcode.ready",
          defaultMessage: "Ready to scan",
        })}
      >
        <Renew
          size={24}
          style={{
            color: "#0f62fe", // Carbon blue-60
            animation: "pulse 2s infinite",
          }}
        />
        <style>
          {`
            @keyframes pulse {
              0%, 100% {
                opacity: 1;
              }
              50% {
                opacity: 0.5;
              }
            }
          `}
        </style>
      </div>
    );
  };

  /**
   * Render success state (green checkmark)
   */
  const renderSuccessState = () => {
    return (
      <div
        className="barcode-feedback barcode-feedback--success"
        data-state="success"
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          width: "32px",
          height: "32px",
          backgroundColor: "#24a148", // Carbon green-50
          borderRadius: "50%",
        }}
        title={intl.formatMessage({
          id: "barcode.success",
          defaultMessage: "Location found",
        })}
      >
        <Checkmark
          size={20}
          style={{
            color: "#ffffff",
          }}
        />
      </div>
    );
  };

  /**
   * Render error state (red X with message)
   */
  const renderErrorState = () => {
    const displayMessage =
      errorMessage ||
      intl.formatMessage({
        id: "barcode.invalidFormat",
        defaultMessage: "Invalid barcode format",
      });

    return (
      <div
        className="barcode-feedback barcode-feedback--error"
        data-state="error"
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          width: "32px",
          height: "32px",
          backgroundColor: "#da1e28", // Carbon red-60
          borderRadius: "50%",
        }}
        title={displayMessage}
      >
        <Error
          size={20}
          style={{
            color: "#ffffff",
          }}
        />
      </div>
    );
  };

  /**
   * Render based on current state
   */
  switch (state) {
    case "success":
      return renderSuccessState();
    case "error":
      return renderErrorState();
    case "ready":
    default:
      return renderReadyState();
  }
};

BarcodeVisualFeedback.propTypes = {
  state: PropTypes.oneOf(["ready", "success", "error"]).isRequired,
  errorMessage: PropTypes.string,
};

BarcodeVisualFeedback.defaultProps = {
  state: "ready",
  errorMessage: "",
};

export default BarcodeVisualFeedback;
