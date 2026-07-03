import React from "react";
import { useHistory, useLocation } from "react-router-dom";
import { ProgressIndicator, ProgressStep } from "@carbon/react";
import { useIntl } from "react-intl";
import { useOrderContext } from "./OrderContext";

/**
 * OrderStepper - Progress indicator for the order workflow.
 *
 * Clinical: Enter → Collect → Label → QA (4 steps)
 * Environmental: Enter → Label → QA (3 steps)
 * Vector: Enter → Label → QA (3 steps)
 *
 * Step completion is based on:
 * - Enter: order has labNumber
 * - Collect: samples have sampleItemId
 * - Label: all samples have storage assigned OR storageSkipped is true
 * - QA: order is finalized
 */

const CLINICAL_ORDER_STEPS = [
  { label: "order.step.enter", path: "/order/clinical/enter", key: "enter" },
  {
    label: "order.step.collect",
    path: "/order/clinical/collect",
    key: "collect",
  },
  { label: "order.step.label", path: "/order/clinical/label", key: "label" },
  { label: "order.step.qa", path: "/order/clinical/qa", key: "qa" },
];

const ENVIRONMENTAL_ORDER_STEPS = [
  {
    label: "order.step.enter",
    path: "/order/environmental/enter",
    key: "enter",
  },
  {
    label: "order.step.label",
    path: "/order/environmental/label",
    key: "label",
  },
  { label: "order.step.qa", path: "/order/environmental/qa", key: "qa" },
];

const VECTOR_ORDER_STEPS = [
  { label: "order.step.enter", path: "/order/vector/enter", key: "enter" },
  { label: "order.step.label", path: "/order/vector/label", key: "label" },
  { label: "order.step.qa", path: "/order/vector/qa", key: "qa" },
  {
    label: "order.step.complete",
    path: "/order/vector/complete",
    key: "complete",
  },
];

// Backward-compat alias used by any code that still imports ORDER_STEPS
const ORDER_STEPS = CLINICAL_ORDER_STEPS;

const OrderStepper = ({ currentStep, steps, onStepClick, className = "" }) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { samples, storageSkipped, labNumber, stepProgress } =
    useOrderContext();

  // If no explicit steps prop, infer from URL prefix
  const resolvedSteps =
    steps ||
    (() => {
      const path = location.pathname;
      if (path.startsWith("/order/vector")) return VECTOR_ORDER_STEPS;
      if (path.startsWith("/order/environmental"))
        return ENVIRONMENTAL_ORDER_STEPS;
      return CLINICAL_ORDER_STEPS;
    })();

  // Determine current step from URL if not provided
  const activeStep =
    currentStep !== undefined
      ? currentStep
      : resolvedSteps.findIndex((step) => location.pathname === step.path);

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

      case "complete":
        // Reaching the Complete step implies the order is finalized.
        return stepProgress?.qa || false;

      default:
        return false;
    }
  };

  const handleStepClick = (stepIndex) => {
    if (onStepClick) {
      onStepClick(stepIndex);
    } else {
      const path = resolvedSteps[stepIndex].path;
      history.push(
        labNumber ? `${path}?order=${encodeURIComponent(labNumber)}` : path,
      );
    }
  };

  return (
    <ProgressIndicator
      currentIndex={activeStep >= 0 ? activeStep : 0}
      className={`order-stepper ${className}`}
      spaceEqually={true}
      onChange={(stepIndex) => handleStepClick(stepIndex)}
    >
      {resolvedSteps.map((step) => (
        <ProgressStep
          key={step.path}
          complete={isStepComplete(step.key)}
          label={intl.formatMessage({ id: step.label })}
        />
      ))}
    </ProgressIndicator>
  );
};

export {
  ORDER_STEPS,
  CLINICAL_ORDER_STEPS,
  ENVIRONMENTAL_ORDER_STEPS,
  VECTOR_ORDER_STEPS,
};
export default OrderStepper;
