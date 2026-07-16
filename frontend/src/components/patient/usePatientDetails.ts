import { useEffect, useRef, useState } from "react";
import { getFromOpenElisServer } from "../utils/Utils";
import type { Nullable, PatientRecord } from "./types";

/**
 * Fetch a patient's full PatientInfoBean (+ photo) by id. Returns
 * { patient, loading, error }. Used by patient-menu pages that drive
 * form state from the URL (`/PatientManagement/:patientId`) instead of
 * relying on the parent to pre-fetch and prop-drill.
 *
 * Mirrors the two REST calls SearchPatientForm makes when a user picks
 * a patient: /rest/patient-details + /rest/patient-photos. The photo is
 * attached to the returned object as `photo` (base64 string), matching
 * the shape CreatePatientForm.buildInitialFormValues expects.
 *
 * Pass `null`/`undefined` for patientId to disable the fetch (search
 * mode, new-patient mode).
 *
 * The hook tracks the most recently requested patientId in a ref and
 * drops late callbacks whose captured id no longer matches — without
 * this, switching patients faster than the network can resolve lets the
 * older response overwrite the newer one.
 */
interface PatientDetailsHookResult {
  patient: Nullable<PatientRecord>;
  loading: boolean;
  error: Nullable<Error>;
}

interface PatientPhotoResponse {
  data?: string;
}

export default function usePatientDetails(
  patientId?: Nullable<string>,
): PatientDetailsHookResult {
  const [patient, setPatient] = useState<Nullable<PatientRecord>>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Nullable<Error>>(null);
  const mounted = useRef(true);
  const currentRequestId = useRef<Nullable<string>>(null);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (!patientId) {
      currentRequestId.current = null;
      setPatient(null);
      setLoading(false);
      setError(null);
      return;
    }

    const requestedId = patientId;
    currentRequestId.current = requestedId;
    setLoading(true);
    setError(null);

    const isStillCurrent = () =>
      mounted.current && currentRequestId.current === requestedId;

    getFromOpenElisServer(
      "/rest/patient-details?patientID=" + requestedId,
      (details: PatientRecord) => {
        if (!isStillCurrent()) return;
        if (!details || !details.patientPK) {
          setPatient(null);
          setLoading(false);
          setError(new Error("Patient not found"));
          return;
        }
        getFromOpenElisServer(
          "/rest/patient-photos/" + details.patientPK + "/false",
          (photoResp: PatientPhotoResponse) => {
            if (!isStillCurrent()) return;
            const photo = photoResp && photoResp.data ? photoResp.data : "";
            setPatient({ ...details, photo });
            setLoading(false);
          },
        );
      },
    );
  }, [patientId]);

  return { patient, loading, error };
}
