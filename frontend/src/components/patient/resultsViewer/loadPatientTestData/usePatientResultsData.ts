import React from "react";
import { PatientData } from "../commons";
import loadPatientData from "./loadPatientData";

type LoadingState = {
  sortedObs: PatientData;
  loaded: boolean;
  error: Object | undefined;
};

const usePatientResultsData = (patientUuid: string): LoadingState => {
  const [state, setState] = React.useState<LoadingState>({
    sortedObs: {},
    loaded: false,
    error: undefined,
  });

  React.useEffect(() => {
    let unmounted = false;
    if (patientUuid) {
      const [data, reloadedDataPromise] = loadPatientData(patientUuid);
      if (!!data) setState({ sortedObs: data, loaded: true, error: undefined });
      reloadedDataPromise.then((reloadedData) => {
        if (reloadedData !== data && !unmounted)
          setState({ sortedObs: reloadedData, loaded: true, error: undefined });
      });
    }
    return () => {
      unmounted = true;
    };
  }, [patientUuid]);

  return state;
};

export default usePatientResultsData;
