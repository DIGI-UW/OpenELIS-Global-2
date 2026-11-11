/**
 * Whether a CAPA row reads as open, overdue or completed.
 *
 * The backend sends dueDate/dateCompleted as yyyy-MM-dd strings, so a lexical
 * compare is a date compare. Shared by the CAPA register and the QA Overview's
 * overdue-CAPAs row so the two counts agree.
 */
export function deriveStatus(row, today) {
  if ((row.nceStatus || "").toLowerCase() === "completed") {
    return "completed";
  }
  if (row.dueDate && row.dueDate < today) {
    return "overdue";
  }
  return "open";
}
