import React, { useState, useContext } from "react";
import { Button } from "@carbon/react";
import { useIntl } from "react-intl";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import ESignatureModal from "./ESignatureModal";
import { SignatureMeaning, isEsigEnabled } from "./api";

/**
 * ESignatureButton - A reusable button component that triggers the electronic signature ceremony.
 *
 * This component abstracts the e-signature flow for use across different notebooks and pages.
 * It handles checking if e-signatures are enabled and manages the modal state internally.
 *
 * Usage:
 * ```jsx
 * <ESignatureButton
 *   meaning="AUTHORED"
 *   context="Sign 3 result(s) as authored"
 *   recordType="RESULT"
 *   recordId={123}
 *   onSign={(signature) => handleResultSigned(signature)}
 *   label="Save"
 * />
 * ```
 *
 * Props:
 * - meaning: string - The signature meaning (AUTHORED, VALIDATED_AND_RELEASED, REJECTED)
 * - context: string - Description of what is being signed (displayed in modal)
 * - recordType: string - The type of record being signed (e.g., "RESULT", "ANALYSIS")
 * - recordId: number - The ID of the record being signed
 * - onSign: function(signature) - Called when signature is successfully executed
 * - onCancel: function - Called when user cancels the signature (optional)
 * - onBeforeSign: function - Called before the signature modal opens (or before onSign fires when e-sig is disabled).
 *   Can return a Promise; if it rejects, the ceremony / sign callback is aborted. Use to run pre-signature
 *   actions like persisting an acknowledgment that gates the signature.
 * - label: string - Button label (optional, defaults to "Sign")
 * - kind: string - Carbon button kind (optional, defaults to "primary")
 * - size: string - Carbon button size (optional)
 * - disabled: boolean - Whether the button is disabled (optional)
 * - style: object - Custom styles for the button (optional)
 * - className: string - Custom CSS class (optional)
 * - children: node - Custom button content (optional, overrides label)
 * - skipEsigCheck: boolean - Skip checking if e-sig is enabled (optional, for testing)
 */
const ESignatureButton = ({
  meaning,
  context,
  recordType,
  recordId,
  onSign,
  onCancel,
  onBeforeSign,
  label,
  kind = "primary",
  size,
  disabled = false,
  style,
  className,
  children,
  skipEsigCheck = false,
}) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEsigEnabledState, setIsEsigEnabledState] = useState(null);
  const [isCheckingEsig, setIsCheckingEsig] = useState(false);

  // Default label based on meaning
  const getDefaultLabel = () => {
    switch (meaning) {
      case SignatureMeaning.AUTHORED:
        return intl.formatMessage({
          id: "esig.button.saveAndSign",
          defaultMessage: "Save",
        });
      case SignatureMeaning.VALIDATED_AND_RELEASED:
        return intl.formatMessage({
          id: "esig.button.validate",
          defaultMessage: "Validate",
        });
      case SignatureMeaning.REJECTED:
        return intl.formatMessage({
          id: "esig.button.reject",
          defaultMessage: "Reject",
        });
      default:
        return intl.formatMessage({
          id: "esig.button.sign",
          defaultMessage: "Sign",
        });
    }
  };

  const buttonLabel = label || getDefaultLabel();

  const handleClick = async () => {
    // Resolve whether e-signatures are enabled (cached on first call).
    let esigEnabled = isEsigEnabledState;
    if (!skipEsigCheck && esigEnabled === null) {
      setIsCheckingEsig(true);
      try {
        const result = await isEsigEnabled();
        esigEnabled = result.enabled;
        setIsEsigEnabledState(esigEnabled);
      } catch (error) {
        // On error, assume e-signatures are enabled for safety.
        esigEnabled = true;
        setIsEsigEnabledState(true);
      } finally {
        setIsCheckingEsig(false);
      }
    }

    // Run onBeforeSign for both the E-Sign-on and E-Sign-off paths so any
    // pre-signature persistence (e.g. a QC acknowledgment that gates release)
    // happens before either the modal opens or onSign is called directly.
    // If the promise rejects, abort the whole flow.
    if (onBeforeSign) {
      try {
        await onBeforeSign();
      } catch (error) {
        return;
      }
    }

    if (!esigEnabled) {
      if (onSign) {
        onSign(null);
      }
      return;
    }

    // Small delay to allow parent modal to close before opening e-sig modal,
    // preventing the visual "stacking" of modals.
    setTimeout(() => {
      setIsModalOpen(true);
    }, 50);
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    if (onCancel) {
      onCancel();
    }
  };

  const handleSignatureSuccess = (signature) => {
    setIsModalOpen(false);
    if (onSign) {
      onSign(signature);
    }
  };

  // Check if user is logged in
  const isLoggedIn = !!userSessionDetails?.userId;

  return (
    <>
      <Button
        kind={kind}
        size={size}
        disabled={disabled || !isLoggedIn || isCheckingEsig}
        onClick={handleClick}
        style={style}
        className={className}
      >
        {children || buttonLabel}
      </Button>

      <ESignatureModal
        open={isModalOpen}
        onClose={handleModalClose}
        onSuccess={handleSignatureSuccess}
        meaning={meaning}
        context={context}
        recordType={recordType}
        recordId={recordId}
      />
    </>
  );
};

// Export SignatureMeaning for convenience
export { SignatureMeaning };

export default ESignatureButton;
