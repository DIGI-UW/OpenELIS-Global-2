import React from "react";
import { Tag } from "@carbon/react";
import { FormattedMessage } from "react-intl";

// SubcontractStatus enum mirror (org.openelisglobal.referral.valueholder.SubcontractStatus).
const STATUS_CONFIG = {
  DRAFT: { type: "gray", labelId: "label.referOut.status.draft" },
  DISPATCHED: { type: "blue", labelId: "label.referOut.status.dispatched" },
  RECEIVED: { type: "teal", labelId: "label.referOut.status.received" },
  RESULTS_RETURNED: {
    type: "cyan",
    labelId: "label.referOut.status.resultsReturned",
  },
  CLOSED: { type: "green", labelId: "label.referOut.status.closed" },
};

const SubcontractStatusTag = ({ status, size = "sm" }) => {
  if (!status) {
    return (
      <Tag type="outline" size={size}>
        <FormattedMessage
          id="label.referOut.status.notReferred"
          defaultMessage="In-house"
        />
      </Tag>
    );
  }
  const config = STATUS_CONFIG[status] || {
    type: "gray",
    labelId: "label.referOut.status.draft",
  };
  return (
    <Tag type={config.type} size={size}>
      <FormattedMessage id={config.labelId} defaultMessage={status} />
    </Tag>
  );
};

export default SubcontractStatusTag;
