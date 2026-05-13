import React, {
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { useTranslation } from "react-i18next";
import {
  DataTable,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
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

// Map an OBSERVATION_INTERPRETATION to a Carbon Tag color so abnormal /
// high / low / critical results stand out without leaning on the custom
// timeline CSS that used to render them.
function interpretationToTagType(interp?: string): string {
  const i = (interp || "").toUpperCase();
  if (i.includes("CRITICAL")) return "red";
  if (i.includes("HIGH")) return "red";
  if (i.includes("LOW")) return "purple";
  if (i === "NORMAL") return "green";
  if (i.includes("ABNORMAL")) return "magenta";
  return "gray";
}

export const GroupedTimeline = () => {
  const { activeTests, timelineData, checkboxes, someChecked } =
    useContext(FilterContext);
  const { t } = useTranslation();

  const {
    data: { rowData },
    loaded,
  } = timelineData;

  if (!activeTests || !timelineData || !loaded) {
    return null;
  }

  // Apply the filter-tree checkboxes the same way the old timeline did:
  // when nothing is checked we show everything; otherwise only the
  // explicitly-checked rows are kept.
  const visibleRows: any[] = !someChecked
    ? rowData
    : (rowData || []).filter((row: any) => checkboxes[row.flatName]);

  // Flatten {test × entries} into one DataTable row per observation so
  // Carbon's table can sort/paginate naturally, and date alignment is
  // implicit (each row carries its own date column).
  const tableRows = (visibleRows || [])
    .flatMap((row: any) =>
      (row.entries || [])
        .filter(
          (e: any) =>
            e && e.value !== undefined && e.value !== null && e.value !== "",
        )
        .map((entry: any, idx: number) => {
          const dateRaw = entry.effectiveDateTime;
          const dateMs = dateRaw ? new Date(dateRaw).getTime() : 0;
          return {
            id: `${row.flatName || row.display}-${idx}`,
            test: row.display,
            date: dateRaw ? new Date(dateRaw).toLocaleString() : "",
            sortDateMs: isNaN(dateMs) ? 0 : dateMs,
            value: String(entry.value),
            interpretation: entry.interpretation || "",
            range: row.range || "",
            units: row.units || "",
          };
        }),
    )
    .sort((a: any, b: any) => b.sortDateMs - a.sortDateMs);

  if (!tableRows.length) {
    return (
      <EmptyState
        displayText={t("data", "data")}
        headerTitle={t("dataTimelineText", "Data Timeline")}
      />
    );
  }

  const headers = [
    { key: "test", header: t("Test", "Test") },
    { key: "date", header: t("Date", "Date") },
    { key: "value", header: t("Result", "Result") },
    { key: "interpretation", header: t("Interpretation", "Interpretation") },
    { key: "range", header: t("Range", "Range") },
    { key: "units", header: t("Units", "Units") },
  ];

  return (
    <DataTable rows={tableRows} headers={headers} isSortable>
      {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
        <TableContainer title={t("patientResults", "Patient Results")}>
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                {headers.map((header) => (
                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row: any) => (
                <TableRow key={row.id} {...getRowProps({ row })}>
                  {row.cells.map((cell: any) => (
                    <TableCell key={cell.id}>
                      {cell.info.header === "interpretation" && cell.value ? (
                        <Tag
                          type={interpretationToTagType(cell.value)}
                          size="sm"
                        >
                          {cell.value}
                        </Tag>
                      ) : (
                        cell.value
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </DataTable>
  );
};

export default GroupedTimeline;
