import React from "react";
import useOverviewData from "./useOverviewData";
import CommonOverview from "./common-overview.component";
import { DataTableSkeleton } from "@carbon/react";
import { navigate } from "../commons";

const defaultOpenTimeline = (patientUuid, panelUuid) => {
  const url = `/patient/${patientUuid}/testresults/timeline/${panelUuid}`;

  navigate({ to: url });
};

interface LabResultProps {
  openTimeline?: (panelUuid: string) => void;
  openTrendline?: (panelUuid: string, testUuid: string) => void;
}

interface LabResultParams {
  patientUuid: string;
}

export const Overview: React.FC<LabResultProps & LabResultParams> = ({
  patientUuid,
  openTimeline = (panelUuid) => defaultOpenTimeline(patientUuid, panelUuid),
  openTrendline,
}) => {
  const { overviewData, loaded } = useOverviewData(patientUuid);

  return (
    <>
      {loaded ? (
        <CommonOverview
          overviewData={overviewData}
          openTimeline={openTimeline}
          openTrendline={openTrendline}
        />
      ) : (
        <DataTableSkeleton columnCount={3} />
      )}
    </>
  );
};
