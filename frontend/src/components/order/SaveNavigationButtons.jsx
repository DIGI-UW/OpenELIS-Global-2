import React from "react";
import { Button } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { useHistory } from "react-router-dom";
import { useOrderContext } from "./OrderContext";
import { ORDER_STEPS } from "./OrderStepper";

/**
 * SaveNavigationButtons - Dual-button component for Save & Next vs Save.
 *
 * Features:
 * - "Save" button: saves progress and stays on current step
 * - "Save & Next" button: saves and auto-advances to next step
 * - Back button: navigates to previous step
 * - Disabled during submission or when validation fails
 */

const SaveNavigationButtons = ({
  currentStep,
  onSave,
  onSaveAndNext,
  canProceed = true,
  showBack = true,
  className = "",
}) => {
  const history = useHistory();
  const { isSubmitting, isReadOnly, isEditMode, saveOrder } = useOrderContext();

  const isLastStep = currentStep >= ORDER_STEPS.length - 1;
  const isFirstStep = currentStep <= 0;

  const handleSave = async () => {
    if (onSave) {
      await onSave();
    } else {
      await saveOrder();
    }
  };

  const handleSaveAndNext = async () => {
    if (onSaveAndNext) {
      await onSaveAndNext();
    } else {
      await saveOrder();
      if (currentStep < ORDER_STEPS.length - 1) {
        history.push(ORDER_STEPS[currentStep + 1].path);
      }
    }
  };

  const handleBack = () => {
    if (!isFirstStep) {
      history.push(ORDER_STEPS[currentStep - 1].path);
    }
  };

  // Don't show save buttons in read-only mode (unless edit mode is enabled)
  if (isReadOnly && !isEditMode) {
    return (
      <div className={`save-navigation-buttons ${className}`}>
        {showBack && !isFirstStep && (
          <Button kind="tertiary" onClick={handleBack}>
            <FormattedMessage id="back.action.button" />
          </Button>
        )}
        {!isLastStep && (
          <Button
            kind="primary"
            className="forward-button"
            onClick={() => history.push(ORDER_STEPS[currentStep + 1].path)}
          >
            <FormattedMessage id="next.action.button" />
          </Button>
        )}
      </div>
    );
  }

  return (
    <div className={`save-navigation-buttons ${className}`}>
      {showBack && !isFirstStep && (
        <Button kind="tertiary" onClick={handleBack} disabled={isSubmitting}>
          <FormattedMessage id="back.action.button" />
        </Button>
      )}

      <div className="save-buttons-group">
        <Button kind="secondary" onClick={handleSave} disabled={isSubmitting}>
          <FormattedMessage id="button.save.stay" />
        </Button>

        {!isLastStep && (
          <Button
            kind="primary"
            className="forward-button"
            onClick={handleSaveAndNext}
            disabled={isSubmitting || !canProceed}
          >
            <FormattedMessage id="button.save.next" />
          </Button>
        )}

        {isLastStep && (
          <Button
            kind="primary"
            className="forward-button"
            onClick={handleSave}
            disabled={isSubmitting || !canProceed}
          >
            <FormattedMessage id="label.button.submit" />
          </Button>
        )}
      </div>
    </div>
  );
};

export default SaveNavigationButtons;
