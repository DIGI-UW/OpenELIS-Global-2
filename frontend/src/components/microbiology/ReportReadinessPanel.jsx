import React, { useEffect, useRef, useState } from "react";
import { Button, Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import MicrobiologyService from "./MicrobiologyService";

const ReportReadinessPanel = ({
  caseId,
  service = MicrobiologyService,
  finalReleaseState = "",
  onReleased,
  refreshToken = 0,
}) => {
  const intl = useIntl();
  const [readiness, setReadiness] = useState(null);
  const [whonetReadiness, setWhonetReadiness] = useState(null);
  const [releaseState, setReleaseState] = useState(null);
  const [saving, setSaving] = useState(false);
  const mountedRef = useRef(true);

  const loadState = () =>
    Promise.all([
      service.getCaseReadiness(caseId),
      service.getWhonetReadiness(caseId),
    ]).then(([caseReadiness, whonetState]) => {
      if (!mountedRef.current) {
        return;
      }
      setReadiness(caseReadiness);
      setWhonetReadiness(whonetState);
    });

  useEffect(() => {
    mountedRef.current = true;
    loadState();
    return () => {
      mountedRef.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [caseId, refreshToken]);

  const releaseFinal = () => {
    setSaving(true);
    service
      .releaseFinalReport(caseId)
      .then((state) => {
        if (mountedRef.current) {
          setReleaseState(state);
        }
        if (onReleased) {
          onReleased();
        }
      })
      .then(loadState)
      .finally(() => {
        if (mountedRef.current) {
          setSaving(false);
        }
      });
  };

  const effectiveReleaseState =
    releaseState?.finalReleaseState || finalReleaseState;
  const finalReleased = effectiveReleaseState === "FINAL_RELEASED";

  const renderReadinessItems = (ready, blockers = []) => {
    if (ready) {
      return (
        <li>
          <span className="microbiology-status-dot microbiology-status-dot--success">
            {"\u2713"}
          </span>
          {intl.formatMessage({ id: "microbiology.readiness.ready" })}
        </li>
      );
    }

    const rows = blockers.length
      ? blockers
      : [intl.formatMessage({ id: "microbiology.release.notEvaluated" })];

    return rows.map((blocker) => (
      <li key={blocker}>
        <span className="microbiology-status-dot microbiology-status-dot--warning">
          !
        </span>
        {blocker}
      </li>
    ));
  };

  return (
    <section
      className="microbiology-card"
      aria-labelledby="microbiology-release-heading"
    >
      <div className="microbiology-card__header">
        <div>
          <h3 id="microbiology-release-heading">
            {intl.formatMessage({ id: "microbiology.release.title" })}
          </h3>
          <p className="microbiology-card__hint">
            {intl.formatMessage({ id: "microbiology.release.hint" })}
          </p>
        </div>
        {effectiveReleaseState && (
          <div data-testid="microbiology-release-state">
            <Tag type={finalReleased ? "green" : "blue"}>
              {effectiveReleaseState}
            </Tag>
          </div>
        )}
      </div>

      <div className="microbiology-report-grid">
        <div className="microbiology-report-summary">
          <h4>
            {intl.formatMessage({
              id: "microbiology.release.readinessChecks",
            })}
          </h4>
          <p>
            {intl.formatMessage({
              id: readiness?.finalReleaseReady
                ? "microbiology.readiness.ready"
                : "microbiology.readiness.blocked",
            })}
          </p>
          <ul className="microbiology-readiness-list">
            {renderReadinessItems(
              readiness?.finalReleaseReady,
              readiness?.blockers,
            )}
          </ul>
        </div>

        <div className="microbiology-report-summary">
          <h4>
            {intl.formatMessage({ id: "microbiology.release.whonetChecks" })}
          </h4>
          <p>
            {intl.formatMessage({
              id: whonetReadiness?.whonetReady
                ? "microbiology.whonet.ready"
                : "microbiology.whonet.blocked",
            })}
          </p>
          <ul className="microbiology-readiness-list">
            {renderReadinessItems(
              whonetReadiness?.whonetReady,
              whonetReadiness?.blockers,
            )}
          </ul>
        </div>
      </div>

      <div className="microbiology-report-actions">
        {finalReleased ? (
          <Tag type="green">
            {intl.formatMessage({ id: "microbiology.release.finalReleased" })}
          </Tag>
        ) : (
          <Button
            onClick={releaseFinal}
            disabled={saving || !readiness?.finalReleaseReady}
          >
            {intl.formatMessage({ id: "microbiology.release.final" })}
          </Button>
        )}
      </div>
    </section>
  );
};

export default ReportReadinessPanel;
