import React from "react";
import { ClickableTile, Grid, Column } from "@carbon/react";
import { useIntl } from "react-intl";

const AlertSummaryTiles = ({ summary }) => {
  const intl = useIntl();

  const tiles = [
    {
      key: "critical",
      labelId: "alerts.summary.critical",
      value: summary?.criticalAlerts ?? 0,
      className: "alert-tile--critical",
    },
    {
      key: "eqaDeadlines",
      labelId: "alerts.summary.eqaDeadlines",
      value: summary?.eqaDeadlines ?? 0,
      className: "alert-tile--warning",
    },
    {
      key: "statOverdue",
      labelId: "alerts.summary.statOverdue",
      value: summary?.statOverdue ?? 0,
      className: "alert-tile--warning",
    },
    {
      key: "sampleExpiration",
      labelId: "alerts.summary.sampleExpiration",
      value: summary?.sampleExpiration ?? 0,
      className: "alert-tile--info",
    },
  ];

  return (
    <Grid condensed>
      {tiles.map((tile) => (
        <Column key={tile.key} lg={4} md={4} sm={4}>
          <ClickableTile className={tile.className}>
            <h4>{intl.formatMessage({ id: tile.labelId })}</h4>
            <p className="alert-tile__count">{tile.value}</p>
          </ClickableTile>
        </Column>
      ))}
    </Grid>
  );
};

export default AlertSummaryTiles;
