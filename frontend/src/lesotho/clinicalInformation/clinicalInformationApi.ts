// LESOTHO: load and save the Clinical Information section of Add Order.
// The order is saved first; this section is then saved for that order's lab
// number at /rest/lesotho/order-clinical-information/{labNumber}.
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../components/utils/Utils";
import type { ClinicalInformationValues } from "./ClinicalInformation";

const endpoint = (labNumber: string) =>
  `/rest/lesotho/order-clinical-information/${encodeURIComponent(labNumber)}`;

/** Calls onLoaded with the order's saved clinical information, if any. */
export const loadClinicalInformation = (
  labNumber: string | null | undefined,
  onLoaded: (values: ClinicalInformationValues) => void,
) => {
  if (!labNumber) return;
  getFromOpenElisServer(endpoint(labNumber), (response) => {
    const data = (response as { data?: unknown } | undefined)?.data;
    if (data && typeof data === "object") {
      onLoaded(data as ClinicalInformationValues);
    }
  });
};

/**
 * Saves the section for the order. Nothing is sent when the section was
 * never filled in or loaded, so an untouched section cannot wipe saved data.
 * onDone receives true when the server saved it (or found it unchanged).
 */
export const saveClinicalInformation = (
  labNumber: string | number | null | undefined,
  values: ClinicalInformationValues | undefined,
  onDone: (ok: boolean) => void = () => {},
) => {
  if (!labNumber || !values) return;
  postToOpenElisServerJsonResponse(
    endpoint(String(labNumber)),
    JSON.stringify(values),
    (response) => {
      const r = response as
        | { error?: unknown; statusCode?: number }
        | undefined;
      onDone(!!r && !r.error && !((r.statusCode ?? 0) >= 400));
    },
  );
};
