import React from "react";
import { useIntl } from "react-intl";
import QITile from "../../qa/qi/QITile";
import { rateTone, thresholdParts } from "../../qa/qi/qiThresholds";
import { formatTat, pctDelta, windowDelta } from "../../reports/tat/tatUtils";
import ServerDataState from "../../utils/ServerDataState";
import { useServerData } from "../../utils/useServerData";
import Sparkline from "./Sparkline";
import { kpisUrl } from "./managementApi";
import "../../qa/common/QAStyles.css";
import "./KpiStrip.css";

const percent = (v) => (v != null ? `${v.toFixed(2)}%` : "—");

const TILES = [
  {
    key: "samples",
    labelKey: "label.mgmt.kpi.totalSamples",
    primary: (v, intl) => (v != null ? intl.formatNumber(v) : "—"),
    // Descriptive: more samples is neither good nor bad news.
    delta: (v, prior) =>
      v != null && prior > 0
        ? {
            ...windowDelta(
              ((v - prior) / prior) * 100,
              0.05,
              (d) => `${d.toFixed(1)}%`,
              true,
            ),
            tone: "flat",
          }
        : null,
  },
  {
    key: "medianTat",
    labelKey: "label.mgmt.kpi.medianTat",
    primary: (v) => formatTat(v),
    delta: (v, prior) =>
      v != null && prior != null
        ? windowDelta(v - prior, 1 / 60, formatTat, false)
        : null,
  },
  {
    key: "onTime",
    labelKey: "label.mgmt.kpi.onTime",
    primary: percent,
    delta: (v, prior) => pctDelta(v, prior, true),
  },
  {
    key: "rejection",
    labelKey: "label.mgmt.kpi.rejectionRate",
    primary: percent,
    delta: (v, prior) => pctDelta(v, prior, false),
  },
  {
    key: "qcPass",
    labelKey: "label.mgmt.kpi.qcPassRate",
    primary: percent,
    delta: (v, prior) => pctDelta(v, prior, true),
  },
];

const KpiStrip = ({ scope }) => {
  const intl = useIntl();
  const query = useServerData(kpisUrl(scope));

  if (query.isError) {
    return <ServerDataState query={query} />;
  }

  return (
    <div className="mgmt-kpis" data-testid="mgmt-kpis">
      {TILES.map((tile) => {
        const kpi = query.data?.[tile.key];
        const tone = rateTone(kpi?.value, kpi?.thresholds);
        const thresholds = thresholdParts(kpi?.thresholds, "REJECTION");
        return (
          <QITile
            key={tile.key}
            testId={`mgmt-kpi-${tile.key}`}
            titleKey={tile.labelKey}
            accent={tone === "gray" ? "blue" : tone}
            loading={!query.data}
            primary={tile.primary(kpi?.value, intl)}
            delta={kpi ? tile.delta(kpi.value, kpi.prior) : null}
            targetLine={intl.formatMessage({ id: "label.mgmt.kpi.vsPrior" })}
            thresholdLine={
              thresholds
                ? intl.formatMessage(
                    { id: "qa.qi.dashboard.tile.thresholds" },
                    { target: thresholds.target, action: thresholds.action },
                  )
                : null
            }
            secondary={kpi ? <Sparkline points={kpi.spark} /> : null}
          />
        );
      })}
    </div>
  );
};

export default KpiStrip;
