import React, {
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { useTranslation } from "react-i18next";
import { EmptyState } from "../commons";
import { ConfigurableLink, useLayoutType } from "../commons";
import { Grid, ShadowBox } from "../commons/utils";
import { makeThrottled, testResultsBasePath } from "../helpers";
import type {
  DateHeaderGridProps,
  PanelNameCornerProps,
  TimelineCellProps,
  DataRowsProps,
} from "./grouped-timeline-types";
import FilterContext from "../filter/filter-context";
//import styles from './grouped-timeline.styles.scss';
import "./grouped-timeline.styles.scss";

const TimeSlots: React.FC<{
  children?: React.ReactNode;
  style?: React.CSSProperties;
  className?: string;
}> = ({ children = undefined, className, ...props }) => (
  <div
    className={`${"timeSlotInner"} ${className ? className : ""}`}
    {...props}
  >
    <div>{children}</div>
  </div>
);

const PanelNameCorner: React.FC<PanelNameCornerProps> = ({
  showShadow,
  panelName,
}) => <TimeSlots className="cornerGridElement">{panelName}</TimeSlots>;

const NewRowStartCell = ({
  title,
  range,
  units,
  conceptUuid,
  shadow = false,
  isString = false,
}) => {
  return (
    <div
      className="rowStartCell"
      style={{
        boxShadow: shadow ? "8px 0 20px 0 rgba(0,0,0,0.15)" : undefined,
      }}
    >
      {!isString ? (
        <ConfigurableLink
          to={"#trendline/" + conceptUuid}
          className="trendlineLink"
        >
          {title}
        </ConfigurableLink>
      ) : (
        <span className="trendlineLink">{title}</span>
      )}
      <span className="rangeUnits">
        {range} {units}
      </span>
    </div>
  );
};

const interpretationToCSS = {
  OFF_SCALE_HIGH: "offScaleHigh",
  CRITICALLY_HIGH: "criticallyHigh",
  HIGH: "high",
  OFF_SCALE_LOW: "offScaleLow",
  CRITICALLY_LOW: "criticallyLow",
  LOW: "low",
  NORMAL: "",
};

const TimelineCell: React.FC<TimelineCellProps> = ({
  text,
  interpretation = "NORMAL",
  zebra,
}) => {
  const additionalClassname: string = interpretationToCSS[interpretation]
    ? interpretationToCSS[interpretation]
    : "";

  return (
    <div
      className={`${"timelineDataCell"} ${zebra ? "timelineCellZebra" : ""} ${additionalClassname}`}
    >
      <p>{text}</p>
    </div>
  );
};

const GridItems = React.memo<{
  sortedTimes: Array<string>;
  obs: any;
  zebra: boolean;
}>(({ sortedTimes, obs, zebra }) => (
  <>
    {sortedTimes.map((_, i) => {
      if (!obs[i]) return <TimelineCell key={i} text={""} zebra={zebra} />;
      return (
        <TimelineCell
          key={i}
          text={obs[i].value}
          interpretation={obs[i].interpretation}
          zebra={zebra}
        />
      );
    })}
  </>
));

const DataRows: React.FC<DataRowsProps> = ({
  timeColumns,
  rowData,
  sortedTimes,
  showShadow,
}) => {
  return (
    <Grid
      dataColumns={timeColumns.length}
      padding
      style={{ gridColumn: "span 2" }}
    >
      {rowData.map((row, index) => {
        const obs = row.entries;
        const { units = "", range = "", obs: values } = row;
        const isString = isNaN(parseFloat(values?.[0]?.value));
        return (
          <React.Fragment key={index}>
            <NewRowStartCell
              {...{
                units,
                range,
                title: row.display,
                shadow: showShadow,
                conceptUuid: row.conceptUuid,
                isString,
              }}
            />
            <GridItems {...{ sortedTimes, obs, zebra: !!(index % 2) }} />
          </React.Fragment>
        );
      })}
    </Grid>
  );
};

const DateHeaderGrid: React.FC<DateHeaderGridProps> = ({
  timeColumns,
  yearColumns,
  dayColumns,
  showShadow,
  xScroll,
  setXScroll,
}) => {
  const ref = useRef();
  const el: HTMLElement | null = ref.current;

  if (el) {
    el.scrollLeft = xScroll;
  }

  const handleScroll = useCallback(
    (e) => {
      setXScroll(e.target.scrollLeft);
    },
    [setXScroll],
  );

  useEffect(() => {
    const div: HTMLElement | null = ref.current;
    if (div) {
      div.addEventListener("scroll", handleScroll);
      return () => div.removeEventListener("scroll", handleScroll);
    }
  }, [handleScroll]);

  return (
    <div ref={ref} style={{ overflowX: "auto" }} className="dateHeaderInner">
      <Grid
        dataColumns={timeColumns.length}
        style={{
          gridTemplateRows: "repeat(3, 24px)",
          zIndex: 2,
          boxShadow: showShadow ? "8px 0 20px 0 rgba(0,0,0,0.15)" : undefined,
        }}
      >
        {yearColumns.map(({ year, size }) => {
          return (
            <TimeSlots
              key={year}
              className="yearColumn"
              style={{ gridColumn: `${size} span` }}
            >
              {year}
            </TimeSlots>
          );
        })}
        {dayColumns.map(({ day, year, size }) => {
          return (
            <TimeSlots
              key={`${day} - ${year}`}
              className="dayColumn"
              style={{ gridColumn: `${size} span` }}
            >
              {day}
            </TimeSlots>
          );
        })}
        {timeColumns.map((time, i) => {
          return (
            <TimeSlots key={time + i} className="timeColumn">
              {time}
            </TimeSlots>
          );
        })}
      </Grid>
    </div>
  );
};

const TimelineDataGroup = ({
  parent,
  subRows,
  xScroll,
  setXScroll,
  panelName,
  setPanelName,
  groupNumber,
}) => {
  const { timelineData } = useContext(FilterContext);
  const {
    data: {
      parsedTime: { timeColumns, sortedTimes },
      rowData,
    },
  } = timelineData;

  const ref = useRef();
  const titleRef = useRef();

  const el: HTMLElement | null = ref.current;
  if (groupNumber === 1 && panelName === "") {
    setPanelName(parent.display);
  }

  if (el) {
    el.scrollLeft = xScroll;
  }

  const handleScroll = makeThrottled((e) => {
    setXScroll(e.target.scrollLeft);
  }, 200);

  useEffect(() => {
    const div: HTMLElement | null = ref.current;
    if (div) {
      div.addEventListener("scroll", handleScroll);
      return () => div.removeEventListener("scroll", handleScroll);
    }
  }, [handleScroll]);

  const onIntersect = (entries, observer) => {
    entries.forEach((entry) => {
      if (entry.intersectionRatio > 0.5) {
        // setPanelName(parent.display);
      }
    });
  };

  const observer = new IntersectionObserver(onIntersect, {
    root: null,
    threshold: 0.5,
  });
  if (titleRef.current) {
    observer.observe(titleRef.current);
  }

  return (
    <>
      <div>
        {groupNumber > 1 && (
          <div className="rowHeader">
            <h6 ref={titleRef}>{parent.display}</h6>
          </div>
        )}
        <div className="gridContainer" ref={ref}>
          <DataRows
            {...{
              timeColumns,
              rowData: subRows,
              sortedTimes,
              showShadow: Boolean(xScroll),
            }}
          />
          <ShadowBox />
        </div>
      </div>
      <div style={{ height: "2em" }}></div>
    </>
  );
};

export const GroupedTimeline = () => {
  const {
    activeTests,
    timelineData,
    parents,
    checkboxes,
    someChecked,
    lowestParents,
  } = useContext(FilterContext);
  const [panelName, setPanelName] = useState("");
  const [xScroll, setXScroll] = useState(0);
  const { t } = useTranslation();
  let shownGroups = 0;
  const tablet = useLayoutType() === "tablet";

  const {
    data: {
      parsedTime: { yearColumns, dayColumns, timeColumns },
      rowData,
    },
    loaded,
  } = timelineData;

  useEffect(() => {
    setPanelName("");
  }, [rowData]);

  if (rowData && rowData?.length === 0) {
    return (
      <EmptyState
        displayText={t("data", "data")}
        headerTitle={t("dataTimelineText", "Data Timeline")}
      />
    );
  }
  if (activeTests && timelineData && loaded) {
    return (
      <div className="timelineHeader" style={{ top: "6.5rem" }}>
        <div className="timelineHeader" style={{ top: "6.5rem" }}>
          <div className="dateHeaderContainer">
            <PanelNameCorner showShadow={true} panelName={panelName} />
            <DateHeaderGrid
              {...{
                timeColumns,
                yearColumns,
                dayColumns,
                showShadow: true,
                xScroll,
                setXScroll,
              }}
            />
          </div>
        </div>
        <div>
          {lowestParents?.map((parent, index) => {
            if (
              parents[parent.flatName].some((kid) => checkboxes[kid]) ||
              !someChecked
            ) {
              shownGroups += 1;
              const subRows = someChecked
                ? rowData?.filter(
                    (row: { flatName: string }) =>
                      parents[parent.flatName].includes(row.flatName) &&
                      checkboxes[row.flatName],
                  )
                : rowData?.filter((row: { flatName: string }) =>
                    parents[parent.flatName].includes(row.flatName),
                  );

              // show kid rows
              return (
                <TimelineDataGroup
                  parent={parent}
                  subRows={subRows}
                  key={index}
                  xScroll={xScroll}
                  setXScroll={setXScroll}
                  panelName={panelName}
                  setPanelName={setPanelName}
                  groupNumber={shownGroups}
                />
              );
            } else return null;
          })}
        </div>
      </div>
    );
  }
  return null;
};

export default GroupedTimeline;
