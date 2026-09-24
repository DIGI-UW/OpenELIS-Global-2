/**
 * action_type is a comma-joined set of these codes — the checkboxes in
 * NCECorrectiveAction.jsx are what writes them. Shared so the NCE dashboard and
 * the CAPA register read one mapping instead of a copy each.
 */
export const ACTION_TYPE_KEYS = {
  1: "banner.menu.nonconformity.correctiveActions",
  2: "nonconform.nce.preventive.action",
  3: "nonconform.nce.concurrent.control.action",
};

/** Renders the codes as a comma-separated label list; "" when there are none. */
export const formatActionType = (actionType, intl) =>
  (actionType || "")
    .split(",")
    .map((c) => c.trim())
    .filter((c) => ACTION_TYPE_KEYS[c])
    .map((c) => intl.formatMessage({ id: ACTION_TYPE_KEYS[c] }))
    .join(", ");
