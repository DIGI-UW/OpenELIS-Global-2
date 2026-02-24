import React, { useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Button, Loading } from "@carbon/react";
import { WarningAlt } from "@carbon/react/icons";
import DismissalModal from "./DismissalModal";
import NCEInlineWrapper from "../NCEInlineWrapper/NCEInlineWrapper";
import useDeltaCheckAlert from "./useDeltaCheckAlert";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import "./DeltaCheckAlert.scss";

/**
 * DeltaCheckAlert displays a purple alert panel when a delta check
 * threshold is exceeded, showing current vs. previous values with
 * dismiss and Report NCE options. Clicking "Report NCE" collapses
 * the alert and opens the inline NCE form in its place.
 */
const DeltaCheckAlert = ({
  analysisIds,
  onAlertDismissed,
  onAlertEscalated,
  onAlertsLoaded,
}) => {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const {
    alerts,
    loading,
    selectedAlert,
    dismissModalOpen,
    openDismissModal,
    closeDismissModal,
    dismissAlert,
    refreshAlerts,
  } = useDeltaCheckAlert(analysisIds);

  // Notify parent of loaded alerts so per-row badges can be rendered
  useEffect(() => {
    onAlertsLoaded?.(alerts);
  }, [alerts, onAlertsLoaded]);

  // Track which alert is being reported as NCE via the inline form
  const [reportingAlertId, setReportingAlertId] = useState(null);

  if (configurationProperties?.NCE_DELTA_CHECK_ENABLED !== "true") {
    return null;
  }

  if (loading) {
    return <Loading small withOverlay={false} />;
  }

  if (!alerts.length) {
    return null;
  }

  const buildDeltaCheckContext = (alert) => ({
    resultValue: String(alert.currentValue ?? ""),
    suggestedSeverity: "Critical",
    qualityFlags: [
      intl.formatMessage(
        { id: "deltacheck.context.flag" },
        {
          changePercent: alert.changePercent?.toFixed(1),
          thresholdPercent: alert.thresholdPercent,
        },
      ),
    ],
  });

  const handleNCECreated = (nceData) => {
    setReportingAlertId(null);
    refreshAlerts();
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage(
        { id: "deltacheck.escalate.success" },
        { nceNumber: nceData?.nceNumber || "" },
      ),
    });
    setNotificationVisible(true);
    onAlertEscalated?.(nceData);
  };

  return (
    <div className="delta-check-alerts">
      {alerts.map((alert) => {
        // If this alert is being reported, show the inline NCE form instead
        if (reportingAlertId === alert.id) {
          return (
            <NCEInlineWrapper
              key={`nce-${alert.id}`}
              resultId={alert.resultId}
              alertId={alert.id}
              context={buildDeltaCheckContext(alert)}
              onNCECreated={handleNCECreated}
              onCancel={() => setReportingAlertId(null)}
            />
          );
        }

        return (
          <div key={alert.id} className="delta-check-alert">
            <div className="delta-check-alert__header">
              <WarningAlt size={16} />
              <span className="delta-check-alert__title">
                <FormattedMessage id="deltacheck.alert.title" />
              </span>
            </div>

            <p className="delta-check-alert__message">
              <FormattedMessage id="deltacheck.alert.description" />
            </p>

            <div className="delta-check-alert__values">
              <div className="delta-check-alert__value-item">
                <span className="delta-check-alert__label">
                  <FormattedMessage id="deltacheck.alert.currentValue" />
                </span>
                <span className="delta-check-alert__number">
                  {alert.currentValue}
                </span>
              </div>
              <div className="delta-check-alert__value-item">
                <span className="delta-check-alert__label">
                  <FormattedMessage id="deltacheck.alert.previousValue" />
                </span>
                <span className="delta-check-alert__number">
                  {alert.previousValue}
                </span>
              </div>
              <div className="delta-check-alert__value-item">
                <span className="delta-check-alert__label">
                  <FormattedMessage id="deltacheck.alert.changePercent" />
                </span>
                <span className="delta-check-alert__number delta-check-alert__number--change">
                  {alert.changePercent?.toFixed(1)}%
                </span>
              </div>
              <div className="delta-check-alert__value-item">
                <span className="delta-check-alert__label">
                  <FormattedMessage id="deltacheck.alert.threshold" />
                </span>
                <span className="delta-check-alert__number">
                  &plusmn;{alert.thresholdPercent}%
                </span>
              </div>
            </div>

            <div className="delta-check-alert__actions">
              <Button
                kind="tertiary"
                size="sm"
                onClick={() => openDismissModal(alert)}
              >
                <FormattedMessage id="deltacheck.alert.dismiss" />
              </Button>
              <Button
                kind="danger--tertiary"
                size="sm"
                renderIcon={WarningAlt}
                onClick={() => setReportingAlertId(alert.id)}
              >
                <FormattedMessage id="nce.actions.reportNCE" />
              </Button>
            </div>
          </div>
        );
      })}

      <DismissalModal
        open={dismissModalOpen}
        alert={selectedAlert}
        onClose={closeDismissModal}
        onDismiss={(alertId, reason, modalCallback) => {
          dismissAlert(alertId, reason, (status, isError) => {
            if (!isError) {
              addNotification({
                kind: NotificationKinds.success,
                title: intl.formatMessage({ id: "notification.title" }),
                message: intl.formatMessage({
                  id: "deltacheck.dismiss.success",
                }),
              });
              setNotificationVisible(true);
              onAlertDismissed?.();
            }
            modalCallback?.(status, isError);
          });
        }}
      />
    </div>
  );
};

export default DeltaCheckAlert;
