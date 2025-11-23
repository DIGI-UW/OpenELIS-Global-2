import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import ShortCodeInput from "./ShortCodeInput";
import PrintLabelButton from "./PrintLabelButton";
import PrintHistoryDisplay from "./PrintHistoryDisplay";
import { postToOpenElisServer } from "../../utils/Utils";

/**
 * LabelManagementModal - Modal for managing storage location labels
 *
 * Features:
 * - Short code input with validation
 * - Print label button
 * - Print history display
 * - Warning dialog for short code changes
 *
 * Props:
 * - open: boolean - Whether modal is open
 * - location: object - Location entity { id, type, name, code, shortCode }
 * - onClose: function - Callback when modal closes
 * - onShortCodeUpdate: function - Callback when short code is updated
 */
const LabelManagementModal = ({
  open,
  location,
  onClose,
  onShortCodeUpdate,
}) => {
  const intl = useIntl();
  const [shortCode, setShortCode] = useState("");
  const [originalShortCode, setOriginalShortCode] = useState("");
  const [isValid, setIsValid] = useState(false);
  const [showWarningDialog, setShowWarningDialog] = useState(false);
  const [pendingShortCode, setPendingShortCode] = useState("");

  // Initialize short code from location prop
  useEffect(() => {
    if (location && location.shortCode) {
      setShortCode(location.shortCode);
      setOriginalShortCode(location.shortCode);
    } else {
      setShortCode("");
      setOriginalShortCode("");
    }
    setIsValid(false);
    setShowWarningDialog(false);
  }, [location, open]);

  /**
   * Handle short code input change
   */
  const handleShortCodeChange = (newValue, isValidValue) => {
    setShortCode(newValue);
    setIsValid(isValidValue);
  };

  /**
   * Handle save short code
   */
  const handleSave = () => {
    if (!isValid || !location) {
      return;
    }

    // Check if short code changed
    if (originalShortCode && shortCode !== originalShortCode) {
      // Show warning dialog
      setPendingShortCode(shortCode);
      setShowWarningDialog(true);
    } else {
      // No change or no original - save directly
      saveShortCode(shortCode);
    }
  };

  /**
   * Save short code to backend
   */
  const saveShortCode = (codeToSave) => {
    if (!location) {
      return;
    }

    const url = `/rest/storage/${location.type}/${location.id}/short-code`;
    const formData = { shortCode: codeToSave || null };

    postToOpenElisServer(
      url,
      formData,
      (response) => {
        if (response.error) {
          // Error from backend validation
          alert(
            intl.formatMessage(
              {
                id: "label.shortCode.error.save",
                defaultMessage: "Error saving short code: {error}",
              },
              { error: response.error },
            ),
          );
        } else {
          // Success
          if (onShortCodeUpdate) {
            onShortCodeUpdate({
              ...location,
              shortCode: response.shortCode || codeToSave,
            });
          }
          handleClose();
        }
      },
      (error) => {
        console.error("Error saving short code:", error);
        alert(
          intl.formatMessage({
            id: "label.shortCode.error.network",
            defaultMessage: "Error saving short code. Please try again.",
          }),
        );
      },
    );
  };

  /**
   * Handle warning dialog confirm
   */
  const handleWarningConfirm = () => {
    setShowWarningDialog(false);
    saveShortCode(pendingShortCode);
  };

  /**
   * Handle warning dialog cancel
   */
  const handleWarningCancel = () => {
    setShowWarningDialog(false);
    setPendingShortCode("");
    // Revert to original short code
    setShortCode(originalShortCode);
    setIsValid(!!originalShortCode);
  };

  /**
   * Handle modal close
   */
  const handleClose = () => {
    setShortCode(originalShortCode);
    setIsValid(!!originalShortCode);
    setShowWarningDialog(false);
    setPendingShortCode("");
    onClose();
  };

  if (!location) {
    return null;
  }

  return (
    <>
      <ComposedModal
        open={open}
        onClose={handleClose}
        data-testid="label-management-modal"
      >
        <ModalHeader
          title={intl.formatMessage({
            id: "label.management.title",
            defaultMessage: "Label Management",
          })}
          label={location.name || location.code}
        />
        <ModalBody>
          <Stack gap={6}>
            <ShortCodeInput
              value={shortCode}
              onChange={handleShortCodeChange}
              locationType={location.type}
              locationId={String(location.id)}
            />
            <div>
              <PrintLabelButton
                locationType={location.type}
                locationId={String(location.id)}
                shortCode={shortCode || null}
                disabled={!shortCode || shortCode.length === 0}
              />
            </div>
            <div>
              <h4 style={{ marginBottom: "0.5rem" }}>
                <FormattedMessage
                  id="label.printHistory"
                  defaultMessage="Print History"
                />
              </h4>
              <PrintHistoryDisplay
                locationType={location.type}
                locationId={String(location.id)}
              />
            </div>
          </Stack>
        </ModalBody>
        <ModalFooter>
          <Button
            kind="secondary"
            onClick={handleClose}
            data-testid="modal-close-button"
          >
            <FormattedMessage
              id="label.button.cancel"
              defaultMessage="Cancel"
            />
          </Button>
          <Button
            kind="primary"
            onClick={handleSave}
            disabled={!isValid || shortCode === originalShortCode}
            data-testid="save-short-code-button"
          >
            <FormattedMessage id="label.button.save" defaultMessage="Save" />
          </Button>
        </ModalFooter>
      </ComposedModal>

      {/* Warning Dialog for Short Code Change */}
      <ComposedModal
        open={showWarningDialog}
        onClose={handleWarningCancel}
        data-testid="short-code-warning-dialog"
      >
        <ModalHeader
          title={intl.formatMessage({
            id: "label.button.confirmTitle",
            defaultMessage: "Are You Sure?",
          })}
        />
        <ModalBody>
          <p>
            {intl.formatMessage({
              id: "label.shortCodeWarning",
              defaultMessage:
                "Changing short code will invalidate existing labels",
            })}
          </p>
        </ModalBody>
        <ModalFooter>
          <Button kind="secondary" onClick={handleWarningCancel}>
            <FormattedMessage
              id="label.button.cancel"
              defaultMessage="Cancel"
            />
          </Button>
          <Button kind="danger" onClick={handleWarningConfirm}>
            <FormattedMessage
              id="label.button.confirm"
              defaultMessage="Confirm"
            />
          </Button>
        </ModalFooter>
      </ComposedModal>
    </>
  );
};

LabelManagementModal.propTypes = {
  open: PropTypes.bool.isRequired,
  location: PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    type: PropTypes.oneOf(["device", "shelf", "rack"]).isRequired,
    name: PropTypes.string,
    code: PropTypes.string,
    shortCode: PropTypes.string,
  }),
  onClose: PropTypes.func.isRequired,
  onShortCodeUpdate: PropTypes.func,
};

LabelManagementModal.defaultProps = {
  location: null,
  onShortCodeUpdate: () => {},
};

export default LabelManagementModal;
