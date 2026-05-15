import React, { useMemo } from "react";
import { Tile, Tag, ProgressBar } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
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
  const intl = useIntl();
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

  const isVectorWorkflow =
    orderData?.sampleOrderItems?.environmentalFields?.workflowType === "vector";

  const fallbackSampleName = intl.formatMessage({
    id: "sample.fallback.name",
    defaultMessage: "Sample",
  });

  const poolGroups = useMemo(() => {
    if (!isVectorWorkflow) return [];
    const visible = (samples || []).filter((s) => !s?.voided);
    const groups = new Map();
    visible.forEach((sample, index) => {
      // Prefer vectorPoolId so multiple pools of the same animal stay
      // distinct groups; fall back to sampleTypeId only for pre-fan-out
      // specimens that don't yet have a pool id.
      const key =
        sample?.vectorPoolId ||
        sample?.typeOfSampleId ||
        sample?.sampleTypeId ||
        `unknown-${index}`;
      if (!groups.has(key)) {
        groups.set(key, {
          key,
          name: sample?.sampleTypeName || sample?.name || fallbackSampleName,
          specimens: [],
        });
      }
      groups.get(key).specimens.push(sample);
    });
    return Array.from(groups.values()).map((g) => {
      const source =
        g.specimens.find(
          (s) =>
            (s.panels && s.panels.length > 0) ||
            (s.tests && s.tests.length > 0),
        ) || g.specimens[0];
      const panels = (source?.panels || []).map((p) => p.name).filter(Boolean);
      const panelTestIds = new Set(
        (source?.panels || []).flatMap((p) =>
          p.testIds ? p.testIds.split(",").map((id) => id.trim()) : [],
        ),
      );
      const tests = (source?.tests || [])
        .filter((t) => !panelTestIds.has(String(t.id)))
        .map((t) => t.name)
        .filter(Boolean);
      return { ...g, count: g.specimens.length, panels, tests };
    });
  }, [samples, isVectorWorkflow, fallbackSampleName]);

  const sampleTypes = isVectorWorkflow
    ? []
    : samples?.map((s) => s.name || s.sampleTypeName).filter(Boolean);

  const testCount = isVectorWorkflow
    ? 0
    : samples?.reduce(
        (count, sample) => count + (sample.tests?.length || 0),
        0,
      );

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

        {/* Sample Types — vector: per-pool with each pool's tests; otherwise:
            the flat comma-list with total test count. */}
        {isVectorWorkflow && poolGroups.length > 0 && (
          <div className="context-item">
            <span className="context-label">
              <FormattedMessage id="sample.types" defaultMessage="Samples" />:
            </span>
            <ul className="context-value context-pool-list">
              {poolGroups.map((pool) => {
                const testNames = [...pool.panels, ...pool.tests];
                return (
                  <li key={pool.key}>
                    <strong>
                      <FormattedMessage
                        id="order.context.poolOf"
                        defaultMessage="Pool of {count} {animal}"
                        values={{ count: pool.count, animal: pool.name }}
                      />
                    </strong>
                    {testNames.length > 0 ? (
                      <span className="pool-tests">
                        : {testNames.join(", ")}
                      </span>
                    ) : (
                      <span className="pool-tests pool-tests-empty">
                        :{" "}
                        <FormattedMessage
                          id="qa.summary.noTests"
                          defaultMessage="No tests selected"
                        />
                      </span>
                    )}
                  </li>
                );
              })}
            </ul>
          </div>
        )}

        {!isVectorWorkflow && sampleTypes && sampleTypes.length > 0 && (
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
