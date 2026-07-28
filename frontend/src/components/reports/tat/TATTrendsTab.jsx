import React, { useState, useEffect, useCallback } from "react";
import { Dropdown, Checkbox, SkeletonText } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";

const INTERVALS = [
  { id: "DAILY", labelKey: "reports.tat.daily" },
  { id: "WEEKLY", labelKey: "reports.tat.weekly" },
  { id: "MONTHLY", labelKey: "reports.tat.monthly" },
];

const COMPARE_OPTIONS = [
  { id: "", labelKey: "reports.tat.compareNone" },
  { id: "LAB_UNIT", labelKey: "reports.tat.labUnit" },
  { id: "PRIORITY", labelKey: "reports.tat.comparePriority" },
  { id: "SAMPLE_TYPE", labelKey: "reports.tat.sampleType" },
  { id: "ORDERING_SITE", labelKey: "reports.tat.orderingSite" },
];

function TATTrendsTab({ filters, buildQueryString }) {
  const intl = useIntl();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [interval, setInterval] = useState("DAILY");
  const [compareBy, setCompareBy] = useState("");
  const [showMedian, setShowMedian] = useState(true);
  const [showMean, setShowMean] = useState(false);
  const [showP90, setShowP90] = useState(true);
  const [showVolume, setShowVolume] = useState(false);

  const fetchData = useCallback(() => {
    if (!filters) return;
    setLoading(true);
    let extra = `&interval=${interval}`;
    if (compareBy) extra += `&compareBy=${compareBy}`;
    const qs = buildQueryString(filters, extra);
    getFromOpenElisServer(`/rest/reports/tat/trend?${qs}`, (res) => {
      setData(res || null);
      setLoading(false);
    });
  }, [filters, buildQueryString, interval, compareBy]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  if (!filters) {
    return (
      <div style={{ padding: "2rem", textAlign: "center", color: "var(--cds-text-helper)" }}>
        <FormattedMessage id="reports.tat.noResults" />
      </div>
    );
  }

  if (loading) {
    return <SkeletonText paragraph lineCount={8} />;
  }

  return (
    <div>
      <div style={{ display: "flex", gap: "1rem", marginBottom: "1rem", flexWrap: "wrap" }}>
        <Dropdown
          id="trend-interval"
          titleText={intl.formatMessage({ id: "reports.tat.aggregation" })}
          items={INTERVALS.map((i) => ({
            id: i.id,
            text: intl.formatMessage({ id: i.labelKey }),
          }))}
          selectedItem={{
            id: interval,
            text: intl.formatMessage({
              id: INTERVALS.find((i) => i.id === interval)?.labelKey,
            }),
          }}
          onChange={({ selectedItem }) => setInterval(selectedItem.id)}
          size="sm"
        />
        <Dropdown
          id="trend-compare"
          titleText={intl.formatMessage({ id: "reports.tat.compareBy" })}
          items={COMPARE_OPTIONS.map((o) => ({
            id: o.id,
            text: intl.formatMessage({ id: o.labelKey }),
          }))}
          selectedItem={{
            id: compareBy,
            text: intl.formatMessage({
              id: COMPARE_OPTIONS.find((o) => o.id === compareBy)?.labelKey || "reports.tat.compareNone",
            }),
          }}
          onChange={({ selectedItem }) => setCompareBy(selectedItem.id)}
          size="sm"
        />
        <div>
          <label style={{ fontSize: "12px", fontWeight: 600, display: "block" }}>{intl.formatMessage({ id: "reports.tat.metrics" })}</label>
          <div style={{ display: "flex", gap: "1rem" }}>
            <Checkbox
              id="show-median"
              labelText={intl.formatMessage({ id: "reports.tat.median" })}
              checked={showMedian}
              onChange={(_, { checked }) => setShowMedian(checked)}
            />
            <Checkbox
              id="show-mean"
              labelText={intl.formatMessage({ id: "reports.tat.mean" })}
              checked={showMean}
              onChange={(_, { checked }) => setShowMean(checked)}
            />
            <Checkbox
              id="show-p90"
              labelText={intl.formatMessage({ id: "reports.tat.percentile90" })}
              checked={showP90}
              onChange={(_, { checked }) => setShowP90(checked)}
            />
          </div>
        </div>
        <Checkbox
          id="show-volume"
          labelText={intl.formatMessage({ id: "reports.tat.showVolume" })}
          checked={showVolume}
          onChange={(_, { checked }) => setShowVolume(checked)}
        />
      </div>

      {data && data.series && data.series.length > 0 ? (
        <div
          style={{
            border: "1px solid var(--cds-border-subtle)",
            borderRadius: "4px",
            padding: "1rem",
            minHeight: "300px",
          }}
        >
          <h4 style={{ marginBottom: "1rem" }}>
            <FormattedMessage id="reports.tat.trend" />
          </h4>
          {data.series.map((series, si) => (
            <div key={si} style={{ marginBottom: "1rem" }}>
              <strong>{series.label}</strong>
              <div style={{ display: "flex", gap: "2px", alignItems: "flex-end", height: "150px" }}>
                {(() => {
                  const getMetricValue = (dp) =>
                    showMedian ? (dp.median || 0) : showMean ? (dp.mean || 0) : showP90 ? (dp.percentile90 || 0) : 0;
                  const maxVal = Math.max(
                    ...series.dataPoints.map((d) => getMetricValue(d)),
                  );
                  return series.dataPoints.map((dp, di) => {
                  const height = maxVal > 0 ? (getMetricValue(dp) / maxVal) * 130 : 0;
                  return (
                    <div
                      key={di}
                      style={{ flex: 1, textAlign: "center" }}
                      title={`${dp.period}: ${showMedian ? "median" : showMean ? "mean" : "p90"} ${getMetricValue(dp)}h, count ${dp.count}`}
                    >
                      <div
                        style={{
                          height: `${height}px`,
                          backgroundColor: "var(--cds-support-success)",
                          borderRadius: "2px 2px 0 0",
                        }}
                      />
                      {showVolume && (
                        <div
                          style={{
                            height: "2px",
                            backgroundColor: "var(--cds-border-subtle)",
                          }}
                        />
                      )}
                      <div style={{ fontSize: "9px", color: "var(--cds-text-secondary)" }}>
                        {dp.period.length > 7 ? dp.period.slice(5) : dp.period}
                      </div>
                    </div>
                  );
                }); })()}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div style={{ padding: "2rem", textAlign: "center", color: "var(--cds-text-helper)" }}>
          <FormattedMessage id="reports.tat.noResults" />
        </div>
      )}
    </div>
  );
}

export default TATTrendsTab;
