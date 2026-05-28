/**
 * QCSummaryTiles Component
 *
 * Displays 4 summary tiles at the top of the QC Dashboard:
 * 1. Instruments In Control (green)
 * 2. Warning Status (amber)
 * 3. Out of Control (red)
 * 4. Overall QC Pass Rate (progress bar)
 */

import React from "react";
import { Grid, Column, Tile, ProgressBar } from "@carbon/react";
import {
  CheckmarkFilled,
  WarningFilled,
  ErrorFilled,
} from "@carbon/icons-react";
import { useIntl } from "react-intl";
import "./QCSummaryTiles.css";

const QCSummaryTiles = ({ summary, loading }) => {
  const intl = useIntl();

  const total = summary?.totalInstruments || 0;
  const compliant = summary?.compliantInstruments || 0;
  const warning = summary?.warningInstruments || 0;
  const nonCompliant = summary?.nonCompliantInstruments || 0;

  const passRate = total > 0 ? Math.round((compliant / total) * 100) : 0;
  const warningPct = total > 0 ? Math.round((warning / total) * 100) : 0;
  const nonCompliantPct =
    total > 0 ? Math.round((nonCompliant / total) * 100) : 0;

  return (
    <Grid fullWidth className="qc-summary-tiles" data-testid="qc-summary-tiles">
      {/* In Control */}
      <Column lg={4} md={4} sm={4}>
        <Tile
          className="qc-summary-tile qc-summary-tile--green"
          data-testid="qc-summary-tile-in-control"
        >
          <div className="qc-summary-tile__icon">
            <CheckmarkFilled size={24} />
          </div>
          <div className="qc-summary-tile__content">
            <span className="qc-summary-tile__label">
              {intl.formatMessage({ id: "qc.dashboard.summary.inControl" })}
            </span>
            <span className="qc-summary-tile__count">{compliant}</span>
            <span className="qc-summary-tile__subtitle">
              {intl.formatMessage(
                { id: "qc.dashboard.summary.ofTotal" },
                { percentage: passRate },
              )}
            </span>
          </div>
        </Tile>
      </Column>

      {/* Warning Status */}
      <Column lg={4} md={4} sm={4}>
        <Tile
          className="qc-summary-tile qc-summary-tile--yellow"
          data-testid="qc-summary-tile-warning"
        >
          <div className="qc-summary-tile__icon">
            <WarningFilled size={24} />
          </div>
          <div className="qc-summary-tile__content">
            <span className="qc-summary-tile__label">
              {intl.formatMessage({
                id: "qc.dashboard.summary.warningStatus",
              })}
            </span>
            <span className="qc-summary-tile__count">{warning}</span>
            <span className="qc-summary-tile__subtitle">
              {intl.formatMessage(
                { id: "qc.dashboard.summary.ofTotal" },
                { percentage: warningPct },
              )}
            </span>
          </div>
        </Tile>
      </Column>

      {/* Out of Control */}
      <Column lg={4} md={4} sm={4}>
        <Tile
          className="qc-summary-tile qc-summary-tile--red"
          data-testid="qc-summary-tile-out-of-control"
        >
          <div className="qc-summary-tile__icon">
            <ErrorFilled size={24} />
          </div>
          <div className="qc-summary-tile__content">
            <span className="qc-summary-tile__label">
              {intl.formatMessage({
                id: "qc.dashboard.summary.outOfControl",
              })}
            </span>
            <span className="qc-summary-tile__count">{nonCompliant}</span>
            <span className="qc-summary-tile__subtitle">
              {intl.formatMessage(
                { id: "qc.dashboard.summary.ofTotal" },
                { percentage: nonCompliantPct },
              )}
            </span>
          </div>
        </Tile>
      </Column>

      {/* Overall Pass Rate */}
      <Column lg={4} md={4} sm={4}>
        <Tile
          className="qc-summary-tile qc-summary-tile--passrate"
          data-testid="qc-summary-tile-pass-rate"
        >
          <div className="qc-summary-tile__content">
            <span className="qc-summary-tile__label">
              {intl.formatMessage({ id: "qc.dashboard.summary.passRate" })}
            </span>
            <span className="qc-summary-tile__count qc-summary-tile__count--large">
              {passRate}%
            </span>
            <ProgressBar
              value={passRate}
              max={100}
              size="small"
              status={loading ? "active" : "finished"}
            />
          </div>
        </Tile>
      </Column>
    </Grid>
  );
};

export default QCSummaryTiles;
