import React from "react";
import {
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Tag,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";

const PERFORMANCE_COLOR_MAP = {
  ACCEPTABLE: "green",
  QUESTIONABLE: "warm-gray",
  UNACCEPTABLE: "red",
};

const StatisticsDisplay = ({ statistics }) => {
  const intl = useIntl();

  if (!statistics) return null;

  const {
    mean,
    standardDeviation,
    participantCount,
    hasEnoughParticipants,
    results,
  } = statistics;

  return (
    <div>
      <h4>{intl.formatMessage({ id: "eqa.results.statistics" })}</h4>

      {!hasEnoughParticipants && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({ id: "eqa.results.minimum.participants" })}
          hideCloseButton
          lowContrast
        />
      )}

      <div style={{ marginBottom: "1rem" }}>
        <p>
          <strong>
            {intl.formatMessage({ id: "eqa.results.participant.count" })}:
          </strong>{" "}
          {participantCount}
        </p>
        {mean != null && (
          <p>
            <strong>{intl.formatMessage({ id: "eqa.results.mean" })}:</strong>{" "}
            {mean}
          </p>
        )}
        {standardDeviation != null && (
          <p>
            <strong>
              {intl.formatMessage({ id: "eqa.results.standardDeviation" })}:
            </strong>{" "}
            {standardDeviation}
          </p>
        )}
      </div>

      {results && results.length > 0 && (
        <StructuredListWrapper>
          <StructuredListHead>
            <StructuredListRow head>
              <StructuredListCell head>
                {intl.formatMessage({ id: "eqa.results.organization" })}
              </StructuredListCell>
              <StructuredListCell head>
                {intl.formatMessage({ id: "eqa.results.value" })}
              </StructuredListCell>
              <StructuredListCell head>
                {intl.formatMessage({ id: "eqa.results.zscore" })}
              </StructuredListCell>
              <StructuredListCell head>
                {intl.formatMessage({ id: "eqa.results.performance" })}
              </StructuredListCell>
            </StructuredListRow>
          </StructuredListHead>
          <StructuredListBody>
            {results.map((r, idx) => (
              <StructuredListRow key={idx}>
                <StructuredListCell>{r.organizationId}</StructuredListCell>
                <StructuredListCell>
                  {r.resultValue != null ? String(r.resultValue) : ""}
                </StructuredListCell>
                <StructuredListCell>
                  {r.zScore != null ? String(r.zScore) : "—"}
                </StructuredListCell>
                <StructuredListCell>
                  {r.performanceStatus ? (
                    <Tag
                      type={
                        PERFORMANCE_COLOR_MAP[r.performanceStatus] || "gray"
                      }
                      size="sm"
                    >
                      {intl.formatMessage({
                        id: `eqa.results.${r.performanceStatus.toLowerCase()}`,
                        defaultMessage: r.performanceStatus,
                      })}
                    </Tag>
                  ) : (
                    "—"
                  )}
                </StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      )}
    </div>
  );
};

export default StatisticsDisplay;
