/**
 * ActiveViolationsBanner Component
 *
 * Attention-only banner above the QC summary tiles showing unresolved
 * Westgard violations (top 5, REJECTION first, newest first) with inline
 * acknowledge. Renders nothing when there are no unresolved violations.
 *
 * Fetches independently of the dashboard poll; the parent re-triggers it
 * via the refreshSignal prop. The endpoint reading and the acknowledge POST
 * are the ones the alerts tab uses, shared from qcDashboardUtils.
 */

import React, { useState, useEffect, useCallback } from "react";
import {
  Tag,
  Button,
  StructuredListWrapper,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
} from "@carbon/react";
import { useHistory } from "react-router-dom";
import { useIntl } from "react-intl";
import {
  ACKNOWLEDGE_FAILED_KEY,
  acknowledgeViolation,
  fetchViolations,
  getSeverityTagType,
  formatTimestamp,
} from "./qcDashboardUtils";
import "./ActiveViolationsBanner.css";

const MAX_ROWS = 5;

const severityRank = (violation) =>
  violation.severity === "REJECTION" ? 0 : 1;

const ActiveViolationsBanner = ({ refreshSignal }) => {
  const intl = useIntl();
  const history = useHistory();

  const [violations, setViolations] = useState([]);
  const [error, setError] = useState(null);

  const loadViolations = useCallback(() => {
    // Banner is supplementary; on fetch failure render nothing.
    fetchViolations({ unresolvedOnly: true }, setViolations, () =>
      setViolations([]),
    );
  }, []);

  useEffect(() => {
    loadViolations();
  }, [loadViolations, refreshSignal]);

  const handleAcknowledge = (violationId) => {
    acknowledgeViolation(
      violationId,
      () => {
        setError(null);
        loadViolations();
      },
      () => setError(intl.formatMessage({ id: ACKNOWLEDGE_FAILED_KEY })),
    );
  };

  if (violations.length === 0) {
    return null;
  }

  const topViolations = [...violations]
    .sort(
      (a, b) =>
        severityRank(a) - severityRank(b) ||
        new Date(b.violationDateTime) - new Date(a.violationDateTime),
    )
    .slice(0, MAX_ROWS);

  return (
    <div
      className="active-violations-banner"
      data-testid="active-violations-banner"
      role="status"
    >
      <div className="active-violations-banner__header">
        <span className="active-violations-banner__title">
          {intl.formatMessage(
            { id: "qc.dashboard.banner.title" },
            { count: violations.length },
          )}
        </span>
        {violations.length > MAX_ROWS && (
          <Button
            kind="ghost"
            size="sm"
            onClick={() => history.push("/qa/qc/alerts")}
            data-testid="active-violations-banner-view-all"
          >
            {intl.formatMessage({ id: "qc.dashboard.banner.viewAll" })}
          </Button>
        )}
      </div>
      {error && <div className="active-violations-banner__error">{error}</div>}
      <StructuredListWrapper isCondensed>
        <StructuredListBody>
          {topViolations.map((violation) => (
            <StructuredListRow
              key={violation.id}
              data-testid={`banner-violation-${violation.id}`}
            >
              <StructuredListCell>
                <Tag type={getSeverityTagType(violation.severity)}>
                  {violation.severity}
                </Tag>
              </StructuredListCell>
              <StructuredListCell>{violation.ruleCode}</StructuredListCell>
              <StructuredListCell>
                {violation.instrumentName || "-"}
              </StructuredListCell>
              <StructuredListCell>
                {violation.testName || "-"}
              </StructuredListCell>
              <StructuredListCell>
                {formatTimestamp(violation.violationDateTime)}
              </StructuredListCell>
              <StructuredListCell>
                <Button
                  kind="tertiary"
                  size="sm"
                  onClick={() => handleAcknowledge(violation.id)}
                  data-testid={`banner-acknowledge-${violation.id}`}
                >
                  {intl.formatMessage({
                    id: "qc.dashboard.alerts.acknowledge",
                  })}
                </Button>
              </StructuredListCell>
            </StructuredListRow>
          ))}
        </StructuredListBody>
      </StructuredListWrapper>
    </div>
  );
};

export default ActiveViolationsBanner;
