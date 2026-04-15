import React from "react";
import { PatientData } from "../commons";
import loadPatientData from "./loadPatientData";

type LoadingState = {
  sortedObs: PatientData;
  loaded: boolean;
  error: unknown;
};

const usePatientResultsData = (patientUuid: string): LoadingState => {
  const [state, setState] = React.useState<LoadingState>({
    sortedObs: {},
    loaded: false,
    error: undefined,
  });

  React.useEffect(() => {
    let unmounted = false;

    if (!patientUuid) {
      setState({ sortedObs: {}, loaded: true, error: undefined });
      return () => {
        unmounted = true;
      };
    }

    const [data, reloadedDataPromise] = loadPatientData(patientUuid);

    setState({
      sortedObs: data ?? {},
      loaded: Boolean(data),
      error: undefined,
    });

    reloadedDataPromise
      .then((reloadedData) => {
        if (!unmounted) {
          setState({
            sortedObs: reloadedData ?? data ?? {},
            loaded: true,
            error: undefined,
          });
        }
      })
      .catch((error) => {
        if (!unmounted) {
          setState({
            sortedObs: data ?? {},
            loaded: true,
            error: data ? undefined : error,
          });
        }
      });

    return () => {
      unmounted = true;
    };
  }, [patientUuid]);

  return state;
};

export default usePatientResultsData;
