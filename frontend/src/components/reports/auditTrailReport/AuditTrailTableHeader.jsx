/**
 * The audit trail columns. Header text is a plain string: a sortable column
 * builds its sort description from it, which reads "[object Object]" for an
 * element.
 */
export const auditTrailHeaderData = (intl) => [
  {
    key: "timeStamp",
    header: intl.formatMessage({ id: "audittrail.table.heading.time" }),
  },
  {
    key: "item",
    header: intl.formatMessage({ id: "audittrail.table.heading.item" }),
  },
  {
    key: "attribute",
    header: intl.formatMessage({ id: "audittrail.table.heading.action" }),
  },
  {
    key: "identifier",
    header: intl.formatMessage({ id: "audittrail.table.heading.identifier" }),
  },
  {
    key: "user",
    header: intl.formatMessage({ id: "audittrail.table.heading.user" }),
  },
  {
    key: "oldValue",
    header: intl.formatMessage({ id: "audittrail.table.heading.oldvalue" }),
  },
  {
    key: "newValue",
    header: intl.formatMessage({ id: "audittrail.table.heading.newvalue" }),
  },
];
