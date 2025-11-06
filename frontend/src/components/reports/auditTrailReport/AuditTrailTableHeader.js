import { FormattedMessage } from "react-intl";
import React from "react";
export const auditTrailHeaderData = [
  {
    key: "timeStamp",
    header: <FormattedMessage id="audittrail.table.heading.time" />,
  },
  {
    key: "item",
    header: <FormattedMessage id="audittrail.table.heading.item" />,
  },
  {
    key: "attribute",
    header: <FormattedMessage id="audittrail.table.heading.action" />,
  },
  {
    key: "identifier",
    header: <FormattedMessage id="audittrail.table.heading.identifier" />,
  },
  {
    key: "user",
    header: <FormattedMessage id="audittrail.table.heading.user" />,
  },
  {
    key: "oldValue",
    header: <FormattedMessage id="audittrail.table.heading.oldvalue" />,
  },
  {
    key: "newValue",
    header: <FormattedMessage id="audittrail.table.heading.newvalue" />,
  },
];
