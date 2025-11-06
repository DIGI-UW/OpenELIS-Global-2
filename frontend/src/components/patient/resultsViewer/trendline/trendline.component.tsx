import React, { useState, useCallback, useMemo, useLayoutEffect } from "react";
import { useTranslation } from "react-i18next";
import { Button, InlineLoading, SkeletonText } from "@carbon/react";
import { ArrowLeft } from "@carbon/react/icons";
import { LineChart } from "@carbon/charts-react";
import {
  formatDate,
  formatTime,
  parseDate,
  ConfigurableLink,
} from "../commons";
import { EmptyState, OBSERVATION_INTERPRETATION } from "../commons";
import { useObstreeData } from "./trendline-resource";
import { testResultsBasePath } from "../helpers";
import CommonDataTable from "../overview/common-datatable.component";
import RangeSelector from "./range-selector.component";
//import styles from './trendline.scss';
import "./trendline.scss";

enum ScaleTypes {
  TIME = "time",
  LINEAR = "linear",
  LOG = "log",
  LABELS = "labels",
}

enum TickRotations {
  ALWAYS = "always",
  AUTO = "auto",
  NEVER = "never",
}

const TrendLineBackground = ({ ...props }) => (
  <div {...props} className="background" />
);

const TrendlineHeader = ({
  patientUuid,
  title,
  referenceRange,
  isValidating,
  showBackToTimelineButton,
}) => {
  const { t } = useTranslation();
  return (
    <div className="header">
      <div className="backButton">
        {showBackToTimelineButton && (
          <ConfigurableLink to="#groupedtimeline">
            <Button
              kind="ghost"
              renderIcon={(props) => <ArrowLeft {...props} size={24} />}
              iconDescription={t("returnToTimeline", "Return to timeline")}
            >
              <span>{t("backToTimeline", "Back to timeline")}</span>
            </Button>
          </ConfigurableLink>
        )}
      </div>
      <div className="content">
        <span className="title">{title}</span>
        <span className="referenceange">{referenceRange}</span>
      </div>
      <div>{isValidating && <InlineLoading className="inlineLoader" />}</div>
    </div>
  );
};

interface TrendlineProps {
  patientUuid: string;
  conceptUuid: string;
  basePath: string;
  hideTrendlineHeader?: boolean;
  showBackToTimelineButton?: boolean;
}

const Trendline: React.FC<TrendlineProps> = ({
  patientUuid,
  conceptUuid,
  basePath,
  hideTrendlineHeader = false,
  showBackToTimelineButton = false,
}) => {
  const { trendlineData, isLoading, isValidating } = useObstreeData(
    patientUuid,
    conceptUuid,
  );
  const { t } = useTranslation();
  const {
    obs,
    display: chartTitle,
    hiNormal,
    lowNormal,
    units: leftAxisTitle,
    range: referenceRange,
  } = trendlineData;
  const bottomAxisTitle = t("date", "Date");
  const [range, setRange] = useState<[Date, Date]>();

  const [upperRange, lowerRange] = useMemo(() => {
    if (obs.length === 0) {
      return [new Date(), new Date()];
    }
    return [new Date(), new Date(Date.parse(obs[obs.length - 1].obsDatetime))];
  }, [obs]);

  const setLowerRange = useCallback(
    (selectedLowerRange: Date) => {
      setRange([
        selectedLowerRange > lowerRange ? selectedLowerRange : lowerRange,
        upperRange,
      ]);
    },
    [setRange, upperRange, lowerRange],
  );

  /**
   * reorder svg element to bring line in front of the area
   */
  useLayoutEffect(() => {
    const graph = document.querySelector("g.cds--cc--area")?.parentElement;
    if (obs && obs.length && graph) {
      graph.insertBefore(graph.children[3], graph.childNodes[2]);
    }
  }, [obs]);

  const data: Array<{
    date: Date;
    value: number;
    group: string;
    min?: number;
    max?: number;
  }> = [];

  //

  const tableData: Array<{
    id: string;
    date: string;
    time: string;
    value:
      | number
      | {
          value: number;
          interpretation: OBSERVATION_INTERPRETATION;
        };
  }> = [];

  const dataset = chartTitle;

  obs.forEach((obs, idx) => {
    const range =
      hiNormal && lowNormal
        ? {
            max: hiNormal,
            min: lowNormal,
          }
        : {};

    data.push({
      date: new Date(Date.parse(obs.obsDatetime)),
      value: parseFloat(obs.value),
      group: chartTitle,
      ...range,
    });

    tableData.push({
      id: `${idx}`,
      date: formatDate(parseDate(obs.obsDatetime)),
      time: formatTime(parseDate(obs.obsDatetime)),
      value: {
        value: parseFloat(obs.value),
        interpretation: obs.interpretation,
      },
    });
  });

  const chartOptions = useMemo(
    () => ({
      bounds: {
        lowerBoundMapsTo: "min",
        upperBoundMapsTo: "max",
      },
      axes: {
        bottom: {
          title: bottomAxisTitle,
          mapsTo: "date",
          scaleType: ScaleTypes.TIME,
          ticks: {
            rotation: TickRotations.ALWAYS,
            // formatter: x => x.toLocaleDateString("en-US", TableDateFormatOption)
          },
          domain: range,
        },
        left: {
          mapsTo: "value",
          title: leftAxisTitle,
          scaleType: ScaleTypes.LINEAR,
          includeZero: false,
        },
      },
      height: "20.125rem",

      color: {
        scale: {
          [chartTitle]: "#6929c4",
        },
      },
      points: {
        radius: 4,
        enabled: true,
      },
      legend: {
        enabled: false,
      },
      tooltip: {
        customHTML: ([{ date, value }]) =>
          `<div class="cds--tooltip cds--tooltip--shown" style="min-width: max-content; font-weight:600">${value} ${leftAxisTitle}<br>
          <span style="color: #c6c6c6; font-size: 0.75rem; font-weight:400">${formatDate(date)}</span></div>`,
      },
    }),
    [bottomAxisTitle, leftAxisTitle, range, chartTitle],
  );

  const tableHeaderData = useMemo(
    () => [
      {
        header: t("date", "Date"),
        key: "date",
      },
      {
        header: t("timeOfTest", "Time of Test"),
        key: "time",
      },
      {
        header: `${t("value", "Value")} (${leftAxisTitle})`,
        key: "value",
      },
    ],
    [leftAxisTitle, t],
  );

  if (isLoading) {
    return <SkeletonText />;
  }

  if (obs.length === 0) {
    return (
      <EmptyState
        displayText={t("observationsDisplayText", "observations")}
        headerTitle={chartTitle}
      />
    );
  }

  return (
    <>
      {!hideTrendlineHeader && (
        <TrendlineHeader
          showBackToTimelineButton={showBackToTimelineButton}
          isValidating={isValidating}
          patientUuid={patientUuid}
          title={dataset}
          referenceRange={referenceRange}
        />
      )}
      <TrendLineBackground>
        <RangeSelector setLowerRange={setLowerRange} upperRange={upperRange} />
        <LineChart data={data} options={chartOptions} />
      </TrendLineBackground>
      <DrawTable {...{ tableData, tableHeaderData }} />
    </>
  );
};

const DrawTable = React.memo<{ tableData; tableHeaderData }>(
  ({ tableData, tableHeaderData }) => {
    return <CommonDataTable data={tableData} tableHeaders={tableHeaderData} />;
  },
);

export default Trendline;
