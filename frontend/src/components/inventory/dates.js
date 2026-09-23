/**
 * A calendar date the user picked, written as the server expects it.
 *
 * Not `toISOString().slice(0, 10)`: a Carbon date picker hands back local
 * midnight, and converting that to UTC moves the day for every zone east of
 * Greenwich. Typed 15 October, stored the 14th. Reading the local calendar
 * fields keeps the day the user pointed at.
 */
export const toIsoDate = (date) => {
  if (!date) return null;
  const pad = (value) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};
