/**
 * ActivityTimelineTab Component
 *
 * Extracted from InstrumentDetailModal. Handles the Activity Timeline tab content
 * — fetching violations and rendering the timeline.
 */

import React, { useState, useEffect, useMemo } from "react";
import { Tag, Loading } from "@carbon/react";
import { useIntl } from "react-intl";
import PropTypes from "prop-types";
import { getFromOpenElisServer } from "../../utils/Utils";
import { getSeverityTagType, formatTimestamp } from "./qcDashboardUtils";

const ActivityTimelineTab = ({ instrument, open }) => {
  const intl = useIntl();

  // Activity Timeline state
  const [violations, setViolations] = useState([]);
  const [timelineLoading, setTimelineLoading] = useState(false);

  // Reset state when modal opens/closes or instrument changes
  useEffect(() => {
    if (!open || !instrument) {
      setViolations([]);
      return;
    }

    // Load timeline data on open
    setTimelineLoading(true);

    getFromOpenElisServer(
      `/rest/qc/violations?instrumentId=${instrument.instrumentId}`,
      (response) => {
        const data = Array.isArray(response) ? response : response?.data || [];
        setViolations(data);
        setTimelineLoading(false);
      },
    );
  }, [open, instrument]);

  // Merge violations + corrective actions into timeline
  const timelineItems = useMemo(() => {
    const violationIds = new Set(violations.map((v) => v.id));

    const violationItems = violations.map((v) => ({
      type: "violation",
      timestamp: v.violationDateTime,
      severity: v.severity,
      ruleCode: v.ruleCode,
      testName: v.testName,
      resolutionStatus: v.resolutionStatus || v.status,
      id: v.id,
    }));

    return violationItems.sort(
      (a, b) => new Date(b.timestamp) - new Date(a.timestamp),
    );
  }, [violations]);

  // Get corrective action status tag type
  const getActionStatusTagType = (status) => {
    switch (status) {
      case "COMPLETED":
        return "green";
      case "IN_PROGRESS":
        return "blue";
      default:
        return "gray";
    }
  };

  // Get violation resolution status tag type
  const getResolutionStatusTagType = (status) => {
    switch (status) {
      case "RESOLVED":
        return "green";
      case "CORRECTIVE_ACTION_PENDING":
        return "blue";
      case "ACKNOWLEDGED":
        return "gray";
      default:
        return "red";
    }
  };

  return (
    <>
      {timelineLoading ? (
        <div className="instrument-detail-timeline__loading">
          <Loading
            withOverlay={false}
            small
            description={intl.formatMessage({
              id: "qc.instrumentDetail.timeline.loading",
            })}
          />
        </div>
      ) : timelineItems.length === 0 ? (
        <div className="instrument-detail-timeline__empty">
          {intl.formatMessage({
            id: "qc.instrumentDetail.timeline.empty",
          })}
        </div>
      ) : (
        <div className="instrument-detail-timeline">
          {timelineItems.map((item) => (
            <div
              key={`${item.type}-${item.id}`}
              className="instrument-detail-timeline__item"
            >
              <div className="instrument-detail-timeline__item-header">
                {item.type === "violation" ? (
                  <>
                    <Tag type={getSeverityTagType(item.severity)} size="sm">
                      {item.severity}
                    </Tag>
                    <span className="instrument-detail-timeline__item-rule">
                      {item.ruleCode}
                    </span>
                    {item.testName && (
                      <span className="instrument-detail-timeline__item-test">
                        {item.testName}
                      </span>
                    )}
                  </>
                ) : (
                  <>
                    <Tag type="blue" size="sm">
                      {intl.formatMessage({
                        id: "qc.instrumentDetail.timeline.correctiveAction",
                      })}
                    </Tag>
                    <span className="instrument-detail-timeline__item-rule">
                      {item.actionType}
                    </span>
                    {item.assignedUserName && (
                      <span className="instrument-detail-timeline__item-test">
                        {item.assignedUserName}
                      </span>
                    )}
                  </>
                )}
                <span className="instrument-detail-timeline__item-time">
                  {formatTimestamp(item.timestamp)}
                </span>
              </div>
              <div className="instrument-detail-timeline__item-status">
                {item.type === "violation" ? (
                  <Tag
                    type={getResolutionStatusTagType(item.resolutionStatus)}
                    size="sm"
                  >
                    {item.resolutionStatus}
                  </Tag>
                ) : (
                  <Tag type={getActionStatusTagType(item.status)} size="sm">
                    {item.status}
                  </Tag>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
};

ActivityTimelineTab.propTypes = {
  instrument: PropTypes.shape({
    instrumentId: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  }),
  open: PropTypes.bool.isRequired,
};

export default ActivityTimelineTab;
