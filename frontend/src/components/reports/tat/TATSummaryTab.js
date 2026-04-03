import React from "react";
import { Tile, InlineNotification, SkeletonText } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import TATBreakdownTable from "./TATBreakdownTable";

function formatTat(hours) {
  if (hours == null) return "—";
  const h = Math.floor(hours);
  const m = Math.round((hours - h) * 60);
  if (h === 0 && m === 0) return "0h 0m";
  if (h === 0) return `${m}m`;
  if (m === 0) return `${h}h`;
  return `${h}h ${m}m`;
}

const STAT_CARDS = [
  { key: "totalCount", labelId: "reports.tat.totalResults", isCount: true },
  { key: "mean", labelId: "reports.tat.meanTat" },
  { key: "median", labelId: "reports.tat.medianTat", highlight: true },
  { key: "percentile90", labelId: "reports.tat.percentile90" },
  { key: "min", labelId: "reports.tat.minTat" },
  { key: "max", labelId: "reports.tat.maxTat" },
  { key: "stdDeviation", labelId: "reports.tat.stdDeviation" },
];

function TATSummaryTab({ data, loading, filters }) {
  const intl = useIntl();

  if (loading) {
    return (
      <div style={{ padding: "1rem" }}>
        <SkeletonText paragraph lineCount={5} />
      </div>
    );
  }

  if (!data || !filters) {
    return (
      <div style={{ padding: "2rem", textAlign: "center", color: "#6f6f6f" }}>
        <FormattedMessage id="reports.tat.noResults" />
      </div>
    );
  }

  const isWorkingTime = filters.calculationMode === "WORKING_TIME";

  return (
    <div>
      {isWorkingTime && data.excludedDaysCount > 0 && (
        <InlineNotification
          kind="info"
          title={intl.formatMessage(
            { id: "reports.tat.workingTimeInfo" },
            {
              weekendDays: "configured weekends",
              holidayCount: data.excludedDaysCount,
            },
          )}
          lowContrast
          hideCloseButton
          style={{ marginBottom: "1rem" }}
        />
      )}

      {isWorkingTime && data.excludedDaysCount === 0 && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({ id: "reports.tat.noHolidaysWarning" })}
          lowContrast
          hideCloseButton
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Stat Cards - 4+3 layout, Median highlighted */}
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(4, 1fr)",
          gap: "1rem",
          marginBottom: "1rem",
        }}
      >
        {STAT_CARDS.slice(0, 4).map((card) => (
          <Tile
            key={card.key}
            style={{
              backgroundColor: card.highlight ? "#E6F5F2" : undefined,
            }}
          >
            <p style={{ fontSize: "12px", color: "#525252", marginBottom: "0.25rem" }}>
              <FormattedMessage id={card.labelId} />
            </p>
            <p style={{ fontSize: "24px", fontWeight: 600 }}>
              {data.totalCount === 0 ? (
                <FormattedMessage id="reports.tat.insufficientData" />
              ) : card.isCount ? (
                data[card.key]?.toLocaleString()
              ) : (
                formatTat(data[card.key])
              )}
            </p>
          </Tile>
        ))}
      </div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(3, 1fr)",
          gap: "1rem",
          marginBottom: "2rem",
        }}
      >
        {STAT_CARDS.slice(4).map((card) => (
          <Tile key={card.key}>
            <p style={{ fontSize: "12px", color: "#525252", marginBottom: "0.25rem" }}>
              <FormattedMessage id={card.labelId} />
            </p>
            <p style={{ fontSize: "24px", fontWeight: 600 }}>
              {data.totalCount === 0 ? "—" : formatTat(data[card.key])}
            </p>
          </Tile>
        ))}
      </div>

      {/* Histogram placeholder - will use @carbon/charts-react in polish */}
      {data.histogram && data.histogram.length > 0 && (
        <div
          style={{
            border: "1px solid #e0e0e0",
            borderRadius: "4px",
            padding: "1rem",
            marginBottom: "2rem",
          }}
        >
          <h4 style={{ marginBottom: "1rem" }}>
            <FormattedMessage id="reports.tat.distribution" />
          </h4>
          <div style={{ display: "flex", gap: "4px", alignItems: "flex-end", height: "200px" }}>
            {data.histogram.map((bin, i) => {
              const maxCount = Math.max(...data.histogram.map((b) => b.count));
              const height = maxCount > 0 ? (bin.count / maxCount) * 180 : 0;
              const colors = ["#0E6B5E", "#0E6B5E", "#0E6B5E", "#0E6B5E", "#0E6B5E",
                "#F1C21B", "#F1C21B", "#FF832B", "#FF832B", "#DA1E28"];
              return (
                <div
                  key={i}
                  style={{ flex: 1, textAlign: "center" }}
                  title={`${bin.binLabel}: ${bin.count}`}
                >
                  <div
                    style={{
                      height: `${height}px`,
                      backgroundColor: colors[i] || "#0E6B5E",
                      borderRadius: "2px 2px 0 0",
                    }}
                  />
                  <div style={{ fontSize: "10px", color: "#525252", marginTop: "4px" }}>
                    {bin.binLabel}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Breakdown Table */}
      {data.breakdown && data.breakdown.length > 0 && (
        <TATBreakdownTable breakdown={data.breakdown} />
      )}
    </div>
  );
}

export default TATSummaryTab;
