import { useState, useEffect } from "react";
import { ObsRecord, OBSERVATION_INTERPRETATION } from "../commons";
import usePatientResultsData from "../loadPatientTestData/usePatientResultsData";

export interface OverviewPanelData {
  id: string;
  key?: string;
  name: string;
  range: string;
  interpretation: OBSERVATION_INTERPRETATION;
  value?: string | number;
  valueCodeableConcept?: Coding;
}

interface Coding {
  coding: Array<{ code: string; display: string }>;
}

export type OverviewPanelEntry = [
  string,
  string,
  Array<OverviewPanelData>,
  Date,
  Date,
  string,
];

export function parseSingleEntry(
  entry: ObsRecord,
  type: string,
  panelName: string,
): Array<OverviewPanelData> {
  if (type === "Test") {
    return [
      {
        id: entry.id,
        name: panelName,
        range: entry.meta?.range || "--",
        interpretation: entry.meta.assessValue
          ? entry.meta.assessValue(entry.value)
          : "--",
        value: entry.value,
      },
    ];
  } else {
    return entry.members.map((gm) => ({
      id: gm.id,
      key: gm.id,
      name: gm.name,
      range: gm.meta?.range || "--",
      interpretation: gm.meta.assessValue(gm.value),
      value: gm.value,
    }));
  }
}

function useOverviewData(patientUuid: string) {
  const { sortedObs, loaded, error } = usePatientResultsData(patientUuid);
  const [overviewData, setDisplayData] = useState<Array<OverviewPanelEntry>>(
    [],
  );

  useEffect(() => {
    setDisplayData(
      Object.entries(sortedObs)
        .map(([panelName, { entries, type, uuid }]): OverviewPanelEntry => {
          const newestEntry = entries[0];

          return [
            panelName,
            type,
            parseSingleEntry(newestEntry, type, panelName),
            new Date(newestEntry.effectiveDateTime),
            new Date(newestEntry.issued),
            uuid,
          ];
        })
        .sort(
          ([, , , date1], [, , , date2]) => date2.getTime() - date1.getTime(),
        ),
    );
  }, [sortedObs]);

  return { overviewData, loaded, error };
}

export default useOverviewData;
