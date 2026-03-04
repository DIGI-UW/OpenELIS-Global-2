import React, { useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Button } from "@carbon/react";
import { Activity, WarningAlt } from "@carbon/react/icons";
import NCEInlineWrapper from "../NCEInlineWrapper/NCEInlineWrapper";

/**
 * DeltaCheckDetailPanel displays delta check details inline when
 * the "Review Delta" button is clicked. Purple-themed per spec.
 */
const DeltaCheckDetailPanel = ({ alert, onClose, onReportNCE }) => {
  const intl = useIntl();

  return (
    <div
      style={{
        borderLeft: "3px solid #6a1b9a",
        background: "#f3e5f5",
        padding: "1rem",
        marginTop: "0.5rem",
        borderRadius: "4px",
      }}
    >
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: "0.5rem",
          marginBottom: "0.75rem",
        }}
      >
        <Activity size={16} />
        <strong>
          <FormattedMessage id="deltacheck.alert.title" />
        </strong>
      </div>

      <p style={{ marginBottom: "0.75rem", fontSize: "0.875rem" }}>
        <FormattedMessage id="deltacheck.alert.description" />
      </p>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(4, 1fr)",
          gap: "0.75rem",
          marginBottom: "1rem",
        }}
      >
        <div>
          <div style={{ fontSize: "0.75rem", color: "var(--cds-text-helper)" }}>
            <FormattedMessage id="deltacheck.alert.currentValue" />
          </div>
          <div style={{ fontWeight: 600 }}>{alert.currentValue}</div>
        </div>
        <div>
          <div style={{ fontSize: "0.75rem", color: "var(--cds-text-helper)" }}>
            <FormattedMessage id="deltacheck.alert.previousValue" />
          </div>
          <div style={{ fontWeight: 600 }}>{alert.previousValue}</div>
        </div>
        <div>
          <div style={{ fontSize: "0.75rem", color: "var(--cds-text-helper)" }}>
            <FormattedMessage id="deltacheck.alert.changePercent" />
          </div>
          <div style={{ fontWeight: 600, color: "#6a1b9a" }}>
            {alert.changePercent?.toFixed(1)}%
          </div>
        </div>
        <div>
          <div style={{ fontSize: "0.75rem", color: "var(--cds-text-helper)" }}>
            <FormattedMessage id="deltacheck.alert.threshold" />
          </div>
          <div style={{ fontWeight: 600 }}>
            &plusmn;{alert.thresholdPercent}%
          </div>
        </div>
      </div>

      <div style={{ display: "flex", gap: "0.5rem" }}>
        <Button kind="tertiary" size="sm" onClick={onClose}>
          <FormattedMessage id="deltacheck.alert.dismiss" />
        </Button>
        <Button
          kind="danger--tertiary"
          size="sm"
          renderIcon={WarningAlt}
          onClick={onReportNCE}
        >
          <FormattedMessage id="nce.actions.reportNCE" />
        </Button>
      </div>
    </div>
  );
};

/**
 * ResultNCEActions provides a "Report NCE" button and an optional
 * "Review Delta" button within the results entry expanded row.
 */
const ResultNCEActions = ({ resultId, context, onNCECreated, deltaAlert }) => {
  const [showForm, setShowForm] = useState(false);
  const [showDelta, setShowDelta] = useState(false);

  const handleNCECreated = (nceData) => {
    setShowForm(false);
    onNCECreated?.(nceData);
  };

  return (
    <div className="result-nce-actions">
      {!showForm && !showDelta && (
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button
            kind="danger--tertiary"
            size="sm"
            renderIcon={WarningAlt}
            onClick={() => setShowForm(true)}
          >
            <FormattedMessage id="nce.actions.reportNCE" />
          </Button>
          {deltaAlert && (
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={Activity}
              onClick={() => setShowDelta(true)}
            >
              <FormattedMessage id="nce.actions.reviewDelta" />
            </Button>
          )}
        </div>
      )}

      {showDelta && deltaAlert && (
        <DeltaCheckDetailPanel
          alert={deltaAlert}
          onClose={() => setShowDelta(false)}
          onReportNCE={() => {
            setShowDelta(false);
            setShowForm(true);
          }}
        />
      )}

      {showForm && (
        <NCEInlineWrapper
          resultId={resultId}
          context={context}
          onNCECreated={handleNCECreated}
          onCancel={() => setShowForm(false)}
          sourceType="results_entry"
          triggerType={deltaAlert && showDelta ? "prompted" : "manual"}
          triggerAction={deltaAlert && showDelta ? "delta_check" : undefined}
        />
      )}
    </div>
  );
};

export default ResultNCEActions;
