import React, { useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Button, DataTableSkeleton } from "@carbon/react";
import { ArrowRight } from "@carbon/react/icons";
import { navigate } from "../commons";
import {
  EmptyState,
  ExternalOverviewProps,
  PanelFilterProps,
  PatientData,
} from "../commons";
import { parseSingleEntry, OverviewPanelEntry } from "./useOverviewData";
import usePatientResultsData from "../loadPatientTestData/usePatientResultsData";
import CommonOverview from "./common-overview.component";
//import styles from './external-overview.scss';
import "./external-overview.scss";
import "./recent-overview.scss";

const resultsToShow = 3;

function getFilteredOverviewData(sortedObs: PatientData, filter) {
  return Object.entries(sortedObs)
    .flatMap(([panelName, { entries, type, uuid }]) => {
      return entries.map((e) => [e, uuid, type, panelName] as PanelFilterProps);
    })
    .filter(filter)
    .map(
      ([
        entry,
        uuid,
        type,
        panelName,
      ]: PanelFilterProps): OverviewPanelEntry => {
        return [
          panelName,
          type,
          parseSingleEntry(entry, type, panelName),
          new Date(entry.effectiveDateTime),
          new Date(entry.issued),
          uuid,
        ];
      },
    )
    .sort(([, , , date1], [, , , date2]) => date2.getTime() - date1.getTime());
}

function useFilteredOverviewData(
  patientUuid: string,
  filter: (filterProps: PanelFilterProps) => boolean = () => true,
) {
  const { sortedObs, loaded, error } = usePatientResultsData(patientUuid);

  const overviewData = useMemo(
    () => getFilteredOverviewData(sortedObs, filter),
    [filter, sortedObs],
  );

  return { overviewData, loaded, error };
}

const ExternalOverview: React.FC<ExternalOverviewProps> = ({
  patientUuid,
  filter,
}) => {
  const { t } = useTranslation();
  const { overviewData, loaded, error } = useFilteredOverviewData(
    patientUuid,
    filter,
  );

  const cardTitle = t("recentResults", "Recent Results");
  const handleSeeAll = useCallback(() => {
    navigate({
      to: `\${openmrsSpaBase}/patient/${patientUuid}/chart/test-results`,
    });
  }, [patientUuid]);

  return (
    <RecentResultsGrid>
      {loaded ? (
        <>
          {(() => {
            if (overviewData.length) {
              return (
                <div className="widgetCard">
                  <div className="externalOverviewHeader">
                    <h4 className="productiveHeading03 text02">{cardTitle}</h4>
                    <Button
                      kind="ghost"
                      renderIcon={(props) => (
                        <ArrowRight size={16} {...props} />
                      )}
                      iconDescription="See all results"
                      onClick={handleSeeAll}
                    >
                      {t("seeAllResults", "See all results")}
                    </Button>
                  </div>
                  <CommonOverview
                    {...{
                      patientUuid,
                      overviewData: overviewData.slice(0, resultsToShow),
                      insertSeparator: true,
                      deactivateToolbar: true,
                      isPatientSummaryDashboard: false,
                      hideToolbar: true,
                    }}
                  />
                  {overviewData.length > resultsToShow && (
                    <Button onClick={handleSeeAll} kind="ghost">
                      {t("moreResultsAvailable", "More results available")}
                    </Button>
                  )}
                </div>
              );
            } else {
              return (
                <EmptyState
                  headerTitle={cardTitle}
                  displayText={t("recentTestResults", "recent test results")}
                />
              );
            }
          })()}
        </>
      ) : (
        <DataTableSkeleton columnCount={3} />
      )}
    </RecentResultsGrid>
  );
};

export default ExternalOverview;

const RecentResultsGrid = (props) => {
  return <div {...props} className="recent-results-grid" />;
};
