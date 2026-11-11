/**
 * StatisticsConfigSection Component
 *
 * Displays current statistics configuration and provides button to open
 * the configuration modal. Extracted from ControlLotSetup for clarity.
 *
 * Task Reference: T054
 * Specification: FR-003, FR-004, FR-005
 */

import React from "react";
import { Grid, Column, Tile, Button } from "@carbon/react";
import { Settings } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import StatisticsConfigModal from "./StatisticsConfigModal";

const StatisticsConfigSection = ({
  statisticsConfig,
  statisticsModalOpen,
  onOpenModal,
  onCloseModal,
  onSave,
}) => {
  const intl = useIntl();

  return (
    <>
      <Tile
        className="control-lot-setup-statistics"
        data-testid="control-lot-statistics-tile"
      >
        <div className="control-lot-setup-statistics-header">
          <h4>
            {intl.formatMessage({
              id: "qc.controlLot.statistics.title",
            })}
          </h4>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Settings}
            onClick={onOpenModal}
            data-testid="control-lot-statistics-config-button"
          >
            {intl.formatMessage({
              id: "qc.controlLot.statistics.configure",
            })}
          </Button>
        </div>
        <Grid>
          <Column lg={4} md={4} sm={4}>
            <div className="stat-item">
              <span className="stat-label">
                {intl.formatMessage({
                  id: "qc.controlLot.statistics.method",
                })}
              </span>
              <span className="stat-value">
                {intl.formatMessage({
                  id: `qc.controlLot.statistics.method.${statisticsConfig.calculationMethod?.toLowerCase()}`,
                })}
              </span>
            </div>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <div className="stat-item">
              <span className="stat-label">
                {intl.formatMessage({
                  id: "qc.controlLot.statistics.mean",
                })}
              </span>
              <span className="stat-value">
                {statisticsConfig.mean?.toFixed(2) || "-"}
              </span>
            </div>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <div className="stat-item">
              <span className="stat-label">
                {intl.formatMessage({
                  id: "qc.controlLot.statistics.sd",
                })}
              </span>
              <span className="stat-value">
                {statisticsConfig.standardDeviation?.toFixed(2) || "-"}
              </span>
            </div>
          </Column>
        </Grid>
      </Tile>

      {statisticsModalOpen && (
        <StatisticsConfigModal
          open={statisticsModalOpen}
          config={statisticsConfig}
          onClose={onCloseModal}
          onSave={onSave}
        />
      )}
    </>
  );
};

export default StatisticsConfigSection;
