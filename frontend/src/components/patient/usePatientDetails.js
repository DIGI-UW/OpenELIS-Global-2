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
 */
export default function usePatientDetails(patientId) {
  const [patient, setPatient] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (!patientId) {
      setPatient(null);
      setLoading(false);
      setError(null);
      return;
    }
    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      "/rest/patient-details?patientID=" + patientId,
      (details) => {
        if (!mounted.current) return;
        if (!details || !details.patientPK) {
          setPatient(null);
          setLoading(false);
          setError(new Error("Patient not found"));
          return;
        }
        getFromOpenElisServer(
          "/rest/patient-photos/" + details.patientPK + "/false",
          (photoResp) => {
            if (!mounted.current) return;
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
