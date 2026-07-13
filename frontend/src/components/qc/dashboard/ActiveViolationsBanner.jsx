/**
 * ActiveViolationsBanner Component
 *
 * Attention-only banner above the QC summary tiles showing unresolved
 * Westgard violations (top 5, REJECTION first, newest first) with inline
 * acknowledge. Renders nothing when there are no unresolved violations.
 *
 * Fetches independently of the dashboard poll; the parent re-triggers it
 * via the refreshSignal prop.
 */

import React, { useState, useEffect, useCallback, useRef } from "react";
import { Tag, Button } from "@carbon/react";
import { useHistory } from "react-router-dom";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { getSeverityTagType, formatTimestamp } from "./qcDashboardUtils";
import "./ActiveViolationsBanner.css";

const MAX_ROWS = 5;

const severityRank = (violation) =>
  violation.severity === "REJECTION" ? 0 : 1;

const ActiveViolationsBanner = ({ refreshSignal }) => {
  const intl = useIntl();
  const intlRef = useRef(intl);
  intlRef.current = intl;
  const history = useHistory();

  const [violations, setViolations] = useState([]);
  const [error, setError] = useState(null);

  const loadViolations = useCallback(() => {
    getFromOpenElisServer("/rest/qc/violations?unresolved=true", (response) => {
      if (Array.isArray(response)) {
        setViolations(response);
      } else if (response && response.data) {
        setViolations(response.data.violations || response.data || []);
      } else {
        // Banner is supplementary; on fetch failure render nothing
        setViolations([]);
      }
    });
  }, []);

  useEffect(() => {
    loadViolations();
  }, [loadViolations, refreshSignal]);

  const handleAcknowledge = (violationId) => {
    postToOpenElisServerFullResponse(
      `/rest/qc/violations/${violationId}/acknowledge`,
      JSON.stringify({}),
      (response) => {
        if (response.ok) {
          setError(null);
          loadViolations();
        } else {
          setError(
            intlRef.current.formatMessage({
              id: "qc.violations.error.acknowledgeFailed",
            }),
          );
        }
      },
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
      <ul className="active-violations-banner__list">
        {topViolations.map((violation) => (
          <li
            key={violation.id}
            className="active-violations-banner__row"
            data-testid={`banner-violation-${violation.id}`}
          >
            <Tag type={getSeverityTagType(violation.severity)}>
              {violation.severity}
            </Tag>
            <span className="active-violations-banner__rule">
              {violation.ruleCode}
            </span>
            <span className="active-violations-banner__details">
              {violation.instrumentName || "-"}
              <span className="active-violations-banner__separator">|</span>
              {violation.testName || "-"}
            </span>
            <span className="active-violations-banner__timestamp">
              {formatTimestamp(violation.violationDateTime)}
            </span>
            <Button
              kind="tertiary"
              size="sm"
              onClick={() => handleAcknowledge(violation.id)}
              data-testid={`banner-acknowledge-${violation.id}`}
            >
              {intl.formatMessage({ id: "qc.dashboard.alerts.acknowledge" })}
            </Button>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default ActiveViolationsBanner;
