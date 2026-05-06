import React from "react";
import { Button } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import { useOrderContext } from "./OrderContext";
import {
  CLINICAL_ORDER_STEPS,
  ENVIRONMENTAL_ORDER_STEPS,
  VECTOR_ORDER_STEPS,
} from "./OrderStepper";

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
  const location = useLocation();
  const { isSubmitting, isReadOnly, isEditMode, saveOrder } = useOrderContext();

  // Infer step set from URL prefix — no workflowType context read needed.
  const steps = (() => {
    const path = location.pathname;
    if (path.startsWith("/order/vector")) return VECTOR_ORDER_STEPS;
    if (path.startsWith("/order/environmental"))
      return ENVIRONMENTAL_ORDER_STEPS;
    return CLINICAL_ORDER_STEPS;
  })();

  const isLastStep = currentStep >= steps.length - 1;
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
      if (currentStep < steps.length - 1) {
        history.push(steps[currentStep + 1].path);
      }
    }
  };

  const handleBack = () => {
    if (!isFirstStep) {
      history.push(steps[currentStep - 1].path);
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
            onClick={() => history.push(steps[currentStep + 1].path)}
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
