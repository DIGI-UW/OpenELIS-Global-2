import React from "react";
import { useHistory, useLocation } from "react-router-dom";
import { ProgressIndicator, ProgressStep } from "@carbon/react";
import { useIntl } from "react-intl";
import { useOrderContext } from "./OrderContext";

/**
 * OrderStepper - Progress indicator for the 4-step sample collection workflow.
 *
 * Shows completed/in-progress/pending states for:
 * - Step 0: Enter Order
 * - Step 1: Collect Sample
 * - Step 2: Label & Store
 * - Step 3: QA Review
 *
 * Step completion is based on:
 * - Enter: order has labNumber
 * - Collect: samples have sampleItemId
 * - Label: all samples have storage assigned OR storageSkipped is true
 * - QA: order is finalized
 */

const ORDER_STEPS = [
  { label: "order.step.enter", path: "/order/enter", key: "enter" },
  { label: "order.step.collect", path: "/order/collect", key: "collect" },
  { label: "order.step.label", path: "/order/label", key: "label" },
  { label: "order.step.qa", path: "/order/qa", key: "qa" },
];

// Vector workflow skips the Collect step
const VECTOR_ORDER_STEPS = ORDER_STEPS.filter((s) => s.key !== "collect");

const OrderStepper = ({ currentStep, onStepClick, className = "" }) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { samples, storageSkipped, labNumber, stepProgress, orderData } =
    useOrderContext();

  const workflowType =
    orderData?.sampleOrderItems?.environmentalFields?.workflowType ||
    "clinical";
  const steps = workflowType === "vector" ? VECTOR_ORDER_STEPS : ORDER_STEPS;

  // Determine current step from URL if not provided
  const activeStep =
    currentStep !== undefined
      ? currentStep
      : steps.findIndex((step) => location.pathname === step.path);

  // Calculate step completion based on actual data state
  const isStepComplete = (stepKey) => {
    switch (stepKey) {
      case "enter":
        // Enter is complete if we have a lab number
        return !!labNumber;

      case "collect":
        // Collect is complete if all samples have sampleItemId
        return samples.length > 0 && samples.every((s) => s.sampleItemId);

      case "label": {
        const allHaveStorage =
          samples.length > 0 && samples.every((s) => s.storageLocationId);
        return allHaveStorage || storageSkipped || stepProgress?.label || false;
      }

      case "qa":
        // QA is complete based on stepProgress (set when order is finalized)
        return stepProgress?.qa || false;

      default:
        return false;
    }
  };

  const handleStepClick = (stepIndex) => {
    if (onStepClick) {
      onStepClick(stepIndex);
    } else {
      // Default behavior: navigate to the step's path
      history.push(steps[stepIndex].path);
    }
  };

  return (
    <ProgressIndicator
      currentIndex={activeStep >= 0 ? activeStep : 0}
      className={`order-stepper ${className}`}
      spaceEqually={true}
      onChange={(stepIndex) => handleStepClick(stepIndex)}
    >
      {steps.map((step) => (
        <ProgressStep
          key={step.path}
          complete={isStepComplete(step.key)}
          label={intl.formatMessage({ id: step.label })}
        />
      ))}
    </ProgressIndicator>
  );
};

export { ORDER_STEPS, VECTOR_ORDER_STEPS };
export default OrderStepper;
