import React, { useState, useEffect } from "react";
import { LineChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";
import { useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../utils/Utils";

const LeveyJenningsChart = ({ controlId, values }) => {
  const intl = useIntl();
  const [chartData, setChartData] = useState(null);

  useEffect(() => {
    if (values && values.length > 0) {
      postToOpenElisServerJsonResponse(
        `/rest/qc/controls/${controlId}/chart-data`,
        JSON.stringify({ values }),
        (response) => {
          if (response && response.dataPoints) {
            setChartData(response);
          }
        },
      );
    }
  }, [controlId, values]);

  if (
    !chartData ||
    !chartData.dataPoints ||
    chartData.dataPoints.length === 0
  ) {
    return <p>{intl.formatMessage({ id: "qc.leveyJennings.noData" })}</p>;
  }

  const data = [];

  chartData.dataPoints.forEach((point) => {
    data.push({
      group: "QC Value",
      key: String(point.index + 1),
      value: Number(point.value),
    });
  });

  const mean = Number(chartData.mean);
  const plus2SD = Number(chartData.plus2SD);
  const minus2SD = Number(chartData.minus2SD);
  const plus3SD = Number(chartData.plus3SD);
  const minus3SD = Number(chartData.minus3SD);

  chartData.dataPoints.forEach((point) => {
    data.push({
      group: intl.formatMessage({ id: "qc.leveyJennings.mean" }),
      key: String(point.index + 1),
      value: mean,
    });
    data.push({
      group: intl.formatMessage({ id: "qc.leveyJennings.plus2SD" }),
      key: String(point.index + 1),
      value: plus2SD,
    });
    data.push({
      group: intl.formatMessage({ id: "qc.leveyJennings.minus2SD" }),
      key: String(point.index + 1),
      value: minus2SD,
    });
    data.push({
      group: intl.formatMessage({ id: "qc.leveyJennings.plus3SD" }),
      key: String(point.index + 1),
      value: plus3SD,
    });
    data.push({
      group: intl.formatMessage({ id: "qc.leveyJennings.minus3SD" }),
      key: String(point.index + 1),
      value: minus3SD,
    });
  });

  const options = {
    title: intl.formatMessage({ id: "qc.leveyJennings.title" }),
    axes: {
      bottom: { title: "Measurement", mapsTo: "key", scaleType: "labels" },
      left: { title: "Value", mapsTo: "value", scaleType: "linear" },
    },
    color: {
      scale: {
        "QC Value": "#0f62fe",
        [intl.formatMessage({ id: "qc.leveyJennings.mean" })]: "#198038",
        [intl.formatMessage({ id: "qc.leveyJennings.plus2SD" })]: "#f1c21b",
        [intl.formatMessage({ id: "qc.leveyJennings.minus2SD" })]: "#f1c21b",
        [intl.formatMessage({ id: "qc.leveyJennings.plus3SD" })]: "#da1e28",
        [intl.formatMessage({ id: "qc.leveyJennings.minus3SD" })]: "#da1e28",
      },
    },
    curve: "curveMonotoneX",
    height: "400px",
    legend: { position: "bottom" },
    tooltip: { showTotal: false },
  };

  return (
    <div>
      <LineChart data={data} options={options} />
    </div>
  );
};

export default LeveyJenningsChart;
