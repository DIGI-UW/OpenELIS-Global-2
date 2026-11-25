import {
  labDateTimeInput,
  labTimeToInstant,
  labTimeZone,
} from "../utils/labClock";
export { labDateTimeInput, labTimeZone };
export const labInputInstant = (value) =>
  value ? labTimeToInstant(new Date(value)).toISOString() : null;
export const labTimestamp = (intl, value) =>
  value == null
    ? intl.formatMessage({ id: "not.available" })
    : `${intl.formatDate(new Date(value), { timeZone: labTimeZone() })} ${intl.formatTime(new Date(value), { timeZone: labTimeZone(), timeZoneName: "short" })}`;
export const positivityDuration = (intl, hours) => {
  if (hours == null || !Number.isFinite(Number(hours)) || Number(hours) < 0)
    return intl.formatMessage({ id: "not.available" });
  const minutes = Math.floor(Number(hours) * 60);
  return intl.formatMessage(
    { id: "microbiology.culture.elapsed" },
    {
      days: Math.floor(minutes / 1440),
      hours: Math.floor((minutes % 1440) / 60),
      minutes: minutes % 60,
    },
  );
};
export const extendedEnd = (end, amount, unit) => {
  const scale = { HOURS: 3600000, DAYS: 86400000, WEEKS: 604800000 }[unit];
  const value = Number(amount);
  if (!end || !scale || !Number.isFinite(value) || value <= 0) return null;
  return new Date(new Date(end).getTime() + value * scale).toISOString();
};
