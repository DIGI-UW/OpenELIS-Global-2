import React, { useCallback } from "react";
import {
  Button,
  TableToolbarContent,
  TableToolbar,
  Toggletip,
  ToggletipButton,
  ToggletipContent,
} from "@carbon/react";
import { ChartLine, Information, Table } from "@carbon/react/icons";
import { EmptyState } from "../commons";
import { OverviewPanelEntry } from "./useOverviewData";
import { useTranslation } from "react-i18next";
import { formatDatetime, navigate } from "../commons";
import CommonDataTable from "./common-datatable.component";
//import styles from './common-overview.scss';
import "./common-overview.scss";

const DashboardResultsCount = 5;

interface CommonOverviewPropsBase {
  overviewData: Array<OverviewPanelEntry>;
  insertSeparator?: boolean;
  patientUuid?: string;
}

interface CommonOverviewPropsWithToolbar {
  openTimeline: (panelUuid: string) => void;
  openTrendline: (panelUuid: string, testUuid: string) => void;
}

interface CommonOverviewPropsWithoutToolbar {
  hideToolbar: true;
}

type Only<T, U> = {
  [P in keyof T]: T[P];
} & {
  [P in keyof Omit<U, keyof T>]?: never;
};

type Either<T, U> = Only<T, U> | Only<U, T>;

type CommonOverviewProps = CommonOverviewPropsBase &
  Either<CommonOverviewPropsWithToolbar, CommonOverviewPropsWithoutToolbar>;

const CommonOverview: React.FC<CommonOverviewProps> = ({
  overviewData = [],
  openTimeline,
  openTrendline,
  insertSeparator = false,
  hideToolbar = false,
  patientUuid,
}) => {
  const { t } = useTranslation();
  const [activeCardUuid, setActiveCardUuid] = React.useState("");

  const headers = [
    { key: "name", header: "Test Name" },
    { key: "value", header: "Value" },
    { key: "range", header: "Reference Range" },
  ];

  const isActiveCard = useCallback(
    (uuid: string) =>
      activeCardUuid === uuid ||
      (!activeCardUuid && uuid === overviewData[0][overviewData[0].length - 1]),
    [activeCardUuid, overviewData],
  );

  const handleSeeAvailableResults = useCallback(() => {
    navigate({
      to: `\${openmrsSpaBase}/patient/${patientUuid}/chart/test-results`,
    });
  }, [patientUuid]);

  if (!overviewData.length)
    return (
      <EmptyState
        headerTitle={t("testResults", "Test Results")}
        displayText={t("testResults", "test results")}
      />
    );

  return (
    <>
      {(() => {
        const cards = overviewData.map(
          ([title, type, data, effectiveDateTime, issuedDateTime, uuid]) => (
            <article
              key={uuid}
              className={
                insertSeparator
                  ? ""
                  : `${"card"} ${isActiveCard(uuid) ? "activeCard" : ""}`
              }
            >
              <CommonDataTable
                {...{
                  title,
                  data,
                  description: (
                    <div className={insertSeparator ? "" : "cardHeader"}>
                      <div className="meta">
                        {formatDatetime(effectiveDateTime, { mode: "wide" })}
                        <InfoTooltip
                          effectiveDateTime={effectiveDateTime}
                          issuedDateTime={issuedDateTime}
                        />
                      </div>
                    </div>
                  ),
                  tableHeaders: headers,
                  toolbar: hideToolbar || (
                    <TableToolbar>
                      <TableToolbarContent>
                        {type === "Test" && (
                          <Button
                            kind="ghost"
                            renderIcon={(props) => (
                              <ChartLine size={16} {...props} />
                            )}
                            onClick={() => {
                              setActiveCardUuid(uuid);
                              openTrendline(uuid, uuid);
                            }}
                          >
                            {t("trend", "Trend")}
                          </Button>
                        )}
                        <Button
                          kind="ghost"
                          renderIcon={(props) => <Table size={16} {...props} />}
                          onClick={() => {
                            setActiveCardUuid(uuid);
                            openTimeline(uuid);
                          }}
                        >
                          {t("timeline", "Timeline")}
                        </Button>
                      </TableToolbarContent>
                    </TableToolbar>
                  ),
                }}
              />
              {data.length > DashboardResultsCount && insertSeparator && (
                <Button onClick={handleSeeAvailableResults} kind="ghost">
                  {t("moreResultsAvailable", "More results available")}
                </Button>
              )}
            </article>
          ),
        );

        if (insertSeparator)
          return cards.reduce((acc, val, i, { length }) => {
            acc.push(val);

            if (i < length - 1) {
              acc.push(<Separator key={i} />);
            }

            return acc;
          }, []);

        return cards;
      })()}
    </>
  );
};

const Separator = ({ ...props }) => <div {...props} className="separator" />;

const InfoTooltip = ({ effectiveDateTime, issuedDateTime }) => {
  const { t } = useTranslation();
  return (
    <Toggletip align="bottom" className="tooltipContainer">
      <ToggletipButton label="Additional information">
        <Information />
      </ToggletipButton>
      <ToggletipContent>
        <div className="tooltip">
          <p>{t("dateCollected", "Displaying date collected")}</p>
          <p>
            <span className="label">{t("resulted", "Resulted")}: </span>{" "}
            {formatDatetime(issuedDateTime, { mode: "wide" })}
          </p>
          <p>
            <span className="label">{t("ordered", "Ordered")}: </span>{" "}
            {formatDatetime(effectiveDateTime, { mode: "wide" })}
          </p>
        </div>
      </ToggletipContent>
    </Toggletip>
  );
};

export default CommonOverview;
