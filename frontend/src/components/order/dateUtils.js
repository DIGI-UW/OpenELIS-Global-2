import {
  differenceInCalendarDays,
  format,
  isValid,
  parse,
  parseISO,
} from "date-fns";

export const configuredDatePattern = (dateLocale) =>
  dateLocale === "fr-FR" ? "dd/MM/yyyy" : "MM/dd/yyyy";

export const formatIsoDateForBackend = (isoDate, dateLocale) => {
  if (!isoDate) {
    return "";
  }
  if (isoDate.includes("/")) {
    return isoDate;
  }
  const parsed = parseISO(isoDate.slice(0, 10));
  return isValid(parsed)
    ? format(parsed, configuredDatePattern(dateLocale))
    : isoDate;
};

export const formatPickerDateForIso = (pickerDate, dateLocale) => {
  if (!pickerDate) {
    return "";
  }
  const pattern = configuredDatePattern(dateLocale);
  const parsed = parse(pickerDate, pattern, new Date());
  return isValid(parsed) && format(parsed, pattern) === pickerDate
    ? format(parsed, "yyyy-MM-dd")
    : "";
};

export const normalizeDateForState = (dateValue, dateLocale) => {
  if (!dateValue) {
    return "";
  }
  const isoDate = dateValue.slice(0, 10);
  const parsedIso = parseISO(isoDate);
  if (isValid(parsedIso) && format(parsedIso, "yyyy-MM-dd") === isoDate) {
    return isoDate;
  }
  return formatPickerDateForIso(dateValue, dateLocale);
};

export const isCollectionDateBeforeAdmissionDate = (
  collectionDate,
  admissionDate,
) =>
  Boolean(collectionDate && admissionDate) &&
  collectionDate.slice(0, 10) < admissionDate.slice(0, 10);

export const daysBetweenIsoDates = (startDate, endDate) => {
  if (!startDate || !endDate) {
    return null;
  }
  const start = parseISO(startDate.slice(0, 10));
  const end = parseISO(endDate.slice(0, 10));
  return isValid(start) && isValid(end)
    ? differenceInCalendarDays(end, start)
    : null;
};
