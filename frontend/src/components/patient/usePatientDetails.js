import { useEffect, useRef, useState } from "react";
import { getFromOpenElisServer } from "../utils/Utils";

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
export default function usePatientDetails(patientId) {
  const [patient, setPatient] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const mounted = useRef(true);
  const currentRequestId = useRef(null);

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
      (details) => {
        if (!isStillCurrent()) return;
        if (!details || !details.patientPK) {
          setPatient(null);
          setLoading(false);
          setError(new Error("Patient not found"));
          return;
        }
        getFromOpenElisServer(
          "/rest/patient-photos/" + details.patientPK + "/false",
          (photoResp) => {
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
