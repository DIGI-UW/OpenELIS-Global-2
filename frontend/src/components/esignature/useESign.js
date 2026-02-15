import { useState, useCallback } from "react";
import { isEsigEnabled } from "./api";

/**
 * useESign - Custom hook for managing electronic signature flow.
 *
 * This hook handles the e-signature modal state and the "is e-sig enabled" check,
 * allowing the modal to be rendered at a higher level in the component tree
 * (outside of other modals) to prevent unmounting issues.
 *
 * Usage:
 * ```jsx
 * const {
 *   openSignatureModal,
 *   signatureModalProps,
 *   isCheckingEnabled,
 * } = useESign({
 *   meaning: SignatureMeaning.AUTHORED,
 *   context: "Sign 3 results as authored",
 *   recordType: "NOTEBOOK_PAGE_SAMPLE",
 *   recordId: pageData?.id,
 *   onSuccess: (signature) => handleSaveResults(signature),
 *   onCancel: () => setShowResultModal(true),
 * });
 *
 * // In parent modal - use a regular Button
 * <Button onClick={() => {
 *   setShowResultModal(false);  // Close parent modal first
 *   openSignatureModal();       // Then trigger e-sig flow
 * }}>
 *   Save Results
 * </Button>
 *
 * // Render modal at page level (outside other modals)
 * <ESignatureModal {...signatureModalProps} />
 * ```
 *
 * @param {Object} options - Hook options
 * @param {string} options.meaning - Signature meaning (AUTHORED, VALIDATED_AND_RELEASED, REJECTED)
 * @param {string} options.context - Description of what is being signed (displayed in modal)
 * @param {string} options.recordType - The type of record being signed
 * @param {number} options.recordId - The ID of the record being signed
 * @param {function} options.onSuccess - Called when signature is successfully executed
 * @param {function} [options.onCancel] - Called when user cancels the signature
 * @param {boolean} [options.skipEsigCheck=false] - Skip checking if e-sig is enabled
 *
 * @returns {Object} Hook return values
 * @returns {function} returns.openSignatureModal - Call to start the signing flow
 * @returns {Object} returns.signatureModalProps - Props to spread onto ESignatureModal
 * @returns {boolean} returns.isCheckingEnabled - True while checking if e-sig is enabled
 * @returns {boolean} returns.isModalOpen - Current modal open state
 */
const useESign = ({
  meaning,
  context,
  recordType,
  recordId,
  onSuccess,
  onCancel,
  skipEsigCheck = false,
}) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEsigEnabledState, setIsEsigEnabledState] = useState(null);
  const [isCheckingEnabled, setIsCheckingEnabled] = useState(false);

  /**
   * Opens the signature modal, checking if e-signatures are enabled first.
   * If e-signatures are disabled, calls onSuccess(null) directly.
   */
  const openSignatureModal = useCallback(async () => {
    // Check if e-signatures are enabled (only once, then cached)
    if (!skipEsigCheck && isEsigEnabledState === null) {
      setIsCheckingEnabled(true);
      try {
        const result = await isEsigEnabled();
        setIsEsigEnabledState(result.enabled);

        if (!result.enabled) {
          // E-signatures disabled - call onSuccess directly without modal
          if (onSuccess) {
            onSuccess(null);
          }
          setIsCheckingEnabled(false);
          return;
        }
      } catch (error) {
        // On error, assume e-signatures are enabled for safety
        console.error("Failed to check e-sig status:", error);
        setIsEsigEnabledState(true);
      } finally {
        setIsCheckingEnabled(false);
      }
    }

    // If we already know e-sig is disabled, proceed without modal
    if (isEsigEnabledState === false) {
      if (onSuccess) {
        onSuccess(null);
      }
      return;
    }

    // Open the signature modal
    setIsModalOpen(true);
  }, [skipEsigCheck, isEsigEnabledState, onSuccess]);

  /**
   * Handles modal close (cancel)
   */
  const handleModalClose = useCallback(() => {
    setIsModalOpen(false);
    if (onCancel) {
      onCancel();
    }
  }, [onCancel]);

  /**
   * Handles successful signature
   */
  const handleSignatureSuccess = useCallback(
    (signature) => {
      setIsModalOpen(false);
      if (onSuccess) {
        onSuccess(signature);
      }
    },
    [onSuccess],
  );

  // Props to spread onto ESignatureModal
  const signatureModalProps = {
    open: isModalOpen,
    onClose: handleModalClose,
    onSuccess: handleSignatureSuccess,
    meaning,
    context,
    recordType,
    recordId,
  };

  return {
    openSignatureModal,
    signatureModalProps,
    isCheckingEnabled,
    isModalOpen,
  };
};

export default useESign;
