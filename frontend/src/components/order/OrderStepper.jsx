import React from "react";
import { useHistory, useLocation } from "react-router-dom";
import { ProgressIndicator, ProgressStep } from "@carbon/react";
import { useIntl } from "react-intl";
import { useOrderContext } from "./OrderContext";

/**
 * OrderStepper - Progress indicator for the order workflow (FR-A12).
 *
 * Clinical: Enter Order → Prepare Samples → Sample check (only while the
 * laboratory's clinical sample acceptance setting is not Off).
 * Environmental: Enter → Label → Sample check.
 * Vector: Enter → Label → Sample check → Complete.
 *
 * A clinical step is complete when the order's recorded progress says so:
 * Prepare Samples once the order is Samples prepared, Sample check once it is
 * Ready for testing. Orders saved before progress was recorded fall back to
 * the step flags derived from their data.
 */

const CLINICAL_ORDER_STEPS = [
  { label: "order.step.enter", path: "/order/clinical/enter", key: "enter" },
  {
    label: "order.step.prepare",
    path: "/order/clinical/collect",
    key: "prepare",
  },
  {
    label: "order.step.sampleCheck",
    path: "/order/clinical/qa",
    key: "check",
  },
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
  {
    label: "order.step.sampleCheck",
    path: "/order/environmental/qa",
    key: "qa",
  },
];

const VECTOR_ORDER_STEPS = [
  { label: "order.step.enter", path: "/order/vector/enter", key: "enter" },
  { label: "order.step.label", path: "/order/vector/label", key: "label" },
  { label: "order.step.sampleCheck", path: "/order/vector/qa", key: "qa" },
  {
    label: "order.step.complete",
    path: "/order/vector/complete",
    key: "complete",
  },
];

// Backward-compat alias used by any code that still imports ORDER_STEPS
const ORDER_STEPS = CLINICAL_ORDER_STEPS;

/**
 * The steps a workflow shows, from the URL prefix. The clinical Sample check
 * step exists only while the sample acceptance setting is not Off (FR-F1).
 */
export const stepsForPath = (pathname, sampleCheckEnabled = true) => {
  if (pathname.startsWith("/order/vector")) return VECTOR_ORDER_STEPS;
  if (pathname.startsWith("/order/environmental"))
    return ENVIRONMENTAL_ORDER_STEPS;
  return sampleCheckEnabled
    ? CLINICAL_ORDER_STEPS
    : CLINICAL_ORDER_STEPS.filter((step) => step.key !== "check");
};

const PROGRESS_RANK = {
  ENTERED: 1,
  SAMPLES_PREPARED: 2,
  READY_FOR_TESTING: 3,
};

/** Whether the recorded progress has reached the given status. */
export const progressReached = (status, target) =>
  Boolean(status) &&
  status !== "CANCELLED" &&
  (PROGRESS_RANK[status] || 0) >= (PROGRESS_RANK[target] || 0);

const timeOf = (timestamp) => {
  if (!timestamp) return null;
  const match = String(timestamp).match(/(\d{2}):(\d{2})/);
  return match ? `${match[1]}:${match[2]}` : null;
};

const OrderStepper = ({
  currentStep,
  steps,
  onStepClick,
  className = "",
  currentStepNote,
}) => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const {
    samples,
    storageSkipped,
    labNumber,
    stepProgress,
    progress,
    sampleCheckEnabled,
  } = useOrderContext();

  const resolvedSteps =
    steps || stepsForPath(location.pathname, sampleCheckEnabled);

  // Determine current step from URL if not provided
  const activeStep =
    currentStep !== undefined
      ? currentStep
      : resolvedSteps.findIndex((step) => location.pathname === step.path);

  const recorded = Boolean(progress?.status);

  // Calculate step completion from the recorded progress, else from data
  const isStepComplete = (stepKey) => {
    switch (stepKey) {
      case "enter":
        return !!labNumber;

      case "prepare":
        if (recorded) {
          return progressReached(progress.status, "SAMPLES_PREPARED");
        }
        return Boolean(stepProgress?.collect && stepProgress?.label);

      case "check":
        if (recorded) {
          return progress.status === "READY_FOR_TESTING";
        }
        return stepProgress?.qa || false;

      case "collect":
        return samples.length > 0 && samples.every((s) => s.sampleItemId);

      case "label": {
        const allHaveStorage =
          samples.length > 0 && samples.every((s) => s.storageLocationId);
        return allHaveStorage || storageSkipped || stepProgress?.label || false;
      }

      case "qa":
        return stepProgress?.qa || false;

      case "complete":
        return stepProgress?.qa || false;

      default:
        return false;
    }
  };

  const completedAt = (stepKey) => {
    if (!recorded) return null;
    if (stepKey === "enter") return timeOf(progress.enteredAt);
    if (stepKey === "prepare") return timeOf(progress.preparedAt);
    if (stepKey === "check") return timeOf(progress.readyAt);
    return null;
  };

  const secondaryLabel = (step, index) => {
    if (progress?.status === "CANCELLED") {
      return intl.formatMessage({ id: "order.status.cancelled" });
    }
    if (isStepComplete(step.key)) {
      const time = completedAt(step.key);
      return time
        ? intl.formatMessage({ id: "order.step.doneAt" }, { time })
        : intl.formatMessage({ id: "order.step.done" });
    }
    if (index === activeStep && currentStepNote) {
      return currentStepNote;
    }
    return intl.formatMessage({ id: "order.step.notStarted" });
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
      {resolvedSteps.map((step, index) => (
        <ProgressStep
          key={step.path}
          data-testid={`order-step-${step.key}`}
          complete={isStepComplete(step.key)}
          label={intl.formatMessage({ id: step.label })}
          secondaryLabel={secondaryLabel(step, index)}
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
