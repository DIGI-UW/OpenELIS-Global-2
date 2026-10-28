import { format, isValid, parse } from "date-fns";
import { labNow } from "../../utils/labClock";

export const BATCH_ENDPOINT = "/rest/batch-workplans";

export const QC_OVERRIDE_PERMISSION = "qa.qc.override";

export const MAX_BATCH_NAME_LENGTH = 100;

/** QC statuses that expand their batch tile on load (FR-QC-008). */
export const AUTO_EXPAND_QC = ["OVERDUE", "FAILED", "VIOLATION", "LOT_EXPIRED"];

export const statusTagType = (status) => {
  switch (status) {
    case "ACTIVE":
      return "blue";
    case "COMPLETED":
      return "green";
    case "ARCHIVED":
      return "gray";
    case "DRAFT":
    default:
      return "purple";
  }
};

/** FR-QC-004: the Carbon Tag kind for each QC status. */
export const qcTagType = (status) => {
  switch (status) {
    case "PASS":
      return "green";
    case "OVERDUE":
    case "FAILED":
    case "VIOLATION":
      return "red";
    case "ESTABLISHMENT":
      return "purple";
    case "NOT_RUN":
    case "LOT_EXPIRED":
    case "NO_CONTROL_LOT":
    default:
      return "gray";
  }
};

/** FR-WP-005: Routine gray, ASAP purple, STAT red. */
export const priorityTagType = (priority) => {
  switch (priority) {
    case "STAT":
    case "FUTURE_STAT":
      return "red";
    case "ASAP":
    case "TIMED":
      return "purple";
    case "ROUTINE":
    default:
      return "gray";
  }
};

/** Notification kind for a non-pass QC status (FR-QC-005). */
export const qcNotificationKind = (status) =>
  status === "ESTABLISHMENT" || status === "NOT_RUN" ? "warning" : "error";

export const parseJson = async (response) => {
  if (!response) {
    return null;
  }
  try {
    return await response.json();
  } catch {
    return null;
  }
};

const displayPattern = (dateLocale) =>
  dateLocale === "fr-FR" ? "dd/MM/yyyy" : "MM/dd/yyyy";

/**
 * A date typed in the site's display format, as yyyy-MM-dd, or "" when it is
 * empty or not a date.
 */
export const displayDateToIso = (text, dateLocale) => {
  if (!text) {
    return "";
  }
  const parsed = parse(text, displayPattern(dateLocale), labNow());
  return isValid(parsed) ? format(parsed, "yyyy-MM-dd") : "";
};

export const isoToDisplayDate = (date, dateLocale) =>
  date ? format(date, displayPattern(dateLocale)) : "";

/**
 * The LocalDateTime the bench QC capture expects ("yyyy-MM-ddTHH:mm:00"), from
 * a display date and an HH:mm time, or null when either is incomplete.
 */
export const toLocalDateTime = (displayDate, time, dateLocale) => {
  const isoDate = displayDateToIso(displayDate, dateLocale);
  if (!isoDate || !/^([01]\d|2[0-3]):[0-5]\d$/.test(time || "")) {
    return null;
  }
  return `${isoDate}T${time}:00`;
};

export const formatDateTime = (intl, value) =>
  value
    ? intl.formatDate(new Date(value), {
        year: "numeric",
        month: "short",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      })
    : "";

export const formatDate = (intl, value) =>
  value
    ? intl.formatDate(new Date(value), {
        year: "numeric",
        month: "short",
        day: "2-digit",
      })
    : "";

/** The test names a batch's control lot statuses talk about. */
export const qcSubjects = (qc) => {
  const lot = (qc?.lots || []).find(
    (candidate) => candidate.status === qc.status,
  );
  return {
    test: lot?.testName || (qc?.testsWithoutControlLot || []).join(", ") || "",
    instrument: qc?.instrumentName || "",
    lot,
  };
};
