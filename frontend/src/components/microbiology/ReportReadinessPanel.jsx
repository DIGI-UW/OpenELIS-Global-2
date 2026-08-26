import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  InlineNotification,
  Layer,
  Link as CarbonLink,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { Link as RouterLink } from "react-router-dom";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import MicrobiologyService from "./MicrobiologyService";

const ReportReadinessPanel = ({
  caseId,
  service = MicrobiologyService,
  finalReleaseState = "",
  patientId,
  preliminaryReleaseAllowed = true,
  onReleased,
  onProjectionLoaded,
  refreshToken = 0,
  amendmentOpen = false,
}) => {
  const intl = useIntl();
  const [readiness, setReadiness] = useState(null);
  const [whonetReadiness, setWhonetReadiness] = useState(null);
  const [projection, setProjection] = useState(null);
  const [releaseState, setReleaseState] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const mountedRef = useRef(true);
  const releaseErrorMessage = (releaseError) =>
    releaseError?.message ||
    intl.formatMessage({ id: "microbiology.release.failed" });

  const loadState = () =>
    Promise.all([
      service.getCaseReadiness(caseId),
      service.getWhonetReadiness(caseId),
      service.getReportProjection
        ? service.getReportProjection(caseId)
        : Promise.resolve({ reportableContent: true, mappingConfigured: true }),
    ]).then(([caseReadiness, whonetState, reportProjection]) => {
      if (!mountedRef.current) {
        return;
      }
      setReadiness(caseReadiness);
      setWhonetReadiness(whonetState);
      setProjection(reportProjection);
      if (onProjectionLoaded) {
        onProjectionLoaded(reportProjection?.projectedResultIds || []);
      }
    });

  useEffect(() => {
    mountedRef.current = true;
    loadState();
    return () => {
      mountedRef.current = false;
    };
  }, [caseId, refreshToken]);

  const releaseFinal = () => {
    setSaving(true);
    setError("");
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
      .catch((releaseError) => {
        if (mountedRef.current) {
          setError(releaseErrorMessage(releaseError));
        }
      })
      .finally(() => {
        if (mountedRef.current) {
          setSaving(false);
        }
      });
  };

  const releasePreliminary = () => {
    setSaving(true);
    setError("");
    service
      .releasePreliminaryReport(caseId)
      .then((state) => {
        if (mountedRef.current) {
          setReleaseState(state);
        }
        if (onReleased) {
          onReleased();
        }
      })
      .then(loadState)
      .catch((releaseError) => {
        if (mountedRef.current) {
          setError(releaseErrorMessage(releaseError));
        }
      })
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
        {formatMicrobiologyEnum(blocker, intl)}
      </li>
    ));
  };

  return (
    <section
      className="microbiology-card"
      data-testid="microbiology-report-card"
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
              {formatMicrobiologyEnum(effectiveReleaseState, intl)}
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

      <Layer className="microbiology-report-projection" level={1}>
        <h4>
          {intl.formatMessage({ id: "microbiology.release.patientReport" })}
        </h4>
        {projection?.reportableContent ? (
          <p data-testid="microbiology-report-projection-content">
            {projection.content}
          </p>
        ) : (
          <p>
            {intl.formatMessage({
              id: "microbiology.release.noReportableContent",
            })}
          </p>
        )}
        <Tag type={projection?.mappingConfigured ? "green" : "warm-gray"}>
          {intl.formatMessage({
            id: projection?.mappingConfigured
              ? "microbiology.release.mappingConfigured"
              : "microbiology.release.mappingRequired",
          })}
        </Tag>
      </Layer>

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "microbiology.case.error" })}
          subtitle={error}
          hideCloseButton
        />
      )}

      <div className="microbiology-report-actions">
        {patientId && (
          <CarbonLink
            as={RouterLink}
            to={`/PatientResults/${encodeURIComponent(patientId)}`}
          >
            {intl.formatMessage({
              id: "microbiology.release.viewPatientResults",
            })}
          </CarbonLink>
        )}
        {amendmentOpen ? (
          <Tag type="purple">
            {intl.formatMessage({
              id: "microbiology.release.useAmendment",
            })}
          </Tag>
        ) : finalReleased ? (
          <Tag type="green">
            {intl.formatMessage({ id: "microbiology.release.finalReleased" })}
          </Tag>
        ) : (
          <>
            {preliminaryReleaseAllowed && (
              <Button
                kind="secondary"
                onClick={releasePreliminary}
                disabled={saving || !projection?.reportableContent}
              >
                {intl.formatMessage({ id: "microbiology.release.preliminary" })}
              </Button>
            )}
            <Button
              onClick={releaseFinal}
              disabled={
                saving ||
                !readiness?.finalReleaseReady ||
                !projection?.reportableContent ||
                !projection?.mappingConfigured
              }
            >
              {intl.formatMessage({ id: "microbiology.release.final" })}
            </Button>
          </>
        )}
      </div>
    </section>
  );
};

export default ReportReadinessPanel;
