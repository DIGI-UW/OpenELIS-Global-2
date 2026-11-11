import React, { useState } from "react";
import { Button, Modal } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import { useOrderContext } from "./OrderContext";
import { stepsForPath } from "./OrderStepper";

/**
 * SaveNavigationButtons - The footer of every order step.
 *
 * Clinical (OGC-1266 FR-A1 to FR-A3, FR-A10): Discard throws the step's
 * unsaved changes away after a confirmation that names what is lost; on an
 * order that was never saved it discards the whole order. Save and exit saves
 * and returns to the dashboard. Save and next (Save and finish on the last
 * step) saves and opens the next step; while the step is not complete it stays
 * visible but disabled, with the count of items still needed beside it.
 *
 * Environmental and vector keep their footer: Save stays on the step, Save &
 * Next advances, Submit ends the workflow.
 */

const SaveNavigationButtons = ({
  currentStep,
  onSave,
  onSaveAndNext,
  onDiscard,
  canProceed = true,
  canSave = true,
  toContinueCount = 0,
  primaryLabelId,
  secondaryAction,
  showBack = true,
  className = "",
}) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const {
    isSubmitting,
    isReadOnly,
    isEditMode,
    saveOrder,
    labNumber,
    orderId,
    orderData,
    samples = [],
    sampleCheckEnabled,
    resetOrder,
    loadOrder,
  } = useOrderContext();
  const currentLabNumber = labNumber || orderData?.sampleOrderItems?.labNo;
  const [discardOpen, setDiscardOpen] = useState(false);

  const steps = stepsForPath(location.pathname, sampleCheckEnabled);
  const workflowRoot = steps[0].path.replace(/\/enter$/, "");
  const clinical = workflowRoot === "/order/clinical";

  const isLastStep = currentStep >= steps.length - 1;
  const isFirstStep = currentStep <= 0;

  // Always carry ?order= when an order is loaded, regardless of how we arrived.
  // Derived from context labNumber so it survives regardless of URL history.
  const navigateTo = (path) => {
    history.push(
      labNumber ? `${path}?order=${encodeURIComponent(labNumber)}` : path,
    );
  };

  // A step's save handler reports its failures itself and answers false; a
  // handler that answers nothing is taken as a success.
  const handleSave = async () => {
    if (onSave) {
      return (await onSave()) !== false;
    }
    await saveOrder();
    return true;
  };

  const handleSaveAndExit = async () => {
    if (!(await handleSave())) {
      return;
    }
    history.push(
      currentLabNumber
        ? `${workflowRoot}?highlight=${encodeURIComponent(currentLabNumber)}`
        : workflowRoot,
    );
  };

  const handleSaveAndNext = async () => {
    if (onSaveAndNext) {
      await onSaveAndNext();
      return;
    }
    if (!(await handleSave())) {
      return;
    }
    if (currentStep < steps.length - 1) {
      navigateTo(steps[currentStep + 1].path);
    }
  };

  const handleBack = () => {
    if (!isFirstStep) {
      navigateTo(steps[currentStep - 1].path);
    }
  };

  const everSaved = Boolean(orderId);
  const testCount = samples.reduce(
    (count, sample) => count + (sample.tests?.length || 0),
    0,
  );
  const sampleCount = samples.filter((sample) => sample.sampleTypeId).length;

  const confirmDiscard = async () => {
    setDiscardOpen(false);
    if (onDiscard) {
      await onDiscard();
      return;
    }
    if (everSaved && labNumber) {
      await loadOrder(labNumber, false);
      return;
    }
    resetOrder();
    history.push(workflowRoot);
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
            onClick={() => navigateTo(steps[currentStep + 1].path)}
          >
            <FormattedMessage id="next.action.button" />
          </Button>
        )}
      </div>
    );
  }

  if (!clinical) {
    return (
      <div className={`save-navigation-buttons ${className}`}>
        {showBack && !isFirstStep && (
          <Button kind="tertiary" onClick={handleBack} disabled={isSubmitting}>
            <FormattedMessage id="back.action.button" />
          </Button>
        )}

        <div className="save-buttons-group">
          <Button
            kind="secondary"
            onClick={handleSave}
            disabled={isSubmitting || !canSave}
          >
            <FormattedMessage id="button.save.stay" />
          </Button>

          <Button
            kind="primary"
            className="forward-button"
            onClick={
              isLastStep && !onSaveAndNext ? handleSave : handleSaveAndNext
            }
            disabled={isSubmitting || !canProceed || !canSave}
          >
            <FormattedMessage
              id={isLastStep ? "label.button.submit" : "button.save.next"}
            />
          </Button>
        </div>
      </div>
    );
  }

  const nextLabelId =
    primaryLabelId ||
    (isLastStep ? "order.nav.saveFinish" : "order.nav.saveNext");
  const blockedNext = !canProceed || !canSave;
  const countText =
    toContinueCount === 1
      ? intl.formatMessage({ id: "order.continue.countOne" })
      : intl.formatMessage(
          { id: "order.continue.count" },
          { count: toContinueCount },
        );

  return (
    <div className={`save-navigation-buttons ${className}`}>
      {showBack && !isFirstStep && (
        <Button kind="tertiary" onClick={handleBack} disabled={isSubmitting}>
          <FormattedMessage id="back.action.button" />
        </Button>
      )}

      <div className="save-buttons-group">
        <Button
          kind="danger--ghost"
          onClick={() => setDiscardOpen(true)}
          disabled={isSubmitting}
        >
          <FormattedMessage id="common.discard" />
        </Button>

        {secondaryAction}

        <Button
          kind="secondary"
          onClick={handleSaveAndExit}
          disabled={isSubmitting || !canSave}
        >
          <FormattedMessage id="order.nav.saveExit" />
        </Button>

        <span className="save-next-group">
          <Button
            kind="primary"
            className="forward-button"
            onClick={handleSaveAndNext}
            disabled={isSubmitting || blockedNext}
            aria-describedby={
              blockedNext && toContinueCount > 0 ? "footer-count" : undefined
            }
          >
            <FormattedMessage id={nextLabelId} />
          </Button>
          {blockedNext && toContinueCount > 0 && (
            <span id="footer-count" className="footer-count">
              {countText}
            </span>
          )}
        </span>
      </div>

      <Modal
        open={discardOpen}
        danger
        size="sm"
        modalHeading={intl.formatMessage({ id: "order.nav.discard.title" })}
        primaryButtonText={intl.formatMessage({
          id: everSaved
            ? "order.nav.discard.changes"
            : "order.nav.discard.order",
        })}
        secondaryButtonText={intl.formatMessage({ id: "common.stay" })}
        onRequestClose={() => setDiscardOpen(false)}
        onRequestSubmit={confirmDiscard}
      >
        <p>
          {intl.formatMessage(
            { id: "order.nav.discard.body" },
            { tests: testCount, samples: sampleCount },
          )}
        </p>
      </Modal>
    </div>
  );
};

export default SaveNavigationButtons;
