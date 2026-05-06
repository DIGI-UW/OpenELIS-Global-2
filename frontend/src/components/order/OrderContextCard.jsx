import React from "react";
import { Tile, Tag, ProgressBar } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { useOrderContext } from "./OrderContext";

/**
 * OrderContextCard - Persistent context card displayed on all workflow steps.
 *
 * Shows key order information to prevent "wrong order" errors:
 * - Lab Number with status badge
 * - Patient/Subject name
 * - Sample types and test count
 * - Step progress indicator
 *
 * Based on FRS: "Persistent context card (Lab Number, Patient, Tests, Status)
 * prevents high-risk 'wrong order' errors in multi-order environments."
 */

const OrderContextCard = ({ className = "" }) => {
  const {
    labNumber,
    orderData,
    samples,
    stepProgress,
    storageSkipped,
    isReadOnly,
  } = useOrderContext();

  // Don't render if no order is loaded
  if (!labNumber && !orderData?.sampleOrderItems?.labNo) {
    return null;
  }

  const displayLabNumber = labNumber || orderData?.sampleOrderItems?.labNo;

  // Patient name
  const patientName = orderData?.patientProperties
    ? `${orderData.patientProperties.firstName || ""} ${orderData.patientProperties.lastName || ""}`.trim()
    : "";

  // Sample types
  const sampleTypes = samples
    ?.map((s) => s.name || s.sampleTypeName)
    .filter(Boolean);

  // Total test count
  const testCount = samples?.reduce(
    (count, sample) => count + (sample.tests?.length || 0),
    0,
  );

  const isVectorWorkflow =
    orderData?.sampleOrderItems?.environmentalFields?.workflowType === "vector";

  // Calculate step completion based on actual data state (same logic as OrderStepper)
  const isEnterComplete = !!displayLabNumber;
  // Vector workflow has no collect step — sample items are created at order entry
  const isCollectComplete = isVectorWorkflow
    ? true
    : samples.length > 0 && samples.every((s) => s.sampleItemId);
  // Vector workflow has no storage requirement
  const allHaveStorage =
    samples.length > 0 && samples.every((s) => s.storageLocationId);
  const isLabelComplete = isVectorWorkflow
    ? true
    : allHaveStorage || storageSkipped;
  const isQaComplete = stepProgress?.qa || false;

  // Vector workflow: 3 steps (enter, label, qa); clinical: 4 steps (enter, collect, label, qa)
  const totalSteps = isVectorWorkflow ? 3 : 4;
  const completedSteps = isVectorWorkflow
    ? [isEnterComplete, isLabelComplete, isQaComplete].filter(Boolean).length
    : [
        isEnterComplete,
        isCollectComplete,
        isLabelComplete,
        isQaComplete,
      ].filter(Boolean).length;
  const progressPercent = (completedSteps / totalSteps) * 100;

  // Determine order status
  const getOrderStatus = () => {
    if (completedSteps === 4) return { label: "Completed", type: "green" };
    if (completedSteps === 0) return { label: "New", type: "gray" };
    return { label: "In Progress", type: "blue" };
  };

  const status = getOrderStatus();

  return (
    <Tile className={`order-context-card ${className}`}>
      <div className="context-card-content">
        {/* Lab Number and Status */}
        <div className="context-primary">
          <span className="context-lab-number">{displayLabNumber}</span>
          <Tag type={status.type} size="sm">
            {status.label}
          </Tag>
          {isReadOnly && (
            <Tag type="purple" size="sm">
              <FormattedMessage
                id="label.readonly"
                defaultMessage="Read Only"
              />
            </Tag>
          )}
        </div>

        {/* Patient/Subject */}
        {patientName && (
          <div className="context-item">
            <span className="context-label">
              <FormattedMessage id="patient.label" defaultMessage="Patient" />:
            </span>
            <span className="context-value">{patientName}</span>
          </div>
        )}

        {/* Sample Types */}
        {sampleTypes && sampleTypes.length > 0 && (
          <div className="context-item">
            <span className="context-label">
              <FormattedMessage id="sample.types" defaultMessage="Samples" />:
            </span>
            <span className="context-value">
              {sampleTypes.join(", ")}
              {testCount > 0 && (
                <span className="test-count">
                  {" "}
                  ({testCount}{" "}
                  <FormattedMessage
                    id="tests.count"
                    defaultMessage="{count, plural, one {test} other {tests}}"
                    values={{ count: testCount }}
                  />
                  )
                </span>
              )}
            </span>
          </div>
        )}

        {/* Step Progress */}
        <div className="context-progress">
          <ProgressBar
            value={progressPercent}
            size="small"
            hideLabel
            status={completedSteps === totalSteps ? "finished" : "active"}
          />
          <span className="progress-text">
            {completedSteps}/{totalSteps}{" "}
            <FormattedMessage id="steps.complete" defaultMessage="steps" />
          </span>
        </div>
      </div>
    </Tile>
  );
};

export default OrderContextCard;
