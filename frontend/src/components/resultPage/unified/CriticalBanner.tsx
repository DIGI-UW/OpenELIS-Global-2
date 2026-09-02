import React, { useEffect, useState } from "react";
import { Button, Tag, TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { WarningFilled } from "@carbon/icons-react";
import { getFromOpenElisServer, putToOpenElisServer } from "../../utils/Utils";

/**
 * OGC-1022 (R3, FR-A4/C2) — the single full-width banner the page is allowed:
 * a critical value needing physician notification. Acknowledgment posts to the
 * Alerts dashboard (the CRITICAL_RESULT alert the save opened) and NEVER gates
 * Save. A comment is required — the dashboard enforces the same rule for
 * critical alerts.
 */
interface AlertDTO {
  id: number;
  alertType?: string;
  status?: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  resolutionNotes?: string;
}

interface CriticalBannerProps {
  analysisId?: string;
  criticalRange?: string;
}

const CriticalBanner: React.FC<CriticalBannerProps> = ({
  analysisId,
  criticalRange,
}) => {
  const intl = useIntl();
  const [alert, setAlert] = useState<AlertDTO | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [comment, setComment] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!analysisId) {
      setLoaded(true);
      return;
    }
    getFromOpenElisServer(
      `/rest/alerts?entityType=ANALYSIS&entityId=${analysisId}`,
      (list: AlertDTO[]) => {
        const criticals = (Array.isArray(list) ? list : []).filter(
          (a) => a.alertType === "CRITICAL_RESULT",
        );
        const open = criticals.find((a) => a.status === "OPEN");
        setAlert(open || criticals[criticals.length - 1] || null);
        setLoaded(true);
      },
    );
  }, [analysisId]);

  const acknowledge = () => {
    if (!alert || !comment.trim()) {
      return;
    }
    setSubmitting(true);
    putToOpenElisServer(
      `/rest/alerts/dashboard/${alert.id}/acknowledge`,
      JSON.stringify({ notes: comment.trim() }),
      (status: number) => {
        setSubmitting(false);
        if (status === 200) {
          setAlert({ ...alert, status: "RESOLVED" });
        }
      },
    );
  };

  const acknowledged = alert !== null && alert.status !== "OPEN";

  return (
    <div className="unifiedCriticalBanner" data-testid="critical-banner">
      <WarningFilled size={20} className="unifiedCriticalBannerIcon" />
      <div className="unifiedCriticalBannerBody">
        <div className="unifiedCriticalBannerTitle">
          <FormattedMessage id="label.results.critical.banner.title" />
        </div>
        <div className="unifiedCriticalBannerText">
          <FormattedMessage id="label.results.critical.banner.body" />
          {criticalRange && (
            <>
              {" "}
              (<FormattedMessage id="label.results.critical.range" />:{" "}
              {criticalRange})
            </>
          )}
        </div>
        {loaded && alert && !acknowledged && (
          <div className="unifiedCriticalBannerAck">
            <TextInput
              id={`critical-ack-comment-${analysisId}`}
              labelText={intl.formatMessage({
                id: "label.results.critical.comment",
              })}
              placeholder={intl.formatMessage({
                id: "label.results.critical.comment.placeholder",
              })}
              value={comment}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
                setComment(e.target.value)
              }
            />
            <Button
              kind="danger"
              size="sm"
              disabled={!comment.trim() || submitting}
              onClick={acknowledge}
              data-testid="critical-acknowledge"
            >
              <FormattedMessage id="label.results.critical.acknowledge" />
            </Button>
          </div>
        )}
        {loaded && acknowledged && (
          <Tag type="green" size="sm">
            <FormattedMessage id="label.results.critical.acknowledged" />
            {alert?.acknowledgedAt ? ` — ${alert.acknowledgedAt}` : ""}
          </Tag>
        )}
      </div>
    </div>
  );
};

export default CriticalBanner;
