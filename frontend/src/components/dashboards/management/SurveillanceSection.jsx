import React from "react";
import { ProgressBar, SkeletonText, Tile } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import ServerDataState from "../../utils/ServerDataState";
import { useServerData } from "../../utils/useServerData";
import { surveillanceUrl } from "./managementApi";
import "./SurveillanceSection.css";

export const trendKey = (delta) =>
  delta > 0
    ? "label.mgmt.surveillance.trendUp"
    : delta < 0
      ? "label.mgmt.surveillance.trendDown"
      : "label.mgmt.surveillance.trendStable";

const SurveillanceSection = ({ scope, scopeLabel }) => {
  const intl = useIntl();
  const query = useServerData(surveillanceUrl(scope));

  if (query.isError) return <ServerDataState query={query} />;
  if (!query.data) return <SkeletonText paragraph lineCount={3} />;
  if (query.data.programs.length === 0) return null;

  const text = (value) =>
    intl.messages[value] ? intl.formatMessage({ id: value }) : value;
  const percent = (value) =>
    value == null
      ? "—"
      : intl.formatNumber(value / 100, {
          style: "percent",
          maximumFractionDigits: 1,
        });

  return (
    <section className="mgmt-surveillance" data-testid="mgmt-surveillance">
      <div className="mgmt-surveillance__header">
        <h3>
          <FormattedMessage id="label.mgmt.surveillance.title" />
        </h3>
        <p className="mgmt-surveillance__meta">
          <FormattedMessage
            id="label.mgmt.surveillance.subtitle"
            values={{ 0: scopeLabel }}
          />
        </p>
      </div>
      <div className="mgmt-surveillance__grid">
        {query.data.programs.map((program) => {
          const trend = program.metrics.find((metric) => metric.delta != null);
          return (
            <Tile
              key={program.code}
              className="mgmt-surveillance__card"
              data-testid="mgmt-surveillance-card"
            >
              <h4>{text(program.name)}</h4>
              <p className="mgmt-surveillance__meta">
                <FormattedMessage
                  id="label.mgmt.surveillance.testCount"
                  values={{
                    0: intl.formatNumber(program.testCount),
                    1: scopeLabel,
                  }}
                />
              </p>
              {program.metrics.map((metric) => (
                <div key={metric.key} className="mgmt-surveillance__metric">
                  <div className="mgmt-surveillance__metric-head">
                    <span>{text(metric.label)}</span>
                    <span className="mgmt-surveillance__value">
                      {percent(metric.value)}
                    </span>
                  </div>
                  <ProgressBar
                    value={metric.value ?? 0}
                    label={text(metric.label)}
                    hideLabel
                    size="small"
                  />
                  <p className="mgmt-surveillance__meta">
                    <FormattedMessage
                      id="label.mgmt.surveillance.ofTested"
                      values={{
                        0: intl.formatNumber(metric.numerator),
                        1: intl.formatNumber(metric.denominator),
                      }}
                    />
                  </p>
                </div>
              ))}
              {program.missingCodes.length > 0 && (
                <p
                  className="mgmt-surveillance__meta"
                  data-testid="mgmt-surveillance-setup"
                >
                  <FormattedMessage
                    id="label.mgmt.surveillance.setup"
                    values={{ 0: program.missingCodes.join(", ") }}
                  />
                </p>
              )}
              {trend && (
                <p className="mgmt-surveillance__meta">
                  <FormattedMessage
                    id={trendKey(trend.delta)}
                    values={{
                      0: text(trend.label),
                      1: intl.formatNumber(Math.abs(trend.delta)),
                    }}
                  />
                </p>
              )}
            </Tile>
          );
        })}
      </div>
    </section>
  );
};

export default SurveillanceSection;
