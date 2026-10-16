/**
 * The toast after a save names the order, so a user saving several orders in a
 * row can tell which one went through (OGC-1483).
 */
export const orderSavedMessage = (intl, labNumber) =>
  labNumber
    ? intl.formatMessage({ id: "save.order.success.labNumber" }, { labNumber })
    : intl.formatMessage({ id: "save.order.success.msg" });
