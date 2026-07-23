import React from "react";
import { Stack, Button, Tag, InlineLoading } from "@carbon/react";
import { Edit } from "@carbon/icons-react";
import { useLocation } from "react-router-dom";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import OrderStepper, { ORDER_STEPS } from "./OrderStepper";
import OrderContextCard from "./OrderContextCard";
import BarcodeScannerBar from "./BarcodeScannerBar";
import SaveNavigationButtons from "./SaveNavigationButtons";
import { useOrderContext, SaveStatus } from "./OrderContext";
import "./order-workflow.scss";

/**
 * OrderWorkflowLayout - Shared layout wrapper for all 4 workflow steps.
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

const SaveStatusIndicator = () => {
  const intl = useIntl();
  const { saveStatus, isDirty } = useOrderContext();

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

const OrderWorkflowLayout = ({
  children,
  currentStep,
  title,
  canProceed = true,
  onSave,
  onSaveAndNext,
  extraButtons,
  showSaveButtons = true,
}) => {
  const intl = useIntl();
  const location = useLocation();
  const { isReadOnly, isEditMode, enableEditMode, labNumber, orderData } =
    useOrderContext();

  // Determine current step from URL if not provided
  const activeStep =
    currentStep !== undefined
      ? currentStep
      : ORDER_STEPS.findIndex((step) => location.pathname === step.path);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "sidenav.label.addorder", link: "/order" },
    {
      label: ORDER_STEPS[activeStep]?.label || "order.step.enter",
      link: ORDER_STEPS[activeStep]?.path || "/order/enter",
    },
  ];

  const handleOrderLoaded = () => {
    // Order loaded via barcode scan - context is already updated
  };

  const canEdit = isReadOnly && !isEditMode;

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
          />

          {/* Persistent Order Context Card */}
          {(labNumber || orderData?.sampleOrderItems?.labNo) && (
            <OrderContextCard className="order-context-section" />
          )}

          {/* Main Content Area */}
          <div
            className={`order-content-section ${isReadOnly && !isEditMode ? "readonly-mode" : ""}`}
          >
            {children}
          </div>

          {/* Save Navigation Buttons - NAV-4 */}
          {showSaveButtons && (
            <div className="order-navigation-section">
              <SaveNavigationButtons
                currentStep={activeStep}
                canProceed={canProceed}
                onSave={onSave}
                onSaveAndNext={onSaveAndNext}
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
