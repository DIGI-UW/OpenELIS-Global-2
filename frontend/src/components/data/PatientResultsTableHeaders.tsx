import type { IntlShape } from "react-intl";
import type { TableHeaderData } from "./LabTableHeaders";

const PATIENT_SEARCH_HEADERS: [string, string][] = [
  ["lastName", "patient.last.name"],
  ["firstName", "patient.first.name"],
  ["gender", "patient.gender"],
  ["dob", "patient.dob"],
  ["subjectNumber", "patient.subject.number"],
  ["nationalId", "patient.natioanalid"],
  ["dataSourceName", "patient.dataSourceName"],
];

/**
 * The patient search result columns with plain-text headers. Carbon builds each
 * sortable header's label ("Click to sort rows by … header") from this value,
 * so a React element here was announced as "[object Object]" (OGC-1443).
 */
export const patientSearchHeaderData = (intl: IntlShape): TableHeaderData[] =>
  PATIENT_SEARCH_HEADERS.map(([key, id]) => ({
    key,
    header: intl.formatMessage({ id }),
  }));
