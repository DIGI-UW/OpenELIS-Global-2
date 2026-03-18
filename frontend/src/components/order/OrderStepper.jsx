import React from "react";
import { useHistory, useLocation } from "react-router-dom";
import { ProgressIndicator, ProgressStep } from "@carbon/react";
import { useIntl } from "react-intl";

/**
 * OrderStepper - Progress indicator for the 4-step sample collection workflow.
 *
 * Shows completed/in-progress/pending states for:
 * - Step 0: Enter Order
 * - Step 1: Collect Sample
 * - Step 2: Label & Store
 * - Step 3: QA Review
 */

const ORDER_STEPS = [
  { label: "order.step.enter", path: "/order/enter" },
  { label: "order.step.collect", path: "/order/collect" },
  { label: "order.step.label", path: "/order/label" },
  { label: "order.step.qa", path: "/order/qa" },
];

const OrderStepper = ({ currentStep, onStepClick, className = "" }) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();

  // Determine current step from URL if not provided
  const activeStep =
    currentStep !== undefined
      ? currentStep
      : ORDER_STEPS.findIndex((step) => location.pathname === step.path);

  const handleStepClick = (stepIndex) => {
    if (onStepClick) {
      onStepClick(stepIndex);
    } else {
      // Default behavior: navigate to the step's path
      history.push(ORDER_STEPS[stepIndex].path);
    }
  };

  return (
    <ProgressIndicator
      currentIndex={activeStep >= 0 ? activeStep : 0}
      className={`order-stepper ${className}`}
      spaceEqually={true}
      onChange={(stepIndex) => handleStepClick(stepIndex)}
    >
      {ORDER_STEPS.map((step, index) => (
        <ProgressStep
          key={step.path}
          complete={index < activeStep}
          current={index === activeStep}
          label={intl.formatMessage({ id: step.label })}
        />
      ))}
    </ProgressIndicator>
  );
};

export { ORDER_STEPS };
export default OrderStepper;
