import React from "react";
import {
  Stack,
  Button,
  Tag,
  InlineLoading,
  InlineNotification,
} from "@carbon/react";
import { Edit } from "@carbon/icons-react";
import { useHistory, useLocation } from "react-router-dom";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import OrderStepper, { stepsForPath } from "./OrderStepper";
import OrderContextCard from "./OrderContextCard";
import RangeNotAppliedWarning from "./RangeNotAppliedWarning";
import BarcodeScannerBar from "./BarcodeScannerBar";
import SaveNavigationButtons from "./SaveNavigationButtons";
import ToContinueChecklist from "./ToContinueChecklist";
import { useOrderContext, SaveStatus } from "./OrderContext";
import "./order-workflow.scss";

/**
 * OrderWorkflowLayout - Shared layout wrapper for all order workflow steps.
 *
 * Provides consistent layout with:
 * - Breadcrumb navigation
 * - Barcode scanner bar (NAV-6)
 * - Progress stepper (NAV-3)
 * - Persistent order context card (Lab Number, Patient, Tests, Status)
 * - Save status indicator (Saved, Saving..., Unsaved changes)
 * - Edit mode toggle for read-only orders
 * - Main content area
 * - Save navigation buttons (NAV-4)
 */

/**
 * The save state of an order that exists. An order the user has not saved yet
 * has no save state to report: saying "Saved" on an untouched new form claims
 * something that never happened, and saying "Unsaved changes" invents changes
 * the user has not made. Both were reported as defects; the indicator is
 * simply absent until there is a saved order or an edit to describe.
 */
const SaveStatusIndicator = () => {
  const intl = useIntl();
  const { saveStatus, isDirty, labNumber, orderId } = useOrderContext();

  if (!orderId && !labNumber && !isDirty && saveStatus !== SaveStatus.SAVING) {
    return null;
  }

  if (saveStatus === SaveStatus.SAVING) {
    return (
      <InlineLoading
        status="active"
        description={intl.formatMessage({
          id: "order.saveStatus.saving",
          defaultMessage: "Saving...",
        })}
        className="save-status-indicator"
      />
    );
  }

  if (saveStatus === SaveStatus.ERROR) {
    return (
      <Tag type="red" size="sm" className="save-status-indicator">
        <FormattedMessage
          id="order.saveStatus.error"
          defaultMessage="Save failed"
        />
      </Tag>
    );
  }

  if (isDirty || saveStatus === SaveStatus.UNSAVED) {
    return (
      <Tag type="gray" size="sm" className="save-status-indicator">
        <FormattedMessage
          id="order.saveStatus.unsaved"
          defaultMessage="Unsaved changes"
        />
      </Tag>
    );
  }

  return (
    <Tag type="green" size="sm" className="save-status-indicator">
      <FormattedMessage id="order.saveStatus.saved" defaultMessage="Saved" />
    </Tag>
  );
};

/**
 * After the entry step saves, the screen states plainly that the order exists
 * and what comes next, instead of a passing toast.
 */
const SavedNextAction = ({ steps, activeStep }) => {
  const intl = useIntl();
  const history = useHistory();
  const { saveStatus, isDirty, labNumber } = useOrderContext();
  const next = steps[activeStep + 1];
  if (
    activeStep !== 0 ||
    isDirty ||
    saveStatus !== SaveStatus.SAVED ||
    !labNumber ||
    !next
  ) {
    return null;
  }
  const nextLabel = intl.formatMessage({ id: next.label });
  return (
    <div className="order-saved-next-action">
      <InlineNotification
        kind="success"
        lowContrast
        hideCloseButton
        title={intl.formatMessage(
          {
            id: "order.saved.title",
            defaultMessage: "Order {labNumber} saved",
          },
          { labNumber },
        )}
        subtitle={intl.formatMessage(
          { id: "order.saved.next", defaultMessage: "Next: {step}" },
          { step: nextLabel },
        )}
      />
      <Button kind="tertiary" size="sm" onClick={() => history.push(next.path)}>
        {nextLabel}
      </Button>
    </div>
  );
};

const OrderWorkflowLayout = ({
  children,
  currentStep,
  title,
  canProceed = true,
  canSave = true,
  onSave,
  onSaveAndNext,
  onDiscard,
  toContinue = [],
  primaryLabelId,
  secondaryAction,
  extraButtons,
  showSaveButtons = true,
}) => {
  const intl = useIntl();
  const location = useLocation();
  const {
    isReadOnly,
    isEditMode,
    enableEditMode,
    labNumber,
    orderData,
    rangeNotApplied,
    sampleCheckEnabled,
    progress,
  } = useOrderContext();
  const currentLabNumber = labNumber || orderData?.sampleOrderItems?.labNo;

  // The steps this workflow shows; the clinical Sample check exists only while
  // the laboratory's acceptance setting is not Off (FR-F1).
  const steps = stepsForPath(location.pathname, sampleCheckEnabled);

  // Determine current step from URL if not provided
  const activeStep =
    currentStep !== undefined
      ? currentStep
      : steps.findIndex((step) => location.pathname === step.path);

  const workflowRoot = (() => {
    const path = location.pathname;
    if (path.startsWith("/order/vector")) return "/order/vector";
    if (path.startsWith("/order/environmental")) return "/order/environmental";
    return "/order/clinical";
  })();

  const workflowLabel = {
    "/order/vector": "sidenav.label.vector.order",
    "/order/environmental": "sidenav.label.environmental.order",
    "/order/clinical": "sidenav.label.clinical.order",
  }[workflowRoot];

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: workflowLabel, link: workflowRoot },
    {
      label: steps[activeStep]?.label || "order.step.enter",
      link: steps[activeStep]?.path || `${workflowRoot}/enter`,
    },
  ];

  const handleOrderLoaded = () => {
    // Order loaded via barcode scan - context is already updated
  };

  const canEdit = isReadOnly && !isEditMode;
  const nextStep = steps[activeStep + 1];
  const nextStepLabel = intl.formatMessage({
    id: nextStep ? nextStep.label : "order.status.complete",
  });
  const toContinueCount = toContinue.length;
  const currentStepNote =
    progress?.status === "CANCELLED"
      ? undefined
      : toContinueCount > 0
        ? intl.formatMessage(
            { id: "order.step.toDo" },
            { count: toContinueCount },
          )
        : intl.formatMessage({ id: "order.step.ready" });

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Stack gap={5}>
        <div className="order-workflow-container">
          {/* Header with title, save status, and edit button */}
          <div className="workflow-header">
            <div className="workflow-title-section">
              {title && (
                <h2 className="order-step-title">
                  {typeof title === "string" ? (
                    <FormattedMessage id={title} />
                  ) : (
                    title
                  )}
                </h2>
              )}
              <SaveStatusIndicator />
            </div>
            <div className="workflow-actions-section">
              {canEdit && (
                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={Edit}
                  onClick={enableEditMode}
                >
                  <FormattedMessage id="button.edit" defaultMessage="Edit" />
                </Button>
              )}
            </div>
          </div>

          {/* Read-only indicator banner */}
          {isReadOnly && !isEditMode && (
            <div className="readonly-banner">
              <FormattedMessage
                id="order.readonly.message"
                defaultMessage="This order is in read-only mode. Click Edit to make changes."
              />
            </div>
          )}

          {/* Barcode Scanner Bar - NAV-6 */}
          <BarcodeScannerBar
            onOrderLoaded={handleOrderLoaded}
            className="order-barcode-section"
          />

          {/* Progress Stepper - NAV-3 */}
          <OrderStepper
            currentStep={activeStep}
            className="order-stepper-section"
            currentStepNote={showSaveButtons ? currentStepNote : undefined}
          />

          {progress?.status === "CANCELLED" && (
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "order.status.cancelled" })}
              subtitle={intl.formatMessage(
                { id: "order.cancelled.notice" },
                { reason: progress.cancelReason || "" },
              )}
            />
          )}

          {/* Persistent Order Context Card */}
          {(labNumber || orderData?.sampleOrderItems?.labNo) && (
            <OrderContextCard className="order-context-section" />
          )}

          {rangeNotApplied?.labNumber &&
            rangeNotApplied.labNumber === currentLabNumber && (
              <RangeNotAppliedWarning
                tests={rangeNotApplied.tests}
                labNumber={currentLabNumber}
              />
            )}

          <SavedNextAction steps={steps} activeStep={activeStep} />

          {/* Main Content Area */}
          <div
            className={`order-content-section ${isReadOnly && !isEditMode ? "readonly-mode" : ""}`}
          >
            {children}
          </div>

          {/* To continue checklist (FR-A9) and the footer (FR-A1) */}
          {showSaveButtons && (
            <div className="order-navigation-section">
              {!(isReadOnly && !isEditMode) && (
                <ToContinueChecklist
                  nextStep={nextStepLabel}
                  items={toContinue}
                />
              )}
              <SaveNavigationButtons
                currentStep={activeStep}
                canProceed={canProceed}
                canSave={canSave}
                onSave={onSave}
                onSaveAndNext={onSaveAndNext}
                onDiscard={onDiscard}
                toContinueCount={toContinueCount}
                primaryLabelId={primaryLabelId}
                secondaryAction={secondaryAction}
              />
              {extraButtons && (
                <div className="order-extra-buttons">{extraButtons}</div>
              )}
            </div>
          )}
        </div>
      </Stack>
    </>
  );
};

export default OrderWorkflowLayout;
