// Backend serializes LocalDate as [year, month, day] (Jackson default with
// WRITE_DATES_AS_TIMESTAMPS). React renders arrays of children by joining
// without separators, so `<td>{date}</td>` becomes "202122" for [2021, 2, 2].
// Normalize to a "YYYY-MM-DD" string at every render/state-init site.
export function toDateString(d) {
  if (!d) return "";
  if (typeof d === "string") return d;
  if (Array.isArray(d) && d.length >= 3) {
    return `${d[0]}-${String(d[1]).padStart(2, "0")}-${String(d[2]).padStart(2, "0")}`;
  }
  return "";
}
