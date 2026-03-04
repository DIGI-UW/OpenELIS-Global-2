import React, { useState, useEffect, useContext, useMemo } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Button } from "@carbon/react";
import { WarningAlt, WarningFilled } from "@carbon/react/icons";
import NCEInlineWrapper from "../NCEInlineWrapper/NCEInlineWrapper";
import { recordPromptDismissal } from "../../../services/NCEIntegrationService";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import "./ValueAlert.scss";

/**
 * ValueAlert displays inline alerts above the results table when
 * result values trigger critical or extreme conditions. Each alert
 * can be dismissed or used to report an NCE.
 */
const ValueAlert = ({ results, validationState }) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [dismissedIds, setDismissedIds] = useState(new Set());
  const [reportingRowId, setReportingRowId] = useState(null);

  // Find results with critical or extreme values
  const triggeredResults = useMemo(() => {
    if (!results?.length || !validationState) return [];
    return results.filter((row) => {
      const vs = validationState[row.id];
      return vs && (vs.isCritical || vs.isExtreme);
    });
  }, [results, validationState]);

  // Auto-clean dismissed IDs when a row is no longer triggered
  useEffect(() => {
    if (dismissedIds.size === 0) return;
    const triggeredSet = new Set(triggeredResults.map((r) => r.id));
    const stale = [...dismissedIds].filter((id) => !triggeredSet.has(id));
    if (stale.length > 0) {
      setDismissedIds((prev) => {
        const next = new Set(prev);
        stale.forEach((id) => next.delete(id));
        return next;
      });
    }
  }, [triggeredResults, dismissedIds]);

  // Also clear reportingRowId if that row is no longer triggered
  useEffect(() => {
    if (reportingRowId === null) return;
    const stillTriggered = triggeredResults.some(
      (r) => r.id === reportingRowId,
    );
    if (!stillTriggered) {
      setReportingRowId(null);
    }
  }, [triggeredResults, reportingRowId]);

  const visibleAlerts = triggeredResults.filter((r) => !dismissedIds.has(r.id));

  if (visibleAlerts.length === 0) {
    return null;
  }

  const handleDismiss = (rowId, row, vs) => {
    // Fire-and-forget: persist dismissal for audit trail
    recordPromptDismissal(
      {
        triggerAction: vs.isCritical ? "critical_value" : "extreme_value",
        sourceType: "results_entry",
        resultId: row.resultId ? String(row.resultId) : undefined,
        context: `${row.testName}: ${row.resultValue}`,
      },
      () => {},
    );
    setDismissedIds((prev) => new Set(prev).add(rowId));
  };

  const handleNCECreated = (nceData) => {
    setReportingRowId(null);
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage(
        { id: "nce.inline.success" },
        { nceNumber: nceData?.nceNumber || "" },
      ),
    });
    setNotificationVisible(true);
  };

  const buildContext = (row, vs) => {
    const isCritical = vs.isCritical;
    const flag = isCritical
      ? intl.formatMessage({ id: "valuealert.context.critical.flag" })
      : intl.formatMessage({
          id: `valuealert.context.extreme.flag.${vs.extremeReason}`,
        });
    return {
      labNumber: row.accessionNumber,
      testName: row.testName,
      resultValue: String(row.resultValue ?? ""),
      suggestedSeverity: isCritical ? "Critical" : "Major",
      qualityFlags: [flag],
    };
  };

  return (
    <div className="value-alerts">
      {visibleAlerts.map((row) => {
        const vs = validationState[row.id];
        const isCritical = vs.isCritical;
        const alertType = isCritical ? "critical" : "extreme";

        // Show inline NCE form if reporting this row
        if (reportingRowId === row.id) {
          return (
            <NCEInlineWrapper
              key={`nce-${row.id}`}
              resultId={row.resultId}
              context={buildContext(row, vs)}
              onNCECreated={handleNCECreated}
              onCancel={() => setReportingRowId(null)}
              sourceType="results_entry"
              triggerType="prompted"
              triggerAction={vs.isCritical ? "critical_value" : "extreme_value"}
            />
          );
        }

        return (
          <div key={row.id} className={`value-alert value-alert--${alertType}`}>
            <div className="value-alert__header">
              {isCritical ? (
                <WarningFilled size={16} />
              ) : (
                <WarningAlt size={16} />
              )}
              <span className="value-alert__title">
                <FormattedMessage id={`valuealert.${alertType}.title`} />
              </span>
            </div>

            <div className="value-alert__values">
              <div className="value-alert__value-item">
                <span className="value-alert__label">
                  <FormattedMessage id="valuealert.testName" />
                </span>
                <span className="value-alert__number">{row.testName}</span>
              </div>
              <div className="value-alert__value-item">
                <span className="value-alert__label">
                  <FormattedMessage id="valuealert.resultValue" />
                </span>
                <span className="value-alert__number">{row.resultValue}</span>
              </div>
              {isCritical && row.lowCritical != null && (
                <div className="value-alert__value-item">
                  <span className="value-alert__label">
                    <FormattedMessage id="valuealert.criticalRange" />
                  </span>
                  <span className="value-alert__number">
                    {row.lowCritical} – {row.highCritical}
                  </span>
                </div>
              )}
              {!isCritical && (
                <div className="value-alert__value-item">
                  <span className="value-alert__label">
                    <FormattedMessage id="valuealert.normalRange" />
                  </span>
                  <span className="value-alert__number">
                    {row.lowerNormalRange} – {row.upperNormalRange}
                  </span>
                </div>
              )}
            </div>

            <div className="value-alert__actions">
              <Button
                kind="tertiary"
                size="sm"
                onClick={() => handleDismiss(row.id, row, vs)}
              >
                <FormattedMessage id="valuealert.dismiss" />
              </Button>
              <Button
                kind="danger--tertiary"
                size="sm"
                renderIcon={WarningAlt}
                onClick={() => setReportingRowId(row.id)}
                disabled={!row.resultId}
                title={
                  !row.resultId
                    ? intl.formatMessage({ id: "valuealert.nce.saveFirst" })
                    : undefined
                }
              >
                <FormattedMessage id="nce.actions.reportNCE" />
              </Button>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default ValueAlert;
